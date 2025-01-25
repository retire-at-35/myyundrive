import SparkMD5 from 'spark-md5'
import { uploadFile } from '@/api/file'

const CHUNK_SIZE = 5 * 1024 * 1024 // 5MB

export class FileUploader {
  constructor(file, filePid) {
    this.file = file
    this.filePid = filePid
    this.chunks = Math.ceil(file.size / CHUNK_SIZE)
    this.currentChunk = 0
    this.fileId = ''
    this.status = ''
  }

  // 计算文件的MD5
  async calculateMD5() {
    return new Promise((resolve, reject) => {
      const spark = new SparkMD5.ArrayBuffer()
      const reader = new FileReader()
      const chunks = []
      let currentChunk = 0

      // 读取文件分片
      const loadNext = () => {
        const start = currentChunk * CHUNK_SIZE
        const end = Math.min(start + CHUNK_SIZE, this.file.size)
        reader.readAsArrayBuffer(this.file.slice(start, end))
      }

      reader.onload = (e) => {
        spark.append(e.target.result)
        currentChunk++

        if (currentChunk < this.chunks) {
          loadNext()
        } else {
          resolve(spark.end())
        }
      }

      reader.onerror = () => {
        reject(new Error('MD5计算失败'))
      }

      loadNext()
    })
  }

  // 上传单个分片
  async uploadChunk(fileMd5) {
    const start = this.currentChunk * CHUNK_SIZE
    const end = Math.min(start + CHUNK_SIZE, this.file.size)
    const chunk = this.file.slice(start, end)

    const formData = new FormData()
    formData.append('file', chunk)
    
    const result = await uploadFile({
      fileId: this.fileId,
      filePid: this.filePid,
      fileName: this.file.name,
      fileMd5: fileMd5,
      chunkIndex: this.currentChunk,
      chunks: this.chunks,
      file: chunk
    })

    if (result.code === 1) {
      this.fileId = result.data.fileId
      this.status = result.data.status
      this.currentChunk++
      return result.data
    }
    throw new Error(result.msg || '上传失败')
  }

  // 开始上传
  async upload(onProgress) {
    try {
      const fileMd5 = await this.calculateMD5()
      
      while (this.currentChunk < this.chunks) {
        const result = await this.uploadChunk(fileMd5)
        
        // 计算上传进度
        const progress = Math.round((this.currentChunk / this.chunks) * 100)
        onProgress && onProgress(progress)

        // 如果返回秒传或上传完成，就结束上传
        if (result.status === 'upload_second' || result.status === 'upload_finish') {
          return {
            status: 'success',  // 统一返回 success 状态
            fileId: this.fileId
          }
        }
      }

      return {
        status: 'success',
        fileId: this.fileId
      }
    } catch (error) {
      throw new Error('文件上传失败：' + error.message)
    }
  }
} 
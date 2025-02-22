<template>
  <el-dialog
    v-if="file"
    v-model="visible"
    :title="fileName"
    :width="dialogWidth"
    :fullscreen="false"
    destroy-on-close
    class="file-preview-dialog"
  >
    <!-- 视频预览 -->
    <div v-if="isVideo" class="video-container">
      <video-player
        v-if="file"
        :file-id="file.fileId"
        :user-id="file.userId"
        class="video-player"
      />
    </div>

    <!-- PDF预览 -->
    <embed
      v-else-if="isPDF"
      :src="previewUrl"
      type="application/pdf"
      class="pdf-viewer"
    />

    <!-- 图片预览 -->
    <div v-else-if="isImage" class="image-container">
      <el-image
        :src="previewUrl"
        fit="contain"
        :preview-src-list="[previewUrl]"
        :initial-index="0"
        class="image-viewer"
      />
    </div>

    <!-- 音频预览 -->
    <div v-else-if="isAudio" class="audio-container">
      <audio
        :src="previewUrl"
        controls
        class="audio-player"
      />
    </div>

    <!-- 文本预览 -->
    <div v-else-if="isTextPreviewable" class="text-container">
      <pre class="text-content">{{ textContent }}</pre>
    </div>

    <!-- Excel预览 -->
    <div v-else-if="isExcel" class="excel-container">
      <div class="excel-toolbar">
        <el-select v-model="currentSheet" placeholder="选择工作表" size="small">
          <el-option
            v-for="sheet in sheetList"
            :key="sheet"
            :label="sheet"
            :value="sheet"
          />
        </el-select>
      </div>
      <div class="excel-content">
        <el-table
          :data="excelData"
          border
          height="100%"
          style="width: 100%"
        >
          <el-table-column
            v-for="(col, index) in excelColumns"
            :key="index"
            :prop="col.prop"
            :label="col.label"
          />
        </el-table>
      </div>
    </div>

    <!-- Word预览 -->
    <div v-else-if="isDoc" class="doc-container">
      <iframe
        :src="`https://view.officeapps.live.com/op/embed.aspx?src=${encodeURIComponent(previewUrl)}`"
        width="100%"
        height="100%"
        frameborder="0"
      />
    </div>

    <!-- 不支持预览的文件类型 -->
    <div v-else class="unsupported">
      <el-icon :size="48"><Files /></el-icon>
      <p>{{ getUnsupportedMessage }}</p>
      <el-button type="primary" @click="handleDownload">下载文件</el-button>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { Files } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getVideoInfo, getFile } from '@/api/file'
import VideoPlayer from '@/components/VideoPlayer.vue'
import * as XLSX from 'xlsx'

const props = defineProps({
  modelValue: Boolean,
  file: {
    type: Object,
    required: true,
    default: () => ({})
  }
})

const emit = defineEmits(['update:modelValue', 'preview-error'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const fileName = computed(() => props.file?.fileName ?? '')
const fileType = computed(() => props.file?.fileType ?? 0)
const previewUrl = ref('')
const textContent = ref('')

// 判断文件类型
const isVideo = computed(() => fileType.value === 1)
const isAudio = computed(() => fileType.value === 2)
const isImage = computed(() => fileType.value === 3)
const isPDF = computed(() => fileType.value === 4)
const isDoc = computed(() => fileType.value === 9)
const isExcel = computed(() => fileType.value === 6)
const isTxt = computed(() => fileType.value === 7)
const isCode = computed(() => fileType.value === 8)

// 判断是否可以文本预览
const isTextPreviewable = computed(() => {
  // 如果文件类型是文本或代码，或者文件扩展名在支持列表中
  return isTxt.value || isCode.value || isTextFile(props.file?.fileName || '')
})

// 判断是否为文本文件
const isTextFile = (fileName) => {
  const textExtensions = [
    '.txt', '.json', '.xml', '.md', '.log',
    '.js', '.ts', '.java', '.py', '.cpp', '.c', '.h',
    '.html', '.css', '.scss', '.less',
    '.yml', '.yaml', '.properties', '.conf',
    '.sh', '.bat'
  ]
  const ext = '.' + fileName.toLowerCase().split('.').pop()
  return textExtensions.includes(ext)
}

// 判断是否为不支持预览的文件类型
const isUnsupported = computed(() => {
  // 如果是文本可预览，则不算不支持
  if (isTextPreviewable.value) return false
  // 如果是视频、音频、图片、PDF、Word、Excel，则支持预览
  if ([1, 2, 3, 4, 5, 6].includes(fileType.value)) return false
  // 其他类型都不支持预览
  return true
})

// 设置对话框宽度
const dialogWidth = computed(() => {
  if (isVideo.value) return '800px'
  if (isImage.value) return '60%'
  if (isDoc.value || isExcel.value || isPDF.value) return '90%'
  return '80%'
})

// 获取不支持预览的提示信息
const getUnsupportedMessage = computed(() => {
  if (fileType.value === 9) return '暂不支持压缩文件在线预览，请下载后查看'
  return '该文件类型暂不支持预览，请下载后查看'
})

// Excel 相关的状态
const currentSheet = ref('')
const sheetList = ref([])
const excelData = ref([])
const excelColumns = ref([])

// 处理 Excel 文件
const handleExcelFile = async (file) => {
  try {
    const response = await getFile(file.fileId, file.userId)
    const data = await response.arrayBuffer()
    const workbook = XLSX.read(data, { type: 'array' })
    
    // 获取所有工作表名称
    sheetList.value = workbook.SheetNames
    currentSheet.value = workbook.SheetNames[0]

    // 读取第一个工作表的数据
    loadSheetData(workbook, currentSheet.value)
  } catch (error) {
    console.error('Excel 文件加载失败:', error)
    ElMessage.error('Excel 文件加载失败')
  }
}

// 加载指定工作表的数据
const loadSheetData = (workbook, sheetName) => {
  const worksheet = workbook.Sheets[sheetName]
  const jsonData = XLSX.utils.sheet_to_json(worksheet, { header: 1 })
  
  if (jsonData.length > 0) {
    // 获取表头
    const headers = jsonData[0]
    excelColumns.value = headers.map((header, index) => ({
      prop: `col${index}`,
      label: header || `Column ${index + 1}`
    }))

    // 处理数据行
    excelData.value = jsonData.slice(1).map(row => {
      const rowData = {}
      row.forEach((cell, index) => {
        rowData[`col${index}`] = cell
      })
      return rowData
    })
  }
}

// 监听工作表切换
watch(currentSheet, (newSheet) => {
  if (newSheet && props.file) {
    handleExcelFile(props.file)
  }
})

// 监听对话框打开
watch(() => visible.value, async (val) => {
  if (!val || !props.file) return
  
  if (isUnsupported.value) {
    emit('preview-error', props.file)
    visible.value = false
    return
  }

  if (isVideo.value) {
    return
  }

  await getPreviewUrl()
})

// 获取预览URL和内容
const getPreviewUrl = async () => {
  if (!props.file) {
    console.warn('No file provided for preview')
    return
  }

  try {
    if (isExcel.value) {
      await handleExcelFile(props.file)
    } else {
      const response = await getFile(props.file.fileId, props.file.userId)
      
      if (isTextPreviewable.value) {
        const reader = new FileReader()
        reader.onload = (e) => {
          textContent.value = e.target.result
        }
        reader.readAsText(response)
      } else {
        const contentType = getContentType(props.file.fileName)
        const blob = new Blob([response], { type: contentType })
        previewUrl.value = URL.createObjectURL(blob)
      }
    }
  } catch (error) {
    console.warn('获取文件预览失败:', error)
    emit('preview-error', props.file)
  }
}

// 获取文件的 Content-Type
const getContentType = (fileName) => {
  const ext = fileName.toLowerCase().split('.').pop()
  const mimeTypes = {
    pdf: 'application/pdf',
    jpg: 'image/jpeg',
    jpeg: 'image/jpeg',
    png: 'image/png',
    gif: 'image/gif',
    mp3: 'audio/mpeg',
    mp4: 'video/mp4',
    webm: 'video/webm',
    m3u8: 'application/vnd.apple.mpegurl',
    ts: 'video/mp2t',
    txt: 'text/plain',
    json: 'application/json',
    xml: 'text/xml',
    js: 'text/javascript',
    css: 'text/css',
    html: 'text/html',
    java: 'text/plain',
    py: 'text/plain',
    cpp: 'text/plain',
    c: 'text/plain',
    h: 'text/plain',
    md: 'text/plain',
    log: 'text/plain'
  }
  return mimeTypes[ext] || 'application/octet-stream'
}

// 下载文件
const handleDownload = () => {
  if (!previewUrl.value || !props.file) return
  
  const link = document.createElement('a')
  link.href = previewUrl.value
  link.download = props.file.fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

// 组件销毁时清理URL
onBeforeUnmount(() => {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
  }
})
</script>

<style scoped>
.file-preview-dialog :deep(.el-dialog__body) {
  padding: 0;
  height: calc(95vh - 120px);
  overflow: auto;
}

.video-container {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #000;
  padding: 20px;
}

.video-player {
  width: 100%;
  height: 100%;
  max-height: calc(90vh - 160px);
}

.text-container {
  width: 100%;
  height: 100%;
  padding: 20px;
  background: #f5f7fa;
  overflow: auto;
}

.text-content {
  margin: 0;
  padding: 20px;
  background: #fff;
  border-radius: 4px;
  font-family: monospace;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.pdf-viewer {
  width: 100%;
  height: 100%;
  min-height: calc(95vh - 120px);
  border: none;
}

.image-container {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #f5f7fa;
}

.image-viewer {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.audio-container {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #f5f7fa;
}

.audio-player {
  width: 80%;
  margin: 20px 0;
}

.unsupported {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  padding: 40px;
  color: #909399;
  background: #f5f7fa;
}

.unsupported .el-icon {
  margin-bottom: 16px;
}

.unsupported p {
  margin: 16px 0;
}

.excel-container,
.doc-container {
  width: 100%;
  height: 100%;
  background: #fff;
}

.excel-container iframe,
.doc-container iframe {
  border: none;
  width: 100%;
  height: 100%;
  min-height: calc(95vh - 120px);
}

.video-container,
.pdf-viewer,
.excel-container,
.doc-container,
.text-container,
.image-container,
.audio-container {
  width: 100%;
  height: 100%;
  min-height: calc(95vh - 120px);
}

.excel-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.excel-toolbar {
  padding: 10px;
  border-bottom: 1px solid #ebeef5;
}

.excel-content {
  flex: 1;
  overflow: auto;
  padding: 10px;
}

.excel-content :deep(.el-table) {
  height: calc(95vh - 200px);
}
</style> 
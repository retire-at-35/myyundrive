import request from '@/utils/request'

// 上传文件
export function uploadFile(data) {
  const formData = new FormData()
  formData.append('fileId', data.fileId || '')
  formData.append('filePid', data.filePid)
  formData.append('fileName', data.fileName)
  formData.append('fileMd5', data.fileMd5)
  formData.append('chunkIndex', data.chunkIndex)
  formData.append('chunks', data.chunks)
  formData.append('file', data.file)

  return request({
    url: '/api/file/uploadFile',
    method: 'post',
    data: formData,
    transformRequest: [function (data) {
      return data
    }]
  })
}

// 获取文件列表
export function getFileList(params) {
  return request({
    url: '/api/file/loadPageList',
    method: 'get',
    params: {
      fileName: params.fileName || '',
      page: params.page || 1,
      pageSize: params.pageSize || 10,
      filePid: params.filePid
    }
  })
}

// 创建文件夹
export function createFolder(data) {
  return request({
    url: '/api/file/newFolder',
    method: 'post',
    data: {
      folderName: data.fileName,
      filePid: data.filePid
    }
  })
}

// 重命名文件或文件夹
export function renameFile(data) {
  return request({
    url: '/api/file/rename',
    method: 'put',
    data: {
      fileId: data.fileId,
      fileName: data.fileName
    }
  })
}

// 获取视频信息（m3u8或ts文件）
export function getVideoInfo(fileId, userId) {
  return request({
    url: '/api/file/ts/getVideoInfo',
    method: 'get',
    params: { 
      fileId,
      userId 
    },
    responseType: 'blob'
  })
}

// 获取普通文件
export function getFile(fileId, userId) {
  return request({
    url: '/api/file/getFile',
    method: 'get',
    params: { 
      fileId,
      userId 
    },
    responseType: 'blob'
  })
}

// 删除文件（移至回收站）
export function deleteFile(fileIds) {
  return request({
    url: '/api/file/deleteFile',
    method: 'delete',
    params: {
      fileIds
    }
  })
}

// 获取所有文件夹信息
export function getAllFolderInfo(filePid) {
  return request({
    url: '/api/file/getAllFolderInfo',
    method: 'get',
    params: {
      filePid: filePid || '0'
    }
  })
}

// 修改文件位置
export function changeFileFolder(data) {
  return request({
    url: '/api/file/changeFileFolder',
    method: 'put',
    data
  })
}

// 下载文件
export function downloadFiles(fileIds) {
  // 使用完整的 API URL
  const url = `${import.meta.env.VITE_API_URL}/api/file/download?fileIds=${fileIds}`
  window.open(url, '_blank')
}

// 获取回收站文件列表
export function getRecycleFileList(params) {
  return request({
    url: '/api/recycle/loadRecycleFileList',
    method: 'get',
    params: {
      fileName: params.fileName || '',
      filePid: params.filePid || '0',
      page: params.page || 1,
      pageSize: params.pageSize || 10
    }
  })
}

// 根据文件ID列表获取文件信息
export function getFileListByIds(params) {
  return request({
    url: '/api/file/loadByFileIds',
    method: 'get',
    params: {
      fileIds: params.fileIds,
      userId: params.userId,
      page: params.page || 1,
      pageSize: params.pageSize || 10
    }
  })
}

// 获取文件夹内容
export function getFileListByPid(params) {
  return request({
    url: '/api/file/loadPageList',
    method: 'get',
    params: {
      filePid: params.filePid,
      page: params.page || 1,
      pageSize: params.pageSize || 10
    }
  })
}

// 获取分享者的文件夹内容
export function getSharerFileList(params) {
  return request({
    url: '/api/file/loadSharerList',
    method: 'get',
    params: {
      userId: params.userId,
      filePid: params.filePid,
      page: params.page || 1,
      pageSize: params.pageSize || 10
    }
  })
}

// 获取文件夹大小
export function getFolderSpace(folderId, type) {
  return request({
    url: '/api/file/getFolderSpace',
    method: 'get',
    params: { 
      folderId,
      type 
    }
  })
} 
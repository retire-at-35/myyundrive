import request from '@/utils/request'

// 获取回收站文件列表
export function getRecycleFileList(params) {
  return request({
    url: '/api/recycle/loadRecycleFileList',
    method: 'get',
    params: {
      fileName: params.fileName || '',
      page: params.page || 1,
      pageSize: params.pageSize || 10
    }
  })
}

// 恢复文件
export function recoverFiles(fileIds) {
  return request({
    url: '/api/recycle/recoverFile',
    method: 'put',
    params: {
      fileIds
    }
  })
}

// 彻底删除文件
export function deleteFiles(fileIds) {
  return request({
    url: '/api/recycle/deleteFile',
    method: 'delete',
    params: {
      fileIds
    }
  })
} 
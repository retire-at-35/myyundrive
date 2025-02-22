import request from '@/utils/request'

// 获取用户列表
export function loadUserList(params) {
  return request({
    url: '/api/admin/loadUserList',
    method: 'get',
    params: {
      page: params.page,
      pageSize: params.pageSize,
      email: params.email,
      nickName: params.nickName
    }
  })
}

// 更新用户状态
export function updateUserStatus(userId, status) {
  return request({
    url: '/api/admin/updateUserStatus',
    method: 'put',
    data: {
      userId,
      status
    }
  })
}

// 更新用户空间大小
export function updateUserSpace(userId, changeSpace) {
  return request({
    url: '/api/admin/updateUserSpace',
    method: 'put',
    data: {
      userId,
      changeSpace
    }
  })
}

// 获取所有文件列表
export function loadAllFileList(params) {
  return request({
    url: '/api/admin/loadAllFileInfo',
    method: 'get',
    params: {
      page: params.page,
      pageSize: params.pageSize,
      fileName: params.fileName
    }
  })
}

// 删除文件
export function deleteFile(userId, fileId) {
  return request({
    url: '/api/admin/deleteFile',
    method: 'delete',
    params: {
      userId,
      fileId
    }
  })
}

// // 下载文件
// export function downloadFiles(fileId) {
//   return request({
//     url: '/api/file/download',
//     method: 'get',
//     params: {
//       fileId
//     },
//     responseType: 'blob'  // 设置响应类型为 blob
//   })
// } 


export function downloadFiles(userId,fileIds) {
  // 使用完整的 API URL
  const url = `${import.meta.env.VITE_API_URL}/api/admin/download?fileIds=${fileIds}&userId=${userId}`
  window.open(url, '_blank')
}
import request from '@/utils/request'

// 创建分享
export function createShare(data) {
  return request({
    url: '/api/share/shareFile',
    method: 'post',
    data
  })
}

// 获取分享信息
export function getShareInfo(shareId) {
  return request({
    url: '/api/share/getShareById',
    method: 'get',
    params: { shareId }
  })
}

// 验证提取码
export function verifyShareCode(shareId, code) {
  return request({
    url: '/api/share/getShareInfoByCodeAndShareId',
    method: 'get',
    params: { shareId, code }
  })
}

// 保存分享文件到我的网盘
export function saveShare2Myself(data) {
  return request({
    url: '/api/share/saveShare2Myself',
    method: 'post',
    data
  })
}

// 获取分享列表
export function loadShareList(params) {
  return request({
    url: '/api/share/loadShareList',
    method: 'get',
    params: {
      page: params.page || 1,
      pageSize: params.pageSize || 10
    }
  })
}

// 取消分享
export function cancelShare(shareIds) {
  return request({
    url: '/api/share/cancelShare',
    method: 'post',
    params: { shareIds }
  })
}

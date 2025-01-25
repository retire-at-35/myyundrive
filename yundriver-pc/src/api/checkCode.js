import request from '@/utils/request'

// 获取验证码图片
export function getCheckCode(type) {
  return request({
    url: '/api/checkCode',
    method: 'get',
    params: { type },
    responseType: 'blob',
    withCredentials: true
  })
} 
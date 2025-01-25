import request from '@/utils/request'

// 发送邮箱验证码
export function sendEmailCode(data) {
  return request({
    url: '/api/sendEmailCode',
    method: 'post',
    data: {
      email: data.email,
      checkCode: data.checkCode,
      type: data.type
    }
  })
} 
import request from '@/utils/request'

// 用户注册
export function register(data) {
  return request({
    url: '/api/register',
    method: 'post',
    data: {
      email: data.email,
      nickName: data.nickName,
      password: data.password,
      checkCode: data.checkCode,
      emailCode: data.emailCode
    }
  })
}

// 用户登录
export function login(data) {
  return request({
    url: '/api/login',
    method: 'post',
    data: {
      email: data.email,
      password: data.password,
      checkCode: data.checkCode
    }
  })
}

// 重置密码
export function resetPassword(data) {
  return request({
    url: '/api/forgetPassword',
    method: 'post',
    data: {
      email: data.email,
      password: data.password,
      emailCode: data.emailCode,
      checkCode: data.checkCode
    }
  })
}

// 修改密码
export function updatePassword(data) {
  return request({
    url: '/api/updatePassword',
    method: 'put',
    data: {
      oldPassword: data.oldPassword,
      password: data.password,
      checkCode: data.checkCode
    }
  })
}

// 更新头像
export function updateAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/api/upload/updateAvatar',
    method: 'put',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 退出登录
export function logout() {
  return request({
    url: '/api/logout',
    method: 'post'
  })
}

// 获取用户空间信息
export function getUserSpace() {
  return request({
    url: '/api/getSpace',
    method: 'get'
  })
} 
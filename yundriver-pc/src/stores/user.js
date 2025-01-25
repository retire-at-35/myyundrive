import { defineStore } from 'pinia'
import { ref } from 'vue'

const STORAGE_KEY = 'cloud_storage_user_info'

export const useUserStore = defineStore('user', () => {
  // 从 localStorage 初始化用户信息
  const userInfo = ref(JSON.parse(localStorage.getItem(STORAGE_KEY)) || {
    nickName: '',
    userId: '',
    isAdmin: false,
    avatar: ''
  })

  // 从 localStorage 初始化登录状态
  const isLogin = ref(!!localStorage.getItem(STORAGE_KEY))

  // 设置用户信息
  const setUserInfo = (info) => {
    userInfo.value = info
    isLogin.value = true
    // 保存到 localStorage
    localStorage.setItem(STORAGE_KEY, JSON.stringify(info))
  }

  // 清除用户信息
  const clearUserInfo = () => {
    userInfo.value = {
      nickName: '',
      userId: '',
      isAdmin: false,
      avatar: ''
    }
    isLogin.value = false
    // 从 localStorage 移除
    localStorage.removeItem(STORAGE_KEY)
  }

  return {
    userInfo,
    isLogin,
    setUserInfo,
    clearUserInfo
  }
}) 
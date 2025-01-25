import axios from 'axios'
import {useRouter} from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

axios.defaults.withCredentials = true
const instance = axios.create({
  baseURL: 'http://114.132.59.198:8081',
  timeout: 8000,
  withCredentials: true
})
// axios请求拦截器
instance.interceptors.request.use(config => {
  return config
}, e => Promise.reject(e))

// 处理未登录情况
const handleUnauthorized = () => {
  ElMessage.error('请先登录')
  // 清除登录信息
  localStorage.removeItem('userInfo')
  const userStore = useUserStore()
  userStore.clearUserInfo()
  
  // 保存当前路径
  const currentPath = router.currentRoute.value.fullPath
  
  // 使用 nextTick 确保在下一个事件循环再执行跳转
  setTimeout(() => {
    router.replace({
      path: '/login',
      query: currentPath !== '/login' ? { redirect: currentPath } : undefined
    })
  }, 0)
}

// axios 响应拦截器
instance.interceptors.response.use(
  response => {
    const res = response.data
    
    // 如果响应的是二进制数据，直接返回
    if (response.config.responseType === 'blob') {
      return response.data
    }

    // 处理业务状态码
    if (res.code === 1) {
      return res
    } else if (res.code === 401) {
      // 未登录或登录过期
      handleUnauthorized()
      return Promise.reject(new Error('未登录'))
    } else {
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
  },
  error => {
    // 处理 HTTP 错误状态码
    if (error.response) {
      if (error.response.status === 401) {
        handleUnauthorized()
      } else {
        ElMessage.error(error.response.data?.message || '请求失败')
      }
    } else {
      ElMessage.error('网络错误，请稍后重试')
    }
    return Promise.reject(error)
  }
)
export default instance

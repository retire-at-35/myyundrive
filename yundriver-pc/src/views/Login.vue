<template>
  <div class="auth-container">
    <div class="auth-box">
      <el-card class="auth-card">
        <div class="header">
          <img src="../assets/logo.png" class="logo" alt="logo" />
          <h2 class="title">用户登录</h2>
          <p class="subtitle">欢迎使用云存储平台</p>
        </div>

        <el-form 
          :model="loginForm" 
          :rules="rules" 
          ref="loginFormRef"
          class="auth-form"
        >
          <el-form-item prop="email">
            <el-input 
              v-model="loginForm.email"
              placeholder="请输入账号"
              prefix-icon="User"
            />
          </el-form-item>
          
          <el-form-item prop="password">
            <el-input 
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <el-form-item prop="verifyCode">
            <div class="verify-code-container">
              <el-input 
                v-model="loginForm.verifyCode"
                placeholder="请输入验证码"
                class="verify-code-input"
              />
              <div class="verify-code-img">
                <img :src="verifyCodeUrl" alt="验证码" @click="refreshVerifyCode" />
              </div>
            </div>
          </el-form-item>

          <el-form-item>
            <el-button 
              type="primary" 
              class="submit-button" 
              :loading="loading"
              @click="handleLogin"
            >
              登录
            </el-button>
          </el-form-item>

          <div class="operation-links">
            <router-link to="/register" replace>注册账号</router-link>
            <router-link to="/forgot-password" replace>忘记密码？</router-link>
          </div>
        </el-form>
      </el-card>

      <div class="footer">
        <p></p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { login } from '@/api/user'
import { getCheckCode } from '@/api/checkCode'
import md5 from 'js-md5'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)  // 添加 loading 状态
const loginFormRef = ref(null)
const verifyCodeUrl = ref('')

const loginForm = reactive({
  email: '',
  password: '',
  verifyCode: ''
})

const rules = {
  email: [
    { required: true, message: '请输入账号', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
  ],
  verifyCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
  ]
}

const getVerifyCodeImage = async () => {
  try {
    const response = await getCheckCode(1)
    const blob = new Blob([response], { type: 'image/jpeg' })
    const url = URL.createObjectURL(blob)
    return url
  } catch (error) {
    console.error('获取验证码失败:', error)
    return ''
  }
}

const refreshVerifyCode = async () => {
  const url = await getVerifyCodeImage()
  verifyCodeUrl.value = url
}

onMounted(() => {
  refreshVerifyCode()
})

const handleLogin = async () => {
  try {
    // 表单验证
    await loginFormRef.value.validate()
    
    loading.value = true  // 添加 loading 状态
    const res = await login({
      email: loginForm.email,
      password: md5(loginForm.password),
      checkCode: loginForm.verifyCode
    })

    if (res.code === 1) {
      // 登录成功
      await userStore.setUserInfo(res.data)
      ElMessage.success('登录成功')
      router.push('/home')
    } else {
      // 登录失败，显示具体错误信息
      ElMessage.error(res.data)
      // 刷新验证码
      refreshVerifyCode()
      // 清空验证码输入
      loginForm.verifyCode = ''
    }
  } catch (error) {
    // 处理网络错误等异常
    if (error?.response?.data?.msg) {
      // 如果有后端返回的错误信息
      ElMessage.error(error.response.data.msg)
    } else if (error?.message) {
      // 表单验证错误
      ElMessage.error(error.message)
    } else {
      // 其他错误
      ElMessage.error('登录失败，请稍后重试')
    }
    // 刷新验证码
    refreshVerifyCode()
    // 清空验证码输入
    loginForm.verifyCode = ''
  } finally {
    loading.value = false  // 结束 loading 状态
  }
}
</script>

<style scoped>
@import '../assets/styles/auth-form.css';
</style> 
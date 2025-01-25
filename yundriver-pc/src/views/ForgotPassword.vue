<template>
  <div class="auth-container">
    <div class="auth-box">
      <el-card class="auth-card">
        <h2 class="title">找回密码</h2>
        <el-form :model="forgotForm" :rules="rules" ref="forgotFormRef">
          <el-form-item prop="email">
            <div class="email-container">
              <el-input 
                v-model="forgotForm.email"
                placeholder="请输入邮箱"
                prefix-icon="Message"
                class="email-input"
              />
              <el-button 
                type="primary" 
                :disabled="isEmailCodeSent"
                @click="handleSendEmailCode"
                style="width: 120px; padding: 8px 0; font-size: 13px;"
              >
                {{ emailBtnText }}
              </el-button>
            </div>
          </el-form-item>

          <el-form-item prop="emailCode">
            <el-input 
              v-model="forgotForm.emailCode"
              placeholder="请输入邮箱验证码"
              prefix-icon="Key"
            />
          </el-form-item>
          
          <el-form-item prop="newPassword">
            <el-input 
              v-model="forgotForm.newPassword"
              type="password"
              placeholder="请输入新密码"
              prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <el-input 
              v-model="forgotForm.confirmPassword"
              type="password"
              placeholder="请确认新密码"
              prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <el-form-item prop="verifyCode">
            <div class="verify-code-container">
              <el-input 
                v-model="forgotForm.verifyCode"
                placeholder="请输入验证码"
                class="verify-code-input"
              />
              <div class="verify-code-img">
                <img :src="verifyCodeUrl" alt="验证码" @click="refreshVerifyCode" />
              </div>
            </div>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" class="submit-button" @click="handleSubmit">
              重置密码
            </el-button>
          </el-form-item>

          <div class="operation-links">
            <router-link to="/login" replace>返回登录</router-link>
          </div>
        </el-form>
      </el-card>

      <!-- 验证码对话框 -->
      <el-dialog
        v-model="verifyDialogVisible"
        title="安全验证"
        width="360px"
        center
        :close-on-click-modal="false"
        class="verify-dialog"
      >
        <div class="verify-dialog-content">
          <div class="verify-input-container">
            <el-input 
              v-model="dialogVerifyCode"
              placeholder="请输入验证码"
              class="verify-input"
            />
            <div class="verify-img">
              <img :src="dialogVerifyCodeUrl" alt="验证码" @click="refreshDialogVerifyCode" />
            </div>
          </div>
        </div>
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="verifyDialogVisible = false">取消</el-button>
            <el-button 
              type="primary" 
              @click="handleVerifyCode"
              :loading="isSending"
              :disabled="isSending"
            >
              {{ isSending ? '发送中...' : '确认' }}
            </el-button>
          </div>
        </template>
      </el-dialog>

      <div class="footer">
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { User, Lock, Message, Key } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { getCheckCode } from '@/api/checkCode'
import { sendEmailCode as sendEmailCodeApi } from '@/api/email'
import { resetPassword } from '@/api/user'
import md5 from 'js-md5'

const router = useRouter()

const forgotForm = reactive({
  email: '',
  emailCode: '',
  newPassword: '',
  confirmPassword: '',
  verifyCode: ''
})

// 邮箱验证相关
const countdown = ref(60)
const isEmailCodeSent = ref(false)
const emailBtnText = ref('发送验证码')
let timer = null

// 验证码对话框相关
const verifyDialogVisible = ref(false)
const dialogVerifyCode = ref('')

// 获取验证码图片的 URL
const verifyCodeUrl = ref('')
const dialogVerifyCodeUrl = ref('')

const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  emailCode: [
    { required: true, message: '请输入邮箱验证码', trigger: 'blur' },
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== forgotForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  verifyCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
  ]
}

const forgotFormRef = ref(null)

const startCountdown = () => {
  isEmailCodeSent.value = true
  timer = setInterval(() => {
    if (countdown.value > 0) {
      countdown.value--
      emailBtnText.value = `${countdown.value}秒后重试`
    } else {
      clearInterval(timer)
      isEmailCodeSent.value = false
      emailBtnText.value = '发送验证码'
      countdown.value = 60
    }
  }, 1000)
}

const handleSendEmailCode = async () => {
  // 验证邮箱格式
  const emailRule = rules.email[1]
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(forgotForm.email)) {
    ElMessage.error('请输入正确的邮箱格式')
    return
  }

  // 显示验证码对话框
  dialogVerifyCode.value = ''
  verifyDialogVisible.value = true
  // 立即获取邮箱验证码的验证码图片
  refreshDialogVerifyCode()
}

const isSending = ref(false)

const handleVerifyCode = async () => {
  if (!dialogVerifyCode.value) {
    ElMessage.error('请输入验证码')
    return
  }

  try {
    isSending.value = true
    await sendEmailCodeApi({
      email: forgotForm.email,
      checkCode: dialogVerifyCode.value,
      type: 1
    })
    verifyDialogVisible.value = false
    ElMessage.success('验证码已发送到邮箱')
    startCountdown()
  } catch (error) {
    refreshDialogVerifyCode()
  } finally {
    isSending.value = false
  }
}

const handleSubmit = async () => {
  if (!forgotFormRef.value) return
  await forgotFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await resetPassword({
          email: forgotForm.email,
          password: md5(forgotForm.newPassword),
          emailCode: forgotForm.emailCode,
          checkCode: forgotForm.verifyCode
        })
        ElMessage.success('重置密码成功')
        // 重置成功后返回登录页
        router.replace('/login')
      } catch (error) {
        // 失败时刷新验证码
        refreshVerifyCode()
        // 清空验证码输入
        forgotForm.verifyCode = ''
      }
    }
  })
}

// 获取验证码图片
const getVerifyCodeImage = async (type) => {
  try {
    const response = await getCheckCode(type)
    // 释放之前的 URL 对象
    if (type === 1 && verifyCodeUrl.value) {
      URL.revokeObjectURL(verifyCodeUrl.value)
    } else if (type === 0 && dialogVerifyCodeUrl.value) {
      URL.revokeObjectURL(dialogVerifyCodeUrl.value)
    }
    
    const blob = new Blob([response], { type: 'image/jpeg' })
    const url = URL.createObjectURL(blob)
    return url
  } catch (error) {
    console.error('获取验证码失败:', error)
    return ''
  }
}

// 刷新登录/注册验证码
const refreshVerifyCode = async () => {
  verifyCodeUrl.value = await getVerifyCodeImage(1)  // 1 表示登录或注册验证码
}

// 刷新邮箱验证码对话框中的验证码
const refreshDialogVerifyCode = async () => {
  dialogVerifyCodeUrl.value = await getVerifyCodeImage(0)  // 0 表示邮箱验证码
}

// 在组件挂载时只获取找回密码用的验证码
onMounted(() => {
  refreshVerifyCode()
})
</script>

<style scoped>
@import '../assets/styles/auth-form.css';
</style> 
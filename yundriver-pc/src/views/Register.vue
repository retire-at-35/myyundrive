<template>
  <div class="auth-container">
    <div class="auth-box">
      <el-card class="auth-card">
        <div class="header">
          <img src="../assets/logo.png" class="logo" alt="logo" />
          <h2 class="title">用户注册</h2>
          <p class="subtitle">欢迎加入我们的云存储平台</p>
        </div>

        <el-form 
          :model="registerForm" 
          :rules="rules" 
          ref="registerFormRef"
          class="auth-form"
        >
          <el-form-item prop="nickName">
            <el-input 
              v-model="registerForm.nickName"
              placeholder="请输入昵称"
              prefix-icon="User"
            />
          </el-form-item>
          
          <el-form-item prop="email">
            <div class="email-container">
              <el-input 
                v-model="registerForm.email"
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
              v-model="registerForm.emailCode"
              placeholder="请输入邮箱验证码"
              prefix-icon="Key"
            />
          </el-form-item>
          
          <el-form-item prop="password">
            <el-input 
              v-model="registerForm.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <el-input 
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="请确认密码"
              prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <el-form-item prop="verifyCode">
            <div class="verify-code-container">
              <el-input 
                v-model="registerForm.verifyCode"
                placeholder="请输入验证码"
                class="verify-code-input"
              />
              <div class="verify-code-img">
                <img :src="verifyCodeUrl" alt="验证码" @click="refreshVerifyCode" />
              </div>
            </div>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" class="submit-button" @click="handleRegister">
              注册
            </el-button>
          </el-form-item>

          <div class="operation-links">
            <router-link to="/login" replace>返回登录</router-link>
          </div>
        </el-form>
      </el-card>

      <div class="footer">
        <p></p>
      </div>
    </div>

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
          <el-button @click="verifyDialogVisible = false" size="default">取消</el-button>
          <el-button 
            type="primary" 
            @click="handleVerifyCode"
            :loading="isSending"
            :disabled="isSending"
            size="default"
          >
            {{ isSending ? '发送中...' : '确认' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { User, Lock, Message, Key } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getCheckCode } from '@/api/checkCode'
import { sendEmailCode as sendEmailCodeApi } from '@/api/email'
import { register } from '@/api/user'
import md5 from 'js-md5'
import { useRouter } from 'vue-router'

const router = useRouter()

const registerForm = reactive({
  nickName: '',
  email: '',
  emailCode: '',
  password: '',
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

// 添加发送状态控制
const isSending = ref(false)

// 定义正则表达式规则
const REGEX_RULES = {
    IP: { pattern: /^([1-9]|[1-9]\d|1\d{2}|2[0-4]\d|25[0-5])(\.(\\d|[1-9]\d|1\d{2}|2[0-4]\d|25[0-5])){3}$/, message: 'IP地址' },
    POSITIVE_INTEGER: { pattern: /^[0-9]*[1-9][0-9]*$/, message: '正整数' },
    NUMBER_LETTER_UNDER_LINE: { pattern: /^\w+$/, message: '由数字、26个英文字母或者下划线组成的字符串' },
    EMAIL: { pattern: /^[\w-]+(\.[\w-]+)*@[\w-]+(\.[\w-]+)+$/, message: '邮箱' },
    PHONE: { pattern: /^(1[0-9])\d{9}$/, message: '手机号码' },
    COMMON: { pattern: /^[a-zA-Z0-9_\u4e00-\u9fa5]+$/, message: '数字，字母，中文，下划线' },
    PASSWORD: { pattern: /^(?=.*\d)(?=.*[a-zA-Z])[\da-zA-Z~!@#$%^&*_]{8,18}$/, message: '数字，字母，特殊字符 8 - 18位的组合' },
    ACCOUNT: { pattern: /^[a-zA-Z][a-zA-Z0-9_\u4e00-\u9fa5]{0,5}$/, message: '字母开头,由数字、英文字母或者下划线组成' },
    MONEY: { pattern: /^[0-9]+(\.[0-9]{1,2})?$/, message: '金额' }
};

// 原规则对象
const rules = {
    nickName: [
        { required: true, message: '请输入昵称', trigger: 'blur' },
        { pattern: REGEX_RULES.COMMON.pattern, message: REGEX_RULES.COMMON.message, trigger: 'blur' }
    ],
    email: [
        { required: true, message: '请输入邮箱', trigger: 'blur' },
        { pattern: REGEX_RULES.EMAIL.pattern, message: '请输入正确的邮箱格式', trigger: 'blur' }
    ],
    emailCode: [
        { required: true, message: '请输入邮箱验证码', trigger: 'blur' }
    ],
    password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { pattern: REGEX_RULES.PASSWORD.pattern, message: REGEX_RULES.PASSWORD.message, trigger: 'blur' }
    ],
    confirmPassword: [
        { required: true, message: '请确认密码', trigger: 'blur' },
        {
            validator: (rule, value, callback) => {
                if (value !== registerForm.password) {
                    callback(new Error('两次输入的密码不一致'));
                } else if (!REGEX_RULES.PASSWORD.pattern.test(value)) {
                    callback(new Error(REGEX_RULES.PASSWORD.message));
                } else {
                    callback();
                }
            },
            trigger: 'blur'
        }
    ],
    verifyCode: [
        { required: true, message: '请输入验证码', trigger: 'blur' },
        { pattern: REGEX_RULES.NUMBER_LETTER_UNDER_LINE.pattern, message: REGEX_RULES.NUMBER_LETTER_UNDER_LINE.message, trigger: 'blur' }
    ]
};

const registerFormRef = ref(null)

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
  if (!emailRegex.test(registerForm.email)) {
    ElMessage.error('请输入正确的邮箱格式')
    return
  }

  // 显示验证码对话框
  dialogVerifyCode.value = ''
  verifyDialogVisible.value = true
  // 立即获取邮箱验证码的验证码图片
  refreshDialogVerifyCode()
}

// 处理验证码确认
const handleVerifyCode = async () => {
  if (!dialogVerifyCode.value) {
    ElMessage.error('请输入验证码')
    return
  }
  try {
    isSending.value = true  // 开始发送时禁用按钮
    await sendEmailCodeApi({
      email: registerForm.email,
      checkCode: dialogVerifyCode.value,
      type: 0  // 0 表示注册
    })
    verifyDialogVisible.value = false
    ElMessage.success('验证码已发送到邮箱')
    startCountdown()
  } catch (error) {
    refreshDialogVerifyCode()
    ElMessage.error(error.message || '发送失败')
  } finally {
    isSending.value = false  // 无论成功失败都启用按钮
  }
}

const handleRegister = async () => {
  if (!registerFormRef.value) return

  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
          await register({
          email: registerForm.email,
          nickName: registerForm.nickName,
          password: md5(registerForm.password),
          checkCode: registerForm.verifyCode,
          emailCode: registerForm.emailCode
        })
        ElMessage.success('注册成功')
        router.replace('/login')
      } catch (error) {
        ElMessage.error(error.message || '注册失败')
        refreshVerifyCode()
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

// 在组件挂载时只获取注册用的验证码
onMounted(() => {
  refreshVerifyCode()
})

// 修改图片的 src 绑定
</script>

<style scoped>
@import '../assets/styles/auth-form.css';

.dialog-footer {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.verify-dialog :deep(.el-dialog__footer) {
  padding: 10px 20px 20px;
}

.dialog-footer .el-button {
  width: 80px;
  padding: 8px 15px;
  font-size: 14px;
}

.verify-dialog-content {
  padding: 10px 0;
}

.verify-input-container {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: center;
}
</style> 
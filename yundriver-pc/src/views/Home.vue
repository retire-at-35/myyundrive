<template>
  <div class="home-container">
    <!-- 顶部导航栏 -->
    <div class="header">
      <div class="logo-container">
        <img src="../assets/logo.png" alt="logo" class="logo" />
        <span class="logo-text">在线移动网盘</span>
      </div>
      <div class="user-section">
        <!-- 存储空间进度条 -->
        <div class="storage-info">
          <div class="storage-text">
            <span>{{ formatStorage(usedStorage) }}</span>
            <span>/{{ formatStorage(totalStorage) }}</span>
          </div>
          <el-progress 
            :percentage="storagePercentage" 
            :format="() => ''" 
            :stroke-width="12"
            class="storage-progress"
            :status="storagePercentage >= 90 ? 'danger' : storagePercentage >= 70 ? 'warning' : ''"
          />
        </div>
        <!-- 用户信息下拉菜单 -->
        <el-dropdown trigger="hover" @command="handleCommand">
          <div class="user-info">
            <el-avatar :size="40" :src="userInfo.avatar || ''" />
            <span class="nickname">{{ userInfo.nickName }}</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="updatePassword">
                <el-icon><Lock /></el-icon>修改密码
              </el-dropdown-item>
              <el-dropdown-item command="updateAvatar">
                <el-icon><Upload /></el-icon>更新头像
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 左侧菜单 -->
    <div class="layout">
      <div class="sidebar">
        <el-menu
          :default-active="activeMenu"
          class="menu"
          router
        >
          <el-menu-item index="/home/index">
            <el-icon><HomeFilled /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="/home/share">
            <el-icon><Share /></el-icon>
            <span>分享</span>
          </el-menu-item>
          <el-menu-item index="/home/recycle">
            <el-icon><Delete /></el-icon>
            <span>回收站</span>
          </el-menu-item>
          <el-menu-item 
            v-if="userStore.userInfo.isAdmin"
            index="/home/setting/users"
          >
            <el-icon><Setting /></el-icon>
            <span>设置</span>
          </el-menu-item>
        </el-menu>
      </div>

      <!-- 主内容区 -->
      <div class="main-content">
        <router-view :update-user-space="updateUserSpace"></router-view>
      </div>
    </div>

    <!-- 修改密码对话框 -->
    <el-dialog
      v-model="passwordDialogVisible"
      title="修改密码"
      width="400px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="100px"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            show-password
            placeholder="请输入原密码"
          />
        </el-form-item>
        <el-form-item label="新密码" prop="password">
          <el-input
            v-model="passwordForm.password"
            type="password"
            show-password
            placeholder="请输入新密码"
          />
        </el-form-item>
        <el-form-item label="验证码" prop="checkCode">
          <div class="verify-code-container">
            <el-input
              v-model="passwordForm.checkCode"
              placeholder="请输入验证码"
              class="verify-code-input"
            />
            <div class="verify-code-img">
              <img :src="verifyCodeUrl" alt="验证码" @click="refreshVerifyCode" />
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="passwordDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleUpdatePassword" :loading="updating">
            确认
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 更新头像对话框 -->
    <el-dialog
      v-model="avatarDialogVisible"
      title="更新头像"
      width="400px"
      :close-on-click-modal="false"
    >
      <el-upload
        class="avatar-uploader"
        :show-file-list="false"
        :before-upload="beforeAvatarUpload"
        :http-request="handleAvatarUpload"
      >
        <img v-if="avatarUrl" :src="avatarUrl" class="avatar-preview" />
        <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
      </el-upload>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="avatarDialogVisible = false">取消</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox, ElMessage } from 'element-plus'
import { 
  HomeFilled, 
  Share, 
  Delete, 
  Setting,
  Lock,
  Upload,
  SwitchButton,
  Plus
} from '@element-plus/icons-vue'
import { updatePassword, updateAvatar, logout, getUserSpace } from '@/api/user'
import { getCheckCode } from '@/api/checkCode'
import md5 from 'js-md5'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const userInfo = userStore.userInfo

// 存储空间数据
const usedStorage = ref(0)
const totalStorage = ref(0)

// 计算存储空间百分比
const storagePercentage = computed(() => {
  if (!totalStorage.value) return 0
  const percentage = Math.round((usedStorage.value / totalStorage.value) * 100)
  return Math.min(percentage, 100) // 确保不超过100%
})

// 格式化存储空间显示
const formatStorage = (size) => {
  if (!size) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let index = 0
  let value = size

  while (value >= 1024 && index < units.length - 1) {
    value /= 1024
    index++
  }

  return `${value.toFixed(2)} ${units[index]}`
}

// 更新用户空间信息
const updateUserSpace = async () => {
  try {
    const res = await getUserSpace()
    if (res.code === 1 && res.data) {
      // 确保使用正确的属性名
      usedStorage.value = Number(res.data.userSpace || 0)
      totalStorage.value = Number(res.data.totalSpace || 0)
    }
  } catch (error) {
    console.error('获取用户空间信息失败:', error)
  }
}

// 在组件挂载时获取用户空间信息
onMounted(async () => {
  await updateUserSpace()
})

// 修改密码相关
const passwordDialogVisible = ref(false)
const passwordFormRef = ref(null)
const updating = ref(false)
const verifyCodeUrl = ref('')

const passwordForm = reactive({
  oldPassword: '',
  password: '',
  checkCode: ''
})

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
  ],
  checkCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ]
}

// 获取验证码
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
  verifyCodeUrl.value = await getVerifyCodeImage()
}

// 更新头像相关
const avatarDialogVisible = ref(false)
const avatarUrl = ref('')

// 处理下拉菜单命令
const handleCommand = async (command) => {
  switch (command) {
    case 'updatePassword':
      passwordDialogVisible.value = true
      refreshVerifyCode()
      break
    case 'updateAvatar':
      avatarDialogVisible.value = true
      avatarUrl.value = userInfo.avatar
      break
    case 'logout':
      try {
        await ElMessageBox.confirm(
          '确定要退出登录吗？',
          '提示',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        await logout()
        userStore.clearUserInfo()
        router.push('/login')
      } catch (error) {
        // 用户取消退出
      }
      break
  }
}

// 修改密码
const handleUpdatePassword = async () => {
  if (!passwordFormRef.value) return
  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        updating.value = true
        const res = await updatePassword({
          oldPassword: md5(passwordForm.oldPassword),
          password: md5(passwordForm.password),
          checkCode: passwordForm.checkCode
        })
        if (res.code === 1) {
          ElMessage.success('修改密码成功')
          passwordDialogVisible.value = false
          // 清空表单
          passwordForm.oldPassword = ''
          passwordForm.password = ''
          passwordForm.checkCode = ''
        }
      } catch (error) {
        refreshVerifyCode()
      } finally {
        updating.value = false
      }
    }
  })
}

// 头像上传前的验证
const beforeAvatarUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB!')
    return false
  }
  return true
}

// 处理头像上传
const handleAvatarUpload = async ({ file }) => {
  try {
    const res = await updateAvatar(file)
    if (res.code === 1) {
      const url = res.msg
      console.log("新的头像"+url);
      avatarUrl.value = url
      userInfo.avatar = url
      // 更新 store 中的用户信息
      userStore.setUserInfo({
        ...userInfo,
        avatar: url
      })
      ElMessage.success('更新头像成功')
      avatarDialogVisible.value = false
    }
  } catch (error) {
    ElMessage.error('更新头像失败，请重试')
  }
}

// 计算当前激活的菜单项
const activeMenu = computed(() => route.path)
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.header {
  height: 64px;
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo-container {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  width: 36px;
  height: 36px;
}

.logo-text {
  font-size: 20px;
  font-weight: bold;
  color: #2c3e50;
}

.user-section {
  display: flex;
  align-items: center;
  gap: 24px;
}

.storage-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 200px;
}

.storage-text {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #606266;
}

.storage-progress {
  margin: 0;
}

:deep(.el-progress-bar__outer) {
  border-radius: 4px;
  background-color: #f0f2f5;
}

:deep(.el-progress-bar__inner) {
  border-radius: 4px;
  transition: all 0.3s;
  background-color: var(--el-color-primary);
}

/* 根据使用比例改变进度条颜色 */
:deep(.el-progress--warning .el-progress-bar__inner) {
  background-color: var(--el-color-warning);
}

:deep(.el-progress--danger .el-progress-bar__inner) {
  background-color: var(--el-color-danger);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 2px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.user-info:hover {
  background-color: #f5f7fa;
}

.nickname {
  font-size: 14px;
  color: #606266;
}

:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
}

:deep(.el-dropdown-menu__item .el-icon) {
  margin-right: 4px;
  font-size: 16px;
}

.layout {
  display: flex;
  height: calc(100vh - 64px);
}

.sidebar {
  width: 200px;
  background-color: #fff;
  border-right: 1px solid #e6e6e6;
}

.menu {
  border-right: none;
}

.main-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.empty-tip {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #909399;
  font-size: 16px;
}

.action-buttons {
  margin-top: 20px;
  display: flex;
  gap: 12px;
}

:deep(.el-menu-item) {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 56px;
  padding: 0 20px;
}

:deep(.el-menu-item.is-active) {
  background-color: #ecf5ff;
}

/* 调整图标大小 */
:deep(.el-menu-item .el-icon) {
  font-size: 20px;
}

.verify-code-container {
  display: flex;
  gap: 12px;
}

.verify-code-input {
  flex: 1;
}

.verify-code-img {
  width: 100px;
  height: 32px;
  cursor: pointer;
}

.verify-code-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-uploader {
  text-align: center;
}

.avatar-uploader .avatar-preview {
  width: 178px;
  height: 178px;
  border-radius: 6px;
}

.avatar-uploader .el-upload {
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}

.avatar-uploader .el-upload:hover {
  border-color: var(--el-color-primary);
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 178px;
  height: 178px;
  text-align: center;
  line-height: 178px;
}
</style> 
<template>
  <div class="share-page">
    <!-- 顶部导航 -->
    <div class="header">
      <div class="logo-container">
        <img src="@/assets/logo.png" alt="logo" class="logo" />
        <span class="logo-text">在线网盘</span>
      </div>
    </div>

    <!-- 主要内容 -->
    <div class="main-content">
      <div class="share-container">
        <!-- 左侧分享信息 -->
        <div class="share-info">
          <div class="share-header">
            <el-avatar :size="80" :src="shareInfo.avatar" />
            <div class="user-meta">
              <div class="username">{{ shareInfo.userName }}</div>
              <div class="share-time">分享于 {{ formatDate(shareInfo.shareTime) }}</div>
            </div>
          </div>
          
          <div class="share-body">
            <div class="share-icon">
              <el-icon :size="80"><Document /></el-icon>
            </div>
            <div class="share-title">分享内容</div>
          </div>
        </div>

        <!-- 右侧验证区域 -->
        <div class="verify-area">
          <div class="verify-header">
            <h3>请输入提取码</h3>
            <p class="verify-tip">提取码验证通过后即可获取文件</p>
          </div>

          <div class="verify-form">
            <el-input
              v-model="accessCode"
              placeholder="请输入5位提取码"
              maxlength="5"
              :prefix-icon="Lock"
              class="code-input"
              @keyup.enter="handleAccess"
            />
            <el-button 
              type="primary" 
              class="verify-btn"
              @click="handleAccess" 
              :loading="accessing"
            >
              验证提取码
            </el-button>
          </div>

          <div class="verify-tips">
            <h4>温馨提示</h4>
            <ul>
              <li>请在有效期内及时保存文件，过期后文件将无法访问</li>
              <li>如果遇到问题，请联系分享者重新分享</li>
              <li>提取码是区分大小写的，请正确输入</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Document, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getShareInfo, verifyShareCode } from '@/api/share'
import { formatDate } from '@/utils/date'

const route = useRoute()
const router = useRouter()
const shareInfo = ref({})
const accessCode = ref('')
const accessing = ref(false)

// 获取分享信息
const fetchShareInfo = async () => {
  try {
    const shareId = route.params.shareId
    const res = await getShareInfo(shareId)
    if (res.code === 1) {
      shareInfo.value = res.data
    }
  } catch (error) {
    console.error('获取分享信息失败:', error)
    ElMessage.error('获取分享信息失败')
  }
}

// 处理提取文件
const handleAccess = async () => {
  if (!accessCode.value) {
    ElMessage.warning('请输入提取码')
    return
  }
  
  if (accessCode.value.length !== 5) {
    ElMessage.warning('提取码必须是5位')
    return
  }

  try {
    accessing.value = true
    const res = await verifyShareCode(route.params.shareId, accessCode.value)
    if (res.code === 1) {
      ElMessage.success('验证成功')
      // 将分享者信息存储到 localStorage
      localStorage.setItem('shareInfo', JSON.stringify(shareInfo.value))
      // 跳转到文件列表页面
      router.push({
        name: 'shareFiles',
        query: {
          fileIds: res.data.fileIds,
          userId: res.data.userId
        }
      })
    }
  } catch (error) {
    console.error('验证失败:', error)
    ElMessage.error('提取码错误')
  } finally {
    accessing.value = false
  }
}

onMounted(() => {
  fetchShareInfo()
})
</script>

<style scoped>
.share-page {
  min-height: 100vh;
  background-color: var(--bg-color);
  display: flex;
  flex-direction: column;
}

.header {
  height: 64px;
  background-color: var(--bg-white);
  box-shadow: var(--shadow-light);
  display: flex;
  align-items: center;
  padding: 0 24px;
  flex-shrink: 0;
}

.logo-container {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  height: 32px;
  width: auto;
}

.logo-text {
  font-size: 18px;
  font-weight: 500;
  color: var(--text-primary);
}

.main-content {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px 20px;
  max-width: none;
}

.share-container {
  display: flex;
  gap: 24px;
  width: 1200px;
  margin: 0 auto;
}

/* 左侧分享信息样式 */
.share-info {
  flex: 1;
  background-color: var(--bg-white);
  border-radius: var(--radius-large);
  padding: 40px;
  box-shadow: var(--shadow-regular);
}

.share-header {
  display: flex;
  align-items: center;
  gap: 24px;
  padding-bottom: 32px;
  border-bottom: 1px solid var(--border-light);
}

.user-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.username {
  font-size: 20px;
  font-weight: 500;
  color: var(--text-primary);
}

.share-time {
  font-size: 14px;
  color: var(--text-secondary);
}

.share-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 80px;
  gap: 32px;
}

.share-icon {
  color: var(--primary-color);
  background-color: var(--border-light);
  padding: 40px;
  border-radius: 50%;
}

.share-title {
  font-size: 24px;
  font-weight: 500;
  color: var(--text-primary);
}

/* 右侧验证区域样式 */
.verify-area {
  width: 440px;
  background-color: var(--bg-white);
  border-radius: var(--radius-large);
  padding: 40px;
  box-shadow: var(--shadow-regular);
  display: flex;
  flex-direction: column;
}

.verify-header {
  text-align: center;
  margin-bottom: 32px;
}

.verify-header h3 {
  margin: 0 0 8px;
  font-size: 20px;
  color: var(--text-primary);
}

.verify-tip {
  margin: 0;
  color: var(--text-secondary);
  font-size: 14px;
}

.verify-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 32px;
}

.code-input :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px var(--border-color);
}

.code-input :deep(.el-input__wrapper):hover {
  box-shadow: 0 0 0 1px var(--primary-color);
}

.verify-btn {
  height: 40px;
  font-size: 16px;
}

.verify-tips {
  margin-top: auto;
  background-color: var(--border-light);
  padding: 16px 20px;
  border-radius: var(--radius-medium);
}

.verify-tips h4 {
  margin: 0 0 12px;
  color: var(--text-primary);
  font-size: 14px;
}

.verify-tips ul {
  margin: 0;
  padding-left: 20px;
  color: var(--text-regular);
  font-size: 13px;
}

.verify-tips li {
  margin-bottom: 8px;
  line-height: 1.5;
}

.verify-tips li:last-child {
  margin-bottom: 0;
}
</style> 
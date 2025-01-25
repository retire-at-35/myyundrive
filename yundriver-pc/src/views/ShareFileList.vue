<template>
  <div class="share-page">
    <!-- 顶部导航 -->
    <div class="header">
      <div class="logo-container">
        <img src="@/assets/logo.png" alt="logo" class="logo" />
        <span class="logo-text">在线网盘</span>
      </div>
      <div class="share-info">
        <el-avatar :size="32" :src="shareInfo.avatar" class="share-avatar" />
        <span class="share-user">{{ shareInfo.userName }}</span>
        <span class="share-time">分享于 {{ formatDate(shareInfo.shareTime) }}</span>
      </div>
    </div>

    <!-- 面包屑导航 -->
    <div class="breadcrumb">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item @click="navigateToFolder('0')">全部文件</el-breadcrumb-item>
        <el-breadcrumb-item 
          v-for="(folder, index) in folderPath" 
          :key="folder.fileId"
          @click="navigateToFolder(folder.fileId, index)"
        >
          {{ folder.fileName }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 在面包屑下方添加批量操作栏 -->
    <div class="action-bar">
      <div class="left">
        <template v-if="!showSelection">
          <el-button size="large" @click="startBatchOperation">批量操作</el-button>
        </template>
        <template v-else>
          <el-button-group>
            <el-button 
              type="primary"
              size="large" 
              @click="handleBatchSaveToCloud"
              :disabled="!selectedFiles.length"
            >
              批量保存到网盘
            </el-button>
            <el-button 
              type="primary"
              size="large" 
              @click="handleBatchDownload"
              :disabled="!selectedFiles.length"
            >
              批量下载
            </el-button>
          </el-button-group>
          <el-button size="large" @click="cancelBatchOperation">
            取消操作
          </el-button>
        </template>
      </div>
    </div>

    <!-- 文件列表 -->
    <div class="main-content">
      <div class="content-area">
        <div class="table-container">
          <div v-if="fileList.length > 0" class="file-list">
            <el-table 
              :data="fileList" 
              style="width: 100%"
              @selection-change="handleSelectionChange"
            >
              <el-table-column
                v-if="showSelection"
                type="selection"
                width="55"
              />
              <el-table-column label="文件名" min-width="400">
                <template #default="{ row }">
                  <div 
                    class="file-name-cell"
                    @click="handleFileClick(row)"
                  >
                    <el-icon :size="20" class="file-icon">
                      <Folder v-if="row.folderType === 1" />
                      <Document v-else-if="isDocumentFile(row.fileName)" />
                      <VideoCamera v-else-if="row.fileCategory === 1" />
                      <Headset v-else-if="row.fileCategory === 2" />
                      <Picture v-else-if="isImageFile(row.fileName)" />
                      <Files v-else />
                    </el-icon>
                    <span class="file-name">{{ row.fileName }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="fileSize" label="大小" width="150">
                <template #default="{ row }">
                  <template v-if="row.folderType === 1">
                    <span v-if="folderSizes[row.fileId] !== undefined">
                      {{ formatFileSize(folderSizes[row.fileId]) }}
                    </span>
                    <span v-else>
                      <el-icon class="is-loading"><Loading /></el-icon>
                    </span>
                  </template>
                  <template v-else>
                    {{ formatFileSize(row.fileSize) }}
                  </template>
                </template>
              </el-table-column>
              <el-table-column prop="lastUpdateTime" label="修改时间" width="200" />
              <el-table-column label="操作" width="200" fixed="right">
                <template #default="{ row }">
                  <el-button-group>
                    <el-button 
                      type="primary" 
                      link 
                      @click.stop="handleSaveToCloud(row)"
                    >
                      保存到网盘
                    </el-button>
                    <el-button 
                      type="primary" 
                      link 
                      @click.stop="handleDownload(row)"
                    >
                      下载
                    </el-button>
                  </el-button-group>
                </template>
              </el-table-column>
            </el-table>

            <!-- 分页 -->
            <div class="pagination">
              <el-pagination
                v-model:current-page="currentPage"
                v-model:page-size="pageSize"
                :total="total"
                :page-sizes="[10, 20, 50, 100]"
                layout="total, sizes, prev, pager, next, jumper"
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
              />
            </div>
          </div>
          <div v-else class="empty-state">
            <el-empty description="暂无文件" />
          </div>
        </div>
      </div>
    </div>

    <!-- 文件预览组件 -->
    <FilePreview
      v-model="previewVisible"
      :file="previewFile"
      @preview-error="handlePreviewError"
    />

    <!-- 添加文件夹选择对话框 -->
    <el-dialog
      v-model="folderDialogVisible"
      title="选择保存位置"
      width="500px"
    >
      <div class="folder-tree">
        <el-tree
          ref="folderTreeRef"
          :data="[{ fileId: '0', fileName: '全部文件', children: [] }]"
          node-key="fileId"
          :props="{
            label: 'fileName',
            children: 'children'
          }"
          :load="loadNode"
          lazy
          highlight-current
          :default-expanded-keys="['0']"
          @node-click="handleFolderSelect"
        />
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="folderDialogVisible = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="confirmSaveToCloud"
            :disabled="!selectedFolderId"
          >
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Document, Folder, VideoCamera, Headset, Picture, Files, Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getFileListByIds, getFileListByPid, downloadFiles, getVideoInfo, getFile, getAllFolderInfo, getSharerFileList, getFolderSpace } from '@/api/file'
import { formatFileSize } from '@/utils/format'
import { formatDate } from '@/utils/date'
import FilePreview from '@/components/FilePreview.vue'
import { saveShare2Myself } from '@/api/share'

const route = useRoute()
const router = useRouter()

const fileList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const shareInfo = ref({})
const currentFolderId = ref('0')
const folderPath = ref([])
const previewVisible = ref(false)
const previewFile = ref(null)

// 批量操作相关
const showSelection = ref(false)
const selectedFiles = ref([])

// 文件夹选择相关
const folderDialogVisible = ref(false)
const folderTreeRef = ref(null)
const selectedFolderId = ref('')
const filesToSave = ref([])

// 添加响应式变量存储文件夹大小
const folderSizes = ref({})

// 添加 loading 状态变量
const loading = ref(false)

// 获取文件列表
const fetchFileList = async () => {
  try {
    if (currentFolderId.value === '0') {
      // 如果是根目录，使用 fileIds 获取文件列表
      const res = await getFileListByIds({
        fileIds: route.query.fileIds,
        userId: route.query.userId,
        page: currentPage.value,
        pageSize: pageSize.value
      })
      if (res.code === 1) {
        fileList.value = res.data.rows
        total.value = res.data.total
      }
    } else {
      // 如果是文件夹内部，使用新的接口获取文件列表
      const res = await getSharerFileList({
        filePid: currentFolderId.value,
        userId: route.query.userId, // 从路由参数中获取分享者ID
        page: currentPage.value,
        pageSize: pageSize.value
      })
      if (res.code === 1) {
        fileList.value = res.data.rows
        total.value = res.data.total
      }
    }
  } catch (error) {
    console.error('获取文件列表失败:', error)
    ElMessage.error('获取文件列表失败')
  }
}

// 处理文件点击
const handleFileClick = (file) => {
  if (file.folderType === 1) {
    // 更新面包屑
    folderPath.value.push({
      fileId: file.fileId,
      fileName: file.fileName
    })
    currentFolderId.value = file.fileId
    currentPage.value = 1
    fetchFileList()
  } else {
    const fileType = getFileTypeNumber(file)
    
    if (fileType === 0) {
      ElMessage.info('该文件类型暂不支持预览')  // 只提示，不下载
      return
    }
    
    previewFile.value = {
      ...file,
      fileType,
      isVideo: fileType === 1,
      userId: route.query.userId,
      previewUrl: `/api/file/getFile?fileId=${file.fileId}&userId=${route.query.userId}`,
      videoInfoUrl: fileType === 1 ? `/api/file/ts/getVideoInfo?fileId=${file.fileId}&userId=${route.query.userId}` : undefined
    }
    previewVisible.value = true
  }
}

// 获取文件类型数字
const getFileTypeNumber = (file) => {
  if (file.fileCategory === 1) return 1 // 视频
  if (file.fileCategory === 2) return 2 // 音频
  if (file.fileCategory === 3) return 3 // 图片
  
  const fileName = file.fileName.toLowerCase()
  if (fileName.endsWith('.pdf')) return 4
  if (fileName.endsWith('.doc') || fileName.endsWith('.docx')) return 5
  if (fileName.endsWith('.xls') || fileName.endsWith('.xlsx')) return 6
  if (fileName.endsWith('.txt') || fileName.endsWith('.md')) return 7
  if (isCodeFile(fileName)) return 8
  return 0 // 其他类型
}

// 判断是否为代码文件
const isCodeFile = (fileName) => {
  const codeExts = ['.js', '.ts', '.java', '.py', '.cpp', '.c', '.h', 
    '.html', '.css', '.scss', '.less', '.json', '.xml']
  return codeExts.some(ext => fileName.toLowerCase().endsWith(ext))
}

// 导航到指定文件夹
const navigateToFolder = (fileId, index) => {
  if (fileId === '0') {
    folderPath.value = []
  } else {
    folderPath.value = folderPath.value.slice(0, index + 1)
  }
  currentFolderId.value = fileId
  // 重置分页
  currentPage.value = 1
  fetchFileList()
}

// 处理下载
const handleDownload = async (file) => {
  if (loading.value) return // 防止重复点击
  
  try {
    loading.value = true
    const url = `${import.meta.env.VITE_API_URL}/api/file/download?fileIds=${file.fileId}&userId=${route.query.userId}`
    window.open(url, '_blank')
    ElMessage.success('下载成功')
  } catch (error) {
    console.error('下载失败:', error)
    ElMessage.error('下载失败')
  } finally {
    loading.value = false
  }
}

// 处理分页
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchFileList()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchFileList()
}

// 文件类型判断
const isDocumentFile = (fileName) => {
  const docExts = ['.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx', '.pdf', '.txt']
  return docExts.some(ext => fileName.toLowerCase().endsWith(ext))
}

const isImageFile = (fileName) => {
  const imgExts = ['.jpg', '.jpeg', '.png', '.gif', '.bmp']
  return imgExts.some(ext => fileName.toLowerCase().endsWith(ext))
}

// 处理预览错误
const handlePreviewError = (file) => {
  ElMessage.info('该文件类型暂不支持预览')  // 只提示，不下载
}

// 开始批量操作
const startBatchOperation = () => {
  showSelection.value = true
}

// 取消批量操作
const cancelBatchOperation = () => {
  showSelection.value = false
  selectedFiles.value = []
}

// 处理选择变化
const handleSelectionChange = (selection) => {
  selectedFiles.value = selection
}

// 批量下载
const handleBatchDownload = () => {
  if (selectedFiles.value.length === 0) {
    ElMessage.warning('请选择要下载的文件')
    return
  }
  
  const fileIds = selectedFiles.value
    .filter(file => !file.folderType)
    .map(file => file.fileId)
    .join(',')
    
  if (fileIds) {
    const url = `${import.meta.env.VITE_API_URL}/api/file/download?fileIds=${fileIds}&userId=${route.query.userId}`
    window.open(url, '_blank')
  } else {
    ElMessage.warning('没有可下载的文件')
  }
}

// 加载文件夹节点
const loadNode = async (node, resolve) => {
  try {
    if (node.level === 0) {
      // 根节点，使用 filePid=-1 获取顶层文件夹
      const res = await getAllFolderInfo('-1')
      if (res.code === 1) {
        // 添加根节点和其子节点
        const rootNode = { fileId: '0', fileName: '全部文件', children: res.data }
        resolve([rootNode])
      } else {
        resolve([{ fileId: '0', fileName: '全部文件', children: [] }])
      }
      return
    }

    // 加载子节点，使用实际的 fileId
    const res = await getAllFolderInfo(node.data.fileId)
    if (res.code === 1 && res.data) {
      resolve(res.data)
    } else {
      resolve([])
    }
  } catch (error) {
    console.error('加载文件夹失败:', error)
    ElMessage.error('加载文件夹失败')
    resolve([])
  }
}

// 处理文件夹选择
const handleFolderSelect = (folder) => {
  selectedFolderId.value = folder.fileId
}

// 打开保存对话框
const openSaveDialog = async (files) => {
  filesToSave.value = Array.isArray(files) ? files : [files]
  folderDialogVisible.value = true
  selectedFolderId.value = ''
  
  // 重置树选择
  if (folderTreeRef.value) {
    folderTreeRef.value.setCurrentKey(null)
    
    // 重新加载根节点数据，使用 filePid=-1
    try {
      const res = await getAllFolderInfo('-1')
      if (res.code === 1) {
        const rootNode = { fileId: '0', fileName: '全部文件', children: res.data }
        folderTreeRef.value.setData([rootNode])
      }
    } catch (error) {
      console.error('加载文件夹失败:', error)
      ElMessage.error('加载文件夹失败')
    }
  }
}

// 确认保存到网盘
const confirmSaveToCloud = async () => {
  if (!selectedFolderId.value) {
    ElMessage.warning('请选择保存位置')
    return
  }

  try {
    const fileIds = filesToSave.value.map(file => file.fileId).join(',')
    await saveShare2Myself({
      fileIds,
      filePid: selectedFolderId.value,
      userId: route.query.userId
    })
    
    ElMessage.success('保存成功')
    folderDialogVisible.value = false
    
    // 如果是批量操作，保存成功后退出选择模式
    if (showSelection.value) {
      cancelBatchOperation()
    }
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  }
}

// 修改单个保存到网盘的处理函数
const handleSaveToCloud = (file) => {
  openSaveDialog(file)
}

// 修改批量保存到网盘的处理函数
const handleBatchSaveToCloud = () => {
  if (selectedFiles.value.length === 0) {
    ElMessage.warning('请选择要保存的文件')
    return
  }
  openSaveDialog(selectedFiles.value)
}

// 获取文件夹大小
const loadFolderSize = async (folderId) => {
  try {
    const res = await getFolderSpace(folderId, 2) // type=2 表示正常文件
    if (res.code === 1) {
      folderSizes.value[folderId] = res.data
    }
  } catch (error) {
    console.error('获取文件夹大小失败:', error)
  }
}

// 监听文件列表变化
watch(() => fileList.value, (newList) => {
  // 获取所有文件夹的大小
  newList.forEach(file => {
    if (file.folderType === 1 && folderSizes.value[file.fileId] === undefined) {
      loadFolderSize(file.fileId)
    }
  })
}, { immediate: true })

onMounted(() => {
  if (!route.query.fileIds || !route.query.userId) {
    ElMessage.error('分享信息不完整')
    router.push('/')
    return
  }
  // 从 localStorage 获取分享者信息
  const storedShareInfo = localStorage.getItem('shareInfo')
  if (storedShareInfo) {
    shareInfo.value = JSON.parse(storedShareInfo)
  } else {
    ElMessage.error('分享信息不完整')
    router.push('/')
    return
  }
  fetchFileList()
})

// 组件卸载时清理
onUnmounted(() => {
  localStorage.removeItem('shareInfo')
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
  height: 56px;
  background-color: var(--bg-white);
  box-shadow: var(--shadow-light);
  display: flex;
  align-items: center;
  padding: 0 24px;
  gap: 24px;
}

.logo-container {
  display: flex;
  align-items: center;
  gap: 8px;
}

.logo {
  height: 24px;
  width: auto;
}

.logo-text {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary);
}

.share-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
  color: var(--text-regular);
}

.share-avatar {
  border: 2px solid var(--border-light);
}

.share-user {
  font-weight: 500;
  color: var(--text-primary);
}

.share-time {
  color: var(--text-secondary);
  font-size: 13px;
}

.breadcrumb {
  padding: 12px 24px;
  background-color: var(--bg-white);
  border-bottom: 1px solid var(--border-light);
}

.main-content {
  flex: 1;
  padding: 24px;
}

.content-area {
  background-color: var(--bg-white);
  border-radius: var(--radius-large);
  padding: 24px;
  min-height: 500px;
}

.file-name-cell {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: color 0.2s;
}

.file-name-cell:hover {
  color: var(--el-color-primary);
}

.file-icon {
  color: var(--text-regular);
}

.file-name {
  color: var(--text-primary);
}

.empty-state {
  padding: 60px 0;
  text-align: center;
}

.pagination {
  margin-top: 20px;
  text-align: right;
}

.el-button-group {
  display: flex;
  gap: 8px;
}

/* 添加批量操作栏样式 */
.action-bar {
  padding: 16px 24px;
  background-color: var(--bg-white);
  border-bottom: 1px solid var(--border-light);
}

.action-bar .left {
  display: flex;
  gap: 16px;
  align-items: center;
}

/* 调整表格选择列样式 */
:deep(.el-table__header) {
  background-color: var(--bg-light);
}

:deep(.el-checkbox__inner) {
  border-radius: 4px;
}

.folder-tree {
  max-height: 400px;
  overflow-y: auto;
  padding: 10px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-base);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
}
</style> 
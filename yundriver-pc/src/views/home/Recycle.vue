<template>
  <div class="page-container">
    <!-- 提示信息 -->
    <div class="notice-bar">
      <el-alert
        title="每天晚上12点都会清理存放超过5天的文件，请及时处理。"
        type="warning"
        :closable="false"
        show-icon
      />
    </div>

    <!-- 顶部操作栏 -->
    <div class="action-bar">
      <div class="left">
        <!-- 批量操作按钮组 -->
        <template v-if="!showSelection">
          <el-button size="large" @click="startBatchOperation">批量操作</el-button>
        </template>
        <template v-else>
          <el-button-group>
            <el-button 
              type="primary" 
              size="large" 
              @click="handleBatchRestore"
              :disabled="!selectedFiles.length"
            >
              恢复
            </el-button>
            <el-button 
              type="danger"
              size="large" 
              @click="handleBatchDelete"
              :disabled="!selectedFiles.length"
            >
              彻底删除
            </el-button>
          </el-button-group>
          <el-button 
            size="large" 
            @click="cancelBatchOperation"
          >
            取消操作
          </el-button>
        </template>
      </div>
      <div class="search">
        <el-input
          v-model="searchKeyword"
          placeholder="输入文件名搜索"
          class="search-input"
          size="large"
          clearable
          @input="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
    </div>

    <!-- 文件列表 -->
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
                <div class="file-name-cell">
                  <!-- 使用缩略图或图标 -->
                  <template v-if="row.fileCover">
                    <el-image
                      :src="`${baseURL}/api/file/getFile/${row.fileCover}`"
                      class="file-cover"
                      :preview-src-list="[]"
                      fit="cover"
                    >
                      <template #error>
                        <el-icon :size="20" class="file-icon">
                          <component :is="getFileIcon(row)" />
                        </el-icon>
                      </template>
                    </el-image>
                  </template>
                  <template v-else>
                    <el-icon :size="20" class="file-icon">
                      <component :is="getFileIcon(row)" />
                    </el-icon>
                  </template>
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
            <el-table-column prop="recoverTime" label="删除时间" width="180" />
            <el-table-column label="剩余保存时间" width="150">
              <template #default="{ row }">
                {{ calculateRemainingTime(row.recoverTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button-group>
                  <el-button 
                    type="primary" 
                    link
                    @click.stop="handleRestore([row])"
                  >
                    恢复
                  </el-button>
                  <el-button 
                    type="danger" 
                    link
                    @click.stop="handleDelete([row])"
                  >
                    彻底删除
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
              background
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </div>

        <!-- 空状态 -->
        <div v-else class="empty-state">
          <el-empty description="回收站为空" />
        </div>
      </div>
    </div>

    <!-- 确认删除对话框 -->
    <el-dialog
      v-model="deleteDialogVisible"
      title="确认删除"
      width="30%"
    >
      <span>确定要彻底删除选中的文件吗？此操作不可恢复。</span>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="deleteDialogVisible = false">取消</el-button>
          <el-button type="danger" @click="confirmDelete">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { Search, Folder, Document, VideoCamera, Headset, Picture, Files, Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getRecycleFileList, recoverFiles, deleteFiles } from '@/api/recycle'
import { formatFileSize, isDocumentFile, isImageFile } from '@/utils/file'
import { getFolderSpace } from '@/api/file'

// 定义 props 接收父组件的更新空间方法
const props = defineProps({
  updateUserSpace: {
    type: Function,
    required: true
  }
})

// 状态变量
const fileList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const showSelection = ref(false)
const selectedFiles = ref([])
const deleteDialogVisible = ref(false)
const searchTimer = ref(null)
const folderSizes = ref({})

// 添加基础URL
const baseURL = import.meta.env.VITE_API_URL

// 获取文件列表
const fetchFileList = async () => {
  try {
    const res = await getRecycleFileList({
      fileName: searchKeyword.value,
      page: currentPage.value,
      pageSize: pageSize.value
    })
    
    if (res.data) {
      // 过滤出顶级文件和文件夹
      const allFiles = res.data.rows
      const topLevelFiles = allFiles.filter(file => {
        // 如果是根目录的文件/文件夹，直接保留
        if (file.filePid === '0') return true
        
        // 如果父文件夹不在回收站列表中，说明是顶级文件/文件夹
        const parentInRecycle = allFiles.some(f => f.fileId === file.filePid)
        return !parentInRecycle
      })

      fileList.value = topLevelFiles
      total.value = topLevelFiles.length // 更新总数为过滤后的数量
    }
  } catch (error) {
    console.error('获取回收站文件列表失败:', error)
    ElMessage.error('获取回收站文件列表失败')
  }
}

// 搜索处理
const handleSearch = () => {
  if (searchTimer.value) {
    clearTimeout(searchTimer.value)
  }
  searchTimer.value = setTimeout(() => {
    currentPage.value = 1
    fetchFileList()
  }, 300)
}

// 分页处理
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchFileList()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchFileList()
}

// 批量操作相关
const startBatchOperation = () => {
  showSelection.value = true
}

const cancelBatchOperation = () => {
  showSelection.value = false
  selectedFiles.value = []
}

const handleSelectionChange = (selection) => {
  selectedFiles.value = selection
}

// 处理恢复
const handleRestore = async (files) => {
  try {
    const fileIds = files.map(file => file.fileId).join(',')
    await recoverFiles(fileIds)
    ElMessage.success('恢复成功')
    // 刷新列表
    await fetchFileList()
    // 更新用户空间信息
    await props.updateUserSpace()
    // 如果是批量操作，重置选择状态
    if (showSelection.value) {
      showSelection.value = false
      selectedFiles.value = []
    }
  } catch (error) {
    console.error('恢复失败:', error)
    ElMessage.error('恢复失败')
  }
}

// 确认删除
const confirmDelete = async () => {
  try {
    const fileIds = selectedFiles.value.map(file => file.fileId).join(',')
    await deleteFiles(fileIds)
    ElMessage.success('删除成功')
    
    // 重置对话框状态
    deleteDialogVisible.value = false
    
    // 刷新列表并重置选择状态
    await fetchFileList()
    // 更新用户空间信息
    await props.updateUserSpace()
    showSelection.value = false
    selectedFiles.value = []
  } catch (error) {
    console.error('删除失败:', error)
    ElMessage.error('删除失败')
  }
}

// 处理删除
const handleDelete = (files) => {
  // 确保 files 是数组
  selectedFiles.value = Array.isArray(files) ? files : [files]
  deleteDialogVisible.value = true
}

// 批量操作
const handleBatchRestore = () => {
  if (selectedFiles.value.length === 0) {
    ElMessage.warning('请选择要恢复的文件')
    return
  }
  handleRestore(selectedFiles.value)
}

const handleBatchDelete = () => {
  if (selectedFiles.value.length === 0) {
    ElMessage.warning('请选择要删除的文件')
    return
  }
  handleDelete(selectedFiles.value)
}

// 修改文件列表列的显示
const getFileIcon = (row) => {
  if (row.folderType === 1) return Folder
  if (row.fileCategory === 1) return VideoCamera
  if (row.fileCategory === 2) return Headset
  if (row.fileCategory === 3) return Picture
  if (row.fileCategory === 4) return Document
  return Files
}

// 计算文件夹大小
const calculateSize = (file) => {
  if (file.folderType === 1) {
    // 获取原始列表中所有属于这个文件夹的子文件（包括子文件夹的文件）
    const getAllChildrenSize = (parentId) => {
      const children = fileList.value.filter(item => item.filePid === parentId)
      let totalSize = 0
      
      children.forEach(child => {
        if (child.folderType === 1) {
          // 如果是文件夹，递归计算
          totalSize += getAllChildrenSize(child.fileId)
        } else {
          // 如果是文件，直接加上文件大小
          totalSize += child.fileSize || 0
        }
      })
      
      return totalSize
    }
    
    return formatFileSize(getAllChildrenSize(file.fileId))
  }
  return formatFileSize(file.fileSize)
}

// 计算剩余保存时间
const calculateRemainingTime = (recoverTime) => {
  if (!recoverTime) return '未知'
  const deleteTime = new Date(recoverTime)
  const expiryTime = new Date(deleteTime.getTime() + 5 * 24 * 60 * 60 * 1000)
  const now = new Date()
  const remainingDays = Math.ceil((expiryTime - now) / (24 * 60 * 60 * 1000))
  
  if (remainingDays <= 0) return '即将删除'
  if (remainingDays === 1) return '最后1天'
  return `${remainingDays}天`
}

// 获取文件夹大小
const loadFolderSize = async (folderId) => {
  try {
    const res = await getFolderSpace(folderId, 1) // type=1 表示回收站文件
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

// 初始化
onMounted(() => {
  fetchFileList()
})

// 组件卸载时清理定时器
onUnmounted(() => {
  if (searchTimer.value) {
    clearTimeout(searchTimer.value)
  }
})
</script>

<style scoped>
@import '../../assets/styles/layout.css';

.notice-bar {
  margin-bottom: 8px;
}

.left {
  display: flex;
  gap: 16px;
  align-items: center;
}

.search {
  width: 360px;
}

.file-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  width: 100%;
}

.file-name-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.file-cover {
  width: 32px;
  height: 32px;
  border-radius: 4px;
  object-fit: cover;
}

.file-icon {
  color: #606266;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.el-table) {
  width: 100% !important;
}

:deep(.el-table__inner-wrapper) {
  width: 100%;
}

:deep(.el-table__body) {
  width: 100% !important;
}

:deep(.el-button-group) {
  display: flex;
  gap: 8px;
}

:deep(.el-button-group .el-button) {
  margin-left: 0 !important;
}
</style> 
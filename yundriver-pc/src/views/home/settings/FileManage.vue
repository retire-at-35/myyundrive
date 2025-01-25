<template>
  <div class="file-manage">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="输入文件名搜索"
        class="search-input"
        clearable
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </div>

    <!-- 文件列表 -->
    <div class="table-container">
      <el-table 
        :data="fileList" 
        style="width: 100%" 
        v-loading="loading"
        :height="tableHeight"
      >
        <el-table-column label="文件名" min-width="300">
          <template #default="{ row }">
            <div class="file-name-cell" @click="handleFileClick(row)">
              <el-icon :size="20" class="file-icon">
                <Document v-if="isDocumentFile(row.fileName)" />
                <VideoCamera v-else-if="row.fileCategory === 1" />
                <Headset v-else-if="row.fileCategory === 2" />
                <Picture v-else-if="isImageFile(row.fileName)" />
                <Files v-else />
              </el-icon>
              <span class="file-name">{{ row.fileName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column 
          prop="email" 
          label="所属用户" 
          width="200"
          show-overflow-tooltip
        />
        <el-table-column prop="fileSize" label="大小" width="120">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="lastUpdateTime" label="上传时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.lastUpdateTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button-group>
              <el-button 
                type="primary" 
                link
                @click="handlePreview(row)"
              >
                预览
              </el-button>
              <el-button 
                type="primary" 
                link
                @click="handleDownload(row)"
              >
                下载
              </el-button>
              <el-button 
                type="danger" 
                link
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>
    </div>

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

    <!-- 文件预览组件 -->
    <file-preview
      v-if="previewFile"
      v-model="showPreview"
      :file="previewFile"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Search, Document, VideoCamera, Headset, Picture, Files } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { formatDate } from '@/utils/date'
import { formatFileSize, isDocumentFile, isImageFile } from '@/utils/file'
import { loadAllFileList, deleteFile } from '@/api/admin'
import { downloadFiles } from '@/api/file'
import FilePreview from '@/components/FilePreview.vue'

// 状态变量
const loading = ref(false)
const fileList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')

// 预览相关
const showPreview = ref(false)
const previewFile = ref(null)

// 计算表格高度
const tableHeight = computed(() => {
  return 'calc(100vh - 280px)'
})

// 获取文件列表
const fetchFileList = async () => {
  loading.value = true
  try {
    const res = await loadAllFileList({
      page: currentPage.value,
      pageSize: pageSize.value,
      fileName: searchKeyword.value
    })
    if (res.code === 1) {
      fileList.value = res.data.rows
      total.value = res.data.total
    }
  } catch (error) {
    console.error('获取文件列表失败:', error)
    ElMessage.error('获取文件列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchFileList()
}

// 重置搜索
const resetSearch = () => {
  searchKeyword.value = ''
  handleSearch()
}

// 处理文件点击
const handleFileClick = (file) => {
  const fileType = getFileTypeNumber(file)
  
  if (fileType === 0) {
    ElMessage.info('该文件类型暂不支持预览')
    return
  }
  
  previewFile.value = {
    ...file,
    fileType,
    isVideo: fileType === 1,
    previewUrl: `/api/file/getFile?fileId=${file.fileId}`,
    videoInfoUrl: fileType === 1 ? `/api/file/ts/getVideoInfo?fileId=${file.fileId}` : undefined
  }
  showPreview.value = true
}

// 处理预览错误
const handlePreviewError = () => {
  ElMessage.info('该文件类型暂不支持预览')
}

// 处理下载
const handleDownload = async (file) => {
  if (loading.value) return // 防止重复点击
  
  try {
    loading.value = true
    await downloadFiles(file.fileId)
    ElMessage.success('下载成功')
  } catch (error) {
    console.error('下载失败:', error)
    ElMessage.error('下载失败')
  } finally {
    loading.value = false
  }
}

// 删除文件
const handleDelete = async (file) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除该文件吗？此操作不可恢复！', 
      '警告', 
      {
        type: 'warning',
        confirmButtonText: '删除',
        confirmButtonClass: 'el-button--danger'
      }
    )
    
    loading.value = true
    const res = await deleteFile(file.userId, file.fileId)
    if (res.code === 1) {
      ElMessage.success('删除成功')
      fetchFileList()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除文件失败:', error)
      ElMessage.error('删除文件失败')
    }
  } finally {
    loading.value = false
  }
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

// 初始化
onMounted(() => {
  fetchFileList()
})
</script>

<style scoped>
.file-manage {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-width: 900px;
}

.search-bar {
  flex-shrink: 0;
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.search-input {
  width: 300px;
}

.table-container {
  flex: 1;
  overflow: hidden;
}

.file-name-cell {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.file-name-cell:hover .file-name {
  color: var(--el-color-primary);
}

.file-icon {
  color: var(--el-text-color-regular);
}

.file-name {
  color: var(--el-text-color-primary);
  transition: color 0.2s;
}

.pagination {
  flex-shrink: 0;
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

:deep(.el-button-group) {
  display: flex;
  gap: 8px;
}
</style> 
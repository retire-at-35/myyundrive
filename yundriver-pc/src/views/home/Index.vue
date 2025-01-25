<template>
  <div class="page-container">
    <!-- 顶部操作栏 -->
    <div class="action-bar">
      <div class="left">
        <el-button type="primary" size="large" @click="handleUpload">上传</el-button>
        <el-button size="large" @click="handleNewFolder">新建文件夹</el-button>
        <!-- 批量操作按钮组 -->
        <template v-if="!showSelection">
          <el-button size="large" @click="startBatchOperation">批量操作</el-button>
        </template>
        <template v-else>
          <el-button-group>
            <el-button 
              type="danger" 
              size="large" 
              @click="handleBatchDelete"
              :disabled="!selectedFiles.length"
            >
              删除
            </el-button>
            <el-button 
              type="primary"
              size="large" 
              @click="handleBatchMove"
              :disabled="!selectedFiles.length"
            >
              移动到
            </el-button>
            <el-button 
              type="primary"
              size="large" 
              @click="handleBatchDownload"
              :disabled="!selectedFiles.length"
            >
              下载
            </el-button>
            <el-button 
              type="primary"
              size="large" 
              @click="handleBatchShare"
              :disabled="!selectedFiles.length"
            >
              分享
            </el-button>
          </el-button-group>
          <el-button size="large" @click="cancelBatchOperation">
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
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
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

    <!-- 文件列表 -->
    <div class="content-area">
      <div class="table-container">
        <div v-if="fileList.length > 0" class="file-list">
          <el-table
            :data="fileList"
            style="width: 100%"
            row-key="fileId"
            :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
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
            <el-table-column label="操作" width="280">
              <template #default="{ row }">
                <el-dropdown trigger="click" @command="(command) => handleCommand(command, row)">
                  <el-button type="primary" link>
                    操作<el-icon class="el-icon--right"><arrow-down /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <!-- 文件夹操作菜单 -->
                      <template v-if="row.folderType === 1">
                        <el-dropdown-item command="open">
                          <el-icon><FolderOpened /></el-icon>打开
                        </el-dropdown-item>
                        <el-dropdown-item command="share">
                          <el-icon><Share /></el-icon>分享
                        </el-dropdown-item>
                        <el-dropdown-item command="download">
                          <el-icon><Download /></el-icon>下载
                        </el-dropdown-item>
                        <el-dropdown-item divided command="move">
                          <el-icon><Right /></el-icon>移动到
                        </el-dropdown-item>
                        <el-dropdown-item command="delete">
                          <el-icon><Delete /></el-icon>删除
                        </el-dropdown-item>
                      </template>
                      <!-- 文件操作菜单 -->
                      <template v-else>
                        <el-dropdown-item command="download">
                          <el-icon><Download /></el-icon>下载
                        </el-dropdown-item>
                        <el-dropdown-item command="share">
                          <el-icon><Share /></el-icon>分享
                        </el-dropdown-item>
                        <el-dropdown-item command="move">
                          <el-icon><Right /></el-icon>移动
                        </el-dropdown-item>
                        <el-dropdown-item command="rename">
                          <el-icon><Edit /></el-icon>重命名
                        </el-dropdown-item>
                        <el-dropdown-item command="delete">
                          <el-icon><Delete /></el-icon>删除
                        </el-dropdown-item>
                      </template>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
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
              :small="false"
              background
              :pager-count="7"
              prev-text="上一页"
              next-text="下一页"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            >
              <template #total>
                <span>共 {{ total }} 条</span>
              </template>
              <template #sizes>
                <el-space>
                  <span>每页</span>
                  <el-select v-model="pageSize" style="width: 100px">
                    <el-option
                      v-for="size in [10, 20, 50, 100]"
                      :key="size"
                      :value="size"
                      :label="`${size}条`"
                    />
                  </el-select>
                  <span>条</span>
                </el-space>
              </template>
              <template #jumper>
                <el-space>
                  <span>前往</span>
                  <el-input
                    v-model="currentPage"
                    class="jump-input"
                    type="number"
                    :min="1"
                    :max="Math.ceil(total / pageSize)"
                  />
                  <span>页</span>
                </el-space>
              </template>
            </el-pagination>
          </div>
        </div>
        <div v-else class="empty-state">
          <el-empty>
            <template #description>
              <p>当前目录为空</p>
            </template>
            <template #default>
              <div class="empty-actions">
                <el-button type="primary" @click="handleUpload">
                  <el-icon><Upload /></el-icon>上传文件
                </el-button>
                <el-button @click="handleNewFolder">
                  <el-icon><FolderAdd /></el-icon>新建文件夹
                </el-button>
              </div>
            </template>
          </el-empty>
        </div>
      </div>
    </div>

    <!-- 文件上传对话框 -->
    <el-dialog
      v-model="uploadDialogVisible"
      title="上传文件"
      width="520px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <div class="upload-dialog-content">
        <!-- 上传按钮区域 -->
        <div class="upload-actions">
          <el-upload
            class="file-uploader"
            :auto-upload="false"
            :show-file-list="false"
            :on-change="handleFileChange"
            :disabled="uploading"
            multiple
          >
            <template #trigger>
              <el-button 
                type="primary" 
                :disabled="uploading"
                size="large"
              >
                <el-icon><Upload /></el-icon>
                选择文件
              </el-button>
            </template>
          </el-upload>
          
          <el-button 
            type="primary" 
            @click="startUpload" 
            :loading="uploading"
            :disabled="uploadingFiles.length === 0"
            size="large"
          >
            {{ uploading ? '正在上传' : '开始上传' }}
          </el-button>
        </div>

        <!-- 文件列表 -->
        <div v-if="uploadingFiles.length > 0" class="upload-list">
          <div 
            v-for="file in uploadingFiles" 
            :key="file.name" 
            class="upload-item"
            :class="{ 'upload-item-error': file.status === 'error' }"
          >
            <div class="file-info">
              <el-icon class="file-icon"><Document /></el-icon>
              <div class="file-details">
                <div class="file-name-row">
                  <span class="file-name" :title="file.name">{{ file.name }}</span>
                  <span class="file-size">{{ formatFileSize(file.size) }}</span>
                </div>
                <div class="progress-row">
                  <el-progress 
                    :percentage="file.progress" 
                    :status="file.status === 'error' ? 'exception' : file.progress === 100 ? 'success' : ''"
                    :show-text="false"
                    :stroke-width="4"
                  />
                  <span class="progress-text" :class="{ 'text-error': file.status === 'error' }">
                    {{ getProgressText(file) }}
                  </span>
                </div>
              </div>
              <el-button 
                v-if="file.status === 'error'"
                type="primary" 
                link
                @click="retryUpload(file)"
                :disabled="uploading"
              >
                重试
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button 
            @click="closeUploadDialog" 
            :disabled="uploading"
          >
            {{ uploading ? '正在上传' : '关闭' }}
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 新建文件夹对话框 -->
    <el-dialog
      v-model="folderDialogVisible"
      title="新建文件夹"
      width="400px"
    >
      <el-form
        ref="folderFormRef"
        :model="folderForm"
        :rules="folderRules"
        label-width="80px"
      >
        <el-form-item label="文件夹名" prop="folderName">
          <el-input 
            v-model="folderForm.folderName"
            placeholder="请输入文件夹名称"
            @keyup.enter="submitNewFolder"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="folderDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitNewFolder" :loading="creating">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 重命名对话框 -->
    <el-dialog
      v-model="renameDialogVisible"
      title="重命名"
      width="400px"
    >
      <el-form
        ref="renameFormRef"
        :model="renameForm"
        :rules="renameRules"
        label-width="80px"
      >
        <el-form-item label="名称" prop="fileName">
          <el-input 
            v-model="renameForm.fileName"
            placeholder="请输入新名称"
            @keyup.enter="submitRename"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="renameDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitRename" :loading="renaming">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 文件预览组件 -->
    <FilePreview
      v-model="previewVisible"
      :file="previewFile"
      @preview-error="handlePreviewError"
    />

    <!-- 确认删除对话框 -->
    <el-dialog
      v-model="deleteDialogVisible"
      title="确认删除"
      width="30%"
    >
      <span>确定要删除选中的文件吗？此操作将文件移至回收站。</span>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="deleteDialogVisible = false">取消</el-button>
          <el-button type="danger" @click="confirmDelete">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 添加移动文件对话框 -->
    <el-dialog
      v-model="moveDialogVisible"
      title="移动到"
      width="500px"
    >
      <div class="folder-tree">
        <el-tree
          ref="folderTreeRef"
          :data="[{ fileId: '0', fileName: '全部文件', children: [] }]"
          node-key="fileId"
          :props="{
            label: 'fileName',
            children: 'children',
            isLeaf: 'isLeaf'
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
          <el-button @click="moveDialogVisible = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="confirmMove"
            :disabled="!selectedFolderId || selectedFiles.some(file => file.fileId === selectedFolderId)"
          >
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 分享对话框 -->
    <el-dialog
      v-model="shareDialogVisible"
      title="创建分享"
      width="400px"
    >
      <el-form
        ref="shareFormRef"
        :model="shareForm"
        label-width="100px"
      >
        <el-form-item label="有效期">
          <el-select v-model="shareForm.validType" class="w-full">
            <el-option :value="0" label="1天" />
            <el-option :value="1" label="7天" />
            <el-option :value="2" label="30天" />
            <el-option :value="3" label="永久有效" />
          </el-select>
        </el-form-item>
        <el-form-item label="提取码">
          <div class="code-input-group">
            <el-radio-group v-model="shareForm.codeType" class="mb-3">
              <el-radio :label="0">自动生成</el-radio>
              <el-radio :label="1">自定义</el-radio>
            </el-radio-group>
            <el-input
              v-if="shareForm.codeType === 1"
              v-model="shareForm.code"
              placeholder="请输入5位提取码"
              maxlength="5"
              show-word-limit
            />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="shareDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmShare" :loading="sharing">
            创建分享
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, reactive, onUnmounted } from 'vue'
import { Search, Folder, Document, VideoCamera, Headset, Picture, Files, Delete, FolderAdd, Upload, Loading, FolderOpened, Share, Download, Right, Edit, ArrowDown } from '@element-plus/icons-vue'
import { FileUploader } from '@/utils/upload'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getFileList, createFolder, renameFile, deleteFile, getAllFolderInfo, changeFileFolder, downloadFiles, getFolderSpace } from '@/api/file'
import FilePreview from '@/components/FilePreview.vue'
import { createShare } from '@/api/share'
import { useUserStore } from '@/stores/user'

const searchKeyword = ref('')
const uploadDialogVisible = ref(false)
const uploadingFiles = ref([])
const uploading = ref(false)

// 文件列表相关
const fileList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 当前文件夹路径
const currentFolderId = ref('0')
const folderPath = ref([])

// 新建文件夹相关
const folderDialogVisible = ref(false)
const folderFormRef = ref(null)
const creating = ref(false)
const folderForm = reactive({
  folderName: ''
})

const folderRules = {
  folderName: [
    { required: true, message: '请输入文件夹名称', trigger: 'blur' },
    { min: 1, max: 100, message: '长度在 1 到 100 个字符', trigger: 'blur' }
  ]
}

// 重命名相关
const renameDialogVisible = ref(false)
const renameFormRef = ref(null)
const renaming = ref(false)
const renameForm = reactive({
  fileId: '',
  fileName: '',
  extension: '' // 保存文件扩展名
})

const renameRules = {
  fileName: [
    { required: true, message: '请输入名称', trigger: 'blur' },
    { min: 1, max: 100, message: '长度在 1 到 100 个字符', trigger: 'blur' }
  ]
}

// 添加响应式变量存储文件夹大小
const folderSizes = ref({})

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

// 处理文件选择
const handleFileChange = (file) => {
  // 检查是否已存在相同文件
  const exists = uploadingFiles.value.some(f => f.name === file.name)
  if (!exists) {
    uploadingFiles.value.push({
      raw: file.raw,
      name: file.name,
      size: file.size,
      progress: 0,
      status: 'waiting'
    })
  }
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

// 开始上传所有文件
const startUpload = async () => {
  if (uploadingFiles.value.length === 0) return
  
  uploading.value = true
  let successCount = 0
  let totalFiles = uploadingFiles.value.length
  
  try {
    for (const file of uploadingFiles.value) {
      if (file.status === 'success') {
        successCount++
        continue
      }
      
      file.status = 'uploading'
      const uploader = new FileUploader(file.raw, currentFolderId.value)
      
      try {
        const result = await uploader.upload((progress) => {
          file.progress = progress
        })
        
        if (result.status === 'success') {
          file.status = 'success'
          file.progress = 100
          successCount++
          // 每个文件上传成功后立即刷新列表
          await fetchFileList()
        }
      } catch (error) {
        file.status = 'error'
        ElMessage.error(`文件 ${file.name} 上传失败：${error.message}`)
      }
    }
    
    // 显示最终上传结果
    if (successCount > 0) {
      if (successCount === totalFiles) {
        ElMessage.success('所有文件上传成功')
      } else {
        ElMessage.warning(`${successCount}/${totalFiles} 个文件上传成功`)
      }
      
      // 延迟关闭对话框，让用户看到上传完成状态
      setTimeout(() => {
        uploadDialogVisible.value = false
        uploadingFiles.value = []
      }, 1000)
    }
  } finally {
    uploading.value = false
  }
}

// 获取文件列表
const fetchFileList = async () => {
  try {
    const res = await getFileList({
      fileName: searchKeyword.value,
      page: currentPage.value,
      pageSize: pageSize.value,
      filePid: currentFolderId.value
    })
    
    if (res.code === 1) {
      fileList.value = res.data.rows || []
      total.value = res.data.total || 0
    }
  } catch (error) {
    console.error('获取文件列表失败:', error)
    ElMessage.error('获取文件列表失败')
  }
}

// 处理搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchFileList()
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

// 监听搜索关键字变化
watch(searchKeyword, () => {
  handleSearch()
})

// 保存当前路径状态
const savePathState = () => {
  const state = {
    currentFolderId: currentFolderId.value,
    folderPath: folderPath.value
  }
  localStorage.setItem('filePathState', JSON.stringify(state))
}

// 恢复路径状态
const restorePathState = () => {
  const stateStr = localStorage.getItem('filePathState')
  if (stateStr) {
    try {
      const state = JSON.parse(stateStr)
      currentFolderId.value = state.currentFolderId
      folderPath.value = state.folderPath
    } catch (error) {
      console.error('恢复路径状态失败:', error)
    }
  }
}

// 导航到指定文件夹
const navigateToFolder = async (folderId, pathIndex) => {
  console.log('导航到文件夹:', folderId, '路径索引:', pathIndex, '当前路径:', folderPath.value)
  
  currentFolderId.value = folderId
  currentPage.value = 1

  // 更新面包屑导航
  if (folderId === '0') {
    folderPath.value = []
  } else if (typeof pathIndex === 'number') {
    folderPath.value = folderPath.value.slice(0, pathIndex + 1)
  } else {
    const folder = fileList.value.find(item => item.fileId === folderId)
    if (folder) {
      folderPath.value.push(folder)
    }
  }

  await fetchFileList()
  savePathState() // 保存路径状态
}

// 初始化加载
onMounted(async () => {
  restorePathState() // 先恢复路径状态
  await fetchFileList() // 然后获取文件列表
})

// 在组件卸载时清除路径状态
onUnmounted(() => {
  localStorage.removeItem('filePathState')
})

// 处理新建文件夹
const handleNewFolder = async () => {
  try {
    const { value: folderName } = await ElMessageBox.prompt('请输入文件夹名称', '新建文件夹', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /^[^\\/:\*\?"<>\|]+$/,
      inputErrorMessage: '文件夹名称不能包含特殊字符',
      beforeClose: (action, instance, done) => {
        if (action === 'confirm' && !instance.inputValue) {
          ElMessage.warning('请输入文件夹名称')
          return
        }
        done()
      }
    })

    if (folderName) {
      const params = {
        fileName: folderName,
        filePid: currentFolderId.value
      }
      console.log('创建文件夹参数:', params) // 添加日志
      
      const res = await createFolder(params)
      if (res.code === 1) {
        ElMessage.success('创建成功')
        await fetchFileList()
      } else {
        ElMessage.error(res.message || '创建失败')
      }
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('创建文件夹失败:', error)
      ElMessage.error('创建文件夹失败')
    }
  }
}

// 判断是否为图片文件
const isImageFile = (fileName) => {
  const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp']
  const ext = fileName.toLowerCase().substring(fileName.lastIndexOf('.'))
  return imageExtensions.includes(ext)
}

// 判断是否为文档文件
const isDocumentFile = (fileName) => {
  const docExtensions = ['.pdf', '.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx', '.txt']
  const ext = fileName.toLowerCase().substring(fileName.lastIndexOf('.'))
  return docExtensions.includes(ext)
}

// 打开重命名对话框
const handleRename = (row) => {
  renameForm.fileId = row.fileId
  
  if (row.folderType === 1) {
    // 文件夹直接使用完整名称
    renameForm.fileName = row.fileName
  } else {
    // 文件只取文件名部分，不包含扩展名
    const lastDotIndex = row.fileName.lastIndexOf('.')
    if (lastDotIndex > -1) {
      renameForm.fileName = row.fileName.substring(0, lastDotIndex)
    } else {
      renameForm.fileName = row.fileName
    }
  }
  
  renameDialogVisible.value = true
}

// 提交重命名
const submitRename = async () => {
  if (!renameFormRef.value) return
  
  await renameFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        renaming.value = true
        
        const res = await renameFile({
          fileId: renameForm.fileId,
          fileName: renameForm.fileName // 直接使用输入的名称，不添加扩展名
        })
        
        if (res.code === 1) {
          ElMessage.success('重命名成功')
          renameDialogVisible.value = false
          await fetchFileList() // 刷新文件列表
        }
      } finally {
        renaming.value = false
      }
    }
  })
}

const previewVisible = ref(false)
const previewFile = ref(null)
const currentFile = ref(null)

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
      previewUrl: `/api/file/getFile?fileId=${file.fileId}`,
      videoInfoUrl: fileType === 1 ? `/api/file/ts/getVideoInfo?fileId=${file.fileId}` : undefined
    }
    previewVisible.value = true
  }
}

// 批量操作相关
const showSelection = ref(false)
const selectedFiles = ref([])
const deleteDialogVisible = ref(false)
const moveDialogVisible = ref(false)
const folderTree = ref([])
const selectedFolderId = ref('')
const folderTreeRef = ref(null)

// 开始批量操作
const startBatchOperation = () => {
  showSelection.value = true
}

// 取消批量操作
const cancelBatchOperation = () => {
  showSelection.value = false
  selectedFiles.value = []
}

// 处理单个或批量删除
const handleDelete = (files) => {
  // 确保 files 是数组
  const filesToDelete = Array.isArray(files) ? files : [files]
  selectedFiles.value = filesToDelete
  deleteDialogVisible.value = true
}

// 处理单个或批量移动
const handleMove = async (files) => {
  selectedFiles.value = files
  moveDialogVisible.value = true
  selectedFolderId.value = ''
}

// 处理文件夹选择
const handleFolderSelect = (folder) => {
  // 不能选择被移动的文件夹或其子文件夹
  if (selectedFiles.value.some(file => file.fileId === folder.fileId)) {
    ElMessage.warning('不能移动到被选中的文件夹')
    return
  }
  
  // 不能移动到当前所在文件夹
  if (folder.fileId === currentFolderId.value) {
    ElMessage.warning('不能移动到当前文件夹')
    return
  }
  
  selectedFolderId.value = folder.fileId
}

// 修改批量删除处理函数
const handleBatchDelete = () => {
  if (selectedFiles.value.length === 0) {
    ElMessage.warning('请选择要删除的文件')
    return
  }
  handleDelete(selectedFiles.value)
}

// 修改批量移动处理函数
const handleBatchMove = () => {
  if (selectedFiles.value.length === 0) {
    ElMessage.warning('请选择要移动的文件')
    return
  }
  handleMove(selectedFiles.value)
}

// 确认移动
const confirmMove = async () => {
  if (!selectedFolderId.value) {
    ElMessage.warning('请选择目标文件夹')
    return
  }

  try {
    await changeFileFolder({
      fileIds: selectedFiles.value.map(file => file.fileId).join(','),
      filePid: selectedFolderId.value
    })
    ElMessage.success('移动成功')
    moveDialogVisible.value = false
    selectedFolderId.value = ''
    showSelection.value = false
    selectedFiles.value = []
    await fetchFileList()
  } catch (error) {
    console.error('移动失败:', error)
    ElMessage.error('移动失败')
  }
}

// 确认删除
const confirmDelete = async () => {
  try {
    const fileIds = selectedFiles.value.map(file => file.fileId).join(',')
    await deleteFile(fileIds)
    ElMessage.success('删除成功')
    
    // 先重置对话框状态
    deleteDialogVisible.value = false
    
    // 然后刷新列表并重置其他状态
    await fetchFileList()
    showSelection.value = false
    selectedFiles.value = []
  } catch (error) {
    console.error('删除失败:', error)
    ElMessage.error('删除失败')
  }
}

// 处理选择变化
const handleSelectionChange = (selection) => {
  selectedFiles.value = selection
}

// 加载文件夹节点
const loadNode = async (node, resolve) => {
  if (node.level === 0) {
    // 根节点，直接返回
    resolve([{ fileId: '0', fileName: '全部文件', children: [] }])
    return
  }

  try {
    // 每次重新获取最新的文件夹数据
    const res = await getAllFolderInfo(node.data.fileId)
    if (res.code === 1 && res.data) {
      // 过滤掉被移动的文件夹及其子文件夹
      const filteredData = res.data.filter(folder => {
        // 检查是否是被移动的文件夹
        const isMovingFolder = selectedFiles.value.some(file => file.fileId === folder.fileId)
        // 检查是否是被移动文件夹的子文件夹
        const isChildOfMovingFolder = selectedFiles.value.some(file => 
          file.folderType === 1 && folder.filePid === file.fileId
        )
        return !isMovingFolder && !isChildOfMovingFolder
      })
      resolve(filteredData)
    } else {
      resolve([])
    }
  } catch (error) {
    console.error('加载文件夹失败:', error)
    resolve([])
  }
}

// 监听对话框打开状态
watch(moveDialogVisible, async (newVal) => {
  if (newVal) {
    // 对话框打开时，重新加载根节点
    if (folderTreeRef.value) {
      folderTreeRef.value.store.setData([{ fileId: '0', fileName: '全部文件', children: [] }])
      folderTreeRef.value.setCurrentKey(null)
      selectedFolderId.value = ''
    }
  }
})

// 处理单个或批量下载
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

// 处理批量下载
const handleBatchDownload = () => {
  if (selectedFiles.value.length === 0) {
    ElMessage.warning('请选择要下载的文件')
    return
  }
  
  if (loading.value) return
  handleDownload(selectedFiles.value[0])
}

// 分享相关
const shareDialogVisible = ref(false)
const shareForm = reactive({
  fileId: '',
  validType: 1, // 默认7天
  codeType: 0, // 0-自动生成，1-自定义
  code: ''
})
const sharing = ref(false)

// 处理分享
const handleShare = (files) => {
  // 确保 files 是数组
  const filesToShare = Array.isArray(files) ? files : [files]
  // 重置表单
  shareForm.fileId = filesToShare.map(file => file.fileId).join(',')
  shareForm.validType = 1
  shareForm.codeType = 0
  shareForm.code = ''
  shareDialogVisible.value = true
}

// 确认分享
const confirmShare = async () => {
  // 验证提取码
  if (shareForm.codeType === 1) {
    if (!shareForm.code) {
      ElMessage.warning('请输入提取码')
      return
    }
    if (!/^\w{5}$/.test(shareForm.code)) {
      ElMessage.warning('提取码必须是5位字母或数字')
      return
    }
  }

  try {
    sharing.value = true
    const res = await createShare({
      fileId: shareForm.fileId,
      validType: shareForm.validType,
      code: shareForm.codeType === 1 ? shareForm.code : undefined
    })
    
    if (res.code === 1) {
      ElMessage.success('分享创建成功')
      // 构建分享信息
      const shareInfo = `分享链接：${window.location.origin}/share/${res.data.shareId}\n提取码：${res.data.code}`
      // 显示分享信息
      ElMessageBox.alert(
        shareInfo,
        '分享信息',
        {
          confirmButtonText: '复制信息',
          callback: () => {
            // 复制分享信息到剪贴板
            navigator.clipboard.writeText(shareInfo)
            ElMessage.success('分享信息已复制到剪贴板')
          }
        }
      )
      shareDialogVisible.value = false
    }
  } catch (error) {
    console.error('创建分享失败:', error)
    ElMessage.error('创建分享失败')
  } finally {
    sharing.value = false
  }
}

// 批量分享
const handleBatchShare = () => {
  if (selectedFiles.value.length === 0) {
    ElMessage.warning('请选择要分享的文件')
    return
  }
  handleShare(selectedFiles.value)
}

// 在文件列表数据更新时获取文件夹大小
watch(() => fileList.value, (newList) => {
  // 获取所有文件夹的大小
  newList.forEach(file => {
    if (file.folderType === 1 && folderSizes.value[file.fileId] === undefined) {
      loadFolderSize(file.fileId)
    }
  })
}, { immediate: true })

// 获取进度文本
const getProgressText = (file) => {
  if (file.status === 'error') return '上传失败'
  if (file.progress === 100) return '上传完成'
  return `${file.progress}%`
}

// 重试上传
const retryUpload = (file) => {
  file.status = 'waiting'
  file.progress = 0
}

// 关闭上传对话框
const closeUploadDialog = () => {
  if (uploading.value) {
    ElMessage.warning('文件正在上传中，请等待上传完成')
    return
  }
  uploadDialogVisible.value = false
  uploadingFiles.value = []
}

// 打开上传对话框
const handleUpload = () => {
  uploadDialogVisible.value = true
  uploadingFiles.value = []
}

// 处理预览错误
const handlePreviewError = (file) => {
  ElMessage.info('该文件类型暂不支持预览')
}

// 添加处理下拉菜单命令的函数
const handleCommand = (command, file) => {
  switch (command) {
    case 'open':
      handleFileClick(file)
      break
    case 'share':
      handleShare(file)
      break
    case 'download':
      handleDownload(file)
      break
    case 'move':
      handleMove([file])
      break
    case 'rename':
      handleRename(file)
      break
    case 'delete':
      handleDelete([file])
      break
  }
}

const userStore = useUserStore()

// 在其他状态变量定义的地方添加
const loading = ref(false)
</script>

<style scoped>
@import '../../assets/styles/layout.css';

/* 其他特定样式 */
.left {
  display: flex;
  gap: 16px;
  align-items: center;
}

.search {
  width: 360px;
}

/* 修改文件名单元格样式 */
.file-name-cell {
  display: flex;
  align-items: center; /* 垂直居中对齐 */
  gap: 12px;
  cursor: pointer;
  height: 40px; /* 固定高度 */
}

.file-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  color: #606266;
  flex-shrink: 0; /* 防止图标被压缩 */
}

.file-name {
  font-size: 14px;
  line-height: 1.5;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 调整表格行高 */
:deep(.el-table__row) {
  height: 50px; /* 设置表格行高 */
}

:deep(.el-table__cell) {
  padding-top: 8px !important;
  padding-bottom: 8px !important;
}

.empty-state {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 60px 0;
  text-align: center;
}

.empty-actions {
  display: flex;
  gap: 16px;
  margin-top: 20px;
}

.empty-actions .el-button {
  display: flex;
  align-items: center;
  gap: 8px;
}

.code-input-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.w-full {
  width: 100%;
}

.mb-3 {
  margin-bottom: 12px;
}

/* 上传对话框样式 */
.upload-dialog-content {
  padding: 0 20px;
}

.upload-actions {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.upload-list {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.upload-item {
  padding: 12px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.upload-item:last-child {
  border-bottom: none;
}

.upload-item-error {
  background-color: var(--el-color-danger-light-9);
}

.file-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.file-icon {
  font-size: 24px;
  color: var(--el-text-color-regular);
}

.file-details {
  flex: 1;
  min-width: 0;
}

.file-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.file-name {
  flex: 1;
  font-size: 14px;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

.progress-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.progress-row :deep(.el-progress-bar) {
  flex: 1;
}

.progress-text {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

.text-error {
  color: var(--el-color-danger);
}

/* 禁止关闭按钮在上传时点击 */
:deep(.el-dialog__close) {
  pointer-events: v-bind(uploading ? 'none' : 'auto');
  color: v-bind(uploading ? 'var(--el-text-color-disabled)' : 'var(--el-text-color-regular)');
}
</style> 
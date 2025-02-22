<template>
  <div class="share-page">
    <div class="content-area">
      <!-- 批量操作栏 -->
      <div class="action-bar">
        <div class="left">
          <el-button 
            type="danger" 
            :disabled="selectedShares.length === 0"
            @click="handleBatchCancel"
          >
            批量取消分享
          </el-button>
        </div>
      </div>

      <!-- 分享列表 -->
      <div v-if="shareList.length > 0" class="share-list">
        <el-table 
          :data="shareList" 
          style="width: 100%"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="55" />

          <!-- 分享链接列 -->
          <el-table-column label="分享链接" min-width="400">
            <template #default="{ row }">
              <div class="share-link">
                <el-link 
                  type="primary" 
                  :href="getShareUrl(row.shareId)" 
                  target="_blank"
                  class="share-url"
                >
                  {{ getShareUrl(row.shareId) }}
                </el-link>
                <template v-if="row.code">
                  <el-divider direction="vertical" />
                  <span class="code-text">提取码：{{ row.code }}</span>
                </template>
              </div>
            </template>
          </el-table-column>

          <!-- 分享时间列 -->
          <el-table-column prop="shareTime" label="分享时间" width="180">
            <template #default="{ row }">
              {{ formatDate(row.shareTime) }}
            </template>
          </el-table-column>

          <!-- 有效期列 -->
          <el-table-column label="有效期" width="180">
            <template #default="{ row }">
              <span v-if="row.validType === 0 || (!row.expireTime)">永久有效</span>
              <span v-else>
                {{ formatDate(row.expireTime) }} 到期
              </span>
            </template>
          </el-table-column>

          
          <!-- 操作列 -->
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button-group>
                <el-button 
                  type="primary" 
                  link
                  @click="copyShareLink(row)"
                >
                  复制链接
                </el-button>
                <el-button 
                  type="danger" 
                  link
                  @click="handleCancelShare(row)"
                >
                  取消分享
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

      <!-- 空状态 -->
      <div v-else class="empty-state">
        <el-empty description="暂无分享" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { loadShareList, cancelShare } from '@/api/share'
import { formatDate } from '@/utils/date'

// 状态变量
const shareList = ref([])
const selectedShares = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 获取分享链接
const getShareUrl = (shareId) => {
  return `${window.location.origin}/share/${shareId}`
}

// 获取分享列表
const fetchShareList = async () => {
  try {
    const res = await loadShareList({
      page: currentPage.value,
      pageSize: pageSize.value
    })
    
    if (res.code === 1) {
      shareList.value = res.data.rows
      total.value = res.data.totalCount
    }
  } catch (error) {
    console.error('获取分享列表失败:', error)
    ElMessage.error('获取分享列表失败')
  }
}

// 复制分享链接
const copyShareLink = async (share) => {
  const shareUrl = getShareUrl(share.shareId)
  let copyText = shareUrl
  if (share.code) {
    copyText += `\n提取码: ${share.code}`
  }

  try {
    await navigator.clipboard.writeText(copyText)
    ElMessage.success('链接已复制到剪贴板')
  } catch (error) {
    console.error('复制失败:', error)
    ElMessage.error('复制失败')
  }
}

// 处理选择变化
const handleSelectionChange = (selection) => {
  selectedShares.value = selection
}

// 批量取消分享
const handleBatchCancel = async () => {
  if (selectedShares.value.length === 0) return

  try {
    await ElMessageBox.confirm(
      `确定要取消这 ${selectedShares.value.length} 个分享吗？`, 
      '提示', 
      {
        type: 'warning'
      }
    )
    
    // 将选中的分享ID用逗号拼接
    const shareIds = selectedShares.value.map(share => share.shareId).join(',')
    const res = await cancelShare(shareIds)
    
    if (res.code === 1) {
      ElMessage.success('取消分享成功')
      fetchShareList()
      selectedShares.value = [] // 清空选择
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量取消分享失败:', error)
      ElMessage.error('批量取消分享失败')
    }
  }
}

// 取消单个分享
const handleCancelShare = async (share) => {
  try {
    await ElMessageBox.confirm('确定要取消分享吗？', '提示', {
      type: 'warning'
    })
    
    const res = await cancelShare(share.shareId)
    if (res.code === 1) {
      ElMessage.success('取消分享成功')
      fetchShareList()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消分享失败:', error)
      ElMessage.error('取消分享失败')
    }
  }
}

// 分页处理
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchShareList()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchShareList()
}

// 初始化
onMounted(() => {
  fetchShareList()
})
</script>

<style scoped>
.share-page {
  height: 100%;
  padding: 24px;
  min-width: 900px;
}

.content-area {
  background-color: var(--el-bg-color);
  border-radius: 8px;
  padding: 24px;
  min-height: 500px;
  width: 100%;
}

.action-bar {
  margin-bottom: 16px;
}

.file-info {
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

.file-name {
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.share-link {
  display: flex;
  align-items: center;
  gap: 8px;
}

.share-url {
  max-width: 400px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.code-text {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 400px;
}

:deep(.el-button-group) {
  display: flex;
  gap: 8px;
}
</style> 
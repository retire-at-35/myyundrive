<template>
  <div class="user-manage">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchForm.email"
        placeholder="输入邮箱搜索"
        class="search-input"
        clearable
      >
        <template #prefix>
          <el-icon><Message /></el-icon>
        </template>
      </el-input>
      <el-input
        v-model="searchForm.nickName"
        placeholder="输入昵称搜索"
        class="search-input"
        clearable
      >
        <template #prefix>
          <el-icon><User /></el-icon>
        </template>
      </el-input>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </div>

    <!-- 表格容器 -->
    <div class="table-container">
      <el-table 
        :data="userList" 
        style="width: 100%" 
        v-loading="loading"
        :height="tableHeight"
      >
        <el-table-column prop="nickName" label="用户名" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column label="存储空间" width="200">
          <template #default="{ row }">
            <div class="space-info">
              <span>{{ formatFileSize(row.userSpace) }}/{{ formatFileSize(row.totalSpace) }}</span>
              <el-button 
                type="primary" 
                link
                @click="handleUpdateSpace(row)"
              >
                扩容
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="joinTime" label="注册时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.joinTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" width="180">
          <template #default="{ row }">
            {{ formatDate(row.lastLoginTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button 
              :type="row.status === 1 ? 'danger' : 'primary'" 
              link
              @click="handleUpdateStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
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

    <!-- 修改空间大小的对话框 -->
    <el-dialog
      v-model="spaceDialogVisible"
      title="扩容存储空间"
      width="400px"
    >
      <el-form :model="spaceForm" label-width="100px">
        <el-form-item label="扩容空间">
          <el-input-number 
            v-model="spaceForm.space" 
            :min="1"
            :max="1024"
          />
          <span class="unit-text">GB</span>
        </el-form-item>
        <div class="space-tip">当前空间：{{ formatFileSize(currentUser?.totalSpace || 0) }}</div>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="spaceDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmUpdateSpace">
            确认
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Message, User } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { formatDate } from '@/utils/date'
import { formatFileSize } from '@/utils/file'
import { loadUserList, updateUserStatus, updateUserSpace } from '@/api/admin'

// 状态变量
const loading = ref(false)
const userList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchForm = ref({
  email: '',
  nickName: ''
})

// 空间修改相关
const spaceDialogVisible = ref(false)
const spaceForm = ref({
  userId: '',
  space: 1
})
const currentUser = ref(null)

// 计算表格高度
const tableHeight = computed(() => {
  return 'calc(100vh - 280px)'
})

// 获取用户列表
const fetchUserList = async () => {
  loading.value = true
  try {
    const res = await loadUserList({
      page: currentPage.value,
      pageSize: pageSize.value,
      email: searchForm.value.email,
      nickName: searchForm.value.nickName
    })
    if (res.code === 1) {
      userList.value = res.data.rows
      total.value = res.data.total
    }
  } catch (error) {
    console.error('获取用户列表失败:', error)
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchUserList()
}

// 重置搜索
const resetSearch = () => {
  searchForm.value = {
    email: '',
    nickName: ''
  }
  handleSearch()
}

// 更新用户状态
const handleUpdateStatus = async (user) => {
  const action = user.status === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确定要${action}该用户吗？`, 
      '提示', 
      {
        type: 'warning'
      }
    )
    
    loading.value = true
    const res = await updateUserStatus(user.userId, user.status === 1 ? 0 : 1)
    if (res.code === 1) {
      ElMessage.success(`${action}成功`)
      fetchUserList()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error(`${action}用户失败:`, error)
      ElMessage.error(`${action}用户失败`)
    }
  } finally {
    loading.value = false
  }
}

// 打开修改空间对话框
const handleUpdateSpace = (user) => {
  currentUser.value = user
  spaceForm.value = {
    userId: user.userId,
    space: 1  // 默认1MB
  }
  spaceDialogVisible.value = true
}

// 确认修改空间
const confirmUpdateSpace = async () => {
  try {
    const res = await updateUserSpace(
      spaceForm.value.userId, 
      spaceForm.value.space  // 直接传入输入的值，不需要转换
    )
    if (res.code === 1) {
      ElMessage.success('扩容成功')
      spaceDialogVisible.value = false
      fetchUserList()
    }
  } catch (error) {
    console.error('扩容失败:', error)
    ElMessage.error('扩容失败')
  }
}

// 分页处理
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchUserList()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchUserList()
}

// 初始化
onMounted(() => {
  fetchUserList()
})
</script>

<style scoped>
.user-manage {
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
  width: 200px;
}

.table-container {
  flex: 1;
  overflow: hidden;
}

.space-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination {
  flex-shrink: 0;
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.unit-text {
  margin-left: 8px;
  color: var(--el-text-color-regular);
}

:deep(.el-dialog__body) {
  padding-top: 20px;
}

.space-tip {
  margin: 8px 0 0 100px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}
</style> 
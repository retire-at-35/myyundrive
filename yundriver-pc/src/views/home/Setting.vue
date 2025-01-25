<template>
  <div class="page-container">
    <div class="settings-layout">
      <!-- 左侧导航 -->
      <div class="settings-sidebar">
        <el-menu
          :default-active="activeMenu"
          class="settings-menu"
          router
        >
          <el-menu-item index="/home/setting/users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/home/setting/files">
            <el-icon><Folder /></el-icon>
            <span>文件管理</span>
          </el-menu-item>
        </el-menu>
      </div>

      <!-- 右侧内容区 -->
      <div class="settings-content">
        <router-view v-slot="{ Component }">
          <keep-alive>
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { User, Folder } from '@element-plus/icons-vue'

const route = useRoute()
const activeMenu = computed(() => route.path)
</script>

<style scoped>
.page-container {
  height: 100%;
  padding: 24px;
  min-width: 900px;
}

.settings-layout {
  display: flex;
  height: 100%;
  background-color: var(--el-bg-color);
  border-radius: 8px;
  overflow: hidden;
  box-shadow: var(--el-box-shadow-light);
}

.settings-sidebar {
  width: 200px;
  background-color: var(--el-bg-color);
  border-right: 1px solid var(--el-border-color-light);
}

.settings-menu {
  border-right: none;
  height: 100%;
}

.settings-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background-color: var(--el-bg-color-page);
}

:deep(.el-menu-item) {
  height: 50px;
  line-height: 50px;
  padding: 0 20px !important;
}

:deep(.el-menu-item .el-icon) {
  margin-right: 12px;
  width: 16px;
}

:deep(.el-menu-item.is-active) {
  background-color: var(--el-color-primary-light-9);
}
</style> 
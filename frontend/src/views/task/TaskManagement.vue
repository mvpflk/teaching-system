<template>
  <div class="page-card">
    <el-tabs
      v-if="!isCreateOrEdit"
      :model-value="activeTab"
      class="task-tabs"
      @tab-click="onTabClick"
    >
      <el-tab-pane label="📋 任务列表" name="list" />
      <el-tab-pane label="📝 题库管理" name="question-bank" />
      <el-tab-pane label="📄 共享试卷" name="library" />
      <el-tab-pane label="📤 我的分享" name="share" />
      <el-tab-pane label="📑 任务模板" name="templates" />
      <el-tab-pane v-if="enabled('feature.review_enabled')" label="✅ 待审核" name="pending-review" />
    </el-tabs>
    <router-view />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSettingsStore } from '@/stores/settings'

const route = useRoute()
const router = useRouter()
const settingsStore = useSettingsStore()
const enabled = (key) => settingsStore.isEnabled(key)

const isCreateOrEdit = computed(() => {
  const name = route.name
  return name === 'TaskCreate' || name === 'TaskEdit'
})

const activeTab = computed(() => {
  const path = route.path.replace(/\/+$/, '')
  const parts = path.split('/')
  return parts[parts.length - 1]
})

const onTabClick = (tab) => {
  router.push(`/teacher/tasks/${tab.paneName}`)
}
</script>

<style scoped>
.task-tabs { margin-bottom: 0; }
.task-tabs :deep(.el-tabs__header) { margin-bottom: 0; }
@media (max-width: 768px) {
  .task-tabs :deep(.el-tabs__nav) { overflow-x: auto; }
}
</style>

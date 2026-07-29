<template>
  <div v-loading="loading" class="typing-settings">
    <div class="page-header">
      <h3>打字功能配置</h3>
      <span class="page-desc">控制哪些专业的学生可以参与打字练习和竞赛</span>
    </div>

    <el-card shadow="never" class="config-card">
      <template #header>
        <span>允许参赛的专业</span>
      </template>

      <div v-if="!loading" class="transfer-wrap">
        <el-transfer
          v-model="selectedMajors"
          :data="majorOptions"
          :titles="['未开放专业', '已开放专业']"
          :button-texts="['移除', '添加']"
          filterable
          filter-placeholder="搜索专业"
          :props="{ key: 'key', label: 'label' }"
          class="major-transfer"
        />
      </div>
      <el-empty v-else description="加载中..." />

      <div class="save-bar">
        <el-button type="primary" :loading="saving" @click="handleSave">
          保存配置
        </el-button>
        <span class="save-hint">保存后立即生效，已开放专业的学生刷新页面即可参与打字</span>
      </div>
    </el-card>

    <el-card shadow="never" class="info-card">
      <template #header><span>说明</span></template>
      <ul class="info-list">
        <li>配置仅影响<strong>学生端</strong>打字功能的访问权限</li>
        <li>教师和管理员不受此限制，始终可以管理文本和竞赛</li>
        <li>竞赛创建时如果指定了参赛专业，则以竞赛设置为准</li>
        <li>学生登录后，侧边栏"打字练习"入口根据此配置自动显示/隐藏</li>
      </ul>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMajors } from '@/api/settings'
import { getTypingMajors, updateTypingMajors } from '@/api/typing'

const loading = ref(true)
const saving = ref(false)
const selectedMajors = ref([])
const majorOptions = ref([])

onMounted(async () => {
  try {
    const [majorsRes, allowedRes] = await Promise.all([
      getMajors(),
      getTypingMajors()
    ])
    if (majorsRes.code === 200) {
      majorOptions.value = (majorsRes.data || []).map(m => ({
        key: m.id,
        label: m.majorName
      }))
    }
    if (allowedRes.code === 200) {
      selectedMajors.value = allowedRes.data || []
    }
  } catch {
    ElMessage.error('加载配置失败')
  }
  loading.value = false
})

async function handleSave() {
  saving.value = true
  try {
    await updateTypingMajors({ majorIds: selectedMajors.value })
    ElMessage.success('配置已保存，立即生效')
  } catch {
    ElMessage.error('保存失败')
  }
  saving.value = false
}
</script>

<style scoped>
.typing-settings { max-width: 800px; margin: 0 auto; display: flex; flex-direction: column; gap: 16px; }
.page-header { margin-bottom: 4px; }
.page-header h3 { margin: 0 0 4px; }
.page-desc { font-size: var(--fs-sm); color: var(--text-secondary); }
.config-card, .info-card { border-radius: var(--radius-md); }
.transfer-wrap { display: flex; justify-content: center; padding: 16px 0; }
.major-transfer { width: 100%; max-width: 600px; }
.major-transfer :deep(.el-transfer-panel) { width: 220px; }
.major-transfer :deep(.el-transfer__buttons) { padding: 0 16px; }
.save-bar { margin-top: 16px; padding-top: 16px; border-top: 1px solid var(--border-light); display: flex; align-items: center; gap: 12px; }
.save-hint { font-size: var(--fs-xs); color: var(--text-secondary); }
.info-list { margin: 0; padding-left: 20px; line-height: 2; color: var(--text-regular); font-size: var(--fs-sm); }

@media (max-width: 768px) {
  .typing-settings { padding: 8px; }
  .major-transfer { flex-direction: column; align-items: center; }
  .major-transfer :deep(.el-transfer-panel) { width: 100%; margin-bottom: 12px; }
  .major-transfer :deep(.el-transfer__buttons) { padding: 8px 0; flex-direction: row; }
}
</style>

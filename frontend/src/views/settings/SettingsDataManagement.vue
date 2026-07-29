<template>
  <div class="maintenance-section">
    <h4>系统状态</h4>
    <div v-loading="statusLoading" class="status-grid">
      <div v-for="(val, key) in status" :key="key" class="status-item">
        <span class="status-label">{{ statusLabels[key] || key }}</span>
        <span class="status-value">{{ val }}</span>
      </div>
    </div>
  </div>

  <el-divider />

  <div class="maintenance-section">
    <h4>数据备份</h4>
    <p class="desc">导出当前数据库的完整 SQL 备份文件，包含所有表结构和数据。</p>
    <el-button type="primary" :loading="exporting" @click="handleExport">
      <el-icon><Download /></el-icon>下载数据库备份
    </el-button>
  </div>

  <el-divider />

  <div class="maintenance-section">
    <h4>数据重置</h4>
    <p class="desc warning-text">以下操作不可逆，请谨慎使用！建议先备份再操作。</p>
    <el-space wrap>
      <el-button type="warning" @click="handleReset('exam_data')">重置考试数据</el-button>
      <el-button type="warning" @click="handleReset('homework_data')">重置作业数据</el-button>
      <el-button type="warning" @click="handleReset('bbs_data')">重置BBS数据</el-button>
      <el-button type="warning" @click="handleReset('credit_data')">重置积分数据</el-button>
      <el-button type="warning" @click="handleReset('notification_data')">重置通知</el-button>
    </el-space>
    <div style="margin-top:16px">
      <el-button type="danger" @click="handleReset('all')">重置全部业务数据</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import { getSettingsStatus, resetData } from '@/api/settings'

const exporting = ref(false)
const statusLoading = ref(false)
const status = ref({})
const statusLabels = {
  serverTime: '服务器时间', javaVersion: 'Java版本',
  users: '用户数', students: '学生数', teachers: '教师数', classes: '班级数',
  exams: '试卷数', examResults: '考试结果', examQuestions: '题目数',
  homeworkAssignments: '作业数', homeworkSubmissions: '提交记录',
  bbsPosts: '帖子数', bbsReplies: '回复数', notifications: '通知数'
}

defineExpose({ loadStatus })

const loadStatus = async () => {
  statusLoading.value = true
  try {
    const res = await getSettingsStatus()
    if (res.code === 200) status.value = res.data
  } catch { /* 状态加载失败不影响页面使用 */ } finally { statusLoading.value = false }
}

const handleExport = async () => {
  exporting.value = true
  try {
    const token = localStorage.getItem('token')
    const xhr = new XMLHttpRequest()
    xhr.open('GET', '/api/settings/actions/export', true)
    xhr.setRequestHeader('Authorization', `Bearer ${token}`)
    xhr.responseType = 'blob'
    xhr.onload = function() {
      if (xhr.status === 200) {
        const url = window.URL.createObjectURL(xhr.response)
        const a = document.createElement('a')
        a.href = url
        a.download = 'teaching_system_backup_' + new Date().toISOString().slice(0, 19).replace(/[:-]/g, '') + '.sql'
        a.click()
        window.URL.revokeObjectURL(url)
        ElMessage.success('备份已下载')
      } else { ElMessage.error('备份导出失败') }
    }
    xhr.onerror = () => ElMessage.error('备份导出失败')
    xhr.send()
  } finally { exporting.value = false }
}

const handleReset = async (type) => {
  const typeNames = {
    exam_data: '考试数据', homework_data: '作业数据', bbs_data: 'BBS数据',
    credit_data: '积分数据', notification_data: '通知', all: '全部业务数据'
  }
  try {
    await ElMessageBox.confirm(
      `确定重置${typeNames[type] || type}吗？` + (type === 'all' ? '\n\n⚠️ 这将清空所有非基础业务数据（试卷、作业、帖子、签到等），但保留用户、教师、学生、班级信息。' : ''),
      '确认重置', { type: 'warning', confirmButtonText: '确认重置', cancelButtonText: '取消' }
    )
    const res = await resetData({ type })
    if (res.code === 200) { ElMessage.success(res.message || '重置成功'); await loadStatus() }
  } catch { /* cancelled */ }
}

onMounted(() => { loadStatus() })
</script>

<style scoped lang="scss">
.maintenance-section { margin: 16px 0; }
.maintenance-section h4 { font-size: var(--fs-lg); margin: 0 0 12px; }
.desc { font-size: var(--fs-sm); color: var(--text-secondary); margin-bottom: 12px; }
.warning-text { color: var(--warning-color); }
.status-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px; margin-top: 8px;
}
.status-item {
  background: var(--bg-section); border-radius: var(--radius-md); padding: 12px 16px;
  display: flex; flex-direction: column; gap: 4px;
}
.status-label { font-size: var(--fs-xs); color: var(--text-secondary); }
.status-value { font-size: var(--fs-xl); font-weight: 600; color: var(--text-primary); }

@media (max-width: 768px) {
  .status-grid { grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 8px; }
  .status-value { font-size: var(--fs-lg); }
}
</style>

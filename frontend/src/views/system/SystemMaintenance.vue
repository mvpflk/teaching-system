<template>
  <div class="maintenance-page">
    <!-- A. 系统信息 -->
    <div class="section">
      <h4 class="section-title">📋 系统信息</h4>
      <el-descriptions
        v-loading="infoLoading"
        :column="3"
        border
        size="small"
      >
        <el-descriptions-item label="系统版本">{{ sysInfo.appVersion || '-' }}</el-descriptions-item>
        <el-descriptions-item label="服务器时间">{{ sysInfo.serverTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Java 版本">{{ sysInfo.javaVersion || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作系统">{{ sysInfo.osName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="CPU 核心">{{ sysInfo.availableProcessors || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最大内存">{{ sysInfo.maxMemoryMB || '-' }} MB</el-descriptions-item>
        <el-descriptions-item label="数据库">{{ sysInfo.dbUrl || '-' }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <!-- B. 数据备份 -->
    <div class="section">
      <h4 class="section-title">💾 数据备份与导出</h4>
      <el-alert
        title="在线备份功能需要服务器配置 mysqldump，当前环境暂不可用"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom:10px"
      />
      <p class="section-desc">请通过数据库管理工具（如 Navicat、MySQL Workbench）或命令行执行备份：<code style="display:block;margin-top:6px">mysqldump -u root -p teaching_system &gt; backup.sql</code></p>
      <el-button type="primary" disabled><el-icon><Download /></el-icon> 立即备份（暂不可用）</el-button>
    </div>

    <!-- C. 数据导入（高危） -->
    <div class="section danger-section">
      <h4 class="section-title">⚠️ 数据导入与恢复</h4>
      <p class="section-desc danger-text">上传 SQL 文件恢复数据，<strong>将覆盖现有数据</strong>。操作需密码确认。</p>
      <el-upload
        ref="uploadRef"
        :auto-upload="false"
        :limit="1"
        accept=".sql,.json"
        :on-change="onFileChange"
        drag
      >
        <el-icon size="32"><UploadFilled /></el-icon>
        <div>拖拽或点击上传备份文件</div>
        <template #tip><span style="font-size:var(--fs-xs);color:var(--danger-color);">仅 .sql / .json 格式</span></template>
      </el-upload>
      <div v-if="importFile" class="import-actions">
        <el-input
          v-model="importPassword"
          type="password"
          placeholder="管理员密码"
          show-password
          size="default"
          style="width:200px"
        />
        <el-button
          type="danger"
          :loading="importLoading"
          :disabled="!importPassword"
          @click="handleImport"
        >
          确认导入（覆盖现有数据）
        </el-button>
      </div>
    </div>

    <!-- D. 数据清空（极度危险） -->
    <div class="section critical-section">
      <h4 class="section-title">🚨 数据清空（极度危险）</h4>
      <p class="section-desc danger-text">清空指定范围的业务数据，操作不可逆。</p>

      <div class="clear-controls">
        <el-select v-model="clearScope" size="default" style="width:260px">
          <el-option value="test_data" label="清空测试数据（任务/BBS）" />
          <el-option value="biz_data" label="清空所有业务数据（考试/作业/BBS/积分/通知）" />
          <el-option value="audit_logs" label="仅清空审计日志" />
          <el-option value="all" label="清空全部（保留用户/班级基础信息）" />
        </el-select>
        <el-button type="danger" size="default" @click="showClearDialog">执行清空</el-button>
      </div>
    </div>

    <!-- 清空确认对话框 -->
    <el-dialog
      v-model="clearVisible"
      title="🚨 确认数据清空"
      width="480px"
      :close-on-click-modal="false"
      destroy-on-close
      append-to-body
    >
      <el-alert
        type="error"
        :closable="false"
        show-icon
        style="margin-bottom:16px"
      >
        <template #title>此操作不可逆！被清空的数据将永久丢失。</template>
      </el-alert>
      <p><strong>清空范围：</strong>{{ scopeLabel(clearScope) }}</p>
      <el-input
        v-model="clearPassword"
        type="password"
        placeholder="请输入管理员密码"
        show-password
        size="default"
        style="margin-bottom:12px"
      />
      <el-input v-model="clearConfirmText" placeholder="请输入&quot;我确认清空所有数据&quot;" size="default" />
      <template #footer>
        <el-button @click="clearVisible = false">取消</el-button>
        <el-button
          type="danger"
          :loading="clearLoading"
          :disabled="!clearPassword || clearConfirmText !== '我确认清空所有数据'"
          @click="handleClear"
        >
          确认清空
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMaintenanceInfo, importMaintenance, clearMaintenance } from '@/api/system'
import { downloadFile } from '@/utils/request'
// ── A. 系统信息 ──
const sysInfo = ref({})
const infoLoading = ref(false)
const loadSysInfo = async () => {
  infoLoading.value = true
  try {
    const r = await getMaintenanceInfo()
    if (r.code === 200) sysInfo.value = r.data || {}
    else ElMessage.error(r.message || '加载失败')
  } catch { ElMessage.error('获取系统信息失败') }
  finally { infoLoading.value = false }
}

// ── B. 备份 ──
const backupLoading = ref(false)
const handleBackup = async () => {
  backupLoading.value = true
  try {
    const filename = `backup_${new Date().toISOString().slice(0, 16).replace(/[:-]/g, '')}.sql`
    await downloadFile('/api/system-maintenance/actions/backup', filename)
    ElMessage.success('备份文件已下载')
  } catch { ElMessage.error('备份请求失败') }
  finally { backupLoading.value = false }
}

// ── C. 导入 ──
const uploadRef = ref(null)
const importFile = ref(null)
const importPassword = ref('')
const importLoading = ref(false)
const onFileChange = (file) => { importFile.value = file.raw }
const handleImport = async () => {
  if (!importFile.value || !importPassword.value) return
  try {
    await ElMessageBox.confirm('确定要导入此文件吗？当前数据将被覆盖。', '二次确认', {
      type: 'warning', confirmButtonText: '确认导入', cancelButtonText: '取消'
    })
  } catch { return }
  importLoading.value = true
  try {
    const form = new FormData()
    form.append('file', importFile.value)
    form.append('password', importPassword.value)
    const r = await importMaintenance(form)
    if (r.code === 200) ElMessage.success(r.data || '导入成功')
    else ElMessage.error(r.message || '导入失败')
  } catch { ElMessage.error('导入请求失败') }
  finally { importLoading.value = false; importPassword.value = '' }
}

// ── D. 清空 ──
const clearScope = ref('test_data')
const clearVisible = ref(false)
const clearPassword = ref('')
const clearConfirmText = ref('')
const clearLoading = ref(false)
const scopeLabel = (s) => ({
  test_data: '考试/作业/BBS', biz_data: '考试/作业/BBS/积分/通知',
  audit_logs: '审计日志', all: '全部业务数据（保留用户/班级）'
}[s] || s)

const showClearDialog = () => {
  clearPassword.value = ''; clearConfirmText.value = ''; clearVisible.value = true
}
const handleClear = async () => {
  clearLoading.value = true
  try {
    const r = await clearMaintenance({ scope: clearScope.value, password: clearPassword.value, confirmText: clearConfirmText.value })
    if (r.code === 200) {
      ElMessage.success(r.data?.cleared ? `已清空：${r.data.cleared}` : '已清空')
      clearVisible.value = false
    } else ElMessage.error(r.message || '清空失败')
  } catch { ElMessage.error('请求失败') }
  finally { clearLoading.value = false }
}

onMounted(loadSysInfo)
</script>

<style scoped lang="scss">
.maintenance-page { max-width: 900px; }

.section {
  background: var(--bg-card, var(--bg-card)); border-radius: var(--radius-lg, 12px);
  padding: 20px; margin-bottom: 20px; box-shadow: var(--shadow-sm);
  &.danger-section { border-left: 4px solid var(--warning-color); }
  &.critical-section { border-left: 4px solid var(--danger-color); background: var(--bg-danger-light); }
}

.section-title { font-size: var(--fs-lg); margin: 0 0 8px; color: var(--text-primary); }
.section-desc { font-size: var(--fs-sm); color: var(--text-secondary); margin-bottom: 12px; }
.danger-text { color: var(--danger-color); strong { color: var(--danger-color); } }

.import-actions { display: flex; gap: 10px; margin-top: 12px; align-items: center; }
.clear-controls { display: flex; gap: 12px; align-items: center; margin-top: 8px; }

@media (max-width: 768px) {
  .maintenance-page { max-width: 100%; }
  .import-actions { flex-direction: column; align-items: stretch; }
  .clear-controls { flex-direction: column; align-items: stretch; }
  :deep(.el-descriptions) { font-size: var(--fs-xs); }
}
</style>

<template>
  <div class="dynamic-params">
    <div class="page-header">
      <h3 class="page-title">动态参数管理</h3>
      <span class="page-subtitle">修改参数后即时生效，带 * 为关键参数需二次确认密码</span>
    </div>

    <el-tabs v-model="activeCategory" @tab-change="onTabChange">
      <el-tab-pane
        v-for="cat in categories"
        :key="cat"
        :name="cat"
        :label="catLabel(cat)"
      />
    </el-tabs>

    <div v-loading="loading" class="params-grid">
      <el-empty v-if="!loading && params.length === 0" description="该分类下暂无参数" :image-size="60" />

      <div v-for="p in params" :key="p.id" class="param-card">
        <div class="card-top">
          <div class="card-info">
            <span class="param-name">{{ p.description || p.key }}</span>
            <span class="param-key">{{ p.key }}</span>
            <span v-if="isCritical(p.key)" class="critical-badge">关键参数</span>
          </div>
        </div>

        <div class="card-control">
          <!-- number -->
          <el-input-number
            v-if="p.valueType === 'number'"
            v-model="editValues[p.key]"
            :min="parseMin(p.validationRule)"
            :max="parseMax(p.validationRule)"
            size="default"
            controls-position="right"
          />
          <!-- boolean -->
          <el-switch
            v-else-if="p.valueType === 'boolean'"
            v-model="editValues[p.key]"
            :active-value="true"
            :inactive-value="false"
            active-text="开"
            inactive-text="关"
            size="default"
          />
          <!-- select -->
          <el-select
            v-else-if="p.valueType === 'select'"
            v-model="editValues[p.key]"
            size="default"
            style="width:100%"
          >
            <el-option
              v-for="opt in parseOptions(p.options)"
              :key="opt"
              :value="opt"
              :label="opt"
            />
          </el-select>
          <!-- string -->
          <el-input v-else v-model="editValues[p.key]" size="default" />

          <span class="default-hint">默认: {{ p.defaultValue || '—' }}</span>
        </div>

        <div class="card-actions">
          <el-button
            size="small"
            type="primary"
            :loading="savingId === p.key"
            @click="saveParam(p)"
          >
            保存
          </el-button>
          <el-button size="small" @click="resetParam(p)">重置默认</el-button>
        </div>
      </div>
    </div>

    <!-- 密码二次确认对话框 -->
    <el-dialog
      v-model="pwdDialogVisible"
      title="二次确认"
      width="380px"
      :close-on-click-modal="false"
      append-to-body
    >
      <p style="margin:0 0 12px;font-size:var(--fs-md);color:var(--text-regular);">
        修改关键参数需要输入当前管理员密码
      </p>
      <el-input
        v-model="confirmPassword"
        type="password"
        placeholder="请输入管理员密码"
        show-password
        size="default"
        @keyup.enter="confirmSaveWithPwd"
      />
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdSaving" @click="confirmSaveWithPwd">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getParamCategories, getAdminParams, updateAdminParams } from '@/api/system'

const categories = ref([])
const activeCategory = ref('')
const params = ref([])
const loading = ref(false)
const savingId = ref(null)

// 编辑值缓存：{ paramId: newValue }
const editValues = ref({})

// 密码对话框
const pwdDialogVisible = ref(false)
const confirmPassword = ref('')
const pwdSaving = ref(false)
const pendingSave = ref(null)

// 关键参数前缀
const CRITICAL_PREFIXES = ['jwt.', 'security.', 'exam.cheat', 'system.enable', 'credit.leaderboard']

const isCritical = (key) => CRITICAL_PREFIXES.some(p => key.startsWith(p))

const catLabel = (cat) => {
  const map = { task: '📋 任务', exam: '📝 考试(旧)', homework: '📋 作业(旧)', credit: '⭐ 积分', bbs: '💬 论坛', security: '🔒 安全', system: '⚙️ 系统', feature: '🔧 功能开关' }
  return map[cat] || cat
}

const parseMin = (rule) => {
  if (!rule) return undefined
  const m = rule.match(/range:(\d+)~(\d+)/)
  return m ? Number(m[1]) : undefined
}
const parseMax = (rule) => {
  if (!rule) return undefined
  const m = rule.match(/range:(\d+)~(\d+)/)
  return m ? Number(m[2]) : undefined
}
const parseOptions = (opts) => {
  if (!opts) return []
  try { return JSON.parse(opts) } catch { return [] }
}

const loadCategories = async () => {
  try {
    const res = await getParamCategories()
    if (res.code === 200 && res.data.length > 0) {
      categories.value = res.data
      activeCategory.value = res.data[0]
      loadParams()
    }
  } catch (e) {
    ElMessage.error('加载分类失败')
  }
}

const onTabChange = () => loadParams()

const loadParams = async () => {
  loading.value = true
  try {
    const res = await getAdminParams({ category: activeCategory.value })
    if (res.code === 200) {
      params.value = res.data || []
      // 初始化编辑值
      const vals = {}
      params.value.forEach(p => {
        if (p.valueType === 'number') vals[p.key] = Number(p.value)
        else if (p.valueType === 'boolean') vals[p.key] = p.value === 'true'
        else vals[p.key] = p.value || ''
      })
      editValues.value = vals
    }
  } catch (e) {
    ElMessage.error('加载参数失败')
  } finally {
    loading.value = false
  }
}

const saveParam = async (p) => {
  const newVal = editValues.value[p.key]
  const payload = { [p.key]: String(newVal) }

  // 关键参数 → 先尝试无密码提交，如果返回403再弹框
  if (isCritical(p.key)) {
    pendingSave.value = payload
    pwdDialogVisible.value = true
    confirmPassword.value = ''
    return
  }

  await doSave(payload, p.key)
}

const doSave = async (payload, paramId) => {
  savingId.value = paramId
  try {
    const res = await updateAdminParams(payload)
    if (res.code === 200) {
      ElMessage.success(`已更新 ${res.data?.updated || 1} 项参数`)
      loadParams()
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (e) {
    ElMessage.error('请求失败')
  } finally {
    savingId.value = null
    pwdDialogVisible.value = false
  }
}

const confirmSaveWithPwd = async () => {
  if (!confirmPassword.value) { ElMessage.warning('请输入密码'); return }
  if (!pendingSave.value) return
  pendingSave.value.password = confirmPassword.value
  const key = Object.keys(pendingSave.value).find(k => k !== 'password')
  pwdSaving.value = true
  await doSave(pendingSave.value, key || '')
  pwdSaving.value = false
  pendingSave.value = null
}

const resetParam = (p) => {
  if (p.valueType === 'number') editValues.value[p.key] = Number(p.defaultValue)
  else if (p.valueType === 'boolean') editValues.value[p.key] = p.defaultValue === 'true'
  else editValues.value[p.key] = p.defaultValue || ''
  ElMessage.info('已重置为默认值，点击保存生效')
}

onMounted(loadCategories)
</script>

<style scoped lang="scss">
.dynamic-params {
  max-width: 900px;
}

.page-header {
  margin-bottom: 16px;
  .page-title { font-size: var(--fs-xl, 20px); margin: 0 0 4px; }
  .page-subtitle { font-size: var(--fs-xs, 12px); color: var(--text-secondary); }
}

.params-grid {
  display: flex; flex-direction: column; gap: 12px;
  margin-top: 16px; min-height: 200px;
}

.param-card {
  background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-md);
  padding: 16px; display: flex; align-items: center; gap: 16px; flex-wrap: wrap;
}

.card-info {
  min-width: 200px; flex: 1;
  .param-name { font-size: var(--fs-md); font-weight: 500; color: var(--text-primary); display: block; }
  .param-key { font-size: var(--fs-xs); color: var(--text-secondary); font-family: monospace; }
}

.critical-badge {
  font-size: 10px; color: var(--danger-color); background: var(--bg-danger-light);
  padding: 1px 6px; border-radius: var(--radius-xs); margin-left: 6px;
}

.card-control {
  display: flex; align-items: center; gap: 10px; flex: 2; min-width: 220px;
  .default-hint { font-size: var(--fs-xs); color: var(--text-placeholder, var(--text-secondary)); white-space: nowrap; }
}

.card-actions {
  display: flex; gap: 8px; flex-shrink: 0;
}

@media (max-width: 768px) {
  .dynamic-params { max-width: 100%; }
  .param-card { flex-direction: column; align-items: stretch; gap: 10px; }
  .card-control { width: 100%; flex-wrap: wrap; }
  .card-actions { justify-content: flex-end; }
  :deep(.el-tabs__item) { font-size: var(--fs-xs); padding: 0 10px !important; }
  :deep(.el-tabs__nav-wrap) { overflow-x: auto; }
}
</style>

<template>
  <div class="page-card">
    <div class="page-header">
      <h3 class="page-title">AI 模型配置</h3>
      <p class="page-desc">管理 DeepSeek / Agnes.ai 及自定义第三方大模型</p>
    </div>

    <!-- Provider 列表 -->
    <el-card shadow="never" style="max-width:680px; margin-bottom:16px">
      <template #header>
        <div style="display:flex; justify-content:space-between; align-items:center">
          <span>已配置的 Provider</span>
          <el-button type="primary" size="small" @click="showAddDialog = true">+ 接入新模型</el-button>
        </div>
      </template>

      <el-table
        v-loading="loadingProviders"
        :data="providers"
        size="small"
        stripe
      >
        <el-table-column prop="label" label="名称" width="120" />
        <el-table-column prop="key" label="标识" width="140">
          <template #default="{ row }">
            <el-tag :type="row.active === 'true' ? 'success' : 'info'" size="small" effect="plain">
              {{ row.key }}
              <span v-if="row.active === 'true'"> · 当前</span>
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="model"
          label="模型"
          min-width="120"
          show-overflow-tooltip
        />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button
              v-if="row.active !== 'true'"
              text
              size="small"
              type="primary"
              @click="activateProvider(row.key)"
            >
              切换
            </el-button>
            <el-button text size="small" @click="editProvider(row)">编辑</el-button>
            <el-popconfirm
              v-if="row.key.startsWith('custom.')"
              title="确定删除此 Provider？"
              @confirm="removeProvider(row.key)"
            >
              <template #reference>
                <el-button text size="small" type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑/新增对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingProviderKey ? '编辑 ' + editingProviderName : '接入新模型'"
      width="520px"
    >
      <el-form v-if="dialogVisible" :model="editForm" label-position="top">
        <el-form-item v-if="!editingProviderKey" label="Provider 标识" required>
          <el-input v-model="editForm.key" placeholder="如 qwen / glm / moonshot（英文小写，不含空格）" maxlength="20" />
          <div class="form-hint">配置后将存储为 custom.{标识}，如 custom.qwen</div>
        </el-form-item>

        <el-form-item label="显示名称">
          <el-input v-model="editForm.label" placeholder="如 通义千问 / 智谱GLM" maxlength="20" />
        </el-form-item>

        <el-form-item label="API Base URL" required>
          <el-input v-model="editForm.baseUrl" placeholder="https://api.example.com/v1（OpenAI兼容端点）" />
          <div class="form-hint">必须兼容 OpenAI 接口格式（/v1/chat/completions）</div>
        </el-form-item>

        <el-form-item label="API Key" required>
          <el-input
            v-model="editForm.apiKey"
            type="password"
            show-password
            placeholder="sk-..."
          />
          <div class="form-hint">密钥将加密存储，保存后回显脱敏值</div>
        </el-form-item>

        <el-form-item label="模型名称" required>
          <el-input v-model="editForm.model" placeholder="如 qwen-plus / glm-4-flash" />
        </el-form-item>

        <el-form-item label="超时（秒）">
          <el-input-number v-model="editForm.timeout" :min="5" :max="300" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存配置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { updateAiConfig, switchAiProvider, listAiProviders, deleteCustomProvider } from '@/api/settings'

const loadingProviders = ref(false)
const saving = ref(false)
const providers = ref([])

const dialogVisible = ref(false)
const showAddDialog = ref(false)
const editingProviderKey = ref('')
const editingProviderName = ref('')
const editForm = reactive({ key: '', label: '', apiKey: '', baseUrl: '', model: '', timeout: 60 })

const PRESETS = {
  deepseek: { label: 'DeepSeek', baseUrl: 'https://api.deepseek.com/v1', model: 'deepseek-chat' },
  agnes: { label: 'Agnes.ai', baseUrl: 'https://apihub.agnes-ai.com/v1', model: 'agnes-2.0-flash' }
}

const loadProviders = async () => {
  loadingProviders.value = true
  try {
    const res = await listAiProviders()
    if (res.code === 200) providers.value = res.data || []
  } catch { ElMessage.error('加载 Provider 列表失败') }
  finally { loadingProviders.value = false }
}

const activateProvider = async (key) => {
  try {
    const res = await switchAiProvider(key)
    if (res.code === 200) {
      ElMessage.success(res.message || '已切换')
      await loadProviders()
    } else {
      ElMessage.error(res.message || '切换失败')
    }
  } catch { ElMessage.error('切换失败') }
}

const editProvider = (row) => {
  editingProviderKey.value = row.key
  editingProviderName.value = row.label
  const preset = PRESETS[row.key]
  editForm.key = row.key
  editForm.label = row.label
  editForm.baseUrl = row.baseUrl || (preset ? preset.baseUrl : '')
  editForm.model = row.model || (preset ? preset.model : '')
  editForm.apiKey = ''
  editForm.timeout = 60
  dialogVisible.value = true
}

const saveEdit = async () => {
  if (!editForm.baseUrl || !editForm.model) {
    ElMessage.warning('请填写 Base URL 和模型名称')
    return
  }
  const provider = editingProviderKey.value || ('custom.' + editForm.key)
  saving.value = true
  try {
    const res = await updateAiConfig({
      baseUrl: editForm.baseUrl,
      model: editForm.model,
      apiKey: editForm.apiKey,
      timeout: String(editForm.timeout)
    }, provider)
    if (res.code === 200) {
      ElMessage.success('配置已保存')
      dialogVisible.value = false
      editingProviderKey.value = ''
      showAddDialog.value = false
      await loadProviders()
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch { ElMessage.error('保存失败') }
  finally { saving.value = false }
}

const removeProvider = async (key) => {
  const name = key.replace('custom.', '')
  try {
    const res = await deleteCustomProvider(name)
    if (res.code === 200) {
      ElMessage.success('已删除')
      await loadProviders()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch { ElMessage.error('删除失败') }
}

// 监听"接入新模型"按钮
const stopWatch = watch(showAddDialog, (val) => {
  if (val) {
    editingProviderKey.value = ''
    editingProviderName.value = ''
    editForm.key = ''
    editForm.label = ''
    editForm.baseUrl = ''
    editForm.model = ''
    editForm.apiKey = ''
    editForm.timeout = 60
    dialogVisible.value = true
    showAddDialog.value = false
  }
})

onMounted(loadProviders)
</script>

<style scoped>
.page-desc { font-size: var(--fs-sm); color: var(--text-secondary); margin: 4px 0 0; }
.form-hint { font-size: var(--fs-xs); color: var(--text-secondary); margin-top: 4px; }
</style>

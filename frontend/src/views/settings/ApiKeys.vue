<template>
  <div class="apikey-page">
    <!-- ── 顶部横幅：当前状态 ── -->
    <div class="apikey-banner">
      <div class="banner-icon">
        <el-icon :size="28"><Key /></el-icon>
      </div>
      <div class="banner-text">
        <h2 class="banner-title">我的 API Key</h2>
        <p class="banner-desc">
          配置个人 API Key 后，当免费调用额度用完时会自动使用你的 Key 继续服务。
          支持任意 OpenAI 兼容接口。
        </p>
      </div>
    </div>

    <!-- ── 主内容区 ── -->
    <div class="apikey-body">
      <!-- 状态摘要卡片 -->
      <div class="summary-cards">
        <div class="summary-card">
          <span class="summary-label">已配置 Key</span>
          <span class="summary-value">{{ keys.length }}</span>
        </div>
        <div class="summary-card">
          <span class="summary-label">当前状态</span>
          <span class="summary-value">
            <span v-if="activeKey" class="status-badge success">已就绪</span>
            <span v-else class="status-badge warn">未配置</span>
          </span>
        </div>
        <div class="summary-card">
          <span class="summary-label">免费额度</span>
          <span class="summary-value">
            <span v-if="activeKey" class="status-badge muted">由 Key 提供</span>
            <span v-else class="status-badge success">可用</span>
          </span>
        </div>
      </div>

      <!-- Key 管理 -->
      <div class="section-card">
        <div class="section-header">
          <div class="section-header-left">
            <h3 class="section-title">已添加的 Key</h3>
            <span class="section-count">{{ keys.length }} 个</span>
          </div>
          <el-button type="primary" @click="openAdd">
            <el-icon><Plus /></el-icon> 添加 Key
          </el-button>
        </div>

        <!-- 加载态 -->
        <div v-if="loading" class="state-box">
          <span class="tb-loading-spin" />
          <span>加载中…</span>
        </div>

        <!-- 空态 -->
        <div v-else-if="keys.length === 0" class="state-box">
          <div class="empty-icon-wrap">
            <el-icon :size="40"><Key /></el-icon>
          </div>
          <p class="empty-title">还没有添加 API Key</p>
          <p class="empty-desc">免费额度用完时系统会自动提醒你配置。点击上方按钮添加。</p>
        </div>

        <!-- Key 列表 -->
        <div v-else class="key-list">
          <div
            v-for="k in keys"
            :key="k.id"
            :class="['key-item', { 'key-item-active': k.isActive }]"
          >
            <div class="key-item-left">
              <div class="key-item-icon">
                <el-icon :size="20"><Key /></el-icon>
              </div>
              <div class="key-item-info">
                <div class="key-item-name">{{ k.label }}</div>
                <div class="key-item-meta">
                  <span class="key-item-url">{{ k.baseUrl }}</span>
                  <span class="key-divider">·</span>
                  <span class="key-item-model">{{ k.model }}</span>
                </div>
              </div>
            </div>
            <div class="key-item-right">
              <el-tag v-if="k.isActive" type="success" size="small" effect="light">使用中</el-tag>
              <el-tag v-else type="info" size="small" effect="plain">停用</el-tag>
              <el-dropdown trigger="click" @command="(cmd) => handleCmd(cmd, k)">
                <el-button class="key-more-btn" text size="small">
                  <el-icon><MoreFilled /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="toggle">
                      {{ k.isActive ? '停用' : '启用' }}
                    </el-dropdown-item>
                    <el-dropdown-item command="edit">编辑</el-dropdown-item>
                    <el-dropdown-item command="delete" divided style="color: var(--el-color-danger)">
                      删除
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ── 添加/编辑对话框 ── -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑 Key' : '添加 Key'"
      width="540px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form v-if="dialogVisible" :model="form" label-position="top" size="default">
        <el-form-item label="快捷选择">
          <el-select
            v-model="selectedPreset"
            placeholder="选择服务商自动填充…"
            clearable
            style="width: 100%"
            @change="applyPreset"
          >
            <el-option
              v-for="p in PRESETS"
              :key="p.label"
              :label="p.label"
              :value="p.label"
            >
              <div class="preset-option">
                <span>{{ p.label }}</span>
                <span class="preset-model">{{ p.model }}</span>
              </div>
            </el-option>
          </el-select>
          <div class="form-tip">选择后自动填入接口地址和模型名</div>
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="标识" required>
              <el-input v-model="form.label" placeholder="如：我的 DeepSeek" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模型名" required>
              <el-input v-model="form.model" placeholder="deepseek-chat" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="接口地址" required :error="urlError">
          <el-input
            v-model="form.baseUrl"
            placeholder="https://api.deepseek.com/v1"
            @input="urlError = ''"
          />
          <div class="form-tip">OpenAI 兼容的 API 端点，必须以 https:// 开头</div>
        </el-form-item>

        <el-form-item label="API Key" :required="!isEdit">
          <el-input
            v-model="form.apiKey"
            type="password"
            show-password
            placeholder="sk-..."
          />
          <div class="form-tip">{{ isEdit ? '留空则不修改' : '你的 API 密钥，加密存储' }}</div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">
          {{ isEdit ? '保存' : '添加' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Key, MoreFilled } from '@element-plus/icons-vue';
import {
  getApiKeys,
  createApiKey,
  updateApiKey,
  deleteApiKey,
  setActiveApiKey,
} from '@/api/apiKeys';

const loading = ref(false);
const saving = ref(false);
const keys = ref([]);

const dialogVisible = ref(false);
const isEdit = ref(false);
const editingId = ref(null);
const form = reactive({ label: '', baseUrl: '', apiKey: '', model: '' });
const urlError = ref('');
const selectedPreset = ref('');

const activeKey = computed(() => keys.value.find((k) => k.isActive));

const PRESETS = [
  { label: 'DeepSeek（深度求索）', baseUrl: 'https://api.deepseek.com/v1', model: 'deepseek-chat' },
  { label: '通义千问（阿里云）', baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1', model: 'qwen-plus' },
  { label: 'GLM（智谱AI）', baseUrl: 'https://open.bigmodel.cn/api/paas/v4', model: 'glm-4-plus' },
  { label: 'Kimi（月之暗面）', baseUrl: 'https://api.moonshot.cn/v1', model: 'moonshot-v1-8k' },
  { label: 'ERNIE（百度千帆）', baseUrl: 'https://aip.baidubce.com', model: 'ernie-4.0' },
  { label: '混元（腾讯云）', baseUrl: 'https://api.hunyuan.cloud.tencent.com/v1', model: 'hunyuan-pro' },
  { label: '豆包（火山引擎）', baseUrl: 'https://ark.cn-beijing.volces.com/api/v3', model: 'doubao-pro' },
  { label: 'OpenAI（ChatGPT）', baseUrl: 'https://api.openai.com/v1', model: 'gpt-4o-mini' },
];

function applyPreset(label) {
  const preset = PRESETS.find((p) => p.label === label);
  if (!preset) return;
  form.baseUrl = preset.baseUrl;
  form.model = preset.model;
  if (!form.label) form.label = preset.label.replace(/（.*）/, '').trim();
  urlError.value = '';
}

function validateUrl(url) {
  if (!url || !url.trim()) return '接口地址不能为空';
  try {
    const u = new URL(url);
    if (u.protocol !== 'https:') return '接口地址必须使用 HTTPS 协议';
    if (!u.hostname) return '接口地址格式不正确';
    return '';
  } catch {
    return '接口地址格式不正确';
  }
}

async function loadKeys() {
  loading.value = true;
  try {
    const res = await getApiKeys();
    if (res.code === 200) keys.value = res.data || [];
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

function resetForm() {
  form.label = '';
  form.baseUrl = '';
  form.apiKey = '';
  form.model = '';
  urlError.value = '';
  selectedPreset.value = '';
  editingId.value = null;
}

function openAdd() {
  isEdit.value = false;
  resetForm();
  dialogVisible.value = true;
}

function openEdit(row) {
  isEdit.value = true;
  editingId.value = row.id;
  form.label = row.label;
  form.baseUrl = row.baseUrl;
  form.apiKey = '';
  form.model = row.model;
  selectedPreset.value = '';
  urlError.value = '';
  dialogVisible.value = true;
}

async function save() {
  if (!form.label || !form.baseUrl || !form.model) {
    ElMessage.warning('请填写完整信息（标识、接口地址、模型名）');
    return;
  }
  if (!isEdit.value && !form.apiKey) {
    ElMessage.warning('请填写 API Key');
    return;
  }
  const err = validateUrl(form.baseUrl);
  if (err) { urlError.value = err; return }
  saving.value = true;
  try {
    const data = { label: form.label, baseUrl: form.baseUrl, apiKey: form.apiKey, model: form.model };
    const res = isEdit.value ? await updateApiKey(editingId.value, data) : await createApiKey(data);
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '已更新' : '已添加');
      dialogVisible.value = false;
      await loadKeys();
    } else ElMessage.error(res.message || '操作失败');
  } catch { ElMessage.error('操作失败') }
  finally { saving.value = false }
}

async function handleCmd(cmd, row) {
  if (cmd === 'edit') { openEdit(row); return }
  if (cmd === 'toggle') {
    try {
      const res = await setActiveApiKey(row.id, !row.isActive);
      if (res.code === 200) { await loadKeys() }
      else ElMessage.error(res.message || '操作失败');
    } catch { ElMessage.error('操作失败') }
    return
  }
  if (cmd === 'delete') {
    try {
      await ElMessageBox.confirm(`确定删除「${row.label}」？`, '删除确认', { type: 'warning' })
      const res = await deleteApiKey(row.id);
      if (res.code === 200) { ElMessage.success('已删除'); await loadKeys() }
      else ElMessage.error(res.message || '删除失败');
    } catch { /* 取消 */ }
  }
}

onMounted(loadKeys);
</script>

<style scoped>
/* ══════════════════════════════════════════════
   API Key 页面 — 独立完整设计
   ══════════════════════════════════════════════ */

.apikey-page {
  max-width: 840px;
  margin: 0 auto;
  padding: 32px 24px;
}

/* ── 顶部横幅 ── */
.apikey-banner {
  display: flex;
  gap: 20px;
  align-items: flex-start;
  margin-bottom: 28px;
}
.banner-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark, #3451db));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.banner-text { flex: 1; min-width: 0; }
.banner-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 6px;
}
.banner-desc {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin: 0;
}

/* ── 状态摘要卡片 ── */
.summary-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}
.summary-card {
  background: var(--bg-card);
  border: 0.5px solid var(--border-base);
  border-radius: 10px;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.summary-label {
  font-size: 13px;
  color: var(--text-secondary);
}
.summary-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1;
}
.status-badge {
  display: inline-block;
  font-size: 14px;
  font-weight: 600;
  padding: 3px 12px;
  border-radius: 12px;
}
.status-badge.success { background: color-mix(in srgb, var(--el-color-success) 12%, transparent); color: var(--el-color-success); }
.status-badge.warn { background: color-mix(in srgb, var(--el-color-warning) 12%, transparent); color: var(--el-color-warning); }
.status-badge.muted { background: var(--bg-secondary); color: var(--text-secondary); }

/* ── Section 卡片 ── */
.section-card {
  background: var(--bg-card);
  border: 0.5px solid var(--border-base);
  border-radius: 12px;
  overflow: hidden;
}
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 0.5px solid var(--border-base);
}
.section-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.section-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
  color: var(--text-primary);
}
.section-count {
  font-size: 12px;
  color: var(--text-secondary);
  background: var(--bg-secondary);
  padding: 2px 8px;
  border-radius: 8px;
}

/* ── 空态 / 加载态 ── */
.state-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 48px 24px;
  color: var(--text-secondary);
  font-size: 14px;
}
.tb-loading-spin {
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 2px solid var(--border-base);
  border-top-color: var(--primary-color);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.empty-icon-wrap {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  background: var(--bg-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-disabled);
}
.empty-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}
.empty-desc {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0;
  text-align: center;
  max-width: 320px;
  line-height: 1.6;
}

/* ── Key 列表 ── */
.key-list {
  padding: 8px 12px;
}
.key-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 12px;
  border-radius: 10px;
  border: 0.5px solid transparent;
  transition: all 0.15s;
  margin-bottom: 4px;
}
.key-item:hover {
  background: var(--bg-secondary);
}
.key-item-active {
  background: color-mix(in srgb, var(--primary-color) 4%, transparent);
  border-color: color-mix(in srgb, var(--primary-color) 20%, transparent);
}
.key-item-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  flex: 1;
}
.key-item-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: var(--bg-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary-color);
  flex-shrink: 0;
}
.key-item-active .key-item-icon {
  background: color-mix(in srgb, var(--primary-color) 12%, transparent);
}
.key-item-info {
  min-width: 0;
}
.key-item-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 3px;
}
.key-item-meta {
  font-size: 12px;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.key-item-url {
  max-width: 280px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.key-divider { color: var(--text-disabled); }
.key-item-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.key-more-btn {
  width: 28px;
  height: 28px;
  padding: 0;
}

/* ── 对话框样式 ── */
.preset-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.preset-model {
  font-size: 11px;
  color: var(--text-secondary);
}
.form-tip {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
  line-height: 1.5;
}

/* ── 移动端适配 ── */
@media (max-width: 768px) {
  .apikey-page { padding: 20px 16px; }
  .summary-cards { grid-template-columns: 1fr; }
  .banner-icon { width: 44px; height: 44px; }
  .banner-icon :deep(.el-icon) { font-size: 22px !important; }
  .banner-title { font-size: 18px; }
  .key-item { flex-wrap: wrap; gap: 8px; }
  .key-item-url { max-width: 160px; }
}
</style>

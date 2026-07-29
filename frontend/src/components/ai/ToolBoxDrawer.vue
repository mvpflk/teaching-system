<template>
  <el-drawer
    v-model="drawerVisible"
    title="工具箱"
    :size="isMobile ? '100%' : '420px'"
    destroy-on-close
    @open="handleOpen"
  >
    <div v-if="loading" class="tb-loading"><span class="tb-loading-spin" /> 加载工具列表...</div>
    <div v-else-if="error" class="tb-error">
      <p>加载失败：{{ error }}</p>
      <el-button size="small" @click="loadTools">重试</el-button>
    </div>
    <div v-else-if="tools.length === 0" class="tb-empty">暂无可用工具</div>
    <div v-else class="tb-list">
      <div
        v-for="tool in tools"
        :key="tool.name"
        class="tb-item"
        @click="handleToolClick(tool)"
      >
        <div class="tb-item-name">{{ tool.name }}</div>
        <div v-if="tool.description" class="tb-item-desc">{{ tool.description }}</div>
      </div>
    </div>
  </el-drawer>

  <el-dialog
    v-model="paramVisible"
    :title="'参数设置 — ' + (selected?.name || '')"
    width="400px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <el-form
      v-if="selected"
      ref="paramFormRef"
      :model="paramData"
      label-position="top"
      size="small"
    >
      <el-form-item
        v-for="(prop, key) in selected.parameters.properties"
        :key="key"
        :label="prop.description || key"
        :required="selected.parameters.required?.includes(key)"
      >
        <el-input
          v-if="prop.type === 'string'"
          v-model="paramData[key]"
          :placeholder="'请输入' + (prop.description || key)"
          clearable
        />
        <el-input-number
          v-else-if="prop.type === 'number' || prop.type === 'integer'"
          v-model="paramData[key]"
          :min="0"
          controls-position="right"
          style="width: 100%"
        />
        <el-switch v-else-if="prop.type === 'boolean'" v-model="paramData[key]" />
        <el-input
          v-else
          v-model="paramData[key]"
          :placeholder="'请输入' + (prop.description || key)"
          clearable
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="paramVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="confirmParam">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue';
import { listTools } from '@/api/agent';

const props = defineProps({ visible: Boolean });
const emit = defineEmits(['update:visible', 'trigger']);

const drawerVisible = computed({
  get: () => props.visible,
  set: (v) => emit('update:visible', v),
});

const isMobile = ref(window.innerWidth < 769);

const loading = ref(false);
const error = ref('');
const tools = ref([]);

const selected = ref(null);
const paramVisible = ref(false);
const paramData = ref({});
const paramFormRef = ref(null);
const submitting = ref(false);

async function loadTools() {
  loading.value = true;
  error.value = '';
  try {
    const res = await listTools();
    if (res.code !== 200) {
      error.value = res.message || '请求失败';
      tools.value = [];
      return;
    }
    tools.value = Array.isArray(res.data) ? res.data : [];
  } catch (e) {
    error.value = e?.message || '请求失败';
    tools.value = [];
  } finally {
    loading.value = false;
  }
}

function handleOpen() {
  loadTools();
}

function initParamForm(tool) {
  const form = {};
  if (tool.parameters?.properties) {
    for (const [key, prop] of Object.entries(tool.parameters.properties)) {
      if (prop.type === 'boolean') form[key] = false;
      else if (prop.type === 'number' || prop.type === 'integer') form[key] = undefined;
      else form[key] = '';
    }
  }
  return form;
}

function handleToolClick(tool) {
  const hasParams =
    tool.parameters?.properties && Object.keys(tool.parameters.properties).length > 0;
  if (!hasParams) {
    emit('trigger', `使用 ${tool.name} 工具——${tool.description || ''}`);
    drawerVisible.value = false;
    return;
  }
  selected.value = tool;
  paramData.value = initParamForm(tool);
  nextTick(() => {
    paramVisible.value = true;
  });
}

async function confirmParam() {
  submitting.value = true;
  try {
    const tool = selected.value;
    const pairs = Object.entries(paramData.value)
      .filter(([, v]) => v !== undefined && v !== null && v !== '')
      .map(([k, v]) => `${k}=${v}`);
    const prompt = `使用 ${tool.name} 工具——${tool.description || ''}，${pairs.join('，')}`;
    emit('trigger', prompt);
    paramVisible.value = false;
    drawerVisible.value = false;
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped>
.tb-loading,
.tb-error,
.tb-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  color: var(--text-secondary);
  font-size: 14px;
  gap: 12px;
}

.tb-loading-spin {
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 2px solid var(--border-base, var(--el-border-color));
  border-top-color: var(--primary-color);
  border-radius: 50%;
  animation: tb-spin 0.6s linear infinite;
}

@keyframes tb-spin {
  to {
    transform: rotate(360deg);
  }
}

.tb-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-bottom: 20px;
}

.tb-item {
  padding: 12px 16px;
  border: 0.5px solid var(--border-base, var(--el-border-color));
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
  background: var(--bg-page);
}
.tb-item:hover {
  border-color: var(--primary-color);
  background: var(--primary-light);
  transform: translateY(-1px);
}
.tb-item:active {
  transform: translateY(0);
}

.tb-item-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.tb-item-desc {
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.5;
}

@media (max-width: 768px) {
  .tb-item {
    padding: 10px 14px;
  }
}
</style>

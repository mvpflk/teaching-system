<!-- 模板加载/保存面板 — 受 feature.template_enabled 控制 -->
<template>
  <div v-if="visible" class="template-panel">
    <div class="tp-row">
      <el-select
        v-model="loadId"
        placeholder="从模板加载…"
        clearable
        size="small"
        class="desktop-width"
        style="width:240px"
        @change="onLoad"
      >
        <el-option
          v-for="t in options"
          :key="t.id"
          :value="t.id"
          :label="t.name"
        />
      </el-select>
      <el-button size="small" @click="onSave">💾 保存为模板</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getTemplateList, saveAsTemplate, getTemplate } from '@/api/taskTemplate'

const props = defineProps({
  visible: { type: Boolean, default: false },
  formTitle: { type: String, default: '' },
  getPayload: { type: Function, default: () => ({}) },
})
const emit = defineEmits(['load'])

const options = ref([])
const loadId = ref(null)

const fetch = async () => {
  if (!props.visible) return
  try { const r = await getTemplateList({ scope: 'PRIVATE' }); if (r.code === 200) options.value = r.data || [] } catch { /* */ }
}

const onLoad = async (id) => {
  if (!id) return
  try {
    const r = await getTemplate(id)
    if (r.code === 200 && r.data) { emit('load', r.data); ElMessage.success('已从模板加载：' + r.data.name) }
  } catch { ElMessage.error('加载模板失败') }
}

const onSave = async () => {
  const name = prompt('请输入模板名称：', props.formTitle)
  if (!name) return
  try {
    const r = await saveAsTemplate({ name, scope: 'PRIVATE', taskId: null, config: props.getPayload() })
    if (r.code === 200) ElMessage.success('模板已保存：' + name)
    else ElMessage.error(r.message || '保存失败')
  } catch { ElMessage.error('保存失败') }
}

watch(() => props.visible, (v) => { if (v) { loadId.value = null; fetch() } })
</script>

<style scoped>
.tp-row { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }

@media (max-width: 768px) {
  :deep(.el-form-item .el-input),
  :deep(.el-form-item .el-select) {
    width: 100%;
  }
}
</style>

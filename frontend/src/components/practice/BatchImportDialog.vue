<template>
  <el-dialog
    v-model="visible"
    title="批量导入实训方案"
    width="700px"
    @close="reset"
  >
    <el-alert type="info" :closable="false" style="margin-bottom: 16px;">
      <template #default>
        <div style="font-size: var(--fs-sm); line-height: 1.8;">
          <strong>格式说明：</strong>用 <code>## 方案标题</code> 分隔不同方案，标题后的文字自动成为方案描述。<br />
          可选：<code>### 步骤</code> 下面列出具体步骤，<code>### 评分维度</code> 下面列出评分项。<br />
          默认启用<strong>简易模式</strong>，学生1步提交即可。
        </div>
      </template>
    </el-alert>

    <el-input
      v-model="markdown"
      type="textarea"
      :rows="14"
      placeholder="## 网页制作&#10;完成一个响应式网页，包含导航栏和表单&#10;&#10;## 数据库设计&#10;设计学生信息管理系统的ER图&#10;&#10;### 步骤&#10;1. 分析需求&#10;2. 设计ER图&#10;3. 转换为关系模式&#10;&#10;### 评分维度&#10;- 实体完整性 30%&#10;- 关系正确性 30%&#10;- 规范化 20%&#10;- 文档规范 20%"
    />

    <div v-if="parsedPlans.length > 0" style="margin-top: 16px;">
      <el-divider>预览（共 {{ parsedPlans.length }} 个方案）</el-divider>
      <div v-for="(p, i) in parsedPlans" :key="i" style="margin-bottom: 12px; padding: 8px 12px; background: var(--bg-page); border-radius: 6px;">
        <strong>{{ i + 1 }}. {{ p.title }}</strong>
        <div v-if="p.description" style="font-size: var(--fs-sm); color: var(--text-secondary); margin-top: 4px;">{{ p.description }}</div>
        <div style="font-size: var(--fs-xs); color: var(--text-placeholder); margin-top: 2px;">
          {{ p.stepCount }} 个步骤 · 简易模式
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button
        type="primary"
        :loading="importing"
        :disabled="!markdown.trim()"
        @click="doImport"
      >
        确认导入 {{ parsedPlans.length > 0 ? `(${parsedPlans.length}个)` : '' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { batchImportPlans } from '@/api/practice'

const props = defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue', 'done'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const markdown = ref('')
const importing = ref(false)

const parsedPlans = computed(() => {
  const text = markdown.value.trim()
  if (!text) return []
  const parts = text.split(/\n(?=## )/)
  return parts.filter(p => p.trim()).map(p => {
    const lines = p.trim().split('\n')
    const title = lines[0].replace(/^#{2,}\s*/, '').trim()
    const descLines = []
    let stepCount = 1
    for (let i = 1; i < lines.length; i++) {
      if (lines[i].match(/^###\s+步骤/)) { stepCount = 0; continue }
      if (stepCount === 0 && lines[i].match(/^\d+[\.\)]\s*|^[-*]\s*/)) { stepCount++; continue }
      if (lines[i].startsWith('###')) break
      if (!lines[i].startsWith('#') && lines[i].trim()) descLines.push(lines[i].trim())
    }
    return { title, description: descLines.join(' ').substring(0, 100), stepCount: Math.max(stepCount, 1) }
  })
})

async function doImport() {
  if (!markdown.value.trim()) return
  importing.value = true
  try {
    const res = await batchImportPlans(markdown.value)
    if (res.code === 200) {
      ElMessage.success(`成功导入 ${res.data?.imported || 0} 个方案`)
      visible.value = false
      emit('done')
    } else {
      ElMessage.error(res.message || '导入失败')
    }
  } catch { ElMessage.error('导入失败') }
  finally { importing.value = false }
}

function reset() { markdown.value = '' }
</script>

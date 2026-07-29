<template>
  <div class="step-sql">
    <div class="step-desc" v-if="step.description" v-html="renderedDesc"></div>

    <!-- 表结构展示 -->
    <div v-if="step.config?.schema" class="schema-panel">
      <h5>📊 数据表结构</h5>
      <pre class="schema-ddl">{{ step.config.schema }}</pre>
    </div>

    <!-- 题目描述 -->
    <div v-if="step.config?.question" class="question-panel">
      <h5>📝 题目要求</h5>
      <p>{{ step.config.question }}</p>
    </div>

    <!-- SQL 编辑器 -->
    <div class="editor-panel">
      <label>SQL 语句</label>
      <textarea
        v-model="sqlCode"
        class="sql-editor"
        placeholder="输入你的 SQL 语句…&#10;例如: SELECT * FROM students WHERE age > 18"
        rows="6"
        spellcheck="false"
      ></textarea>
      <div class="editor-actions">
        <el-button type="primary" size="small" :loading="running" @click="runSql">
          <el-icon><CaretRight /></el-icon> 运行并提交
        </el-button>
        <el-button size="small" @click="sqlCode = ''">清空</el-button>
      </div>
    </div>

    <!-- 执行结果 -->
    <div v-if="sqlResult" class="result-panel">
      <el-alert
        :title="sqlResult.passed ? `✅ 正确！(${sqlResult.score || 0}分)` : `❌ 不正确`"
        :type="sqlResult.passed ? 'success' : 'error'"
        :closable="false" show-icon
      />
      <div v-if="sqlResult.error" class="error-msg">{{ sqlResult.error }}</div>
      <div v-if="sqlResult.detail" class="detail-msg">{{ sqlResult.detail }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { CaretRight } from '@element-plus/icons-vue'
import { renderMarkdown } from '@/utils/markdown'
import request from '@/utils/request'

const props = defineProps({
  step: { type: Object, default: () => ({}) },
  stepIndex: { type: Number, default: 0 },
  taskId: { type: Number, default: 0 },
  modelValue: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['update:modelValue', 'saved'])

const sqlCode = ref(props.modelValue?.sql || '')
const running = ref(false)
const sqlResult = ref(props.modelValue?.sqlResult || null)

async function runSql() {
  if (!sqlCode.value.trim()) return ElMessage.warning('请输入 SQL 语句')
  running.value = true
  try {
    const res = await request({
      url: `/training/eval/sql/${props.taskId}`,
      method: 'post',
      data: {
        sql: sqlCode.value,
        expectedSql: props.step.config?.expectedSql || '',
        schema: props.step.config?.schema || ''
      }
    })
    if (res.code === 200) {
      sqlResult.value = res.data
      emit('update:modelValue', { ...props.modelValue, sql: sqlCode.value, sqlResult: res.data })
      emit('saved')
    } else {
      ElMessage.error(res.message || '执行失败')
    }
  } catch (e) {
    ElMessage.error('请求失败: ' + (e.message || '网络错误'))
  }
  running.value = false
}

const renderedDesc = computed(() => renderMarkdown(props.step.description || ''))
</script>

<style scoped>
.step-sql { display: flex; flex-direction: column; gap: 12px; }
.schema-panel, .question-panel { padding: 12px; background: var(--bg-page); border-radius: var(--radius-sm); }
.schema-panel h5, .question-panel h5 { margin: 0 0 8px; }
.schema-ddl { font-family: 'JetBrains Mono', 'Fira Code', monospace; font-size: var(--fs-sm); white-space: pre-wrap; color: var(--el-color-info); }
.question-panel p { margin: 0; font-size: var(--fs-sm); }
.editor-panel label { display: block; font-weight: 600; margin-bottom: 4px; }
.sql-editor { width: 100%; font-family: 'JetBrains Mono', 'Fira Code', monospace; font-size: var(--fs-sm); padding: 10px; border: 1px solid var(--el-border-color); border-radius: var(--radius-sm); background: #1e1e2e; color: #cdd6f4; resize: vertical; }
.sql-editor::placeholder { color: #6c7086; }
.editor-actions { margin-top: 8px; display: flex; gap: 8px; }
.result-panel { padding: 12px; background: var(--bg-page); border-radius: var(--radius-sm); }
.error-msg { color: var(--el-color-danger); font-size: var(--fs-sm); margin-top: 4px; }
.detail-msg { color: var(--el-color-info); font-size: var(--fs-sm); margin-top: 4px; }
</style>

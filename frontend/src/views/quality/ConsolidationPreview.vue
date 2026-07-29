<template>
  <div v-loading="loading" class="cp-page">
    <div class="cp-header">
      <el-button text @click="$router.back()"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
      <h2>巩固材料</h2>
    </div>
    <div v-if="error" class="cp-error">{{ error }}</div>
    <el-empty v-else-if="!loading && !content" description="巩固材料不存在或已被删除" />
    <div v-else-if="content" class="cp-content">
      <div class="cp-toolbar">
        <el-button type="primary" @click="publish"><el-icon><Upload /></el-icon> 发布给学生</el-button>
        <el-button @click="exportPdf"><el-icon><Download /></el-icon> 导出PDF</el-button>
        <el-button disabled title="功能开发中"><el-icon><Refresh /></el-icon> 重新生成</el-button>
      </div>
      <div ref="exportArea" class="cp-markdown" v-html="rendered"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Upload, Download, Refresh } from '@element-plus/icons-vue'
import { getAiOutput, publishAiOutput } from '@/api/aiOutput'
import { renderMarkdown } from '@/utils/markdown'
import { exportPdf as doExportPdf } from '@/utils/exportPdf'

const route = useRoute()
const loading = ref(true)
const error = ref('')
const content = ref('')
const outputId = ref(null)
const rendered = computed(() => content.value ? renderMarkdown(content.value) : '')
const exportArea = ref(null)

async function load() {
  try {
    const res = await getAiOutput(route.params.id)
    if (res.code === 200 && res.data) {
      content.value = res.data.content || ''
      outputId.value = res.data.id
    }
  } catch { error.value = '加载失败' }
  finally { loading.value = false }
}

async function publish() {
  if (!outputId.value) return
  try { await publishAiOutput(outputId.value); ElMessage.success('已发布') }
  catch { ElMessage.error('发布失败') }
}
async function exportPdf() {
  if (!exportArea.value) return
  try { await doExportPdf(exportArea.value, `巩固材料_${new Date().toISOString().slice(0,10)}`) }
  catch { ElMessage.error('导出失败') }
}

onMounted(load)
</script>

<style scoped>
.cp-page { max-width: 800px; margin: 0 auto; padding: 24px; }
.cp-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.cp-header h2 { margin: 0; font-size: var(--fs-lg); }
.cp-toolbar { display: flex; gap: 8px; margin-bottom: 20px; }
.cp-markdown { padding: 24px; background: #fff; border: 1px solid #ebeef5; border-radius: 8px; font-size: var(--fs-md); line-height: 1.9; }
.cp-error { color: var(--danger-color); padding: 20px; text-align: center; }

@media (max-width: 768px) {
  .cp-page { padding: 12px; }
  .cp-toolbar { flex-wrap: wrap; }
  .cp-markdown { padding: 16px; font-size: var(--fs-sm); }
}
</style>

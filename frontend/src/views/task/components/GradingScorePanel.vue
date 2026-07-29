<template>
  <div>
    <!-- 互评质量对话框 -->
    <el-dialog
      :model-value="visible"
      title="互评质量分析"
      width="700px"
      destroy-on-close
      append-to-body
      @update:model-value="$emit('update:visible', $event)"
    >
      <div v-loading="qualityLoading" class="quality-body">
        <el-empty v-if="!qualityData.hasData && !qualityLoading" description="暂无互评数据" :image-size="60" />
        <template v-else-if="qualityData.hasData">
          <div class="quality-summary">班级均分 {{ qualityData.classAvg }} · {{ qualityData.totalReviewers }} 位评分人</div>
          <el-table :data="qualityData.reviewers" size="small" stripe max-height="400">
            <el-table-column prop="reviewerName" label="评分人" width="110" />
            <el-table-column label="评分数" width="70" align="center">
              <template #default="{ row }">{{ row.reviewCount }}</template>
            </el-table-column>
            <el-table-column label="均分" width="70" align="center">
              <template #default="{ row }">{{ row.avgScore }}</template>
            </el-table-column>
            <el-table-column label="偏差" width="80" align="center">
              <template #default="{ row }">
                <span :style="{ color: Math.abs(row.deviation) > 15 ? 'var(--el-color-danger)' : 'var(--text-secondary)' }">
                  {{ row.deviation > 0 ? '+' : '' }}{{ row.deviation }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="满分/零分" width="90" align="center">
              <template #default="{ row }">{{ row.fullMarkCount }}/{{ row.zeroCount }}</template>
            </el-table-column>
            <el-table-column label="标记" min-width="160">
              <template #default="{ row }">
                <el-tag v-for="f in row.flags" :key="f" size="small" type="warning" style="margin:1px 2px">{{ f }}</el-tag>
                <span v-if="!row.flags?.length" style="color:var(--el-color-success)">正常</span>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </div>
    </el-dialog>

    <!-- 批量更正成绩对话框 -->
    <el-dialog
      v-model="showBatchRegrade"
      title="批量更正成绩"
      width="520px"
      destroy-on-close
      append-to-body
    >
      <div class="batch-regrade-body">
        <el-alert title="此功能用于修正因题目数据变更（如选项数修正）导致的误判" type="info" :closable="false" show-icon style="margin-bottom:16px" />
        <el-form label-position="top">
          <el-form-item label="选择方式">
            <el-radio-group v-model="batchMode">
              <el-radio value="question">按受影响题目ID重评</el-radio>
              <el-radio value="all">重评本任务所有已提交学生</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="batchMode === 'question'" label="受影响的题目ID">
            <el-input v-model="batchQuestionId" placeholder="输入question_bank表的题目ID" clearable />
            <div class="form-hint">系统会自动找到所有回答过此题的学生并重新判分</div>
          </el-form-item>
          <el-form-item v-if="batchMode === 'all'" label="影响范围">
            <span class="form-hint">将对本任务所有已提交/已评分学生的客观题逐一重新判分，仅更正结果发生变化的记录</span>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="showBatchRegrade = false">取消</el-button>
        <el-button type="primary" :loading="batchRegrading" @click="doBatchRegrade">开始重评</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { batchRegradeSubmissions, regradeSubmissionResult } from '@/api/task'
import { getPeerReviewQuality } from '@/api/peerReview'

const props = defineProps({
  taskId: { type: [String, Number], required: true },
  visible: { type: Boolean, default: false },
})

const emit = defineEmits(['update:visible', 'done'])

const showBatchRegrade = ref(false)
const batchMode = ref('question')
const batchQuestionId = ref('')
const batchRegrading = ref(false)

const qualityLoading = ref(false)
const qualityData = ref({})

async function doBatchRegrade() {
  if (batchMode.value === 'question' && !batchQuestionId.value.trim()) {
    ElMessage.warning('请输入受影响的题目ID')
    return
  }
  try {
    await ElMessageBox.confirm(
      batchMode.value === 'question'
        ? `将对所有回答过题目${batchQuestionId.value}的学生进行重新评分，仅更正结果发生变化的记录。确定继续？`
        : `将对本任务所有已提交学生的客观题逐一重新评分。确定继续？`,
      '确认批量重评',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }

  batchRegrading.value = true
  try {
    const body = batchMode.value === 'question'
      ? { questionId: Number(batchQuestionId.value.trim()) }
      : { taskId: Number(props.taskId) }
    const res = await batchRegradeSubmissions(body)
    if (res.code === 200) {
      const d = res.data
      ElMessage.success(`批量重评完成: 共处理${d.total}份提交, ${d.affected}份有变更`)
      showBatchRegrade.value = false
      emit('done')
    } else {
      ElMessage.error(res.message || '批量重评失败')
    }
  } catch { ElMessage.error('批量重评失败') }
  batchRegrading.value = false
}

watch(() => props.visible, async (v) => {
  if (v) {
    qualityLoading.value = true
    try {
      const r = await getPeerReviewQuality(props.taskId)
      if (r.code === 200) qualityData.value = r.data || {}
    } catch { /* */ }
    finally { qualityLoading.value = false }
  }
})

defineExpose({ showBatchRegrade })
</script>

<style scoped>
.quality-summary { font-size: var(--fs-sm); color: var(--text-secondary); margin-bottom: 12px; }
.batch-regrade-body { padding: 0 4px; }
.form-hint { font-size: var(--fs-xs); color: var(--text-secondary); margin-top: 4px; }
</style>

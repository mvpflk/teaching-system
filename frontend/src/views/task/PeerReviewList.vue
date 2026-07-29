<template>
  <div class="page-card">
    <div class="page-header"><h3 class="page-title">互评任务</h3></div>
    <el-empty v-if="reviews.length === 0 && !loading" description="暂无待评任务" />
    <div v-loading="loading" class="pr-list">
      <div
        v-for="r in paginated"
        :key="r.reviewId"
        class="pr-card"
        @click="openGrade(r)"
      >
        <div class="pr-card-header">
          <el-tag size="small" type="warning">待评</el-tag>
          <span class="pr-task-title">{{ r.taskTitle }}</span>
        </div>
        <div class="pr-content">{{ truncate(r.content, 120) }}</div>
        <div class="pr-card-footer">
          <el-button type="primary" size="small">开始评分</el-button>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="gradeVisible"
      title="互评打分"
      width="500px"
      :close-on-click-modal="false"
      destroy-on-close
      append-to-body
    >
      <div v-if="current.dimensions?.length">
        <div v-for="d in current.dimensions" :key="d.id" class="pr-dim">
          <div class="pr-dim-label">{{ d.name }} <span class="pr-dim-weight">(权重 {{ (d.weight * 100).toFixed(0) }}%)</span></div>
          <div class="pr-dim-slider">
            <span class="pr-score-label">{{ scores[d.id] || 0 }}分</span>
            <input
              v-model.number="scores[d.id]"
              type="range"
              :min="0"
              :max="d.maxScore || 100"
              class="pr-range"
            />
          </div>
        </div>
      </div>
      <div v-else class="pr-direct">
        <label>评分 (0-100)</label>
        <el-input-number
          v-model="scores.totalScore"
          :min="0"
          :max="100"
          style="width:100%"
        />
      </div>
      <el-input
        v-model="comment"
        type="textarea"
        :rows="2"
        placeholder="评语（可选）"
        class="pr-comment"
      />
      <template #footer>
        <el-button @click="gradeVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitGrade">提交评分</el-button>
      </template>
    </el-dialog>

    <div v-if="paginated.length < reviews.length || reviews.length > pageSize" class="pagination-wrap">
      <el-pagination
        v-model:current-page="page"
        layout="total,prev,next"
        :total="reviews.length"
        :page-size="pageSize"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getPendingReviews, submitReview } from '@/api/peerReview'

const loading = ref(false)
const reviews = ref([])
const gradeVisible = ref(false)
const current = ref({})
const scores = reactive({})
const comment = ref('')
const submitting = ref(false)
const page = ref(1)
const pageSize = 10

const paginated = computed(() => {
  const start = (page.value - 1) * pageSize
  return reviews.value.slice(start, start + pageSize)
})

const load = async () => {
  loading.value = true
  try {
    const res = await getPendingReviews()
    if (res.code === 200) reviews.value = res.data || []
  } catch { ElMessage.error('加载待评任务失败') } finally { loading.value = false }
}

const openGrade = (r) => {
  current.value = r
  Object.keys(scores).forEach(k => delete scores[k])
  comment.value = ''
  if (r.dimensions?.length) {
    r.dimensions.forEach(d => { scores[d.id] = 0 })
  } else {
    scores.totalScore = 0
  }
  gradeVisible.value = true
}

const submitGrade = async () => {
  if (submitting.value) return
  submitting.value = true
  try {
    const data = { totalScore: scores.totalScore, dimensions: { ...scores }, comment: comment.value }
    delete data.dimensions.totalScore
    const res = await submitReview(current.value.reviewId, data)
    if (res.code === 200) { ElMessage.success('评分已提交'); gradeVisible.value = false; load() }
  } catch { ElMessage.error('提交评分失败') } finally { submitting.value = false }
}

const truncate = (text, len) => text && text.length > len ? text.slice(0, len) + '...' : text || '（无内容）'

onMounted(() => { load() })
</script>

<style scoped>
.pr-list { display: flex; flex-direction: column; gap: 12px; }
.pr-card {
  padding: 16px; border: 1px solid var(--border-light); border-radius: var(--radius-md);
  cursor: pointer; transition: all var(--transition-fast);
}
.pr-card:hover { border-color: var(--primary-color); box-shadow: var(--shadow-sm); }
.pr-card-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.pr-task-title { font-weight: 600; color: var(--text-primary); }
.pr-content { font-size: var(--fs-sm); color: var(--text-secondary); margin-bottom: 10px; }
.pr-card-footer { text-align: right; }
.pr-dim { margin-bottom: 16px; }
.pr-dim-label { font-size: var(--fs-sm); font-weight: 500; color: var(--text-primary); }
.pr-dim-weight { color: var(--text-secondary); font-weight: 400; }
.pr-dim-slider { display: flex; align-items: center; gap: 10px; margin-top: 4px; }
.pr-score-label { font-size: var(--fs-sm); font-weight: 600; color: var(--primary-color); min-width: 36px; }
.pr-range { flex: 1; height: 6px; -webkit-appearance: none; appearance: none; background: var(--border-light); border-radius: 3px; outline: none; }
.pr-range::-webkit-slider-thumb { -webkit-appearance: none; appearance: none; width: 24px; height: 24px; border-radius: 50%; background: var(--primary-color); cursor: pointer; }
.pr-comment { margin-top: 12px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: center; }

@media (max-width: 768px) {
  .pr-card { padding: 12px; }
  .pr-task-title { font-size: var(--fs-sm); }
  .pr-content { font-size: var(--fs-xs); }
}
</style>

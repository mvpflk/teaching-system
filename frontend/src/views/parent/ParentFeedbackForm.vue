<template>
  <div class="feedback-form">
    <div class="page-header">
      <el-button text @click="$router.back()"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
      <h2>{{ form?.title || '反馈问卷' }}</h2>
    </div>

    <div v-if="loading" class="sk-list">
      <div
        v-for="i in 4"
        :key="i"
        class="sk-line w-60"
        style="margin-bottom:12px;height:20px"
      />
    </div>

    <el-card v-else-if="form" class="form-card">
      <div class="form-meta">
        <el-tag size="small">{{ form.className }}</el-tag>
        <span class="form-period">周期: {{ form.period }}</span>
      </div>

      <div class="rating-group">
        <div class="rating-item">
          <label>满意度</label>
          <el-rate
            v-model="resp.satisfaction"
            :max="5"
            show-score
            score-template="{value}分"
          />
        </div>
        <div class="rating-item">
          <label>教学质量</label>
          <el-rate
            v-model="resp.teachingQuality"
            :max="5"
            show-score
            score-template="{value}分"
          />
        </div>
        <div class="rating-item">
          <label>作业量</label>
          <el-rate
            v-model="resp.homeworkLoad"
            :max="5"
            show-score
            score-template="{value}分"
          />
        </div>
        <div class="rating-item">
          <label>家校沟通</label>
          <el-rate
            v-model="resp.communication"
            :max="5"
            show-score
            score-template="{value}分"
          />
        </div>
      </div>

      <div class="comment-section">
        <label>建议或意见</label>
        <el-input
          v-model="resp.comment"
          type="textarea"
          :rows="4"
          placeholder="选填，分享您的想法..."
          maxlength="500"
          show-word-limit
        />
      </div>

      <el-button
        type="primary"
        size="large"
        :loading="submitting"
        class="submit-btn"
        @click="doSubmit"
      >
        提交反馈
      </el-button>
    </el-card>

    <el-empty v-else-if="!loading" description="问卷不存在或已过期" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPendingFeedbackForms, submitFeedbackResponse } from '@/api/parentFeedback'

const route = useRoute()
const form = ref(null)
const loading = ref(true)
const submitting = ref(false)
const resp = ref({ satisfaction: 0, teachingQuality: 0, homeworkLoad: 0, communication: 0, comment: '' })

onMounted(async () => {
  const formId = Number(route.params.formId)
  if (!formId) { loading.value = false; return }
  try {
    const res = await getPendingFeedbackForms()
    if (res.code === 200) {
      const found = (res.data || []).find(f => f.formId === formId)
      if (found) form.value = found
    }
  } finally { loading.value = false }
})

async function doSubmit() {
  if (!resp.value.satisfaction || !resp.value.teachingQuality || !resp.value.homeworkLoad || !resp.value.communication) {
    ElMessage.warning('请完成所有评分')
    return
  }
  submitting.value = true
  try {
    const res = await submitFeedbackResponse({
      formId: form.value.formId,
      satisfaction: resp.value.satisfaction,
      teachingQuality: resp.value.teachingQuality,
      homeworkLoad: resp.value.homeworkLoad,
      communication: resp.value.communication,
      comment: resp.value.comment
    })
    if (res.code === 200) {
      ElMessage.success('感谢您的反馈！')
      setTimeout(() => window.history.back(), 1500)
    } else {
      ElMessage.error(res.message || '提交失败')
    }
  } catch (e) { ElMessage.error(e.message || '提交失败') }
  finally { submitting.value = false }
}
</script>

<style scoped lang="scss">
.feedback-form { max-width: 640px; margin: 0 auto; padding: 8px; }
.page-header { margin-bottom: 24px; display: flex; align-items: center; gap: 16px; h2 { margin: 0; font-size: 22px; } }
.form-card { padding: 24px; }
.form-meta { margin-bottom: 24px; display: flex; align-items: center; gap: 12px; .form-period { font-size: var(--fs-md); color: var(--text-secondary); } }
.rating-group { margin-bottom: 24px; }
.rating-item { margin-bottom: 20px; label { display: block; font-size: var(--fs-md); font-weight: 500; margin-bottom: 8px; color: var(--text-primary); } }
.comment-section { margin-bottom: 24px; label { display: block; font-size: var(--fs-md); font-weight: 500; margin-bottom: 8px; } }
.submit-btn { width: 100%; }
.sk-line { height: 14px; background: var(--bg-secondary); border-radius: var(--radius-xs); animation: sk-shimmer 1.6s infinite; }
@keyframes sk-shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
</style>

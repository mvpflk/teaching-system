<template>
  <van-dialog
    :show="visible"
    title="学习资源"
    :show-confirm-button="false"
    close-on-click-overlay
    @update:show="$emit('update:visible', $event)"
  >
    <div class="lr-popup">
      <div class="lr-node-name">{{ currentNodeName }}</div>
      <div v-if="currentResources?.videoUrl" class="lr-section">
        <div class="lr-section-title">教学视频</div>
        <a class="lr-video-link" :href="currentResources.videoUrl" target="_blank">点击打开视频（新窗口打开）</a>
      </div>
      <div v-if="exampleQuestions.length" class="lr-section">
        <div class="lr-section-title">例题 ({{ exampleQuestions.length }})</div>
        <div v-for="q in exampleQuestions" :key="'e'+q.id" class="lr-q-item" @click="$emit('open-question', q)">
          <span class="lr-q-type">{{ typeLabel(q.questionType) }}</span>
          <span class="lr-q-text lr-clamp">{{ q.questionText || '' }}</span>
        </div>
      </div>
      <div v-if="practiceQuestions.length" class="lr-section">
        <div class="lr-section-title">巩固练习 ({{ practiceQuestions.length }})</div>
        <div v-for="q in practiceQuestions" :key="'p'+q.id" class="lr-q-item" @click="$emit('open-question', q)">
          <span class="lr-q-type">{{ typeLabel(q.questionType) }}</span>
          <span class="lr-q-text lr-clamp">{{ q.questionText || '' }}</span>
        </div>
      </div>
      <div v-if="inlineExamples.length" class="lr-section">
        <div class="lr-section-title">AI 推荐例题 ({{ inlineExamples.length }})</div>
        <div v-for="q in inlineExamples" :key="q._key" class="lr-q-item" @click="$emit('open-question', q)">
          <span class="lr-q-type">📝 例题</span>
          <span class="lr-q-text lr-clamp">{{ q.questionText || '' }}</span>
        </div>
      </div>
      <div v-if="inlinePractices.length" class="lr-section">
        <div class="lr-section-title">AI 巩固练习 ({{ inlinePractices.length }})</div>
        <div v-for="q in inlinePractices" :key="q._key" class="lr-q-item" @click="$emit('open-question', q)">
          <span class="lr-q-type">✏️ 练习</span>
          <span class="lr-q-text lr-clamp">{{ q.questionText || '' }}</span>
        </div>
      </div>
      <van-loading v-if="lrLoading" size="20" style="display:block;margin:12px auto" />
    </div>
  </van-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { typeLabel } from '@/composables/useQuestionHelpers'
import { getQuestionDetail } from '@/api/precision'

const props = defineProps({
  visible: { type: Boolean, default: false },
  node: { type: Object, default: null },
  subject: { type: Object, required: true }
})
defineEmits(['update:visible', 'open-question'])

const lrLoading = ref(false)
const currentResources = ref(null)
const currentNodeName = ref('')
const exampleQuestions = ref([])
const practiceQuestions = ref([])
const inlineExamples = ref([])
const inlinePractices = ref([])

async function open(w) {
  currentNodeName.value = w.nodeName || ('知识点 #' + w.nodeId)
  const lr = w.learningResources
  currentResources.value = lr
  exampleQuestions.value = []
  practiceQuestions.value = []
  inlineExamples.value = []
  inlinePractices.value = []

  if (!lr.videoUrl && lr.videoUrls && Array.isArray(lr.videoUrls) && lr.videoUrls.length) {
    lr.videoUrl = lr.videoUrls[0].url || lr.videoUrls[0]
  }

  const ids = [...(lr.exampleIds || []), ...(lr.practiceIds || [])]
  const hasInlined = (lr.examples?.length || 0) + (lr.practices?.length || 0) > 0

  if (!ids.length && !hasInlined) return
  lrLoading.value = true
  try {
    if (ids.length) {
      const qs = await Promise.all(ids.map(id =>
        getQuestionDetail(id).then(r => (r.code === 200 ? r.data : null)).catch(() => null)))
      const valid = qs.filter(Boolean)
      exampleQuestions.value = valid.filter(q => (lr.exampleIds || []).includes(q.id))
      practiceQuestions.value = valid.filter(q => (lr.practiceIds || []).includes(q.id))
    }
    if (hasInlined) {
      inlineExamples.value = (lr.examples || []).map((ex, i) => ({
        ...ex, _key: 'ie' + i, questionText: ex.question, correctAnswer: ex.answer,
        questionType: 'FILL_IN', _inline: true
      }))
      inlinePractices.value = (lr.practices || []).map((pr, i) => ({
        ...pr, _key: 'ip' + i, questionText: pr.question, correctAnswer: pr.answer,
        questionType: 'FILL_IN', _inline: true
      }))
    }
  } catch (e) {
    console.error('加载学习资源失败:', e)
  } finally {
    lrLoading.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.lr-popup { padding: 16px; }
.lr-node-name { font-size: var(--fs-md); font-weight: 600; color: var(--text-primary); margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px solid var(--border-base); }
.lr-section { margin-top: 14px; }
.lr-section-title { font-size: var(--fs-sm); font-weight: 600; color: var(--text-regular); margin-bottom: 6px; }
.lr-video-link { display: flex; align-items: center; gap: 6px; padding: 10px 12px; background: var(--primary-light); border-radius: 6px; color: var(--primary-color); text-decoration: none; font-size: var(--fs-sm); }
.lr-q-item { display: flex; align-items: center; gap: 8px; padding: 8px 10px; margin-bottom: 4px; background: var(--bg-secondary); border-radius: 4px; cursor: pointer; font-size: var(--fs-xs); }
.lr-q-item:active { background: var(--primary-light); }
.lr-q-type { color: var(--primary-color); font-weight: 500; white-space: nowrap; }
.lr-q-text { flex: 1; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.lr-clamp { display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; white-space: normal; }
</style>

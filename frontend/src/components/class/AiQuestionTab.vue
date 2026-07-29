<template>
  <div v-loading="aiLoading" class="ai-recommended-list">
    <div v-if="!aiLoading && aiQuestions.length === 0" class="empty-hint">暂无AI推荐的题目，请先在AI教学助手中生成并审核课堂提问</div>
    <div v-for="(group, cat) in groupedAiQuestions" :key="cat" class="ai-category-group">
      <div class="ai-category-title">{{ catLabel(cat) }}</div>
      <div
        v-for="q in group"
        :key="q.id"
        class="quiz-item"
        @click="selectAiQuestion(q)"
      >
        <div class="quiz-content" v-html="renderMath(q.content || q.questionText)"></div>
        <div class="quiz-meta">
          <el-tag
            v-if="q.intent"
            type="info"
            size="small"
            effect="plain"
          >
            💡 {{ q.intent }}
          </el-tag>
          <DifficultyBadge :difficulty-level="q.difficultyLevel" :tier="q.tier" :out-of-syllabus="q.outOfSyllabus" />
          <el-tag type="success" size="small">AI生成</el-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getAiRecommended } from '@/api/classroom'
import { renderMath } from '@/composables/useQuestionHelpers'
import DifficultyBadge from '@/components/common/DifficultyBadge.vue'

const props = defineProps({ subject: { type: String, default: '' } })
const emit = defineEmits(['select'])

const aiQuestions = ref([])
const aiLoading = ref(false)

const groupedAiQuestions = computed(() => {
  const groups = {}
  aiQuestions.value.forEach(q => {
    const cat = q.aiCategory || 'OTHER'
    if (!groups[cat]) groups[cat] = []
    groups[cat].push(q)
  })
  return groups
})

const catLabel = (cat) => {
  const map = { RECALL: '回忆型', COMPREHEND: '理解型', APPLY: '应用型', EXTEND: '拓展型' }
  return map[cat] || cat
}

const selectAiQuestion = (q) => {
  emit('select', q)
}

const loadAiQuestions = async () => {
  aiLoading.value = true
  try {
    const res = await getAiRecommended({ subject: props.subject || '' })
    if (res.code === 200) aiQuestions.value = res.data || []
  } finally { aiLoading.value = false }
}

const addQuestions = (questions) => {
  aiQuestions.value = [...questions, ...aiQuestions.value]
}

defineExpose({ loadAiQuestions, addQuestions })

onMounted(() => { loadAiQuestions() })
</script>

<style scoped lang="scss">
.ai-category-group { margin-bottom: 12px; }
.ai-category-title { font-size: var(--fs-sm); font-weight: 600; color: var(--text-secondary); margin-bottom: var(--spacing-xs); padding-left: var(--spacing-xs); }
.ai-recommended-list .quiz-item { cursor: pointer; padding: 10px 12px; border: 0.5px solid var(--border-color); border-radius: var(--radius-md); margin-bottom: var(--spacing-sm); transition: border-color var(--transition-base), background var(--transition-base); }
.ai-recommended-list .quiz-item:hover { border-color: var(--primary-color); background: var(--primary-light); }
.quiz-meta { display: flex; gap: var(--spacing-xs); margin-top: var(--spacing-xs); }
.empty-hint { text-align: center; color: var(--text-secondary); font-size: var(--fs-sm); padding: 40px 20px; }
.quiz-content { font-size: var(--fs-sm); color: var(--text-regular); line-height: 1.6; }
</style>

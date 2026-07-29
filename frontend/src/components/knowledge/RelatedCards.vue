<template>
  <div v-if="cards.length" class="related-cards mb-16">
    <div class="rc-header">
      <span class="rc-title">你答错了 {{ wrongNodes.length }} 个知识点，这里有捷径</span>
    </div>
    <div class="rc-list">
      <div
        v-for="c in cards"
        :key="c.cardId"
        class="rc-card"
        @click="goArticle(c.articleId)"
      >
        <div class="rc-card-front">{{ c.frontText }}</div>
        <div class="rc-card-meta">
          <span>{{ c.estimatedMinutes }} 分钟</span>
          <span class="rc-arrow">查看 →</span>
        </div>
      </div>
    </div>
    <div class="rc-footer">
      完成全部预计 {{ totalEstimated }} 分钟
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getRelatedCards } from '@/api/knowledgeBase'

const props = defineProps({
  submissionId: { type: Number, default: null }
})

const router = useRouter()
const cards = ref([])
const wrongNodes = ref([])
const totalEstimated = ref(0)

const load = async () => {
  if (!props.submissionId) return
  try {
    const r = await getRelatedCards(props.submissionId, 5)
    if (r.code === 200 && r.data && r.data.totalCards > 0) {
      cards.value = r.data.cards || []
      wrongNodes.value = r.data.wrongKnowledgeNodes || []
      totalEstimated.value = r.data.estimatedMinutes || 0
    }
  } catch { /* 静默失败 */ }
}

const goArticle = (articleId) => {
  router.push(`/knowledge-base/article/${articleId}`)
}

watch(() => props.submissionId, (v) => { if (v) load() }, { immediate: true })
</script>

<style scoped lang="scss">
.related-cards {
  background: var(--bg-card);
  border: 0.5px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 16px 20px;
}

.rc-header {
  margin-bottom: 12px;
}

.rc-title {
  font-size: var(--fs-md);
  font-weight: 600;
  color: var(--text-primary);
}

.rc-list {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.rc-card {
  flex: 1;
  min-width: 160px;
  padding: 12px 14px;
  background: linear-gradient(135deg, #eff6ff, #f0f4ff);
  border: 1px solid rgba(67, 97, 238, 0.12);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: transform 0.15s ease, border-color 0.15s ease;

  &:hover {
    transform: translateY(-2px);
    border-color: var(--primary-color);
  }
}

.rc-card-front {
  font-size: var(--fs-sm);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.rc-card-meta {
  display: flex;
  justify-content: space-between;
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}

.rc-arrow {
  color: var(--primary-color);
  font-weight: 500;
}

.rc-footer {
  margin-top: 10px;
  font-size: var(--fs-xs);
  color: var(--text-disabled);
  text-align: right;
}

.mb-16 { margin-bottom: 16px; }
</style>

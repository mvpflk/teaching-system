<template>
  <div class="review-page">
    <div class="review-header">
      <h3>🕐 今日复习</h3>
      <p v-if="cards.length" class="subtitle">共 {{ cards.length }} 张卡片待复习，坚持就是胜利</p>
    </div>
    <FlashcardDeck
      v-if="cards.length"
      :cards="cards.map(c => c.flashcard)"
      @rate="onRate"
      @restart="loadCards"
    />
    <div v-else class="empty-state">
      <div class="empty-icon">🎉</div>
      <h4>今天没有待复习的卡片</h4>
      <p>所有卡片都已复习完毕，好样的！</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTodayReview, rateFlashcard } from '@/api/knowledgeBase'
import FlashcardDeck from '@/components/knowledge/FlashcardDeck.vue'

const cards = ref([])

onMounted(() => loadCards())

async function loadCards() {
  try {
    const r = await getTodayReview()
    cards.value = r.data || []
  } catch (e) {
    console.error('加载复习卡片失败:', e)
    ElMessage.error('加载复习卡片失败')
  }
}

async function onRate({ flashcard, rating }) {
  try {
    const r = await rateFlashcard(flashcard.id, rating)
    if (r.code === 200) ElMessage.success(r.data?.message || '评分成功')
  } catch (e) {
    console.error('评分失败:', e)
    ElMessage.error('评分失败，请重试')
  }
}
</script>

<style scoped>
.review-page { max-width: 600px; margin: 0 auto; }
.review-header { margin-bottom: 20px; }
.review-header h3 { margin: 0 0 4px; }
.subtitle { color: var(--text-secondary); font-size: var(--fs-md); margin: 0; }
.empty-state { text-align: center; padding: 60px 0; }
.empty-icon { font-size: 48px; margin-bottom: 16px; }
.empty-state h4 { margin: 0 0 8px; color: var(--text-primary); }
.empty-state p { margin: 0; color: var(--text-secondary); font-size: var(--fs-md); }
</style>

<template>
  <div class="deck">
    <div class="deck-header">
      <div class="deck-progress">
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: ((currentIndex + 1) / shuffledCards.length * 100) + '%' }" />
        </div>
        <span class="progress-text">{{ currentIndex + 1 }} / {{ shuffledCards.length }}</span>
      </div>
    </div>
    <transition name="card-swap" mode="out-in">
      <FlashcardItem
        v-if="currentCard"
        :key="currentCard.id"
        :card="currentCard"
        @rate="onRate"
      />
    </transition>
    <el-empty v-if="shuffledCards.length === 0" description="暂无记忆卡片" />
    <div v-if="done" class="done-message">
      <el-result icon="success" title="本轮复习完成！" sub-title="继续保持，知识会越来越牢固" />
      <el-button type="primary" @click="$emit('restart')">再来一轮</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import FlashcardItem from './FlashcardItem.vue'

const props = defineProps({ cards: Array })
const emit = defineEmits(['rate', 'restart'])

const shuffledCards = ref([])
const currentIndex = ref(0)
const done = ref(false)

const currentCard = computed(() => shuffledCards.value[currentIndex.value] || null)

function shuffle(array) {
  const arr = [...array]
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [arr[i], arr[j]] = [arr[j], arr[i]]
  }
  return arr
}

onMounted(() => { shuffledCards.value = shuffle(props.cards) })

watch(() => props.cards, () => {
  shuffledCards.value = shuffle(props.cards)
  currentIndex.value = 0
  done.value = false
})

function onRate(rating) {
  emit('rate', { flashcard: currentCard.value, rating })
  if (currentIndex.value < shuffledCards.value.length - 1) {
    currentIndex.value++
  } else {
    done.value = true
  }
}
</script>

<style scoped>
.deck { max-width: 480px; margin: 0 auto; }
.deck-header { margin-bottom: 16px; }
.deck-progress { display: flex; align-items: center; gap: 12px; }
.progress-bar { flex: 1; height: 4px; background: var(--bg-section); border-radius: 2px; overflow: hidden; }
.progress-fill { height: 100%; background: var(--primary-color); border-radius: 2px; transition: width 0.3s ease; }
.progress-text { font-size: var(--fs-xs); color: var(--text-secondary); white-space: nowrap; }
.done-message { text-align: center; padding: 40px 0; }
.card-swap-enter-active { transition: all 0.25s ease-out; }
.card-swap-leave-active { transition: all 0.15s ease-in; }
.card-swap-enter-from { opacity: 0; transform: translateX(20px); }
.card-swap-leave-to { opacity: 0; transform: translateX(-20px); }
</style>

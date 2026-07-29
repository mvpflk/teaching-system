<template>
  <div v-if="totalCards > 0" class="wcl-link">
    <span class="wcl-text">复习：{{ nodeName || '知识点' }} · {{ totalCards }}张卡片 · 约{{ estimatedMinutes }}分钟</span>
    <el-button size="small" type="primary" text @click="goCards">开始复习</el-button>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getCardsByNodeId } from '@/api/knowledgeBase'

const props = defineProps({
  nodeId: { type: Number, default: null },
  nodeName: { type: String, default: '' }
})

const router = useRouter()
const totalCards = ref(0)
const estimatedMinutes = computed(() => totalCards.value * 2)

const load = async () => {
  if (!props.nodeId) return
  try {
    const r = await getCardsByNodeId(props.nodeId, 3)
    if (r.code === 200 && r.data) {
      totalCards.value = r.data.totalCards || 0
    }
  } catch { /* 静默失败 */ }
}

const goCards = () => {
  router.push(`/knowledge-base/discover?nodeId=${props.nodeId}`)
}

watch(() => props.nodeId, (v) => { if (v) load() }, { immediate: true })
</script>

<style scoped lang="scss">
.wcl-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 8px;
  margin-top: 4px;
  background: var(--primary-light, #f0f4ff);
  border-radius: var(--radius-sm, 4px);
  font-size: var(--fs-xs);
}

.wcl-text {
  color: var(--text-secondary);
}
</style>

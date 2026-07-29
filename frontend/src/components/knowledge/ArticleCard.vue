<template>
  <div class="article-card" @click="$router.push(`/knowledge-base/article/${article.id}`)">
    <div class="card-header">
      <el-tag v-if="article.difficulty" size="small" :type="diffType">
        {{ '⭐'.repeat(article.difficulty) }}
      </el-tag>
      <span v-if="article.chapter" class="card-chapter">{{ article.chapter }}</span>
    </div>
    <h4 class="card-title">{{ article.title }}</h4>
    <p v-if="article.excerpt" class="card-excerpt">{{ article.excerpt }}</p>
    <div v-if="parsedTags.length" class="card-tags">
      <el-tag
        v-for="t in parsedTags"
        :key="t"
        size="small"
        round
      >
        {{ t }}
      </el-tag>
    </div>
    <div class="card-footer">
      <span>👁 {{ article.viewCount || 0 }}</span>
      <span v-if="article.progress">📝 {{ article.progress }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ article: Object })

const diffType = computed(() =>
  props.article.difficulty === 1 ? '' : props.article.difficulty === 2 ? 'warning' : 'danger')

const parsedTags = computed(() => {
  if (!props.article.tags) return []
  try {
    return typeof props.article.tags === 'string' ? JSON.parse(props.article.tags) : props.article.tags
  } catch { return [] }
})
</script>

<style scoped>
.article-card {
  border: 0.5px solid var(--border-light); border-radius: var(--radius-md); padding: 16px;
  cursor: pointer; transition: all 0.2s; background: var(--bg-card);
}
.article-card:hover {
  border-color: var(--primary-color); transform: translateY(-2px);
}
.article-card:active { transform: translateY(0); }
.card-header { display: flex; gap: 8px; align-items: center; margin-bottom: 8px; }
.card-chapter { font-size: var(--fs-xs); color: var(--text-secondary); }
.card-title { font-size: var(--fs-md); font-weight: 600; margin: 0 0 6px; color: var(--text-primary); line-height: 1.4; }
.card-excerpt {
  font-size: var(--fs-xs); color: var(--text-secondary); display: -webkit-box;
  -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; margin: 0; line-height: 1.5;
}
.card-tags { display: flex; gap: 4px; flex-wrap: wrap; margin-top: 8px; }
.card-footer { display: flex; justify-content: space-between; font-size: var(--fs-xs); color: var(--text-disabled); margin-top: 10px; }
</style>

<template>
  <div class="qcard" :class="{ 'qcard--in-basket': inBasket }">
    <QuestionRenderer
      :question="q"
      mode="display"
      size="small"
      :show-answer="showAnswer || forceShowAnswer"
      :show-explanation="showAnswer || forceShowAnswer"
      :show-meta="false"
      :category-path="categoryPath"
    />
    <div class="qcard__stats">
      <span>组卷 {{ usedCount }} 次</span>
      <span v-if="q.creatorName"> · {{ q.creatorName }}</span>
    </div>
    <div class="qcard__actions">
      <el-button text size="small" @click="showAnswer = !showAnswer">
        {{ showAnswer ? '收起解析 ▴' : '查看解析 ▾' }}
      </el-button>
      <el-button text size="small" @click="$emit('preview', q)">预览</el-button>
      <el-button
        size="small"
        :type="inBasket ? 'success' : 'primary'"
        @click="$emit('toggle-basket', q)"
      >
        {{ inBasket ? '✓ 已在篮' : '+ 加入试题篮' }}
      </el-button>
      <slot name="extra-actions" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import QuestionRenderer from '@/components/question/QuestionRenderer.vue';

const props = defineProps({
  q: { type: Object, required: true },
  usedCount: { type: Number, default: 0 },
  categoryPath: { type: String, default: '' },
  inBasket: { type: Boolean, default: false },
  forceShowAnswer: { type: Boolean, default: false },
});
defineEmits(['toggle-basket', 'preview']);

const showAnswer = ref(false);
</script>

<style scoped>
.qcard {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 14px 16px;
}
.qcard--in-basket {
  border-color: var(--primary-color);
}
.qcard__stats {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  margin-top: 6px;
  margin-bottom: 8px;
}
.qcard__actions {
  display: flex;
  gap: 4px;
  align-items: center;
  flex-wrap: wrap;
  margin-top: 8px;
}
</style>

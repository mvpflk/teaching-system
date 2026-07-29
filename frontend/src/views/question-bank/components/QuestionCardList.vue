<template>
  <div v-loading="loading && list.length > 0" class="qlist">
    <el-result v-if="error" icon="error" title="加载失败" sub-title="请检查网络后重试">
      <template #extra><el-button size="small" @click="$emit('retry')">重试</el-button></template>
    </el-result>
    <!-- 骨架屏：首次加载时展示，替代全屏 spinner -->
    <template v-else-if="loading && !list.length">
      <div v-for="i in 5" :key="'sk'+i" class="qcard qcard--skeleton">
        <div class="sk-line sk-line--1" />
        <div class="sk-line sk-line--2" />
        <div class="sk-line sk-line--3" />
        <div class="sk-tags">
          <div class="sk-tag" />
          <div class="sk-tag" />
          <div class="sk-tag" />
        </div>
      </div>
    </template>
    <el-empty v-else-if="!loading && !list.length" description="暂无题目，调整筛选或点击「添加题目」" />
    <QuestionCard
      v-for="q in list" :key="q.id" :q="q"
      :used-count="usage[q.id] || 0"
      :category-path="categoryPathOf(q.categoryId)"
      :in-basket="basket.has(q.id)"
      :force-show-answer="showAnswerAll"
      @toggle-basket="basket.toggle(q.id)"
      @preview="$emit('preview', q)"
    >
      <template #extra-actions>
        <slot name="card-actions" :q="q" />
      </template>
    </QuestionCard>
    <div v-if="total > pageSize" class="qlist__pager">
      <el-pagination
        :current-page="pageNum" :page-size="pageSize" :total="total"
        :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next"
        background small
        @current-change="$emit('update:pageNum', $event); $emit('page')"
        @size-change="$emit('update:pageSize', $event); $emit('page')" />
    </div>
  </div>
</template>

<script setup>
import QuestionCard from './QuestionCard.vue'
import { useQuestionBasketStore } from '@/stores/questionBasket'

defineProps({
  list: { type: Array, default: () => [] },
  loading: Boolean,
  error: Boolean,
  total: { type: Number, default: 0 },
  pageNum: { type: Number, default: 1 },
  pageSize: { type: Number, default: 20 },
  usage: { type: Object, default: () => ({}) },
  showAnswerAll: Boolean,
  categoryPathOf: { type: Function, required: true },
})
defineEmits(['update:pageNum', 'update:pageSize', 'page', 'preview', 'retry'])
const basket = useQuestionBasketStore()
</script>

<style scoped>
.qlist { display: flex; flex-direction: column; gap: 10px; min-height: 200px; }
.qlist__pager { display: flex; justify-content: center; margin-top: 10px; }

/* 骨架屏 */
.qcard--skeleton { pointer-events: none; }
.sk-line { height: 14px; border-radius: 4px; background: linear-gradient(90deg, var(--bg-secondary) 25%, var(--border-light) 50%, var(--bg-secondary) 75%); background-size: 200% 100%; animation: sk-shimmer 1.5s infinite; margin-bottom: 8px; }
.sk-line--1 { width: 70%; }
.sk-line--2 { width: 50%; }
.sk-line--3 { width: 30%; }
.sk-tags { display: flex; gap: 6px; margin-top: 8px; }
.sk-tag { width: 40px; height: 20px; border-radius: 4px; background: var(--bg-secondary); animation: sk-shimmer 1.5s infinite; }
@keyframes sk-shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
</style>

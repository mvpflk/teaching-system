<template>
  <div class="filter-chips">
    <div class="chip-row">
      <span class="chip-label">排序</span>
      <el-check-tag :checked="!model.sort || model.sort === 'latest'" @change="set('sort', 'latest')">最新入库</el-check-tag>
      <el-check-tag :checked="model.sort === 'mostUsed'" @change="set('sort', 'mostUsed')">最常组卷</el-check-tag>
    </div>
    <div class="chip-row">
      <span class="chip-label">题型</span>
      <el-check-tag :checked="!model.questionType" @change="set('questionType', '')">全部</el-check-tag>
      <el-check-tag v-for="t in COMMON_TYPES" :key="t" :checked="model.questionType === t"
        @change="set('questionType', t)">{{ QUESTION_TYPE_LABEL[t] }}</el-check-tag>
      <el-popover trigger="click" width="320">
        <template #reference>
          <el-check-tag :checked="isMoreTypeActive">更多 ▾</el-check-tag>
        </template>
        <el-check-tag v-for="t in MORE_TYPES" :key="t" :checked="model.questionType === t"
          style="margin:4px" @change="set('questionType', t)">{{ QUESTION_TYPE_LABEL[t] }}</el-check-tag>
      </el-popover>
    </div>
    <div class="chip-row">
      <span class="chip-label">难度</span>
      <el-check-tag :checked="!model.difficultyLevel" @change="set('difficultyLevel', null)">全部</el-check-tag>
      <el-check-tag :checked="model.difficultyLevel === 1" @change="set('difficultyLevel', 1)">基础</el-check-tag>
      <el-check-tag :checked="model.difficultyLevel === 2" @change="set('difficultyLevel', 2)">中等</el-check-tag>
      <el-check-tag :checked="model.difficultyLevel === 3" @change="set('difficultyLevel', 3)">进阶</el-check-tag>
    </div>
    <div class="chip-row">
      <span class="chip-label">来源</span>
      <el-check-tag :checked="!model.source" @change="set('source', '')">全部</el-check-tag>
      <el-check-tag v-for="(label, v) in SOURCE_LABEL" :key="v" :checked="model.source === v"
        @change="set('source', v)">{{ label }}</el-check-tag>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { QUESTION_TYPE_LABEL } from '@/constants/questionTypes'

const COMMON_TYPES = ['SINGLE_CHOICE', 'MULTI_CHOICE', 'TRUE_FALSE', 'FILL_IN', 'SHORT_ANSWER', 'PROGRAMMING']
const MORE_TYPES = ['DRAG_SORT', 'MATCHING', 'CLOZE', 'ESSAY', 'COMPOSITE', 'FILE_UPLOAD', 'AUDIO_VIDEO', 'CLASSROOM_MANUAL']
const SOURCE_LABEL = { MANUAL: '手动', AI: 'AI', WORD_IMPORT: 'Word', EXCEL_IMPORT: 'Excel', PAPER_IMPORT: '整卷' }

const props = defineProps({ model: { type: Object, required: true } })
const emit = defineEmits(['change'])
const set = (key, val) => emit('change', { ...props.model, [key]: val })
const isMoreTypeActive = computed(() => MORE_TYPES.includes(props.model.questionType))
</script>

<style scoped>
.filter-chips { display: flex; flex-direction: column; gap: 6px; }
.chip-row { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; }
.chip-label { width: 36px; flex-shrink: 0; font-size: var(--fs-xs); color: var(--text-secondary); }
</style>

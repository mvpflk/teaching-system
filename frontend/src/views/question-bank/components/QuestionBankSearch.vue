<template>
  <el-card v-show="!isMobile || showSearch" shadow="never" class="search-card">
    <div class="search-row">
      <div class="search-filters">
        <div class="filter-group">
          <el-select :model-value="subjectId" placeholder="学科" clearable class="sf-item" @update:model-value="$emit('update:subjectId', $event); $emit('subjectChange', $event)">
            <el-option v-for="s in categoryTree" :key="s.id" :value="s.id" :label="s.name" />
          </el-select>
          <el-select :model-value="chapterId" placeholder="章节" clearable :disabled="!subjectId" class="sf-item" @update:model-value="$emit('update:chapterId', $event); $emit('chapterChange', $event)">
            <el-option v-for="c in chapters" :key="c.id" :value="c.id" :label="c.name" />
          </el-select>
          <el-select :model-value="taskId" placeholder="任务" clearable :disabled="!chapterId" class="sf-item" @update:model-value="$emit('update:taskId', $event); $emit('taskChange', $event)">
            <el-option v-for="t in tasks" :key="t.id" :value="t.id" :label="t.name" />
          </el-select>
          <el-select :model-value="kpId" placeholder="知识点" clearable :disabled="!taskId || kps.length === 0" class="sf-item" @update:model-value="$emit('update:kpId', $event); $emit('applyFilter')">
            <el-option v-for="k in kps" :key="k.id" :value="k.id" :label="k.name" />
          </el-select>
        </div>
        <div class="filter-group">
          <el-select :model-value="questionType" placeholder="题型" clearable filterable class="sf-type" @update:model-value="$emit('update:questionType', $event); $emit('search')">
            <el-option v-for="(label, key) in typeLabels" :key="key" :value="key" :label="label" />
          </el-select>
          <el-input :model-value="keyword" placeholder="搜索题目内容" clearable class="sf-keyword" @update:model-value="$emit('update:keyword', $event)" @keyup.enter="$emit('search')">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button type="primary" @click="$emit('search')">查询</el-button>
          <el-button v-if="subjectId || chapterId || taskId || kpId" size="small" @click="$emit('clearFilter')">清除</el-button>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
defineProps({
  isMobile: Boolean, showSearch: Boolean, categoryTree: Array,
  chapters: Array, tasks: Array, kps: Array,
  typeLabels: Object, subjectId: [String, Number], chapterId: [String, Number],
  taskId: [String, Number], kpId: [String, Number], questionType: String, keyword: String
})
defineEmits(['subjectChange', 'chapterChange', 'taskChange', 'applyFilter', 'search', 'clearFilter',
  'update:subjectId', 'update:chapterId', 'update:taskId', 'update:kpId', 'update:questionType', 'update:keyword'])
</script>

<style scoped>
.search-card { margin-bottom: 12px; }
.search-card :deep(.el-card__body) { padding: 12px 16px; }
.search-row { display: flex; align-items: center; }
.search-filters { display: flex; flex-direction: column; gap: 10px; flex: 1; }
.filter-group { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.sf-item { width: 130px; }
.sf-type { width: 100px; }
.sf-keyword { width: 200px; }
@media (max-width: 768px) {
  .filter-group { gap: 6px; }
  .sf-item, .sf-type { width: 100%; }
  .sf-keyword { width: 100%; }
}
</style>

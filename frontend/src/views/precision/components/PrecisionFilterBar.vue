<template>
  <div class="filter-bar">
    <el-select
      :model-value="selectedClassId"
      placeholder="选择班级查看分析"
      :clearable="!isSingleClass"
      :disabled="isSingleClass"
      size="default"
      class="filter-class"
      @update:model-value="$emit('update:selectedClassId', $event)"
    >
      <el-option v-for="c in classList" :key="c.id" :label="c.className || c.name" :value="c.id" />
    </el-select>
    <el-tag v-if="isSingleClass" size="small" effect="plain" type="info" class="single-class-tip">仅可查看本班</el-tag>
    <el-select
      :model-value="filterSubject"
      placeholder="全部学科"
      clearable
      size="default"
      class="filter-subj"
      @update:model-value="$emit('update:filterSubject', $event)"
    >
      <el-option v-if="allowedSubjects.length > 1" label="全部学科" value="" />
      <el-option v-for="s in allowedSubjects" :key="s" :label="s" :value="s" />
    </el-select>
    <el-input
      :model-value="searchName"
      placeholder="搜索学生姓名"
      clearable
      size="default"
      class="filter-search"
      @update:model-value="$emit('update:searchName', $event)"
    />
    <el-button type="primary" @click="$emit('remindAll')">一键提醒</el-button>
  </div>
</template>

<script setup>
defineProps({
  selectedClassId: String, isSingleClass: Boolean, classList: Array,
  filterSubject: String, allowedSubjects: Array, searchName: String
})
defineEmits(['update:selectedClassId', 'update:filterSubject', 'update:searchName', 'remindAll'])
</script>

<style scoped>
.filter-bar {
  display: flex;
  gap: 10px;
  margin-bottom: var(--spacing-md);
  align-items: center;
  flex-wrap: wrap;
}
.filter-class { width: 180px; }
.filter-subj { width: 150px; }
.filter-search { width: 180px; flex: 1; min-width: 140px; }
.single-class-tip { flex-shrink: 0; }
</style>

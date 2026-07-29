<template>
  <div class="filter-bar-wrapper">
    <el-form
      :model="model"
      inline
      class="filter-bar"
      @keyup.enter="onSearch"
    >
      <slot />
      <el-form-item>
        <el-button type="primary" size="small" @click="onSearch">搜索</el-button>
        <el-button size="small" @click="onReset">重置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
defineProps({
  model: { type: Object, default: () => ({}) },
})
const emit = defineEmits(['search', 'reset'])
const onSearch = () => emit('search')
const onReset = () => emit('reset')
</script>

<style scoped>
.filter-bar-wrapper {
  margin-bottom: 12px;
}
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
.filter-bar .el-form-item {
  margin-bottom: 0;
}
@media (max-width: 768px) {
  .filter-bar {
    gap: 6px;
  }
  .filter-bar .el-form-item {
    width: 100%;
  }
  .filter-bar .el-form-item .el-select,
  .filter-bar .el-form-item .el-input {
    width: 100% !important;
  }
}
</style>

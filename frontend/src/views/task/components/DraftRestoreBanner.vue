<template>
  <el-alert v-if="visible" type="info" show-icon :closable="false" class="draft-banner">
    <template #title>
      已恢复上次未完成的草稿
      <el-button size="small" text type="primary" class="ml-2" @click="$emit('discard')">放弃草稿</el-button>
      <el-button size="small" text @click="$emit('dismiss')">知道了</el-button>
    </template>
  </el-alert>
</template>

<script setup>
defineProps({ visible: Boolean })
defineEmits(['discard', 'dismiss'])
</script>

<style scoped>
.draft-banner { margin-bottom: 16px; }
.draft-banner :deep(.el-alert__title) { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.ml-2 { margin-left: 8px; }
</style>

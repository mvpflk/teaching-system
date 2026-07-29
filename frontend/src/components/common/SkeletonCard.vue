<template>
  <div class="skeleton-card">
    <div v-if="hasAvatar" class="sk-row">
      <el-skeleton animated>
        <template #template>
          <el-skeleton-item variant="circle" style="width:40px;height:40px" />
        </template>
      </el-skeleton>
      <div class="sk-lines">
        <el-skeleton animated :rows="rows" :loading="true" />
      </div>
    </div>
    <el-skeleton
      v-else
      animated
      :rows="rows"
      :loading="true"
    />
  </div>
</template>

<script setup>
defineProps({
  rows: { type: Number, default: 3 },
  hasAvatar: { type: Boolean, default: false }
})
</script>

<style scoped>
.skeleton-card {
  padding: 16px;
  background: var(--bg-card);
  border: 0.5px solid var(--border-color);
  border-radius: var(--radius-md);
  margin-bottom: var(--mobile-card-gap, 8px);
}
.sk-row {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}
.sk-lines {
  flex: 1;
}
</style>

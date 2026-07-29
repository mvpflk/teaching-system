<template>
  <div class="recent-section" v-if="tasks.length > 0">
    <h4>最近任务</h4>
    <div class="recent-list">
      <div v-for="t in tasks" :key="t.id" class="recent-item" @click="$emit('select', t)">
        <span class="recent-title">{{ t.title }}</span>
        <el-tag size="small">{{ t.status }}</el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({ tasks: { type: Array, default: () => [] } })
defineEmits(['select'])
</script>

<style scoped>
.recent-section { margin-top: 24px; }
.recent-section h4 { margin: 0 0 12px; color: var(--text-primary); }
.recent-item { display: flex; justify-content: space-between; align-items: center; padding: 8px 12px;
  cursor: pointer; border-radius: var(--radius-sm); transition: background 0.15s; }
.recent-item:hover { background: var(--bg-page); }
.recent-title { font-size: var(--fs-sm); }
</style>

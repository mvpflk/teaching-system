<template>
  <div class="edtc">
    <div class="edtc-title">📋 今日任务 · {{ stageName }}</div>
    <div class="edtc-row"><span>🔤 词汇 {{ task.vocabCount || 0 }} 词</span><span v-if="task.grammarCount">📝 语法 {{ task.grammarCount }} 题</span></div>
    <slot />
  </div>
</template>
<script setup>
defineProps({ task: Object, stageName: String })
</script>
<style scoped>
.edtc { padding: 16px; background: linear-gradient(135deg, var(--primary-light), #f5f0ff); border: 1px solid var(--primary-color); border-radius: 8px; margin-bottom: 14px; }
.edtc-title { font-size: var(--fs-md); font-weight: 600; margin-bottom: 8px; color: var(--text-primary); }
.edtc-row { display: flex; gap: 20px; font-size: var(--fs-md); color: var(--text-regular); }
</style>

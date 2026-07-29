<template>
  <div v-if="displayErrors.length > 0" class="error-inline">
    <span class="error-inline-label">最近错误</span>
    <div class="error-inline-tags">
      <span
        v-for="(e, i) in displayErrors"
        :key="i"
        class="error-chip"
      >"{{ e.char }}" → "{{ e.expected }}"</span>
      <span v-if="errorListLength > 8 && !showAllErrors" class="error-more" @click="$emit('toggle-show-all')">+{{ errorListLength - 8 }}</span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  displayErrors: { type: Array, default: () => [] },
  showAllErrors: { type: Boolean, default: false },
  errorListLength: { type: Number, default: 0 }
})

defineEmits(['toggle-show-all'])
</script>

<style scoped>
.error-inline { display: flex; align-items: flex-start; gap: 8px; margin-top: 4px; }
.error-inline-label { font-size: var(--fs-xs); color: var(--typing-incorrect); white-space: nowrap; padding-top: 3px; }
.error-inline-tags { display: flex; flex-wrap: wrap; gap: 4px; }
.error-chip { font-size: var(--fs-xs); padding: 2px 8px; background: var(--typing-incorrect-bg); color: var(--typing-incorrect); border-radius: 10px; white-space: nowrap; }
.error-more { font-size: var(--fs-xs); color: var(--typing-pending); cursor: pointer; padding: 2px 6px; }
.error-more:hover { color: var(--typing-text); }
</style>

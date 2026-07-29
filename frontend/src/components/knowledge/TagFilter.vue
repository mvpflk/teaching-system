<template>
  <div v-if="usefulTags.length > 0" class="tag-filter">
    <span
      v-for="t in usefulTags"
      :key="t.name"
      class="tag-item"
      :class="{ active: selected.includes(t.name) }"
      @click="toggle(t.name)"
    >
      {{ t.name }} ({{ t.count }})
    </span>
    <span
      v-if="tags.length > usefulTags.length"
      class="tag-more-hint"
    >… 另有 {{ tags.length - usefulTags.length }} 个冷门标签未显示</span>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue';

const props = defineProps({ tags: Array, modelValue: Array });
const emit = defineEmits(['update:modelValue']);
const selected = ref(props.modelValue || []);

// Only show tags with count >= 2 — single-article tags are useless as filters
const usefulTags = computed(() => (props.tags || []).filter((t) => t.count >= 2));

watch(
  () => props.modelValue,
  (v) => {
    selected.value = v || [];
  }
);

function toggle(name) {
  const idx = selected.value.indexOf(name);
  if (idx >= 0) selected.value.splice(idx, 1);
  else selected.value.push(name);
  emit('update:modelValue', [...selected.value]);
}
</script>

<style scoped>
.tag-filter {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}
.tag-item {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 14px;
  font-size: var(--fs-xs);
  cursor: pointer;
  background: var(--bg-section);
  color: var(--text-secondary);
  transition: all 0.15s;
  user-select: none;
  white-space: nowrap;
}
.tag-item:hover {
  background: var(--primary-color-light, #e8ecff);
  color: var(--primary-color, var(--primary-color));
}
.tag-item.active {
  background: var(--primary-color, var(--primary-color));
  color: #fff;
}
.tag-more-hint {
  display: inline-block;
  padding: 4px 6px;
  font-size: 11px;
  color: var(--text-placeholder);
  align-self: center;
}
</style>

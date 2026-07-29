<template>
  <el-drawer
    v-model="visible"
    title="📒 我的词汇本"
    direction="rtl"
    size="380px"
  >
    <el-input
      v-model="keyword"
      placeholder="搜索词汇..."
      clearable
      size="small"
      style="margin-bottom:12px"
    />
    <div v-for="g in filteredGroups" :key="g.stage" style="margin-bottom:16px">
      <h4 style="font-size:var(--fs-sm);color:var(--text-secondary);margin:0 0 6px;display:flex;justify-content:space-between">
        <span>{{ g.stageName }} ({{ g.count }})</span>
        <span style="font-size:var(--fs-xs)">{{ g.words.filter(w => w.masterLevel >= 3).length }} 已掌握</span>
      </h4>
      <div v-for="w in g.words" :key="w.word" style="display:flex;justify-content:space-between;align-items:center;padding:6px 0;font-size:var(--fs-sm);border-bottom:1px solid var(--border-light,#eee)">
        <div>
          <strong>{{ w.word }}</strong>
          <span style="color:var(--text-secondary);font-size:var(--fs-xs);margin-left:6px">{{ w.phonetic }}</span>
        </div>
        <span style="color:var(--text-secondary);font-size:var(--fs-xs)">{{ w.meaning }}</span>
      </div>
    </div>
    <el-empty v-if="!filteredGroups.length" description="暂无词汇数据" :image-size="48" />
  </el-drawer>
</template>
<script setup>
import { ref, computed } from 'vue'
const props = defineProps({ modelValue: Boolean, groups: { type: Array, default: () => [] } })
const emit = defineEmits(['update:modelValue'])
const visible = computed({ get: () => props.modelValue, set: (v) => emit('update:modelValue', v) })
const keyword = ref('')
const filteredGroups = computed(() => {
  if (!keyword.value.trim()) return props.groups
  const kw = keyword.value.toLowerCase()
  return props.groups.map(g => ({
    ...g,
    words: (g.words || []).filter(w =>
      String(w.word || '').toLowerCase().includes(kw) || (String(w.meaning || '').includes(kw)))  // R112修复：防非字符串类型崩溃
  })).filter(g => g.words.length > 0)
})
</script>

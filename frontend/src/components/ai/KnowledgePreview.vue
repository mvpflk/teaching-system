<template>
  <div class="kp-preview">
    <div v-if="!content" class="kp-empty">
      <el-icon><InfoFilled /></el-icon>
      <span>该知识点暂无知识库内容，请导入或手动编辑</span>
    </div>
    <div v-else class="kp-body">
      <div class="kp-meta">
        <el-icon v-if="content" class="kp-has-icon"><StarFilled /></el-icon>
        <span v-if="content" class="kp-has-text">已加载知识库内容（{{ charCount }}字）</span>
      </div>
      <div class="kp-content" v-html="renderedHtml"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue';
import { getNodeContent } from '@/api/knowledgeNode';
import { renderMarkdown } from '@/utils/markdown';
import { InfoFilled, StarFilled } from '@element-plus/icons-vue';

const props = defineProps({
  categoryId: [Number, String],
  includeChildren: { type: Boolean, default: false },
});

const content = ref('');
const charCount = computed(() => content.value?.length || 0);

const renderedHtml = computed(() => renderMarkdown(content.value));

watch(
  [() => props.categoryId, () => props.includeChildren],
  async ([id, inc]) => {
    if (!id) {
      content.value = '';
      return;
    }
    try {
      const res = await getNodeContent(id, inc);
      if (res.code === 200) content.value = res.data?.content || '';
    } catch {
      content.value = '';
    }
  },
  { immediate: true }
);
</script>

<style scoped>
.kp-preview {
  min-height: 60px;
}
.kp-empty {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
  font-size: var(--fs-sm);
  padding: 12px 0;
}
.kp-meta {
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.kp-has-icon {
  color: var(--el-color-warning);
  font-size: var(--fs-md);
}
.kp-has-text {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}
.kp-content {
  max-height: 300px;
  overflow-y: auto;
  padding: 16px;
  background: var(--bg-section);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  font-size: var(--fs-sm);
  line-height: var(--lh-relaxed);
}
.kp-content :deep(h1) {
  font-size: var(--fs-lg);
  margin: 0 0 8px;
}
.kp-content :deep(h2) {
  font-size: var(--fs-md);
  margin: 8px 0 6px;
}
.kp-content :deep(h3) {
  font-size: var(--fs-md);
  margin: 6px 0 4px;
}
.kp-content :deep(p) {
  margin: 4px 0;
}
.kp-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
}
.kp-content :deep(th),
.kp-content :deep(td) {
  border: 1px solid var(--border-color);
  padding: 6px 10px;
  text-align: left;
}
.kp-content :deep(th) {
  background: var(--bg-secondary);
  font-weight: 600;
}
</style>

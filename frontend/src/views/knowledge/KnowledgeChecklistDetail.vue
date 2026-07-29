<template>
  <div class="kcd-page">
    <!-- 返回 -->
    <el-page-header :content="checklist?.title || '加载中...'" @back="$router.back()">
      <template #icon>
        <el-icon><ArrowLeft /></el-icon>
      </template>
    </el-page-header>

    <!-- 加载中 -->
    <div v-if="loading" class="kcd-skeleton">
      <el-skeleton :rows="2" animated style="margin-bottom: 16px" />
      <el-skeleton :rows="8" animated />
    </div>

    <!-- 内容 -->
    <template v-else-if="checklist">
      <div class="kcd-meta">
        <el-tag size="small" type="primary">{{ checklist.subject }}</el-tag>
        <span class="kcd-date">{{ fmtDate(checklist.createdAt) }}</span>
      </div>
      <div class="kcd-content markdown-body" v-html="renderedContent" />
    </template>

    <!-- 不存在 -->
    <el-empty v-else description="知识清单不存在或未发布" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getChecklistDetail } from '@/api/knowledgeBase';
import { renderMarkdown } from '@/utils/markdown';
import { ArrowLeft } from '@element-plus/icons-vue';
import dayjs from 'dayjs';

import '@/assets/markdown-body.css';

const route = useRoute();
const router = useRouter();
const checklist = ref(null);
const loading = ref(false);

const fmtDate = (d) => (d ? dayjs(d).format('YYYY-MM-DD HH:mm') : '');

const renderedContent = computed(() => renderMarkdown(checklist.value?.content || ''));

const loadDetail = async (id) => {
  if (!id) return;
  loading.value = true;
  try {
    const r = await getChecklistDetail(id);
    if (r.code === 200 && r.data) {
      checklist.value = r.data;
    } else {
      checklist.value = null;
    }
  } catch {
    checklist.value = null;
  } finally {
    loading.value = false;
  }
};

onMounted(() => loadDetail(route.params.id));
watch(
  () => route.params.id,
  (newId) => {
    if (newId) loadDetail(newId);
  }
);
</script>

<style scoped lang="scss">
.kcd-page {
  padding: var(--spacing-lg, 24px);
  max-width: 860px;
  margin: 0 auto;
}

.kcd-skeleton {
  margin-top: 24px;
}

.kcd-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 20px;
  margin-bottom: 24px;
}

.kcd-date {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}

.kcd-content {
  padding: 24px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  line-height: 1.8;

  :deep(h1) {
    font-size: 1.6em;
    margin-top: 0;
  }
  :deep(h2) {
    font-size: 1.3em;
    margin-top: 1.5em;
    padding-bottom: 0.3em;
    border-bottom: 1px solid var(--border-light);
  }
  :deep(h3) {
    font-size: 1.15em;
    margin-top: 1.3em;
  }
  :deep(table) {
    width: 100%;
    border-collapse: collapse;
    margin: 1em 0;
  }
  :deep(th),
  :deep(td) {
    padding: 8px 12px;
    border: 1px solid var(--border-color);
    text-align: left;
  }
  :deep(th) {
    background: var(--bg-section);
    font-weight: 600;
  }
  :deep(blockquote) {
    border-left: 4px solid var(--primary-color);
    padding: 8px 16px;
    margin: 1em 0;
    background: var(--primary-light);
  }
  :deep(code) {
    background: var(--bg-secondary);
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 0.9em;
  }
  :deep(pre) {
    background: var(--bg-secondary);
    padding: 16px;
    border-radius: var(--radius-md);
    overflow-x: auto;
  }
}

@media (max-width: 768px) {
  .kcd-page {
    padding: var(--spacing-md, 16px);
  }
}
</style>

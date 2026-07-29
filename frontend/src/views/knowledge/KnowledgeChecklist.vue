<template>
  <div class="kc-page">
    <!-- 搜索栏 -->
    <div class="kc-search-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索知识清单..."
        :prefix-icon="Search"
        clearable
        size="large"
        @keyup.enter="onSearch"
        @clear="onSearch"
      />
    </div>

    <!-- 骨架加载 -->
    <div v-if="loading" class="kc-grid">
      <div v-for="i in 6" :key="i" class="kc-card-skeleton">
        <el-skeleton :rows="3" animated />
      </div>
    </div>

    <!-- 空状态 -->
    <el-empty v-else-if="checklists.length === 0" description="暂无知识清单">
      <template #image>
        <div style="font-size: 64px; opacity: 0.3">📋</div>
      </template>
    </el-empty>

    <!-- 清单卡片网格 -->
    <div v-else class="kc-grid">
      <div
        v-for="item in checklists"
        :key="item.id"
        class="kc-card"
        @click="router.push(`/knowledge-base/checklist/${item.id}`)"
      >
        <div class="kc-card-header">
          <el-tag size="small" type="primary">{{ item.subject }}</el-tag>
        </div>
        <h4 class="kc-card-title">{{ item.title || '知识清单' }}</h4>
        <p class="kc-card-excerpt">{{ item.excerpt }}</p>
        <div class="kc-card-footer">
          <span class="kc-card-date">{{ fmtDate(item.createdAt) }}</span>
          <el-icon :size="14"><ArrowRight /></el-icon>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="total > size" class="kc-pagination">
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        background
        @current-change="fetchData"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { listChecklists } from '@/api/knowledgeBase';
import { Search, ArrowRight } from '@element-plus/icons-vue';
import dayjs from 'dayjs';

const router = useRouter();
const keyword = ref('');
const checklists = ref([]);
const loading = ref(false);
const page = ref(1);
const size = ref(12);
const total = ref(0);
let fetchSeq = 0;

const fmtDate = (d) => (d ? dayjs(d).format('YYYY-MM-DD') : '');

const onSearch = () => {
  page.value = 1;
  fetchData();
};

const fetchData = async () => {
  const seq = ++fetchSeq;
  loading.value = true;
  try {
    const r = await listChecklists({
      keyword: keyword.value || undefined,
      page: page.value,
      size: size.value,
    });
    if (seq !== fetchSeq) return;
    if (r.code === 200 && r.data) {
      checklists.value = r.data.records || [];
      total.value = r.data.total || 0;
    }
  } catch {
    /* 静默降级 */
  } finally {
    if (seq === fetchSeq) loading.value = false;
  }
};

onMounted(() => fetchData());
</script>

<style scoped lang="scss">
.kc-page {
  padding: var(--spacing-lg, 24px);
}

.kc-search-bar {
  margin-bottom: 20px;
  max-width: 480px;
}

.kc-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.kc-card-skeleton {
  padding: 20px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
}

.kc-card {
  padding: 20px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition:
    box-shadow 0.2s,
    border-color 0.2s;

  &:hover {
    border-color: var(--primary-color);
    box-shadow: 0 2px 12px rgba(67, 97, 238, 0.08);
  }
}

.kc-card-header {
  margin-bottom: 10px;
}

.kc-card-title {
  margin: 0 0 10px;
  font-size: var(--fs-md);
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.kc-card-excerpt {
  margin: 0 0 14px;
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.kc-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: var(--fs-xs);
  color: var(--text-disabled);
}

.kc-pagination {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

@media (max-width: 768px) {
  .kc-page {
    padding: var(--spacing-md, 16px);
  }
  .kc-grid {
    grid-template-columns: 1fr;
  }
  .kc-search-bar {
    max-width: 100%;
  }
}
</style>

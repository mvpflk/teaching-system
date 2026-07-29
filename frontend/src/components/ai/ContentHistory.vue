<template>
  <div class="ch-wrap">
    <div class="ch-filter">
      <el-select
        v-model="typeFilter"
        placeholder="类型筛选"
        size="small"
        class="ch-filter-select"
        clearable
        @change="onFilterChange"
      >
        <el-option label="教学设计" value="TEACHING_DESIGN" />
        <el-option label="知识清单" value="KNOWLEDGE_CHECKLIST" />
        <el-option label="实训方案" value="PRACTICE_PLAN" />
        <el-option label="综合练习" value="COMPREHENSIVE_EXERCISES" />
        <el-option label="课堂提问" value="CLASSROOM_QUESTIONS" />
        <el-option label="配套练习" value="KNOWLEDGE_PRACTICE" />
      </el-select>
      <el-input
        v-model="keyword"
        placeholder="搜索关键词"
        size="small"
        class="ch-filter-input"
        clearable
        @keyup.enter="onSearch"
        @clear="onSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>
    <el-table
      v-if="!isMobile"
      v-loading="loading"
      :data="items"
      stripe
      size="small"
      empty-text="暂无生成记录"
    >
      <el-table-column
        prop="title"
        label="标题"
        min-width="180"
        show-overflow-tooltip
      >
        <template #default="{ row }">
          <span>{{ row.title }}</span>
          <el-tag
            v-if="row.questionCount"
            size="small"
            type="warning"
            style="margin-left: 6px"
          >
            {{ row.questionCount }}题
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="contentType" label="类型" width="110">
        <template #default="{ row }">{{ typeLabel(row.contentType || row.outputType) }}</template>
      </el-table-column>
      <el-table-column prop="subject" label="学科" width="100" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag
            :type="
              row.status === 1
                ? 'success'
                : row.status === 2
                  ? 'info'
                  : row.status === 3
                    ? 'warning'
                    : 'info'
            "
            size="small"
          >
            {{
              row.status === 1
                ? '已发布'
                : row.status === 2
                  ? '已归档'
                  : row.status === 3
                    ? '待审核'
                    : '草稿'
            }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="时间" width="100">
        <template #default="{ row }">{{ fmtDate(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <div class="ch-actions">
            <el-button
              size="small"
              text
              type="primary"
              @click="$emit('view', row)"
            >
              <el-icon><View /></el-icon> 查看
            </el-button>
            <el-button
              size="small"
              text
              type="primary"
              @click="$emit('exportWord', row)"
            >
              <el-icon><Download /></el-icon> 导出
            </el-button>
            <el-button
              v-if="(row.outputType || row.contentType) === 'KNOWLEDGE_CHECKLIST' && row.id"
              size="small"
              text
              type="success"
              @click="$emit('generatePractice', row.id)"
            >
              <el-icon><Notebook /></el-icon> 配套练习
            </el-button>
            <el-button
              v-if="row.status === 0"
              size="small"
              text
              type="success"
              @click="$emit('publish', row.id)"
            >
              <el-icon><Check /></el-icon> 发布
            </el-button>
            <el-button
              size="small"
              text
              type="danger"
              @click="$emit('archive', row.id)"
            >
              <el-icon><Delete /></el-icon> 归档
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 移动端卡片列表 -->
    <div v-if="isMobile" v-loading="loading" class="ch-mobile">
      <el-empty v-if="!loading && !items.length" description="暂无生成记录" :image-size="80" />
      <div v-else class="ch-mobile-list">
        <div v-for="row in items" :key="row.id" class="ch-mobile-card">
          <div class="ch-mobile-card-top">
            <span class="ch-mobile-card-title">{{ row.title }}</span>
            <el-tag
              v-if="row.questionCount"
              size="small"
              type="warning"
            >
              {{ row.questionCount }}题
            </el-tag>
          </div>
          <div class="ch-mobile-card-meta">
            <el-tag size="small" :type="statusTagType(row.status)" effect="plain">
              {{
                statusLabel(row.status)
              }}
            </el-tag>
            <el-tag size="small" type="info" effect="plain">
              {{
                typeLabel(row.contentType || row.outputType)
              }}
            </el-tag>
            <span v-if="row.subject" class="ch-mobile-card-subject">{{ row.subject }}</span>
            <span class="ch-mobile-card-date">{{ fmtDate(row.createdAt) }}</span>
          </div>
          <div class="ch-mobile-card-actions">
            <el-button
              size="small"
              text
              type="primary"
              @click="$emit('view', row)"
            >
              <el-icon><View /></el-icon> 查看
            </el-button>
            <el-button
              size="small"
              text
              type="primary"
              @click="$emit('exportWord', row)"
            >
              <el-icon><Download /></el-icon> 导出
            </el-button>
            <el-button
              v-if="(row.outputType || row.contentType) === 'KNOWLEDGE_CHECKLIST' && row.id"
              size="small"
              text
              type="success"
              @click="$emit('generatePractice', row.id)"
            >
              <el-icon><Notebook /></el-icon> 练习
            </el-button>
            <el-button
              v-if="row.status === 0"
              size="small"
              text
              type="success"
              @click="$emit('publish', row.id)"
            >
              <el-icon><Check /></el-icon> 发布
            </el-button>
            <el-button
              size="small"
              text
              type="danger"
              @click="$emit('archive', row.id)"
            >
              <el-icon><Delete /></el-icon> 归档
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="hasMore" class="ch-loadmore">
      <el-button
        :loading="loading"
        size="small"
        text
        type="primary"
        @click="$emit('loadMore')"
      >
        {{ loading ? '加载中...' : '加载更多' }}
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import dayjs from 'dayjs';
import { Search, View, Download, Check, Delete, Notebook } from '@element-plus/icons-vue';
import { useIsMobile } from '@/composables/useIsMobile';

const props = defineProps({
  items: { type: Array, default: () => [] },
  loading: Boolean,
  hasMore: Boolean,
});
const emit = defineEmits([
  'filter',
  'search',
  'view',
  'publish',
  'archive',
  'exportWord',
  'generatePractice',
  'loadMore',
]);

const { isMobile } = useIsMobile();
const typeFilter = ref('');
const keyword = ref('');

const typeLabel = (t) =>
  ({
    TEACHING_DESIGN: '教学设计',
    KNOWLEDGE_CHECKLIST: '知识清单',
    PRACTICE_PLAN: '实训方案',
    COMPREHENSIVE_EXERCISES: '综合练习',
    CLASSROOM_QUESTIONS: '课堂提问',
    KNOWLEDGE_PRACTICE: '配套练习',
  })[t] || t;

const statusLabel = (s) => ({ 0: '草稿', 1: '已发布', 2: '已归档', 3: '待审核' })[s] || '草稿';
const statusTagType = (s) => ({ 0: 'info', 1: 'success', 2: 'info', 3: 'warning' })[s] || 'info';

const fmtDate = (d) => (d ? dayjs(d).format('MM-DD HH:mm') : '');

const onFilterChange = (type) => emit('filter', type);
const onSearch = () => emit('search', keyword.value || '');
</script>

<style scoped>
.ch-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}
.ch-filter-select {
  width: 180px;
}
.ch-filter-input {
  width: 180px;
}
@media (max-width: 768px) {
  .ch-filter-select,
  .ch-filter-input {
    width: 100%;
    flex: 1 1 45%;
  }
}
.ch-actions {
  display: flex;
  gap: 2px;
  align-items: center;
  flex-wrap: nowrap;
}
.ch-actions .el-button {
  padding: 4px 6px;
  font-size: var(--fs-xs);
}
.ch-loadmore {
  display: flex;
  justify-content: center;
  padding-top: 12px;
}

/* 移动端卡片列表 */
.ch-mobile {
  min-height: 120px;
}
.ch-mobile-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.ch-mobile-card {
  padding: 12px;
  background: var(--bg-card);
  border: 0.5px solid var(--border-color);
  border-radius: var(--radius-md);
}
.ch-mobile-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}
.ch-mobile-card-title {
  font-size: var(--fs-md);
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}
.ch-mobile-card-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
  font-size: var(--fs-xs);
}
.ch-mobile-card-subject {
  color: var(--text-secondary);
}
.ch-mobile-card-date {
  color: var(--text-disabled);
  margin-left: auto;
}
.ch-mobile-card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0;
  border-top: 0.5px solid var(--border-light);
  padding-top: 8px;
}
.ch-mobile-card-actions .el-button {
  padding: 4px 8px;
  font-size: var(--fs-xs);
}
</style>

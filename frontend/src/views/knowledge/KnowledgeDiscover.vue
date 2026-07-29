<template>
  <div class="discover-page">
    <div class="search-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索知识点..."
        clearable
        size="large"
        @clear="onSearch"
        @keyup.enter="onSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>
    <div v-if="chapters.length > 1" class="chapter-quick-bar">
      <span
        class="chapter-pill"
        :class="{ active: kbStore.selectedChapter.value === '' }"
        @click="onChapterSelect('')"
      >全部</span>
      <span
        v-for="ch in chapters"
        :key="ch.name"
        class="chapter-pill"
        :class="{ active: kbStore.selectedChapter.value === ch.name }"
        @click="onChapterSelect(ch.name)"
      >{{ ch.name }}</span>
    </div>
    <TagFilter v-model="selectedTags" :tags="tags" @update:model-value="onFilterChange" />
    <div v-if="selectedTags.length > 0" class="filter-status">
      🔍 已筛选：<b>{{ selectedTags.join('、') }}</b> · {{ total }} 篇结果
      <el-button
        link
        type="primary"
        size="small"
        @click="clearTagFilter"
      >
        清除
      </el-button>
    </div>
    <div v-if="loading" class="sk-grid">
      <div v-for="i in 6" :key="i" class="sk-card">
        <div class="sk-line" style="width: 60%"></div>
        <div class="sk-line" style="width: 40%"></div>
      </div>
    </div>
    <div v-else-if="articles.length === 0" class="empty"><el-empty description="暂无文章" /></div>
    <div
      v-else
      ref="articleZoneRef"
      class="article-grid"
      :class="{ 'is-mobile': isMobile }"
    >
      <ArticleCard v-for="a in articles" :key="a.id" :article="a" />
    </div>
    <div v-if="total > size" class="pagination">
      <el-pagination
        v-model:current-page="page"
        layout="prev, pager, next"
        :total="total"
        :page-size="size"
        background
        @current-change="fetchData"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, computed } from 'vue';
import { useRoute } from 'vue-router';
import { listArticles, getTags } from '@/api/knowledgeBase';
import { useIsMobile } from '@/composables/useIsMobile';
import { useKnowledgeBaseStore } from '@/composables/useKnowledgeBaseStore';
import { Search } from '@element-plus/icons-vue';
import ArticleCard from '@/components/knowledge/ArticleCard.vue';
import TagFilter from '@/components/knowledge/TagFilter.vue';

const route = useRoute();
const { isMobile } = useIsMobile();
const kbStore = useKnowledgeBaseStore();

const SUBJECT_ID = computed(() => Number(route.query.subjectId) || 1);
const keyword = ref('');
const chapters = ref([]);
const tags = ref([]);
const selectedTags = ref([]);
const articles = ref([]);
const loading = ref(false);
const page = ref(1);
const size = ref(12);
const total = ref(0);

onMounted(async () => {
  await kbStore.loadTree(SUBJECT_ID.value);
  chapters.value = kbStore.chapterTree.value;
  try {
    const r = await getTags(SUBJECT_ID.value);
    tags.value = r.data || [];
  } catch (e) {
    console.error('getTags failed:', e);
  }
  fetchData();
});

watch(
  () => kbStore.selectedChapter.value,
  () => {
    page.value = 1;
    fetchData();
  }
);
watch(
  () => kbStore.selectedTask.value,
  () => {
    page.value = 1;
    fetchData();
  }
);

let fetchSeq = 0;
async function fetchData() {
  const seq = ++fetchSeq;

  loading.value = true;
  const params = {
    subjectId: SUBJECT_ID.value,
    keyword: keyword.value || undefined,
    chapter: kbStore.selectedChapter.value || undefined,
    task: kbStore.selectedTask.value || undefined,
    tags: selectedTags.value.join(',') || undefined,
    page: page.value,
    size: size.value,
  };
  try {
    const r = await listArticles(params);
    if (seq !== fetchSeq) return;
    articles.value = r.data?.records || [];
    total.value = r.data?.total || 0;
  } catch (e) {
    if (seq !== fetchSeq) return;
    console.error('[Discover] fetchData failed:', e);
    articles.value = [];
    total.value = 0;
  } finally {
    if (seq === fetchSeq) loading.value = false;
  }
}

const articleZoneRef = ref(null);

function onSearch() {
  page.value = 1;
  fetchData();
}
function onFilterChange() {
  page.value = 1;
  fetchData();
  scrollToArticles();
}
function onChapterSelect(chapter) {
  kbStore.selectedChapter.value = chapter;
  kbStore.selectedTask.value = '';
  page.value = 1;
  fetchData();
}

function clearTagFilter() {
  selectedTags.value = [];
  page.value = 1;
  fetchData();
}

function scrollToArticles() {
  setTimeout(() => {
    articleZoneRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }, 200);
}
</script>

<style scoped>
.discover-page {
  max-width: 900px;
  margin: 0 auto;
}
.search-bar {
  margin-bottom: 16px;
}
.article-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}
.article-grid.is-mobile {
  grid-template-columns: 1fr;
}
.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
.sk-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}
.sk-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.sk-line {
  height: 14px;
  border-radius: 4px;
  background: linear-gradient(
    90deg,
    var(--bg-section) 25%,
    var(--bg-card) 50%,
    var(--bg-section) 75%
  );
  background-size: 200% 100%;
  animation: sk-shimmer 1.5s infinite;
}
@keyframes sk-shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
.empty {
  padding: 40px 0;
}
.chapter-quick-bar {
  display: flex;
  gap: 6px;
  overflow-x: auto;
  padding-bottom: 4px;
  margin-bottom: 12px;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
}
.chapter-quick-bar::-webkit-scrollbar {
  display: none;
}
.chapter-pill {
  white-space: nowrap;
  padding: 6px 14px;
  border-radius: 16px;
  font-size: var(--fs-xs);
  cursor: pointer;
  background: var(--bg-section);
  color: var(--text-secondary);
  transition: all 0.2s;
  user-select: none;
}
.chapter-pill.active {
  background: var(--primary-color);
  color: #fff;
}
.chapter-pill:active {
  transform: scale(0.96);
}
.filter-status {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  margin-bottom: 8px;
  background: var(--primary-light);
  border-radius: 8px;
  font-size: var(--fs-xs);
  color: var(--text-regular);
}
</style>

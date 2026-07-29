<template>
  <div class="article-page">
    <el-page-header :content="article?.title || '加载中...'" @back="$router.back()" />
    <!-- 骨架加载 -->
    <div v-if="loading" class="article-skeleton">
      <div class="sk-line" style="height: 20px; width: 60%; margin-bottom: 12px"></div>
      <div class="sk-line" style="height: 14px; width: 100%; margin-bottom: 8px"></div>
      <div class="sk-line" style="height: 14px; width: 90%; margin-bottom: 8px"></div>
      <div class="sk-line" style="height: 14px; width: 95%; margin-bottom: 8px"></div>
      <div class="sk-line" style="height: 14px; width: 70%; margin-bottom: 16px"></div>
      <div class="sk-line" style="height: 14px; width: 100%; margin-bottom: 8px"></div>
      <div class="sk-line" style="height: 14px; width: 85%; margin-bottom: 8px"></div>
      <div class="sk-line" style="height: 14px; width: 60%; margin-bottom: 8px"></div>
    </div>
    <el-tabs v-else-if="article" v-model="activeTab" class="article-tabs">
      <el-tab-pane label="📖 阅读" name="read">
        <div class="article-content markdown-body" v-html="renderedContent" />
        <div v-if="article.memoryTips" class="memory-tips">
          <h4>🧠 记忆口诀</h4>
          <div v-html="renderedMemoryTips" />
        </div>
        <div v-if="article.examFocus" class="exam-focus">
          <h4>📋 考试重点</h4>
          <div v-html="renderedExamFocus" />
        </div>
        <div v-if="isStudent" class="article-actions">
          <el-button
            v-if="!learningStarted"
            type="primary"
            size="large"
            :loading="learningLoading"
            @click="handleStartLearning"
          >
            🃏 开始记忆
          </el-button>
          <el-button
            plain
            :loading="favLoading"
            class="fav-btn"
            @click="handleFavorite"
          >
            {{ isFavorited ? '❤️ 已收藏' : '🤍 收藏' }}
          </el-button>
        </div>
      </el-tab-pane>
      <el-tab-pane v-if="isStudent" label="🃏 记忆卡" name="flashcards">
        <FlashcardDeck
          v-if="cards.length"
          :cards="cards"
          @rate="onCardRate"
          @restart="restartDeck"
        />
        <el-empty v-else description="该文章暂无记忆卡片" />
      </el-tab-pane>
      <el-tab-pane v-if="isStudent" label="📝 自测" name="quiz">
        <ArticleQuiz :questions="quizQuestions" :article-id="route.params.id" />
      </el-tab-pane>
    </el-tabs>
    <div v-if="isStudent && recommended.length" class="recommend-section">
      <div class="rec-header">📚 猜你喜欢</div>
      <div class="rec-scroll">
        <div
          v-for="a in recommended"
          :key="a.id"
          class="rec-card"
          @click="$router.push(`/knowledge-base/article/${a.id}`)"
        >
          <div class="rec-card-title">{{ a.title }}</div>
          <div class="rec-card-chapter">{{ a.chapter }}</div>
          <el-tag :type="a.difficulty <= 2 ? 'success' : 'warning'" size="small" round>
            {{ ['', '基础', '中等', '困难', '挑战'][a.difficulty] || '' }}
          </el-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  getArticle,
  rateFlashcard,
  startLearning,
  toggleFavorite,
  getRecommendations,
} from '@/api/knowledgeBase';
import { renderMarkdown } from '@/utils/markdown';
import { formatKnowledgeContent } from '@/utils/knowledgeFormat';
import '@/assets/markdown-body.css';
import { useUserStore } from '@/stores/user';
import FlashcardDeck from '@/components/knowledge/FlashcardDeck.vue';
import ArticleQuiz from '@/components/knowledge/ArticleQuiz.vue';

const route = useRoute();
const userStore = useUserStore();
const isStudent = computed(() => userStore.isStudent);

const loading = ref(true);
const article = ref(null);
const cards = ref([]);
const activeTab = ref('read');
const learningStarted = ref(false);
const isFavorited = ref(false);
const favLoading = ref(false);
const learningLoading = ref(false);
const recommended = ref([]);

const renderedContent = computed(() => {
  const raw = article.value?.contentMd || '';
  return renderMarkdown(formatKnowledgeContent(raw));
});

const renderedMemoryTips = computed(() => {
  const raw = article.value?.memoryTips || '';
  return renderMarkdown(formatKnowledgeContent(raw));
});

const renderedExamFocus = computed(() => {
  const raw = article.value?.examFocus || '';
  return renderMarkdown(formatKnowledgeContent(raw));
});

const quizQuestions = computed(() => {
  if (!article.value?.quiz) return [];
  try {
    return typeof article.value.quiz === 'string'
      ? JSON.parse(article.value.quiz)
      : article.value.quiz;
  } catch (e) {
    console.warn('Failed to parse quiz JSON:', e);
    return [];
  }
});

async function loadArticle(id) {
  loading.value = true;
  try {
    const r = await getArticle(id);
    article.value = r.data?.article;
    cards.value = r.data?.article?.flashcards || [];
    learningStarted.value = r.data?.progress?.reviewedCards > 0;
    isFavorited.value = !!r.data?.isFavorited;
    activeTab.value = 'read';
  } catch (e) {
    console.error('加载文章失败:', e);
    ElMessage.error('加载文章失败');
  } finally {
    loading.value = false;
  }
  if (!isStudent.value) return;
  try {
    const rec = await getRecommendations(article.value?.subjectId || 24);
    recommended.value = (rec.data || []).slice(0, 4);
  } catch (e) {
    console.error('推荐加载失败:', e);
  }
  nextTick(() => {
    const saved = localStorage.getItem(`kb_scroll_${route.params.id}`);
    if (saved) {
      const el = document.querySelector('.article-page') || document.querySelector('.kb-main');
      if (el) el.scrollTop = parseInt(saved, 10);
    }
  });
}

onMounted(() => loadArticle(route.params.id));

// 关键修复：同一组件内切换文章时 onMounted 不会重新触发，必须 watch 路由参数
watch(
  () => route.params.id,
  (newId) => {
    if (newId) loadArticle(newId);
  }
);

async function onCardRate({ flashcard, rating }) {
  try {
    const r = await rateFlashcard(flashcard.id, rating);
    if (r.code === 200) ElMessage.success(r.data?.message || '评分成功');
  } catch (e) {
    console.error('评分失败:', e);
    ElMessage.error('评分失败，请重试');
  }
}

function restartDeck() {
  getArticle(route.params.id)
    .then((r) => {
      cards.value = r.data?.article?.flashcards || [];
    })
    .catch((e) => {
      console.error('restartDeck failed:', e);
      ElMessage.error('重新加载卡片失败');
    });
}

async function handleStartLearning() {
  learningLoading.value = true;
  try {
    await startLearning(route.params.id);
    learningStarted.value = true;
    ElMessage.success('已加入记忆队列，卡片将在今日复习中出现');
  } catch (e) {
    console.error('加入学习队列失败:', e);
    ElMessage.error('加入学习队列失败，请重试');
  } finally {
    learningLoading.value = false;
  }
}

onBeforeUnmount(() => {
  const el = document.querySelector('.article-page') || document.querySelector('.kb-main');
  if (el && isStudent.value) {
    localStorage.setItem(`kb_scroll_${route.params.id}`, String(el.scrollTop));
  }
});

async function handleFavorite() {
  favLoading.value = true;
  try {
    const r = await toggleFavorite(route.params.id);
    if (r.code === 200) {
      isFavorited.value = r.data?.favorited;
      ElMessage.success(isFavorited.value ? '已收藏' : '已取消收藏');
    }
  } catch (e) {
    console.error('收藏操作失败:', e);
    ElMessage.error('收藏操作失败，请重试');
  } finally {
    favLoading.value = false;
  }
}
</script>

<style scoped>
.article-page {
  max-width: 860px;
  margin: 0 auto;
}
.markdown-body {
  padding: 16px 0;
}

.memory-tips {
  background: var(--bg-warning-light);
  border: 1px solid var(--el-color-warning);
  border-radius: 10px;
  padding: 16px;
  margin-top: 20px;
}
.memory-tips h4 {
  margin: 0 0 8px;
  color: var(--el-color-warning);
}
.exam-focus {
  background: var(--primary-light);
  border: 1px solid var(--primary-color);
  border-radius: 10px;
  padding: 16px;
  margin-top: 16px;
}
.exam-focus h4 {
  margin: 0 0 8px;
  color: var(--primary-color);
}
.article-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--border-light);
}
.article-actions .fav-btn {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
}
.article-skeleton {
  padding: 16px 0;
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

.recommend-section {
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid var(--border-light, #eee);
}
.rec-header {
  font-size: var(--fs-md, 15px);
  font-weight: 600;
  margin-bottom: 12px;
  color: var(--text-primary, #333);
}
.rec-scroll {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 6px;
  scrollbar-width: thin;
}
.rec-card {
  flex: 0 0 200px;
  padding: 14px;
  border: 1px solid var(--border-light, #eee);
  border-radius: var(--radius-md, 10px);
  cursor: pointer;
  transition: all 0.2s;
  background: var(--bg-card, #fff);
}
.rec-card:hover {
  border-color: var(--primary-color, var(--primary-color));
  box-shadow: 0 4px 12px rgba(67, 97, 238, 0.1);
  transform: translateY(-2px);
}
.rec-card-title {
  font-size: var(--fs-sm, 14px);
  font-weight: 600;
  color: var(--text-primary, #333);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.rec-card-chapter {
  font-size: var(--fs-xs, 11px);
  color: var(--text-secondary, #999);
  margin-bottom: 6px;
}

/* 移动端适配:窄屏阅读优化(390px~768px) */
@media (max-width: 768px) {
  .article-page {
    max-width: 100%;
    padding: 0 16px;
  }
  .markdown-body {
    font-size: 15px;
    line-height: 1.75;
    padding: 12px 0;
  }
  .article-actions {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }
  .article-actions .el-button {
    width: 100%;
  }
  .rec-card {
    flex: 0 0 160px;
    padding: 12px;
  }
  .memory-tips,
  .exam-focus {
    padding: 14px;
    margin-top: 16px;
  }
  .recommend-section {
    margin-top: 20px;
    padding-top: 16px;
  }
  .rec-scroll {
    gap: 8px;
  }
}
</style>

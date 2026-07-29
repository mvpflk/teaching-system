<template>
  <div class="mcd-page">
    <div class="page-header">
      <el-button text @click="$router.push('/student/checkpoint')">&larr; 返回</el-button>
      <div class="mcd-title-wrap">
        <h2>我的记忆卡</h2>
        <div v-if="unreviewedCount > 0" class="mcd-badge">{{ unreviewedCount }}</div>
      </div>
      <div class="mcd-modes">
        <el-radio-group v-model="mode" size="small">
          <el-radio-button value="flip">翻转模式</el-radio-button>
          <el-radio-button value="list">列表模式</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <div v-if="unreviewedCount > 0" class="mcd-alert">
      <el-alert
        :title="'你还有 ' + unreviewedCount + ' 张卡片未复习'"
        type="warning"
        show-icon
        :closable="false"
      />
    </div>

    <div v-loading="loading" class="mcd-body">
      <el-empty
        v-if="!loading && cards.length === 0"
        description="暂无记忆卡，闯关通过后自动生成"
      />

      <div v-if="mode === 'flip'" class="mcd-flip-area">
        <div v-for="card in cards" :key="card.id" class="mcd-card-wrap">
          <div class="mcd-card-label">{{ card.taskName }}</div>
          <div class="mcd-card-kw-list">
            <MemoryCardFlip
              v-for="(kw, ki) in cardKeywords(card)"
              :key="ki"
              :keyword="kw"
              @click="markReviewed(card.id)"
            />
          </div>
          <div class="mcd-card-meta">
            <span>复习 {{ card.reviewCount || 0 }} 次</span>
            <span v-if="card.lastReviewedAt">{{ formatTime(card.lastReviewedAt) }}</span>
          </div>
        </div>
      </div>

      <div v-else class="mcd-list-area">
        <div v-for="card in cards" :key="card.id" class="mcd-list-card">
          <div class="mcd-list-header">
            <span class="mcd-list-task">{{ card.taskName }}</span>
            <span class="mcd-list-chapter">{{ card.chapterName }}</span>
          </div>
          <div class="mcd-list-kws">
            <span
              v-for="(kw, ki) in cardKeywords(card)"
              :key="ki"
              class="mcd-list-pill"
              :class="kw.type === 'number' ? 'pill-number' : 'pill-concept'"
              @click="showPillDetail(kw)"
            >{{ kw.front }}</span>
          </div>
          <div class="mcd-list-meta">
            <span>复习 {{ card.reviewCount || 0 }} 次</span>
            <el-button
              text
              size="small"
              type="primary"
              @click="markReviewed(card.id)"
            >
              标记已复习
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="pillDialog" title="关键词释义" width="320px">
      <div class="pill-detail">
        <div class="pill-term">{{ selectedKw?.front }}</div>
        <div class="pill-meaning">{{ selectedKw?.back }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { listMemoryCards, reviewMemoryCard, getUnreviewedCount } from '@/api/checkpoint';
import MemoryCardFlip from '@/components/checkpoint/MemoryCardFlip.vue';

const mode = ref('flip');
const loading = ref(true);
const cards = ref([]);
const unreviewedCount = ref(0);
const pillDialog = ref(false);
const selectedKw = ref(null);

onMounted(async () => {
  const [cardRes, countRes] = await Promise.all([listMemoryCards(0), getUnreviewedCount()]);
  if (cardRes.code === 200) cards.value = cardRes.data;
  if (countRes.code === 200) unreviewedCount.value = countRes.data.count;
  loading.value = false;
});

function cardKeywords(card) {
  if (card.card?.keywords) return card.card.keywords;
  return [];
}

function formatTime(t) {
  if (!t) return '';
  return t.substring(0, 10);
}

async function markReviewed(cardId) {
  await reviewMemoryCard(cardId);
  const card = cards.value.find((c) => c.id === cardId);
  if (card) {
    card.reviewCount = (card.reviewCount || 0) + 1;
    card.lastReviewedAt = new Date().toISOString();
  }
  if (unreviewedCount.value > 0) unreviewedCount.value--;
}

function showPillDetail(kw) {
  selectedKw.value = kw;
  pillDialog.value = true;
}
</script>

<style scoped>
.mcd-page {
  max-width: 900px;
  margin: 0 auto;
  padding: var(--spacing-lg);
}
.page-header {
  position: relative;
}
.mcd-title-wrap {
  position: relative;
  display: inline-block;
}
.mcd-modes {
  margin-top: 8px;
}
.mcd-alert {
  margin: var(--spacing-md) 0;
}
.mcd-body {
}

.mcd-flip-area {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}
.mcd-card-wrap {
}
.mcd-card-label {
  font-weight: 600;
  font-size: var(--fs-base);
  color: var(--text-primary);
  margin-bottom: 8px;
}
.mcd-card-kw-list {
  display: flex;
  gap: var(--spacing-md);
  flex-wrap: wrap;
}
.mcd-card-meta {
  margin-top: 8px;
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  display: flex;
  gap: 12px;
}

.mcd-list-area {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.mcd-list-card {
  background: var(--bg-card);
  border: 0.5px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: var(--spacing-md);
}
.mcd-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.mcd-list-task {
  font-weight: 600;
  font-size: var(--fs-base);
  color: var(--text-primary);
}
.mcd-list-chapter {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}
.mcd-list-kws {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 10px;
}
.mcd-list-pill {
  padding: 2px 10px;
  border-radius: 999px;
  font-size: var(--fs-xs);
  cursor: pointer;
  transition: opacity var(--transition-base);
}
.mcd-list-pill:hover {
  opacity: 0.8;
}
.pill-concept {
  background: var(--primary-light);
  color: var(--primary-color);
}
.pill-number {
  background: var(--bg-danger-light);
  color: var(--el-color-danger);
}
.mcd-list-meta {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}

.pill-detail {
  text-align: center;
}
.pill-term {
  font-size: var(--fs-xl);
  font-weight: 700;
  color: var(--primary-color);
  margin-bottom: 12px;
}
.pill-meaning {
  font-size: var(--fs-base);
  color: var(--text-regular);
  line-height: 1.6;
}

.mcd-badge {
  position: absolute;
  top: -8px;
  right: -8px;
  background: var(--el-color-danger);
  color: white;
  font-size: 11px;
  font-weight: 700;
  min-width: 20px;
  height: 20px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 6px;
}

.mcd-flip-card {
  perspective: 1000px;
}
.mcd-flip-inner {
  transition: transform 0.6s;
  transform-style: preserve-3d;
}
.mcd-flip-card:hover .mcd-flip-inner {
  transform: rotateY(180deg);
}
</style>

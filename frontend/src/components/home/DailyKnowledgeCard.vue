<template>
  <div v-if="card" class="daily-card-wrapper mb-24">
    <div class="daily-card" :class="{ flipped: isFlipped }" @click="flip">
      <div class="dc-inner">
        <div class="dc-front">
          <div class="dc-subject">
            {{ card.subjectName || '知识卡片' }}
            <span v-if="card.cardType" class="dc-type-badge" :class="'dc-type-' + card.cardType">{{
              typeLabel
            }}</span>
          </div>
          <div class="dc-text">{{ card.frontText }}</div>
          <div class="dc-hint">点击翻转查看答案</div>
        </div>
        <div class="dc-back">
          <div class="dc-label">答案</div>
          <div class="dc-text">{{ card.backText }}</div>
          <div v-if="card.knowledgeNodeName" class="dc-knowledge">{{ card.knowledgeNodeName }}</div>
          <button class="dc-next-btn" @click.stop="nextCard">
            {{ loading ? '加载中...' : '下一张 →' }}
          </button>
        </div>
      </div>
    </div>
    <div class="dc-stats">
      <span>已完成 {{ card.todayReviewed || 0 }}/{{ card.dailyGoal || 5 }}</span>
      <span v-if="card.streakDays > 0"> · 连续打卡 {{ card.streakDays }} 天 🔥</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { getDailyCard, rateFlashcard } from '@/api/knowledgeBase';

const card = ref(null);
const isFlipped = ref(false);
const loading = ref(false);

const TYPE_LABELS = {
  DEFINITION: '📖 概念',
  PROCEDURE: '📋 步骤',
  COMPARISON: '⚖️ 对比',
  APPLICATION: '✏️ 解题',
  SCENARIO: '🎯 场景',
};
const typeLabel = computed(() => TYPE_LABELS[card.value?.cardType] || '');

// 刷新统计数字（从后端获取准确的 todayReviewed）
const refreshStats = async () => {
  try {
    const res = await getDailyCard();
    if (res.code === 200 && res.data) {
      if (card.value) {
        card.value.todayReviewed = res.data.todayReviewed || 0;
        card.value.streakDays = res.data.streakDays || 0;
      }
    }
  } catch {
    /* 静默 */
  }
};

// 翻转：先记录复习，再刷新统计
const flip = async () => {
  if (loading.value) return;
  isFlipped.value = !isFlipped.value;

  // 翻到背面 → 记录复习（等后端写入完成）
  if (isFlipped.value && card.value?.cardId) {
    loading.value = true;
    try {
      await rateFlashcard(card.value.cardId, 3);
      await refreshStats();
    } catch {
      /* 静默 */
    }
    loading.value = false;
  }
};

// 加载下一张卡（当前卡已记录复习，后端自动排除）
const nextCard = async () => {
  loading.value = true;
  try {
    const res = await getDailyCard();
    if (res.code === 200 && res.data && res.data.cardId) {
      card.value = res.data;
      isFlipped.value = false;
    }
  } catch {
    /* 静默 */
  }
  loading.value = false;
};

onMounted(async () => {
  try {
    const res = await getDailyCard();
    if (res.code === 200 && res.data && res.data.cardId) {
      card.value = res.data;
    }
  } catch {
    /* 静默失败，不阻塞首页 */
  }
});
</script>

<style scoped lang="scss">
.daily-card-wrapper {
  background: var(--bg-card);
  border: 0.5px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 16px 20px;
}

.daily-card {
  cursor: pointer;
  height: 160px;
  perspective: 800px;
}

.dc-inner {
  position: relative;
  width: 100%;
  height: 100%;
  transform-style: preserve-3d;
  transition: transform 0.5s ease;

  .flipped & {
    transform: rotateY(180deg);
  }
}

.dc-front,
.dc-back {
  position: absolute;
  inset: 0;
  backface-visibility: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 8px;
  border-radius: var(--radius-md);
}

.dc-front {
  background: linear-gradient(135deg, var(--primary-light), var(--bg-card));
  border: 1px solid rgba(67, 97, 238, 0.15);
}

.dc-back {
  background: linear-gradient(135deg, var(--success-light), var(--bg-card));
  border: 1px solid rgba(16, 185, 129, 0.15);
  transform: rotateY(180deg);
}

.dc-subject {
  font-size: var(--fs-xs);
  color: var(--primary-color);
  font-weight: 600;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
  flex-wrap: wrap;
}

.dc-type-badge {
  font-size: 0.7rem;
  font-weight: 500;
  padding: 1px 8px;
  border-radius: 10px;
  background: var(--primary-light, rgba(67, 97, 238, 0.08));
  color: var(--primary-color);
}

.dc-text {
  font-size: var(--fs-lg);
  font-weight: 700;
  color: var(--text-primary);
  text-align: center;
  line-height: 1.5;
}

.dc-hint {
  margin-top: 10px;
  font-size: var(--fs-xs);
  color: var(--text-disabled);
}

.dc-label {
  font-size: var(--fs-xs);
  color: var(--el-color-success);
  font-weight: 600;
  margin-bottom: 8px;
}

.dc-knowledge {
  margin-top: 10px;
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  background: var(--bg-secondary);
  padding: 2px 10px;
  border-radius: var(--radius-full);
}

.dc-next-btn {
  margin-top: 12px;
  padding: 6px 20px;
  font-size: var(--fs-sm);
  color: #fff;
  background: var(--primary-color);
  border: none;
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: opacity 0.2s;
  &:hover {
    opacity: 0.85;
  }
}

.dc-stats {
  margin-top: 10px;
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  text-align: center;
}
</style>

<template>
  <div class="ss-activity">
    <div class="ss-activity-header">
      <span class="ss-activity-title">互动记录</span>
      <div class="ss-activity-filter">
        <el-button
          v-for="f in logFilters"
          :key="f.key"
          size="small"
          :type="logFilter === f.key ? 'primary' : ''"
          :plain="logFilter !== f.key"
          @click="$emit('update:logFilter', f.key)"
        >
          {{ f.label }}
        </el-button>
      </div>
    </div>
    <div class="ss-activity-body">
      <div v-if="!filteredLogs.length" class="ss-activity-empty">
        暂无记录，点击上方卡片开始互动
      </div>
      <TransitionGroup
        v-else
        name="log-in"
        tag="div"
        class="ss-activity-items"
      >
        <div
          v-for="l in visibleLogs"
          :key="l.id"
          class="ss-activity-item"
          :class="{ expanded: l._expanded }"
          @click="expandedLogIds.has(l.id) ? expandedLogIds.delete(l.id) : expandedLogIds.add(l.id)"
        >
          <span class="ss-ai-time">{{ fmtTime(l.createdAt) }}</span>
          <span class="ss-ai-type" :class="'type-' + l.sessionType.toLowerCase()">{{
            typeLabel(l.sessionType)
          }}</span>
          <span class="ss-ai-text">{{ logSummary(l) }}</span>
          <span v-if="l._resultLabel" class="ss-ai-result" :class="l._resultClass">{{
            l._resultLabel
          }}</span>
          <el-icon class="ss-ai-expand"><ArrowRight /></el-icon>
          <div v-if="l._expanded" class="ss-ai-detail">
            <div v-if="l.questionText" class="ss-ai-detail-row">
              <span class="ss-ai-detail-label">题目：</span>{{ l.questionText }}
            </div>
            <div v-if="l.teacherName" class="ss-ai-detail-row">
              <span class="ss-ai-detail-label">教师：</span>{{ l.teacherName }}
            </div>
            <div v-if="l.participantCount != null" class="ss-ai-detail-row">
              <span class="ss-ai-detail-label">参与：</span>{{ l.participantCount }}人
            </div>
          </div>
        </div>
      </TransitionGroup>
      <div v-if="hasMoreLogs" class="ss-activity-more">
        <button class="qp-link" @click="loadMoreLogs">
          加载更多 ({{ filteredLogs.length - visibleLogs.length }}条)
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { ArrowRight } from '@element-plus/icons-vue';
import dayjs from 'dayjs';

const props = defineProps({
  logs: { type: Array, default: () => [] },
  logFilter: { type: String, default: 'ALL' },
});

defineEmits(['update:logFilter']);

const logFilters = [
  { key: 'ALL', label: '全部' },
  { key: 'QUIZ', label: '抽问' },
  { key: 'BUZZ', label: '抢答' },
  { key: 'POLL', label: '投票' },
];

const typeLabel = (t) => ({ QUIZ: '抽问', BUZZ: '抢答', POLL: '投票' })[t] || t;

const fmtTime = (t) => (t ? dayjs(t).format('HH:mm:ss') : '');

const resultLabel = (l) => {
  if (l.sessionType === 'QUIZ') {
    if (l.scoreEarned > 0) return { label: '✓', cls: 'correct' };
    if (l.scoreEarned === 0 && l.result != null) return { label: '✗', cls: 'wrong' };
    return null;
  }
  if (l.sessionType === 'BUZZ') {
    if (l.scoreEarned > 0) return { label: '✓', cls: 'correct' };
    if (l.scoreEarned === 0 && l.result != null) return { label: '✗', cls: 'wrong' };
    return null;
  }
  if (l.sessionType === 'POLL') return { label: `${l.totalVotes || 0}票`, cls: 'votes' };
  return null;
};

const logSummary = (l) => {
  if (l.sessionType === 'QUIZ') {
    const name = l.winnerName || '学生';
    return `抽中了 ${name}`;
  }
  if (l.sessionType === 'BUZZ') {
    const name = l.winnerName || '等待中';
    return `${name} 抢到`;
  }
  if (l.sessionType === 'POLL') return (l.questionText || '').slice(0, 24);
  return l.sessionType;
};

const logDisplayCount = ref(20);
const expandedLogIds = ref(new Set());

const filteredLogs = computed(() => {
  const enriched = props.logs.map((l) => {
    const r = resultLabel(l);
    return {
      ...l,
      _resultLabel: r?.label || '',
      _resultClass: r?.cls || '',
      _expanded: expandedLogIds.value.has(l.id),
    };
  });
  if (props.logFilter === 'ALL') return enriched;
  return enriched.filter((l) => l.sessionType === props.logFilter);
});

const visibleLogs = computed(() => filteredLogs.value.slice(0, logDisplayCount.value));
const hasMoreLogs = computed(() => filteredLogs.value.length > logDisplayCount.value);
const loadMoreLogs = () => {
  logDisplayCount.value += 20;
};
</script>

<style scoped lang="scss">
$quiz-color: var(--primary-color);
$buzz-color: #f59e0b;
$vote-color: #06b6d4;

.ss-activity {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--bg-card);
  border: 0.5px solid var(--border-color);
  border-radius: var(--radius-lg);
  overflow: hidden;
  min-height: 0;
}

.ss-activity-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 0.5px solid var(--border-light);
  flex-shrink: 0;
  flex-wrap: wrap;
  gap: 8px;
}

.ss-activity-title {
  font-size: var(--fs-md);
  font-weight: 700;
  color: var(--text-primary);
}

.ss-activity-filter {
  display: flex;
  gap: 4px;
}

.ss-activity-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px 12px;
}

.ss-activity-empty {
  text-align: center;
  padding: 40px 0;
  font-size: var(--fs-sm);
  color: var(--text-secondary);
}

.ss-activity-more {
  text-align: center;
  padding: 8px 0;
}

.ss-activity-items {
  display: flex;
  flex-direction: column;
}

.ss-activity-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background 0.15s ease;
  font-size: var(--fs-md);

  &:hover {
    background: var(--bg-hover);
  }

  &.expanded .ss-ai-expand {
    transform: rotate(90deg);
  }
}

.log-in-enter-active {
  transition: all 0.25s ease-out;
}
.log-in-enter-from {
  opacity: 0;
  transform: translateX(-8px);
}

.ss-ai-time {
  font-size: var(--fs-xs);
  color: var(--text-disabled);
  font-family: 'JetBrains Mono', 'SF Mono', monospace;
  flex-shrink: 0;
  width: 60px;
}

.ss-ai-type {
  font-size: var(--fs-xs);
  font-weight: 600;
  padding: 1px 8px;
  border-radius: var(--radius-xs);
  flex-shrink: 0;

  &.type-quiz {
    background: rgba($quiz-color, 0.08);
    color: $quiz-color;
  }
  &.type-buzz {
    background: rgba($buzz-color, 0.1);
    color: #d97706;
  }
  &.type-poll {
    background: rgba($vote-color, 0.1);
    color: #058ca3;
  }
}

.ss-ai-text {
  flex: 1;
  color: var(--text-regular);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ss-ai-result {
  font-size: var(--fs-xs);
  font-weight: 700;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  flex-shrink: 0;

  &.correct {
    background: var(--bg-success-light);
    color: var(--el-color-success);
  }
  &.wrong {
    background: var(--bg-danger-light);
    color: var(--el-color-danger);
  }
  &.votes {
    background: var(--bg-secondary);
    color: var(--text-secondary);
  }
}

.ss-ai-expand {
  font-size: var(--fs-xs);
  color: var(--text-disabled);
  transition: transform 0.2s ease;
  flex-shrink: 0;
}

.ss-ai-detail {
  width: 100%;
  padding: 8px 0 4px;
  border-top: 0.5px solid var(--border-light);
  margin-top: 6px;
}

.ss-ai-detail-row {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  line-height: 1.6;
}

.ss-ai-detail-label {
  font-weight: 600;
  color: var(--text-regular);
}

.qp-link {
  background: none;
  border: none;
  color: var(--primary-color);
  cursor: pointer;
  font-size: var(--fs-sm);
  padding: 4px 8px;

  &:hover {
    text-decoration: underline;
  }
}
</style>

<template>
  <div class="overview-page">
    <div class="overview-header">
      <h3>📊 总览看板</h3>
      <div class="header-actions">
        <span v-if="lastRefresh" class="refresh-hint">更新于 {{ lastRefresh }}</span>
        <el-button
          size="small"
          :loading="loading"
          @click="loadData"
        >
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
      </div>
    </div>

    <!-- A. 实时状态卡片 -->
    <h4 class="group-title">🟢 实时状态</h4>
    <div class="card-grid cols-3">
      <div class="stat-card" style="--accent: var(--success-color)">
        <div class="card-icon" style="background: var(--bg-success-light)">
          <el-icon size="24" color="var(--success-color)"><User /></el-icon>
        </div>
        <div class="card-body">
          <span class="card-val">{{ fmt(data.onlineUsers) }}</span><span class="card-lbl">近15分钟活跃用户</span>
        </div>
      </div>
      <div class="stat-card" style="--accent: var(--primary-color)">
        <div class="card-icon" style="background: var(--primary-light)">
          <el-icon size="24" color="var(--primary-color)"><TrendCharts /></el-icon>
        </div>
        <div class="card-body">
          <span class="card-val">{{ fmt(data.todayLogins) }}</span><span class="card-lbl">今日登录人次</span>
        </div>
      </div>
      <div class="stat-card" style="--accent: var(--warning-color)">
        <div class="card-icon" style="background: var(--bg-warning-light)">
          <el-icon size="24" color="var(--el-color-warning)"><Clock /></el-icon>
        </div>
        <div class="card-body">
          <span class="card-val small">{{ data.uptime || '-' }}</span><span class="card-lbl">系统运行时间</span>
        </div>
      </div>
    </div>

    <!-- B. 核心业务概览 -->
    <h4 class="group-title">📝 核心业务概览</h4>
    <div class="card-grid cols-4">
      <div class="stat-card" style="--accent: var(--danger-color)">
        <div class="card-icon" style="background: var(--bg-danger-light)">
          <el-icon size="24" color="var(--danger-color)"><Document /></el-icon>
        </div>
        <div class="card-body">
          <span class="card-val">{{ fmt(data.ongoingExams) }}</span><span class="card-lbl">进行中任务</span>
        </div>
        <div class="card-detail">
          <span class="detail-badge is-info">今日发布 {{ fmt(data.todayExamsCreated) }}</span>
          <span class="detail-badge is-danger">待批改 {{ fmt(data.pendingExamGrades) }}</span>
        </div>
      </div>
      <div class="stat-card" style="--accent: var(--success-color)">
        <div class="card-icon" style="background: var(--bg-success-light)">
          <el-icon size="24" color="var(--success-color)"><EditPen /></el-icon>
        </div>
        <div class="card-body">
          <span class="card-val">{{ fmt(data.ungradedSubmissions) }}</span><span class="card-lbl">待批改任务</span>
        </div>
        <div class="card-detail">
          <span class="detail-badge is-info">今日布置 {{ fmt(data.todayHomeworkAssigned) }}</span>
          <span class="detail-badge is-warning">逾期 {{ fmt(data.overdueSubmissions) }}</span>
        </div>
      </div>
      <div class="stat-card" style="--accent: var(--primary-color)">
        <div class="card-icon" style="background: var(--primary-light)">
          <el-icon size="24" color="var(--primary-color)"><ChatDotSquare /></el-icon>
        </div>
        <div class="card-body">
          <span class="card-val">{{ fmt(data.todayBbsPosts) }}</span><span class="card-lbl">今日发帖</span>
        </div>
        <div class="card-detail">
          <span class="detail-badge is-success">回复 {{ fmt(data.todayBbsReplies) }}</span>
        </div>
      </div>
      <div class="stat-card" style="--accent: var(--warning-color)">
        <div class="card-icon" style="background: var(--bg-warning-light)">
          <el-icon size="24" color="var(--warning-color)"><Coin /></el-icon>
        </div>
        <div class="card-body">
          <span class="card-val">{{ fmt(data.todayCreditsAwarded) }}</span><span class="card-lbl">今日发放积分</span>
        </div>
        <div class="card-detail">
          <span class="detail-badge is-warning">兑换 {{ fmt(data.todayRedeemCount) }} 次</span>
        </div>
      </div>
    </div>

    <!-- C. 近7日活跃趋势 -->
    <h4 class="group-title">📈 近7日活跃用户趋势</h4>
    <div class="chart-card">
      <div v-if="trendBars.length" class="bar-chart">
        <div
          v-for="(bar, i) in trendBars"
          :key="i"
          class="bar-col"
          :style="{ animationDelay: i * 40 + 'ms' }"
        >
          <span class="bar-val">{{ bar.count }}</span>
          <div class="bar-track">
            <div class="bar-fill" :style="{ height: bar.pct + '%' }"></div>
          </div>
          <span class="bar-label">{{ bar.date }}</span>
        </div>
      </div>
      <EmptyState v-else variant="no-data" description="暂无趋势数据，请稍后刷新" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { ElMessage } from 'element-plus';
import { getAdminOverview } from '@/api/system';
import EmptyState from '@/components/common/EmptyState.vue';

const data = ref({});
const loading = ref(false);
const lastRefresh = ref('');
let timer = null;

const fmt = (v) => {
  if (v == null) return '0';
  return Number(v).toLocaleString();
};

const trendBars = computed(() => {
  const trend = data.value.weeklyActiveTrend || [];
  if (!trend.length) return [];
  const max = Math.max(...trend.map((t) => t.count || 0), 1);
  return trend.map((t) => ({
    date: t.date,
    count: t.count,
    pct: Math.round(((t.count || 0) * 100) / max),
  }));
});

const loadData = async () => {
  loading.value = true;
  try {
    const r = await getAdminOverview();
    if (r.code === 200) {
      data.value = r.data || {};
      const now = new Date();
      lastRefresh.value = now.toLocaleTimeString('zh-CN', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
      });
    } else {
      ElMessage.error(r.message || '加载失败');
    }
  } catch {
    ElMessage.error('获取看板数据失败');
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadData();
  timer = setInterval(loadData, 60000); // 60秒自动刷新
});
onUnmounted(() => {
  if (timer) clearInterval(timer);
});
</script>

<style scoped lang="scss">
.overview-page {
  max-width: 1100px;
}

/* B1 新增：Header Banner 渐变（浅主色，不抢眼但立专业感） */
.overview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: var(--spacing-md) var(--spacing-lg);
  border-radius: var(--radius-lg);
  background:
    linear-gradient(135deg, rgba(67, 97, 238, 0.08) 0%, rgba(76, 201, 240, 0.06) 100%),
    var(--bg-card);
  border: 1px solid var(--border-light);
  position: relative;
  overflow: hidden;
  h3 {
    margin: 0;
    font-size: var(--fs-2xl, 22px);
    font-weight: 600;
    letter-spacing: -0.01em;
  }
  h3::after {
    content: '';
    display: inline-block;
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--success-color);
    margin-left: 10px;
    margin-bottom: 4px;
    box-shadow: 0 0 0 3px rgba(82, 196, 26, 0.15);
  }
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.refresh-hint {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}

/* B1 新增：分组标题 + 左侧语义色条 */
.group-title {
  font-size: var(--fs-md);
  font-weight: 600;
  margin: 20px 0 12px;
  color: var(--text-primary);
  padding-left: 10px;
  border-left: 3px solid var(--primary-color);
  letter-spacing: 0.01em;
  line-height: var(--lh-tight);
}

/* B1 新增：Fluid Grid（auto-fill 自适应，≤500px 自动 2 列，大屏自然分栏） */
.card-grid {
  display: grid;
  gap: var(--spacing-md);
  &.cols-3 {
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  }
  &.cols-4 {
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  }
}

/* B2 新增：stat-card 色条 + tabular-nums 数字对齐 + 升起动效 */
.stat-card {
  position: relative;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--spacing-md);
  padding-left: calc(var(--spacing-md) + 6px);
  box-shadow: var(--shadow-sm);
  transition:
    transform var(--transition-base),
    box-shadow var(--transition-base);
  animation: card-rise var(--dur-slow) var(--ease-enter) both;
  /* 左侧语义色条（B2）：继承父元素 --accent */
  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 14px;
    bottom: 14px;
    width: 4px;
    border-radius: 0 4px 4px 0;
    background: var(--accent, var(--primary-color));
    opacity: 0.9;
  }
  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--shadow-md);
  }

  .card-icon {
    width: 44px;
    height: 44px;
    border-radius: var(--radius-md);
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 10px;
  }
  /* B2 新增：数字 tabular + 固定行高，确保 1 和 8 宽度一致不跳动 */
  .card-val {
    font-size: 28px;
    font-weight: 700;
    color: var(--text-primary);
    display: block;
    line-height: 1.15;
    letter-spacing: -0.02em;
    font-variant-numeric: tabular-nums;
    font-feature-settings: 'tnum';
    &.small {
      font-size: var(--fs-lg);
    }
  }
  .card-lbl {
    font-size: var(--fs-xs);
    color: var(--text-secondary);
    margin-top: 4px;
    display: block;
    line-height: var(--lh-normal);
  }
  /* B1 新增：card-detail 徽章胶囊化（4 种状态色） */
  .card-detail {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
    margin-top: 12px;
    padding-top: 12px;
    border-top: 1px dashed var(--border-light);
    font-size: var(--fs-xs);
  }
}

/* B1 新增：detail-badge 胶囊徽章（与 EmptyState 按钮色板一致） */
.detail-badge {
  display: inline-flex;
  align-items: center;
  padding: 3px 9px;
  border-radius: 999px;
  line-height: var(--lh-tight);
  font-weight: 500;
  background: var(--bg-section);
  color: var(--text-secondary);
  &.is-info {
    background: var(--primary-light);
    color: var(--primary-color);
  }
  &.is-success {
    background: var(--bg-success-light);
    color: var(--success-color);
  }
  &.is-warning {
    background: var(--bg-warning-light);
    color: var(--warning-color);
  }
  &.is-danger {
    background: var(--bg-danger-light);
    color: var(--danger-color);
  }
}

@keyframes card-rise {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* —— 柱状图卡片（B1: 动画增强 + 渐变更柔和）—— */
.chart-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 24px 20px 16px;
  box-shadow: var(--shadow-sm);
  animation: card-rise var(--dur-slow) var(--ease-enter) 60ms both;
}
.bar-chart {
  display: flex;
  justify-content: space-around;
  align-items: flex-end;
  gap: 8px;
  height: 180px;
}
.bar-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  flex: 1;
  max-width: 60px;
  /* B1 新增：stagger 入场（通过 animationDelay 内联控制不同的 delay） */
  animation: col-rise var(--dur-slow) var(--ease-enter) both;
}
@keyframes col-rise {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
.bar-val {
  font-size: var(--fs-xs);
  font-weight: 600;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.bar-track {
  flex: 1;
  width: 100%;
  background: var(--bg-section);
  border-radius: 6px 6px 0 0;
  display: flex;
  align-items: flex-end;
  overflow: hidden;
  position: relative;
}
.bar-fill {
  width: 100%;
  background: linear-gradient(
    to top,
    var(--primary-color),
    color-mix(in srgb, var(--primary-color) 55%, white)
  );
  border-radius: 6px 6px 0 0;
  transition: height var(--dur-base) var(--ease-standard);
  min-height: 4px;
  /* B1 新增：填充层次光效 */
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.2);
}
.bar-label {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}

/* B4/B1 新增：无障碍降级 */
@media (prefers-reduced-motion: reduce) {
  .stat-card,
  .chart-card,
  .bar-col {
    animation: none !important;
  }
  .stat-card:hover {
    transform: none;
  }
  .bar-fill {
    transition: none;
  }
}

@media (max-width: 768px) {
  .overview-page {
    max-width: 100%;
  }
  .overview-header {
    padding: var(--spacing-sm) var(--spacing-md);
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-sm);
    h3 {
      font-size: var(--fs-xl);
    }
  }
  .stat-card {
    padding: var(--spacing-sm) var(--spacing-md) var(--spacing-sm) calc(var(--spacing-md) + 6px);
  }
  .stat-card .card-val {
    font-size: 22px;
  }
  .bar-chart {
    height: 140px;
  }
}

@media (max-width: 480px) {
  .card-grid {
    grid-template-columns: repeat(2, 1fr) !important;
  }
}
</style>

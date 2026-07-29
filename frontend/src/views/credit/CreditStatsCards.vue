<template>
  <!-- 积分卡片 -->
  <el-row :gutter="20" class="mb-20">
    <el-col :xs="24" :sm="8">
      <div class="stat-card credits">
        <div class="card-icon"><el-icon size="40"><Coin /></el-icon></div>
        <div class="card-body">
          <span class="label">当前积分</span>
          <span class="value">{{ info.totalCredits || 0 }}</span>
        </div>
      </div>
    </el-col>
    <el-col :xs="24" :sm="8">
      <div class="stat-card title">
        <div class="card-icon"><el-icon size="40"><Medal /></el-icon></div>
        <div class="card-body">
          <span class="label">当前称号</span>
          <span class="value">{{ info.titleName || '未定级' }}</span>
        </div>
      </div>
    </el-col>
    <el-col :xs="24" :sm="8">
      <div class="stat-card streak">
        <div class="card-icon"><el-icon size="40"><TrendCharts /></el-icon></div>
        <div class="card-body">
          <span class="label">连续签到</span>
          <span class="value">{{ info.currentStreak || 0 }} 天</span>
        </div>
      </div>
    </el-col>
  </el-row>

  <!-- 签到 + 今日统计 -->
  <el-row :gutter="20" class="mb-20">
    <el-col :xs="24" :sm="12">
      <div class="page-card sign-card">
        <div class="card-header-custom">
          <h3>每日签到</h3>
          <el-tag v-if="info.signedToday" type="success">今日已签</el-tag>
        </div>
        <div class="sign-body">
          <div class="streak-info">
            <span class="day-number">{{ info.currentStreak || 0 }}</span>
            <span class="day-text">天连续签到</span>
          </div>
          <div class="sign-rewards">
            <div class="reward-item" :class="{ active: info.currentStreak >= 1 }">第1天<br><small>+3</small></div>
            <div class="reward-item" :class="{ active: info.currentStreak >= 3 }">第3天<br><small>+5</small></div>
            <div class="reward-item" :class="{ active: info.currentStreak >= 7 }">第7天<br><small>+10</small></div>
            <div class="reward-item" :class="{ active: info.currentStreak >= 30 }">第30天<br><small>+20</small></div>
          </div>
          <el-button
            type="primary"
            size="large"
            :disabled="info.signedToday"
            :loading="signing"
            class="sign-btn"
            @click="handleSignIn"
          >
            {{ info.signedToday ? '已签到' : '点击签到' }}
          </el-button>
        </div>
      </div>
    </el-col>

    <el-col :xs="24" :sm="12">
      <div class="page-card">
        <div class="card-header-custom">
          <h3>今日统计</h3>
        </div>
        <div class="today-stats">
          <div class="stat-row">
            <span>今日获得</span>
            <span class="text-success">+{{ info.todayEarned || 0 }}</span>
          </div>
          <div class="stat-row">
            <span>今日消费</span>
            <span class="text-danger">-{{ info.todaySpent || 0 }}</span>
          </div>
          <el-divider />
          <div class="stat-row total">
            <span>总积分</span>
            <span class="text-warning">{{ info.totalCredits || 0 }}</span>
          </div>
        </div>

        <el-divider />
        <h4 class="mt-10">称号升级进度</h4>
        <div v-for="t in titleLevels" :key="t.id" class="title-progress-item">
          <span>{{ t.levelName }}</span>
          <span class="min-credits">{{ t.minCredits }} 分</span>
          <el-progress
            v-if="isCurrentTitle(t)"
            :percentage="getProgress(t)"
            :color="getProgressColor(t)"
            :stroke-width="8"
          />
        </div>
      </div>
    </el-col>
  </el-row>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { signIn } from '@/api/credit'

const props = defineProps({
  info: { type: Object, default: () => ({}) },
  titleLevels: { type: Array, default: () => [] }
})
const emit = defineEmits(['signed'])

const signing = ref(false)

const isCurrentTitle = (t) => {
  const level = props.info.titleLevel || 0
  return t.levelNumber === level
}

const getProgress = (t) => {
  if (!t.maxCredits) return 100
  const cur = props.info.totalCredits || 0
  const min = t.minCredits || 0
  const max = t.maxCredits
  return Math.min(100, ((cur - min) / (max - min)) * 100)
}

const getProgressColor = (t) => {
  const colors = ['', 'var(--text-secondary)', 'var(--success-color)', 'var(--warning-color)', 'var(--danger-color)', 'var(--primary-color)']
  return colors[t.levelNumber] || 'var(--primary-color)'
}

const handleSignIn = async () => {
  signing.value = true
  try {
    const res = await signIn()
    if (res.code === 200) {
      ElMessage.success(`签到成功！获得 ${res.data.creditEarned} 积分，连续 ${res.data.currentStreak} 天`)
      emit('signed')
    }
  } catch {
    ElMessage.warning('签到失败')
  } finally {
    signing.value = false
  }
}
</script>

<style scoped lang="scss">
.stat-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 20px;
  box-shadow: 0 2px 12px var(--border-color);

  .card-icon { opacity: 0.2; }
  .card-body {
    .label { font-size: var(--fs-md); color: var(--text-secondary); display: block; }
    .value { font-size: var(--fs-3xl); font-weight: bold; color: var(--text-primary); }
  }
  &.credits .value { color: var(--warning-color); }
  &.title .value { color: var(--primary-color); }
  &.streak .value { color: var(--success-color); }
}

.sign-card {
  .sign-body {
    text-align: center;
    padding: 20px 0;

    .streak-info {
      .day-number { font-size: 48px; font-weight: bold; color: var(--primary-color); display: block; }
      .day-text { color: var(--text-secondary); }
    }

    .sign-rewards {
      display: flex; justify-content: center; gap: 20px; margin: 20px 0;

      .reward-item {
        width: 80px; height: 60px;
        background: var(--bg-section); border-radius: var(--radius-md);
        display: flex; flex-direction: column; align-items: center;
        justify-content: center; font-size: var(--fs-xs); color: var(--text-secondary);
        transition: all 0.3s;
        small { color: var(--text-secondary); }
        &.active { background: var(--primary-light); color: var(--primary-color); small { color: var(--primary-color); } }
      }
    }

    .sign-btn { width: 200px; height: 44px; font-size: var(--fs-lg); }
  }
}

.card-header-custom {
  display: flex; justify-content: space-between; align-items: center;
  padding-bottom: 15px; border-bottom: 1px solid var(--border-light); margin-bottom: 15px;
  h3 { margin: 0; font-size: var(--fs-lg); }
}

.today-stats {
  .stat-row {
    display: flex; justify-content: space-between; padding: 8px 0;
    &.total { font-weight: bold; font-size: var(--fs-lg); }
  }
}

.title-progress-item {
  display: flex; align-items: center; gap: 10px; padding: 8px 0;
  .min-credits { color: var(--text-secondary); font-size: var(--fs-xs); white-space: nowrap; width: 50px; }
  :deep(.el-progress) { flex: 1; }
}

.text-success { color: var(--success-color); }
.text-danger { color: var(--danger-color); }
.text-warning { color: var(--warning-color); }
.mb-20 { margin-bottom: 20px; }

@media (max-width: 768px) {
  .page-card {
    padding: var(--spacing-md);
  }

  :deep(.el-form-item .el-input),
  :deep(.el-form-item .el-select) {
    width: 100%;
  }
}
</style>

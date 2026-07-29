<template>
  <div class="inspector-dashboard">
    <div class="page-header">
      <h3 class="page-title">📋 教务巡视监督面板</h3>
      <span class="header-subtitle">{{ greeting }}，以下是全校教学运行概况</span>
    </div>

    <el-alert 
      type="info" 
      :closable="false" 
      style="margin-bottom: 16px;"
    >
      <template #title>
        <span style="font-size:var(--fs-xs);font-weight:normal;">
          📌 本仪表板数据来源于系统内教师的操作记录和学生提交数据，反映<b>系统使用情况</b>，非独立教学质量评估。建议结合外部数据（统考成绩、技能证书等）综合判断。
        </span>
      </template>
    </el-alert>

    <!-- 核心指标卡片 -->
    <div v-if="loading" class="stat-grid">
      <div v-for="i in 4" :key="i" class="stat-card sk-card"><div class="sk-line w-60" /><div class="sk-line w-40" /></div>
    </div>
    <div v-else class="stat-grid">
      <div
        v-for="s in statsCards"
        :key="s.label"
        class="stat-card"
        @click="s.link && $router.push(s.link)"
      >
        <div class="stat-icon" :style="{ background: s.bg }">{{ s.icon }}</div>
        <div class="stat-info">
          <div class="stat-value">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </div>
      </div>
    </div>

    <!-- 任务运行状态 -->
    <div class="section-title">📊 任务运行状态</div>
    <div v-if="loading" class="status-grid">
      <div v-for="i in 4" :key="i" class="page-card sk-card"><div class="sk-line w-60" /><div class="sk-line w-80" /><div class="sk-line w-40" /></div>
    </div>
    <div v-else class="status-grid">
      <div class="page-card">
        <h4>📋 任务概览</h4>
        <div class="status-items">
          <div class="status-item">📝 总任务数 <strong>{{ dash.totalTasks || 0 }}</strong></div>
          <div class="status-item"><span class="dot published"></span>已发布 <strong>{{ dash.publishedTasks || 0 }}</strong></div>
          <div class="status-item"><span class="dot ongoing"></span>进行中 <strong>{{ dash.ongoingTasks || 0 }}</strong></div>
          <div class="status-item">🔒 已关闭 <strong>{{ dash.closedTasks || 0 }}</strong></div>
        </div>
      </div>
      <div class="page-card">
        <h4>📤 提交与批改</h4>
        <div class="status-items">
          <div class="status-item">📨 总提交 <strong>{{ dash.totalSubmissions || 0 }}</strong> 份</div>
          <div class="status-item">⏳ 待批改 <strong>{{ dash.pendingSubmissions || 0 }}</strong> 份</div>
          <div class="status-item">✅ 已批改 <strong>{{ dash.gradedSubmissions || 0 }}</strong> 份</div>
        </div>
      </div>
      <div class="page-card">
        <h4>🔍 审核与互评</h4>
        <div class="status-items">
          <div class="status-item">📋 审核中任务 <strong>{{ dash.pendingReviewTasks || 0 }}</strong></div>
          <div class="status-item">👥 互评总数 <strong>{{ dash.totalPeerReviews || 0 }}</strong></div>
          <div class="status-item">✅ 互评已提交 <strong>{{ dash.submittedPeerReviews || 0 }}</strong></div>
        </div>
      </div>
      <div class="page-card">
        <h4>⭐ 积分与活跃</h4>
        <div class="status-items">
          <div class="status-item">📅 今日签到 <strong>{{ dash.todaySignCount || 0 }}</strong> 人</div>
          <div class="status-item">💬 今日发帖 <strong>{{ dash.todayPosts || 0 }}</strong> 篇</div>
          <div class="status-item">💰 本月发分 <strong>{{ formatNumber(dash.monthCreditsAwarded) }}</strong></div>
          <div class="status-item">🚶 本周巡课 <strong>{{ dash.patrolsThisWeek || 0 }}</strong> 次</div>
          <div class="status-item">📐 纪律均分 <strong>{{ dash.avgDisciplineScore || 0 }}</strong></div>
          <div class="status-item">🌟 德育均分 <strong>{{ dash.avgMoralScore || 0 }}</strong></div>
          <div class="status-item">📚 本月教研 <strong>{{ dash.researchActivitiesThisMonth || 0 }}</strong> 次</div>
        </div>
      </div>
    </div>

    <!-- 预警消息 -->
    <div class="section-title">🔔 未读预警</div>
    <div class="alert-snippet" @click="$router.push('/inspector/alerts')">
      <div v-if="alertsLoading" class="sk-line w-60" style="height:20px" />
      <template v-else>
        <div v-if="alertList.length === 0" class="alert-none">暂无未读预警</div>
        <div v-for="a in alertList" :key="a.id" class="alert-item">
          <span class="alert-icon">⚠️</span>
          <span class="alert-msg">{{ truncate(a.alertMessage, 35) }}</span>
          <span class="alert-time">{{ a.triggedAt }}</span>
        </div>
      </template>
    </div>

    <!-- AI 巡视建议 -->
    <div class="section-title">💡 AI 巡视建议</div>
    <div
      v-loading="aiLoading"
      class="ai-rec-block"
      style="cursor:pointer"
      @click="$router.push('/inspector/ai-assistant')"
    >
      <div v-if="aiRecs.length === 0 && !aiLoading" class="ai-rec-none">点击前往AI助手获取巡视建议</div>
      <div v-for="r in aiRecs" :key="(r.id||'') + (r.name||'')" class="ai-rec-item">
        <el-tag :type="r.type === 'CLASS' ? 'primary' : 'warning'" size="small">{{ r.type === 'CLASS' ? '班级' : r.type === 'TEACHER' ? '教师' : '问题' }}</el-tag>
        <span class="ai-rec-name">{{ r.name }}</span>
        <span class="ai-rec-reason">{{ r.reason }}</span>
      </div>
    </div>

    <!-- 教研质量排行 -->
    <div class="section-title">📊 教研质量排行</div>
    <div v-if="qualityLoading" class="quality-grid">
      <div v-for="i in 3" :key="i" class="quality-card sk-card"><div class="sk-line w-60" /><div class="sk-line w-40" /></div>
    </div>
    <div v-else-if="qualityList.length" class="quality-grid">
      <div v-for="g in qualityList.slice(0, 5)" :key="g.groupId" class="quality-card">
        <div class="quality-rank">{{ g.rank }}</div>
        <div class="quality-info">
          <div class="quality-name">{{ g.groupName }}</div>
          <el-progress :percentage="Math.round(g.totalScore)" :color="qualityColor(g.totalScore)" :stroke-width="10" />
        </div>
        <div class="quality-score">{{ g.totalScore }}分</div>
      </div>
      <el-empty v-if="qualityList.length === 0" description="暂无数据" :image-size="32" />
    </div>

    <!-- 趋势对比 -->
    <div class="section-title">📈 趋势对比（本周 vs 上周）</div>
    <div v-if="trendLoading" class="trend-grid">
      <div v-for="i in 3" :key="i" class="trend-card sk-card"><div class="sk-line w-40" /><div class="sk-line w-60" /></div>
    </div>
    <div v-else class="trend-grid">
      <div class="trend-card">
        <div class="trend-label">提交率</div>
        <div class="trend-value">{{ trendCurrent.submissionRate }}%</div>
        <div :class="['trend-change', trendChangeClass(trendTrend.submissionRateChange) ]">
          {{ trendTrend.submissionRateChange >= 0 ? '↑' : '↓' }} {{ Math.abs(trendTrend.submissionRateChange) }}%
        </div>
      </div>
      <div class="trend-card">
        <div class="trend-label">及格率</div>
        <div class="trend-value">{{ trendCurrent.passRate }}%</div>
        <div :class="['trend-change', trendChangeClass(trendTrend.passRateChange) ]">
          {{ trendTrend.passRateChange >= 0 ? '↑' : '↓' }} {{ Math.abs(trendTrend.passRateChange) }}%
        </div>
      </div>
      <div class="trend-card">
        <div class="trend-label">平均分</div>
        <div class="trend-value">{{ trendCurrent.avgScore }}</div>
        <div :class="['trend-change', trendChangeClass(trendTrend.avgScoreChange) ]">
          {{ trendTrend.avgScoreChange >= 0 ? '↑' : '↓' }} {{ Math.abs(trendTrend.avgScoreChange) }}
        </div>
      </div>
    </div>

    <!-- 审核统计数据 -->
    <div class="section-title">✅ 审核状态</div>
    <div v-if="loading" class="stat-grid">
      <div v-for="i in 4" :key="i" class="stat-card sk-card"><div class="sk-line w-60" /><div class="sk-line w-40" /></div>
    </div>
    <div v-else class="stat-grid">
      <div class="stat-card" style="cursor:default">
        <div class="stat-icon" style="background:#909399">⏳</div>
        <div class="stat-info">
          <div class="stat-value">{{ dash.pendingGroupReviews ?? 0 }}</div>
          <div class="stat-label">待备课审核</div>
        </div>
      </div>
      <div class="stat-card" style="cursor:default">
        <div class="stat-icon" style="background:var(--el-color-warning)">📋</div>
        <div class="stat-info">
          <div class="stat-value">{{ dash.pendingTeachingReviews ?? 0 }}</div>
          <div class="stat-label">待教研审核</div>
        </div>
      </div>
      <div class="stat-card" style="cursor:default">
        <div class="stat-icon" style="background:var(--primary-color)">⏱️</div>
        <div class="stat-info">
          <div class="stat-value">{{ (dash.avgReviewHours ?? 0) + 'h' }}</div>
          <div class="stat-label">平均审核时效</div>
        </div>
      </div>
      <div class="stat-card" style="cursor:default">
        <div class="stat-icon" style="background:var(--el-color-danger)">📊</div>
        <div class="stat-info">
          <div class="stat-value">{{ (dash.rejectionRate ?? 0) + '%' }}</div>
          <div class="stat-label">审核驳回率</div>
        </div>
      </div>
    </div>

    <!-- 年级分布 -->
    <div class="section-title">🏫 年级分布</div>
    <div v-if="gradeList.length" class="grade-bar page-card">
      <div v-for="g in gradeList" :key="g.name" class="grade-item">
        <span class="grade-name">{{ g.name }}</span>
        <div class="grade-bar-track"><div class="grade-bar-fill" :style="{ width: g.percent + '%' }"></div></div>
        <span class="grade-count">{{ g.count }}班</span>
      </div>
    </div>

    <!-- 快捷巡视入口 -->
    <div class="section-title">🔍 快捷巡视入口</div>
    <div class="quick-links">
      <div class="quick-card" @click="$router.push('/inspector/exams')">
        <div class="quick-icon">📝</div>
        <div class="quick-label">任务分析</div>
      </div>
      <div class="quick-card" @click="$router.push('/inspector/teachers')">
        <div class="quick-icon">👨‍🏫</div>
        <div class="quick-label">教师活跃</div>
      </div>
      <div class="quick-card" @click="$router.push('/inspector/classes')">
        <div class="quick-icon">🏫</div>
        <div class="quick-label">班级对比</div>
      </div>
      <div class="quick-card" @click="$router.push('/inspector/alerts')">
        <div class="quick-icon">🔔</div>
        <div class="quick-label">预警中心</div>
      </div>
      <div class="quick-card" @click="$router.push('/inspector/reports')">
        <div class="quick-icon">📈</div>
        <div class="quick-label">巡视报告</div>
      </div>
      <div class="quick-card" @click="$router.push('/inspector/classroom-patrols')">
        <div class="quick-icon">🚶</div>
        <div class="quick-label">课堂巡课</div>
      </div>
      <div class="quick-card" @click="$router.push('/inspector/moral-inspections')">
        <div class="quick-icon">🌟</div>
        <div class="quick-label">德育巡视</div>
      </div>
      <div class="quick-card" @click="$router.push('/inspector/research-activities')">
        <div class="quick-icon">📚</div>
        <div class="quick-label">教研活动</div>
      </div>
      <div class="quick-card" @click="$router.push('/inspector/parent-feedback')">
        <div class="quick-icon">👪</div>
        <div class="quick-label">家长反馈</div>
      </div>
      <div class="quick-card" @click="$router.push('/inspector/ai-assistant')">
        <div class="quick-icon">🤖</div>
        <div class="quick-label">AI助手</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useSettingsStore } from '@/stores/settings'
import { getDashboard, getDashboardTrend, getAlertLogs, getRecommendations, getTeachingGroupQuality } from '@/api/inspector'

const settingsStore = useSettingsStore()

const loading = ref(true)
const dash = ref({})

// ── 趋势 ────────────────────────────────
const trendLoading = ref(false)
const trendCurrent = ref({})
const trendTrend = ref({})
const trendChangeClass = (val) => val >= 0 ? 'trend-up' : 'trend-down'

const loadTrend = async () => {
  trendLoading.value = true
  try {
    const res = await getDashboardTrend('WEEKLY')
    if (res.code === 200) {
      trendCurrent.value = res.data.currentPeriod || {}
      trendTrend.value = res.data.trend || {}
    }
  } catch {}
  finally { trendLoading.value = false }
}

// ── 预警 ────────────────────────────────
const alertsLoading = ref(false)
const alertList = ref([])
const truncate = (s, n) => s && s.length > n ? s.slice(0, n) + '…' : s || ''

const loadAlerts = async () => {
  alertsLoading.value = true
  try {
    const res = await getAlertLogs({ page: 1, size: 5, isRead: false })
    if (res.code === 200) alertList.value = res.data.records || []
  } catch {}
  finally { alertsLoading.value = false }
}

// ── AI 巡视建议 ──────────────────────────
const aiLoading = ref(false)
const aiRecs = ref([])
const loadAiRecs = async () => {
  aiLoading.value = true
  try {
    const res = await getRecommendations()
    if (res.code === 200) aiRecs.value = (res.data.recommendations || []).slice(0, 3)
  } catch {}
  finally { aiLoading.value = false }
}

// ── 教研质量排行 ──────────────────────────
const qualityLoading = ref(false)
const qualityList = ref([])
const qualityColor = (score) => {
  if (score >= 80) return 'var(--el-color-success)'
  if (score >= 60) return 'var(--el-color-warning)'
  return 'var(--el-color-danger)'
}
const loadQuality = async () => {
  qualityLoading.value = true
  try {
    const res = await getTeachingGroupQuality()
    if (res.code === 200) qualityList.value = res.data || []
  } catch {}
  finally { qualityLoading.value = false }
}

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 12) return '早上好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const formatNumber = (n) => {
  if (!n) return '0'
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return n.toLocaleString()
}

const statsCards = computed(() => {
  const d = dash.value
  return [
    { label: '学生总数', value: d.studentCount ?? '…', icon: '👨‍🎓', bg: 'var(--primary-gradient)' },
    { label: '教师总数', value: d.teacherCount ?? '…', icon: '👨‍🏫', bg: 'var(--gradient-green)' },
    { label: '班级总数', value: d.classCount ?? '…', icon: '🏫', bg: 'var(--gradient-orange)' },
    { label: '帖子总数', value: d.bbsPostCount ?? '…', icon: '💬', bg: 'var(--gradient-gray)' },
  ]
})

const gradeList = computed(() => {
  const dist = dash.value.gradeDistribution || {}
  const total = Object.values(dist).reduce((s, c) => s + c, 0) || 1
  return Object.entries(dist).map(([name, count]) => ({
    name, count, percent: Math.round(count * 100 / total)
  }))
})

onMounted(async () => {
  try {
    const [dashRes] = await Promise.all([getDashboard(), loadTrend(), loadAlerts(), loadAiRecs(), loadQuality()])
    if (dashRes.code === 200) dash.value = dashRes.data || {}
    else ElMessage.error(dashRes.message || '加载失败')
  } catch { ElMessage.error('网络异常，请刷新重试') }
  finally { loading.value = false }
})
</script>

<style scoped lang="scss">
.inspector-dashboard { max-width: 1280px; margin: 0 auto; padding: var(--spacing-lg, 24px); }

.page-header {
  display: flex; align-items: baseline; gap: 12px; margin-bottom: 20px;
  .page-title { font-size: var(--fs-2xl, 22px); margin: 0; }
  .header-subtitle { font-size: var(--fs-sm); color: var(--text-secondary); }
}

.stat-grid {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px;
  .stat-card {
    display: flex; align-items: center; gap: 14px; padding: 18px;
    background: var(--bg-card); border-radius: var(--radius-lg, 12px);
    box-shadow: var(--shadow-sm); cursor: pointer; transition: transform 0.15s, box-shadow 0.15s;
    &:hover { transform: translateY(-2px); box-shadow: var(--shadow-md); }
    .stat-icon {
      width: 48px; height: 48px; border-radius: var(--radius-md);
      display: flex; align-items: center; justify-content: center; font-size: 22px; flex-shrink: 0;
    }
    .stat-value { font-size: var(--fs-2xl); font-weight: 700; color: var(--text-primary); }
    .stat-label { font-size: var(--fs-xs); color: var(--text-secondary); margin-top: 2px; }
  }
}

.section-title { font-size: var(--fs-lg); font-weight: 600; margin: 24px 0 12px; color: var(--text-primary); }

.status-grid {
  display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px;
  .page-card {
    padding: 16px; background: var(--bg-card); border-radius: var(--radius-md);
    box-shadow: var(--shadow-sm);
    h4 { font-size: var(--fs-md); margin: 0 0 10px; color: var(--text-primary); }
  }
}

.status-items { display: flex; flex-direction: column; gap: 8px; }
.status-item { font-size: var(--fs-sm); color: var(--text-secondary); }
.status-item strong { color: var(--text-primary); margin-left: 4px; }
.dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-right: 4px; }
.dot.ongoing { background: var(--el-color-success); }
.dot.published { background: var(--primary-color); }

.grade-bar { padding: 16px; }
.grade-item { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; font-size: var(--fs-sm); }
.grade-name { width: 60px; flex-shrink: 0; color: var(--text-secondary); }
.grade-bar-track { flex: 1; height: 18px; background: var(--bg-secondary); border-radius: var(--radius-xs); overflow: hidden; }
.grade-bar-fill { height: 100%; background: var(--primary-gradient); border-radius: var(--radius-xs); min-width: 2px; transition: width 0.6s; }
.grade-count { width: 36px; text-align: right; color: var(--text-regular); }

.quick-links { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 24px; }
.quick-card {
  padding: 20px 12px; text-align: center; background: var(--bg-card);
  border-radius: var(--radius-md); cursor: pointer; box-shadow: var(--shadow-sm);
  transition: transform 0.15s;
  &:hover { transform: translateY(-2px); }
  .quick-icon { font-size: 28px; margin-bottom: 6px; }
  .quick-label { font-size: var(--fs-sm); color: var(--text-regular); }
}

.sk-card { display: flex; flex-direction: column; gap: 8px; padding: 16px; }
.sk-line { height: 14px; background: var(--bg-secondary); border-radius: var(--radius-xs); position: relative; overflow: hidden; }
.sk-line::after { content: ''; position: absolute; inset: 0; background: linear-gradient(90deg,transparent,rgba(255,255,255,0.4),transparent); animation: sk-shimmer 1.6s infinite; }
@keyframes sk-shimmer { 0% { transform: translateX(-100%) } 100% { transform: translateX(100%) } }
.w-40 { width: 40% } .w-60 { width: 60% } .w-80 { width: 80% }

.trend-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; margin-bottom: 24px; }
.trend-card { text-align: center; padding: 18px; background: var(--bg-card); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); }
.trend-label { font-size: var(--fs-sm); color: var(--text-secondary); margin-bottom: 4px; }
.trend-value { font-size: 28px; font-weight: 700; color: var(--text-primary); }
.trend-change { font-size: var(--fs-md); font-weight: 600; margin-top: 4px; &.trend-up { color: var(--success-color); } &.trend-down { color: var(--danger-color); } }

.alert-snippet { padding: 12px 16px; background: var(--bg-card); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); margin-bottom: 8px; cursor: pointer; transition: background 0.15s; &:hover { background: var(--bg-secondary); } }
.alert-none { font-size: var(--fs-sm); color: var(--text-secondary); padding: 8px 0; }
.alert-item { display: flex; align-items: center; gap: 10px; padding: 6px 0; font-size: var(--fs-sm); border-bottom: 1px solid var(--border-color, #eee); &:last-child { border-bottom: none; } }
.alert-icon { flex-shrink: 0; }
.alert-msg { flex: 1; color: var(--text-regular); overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.alert-time { font-size: var(--fs-xs); color: var(--text-secondary); white-space: nowrap; }

.ai-rec-block { padding: 12px 16px; background: var(--bg-card); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); margin-bottom: 8px; }
.ai-rec-none { font-size: var(--fs-sm); color: var(--text-secondary); padding: 8px 0; text-align: center; }
.ai-rec-item { display: flex; align-items: center; gap: 10px; padding: 6px 0; font-size: var(--fs-sm); border-bottom: 1px solid var(--border-color, #eee); &:last-child { border-bottom: none; } }
.ai-rec-name { font-weight: 600; color: var(--text-primary); white-space: nowrap; }
.ai-rec-reason { color: var(--text-secondary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.quality-grid { display: flex; flex-direction: column; gap: 10px; margin-bottom: 24px; }
.quality-card { display: flex; align-items: center; gap: 12px; padding: 12px 16px; background: var(--bg-card); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); }
.quality-rank { width: 24px; height: 24px; border-radius: 50%; background: var(--el-color-primary); color: #fff; display: flex; align-items: center; justify-content: center; font-size: var(--fs-xs); font-weight: 600; flex-shrink: 0; }
.quality-info { flex: 1; .quality-name { font-size: var(--fs-md); font-weight: 500; margin-bottom: 6px; color: var(--text-primary); } }
.quality-score { font-size: var(--fs-lg); font-weight: 700; color: var(--text-primary); white-space: nowrap; }

@media (max-width: 768px) {
  .inspector-dashboard { padding: var(--spacing-md, 16px); }
  .stat-grid { grid-template-columns: repeat(2, 1fr); gap: 10px; }
  .stat-card { padding: 14px; .stat-value { font-size: var(--fs-xl); } }
  .status-grid { grid-template-columns: 1fr; }
  .trend-grid { grid-template-columns: 1fr; }
  .quick-links { grid-template-columns: repeat(2, 1fr); }
}
</style>

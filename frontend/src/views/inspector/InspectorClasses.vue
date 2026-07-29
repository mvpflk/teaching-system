<template>
  <div class="inspector-classes">
    <div class="page-header">
      <h3 class="page-title">🏫 班级综合对比</h3>
      <span class="header-subtitle">多维度班级数据分析与对比</span>
    </div>

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="📝 成绩分析" name="score" />
      <el-tab-pane label="👥 互评质量" name="peer" />
      <el-tab-pane label="⭐ 积分排行" name="credit" />
    </el-tabs>

    <!-- 筛选栏（成绩分析） -->
    <div v-if="activeTab === 'score'" class="filter-bar">
      <el-select
        v-model="filters.grade"
        placeholder="年级"
        clearable
        size="default"
        @change="loadScoreData"
      >
        <el-option
          v-for="g in gradeOptions"
          :key="g"
          :value="g"
          :label="g"
        />
      </el-select>
      <el-select
        v-model="filters.taskId"
        placeholder="选择任务"
        clearable
        filterable
        size="default"
        @change="loadScoreData"
      >
        <el-option
          v-for="t in taskOptions"
          :key="t.id"
          :value="t.id"
          :label="t.title"
        />
      </el-select>
      <el-button size="default" @click="resetScoreFilters"><el-icon><Refresh /></el-icon>重置</el-button>
    </div>

    <!-- 筛选栏（互评） -->
    <div v-if="activeTab === 'peer'" class="filter-bar">
      <el-select
        v-model="peerFilters.grade"
        placeholder="年级"
        clearable
        size="default"
        @change="loadPeerData"
      >
        <el-option
          v-for="g in gradeOptions"
          :key="g"
          :value="g"
          :label="g"
        />
      </el-select>
    </div>

    <!-- 📝 成绩分析 -->
    <template v-if="activeTab === 'score'">
      <div v-if="scoreSummary.classCount" class="summary-row">
        <div class="summary-item"><span class="s-val">{{ scoreSummary.classCount }}</span><span class="s-lbl">班级数</span></div>
        <div class="summary-item"><span class="s-val">{{ scoreSummary.totalGraded }}</span><span class="s-lbl">已批改</span></div>
        <div class="summary-item primary"><span class="s-val">{{ scoreSummary.overallAvgScore }}</span><span class="s-lbl">总均分</span></div>
        <div class="summary-item" :class="(scoreSummary.overallPassRate || 0) >= 60 ? 'good' : 'bad'">
          <span class="s-val">{{ scoreSummary.overallPassRate }}%</span><span class="s-lbl">总及格率</span>
        </div>
      </div>

      <div v-loading="scoreLoading" class="card-grid">
        <div
          v-for="c in scoreClasses"
          :key="c.classId"
          class="data-card"
          @click="openScoreDetail(c)"
        >
          <div class="dc-header">
            <span class="dc-grade">{{ c.grade }}</span>
            <span class="dc-name">{{ c.className }}</span>
          </div>
          <div class="dc-metrics">
            <div class="dm-item primary"><span class="dm-val">{{ c.avgScore }}</span><span class="dm-lbl">平均分</span></div>
            <div class="dm-item"><span class="dm-val">{{ c.maxScore }}</span><span class="dm-lbl">最高</span></div>
            <div class="dm-item"><span class="dm-val">{{ c.minScore }}</span><span class="dm-lbl">最低</span></div>
            <div class="dm-item" :class="(c.passRate || 0) >= 60 ? 'good' : 'bad'">
              <span class="dm-val">{{ c.passRate }}%</span><span class="dm-lbl">及格率</span>
            </div>
          </div>
          <div class="dc-sub">已提交 {{ c.submittedCount }} · 已批改 {{ c.gradedCount }}</div>
          <div class="dc-arrow">查看分布 →</div>
        </div>
        <div v-if="scoreClasses.length === 0 && !scoreLoading" class="empty-full">
          <el-empty description="暂无成绩数据" :image-size="80" />
        </div>
      </div>
    </template>

    <!-- 👥 互评质量 -->
    <template v-if="activeTab === 'peer'">
      <div v-if="peerSummary.totalReviews" class="summary-row">
        <div class="summary-item"><span class="s-val">{{ peerSummary.totalReviews }}</span><span class="s-lbl">互评总数</span></div>
        <div class="summary-item"><span class="s-val">{{ peerSummary.submittedReviews }}</span><span class="s-lbl">已提交</span></div>
      </div>

      <div v-loading="peerLoading" class="card-grid">
        <div v-for="c in peerClasses" :key="c.classId" class="data-card">
          <div class="dc-header">
            <span class="dc-grade">{{ c.grade }}</span>
            <span class="dc-name">{{ c.className }}</span>
          </div>
          <div class="dc-metrics">
            <div class="dm-item primary"><span class="dm-val">{{ c.participationRate }}%</span><span class="dm-lbl">参与率</span></div>
            <div class="dm-item"><span class="dm-val">{{ c.submittedReviews }}/{{ c.totalReviews }}</span><span class="dm-lbl">已交/总数</span></div>
            <div class="dm-item"><span class="dm-val">{{ c.avgScore }}</span><span class="dm-lbl">均分</span></div>
            <div class="dm-item" :class="(c.scoreStdDev || 0) <= 10 ? 'good' : (c.scoreStdDev || 0) <= 20 ? '' : 'bad'">
              <span class="dm-val">{{ c.scoreStdDev }}</span><span class="dm-lbl">离散度</span>
            </div>
          </div>
          <div class="dc-note">离散度越低评分越一致</div>
        </div>
        <div v-if="peerClasses.length === 0 && !peerLoading" class="empty-full">
          <el-empty description="暂无互评数据" :image-size="80" />
        </div>
      </div>
    </template>

    <!-- ⭐ 积分排行 -->
    <template v-if="activeTab === 'credit'">
      <div v-if="creditData.totalIssued" class="summary-row">
        <div class="summary-item"><span class="s-val">{{ formatNumber(creditData.totalIssued) }}</span><span class="s-lbl">累计发放</span></div>
        <div class="summary-item primary"><span class="s-val">{{ formatNumber(creditData.monthIssued) }}</span><span class="s-lbl">本月发放</span></div>
      </div>

      <div v-loading="creditLoading" class="card-grid">
        <div v-for="(c, i) in (creditData.classRanking || [])" :key="c.classId" class="data-card">
          <div class="dc-header">
            <span class="dc-rank">#{{ i + 1 }}</span>
            <span class="dc-grade">{{ c.grade }}</span>
            <span class="dc-name">{{ c.className }}</span>
          </div>
          <div class="dc-metrics">
            <div class="dm-item primary"><span class="dm-val">{{ c.avgCredits }}</span><span class="dm-lbl">人均积分</span></div>
            <div class="dm-item"><span class="dm-val">{{ formatNumber(c.totalCredits) }}</span><span class="dm-lbl">总积分</span></div>
          </div>
        </div>
        <div v-if="(!creditData.classRanking || creditData.classRanking.length === 0) && !creditLoading" class="empty-full">
          <el-empty description="暂无积分数据" :image-size="80" />
        </div>
      </div>

      <!-- 异常积分 -->
      <div v-if="creditData.anomalies && creditData.anomalies.length" class="section-title">⚠️ 异常积分检测（单日超500）</div>
      <div v-if="creditData.anomalies && creditData.anomalies.length" class="anomaly-list">
        <div v-for="a in creditData.anomalies" :key="a.date" class="anomaly-item">
          <span class="anom-date">{{ a.date }}</span>
          <span class="anom-amount">发放 {{ formatNumber(a.amount) }} 分</span>
          <span class="anom-count">{{ a.count }} 笔</span>
        </div>
      </div>
    </template>

    <!-- 班级详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      :title="detailTitle"
      width="550px"
      :close-on-click-modal="false"
      append-to-body
    >
      <div v-if="detailClass" class="class-detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="班级">{{ detailClass.grade }} {{ detailClass.className }}</el-descriptions-item>
          <el-descriptions-item label="平均分">{{ detailClass.avgScore }}</el-descriptions-item>
          <el-descriptions-item label="最高分">{{ detailClass.maxScore }}</el-descriptions-item>
          <el-descriptions-item label="最低分">{{ detailClass.minScore }}</el-descriptions-item>
          <el-descriptions-item label="及格率">{{ detailClass.passRate }}%</el-descriptions-item>
          <el-descriptions-item label="及格人数">{{ detailClass.passCount }}</el-descriptions-item>
          <el-descriptions-item label="已提交">{{ detailClass.submittedCount }}</el-descriptions-item>
          <el-descriptions-item label="已批改">{{ detailClass.gradedCount }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="detailClass.scoreDistribution && detailClass.scoreDistribution.length" class="detail-section">
          <h4>📊 分数分布</h4>
          <div v-for="d in detailClass.scoreDistribution" :key="d.range" class="dist-item">
            <span class="dist-range">{{ d.range }}</span>
            <div class="dist-track"><div class="dist-fill" :style="{ width: maxDetailDist > 0 ? (d.count / maxDetailDist * 100) + '%' : '0%' }"></div></div>
            <span class="dist-count">{{ d.count }}人</span>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getScoreAnalysis, getPeerReviewStats, getCreditStats } from '@/api/inspector'
import { listTasks } from '@/api/task'
import { getGrades } from '@/api/settings'

const activeTab = ref('score')
const gradeOptions = ref([])
const taskOptions = ref([])

// ── 成绩分析 ──────────────────────────────
const scoreLoading = ref(false)
const scoreClasses = ref([])
const scoreSummary = ref({})
const filters = reactive({ grade: '', taskId: null })

const loadScoreData = async () => {
  scoreLoading.value = true
  try {
    const params = {}
    if (filters.grade) params.grade = filters.grade
    if (filters.taskId) params.taskId = filters.taskId
    const res = await getScoreAnalysis(params)
    if (res.code === 200) {
      scoreClasses.value = res.data.classes || []
      scoreSummary.value = res.data.summary || {}
    }
  } catch { ElMessage.error('加载失败') }
  finally { scoreLoading.value = false }
}

const resetScoreFilters = () => {
  filters.grade = ''; filters.taskId = null
  loadScoreData()
}

// ── 互评统计 ──────────────────────────────
const peerLoading = ref(false)
const peerClasses = ref([])
const peerSummary = ref({})
const peerFilters = reactive({ grade: '' })

const loadPeerData = async () => {
  peerLoading.value = true
  try {
    const params = {}
    if (peerFilters.grade) params.grade = peerFilters.grade
    const res = await getPeerReviewStats(params)
    if (res.code === 200) {
      peerClasses.value = res.data.classes || []
      peerSummary.value = res.data.summary || {}
    }
  } catch { ElMessage.error('加载失败') }
  finally { peerLoading.value = false }
}

// ── 积分排行 ──────────────────────────────
const creditLoading = ref(false)
const creditData = ref({})

const loadCreditData = async () => {
  creditLoading.value = true
  try {
    const res = await getCreditStats()
    if (res.code === 200) creditData.value = res.data || {}
  } catch { ElMessage.error('加载失败') }
  finally { creditLoading.value = false }
}

// ── 详情对话框 ────────────────────────────
const detailVisible = ref(false)
const detailClass = ref(null)
const detailTitle = computed(() => {
  if (!detailClass.value) return ''
  return `${detailClass.value.grade} ${detailClass.value.className} — 成绩详情`
})
const maxDetailDist = computed(() => {
  const dist = detailClass.value?.scoreDistribution
  if (!dist || !dist.length) return 0
  return Math.max(...dist.map(d => d.count), 1)
})

const openScoreDetail = (c) => {
  detailClass.value = c
  detailVisible.value = true
}

const formatNumber = (n) => {
  if (!n) return '0'
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return n.toLocaleString()
}

const onTabChange = (tab) => {
  if (tab === 'score') loadScoreData()
  else if (tab === 'peer') loadPeerData()
  else if (tab === 'credit') loadCreditData()
}
onMounted(async () => {
  loadScoreData()
  try {
    const [gradeRes, taskRes] = await Promise.all([
      getGrades(),
      listTasks({ size: 999 })
    ])
    if (gradeRes.code === 200) gradeOptions.value = (gradeRes.data || []).map(g => g.gradeName)
    if (taskRes.code === 200) taskOptions.value = (taskRes.data.records || []).filter(t => t.status !== 'DRAFT')
  } catch { ElMessage.error('加载失败') }
})
</script>

<style scoped lang="scss">
.inspector-classes { max-width: 1280px; margin: 0 auto; padding: var(--spacing-lg, 24px); }

.page-header {
  display: flex; align-items: baseline; gap: 12px; margin-bottom: 4px;
  .page-title { font-size: var(--fs-2xl, 22px); margin: 0; }
  .header-subtitle { font-size: var(--fs-sm); color: var(--text-secondary); }
}

.filter-bar { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 20px; }

.summary-row {
  display: flex; gap: 16px; flex-wrap: wrap; margin-bottom: 20px;
  .summary-item {
    padding: 14px 20px; background: var(--bg-card); border-radius: var(--radius-md);
    text-align: center; box-shadow: var(--shadow-sm); min-width: 90px;
    .s-val { display: block; font-size: 22px; font-weight: 700; color: var(--text-primary); }
    .s-lbl { display: block; font-size: var(--fs-xs); color: var(--text-secondary); margin-top: 2px; }
    &.primary .s-val { color: var(--primary-color); }
    &.good .s-val { color: var(--success-color); }
    &.bad .s-val { color: var(--danger-color); }
  }
}

.section-title { font-size: var(--fs-md); font-weight: 600; margin: 20px 0 10px; color: var(--text-primary); }

.card-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 14px;
  .data-card {
    background: var(--bg-card); border-radius: var(--radius-lg); padding: 18px;
    box-shadow: var(--shadow-sm); cursor: pointer; transition: all 0.2s;
    &:hover { transform: translateY(-2px); box-shadow: var(--shadow-base); }
    .dc-header { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
    .dc-rank { font-size: var(--fs-lg); font-weight: 800; color: var(--primary-color); }
    .dc-grade { font-size: var(--fs-xs); color: var(--primary-color); background: var(--primary-light); padding: 2px 8px; border-radius: var(--radius-xs); }
    .dc-name { font-size: var(--fs-md); font-weight: 600; color: var(--text-primary); }
    .dc-metrics { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; margin-bottom: 8px; }
    .dm-item { text-align: center;
      .dm-val { display: block; font-size: var(--fs-lg); font-weight: 700; color: var(--text-primary); }
      .dm-lbl { display: block; font-size: var(--fs-xs); color: var(--text-secondary); }
      &.primary .dm-val { color: var(--primary-color); }
      &.good .dm-val { color: var(--success-color); }
      &.bad .dm-val { color: var(--danger-color); }
    }
    .dc-sub { font-size: var(--fs-xs); color: var(--text-secondary); margin-bottom: 6px; }
    .dc-note { font-size: var(--fs-xs); color: var(--text-secondary); font-style: italic; }
    .dc-arrow { text-align: right; font-size: var(--fs-xs); color: var(--primary-color); }
  }
}

.empty-full { grid-column: 1 / -1; padding: 30px; }

.anomaly-list {
  display: flex; flex-direction: column; gap: 8px;
  .anomaly-item {
    display: flex; align-items: center; gap: 16px; padding: 10px 14px;
    background: var(--bg-warning-light); border-radius: var(--radius-md); font-size: var(--fs-sm);
    .anom-date { font-weight: 600; color: var(--text-primary); }
    .anom-amount { color: var(--danger-color); font-weight: 500; }
    .anom-count { color: var(--text-secondary); }
  }
}

.class-detail {
  .detail-section { margin-top: 20px;
    h4 { font-size: var(--fs-md); margin: 0 0 10px; color: var(--text-primary); }
  }
  .dist-item { display: flex; align-items: center; gap: 10px; font-size: var(--fs-xs); margin-bottom: 6px; }
  .dist-range { width: 60px; text-align: right; color: var(--text-secondary); }
  .dist-track { flex: 1; height: 20px; background: var(--bg-secondary); border-radius: var(--radius-xs); overflow: hidden; }
  .dist-fill { height: 100%; background: var(--primary-gradient); border-radius: var(--radius-xs); transition: width 0.6s; min-width: 2px; }
  .dist-count { width: 40px; color: var(--text-regular); }
}

@media (max-width: 768px) {
  .inspector-classes { padding: var(--spacing-md, 16px); }
  .filter-bar {
    flex-direction: column; align-items: stretch;
    :deep(.el-select) { width: 100%; }
    :deep(.el-button) { width: 100%; margin-left: 0; }
  }
  .summary-row { gap: 8px; .summary-item { padding: 10px 14px; .s-val { font-size: var(--fs-lg); } } }
  .card-grid { grid-template-columns: 1fr; }
  :deep(.el-tabs__item) { font-size: var(--fs-sm); padding: 0 12px !important; }
  :deep(.el-tabs__nav-wrap) { overflow-x: auto; }
}
</style>

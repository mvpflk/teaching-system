<template>
  <div class="inspector-exams">
    <div class="page-header">
      <h3 class="page-title">📝 考试成绩分析</h3>
      <span class="header-subtitle">按班级聚合的成绩统计与分布</span>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-select
        v-model="filters.grade"
        placeholder="年级"
        clearable
        size="default"
        @change="loadData"
      >
        <el-option
          v-for="g in gradeOptions"
          :key="g"
          :value="g"
          :label="g"
        />
      </el-select>
      <el-select
        v-model="filters.classId"
        placeholder="班级"
        clearable
        size="default"
        @change="loadData"
      >
        <el-option
          v-for="c in filteredClassOptions"
          :key="c.id"
          :value="c.id"
          :label="(c.grade||'') + c.className"
        />
      </el-select>
      <el-select
        v-model="filters.taskId"
        placeholder="选择任务"
        clearable
        filterable
        size="default"
        @change="loadData"
      >
        <el-option
          v-for="t in taskOptions"
          :key="t.id"
          :value="t.id"
          :label="t.title"
        />
      </el-select>
      <el-button size="default" @click="resetFilters"><el-icon><Refresh /></el-icon>重置</el-button>
    </div>

    <!-- 汇总概览 -->
    <div v-if="summary.classCount" class="summary-row">
      <div class="summary-item"><span class="s-val">{{ summary.classCount }}</span><span class="s-lbl">班级数</span></div>
      <div class="summary-item"><span class="s-val">{{ summary.totalSubmitted }}</span><span class="s-lbl">总提交</span></div>
      <div class="summary-item"><span class="s-val">{{ summary.totalGraded }}</span><span class="s-lbl">已批改</span></div>
      <div class="summary-item primary"><span class="s-val">{{ summary.overallAvgScore }}</span><span class="s-lbl">总均分</span></div>
      <div class="summary-item" :class="(summary.overallPassRate || 0) >= 60 ? 'good' : 'bad'">
        <span class="s-val">{{ summary.overallPassRate }}%</span><span class="s-lbl">总及格率</span>
      </div>
    </div>

    <!-- 班级成绩卡片 -->
    <div v-if="classStats.length" class="section-title">📊 班级成绩对比</div>
    <div v-if="loading" class="class-cards"><div v-for="i in 4" :key="i" class="sk-card"><div class="sk-line w-60" /><div class="sk-line w-80" /><div class="sk-line w-40" /></div></div>
    <div v-else class="class-cards">
      <div
        v-for="c in classStats"
        :key="c.classId"
        class="class-card"
        @click="openClassDetail(c)"
      >
        <div class="cc-header">
          <span class="cc-grade">{{ c.grade }}</span>
          <span class="cc-name">{{ c.className }}</span>
        </div>
        <div class="cc-metrics">
          <div class="metric primary"><span class="m-val">{{ c.avgScore }}</span><span class="m-lbl">平均分</span></div>
          <div class="metric"><span class="m-val">{{ c.maxScore }}</span><span class="m-lbl">最高</span></div>
          <div class="metric"><span class="m-val">{{ c.minScore }}</span><span class="m-lbl">最低</span></div>
          <div class="metric" :class="(c.passRate || 0) >= 60 ? 'good' : 'bad'">
            <span class="m-val">{{ c.passRate }}%</span><span class="m-lbl">及格率</span>
          </div>
        </div>
        <div class="cc-sub">
          <span>已提交 {{ c.submittedCount }}</span>
          <span>已批改 {{ c.gradedCount }}</span>
        </div>
        <div class="cc-arrow">查看详情 →</div>
      </div>
      <div v-if="classStats.length === 0 && !loading" class="empty-full">
        <el-empty description="暂无成绩数据，请选择筛选条件" :image-size="80" />
      </div>
    </div>

    <!-- 总体分数分布 -->
    <div v-if="summary.overallScoreDistribution && summary.overallScoreDistribution.length" class="section-title">📈 总体分数分布</div>
    <div v-if="summary.overallScoreDistribution && summary.overallScoreDistribution.length" class="dist-card page-card">
      <div v-for="d in summary.overallScoreDistribution" :key="d.range" class="dist-item">
        <span class="dist-range">{{ d.range }}</span>
        <div class="dist-track"><div class="dist-fill" :style="{ width: maxOverallDist > 0 ? (d.count / maxOverallDist * 100) + '%' : '0%' }"></div></div>
        <span class="dist-count">{{ d.count }}人</span>
      </div>
    </div>

    <!-- 班级详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      :title="detailTitle"
      width="600px"
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
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import { getScoreAnalysis } from '@/api/inspector'
import { listTasks } from '@/api/task'
import { getGrades } from '@/api/settings'
import { getClassList } from '@/api/classes'

const loading = ref(false)
const classStats = ref([])
const summary = ref({})

const route = useRoute()
const filters = reactive({ grade: '', classId: null, taskId: null })
const gradeOptions = ref([])
const classOptions = ref([])
const taskOptions = ref([])
const filteredClassOptions = computed(() => {
  if (!filters.grade) return classOptions.value
  return classOptions.value.filter(c => c.grade === filters.grade)
})

const maxOverallDist = computed(() => {
  const dist = summary.value.overallScoreDistribution
  if (!dist || !dist.length) return 0
  return Math.max(...dist.map(d => d.count), 1)
})

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

const openClassDetail = (c) => {
  detailClass.value = c
  detailVisible.value = true
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {}
    if (filters.grade) params.grade = filters.grade
    if (filters.classId) params.classId = filters.classId
    if (filters.taskId) params.taskId = filters.taskId
    const res = await getScoreAnalysis(params)
    if (res.code === 200) {
      classStats.value = res.data.classes || []
      summary.value = res.data.summary || {}
    }
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

const resetFilters = () => {
  filters.grade = ''; filters.classId = null; filters.taskId = null
  loadData()
}
onMounted(async () => {
  if (route.query.classId) filters.classId = Number(route.query.classId)
  loadData()
  try {
    const [gradeRes, clsRes, taskRes] = await Promise.all([
      getGrades(),
      getClassList(),
      listTasks({ size: 999 })
    ])
    if (gradeRes.code === 200) gradeOptions.value = (gradeRes.data || []).map(g => g.gradeName)
    if (clsRes.code === 200) classOptions.value = (clsRes.data.records || []).map(c => ({
      id: c.id, className: c.className, grade: c.grade || ''
    }))
    if (taskRes.code === 200) taskOptions.value = (taskRes.data.records || []).filter(t => t.status !== 'DRAFT')
  } catch { ElMessage.error('加载失败') }
})
</script>

<style scoped lang="scss">
.inspector-exams { max-width: 1280px; margin: 0 auto; padding: var(--spacing-lg, 24px); }

.page-header {
  display: flex; align-items: baseline; gap: 12px; margin-bottom: 16px;
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

.section-title { font-size: var(--fs-lg); font-weight: 600; margin: 20px 0 12px; color: var(--text-primary); }

.class-cards {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 14px;
  .class-card {
    background: var(--bg-card); border-radius: var(--radius-lg); padding: 18px;
    box-shadow: var(--shadow-sm); cursor: pointer; transition: all 0.2s;
    &:hover { transform: translateY(-2px); box-shadow: var(--shadow-base); }
    .cc-header { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
    .cc-grade { font-size: var(--fs-xs); color: var(--primary-color); background: var(--primary-light); padding: 2px 8px; border-radius: var(--radius-xs); }
    .cc-name { font-size: var(--fs-md); font-weight: 600; color: var(--text-primary); }
    .cc-metrics { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; margin-bottom: 10px; }
    .metric { text-align: center;
      .m-val { display: block; font-size: var(--fs-lg); font-weight: 700; color: var(--text-primary); }
      .m-lbl { display: block; font-size: var(--fs-xs); color: var(--text-secondary); }
      &.primary .m-val { color: var(--primary-color); }
      &.good .m-val { color: var(--success-color); }
      &.bad .m-val { color: var(--danger-color); }
    }
    .cc-sub { display: flex; justify-content: space-between; font-size: var(--fs-xs); color: var(--text-secondary); margin-bottom: 8px; }
    .cc-arrow { text-align: right; font-size: var(--fs-xs); color: var(--primary-color); }
  }
}

.empty-full { grid-column: 1 / -1; padding: 30px; }

.dist-card {
  padding: 16px;
  .dist-item { display: flex; align-items: center; gap: 10px; font-size: var(--fs-xs); margin-bottom: 8px; }
  .dist-range { width: 60px; text-align: right; color: var(--text-secondary); }
  .dist-track { flex: 1; height: 20px; background: var(--bg-secondary); border-radius: var(--radius-xs); overflow: hidden; }
  .dist-fill { height: 100%; background: var(--primary-gradient); border-radius: var(--radius-xs); transition: width 0.6s; min-width: 2px; }
  .dist-count { width: 40px; color: var(--text-regular); }
}

.page-card { background: var(--bg-card); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); }

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

.sk-card { background: var(--bg-card); border-radius: var(--radius-md); padding: 16px; display: flex; flex-direction: column; gap: 8px; }
.sk-line { height: 14px; background: var(--bg-secondary); border-radius: var(--radius-xs); position: relative; overflow: hidden; }
.sk-line::after { content: ''; position: absolute; inset: 0; background: linear-gradient(90deg,transparent,rgba(255,255,255,0.4),transparent); animation: sk-shimmer 1.6s infinite; }
@keyframes sk-shimmer { 0% { transform: translateX(-100%) } 100% { transform: translateX(100%) } }
.w-40 { width: 40% } .w-60 { width: 60% } .w-80 { width: 80% }

@media (max-width: 768px) {
  .inspector-exams { padding: var(--spacing-md, 16px); }
  .filter-bar {
    flex-direction: column; align-items: stretch;
    :deep(.el-select) { width: 100%; }
    :deep(.el-button) { width: 100%; margin-left: 0; }
  }
  .summary-row { gap: 8px; .summary-item { padding: 10px 14px; .s-val { font-size: var(--fs-lg); } } }
  .class-cards { grid-template-columns: 1fr; }
}
</style>

<template>
  <div v-loading="loading" class="cd-page">
    <div class="cd-header">
      <el-button text @click="$router.back()"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
      <h2>{{ data.taskTitle || route.query.title || '对比分析' }}</h2>
      <el-tag v-if="data.subject || route.query.subject" type="info" effect="plain">{{ data.subject || route.query.subject }}</el-tag>
      <el-tag v-if="classCount > 0" type="success" effect="plain">{{ classCount }} 个班级</el-tag>
      <div style="margin-left:auto;display:flex;gap:8px;align-items:center">
        <el-checkbox v-model="blinded" size="small" style="margin-right:4px">盲化导出</el-checkbox>
        <el-button size="small" @click="exportReport"><el-icon><Download /></el-icon> 导出 PDF</el-button>
        <el-button size="small" @click="exportScoresCSV"><el-icon><List /></el-icon> 导出成绩 CSV</el-button>
      </div>
    </div>

    <div v-if="!loading && Object.keys(data).length" ref="exportArea">
      <el-tabs v-model="activeTab" type="border-card">
        <!-- ====== TAB 1: 对比概览 ====== -->
        <el-tab-pane name="overview">
          <template #label>
            <span><el-icon><Histogram /></el-icon> 对比概览</span>
          </template>

          <!-- 班级概览卡片 -->
          <el-row v-if="data.classes?.length" :gutter="16">
            <el-col
              v-for="(c, idx) in data.classes"
              :key="c.classId"
              :xs="24"
              :sm="12"
              :md="8"
              :lg="6"
            >
              <el-card shadow="never" class="cd-stat" :class="rankClass(idx)">
                <div class="cd-stat-rank">{{ rankLabel(idx) }}</div>
                <div class="cd-stat-name">
                  {{ c.className }}
                  <el-tag v-if="c.researchGroup === 'EXPERIMENT'" size="small" type="success" effect="dark">实验班</el-tag>
                  <el-tag v-else-if="c.researchGroup === 'CONTROL'" size="small" type="primary" effect="dark">对照班</el-tag>
                </div>
                <div class="cd-stat-v">{{ c.avgScore }}<span class="cd-stat-unit">分</span></div>
                <div class="cd-stat-sub">及格率 {{ c.passRate }}% · {{ c.studentCount }}人</div>
              </el-card>
            </el-col>
          </el-row>
          <el-empty v-if="!loading && (!data.classes || data.classes.length === 0)" description="暂无对比数据" />

          <!-- 课题组别对比（E7） -->
          <el-card v-if="data.researchGroupComparison" shadow="never" class="cd-card rg-compare">
            <template #header><el-icon><DataAnalysis /></el-icon> 课题组别对比：实验班 vs 对照班</template>
            <el-row :gutter="16">
              <el-col :span="12">
                <div class="rg-card exp">
                  <div class="rg-label"><el-tag type="success" effect="dark">实验班</el-tag></div>
                  <div class="rg-v">{{ data.researchGroupComparison.experiment.avgScore }}<span class="rg-unit">分</span></div>
                  <div class="rg-sub">及格率 {{ data.researchGroupComparison.experiment.passRate }}% · {{ data.researchGroupComparison.experiment.studentCount }}人</div>
                </div>
              </el-col>
              <el-col :span="12">
                <div class="rg-card ctrl">
                  <div class="rg-label"><el-tag type="primary" effect="dark">对照班</el-tag></div>
                  <div class="rg-v">{{ data.researchGroupComparison.control.avgScore }}<span class="rg-unit">分</span></div>
                  <div class="rg-sub">及格率 {{ data.researchGroupComparison.control.passRate }}% · {{ data.researchGroupComparison.control.studentCount }}人</div>
                </div>
              </el-col>
            </el-row>
            <div style="margin-top:12px;text-align:center;font-size:14px;color:var(--text-regular)">
              实验班平均分<span :style="{color: expDiff > 0 ? 'var(--el-color-success)' : 'var(--el-color-danger)', fontWeight:'bold'}">{{ expDiff > 0 ? '+' : '' }}{{ expDiff }} 分</span>
              {{ expDiff > 0 ? '高于' : expDiff < 0 ? '低于' : '等于' }}对照班
            </div>
            <!-- 差异显著的知识点 -->
            <div v-if="data.researchGroupComparison.highlightedKps?.length" style="margin-top:16px">
              <div style="font-weight:600;margin-bottom:8px">差异显著的知识点（>15%）</div>
              <el-tag
                v-for="hk in data.researchGroupComparison.highlightedKps"
                :key="hk.kpId"
                :type="hk.diff > 0 ? 'success' : 'danger'"
                effect="plain"
                style="margin:4px"
              >{{ hk.kpName }} {{ hk.diff > 0 ? '+' : '' }}{{ hk.diff }}%</el-tag>
            </div>
          </el-card>

          <!-- 分数分布直方图 -->
          <el-card v-if="data.scoreOverview?.distribution" shadow="never" class="cd-card">
            <template #header>
              <div class="cd-card-head">
                <span>分数分布</span>
                <el-select
                  v-model="distClassFilter"
                  size="small"
                  style="width:140px"
                  @change="renderDistributionChart"
                >
                  <el-option label="全部班级汇总" value="all" />
                  <el-option
                    v-for="c in data.classes"
                    :key="c.classId"
                    :label="c.className"
                    :value="String(c.classId)"
                  />
                </el-select>
              </div>
            </template>
            <div ref="dChart" style="height:240px"></div>
          </el-card>

          <!-- 逐题正确率柱状图 -->
          <el-card v-if="data.perQuestion?.length" shadow="never" class="cd-card">
            <template #header>
              <div class="cd-card-head">
                <span>逐题正确率对比</span>
                <span class="cd-card-hint">每个题目各班级正确率并排，可悬停查看详情</span>
              </div>
            </template>
            <div ref="qChart" style="height:340px"></div>
          </el-card>

          <!-- 知识点对比分析 -->
          <el-card v-if="data.perKp?.length" shadow="never" class="cd-card">
            <template #header>
              <div class="cd-card-head">
                <span>知识点对比分析</span>
                <span class="cd-card-hint">同一知识点在不同班级的正确率，差异超过<strong>20%</strong>高亮标记</span>
              </div>
            </template>

            <el-alert
              :title="kpSummary"
              :type="highlightedCount > 0 ? 'warning' : 'success'"
              :closable="false"
              show-icon
              style="margin-bottom:16px"
            />

            <div
              v-for="kp in sortedKps"
              :key="kp.kpId"
              class="cd-kp-card"
              :class="{ 'cd-kp-card--hl': isHighlighted(kp.kpId) }"
            >
              <div class="cd-kp-head">
                <div class="cd-kp-head-left">
                  <span class="cd-kp-name">{{ kp.kpName }}</span>
                  <el-tag
                    v-if="isHighlighted(kp.kpId)"
                    type="danger"
                    size="small"
                    effect="dark"
                  >
                    差异 {{ kp._delta }}%
                  </el-tag>
                  <el-tag
                    v-else
                    size="small"
                    effect="plain"
                    type="info"
                  >
                    差异 {{ kp._delta }}%
                  </el-tag>
                </div>
                <el-button
                  v-if="isHighlighted(kp.kpId)"
                  size="small"
                  type="primary"
                  @click="generateMaterial(kp)"
                >
                  <el-icon><MagicStick /></el-icon> AI生成巩固材料
                </el-button>
              </div>

              <div class="cd-kp-ranks">
                <div v-for="(c, ci) in kp._sorted" :key="c.classId" class="cd-kp-rank-item">
                  <div class="cd-kp-rank-left">
                    <span class="cd-kp-rank-pos">{{ ci + 1 }}</span>
                    <span class="cd-kp-rank-cls">{{ getClassName(c.classId) }}</span>
                    <el-tag
                      v-if="ci === 0"
                      size="small"
                      type="success"
                      effect="plain"
                    >
                      表现最佳
                    </el-tag>
                    <el-tag
                      v-if="ci === kp._sorted.length - 1 && ci > 0"
                      size="small"
                      type="danger"
                      effect="plain"
                    >
                      需加强
                    </el-tag>
                  </div>
                  <div class="cd-kp-rank-right">
                    <span v-if="ci > 0" class="cd-kp-rank-delta" :style="{ color: c._gap <= 10 ? 'var(--el-color-success)' : c._gap <= 20 ? 'var(--el-color-warning)' : 'var(--el-color-danger)' }">
                      −{{ c._gap }}%
                    </span>
                    <div class="cd-kp-rank-track">
                      <div class="cd-kp-rank-fill" :style="{ width: c.correctRate + '%', background: rankBarColor(c.correctRate) }" />
                    </div>
                    <span class="cd-kp-rank-pct" :style="{ color: rankBarColor(c.correctRate), fontWeight: 700 }">{{ c.correctRate }}%</span>
                  </div>
                </div>
              </div>

              <div v-if="kp._sorted.length >= 2" class="cd-kp-insight">
                💡 {{ kp._sorted[0].correctRate > kp._sorted[kp._sorted.length - 1].correctRate + 10
                  ? `「${getClassName(kp._sorted[kp._sorted.length - 1].classId)}」该知识点正确率落后 ${kp._delta}%，建议针对性巩固`
                  : '各班该知识点掌握情况较为均衡' }}
              </div>
            </div>
          </el-card>
        </el-tab-pane>

        <!-- ====== TAB 2: 学生明细 ====== -->
        <el-tab-pane name="students">
          <template #label>
            <span><el-icon><User /></el-icon> 学生明细
              <el-badge :value="studentCount" :hidden="!studentCount" style="margin-left:4px" />
            </span>
          </template>

          <el-table
            v-if="data.students?.length"
            :data="data.students || []"
            stripe
            size="small"
            max-height="600"
          >
            <el-table-column type="index" label="#" width="50" />
            <el-table-column label="姓名" width="90">
              <template #default="{ row }">
                <el-button
                  text
                  type="primary"
                  size="small"
                  @click="router.push(`/teacher/quality/student/${row.studentId}?subject=${encodeURIComponent(data.subject || '')}`)"
                >
                  {{ row.name }}
                </el-button>
              </template>
            </el-table-column>
            <el-table-column prop="className" label="班级" width="110" />
            <el-table-column
              label="得分"
              width="80"
              sortable
              sort-by="score"
            >
              <template #default="{ row }">
                <span :style="{ color: studentScoreColor(row.score), fontWeight: 700 }">
                  {{ row.score ?? '-' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="等级" width="85">
              <template #default="{ row }">
                <span
                  class="cd-label-tag"
                  :style="{
                    background: (labelColors[row.label] || {}).bg || 'var(--bg-section)',
                    color: (labelColors[row.label] || {}).color || 'var(--text-secondary)'
                  }"
                >{{ row.label }}</span>
              </template>
            </el-table-column>
            <el-table-column label="薄弱知识点" min-width="220">
              <template #default="{ row }">
                <template v-if="row.weakPoints?.length">
                  <div v-for="wp in row.weakPoints" :key="wp.kpId" style="display:inline-flex;align-items:center;gap:2px;margin:2px">
                    <el-popover trigger="hover" placement="top" :width="260">
                      <template #reference>
                        <el-tag
                          size="small"
                          effect="plain"
                          style="cursor:pointer"
                          :type="wp.errorRate >= 70 ? 'danger' : 'warning'"
                        >
                          {{ wp.kpName }}
                          <span
                            v-if="precisionStatus[`${row.studentId}::${wp.kpId}`]?.exists"
                            :style="{ marginLeft: '4px', fontSize: '12px' }"
                            :title="precisionStatus[`${row.studentId}::${wp.kpId}`]?.status"
                          >
                            {{ precisionStatus[`${row.studentId}::${wp.kpId}`]?.status === 'mastered' ? '✅' :
                              precisionStatus[`${row.studentId}::${wp.kpId}`]?.status === 'weak' ? '🔴' : '📦' }}
                          </span>
                        </el-tag>
                      </template>
                      <div>
                        <div style="font-weight:600;margin-bottom:4px">{{ wp.kpName }}</div>
                        <div style="font-size:var(--fs-xs);color:#606266;margin-bottom:2px">
                          错误率 {{ wp.errorRate }}% ({{ wp.wrong }}/{{ wp.total }}题)
                        </div>
                        <div v-if="precisionStatus[`${row.studentId}::${wp.kpId}`]?.exists" style="font-size:var(--fs-xs);color:#606266">
                          偏科进度：{{ precisionStatus[`${row.studentId}::${wp.kpId}`]?.masteryPercent || 0 }}%
                          · {{ precisionStatus[`${row.studentId}::${wp.kpId}`]?.status === 'mastered' ? '已掌握' :
                            precisionStatus[`${row.studentId}::${wp.kpId}`]?.status === 'weak' ? '薄弱' : '学习中' }}
                        </div>
                        <div v-else style="font-size:var(--fs-xs);color:var(--el-color-warning, #e6a23c)">未纳入偏科提分</div>
                      </div>
                    </el-popover>
                  </div>
                </template>
                <span v-else style="color:var(--el-color-success, #67c23a);font-size:var(--fs-xs)">无·已全部掌握</span>
              </template>
            </el-table-column>
          </el-table>

          <el-empty v-if="!data.students?.length" description="暂无学生数据" />
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-empty v-if="!loading && !Object.keys(data).length" description="暂无数据" />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, MagicStick, Histogram, User, Download, DataAnalysis } from '@element-plus/icons-vue'
import { List } from '@element-plus/icons-vue'
import { exportClassScores, exportResearchData } from '@/api/analytics'
import request from '@/utils/request'
import { exportPdf } from '@/utils/exportPdf'
import { useConsolidationGenerator } from '@/composables/useConsolidationGenerator'

const route = useRoute()
const router = useRouter()
const taskId = route.params.taskId
const taskIds = route.query.ids
const loading = ref(false)
const data = ref({})
const qChart = ref(null)
const dChart = ref(null)
const exportArea = ref(null)
const distClassFilter = ref('all')   // 'all' | classId
const precisionStatus = ref({})     // "studentId::kpId" → {exists, status, masteryPercent}
const activeTab = ref('overview')
const blinded = ref(false)  // P0-3: 盲化导出
let chartInstance = null
let distChartInstance = null

const classCount = computed(() => (data.value.classes || []).length)

// 知识点按差异降序排列
const sortedKps = computed(() => {
  const kps = (data.value.perKp || []).map(kp => {
    const cc = [...(kp.classes || [])]
    // 按正确率降序
    cc.sort((a, b) => (b.correctRate || 0) - (a.correctRate || 0))
    // 计算与最佳班级的差距
    const best = cc[0]?.correctRate || 0
    cc.forEach((c, i) => { c._gap = i === 0 ? 0 : Math.round((best - c.correctRate) * 10) / 10 })
    const rates = cc.map(c => c.correctRate || 0)
    const delta = cc.length >= 2 ? Math.round((Math.max(...rates) - Math.min(...rates)) * 10) / 10 : 0
    return { ...kp, _sorted: cc, _delta: delta, _maxGap: delta }
  })
  kps.sort((a, b) => b._maxGap - a._maxGap)
  return kps
})

const highlightedCount = computed(() => sortedKps.value.filter(kp => kp._delta > 20).length)

// E7: 实验班 vs 对照班 分数差
const expDiff = computed(() => {
  const rg = data.value.researchGroupComparison
  if (!rg) return 0
  const e = rg.experiment?.avgScore || 0
  const c = rg.control?.avgScore || 0
  return Math.round((e - c) * 10) / 10
})
const highlightedIds = computed(() => new Set((data.value.highlightedKps || []).map(k => k.kpId)))
const isHighlighted = (kpId) => highlightedIds.value.has(kpId)

const kpSummary = computed(() => {
  const n = classCount.value
  const h = highlightedCount.value
  const total = sortedKps.value.length
  if (n < 2) return `仅 ${n} 个班级有数据，无法对比`
  if (h === 0) return `${n} 个班级在 ${total} 个知识点上表现较为均衡，无显著差异（>20%）`
  return `${n} 个班级对比完成，${h} 个知识点班级间差异超过 20%`
})

function getClassName(cid) {
  return (data.value.classes || []).find(c => c.classId === cid)?.className || ('班级' + cid)
}

// ─── 学生明细 ────────────────────────────
const studentCount = computed(() => (data.value.students || []).length)

const labelColors = {
  '已达标': { bg: 'var(--bg-success-light)', color: 'var(--el-color-success)' },
  '成长中': { bg: 'var(--primary-light)', color: 'var(--primary-color)' },
  '发展中': { bg: 'var(--bg-warning-light)', color: 'var(--el-color-warning)' },
  '起步期': { bg: 'var(--bg-danger-light)', color: 'var(--el-color-danger)' }
}

const studentScoreColor = (score) => {
  if (score == null) return 'var(--text-secondary)'
  if (score >= 85) return 'var(--el-color-success)'
  if (score >= 70) return 'var(--primary-color)'
  if (score >= 60) return 'var(--el-color-warning)'
  return 'var(--el-color-danger)'
}

const rankBarColor = (r) => r >= 80 ? 'var(--el-color-success)' : r >= 60 ? 'var(--primary-color)' : r >= 40 ? 'var(--el-color-warning)' : 'var(--el-color-danger)'

// 排名徽章
function rankClass(idx) {
  if (classCount.value <= 1) return ''
  if (idx === 0) return 'cd-stat--gold'
  if (idx === classCount.value - 1) return 'cd-stat--tail'
  return ''
}
function rankLabel(idx) {
  if (classCount.value <= 1) return ''
  if (idx === 0) return '🥇'
  if (idx === 1 && classCount.value > 2) return '🥈'
  if (idx === 2 && classCount.value > 3) return '🥉'
  return ''
}

// ─── 数据加载 ───

async function load() {
  loading.value = true
  try {
    const url = taskIds
      ? `/teacher/comparison/${taskId}/diagnosis?ids=${taskIds}`
      : `/teacher/comparison/${taskId}/diagnosis`
    const res = await request.get(url)
    if (res.code === 200) {
      // 班级按均分降序
      const raw = res.data || {}
      if (raw.classes) raw.classes.sort((a, b) => (b.avgScore || 0) - (a.avgScore || 0))
      data.value = raw
    }
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
  // 必须在 loading=false 之后渲染 — el-tabs 的 v-if 依赖 loading，DOM 此时才就绪
  await nextTick()
  if (activeTab.value === 'overview') { renderChart(); renderDistributionChart() }
  // 预加载偏科提分状态，供学生明细 tab 使用
  fetchPrecisionStatus()
}

// 切换回概览 tab 时重新渲染图表
watch(activeTab, (val) => {
  if (val === 'overview') nextTick(() => { renderChart(); renderDistributionChart() })
  if (val === 'students') fetchPrecisionStatus()
})

// ─── 偏科提分状态 ───

async function fetchPrecisionStatus() {
  const students = data.value.students || []
  const pairs = []
  for (const s of students) {
    for (const wp of (s.weakPoints || [])) {
      pairs.push({ studentId: s.studentId, kpId: wp.kpId })
    }
  }
  if (!pairs.length) return
  try {
    const res = await request.post('/precision/teacher/student-kp-status', { pairs })
    if (res.code === 200) {
      const map = {}
      for (const item of (res.data || [])) {
        map[`${item.studentId}::${item.kpId}`] = item
      }
      precisionStatus.value = map
    }
  } catch { /* 偏科模块未开放静默失败 */ }
}

// ─── ECharts ───

// ─── 分数分布 ───

function renderDistributionChart() {
  if (!dChart.value) return
  const distData = distClassFilter.value === 'all'
    ? data.value.scoreOverview?.distribution
    : (data.value.perClassDistribution || {})[Number(distClassFilter.value)]
  if (!distData) return

  import('@/utils/echarts').then(({ default: echarts, cssVar }) => {
    if (distChartInstance) { distChartInstance.dispose(); distChartInstance = null }
    const prev = echarts.getInstanceByDom(dChart.value)
    if (prev) prev.dispose()
    distChartInstance = echarts.init(dChart.value)

    const buckets = ['<60', '60-74', '75-89', '90-100']
    const labels = ['起步期', '发展中', '成长中', '已达标']
    const values = buckets.map(b => distData[b] || 0)
    const colors = [cssVar('--el-color-danger'), cssVar('--el-color-warning'), cssVar('--primary-color'), cssVar('--el-color-success')]

    distChartInstance.setOption({
      tooltip: {
        trigger: 'axis',
        formatter: (params) => {
          const p = params[0]
          return `${labels[p.dataIndex]} (${buckets[p.dataIndex]}分)<br/>${p.value} 人`
        }
      },
      grid: { left: 40, right: 20, top: 10, bottom: 30 },
      xAxis: {
        type: 'category', data: labels,
        axisLabel: { fontSize: 12 }
      },
      yAxis: { type: 'value', minInterval: 1, axisLabel: { formatter: '{value}人' } },
      series: [{
        type: 'bar', barMaxWidth: 60,
        data: values.map((v, i) => ({ value: v, itemStyle: { color: colors[i], borderRadius: [4, 4, 0, 0] } })),
        label: { show: true, position: 'top', formatter: p => p.value > 0 ? p.value : '', fontSize: 12 }
      }]
    })
  })
}

function renderChart() {
  if (!qChart.value || !data.value.perQuestion?.length) return
  import('@/utils/echarts').then(({ default: echarts, cssVar }) => {
    // 清理旧实例
    if (chartInstance) { chartInstance.dispose(); chartInstance = null }
    const prev = echarts.getInstanceByDom(qChart.value)
    if (prev) prev.dispose()
    chartInstance = echarts.init(qChart.value)

    const labels = data.value.perQuestion.map((_, i) => 'Q' + (i + 1))
    const cls = data.value.classes || []
    const colors = [cssVar('--primary-color'), cssVar('--el-color-danger'), cssVar('--el-color-success'), cssVar('--el-color-warning'), cssVar('--text-secondary'), cssVar('--el-color-danger')]

    chartInstance.setOption({
      tooltip: {
        trigger: 'axis',
        formatter: (params) => {
          let s = params[0]?.axisValue + '<br/>'
          params.forEach(p => { s += `${p.marker} ${p.seriesName}: ${p.value}%<br/>` })
          return s
        }
      },
      legend: {
        data: cls.map(c => c.className),
        type: 'scroll', bottom: 0
      },
      color: colors,
      grid: { left: 40, right: 20, top: 20, bottom: 40 },
      xAxis: {
        type: 'category', data: labels,
        axisLabel: { rotate: labels.length > 30 ? 45 : 0, fontSize: 11 }
      },
      yAxis: { max: 100, axisLabel: { formatter: '{value}%' } },
      series: cls.map(c => ({
        name: c.className, type: 'bar', barMaxWidth: Math.max(8, 120 / labels.length),
        data: data.value.perQuestion.map(q => {
          const found = (q.classes || []).find(cc => cc.classId === c.classId)
          return found ? found.correctRate : null
        }),
        emphasis: { itemStyle: { shadowBlur: 8, shadowColor: 'rgba(0,0,0,.3)' } }
      }))
    })
  })
}

// ─── 生成巩固材料 ───

const consolidation = useConsolidationGenerator()

async function generateMaterial(kp) {
  const outputId = await consolidation.generate({
    taskId: Number(taskId),
    knowledgeNodeIds: [kp.kpId],
    subject: data.value.subject || ''
  })
  if (outputId) router.push(`/teacher/quality/consolidation/${outputId}`)
}

async function exportReport() {
  if (!exportArea.value) { ElMessage.warning('请等待页面加载完成'); return }
  try {
    await exportPdf(exportArea.value,
      `${data.value.taskTitle || '对比分析'}_${new Date().toISOString().slice(0, 10)}_对比分析报告`,
      { format: 'a3', orientation: 'portrait' })
    ElMessage.success('报告已导出')
    } catch { ElMessage.error('导出失败') }
  }

  async function exportScoresCSV() {
    const classId = route.query.classId
    if (!classId) { ElMessage.warning('请先选择班级'); return }
    try {
      const res = await exportClassScores(classId, route.query.subject, null, null, blinded.value)
      const blob = new Blob([res], { type: 'text/csv;charset=utf-8' })
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      const prefix = blinded.value ? 'scores_blinded' : 'scores'
      a.download = `${prefix}_${classId}_${new Date().toISOString().slice(0, 10)}.csv`
      a.click()
      window.URL.revokeObjectURL(url)
      ElMessage.success(blinded.value ? '盲化成绩导出成功（学生姓名已编码）' : '成绩导出成功')
    } catch { ElMessage.error('导出失败') }
  }

  onMounted(load)
onUnmounted(() => {
  if (chartInstance) { chartInstance.dispose(); chartInstance = null }
  if (distChartInstance) { distChartInstance.dispose(); distChartInstance = null }
})
</script>

<style scoped>
@import '@/styles/knowledge-point-card.css';

.cd-page { margin: 0 auto; padding: 24px; }
.cd-header { display: flex; align-items: center; gap: 12px; margin-bottom: 24px; flex-wrap: wrap; }
.cd-header h2 { margin: 0; font-size: var(--fs-xl); color: var(--text-primary); }

/* 概览卡片 */
.cd-stat { text-align: center; padding: 16px 12px; position: relative; border: 0.5px solid var(--border-light); }
.cd-stat-name { font-size: var(--fs-sm); color: var(--text-secondary); margin-bottom: 4px; }
.cd-stat-v { font-size: 30px; font-weight: 700; color: var(--primary-color); }
.cd-stat-unit { font-size: var(--fs-md); font-weight: 400; color: var(--text-secondary); }
.cd-stat-sub { font-size: var(--fs-xs); color: var(--text-secondary); margin-top: 4px; }
.cd-stat-rank { position: absolute; top: 6px; right: 10px; font-size: var(--fs-lg); }
.cd-stat--gold { border-top: 2px solid var(--el-color-warning); }
.cd-stat--tail { border-top: 2px solid var(--bg-secondary); }

.cd-card { margin-top: 20px; }
.cd-card-head { display: flex; align-items: baseline; gap: 12px; }
.cd-card-hint { font-size: var(--fs-xs); color: var(--text-secondary); font-weight: 400; }

/* 学生明细 — 等级标签 */
.cd-label-tag {
  font-size: var(--fs-xs); padding: 2px 8px; border-radius: 10px;
  font-weight: 500; white-space: nowrap;
}

/* E7: 课题组别对比 */
.rg-compare { border: 2px solid var(--border-color); }
.rg-card { text-align: center; padding: 16px; border-radius: 8px; }
.rg-card.exp { background: var(--bg-success-light); border: 1px solid var(--el-color-success); }
.rg-card.ctrl { background: var(--primary-light); border: 1px solid var(--primary-color); }
.rg-label { margin-bottom: 8px; }
.rg-v { font-size: 28px; font-weight: 700; }
.rg-unit { font-size: 14px; color: var(--text-secondary); margin-left: 2px; }
.rg-sub { font-size: 13px; color: var(--text-regular); margin-top: 4px; }

@media (max-width: 768px) {
  .cd-page { padding: 12px; }
  .cd-header { flex-direction: column; align-items: flex-start; }
  .cd-header h2 { font-size: var(--fs-lg); }
  .cd-kp-rank-left { min-width: 120px; }
  .cd-kp-rank-track { min-width: 60px; }
}
</style>

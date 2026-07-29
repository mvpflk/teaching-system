<template>
  <div class="ua-page">
    <div class="ua-header">
      <h2><el-icon><DataAnalysis /></el-icon> 功能使用分析</h2>
      <div class="ua-header-right">
        <span v-if="stats.updatedAt" class="ua-updated">数据截至 {{ stats.updatedAt }}</span>
        <el-select v-model="days" style="width:120px" @change="loadStats">
          <el-option :value="7" label="近7天" />
          <el-option :value="30" label="近30天" />
          <el-option :value="90" label="近90天" />
        </el-select>
      </div>
    </div>

    <!-- 加载失败提示 -->
    <el-alert
      v-if="errorMsg"
      :title="errorMsg"
      type="error"
      show-icon
      closable
      style="margin-bottom:16px"
      @close="errorMsg=''"
    />

    <!-- 空状态：无埋点数据 -->
    <el-empty v-if="!loading && !errorMsg && isEmpty" description="暂无使用数据。功能埋点已就绪，数据将随师生使用逐步积累。" />

    <template v-else>
      <el-row v-loading="loading" :gutter="16">
        <el-col :xs="12" :md="6">
          <el-card shadow="never" class="ua-stat">
            <div class="ua-stat-v">{{ stats.uniqueUsers || 0 }}</div>
            <div class="ua-stat-l">活跃用户</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :md="6">
          <el-card shadow="never" class="ua-stat">
            <div class="ua-stat-v">{{ stats.totalEvents || 0 }}</div>
            <div class="ua-stat-l">事件总数</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :md="6">
          <el-card shadow="never" class="ua-stat">
            <div class="ua-stat-v">{{ examGenerateCount }}</div>
            <div class="ua-stat-l">组卷次数</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :md="6">
          <el-card shadow="never" class="ua-stat">
            <div class="ua-stat-v">{{ diagnosisCount }}</div>
            <div class="ua-stat-l">诊断次数</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 组卷漏斗 -->
      <el-card
        v-if="examGenerateCount > 0"
        shadow="never"
        class="ua-card"
        style="margin-top:16px"
      >
        <template #header><span>组卷漏斗</span></template>
        <div class="ua-funnel">
          <div class="ua-fstep">
            <span class="ua-fstep-label">生成</span>
            <div class="ua-fstep-bar">
              <div class="ua-fstep-fill" :style="{ width: '100%' }">{{ examGenerateCount }}</div>
            </div>
          </div>
          <div class="ua-farrow">→</div>
          <div class="ua-fstep">
            <span class="ua-fstep-label">发布</span>
            <div class="ua-fstep-bar">
              <div class="ua-fstep-fill ua-fstep-fill--pub" :style="{ width: Math.max(publishRate, 5) + '%' }">{{ examPublishCount }}</div>
            </div>
            <span class="ua-fstep-note">采纳率 {{ publishRate }}%</span>
          </div>
        </div>
      </el-card>

      <!-- 诊断反馈 -->
      <el-card
        v-if="diagnosisCount > 0"
        shadow="never"
        class="ua-card"
        style="margin-top:16px"
      >
        <template #header><span>诊断反馈</span></template>
        <el-row :gutter="16">
          <el-col :span="12">
            <div class="ua-gauge">
              <div class="ua-gauge-label">诊断使用次数</div>
              <div class="ua-gauge-v">{{ diagnosisCount }}</div>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="ua-gauge">
              <div class="ua-gauge-label">满意率</div>
              <div class="ua-gauge-v" :class="satisfactionRate >= 70 ? 'ua-green' : satisfactionRate > 0 ? 'ua-orange' : ''">
                {{ satisfactionRate }}%
              </div>
              <div v-if="satisfactionTotal > 0" class="ua-gauge-detail">{{ satisfactionTotal }} 人反馈满意</div>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <!-- 学科分布 -->
      <el-card
        v-if="hasSubjectData"
        shadow="never"
        class="ua-card"
        style="margin-top:16px"
      >
        <template #header><span>学科使用分布</span></template>
        <div class="ua-subj">
          <div v-for="(cnt, subj) in stats.bySubject" :key="subj" class="ua-subj-item">
            <span class="ua-subj-name">{{ subj }}</span>
            <el-progress :percentage="subjPercent(cnt)" :stroke-width="10" :color="'#4361ee'" />
            <span class="ua-subj-cnt">{{ cnt }}次</span>
          </div>
        </div>
      </el-card>

      <!-- 每日趋势图 — 简易柱状 -->
      <el-card
        v-if="hasDailyTrend"
        shadow="never"
        class="ua-card"
        style="margin-top:16px"
      >
        <template #header><span>每日活跃趋势</span></template>
        <div class="ua-trend">
          <div v-for="(cnt, day) in dailyTrendList" :key="day" class="ua-trend-bar">
            <div class="ua-trend-fill" :style="{ height: trendHeight(cnt) + '%' }" :title="day + ': ' + cnt + '次'"></div>
            <span class="ua-trend-day">{{ day.slice(5) }}</span>
          </div>
        </div>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { DataAnalysis } from '@element-plus/icons-vue'
import request from '@/utils/request'

const days = ref(30)
const stats = ref({})
const loading = ref(false)
const errorMsg = ref('')

async function loadStats() {
  loading.value = true
  errorMsg.value = ''
  try {
    // 注意: baseURL 已是 /api，不要再加 /api 前缀
    const res = await request.get('/user-events/stats', { params: { days: days.value } })
    if (res.code === 200) {
      stats.value = res.data || {}
      stats.value.updatedAt = new Date().toLocaleString('zh-CN')
    } else {
      stats.value = {}
      errorMsg.value = res.message || '获取数据失败'
    }
  } catch (e) {
    stats.value = {}
    const msg = e?.response?.data?.message || e?.message || ''
    if (msg.includes('403') || msg.includes('权限')) {
      errorMsg.value = '暂无访问权限，请联系管理员'
    } else if (msg.includes('404')) {
      errorMsg.value = '埋点统计服务未就绪'
    } else {
      errorMsg.value = '数据加载失败，请稍后重试'
    }
  }
  finally { loading.value = false }
}

const isEmpty = computed(() => !stats.value.totalEvents || stats.value.totalEvents === 0)

const examGenerateCount = computed(() => stats.value.byType?.['EXAM_PAPER_GENERATE'] || 0)
const examPublishCount = computed(() => stats.value.byType?.['EXAM_PAPER_PUBLISH'] || 0)
const publishRate = computed(() => examGenerateCount.value > 0
  ? Math.round(examPublishCount.value / examGenerateCount.value * 100) : 0)

const diagnosisCount = computed(() => stats.value.byType?.['DIAGNOSIS_START'] || 0)
const satisfactionTotal = computed(() => stats.value.byType?.['DIAGNOSIS_SATISFACTION'] || 0)
// 满意率 = 满意反馈数 / 总诊断次数 × 100（无反馈时显示 "—"）
const satisfactionRate = computed(() => {
  if (diagnosisCount.value === 0) return 0
  if (satisfactionTotal.value === 0) return 0  // 无人反馈 ≠ 100%
  return Math.min(100, Math.round(satisfactionTotal.value / diagnosisCount.value * 100))
})

const hasSubjectData = computed(() => {
  const s = stats.value.bySubject
  return s && Object.keys(s).length > 0
})

const maxSubj = computed(() => Math.max(1, ...Object.values(stats.value.bySubject || { a: 1 })))
const subjPercent = (cnt) => Math.round(cnt / maxSubj.value * 100)

// 每日趋势 (只取最近14天展示)
const hasDailyTrend = computed(() => {
  const d = stats.value.dailyTrend
  return d && Object.keys(d).length > 0
})
const dailyTrendList = computed(() => {
  const raw = stats.value.dailyTrend || {}
  const entries = Object.entries(raw).sort((a, b) => a[0].localeCompare(b[0]))
  // 取最近14天
  const recent = entries.slice(-14)
  return Object.fromEntries(recent)
})
const maxDaily = computed(() => Math.max(1, ...Object.values(dailyTrendList.value)))
const trendHeight = (cnt) => Math.round(cnt / maxDaily.value * 100)

onMounted(loadStats)
</script>

<style scoped>
.ua-page { max-width: 960px; margin: 0 auto; padding: 24px; }
.ua-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; flex-wrap: wrap; gap: 8px; }
.ua-header h2 { font-size: var(--fs-xl); display: flex; align-items: center; gap: 8px; margin: 0; color: var(--text-primary); }
.ua-header-right { display: flex; align-items: center; gap: 12px; }
.ua-updated { font-size: var(--fs-xs); color: var(--text-disabled, var(--text-disabled)); }
.ua-stat { text-align: center; padding: 8px; }
.ua-stat-v { font-size: 32px; font-weight: 700; color: var(--primary-color); }
.ua-stat-l { font-size: var(--fs-xs); color: var(--text-secondary); margin-top: 4px; }
.ua-card { margin-bottom: 0; }
.ua-funnel { display: flex; align-items: center; gap: 16px; }
.ua-fstep { flex: 1; }
.ua-fstep-label { font-size: var(--fs-sm); font-weight: 600; margin-bottom: 4px; display: block; }
.ua-fstep-bar { height: 28px; background: var(--bg-section); border-radius: 4px; overflow: hidden; }
.ua-fstep-fill { height: 100%; background: var(--primary-color); border-radius: 4px; display: flex; align-items: center; justify-content: center; color: #fff; font-weight: 600; font-size: var(--fs-sm); min-width: 40px; }
.ua-fstep-fill--pub { background: var(--el-color-success); }
.ua-fstep-note { font-size: var(--fs-xs); color: var(--text-secondary); margin-top: 2px; }
.ua-farrow { font-size: var(--fs-2xl); color: var(--text-secondary); padding-top: 16px; }
.ua-gauge { text-align: center; padding: 16px; }
.ua-gauge-label { font-size: var(--fs-sm); color: var(--text-secondary); }
.ua-gauge-v { font-size: 42px; font-weight: 700; color: var(--primary-color); }
.ua-gauge-detail { font-size: var(--fs-xs); color: var(--text-disabled, var(--text-disabled)); margin-top: 4px; }
.ua-green { color: var(--el-color-success) !important; }
.ua-orange { color: var(--el-color-warning) !important; }
.ua-subj { display: flex; flex-direction: column; gap: 12px; }
.ua-subj-item { display: flex; align-items: center; gap: 10px; }
.ua-subj-name { min-width: 80px; font-size: var(--fs-sm); color: var(--text-primary); }
.ua-subj-cnt { min-width: 40px; text-align: right; font-size: var(--fs-xs); color: var(--text-secondary); }
/* 每日趋势简易柱状图 */
.ua-trend { display: flex; align-items: flex-end; gap: 4px; height: 120px; padding-top: 8px; }
.ua-trend-bar { flex: 1; display: flex; flex-direction: column; align-items: center; height: 100%; justify-content: flex-end; }
.ua-trend-fill { width: 100%; max-width: 28px; min-height: 2px; background: var(--primary-color); border-radius: 3px 3px 0 0; transition: height 0.3s; cursor: pointer; }
.ua-trend-fill:hover { background: var(--primary-dark); }
.ua-trend-day { font-size: 10px; color: var(--text-disabled, var(--text-disabled)); margin-top: 4px; writing-mode: vertical-rl; }
</style>

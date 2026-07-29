<template>
  <div class="teacher-profile">
    <div class="back-bar">
      <el-button text @click="$router.back()"><el-icon><ArrowLeft /></el-icon>返回</el-button>
    </div>

    <div v-loading="loading" class="profile-content">
      <div class="profile-header">
        <div class="profile-avatar">{{ (profile.teacherName || '?')[0] }}</div>
        <div>
          <h2 class="profile-name">教师档案 — {{ profile.teacherName }}</h2>
          <div class="profile-meta">教师ID: {{ profile.teacherId }}</div>
        </div>
      </div>

      <div class="stat-grid">
        <div class="stat-card"><span class="s-val">{{ profile.tasksCreated }}</span><span class="s-lbl">总任务数</span></div>
        <div class="stat-card"><span class="s-val">{{ profile.submissionsReceived }}</span><span class="s-lbl">总提交数</span></div>
        <div class="stat-card"><span class="s-val">{{ profile.submissionsGraded }}</span><span class="s-lbl">总批改数</span></div>
        <div class="stat-card"><span class="s-val">{{ profile.avgResponseHours }}h</span><span class="s-lbl">平均响应</span></div>
        <div class="stat-card"><span class="s-val">{{ profile.gradedOnTimeRate }}%</span><span class="s-lbl">批改率</span></div>
        <div class="stat-card"><span class="s-val">{{ profile.rectificationCompleted }}</span><span class="s-lbl">整改完成</span></div>
      </div>

      <div class="org-section">
        <h4 class="org-title">所属组织</h4>
        <div v-if="teachingGroups.length === 0 && lessonPrepGroups.length === 0" class="org-empty">
          暂未加入任何教研组或备课组
        </div>
        <div v-if="teachingGroups.length > 0" class="org-row">
          <span class="org-label">教研组</span>
          <span class="org-tags">
            <el-tag
              v-for="g in teachingGroups"
              :key="g.id"
              :type="g.role === 'LEADER' ? 'primary' : 'info'"
              size="small"
              style="margin-right:6px;margin-bottom:4px"
            >
              {{ g.name }} - {{ g.role === 'LEADER' ? '组长' : '组员' }}
            </el-tag>
          </span>
        </div>
        <div v-if="lessonPrepGroups.length > 0" class="org-row">
          <span class="org-label">备课组</span>
          <span class="org-tags">
            <el-tag
              v-for="g in lessonPrepGroups"
              :key="g.id"
              :type="g.role === 'LEADER' ? 'primary' : 'info'"
              size="small"
              style="margin-right:6px;margin-bottom:4px"
            >
              {{ g.name }} - {{ g.role === 'LEADER' ? '组长' : '组员' }}
            </el-tag>
          </span>
        </div>
      </div>

      <div class="score-card">
        <div class="score-number">{{ profile.activityScore }}</div>
        <div class="score-label">活跃度分数</div>
      </div>

      <div class="chart-section">
        <div class="chart-box">
          <h4>近4周活跃度趋势</h4>
          <div id="trend-chart" style="width:100%;height:300px" />
        </div>
        <div class="chart-box">
          <h4>任务创建 vs 批改</h4>
          <div id="compare-chart" style="width:100%;height:300px" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import echarts, { cssVar } from '@/utils/echarts'
import { getTeacherProfile } from '@/api/inspector'

const route = useRoute()
const loading = ref(true)
const profile = ref({})
const teachingGroups = computed(() => profile.value.teachingGroups || [])
const lessonPrepGroups = computed(() => profile.value.lessonPrepGroups || [])
let trendChart = null
let compareChart = null

const resizeHandler = () => { trendChart?.resize(); compareChart?.resize() }

const initCharts = () => {
  const trend = profile.value.activityTrend || []
  const weeks = trend.map(t => t.week)
  const scores = trend.map(t => t.score)

  trendChart = echarts.init(document.getElementById('trend-chart'))
  trendChart.setOption({
    tooltip: { trigger: 'axis' }, grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: weeks.length ? weeks : ['W1', 'W2', 'W3', 'W4'] },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ type: 'line', data: scores.length ? scores : [0, 0, 0, 0], smooth: true, areaStyle: { opacity: 0.15 }, itemStyle: { color: cssVar('--primary-color') } }]
  })

  compareChart = echarts.init(document.getElementById('compare-chart'))
  compareChart.setOption({
    tooltip: { trigger: 'axis' }, grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: ['任务创建', '批改数量'] },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      type: 'bar', data: [profile.value.tasksCreated || 0, profile.value.submissionsGraded || 0],
      itemStyle: { color: [cssVar('--primary-color'), cssVar('--el-color-success')], borderRadius: [4, 4, 0, 0] },
      barWidth: 60
    }]
  })
}

onMounted(async () => {
  window.addEventListener('resize', resizeHandler)
  try {
    const res = await getTeacherProfile(route.params.id)
    if (res.code === 200) { profile.value = res.data; setTimeout(initCharts, 100) }
    else ElMessage.error(res.message)
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeHandler)
  trendChart?.dispose(); compareChart?.dispose()
})
</script>

<style scoped lang="scss">
.teacher-profile { max-width: 1200px; margin: 0 auto; padding: var(--spacing-lg, 24px); }
.back-bar { margin-bottom: 16px; }
.profile-content { }
.profile-header { display: flex; align-items: center; gap: 16px; margin-bottom: 24px; }
.profile-avatar { width: 56px; height: 56px; border-radius: 50%; background: var(--primary-gradient); display: flex; align-items: center; justify-content: center; font-size: var(--fs-2xl); color: #fff; font-weight: 700; }
.profile-name { font-size: var(--fs-xl); font-weight: 700; margin: 0; }
.profile-meta { font-size: var(--fs-sm); color: var(--text-secondary); margin-top: 4px; }

.stat-grid { display: grid; grid-template-columns: repeat(6, 1fr); gap: 12px; margin-bottom: 24px; }
.stat-card { text-align: center; padding: 16px 10px; background: var(--bg-card); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); }
.stat-card .s-val { display: block; font-size: 22px; font-weight: 700; color: var(--text-primary); }
.stat-card .s-lbl { display: block; font-size: var(--fs-xs); color: var(--text-secondary); margin-top: 4px; }

.org-section { background: var(--bg-card); border-radius: var(--radius-md); padding: 16px 20px; margin-bottom: 20px; box-shadow: var(--shadow-sm); }
.org-title { font-size: var(--fs-md); font-weight: 600; margin: 0 0 12px; color: var(--text-primary); }
.org-empty { font-size: var(--fs-sm); color: var(--text-secondary); padding: 8px 0; }
.org-row { display: flex; align-items: flex-start; gap: 12px; margin-bottom: 8px; &:last-child { margin-bottom: 0; } }
.org-label { flex-shrink: 0; width: 56px; font-size: var(--fs-sm); color: var(--text-secondary); line-height: 24px; }
.org-tags { flex: 1; }

.score-card { text-align: center; padding: 24px; margin-bottom: 24px; background: var(--primary-gradient); border-radius: var(--radius-lg); color: #fff; }
.score-number { font-size: 48px; font-weight: 800; }
.score-label { font-size: var(--fs-md); opacity: 0.85; margin-top: 4px; }

.chart-section { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.chart-box { background: var(--bg-card); border-radius: var(--radius-md); padding: 16px; box-shadow: var(--shadow-sm); h4 { margin: 0 0 12px; font-size: var(--fs-md); color: var(--text-primary); } }

@media (max-width: 768px) {
  .teacher-profile { padding: var(--spacing-md, 16px); }
  .stat-grid { grid-template-columns: repeat(3, 1fr); }
  .chart-section { grid-template-columns: 1fr; }
}
</style>

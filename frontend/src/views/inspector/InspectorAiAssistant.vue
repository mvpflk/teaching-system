<template>
  <div class="inspector-ai">
    <div class="page-header">
      <h3 class="page-title">🤖 AI巡视助手</h3>
      <span class="header-subtitle">智能分析教学数据，辅助巡视决策</span>
    </div>

    <div class="ai-cards">
      <el-card class="ai-card">
        <template #header><div class="card-title">📝 生成周报</div></template>
        <p class="card-desc">自动汇总教学运行数据，生成Markdown周报</p>
        <el-form-item label="起始">
          <el-date-picker
            v-model="weeklyForm.start"
            type="date"
            value-format="YYYY-MM-DD"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="截止" style="margin-top:10px">
          <el-date-picker
            v-model="weeklyForm.end"
            type="date"
            value-format="YYYY-MM-DD"
            style="width:100%"
          />
        </el-form-item>
        <el-button
          type="primary"
          :loading="weeklyLoading"
          style="width:100%;margin-top:8px"
          @click="genWeekly"
        >
          生成周报
        </el-button>
        <div v-if="weeklyResult" class="result-box markdown-box" v-html="weeklyResult" />
      </el-card>

      <el-card class="ai-card">
        <template #header><div class="card-title">🔍 异常扫描</div></template>
        <p class="card-desc">一键检测全校教学异常（秒级响应）</p>
        <el-button
          type="primary"
          :loading="anomalyLoading"
          style="width:100%"
          @click="scanAnomalies"
        >
          开始扫描
        </el-button>
        <div v-if="anomalyResult" class="result-box">
          <div v-if="anomalyResult.scoreDropClasses?.length" class="anomaly-group">
            <h5>📉 成绩下滑班级</h5>
            <el-tag
              v-for="c in anomalyResult.scoreDropClasses"
              :key="c.classId"
              type="danger"
              size="small"
              style="margin:2px"
            >
              {{ c.className }} ↓{{ c.drop }}分
            </el-tag>
          </div>
          <div v-if="anomalyResult.lowSubmitClasses?.length" class="anomaly-group">
            <h5>📋 提交率低班级</h5>
            <el-tag
              v-for="c in anomalyResult.lowSubmitClasses"
              :key="c.classId"
              type="warning"
              size="small"
              style="margin:2px"
            >
              {{ c.className }} {{ c.rate }}%
            </el-tag>
          </div>
          <div v-if="anomalyResult.inactiveTeachers?.length" class="anomaly-group">
            <h5>😴 不活跃教师</h5>
            <el-tag
              v-for="t in anomalyResult.inactiveTeachers"
              :key="t.teacherId"
              type="info"
              size="small"
              style="margin:2px"
            >
              {{ t.teacherName }} {{ t.days }}天
            </el-tag>
          </div>
          <div v-if="anomalyResult.backlogTeachers?.length" class="anomaly-group">
            <h5>📚 批改积压教师</h5>
            <el-tag
              v-for="t in anomalyResult.backlogTeachers"
              :key="t.teacherId"
              type="danger"
              size="small"
              style="margin:2px"
            >
              {{ t.teacherName }} {{ t.count }}份
            </el-tag>
          </div>
          <div v-if="anomalyResult.overdueIssues?.length" class="anomaly-group">
            <h5>⏰ 超期问题</h5>
            <el-tag
              v-for="i in anomalyResult.overdueIssues"
              :key="i.issueId"
              type="danger"
              size="small"
              style="margin:2px"
            >
              {{ i.title }} {{ i.days }}天
            </el-tag>
          </div>
          <div v-if="anomalyResult.totalAlerts" class="anomaly-group">
            <h5>🔔 未读预警 <el-badge :value="anomalyResult.totalAlerts" type="danger" /></h5>
          </div>
          <div v-if="noAnomalies" style="color:var(--success-color);text-align:center;padding:20px">✅ 未发现异常</div>
        </div>
      </el-card>

      <el-card class="ai-card">
        <template #header><div class="card-title">💡 智能建议</div></template>
        <p class="card-desc">AI分析异常数据，推荐重点关注对象</p>
        <el-button
          type="primary"
          :loading="recLoading"
          style="width:100%"
          @click="getRecs"
        >
          获取建议
        </el-button>
        <div v-if="recList.length" class="result-box">
          <div v-for="r in recList" :key="(r.id||'') + (r.name||'')" class="rec-item">
            <div class="rec-header">
              <el-tag :type="r.type === 'CLASS' ? 'primary' : 'warning'" size="small">{{ r.type === 'CLASS' ? '班级' : r.type === 'TEACHER' ? '教师' : '问题' }}</el-tag>
              <span class="rec-priority">⭐ {{ r.priority }}</span>
            </div>
            <div class="rec-name">{{ r.name }}</div>
            <div class="rec-reason">{{ r.reason }}</div>
          </div>
        </div>
      </el-card>

      <el-card class="ai-card">
        <template #header><div class="card-title">📚 教研分析</div></template>
        <p class="card-desc">AI分析教研组/备课组质量</p>
        <div style="display:flex;gap:8px">
          <el-button
            type="primary"
            :loading="researchLoading"
            style="flex:1"
            @click="analyzeResearch"
          >
            教研活动
          </el-button>
          <el-button
            type="primary"
            :loading="prepLoading"
            style="flex:1"
            @click="analyzePrep"
          >
            备课活动
          </el-button>
        </div>
        <div v-if="researchResult" class="result-box markdown-box" v-html="researchResult" />
      </el-card>
    </div>

    <p class="ai-disclaimer">AI生成内容仅供参考，请结合实际情况判断。</p>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getWeeklySummary, getAnomalies, getRecommendations, getTeachingResearchAnalysis, getLessonPrepAnalysis } from '@/api/inspector'
import { renderMarkdown } from '@/utils/markdown'

const weeklyLoading = ref(false)
const weeklyResult = ref('')
const weeklyForm = ref({ start: '', end: '' })

const anomalyLoading = ref(false)
const anomalyResult = ref(null)
const noAnomalies = computed(() => {
  const a = anomalyResult.value
  if (!a) return false
  return !(a.scoreDropClasses?.length || a.lowSubmitClasses?.length || a.inactiveTeachers?.length || a.backlogTeachers?.length || a.overdueIssues?.length)
})

const recLoading = ref(false)
const recList = ref([])

const researchLoading = ref(false)
const prepLoading = ref(false)
const researchResult = ref('')

const genWeekly = async () => {
  weeklyLoading.value = true
  try {
    const res = await getWeeklySummary({ weekStart: weeklyForm.value.start, weekEnd: weeklyForm.value.end })
    if (res.code === 200) weeklyResult.value = renderMarkdown(res.data || '')
    else ElMessage.error(res.message)
  } catch { ElMessage.error('生成失败') }
  finally { weeklyLoading.value = false }
}

const scanAnomalies = async () => {
  anomalyLoading.value = true
  try {
    const res = await getAnomalies()
    if (res.code === 200) anomalyResult.value = res.data
    else ElMessage.error(res.message)
  } catch { ElMessage.error('扫描失败') }
  finally { anomalyLoading.value = false }
}

const getRecs = async () => {
  recLoading.value = true
  try {
    const res = await getRecommendations()
    if (res.code === 200) recList.value = (res.data.recommendations || []).sort((a, b) => (a.priority || 5) - (b.priority || 5))
    else ElMessage.error(res.message)
  } catch { ElMessage.error('获取失败') }
  finally { recLoading.value = false }
}

const analyzeResearch = async () => {
  researchLoading.value = true
  try {
    const res = await getTeachingResearchAnalysis()
    // 已通过 renderMarkdown() 消毒
    if (res.code === 200) researchResult.value = renderMarkdown(res.data || '')
    else ElMessage.error(res.message)
  } catch { ElMessage.error('分析失败') }
  finally { researchLoading.value = false }
}

const analyzePrep = async () => {
  prepLoading.value = true
  try {
    const res = await getLessonPrepAnalysis()
    if (res.code === 200) researchResult.value = renderMarkdown(res.data || '')
    else ElMessage.error(res.message)
  } catch { ElMessage.error('分析失败') }
  finally { prepLoading.value = false }
}
</script>

<style scoped lang="scss">
.inspector-ai { max-width: 1400px; margin: 0 auto; padding: var(--spacing-lg, 24px); }
.page-header { margin-bottom: 20px; .page-title { font-size: var(--fs-2xl); margin: 0; } .header-subtitle { font-size: var(--fs-sm); color: var(--text-secondary); } }
.ai-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 24px; }
.ai-card { .card-title { font-size: var(--fs-lg); font-weight: 600; } .card-desc { font-size: var(--fs-sm); color: var(--text-secondary); margin: 0 0 12px; } }
.result-box { margin-top: 16px; max-height: 500px; overflow-y: auto; }
.markdown-box { background: var(--bg-card); padding: 16px; border-radius: var(--radius-md); white-space: pre-wrap; font-size: var(--fs-md); line-height: 1.7; }
.anomaly-group { margin-bottom: 12px; h5 { margin: 0 0 4px; font-size: var(--fs-sm); color: var(--text-primary); } }
.rec-item { padding: 10px; margin-bottom: 8px; background: var(--bg-card); border-radius: var(--radius-md); }
.rec-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.rec-priority { font-size: var(--fs-xs); color: var(--warning-color); }
.rec-name { font-weight: 600; font-size: var(--fs-md); color: var(--text-primary); }
.rec-reason { font-size: var(--fs-sm); color: var(--text-secondary); margin-top: 2px; }
.ai-disclaimer { text-align: center; font-size: var(--fs-xs); color: var(--text-disabled); }
@media (max-width: 1024px) {
  .ai-cards { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 768px) {
  .inspector-ai { padding: var(--spacing-md, 16px); }
  .ai-cards { grid-template-columns: 1fr; }
}
</style>

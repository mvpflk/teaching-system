<template>
  <el-dialog
    v-model="visible"
    :title="'成绩分析 — ' + (data.taskTitle || '')"
    width="740px"
    :close-on-click-modal="false"
    destroy-on-close
    append-to-body
  >
    <div v-loading="loading" class="analysis-body">
      <el-empty v-if="!loading && errorMsg" :description="errorMsg" :image-size="80" />
      <el-empty v-else-if="!loading && !data.hasData" description="暂无成绩数据" :image-size="80" />
      <template v-else-if="data.hasData">
        <div class="stat-cards">
          <div class="stat-item"><span class="sv">{{ data.avgRate }}%</span><span class="sl">平均得分率</span></div>
          <div class="stat-item"><span class="sv">{{ data.maxRate }}%</span><span class="sl">最高得分率</span></div>
          <div class="stat-item"><span class="sv">{{ data.minRate }}%</span><span class="sl">最低得分率</span></div>
          <div class="stat-item"><span class="sv">{{ data.passRate }}%</span><span class="sl">及格率</span></div>
          <div class="stat-item"><span class="sv">{{ data.gradedCount }}/{{ data.participantCount }}</span><span class="sl">已批/参考</span></div>
        </div>
        <div style="text-align:right;margin-bottom:8px">
          <el-button
            size="small"
            type="primary"
            :loading="aiGenerating"
            @click="openWeakNodeDialog"
          >
            🤖 AI衍生训练
          </el-button>
          <el-button size="small" @click="exportCSV">导出 CSV</el-button>
        </div>

        <!-- 薄弱知识点选择抽屉（避免嵌套dialog的z-index问题） -->
        <el-drawer
          v-model="weakNodeDialogVisible"
          title="选择薄弱知识点"
          size="420px"
          append-to-body
          destroy-on-close
          direction="rtl"
        >
          <div style="padding:0 20px">
            <div style="margin-bottom:12px;color:var(--text-secondary);font-size:var(--fs-sm)">
              选中知识点将按薄弱程度比例分配题目，共生成 10 题
            </div>
            <el-checkbox
              v-model="selectAllWeak"
              :indeterminate="weakIndeterminate"
              style="margin-bottom:10px"
              @change="handleSelectAllWeak"
            >
              全选
            </el-checkbox>
            <div v-for="n in weakNodeList" :key="n.nodeId" class="wn-row">
              <el-checkbox v-model="selectedNodeIds" :value="n.nodeId">
                <span class="wn-name">{{ n.name }}</span>
                <el-tag
                  v-if="n.severity === 'SEVERE'"
                  size="small"
                  type="danger"
                  effect="plain"
                  class="wn-tag"
                >
                  严重({{ n.accuracy }}%)
                </el-tag>
                <el-tag
                  v-else
                  size="small"
                  type="warning"
                  effect="plain"
                  class="wn-tag"
                >
                  偏弱({{ n.accuracy }}%)
                </el-tag>
                <span class="wn-count">{{ n.questionCount }}题</span>
              </el-checkbox>
            </div>
          </div>
          <template #footer>
            <div style="padding:12px 20px;border-top:1px solid var(--border-light);display:flex;gap:8px;justify-content:flex-end">
              <el-button @click="weakNodeDialogVisible = false">取消</el-button>
              <el-button type="primary" :loading="aiGenerating" @click="generateRemedial">
                确认生成（{{ selectedNodeIds.length }}个知识点）
              </el-button>
            </div>
          </template>
        </el-drawer>
        <div class="chart-section">
          <div class="chart-title"><el-icon><TrendCharts /></el-icon> 得分率分布</div>
          <div ref="barChartRef" class="chart-box"></div>
        </div>
        <div v-if="data.questionAccuracy && data.questionAccuracy.length" class="chart-section">
          <div class="chart-title"><el-icon><Aim /></el-icon> 逐题正确率<span class="q-hint"> — 点击展开查看学生作答明细</span></div>
          <template
            v-for="q in sortedQuestions"
            :key="q.questionId"
          >
            <div
              class="q-row"
              :class="{ 'q-worst': q.isWorst, 'q-expanded': expandedId === q.questionId }"
              @click="toggleExpand(q)"
            >
              <span class="q-text">{{ q.questionText }}</span>
              <span class="q-meta">{{ q.correctCount }}/{{ q.correctCount + q.wrongCount }}</span>
              <div class="q-bar"><div class="q-fill" :style="{ width: q.accuracy + '%', background: q.accuracy >= 60 ? 'var(--el-color-success)' : q.accuracy >= 40 ? 'var(--el-color-warning)' : 'var(--el-color-danger)' }"></div></div>
              <span class="q-pct" :style="{ color: q.isWorst ? 'var(--el-color-danger)' : 'var(--text-secondary)' }">{{ q.accuracy }}%</span>
              <el-icon class="q-expand-icon"><ArrowRight v-if="expandedId !== q.questionId" /><ArrowDown v-else /></el-icon>
            </div>
            <!-- 展开的学生答题明细 — 显示在对应题目下方 -->
            <div v-if="expandedId === q.questionId && expandedAnswers.length" class="q-detail">
              <div class="qd-header">
                <span class="qd-title">学生作答明细</span>
                <el-tag size="small" type="success" effect="plain">✓ {{ expandedAnswers.filter(a=>a.isCorrect).length }}人正确</el-tag>
                <el-tag size="small" type="danger" effect="plain">✗ {{ expandedAnswers.filter(a=>!a.isCorrect).length }}人错误</el-tag>
              </div>
              <!-- 客观题选项展示 -->
              <div v-if="q.options && parseOptions(q.options).length" class="qd-options">
                <div class="qd-options-title">题目选项</div>
                <div
                  v-for="(opt, oi) in parseOptions(q.options)"
                  :key="oi"
                  class="qd-option-item"
                  :class="{ 'qd-option-correct': isOptionCorrect(q.correctAnswer, oi) }"
                >
                  <span class="qd-option-label">{{ String.fromCharCode(65 + oi) }}.</span>
                  <span class="qd-option-text">{{ opt }}</span>
                  <el-icon v-if="isOptionCorrect(q.correctAnswer, oi)" class="qd-option-check"><Check /></el-icon>
                </div>
              </div>
              <div class="qd-grid">
                <div
                  v-for="a in expandedAnswers"
                  :key="a.studentName"
                  class="qd-item"
                  :class="{ 'qd-correct': a.isCorrect, 'qd-wrong': !a.isCorrect }"
                >
                  <span class="qd-name">{{ a.studentName }}</span>
                  <span class="qd-answer" :title="a.studentAnswer">
                    <template v-if="q.options && parseOptions(q.options).length && a.studentAnswer">
                      <span
                        v-for="ch in a.studentAnswer"
                        :key="ch"
                        class="qd-opt-ch"
                        :class="isOptionCorrect(q.correctAnswer, ch.charCodeAt(0) - 65) ? 'ch-correct' : 'ch-wrong'"
                      >{{ ch }}</span>
                    </template>
                    <template v-else>{{ a.studentAnswer || '(空)' }}</template>
                  </span>
                  <el-icon v-if="a.isCorrect" class="qd-icon-correct"><Check /></el-icon>
                  <el-icon v-else class="qd-icon-wrong"><Close /></el-icon>
                </div>
              </div>
            </div>
          </template>
        </div>
      </template>
    </div>
  </el-dialog>

</template>

<script setup>
import { ref, computed, watch, nextTick, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { generateRemedialTask, getScoreAnalysis } from '@/api/task'
import { downloadFile } from '@/utils/request'
import echarts from '@/utils/echarts'
import { TrendCharts, Aim, ArrowDown, ArrowRight, Check, Close } from '@element-plus/icons-vue'
import { parseOptions } from '@/utils/questionHelpers'

const props = defineProps({ modelValue: Boolean, taskId: [Number, String] })
const emit = defineEmits(['update:modelValue'])
const visible = ref(false), loading = ref(false), data = ref({}), barChartRef = ref(null), errorMsg = ref('')

const expandedId = ref(null)
const expandedAnswers = computed(() => {
  if (!expandedId.value || !data.value.questionAccuracy) return []
  const q = data.value.questionAccuracy.find(q => q.questionId === expandedId.value)
  return q?.studentAnswers || []
})

const toggleExpand = (q) => {
  expandedId.value = expandedId.value === q.questionId ? null : q.questionId
}

/** 判断选项索引(0-based)是否在正确答案中，兼容单选"A"和多选"AB" */
const isOptionCorrect = (correctAnswer, optionIndex) => {
  if (!correctAnswer || optionIndex == null || optionIndex < 0) return false
  return correctAnswer.includes(String.fromCharCode(65 + optionIndex))
}

const aiGenerating = ref(false)
const weakNodeDialogVisible = ref(false)
const weakNodeList = ref([])
const selectedNodeIds = ref([])

const selectAllWeak = computed({
  get: () => weakNodeList.value.length > 0 && selectedNodeIds.value.length === weakNodeList.value.length,
  set: () => {}
})
const weakIndeterminate = computed(() =>
  selectedNodeIds.value.length > 0 && selectedNodeIds.value.length < weakNodeList.value.length
)
const handleSelectAllWeak = (val) => {
  selectedNodeIds.value = val ? weakNodeList.value.map(n => n.nodeId) : []
}

const openWeakNodeDialog = () => {
  weakNodeList.value = data.value.weakNodeSummary || []
  if (weakNodeList.value.length === 0) {
    ElMessage.warning('暂无薄弱知识点数据')
    return
  }
  selectedNodeIds.value = weakNodeList.value.map(n => n.nodeId)
  weakNodeDialogVisible.value = true
}

const generateRemedial = async () => {
  if (selectedNodeIds.value.length === 0) {
    ElMessage.warning('请选择至少一个薄弱知识点')
    return
  }
  weakNodeDialogVisible.value = false
  aiGenerating.value = true
  try {
    const res = await generateRemedialTask(props.taskId, { nodeIds: selectedNodeIds.value })
    if (res.code === 200) {
      const d = res.data || {}
      ElMessage.success('已创建衍生训练任务「' + (d.taskTitle || '') + '」（' + (d.questionCount || 0) + '题）')
    } else { ElMessage.error(res.message || '生成失败') }
  } catch (e) { ElMessage.error('AI生成失败') }
  finally { aiGenerating.value = false }
}

let chartInstance = null

const sortedQuestions = computed(() => {
  if (!data.value.questionAccuracy) return []
  return [...data.value.questionAccuracy].sort((a, b) => a.accuracy - b.accuracy)
})

const loadData = async () => {
  if (!props.taskId) return
  loading.value = true
  try {
    const res = await getScoreAnalysis(props.taskId)
    if (res.code === 200) {
      data.value = res.data || {}
      await nextTick()
      if (data.value.hasData) renderChart()
    } else {
      errorMsg.value = res.message || '加载失败'
    }
  } catch (e) {
    errorMsg.value = e?.response?.data?.message || e?.message || '加载成绩分析失败'
  } finally { loading.value = false }
}

const renderChart = async () => {
  if (!barChartRef.value || !data.value.distribution) return
  if (chartInstance) chartInstance.dispose()
  chartInstance = echarts.init(barChartRef.value)
  const colors = (await import('@/utils/chartColors')).CHART_COLORS
  const dist = data.value.distribution
  chartInstance.setOption({
    tooltip: { trigger: 'axis', valueFormatter: v => v + '人' },
    xAxis: { type: 'category', data: dist.map(d => d.label), axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', name: '人数', minInterval: 1 },
    series: [{ type: 'bar', data: dist.map(d => d.count), barMaxWidth: 50, itemStyle: { color: colors.primary } }],
    grid: { left: 40, right: 20, top: 10, bottom: 40 }
  })
}

const exportCSV = () => {
  const filename = `score-analysis-${props.taskId}.csv`
  downloadFile(`/api/task/${props.taskId}/actions/score-analysis/export`, filename)
    .catch(() => {})
}

watch(() => props.modelValue, (v) => { visible.value = v; if (v) { data.value = {}; loadData() } })
watch(visible, (v) => emit('update:modelValue', v))

onUnmounted(() => { chartInstance?.dispose() })
</script>

<style scoped>
.analysis-body { min-height: 200px; }
.stat-cards { display: grid; grid-template-columns: repeat(5, 1fr); gap: 10px; margin-bottom: 14px; }
.stat-item { text-align: center; padding: 12px 8px; background: var(--bg-section); border-radius: var(--radius-md); }
.sv { display: block; font-size: var(--fs-xl); font-weight: 700; color: var(--text-primary); }
.sl { display: block; font-size: var(--fs-xs); color: var(--text-secondary); margin-top: 2px; }
.chart-section { margin-bottom: 18px; }
.chart-title { font-size: var(--fs-md); font-weight: 600; color: var(--text-primary); margin-bottom: 8px; }
.chart-box { width: 100%; height: 220px; }
.q-row { display: flex; align-items: center; gap: 8px; padding: 6px 0; border-bottom: 1px solid var(--bg-section); font-size: var(--fs-sm); cursor: pointer; transition: background .12s; }
.q-row:hover { background: var(--bg-hover); }
.q-row.q-worst { background: var(--bg-danger-light); margin: 0 -8px; padding: 6px 8px; border-radius: var(--radius-sm); }
.q-row.q-expanded { background: var(--primary-light); margin: 0 -8px; padding: 6px 8px; border-radius: var(--radius-sm); }
.q-text { flex: 1; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; color: var(--text-primary); line-height: 1.4; word-break: break-all; }
.q-meta { font-size: var(--fs-xs); color: var(--text-secondary); flex-shrink: 0; width: 45px; text-align: right; }
.q-bar { width: 100px; height: 12px; background: var(--bg-secondary); border-radius: var(--radius-xs); overflow: hidden; flex-shrink: 0; }
.q-fill { height: 100%; border-radius: var(--radius-xs); transition: width 0.5s; }
.q-pct { width: 40px; text-align: right; font-weight: 600; font-size: var(--fs-xs); }
.q-expand-icon { flex-shrink: 0; color: var(--text-disabled); font-size: var(--fs-xs); }
.q-hint { font-size: var(--fs-xs); color: var(--text-disabled); font-weight: 400; }

.q-detail { margin: 8px 0 14px; padding: 12px; background: var(--bg-card); border: 1px solid var(--border-base); border-radius: var(--radius-md); }
.qd-header { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.qd-title { font-size: var(--fs-md); font-weight: 600; color: var(--text-primary); }
.qd-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: 6px; }
.qd-item { display: flex; align-items: center; gap: 6px; padding: 6px 10px; border-radius: var(--radius-sm); font-size: var(--fs-sm); }
.qd-correct { background: var(--bg-success-light); color: var(--el-color-success); }
.qd-wrong { background: var(--bg-danger-light); color: var(--el-color-danger); }
.qd-name { font-weight: 500; min-width: 40px; }
.qd-answer { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: var(--fs-xs); opacity: .8; }
.qd-icon-correct { color: var(--el-color-success); font-size: var(--fs-md); }
.qd-icon-wrong { color: var(--el-color-danger); font-size: var(--fs-md); }

/* 客观题选项展示 */
.qd-options { margin-bottom: 12px; padding: 10px 12px; background: var(--bg-section); border-radius: var(--radius-sm); }
.qd-options-title { font-size: var(--fs-xs); color: var(--text-secondary); margin-bottom: 6px; font-weight: 600; }
.qd-option-item { display: flex; align-items: flex-start; gap: 6px; padding: 4px 8px; border-radius: var(--radius-xs); font-size: var(--fs-sm); margin-bottom: 2px; transition: background .12s; }
.qd-option-correct { background: var(--bg-success-light); font-weight: 600; }
.qd-option-label { flex-shrink: 0; font-weight: 600; min-width: 18px; }
.qd-option-text { flex: 1; line-height: 1.5; }
.qd-option-check { color: var(--el-color-success); flex-shrink: 0; font-size: var(--fs-sm); margin-top: 2px; }

/* 学生答案选项字母高亮 */
.qd-opt-ch { display: inline-block; padding: 0 3px; border-radius: 2px; font-weight: 600; margin: 0 1px; }
.ch-correct { color: var(--el-color-success); background: rgba(0,128,0,.1); }
.ch-wrong { color: var(--el-color-danger); background: rgba(255,0,0,.1); }

.wn-row { padding: 6px 0; border-bottom: 1px solid var(--bg-section); font-size: var(--fs-sm); }
.wn-name { font-weight: 500; margin-right: 8px; }
.wn-tag { margin: 0 4px; font-size: var(--fs-xs); }
.wn-count { font-size: var(--fs-xs); color: var(--text-secondary); margin-left: 4px; }

@media (max-width: 768px) {
  .stat-cards { grid-template-columns: repeat(3, 1fr); }
  .chart-box { height: 180px; }
  .q-bar { width: 50px; }
  .qd-grid { grid-template-columns: repeat(auto-fill, minmax(100px, 1fr)); }
}
</style>

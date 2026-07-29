<template>
  <div class="step-player">
    <div class="sp-header">
      <el-button text @click="$router.back()">← 返回</el-button>
      <h3>{{ task?.title || '加载中...' }}</h3>
    </div>

    <!-- 步骤进度条 -->
    <div class="sp-progress" v-if="steps.length > 0">
      <div
        v-for="(step, i) in steps"
        :key="i"
        class="sp-progress-dot"
        :class="{ completed: step._completed, current: i === currentIndex }"
        @click="goToStep(i)"
      >
        <span class="dot-num">{{ i + 1 }}</span>
        <span class="dot-label">{{ step.title }}</span>
      </div>
    </div>

    <!-- 当前步骤交互区 -->
    <div class="sp-body" v-if="currentStep">
      <div class="step-header">
        <el-tag size="small" :type="stepTagType(currentStep.type)">{{ stepTypeLabel(currentStep.type) }}</el-tag>
        <span class="step-score" v-if="currentStep.score?.max">满分: {{ currentStep.score.max }}</span>
      </div>
      <component
        :is="stepComponent"
        :key="currentIndex"
        :step="currentStep"
        :step-index="currentIndex"
        :task-id="taskId"
        :model-value="stepData[currentIndex]"
        @update:model-value="(v) => stepData[currentIndex] = v"
        @saved="onStepSaved"
      />
    </div>

    <!-- 空状态 -->
    <el-empty v-if="!loading && steps.length === 0" description="该任务暂无步骤" />

    <!-- 底部操作栏 -->
    <div class="sp-footer" v-if="steps.length > 0">
      <el-button :disabled="currentIndex === 0" @click="prevStep">上一步</el-button>
      <el-button @click="saveDraft" :loading="saving">暂存草稿</el-button>
      <el-button v-if="currentIndex < steps.length - 1" type="primary" @click="nextStep">下一步</el-button>
      <el-button v-else type="success" :loading="submitting" @click="submitAll">全部提交</el-button>
    </div>

    <!-- 技能雷达图弹窗 -->
    <el-dialog v-model="showSkillRadar" title="实训完成 — 技能评估" width="560px" :close-on-click-modal="false">
      <div class="radar-content">
        <div ref="radarChartRef" class="radar-chart"></div>
        <div class="radar-summary">
          <p>🎉 恭喜完成实训任务「{{ task?.title || '实训任务' }}」</p>
          <p>技能雷达图展示了你在各维度的表现，持续练习可全面提升。</p>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="router.push({ name: 'TrainingHub' })">返回实训中心</el-button>
        <el-button @click="$router.back()">返回任务列表</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, markRaw } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getStudentSteps, saveStepProgress, submitAllSteps } from '@/api/training'
import TextStep from './components/steps/TextStep.vue'
import FileStep from './components/steps/FileStep.vue'
import ChoiceStep from './components/steps/ChoiceStep.vue'
import SimStep from './components/steps/SimStep.vue'
import OfficeStep from './components/steps/OfficeStep.vue'
import WebStep from './components/steps/WebStep.vue'
import ExcelStep from './components/steps/ExcelStep.vue'
import PptStep from './components/steps/PptStep.vue'
import SqlStep from './components/steps/SqlStep.vue'
import { primaryColor } from '@/utils/theme'

const route = useRoute()
const router = useRouter()
const taskId = Number(route.params.taskId)
const task = ref(null)
const steps = ref([])
const currentIndex = ref(0)
const stepData = ref({})
const submitting = ref(false)
const saving = ref(false)
const loading = ref(true)
const showSkillRadar = ref(false)
const radarChartRef = ref(null)
let radarInstance = null

const currentStep = computed(() => steps.value[currentIndex.value] || null)

const COMPONENT_MAP = {
  text: markRaw(TextStep),
  file: markRaw(FileStep),
  sim: markRaw(SimStep),
  office: markRaw(OfficeStep),
  web: markRaw(WebStep),
  choice: markRaw(ChoiceStep),
  excel: markRaw(ExcelStep),
  ppt: markRaw(PptStep),
  sql: markRaw(SqlStep)
}
const stepComponent = computed(() => {
  if (!currentStep.value) return null
  return COMPONENT_MAP[currentStep.value.type] || TextStep
})

function stepTypeLabel(type) {
  return { text: '文字论述', file: '文件提交', sim: '仿真操作', office: 'Word 文档', web: '网页文件', choice: '选择题', excel: 'Excel 表格', ppt: 'PPT 演示', sql: 'SQL 查询' }[type] || type
}
function stepTagType(type) {
  return { text: '', file: 'warning', sim: 'danger', office: '', web: 'warning', choice: 'success', excel: 'success', ppt: 'warning', sql: 'danger' }[type] || ''
}

const DRAFT_KEY = () => `training_draft_${taskId}`

async function load() {
  loading.value = true
  try {
    const res = await getStudentSteps(taskId)
    if (res.code === 200) {
      task.value = res.data.task || {}
      steps.value = (res.data.steps || []).map(s => ({ ...s }))  // 保留服务端 _completed 状态，不强制覆盖
      // 恢复草稿
      const draft = loadDraft()
      if (draft) {
        stepData.value = draft.data || {}
        currentIndex.value = Math.min(draft.currentIndex || 0, steps.value.length - 1)
      }
    }
  } catch (e) {
    // 实训模块实验期暂停（后端 410），静默展示空态，不弹错误提示
    steps.value = []
  } finally {
    loading.value = false
  }
}

function loadDraft() {
  try {
    const raw = localStorage.getItem(DRAFT_KEY())
    return raw ? JSON.parse(raw) : null
  } catch { return null }
}
function saveDraftToLocal() {
  try {
    localStorage.setItem(DRAFT_KEY(), JSON.stringify({
      data: stepData.value,
      currentIndex: currentIndex.value,
      savedAt: new Date().toISOString()
    }))
  } catch { /* localStorage full, ignore */ }
}

function goToStep(i) {
  if (currentStep.value) saveCurrentStepSilent()
  currentIndex.value = i
}

function prevStep() { if (currentIndex.value > 0) goToStep(currentIndex.value - 1) }
function nextStep() { if (currentIndex.value < steps.value.length - 1) goToStep(currentIndex.value + 1) }

let saveTimer = null
function onStepSaved() {
  clearTimeout(saveTimer)
  saveTimer = setTimeout(() => saveCurrentStepSilent(), 500)
}

async function saveCurrentStepSilent() {
  try {
    await saveStepProgress(taskId, currentIndex.value, stepData.value[currentIndex.value] || {})
    saveDraftToLocal()
  } catch { /* 静默失败 */ }
}

async function saveDraft() {
  saving.value = true
  await saveCurrentStepSilent()
  ElMessage.success('草稿已保存')
  saving.value = false
}

async function submitAll() {
  submitting.value = true
  try {
    await saveCurrentStepSilent()
    const res = await submitAllSteps(taskId)
    if (res.code === 200) {
      localStorage.removeItem(DRAFT_KEY())
      ElMessage.success('提交成功！')
      showSkillRadar.value = true
      await nextTick()
      renderSkillRadar()
    } else {
      ElMessage.error(res.message || '提交失败')
    }
  } catch (e) {
    ElMessage.error('提交失败: ' + (e.message || '网络错误'))
  } finally {
    submitting.value = false
  }
}

async function renderSkillRadar() {
  if (!radarChartRef.value) return
  try {
    const echarts = await import('@/utils/echarts')
    if (radarInstance) radarInstance.dispose()
    radarInstance = echarts.init(radarChartRef.value)

    // 根据步骤类型构建维度：理论理解/操作技能/创新思维/规范遵循/综合应用
    const dimensions = ['理论理解', '操作技能', '创新思维', '规范遵循', '综合应用']
    const scores = [0, 0, 0, 0, 0]
    let stepCount = 0

    for (const [idx, s] of steps.value.entries()) {
      const score = stepData.value[idx]?.checkResult?.score
        || stepData.value[idx]?.sqlResult?.score
        || (stepData.value[idx] ? 80 : 0) // 默认80分（主观题）
      const type = s.type
      if (type === 'text' || type === 'choice') scores[0] += score   // 理论理解
      else if (type === 'file' || type === 'office') scores[3] += score  // 规范遵循
      else if (type === 'sim' || type === 'sql') scores[1] += score     // 操作技能
      else if (type === 'web' || type === 'excel' || type === 'ppt') scores[2] += score  // 创新思维
      else scores[4] += score  // 综合应用
      stepCount++
    }

    const avgScores = stepCount > 0
      ? scores.map(s => Math.round(s / stepCount))
      : [60, 60, 60, 60, 60]

    radarInstance.setOption({
      tooltip: {},
      radar: {
        center: ['50%', '50%'],
        radius: '65%',
        indicator: dimensions.map((d, i) => ({ name: d, max: 100 })),
        axisName: { fontSize: 12 }
      },
      series: [{
        type: 'radar',
        data: [{ value: avgScores, name: '你的表现', areaStyle: { color: 'rgba(67,97,238,0.2)' } }],
        lineStyle: { color: primaryColor, width: 2 },
        itemStyle: { color: primaryColor }
      }]
    })
  } catch {}
}

onMounted(load)
onUnmounted(() => { clearTimeout(saveTimer); if (radarInstance) radarInstance.dispose() })
</script>

<style scoped>
.step-player { max-width: 900px; margin: 0 auto; padding: 16px; display: flex; flex-direction: column; min-height: 100vh; }
.sp-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.sp-header h3 { margin: 0; }
.sp-progress { display: flex; gap: 8px; margin-bottom: 20px; overflow-x: auto; padding-bottom: 8px; }
.sp-progress-dot { display: flex; align-items: center; gap: 6px; padding: 6px 12px; border-radius: 20px; cursor: pointer;
  border: 1px solid var(--border-light); white-space: nowrap; font-size: var(--fs-sm); transition: all 0.2s; }
.sp-progress-dot.completed { background: var(--color-success-light, #e6f7e6); border-color: var(--color-success, #67c23a); }
.sp-progress-dot.current { background: var(--primary-light, var(--primary-light)); border-color: var(--primary-color, var(--primary-color)); font-weight: 600; }
.dot-num { width: 22px; height: 22px; border-radius: 50%; background: var(--bg-page);
  display: flex; align-items: center; justify-content: center; font-size: var(--fs-xs); border: 1px solid var(--border-light); }
.sp-progress-dot.completed .dot-num { background: var(--color-success, #67c23a); color: #fff; border-color: transparent; }
.sp-progress-dot.current .dot-num { background: var(--primary-color, var(--primary-color)); color: #fff; border-color: transparent; }
.sp-body { flex: 1; margin-bottom: 16px; }
.step-header { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.step-score { font-size: var(--fs-sm); color: var(--text-secondary); }
.sp-footer { display: flex; justify-content: space-between; padding: 12px 0; border-top: 1px solid var(--border-light); }
.radar-chart { width: 100%; height: 320px; }
.radar-summary { text-align: center; margin-top: 12px; color: var(--text-secondary); }
.radar-summary p { margin: 4px 0; }
</style>

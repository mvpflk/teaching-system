<template>
  <el-drawer
    v-model="visible"
    title="任务详情"
    size="480px"
    :close-on-click-modal="false"
    @closed="emit('closed')"
  >
    <template v-if="loading">
      <el-skeleton :rows="6" animated />
    </template>
    <template v-else-if="detail && detail.hasData">
      <!-- 任务基本信息 -->
      <div class="td-section">
        <h4 class="td-section-title">📋 任务信息</h4>
        <div class="td-info-grid">
          <div class="td-info-item"><span class="td-label">类型</span><el-tag size="small">{{ TASK_TYPE_LABEL[detail.taskType] || detail.taskType }}</el-tag><el-tag v-if="taskSource" size="small" type="success" effect="plain" style="margin-left:4px">AI 生成</el-tag></div>
          <div class="td-info-item"><span class="td-label">学科</span><span>{{ detail.subject || '通用' }}</span></div>
          <div class="td-info-item"><span class="td-label">满分</span><span>{{ detail.totalScore }}分</span></div>
          <div class="td-info-item"><span class="td-label">截止</span><span>{{ fmtDate(detail.deadline) }}</span></div>
          <div class="td-info-item"><span class="td-label">题目</span><span>{{ detail.questionCount || 0 }}题</span></div>
          <div class="td-info-item"><span class="td-label">教师</span><span>{{ detail.teacherName }}</span></div>
        </div>
        <div v-if="detail.description" class="td-desc">{{ detail.description }}</div>
      </div>

      <!-- 提交与批改进度 -->
      <div class="td-section">
        <h4 class="td-section-title">📊 提交与批改</h4>
        <div class="td-progress-cards">
          <div class="td-pc green">
            <div class="td-pc-val">{{ detail.submittedCount }}</div>
            <div class="td-pc-lbl">已提交</div>
          </div>
          <div class="td-pc blue">
            <div class="td-pc-val">{{ detail.gradedCount }}</div>
            <div class="td-pc-lbl">已批改</div>
          </div>
          <div class="td-pc orange">
            <div class="td-pc-val">{{ detail.pendingCount }}</div>
            <div class="td-pc-lbl">待批改</div>
          </div>
        </div>
      </div>

      <!-- 成绩概览 -->
      <div v-if="detail.avgRate != null" class="td-section">
        <h4 class="td-section-title">📈 成绩概览</h4>
        <div class="td-score-grid">
          <div class="td-si"><span class="td-si-val">{{ detail.avgRate }}%</span><span class="td-si-lbl">平均得分率</span></div>
          <div class="td-si"><span class="td-si-val">{{ detail.maxRate }}%</span><span class="td-si-lbl">最高</span></div>
          <div class="td-si"><span class="td-si-val">{{ detail.minRate }}%</span><span class="td-si-lbl">最低</span></div>
          <div class="td-si"><span class="td-si-val">{{ detail.passRate }}%</span><span class="td-si-lbl">及格率</span></div>
        </div>
      </div>

      <!-- 逐题正确率 -->
      <div v-if="detail.questionAccuracy && detail.questionAccuracy.length" class="td-section">
        <h4 class="td-section-title">🎯 逐题正确率</h4>
        <div
          v-for="q in detail.questionAccuracy"
          :key="q.questionId"
          class="td-qa"
          :class="{ worst: q.isWorst }"
        >
          <div class="td-qa-text">{{ q.questionText }}</div>
          <div class="td-qa-bar-wrap">
            <div class="td-qa-bar"><div class="td-qa-fill" :style="{width:q.accuracy+'%'}"></div></div>
            <span class="td-qa-pct">{{ q.accuracy }}%</span>
          </div>
        </div>
      </div>

      <!-- 达标概况 -->
      <el-card v-if="retakeOverview" class="retake-overview-card" style="margin-top:12px;">
        <template #header>📊 达标概况</template>
        <div style="display:grid; grid-template-columns:repeat(3,1fr); gap:8px; text-align:center; margin-bottom:12px;">
          <div>
            <div style="font-size:24px; font-weight:700; color:var(--el-color-success);">{{ retakeOverview.firstPassed }}</div>
            <div style="font-size:12px; color:var(--text-secondary);">首次达标</div>
          </div>
          <div>
            <div style="font-size:24px; font-weight:700; color:var(--el-color-warning);">{{ retakeOverview.retakePassed }}</div>
            <div style="font-size:12px; color:var(--text-secondary);">重测达标</div>
          </div>
          <div>
            <div style="font-size:24px; font-weight:700; color:var(--el-color-info);">{{ retakeOverview.stillFailing }}</div>
            <div style="font-size:12px; color:var(--text-secondary);">待巩固</div>
          </div>
        </div>
        <el-progress
          :percentage="retakeOverview.totalStudents > 0 ? Math.round((retakeOverview.firstPassed + retakeOverview.retakePassed) / retakeOverview.totalStudents * 100) : 0"
          :text-inside="true"
          :stroke-width="20"
          status="success"
        />
        <div style="font-size:11px; color:var(--text-secondary); margin-top:6px;">
          统计口径：首次成绩计入平均分，达标率含重测通过
        </div>
      </el-card>

      <!-- 操作按钮 -->
      <div class="td-actions">
        <el-button type="primary" @click="goGrading">📝 去批改</el-button>
        <el-button type="success" @click="goAnalysis">📊 成绩分析</el-button>
      </div>
    </template>
    <el-empty v-else description="暂无数据" />
  </el-drawer>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getTaskDetail } from '@/api/classes'
import { getTaskSource } from '@/api/agent'
import { TASK_TYPE_LABEL } from '@/constants/taskType'
import dayjs from 'dayjs'

const props = defineProps({
  modelValue: Boolean,
  classId: [String, Number],
  taskId: [String, Number]
})
const emit = defineEmits(['update:modelValue', 'closed'])
const router = useRouter()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const loading = ref(false)
const detail = ref(null)
const taskSource = ref(null)

const retakeOverview = computed(() => detail.value?.retakeOverview || null)
const fmtDate = (d) => d ? dayjs(d).format('MM-DD HH:mm') : '-'

const load = async () => {
  if (!props.taskId) return
  loading.value = true
  try {
    const res = await getTaskDetail(props.classId, props.taskId)
    if (res.code === 200) detail.value = res.data
    const sr = await getTaskSource(props.taskId)
    if (sr.code === 200 && sr.data.source === 'ai') taskSource.value = sr.data
  } finally { loading.value = false }
}

watch(() => props.taskId, (id) => { if (id) load() })
watch(() => props.modelValue, (v) => { if (v && props.taskId) load() })

const goGrading = () => router.push(`/teacher/tasks/${props.taskId}/grading`)
const goAnalysis = () => router.push({ path: '/inspector/exams', query: { taskId: props.taskId } })
</script>

<style scoped lang="scss">
.td-section { margin-bottom: 20px; }
.td-section-title { font-size: var(--fs-md); font-weight: 600; margin: 0 0 10px; color: var(--text-primary); }
.td-info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.td-info-item { display: flex; align-items: center; gap: 8px; font-size: var(--fs-sm); }
.td-label { color: var(--text-secondary); min-width: 40px; }
.td-desc { margin-top: 8px; font-size: var(--fs-sm); color: var(--text-secondary); line-height: 1.6; }
.td-progress-cards { display: flex; gap: 12px; }
.td-pc { flex: 1; text-align: center; border-radius: 8px; padding: 14px 8px; }
.td-pc.green { background: var(--el-color-success-light, #e8f5e9); }
.td-pc.blue { background: var(--el-color-primary-light, #e3f2fd); }
.td-pc.orange { background: var(--el-color-warning-light, #fff3e0); }
.td-pc-val { font-size: var(--fs-2xl); font-weight: 700; }
.td-pc.green .td-pc-val { color: var(--el-color-success, var(--el-color-success)); }
.td-pc.blue .td-pc-val { color: var(--el-color-primary, #1565c0); }
.td-pc.orange .td-pc-val { color: var(--el-color-warning, #e65100); }
.td-pc-lbl { font-size: var(--fs-xs); color: var(--text-secondary); margin-top: 2px; }
.td-score-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.td-si { text-align: center; }
.td-si-val { display: block; font-size: var(--fs-xl); font-weight: 700; color: var(--text-primary); }
.td-si-lbl { display: block; font-size: var(--fs-xs); color: var(--text-secondary); }
.td-qa { margin-bottom: 8px; }
.td-qa.worst { background: #fff3e0; border-radius: 4px; padding: 4px 8px; }
.td-qa-text { font-size: var(--fs-xs); color: var(--text-primary); margin-bottom: 2px; }
.td-qa-bar-wrap { display: flex; align-items: center; gap: 8px; }
.td-qa-bar { flex: 1; height: 8px; background: var(--bg-secondary); border-radius: 4px; overflow: hidden; }
.td-qa-fill { height: 100%; background: var(--el-color-primary); border-radius: 4px; min-width: 2px; }
.worst .td-qa-fill { background: var(--el-color-danger); }
.td-qa-pct { font-size: var(--fs-xs); font-weight: 600; color: var(--text-secondary); width: 36px; text-align: right; }
.td-actions { display: flex; gap: 8px; margin-top: 8px; }
@media (max-width: 768px) {
  .td-info-grid { grid-template-columns: 1fr; }
  .td-score-grid { grid-template-columns: repeat(2, 1fr); }
  .td-progress-cards { flex-direction: column; }
}
</style>

<template>
  <div class="ph-page">
    <div class="ph-navbar">
      <el-button text class="ph-navbar-back" @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回</span>
      </el-button>
      <span class="ph-navbar-title">偏科提分</span>
      <span class="ph-navbar-placeholder"></span>
    </div>

    <div class="ph-subjects">
      <div
        v-for="s in subjects"
        :key="s.key"
        class="ph-subject-card"
        :class="{ active: selectedSubject?.key === s.key }"
        @click="selectSubject(s)"
      >
        <span class="ph-subj-icon">{{ s.icon }}</span>
        <div class="ph-subj-info">
          <span class="ph-subj-name">{{ s.label }}</span>
          <span class="ph-subj-meta">{{ s.desc }}</span>
        </div>
        <div class="ph-subj-right">
          <el-tag
            v-if="subjectDiagStatus[s.key]?.score !== undefined"
            :type="subjectDiagStatus[s.key].score >= 60 ? 'success' : subjectDiagStatus[s.key].score >= 35 ? 'warning' : 'danger'"
            size="small"
            effect="plain"
          >
            {{ subjectDiagStatus[s.key].score }}分
          </el-tag>
          <span v-else class="ph-subj-new">待诊断</span>
          <el-icon class="ph-subj-arrow"><ArrowRight /></el-icon>
        </div>
      </div>
    </div>

    <template v-if="selectedSubject">
      <el-tabs v-model="activeTabName" class="ph-tabs">
        <el-tab-pane label="诊断" name="diagnosis">
          <DiagnosisOverview ref="diagRef" :subject="selectedSubject" />
        </el-tab-pane>
        <el-tab-pane label="学习包" name="pack">
          <LearningPackList :subject="selectedSubject" :week-no="weekNo" />
        </el-tab-pane>
        <el-tab-pane label="小测" name="test">
          <OnlineTestTab :subject="selectedSubject" />
        </el-tab-pane>
        <el-tab-pane label="报告" name="report">
          <ProgressReport :subject="selectedSubject" @show-resources="openLr" />
        </el-tab-pane>
      </el-tabs>
    </template>

    <EmptyState
      v-else
      description="选择上方学科卡片开始精准提分"
    />
  </div>

  <LearningResourcesDialog
    ref="lrRef"
    v-model:visible="lrVisible"
    :node="lrNode"
    :subject="selectedSubject"
    @open-question="openQuestion"
  />

  <QuestionPreviewDialog
    v-model:visible="previewVisible"
    :question="previewQuestionData"
    :subject="selectedSubject"
    @answer-submitted="onPreviewAnswered"
  />
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { getDashboard } from '@/api/precision'
import EmptyState from '@/components/common/EmptyState.vue'
import DiagnosisOverview from './components/DiagnosisOverview.vue'
import LearningPackList from './components/LearningPackList.vue'
import OnlineTestTab from './components/OnlineTestTab.vue'
import ProgressReport from './components/ProgressReport.vue'
import LearningResourcesDialog from './components/LearningResourcesDialog.vue'
import QuestionPreviewDialog from './components/QuestionPreviewDialog.vue'

const router = useRouter()

const subjects = [
  { key: '英语[职高]', label: '英语提分', icon: '📖', desc: '词汇+阅读+语法·每天15分钟' },
  { key: '数学[职高]', label: '数学提分', icon: '📐', desc: '诊断补漏+分步提示·每天15分钟' }
]
const selectedSubject = ref(null)
const activeTabName = ref('diagnosis')
const weekNo = ref(1)
const subjectDiagStatus = ref({})

const diagRef = ref(null)
const lrRef = ref(null)
const lrVisible = ref(false)
const lrNode = ref(null)
const previewVisible = ref(false)
const previewQuestionData = ref(null)

function selectSubject(s) {
  if (s.key === '英语[职高]') { router.push('/precision/english'); return }
  selectedSubject.value = s
  activeTabName.value = 'diagnosis'
  diagRef.value?.resetDiagnosis()
}

function openLr(w) {
  lrNode.value = w
  lrVisible.value = true
  lrRef.value?.open(w)
}

function openQuestion(q) {
  previewQuestionData.value = q
  previewVisible.value = true
}

function onPreviewAnswered() {}

onMounted(async () => {
  try {
    const res = await getDashboard()
    if (res.code === 200 && res.data?.profile) {
      const pf = res.data.profile
      for (const s of subjects) {
        if (pf[s.key]?.diagnoseScore !== undefined) {
          subjectDiagStatus.value[s.key] = { score: pf[s.key].diagnoseScore }
        }
      }
    }
  } catch (e) {
    console.error('加载仪表盘失败:', e)
  }
})
</script>

<style scoped>
.ph-page { min-height: 100vh; background: var(--bg-page); padding-bottom: 32px; }

/* 顶部导航栏：sticky 实现固定+占位 */
.ph-navbar {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  height: 48px;
  background: var(--bg-card, #fff);
  border-bottom: 1px solid var(--border-light, #ebeef5);
  padding: 0 12px;
}
.ph-navbar-back {
  color: var(--primary-color);
  font-size: var(--fs-md);
}
.ph-navbar-back :deep(.el-icon) { margin-right: 2px; }
.ph-navbar-title {
  flex: 1;
  text-align: center;
  font-size: var(--fs-lg);
  font-weight: 600;
  color: var(--text-primary);
}
.ph-navbar-placeholder { width: 56px; flex-shrink: 0; }

.ph-subjects { display: flex; flex-direction: column; gap: 10px; padding: 16px; }
.ph-subject-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  background: var(--bg-card, #fff);
  border: 1px solid var(--border-base, #e8e8ed);
  border-radius: var(--radius-md, 8px);
  cursor: pointer;
  transition: border-color 0.15s ease;
}
.ph-subject-card:hover { border-color: var(--primary-color); }
.ph-subject-card.active { border-color: var(--primary-color); background: var(--primary-light); }
.ph-subj-icon { font-size: 28px; }
.ph-subj-info { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.ph-subj-name { font-size: var(--fs-md); font-weight: 600; color: var(--text-primary); }
.ph-subj-meta { font-size: var(--fs-xs); color: var(--text-secondary); }
.ph-subj-right { display: flex; align-items: center; gap: 8px; }
.ph-subj-new { font-size: var(--fs-xs); color: var(--text-disabled); }
.ph-subj-arrow { color: var(--text-disabled); }

.ph-tabs { padding: 0 16px; }
.ph-tabs :deep(.el-tabs__header) { margin-bottom: 12px; }
</style>
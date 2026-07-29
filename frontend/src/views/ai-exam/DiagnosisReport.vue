<template>
  <div v-loading="loading" class="diag-page">
    <DiagnosisHeader
      :report-title="data.taskTitle || route.query.title || '—'"
      :subject-label="data.subject"
      :class-count="classCount"
      :student-count="studentCount"
      @back="router.back()"
    />

    <el-alert v-if="polling" type="info" :closable="false" show-icon style="margin-bottom:16px">
      <template #title>AI 诊断分析生成中，请稍候...（已等待 {{ pollSeconds }} 秒）</template>
    </el-alert>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon style="margin-bottom:16px">
      <el-button text size="small" @click="fetchAll">重试</el-button>
    </el-alert>

    <el-tabs v-if="ready" v-model="activeTab" type="border-card" class="diag-tabs">
      <el-tab-pane name="overview">
        <template #label><span><el-icon><DataAnalysis /></el-icon> 班级概览</span></template>
        <OverviewTab
          :sorted-classes="sortedClasses"
          :score-distribution="scoreDistribution"
          :label-counts="labelCounts"
          :ai-text="aiText"
          :ai-running="aiRunning"
          @trigger-analysis="triggerAiAnalysis"
        />
      </el-tab-pane>

      <el-tab-pane name="perQuestion">
        <template #label>
          <span><el-icon><List /></el-icon> 逐题分析
            <el-badge :value="weakQuestionCount" :hidden="!weakQuestionCount" style="margin-left:4px" />
          </span>
        </template>
        <PerQuestionTab
          :questions="filteredQuestions"
          :weak-question-count="weakQuestionCount"
          :kp-question-filter="kpQuestionFilter"
          :active-kp-filter-name="activeKpFilterName"
          :question-filter="questionFilter"
          :expanded-questions="expandedQuestions"
          :classes="data.classes || []"
          @filter-change="questionFilter = $event"
          @toggle-expand="toggleExpand"
          @clear-kp-filter="clearKpFilter"
        />
      </el-tab-pane>

      <el-tab-pane name="perStudent">
        <template #label>
          <span><el-icon><User /></el-icon> 学生诊断
            <el-badge :value="labelCounts['起步期'] || 0" :hidden="!labelCounts['起步期']" style="margin-left:4px" />
          </span>
        </template>
        <PerStudentTab
          :students="filteredStudents"
          :student-filter="studentFilter"
          @student-filter-change="studentFilter = $event"
        />
      </el-tab-pane>

      <el-tab-pane v-if="data.perKp?.length" name="perKp">
        <template #label>
          <span><el-icon><Connection /></el-icon> 知识点对比
            <el-badge :value="(data.highlightedKps||[]).length" :hidden="!(data.highlightedKps||[]).length" style="margin-left:4px" />
          </span>
        </template>
        <PerKpTab
          :kp-list="sortedKps"
          :highlighted-kps="data.highlightedKps || []"
          :kp-data-quality-msg="kpDataQualityMsg"
          :kp-data-quality-type="kpDataQualityType"
          @drill-to-kp="drillToKpQuestions"
          @generate-material="generateMaterial"
        />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { DataAnalysis, List, User, Connection } from '@element-plus/icons-vue'
import { useConsolidationGenerator } from '@/composables/useConsolidationGenerator'
import { useDiagnosisData } from '@/composables/useDiagnosisData'
import DiagnosisHeader from './components/DiagnosisHeader.vue'
import OverviewTab from './components/OverviewTab.vue'
import PerQuestionTab from './components/PerQuestionTab.vue'
import PerStudentTab from './components/PerStudentTab.vue'
import PerKpTab from './components/PerKpTab.vue'

const route = useRoute()
const router = useRouter()
const taskId = route.params.taskId
const taskIds = route.query.ids

const {
  loading, ready, error, data, aiText, aiRunning, polling, pollSeconds,
  activeTab, questionFilter, studentFilter, kpQuestionFilter, expandedQuestions,
  classCount, studentCount, sortedClasses, labelCounts,
  scoreDistribution, weakQuestionCount,
  filteredQuestions, filteredStudents, sortedKps,
  kpDataQualityMsg, kpDataQualityType, activeKpFilterName,
  toggleExpand, fetchAll, triggerAiAnalysis, drillToKpQuestions, clearKpFilter
} = useDiagnosisData(taskId, taskIds)

const consolidation = useConsolidationGenerator()

async function generateMaterial(kp) {
  const outputId = await consolidation.generate({
    taskId: Number(taskId),
    knowledgeNodeIds: [kp.kpId],
    subject: data.value.subject || ''
  })
  if (outputId) router.push(`/teacher/quality/consolidation/${outputId}`)
}
</script>

<style scoped>
.diag-page { margin: 0 auto; padding: 24px; }
.diag-tabs { --el-border-color: var(--border-base); }

@media (max-width: 768px) {
  .diag-page { padding: 12px; }
}
</style>

<template>
  <div>
    <el-row v-if="sortedClasses?.length" :gutter="16">
      <el-col
        v-for="(c, idx) in sortedClasses"
        :key="c.classId"
        :xs="24" :sm="12" :md="8" :lg="6"
      >
        <el-card shadow="never" class="diag-stat" :class="rankClass(idx, sortedClasses.length)">
          <div class="diag-stat-rank">{{ idx === 0 ? '🥇' : idx === 1 ? '🥈' : idx === 2 ? '🥉' : '' }}</div>
          <div class="diag-stat-name">{{ c.className }}</div>
          <div class="diag-stat-val">{{ c.avgScore }}<span class="diag-stat-unit">分</span></div>
          <div class="diag-stat-sub">及格率 {{ c.passRate }}% · {{ c.studentCount }}人</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row v-if="scoreDistribution?.length" :gutter="16" style="margin-top:16px">
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="diag-card">
          <template #header><span class="diag-card-title">分数段分布</span></template>
          <ScoreDistributionPanel :distribution="scoreDistribution" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="diag-card">
          <template #header><span class="diag-card-title">发展等级分布</span></template>
          <div class="diag-label-grid">
            <div v-for="lb in labelList" :key="lb.key" class="diag-label-item">
              <div class="diag-label-dot" :style="{background:lb.color}"></div>
              <span class="diag-label-name">{{ lb.key }}</span>
              <span class="diag-label-desc">{{ lb.label }}</span>
              <span class="diag-label-count">{{ labelCounts[lb.key] || 0 }}人</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <AiAnalysisCard
      :ai-text="aiText"
      :ai-running="aiRunning"
      @trigger-analysis="$emit('trigger-analysis')"
    />
  </div>
</template>

<script setup>
import ScoreDistributionPanel from './ScoreDistributionPanel.vue'
import AiAnalysisCard from './AiAnalysisCard.vue'
import { GRADE_LABEL_THRESHOLDS } from '@/constants/grading'

defineProps({
  sortedClasses: { type: Array, default: () => [] },
  scoreDistribution: { type: Array, default: () => [] },
  labelCounts: { type: Object, default: () => ({}) },
  aiText: { type: String, default: '' },
  aiRunning: { type: Boolean, default: false }
})
defineEmits(['trigger-analysis'])

const labelList = GRADE_LABEL_THRESHOLDS

function rankClass(idx, total) {
  if (total <= 1) return ''
  if (idx === 0) return 'diag-stat--gold'
  if (idx === total - 1) return 'diag-stat--tail'
  return ''
}
</script>

<style scoped>
.diag-stat { text-align: center; padding: 14px 10px; border: 0.5px solid var(--border-light); position: relative; }
.diag-stat--gold { border-top: 2px solid var(--el-color-warning); }
.diag-stat--tail { border-top: 2px solid var(--bg-secondary); }
.diag-stat-rank { position: absolute; top: 6px; right: 10px; font-size: var(--fs-lg); }
.diag-stat-name { font-size: var(--fs-sm); color: var(--text-secondary); margin-bottom: 4px; }
.diag-stat-val { font-size: 28px; font-weight: 700; color: var(--primary-color); }
.diag-stat-unit { font-size: var(--fs-md); font-weight: 400; color: var(--text-secondary); }
.diag-stat-sub { font-size: var(--fs-xs); color: var(--text-secondary); margin-top: 2px; }
.diag-card { margin-bottom: 0; }
.diag-card-title { font-size: var(--fs-md); font-weight: 700; }
.diag-label-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.diag-label-item { display: flex; align-items: center; gap: 6px; font-size: var(--fs-xs); }
.diag-label-dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }
.diag-label-name { font-weight: 600; min-width: 40px; }
.diag-label-desc { color: var(--text-secondary); flex: 1; }
.diag-label-count { color: var(--text-primary); font-weight: 700; }
</style>

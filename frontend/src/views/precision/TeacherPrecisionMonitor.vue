<template>
  <div class="precision-monitor">
    <div class="page-header">
      <h2 class="page-title">偏科监督</h2>
      <el-button plain :loading="loading" @click="loadAll">刷新</el-button>
    </div>

    <PrecisionStatsStrip v-model="filterCard" :overview="overview" @toggle="toggleCard" />

    <PrecisionFilterBar
      :selected-class-id="selectedClassId"
      :is-single-class="isSingleClass"
      :class-list="classList"
      :filter-subject="filterSubject"
      :allowed-subjects="allowedSubjects"
      :search-name="searchName"
      @update:selected-class-id="selectedClassId = $event; loadEnglishData()"
      @update:filter-subject="filterSubject = $event; loadAll()"
      @update:search-name="searchName = $event"
      @remind-all="handleRemindAll"
    />

    <PrecisionStudentTable
      :sorted-students="sortedStudents"
      :selected-class-id="selectedClassId"
      :table-row-class-name="tableRowClassName"
      :format-active-time="formatActiveTime"
      :active-time-class="activeTimeClass"
      :trend-emoji="trendEmoji"
      :total="filteredStudents.length"
      :page="studentPage"
      :page-size="studentPageSize"
      @open-detail="openStudentDetail"
      @remind-one="handleRemindOne"
      @update:page="studentPage = $event"
    />

    <el-collapse v-model="activePanels" class="dashboard-collapse">
      <el-collapse-item name="layer2">
        <template #title>
          <div class="collapse-title">
            <span>📊 班级健康度 &amp; 薄弱分析</span>
            <el-tag v-if="selectedClassId" size="small" effect="plain" type="info">
              {{ (classList.find(c => c.id === selectedClassId) || {}).className || '' }}
            </el-tag>
            <span v-else class="collapse-hint">— 选择班级后查看数据</span>
          </div>
        </template>
        <ClassHealthGrid
          :class-id="selectedClassId" :class-students="classStudents"
          :weak-top3="weakTop3" :weak-nodes="weakNodes"
          @compose="handleCompose" @compose-from-weak="handleComposeFromWeak"
        />
        <StudentWeaknessPanel
          ref="weaknessPanelRef" :class-id="selectedClassId"
          :filter-subject="filterSubject" @update:weak-nodes="weakNodes = $event"
        />
      </el-collapse-item>
      <el-collapse-item name="layer3">
        <template #title>
          <div class="collapse-title">
            <span>🔤 英语偏科概览</span>
            <span class="title-stat">参与 {{ engData.totalStudents || 0 }} 人</span>
            <span v-if="engData.stageCount" class="title-stat title-stat--warn">关注 {{ engData.stageCount }} 人</span>
          </div>
        </template>
        <EnglishWeaknessPanel
          :eng-data="engData" :eng-students="engStudents"
          :selected-class-id="selectedClassId" @remind="remindEnglish"
        />
      </el-collapse-item>
    </el-collapse>

    <StudentDetailDialog
      :visible="detailVisible" :loading="detailLoading" :error="detailError"
      :student-name="detailName" :student-id="detailStudentId"
      :encouragement="encouragement" :student-row="detailStudentRow"
      :subject="filterSubject"
      @update:visible="detailVisible = $event"
      @retry="openStudentDetail({ studentId: detailStudentId, studentName: detailName })"
    />
  </div>
</template>

<script setup>
import { useTeacherPrecisionMonitor } from '@/composables/useTeacherPrecisionMonitor'
import PrecisionStatsStrip from './components/PrecisionStatsStrip.vue'
import PrecisionFilterBar from './components/PrecisionFilterBar.vue'
import PrecisionStudentTable from './components/PrecisionStudentTable.vue'
import StudentDetailDialog from './components/StudentDetailDialog.vue'
import ClassHealthGrid from './components/ClassHealthGrid.vue'
import StudentWeaknessPanel from './components/StudentWeaknessPanel.vue'
import EnglishWeaknessPanel from './components/EnglishWeaknessPanel.vue'

const {
  loading, filterSubject, searchName, selectedClassId, classList, overview,
  weakTop3, weakNodes, filterCard, weaknessPanelRef,
  engData, engStudents, studentPage, studentPageSize,
  activePanels, detailVisible, detailStudentId, detailName, detailLoading,
  detailError, encouragement, detailStudentRow,
  allowedSubjects, isSingleClass,
  filteredStudents, sortedStudents, classStudents, tableRowClassName,
  loadAll, loadEnglishData, toggleCard,
  handleComposeFromWeak, handleRemindAll, handleRemindOne, handleCompose,
  openStudentDetail, remindEnglish,
  formatActiveTime, activeTimeClass, trendEmoji
} = useTeacherPrecisionMonitor()
</script>

<style scoped>
.precision-monitor {
  margin: 0 auto;
  padding: var(--spacing-lg) var(--spacing-md);
}
.dashboard-collapse {
  margin-top: var(--spacing-lg);
  border: none;
}
:deep(.dashboard-collapse .el-collapse-item__header) {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 14px 18px;
  font-weight: 600;
  font-size: var(--fs-md);
  margin-bottom: 4px;
}
:deep(.dashboard-collapse .el-collapse-item__wrap) {
  background: transparent;
  border: none;
  padding-top: var(--spacing-sm);
}
:deep(.dashboard-collapse .el-collapse-item__content) {
  padding-bottom: var(--spacing-md);
}
.collapse-title {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}
.collapse-hint {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  font-weight: 400;
}
.title-stat { font-size: var(--fs-xs); color: var(--text-secondary); font-weight: 400; }
.title-stat--warn { color: var(--el-color-danger); font-weight: 600; }

@media (min-width: 769px) and (max-width: 1024px) {
  .precision-monitor { padding: var(--spacing-md) var(--spacing-sm); }
  .collapse-title { flex-wrap: wrap; gap: 6px; }
}
@media (max-width: 768px) {
  .precision-monitor { padding: 12px 8px; }
  .collapse-title { flex-wrap: wrap; gap: 6px; font-size: var(--fs-sm); }
  .collapse-hint { display: none; }
  :deep(.dashboard-collapse .el-collapse-item__header) { padding: 12px 14px; font-size: var(--fs-sm); }
}
</style>

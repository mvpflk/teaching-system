<template>
  <div v-if="classId && classStudents.length" class="health-grid-wrapper">
    <!-- 班级健康度面板 -->
    <div class="health-panel">
      <div class="health-bars">
        <div class="health-item">
          <span class="health-label">今日练习</span>
          <div class="health-bar-bg">
            <div class="health-bar" :style="{ width: healthTodayPct + '%', background: 'var(--primary-color)' }"></div>
          </div>
          <span class="health-val">{{ healthTodayCount }}/{{ classStudents.length }}人</span>
        </div>
        <div class="health-item">
          <span class="health-label">本周活跃</span>
          <div class="health-bar-bg">
            <div class="health-bar" :style="{ width: healthActivePct + '%', background: 'var(--el-color-success)' }"></div>
          </div>
          <span class="health-val">{{ healthActiveCount }}/{{ classStudents.length }}人</span>
        </div>
        <div class="health-item">
          <span class="health-label">预警学生</span>
          <div class="health-bar-bg">
            <div class="health-bar" :style="{ width: healthWarnPct + '%', background: 'var(--el-color-danger)' }"></div>
          </div>
          <span class="health-val">{{ healthWarnCount }}人</span>
        </div>
        <div class="health-item">
          <span class="health-label">平均掌握度</span>
          <div class="health-bar-bg">
            <div class="health-bar" :style="{ width: healthAvgMastery + '%', background: healthAvgMastery < 50 ? 'var(--el-color-danger)' : healthAvgMastery < 70 ? 'var(--el-color-warning)' : 'var(--el-color-success)' }"></div>
          </div>
          <span class="health-val">{{ healthAvgMastery }}%</span>
        </div>
      </div>
    </div>

    <!-- 薄弱模块 TOP3 表 + 组卷按钮 -->
    <div class="section-panel" style="margin-top:16px">
      <h4 class="section-title">薄弱模块 TOP3</h4>
      <el-table
        v-if="weakTop3.length"
        :data="weakTop3"
        size="default"
        style="border:1px solid var(--border-color)"
      >
        <el-table-column prop="name" label="知识点" min-width="180" />
        <el-table-column prop="errorCount" label="薄弱人数" width="100" sortable>
          <template #default="{ row }">
            <el-tag type="danger" size="small" effect="plain">{{ row.errorCount }}人</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="$emit('compose', row)">生成补强卷</el-button>
          </template>
        </el-table-column>
      </el-table>
      <EmptyState
        v-else
        icon="DataAnalysis"
        :title="classId ? '暂无薄弱数据' : '暂无薄弱数据'"
        description="暂无足够错题记录用于分析"
      />

      <!-- 基于薄弱点组卷按钮 -->
      <div v-if="classId && weakNodes.length >= 3" class="compose-bar">
        <el-button type="danger" size="default" @click="$emit('compose-from-weak')">
          <el-icon><Document /></el-icon>
          基于薄弱点组卷（前{{ Math.min(5, weakNodes.length) }}个知识点）
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import EmptyState from '@/components/common/EmptyState.vue'

const props = defineProps({
  classId: { type: [String, Number], default: '' },
  classStudents: { type: Array, default: () => [] },
  weakTop3: { type: Array, default: () => [] },
  weakNodes: { type: Array, default: () => [] }
})

defineEmits(['compose', 'compose-from-weak'])

const healthTodayCount = computed(() =>
  props.classStudents.filter(s => s.lastActiveAt && (Date.now() - new Date(s.lastActiveAt).getTime()) / 86400000 <= 1).length
)
const healthTodayPct = computed(() => props.classStudents.length ? Math.round(healthTodayCount.value / props.classStudents.length * 100) : 0)
const healthActiveCount = computed(() => props.classStudents.filter(s => s.weeklyPracticeCount > 0).length)
const healthActivePct = computed(() => props.classStudents.length ? Math.round(healthActiveCount.value / props.classStudents.length * 100) : 0)
const healthWarnCount = computed(() => props.classStudents.filter(s => s.warning).length)
const healthWarnPct = computed(() => props.classStudents.length ? Math.round(healthWarnCount.value / props.classStudents.length * 100) : 0)
const healthAvgMastery = computed(() => {
  const list = props.classStudents.filter(s => s.lastTestScore > 0)
  return list.length ? Math.round(list.reduce((s, x) => s + x.lastTestScore, 0) / list.length) : 0
})
</script>

<style scoped>
.health-grid-wrapper {
  margin-bottom: var(--spacing-md);
}
.health-panel {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 14px 18px;
  margin-bottom: var(--spacing-md);
}
.health-bars {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px 20px;
}
.health-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--fs-xs);
}
.health-label {
  width: 68px;
  color: var(--text-secondary);
  flex-shrink: 0;
}
.health-bar-bg {
  flex: 1;
  height: 8px;
  background: var(--border-color);
  border-radius: 4px;
  overflow: hidden;
}
.health-bar {
  height: 100%;
  border-radius: 4px;
  transition: width 0.4s;
}
.health-val {
  width: 64px;
  text-align: right;
  color: var(--text-primary);
  font-weight: 600;
  font-size: var(--fs-xs);
}

.section-panel {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 16px;
}
.section-title {
  font-size: var(--fs-md);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 12px;
}
.compose-bar {
  margin-top: 12px;
  text-align: center;
  padding: 12px;
  background: var(--el-color-danger-light);
  border: 1px solid var(--el-color-danger);
  border-radius: var(--radius-md);
}

@media (max-width: 768px) {
  .health-panel {
    padding: 12px 14px;
  }
  .health-bars {
    grid-template-columns: 1fr;
    gap: 8px;
  }
  .health-label {
    width: 56px;
  }
  .health-val {
    width: 56px;
  }
  .section-panel {
    padding: 12px;
  }
  .compose-bar {
    padding: 10px;
  }
  .compose-bar .el-button {
    width: 100%;
    justify-content: center;
  }
}
</style>

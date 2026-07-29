<template>
  <div>
    <!-- 任务信息 -->
    <el-page-header class="mb-16" @back="$router.back()">
      <template #content>
        <span class="task-title">{{ task.title || '加载中...' }}</span>
        <el-tag
          v-if="task.taskType"
          :type="TASK_TYPE_TAG[task.taskType] || ''"
          size="small"
          class="ml-8"
        >
          {{ TASK_TYPE_LABEL[task.taskType] }}
        </el-tag>
        <el-tag
          v-if="task.status"
          :type="TASK_STATUS_TAG[task.status] || ''"
          size="small"
          class="ml-8"
        >
          {{ TASK_STATUS_LABEL[task.status] }}
        </el-tag>
      </template>
      <template #extra>
        <template v-if="isMobile">
          <el-button
            type="primary"
            size="small"
            @click="$router.push(`/teacher/grading/${taskId}`)"
          >
            批阅
          </el-button>
          <el-button
            size="small"
            :loading="autoGrading"
            @click="$emit('auto-grade')"
          >
            自动评分
          </el-button>
          <el-dropdown trigger="click" @command="handleMobileCmd">
            <el-button size="small">
              更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="export">📥 导出成绩</el-dropdown-item>
                <el-dropdown-item command="analysis">📊 成绩分析</el-dropdown-item>
                <el-dropdown-item command="stats">📈 统计分析</el-dropdown-item>
                <el-dropdown-item command="resend">📢 一键重发</el-dropdown-item>
                <el-dropdown-item divided command="passSettings">⚙️ 达标设置</el-dropdown-item>
                <el-dropdown-item
                  v-if="task.taskType === 'SURVEY'"
                  command="survey"
                >
                  📋 问卷统计
                </el-dropdown-item>
                <template v-if="peerReviewEnabled">
                  <el-dropdown-item
                    command="assignPR"
                    :disabled="assigningPR"
                    divided
                  >
                    🔀 分配互评
                  </el-dropdown-item>
                  <el-dropdown-item
                    command="fusePR"
                    :disabled="fusingPR"
                  >
                    🔗 融合互评分
                  </el-dropdown-item>
                  <el-dropdown-item command="quality">✅ 互评质量</el-dropdown-item>
                </template>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button
            type="primary"
            @click="$router.push(`/teacher/grading/${taskId}`)"
          >
            批阅工作台
          </el-button>
          <el-button :loading="autoGrading" @click="$emit('auto-grade')">自动评分</el-button>
          <el-button type="success" @click="$emit('export-grades')">导出成绩</el-button>
          <el-button type="warning" @click="$emit('show-analysis')">成绩分析</el-button>
          <el-button
            type="info"
            plain
            @click="$router.push(`/teacher/tasks/${taskId}/manual-entry`)"
          >
            <el-icon><EditPen /></el-icon> 纸质录入
          </el-button>
          <el-dropdown trigger="click">
            <el-button>
              更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$emit('show-stats')">
                  <el-icon><TrendCharts /></el-icon> 统计分析
                </el-dropdown-item>
                <el-dropdown-item @click="$emit('resend')">
                  <el-icon><Promotion /></el-icon> 一键重发
                </el-dropdown-item>
                <el-dropdown-item divided @click="showPassDialog = true">
                  <el-icon><Setting /></el-icon> 达标设置
                </el-dropdown-item>
                <el-dropdown-item
                  v-if="task.taskType === 'SURVEY'"
                  @click="$emit('show-survey')"
                >
                  <el-icon><DataAnalysis /></el-icon> 问卷统计
                </el-dropdown-item>
                <template v-if="peerReviewEnabled">
                  <el-dropdown-item
                    :disabled="assigningPR"
                    @click="$emit('assign-pr')"
                  >
                    分配互评
                  </el-dropdown-item>
                  <el-dropdown-item
                    :disabled="fusingPR"
                    @click="$emit('fuse-pr')"
                  >
                    融合互评分
                  </el-dropdown-item>
                  <el-dropdown-item @click="$emit('show-quality')">互评质量</el-dropdown-item>
                </template>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </template>
    </el-page-header>

    <!-- 搜索筛选 -->
    <div class="grade-filter">
      <el-input
        :model-value="searchQuery"
        placeholder="搜索学生姓名..."
        clearable
        size="small"
        :style="isMobile ? 'width:100%' : 'width:200px'"
        @update:model-value="$emit('update:searchQuery', $event)"
      />
      <el-checkbox-group
        :model-value="statusFilter"
        size="small"
        class="status-chips"
        @update:model-value="$emit('update:statusFilter', $event)"
      >
        <el-checkbox-button
          v-for="opt in statusFilterOptions"
          :key="opt.value"
          :value="opt.value"
          :type="opt.type || ''"
        >
          {{ opt.label }}
          <span v-if="opt.count !== undefined" class="chip-count">({{ opt.count }})</span>
        </el-checkbox-button>
      </el-checkbox-group>
      <span v-if="submissions.length" class="grade-stats">
        共 {{ submissions.length }} 人 · 已批
        {{ submissions.filter((s) => s.status === 'GRADED').length }}
      </span>
      <el-radio-group
        :model-value="viewMode"
        size="small"
        :style="isMobile ? 'width:100%' : 'margin-left:12px'"
        @update:model-value="$emit('update:viewMode', $event)"
      >
        <el-radio-button value="submissions">提交列表</el-radio-button>
        <el-radio-button value="board">📊 看板</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 自动评分进度 -->
    <div v-if="autoGrading" class="auto-grade-progress">
      <el-progress
        :percentage="autoGradePercent"
        :stroke-width="12"
        :text-inside="true"
        :status="autoGradePercent === 100 ? 'success' : ''"
        style="max-width: 400px"
      />
      <span class="auto-grade-text">自动评分 {{ autoGradeDone }}/{{ autoGradeTotal }}</span>
    </div>

    <!-- 达标设置对话框 -->
    <el-dialog v-model="showPassDialog" title="达标设置" width="480px" destroy-on-close append-to-body>
      <el-form label-width="120px" label-position="left">
        <el-form-item label="开启达标模式">
          <el-switch v-model="passEnabled" />
          <span style="font-size:var(--fs-xs);color:var(--text-secondary);margin-left:8px">
            开启后学生未达到得分率将自动安排重测
          </span>
        </el-form-item>
        <template v-if="passEnabled">
          <el-form-item label="达标得分率">
            <el-slider v-model="passForm.passRate" :min="50" :max="100" :step="5" show-input style="width:100%" />
          </el-form-item>
          <el-form-item label="最大重测次数（含首次）">
            <el-select v-model="passForm.maxAttempts" style="width:100%">
              <el-option :value="2" label="1次重测" />
              <el-option :value="3" label="2次重测" />
            </el-select>
          </el-form-item>
          <el-form-item label="重测截止（小时）">
            <el-input-number v-model="passForm.retakeDeadlineHours" :min="1" :max="720" style="width:100%" />
          </el-form-item>
          <el-alert type="info" :closable="false" show-icon>
            <template #title>
              学生得分低于 {{ passForm.passRate }}% 将自动安排重测，首次成绩计入统计
            </template>
          </el-alert>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="showPassDialog = false">取消</el-button>
        <el-button type="primary" :loading="passSaving" @click="savePassConfig">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { ArrowDown, DataAnalysis, TrendCharts, Promotion, EditPen, Setting } from '@element-plus/icons-vue';
import {
  TASK_TYPE_LABEL,
  TASK_TYPE_TAG,
  TASK_STATUS_LABEL,
  TASK_STATUS_TAG,
  SUBMISSION_STATUS_LABEL,
  SUBMISSION_STATUS_TAG,
} from '@/constants/taskType';
import { adjustPassRate } from '@/api/task';

const props = defineProps({
  task: { type: Object, default: () => ({}) },
  taskId: { type: [String, Number], required: true },
  isMobile: { type: Boolean, default: false },
  autoGrading: { type: Boolean, default: false },
  autoGradePercent: { type: Number, default: 0 },
  autoGradeDone: { type: Number, default: 0 },
  autoGradeTotal: { type: Number, default: 0 },
  searchQuery: { type: String, default: '' },
  statusFilter: { type: Array, default: () => [] },
  viewMode: { type: String, default: 'submissions' },
  peerReviewEnabled: { type: Boolean, default: false },
  submissions: { type: Array, default: () => [] },
  assigningPR: { type: Boolean, default: false },
  fusingPR: { type: Boolean, default: false },
});

const emit = defineEmits([
  'update:searchQuery',
  'update:statusFilter',
  'update:viewMode',
  'auto-grade',
  'export-grades',
  'show-analysis',
  'show-stats',
  'show-survey',
  'resend',
  'assign-pr',
  'fuse-pr',
  'show-quality',
  'refresh',
]);

// ── 达标设置对话框 ──
const showPassDialog = ref(false);
const passSaving = ref(false);
const passEnabled = ref(false);
const passForm = ref({
  passRate: 60,
  maxAttempts: 2,
  retakeDeadlineHours: 48,
});

watch(showPassDialog, (v) => {
  if (v && props.task) {
    const hasPass = props.task.passRate > 0;
    passEnabled.value = hasPass;
    passForm.value = {
      passRate: hasPass ? props.task.passRate : 60,
      maxAttempts: props.task.maxAttempts || 2,
      retakeDeadlineHours: props.task.retakeDeadlineHours || 48,
    };
  }
});

const savePassConfig = async () => {
  passSaving.value = true;
  try {
    const data = {};
    if (passEnabled.value) {
      data.passRate = passForm.value.passRate;
      data.maxAttempts = passForm.value.maxAttempts;
      data.retakeDeadlineHours = passForm.value.retakeDeadlineHours;
    } else {
      data.passRate = 0;
    }
    const res = await adjustPassRate(props.taskId, data);
    if (res.code === 200) {
      ElMessage.success('达标配置已更新');
      showPassDialog.value = false;
      emit('refresh');
    } else {
      ElMessage.error(res.message || '保存失败');
    }
  } catch {
    ElMessage.error('保存失败');
  } finally {
    passSaving.value = false;
  }
};

const statusFilterOptions = computed(() => {
  const counts = {};
  for (const s of props.submissions) {
    const label = SUBMISSION_STATUS_LABEL[s.status] || s.status;
    counts[label] = (counts[label] || 0) + 1;
  }
  return Object.keys(SUBMISSION_STATUS_LABEL)
    .map((k) => ({
      value: k,
      label: SUBMISSION_STATUS_LABEL[k],
      type: SUBMISSION_STATUS_TAG[k],
      count: counts[SUBMISSION_STATUS_LABEL[k]] || 0,
    }))
    .filter((opt) => opt.count > 0 || props.statusFilter.includes(opt.value));
});

const handleMobileCmd = (cmd) => {
  const actions = {
    export: () => emit('export-grades'),
    analysis: () => emit('show-analysis'),
    stats: () => emit('show-stats'),
    resend: () => emit('resend'),
    survey: () => emit('show-survey'),
    assignPR: () => emit('assign-pr'),
    fusePR: () => emit('fuse-pr'),
    quality: () => emit('show-quality'),
    passSettings: () => { showPassDialog.value = true },
  };
  actions[cmd]?.();
};
</script>

<style scoped>
.mb-16 {
  margin-bottom: 16px;
}
.ml-8 {
  margin-left: 8px;
}
.task-title {
  font-size: var(--fs-lg);
  font-weight: 600;
  color: var(--text-primary);
}
.grade-filter {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.grade-stats {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  white-space: nowrap;
}
.status-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.status-chips :deep(.el-checkbox-button) {
  --el-checkbox-button-checked-bg-color: var(--el-color-primary);
}
.status-chips :deep(.el-checkbox-button__inner) {
  font-size: var(--fs-xs);
  padding: 4px 10px;
  border-radius: 14px !important;
  border: 1px solid var(--border-light);
}
.status-chips :deep(.el-checkbox-button.is-checked .el-checkbox-button__inner) {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}
.chip-count {
  font-size: var(--fs-xs);
  opacity: 0.7;
}
.auto-grade-progress {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.auto-grade-text {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  white-space: nowrap;
}

@media (max-width: 768px) {
  .grade-filter {
    flex-wrap: wrap;
    gap: 6px;
  }
  .grade-filter :deep(.el-radio-group) {
    width: 100% !important;
    display: flex;
  }
  .grade-filter :deep(.el-radio-button) {
    flex: 1;
  }
  .grade-filter :deep(.el-radio-button__inner) {
    width: 100%;
    font-size: var(--fs-xs);
    padding: 6px 4px;
  }
  .grade-stats {
    font-size: var(--fs-xs);
    width: 100%;
    text-align: center;
  }
  :deep(.el-page-header__extra) {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
  }
  :deep(.el-page-header__extra .el-button) {
    font-size: var(--fs-xs);
  }
}
</style>

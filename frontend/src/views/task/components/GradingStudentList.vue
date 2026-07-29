<template>
  <div v-show="viewMode === 'submissions'">
    <!-- 达标状态筛选工具栏 -->
    <div style="margin-bottom: 12px; display: flex; gap: 8px; align-items: center; flex-wrap: wrap">
      <el-button
        size="small"
        :type="retakeFilter === 'all' ? 'primary' : ''"
        @click="retakeFilter = 'all'"
      >
        全部学生
      </el-button>
      <el-button
        size="small"
        :type="retakeFilter === 'retake' ? 'primary' : ''"
        @click="retakeFilter = 'retake'"
      >
        📌 仅看可再练
      </el-button>
      <el-button
        size="small"
        :type="retakeFilter === 'passed' ? 'primary' : ''"
        @click="retakeFilter = 'passed'"
      >
        仅看已达标
      </el-button>
      <el-button size="small" @click="exportRetakeList">📤 导出待提升名单</el-button>
    </div>

    <!-- 移动端卡片 -->
    <template v-if="isMobile">
      <div
        v-for="s in filteredSubmissions"
        :key="s.id"
        class="mobile-sub-card"
        @click="$emit('open-grade', s)"
      >
        <div class="msc-top">
          <div>
            <span class="msc-name">{{ s.studentName || s.studentId }}</span>
            <div v-if="s.className || s.grade" class="msc-class">
              {{ [s.grade, s.className].filter(Boolean).join(' ') }}
            </div>
          </div>
          <el-tag :type="SUBMISSION_STATUS_TAG[s.status] || ''" size="small">
            {{ SUBMISSION_STATUS_LABEL[s.status] || s.status }}
          </el-tag>
        </div>
        <div class="msc-mid">
          <span class="msc-label">得分: </span>
          <template v-if="task.scoreType === 'GRADE_5'">{{ s.gradeLevel || '-' }}</template>
          <template v-else-if="task.scoreType === 'PASS_FAIL'">
            {{ s.gradeLevel === 'PASS' ? '通过' : s.gradeLevel === 'FAIL' ? '未通过' : '-' }}
          </template>
          <template v-else>{{ s.score != null ? s.score : '-' }}</template>
          <span class="msc-time">{{ formatTime(s.submittedAt) }}</span>
        </div>
        <div class="msc-bot">
          <el-tag v-if="s.gradeType === 'AUTO'" size="small" type="info">自动</el-tag>
          <el-tag v-else-if="s.gradeType === 'TEACHER'" size="small">教师</el-tag>
          <el-button size="small" @click.stop="$emit('open-grade', s)">查看</el-button>
          <el-button
            v-if="hasSubjective && s.status !== 'EXEMPTED' && s.status !== 'PENDING'"
            size="small"
            type="primary"
            @click.stop="$emit('open-grade', s)"
          >
            {{ s.status === 'GRADED' ? '重评' : '评分' }}
          </el-button>
          <el-button v-if="s.status === 'EXEMPTED'" size="small" disabled>已豁免</el-button>
          <el-button
            v-if="s.status === 'PENDING' && s.extraSubmitAllowed !== 1"
            size="small"
            type="warning"
            @click.stop="$emit('allow-extra', s)"
          >
            允许补交
          </el-button>
          <el-tag v-if="s.extraSubmitAllowed === 1" size="small" type="success">已特许</el-tag>
        </div>
      </div>
    </template>

    <!-- 桌面端表格 -->
    <el-table
      v-else
      :data="filteredSubmissions"
      stripe
      @sort-change="$emit('sort-change', $event)"
    >
      <template #empty><el-empty description="暂无提交记录" /></template>
      <el-table-column type="expand">
        <template #default="{ row }">
          <div
            v-if="row.retakeHistory && row.retakeHistory.length"
            style="padding: 8px 16px; font-size: 13px"
          >
            <div
              v-for="(r, i) in row.retakeHistory"
              :key="r.id"
              style="
                display: flex;
                align-items: center;
                gap: 12px;
                padding: 4px 0;
                border-bottom: 1px solid var(--border-light);
              "
            >
              <span v-if="i === 0">📝 首次</span>
              <span v-else>🔄 重测 #{{ i }}</span>
              <span>: {{ r.score }}/{{ r.totalScore }}</span>
              <el-tag v-if="r.passed === true" size="small" type="success">达标</el-tag>
              <el-tag v-else-if="r.passed === false" size="small" type="danger">未达标</el-tag>
              <el-tag v-else size="small" type="info">—</el-tag>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column
        label="学生"
        min-width="140"
        sortable="custom"
        prop="studentName"
      >
        <template #default="{ row }">
          <div>{{ row.studentName || row.studentId }}</div>
          <div v-if="row.className || row.grade" class="student-sub">
            {{ [row.grade, row.className].filter(Boolean).join(' ') }}
          </div>
        </template>
      </el-table-column>
      <el-table-column
        label="状态"
        width="100"
        sortable="custom"
        prop="status"
      >
        <template #default="{ row }">
          <el-tag :type="SUBMISSION_STATUS_TAG[row.status] || ''" size="small">
            {{ SUBMISSION_STATUS_LABEL[row.status] || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="得分" width="100">
        <template #default="{ row }">
          <template v-if="task.scoreType === 'GRADE_5'">{{ row.gradeLevel || '-' }}</template>
          <template v-else-if="task.scoreType === 'PASS_FAIL'">
            {{ row.gradeLevel === 'PASS' ? '通过' : row.gradeLevel === 'FAIL' ? '未通过' : '-' }}
          </template>
          <template v-else>{{ row.score != null ? row.score : '-' }}</template>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="170">
        <template #default="{ row }">{{ formatTime(row.submittedAt) }}</template>
      </el-table-column>
      <el-table-column label="评分方式" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.gradeType === 'AUTO'" size="small" type="info">自动</el-tag>
          <el-tag v-else-if="row.gradeType === 'TEACHER'" size="small">教师</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="达标状态" width="160" align="center">
        <template #default="{ row }">
          <template v-if="!row.passRate">
            <span style="color: #909399">—</span>
          </template>
          <template v-else>
            <el-tag v-if="row.passed" type="success" size="small">
              ✅
              {{
                row.passType === 'first'
                  ? '已达标·首次'
                  : '已达标·提升' + (row.scoreImprove || 0) + '分'
              }}
            </el-tag>
            <el-tag v-else-if="row.canRetake" type="warning" size="small">
              🔄 可再练·{{ row.attemptNumber }}/{{ row.maxAttempts }}
            </el-tag>
            <el-tag v-else type="info" size="small"> 📌 待巩固 </el-tag>
          </template>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="$emit('open-grade', row)">查看</el-button>
          <el-button
            v-if="hasSubjective && row.status !== 'EXEMPTED' && row.status !== 'PENDING'"
            size="small"
            type="primary"
            @click="$emit('open-grade', row)"
          >
            {{ row.status === 'GRADED' ? '重评' : '评分' }}
          </el-button>
          <el-button v-if="row.status === 'EXEMPTED'" size="small" disabled>已豁免</el-button>
          <el-button
            v-if="row.status === 'PENDING' && row.extraSubmitAllowed !== 1"
            size="small"
            type="warning"
            @click="$emit('allow-extra', row)"
          >
            允许补交
          </el-button>
          <el-tag v-if="row.extraSubmitAllowed === 1" size="small" type="success">已特许</el-tag>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div v-if="total > pageSize" class="pagination-wrap">
      <el-pagination
        :current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        background
        @current-change="$emit('update:page', $event)"
      />
    </div>

    <!-- 预览对话框 -->
    <TaskPreviewDialog
      v-model="previewVisible"
      :task="task"
      :submission="previewSubmission"
      @open-workbench="$emit('open-grade', $event)"
    />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { SUBMISSION_STATUS_LABEL, SUBMISSION_STATUS_TAG } from '@/constants/taskType';
import TaskPreviewDialog from './TaskPreviewDialog.vue';
import dayjs from 'dayjs';

const props = defineProps({
  task: { type: Object, default: () => ({}) },
  submissions: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  hasSubjective: { type: Boolean, default: false },
  isMobile: { type: Boolean, default: false },
  page: { type: Number, default: 1 },
  total: { type: Number, default: 0 },
  pageSize: { type: Number, default: 60 },
  searchQuery: { type: String, default: '' },
  statusFilter: { type: Array, default: () => [] },
  sortBy: { type: String, default: '' },
  sortOrder: { type: String, default: '' },
  viewMode: { type: String, default: 'submissions' },
});

const emit = defineEmits(['update:page', 'open-grade', 'allow-extra', 'sort-change']);

const retakeFilter = ref('all');
const previewVisible = ref(false);
const previewSubmission = ref({});

const sortedSubmissions = computed(() => {
  let list = [...props.submissions];
  const q = props.searchQuery.toLowerCase();
  if (q) list = list.filter((s) => (s.studentName || '').toLowerCase().includes(q));
  if (props.statusFilter.length) list = list.filter((s) => props.statusFilter.includes(s.status));
  return list.sort((a, b) => {
    if (props.sortBy === 'studentName') {
      const va = (a.studentName || '').toLowerCase();
      const vb = (b.studentName || '').toLowerCase();
      return props.sortOrder === 'ascending' ? va.localeCompare(vb) : vb.localeCompare(va);
    }
    if (props.sortBy === 'status') {
      const va = SUBMISSION_STATUS_LABEL[a.status] || a.status || '';
      const vb = SUBMISSION_STATUS_LABEL[b.status] || b.status || '';
      return props.sortOrder === 'ascending' ? va.localeCompare(vb) : vb.localeCompare(va);
    }
    const sa = a.score != null ? Number(a.score) : -1;
    const sb = b.score != null ? Number(b.score) : -1;
    return sb - sa;
  });
});

const filteredSubmissions = computed(() => {
  const list = sortedSubmissions.value;
  if (retakeFilter.value === 'retake') return list.filter((s) => s.canRetake);
  if (retakeFilter.value === 'passed') return list.filter((s) => s.passed);
  return list;
});

const exportRetakeList = () => {
  const list = props.submissions.filter((s) => s.canRetake);
  if (!list.length) {
    ElMessage.info('没有待提升的学生');
    return;
  }
  const rows = [['学生姓名', '学号', '当前得分', '重测次数', '达标状态']];
  for (const s of list) {
    rows.push([
      csvEscape(s.studentName || ''),
      csvEscape(s.studentNo || ''),
      s.score ?? '-',
      `${s.attemptNumber || '-'}/${s.maxAttempts || '-'}`,
      s.passed ? '已达标' : '待巩固',
    ]);
  }
  const csv = rows.map((r) => r.join(',')).join('\n');
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8' });
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = `待提升名单_${props.task.title || ''}.csv`;
  a.click();
  ElMessage.success('导出成功');
};

const csvEscape = (v) => {
  const s = String(v ?? '');
  return s.includes(',') || s.includes('"') || s.includes('\n') ? `"${s.replace(/"/g, '""')}"` : s;
};

const formatTime = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-');
</script>

<style scoped>
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}
.mobile-sub-card {
  padding: 12px;
  margin-bottom: 8px;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
}
.msc-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.msc-name {
  font-weight: 600;
  font-size: var(--fs-base);
  color: var(--text-primary);
}
.msc-class {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}
.student-sub {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}
.msc-mid {
  font-size: var(--fs-sm);
  color: var(--text-regular);
  margin-bottom: 8px;
}
.msc-label {
  color: var(--text-secondary);
}
.msc-time {
  margin-left: 12px;
  color: var(--text-secondary);
}
.msc-bot {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

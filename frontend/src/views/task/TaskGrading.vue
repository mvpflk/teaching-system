<template>
  <div v-loading="pageLoading" class="page-card">
    <GradingHeader
      :task="task"
      :task-id="taskId"
      :is-mobile="isMobile"
      :auto-grading="autoGrading"
      :auto-grade-percent="autoGradePercent"
      :auto-grade-done="autoGradeDone"
      :auto-grade-total="autoGradeTotal"
      :search-query="searchQuery"
      :status-filter="statusFilter"
      :view-mode="viewMode"
      :peer-review-enabled="peerReviewEnabled"
      :submissions="submissions"
      :assigning-p-r="assigningPR"
      :fusing-p-r="fusingPR"
      @update:search-query="searchQuery = $event"
      @update:status-filter="statusFilter = $event"
      @update:view-mode="viewMode = $event"
      @auto-grade="autoGrade"
      @export-grades="exportGrades"
      @show-analysis="showScoreAnalysis = true"
      @show-stats="showTaskStats = true"
      @show-survey="showSurveyStats = true"
      @resend="handleResend"
      @assign-pr="assignPeerReview"
      @fuse-pr="fusePeerScoresFn"
      @show-quality="showQuality = true"
      @refresh="loadTask(); loadSubmissions(); loadBoard()"
    />

    <PenetrationCheckin />

    <GradingBoardView
      ref="boardViewRef"
      :task-id="taskId"
      :is-mobile="isMobile"
      :view-mode="viewMode"
      :regrading-row="regradingRow"
      @regrade="regradeOne"
      @show-batch-regrade="scorePanelRef.showBatchRegrade = true"
    />

    <GradingStudentList
      :task="task"
      :submissions="submissions"
      :has-subjective="hasSubjective"
      :is-mobile="isMobile"
      :page="subPage"
      :total="subTotal"
      :page-size="subPageSize"
      :search-query="searchQuery"
      :status-filter="statusFilter"
      :sort-by="sortBy"
      :sort-order="sortOrder"
      :view-mode="viewMode"
      @update:page="
        subPage = $event;
        loadSubmissions();
      "
      @open-grade="openGrade"
      @allow-extra="allowExtra"
      @sort-change="onSortChange"
    />

    <ScoreAnalysisDialog v-model="showScoreAnalysis" :task-id="taskId" />

    <GradingScorePanel
      ref="scorePanelRef"
      :task-id="taskId"
      :visible="showQuality"
      @update:visible="showQuality = $event"
      @done="
        loadBoard();
        loadSubmissions();
      "
    />

    <el-dialog
      v-model="showSurveyStats"
      title="问卷统计"
      width="800px"
      destroy-on-close
      append-to-body
    >
      <SurveyStats :task-id="taskId" />
    </el-dialog>
    <el-dialog
      v-model="showTaskStats"
      title="统计分析"
      width="900px"
      destroy-on-close
      append-to-body
    >
      <TaskStats :task-id="taskId" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useIsMobile } from '@/composables/useIsMobile';
import { useSettingsStore } from '@/stores/settings';
import {
  getTask,
  getTaskQuestions,
  getTaskSubmissions,
  autoGradeSubmission,
  regradeSubmissionResult,
  resendToPending,
  allowExtraSubmit,
  getScoreAnalysis,
} from '@/api/task';
import { assignPeerReviews, fusePeerScores } from '@/api/peerReview';
import SurveyStats from './SurveyStats.vue';
import TaskStats from './TaskStats.vue';
import PenetrationCheckin from '@/components/research/PenetrationCheckin.vue';
import { SUBJECTIVE_TYPES } from '@/constants/questionTypes';
import ScoreAnalysisDialog from './ScoreAnalysisDialog.vue';
import GradingHeader from './components/GradingHeader.vue';
import GradingStudentList from './components/GradingStudentList.vue';
import GradingBoardView from './components/GradingBoardView.vue';
import GradingScorePanel from './components/GradingScorePanel.vue';
import dayjs from 'dayjs';

const route = useRoute();
const router = useRouter();

const taskId = route.params.id;
const task = ref({});
const submissions = ref([]);
const pageLoading = ref(false);
const subPage = ref(1);
const subPageSize = ref(60);
const subTotal = ref(0);
const { isMobile } = useIsMobile();
const settingsStore = useSettingsStore();
const peerReviewEnabled = computed(() => settingsStore.isEnabled('feature.re_review_enabled'));
const autoGrading = ref(false);
const autoGradePercent = ref(0);
const autoGradeDone = ref(0);
const autoGradeTotal = ref(0);
const showScoreAnalysis = ref(false);
const assigningPR = ref(false),
  fusingPR = ref(false),
  showQuality = ref(false),
  showSurveyStats = ref(false),
  showTaskStats = ref(false);
const searchQuery = ref('');
const statusFilter = ref([]);
const sortBy = ref('');
const sortOrder = ref('');
const viewMode = ref('submissions');
const regradingRow = ref(null);
const hasSubjective = ref(false);

const boardViewRef = ref(null);
const scorePanelRef = ref(null);

const onSortChange = ({ prop, order }) => {
  sortBy.value = prop || '';
  sortOrder.value = order || '';
};

const loadBoard = () => boardViewRef.value?.loadBoard();

const loadTask = async () => {
  pageLoading.value = true;
  try {
    const res = await getTask(taskId);
    if (res.code === 200) task.value = res.data;
  } catch {
    ElMessage.error('加载任务失败');
  } finally {
    pageLoading.value = false;
  }
};

const loadSubmissions = async () => {
  try {
    const res = await getTaskSubmissions(taskId, { page: subPage.value, size: subPageSize.value });
    if (res.code === 200) {
      if (res.data?.records) {
        submissions.value = res.data.records;
        subTotal.value = res.data.total || 0;
      } else {
        submissions.value = res.data || [];
        subTotal.value = submissions.value.length;
      }
    }
  } catch {
    ElMessage.error('加载提交列表失败');
  }
};

const openGrade = (row) => router.push(`/teacher/grading/${taskId}?studentId=${row.studentId}`);

const allowExtra = async (sub) => {
  try {
    await allowExtraSubmit(taskId, sub.studentId);
    ElMessage.success(`已为 ${sub.studentName || sub.studentId} 开启补交通道`);
    sub.extraSubmitAllowed = 1;
  } catch {
    ElMessage.error('操作失败');
  }
};

const regradeOne = async (row) => {
  const sid = row.submissionId || row.id;
  if (!sid) {
    ElMessage.warning('未找到提交记录ID');
    return;
  }
  regradingRow.value = row.studentId;
  try {
    const res = await regradeSubmissionResult(sid);
    if (res.code === 200) {
      ElMessage.success(
        res.data?.message ||
          `重评完成: ${res.data?.changed || 0}题变更, 新总分${res.data?.newTotal || 0}`
      );
      await loadBoard();
      await loadSubmissions();
    } else {
      ElMessage.error(res.message || '重评失败');
    }
  } catch {
    ElMessage.error('重评失败');
  }
  regradingRow.value = null;
};

const autoGrade = async () => {
  let allSubs = submissions.value;
  if (subTotal.value > subPageSize.value) {
    try {
      const res = await getTaskSubmissions(taskId, { page: 1, size: 999 });
      if (res.code === 200) allSubs = res.data?.records || res.data || [];
    } catch {
      /* */
    }
  }
  const pending = allSubs.filter((s) => s.status === 'SUBMITTED' && !s.gradeType);
  if (pending.length === 0) {
    ElMessage.info('没有可自动评分的提交');
    return;
  }
  try {
    await ElMessageBox.confirm(
      `将对 ${pending.length} 份未评分提交进行自动评分，已有教师评分的提交不会受影响。确认？`,
      '自动评分',
      { type: 'info' }
    );
  } catch {
    return;
  }
  autoGrading.value = true;
  autoGradePercent.value = 0;
  autoGradeDone.value = 0;
  autoGradeTotal.value = pending.length;
  try {
    const CONCURRENCY = 5;
    for (let i = 0; i < pending.length; i += CONCURRENCY) {
      const batch = pending.slice(i, i + CONCURRENCY);
      await Promise.all(batch.map((s) => autoGradeSubmission(taskId, s.id)));
      autoGradeDone.value += batch.length;
      autoGradePercent.value = Math.round((autoGradeDone.value / pending.length) * 100);
    }
    ElMessage.success(`已自动评分 ${autoGradeDone.value} 份`);
    loadSubmissions();
  } catch {
    ElMessage.error('自动评分失败');
  } finally {
    autoGrading.value = false;
    autoGradePercent.value = 0;
  }
};

const handleResend = async () => {
  try {
    await ElMessageBox.confirm('确定要为所有未提交的学生重新开放提交权限吗？', '一键重发', {
      type: 'warning',
      confirmButtonText: '确定',
    });
  } catch {
    return;
  }
  try {
    const r = await resendToPending(taskId);
    if (r.code === 200)
      ElMessage.success(r.message || `已为 ${r.data?.count || 0} 名学生重新开放提交`);
  } catch {
    ElMessage.error('操作失败');
  }
};

const csvEscape = (v) => {
  const s = String(v ?? '');
  return s.includes(',') || s.includes('"') || s.includes('\n') ? `"${s.replace(/"/g, '""')}"` : s;
};

const assignPeerReview = async () => {
  assigningPR.value = true;
  try {
    const res = await assignPeerReviews(taskId);
    if (res.code === 200) ElMessage.success(`已分配 ${res.data.assigned} 份互评`);
    else ElMessage.error(res.message || '分配失败');
  } catch {
    ElMessage.error('分配失败');
  } finally {
    assigningPR.value = false;
  }
};

const fusePeerScoresFn = async () => {
  fusingPR.value = true;
  try {
    const res = await fusePeerScores(taskId);
    if (res.code === 200) ElMessage.success(`已融合 ${res.data.updated} 份提交的互评分数`);
    else ElMessage.error(res.message || '融合失败');
  } catch {
    ElMessage.error('融合失败');
  } finally {
    fusingPR.value = false;
  }
};

const exportGrades = async () => {
  let analysis = null;
  try {
    const aRes = await getScoreAnalysis(taskId);
    if (aRes.code === 200) analysis = aRes.data;
  } catch {
    /* 忽略 */
  }
  const rows = [['考试成绩单 — ' + (task.value.title || taskId)]];
  if (analysis?.hasData)
    rows.push([
      '总分',
      analysis.totalScore || '',
      '平均得分率',
      (analysis.avgRate || 0) + '%',
      '最高',
      (analysis.maxRate || 0) + '%',
      '最低',
      (analysis.minRate || 0) + '%',
      '及格率',
      (analysis.passRate || 0) + '%',
    ]);
  rows.push(['']);
  rows.push(['学生', '状态', '得分', '评分方式', '提交时间']);
  for (const s of submissions.value) {
    rows.push([
      csvEscape(s.studentName || s.studentId),
      s.status || '-',
      s.score != null ? s.score : s.gradeLevel || '-',
      s.gradeType || '-',
      s.submittedAt ? dayjs(s.submittedAt).format('YYYY-MM-DD HH:mm') : '-',
    ]);
  }
  const csv = rows.map((r) => r.join(',')).join('\n');
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8' });
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = `成绩单_${task.value.title || taskId}.csv`;
  a.click();
  ElMessage.success('导出成功');
};

const loadQuestions = async () => {
  try {
    const r = await getTaskQuestions(taskId);
    if (r.code === 200)
      hasSubjective.value = (r.data || []).some((q) => SUBJECTIVE_TYPES.includes(q.questionType));
  } catch {
    /* */
  }
};

onMounted(() => {
  loadTask();
  loadSubmissions();
  loadQuestions();
});
</script>

<style scoped>
.page-card {
  padding: var(--spacing-lg);
}
</style>

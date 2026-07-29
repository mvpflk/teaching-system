import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  getWrongList,
  toggleWrongMastered,
  deleteWrongQuestion,
  batchDeleteWrongQuestions,
  getStudentStats,
  recordPractice,
} from '@/api/wrong';
import { getWeaknessAnalysis } from '@/api/wrong';
import { generateRemedial } from '@/api/ai';
import { createTask } from '@/api/task';
import { QUESTION_TYPE_LABEL } from '@/constants/questionTypes';
import { useIsMobile } from '@/composables/useIsMobile';
import echarts, { cssVar } from '@/utils/echarts';

const OBJECTIVE_TYPES = ['SINGLE_CHOICE', 'MULTI_CHOICE', 'TRUE_FALSE', 'FILL_IN'];
const SOURCE_TYPE_LABEL = {
  EXAM: '考试',
  HOMEWORK: '作业',
  QUIZ: '大屏抽问',
  BUZZ: '抢答',
  PRACTICE: '衍生练习',
};
const REDO_KEY = 'wrongbook_redo';

export function useWrongBook() {
  const router = useRouter();
  const { isMobile } = useIsMobile();

  const list = ref([]);
  const loading = ref(false);
  const page = ref(1);
  const total = ref(0);
  const pageSize = 20;
  const statusFilter = ref('all');
  const sourceTypeFilter = ref('');
  const groupBySubject = ref(false);
  const activeTab = ref('list');
  const stats = ref({
    total: 0,
    mastered: 0,
    unmastered: 0,
    weekPractice: 0,
    streak: 0,
    manualMastered: 0,
    practiceMastered: 0,
  });

  const detailVisible = ref(false);
  const detailRow = ref({});

  const weakList = ref([]);
  const weakLoading = ref(false);
  const weakChartRef = ref(null);
  let weakChartInstance = null;

  const remedialDialogVisible = ref(false);
  const remedialLoading = ref(false);
  const remedialQuestions = ref([]);
  const remedialNodeName = ref('');

  const redoVisible = ref(false);
  const redoList = ref([]);
  const redoIndex = ref(0);
  const redoAnswer = ref('');
  const redoRevealed = ref(false);
  const redoChecked = ref(false);
  const redoPassed = ref(false);
  const redoLoaded = ref(false);
  const redoMultiSel = ref([]);

  const redoQuestion = computed(() => redoList.value[redoIndex.value] || null);
  const isRedoMulti = computed(() => redoQuestion.value?.questionType === 'MULTI_CHOICE');

  const parsedRedoOptions = computed(() => {
    const q = redoQuestion.value;
    if (!q?.options) return [];
    try {
      const raw = typeof q.options === 'string' ? JSON.parse(q.options) : q.options;
      return raw.map((o) => String(o).replace(/^[A-D][.、．]\s*/, ''));
    } catch {
      return [];
    }
  });

  const parsedDetailOptions = computed(() => {
    if (!detailRow.value.options) return [];
    try {
      const raw = JSON.parse(detailRow.value.options);
      return raw.map((o) => String(o).replace(/^[A-D][.、．]\s*/, ''));
    } catch {
      return [];
    }
  });

  const correctLetters = computed(() =>
    (detailRow.value.correctAnswer || '')
      .split(',')
      .map((s) => s.trim().toUpperCase())
      .filter(Boolean)
  );
  const myLetters = computed(() =>
    (detailRow.value.myAnswer || '')
      .split(',')
      .map((s) => s.trim().toUpperCase())
      .filter(Boolean)
  );

  const subjectGroups = computed(() => {
    const groups = {};
    for (const row of list.value) {
      const key = row.subject || '未分类';
      if (!groups[key]) groups[key] = { subject: key, items: [], total: 0, mastered: 0 };
      groups[key].items.push(row);
      groups[key].total++;
      if (row.isMastered) groups[key].mastered++;
    }
    return Object.values(groups).sort((a, b) => b.total - a.total);
  });

  const isObjectiveType = (type) => OBJECTIVE_TYPES.includes(type);
  const isCorrectOption = (idx) => correctLetters.value.includes(String.fromCharCode(65 + idx));
  const isWrongOption = (idx) =>
    myLetters.value.includes(String.fromCharCode(65 + idx)) && !isCorrectOption(idx);

  function goBack() {
    router.back();
  }

  function goToSource(row) {
    if (row.sourceTaskId) router.push(`/student/tasks/detail/${row.sourceTaskId}`);
  }

  async function loadList() {
    loading.value = true;
    try {
      const params = {
        page: page.value,
        pageSize,
        mastered: statusFilter.value === 'all' ? 0 : statusFilter.value === 'mastered' ? 1 : 2,
      };
      if (sourceTypeFilter.value) params.sourceType = sourceTypeFilter.value;
      const res = await getWrongList(params);
      list.value = res.data?.records || [];
      total.value = res.data?.total || 0;
    } finally {
      loading.value = false;
    }
  }

  function showDetail(row) {
    detailRow.value = row;
    detailVisible.value = true;
  }

  async function toggleMastered(row) {
    const action = row.isMastered ? 'unmastered' : 'mastered';
    if (action === 'mastered') {
      try {
        await ElMessageBox.confirm(
          '手动标记不会记录为练习成果。建议通过"错题重做"或"衍生练习"来真正掌握。确定标记？',
          '确认标记',
          { confirmButtonText: '确定标记', cancelButtonText: '取消', type: 'info' }
        );
      } catch {
        return;
      }
    }
    try {
      await toggleWrongMastered(row.id, action === 'mastered');
      row.isMastered = !row.isMastered;
      ElMessage.success(row.isMastered ? '已标记为已掌握' : '已标记为未掌握');
      loadStats();
    } catch {
      ElMessage.error('操作失败，请重试');
    }
  }

  async function handleDelete(row) {
    try {
      await ElMessageBox.confirm('确定删除该错题记录？', '确认', { type: 'warning' });
      await deleteWrongQuestion(row.id);
      ElMessage.success('已删除');
      loadList();
    } catch {
      /* cancelled */
    }
  }

  async function batchDelete() {
    const masteredIds = list.value.filter((r) => r.isMastered).map((r) => r.id);
    if (!masteredIds.length) {
      ElMessage.info('没有已掌握的错题可删除');
      return;
    }
    try {
      await ElMessageBox.confirm(
        `确定删除 ${masteredIds.length} 条已掌握的错题记录？`,
        '批量删除',
        { type: 'warning' }
      );
      await batchDeleteWrongQuestions(masteredIds);
      ElMessage.success(`已删除 ${masteredIds.length} 条`);
      loadList();
    } catch {
      /* cancelled */
    }
  }

  function saveRedoState() {
    if (redoList.value.length) {
      sessionStorage.setItem(
        REDO_KEY,
        JSON.stringify({ ids: redoList.value.map((q) => q.id), index: redoIndex.value })
      );
    }
  }

  function clearRedoState() {
    sessionStorage.removeItem(REDO_KEY);
  }

  function markMastered() {
    const q = redoQuestion.value;
    if (!q) return;
    toggleWrongMastered(q.id, true);
    recordPractice(q.id, true).catch(() => {});
    q.mastered = true;
    ElMessage.success('已标记为已掌握');
  }

  function retryQuestion() {
    redoAnswer.value = '';
    redoMultiSel.value = [];
    redoRevealed.value = false;
    redoChecked.value = false;
    redoPassed.value = false;
  }

  const handleRedoCommand = (cmd) => {
    if (cmd === 'redo') startRedo();
    else if (cmd === 'derived') router.push('/student/derived-practice');
  };

  async function startRedo(fromResume) {
    try {
      const res = await getWrongList({ page: 1, pageSize: 999, mastered: 2 });
      const all = res.data?.records || [];
      if (!all.length) {
        ElMessage.info('没有未掌握的错题');
        return;
      }
      redoList.value = all;
      redoLoaded.value = true;
      redoIndex.value = fromResume
        ? JSON.parse(sessionStorage.getItem(REDO_KEY) || '{}').index || 0
        : 0;
      redoAnswer.value = '';
      redoRevealed.value = false;
      redoChecked.value = false;
      redoPassed.value = false;
      redoVisible.value = true;
      saveRedoState();
    } catch {
      ElMessage.error('加载错题失败');
    }
  }

  function redoReveal() {
    redoRevealed.value = true;
  }

  function redoNext() {
    if (redoIndex.value < redoList.value.length - 1) {
      redoIndex.value++;
      redoAnswer.value = '';
      redoMultiSel.value = [];
      redoRevealed.value = false;
      redoChecked.value = false;
      redoPassed.value = false;
      saveRedoState();
    } else {
      redoVisible.value = false;
      clearRedoState();
    }
  }

  function redoPrev() {
    if (redoIndex.value > 0) {
      redoIndex.value--;
      redoAnswer.value = '';
      redoMultiSel.value = [];
      redoRevealed.value = false;
      redoChecked.value = false;
      redoPassed.value = false;
      saveRedoState();
    }
  }

  function checkRedoAnswer() {
    const q = redoQuestion.value;
    if (!q) return;
    let userAns = '';
    if (isRedoMulti.value) {
      userAns = redoMultiSel.value.map((v, i) => (v ? String.fromCharCode(65 + i) : '')).join('');
      if (!userAns) {
        ElMessage.warning('请选择至少一个选项');
        return;
      }
    } else {
      if (!redoAnswer.value.trim()) {
        ElMessage.warning('请先输入你的答案');
        return;
      }
      userAns = redoAnswer.value.trim();
    }
    const correct = (q.correctAnswer || '').trim();
    let isCorrect = false;

    if (q.questionType === 'MULTI_CHOICE') {
      const userSorted = userAns.split('').sort().join('');
      const correctSorted = correct
        .replace(/[,，、]/g, '')
        .split('')
        .sort()
        .join('');
      isCorrect = userSorted === correctSorted;
    } else if (q.questionType === 'SINGLE_CHOICE' || q.questionType === 'TRUE_FALSE') {
      if (q.questionType === 'TRUE_FALSE') {
        const u = userAns.toUpperCase().replace(/[.。、)）]$/, '');
        const c = correct.toUpperCase().replace(/[.。、)）]$/, '');
        const isTrue = (v) => ['A', 'T', 'TRUE', 'YES', '对', '正确', '√'].includes(v);
        const isFalse = (v) => ['B', 'F', 'FALSE', 'NO', '错', '错误', '×'].includes(v);
        isCorrect = (isTrue(u) && isTrue(c)) || (isFalse(u) && isFalse(c));
      } else {
        isCorrect = userAns.toUpperCase() === correct.toUpperCase();
      }
    } else if (q.questionType === 'FILL_IN') {
      const accepted = correct
        .split(/[,，|]/)
        .map((s) => s.trim().toLowerCase())
        .filter(Boolean);
      isCorrect = accepted.some((a) => userAns.toLowerCase() === a);
    }

    redoChecked.value = true;
    if (isCorrect) {
      redoPassed.value = true;
      ElMessage.success('✓ 回答正确！');
      toggleWrongMastered(q.id, true);
      recordPractice(q.id, true).catch(() => {});
      q.mastered = true;
      setTimeout(redoNext, 1000);
    } else {
      ElMessage.warning('答案不正确，看看正确答案吧');
      recordPractice(q.id, false).catch(() => {});
      redoRevealed.value = true;
    }
  }

  function onRedoOpened() {
    /* noop */
  }

  async function loadWeakness() {
    weakLoading.value = true;
    try {
      const res = await getWeaknessAnalysis();
      weakList.value = res.data || [];
      await nextTick();
      renderWeakChart();
    } catch {
      weakList.value = [];
    } finally {
      weakLoading.value = false;
    }
  }

  function renderWeakChart() {
    if (!weakChartRef.value || !weakList.value.length) return;
    if (weakChartInstance) weakChartInstance.dispose();
    weakChartInstance = echarts.init(weakChartRef.value);
    const names = weakList.value.map((w) =>
      w.knowledgeNodeName?.length > 8
        ? w.knowledgeNodeName.substring(0, 8) + '...'
        : w.knowledgeNodeName
    );
    const counts = weakList.value.map((w) => w.errorCount);
    weakChartInstance.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: '3%', right: '4%', bottom: '10%', top: '8%', containLabel: true },
      xAxis: { type: 'category', data: names, axisLabel: { rotate: 30, fontSize: 11 } },
      yAxis: { type: 'value', name: '错误次数', minInterval: 1 },
      series: [
        {
          type: 'bar',
          data: counts,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: cssVar('--el-color-danger') },
              { offset: 1, color: '#f89898' },
            ]),
          },
          label: { show: true, position: 'top', color: cssVar('--el-color-danger') },
        },
      ],
    });
    window.addEventListener('resize', _weakChartResize);
  }

  const _weakChartResize = () => weakChartInstance?.resize();

  async function generateRemedialForNode(row) {
    remedialNodeName.value = row.knowledgeNodeName;
    remedialDialogVisible.value = true;
    remedialLoading.value = true;
    remedialQuestions.value = [];
    try {
      const res = await generateRemedial({
        knowledgeNodeIds: [row.knowledgeNodeId],
        subject: undefined,
      });
      if (res.code === 429) {
        ElMessage.error(res.message || '今日练习次数已用完');
      } else if (res.code === 200 && res.data) {
        remedialQuestions.value = res.data.questions || [];
        const quotaMsg =
          res.data.quotaRemaining !== undefined ? `，今日剩余 ${res.data.quotaRemaining} 次` : '';
        ElMessage.success(`已生成 ${remedialQuestions.value.length} 道针对性练习${quotaMsg}`);
        await nextTick();
      } else {
        ElMessage.error(res.message || '生成失败');
      }
    } catch {
      ElMessage.error('AI生成失败，请稍后重试');
    } finally {
      remedialLoading.value = false;
    }
  }

  const qTypeTag = (type) =>
    ({
      SINGLE_CHOICE: 'primary',
      MULTI_CHOICE: 'success',
      TRUE_FALSE: 'warning',
      FILL_IN: 'info',
      ESSAY: '',
    })[type] || 'info';

  function parsedRemedialOpts(opts) {
    if (!opts) return [];
    try {
      return (typeof opts === 'string' ? JSON.parse(opts) : opts).map((o) =>
        String(o).replace(/^[A-D][.、．]\s*/, '')
      );
    } catch {
      return [];
    }
  }

  async function startRemedialPractice() {
    if (!remedialQuestions.value.length) {
      ElMessage.warning('没有可练习的题目');
      return;
    }
    try {
      const questionIds = remedialQuestions.value.map((q) => q.id);
      const res = await createTask({
        title: `针对性练习 - ${remedialNodeName.value}`,
        taskType: 'AFTER_CLASS',
        scoreType: 'POINT_100',
        targetType: 'PERSONAL',
        questionIds,
      });
      if (res.code === 200) {
        ElMessage.success('个人练习任务已创建');
        remedialDialogVisible.value = false;
        router.push(`/student/tasks/${res.data.id}`);
      } else {
        ElMessage.error(res.message || '创建任务失败');
      }
    } catch {
      ElMessage.error('创建任务失败');
    }
  }

  function generateDerivedForSubject(subject) {
    router.push(`/student/derived-practice?subject=${encodeURIComponent(subject)}`);
  }

  watch(activeTab, (val) => {
    if (val === 'weakness' && !weakList.value.length && !weakLoading.value) loadWeakness();
  });

  async function loadStats() {
    try {
      const res = await getStudentStats();
      if (res.code === 200) stats.value = res.data;
    } catch {
      /* silent */
    }
  }

  onMounted(async () => {
    loadList();
    loadStats();
    clearRedoState();
    const saved = sessionStorage.getItem(REDO_KEY);
    if (saved) {
      try {
        const { ids, index } = JSON.parse(saved);
        if (ids?.length) {
          const res = await getWrongList({ page: 1, pageSize: 999, mastered: 2 });
          const all = res.data?.records || [];
          const matched = all.filter((q) => ids.includes(q.id));
          if (matched.length) {
            redoList.value = matched;
            try {
              await ElMessageBox.confirm(
                `你有未完成的错题重做（${matched.length}题，上次做到第${(index || 0) + 1}题），继续吗？`,
                '恢复重做',
                { confirmButtonText: '继续', cancelButtonText: '重新开始', type: 'info' }
              );
            } catch {
              clearRedoState();
              return;
            }
            startRedo(true);
          } else {
            clearRedoState();
          }
        }
      } catch {
        clearRedoState();
      }
    }
  });

  onUnmounted(() => {
    window.removeEventListener('resize', _weakChartResize);
    weakChartInstance?.dispose();
  });

  return {
    isMobile,
    list,
    loading,
    page,
    total,
    pageSize,
    statusFilter,
    sourceTypeFilter,
    groupBySubject,
    activeTab,
    stats,
    detailVisible,
    detailRow,
    weakList,
    weakLoading,
    weakChartRef,
    remedialDialogVisible,
    remedialLoading,
    remedialQuestions,
    remedialNodeName,
    redoVisible,
    redoList,
    redoIndex,
    redoAnswer,
    redoRevealed,
    redoChecked,
    redoPassed,
    redoLoaded,
    redoMultiSel,
    redoQuestion,
    isRedoMulti,
    parsedRedoOptions,
    parsedDetailOptions,
    subjectGroups,
    isObjectiveType,
    isCorrectOption,
    isWrongOption,
    SOURCE_TYPE_LABEL,
    QUESTION_TYPE_LABEL,
    goBack,
    goToSource,
    loadList,
    showDetail,
    toggleMastered,
    handleDelete,
    batchDelete,
    handleRedoCommand,
    startRedo,
    redoNext,
    redoPrev,
    checkRedoAnswer,
    saveRedoState,
    clearRedoState,
    markMastered,
    retryQuestion,
    onRedoOpened,
    redoReveal,
    generateRemedialForNode,
    startRemedialPractice,
    qTypeTag,
    parsedRemedialOpts,
    generateDerivedForSubject,
  };
}

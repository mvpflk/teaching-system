import { ref, reactive, watch } from 'vue';
import {
  startQuiz,
  gradeQuiz,
  getQuestions,
  getQuestionFilters,
  getAbsentStudents,
  removeFromQuizPool,
  resetQuizPool,
} from '@/api/classroom';
import { getQuestionBankList } from '@/api/questionBank';
import { getNodeTree } from '@/api/knowledgeNode';
import { ElMessage, ElMessageBox } from 'element-plus';
import { findChildren } from '@/utils/category';

export function useLabQuiz() {
  const stage = ref(1);
  const mySubjects = ref([]);
  const sysChapters = ref([]);
  const sysTasks = ref([]);
  const sysKps = ref([]);
  const sysChapterId = ref(null);
  const sysTaskId = ref(null);
  const sysFilter = reactive({
    subjectId: null,
    categoryId: null,
    questionType: '',
    difficultyLevel: null,
    keyword: '',
  });
  const sysQuestions = ref([]);
  const sysLoading = ref(false);
  const sysPage = ref(1);
  const sysPageSize = ref(15);
  const sysTotal = ref(0);
  const selectedSysQ = ref(null);
  const sysTree = ref([]);
  const localFilter = reactive({ subject: '', tag: '', keyword: '' });
  const localFilterOpts = reactive({ subjects: [], chapters: [], tags: [] });
  const localQuestions = ref([]);
  const localLoading = ref(false);
  const localPage = ref(1);
  const localPageSize = ref(15);
  const localTotal = ref(0);
  const selectedLocalQ = ref(null);
  const selectedQuestionText = ref('');
  const txtUploading = ref(false);
  const showImportDialog = ref(false);
  const showEditDialog = ref(false);
  const editingQuestion = ref(null);
  const savingEdit = ref(false);
  const aiTabRef = ref(null);
  const selectedAiQ = ref(null);
  const aiSubject = ref('');
  const showAiGen = ref(false);
  const aiGenSubjects = ref([]);
  const settings = reactive({
    excludeAbsent: true,
    downWeightPicked: true,
    downWeightCorrect: true,
  });
  const pickedSet = ref(new Set());
  const correctSet = ref(new Set());
  const picking = ref(false);
  const pickedStudent = ref(null);
  const studentAnswer = ref('');
  const currentSessionId = ref(null);
  const grading = ref(null);

  watch(mySubjects, (val) => {
    aiGenSubjects.value = (val || []).map((s) => s.subjectName).filter(Boolean);
  });

  const onSysSubjectChange = async (subjectId) => {
    sysChapterId.value = null;
    sysTaskId.value = null;
    sysFilter.categoryId = null;
    sysChapters.value = [];
    sysTasks.value = [];
    sysKps.value = [];
    if (!subjectId) {
      loadSysQuestions();
      return;
    }
    const res = await getNodeTree();
    if (res.code === 200) {
      sysTree.value = res.data || [];
      const sn = sysTree.value.find((n) => n.subjectId === subjectId && n.level === 1);
      sysChapters.value = sn?.children || [];
    }
    loadSysQuestions();
  };
  const onSysChapterChange = (val) => {
    sysTaskId.value = null;
    sysFilter.categoryId = null;
    sysTasks.value = [];
    sysKps.value = [];
    if (val) sysTasks.value = findChildren(sysTree.value, val);
    loadSysQuestions();
  };
  const onSysTaskChange = (val) => {
    sysFilter.categoryId = null;
    sysKps.value = [];
    if (val) sysKps.value = findChildren(sysTree.value, val);
    loadSysQuestions();
  };

  const loadSysQuestions = async () => {
    sysLoading.value = true;
    try {
      const params = { page: sysPage.value, pageSize: sysPageSize.value };
      if (sysFilter.subjectId) params.subjectId = sysFilter.subjectId;
      if (sysFilter.categoryId) params.categoryId = sysFilter.categoryId;
      if (sysFilter.questionType) params.questionType = sysFilter.questionType;
      if (sysFilter.difficultyLevel) params.difficultyLevel = sysFilter.difficultyLevel;
      if (sysFilter.keyword) params.keyword = sysFilter.keyword;
      const res = await getQuestionBankList(params);
      if (res.code === 200) {
        sysQuestions.value = res.data?.records || res.data || [];
        sysTotal.value = res.data?.total || 0;
      }
    } catch {
      ElMessage.error('加载系统题库失败');
    } finally {
      sysLoading.value = false;
    }
  };

  const selectSysQuestion = (q) => {
    selectedSysQ.value = q;
    selectedLocalQ.value = null;
    selectedAiQ.value = null;
    let fullText = q.questionText || q.content || '';
    try {
      let opts;
      if (q.options) opts = typeof q.options === 'string' ? JSON.parse(q.options) : q.options;
      if (opts && Array.isArray(opts) && opts.length > 0) {
        const lb = ['A', 'B', 'C', 'D', 'E', 'F'];
        fullText += '\n\n' + opts.map((o, i) => `${lb[i] || i + 1}. ${o}`).join('\n');
      }
    } catch {
      /* */
    }
    selectedQuestionText.value = fullText;
  };

  const loadLocalFilters = async () => {
    try {
      const res = await getQuestionFilters();
      if (res.code === 200) {
        localFilterOpts.subjects = res.data.subjects || [];
        localFilterOpts.chapters = res.data.chapters || [];
        localFilterOpts.tags = res.data.tags || [];
      }
    } catch {
      /* */
    }
  };
  const loadLocalQuestions = async () => {
    localLoading.value = true;
    try {
      const params = { page: localPage.value, pageSize: localPageSize.value };
      if (localFilter.subject) params.subject = localFilter.subject;
      if (localFilter.tag) params.tag = localFilter.tag;
      if (localFilter.keyword) params.keyword = localFilter.keyword;
      const res = await getQuestions(params);
      if (res.code === 200) {
        localQuestions.value = res.data?.records || [];
        localTotal.value = res.data?.total || 0;
      }
    } catch {
      ElMessage.error('加载本地题库失败');
    } finally {
      localLoading.value = false;
    }
  };
  const selectLocalQuestion = (q) => {
    selectedLocalQ.value = q;
    selectedSysQ.value = null;
    selectedAiQ.value = null;
    selectedQuestionText.value = q.content;
  };
  const editLocalQuestion = (q) => {
    editingQuestion.value = { ...q };
    showEditDialog.value = true;
  };
  const deleteLocalQuestion = (q) => {
    ElMessageBox.confirm('确定删除此题目？', '提示', { type: 'warning' })
      .then(() => {
        const idx = localQuestions.value.findIndex((item) => item.id === q.id);
        if (idx !== -1) localQuestions.value.splice(idx, 1);
      })
      .catch(() => {});
  };
  const saveLocalQuestion = async () => {
    if (!editingQuestion.value) return;
    savingEdit.value = true;
    try {
      savingEdit.value = false;
      showEditDialog.value = false;
    } catch {
      savingEdit.value = false;
    }
  };
  const onAiQuestionSelect = (q) => {
    selectedAiQ.value = q;
    selectedSysQ.value = null;
    selectedLocalQ.value = null;
    selectedQuestionText.value = q.content || q.questionText || '';
  };

  const onAiGenerated = (qs, sceneMode, cQuestionPool, aTabRef) => {
    if (sceneMode === 'CLASSROOM') {
      for (const q of qs)
        cQuestionPool.value.push({
          id: 'ai_' + Date.now() + '_' + Math.random(),
          content: q.questionText || q.content,
          tag: 'AI生成',
        });
      ElMessage.success(`AI生成了${qs.length}道题，已加入抽题池`);
    } else {
      aTabRef?.addQuestions(qs);
      ElMessage.success(`AI生成了${qs.length}道课堂提问`);
    }
  };
  const clearSelection = () => {
    selectedSysQ.value = null;
    selectedLocalQ.value = null;
    selectedAiQ.value = null;
    selectedQuestionText.value = '';
  };
  const onPageChange = (source, page) => {
    if (source === 'sys') {
      sysPage.value = page;
      loadSysQuestions();
    } else if (source === 'local') {
      localPage.value = page;
      loadLocalQuestions();
    }
  };

  const buildWeights = () => {
    const w = {};
    pickedSet.value.forEach((id) => {
      w[id] = (w[id] || 1.0) * 0.3;
    });
    correctSet.value.forEach((id) => {
      w[id] = (w[id] || 1.0) * 0.2;
    });
    return w;
  };

  const confirmRemoveFromPool = async (classId, student) => {
    try {
      await ElMessageBox.confirm(
        `将«${student.studentName}»从抽问池移除？该生将不再被抽到`,
        '手动移除',
        { type: 'warning', confirmButtonText: '移除', cancelButtonText: '取消' }
      );
      await removeFromQuizPool(classId, student.studentId);
      ElMessage.success(`已移除 ${student.studentName}`);
      pickedStudent.value = null;
    } catch {
      /* 取消 */
    }
  };

  const pickStudent = async (classId, sseConn) => {
    picking.value = true;
    try {
      const excludeIds = [];
      if (settings.excludeAbsent) {
        try {
          const aRes = await getAbsentStudents(classId);
          if (aRes.code === 200 && aRes.data)
            aRes.data.forEach((id) => {
              if (!excludeIds.includes(id)) excludeIds.push(id);
            });
        } catch {
          /* */
        }
      }
      const q = selectedSysQ.value || selectedLocalQ.value || selectedAiQ.value;
      const res = await startQuiz({
        classId,
        questionId: q?.id || null,
        questionText: selectedQuestionText.value,
        sceneMode: 'LAB',
        questionType: q?.questionType || null,
        options:
          typeof q?.options === 'string'
            ? q.options
            : q?.options
              ? JSON.stringify(q.options)
              : null,
        excludeStudentIds: excludeIds,
        studentWeights: buildWeights(),
      });
      if (res.code === 200) {
        pickedStudent.value = res.data;
        currentSessionId.value = res.data.sessionId;
        studentAnswer.value = '';
        stage.value = 3;
        if (res.data.offlinePick)
          ElMessage.warning(
            `⚠️ ${res.data.studentName} 可能不在线（当前仅${res.data.onlineCount || 0}人在线），请确认学生是否能作答`
          );
        pickupAnswerListener(sseConn, true);
      } else {
        ElMessage.error(res.message || '抽人失败');
      }
    } catch (e) {
      if (e?.response?.status === 409) {
        try {
          await ElMessageBox.confirm(
            '本轮所有学生已被抽过一遍，是否重置并开始新一轮？',
            '抽问池已空',
            { type: 'warning', confirmButtonText: '重置', cancelButtonText: '取消' }
          );
          await resetQuizPool(classId);
          pickedSet.value = new Set();
          correctSet.value = new Set();
          ElMessage.success('抽问池已重置，请重新选题抽人');
        } catch {
          /* 用户取消 */
        }
      }
    } finally {
      picking.value = false;
    }
  };

  const grade = async (result, score, classId, sseConn, emit) => {
    if (result === -1) {
      pickupAnswerListener(sseConn, false);
      stage.value = 1;
      pickedStudent.value = null;
      studentAnswer.value = '';
      return;
    }
    if (grading.value !== null) return;
    grading.value = result;
    try {
      const res = await gradeQuiz({
        sessionId: currentSessionId.value,
        studentId: pickedStudent.value.studentId,
        result,
        score,
        response: '',
      });
      if (res.code === 200) {
        pickedSet.value.add(pickedStudent.value.studentId);
        if (result > 0) correctSet.value.add(pickedStudent.value.studentId);
        ElMessage.success(
          { 1: `回答正确 +${score}分`, 2: `部分正确 +${score}分`, 0: '回答错误，已记录错题本' }[
            result
          ]
        );
        emit('scored', { studentId: pickedStudent.value.studentId, ...res.data });
        pickupAnswerListener(sseConn, false);
        stage.value = 1;
        pickedStudent.value = null;
        studentAnswer.value = '';
      } else {
        ElMessage.error(res.message || '评分失败');
      }
    } catch {
      ElMessage.error('评分请求失败');
    } finally {
      grading.value = null;
    }
  };

  let _answerHandler = null;
  const pickupAnswerListener = (sseConn, attach) => {
    if (!sseConn) return;
    if (!attach && _answerHandler) {
      sseConn.removeEventListener('answer:submitted', _answerHandler);
      _answerHandler = null;
      return;
    }
    if (attach) {
      _answerHandler = (e) => {
        const d = JSON.parse(e.data);
        if (d.studentId === pickedStudent.value?.studentId)
          studentAnswer.value = d.answerText || '';
      };
      sseConn.addEventListener('answer:submitted', _answerHandler);
    }
  };

  return {
    stage,
    mySubjects,
    sysChapters,
    sysTasks,
    sysKps,
    sysChapterId,
    sysTaskId,
    sysFilter,
    sysQuestions,
    sysLoading,
    sysPage,
    sysPageSize,
    sysTotal,
    selectedSysQ,
    localFilter,
    localFilterOpts,
    localQuestions,
    localLoading,
    localPage,
    localPageSize,
    localTotal,
    selectedLocalQ,
    selectedAiQ,
    selectedQuestionText,
    txtUploading,
    showImportDialog,
    settings,
    picking,
    pickedStudent,
    studentAnswer,
    grading,
    showEditDialog,
    editingQuestion,
    savingEdit,
    aiTabRef,
    aiSubject,
    showAiGen,
    aiGenSubjects,
    onSysSubjectChange,
    onSysChapterChange,
    onSysTaskChange,
    loadSysQuestions,
    selectSysQuestion,
    selectLocalQuestion,
    onAiQuestionSelect,
    loadLocalFilters,
    loadLocalQuestions,
    editLocalQuestion,
    deleteLocalQuestion,
    saveLocalQuestion,
    clearSelection,
    onPageChange,
    confirmRemoveFromPool,
    pickStudent,
    grade,
    onAiGenerated,
    pickupAnswerListener,
  };
}

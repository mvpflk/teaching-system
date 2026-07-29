import { ref, reactive, computed } from 'vue';
import {
  startQuiz,
  gradeQuiz,
  getQuestions,
  getAbsentStudents,
  deleteQuestion,
} from '@/api/classroom';
import { getStudents as getClassStudents } from '@/api/classes';
import { getNodeTree } from '@/api/knowledgeNode';
import { ElMessage, ElMessageBox } from 'element-plus';

export function useClassroomQuiz() {
  const cState = ref('idle');
  const cMySubjects = ref([]);
  const cSel = reactive({ subjectId: null, chapterId: null, taskId: null });
  const cChapters = ref([]);
  const cTasks = ref([]);
  const cQuestionPool = ref([]);
  const cQuestionQueue = ref([]);
  const cStudentPool = ref([]);
  const studentCallCounts = reactive({});
  const cCurrentQuestion = ref(null);
  const cCurrentStudent = ref(null);
  const cGrading = ref(false);
  const cAbsentCount = ref(0);
  const cStats = reactive({ drawn: 0, called: 0, correct: 0, total: 0 });
  const cCurrentSessionId = ref(null);
  const cCurrentStudentScore = ref(0);
  const cLastResult = ref(null);
  const cScore = ref(1);
  const isFullscreen = ref(false);
  const showSummary = ref(false);
  const summaryScoreMap = reactive({});

  const shuffle = (arr) => {
    const a = [...arr];
    for (let i = a.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [a[i], a[j]] = [a[j], a[i]];
    }
    return a;
  };
  const progressPercent = computed(() =>
    cStudentPool.value.length
      ? Math.min(100, Math.round((cStats.called / cStudentPool.value.length) * 100))
      : 0
  );
  const summaryLeaderboard = computed(() =>
    Object.values(summaryScoreMap)
      .sort((a, b) => b.score - a.score)
      .slice(0, 10)
  );

  const onCSubjectChange = async (sid) => {
    cSel.chapterId = null;
    cSel.taskId = null;
    cChapters.value = [];
    cTasks.value = [];
    cQuestionPool.value = [];
    cQuestionQueue.value = [];
    if (!sid) return;
    try {
      const res = await getNodeTree();
      if (res.code === 200) {
        const root = (res.data || []).find((n) => n.subjectId === sid && n.level === 1);
        cChapters.value = root?.children || [];
      }
    } catch {
      /* */
    }
  };
  const onCChapterChange = (cid) => {
    cSel.taskId = null;
    cTasks.value = [];
    cQuestionPool.value = [];
    cQuestionQueue.value = [];
    if (!cid) return;
    const chapter = cChapters.value.find((ch) => ch.id === cid);
    cTasks.value = chapter?.children || [];
  };
  const onCTaskChange = async (tid) => {
    cQuestionPool.value = [];
    cQuestionQueue.value = [];
    if (!tid) return;
    try {
      const res = await getQuestions({ taskId: tid, page: 1, pageSize: 10000 });
      if (res.code === 200) {
        cQuestionPool.value = (res.data?.records || []).filter((q) => q.content);
        cQuestionQueue.value = shuffle(cQuestionPool.value);
      }
    } catch {
      /* */
    }
  };

  const loadClassroomStudentPool = async (classId) => {
    try {
      const [sRes, aRes] = await Promise.all([
        getClassStudents(classId),
        getAbsentStudents(classId),
      ]);
      const absentIds = new Set(aRes.code === 200 && aRes.data ? aRes.data : []);
      cAbsentCount.value = absentIds.size;
      if (sRes.code === 200 && sRes.data) {
        cStudentPool.value = (sRes.data || [])
          .filter((s) => !absentIds.has(s.id))
          .map((s) => ({
            id: s.id,
            name: s.realName || s.name || s.studentName || '未知',
            avatarUrl: s.avatarUrl || null,
          }));
      }
    } catch {
      /* */
    }
  };

  const drawNextQuestion = () => {
    if (cQuestionQueue.value.length === 0) {
      if (cQuestionPool.value.length === 0) return null;
      cQuestionQueue.value = shuffle(cQuestionPool.value);
    }
    return cQuestionQueue.value.shift() || null;
  };
  const drawNextStudent = () => {
    const pool = cStudentPool.value;
    if (!pool.length) return null;
    const items = pool.map((s) => {
      const calls = studentCallCounts[s.id] || 0;
      return {
        student: s,
        weight: calls === 0 ? 1.0 : calls === 1 ? 0.2 : calls === 2 ? 0.08 : 0.03,
      };
    });
    const totalWeight = items.reduce((sum, it) => sum + it.weight, 0);
    if (totalWeight <= 0) return pool[Math.floor(Math.random() * pool.length)];
    const rand = Math.random() * totalWeight;
    let cumulative = 0;
    for (const item of items) {
      cumulative += item.weight;
      if (rand < cumulative) {
        studentCallCounts[item.student.id] = (studentCallCounts[item.student.id] || 0) + 1;
        return item.student;
      }
    }
    const last = items[items.length - 1];
    studentCallCounts[last.student.id] = (studentCallCounts[last.student.id] || 0) + 1;
    return last.student;
  };

  const enterFullscreen = () => {
    const el = document.querySelector('.qpc-body');
    if (el?.requestFullscreen) {
      el.requestFullscreen();
      isFullscreen.value = true;
    }
  };
  const exitFullscreen = () => {
    if (document.fullscreenElement) {
      document.exitFullscreen();
      isFullscreen.value = false;
    }
  };

  const classroomDraw = async (classId, startWheelFn, startCountdownFn) => {
    if (!cQuestionPool.value.length) return ElMessage.warning('题库为空');
    if (!cStudentPool.value.length) return ElMessage.warning('无可用学生');
    const q = drawNextQuestion();
    const s = drawNextStudent();
    if (!q || !s) return ElMessage.error('抽取失败');
    cCurrentQuestion.value = q;
    cCurrentStudent.value = s;
    cCurrentStudentScore.value = 0;
    cLastResult.value = null;
    cStats.drawn++;
    cState.value = 'active';
    if (!isFullscreen.value) setTimeout(() => enterFullscreen(), 100);
    startWheelFn(s, cStudentPool.value);
    setTimeout(() => {
      if (cState.value === 'active') startCountdownFn();
    }, 2800);
    try {
      const res = await startQuiz({
        classId,
        questionId: q.id,
        questionText: q.content,
        sceneMode: 'CLASSROOM',
        excludeStudentIds: [],
        studentWeights: {},
      });
      if (res.code === 200) cCurrentSessionId.value = res.data?.sessionId;
    } catch {
      /* */
    }
  };

  const classroomGrade = async (
    result,
    classId,
    startWheelFn,
    startCountdownFn,
    playCorrectFn,
    playWrongFn,
    stopCountdownFn,
    emit
  ) => {
    if (cGrading.value) return;
    cGrading.value = true;
    stopCountdownFn();
    cStats.total++;
    cLastResult.value = result > 0;
    const score = cScore.value;
    const sid = cCurrentStudent.value?.id;
    if (sid) {
      if (!summaryScoreMap[sid])
        summaryScoreMap[sid] = {
          studentId: sid,
          name: cCurrentStudent.value.name,
          avatarUrl: cCurrentStudent.value.avatarUrl,
          score: 0,
        };
      if (result > 0) summaryScoreMap[sid].score += score;
    }
    if (result > 0) playCorrectFn();
    else playWrongFn();
    try {
      if (cCurrentSessionId.value)
        await gradeQuiz({
          sessionId: cCurrentSessionId.value,
          studentId: cCurrentStudent.value.id,
          result,
          score,
          response: '',
        });
      if (result > 0) {
        cStats.correct++;
        cCurrentStudentScore.value += score;
        ElMessage.success(
          `${cCurrentStudent.value.name} ${result === 1 ? '回答正确' : '部分正确'} +${score}分`
        );
        emit('scored', { studentId: cCurrentStudent.value.id });
      } else {
        ElMessage.info(`${cCurrentStudent.value.name} 回答错误 — 自动换下一位学生`);
        const s = drawNextStudent();
        if (s) {
          cCurrentStudent.value = s;
          cCurrentStudentScore.value = 0;
          cStats.called++;
          cLastResult.value = null;
          startWheelFn(s, cStudentPool.value);
          setTimeout(() => {
            if (cState.value === 'active') startCountdownFn();
          }, 2800);
        }
      }
    } catch {
      if (result > 0) {
        cStats.correct++;
        cCurrentStudentScore.value++;
      }
    } finally {
      cGrading.value = false;
    }
  };

  const classroomSwitchQuestion = (stopCountdownFn, classId) => {
    stopCountdownFn();
    const q = drawNextQuestion();
    if (!q) {
      ElMessage.warning('题库已用完');
      return;
    }
    cCurrentQuestion.value = q;
    cCurrentSessionId.value = null;
    cLastResult.value = null;
    cStats.drawn++;
    startQuiz({
      classId,
      questionId: q.id,
      questionText: q.content,
      sceneMode: 'CLASSROOM',
      excludeStudentIds: [],
      studentWeights: {},
    })
      .then((res) => {
        if (res.code === 200) cCurrentSessionId.value = res.data?.sessionId;
      })
      .catch(() => {});
  };

  const classroomSwitchStudent = (stopCountdownFn, startWheelFn, startCountdownFn) => {
    stopCountdownFn();
    const s = drawNextStudent();
    if (!s) {
      ElMessage.warning('学生已轮完');
      return;
    }
    cCurrentStudent.value = s;
    cCurrentStudentScore.value = 0;
    cCurrentSessionId.value = null;
    cLastResult.value = null;
    cStats.called++;
    startWheelFn(s, cStudentPool.value);
    setTimeout(() => {
      if (cState.value === 'active') startCountdownFn();
    }, 2800);
  };

  const endClassroom = async (stopCountdownFn) => {
    stopCountdownFn();
    if (isFullscreen.value) exitFullscreen();
    try {
      await ElMessageBox.confirm('确定结束本轮课堂抽问吗？将显示本轮总结。', '结束课堂', {
        type: 'warning',
        confirmButtonText: '确定',
      });
    } catch {
      return;
    }
    showSummary.value = true;
  };

  const restartSession = () => {
    showSummary.value = false;
    Object.keys(summaryScoreMap).forEach((k) => delete summaryScoreMap[k]);
    cQuestionQueue.value = shuffle(cQuestionPool.value);
    Object.keys(studentCallCounts).forEach((k) => delete studentCallCounts[k]);
    Object.assign(cStats, { drawn: 0, called: 0, correct: 0, total: 0 });
    cState.value = 'idle';
  };
  const exitSummary = () => {
    showSummary.value = false;
    cState.value = 'idle';
  };

  const clearClassroomPool = async () => {
    try {
      await ElMessageBox.confirm('确定清空该任务下所有抽问题目？', '确认清空', { type: 'warning' });
    } catch {
      return;
    }
    for (const q of cQuestionPool.value) {
      try {
        await deleteQuestion(q.id);
      } catch {
        /* */
      }
    }
    cQuestionPool.value = [];
    cQuestionQueue.value = [];
    ElMessage.success('已清空');
  };

  const onClassroomKeydown = (e) => {
    if (cState.value !== 'active' || cGrading.value) return null;
    if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return null;
    switch (e.key) {
      case '1':
        return { action: 'grade', result: 1 };
      case '2':
        return { action: 'grade', result: 2 };
      case '3':
        return { action: 'grade', result: 0 };
      case 's':
      case 'S':
        return { action: 'switchStudent' };
      case 'q':
      case 'Q':
        return { action: 'switchQuestion' };
      default:
        return null;
    }
  };

  const onFsChange = () => {
    if (!document.fullscreenElement) isFullscreen.value = false;
  };

  const onKeydown = (e, sceneMode, stopCountdownFn) => {
    if (e.key === 'Escape' && sceneMode === 'CLASSROOM') {
      if (document.querySelector('.el-overlay')) return;
      e.preventDefault();
      e.stopPropagation();
      if (isFullscreen.value) exitFullscreen();
      if (cState.value === 'active') endClassroom(stopCountdownFn);
    }
  };

  return {
    cState,
    cMySubjects,
    cSel,
    cChapters,
    cTasks,
    cQuestionPool,
    cQuestionQueue,
    cStudentPool,
    studentCallCounts,
    cCurrentQuestion,
    cCurrentStudent,
    cGrading,
    cAbsentCount,
    cStats,
    cCurrentStudentScore,
    cLastResult,
    cScore,
    isFullscreen,
    showSummary,
    summaryScoreMap,
    progressPercent,
    summaryLeaderboard,
    onCSubjectChange,
    onCChapterChange,
    onCTaskChange,
    loadClassroomStudentPool,
    classroomDraw,
    classroomGrade,
    classroomSwitchQuestion,
    classroomSwitchStudent,
    endClassroom,
    restartSession,
    exitSummary,
    clearClassroomPool,
    onClassroomKeydown,
    onFsChange,
    onKeydown,
  };
}

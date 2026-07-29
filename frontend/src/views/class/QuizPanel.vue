<template>
  <div class="quiz-panel">
    <QuizHeader
      :scene-mode="sceneMode"
      :stage="stage"
      :c-state="cState"
      @back="$emit('back')"
    />

    <template v-if="sceneMode !== 'CLASSROOM'">
      <Transition name="slide-fade" mode="out-in">
        <LabQuestionSelector
          v-if="stage === 1"
          key="stage1"
          :my-subjects="mySubjects"
          :sys-chapters="sysChapters"
          :sys-tasks="sysTasks"
          :sys-kps="sysKps"
          :sys-questions="sysQuestions"
          :sys-loading="sysLoading"
          :sys-page="sysPage"
          :sys-page-size="sysPageSize"
          :sys-total="sysTotal"
          :sys-filter="sysFilter"
          :sys-chapter-id="sysChapterId"
          :sys-task-id="sysTaskId"
          :local-questions="localQuestions"
          :local-loading="localLoading"
          :local-page="localPage"
          :local-page-size="localPageSize"
          :local-total="localTotal"
          :local-filter="localFilter"
          :local-filter-opts="localFilterOpts"
          :selected-text="selectedQuestionText"
          :selected-id="selectedSysQ?.id || selectedLocalQ?.id"
          @sys-subject-change="onSysSubjectChange"
          @sys-chapter-change="onSysChapterChange"
          @sys-task-change="onSysTaskChange"
          @load-sys="loadSysQuestions"
          @load-local="loadLocalQuestions"
          @page-change="onPageChange"
          @open-import="showImportDialog = true"
          @select-system="selectSysQuestion"
          @select-local="selectLocalQuestion"
          @edit-question="editLocalQuestion"
          @delete-question="deleteLocalQuestion"
          @clear-selection="clearSelection"
          @confirm-selection="stage = 2"
          @update:sys-chapter-id="sysChapterId = $event"
          @update:sys-task-id="sysTaskId = $event"
          @tab-ai="aiTabRef?.loadAiQuestions()"
        >
          <template #ai-tab>
            <AiQuestionTab ref="aiTabRef" :subject="aiSubject" @select="onAiQuestionSelect" />
          </template>
        </LabQuestionSelector>

        <LabStudentPicker
          v-else-if="stage === 2"
          key="stage2"
          :question-text="selectedQuestionText"
          :picking="picking"
          :picked-student="pickedStudent"
          :settings="settings"
          @go-back="stage = 1"
          @pick="() => pickStudent(props.classId, props.sseConn)"
          @confirm="stage = 3"
          @remove-from-pool="(s) => confirmRemoveFromPool(props.classId, s)"
          @clear-picked="pickedStudent = null"
        />

        <LabGradingPanel
          v-else-if="stage === 3"
          key="stage3"
          :picked-student="pickedStudent"
          :question-text="selectedQuestionText"
          :student-answer="studentAnswer"
          :grading="grading !== null"
          @grade="(r, s) => grade(r, s, props.classId, props.sseConn, emit)"
          @skip="() => grade(-1, 1, props.classId, props.sseConn, emit)"
        />
      </Transition>
    </template>

    <template v-else>
      <div class="qpc-body" :class="{ fullscreen: isFullscreen }">
        <ClassroomIdleConfig
          v-if="cState === 'idle'"
          :sel="cSel"
          :subjects="cMySubjects"
          :chapters="cChapters"
          :tasks="cTasks"
          :student-pool="cStudentPool"
          :question-pool="cQuestionPool"
          :absent-count="cAbsentCount"
          @subject-change="onCSubjectChange"
          @chapter-change="onCChapterChange"
          @task-change="onCTaskChange"
          @open-import="showImportDialog = true"
          @clear-pool="clearClassroomPool"
          @start="classroomDraw"
        />

        <StudentWheel
          :spinning="wheelSpinning"
          :landed="wheelLanded"
          :display-name="wheelDisplayName"
          :student="cCurrentStudent"
          @skip="skipWheel"
        />

        <QuizSummary
          v-if="showSummary"
          :stats="cStats"
          :leaderboard="summaryLeaderboard"
          @restart="restartSession"
          @exit="exitSummary"
        />

        <ClassroomActiveView
          v-else-if="cState === 'active'"
          :current-question="cCurrentQuestion"
          :current-student="cCurrentStudent"
          :stats="cStats"
          :student-pool-length="cStudentPool.length"
          :c-score="cScore"
          :grading="cGrading"
          :last-result="cLastResult"
          :current-student-score="cCurrentStudentScore"
          :countdown-active="countdownActive"
          :countdown-seconds="countdownSeconds"
          :countdown-dash-offset="countdownDashOffset"
          :question-queue-length="cQuestionQueue.length"
          :progress-percent="progressPercent"
          @update:c-score="cScore = $event"
          @grade="classroomGrade"
          @switch-question="classroomSwitchQuestion"
          @switch-student="classroomSwitchStudent"
          @end="endClassroom"
        />
      </div>
    </template>

    <QuestionImportDialog
      v-model="showImportDialog"
      :subjects="cMySubjects"
      :chapters="uploadChapters"
      :tasks="uploadTasks"
      :form="uploadForm"
      :uploading="txtUploading"
      @subject-change="onUploadSubjectChange"
      @chapter-change="onUploadChapterChange"
      @upload="handleTxtUpload"
    />

    <AiGenerateDialog
      v-model="showAiGen"
      :subjects="aiGenSubjects"
      @generated="handleAiGenerated"
    />

    <el-dialog
      v-model="showEditDialog"
      title="编辑题目"
      width="420px"
      destroy-on-close
      @closed="editingQuestion = null"
    >
      <el-form v-if="editingQuestion" label-position="top">
        <el-form-item label="题目内容">
          <el-input
            v-model="editingQuestion.content"
            type="textarea"
            :rows="3"
            placeholder="题目内容"
          />
        </el-form-item>
        <el-form-item label="学科">
          <el-input v-model="editingQuestion.subject" placeholder="如：计算机基础" />
        </el-form-item>
        <el-form-item label="类别（可选）">
          <el-input v-model="editingQuestion.tag" placeholder="如：课堂提问" />
        </el-form-item>
        <el-form-item label="章节（可选）">
          <el-input v-model="editingQuestion.chapter" placeholder="如：第一章" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="savingEdit" @click="saveLocalQuestion">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { watch, onMounted, onBeforeUnmount, onActivated, onDeactivated, reactive, ref } from 'vue';
import { batchImportTxt } from '@/api/classroom';
import { getNodeTree } from '@/api/knowledgeNode';
import { getMySubjects } from '@/api/settings';
import { ElMessage } from 'element-plus';
import AiQuestionTab from '@/components/class/AiQuestionTab.vue';

import QuizHeader from './quiz/QuizHeader.vue';
import LabQuestionSelector from './quiz/LabQuestionSelector.vue';
import LabStudentPicker from './quiz/LabStudentPicker.vue';
import LabGradingPanel from './quiz/LabGradingPanel.vue';
import ClassroomIdleConfig from './quiz/ClassroomIdleConfig.vue';
import StudentWheel from './quiz/StudentWheel.vue';
import QuizSummary from './quiz/QuizSummary.vue';
import QuestionImportDialog from './quiz/QuestionImportDialog.vue';
import AiGenerateDialog from './quiz/AiGenerateDialog.vue';
import ClassroomActiveView from './quiz/ClassroomActiveView.vue';

import { useQuizAudio } from '@/composables/useQuizAudio';
import { useCountdown } from '@/composables/useCountdown';
import { useWheelAnimation } from '@/composables/useWheelAnimation';
import { useLabQuiz } from '@/composables/useLabQuiz';
import { useClassroomQuiz } from '@/composables/useClassroomQuiz';

const props = defineProps({ classId: [String, Number], sceneMode: String, sseConn: Object });
const emit = defineEmits(['back', 'scored']);

const { playTick, playDing, playCorrect, playWrong, playBuzz } = useQuizAudio();
const { countdownActive, countdownSeconds, countdownDashOffset, startCountdown, stopCountdown } =
  useCountdown(playBuzz);
const {
  wheelSpinning,
  wheelLanded,
  wheelDisplayName,
  startWheel,
  skipWheel: skipWheelAnim,
} = useWheelAnimation(playTick, playDing);

const {
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
} = useLabQuiz();

const {
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
  classroomDraw: _classroomDraw,
  classroomGrade: _classroomGrade,
  classroomSwitchQuestion: _classroomSwitchQuestion,
  classroomSwitchStudent: _classroomSwitchStudent,
  endClassroom: _endClassroom,
  restartSession,
  exitSummary,
  clearClassroomPool,
  onClassroomKeydown,
  onFsChange,
  onKeydown,
} = useClassroomQuiz();

const startCountdownForClassroom = () => startCountdown(playTick);
const skipWheel = () => skipWheelAnim(cCurrentStudent.value?.name);
const classroomDraw = () => _classroomDraw(props.classId, startWheel, startCountdownForClassroom);
const classroomGrade = (result) =>
  _classroomGrade(
    result,
    props.classId,
    startWheel,
    startCountdownForClassroom,
    playCorrect,
    playWrong,
    stopCountdown,
    emit
  );
const classroomSwitchQuestion = () => _classroomSwitchQuestion(stopCountdown, props.classId);
const classroomSwitchStudent = () =>
  _classroomSwitchStudent(stopCountdown, startWheel, startCountdownForClassroom);
const endClassroom = () => _endClassroom(stopCountdown);
const handleAiGenerated = (qs) => onAiGenerated(qs, props.sceneMode, cQuestionPool, aiTabRef.value);

const uploadForm = reactive({ subjectId: null, chapterId: null, taskId: null });
const uploadChapters = ref([]);
const uploadTasks = ref([]);

const onUploadSubjectChange = async (sid) => {
  uploadForm.chapterId = null;
  uploadForm.taskId = null;
  uploadChapters.value = [];
  uploadTasks.value = [];
  if (!sid) return;
  const res = await getNodeTree();
  if (res.code === 200) {
    const root = (res.data || []).find((n) => n.subjectId === sid && n.level === 1);
    uploadChapters.value = root?.children || [];
  }
};

const onUploadChapterChange = (cid) => {
  uploadForm.taskId = null;
  uploadTasks.value = [];
  if (!cid) return;
  const chapter = uploadChapters.value.find((ch) => ch.id === cid);
  uploadTasks.value = chapter?.children || [];
};

const handleTxtUpload = async (options) => {
  if (!uploadForm.taskId) {
    ElMessage.warning('请选择所属任务');
    return;
  }
  const fd = new FormData();
  fd.append('file', options.file);
  fd.append('taskId', uploadForm.taskId);
  txtUploading.value = true;
  try {
    const res = await batchImportTxt(fd);
    if (res.code === 200) {
      ElMessage.success(`成功导入 ${(res.data || []).length} 道题目`);
      showImportDialog.value = false;
      if (cSel.taskId === uploadForm.taskId) onCTaskChange(cSel.taskId);
    } else {
      ElMessage.error(res.message || '导入失败');
    }
  } catch {
    ElMessage.error('上传失败');
  } finally {
    txtUploading.value = false;
  }
};

watch(
  () => props.classId,
  (n, o) => {
    if (n && o && String(n) !== String(o)) {
      cState.value = 'idle';
      cSel.taskId = null;
      cQuestionPool.value = [];
      cQuestionQueue.value = [];
      cStudentPool.value = [];
      Object.keys(studentCallCounts).forEach((k) => delete studentCallCounts[k]);
      Object.assign(cStats, { drawn: 0, called: 0, correct: 0, total: 0 });
      loadClassroomStudentPool(props.classId);
    }
  }
);
watch(
  () => props.sceneMode,
  (mode) => {
    if (mode === 'CLASSROOM' && !cStudentPool.value.length) loadClassroomStudentPool(props.classId);
  }
);

const handleClassroomKeydown = (e) => {
  if (props.sceneMode !== 'CLASSROOM') return;
  const cmd = onClassroomKeydown(e);
  if (!cmd) return;
  switch (cmd.action) {
    case 'grade':
      classroomGrade(cmd.result);
      break;
    case 'switchStudent':
      classroomSwitchStudent();
      break;
    case 'switchQuestion':
      classroomSwitchQuestion();
      break;
  }
};

// 命名函数引用——保证 addEventListener/removeEventListener 使用同一引用
const _keydownHandler = (e) => onKeydown(e, props.sceneMode, stopCountdown);

onActivated(() => {
  document.addEventListener('fullscreenchange', onFsChange);
  document.addEventListener('keydown', _keydownHandler);
});
onDeactivated(() => {
  document.removeEventListener('fullscreenchange', onFsChange);
  document.removeEventListener('keydown', _keydownHandler);
});
onMounted(async () => {
  // fullscreenchange + _keydownHandler 由 onActivated 注册（首次挂载也会触发），此处不重复
  document.addEventListener('keydown', handleClassroomKeydown);
  try {
    const [subjRes] = await Promise.all([getMySubjects()]);
    if (subjRes.code === 200) {
      mySubjects.value = subjRes.data || [];
      cMySubjects.value = subjRes.data || [];
    }
  } catch {
    /* */
  }
  loadSysQuestions();
  loadLocalFilters();
  loadLocalQuestions();
  if (props.sceneMode === 'CLASSROOM') await loadClassroomStudentPool(props.classId);
});
onBeforeUnmount(() => {
  document.removeEventListener('fullscreenchange', onFsChange);
  document.removeEventListener('keydown', _keydownHandler);
  document.removeEventListener('keydown', handleClassroomKeydown);
  pickupAnswerListener(props.sseConn, false);
});
</script>

<style scoped lang="scss">
.quiz-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.qpc-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}
.qpc-body.fullscreen {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: var(--bg-page);
}
.slide-fade-enter-active {
  transition: all 0.3s ease-out;
}
.slide-fade-leave-active {
  transition: all 0.2s ease-in;
}
.slide-fade-enter-from {
  opacity: 0;
  transform: translateY(12px);
}
.slide-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>

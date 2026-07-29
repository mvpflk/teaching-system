import { ref, reactive, computed, toRaw } from 'vue'
import { TASK_BEHAVIOR, BEHAVIOR_DEFAULT_SCORE_TYPE } from '@/constants/taskType'

export function useTaskForm(options) {
  const { subjectOptions, gradeOptions, classOptions } = options

  const formRef = ref(null)
  const saving = ref(false)

  const form = reactive({
    title: '', taskBehavior: '', taskType: '', scoreType: 'POINT_100',
    subject: '', gradeId: null, targetIds: [], description: '',
    deadline: '', totalScore: 100, isRequired: 1, notifyParents: 0, allowResubmit: 0,
    autoWrongbook: 1,
    difficultyLevel: '',
    isAnonymous: 0, surveySchema: '',
    scorePresets: { single: 2, multi: 3, judge: 1, fill: 2, other: 5 },
  })

  const examConfig = reactive({
    durationMinutes: 120,
    shuffleQuestions: true, shuffleOptions: true, allowRetake: false,
    fullscreenLock: true, disableContextMenu: true, disableCopyPaste: false, maxWarnings: 3,
    // 达标配置（2026-07-03 新增，替代 passingScore）
    passRate: 0,           // 默认不启用
    maxAttempts: 2,        // 含首次
    retakeDeadlineHours: 48,
    passMode: 'objective', // objective(仅客观题) | all(全判定)
  })
  const homeworkConfig = reactive({ allowLateSubmit: true, latePenaltyRatio: 0.8, maxLateHours: 24 })

  const isExamBehavior = computed(() => form.taskBehavior === TASK_BEHAVIOR.EXAM)
  const isHomeworkBehavior = computed(() => form.taskBehavior === TASK_BEHAVIOR.HOMEWORK)
  const isSurveyBehavior = computed(() => form.taskBehavior === TASK_BEHAVIOR.SURVEY)
  // 兼容旧代码：类型判断
  const isExamType = isExamBehavior
  const isHomeworkType = isHomeworkBehavior
  const isSurveyType = isSurveyBehavior
  const isPracticeType = computed(() => false)

  const rules = {
    title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
    gradeId: [{ required: true, message: '请选择年级', trigger: 'change' }],
    targetIds: [{ type: 'array', required: true, message: '请选择班级', trigger: 'change' }],
  }

  const filteredClassOptions = computed(() => {
    if (!form.gradeId) return []
    const grade = gradeOptions?.value?.find(g => g.id === form.gradeId)
    if (!grade) return []
    return (classOptions?.value || []).filter(c => c.grade === grade.gradeName)
  })

  const onGradeFilter = () => { form.targetIds = [] }

  const onBehaviorChange = (behavior) => {
    form.taskBehavior = behavior
    form.scoreType = BEHAVIOR_DEFAULT_SCORE_TYPE[behavior] || 'POINT_100'
  }

  const resetForm = () => {
    form.title = ''; form.taskBehavior = ''; form.taskType = ''; form.scoreType = 'POINT_100'
    form.subject = ''; form.gradeId = null; form.targetIds = []
    form.description = ''; form.deadline = ''; form.totalScore = 100
    form.isRequired = 1; form.notifyParents = 0; form.allowResubmit = 0
    form.autoWrongbook = 1
    form.difficultyLevel = ''
    form.isAnonymous = 0; form.surveySchema = ''
    form.scorePresets = { single: 2, multi: 3, judge: 1, fill: 2, other: 5 }
    Object.assign(examConfig, {
      durationMinutes: 120,
      shuffleQuestions: true, shuffleOptions: true, allowRetake: false,
      fullscreenLock: true, disableContextMenu: true, disableCopyPaste: false, maxWarnings: 3,
      // 达标配置
      passRate: 0, maxAttempts: 2, retakeDeadlineHours: 48, passMode: 'objective',
    })
    Object.assign(homeworkConfig, { allowLateSubmit: true, latePenaltyRatio: 0.8, maxLateHours: 24 })
  }

  const getPayload = () => {
    const { taskBehavior, ...rest } = toRaw(form)
    const p = { ...rest }
    p.taskBehavior = taskBehavior
    // 默认截止时间为7天后
    if (!p.deadline || p.deadline === '') {
      const d = new Date(Date.now() + 7 * 86400000)
      const pad = n => String(n).padStart(2, '0')
      p.deadline = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
    }
    if (isExamBehavior.value) {
      // 达标字段作为独立字段发送（不混入 taskConfig JSON）
      p.passRate = examConfig.passRate || 0;
      p.maxAttempts = examConfig.maxAttempts || 1;
      p.retakeDeadlineHours = examConfig.passRate > 0 ? examConfig.retakeDeadlineHours : null;
      p.passMode = examConfig.passRate > 0 ? (examConfig.passMode || 'objective') : 'objective';
      // taskConfig 仅包含非达标配置
      const { passRate, maxAttempts, retakeDeadlineHours, passMode, ...restConfig } = examConfig;
      p.taskConfig = JSON.stringify(restConfig);
      p.scorePresets = form.scorePresets;
    } else if (isHomeworkBehavior.value) {
      p.taskConfig = JSON.stringify({ ...homeworkConfig })
    } else if (isSurveyBehavior.value) {
      p.isAnonymous = form.isAnonymous
      p.taskConfig = form.surveySchema
    }
    return p
  }

  const validate = async () => {
    if (!formRef.value) return true
    try { await formRef.value.validate(); return true }
    catch { return false }
  }

  return {
    formRef, saving, form, examConfig, homeworkConfig,
    isExamBehavior, isHomeworkBehavior, isSurveyBehavior,
    isExamType, isHomeworkType, isSurveyType, isPracticeType,
    rules, filteredClassOptions,
    onBehaviorChange, onGradeFilter, resetForm, getPayload, validate,
  }
}

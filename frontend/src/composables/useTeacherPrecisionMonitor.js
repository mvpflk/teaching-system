import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTeacherOverview, getTeacherStudents, remindAll, composeRemedial, getTeacherWeakTop, remindStudent } from '@/api/precision'
import { getMyClasses } from '@/api/classes'
import { getTeacherEncouragementPreview } from '@/api/analytics'
import { getTeacherEnglishStudents, remindEnglishStudents } from '@/api/precisionEnglish'
import { useUserStore } from '@/stores/user'

export function useTeacherPrecisionMonitor() {
  const userStore = useUserStore()
  const MATH_ENGLISH_SUBJECTS = ['数学[职高]', '数学[普高]', '数学[初中]', '英语[职高]', '英语[普高]', '英语[初中]']

  const allowedSubjects = computed(() => {
    if (userStore.isAdmin) return MATH_ENGLISH_SUBJECTS
    const mine = (userStore.teachingSubjects || []).filter(s => MATH_ENGLISH_SUBJECTS.includes(s))
    if (mine.length === 0 && userStore.isHeadTeacher) return MATH_ENGLISH_SUBJECTS
    return mine
  })

  const accessibleClassIds = computed(() => {
    if (userStore.isAdmin) return null
    const mathEng = (userStore.teacherSummary?.teachingClasses || [])
      .filter(tc => MATH_ENGLISH_SUBJECTS.includes(tc.subject))
    if (mathEng.length > 0) {
      const ids = mathEng.map(tc => tc.classId)
      if (userStore.teacherSummary?.headClassId) ids.push(userStore.teacherSummary.headClassId)
      return [...new Set(ids)]
    }
    if (userStore.isHeadTeacher && userStore.teacherSummary?.headClassId) {
      return [userStore.teacherSummary.headClassId]
    }
    return []
  })

  const isSingleClass = computed(() => {
    if (userStore.isAdmin) return false
    return accessibleClassIds.value && accessibleClassIds.value.length === 1
  })

  const PANELS_KEY = 'precision-monitor-panels'
  function loadPanelsState() {
    try {
      const saved = localStorage.getItem(PANELS_KEY)
      if (saved) return JSON.parse(saved)
    } catch { /* 忽略 */ }
    return ['layer2']
  }
  const activePanels = ref(loadPanelsState())
  watch(activePanels, (val) => {
    localStorage.setItem(PANELS_KEY, JSON.stringify(val))
  }, { deep: true })

  const loading = ref(false), filterSubject = ref(''), searchName = ref('')
  const selectedClassId = ref('')
  const classList = ref([])
  const overview = ref({ studentCount: 0, mathActive: 0, englishActive: 0, unmasteredCount: 0 })
  const students = ref([]), weakTop3 = ref([]), weakNodes = ref([])
  const filterCard = ref('all')
  const weaknessPanelRef = ref(null)

  const engData = ref({ totalStudents: 0, stageCount: 0, avgVocab: 0 }), engStudents = ref([])

  const remindEnglish = async (msg) => {
    const classId = selectedClassId.value || (classList.value.length > 0 ? classList.value[0].id : null)
    if (!classId) return
    try { await remindEnglishStudents(classId, msg || '') } catch { /* 忽略 */ }
    ElMessage.success('已推送提醒')
  }

  const studentPage = ref(1)
  const studentPageSize = ref(20)

  function onClassChange() {
    studentPage.value = 1
    loadEnglishData()
  }

  function formatActiveTime(isoStr) {
    if (!isoStr) return '从未'
    const now = Date.now()
    const t = new Date(isoStr).getTime()
    const diffMin = Math.floor((now - t) / 60000)
    if (diffMin < 1) return '刚刚'
    if (diffMin < 60) return diffMin + '分钟前'
    const diffHour = Math.floor(diffMin / 60)
    if (diffHour < 24) return diffHour + '小时前'
    const diffDay = Math.floor(diffHour / 24)
    if (diffDay < 7) return diffDay + '天前'
    return diffDay + '天前'
  }

  function activeTimeClass(isoStr) {
    if (!isoStr) return 'text-muted'
    const diffDay = (Date.now() - new Date(isoStr).getTime()) / 86400000
    if (diffDay <= 1) return 'active-now'
    if (diffDay <= 3) return 'active-recent'
    return 'active-old'
  }

  const TREND_EMOJI = { up: '\u{1F4C8}', down: '\u{1F4C9}', new: '\u{2728}', flat: '\u{2796}' }
  function trendEmoji(trend) { return TREND_EMOJI[trend] || '' }

  function calcTrend(row) {
    const w = row.weeklyPracticeCount || 0
    const prev = row.prevWeekPracticeCount || 0
    if (w > 0 && prev === 0) return 'new'
    if (w > prev) return 'up'
    if (w < prev) return 'down'
    return 'flat'
  }

  const processedStudents = computed(() => {
    return students.value.map(s => ({
      ...s,
      mathScore: s.mathEstimate || '-',
      engVocab: s.engVocab || '-',
      streakWeeks: s.streakWeeks || 0,
      lastTestScore: s.lastTestScore || 0,
      weeklyPracticeCount: s.weeklyPracticeCount || 0,
      trend: calcTrend(s)
    }))
  })

  const filteredStudents = computed(() => {
    let list = processedStudents.value
    if (accessibleClassIds.value) {
      const allowedSet = new Set(accessibleClassIds.value)
      list = list.filter(s => allowedSet.has(s.classId))
    }
    if (selectedClassId.value) list = list.filter(s => s.classId === selectedClassId.value)
    if (filterCard.value === 'active') list = list.filter(s => s.weeklyPracticeCount > 0)
    else if (filterCard.value === 'engActive') list = list.filter(s => s.engVocab > 0)
    else if (filterCard.value === 'warning') list = list.filter(s => s.warning)
    if (searchName.value) list = list.filter(s => (s.studentName || '').includes(searchName.value))
    return list
  })

  const pagedStudents = computed(() => {
    const start = (studentPage.value - 1) * studentPageSize.value
    return filteredStudents.value.slice(start, start + studentPageSize.value)
  })

  const sortedStudents = computed(() => {
    const list = [...pagedStudents.value]
    return list.sort((a, b) => {
      if (a.warning !== b.warning) return a.warning ? -1 : 1
      const aCnt = a.weeklyPracticeCount || 0, bCnt = b.weeklyPracticeCount || 0
      if (aCnt !== bCnt) return bCnt - aCnt
      if (a.lastActiveAt && b.lastActiveAt) return b.lastActiveAt.localeCompare(a.lastActiveAt)
      if (a.lastActiveAt) return -1
      if (b.lastActiveAt) return 1
      return (a.lastTestScore || 0) - (b.lastTestScore || 0)
    })
  })

  const tableRowClassName = ({ row }) => row.warning ? 'row-warning' : ''

  function toggleCard(card) {
    filterCard.value = filterCard.value === card ? 'all' : card
    studentPage.value = 1
  }

  watch(searchName, () => { studentPage.value = 1 })
  watch(selectedClassId, () => { studentPage.value = 1 })

  const classStudents = computed(() => {
    if (selectedClassId.value) return processedStudents.value.filter(s => s.classId === selectedClassId.value)
    return processedStudents.value
  })

  async function loadAll() {
    loading.value = true
    try {
      const [oRes, sRes, wRes, clsRes] = await Promise.all([
        getTeacherOverview(),
        getTeacherStudents(null, filterSubject.value || null),
        getTeacherWeakTop(filterSubject.value || null, 5),
        getMyClasses()
      ])
      if (oRes.code === 200) overview.value = { ...overview.value, ...oRes.data }
      if (sRes.code === 200) students.value = (sRes.data || [])
      if (wRes.code === 200) weakTop3.value = wRes.data || []
      else weakTop3.value = []
      if (clsRes.code === 200) {
        const allClasses = clsRes.data || []
        if (accessibleClassIds.value) {
          const allowedSet = new Set(accessibleClassIds.value)
          classList.value = allClasses.filter(c => allowedSet.has(c.id))
        } else {
          classList.value = allClasses
        }
        if (isSingleClass.value && classList.value.length === 1) {
          selectedClassId.value = classList.value[0].id
        }
      }
      await loadEnglishData()
    } catch { ElMessage.error('加载失败') }
    loading.value = false
  }

  async function loadEnglishData() {
    const classId = selectedClassId.value || (classList.value.length > 0 ? classList.value[0].id : null)
    let engLoaded = false
    if (classId) {
      try {
        const engRes = await getTeacherEnglishStudents(classId)
        if (engRes.code === 200) {
          const d = engRes.data || {}
          engStudents.value = d.students || []
          engData.value = {
            totalStudents: d.totalDiagnosed || 0,
            stageCount: d.stage1to2 || 0,
            avgVocab: Math.round((engStudents.value.reduce((s, x) => s + (x.vocabKnown || 0), 0)) / Math.max(engStudents.value.length, 1))
          }
          engLoaded = engStudents.value.length > 0
        }
      } catch { /* 忽略 */ }
    }
    if (!engLoaded && students.value.length) {
      const engList = students.value
        .filter(s => (s.engVocab || 0) > 0)
        .map(s => ({ studentId: s.studentId, studentName: s.studentName, vocabKnown: s.engVocab || 0, stage: 0, streak: s.streakWeeks || 0 }))
      if (engList.length) {
        engStudents.value = engList
        engData.value = {
          totalStudents: engList.length,
          stageCount: engList.filter(s => s.vocabKnown < 100).length,
          avgVocab: Math.round(engList.reduce((s, x) => s + x.vocabKnown, 0) / engList.length)
        }
      }
    }
  }

  async function handleComposeFromWeak() {
    try {
      await ElMessageBox.confirm(
        '将基于前5个薄弱知识点为当前班级生成补强试卷，确认？',
        '薄弱点组卷',
        { type: 'warning', confirmButtonText: '生成', cancelButtonText: '取消' }
      )
      const classId = selectedClassId.value || null
      const res = await composeRemedial(null, filterSubject.value || '数学[职高]', classId)
      if (res.code === 200) {
        ElMessage.success(res.data?.message || '已基于薄弱点生成补强卷')
      } else {
        ElMessage.warning(res.msg || '生成失败')
      }
    } catch { /* 取消 */ }
  }

  async function handleRemindAll() {
    const scope = selectedClassId.value ? '当前班级的' : '所有管辖班级的'
    const subj = filterSubject.value || '数学[职高]'
    try {
      await ElMessageBox.confirm(`确定提醒${scope}${subj}偏科学生提交本周小测？`, '确认', { type: 'info' })
      await remindAll(subj, selectedClassId.value || null)
      ElMessage.success('已发送提醒')
    } catch { /* 忽略 */ }
  }

  async function handleRemindOne(row) {
    try {
      const res = await remindStudent(row.studentId, filterSubject.value || '数学[职高]')
      if (res.code === 200 && res.data) ElMessage.success('已提醒 ' + row.studentName)
      else ElMessage.warning('提醒失败：' + (res.msg || '未知错误'))
    } catch { ElMessage.error('提醒发送失败') }
  }

  async function handleCompose(row) {
    try {
      const classId = selectedClassId.value || null
      const res = await composeRemedial(row.nodeId || null, filterSubject.value || '数学[职高]', classId)
      if (res.code === 200) ElMessage.success(res.data?.message || '已生成补强卷')
      else ElMessage.warning(res.msg || '生成失败')
    } catch { ElMessage.error('生成失败') }
  }

  const detailVisible = ref(false), detailStudentId = ref(null), detailName = ref(''), detailLoading = ref(false), detailError = ref('')
  const encouragement = ref(null), detailStudentRow = ref(null)

  async function openStudentDetail(row) {
    detailStudentId.value = row.studentId
    detailName.value = row.studentName
    detailStudentRow.value = row
    detailVisible.value = true
    detailLoading.value = true
    detailError.value = ''
    encouragement.value = null
    try {
      const res = await getTeacherEncouragementPreview(row.studentId)
      if (res.code === 200) encouragement.value = res.data
    } catch { detailError.value = '加载失败' }
    detailLoading.value = false
  }

  onMounted(() => { loadAll() })

  return {
    loading, filterSubject, searchName, selectedClassId, classList, overview,
    students, weakTop3, weakNodes, filterCard, weaknessPanelRef,
    engData, engStudents, studentPage, studentPageSize,
    activePanels, detailVisible, detailStudentId, detailName, detailLoading,
    detailError, encouragement, detailStudentRow,
    allowedSubjects, accessibleClassIds, isSingleClass,
    processedStudents, filteredStudents, pagedStudents, sortedStudents,
    classStudents, tableRowClassName,
    loadAll, loadEnglishData, onClassChange, toggleCard,
    handleComposeFromWeak, handleRemindAll, handleRemindOne, handleCompose,
    openStudentDetail, remindEnglish,
    formatActiveTime, activeTimeClass, trendEmoji, calcTrend
  }
}

import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { startDiagnosis } from '@/api/simulation'
import { getAiOutputResult } from '@/api/aiOutput'
import { cssVar } from '@/utils/echarts'

export function useDiagnosisData(taskId, taskIds) {
  const loading = ref(true)
  const ready = ref(false)
  const error = ref('')
  const data = ref({})
  const aiText = ref('')
  const aiRunning = ref(false)
  const polling = ref(false)
  const pollTimer = ref(null)
  const pollSeconds = ref(0)

  const activeTab = ref('overview')
  const questionFilter = ref('all')
  const studentFilter = ref('all')
  const kpQuestionFilter = ref(null)
  const expandedQuestions = ref(new Set())

  const classCount = computed(() => (data.value.classes || []).length)
  const studentCount = computed(() => (data.value.students || []).length)
  const sortedClasses = computed(() => [...(data.value.classes || [])].sort((a, b) => (b.avgScore || 0) - (a.avgScore || 0)))
  const scoreOverview = computed(() => data.value.scoreOverview)
  const labelCounts = computed(() => data.value.labelCounts || {})

  const scoreDistribution = computed(() => {
    const dist = scoreOverview.value?.distribution || {}
    const total = scoreOverview.value?.totalStudents || 1
    return [
      { label: '90-100', count: dist['90-100'] || 0, pct: Math.round((dist['90-100'] || 0) / total * 100), color: cssVar('--el-color-success', '#67c23a') },
      { label: '75-89', count: dist['75-89'] || 0, pct: Math.round((dist['75-89'] || 0) / total * 100), color: '#409eff' },
      { label: '60-74', count: dist['60-74'] || 0, pct: Math.round((dist['60-74'] || 0) / total * 100), color: cssVar('--el-color-warning', '#e6a23c') },
      { label: '<60', count: dist['<60'] || 0, pct: Math.round((dist['<60'] || 0) / total * 100), color: cssVar('--el-color-danger', '#f56c6c') }
    ]
  })

  const processedQuestions = computed(() =>
    (data.value.perQuestion || []).map((q, i) => {
      const clsData = q.classes || []
      const rates = clsData.map(c => c.correctRate || 0)
      const avgRate = rates.length ? Math.round(rates.reduce((s, r) => s + r, 0) / rates.length * 10) / 10 : 0
      return { ...q, _idx: i + 1, _rate: avgRate, _classes: clsData }
    }).sort((a, b) => a._rate - b._rate)
  )

  const weakQuestionCount = computed(() => processedQuestions.value.filter(q => q._rate < 70).length)
  const dangerQuestionCount = computed(() => processedQuestions.value.filter(q => q._rate < 50).length)

  const filteredQuestions = computed(() => {
    let all = processedQuestions.value
    if (kpQuestionFilter.value) all = all.filter(q => q.kpId === kpQuestionFilter.value)
    if (questionFilter.value === 'weak') return all.filter(q => q._rate < 70)
    if (questionFilter.value === 'danger') return all.filter(q => q._rate < 50)
    return all
  })

  const filteredStudents = computed(() => {
    const all = data.value.students || []
    if (studentFilter.value === 'all') return all
    return all.filter(s => s.label === studentFilter.value)
  })

  const sortedKps = computed(() => {
    const kps = (data.value.perKp || []).map(kp => {
      const cc = [...(kp.classes || [])]
      cc.sort((a, b) => (b.correctRate || 0) - (a.correctRate || 0))
      const best = cc[0]?.correctRate || 0
      cc.forEach((c, i) => { c._gap = i === 0 ? 0 : Math.round((best - c.correctRate) * 10) / 10 })
      const rates = cc.map(c => c.correctRate || 0)
      const delta = cc.length >= 2 ? Math.round((Math.max(...rates) - Math.min(...rates)) * 10) / 10 : 0
      return { ...kp, _sorted: cc, _delta: delta }
    })
    kps.sort((a, b) => b._delta - a._delta)
    return kps
  })

  const highlightedIds = computed(() => new Set((data.value.highlightedKps || []).map(k => k.kpId)))
  const isHighlighted = kpId => highlightedIds.value.has(kpId)

  const kpDataQuality = computed(() => data.value._kpDataQuality || {})
  const kpDataQualityMsg = computed(() => {
    const q = kpDataQuality.value
    if (!q || q.totalQuestions === undefined) return ''
    if (q.estimatedMapped > 0) {
      return `${q.estimatedMapped}/${q.totalQuestions} 道题目的知识点为算法推测（题目未精确标注到四级知识点），${q.l3NodesWithRoundRobin} 个三级节点触发轮询分配`
    }
    return `全部 ${q.totalQuestions} 道题目已精确标注到四级知识点 ✓`
  })
  const kpDataQualityType = computed(() => {
    const q = kpDataQuality.value
    return (q && q.estimatedMapped > 0) ? 'warning' : 'success'
  })

  const activeKpFilterName = computed(() => {
    if (!kpQuestionFilter.value) return ''
    const kp = (data.value.perKp || []).find(k => k.kpId === kpQuestionFilter.value)
    return kp?.kpName || '知识点'
  })

  function getClassName(cid) {
    return (data.value.classes || []).find(c => c.classId === cid)?.className || ('班级' + cid)
  }

  function rateColor(r) {
    return r >= 80 ? cssVar('--el-color-success', '#67c23a') : r >= 60 ? cssVar('--el-color-primary', '#409eff') : r >= 40 ? cssVar('--el-color-warning', '#e6a23c') : cssVar('--el-color-danger', '#f56c6c')
  }

  function rankBarColor(r) {
    return r >= 80 ? cssVar('--el-color-success', '#67c23a') : r >= 60 ? cssVar('--el-color-primary', '#409eff') : r >= 40 ? cssVar('--el-color-warning', '#e6a23c') : cssVar('--el-color-danger', '#f56c6c')
  }

  function questionRowClass(q) {
    return q._rate < 50 ? 'diag-q--danger' : q._rate < 70 ? 'diag-q--weak' : ''
  }

  function getOptionLetter(idx) { return String.fromCharCode(65 + idx) }

  function stripOptionPrefix(opt) {
    if (!opt) return ''
    return opt.replace(/^[A-Za-z]\s*[.、．)）:：]\s*/, '')
  }

  function isCorrectOption(opt, correctAnswer, idx) {
    if (!correctAnswer) return false
    const letter = getOptionLetter(idx)
    const ca = correctAnswer.trim().toUpperCase()
    return ca === letter || ca === stripOptionPrefix(opt).trim()
  }

  function isWrongOption(opt, topWrongAnswers, idx) {
    if (!topWrongAnswers?.length) return false
    return topWrongAnswers.includes(getOptionLetter(idx))
  }

  function toggleExpand(qId) {
    const s = new Set(expandedQuestions.value)
    if (s.has(qId)) s.delete(qId); else s.add(qId)
    expandedQuestions.value = s
  }

  function rankClass(idx) {
    if (classCount.value <= 1) return ''
    if (idx === 0) return 'diag-stat--gold'
    if (idx === classCount.value - 1) return 'diag-stat--tail'
    return ''
  }

  async function fetchAll() {
    loading.value = true
    error.value = ''
    try {
      const url = taskIds
        ? `/teacher/comparison/${taskId}/diagnosis?ids=${taskIds}`
        : `/teacher/comparison/${taskId}/diagnosis`
      const res = await request.get(url)
      if (res.code !== 200) throw new Error(res.message || '加载失败')
      data.value = res.data || {}
      if (data.value.aiAnalysis) aiText.value = data.value.aiAnalysis
      ready.value = true
    } catch (e) {
      error.value = e.message || '加载失败'
    } finally {
      loading.value = false
    }
  }

  async function triggerAiAnalysis() {
    aiRunning.value = true
    polling.value = true
    pollSeconds.value = 0
    let taskFailed = false
    try {
      const targetId = taskIds ? taskIds.split(',')[0] : taskId
      const res = await startDiagnosis(Number(targetId))
      if (res.code !== 200) { ElMessage.error(res.message || '启动失败'); aiRunning.value = false; polling.value = false; return }
      const asyncTaskId = res.data?.asyncTaskId
      ElMessage.success('AI 分析已启动')
      let done = false
      const start = Date.now()
      while (!done && (Date.now() - start) < 180_000) {
        await new Promise(r => { pollTimer.value = setTimeout(r, 4000) })
        pollSeconds.value = Math.round((Date.now() - start) / 1000)
        if (asyncTaskId) {
          try {
            const ts = await getAiOutputResult(asyncTaskId)
            if (ts.code === 200 && ts.data?.status === 'FAILED') { taskFailed = true; ElMessage.error(ts.data?.error || 'AI 分析失败，请稍后重试'); break }
          } catch (_) {}
        }
        const poll = await request.get(`/ai-output/actions/diagnose/${targetId}/result`)
        if (poll.code !== 200 || !poll.data) continue
        done = true
        aiText.value = poll.data.content || ''
      }
      if (!done && !taskFailed) ElMessage.warning('AI 分析超时（3分钟）')
    } catch (e) {
      ElMessage.error('AI 分析失败')
    } finally {
      aiRunning.value = false
      polling.value = false
      clearTimeout(pollTimer.value)
    }
  }

  function drillToKpQuestions(kpId) {
    kpQuestionFilter.value = kpId
    questionFilter.value = 'all'
    activeTab.value = 'perQuestion'
    nextTick(() => window.scrollTo({ top: 0, behavior: 'smooth' }))
  }

  function clearKpFilter() {
    kpQuestionFilter.value = null
  }

  onMounted(fetchAll)
  onBeforeUnmount(() => clearTimeout(pollTimer.value))

  return {
    loading, ready, error, data, aiText, aiRunning, polling, pollSeconds,
    activeTab, questionFilter, studentFilter, kpQuestionFilter, expandedQuestions,
    classCount, studentCount, sortedClasses, scoreOverview, labelCounts,
    scoreDistribution, processedQuestions, weakQuestionCount, dangerQuestionCount,
    filteredQuestions, filteredStudents, sortedKps, isHighlighted,
    kpDataQualityMsg, kpDataQualityType, activeKpFilterName,
    getClassName, rateColor, rankBarColor, questionRowClass,
    getOptionLetter, stripOptionPrefix, isCorrectOption, isWrongOption,
    toggleExpand, rankClass,
    fetchAll, triggerAiAnalysis, drillToKpQuestions, clearKpFilter
  }
}

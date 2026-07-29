import { reactive, ref } from 'vue'
import { getQuestionBankList, getQuestionUsageStats } from '@/api/questionBank'

export function useQuestionBankList() {
  const loading = ref(false)
  const error = ref(false)
  const list = ref([])
  const total = ref(0)
  const pageNum = ref(1)
  const pageSize = ref(20)
  const usage = ref({})
  const filters = reactive({
    questionType: '', difficultyLevel: null, tier: '',
    knowledgeDim: '', source: '', sort: 'latest', keyword: '',
  })
  const categoryId = ref(null)
  const statusTab = ref('1')
  const subjectFilter = ref('')   // 学科隔离：非管理员默认限定所授学科
  let _loadId = 0

  const loadList = async () => {
    const thisLoad = ++_loadId
    loading.value = true
    error.value = false
    try {
      const params = { page: pageNum.value, pageSize: pageSize.value }
      if (subjectFilter.value) params.subject = subjectFilter.value
      if (categoryId.value) params.categoryId = categoryId.value
      if (filters.questionType) params.questionType = filters.questionType
      if (filters.difficultyLevel) params.difficultyLevel = filters.difficultyLevel
      if (filters.tier) params.tier = filters.tier
      if (filters.knowledgeDim) params.knowledgeDim = filters.knowledgeDim
      if (filters.source) params.source = filters.source
      if (filters.sort && filters.sort !== 'latest') params.sort = filters.sort
      if (filters.keyword) params.keyword = filters.keyword
      if (statusTab.value && statusTab.value !== '-1') params.status = Number(statusTab.value)
      const res = await getQuestionBankList(params)
      if (res.code === 200) {
        list.value = res.data.records || []
        total.value = res.data.total || 0
      } else {
        error.value = true
      }
    } catch { error.value = true } finally { loading.value = false }
    const ids = list.value.map(q => q.id)
    if (ids.length && thisLoad === _loadId) {
      try {
        const res = await getQuestionUsageStats(ids)
        if (thisLoad === _loadId) usage.value = res.data || {}
      } catch { if (thisLoad === _loadId) usage.value = {} }
    }
  }

  const applyFilters = () => { pageNum.value = 1; loadList() }
  const resetFilters = () => {
    Object.assign(filters, { questionType: '', difficultyLevel: null, tier: '', knowledgeDim: '', source: '', sort: 'latest', keyword: '' })
    categoryId.value = null
    applyFilters()
  }

  return { loading, error, list, total, pageNum, pageSize, usage, filters, categoryId, subjectFilter, statusTab, loadList, applyFilters, resetFilters }
}

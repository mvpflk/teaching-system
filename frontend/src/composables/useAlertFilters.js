import { reactive, ref, computed } from 'vue'

export function useAlertFilters() {
  const filters = reactive({
    classId: null,
    alertType: null,
    handledStatus: null,
    studentName: null,
    page: 1,
    pageSize: 20
  })

  const selectedIds = ref([])

  const pagination = computed(() => ({
    page: filters.page,
    pageSize: filters.pageSize
  }))

  const hasAnyFilter = computed(() =>
    filters.classId || filters.alertType || filters.handledStatus || filters.studentName
  )

  function resetFilters() {
    filters.classId = null
    filters.alertType = null
    filters.handledStatus = null
    filters.studentName = null
    filters.page = 1
  }

  function updateFilter(key, value) {
    filters[key] = value
  }

  function clearSelection() {
    selectedIds.value = []
  }

  return {
    filters,
    selectedIds,
    pagination,
    hasAnyFilter,
    resetFilters,
    updateFilter,
    clearSelection
  }
}

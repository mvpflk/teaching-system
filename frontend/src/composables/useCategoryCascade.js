import { ref, computed } from 'vue'
import { findChildren } from '@/utils/category'

/**
 * 知识树四级级联选择器 composable
 * 统一学科→章节→任务→知识点 的联动逻辑，消除5处重复实现
 *
 * @param {Ref<Array>} categoryTree - 知识树数据（ref或computed）
 * @returns {Object} 级联状态与操作方法
 */
export function useCategoryCascade(categoryTree) {
  const selectedSubjectId = ref(null)
  const selectedChapterId = ref(null)
  const selectedTaskId = ref(null)
  const selectedKpId = ref(null)

  const chapters = ref([])
  const tasks = ref([])
  const kps = ref([])

  const categoryId = computed(() =>
    selectedKpId.value || selectedTaskId.value || selectedChapterId.value || selectedSubjectId.value || null
  )

  const onSubjectChange = (val) => {
    selectedChapterId.value = null
    selectedTaskId.value = null
    selectedKpId.value = null
    chapters.value = val ? findChildren(categoryTree.value, val) : []
    tasks.value = []
    kps.value = []
  }

  const onChapterChange = (val) => {
    selectedTaskId.value = null
    selectedKpId.value = null
    tasks.value = val ? findChildren(categoryTree.value, val) : []
    kps.value = []
  }

  const onTaskChange = (val) => {
    selectedKpId.value = null
    kps.value = val ? findChildren(categoryTree.value, val) : []
  }

  const reset = () => {
    selectedSubjectId.value = null
    selectedChapterId.value = null
    selectedTaskId.value = null
    selectedKpId.value = null
    chapters.value = []
    tasks.value = []
    kps.value = []
  }

  return {
    selectedSubjectId,
    selectedChapterId,
    selectedTaskId,
    selectedKpId,
    chapters,
    tasks,
    kps,
    categoryId,
    onSubjectChange,
    onChapterChange,
    onTaskChange,
    reset,
  }
}

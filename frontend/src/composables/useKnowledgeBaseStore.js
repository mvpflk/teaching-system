import { ref } from 'vue';
import { getChapterTree } from '@/api/knowledgeBase';

const chapterTree = ref([]);
const selectedChapter = ref('');
const selectedTask = ref('');
const todayCount = ref(0);
const selectedSubjectId = ref(null);

let loadedSubjectId = null;

export function useKnowledgeBaseStore() {
  const loadTree = async (subjectId) => {
    if (loadedSubjectId === subjectId) return;
    try {
      const res = await getChapterTree(subjectId);
      chapterTree.value = res.data || [];
      loadedSubjectId = subjectId;
    } catch {
      /* */
    }
  };

  const reset = () => {
    selectedChapter.value = '';
    selectedTask.value = '';
  };

  return {
    chapterTree,
    selectedChapter,
    selectedTask,
    todayCount,
    selectedSubjectId,
    loadTree,
    reset,
  };
}

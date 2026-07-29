import { ref, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { listAiOutputs, publishAiOutput, archiveAiOutput } from '@/api/aiOutput';

const QUESTION_HISTORY_TYPES = new Set([
  'COMPREHENSIVE_EXERCISES',
  'CLASSROOM_QUESTIONS',
  'SYNC_EXERCISES',
  'KNOWLEDGE_PRACTICE',
]);

export function useAiHistory({ genResult, genId }) {
  const historyItems = ref([]);
  const historyLoading = ref(false);
  const historyType = ref(null);
  const historyKeyword = ref('');
  const historyPage = ref(1);
  const historyTotal = ref(0);
  const PAGE_SIZE = 20;
  const historyHasMore = computed(() => historyItems.value.length < historyTotal.value);

  const loadHistory = async (append = false) => {
    historyLoading.value = true;
    try {
      const page = append ? historyPage.value : 1;
      const res = await listAiOutputs({
        contentType: historyType.value,
        keyword: historyKeyword.value,
        page,
        pageSize: PAGE_SIZE,
      });
      if (res.code === 200) {
        const data = res.data || [];
        if (append) {
          historyItems.value = [...historyItems.value, ...data];
        } else {
          historyItems.value = data;
        }
        historyPage.value = append ? page : 1;
        // 尝试从响应中读取总数（后端 Page 对象返回 total 字段）
        historyTotal.value = res.data?.total || res.data?.length || historyItems.value.length;
        // 如果后端没返回 total，用当前页是否满页推断
        if (!res.data?.total && data.length < PAGE_SIZE) {
          historyTotal.value = historyItems.value.length;
        } else if (!res.data?.total) {
          historyTotal.value = historyItems.value.length + 1; // 还有下一页
        }
      }
    } catch {
      /* */
    } finally {
      historyLoading.value = false;
    }
  };

  const loadMore = async () => {
    if (historyLoading.value || !historyHasMore.value) return;
    historyPage.value++;
    await loadHistory(true);
  };

  const onHistoryFilter = (type) => {
    historyType.value = type || null;
    historyPage.value = 1;
    historyTotal.value = 0;
    loadHistory();
  };

  const onHistorySearch = (kw) => {
    historyKeyword.value = kw || '';
    historyPage.value = 1;
    historyTotal.value = 0;
    loadHistory();
  };

  const viewHistory = (row) => {
    if (row.batchId && row.questions?.length) {
      genResult.value = {
        type: 'questions',
        batchId: row.batchId,
        title: row.title,
        questions: row.questions,
        count: row.questions.length,
      };
      genId.value = null;
      return;
    }
    const oType = row.outputType || row.contentType;
    if (oType && QUESTION_HISTORY_TYPES.has(oType) && row.content && row.content.startsWith('{')) {
      try {
        const parsed = JSON.parse(row.content);
        if (parsed.questions && parsed.questions.length) {
          genResult.value = {
            type: 'questions',
            title: row.title,
            questions: parsed.questions,
            count: parsed.count || parsed.questions.length,
          };
          genId.value = row.id;
          return;
        }
      } catch {
        /* 回退到 content 展示 */
      }
    }
    genResult.value = { type: 'content', id: row.id, title: row.title, content: row.content };
    genId.value = row.id;
  };

  const publishHistory = async (id) => {
    try {
      await publishAiOutput(id);
      ElMessage.success('已发布');
      loadHistory();
    } catch {
      ElMessage.error('发布失败，请重试');
    }
  };

  const archiveHistory = async (id) => {
    try {
      await ElMessageBox.confirm('归档后将移入回收站，可在系统设置中恢复。确定归档？', '确认', {
        type: 'warning',
        confirmButtonText: '归档',
        cancelButtonText: '取消',
      });
      await archiveAiOutput(id);
      ElMessage.success('已归档');
      loadHistory();
    } catch {
      /* 取消 */
    }
  };

  return {
    historyItems,
    historyLoading,
    historyType,
    historyKeyword,
    historyHasMore,
    loadHistory,
    loadMore,
    onHistoryFilter,
    onHistorySearch,
    viewHistory,
    publishHistory,
    archiveHistory,
  };
}

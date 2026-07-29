import { ref, onMounted } from 'vue'
import { listSimTasks } from '@/api/simulation'

/**
 * 仿真任务列表 composable
 * @param {string} category - 可选分类过滤: 'win7' | 'network' | null（全部）
 * @returns {{ tasks, loading, reload }}
 */
export function useSimTaskList(category) {
  const tasks = ref([])
  const loading = ref(false)

  async function loadTasks() {
    loading.value = true
    try {
      const res = await listSimTasks(category)
      if (res.code === 200) tasks.value = res.data || []
    } catch {
      tasks.value = []
    } finally {
      loading.value = false
    }
  }

  onMounted(loadTasks)
  return { tasks, loading, reload: loadTasks }
}

import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * 共享的 Agent 模式列表 —— 基于用户角色返回可用模式
 * 替代 AgentPage.vue 和 AgentAssistant.vue 中的重复 computed
 */
export function useAgentModes() {
  const userStore = useUserStore()

  const availableModes = computed(() => {
    if (userStore.isAdmin || userStore.isInspector)
      return [
        { value: 'LESSON_PREP', label: '备课助手' },
        { value: 'ANALYTICS', label: '学情分析' },
        { value: 'STUDY_BUDDY', label: 'AI学伴' },
      ]
    if (userStore.isTeacher)
      return [
        { value: 'LESSON_PREP', label: '备课助手' },
        { value: 'ANALYTICS', label: '学情分析' },
      ]
    return [{ value: 'STUDY_BUDDY', label: 'AI学伴' }]
  })

  /** 根据模式值获取显示标签 */
  function getModeLabel(modeValue) {
    return availableModes.value.find((m) => m.value === modeValue)?.label || 'AI助手'
  }

  return {
    availableModes,
    getModeLabel,
  }
}

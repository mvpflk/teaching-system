import { ref, reactive } from 'vue'
import { getRules, saveRule, deleteRule } from '@/api/alert'
import { ElMessage, ElMessageBox } from 'element-plus'

/** 规则提示文本——纯函数，可跨组件复用 */
export function getRuleHint(row) {
  if (row.alertType === 'LOW_SCORE') return `连续${row.minConsecutive || 3}次低于${row.scoreThreshold || 60}分触发`
  if (row.alertType === 'MISSING') return `连续${row.minConsecutive || 3}次缺交触发`
  return row.description || ''
}

export function useAlertRules() {
  const rules = ref([])
  const loading = ref(false)
  const showRuleDialog = ref(false)
  const addingRule = ref(false)
  const newRule = reactive({
    name: '',
    alertType: 'LOW_SCORE',
    minConsecutive: 3,
    scoreThreshold: 60,
    cooldownDays: 7,
    daysLookback: 90,
    taskTypes: ''
  })

  async function loadRules() {
    try {
      const res = await getRules()
      if (res.code === 200) rules.value = res.data || []
    } catch { /* */ }
  }

  async function addRule(ruleData) {
    if (!ruleData.name) { ElMessage.warning('请输入规则名称'); return }
    try {
      await saveRule({
        name: ruleData.name,
        alertType: ruleData.alertType,
        minConsecutive: ruleData.minConsecutive,
        scoreThreshold: ruleData.alertType === 'LOW_SCORE' ? ruleData.scoreThreshold : null,
        cooldownDays: ruleData.cooldownDays,
        daysLookback: ruleData.daysLookback || 90,
        taskTypes: ruleData.taskTypes || null,
        isEnabled: 1,
        isBuiltin: 0
      })
      ElMessage.success('规则已添加')
      addingRule.value = false
      Object.assign(newRule, {
        name: '', alertType: 'LOW_SCORE', minConsecutive: 3,
        scoreThreshold: 60, cooldownDays: 7, daysLookback: 90, taskTypes: ''
      })
      await loadRules()
    } catch { ElMessage.error('添加失败') }
  }

  async function saveAllRules(rulesList) {
    try {
      for (const r of rulesList) {
        await saveRule({
          id: r.id, minConsecutive: r.minConsecutive,
          scoreThreshold: r.scoreThreshold, cooldownDays: r.cooldownDays,
          isEnabled: r.isEnabled, daysLookback: r.daysLookback,
          taskTypes: r.taskTypes
        })
      }
      ElMessage.success('规则已保存')
      showRuleDialog.value = false
    } catch { ElMessage.error('保存失败') }
  }

  async function removeRule(rule) {
    try {
      await ElMessageBox.confirm(`确定删除规则"${rule.name}"？`, '提示', { type: 'warning' })
      await deleteRule(rule.id)
      ElMessage.success('已删除')
      await loadRules()
    } catch { /* cancelled */ }
  }

  return {
    rules,
    loading,
    showRuleDialog,
    addingRule,
    newRule,
    loadRules,
    addRule,
    saveAllRules,
    removeRule,
    getRuleHint
  }
}

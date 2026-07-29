<template>
  <div>
    <p class="text-muted">配置各项行为获得的积分值</p>
    <el-table
      v-loading="loadingRules"
      :data="rules"
      stripe
      size="small"
    >
      <el-table-column prop="ruleName" label="规则名称" width="150" />
      <el-table-column label="行为" width="100">
        <template #default="{ row }">{{ {'sign':'签到','homework':'作业','exam':'考试','other':'其他'}[row.actionType] || row.actionType }}</template>
      </el-table-column>
      <el-table-column prop="creditValue" label="积分值" width="100">
        <template #default="{ row }">
          <el-input-number
            v-model="row.creditValue"
            :min="0"
            :max="999"
            size="small"
            style="width:120px"
          />
        </template>
      </el-table-column>
      <el-table-column prop="maxDailyCount" label="日限" width="80" />
      <el-table-column prop="description" label="说明" min-width="200" />
      <el-table-column label="状态" width="70">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80">
        <template #default="{ row }">
          <el-button
            size="small"
            type="primary"
            :loading="savingRule"
            @click="saveRule(row)"
          >
            保存
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminRules, updateAdminRule } from '@/api/credit'

const emit = defineEmits(['changed'])

const rules = ref([])
const loadingRules = ref(false)
const savingRule = ref(false)

const loadRules = async () => {
  loadingRules.value = true
  try {
    const res = await getAdminRules()
    if (res.code === 200) rules.value = res.data
  } finally { loadingRules.value = false }
}

const saveRule = async (rule) => {
  if (savingRule.value) return
  savingRule.value = true
  try {
    const res = await updateAdminRule(rule.id, rule)
    if (res.code === 200) { ElMessage.success('规则已更新'); emit('changed') }
  } finally { savingRule.value = false }
}

onMounted(() => { loadRules() })
</script>

<style scoped>
.text-muted { color: var(--text-secondary); font-size: var(--fs-sm); margin-bottom: 12px; }

@media (max-width: 768px) {
  :deep(.el-table) { font-size: var(--fs-xs); }
}
</style>

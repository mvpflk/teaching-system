<template>
  <el-dialog :model-value="visible" title="预警规则设置" width="700px" destroy-on-close @close="emit('close')">
    <div class="am-rules-intro">
      <el-alert type="info" :closable="false" show-icon>
        <template #title>规则说明</template>
        <template #default>
          预警系统会自动扫描学生最近的提交记录，当满足规则条件时生成预警通知。
          <b>低分预警</b>：连续N次成绩低于阈值。 <b>缺交预警</b>：连续N次任务未提交。
          冷却期内同一学生同一规则不会重复触发。
        </template>
      </el-alert>
    </div>
    <div style="margin-bottom:12px;display:flex;justify-content:space-between;align-items:center">
      <span style="font-size:var(--fs-sm);font-weight:600">已有规则</span>
      <el-button size="small" type="primary" @click="$emit('add-rule')" v-if="!addingRule">
        <el-icon><Plus /></el-icon> 新增规则
      </el-button>
    </div>
    <el-card v-if="addingRule" shadow="never" style="margin-bottom:12px;background:var(--bg-section)">
      <el-form :model="newRule" label-width="80px" size="small" inline>
        <el-form-item label="规则名称">
          <el-input v-model="newRule.name" placeholder="如：连续3次低于50分" style="width:180px" />
        </el-form-item>
        <el-form-item label="预警类型">
          <el-select v-model="newRule.alertType" style="width:130px">
            <el-option label="低分预警" value="LOW_SCORE" />
            <el-option label="缺交预警" value="MISSING" />
            <el-option label="成绩骤降" value="SCORE_DROP" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="newRule.alertType === 'LOW_SCORE'" label="分数阈值">
          <el-input-number v-model="newRule.scoreThreshold" :min="0" :max="100" style="width:80px" />
        </el-form-item>
        <el-form-item label="连续次数">
          <el-input-number v-model="newRule.minConsecutive" :min="2" :max="10" style="width:80px" />
        </el-form-item>
        <el-form-item label="冷却(天)">
          <el-input-number v-model="newRule.cooldownDays" :min="1" :max="30" style="width:80px" />
        </el-form-item>
        <el-form-item>
          <el-button size="small" type="primary" @click="$emit('confirm-add')">确认添加</el-button>
          <el-button size="small" @click="$emit('cancel-add')">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <el-table :data="rules" size="small" class="am-rules-table">
      <el-table-column prop="name" label="规则名称" width="120" />
      <el-table-column label="说明" min-width="180">
        <template #default="{ row }">
          <span class="am-rule-desc">{{ row.description || getRuleHint(row) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="连续次数" width="100">
        <template #default="{ row }">
          <el-input-number v-model="row.minConsecutive" :min="2" :max="10" size="small" controls-position="right" style="width:72px" />
        </template>
      </el-table-column>
      <el-table-column label="冷却(天)" width="90">
        <template #default="{ row }">
          <el-input-number v-model="row.cooldownDays" :min="1" :max="30" size="small" controls-position="right" style="width:72px" />
        </template>
      </el-table-column>
      <el-table-column label="启用" width="60" align="center">
        <template #default="{ row }">
          <el-switch v-model="row.isEnabled" :active-value="1" :inactive-value="0" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="60" align="center">
        <template #default="{ row }">
          <el-button v-if="row.isBuiltin !== 1" size="small" type="danger" link @click="$emit('delete-rule', row)">删除</el-button>
          <el-tag v-else size="small" type="info">内置</el-tag>
        </template>
      </el-table-column>
    </el-table>
    <template #footer>
      <el-button @click="emit('close')">关闭</el-button>
      <el-button type="primary" @click="emit('save-rules')">保存规则</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { Plus } from '@element-plus/icons-vue'
import { getRuleHint } from '@/composables/useAlertRules'

defineProps({
  visible: { type: Boolean, default: false },
  rules: { type: Array, default: () => [] },
  addingRule: { type: Boolean, default: false },
  newRule: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['close', 'add-rule', 'delete-rule', 'save-rules', 'cancel-add', 'confirm-add'])
</script>

<style scoped>
.am-rules-intro { margin-bottom: 16px; }
.am-rule-desc { font-size: var(--fs-xs); color: var(--text-secondary); }
</style>

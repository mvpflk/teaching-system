<template>
  <el-dialog :model-value="visible" title="AI审核结果" width="760px" :close-on-click-modal="false" @update:model-value="$emit('update:visible', $event)">
    <div v-if="reviewing" style="text-align:center;padding:40px">
      <el-icon class="is-loading" :size="36"><Loading /></el-icon>
      <p style="margin-top:16px;color:var(--text-secondary)">AI正在逐题审核中，请稍候...</p>
    </div>
    <template v-else-if="result">
      <div class="review-summary">
        <div class="review-stat"><span class="review-stat__label">总计</span><span class="review-stat__val">{{ result.total }}</span></div>
        <div class="review-stat review-stat--pass"><span class="review-stat__label">通过</span><span class="review-stat__val">{{ result.approved }}</span></div>
        <div class="review-stat review-stat--fix"><span class="review-stat__label">需修改</span><span class="review-stat__val">{{ result.needsFix }}</span></div>
        <div class="review-stat review-stat--reject"><span class="review-stat__label">建议驳回</span><span class="review-stat__val">{{ result.rejected }}</span></div>
        <div v-if="result.autoApproved > 0" class="review-stat review-stat--auto"><span class="review-stat__label">已自动入库</span><span class="review-stat__val">{{ result.autoApproved }}</span></div>
      </div>
      <el-table :data="result.results" size="small" max-height="400" stripe>
        <el-table-column label="#" width="40">
          <template #default="{ $index }">{{ $index + 1 }}</template>
        </el-table-column>
        <el-table-column label="题目" min-width="200">
          <template #default="{ row }">
            <span class="review-qtext">{{ row.questionText }}</span>
            <el-tag size="small" style="margin-left:4px">{{ typeLabels[row.questionType] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核结论" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.verdict === 'APPROVED'" type="success" size="small">通过</el-tag>
            <el-tag v-else-if="row.verdict === 'REJECTED'" type="danger" size="small">驳回</el-tag>
            <el-tag v-else type="warning" size="small">需修改</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="理由" min-width="180">
          <template #default="{ row }"><span class="review-reason">{{ row.reason }}</span></template>
        </el-table-column>
      </el-table>
    </template>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">关闭</el-button>
      <el-button v-if="result && result.approved > 0 && !result.autoApproved" type="success" @click="$emit('approveAll')">将通过的全部入库</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
defineProps({ visible: Boolean, reviewing: Boolean, result: Object, typeLabels: Object })
defineEmits(['update:visible', 'approveAll'])
</script>

<style scoped>
.review-summary { display: flex; gap: 16px; margin-bottom: 16px; flex-wrap: wrap; }
.review-stat { display: flex; flex-direction: column; align-items: center; padding: 8px 16px; border-radius: var(--radius-md); background: var(--bg-secondary); min-width: 72px; }
.review-stat__label { font-size: var(--fs-xs); color: var(--text-secondary); }
.review-stat__val { font-size: var(--fs-2xl); font-weight: 700; color: var(--text-primary); }
.review-stat--pass .review-stat__val { color: var(--el-color-success); }
.review-stat--fix .review-stat__val { color: var(--el-color-warning); }
.review-stat--reject .review-stat__val { color: var(--el-color-danger); }
.review-stat--auto .review-stat__val { color: var(--primary-color); }
.review-qtext { font-size: var(--fs-xs); line-height: 1.4; }
.review-reason { font-size: var(--fs-xs); color: var(--text-secondary); line-height: 1.4; }
</style>

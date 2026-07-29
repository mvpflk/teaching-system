<template>
  <el-dialog
    v-model="visible"
    title="🎉 兑换成功"
    width="380px"
    class="redeem-dialog"
    append-to-body
  >
    <div class="result-body">
      <div class="result-icon">
        <el-icon size="56" :color="'var(--el-color-success)'"><SuccessFilled /></el-icon>
      </div>
      <h3>{{ redeemResult?.itemName }}</h3>
      <div class="result-details">
        <div class="result-row">
          <span>消耗积分</span>
          <span class="text-danger">-{{ redeemResult?.creditPrice }}</span>
        </div>
        <div class="result-row">
          <span>剩余积分</span>
          <span class="text-warning">{{ redeemResult?.remainingCredits }}</span>
        </div>
      </div>
      <p class="result-msg">{{ redeemResult?.message }}</p>
    </div>
    <template #footer>
      <el-button type="primary" style="width:100%" @click="visible = false">知道了</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  redeemResult: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})
</script>

<style scoped lang="scss">
.redeem-dialog {
  .result-body {
    text-align: center;
    padding: 16px 0;

    .result-icon { margin-bottom: 16px; }
    h3 { font-size: var(--fs-lg); font-weight: 500; margin: 0 0 20px; }

    .result-details {
      background: var(--bg-secondary);
      border-radius: var(--radius-md);
      padding: 12px 16px;
      margin-bottom: 12px;

      .result-row {
        display: flex;
        justify-content: space-between;
        padding: 6px 0;
        font-size: var(--fs-md);
        color: var(--text-regular);
        + .result-row { border-top: 1px solid var(--border-light); }
      }
    }
    .result-msg { font-size: var(--fs-sm); color: var(--text-secondary); }
  }
}

@media (max-width: 768px) {
  :deep(.el-form-item .el-input),
  :deep(.el-form-item .el-select) {
    width: 100%;
  }
}
</style>

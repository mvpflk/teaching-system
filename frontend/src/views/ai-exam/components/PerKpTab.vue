<template>
  <div>
    <el-alert
      v-if="kpDataQualityMsg"
      :title="kpDataQualityMsg"
      :type="kpDataQualityType"
      :closable="false"
      show-icon
      style="margin-bottom:16px"
    />
    <el-alert
      v-if="highlightedKps?.length"
      :title="`${highlightedKps.length} 个知识点班级间差异超过 20%`"
      type="warning"
      :closable="false"
      show-icon
      style="margin-bottom:16px"
    />

    <KpComparisonCard
      v-for="kp in kpList"
      :key="kp.kpId"
      :kp="kp"
      :highlighted="isHighlighted(kp.kpId, highlightedKps)"
      @drill="$emit('drill-to-kp', $event)"
      @generate-material="$emit('generate-material', $event)"
    />
  </div>
</template>

<script setup>
import KpComparisonCard from './KpComparisonCard.vue'

defineProps({
  kpList: { type: Array, default: () => [] },
  highlightedKps: { type: Array, default: () => [] },
  kpDataQualityMsg: { type: String, default: '' },
  kpDataQualityType: { type: String, default: 'success' }
})
defineEmits(['drill-to-kp', 'generate-material'])

function isHighlighted(kpId, highlightedKps) {
  return highlightedKps.some(k => k.kpId === kpId)
}
</script>

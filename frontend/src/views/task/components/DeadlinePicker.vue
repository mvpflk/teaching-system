<template>
  <div class="deadline-wrap">
    <el-radio-group v-model="activeDl" size="small" @change="onQuickDeadlineChange">
      <el-radio-button v-for="opt in quickDeadlines" :key="opt.label" :value="opt.label">{{ opt.label }}</el-radio-button>
    </el-radio-group>
    <el-popover :visible="customDlVisible" placement="bottom" :width="280" trigger="click">
      <template #reference>
        <el-button size="small" :type="customDlActive ? 'primary' : ''" @click.stop="customDlVisible = true">自定义</el-button>
      </template>
      <el-date-picker v-model="customDeadline" type="datetime" placeholder="自定义截止时间" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" @change="onCustomDeadline" />
    </el-popover>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  quickDeadlines: { type: Array, default: () => [] },
})
const emit = defineEmits(['update:modelValue'])

const activeDl = ref('')
const customDlVisible = ref(false)
const customDeadline = ref('')
const customDlActive = computed(() => !activeDl.value && !!props.modelValue)

const onQuickDeadlineChange = (label) => {
  const opt = props.quickDeadlines.find(o => o.label === label)
  if (!opt) return
  const d = new Date(Date.now() + opt.minutes * 60000)
  const pad = n => String(n).padStart(2, '0')
  const val = `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  customDeadline.value = ''
  customDlVisible.value = false
  emit('update:modelValue', val)
}

const onCustomDeadline = (val) => {
  if (val) {
    activeDl.value = ''
    customDlVisible.value = false
    emit('update:modelValue', val)
  }
}
</script>

<style scoped>
.deadline-wrap { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; }
.deadline-wrap :deep(.el-radio-group) { flex-shrink: 0; }
.deadline-wrap :deep(.el-radio-button__inner) { font-size: var(--fs-xs); padding: 4px 12px; }
</style>

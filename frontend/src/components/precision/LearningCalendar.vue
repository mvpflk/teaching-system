<template>
  <div class="cal">
    <div class="cal-header"><el-button text size="small" @click="prevMonth">←</el-button><span>{{ year }}年{{ month }}月</span><el-button text size="small" @click="nextMonth">→</el-button></div>
    <div class="cal-grid">
      <span
        v-for="d in days"
        :key="d"
        class="cal-day"
        :class="{ active: d > 0 && practiced.includes(d.toString()) }"
      >{{ d > 0 ? d : '' }}</span>
    </div>
  </div>
</template>
<script setup>
import { ref, computed } from 'vue'
const props = defineProps({ practiced: { type: Array, default: () => [] } })
const now = new Date()
const year = ref(now.getFullYear()), month = ref(now.getMonth() + 1)
const days = computed(() => {
  const first = new Date(year.value, month.value - 1, 1), last = new Date(year.value, month.value, 0)
  const startDow = (first.getDay() + 6) % 7; const arr = []; for (let i = 0; i < startDow; i++) arr.push(0)
  for (let i = 1; i <= last.getDate(); i++) arr.push(i); return arr
})
const prevMonth = () => { if (month.value === 1) { year.value--; month.value = 12 } else month.value-- }
const nextMonth = () => { if (month.value === 12) { year.value++; month.value = 1 } else month.value++ }
</script>
<style scoped>
.cal { padding: 12px; background: var(--bg-card, #fff); border: 1px solid var(--border-base, #e8e8ed); border-radius: 8px; margin-bottom: 14px; }
.cal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; font-size: var(--fs-sm); font-weight: 600; }
.cal-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 4px; text-align: center; font-size: var(--fs-xs); }
.cal-day { padding: 6px 0; border-radius: 4px; }
.cal-day.active { background: var(--primary-light); color: var(--primary-color); font-weight: 600; }
</style>

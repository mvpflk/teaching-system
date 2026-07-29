<template>
  <div v-if="spinning" class="qpc-wheel-overlay" @click.self="$emit('skip')">
    <div class="qpc-wheel">
      <div class="qpc-wheel-title">🎯 抽取学生中...</div>
      <div class="qpc-wheel-name" :class="{ landed }">
        {{ displayName }}
      </div>
      <div v-if="landed" class="qpc-wheel-result">
        <el-avatar v-if="student?.avatarUrl" :size="48" :src="student.avatarUrl" />
        <span v-else class="qpc-wheel-avatar">{{ (student?.name || '?').charAt(0) }}</span>
        <span class="qpc-wheel-label">被抽中！</span>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  spinning: Boolean,
  landed: Boolean,
  displayName: String,
  student: Object
})
defineEmits(['skip'])
</script>

<style scoped>
.qpc-wheel-overlay { position: fixed; inset: 0; z-index: 1000; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,0.85); animation: fadeIn 0.3s ease; }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
.qpc-wheel { text-align: center; padding: 60px; }
.qpc-wheel-title { font-size: var(--fs-2xl); color: rgba(255,255,255,0.7); margin-bottom: 40px; font-weight: 500; }
.qpc-wheel-name { font-size: 72px; font-weight: 900; color: #fff; text-shadow: 0 0 40px rgba(67,97,238,0.5); min-height: 100px; display: flex; align-items: center; justify-content: center; transition: transform 0.1s ease; }
.qpc-wheel-name.landed { animation: wheelLand 0.5s ease; color: var(--el-color-warning); text-shadow: 0 0 60px rgba(245,158,11,0.6); }
@keyframes wheelLand { 0% { transform: scale(1.3); } 50% { transform: scale(0.9); } 100% { transform: scale(1); } }
.qpc-wheel-result { margin-top: 30px; display: flex; flex-direction: column; align-items: center; gap: 12px; animation: fadeIn 0.4s ease; }
.qpc-wheel-avatar { width: 48px; height: 48px; border-radius: 50%; background: linear-gradient(135deg, var(--el-color-warning), #ff9800); display: flex; align-items: center; justify-content: center; font-size: 22px; font-weight: 800; color: #fff; }
.qpc-wheel-label { font-size: var(--fs-xl); color: var(--el-color-success); font-weight: 700; }
</style>

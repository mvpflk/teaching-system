<template>
  <div class="ai-float-button" :style="{ bottom: bottom + 'px' }">
    <el-badge :value="badge" :hidden="!badge">
      <el-button
        :type="active ? 'primary' : 'default'"
        circle
        size="large"
        class="float-btn"
        @click="$emit('click')"
      >
        <el-icon :size="22"><ChatDotRound /></el-icon>
      </el-button>
    </el-badge>
  </div>
</template>

<script setup>
defineProps({
  badge: { type: Number, default: 0 },
  active: { type: Boolean, default: false },
  bottom: { type: Number, default: 140 }
})

defineEmits(['click'])
</script>

<style scoped>
.ai-float-button {
  position: fixed;
  right: 24px;
  z-index: 1000;
}

.float-btn {
  width: 48px;
  height: 48px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transition: transform 0.2s;
}

.float-btn:hover {
  transform: scale(1.1);
}
</style>

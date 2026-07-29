<template>
  <div class="cp-path">
    <div v-for="(node, i) in nodes" :key="node.configId" class="cp-path-segment">
      <div
        v-if="i > 0"
        class="cp-path-line"
        :class="{ 'cp-path-line-done': node.status === 'done' || node.status === 'current' }"
      ></div>
      <div
        class="cp-path-node"
        :class="[`cp-node-${node.status}`, { 'cp-node-boss': node.checkpointType === 'BOSS' }]"
        @click="handleNodeClick(node)"
      >
        <span v-if="node.checkpointType === 'BOSS'" class="cp-node-icon">👑</span>
        <span v-else-if="node.status === 'done'" class="cp-node-icon">✓</span>
        <span v-else-if="node.status === 'locked'" class="cp-node-icon">🔒</span>
        <span v-else class="cp-node-num">{{ node.seq }}</span>
      </div>
      <div class="cp-node-label" :class="{ 'cp-label-active': node.status === 'current' }">
        {{ node.taskName }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  checkpoints: { type: Array, default: () => [] },
});

const emit = defineEmits(['select']);

const nodes = computed(() => {
  return props.checkpoints.map((cp) => ({
    ...cp,
    status: cp.passed ? 'done' : cp.isLocked ? 'locked' : 'current',
  }));
});

function handleNodeClick(node) {
  emit('select', node);
}
</script>

<style scoped>
.cp-path {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 0;
}
.cp-path-segment {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.cp-path-line {
  width: 2px;
  height: 24px;
  background: #e4e4e7;
  transition: background 0.3s;
}
.cp-path-line-done {
  background: #22c55e;
}
.cp-path-node {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
  z-index: 1;
}
.cp-node-done {
  background: #22c55e;
  color: white;
}
.cp-node-current {
  background: var(--primary-color);
  color: white;
  animation: pulse-glow 2s infinite;
}
.cp-node-locked {
  background: #f4f4f5;
  color: #a1a1aa;
  border: 2px dashed #d4d4d8;
  cursor: not-allowed;
}
.cp-node-boss {
  width: 52px;
  height: 52px;
  color: white;
}
.cp-node-boss.cp-node-current {
  background: linear-gradient(135deg, var(--primary-color), #7c3aed);
}
.cp-node-icon {
  font-size: 16px;
}
.cp-node-num {
  font-size: 14px;
}
.cp-node-label {
  font-size: 12px;
  color: #71717a;
  margin-top: 8px;
  text-align: center;
  max-width: 100px;
}
.cp-label-active {
  color: var(--primary-color);
  font-weight: 600;
}
@keyframes pulse-glow {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(67, 97, 238, 0.4);
  }
  50% {
    box-shadow: 0 0 0 12px rgba(67, 97, 238, 0);
  }
}
</style>

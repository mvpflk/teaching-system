<template>
  <div class="cat-card" @click="$emit('click')">
    <div class="cat-icon">
      <el-icon :size="36"><component :is="iconComponent" /></el-icon>
    </div>
    <h4 class="cat-name">{{ title }}</h4>
    <p class="cat-desc">{{ description }}</p>
    <el-tag size="small" type="info">{{ taskCount || 0 }} 个任务</el-tag>
    <el-button type="primary" size="small" style="margin-top:12px" @click.stop="$emit('click')">进入</el-button>
  </div>
</template>

<script setup>
import { computed, markRaw } from 'vue'
import { Document, Monitor, EditPen, Grid } from '@element-plus/icons-vue'

const props = defineProps({
  title: String,
  description: String,
  icon: String,
  taskCount: Number
})
defineEmits(['click'])

const ICONS = { Document: markRaw(Document), Monitor: markRaw(Monitor), EditPen: markRaw(EditPen), Grid: markRaw(Grid) }
const iconComponent = computed(() => ICONS[props.icon] || Grid)
</script>

<style scoped>
.cat-card { cursor: pointer; padding: 20px 16px; background: var(--bg-card); border: 0.5px solid var(--border-light);
  border-radius: var(--radius-md); text-align: center; transition: border-color 0.2s, transform 0.15s; }
.cat-card:hover { border-color: var(--primary-color); transform: translateY(-2px); }
.cat-icon { width: 60px; height: 60px; border-radius: 14px; background: var(--primary-light, var(--primary-light));
  display: flex; align-items: center; justify-content: center; color: var(--primary-color); margin: 0 auto 8px; }
.cat-name { font-size: 16px; color: var(--text-primary); margin: 0 0 4px; font-weight: 600; }
.cat-desc { font-size: var(--fs-sm); color: var(--text-secondary); margin: 0 0 8px; }
</style>

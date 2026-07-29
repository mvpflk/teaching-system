<template>
  <div class="sim-task-list">
    <div class="sim-task-list__header">
      <h3 class="sim-task-list__title">网络应用基础实训</h3>
      <p class="sim-task-list__desc">选择一个网络命令任务开始练习</p>
    </div>
    <div v-loading="loading" class="sim-task-list__grid">
      <div
        v-for="task in tasks"
        :key="task.id"
        class="sim-task-card"
        @click="startTask(task)"
      >
        <div class="sim-task-card__header">
          <el-tag :type="task.mode === 'exam' ? 'danger' : 'primary'" size="small" effect="plain">{{ task.mode === 'exam' ? '考试' : '练习' }}</el-tag>
          <span class="sim-task-card__difficulty">{{ '⭐'.repeat(task.difficulty || 1) }}</span>
        </div>
        <h4 class="sim-task-card__title">{{ task.title }}</h4>
        <p class="sim-task-card__desc">{{ task.description || '网络命令操作实训' }}</p>
        <div class="sim-task-card__footer">
          <span>{{ task.timeLimit }}秒</span>
          <el-button type="primary" size="small" @click.stop="startTask(task)">开始</el-button>
        </div>
      </div>
    </div>
    <el-empty v-if="!loading && !tasks.length" description="暂无网络实训任务" />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useSimTaskList } from '@/composables/useSimTaskList'

const router = useRouter()
const { tasks, loading } = useSimTaskList('network')

function startTask(task) {
  router.push(`/student/training/network/practice/${task.id}`).catch(() => {})
}
</script>

<style scoped>
.sim-task-list { max-width: 1100px; margin: 0 auto; padding: 24px 16px; }
.sim-task-list__header { margin-bottom: 20px; }
.sim-task-list__title { font-size: var(--fs-xl); color: var(--text-primary); margin: 0 0 6px; }
.sim-task-list__desc { color: var(--text-secondary); font-size: var(--fs-sm); margin: 0; }

.sim-task-list__grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.sim-task-card {
  cursor: pointer;
  padding: 16px;
  background: var(--bg-card);
  border: 0.5px solid var(--border-light);
  border-radius: var(--radius-md);
  transition: border-color 0.2s;
}
.sim-task-card:hover { border-color: var(--primary-color); }

.sim-task-card__header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.sim-task-card__difficulty { font-size: var(--fs-xs); color: var(--text-secondary); }
.sim-task-card__title { font-size: var(--fs-md); color: var(--text-primary); margin: 0 0 6px; }
.sim-task-card__desc { font-size: var(--fs-xs); color: var(--text-secondary); margin: 0 0 10px; line-height: 1.4; }
.sim-task-card__footer { display: flex; justify-content: space-between; align-items: center; font-size: var(--fs-xs); color: var(--text-secondary); }

@media (max-width: 768px) {
  .sim-task-list__grid { grid-template-columns: 1fr; }
  .sim-task-list { padding: 12px 8px; }
}
</style>

<template>
  <el-dialog
    v-model="visible"
    title="📋 课堂任务"
    :width="isMobile ? '95%' : '500px'"
    destroy-on-close
  >
    <div v-if="classTasks.length === 0" style="text-align:center;padding:30px;color:var(--text-secondary)">
      当前没有进行中的课堂任务
    </div>
    <div v-for="t in classTasks" :key="t.id" style="padding:14px;border:0.5px solid var(--border-color);border-radius:var(--radius-md);margin-bottom:10px">
      <div style="font-weight:600;margin-bottom:6px">{{ t.title }}</div>
      <div style="font-size:var(--fs-xs);color:var(--text-secondary);margin-bottom:8px">
        截止: {{ t.deadline || '未设置' }} | 满分: {{ t.totalScore }}
      </div>
      <div v-if="t._progress" style="margin-bottom:8px">
        <el-progress :percentage="t._progress.percent" :format="() => t._progress.submitted + '/' + t._progress.total" />
      </div>
      <el-button type="primary" size="small" @click="startClassroomTask(t)">启动任务</el-button>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, onBeforeUnmount } from 'vue'
import { getActiveClassTasks, startClassTask, getTaskProgress } from '@/api/classroom'
import { ElMessage } from 'element-plus'

const props = defineProps({ classId: [String, Number], isMobile: Boolean })
const emit = defineEmits(['taskStarted'])

const visible = ref(false)
const classTasks = ref([])
let taskProgressTimer = null

const open = () => {
  visible.value = true
  loadClassTasks()
}
defineExpose({ open })

const loadClassTasks = async () => {
  if (!props.classId) return
  try {
    const res = await getActiveClassTasks(props.classId)
    if (res.code === 200) {
      classTasks.value = res.data || []
      for (const t of classTasks.value) await loadTaskProgress(t)
    }
  } catch { /* */ }
}

const loadTaskProgress = async (task) => {
  try {
    const res = await getTaskProgress(props.classId, task.id)
    if (res.code === 200) {
      task._progress = {
        total: res.data.totalStudents,
        submitted: res.data.submittedCount,
        percent: res.data.totalStudents > 0 ? Math.round(res.data.submittedCount / res.data.totalStudents * 100) : 0
      }
    }
  } catch { /* */ }
}

const startClassroomTask = async (task) => {
  try {
    await startClassTask(props.classId, task.id)
    ElMessage.success('任务已启动')
    emit('taskStarted', task)
    // 先清除旧 timer 再启动新的
    if (taskProgressTimer) clearInterval(taskProgressTimer)
    taskProgressTimer = setInterval(() => loadTaskProgress(task), 5000)
  } catch { ElMessage.error('启动失败') }
}

onBeforeUnmount(() => {
  if (taskProgressTimer) clearInterval(taskProgressTimer)
})
</script>

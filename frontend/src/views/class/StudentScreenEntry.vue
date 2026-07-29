<template>
  <div v-loading="true" class="student-entry-loading" element-loading-text="正在连接课堂...">
    <div v-if="error" class="student-entry-error">
      <el-icon style="font-size:48px;color:var(--text-disabled)"><Monitor /></el-icon>
      <p>当前没有活跃的课堂互动</p>
      <p class="student-entry-hint">请等待教师发起课堂互动后刷新页面</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const error = ref(false)

onMounted(async () => {
  try {
    const info = await request({ url: '/student/actions/my-class', method: 'get' })
    if (info.code === 200 && info.data?.classId) {
      router.replace(`/class/${info.data.classId}/smart-screen/student`)
    } else {
      error.value = true
    }
  } catch { error.value = true }
})
</script>

<style scoped>
.student-entry-loading { text-align: center; padding: 80px 24px; }
.student-entry-error { color: var(--text-secondary); }
.student-entry-error p { margin: 8px 0; }
.student-entry-hint { font-size: var(--fs-sm); color: var(--text-disabled); }
</style>

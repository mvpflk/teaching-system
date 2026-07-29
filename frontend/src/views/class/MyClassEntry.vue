<template>
  <div v-loading="true" class="my-class-loading" element-loading-text="加载班级信息..." />
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()

onMounted(async () => {
  try {
    const info = await request({ url: '/student/actions/my-class', method: 'get' })
    if (info.code === 200 && info.data?.classId) {
      router.replace(`/class/${info.data.classId}/home`)
    } else {
      router.replace('/home')
    }
  } catch { router.replace('/home') }
})
</script>

<style scoped>
@media (max-width: 768px) {
  :deep(.el-form-item .el-input),
  :deep(.el-form-item .el-select) {
    width: 100%;
  }
}
</style>

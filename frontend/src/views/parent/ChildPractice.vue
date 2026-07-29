<template>
  <div class="cp-page">
    <div class="cp-header">
      <el-button text @click="$router.back()"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
      <h3>实训记录</h3>
    </div>
    <el-table
      v-loading="loading"
      :data="list"
      empty-text="暂无实训记录"
      stripe
    >
      <el-table-column prop="title" label="实训任务" min-width="160" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'GRADED' ? 'success' : row.status === 'SUBMITTED' ? 'warning' : 'info'" size="small">
            {{ row.status === 'GRADED' ? '已评分' : row.status === 'SUBMITTED' ? '已提交' : '未提交' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="成绩" width="80">
        <template #default="{ row }">{{ row.score != null ? row.score : '-' }}</template>
      </el-table-column>
      <el-table-column
        prop="comment"
        label="评语"
        min-width="160"
        show-overflow-tooltip
      >
        <template #default="{ row }">{{ row.comment || '-' }}</template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getChildPractices } from '@/api/parent'

const route = useRoute()
const studentId = Number(route.params.studentId)
const list = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const res = await getChildPractices(studentId)
    if (res.code === 200) list.value = res.data || []
  } finally { loading.value = false }
})
</script>

<style scoped>
.cp-page { max-width: 800px; margin: 0 auto; padding: 16px; }
.cp-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.cp-header h3 { margin: 0; }
</style>

<template>
  <div v-if="visible" class="penetration-card">
    <el-card shadow="never">
      <div class="checkin-row">
        <el-icon :size="20" color="var(--el-color-warning, #e6a23c)"><Warning /></el-icon>
        <span class="checkin-label">今日对照班教学中，是否参考了AI诊断结果？</span>
        <el-button
          size="small"
          type="success"
          :loading="submitting"
          @click="submit(false)"
        >否，未参考</el-button>
        <el-button
          size="small"
          type="warning"
          :loading="submitting"
          @click="submit(true)"
        >是，参考了</el-button>
      </div>
    </el-card>
  </div>
  <div v-else-if="checked" class="checked-done">
    <el-tag type="info" size="small">✅ 今日已记录：{{ checkedReferenced ? '参考了AI' : '未参考AI' }}</el-tag>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Warning } from '@element-plus/icons-vue'
import request from '@/utils/request'

const visible = ref(false)
const checked = ref(false)
const checkedReferenced = ref(false)
const submitting = ref(false)

const fetchToday = async () => {
  try {
    const res = await request({ url: '/teacher/activity/penetration-check/today', method: 'get' })
    if (res.code === 200 && res.data) {
      if (res.data.checked) {
        checked.value = true
        checkedReferenced.value = res.data.referencedAi
        visible.value = false
      } else {
        visible.value = true
      }
    }
  } catch { /* ignore */ }
}

const submit = async (referencedAi) => {
  submitting.value = true
  try {
    const res = await request({
      url: '/teacher/activity/penetration-check',
      method: 'post',
      data: { referencedAi }
    })
    if (res.code === 200) {
      visible.value = false
      checked.value = true
      checkedReferenced.value = referencedAi
    }
  } catch { /* ignore */ } finally {
    submitting.value = false
  }
}

onMounted(fetchToday)
</script>

<style scoped>
.penetration-card {
  margin-bottom: 12px;
}
.penetration-card :deep(.el-card__body) {
  padding: 12px 16px;
}
.checkin-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.checkin-label {
  flex: 1;
  min-width: 200px;
  font-size: var(--fs-sm);
  color: #606266;
}
.checked-done {
  margin-bottom: 12px;
  text-align: center;
}
</style>

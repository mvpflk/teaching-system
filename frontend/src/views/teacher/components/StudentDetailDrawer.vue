<template>
  <el-drawer
    :model-value="visible"
    :title="(student?.studentName || '') + ' — 预警详情'"
    size="480px"
    direction="rtl"
    @close="emit('close')"
  >
    <div v-if="student" class="am-detail">
      <div class="am-detail-info">
        <div class="am-detail-row"><span class="am-detail-label">班级</span><span>{{ student.className }}</span></div>
        <div class="am-detail-row"><span class="am-detail-label">预警类型</span><span>{{ student.ruleName }}</span></div>
        <div class="am-detail-row"><span class="am-detail-label">触发时间</span><span>{{ fmtFull(student.createTime) }}</span></div>
        <div class="am-detail-row"><span class="am-detail-label">详情</span><span>{{ student.alertSummary }}</span></div>
      </div>
      <el-divider />
      <div class="am-detail-history">
        <h4>该学生所有预警</h4>
        <el-timeline v-if="history.length">
          <el-timeline-item
            v-for="h in history" :key="h.id"
            :timestamp="fmtFull(h.createTime)"
            :type="h.handledStatus === 'UNREAD' ? 'danger' : h.handledStatus === 'CONTACTED' ? 'success' : 'info'"
            placement="top"
          >
            <div class="am-tl-title">{{ h.ruleName }}</div>
            <div class="am-tl-summary">{{ h.alertSummary }}</div>
            <el-tag size="small" :type="statusTag(h.handledStatus)">{{ statusLabel(h.handledStatus) }}</el-tag>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无其他预警" :image-size="80" />
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { fmtFull } from '@/utils/date'

defineProps({
  visible: { type: Boolean, default: false },
  student: { type: Object, default: null },
  history: { type: Array, default: () => [] }
})

const emit = defineEmits(['close'])

const STATUS_MAP = { UNREAD: '未读', READ: '已读', CONTACTED: '已联系家长', IGNORED: '已忽略' }
const statusLabel = (s) => STATUS_MAP[s] || s
const statusTag = (s) => ({ UNREAD: 'danger', CONTACTED: 'success', IGNORED: 'info' }[s] || '')
</script>

<style scoped>
.am-detail-info { display: flex; flex-direction: column; gap: 8px; }
.am-detail-row { display: flex; gap: 12px; }
.am-detail-label { min-width: 70px; color: var(--text-secondary); font-size: var(--fs-sm); }
.am-detail-history h4 { font-size: var(--fs-md); font-weight: 600; margin-bottom: 12px; }
.am-tl-title { font-weight: 600; margin-bottom: 4px; }
.am-tl-summary { font-size: var(--fs-sm); color: var(--text-regular); margin-bottom: 6px; }
</style>

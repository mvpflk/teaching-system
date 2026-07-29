<template>
  <div class="am-dashboard">
    <div class="am-dash-card am-dash-total">
      <div class="am-dash-num">{{ stats.total }}</div>
      <div class="am-dash-label">预警总数</div>
    </div>
    <div class="am-dash-card am-dash-unread">
      <div class="am-dash-num">{{ stats.unread }}</div>
      <div class="am-dash-label">待处理</div>
    </div>
    <div class="am-dash-card am-dash-low">
      <div class="am-dash-num">{{ stats.lowScore }}</div>
      <div class="am-dash-label">低分预警</div>
    </div>
    <div class="am-dash-card am-dash-miss">
      <div class="am-dash-num">{{ stats.missing }}</div>
      <div class="am-dash-label">缺交预警</div>
    </div>
    <div class="am-dash-card am-dash-contacted">
      <div class="am-dash-num">{{ stats.contacted }}</div>
      <div class="am-dash-label">已联系</div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  stats: { type: Object, required: true }
})
</script>

<style scoped>
.am-dashboard { display: grid; grid-template-columns: repeat(5, 1fr); gap: 10px; margin-bottom: 16px; }
.am-dash-card { padding: 16px; border-radius: var(--radius-md); text-align: center; border: 1px solid var(--border-light); }
.am-dash-num { font-size: 28px; font-weight: 800; line-height: 1.2; }
.am-dash-label { font-size: var(--fs-xs); color: var(--text-secondary); margin-top: 4px; }
.am-dash-total .am-dash-num { color: var(--text-primary); }
.am-dash-unread .am-dash-num { color: var(--el-color-danger); }
.am-dash-low .am-dash-num { color: var(--el-color-danger); }
.am-dash-miss .am-dash-num { color: var(--el-color-warning); }
.am-dash-contacted .am-dash-num { color: var(--el-color-success); }
@media (max-width: 768px) {
  .am-dashboard { grid-template-columns: repeat(2, 1fr); }
}
</style>

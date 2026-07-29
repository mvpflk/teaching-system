<template>
  <div class="ph-card">
    <div class="ph-card-title">进步报告</div>
    <p class="ph-card-desc">掌握度变化趋势 + 薄弱知识点分析</p>
    <van-button
      plain
      block
      round
      type="primary"
      :loading="reportLoading"
      @click="loadReport"
    >
      查看报告
    </van-button>
    <div v-if="reportData" class="ph-report-data">
      <div class="ph-stat-row">
        <div class="ph-stat">
          <span class="ph-stat-num">{{ reportData.avgMastery }}%</span><span class="ph-stat-label">掌握度</span>
        </div>
        <div class="ph-stat">
          <span class="ph-stat-num">{{ reportData.nodeCount }}</span><span class="ph-stat-label">知识点</span>
        </div>
        <div class="ph-stat">
          <span class="ph-stat-num" style="color: var(--el-color-success)">{{
            reportData.masteredCount || 0
          }}</span><span class="ph-stat-label">已掌握</span>
        </div>
      </div>
      <div v-if="reportData.weakNodes?.length" class="ph-weak-section">
        <div class="ph-card-subtitle">薄弱知识点 TOP {{ reportData.weakNodes.length }}</div>
        <div v-for="w in reportData.weakNodes" :key="w.nodeId" class="ph-weak-item">
          <div class="ph-weak-head">
            <span class="ph-weak-name">{{ w.nodeName }}</span>
            <span class="ph-weak-pct">{{ w.masteryPercent }}%</span>
          </div>
          <van-progress
            :percentage="w.masteryPercent"
            stroke-width="6"
            color="var(--el-color-warning)"
            :show-pivot="false"
          />
          <van-button
            v-if="w.learningResources"
            size="mini"
            plain
            type="primary"
            style="margin-top: 6px"
            @click="$emit('show-resources', w)"
          >
            学习资源
          </van-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { showToast } from 'vant';
import 'vant/es/toast/style';
import { getReport } from '@/api/precision';

const props = defineProps({
  subject: { type: Object, required: true },
});
defineEmits(['show-resources']);

const reportLoading = ref(false);
const reportData = ref(null);

async function loadReport() {
  reportLoading.value = true;
  try {
    const res = await getReport(props.subject.key);
    reportData.value = res.data;
  } catch {
    showToast('获取失败');
  }
  reportLoading.value = false;
}
</script>

<style scoped>
.ph-card {
  margin: 0 16px;
  padding: 20px;
  background: var(--bg-card, #fff);
  border: 1px solid var(--border-base, #e8e8ed);
  border-radius: var(--radius-md, 8px);
}
.ph-card-title {
  font-size: var(--fs-lg);
  font-weight: 600;
  color: var(--text-primary, var(--text-primary));
  margin-bottom: 6px;
}
.ph-card-subtitle {
  font-size: var(--fs-md);
  font-weight: 600;
  color: var(--text-primary, var(--text-primary));
  margin-bottom: 10px;
}
.ph-card-desc {
  font-size: var(--fs-sm);
  color: var(--text-secondary, var(--text-secondary));
  margin: 0 0 16px;
  line-height: 1.5;
}
.ph-report-data {
  margin-top: 12px;
}
.ph-stat-row {
  display: flex;
  gap: 16px;
}
.ph-stat {
  flex: 1;
  text-align: center;
  padding: 12px;
  background: var(--bg-secondary, var(--bg-secondary));
  border-radius: var(--radius-sm, 4px);
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.ph-stat-num {
  font-size: var(--fs-lg);
  font-weight: 700;
  color: var(--text-primary, var(--text-primary));
}
.ph-stat-label {
  font-size: var(--fs-xs);
  color: var(--text-secondary, var(--text-secondary));
}
.ph-weak-section {
  border-top: 1px solid var(--border-base, #e8e8ed);
  padding-top: 12px;
  margin-top: 12px;
}
.ph-weak-item {
  padding: 10px 0;
  border-bottom: 1px solid var(--bg-secondary, var(--bg-secondary));
}
.ph-weak-item:last-child {
  border-bottom: none;
}
.ph-weak-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}
.ph-weak-name {
  font-size: var(--fs-sm);
  font-weight: 500;
  color: var(--text-primary, var(--text-primary));
}
.ph-weak-pct {
  font-size: var(--fs-sm);
  font-weight: 700;
  color: var(--el-color-warning);
}
</style>

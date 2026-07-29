<template>
  <div class="pr-page">
    <div class="pr-header">
      <el-page-header @back="$router.back()">
        <template #content><span>进步报告</span></template>
      </el-page-header>
    </div>

    <el-tabs v-model="activeTab" type="border-card" @tab-change="onTabChange">
      <el-tab-pane name="english">
        <template #label><span>英语</span></template>
        <div v-loading="engLoading" class="pr-content">
          <template v-if="engReady">
            <div class="pr-chart-box">
              <div ref="engChartRef" class="pr-chart"></div>
            </div>
            <div class="pr-cards">
              <div class="pr-stat-card">
                <div class="pr-stat-num">{{ eng.vocabTotal || 0 }}</div>
                <div class="pr-stat-label">接触词汇</div>
              </div>
              <div class="pr-stat-card">
                <div class="pr-stat-num" style="color: var(--success-color)">
                  {{ eng.vocabMastered || 0 }}
                </div>
                <div class="pr-stat-label">已掌握(L3+)</div>
              </div>
              <div class="pr-stat-card">
                <div class="pr-stat-num" style="color: var(--primary-color)">
                  {{ eng.streakWeeks || 0 }}
                </div>
                <div class="pr-stat-label">连续完成(周)</div>
              </div>
              <div class="pr-stat-card">
                <div class="pr-stat-num" style="color: var(--warning-color)">
                  {{ engReport.lastTestScore || '-' }}
                </div>
                <div class="pr-stat-label">最近小测</div>
              </div>
            </div>
            <div v-if="engReport.weakNodes?.length" class="pr-weak">
              <div class="pr-section-title">待加强知识点</div>
              <div
                v-for="w in engReport.weakNodes.slice(0, 5)"
                :key="w.nodeId || Math.random()"
                class="pr-weak-item"
              >
                <div style="flex: 1; min-width: 0">
                  <span class="pr-weak-name">{{
                    w.nodeName || (w.nodeId ? '知识点 #' + w.nodeId : '未知知识点')
                  }}</span>
                  <WeaknessCardLink v-if="w.nodeId" :node-id="w.nodeId" :node-name="w.nodeName" />
                </div>
                <el-progress
                  :percentage="w.masteryPercent || 0"
                  :stroke-width="6"
                  color="var(--el-color-warning)"
                  :show-text="false"
                  style="flex: 1; margin: 0 10px"
                />
                <span class="pr-weak-pct">{{ w.masteryPercent || 0 }}%</span>
              </div>
            </div>
          </template>
          <el-empty
            v-else-if="!engLoading"
            description="暂无英语学习数据，先完成诊断测试"
            :image-size="48"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane name="math">
        <template #label><span>数学</span></template>
        <div v-loading="mathLoading" class="pr-content">
          <template v-if="mathReady">
            <div class="pr-chart-box">
              <div ref="mathChartRef" class="pr-chart"></div>
            </div>
            <div class="pr-cards">
              <div class="pr-stat-card">
                <div class="pr-stat-num">{{ mathReport.avgMastery || 0 }}%</div>
                <div class="pr-stat-label">平均掌握度</div>
              </div>
              <div class="pr-stat-card">
                <div class="pr-stat-num">{{ mathReport.nodeCount || 0 }}</div>
                <div class="pr-stat-label">覆盖知识点</div>
              </div>
              <div class="pr-stat-card">
                <div class="pr-stat-num" style="color: var(--success-color)">
                  {{ mathReport.masteredCount || 0 }}
                </div>
                <div class="pr-stat-label">已掌握</div>
              </div>
              <div class="pr-stat-card">
                <div class="pr-stat-num" style="color: var(--primary-color)">
                  {{ mathReport.streakWeeks || 0 }}
                </div>
                <div class="pr-stat-label">连续完成(周)</div>
              </div>
            </div>
            <div v-if="mathReport.weakNodes?.length" class="pr-weak">
              <div class="pr-section-title">薄弱知识点 TOP {{ mathReport.weakNodes.length }}</div>
              <div
                v-for="w in mathReport.weakNodes"
                :key="w.nodeId || Math.random()"
                class="pr-weak-item"
              >
                <div style="flex: 1; min-width: 0">
                  <span class="pr-weak-name">{{
                    w.nodeName || (w.nodeId ? '知识点 #' + w.nodeId : '未知知识点')
                  }}</span>
                  <WeaknessCardLink v-if="w.nodeId" :node-id="w.nodeId" :node-name="w.nodeName" />
                </div>
                <el-progress
                  :percentage="w.masteryPercent || 0"
                  :stroke-width="6"
                  color="var(--el-color-warning)"
                  :show-text="false"
                  style="flex: 1; margin: 0 10px"
                />
                <span class="pr-weak-pct">{{ w.masteryPercent || 0 }}%</span>
              </div>
            </div>
          </template>
          <el-empty
            v-else-if="!mathLoading"
            description="暂无数学学习数据，先完成诊断测试"
            :image-size="48"
          />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import { getDashboard, getReport } from '@/api/precision';
import WeaknessCardLink from '@/components/knowledge/WeaknessCardLink.vue';
import {
  primaryColor,
  textPrimary,
  textDisabled,
  bgSecondary,
  elSuccess,
  elWarning,
} from '@/utils/theme';

const activeTab = ref('english');
const engLoading = ref(true),
  mathLoading = ref(true);
const engReady = ref(false),
  mathReady = ref(false);
const eng = ref({ vocabTotal: 0, vocabMastered: 0, streakWeeks: 0 });
const math = ref({ nodesTotal: 0, nodesMastered: 0, avgMastery: 0 });
const engReport = ref({}),
  mathReport = ref({});
const engChartRef = ref(null),
  mathChartRef = ref(null);
let engChart = null,
  mathChart = null,
  disposed = false;

onMounted(async () => {
  window.addEventListener('resize', handleResize);
  try {
    const d = await getDashboard();
    if (d.code === 200) {
      if (d.data.english) eng.value = { ...eng.value, ...d.data.english };
      if (d.data.math) math.value = { ...math.value, ...d.data.math };
    }
  } catch (e) {
    console.error('加载仪表盘失败:', e);
  }
  await loadReport('英语[职高]');
  engLoading.value = false;
});

onUnmounted(() => {
  disposed = true;
  engChart?.dispose();
  engChart = null;
  mathChart?.dispose();
  mathChart = null;
  window.removeEventListener('resize', handleResize);
});

async function loadReport(subject) {
  try {
    const res = await getReport(subject);
    if (res.code === 200) {
      if (subject.includes('英语')) {
        engReport.value = res.data;
        engReady.value = true;
        await nextTick();
        renderEngChart();
      } else {
        mathReport.value = res.data;
        mathReady.value = true;
        await nextTick();
        renderMathChart();
      }
    }
  } catch (e) {
    console.error('加载报告失败:', e);
  }
}

async function onTabChange(name) {
  if (name === 'math' && !mathReady.value) {
    mathLoading.value = true;
    await loadReport('数学[职高]');
    mathLoading.value = false;
  }
}

function handleResize() {
  if (engChart && !engChart.isDisposed()) engChart.resize();
  if (mathChart && !mathChart.isDisposed()) mathChart.resize();
}

function renderEngChart() {
  if (!engChartRef.value) return;
  if (engChart) engChart.dispose();
  import('@/utils/echarts').then(({ default: echarts }) => {
    if (disposed) return;
    engChart = echarts.init(engChartRef.value);
    engChart.setOption({
      grid: { top: 20, right: 24, bottom: 24, left: 24, containLabel: true },
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: ['接触词汇', '已掌握(L3+)', '复习中', '待学习'],
        axisLabel: { fontSize: 10 },
      },
      yAxis: { type: 'value', minInterval: 1 },
      series: [
        {
          type: 'bar',
          barWidth: 24,
          data: [
            { value: eng.value.vocabTotal || 0, itemStyle: { color: primaryColor } },
            { value: eng.value.vocabMastered || 0, itemStyle: { color: elSuccess } },
            {
              value: Math.max(0, engReport.value.learningCount || 0),
              itemStyle: { color: elWarning },
            },
            {
              value: Math.max(
                0,
                (engReport.value.nodeCount || 0) -
                  (engReport.value.masteredCount || 0) -
                  (engReport.value.learningCount || 0)
              ),
              itemStyle: { color: textDisabled },
            },
          ],
        },
      ],
    });
  });
}

function renderMathChart() {
  if (!mathChartRef.value) return;
  if (mathChart) mathChart.dispose();
  import('@/utils/echarts').then(({ default: echarts }) => {
    if (disposed) return;
    mathChart = echarts.init(mathChartRef.value);
    mathChart.setOption({
      series: [
        {
          type: 'gauge',
          startAngle: 210,
          endAngle: -30,
          center: ['50%', '55%'],
          radius: '90%',
          progress: { show: true, width: 12, itemStyle: { color: primaryColor } },
          axisLine: {
            lineStyle: {
              width: 12,
              color: [
                [mathReport.value.avgMastery / 100 || 0, primaryColor],
                [1, bgSecondary],
              ],
            },
          },
          axisTick: { show: false },
          splitLine: { show: false },
          axisLabel: { show: false },
          detail: {
            valueAnimation: true,
            fontSize: 22,
            fontWeight: 700,
            color: textPrimary,
            offsetCenter: [0, 16],
          },
          data: [{ value: mathReport.value.avgMastery || 0 }],
        },
      ],
    });
  });
}
</script>

<style scoped>
.pr-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px;
}
.pr-header {
  margin-bottom: 16px;
}
.pr-content {
  padding: 8px 0;
}
.pr-chart-box {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 12px 8px 8px;
  margin-bottom: 16px;
}
.pr-chart {
  width: 100%;
  height: 200px;
}
.pr-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-bottom: 16px;
}
.pr-stat-card {
  text-align: center;
  padding: 14px 8px;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
}
.pr-stat-num {
  font-size: var(--fs-xl);
  font-weight: 700;
  color: var(--text-primary);
}
.pr-stat-label {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  margin-top: 2px;
}

.pr-weak {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 16px;
}
.pr-section-title {
  font-size: var(--fs-md);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
}
.pr-weak-item {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}
.pr-weak-name {
  font-size: var(--fs-xs);
  color: var(--text-regular);
  width: 90px;
  flex-shrink: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pr-weak-pct {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  width: 36px;
  text-align: right;
  flex-shrink: 0;
}

@media (min-width: 768px) {
  .pr-cards {
    grid-template-columns: repeat(4, 1fr);
  }
}
</style>

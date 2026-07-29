<template>
  <div class="co-page">
    <div class="page-header">
      <el-button text @click="$router.push('/student/checkpoint')">← 返回</el-button>
      <h2>{{ overview.subjectName }}</h2>
      <span class="co-progress">{{ overview.passedCheckpoints }}/{{ overview.totalCheckpoints }} 关</span>
    </div>
    <el-progress
      :percentage="overview.progress || 0"
      :stroke-width="8"
      :color="progressColors"
      class="co-bar"
    />
    <div v-loading="loading" class="co-body">
      <CheckpointPath
        v-if="checkpoints.length > 0"
        :checkpoints="checkpoints"
        @select="handleNodeSelect"
      />
      <el-empty v-else description="暂无关卡" />
    </div>
    <el-dialog v-model="reviewVisible" title="关卡回顾" width="400px">
      <div v-if="selectedCheckpoint" class="co-review">
        <div class="co-review-title">{{ selectedCheckpoint.taskName }}</div>
        <div class="co-review-chapter">{{ selectedCheckpoint.chapterName }}</div>
        <div class="co-review-stats">
          <span>尝试次数：{{ selectedCheckpoint.attempts }}</span>
          <span>关键词掌握：{{ selectedCheckpoint.keywordsPassed ? '已通过' : '未通过' }}</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getOverview } from '@/api/checkpoint';
import CheckpointPath from '@/components/checkpoint/CheckpointPath.vue';

const route = useRoute();
const router = useRouter();
const subjectId = computed(() => Number(route.params.subjectId));
const overview = ref({});
const loading = ref(true);
const reviewVisible = ref(false);
const selectedCheckpoint = ref(null);

const checkpoints = computed(() => overview.value.checkpoints || []);

const progressColors = [
  { color: 'var(--primary-color)', percentage: 50 },
  { color: 'var(--el-color-success)', percentage: 100 },
];

onMounted(async () => {
  const res = await getOverview(subjectId.value);
  if (res.code === 200) overview.value = res.data;
  loading.value = false;
});

function handleNodeSelect(node) {
  if (node.status === 'locked') {
    ElMessage.warning('请先通过上一关');
    return;
  }
  if (node.status === 'done') {
    selectedCheckpoint.value = node;
    reviewVisible.value = true;
    return;
  }
  if (node.checkpointType === 'BOSS') {
    router.push(`/student/checkpoint/${subjectId.value}/boss/${node.configId}`);
  } else if (node.checkpointType === 'MIXED') {
    router.push(`/student/checkpoint/${subjectId.value}/mixed/${node.configId}`);
  } else {
    router.push(`/student/checkpoint/${subjectId.value}/${node.configId}`);
  }
}
</script>

<style scoped>
.co-page {
  max-width: 600px;
  margin: 0 auto;
  padding: var(--spacing-lg);
}
.co-progress {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  margin-left: var(--spacing-sm);
}
.co-bar {
  margin: var(--spacing-md) 0;
}
.co-body {
  min-height: 400px;
}
.co-review {
  text-align: center;
}
.co-review-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}
.co-review-chapter {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 16px;
}
.co-review-stats {
  display: flex;
  justify-content: center;
  gap: 24px;
  font-size: 13px;
  color: var(--text-regular);
}
</style>

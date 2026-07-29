<template>
  <div class="sp-page">
    <div class="page-header">
      <h2>闯关学习</h2>
      <span class="sp-sub">选择学科，打通关卡</span>
    </div>

    <el-empty v-if="!loading && subjects.length === 0" description="暂无可用的闯关学科" />

    <div v-loading="loading" class="sp-grid">
      <div
        v-for="s in subjects"
        :key="s.subjectId"
        class="sp-card"
        @click="$router.push(`/student/checkpoint/${s.subjectId}`)"
      >
        <div class="sp-card-icon">{{ getSubjectIcon(s.subjectName) }}</div>
        <div class="sp-card-header">
          <span class="sp-card-title">{{ s.subjectName }}</span>
          <span class="sp-card-arrow">&rarr;</span>
        </div>
        <div class="sp-card-progress">
          <el-progress :percentage="s.progress" :stroke-width="6" :show-text="false" />
          <span class="sp-progress-text">{{ s.passedCheckpoints }}/{{ s.totalCheckpoints }} 关</span>
        </div>
        <div class="sp-card-footer">
          <span v-if="s.progress >= 100" class="sp-done">全部通关</span>
          <span v-else class="sp-ongoing">进行中</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { listSubjects } from '@/api/checkpoint';

const subjectIcons = {
  数学: '📐',
  英语: '📖',
  计算机: '💻',
  语文: '📝',
  物理: '⚡',
  化学: '🧪',
  生物: '🧬',
  历史: '📜',
  地理: '🌍',
  政治: '⚖️',
};

function getSubjectIcon(name) {
  for (const [key, icon] of Object.entries(subjectIcons)) {
    if (name.includes(key)) return icon;
  }
  return '📚';
}

const subjects = ref([]);
const loading = ref(true);

onMounted(async () => {
  const res = await listSubjects();
  if (res.code === 200) subjects.value = res.data;
  loading.value = false;
});
</script>

<style scoped>
.sp-page {
  max-width: 900px;
  margin: 0 auto;
  padding: var(--spacing-lg);
}
.sp-sub {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  margin-left: var(--spacing-sm);
}
.sp-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: var(--spacing-md);
  margin-top: var(--spacing-md);
}
.sp-card {
  background: var(--bg-card);
  border: 0.5px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: var(--spacing-md);
  cursor: pointer;
  transition: transform var(--transition-base);
}
.sp-card:hover {
  transform: translateY(-2px);
}
.sp-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.sp-card-title {
  font-weight: 600;
  font-size: var(--fs-lg);
  color: var(--text-primary);
}
.sp-card-arrow {
  color: var(--text-secondary);
  font-size: var(--fs-lg);
}
.sp-progress-text {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  margin-top: 4px;
  display: block;
}
.sp-card-icon {
  font-size: 28px;
  margin-bottom: 8px;
}
.sp-card-footer {
  margin-top: 12px;
}
.sp-done {
  color: var(--el-color-warning);
  font-size: var(--fs-xs);
  font-weight: 500;
}
.sp-ongoing {
  color: var(--primary-color);
  font-size: var(--fs-xs);
}
</style>

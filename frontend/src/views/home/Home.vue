<template>
  <div class="page-container">
    <!-- 1. 欢迎区 -->
    <HomeWelcomeBanner
      :real-name="userInfo?.realName"
      :greeting="greeting"
      :summary-line="summaryLine"
      :is-student="isStudent"
      :is-teacher="isTeacher"
      :is-admin="isAdmin"
    />

    <!-- 1.5 新人引导（仅教师） -->
    <GettingStartedGuide v-if="isTeacher || isAdmin" />

    <!-- 2. 统计区 -->
    <HomeStatCards
      :is-student="isStudent"
      :stats="stats"
      :submission-rate="submissionRate"
      :pass-rate="passRate"
      :submission-rate-color="submissionRateColor"
      :pass-rate-color="passRateColor"
      :submission-rate-class="submissionRateClass"
      :pass-rate-class="passRateClass"
    />

    <!-- 今日知识卡片（学生端） -->
    <DailyKnowledgeCard v-if="isStudent" />

    <!-- 错题本提醒卡片（学生端） -->
    <div
      v-if="isStudent && wrongBookStats"
      class="wb-reminder mb-24"
      @click="$router.push('/wrong-book')"
    >
      <div class="wb-reminder-left">
        <el-icon><Notebook /></el-icon>
        <div class="wb-reminder-text">
          <span class="wb-reminder-title">错题本</span>
          <span v-if="wrongBookStats.unmastered > 0" class="wb-reminder-desc">
            还有 <strong>{{ wrongBookStats.unmastered }}</strong> 道错题未掌握
            <template v-if="wrongBookStats.streak > 1">，已连续 {{ wrongBookStats.streak }} 天练习 🔥</template>
          </span>
          <span v-else class="wb-reminder-desc">全部已掌握</span>
        </div>
      </div>
      <span class="wb-reminder-action">
        {{ wrongBookStats.unmastered > 0 ? '去复习 →' : '查看详情 →' }}
      </span>
    </div>

    <!-- 偏科提分入口（学生端） -->
    <div
      v-if="isStudent"
      class="wb-reminder mb-24"
      style="background: var(--primary-light); border-color: var(--primary-color)"
      @click="$router.push('/student/precision')"
    >
      <div class="wb-reminder-left">
        <el-icon style="color: var(--primary-color)"><TrendCharts /></el-icon>
        <div class="wb-reminder-text">
          <span class="wb-reminder-title">偏科提分</span>
          <span class="wb-reminder-desc">数学/英语精准补差 · 每天20分钟 · 周末线上反馈</span>
        </div>
      </div>
      <span class="wb-reminder-action">进入提分 →</span>
    </div>

    <!-- 教师班级筛选 -->
    <div v-if="(isTeacher || isAdmin) && teachingClasses.length > 0" class="filter-bar mb-24">
      <span class="filter-label">筛选班级</span>
      <el-select
        v-model="selectedClassId"
        placeholder="全部班级"
        clearable
        @change="onClassChange"
      >
        <el-option :value="''" label="全部班级" />
        <el-option
          v-for="c in teachingClasses"
          :key="c.classId"
          :value="c.classId"
          :label="c.className + (c.subject ? ' (' + c.subject + ')' : '')"
        />
      </el-select>
      <span v-if="selectedClassId" class="filter-hint">当前统计范围为该班级</span>
    </div>

    <!-- 3. 待办统计卡片（教师端） -->
    <div v-if="(isTeacher || isAdmin) && hasPendingStats" class="pending-stats-row">
      <div
        v-if="stats.pendingReview > 0"
        class="pending-stat-card"
        @click="$router.push('/teacher/tasks/list?status=IN_PROGRESS')"
      >
        <span class="pending-stat-num">{{ stats.pendingReview }}</span>
        <span class="pending-stat-label">份待批阅</span>
        <el-icon><DocumentChecked /></el-icon>
      </div>
      <div
        v-if="stats.pendingPublish > 0"
        class="pending-stat-card"
        @click="$router.push('/teacher/tasks/list?status=DRAFT')"
      >
        <span class="pending-stat-num">{{ stats.pendingPublish }}</span>
        <span class="pending-stat-label">个待发布任务</span>
        <el-icon><UploadFilled /></el-icon>
      </div>
      <div
        v-if="stats.pendingAiReview > 0"
        class="pending-stat-card"
        @click="$router.push('/teacher/tasks/question-bank')"
      >
        <span class="pending-stat-num">{{ stats.pendingAiReview }}</span>
        <span class="pending-stat-label">道AI题目待审</span>
        <el-icon><Cpu /></el-icon>
      </div>
      <div
        v-if="stats.pendingResourceReview > 0"
        class="pending-stat-card"
        @click="$router.push('/system-management/category-manager')"
      >
        <span class="pending-stat-num">{{ stats.pendingResourceReview }}</span>
        <span class="pending-stat-label">个学习资源待审核</span>
        <el-icon><Notebook /></el-icon>
      </div>
    </div>

    <!-- 3.5 质量预警（教师端） -->
    <div v-if="(isTeacher || isAdmin) && stats.qualityAlerts?.length" class="quality-alerts-row">
      <div
        v-for="alert in stats.qualityAlerts"
        :key="alert.firstTaskId"
        class="quality-alert-card"
        @click="
          $router.push(
            `/teacher/quality/comparison/${alert.firstTaskId}?ids=${alert.taskIds}&title=${encodeURIComponent(alert.title)}&subject=${encodeURIComponent(alert.subject || '')}`
          )
        "
      >
        <div class="qa-card-left">
          <span class="qa-card-icon">⚠️</span>
          <div class="qa-card-body">
            <div class="qa-card-title">「{{ alert.title }}」</div>
            <div class="qa-card-desc">
              班级间均分差 {{ alert.scoreDelta }} 分
              <template v-if="alert.kpCount > 0">，{{ alert.kpCount }} 个知识点差异明显</template>
            </div>
          </div>
        </div>
        <span class="qa-card-action">查看对比 →</span>
      </div>
    </div>

    <!-- 4. 待办区 -->
    <div v-if="pendingHomework.length > 0" class="section-header mb-16">
      <h3 class="section-title">待办任务</h3>
      <el-button
        class="view-all-btn"
        size="small"
        @click="$router.push(isStudent ? '/student/tasks' : '/teacher/tasks')"
      >
        查看全部 <el-icon><ArrowRight /></el-icon>
      </el-button>
    </div>
    <div v-if="pendingHomework.length > 0" class="todo-grid mb-24">
      <HomeTodoCard
        v-for="item in pendingHomework"
        :key="item.id"
        :homework="item"
        @click="onTodoClick"
      />
    </div>
    <EmptyState v-else-if="!isStudent" description="暂无待办任务" class="mb-24" />

    <!-- 4. 班级概览区（缩小版图表） -->
    <HomeDashboardCharts
      v-if="isTeacher || isAdmin"
      :dash-data="dashData"
      :submission-rate="submissionRate"
      :pass-rate="passRate"
      :submission-rate-color="submissionRateColor"
      :pass-rate-color="passRateColor"
    />

    <!-- 5. 快捷操作区 -->
    <HomeQuickActions v-if="isTeacher || isAdmin" class="mb-24" />

    <!-- 近期任务 + 积分排行 -->
    <HomeBottomGrid
      :is-student="isStudent"
      :homework-list="pendingHomework"
      :top-students="topStudents"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import {
  Notebook,
  ArrowRight,
  TrendCharts,
  DocumentChecked,
  UploadFilled,
  Cpu,
} from '@element-plus/icons-vue';
import { getHomeworkList } from '@/api/task';
import { getCreditRanking, getCreditInfo } from '@/api/credit';
import { getStudentStats } from '@/api/wrong';
import { useHomeCharts } from './useHomeCharts';
import HomeWelcomeBanner from './HomeWelcomeBanner.vue';
import HomeStatCards from './HomeStatCards.vue'
import EmptyState from '@/components/common/EmptyState.vue';
import HomeQuickActions from './HomeQuickActions.vue';
import HomeDashboardCharts from './HomeDashboardCharts.vue';
import HomeTodoCard from './HomeTodoCard.vue';
import HomeBottomGrid from './HomeBottomGrid.vue';
import DailyKnowledgeCard from '@/components/home/DailyKnowledgeCard.vue';
import GettingStartedGuide from '@/components/common/GettingStartedGuide.vue';

const router = useRouter();
const userStore = useUserStore();
const userInfo = computed(() => userStore.userInfo);
const isStudent = computed(() => userStore.isStudent);
const isTeacher = computed(() => userStore.isTeacher);
const isAdmin = computed(() => userStore.isAdmin);

const stats = ref({
  pendingHomework: 0,
  pendingExam: 0,
  totalCredits: 0,
  totalStudents: 0,
  totalHomework: 0,
});
const wrongBookStats = ref(null);
const pendingHomework = ref([]);
const topStudents = ref([]);

const teachingClasses = computed(() => userStore.teacherSummary?.teachingClasses || []);

// 教师端默认选中第一个任教班级
const selectedClassId = ref(null);
const onClassChange = () => loadTeacherCharts(stats.value);

const {
  dashData,
  submissionRate,
  passRate,
  submissionRateColor,
  passRateColor,
  submissionRateClass,
  passRateClass,
  loadTeacherCharts,
} = useHomeCharts(selectedClassId);

const greeting = computed(() => {
  const h = new Date().getHours();
  return h < 12 ? '早上好' : h < 18 ? '下午好' : '晚上好';
});

const summaryLine = computed(() => {
  const parts = [];
  if (isStudent.value) {
    const wb = wrongBookStats.value;
    if (wb) {
      if (wb.unmastered > 0) parts.push(`${wb.unmastered} 道错题未掌握`);
      if (wb.streak > 1) parts.push(`连续 ${wb.streak} 天练习`);
      if (wb.weekPractice > 0) parts.push(`本周 ${wb.weekPractice} 次练习`);
    }
    if (stats.value?.taskCount != null) parts.push(`${stats.value.taskCount} 个待完成任务`);
    if (!parts.length) parts.push('一切就绪');
  }
  if (isTeacher.value || isAdmin.value) {
    if (stats.value?.pendingReview > 0) parts.push(`${stats.value.pendingReview} 份待批阅`);
    if (stats.value?.pendingPublish > 0) parts.push(`${stats.value.pendingPublish} 个待发布`);
    if (stats.value?.totalStudents > 0) parts.push(`共 ${stats.value.totalStudents} 名学生`);
    if (!parts.length && selectedClassId.value) parts.push('当前班级数据已加载');
  }
  return parts.join(' · ');
});

const hasPendingStats = computed(
  () =>
    stats.value.pendingReview > 0 ||
    stats.value.pendingPublish > 0 ||
    stats.value.pendingAiReview > 0 ||
    stats.value.pendingResourceReview > 0
);

const onTodoClick = (item) => {
  if (!item || !item.id) return;
  if (isStudent.value) {
    router.push(`/student/tasks/${item.id}`);
  } else {
    router.push(`/teacher/tasks`);
  }
};

onMounted(async () => {
  if (!userStore.role) {
    try {
      await userStore.getInfo();
    } catch (e) {
      /* 网络异常容错 */
    }
  }

  // 教师端默认选中第一个任教班级
  if ((isTeacher.value || isAdmin.value) && teachingClasses.value.length > 0) {
    selectedClassId.value = teachingClasses.value[0].classId;
  }

  if (isStudent.value) {
    const results = await Promise.allSettled([
      getHomeworkList({ pageNum: 1, pageSize: 10 }),
      getStudentStats(),
      getCreditInfo(),
      getCreditRanking({ limit: 5 }),
    ]);
    // 待办任务
    const hwRes = results[0];
    if (hwRes.status === 'fulfilled' && hwRes.value.code === 200) {
      pendingHomework.value = (hwRes.value.data.records || []).filter(
        (h) => !h.submitCount || h.submitCount < (h.totalStudents || 1)
      );
      // 按任务类型拆分:考试类(FORMATIVE/SUMMATIVE)计入 pendingExam,其余计入 pendingHomework
      const EXAM_TYPES = ['FORMATIVE', 'SUMMATIVE'];
      stats.value.pendingHomework = pendingHomework.value.filter(
        (h) => !EXAM_TYPES.includes(h.taskType)
      ).length;
      stats.value.pendingExam = pendingHomework.value.filter((h) =>
        EXAM_TYPES.includes(h.taskType)
      ).length;
    } else {
      console.error('Home: 加载待办任务失败', hwRes.reason);
    }
    // 错题统计
    const statsRes = results[1];
    if (statsRes.status === 'fulfilled' && statsRes.value.code === 200) {
      wrongBookStats.value = statsRes.value.data;
    } else {
      console.error('Home: 加载错题统计失败', statsRes.reason);
    }
    // 积分信息
    const creditRes = results[2];
    if (creditRes.status === 'fulfilled' && creditRes.value.code === 200) {
      stats.value.totalCredits = creditRes.value.data.totalCredits || 0;
    } else {
      console.error('Home: 加载积分信息失败', creditRes.reason);
    }
    // 积分排行
    const rankRes = results[3];
    if (rankRes.status === 'fulfilled' && rankRes.value.code === 200) {
      topStudents.value = rankRes.value.data;
    } else {
      console.error('Home: 加载积分排行失败', rankRes.reason);
    }
  } else {
    try {
      const r = await getHomeworkList({ pageNum: 1, pageSize: 10 });
      if (r.code === 200) {
        pendingHomework.value = r.data.records || [];
        stats.value.pendingHomework = pendingHomework.value.length;
      }
    } catch (e) {
      console.error('Home: 加载待办任务失败', e);
      ElMessage.warning('部分数据加载失败');
    }
    try {
      const r = await getCreditRanking({ limit: 5 });
      if (r.code === 200) topStudents.value = r.data;
    } catch (e) {
      console.error('Home: 加载积分排行失败', e);
    }
    await loadTeacherCharts(stats.value);
  }
});
</script>

<style scoped lang="scss">
.filter-bar {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 12px 20px;
  border: 0.5px solid var(--border-color);
  display: flex;
  align-items: center;
  gap: 12px;

  .filter-label {
    font-size: var(--fs-sm);
    font-weight: 500;
    color: var(--text-regular);
    white-space: nowrap;
  }
  .filter-hint {
    font-size: var(--fs-xs);
    color: var(--text-secondary);
  }
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-title {
  font-size: var(--fs-md);
  font-weight: 500;
  margin: 0;
  color: var(--text-primary);
}

.view-all-btn {
  color: var(--text-secondary) !important;
  font-size: var(--fs-sm);
  padding: 4px 12px;
  border-radius: var(--radius-sm);
  border: 0.5px solid transparent;
  transition: all 0.2s;
}
.view-all-btn:hover {
  color: var(--primary-color) !important;
  background: var(--primary-light);
  border-color: var(--primary-color);
}

.todo-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.wb-reminder {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  background: var(--bg-card);
  border: 0.5px solid var(--border-color);
  border-left: 3px solid var(--el-color-danger);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition:
    transform var(--transition-fast),
    border-color var(--transition-fast);
}
.wb-reminder:hover {
  border-color: var(--el-color-danger);
  transform: translateY(-1px);
}
.wb-reminder-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.wb-reminder-left .el-icon {
  font-size: var(--fs-2xl);
  color: var(--el-color-danger);
}
.wb-reminder-text {
  display: flex;
  flex-direction: column;
}
.wb-reminder-title {
  font-size: var(--fs-md);
  font-weight: 600;
  color: var(--text-primary);
}
.wb-reminder-desc {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  margin-top: 2px;
}
.wb-reminder-desc strong {
  color: var(--el-color-danger);
}
.wb-reminder-action {
  font-size: var(--fs-sm);
  color: var(--el-color-primary);
  font-weight: 500;
}

.mb-16 {
  margin-bottom: 16px;
}
.mb-24 {
  margin-bottom: 24px;
}
.text-danger {
  color: var(--danger-color) !important;
}
.text-warning {
  color: var(--warning-color) !important;
}
.font-medium {
  font-weight: 500;
}

.pending-stats-row {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}
.pending-stat-card {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 18px;
  background: var(--bg-card);
  border: 0.5px solid var(--border-color);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s;
}
.pending-stat-card:hover {
  border-color: var(--primary-color);
  transform: translateY(-1px);
}
.pending-stat-num {
  font-size: 22px;
  font-weight: 700;
  color: var(--primary-color);
}
.pending-stat-label {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
}
.pending-stat-card .el-icon {
  font-size: 22px;
  color: var(--text-disabled, var(--text-disabled));
  margin-left: auto;
}

@media (max-width: 768px) {
  .pending-stats-row {
    flex-direction: column;
  }
}

/* 质量预警卡片 */
.quality-alerts-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 24px;
}
.quality-alert-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border-radius: 8px;
  border: 0.5px solid var(--el-color-warning);
  background: var(--bg-warning-light);
  cursor: pointer;
  transition: all 0.15s;
}
.quality-alert-card:hover {
  border-color: var(--el-color-warning);
  background: var(--bg-warning-light);
}
.qa-card-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.qa-card-icon {
  font-size: var(--fs-lg);
}
.qa-card-title {
  font-size: var(--fs-sm);
  font-weight: 600;
  color: var(--text-primary);
}
.qa-card-desc {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  margin-top: 2px;
}
.qa-card-action {
  font-size: var(--fs-xs);
  color: var(--primary-color);
  font-weight: 500;
  white-space: nowrap;
}

@media (max-width: 1024px) {
  .todo-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
    padding: 12px;
  }
  .filter-hint {
    font-size: var(--fs-xs);
  }
  .todo-grid {
    grid-template-columns: 1fr;
  }
}
</style>

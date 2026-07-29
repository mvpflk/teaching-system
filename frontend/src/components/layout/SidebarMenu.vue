<template>
  <el-menu
    :default-active="activeMenu"
    :collapse="collapse"
    :collapse-transition="false"
    router
    class="sidebar-menu"
    @select="onSelect"
  >
    <el-menu-item index="/home">
      <el-icon><House /></el-icon>
      <template #title>首页</template>
    </el-menu-item>

    <el-menu-item v-if="isTeacher || isAdmin" index="/teacher/agent/chat">
      <el-icon><ChatDotRound /></el-icon>
      <template #title>AI 对话助手</template>
    </el-menu-item>
    <el-menu-item v-if="isSuperAdmin" index="/system-management/ai-config">
      <el-icon><Setting /></el-icon>
      <template #title>AI 模型配置</template>
    </el-menu-item>
    <el-menu-item v-else-if="isTeacher || isAdmin || isStudent" index="/my/api-keys">
      <el-icon><Key /></el-icon>
      <template #title>API Key 配置</template>
    </el-menu-item>

    <el-sub-menu v-if="isTeacher || isAdmin" index="tasks">
      <template #title>
        <el-icon><List /></el-icon>
        <span>任务中心</span>
      </template>
      <el-menu-item index="/teacher/tasks/list">任务管理</el-menu-item>
      <el-menu-item index="/teacher/tasks/question-bank">题库管理</el-menu-item>
      <el-menu-item index="/teacher/tasks/library">共享试卷</el-menu-item>
      <el-menu-item index="/teacher/tasks/share">我的分享</el-menu-item>
      <el-menu-item v-if="enabled('feature.template_enabled')" index="/teacher/tasks/templates">
        任务模板
      </el-menu-item>
      <el-menu-item v-if="enabled('feature.review_enabled')" index="/teacher/tasks/pending-review">
        待审核
      </el-menu-item>
      <el-menu-item
        v-if="enabled('feature.ai_content_enabled')"
        index="/teacher/tasks/ai-assistant"
      >
        AI 教学助手
      </el-menu-item>
      <el-menu-item
        v-if="enabled('feature.ai_content_enabled') && showAiCultureModules"
        index="/teacher/ai/exam-paper"
      >
        <el-icon><Tickets /></el-icon><span>AI 智能组卷</span>
      </el-menu-item>
      <el-menu-item
        v-if="enabled('feature.ai_content_enabled') && showAiCultureModules"
        index="/teacher/ai/diagnosis"
      >
        <el-icon><DataAnalysis /></el-icon><span>AI 批改诊断</span>
      </el-menu-item>
      <el-menu-item
        v-if="enabled('feature.ai_content_enabled')"
        index="/teacher/quality/comparison"
      >
        <el-icon><Histogram /></el-icon><span>质量分析</span>
      </el-menu-item>
      <el-menu-item index="/teacher/alerts">
        <el-icon><Bell /></el-icon>
        <template #title>学业预警</template>
      </el-menu-item>
    </el-sub-menu>

    <!-- ── 学生端：学习中心 ── -->
    <el-sub-menu v-if="isStudent" index="student-study">
      <template #title>
        <el-icon><Reading /></el-icon>
        <span>学习中心</span>
      </template>
      <el-menu-item index="/student/tasks">
        我的任务
        <el-badge
          v-if="badgeData.value > 0"
          :value="badgeData.value"
          :class="['menu-badge', 'badge-' + badgeData.type]"
        />
      </el-menu-item>
      <el-menu-item v-if="enabled('feature.knowledge_base')" index="/knowledge-base/discover">
        知识库
      </el-menu-item>
      <el-menu-item index="/wrong-book">
        错题本
        <el-badge
          v-if="wrongBookUnmastered > 0"
          :value="wrongBookUnmastered"
          :max="99"
          class="menu-badge badge-warning"
        />
      </el-menu-item>
    </el-sub-menu>

    <!-- ── 学生端：提分中心 ── -->
    <el-sub-menu v-if="isStudent" index="student-boost">
      <template #title>
        <el-icon><TrendCharts /></el-icon>
        <span>提分中心</span>
      </template>
      <el-menu-item v-if="canAccessRemedial" index="/student/precision">偏科提分</el-menu-item>
      <el-menu-item v-if="enabled('feature.checkpoint_enabled')" index="/student/checkpoint">
        闯关学习
      </el-menu-item>
      <el-menu-item
        v-if="enabled('feature.checkpoint_enabled')"
        index="/student/checkpoint/memory-cards/0"
      >
        我的记忆卡
        <el-badge
          v-if="unreviewedCardCount > 0"
          :value="unreviewedCardCount"
          :max="99"
          class="menu-badge badge-warning"
        />
      </el-menu-item>
    </el-sub-menu>

    <!-- ── 学生端：成长中心 ── -->
    <el-sub-menu v-if="isStudent" index="student-growth">
      <template #title>
        <el-icon><TrophyBase /></el-icon>
        <span>成长中心</span>
      </template>
      <el-menu-item index="/student/growth">我的成长</el-menu-item>
      <el-menu-item v-if="enabled('feature.credit_enabled')" index="/credit/index">
        我的积分
      </el-menu-item>
      <el-menu-item
        v-if="enabled('feature.credit_enabled') && enabled('feature.shop_enabled')"
        index="/credit/shop"
      >
        积分商城
      </el-menu-item>
      <el-menu-item v-if="enabled('feature.credit_enabled')" index="/credit/ranking">
        积分排行
      </el-menu-item>
      <el-menu-item v-if="enabled('feature.credit_enabled')" index="/credit/moral-rank">
        德育积分榜
      </el-menu-item>
    </el-sub-menu>

    <!-- ── 教师/管理端知识库 ── -->
    <el-menu-item
      v-if="(isTeacher || isAdmin) && enabled('feature.knowledge_base')"
      index="/knowledge-base/admin/articles"
    >
      <el-icon><Reading /></el-icon>
      <template #title>知识库管理</template>
    </el-menu-item>

    <el-menu-item
      v-if="(isTeacher || isAdmin) && enabled('feature.knowledge_base')"
      index="/knowledge-base/admin/card-review"
    >
      <el-icon><Checked /></el-icon>
      <template #title>卡片审核</template>
    </el-menu-item>

    <!-- ── 学生端：桌面专属入口（移动端自动隐藏）── -->
    <el-menu-item v-if="isStudent" class="desktop-only" index="/student/my-class">
      <el-icon><School /></el-icon>
      <template #title>我的班级</template>
    </el-menu-item>
    <el-menu-item v-if="isStudent" class="desktop-only" index="/classroom/student-entry">
      <el-icon><Monitor /></el-icon>
      <template #title>课堂互动</template>
    </el-menu-item>
    <el-menu-item v-if="isStudent" class="desktop-only" index="/typing">
      <el-icon><Key /></el-icon>
      <template #title>打字练习</template>
    </el-menu-item>
    <el-menu-item
      v-if="isStudent && enabled('feature.training_center')"
      class="desktop-only"
      index="/student/training"
    >
      <el-icon><Monitor /></el-icon>
      <template #title>实训中心</template>
    </el-menu-item>
    <el-menu-item
      v-if="isStudent && enabled('feature.re_review_enabled')"
      class="desktop-only"
      index="/student/peer-reviews"
    >
      <el-icon><ChatLineSquare /></el-icon>
      <template #title>互评任务</template>
    </el-menu-item>

    <el-sub-menu v-if="isParent && enabled('feature.parent_enabled')" index="parent">
      <template #title>
        <el-icon><UserFilled /></el-icon>
        <span>我的孩子</span>
      </template>
      <el-menu-item index="/parent/children">孩子列表</el-menu-item>
    </el-sub-menu>

    <el-menu-item index="/showcase">
      <el-icon><Medal /></el-icon>
      <template #title>作品展示墙</template>
    </el-menu-item>

    <el-menu-item v-if="enabled('feature.bbs_enabled')" index="/bbs">
      <el-icon><ChatDotSquare /></el-icon>
      <template #title>师生论坛</template>
    </el-menu-item>

    <el-menu-item index="/notification">
      <el-icon><Bell /></el-icon>
      <template #title>
        消息通知
        <el-badge
          v-if="notifStore.unreadCount > 0"
          :value="notifStore.unreadCount"
          class="menu-badge badge-normal"
        />
      </template>
    </el-menu-item>

    <el-menu-item
      v-if="(isTeacher || isParent) && enabled('feature.message_enabled')"
      index="/messages"
    >
      <el-icon><ChatDotSquare /></el-icon>
      <template #title>家校消息</template>
    </el-menu-item>

    <el-sub-menu
      v-if="(isInspector || isAdmin || isRegionAdmin) && enabled('feature.inspector_enabled')"
      index="inspector"
    >
      <template #title>
        <el-icon><Monitor /></el-icon>
        <span>巡视监督</span>
      </template>
      <el-menu-item index="/inspector/dashboard">监督面板</el-menu-item>
      <el-menu-item index="/inspector/exams">任务分析</el-menu-item>
      <el-menu-item index="/inspector/teachers">教师活跃度</el-menu-item>
      <el-menu-item index="/inspector/classes">班级对比</el-menu-item>
      <el-menu-item index="/inspector/records">巡视记录</el-menu-item>
      <el-menu-item index="/inspector/issues">问题台账</el-menu-item>
      <el-menu-item index="/inspector/notices">整改通知</el-menu-item>
      <el-menu-item index="/inspector/reports">巡视报告</el-menu-item>
      <el-menu-item index="/inspector/alerts">预警中心</el-menu-item>
      <el-menu-item index="/inspector/classroom-patrols">课堂巡课</el-menu-item>
      <el-menu-item index="/inspector/moral-inspections">德育巡视</el-menu-item>
      <el-menu-item index="/inspector/research-activities">教研活动</el-menu-item>
      <el-menu-item index="/inspector/review-monitor">审核监控</el-menu-item>
      <el-menu-item index="/inspector/parent-feedback">家长反馈</el-menu-item>
      <el-menu-item index="/inspector/ai-assistant">🤖 AI巡视助手</el-menu-item>
      <el-menu-item index="/inspector/practice-monitor">📊 实训监控</el-menu-item>
    </el-sub-menu>

    <el-sub-menu v-if="isAdmin" index="teacher">
      <template #title>
        <el-icon><UserFilled /></el-icon>
        <span>教师管理</span>
      </template>
      <el-menu-item index="/teacher/list">教师列表</el-menu-item>
    </el-sub-menu>

    <el-sub-menu v-if="isTeacher" index="class">
      <template #title>
        <el-icon><School /></el-icon>
        <span>班级管理</span>
      </template>
      <el-menu-item v-if="isTeacher" index="/class/list">班级列表</el-menu-item>
      <el-menu-item v-if="isTeacher" index="/class/students">学生管理</el-menu-item>
      <el-menu-item v-if="isTeacher" index="/teacher/groups">分组管理</el-menu-item>
    </el-sub-menu>

    <!-- 更多工具 -->
    <el-sub-menu v-if="isTeacher" index="more-tools">
      <template #title>
        <el-icon><MoreFilled /></el-icon>
        <span>更多工具</span>
      </template>
      <el-menu-item index="/teacher/typing/monitor">竞赛驾驶舱</el-menu-item>
      <el-menu-item index="/teacher/typing/competitions">竞赛管理</el-menu-item>
      <el-menu-item index="/teacher/wrong-monitor">错题监督</el-menu-item>
      <el-menu-item v-if="canAccessRemedial" index="/teacher/precision-monitor">
        偏科监督
      </el-menu-item>
      <el-menu-item v-if="enabled('feature.training_center')" index="/training">
        <el-icon><Monitor /></el-icon>
        <template #title>实训中心</template>
      </el-menu-item>
      <el-menu-item index="/teacher/notices">我的通知书</el-menu-item>
    </el-sub-menu>

    <!-- 管理员仍看到完整菜单 -->
    <el-sub-menu v-if="isAdmin" index="typing-admin">
      <template #title>
        <el-icon><Key /></el-icon>
        <span>打字竞赛</span>
      </template>
      <el-menu-item index="/teacher/typing/monitor">竞赛驾驶舱</el-menu-item>
      <el-menu-item index="/teacher/typing/competitions">竞赛管理</el-menu-item>
      <el-menu-item index="/teacher/wrong-monitor">错题监督</el-menu-item>
      <el-menu-item v-if="canAccessRemedial" index="/teacher/precision-monitor">
        偏科监督
      </el-menu-item>
      <el-menu-item v-if="enabled('feature.training_center')" index="/training">
        <el-icon><Monitor /></el-icon>
        <template #title>实训中心</template>
      </el-menu-item>
    </el-sub-menu>

    <el-menu-item v-if="isAdmin && enabled('feature.training_center')" index="/training">
      <el-icon><Monitor /></el-icon>
      <template #title>实训中心</template>
    </el-menu-item>

    <el-menu-item v-if="isTeacher" index="/teacher/rectification">
      <el-icon><Finished /></el-icon>
      <template #title>我的整改</template>
    </el-menu-item>
    <el-menu-item v-if="isAdmin" index="/teacher/notices">
      <el-icon><Notification /></el-icon>
      <template #title>我的通知书</template>
    </el-menu-item>
    <el-menu-item
      v-if="isTeacher && isTeachingGroupLeader && enabled('feature.research_enabled')"
      index="/teacher/research"
    >
      <el-icon><Document /></el-icon>
      <template #title>教研工作台</template>
    </el-menu-item>
    <el-menu-item
      v-if="isTeacher && isTeachingGroupLeader && enabled('feature.research_enabled')"
      index="/teacher/research/baseline"
    >
      <el-icon><DataAnalysis /></el-icon>
      <template #title>基线快照</template>
    </el-menu-item>
    <el-menu-item v-if="isTeacher && isLessonPrepGroupLeader" index="/teacher/lesson-prep">
      <el-icon><Edit /></el-icon>
      <template #title>备课工作台</template>
    </el-menu-item>
    <el-menu-item
      v-if="(isAdmin || isInspector) && enabled('feature.research_enabled')"
      index="/teacher/activity"
    >
      <el-icon><Timer /></el-icon>
      <template #title>教师行为日志</template>
    </el-menu-item>
    <el-menu-item v-if="isAdmin || isSuperAdmin" index="/teacher/analytics/usage">
      <el-icon><DataAnalysis /></el-icon>
      <template #title>功能使用分析</template>
    </el-menu-item>
    <el-menu-item v-if="isSuperAdmin" index="/system-management">
      <el-icon><Tools /></el-icon>
      <template #title>系统管理中心</template>
    </el-menu-item>
  </el-menu>
</template>

<script setup>
import { watch, onMounted, ref, onUnmounted } from 'vue';
import { Tickets, DataAnalysis, Histogram, Timer, Checked, Setting, Key } from '@element-plus/icons-vue';
import { usePendingCountStore } from '@/stores/pendingCount';
import { useNotificationStore } from '@/stores/notification';
import { getStudentStats } from '@/api/wrong';
import { getUnreviewedCount } from '@/api/checkpoint';

const props = defineProps({
  activeMenu: String,
  collapse: Boolean,
  isTeacher: Boolean,
  isStudent: Boolean,
  isAdmin: Boolean,
  isInspector: Boolean,
  isSuperAdmin: Boolean,
  isRegionAdmin: { type: Boolean, default: false },
  isParent: Boolean,
  isHeadTeacher: Boolean,
  isTeachingGroupLeader: Boolean,
  isLessonPrepGroupLeader: Boolean,
  showPracticePlans: Boolean,
  showAiCultureModules: Boolean,
  canAccessRemedial: Boolean,
  features: { type: Object, default: () => ({}) },
});
const emit = defineEmits(['select']);
const onSelect = () => {
  emit('select');
};

const pendingStore = usePendingCountStore();
const { badgeData } = pendingStore;
const notifStore = useNotificationStore();

const _wrongBookTimer = ref(null);
const wrongBookUnmastered = ref(0);
const _memoryCardTimer = ref(null);
const unreviewedCardCount = ref(0);

function startWrongBookPolling() {
  if (_wrongBookTimer.value) return;
  const fetch = async () => {
    try {
      const res = await getStudentStats();
      if (res.code === 200) wrongBookUnmastered.value = res.data.unmastered || 0;
    } catch {
      /* 静默失败 */
    }
  };
  fetch();
  _wrongBookTimer.value = setInterval(fetch, 120000);
}

function startMemoryCardPolling() {
  if (_memoryCardTimer.value) return;
  const fetch = async () => {
    try {
      const res = await getUnreviewedCount();
      if (res.code === 200) unreviewedCardCount.value = res.data.count || 0;
    } catch {
      /* 静默失败 */
    }
  };
  fetch();
  _memoryCardTimer.value = setInterval(fetch, 120000);
}

function stopWrongBookPolling() {
  if (_wrongBookTimer.value) {
    clearInterval(_wrongBookTimer.value);
    _wrongBookTimer.value = null;
  }
}
function stopMemoryCardPolling() {
  if (_memoryCardTimer.value) {
    clearInterval(_memoryCardTimer.value);
    _memoryCardTimer.value = null;
  }
}

watch(
  () => props.isStudent,
  (v) => {
    if (v) {
      pendingStore.startPolling(300000);
      startWrongBookPolling();
      startMemoryCardPolling();
    } else {
      pendingStore.stopPolling();
      stopWrongBookPolling();
      stopMemoryCardPolling();
    }
  }
);
onMounted(() => {
  if (props.isStudent) {
    pendingStore.startPolling(300000);
    startWrongBookPolling();
    startMemoryCardPolling();
  }
});
onUnmounted(() => {
  stopWrongBookPolling();
  stopMemoryCardPolling();
});

/** 功能开关：无 key 的菜单始终显示，有 key 的仅在值为 true 时显示 */
const enabled = (key) => {
  const val = props.features[key];
  return val === true || val === undefined;
};
</script>

<style scoped>
/* 红点颜色分级 */
.badge-urgent :deep(.el-badge__content) {
  background-color: var(--el-color-danger, #f56c6c);
  animation: badgePulse 1.5s ease-in-out infinite;
}
.badge-warning :deep(.el-badge__content) {
  background-color: var(--el-color-warning, #e6a23c);
}
.badge-normal :deep(.el-badge__content) {
  background-color: var(--text-placeholder, #c0c4cc);
}
@keyframes badgePulse {
  0%,
  100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.2);
  }
}

@media (max-width: 768px) {
  :deep(.el-form-item .el-input),
  :deep(.el-form-item .el-select) {
    width: 100%;
  }
  /* 桌面专属菜单项在移动端隐藏 */
  .desktop-only {
    display: none !important;
  }
}
</style>

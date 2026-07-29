<template>
  <div class="mobile-bottom-nav">
    <!-- 学生: 首页 / 学习 / 提分 / 成长 / 我的 -->
    <template v-if="isStudent && !isAdmin">
      <div class="nav-item" :class="{ active: route.path === '/home' }" @click="go('/home')">
        <el-icon size="20"><House /></el-icon><span>首页</span>
      </div>
      <div class="nav-item" :class="{ active: activeLearning }" @click="go('/student/tasks')">
        <el-badge
          :value="badgeData.value"
          :hidden="!badgeData.value"
          :max="99"
          :class="'badge-' + badgeData.type"
        >
          <el-icon size="20"><Reading /></el-icon>
        </el-badge><span>学习</span>
      </div>
      <div class="nav-item" :class="{ active: activeBoost }" @click="go('/student/precision')">
        <el-icon size="20"><TrendCharts /></el-icon><span>提分</span>
      </div>
      <div
        class="nav-item"
        :class="{
          active: route.path.startsWith('/student/growth') || route.path.startsWith('/credit'),
        }"
        @click="go('/student/growth')"
      >
        <el-icon size="20"><TrophyBase /></el-icon><span>成长</span>
      </div>
      <div class="nav-item" :class="{ active: route.path === '/profile' }" @click="go('/profile')">
        <el-icon size="20"><User /></el-icon><span>我的</span>
      </div>
    </template>

    <!-- 教师(非管理): 首页 / 班级 / BBS / 通知 / 我的 -->
    <template v-else-if="isTeacher">
      <div class="nav-item" :class="{ active: route.path === '/home' }" @click="go('/home')">
        <el-icon size="20"><House /></el-icon><span>首页</span>
      </div>
      <div
        class="nav-item"
        :class="{ active: route.path.startsWith('/class') }"
        @click="go('/class/list')"
      >
        <el-icon size="20"><School /></el-icon><span>班级</span>
      </div>
      <div class="nav-item" :class="{ active: route.path.startsWith('/bbs') }" @click="go('/bbs')">
        <el-icon size="20"><ChatDotSquare /></el-icon><span>论坛</span>
      </div>
      <div
        class="nav-item"
        :class="{ active: route.path === '/notification' }"
        @click="go('/notification')"
      >
        <el-badge
          :value="notifStore.unreadCount"
          :hidden="!notifStore.unreadCount"
          :max="99"
          class="badge-normal"
        >
          <el-icon size="20"><Bell /></el-icon>
        </el-badge><span>通知</span>
      </div>
      <div class="nav-item" :class="{ active: route.path === '/profile' }" @click="go('/profile')">
        <el-icon size="20"><User /></el-icon><span>我的</span>
      </div>
    </template>

    <!-- 管理员: 首页 / 巡视 / 通知 / BBS / 我的 -->
    <template v-else-if="isAdmin">
      <div class="nav-item" :class="{ active: route.path === '/home' }" @click="go('/home')">
        <el-icon size="20"><House /></el-icon><span>首页</span>
      </div>
      <div
        class="nav-item"
        :class="{ active: route.path.startsWith('/inspector') }"
        @click="go('/inspector/dashboard')"
      >
        <el-icon size="20"><Monitor /></el-icon><span>巡视</span>
      </div>
      <div
        class="nav-item"
        :class="{ active: route.path === '/notification' }"
        @click="go('/notification')"
      >
        <el-badge
          :value="notifStore.unreadCount"
          :hidden="!notifStore.unreadCount"
          :max="99"
          class="badge-normal"
        >
          <el-icon size="20"><Bell /></el-icon>
        </el-badge><span>通知</span>
      </div>
      <div class="nav-item" :class="{ active: route.path.startsWith('/bbs') }" @click="go('/bbs')">
        <el-icon size="20"><ChatDotSquare /></el-icon><span>论坛</span>
      </div>
      <div class="nav-item" :class="{ active: route.path === '/profile' }" @click="go('/profile')">
        <el-icon size="20"><User /></el-icon><span>我的</span>
      </div>
    </template>

    <!-- 巡视员(非管理): 首页 / 巡视 / 通知 / BBS / 我的 -->
    <template v-else-if="isInspector">
      <div class="nav-item" :class="{ active: route.path === '/home' }" @click="go('/home')">
        <el-icon size="20"><House /></el-icon><span>首页</span>
      </div>
      <div
        class="nav-item"
        :class="{ active: route.path.startsWith('/inspector') }"
        @click="go('/inspector/dashboard')"
      >
        <el-icon size="20"><Monitor /></el-icon><span>巡视</span>
      </div>
      <div
        class="nav-item"
        :class="{ active: route.path === '/notification' }"
        @click="go('/notification')"
      >
        <el-badge
          :value="notifStore.unreadCount"
          :hidden="!notifStore.unreadCount"
          :max="99"
          class="badge-normal"
        >
          <el-icon size="20"><Bell /></el-icon>
        </el-badge><span>通知</span>
      </div>
      <div class="nav-item" :class="{ active: route.path.startsWith('/bbs') }" @click="go('/bbs')">
        <el-icon size="20"><ChatDotSquare /></el-icon><span>论坛</span>
      </div>
      <div class="nav-item" :class="{ active: route.path === '/profile' }" @click="go('/profile')">
        <el-icon size="20"><User /></el-icon><span>我的</span>
      </div>
    </template>

    <!-- 家长: 首页 / 孩子 / 通知 / 我的 -->
    <template v-else-if="isParent">
      <div class="nav-item" :class="{ active: route.path === '/home' }" @click="go('/home')">
        <el-icon size="20"><House /></el-icon><span>首页</span>
      </div>
      <div
        class="nav-item"
        :class="{ active: route.path.startsWith('/parent') }"
        @click="go('/parent/children')"
      >
        <el-icon size="20"><UserFilled /></el-icon><span>孩子</span>
      </div>
      <div
        class="nav-item"
        :class="{ active: route.path === '/notification' }"
        @click="go('/notification')"
      >
        <el-badge
          :value="notifStore.unreadCount"
          :hidden="!notifStore.unreadCount"
          :max="99"
          class="badge-normal"
        >
          <el-icon size="20"><Bell /></el-icon>
        </el-badge><span>通知</span>
      </div>
      <div class="nav-item" :class="{ active: route.path === '/profile' }" @click="go('/profile')">
        <el-icon size="20"><User /></el-icon><span>我的</span>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { usePendingCountStore } from '@/stores/pendingCount';
import { useNotificationStore } from '@/stores/notification';

const route = useRoute();
const router = useRouter();

const props = defineProps({
  isTeacher: Boolean,
  isStudent: Boolean,
  isAdmin: Boolean,
  isParent: Boolean,
  isInspector: Boolean,
});

const emit = defineEmits(['navigate']);

const pendingStore = usePendingCountStore();
const { badgeData } = pendingStore;
const notifStore = useNotificationStore();

/** 学习Tab激活范围：任务/错题本/知识库/衍生练习 */
const activeLearning = computed(() =>
  ['/student/tasks', '/wrong-book', '/knowledge-base', '/student/derived-practice'].some((p) =>
    route.path.startsWith(p)
  )
);
/** 提分Tab激活范围：偏科提分/闯关学习/记忆卡/英语学习 */
const activeBoost = computed(() =>
  ['/student/precision', '/student/checkpoint', '/precision/english'].some((p) =>
    route.path.startsWith(p)
  )
);

onMounted(() => {
  if (props.isStudent) pendingStore.startPolling(60000);
});

const go = (path) => {
  emit('navigate');
  router.push(path);
};
</script>

<style scoped>
.mobile-bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 999;
  display: flex;
  justify-content: space-around;
  align-items: center;
  height: 56px;
  /* B6 新增：毛玻璃质感（与 iOS/微信小程序一致），不支持则降级纯色 */
  background: rgba(255, 255, 255, 0.85);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  backdrop-filter: blur(20px) saturate(180%);
  border-top: 0.5px solid var(--border-light);
  padding-bottom: env(safe-area-inset-bottom, 0);
}
@supports not ((backdrop-filter: blur(20px)) or (-webkit-backdrop-filter: blur(20px))) {
  .mobile-bottom-nav {
    background: var(--bg-card); /* 降级 */
  }
}
.dark .mobile-bottom-nav {
  background: rgba(30, 32, 53, 0.85);
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  padding: 4px 8px;
  /* B6 修复：10px 小于最低可识别字号 → 11px + 字重 500 */
  font-size: 11px;
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  min-width: 48px;
  position: relative;
  transition:
    transform var(--transition-fast),
    color var(--transition-fast);
  -webkit-tap-highlight-color: transparent;
}
.nav-item:active {
  transform: scale(0.96);
} /* 触摸按压反馈 */
.nav-item :deep(.el-icon) {
  transition:
    transform var(--transition-spring),
    color var(--transition-fast);
}
.nav-item.active {
  color: var(--primary-color);
}
/* B6 新增：激活态小圆点指示器 */
.nav-item.active::after {
  content: '';
  position: absolute;
  bottom: 2px;
  left: 50%;
  transform: translateX(-50%);
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--primary-color);
  box-shadow: 0 0 6px rgba(67, 97, 238, 0.4);
}
.nav-item.active :deep(.el-icon) {
  transform: scale(1.1);
}

.nav-item :deep(.el-badge__content) {
  font-size: 10px;
  height: 16px;
  line-height: 16px;
  padding: 0 4px;
}
.badge-urgent :deep(.el-badge__content) {
  background-color: var(--danger-color);
  animation: badgePulse 1.5s ease-in-out infinite;
}
.badge-warning :deep(.el-badge__content) {
  background-color: var(--warning-color);
}
.badge-normal :deep(.el-badge__content) {
  background-color: var(--info-color);
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
/* reduced-motion：徽章不跳动，激活无缩放，按压无变换 */
@media (prefers-reduced-motion: reduce) {
  .nav-item,
  .nav-item :deep(.el-icon) {
    transition: none !important;
  }
  .nav-item:active,
  .nav-item.active :deep(.el-icon) {
    transform: none !important;
  }
  .badge-urgent :deep(.el-badge__content) {
    animation: none !important;
  }
}

@media (max-width: 768px) {
  :deep(.el-form-item .el-input),
  :deep(.el-form-item .el-select) {
    width: 100%;
  }
}
</style>

<template>
  <div class="system-center">
    <div class="page-header">
      <h3 class="page-title">⚙️ 系统管理中心</h3>
      <span class="page-subtitle">超级管理员专属</span>
    </div>

    <el-container class="center-container">
      <!-- 左侧垂直导航 -->
      <el-aside class="center-aside" width="200px">
        <el-menu
          :default-active="activeMenu"
          router
          class="center-menu"
          @select="onMenuSelect"
        >
          <el-menu-item index="/system-management/overview">
            <el-icon><DataAnalysis /></el-icon>
            <span>总览看板</span>
          </el-menu-item>
          <el-menu-item index="/system-management/basic">
            <el-icon><Setting /></el-icon>
            <span>基础设置</span>
          </el-menu-item>
          <el-menu-item index="/system-management/dynamic-params">
            <el-icon><Operation /></el-icon>
            <span>动态参数</span>
          </el-menu-item>
          <el-menu-item index="/system-management/credit-rules">
            <el-icon><Coin /></el-icon>
            <span>积分规则</span>
          </el-menu-item>
          <el-menu-item index="/system-management/audit-logs">
            <el-icon><Document /></el-icon>
            <span>审计日志</span>
          </el-menu-item>
          <el-menu-item index="/system-management/feedback">
            <el-icon><ChatLineSquare /></el-icon>
            <span>用户反馈</span>
          </el-menu-item>
          <el-menu-item index="/system-management/monitor">
            <el-icon><Monitor /></el-icon>
            <span>系统监控</span>
          </el-menu-item>
          <el-menu-item index="/system-management/maintenance">
            <el-icon><Tools /></el-icon>
            <span>系统运维</span>
          </el-menu-item>
          <el-menu-item index="/system-management/graduation">
            <el-icon><TrophyBase /></el-icon>
            <span>毕业管理</span>
          </el-menu-item>
          <el-menu-item index="/system-management/class-type-config">
            <el-icon><CollectionTag /></el-icon>
            <span>班级类型配置</span>
          </el-menu-item>
          <el-menu-item index="/system-management/terms">
            <el-icon><Calendar /></el-icon>
            <span>学期管理</span>
          </el-menu-item>
          <el-menu-item index="/system-management/remedial-class-config">
            <el-icon><TrendCharts /></el-icon>
            <span>偏科提分配置</span>
          </el-menu-item>
          <el-menu-item index="/system-management/teaching-groups">
            <el-icon><UserFilled /></el-icon>
            <span>教研备课</span>
          </el-menu-item>
          <el-menu-item index="/system-management/ai-config">
            <el-icon><MagicStick /></el-icon>
            <span>AI 配置</span>
          </el-menu-item>
          <el-menu-item index="/system-management/category-manager">
            <el-icon><CollectionTag /></el-icon>
            <span>知识点分类管理</span>
          </el-menu-item>
          <el-menu-item index="/system-management/exam-syllabus">
            <el-icon><Collection /></el-icon>
            <span>考纲管理</span>
          </el-menu-item>
          <el-menu-item index="/system-management/typing-settings">
            <el-icon><Setting /></el-icon>
            <span>打字功能配置</span>
          </el-menu-item>
          <el-menu-item index="/system-management/typing-texts">
            <el-icon><EditPen /></el-icon>
            <span>打字文本管理</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- 右侧内容区 -->
      <el-main class="center-main">
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();
const activeMenu = computed(() => route.path);

const onMenuSelect = () => {};
</script>

<style scoped lang="scss">
.system-center {
  margin: 0 auto;
  padding: var(--spacing-lg, 24px);
}

.page-header {
  margin-bottom: 24px;
  .page-title {
    font-size: var(--fs-2xl, 22px);
    color: var(--text-primary);
    margin: 0 0 4px;
  }
  .page-subtitle {
    font-size: var(--fs-sm, 13px);
    color: var(--text-secondary);
  }
}

.center-container {
  background: var(--bg-card, var(--bg-card));
  border-radius: var(--radius-lg, 12px);
  box-shadow: var(--shadow-sm);
  min-height: 60vh;
}

.center-aside {
  border-right: 1px solid var(--border-light);
  .center-menu {
    border-right: none;
    height: 100%;
    padding-top: 8px;
  }
}

.center-main {
  padding: var(--spacing-lg, 24px);
  background: var(--bg-section, var(--bg-section));
  border-radius: 0 var(--radius-lg, 12px) var(--radius-lg, 12px) 0;
}

// 移动端：侧边菜单收起到顶部
@media (max-width: 768px) {
  .system-center {
    padding: var(--spacing-md, 16px);
  }
  .center-container {
    flex-direction: column;
  }
  .center-aside {
    width: 100% !important;
    border-right: none;
    border-bottom: 1px solid var(--border-light);
    .center-menu {
      display: flex;
      flex-direction: row;
      overflow-x: auto;
      .el-menu-item {
        flex-shrink: 0;
        padding: 0 14px;
        font-size: var(--fs-sm);
      }
    }
  }
  .center-main {
    border-radius: 0 0 var(--radius-lg) var(--radius-lg) 0;
    padding: var(--spacing-md, 16px);
  }
}
</style>

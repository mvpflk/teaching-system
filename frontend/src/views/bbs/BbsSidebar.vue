<template>
  <aside class="bbs-sidebar">
    <!-- 热门话题 -->
    <div class="sidebar-card">
      <h4 class="sidebar-title">
        <el-icon><TrendCharts /></el-icon> 热门话题
      </h4>
      <div v-if="hotPosts.length === 0" class="sidebar-empty">暂无热门帖子</div>
      <div
        v-for="(t, i) in hotPosts"
        :key="t.id"
        class="hot-item"
        @click="$emit('goDetail', t.id)"
      >
        <span class="hot-rank" :class="'rank-' + (i + 1)">{{ i + 1 }}</span>
        <span class="hot-title">{{ t.title }}</span>
        <span class="hot-count">{{ t.replyCount || 0 }}回复</span>
      </div>
    </div>

    <!-- 活跃用户 -->
    <div class="sidebar-card">
      <h4 class="sidebar-title">
        <el-icon><UserFilled /></el-icon> 活跃用户
      </h4>
      <div v-if="activeUsers.length === 0" class="sidebar-empty">近7天暂无活跃用户</div>
      <div class="user-list">
        <div v-for="u in activeUsers" :key="u.userId" class="user-row">
          <el-avatar :size="28">{{ u.userName?.charAt(0) }}</el-avatar>
          <span class="user-name">{{ u.userName }}</span>
          <span class="user-score">{{ u.activityScore }}分</span>
        </div>
      </div>
    </div>

    <!-- 版块统计 -->
    <div class="sidebar-card">
      <h4 class="sidebar-title">
        <el-icon><DataAnalysis /></el-icon> 版块统计
      </h4>
      <div
        v-for="cat in categories"
        :key="cat.id"
        class="stat-row"
        @click="$emit('switchCategory', cat.id)"
      >
        <span class="stat-emoji">{{ cat.icon || '📋' }}</span>
        <span class="stat-name">{{ cat.name }}</span>
        <span class="stat-count">{{ cat.postCount || 0 }}帖</span>
      </div>
    </div>
  </aside>
</template>

<script setup>
defineProps({
  hotPosts: { type: Array, default: () => [] },
  activeUsers: { type: Array, default: () => [] },
  categories: { type: Array, default: () => [] }
})

defineEmits(['goDetail', 'switchCategory'])
</script>

<style scoped>
.bbs-sidebar { display: flex; flex-direction: column; gap: 16px; }
.sidebar-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 16px;
  border: 0.5px solid var(--border-color);
}
.sidebar-title { font-size: var(--fs-md); font-weight: 600; margin-bottom: 12px; display: flex; align-items: center; gap: 6px; color: var(--text-primary); }
.hot-item { display: flex; align-items: center; gap: 8px; padding: 6px 0; cursor: pointer; border-bottom: 1px solid var(--border-light); }
.hot-item:last-child { border-bottom: none; }
.hot-item:hover { color: var(--primary-color); }
.hot-rank { width: 20px; height: 20px; border-radius: 4px; display: flex; align-items: center; justify-content: center; font-size: var(--fs-xs); font-weight: 600; background: var(--bg-section); color: var(--text-secondary); flex-shrink: 0; }
.hot-rank.rank-1 { background: var(--bg-danger-light); color: var(--el-color-danger); }
.hot-rank.rank-2 { background: var(--bg-warning-light); color: var(--el-color-warning); }
.hot-rank.rank-3 { background: #fff8d0; color: #854f0b; }
.hot-title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: var(--fs-sm); }
.hot-count { font-size: var(--fs-xs); color: var(--text-secondary); flex-shrink: 0; }
.user-list { display: flex; flex-direction: column; gap: 8px; }
.user-row { display: flex; align-items: center; gap: 8px; }
.user-name { font-size: var(--fs-sm); flex: 1; }
.user-score { font-size: var(--fs-xs); color: var(--text-secondary); }
.stat-row { display: flex; align-items: center; gap: 8px; padding: 4px 0; cursor: pointer; font-size: var(--fs-sm); }
.stat-row:hover { color: var(--primary-color); }
.stat-emoji { font-size: var(--fs-md); flex-shrink: 0; }
.stat-name { flex: 1; }
.stat-count { font-size: var(--fs-xs); color: var(--text-secondary); }
.sidebar-empty { text-align: center; padding: 12px; color: var(--text-secondary); font-size: var(--fs-xs); }
</style>

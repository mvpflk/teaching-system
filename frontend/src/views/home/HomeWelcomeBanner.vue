<template>
  <div class="welcome-banner">
    <div class="welcome-left">
      <h2 class="welcome-greeting">{{ greeting }}，{{ realName }}</h2>
      <p v-if="summaryLine" class="welcome-summary">{{ summaryLine }}</p>
    </div>
    <div class="welcome-right">
      <span v-if="isStudent" class="role-badge role-student">学生</span>
      <span v-if="isTeacher" class="role-badge role-teacher">教师</span>
      <span v-if="isAdmin" class="role-badge role-admin">管理员</span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  realName: String,
  greeting: String,
  summaryLine: String,
  isStudent: Boolean,
  isTeacher: Boolean,
  isAdmin: Boolean
})
</script>

<style scoped lang="scss">
.welcome-banner {
  background: var(--bg-card);
  border: 0.5px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 20px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0;

  h2.welcome-greeting { font-size: var(--fs-xl); font-weight: 600; color: var(--text-primary); margin: 0 0 4px; }
  p.welcome-summary { font-size: var(--fs-sm); color: var(--text-secondary); margin: 0; }
}

.role-badge {
  display: inline-flex;
  align-items: center;
  padding: 3px 12px;
  border-radius: var(--radius-sm);
  font-size: var(--fs-xs);
  font-weight: 500;
  &.role-student { background: var(--bg-success-light); color: var(--el-color-success); }
  &.role-teacher { background: var(--primary-light); color: var(--primary-color); }
  &.role-admin { background: var(--bg-danger-light); color: var(--el-color-danger); }
}

@media (max-width: 768px) {
  .welcome-banner { flex-direction: column; align-items: flex-start; gap: 10px; padding: 16px 20px; }
  h2.welcome-greeting { font-size: var(--fs-lg); }
}
</style>

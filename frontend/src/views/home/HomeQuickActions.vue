<template>
  <div class="quick-actions mb-24">
    <span class="actions-label">快捷操作</span>
    <div class="actions-list">
      <div class="action-item" @click="$router.push('/teacher/tasks/create?behavior=HOMEWORK')">
        <div class="action-icon" style="background: var(--primary-light); color: var(--primary-color);">
          <el-icon size="22"><EditPen /></el-icon>
        </div>
        <span>布置作业</span>
      </div>
      <div class="action-item" @click="$router.push('/teacher/tasks/create?behavior=EXAM')">
        <div class="action-icon" style="background: var(--bg-success-light); color: var(--el-color-success);">
          <el-icon size="22"><DocumentCopy /></el-icon>
        </div>
        <span>创建考试</span>
      </div>
      <div class="action-item" @click="$router.push('/bbs')">
        <div class="action-icon" style="background: var(--bg-warning-light); color: var(--el-color-warning);">
          <el-icon size="22"><ChatDotSquare /></el-icon>
        </div>
        <span>师生论坛</span>
      </div>
      <div class="action-item" @click="$router.push('/credit/index')">
        <div class="action-icon" style="background: var(--primary-light); color: var(--primary-color);">
          <el-icon size="22"><Coin /></el-icon>
        </div>
        <span>积分中心</span>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.quick-actions {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 16px 20px;
  border: 0.5px solid var(--border-color);
  display: flex;
  align-items: center;
  gap: 24px;

  .actions-label {
    font-size: var(--fs-sm);
    font-weight: 500;
    color: var(--text-secondary);
    white-space: nowrap;
  }

  .actions-list {
    display: flex;
    gap: 20px;
    flex: 1;
  }

  .action-item {
    display: flex;
    align-items: center;
    gap: 10px;
    cursor: pointer;
    padding: 6px 12px;
    border-radius: var(--radius-md);
    transition: background var(--transition-fast);

    &:hover { background: var(--bg-secondary); }

    .action-icon {
      width: 40px; height: 40px;
      border-radius: var(--radius-md);
      display: flex; align-items: center; justify-content: center;
    }

    span { font-size: var(--fs-sm); color: var(--text-regular); font-weight: 500; }
  }
}

@media (max-width: 768px) {
  .quick-actions {
    flex-direction: column; align-items: stretch; gap: 8px;
    padding: 12px;
    .actions-label { font-size: var(--fs-xs); margin-bottom: 4px; }
    .actions-list {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
    }
    .action-item {
      flex-direction: column; align-items: center; justify-content: center;
      gap: 6px; padding: 14px 8px;
      min-height: 72px;
      background: var(--bg-section);
      border-radius: var(--radius-md);
      transition: background 0.2s;
      &:active { background: var(--primary-light); }
      .action-icon { width: 44px; height: 44px; }
      :deep(.el-icon) { font-size: 28px; }
      span { font-size: var(--fs-sm); text-align: center; }
    }
  }
}
</style>

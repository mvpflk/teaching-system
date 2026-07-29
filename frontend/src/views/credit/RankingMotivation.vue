<template>
  <div class="motivation-footer">
    <p>📣 每次签到 +5积分 · 作业优秀 +5积分 · 考试满分 +50积分 · 论坛发帖 +2积分</p>
    <p class="motivation-sub">连续签到7天以上可获得 ⚡ 闪电加成！快去商城兑换心仪的奖励吧～</p>
  </div>
</template>

<style scoped lang="scss">
.motivation-footer {
  text-align: center; padding: 20px 0 8px;
  border-top: 1px solid var(--border-light); margin-top: 20px;
  p { font-size: var(--fs-sm); color: var(--text-secondary); margin: 4px 0; }
  .motivation-sub { font-size: var(--fs-xs); color: var(--primary-color); }
}
@media (max-width: 768px) {
  .motivation-footer {
    padding: 12px 0 4px; margin-top: 12px;
    p { font-size: var(--fs-xs); }
    .motivation-sub { font-size: var(--fs-xs); }
  }
}
</style>

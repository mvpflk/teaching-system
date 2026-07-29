<template>
  <div class="bottom-grid">
    <!-- 近期任务 -->
    <div class="section-card">
      <div class="section-header">
        <div class="section-header__left">
          <div class="section-icon section-icon--primary">
            <el-icon><List /></el-icon>
          </div>
          <h3 class="section-title">{{ isStudent ? '待完成任务' : '近期任务' }}</h3>
        </div>
        <el-button
          class="view-all-btn"
          size="small"
          type="primary"
          text
          @click="$router.push(isStudent ? '/student/tasks' : '/teacher/tasks')"
        >
          查看全部
          <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>

      <el-table
        v-if="homeworkList.length > 0 && !isMobile"
        :data="homeworkList"
        style="width: 100%"
        size="default"
        :show-header="true"
        class="v2-table"
      >
        <el-table-column prop="title" label="任务标题" min-width="160">
          <template #default="{ row }">
            <span class="task-title-cell">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="className" label="班级" width="120" />
        <el-table-column prop="deadline" label="截止时间" width="160">
          <template #default="{ row }">
            <span :class="{ 'text-danger': isUrgent(row.deadline) }">{{
              formatTime(row.deadline)
            }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              link
              @click="viewDetail(row.id)"
            >
              {{ isStudent ? '去完成' : '查看' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-else-if="homeworkList.length > 0 && isMobile" class="mobile-cards">
        <div
          v-for="h in homeworkList"
          :key="h.id"
          class="mobile-card"
          @click="viewDetail(h.id)"
        >
          <div class="mc-title">{{ h.title }}</div>
          <div class="mc-meta">
            {{ h.className || '' }} ·
            {{ isUrgent(h.deadline) ? '即将截止' : formatTime(h.deadline) }}
          </div>
        </div>
      </div>

      <EmptyState
        v-else
        type="tasks"
        description="暂无任务"
        :image-size="80"
      />
    </div>

    <!-- 积分排行榜 -->
    <div class="section-card">
      <div class="section-header">
        <div class="section-header__left">
          <div class="section-icon section-icon--warning">
            <el-icon><Trophy /></el-icon>
          </div>
          <h3 class="section-title">积分排行榜</h3>
        </div>
        <el-button
          class="view-all-btn"
          size="small"
          type="primary"
          text
          @click="$router.push('/credit/ranking')"
        >
          查看全部
          <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>

      <el-table
        v-if="topStudents.length > 0 && !isMobile"
        :data="topStudents"
        style="width: 100%"
        size="default"
        :show-header="true"
        class="v2-table"
      >
        <el-table-column
          type="index"
          label="排名"
          width="70"
          align="center"
        >
          <template #default="{ $index }">
            <span
              class="rank-badge"
              :class="{
                'rank-badge--gold': $index === 0,
                'rank-badge--silver': $index === 1,
                'rank-badge--bronze': $index === 2,
              }"
            >
              {{ $index + 1 }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="realName" label="姓名" />
        <el-table-column
          prop="totalCredits"
          label="积分"
          width="90"
          align="right"
        >
          <template #default="{ row }">
            <span class="credit-score">{{ row.totalCredits }}</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="titleName"
          label="称号"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-tag
              :type="getTitleType(row.titleLevel)"
              size="small"
              effect="light"
              round
            >
              {{ row.titleName }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div v-else-if="topStudents.length > 0 && isMobile" class="mobile-cards">
        <div v-for="(s, i) in topStudents" :key="i" class="mobile-card rank-card">
          <span
            class="mc-rank"
            :class="{
              'mc-rank--gold': i === 0,
              'mc-rank--silver': i === 1,
              'mc-rank--bronze': i === 2,
            }"
          >#{{ i + 1 }}</span>
          <span class="mc-name">{{ s.realName }}</span>
          <span class="mc-score">{{ s.totalCredits }}分</span>
          <el-tag
            :type="getTitleType(s.titleLevel)"
            size="small"
            effect="light"
            round
          >
            {{ s.titleName }}
          </el-tag>
        </div>
      </div>

      <EmptyState
        v-else
        type="trophy"
        description="暂无排行数据"
        :image-size="80"
      />
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';
import dayjs from 'dayjs';
import { List, Trophy, ArrowRight } from '@element-plus/icons-vue';
import { useIsMobile } from '@/composables/useIsMobile';
import EmptyState from '@/components/common/EmptyState.vue';

const props = defineProps({
  isStudent: Boolean,
  homeworkList: Array,
  topStudents: Array,
});

const router = useRouter();
const { isMobile } = useIsMobile();

const formatTime = (t) => (t ? dayjs(t).format('MM-DD HH:mm') : '-');
const isUrgent = (t) => t && dayjs(t).diff(dayjs(), 'hour') < 24;
const getTitleType = (l) => ['', 'info', 'success', 'warning', 'danger', ''][l] || 'info';

const viewDetail = (id) => {
  if (props.isStudent) router.push(`/student/tasks/${id}`);
  else router.push('/teacher/tasks');
};
</script>

<style scoped>
.bottom-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--ed-bottom-grid-gap, 16px);
}

/* === 卡片容器 === */

.section-card {
  background: var(--ed-section-bg, var(--bg-card));
  border-radius: var(--ed-section-radius, var(--ed-radius-xl, 16px));
  padding: var(--ed-section-padding, 20px 24px);
  border: 1px solid var(--ed-section-border, var(--border-light));
  display: flex;
  flex-direction: column;
}

/* === Section Header === */

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--ed-section-header-mb, 16px);
  padding-bottom: var(--ed-section-header-pb, 14px);
  border-bottom: 1px solid var(--ed-section-header-border, var(--border-lighter));
}

.section-header__left {
  display: flex;
  align-items: center;
  gap: var(--ed-section-icon-gap, 10px);
}

.section-icon {
  width: var(--ed-section-icon-size, 32px);
  height: var(--ed-section-icon-size, 32px);
  border-radius: var(--ed-section-icon-radius, var(--ed-radius-md, 8px));
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}

.section-icon--primary {
  background: var(--ed-section-icon-primary-bg, var(--primary-light));
  color: var(--ed-section-icon-primary-color, var(--primary-color));
}

.section-icon--warning {
  background: var(--ed-section-icon-warning-bg, var(--warning-light));
  color: var(--ed-section-icon-warning-color, var(--warning-color));
}

.section-icon--success {
  background: var(--ed-section-icon-success-bg, var(--success-light));
  color: var(--ed-section-icon-success-color, var(--success-color));
}

.section-title {
  font-size: var(--ed-section-title-size, var(--ed-fs-md, 14px));
  font-weight: var(--ed-section-title-weight, 600);
  color: var(--ed-section-title-color, var(--text-primary));
  margin: 0;
  line-height: 1.4;
}

.view-all-btn {
  font-size: var(--ed-fs-sm, 13px) !important;
  font-weight: var(--ed-fw-medium, 500);
}

/* === 表格增强 === */

.task-title-cell {
  font-weight: var(--ed-fw-medium, 500);
  color: var(--text-primary);
}

.credit-score {
  font-weight: var(--ed-fw-semibold, 600);
  color: var(--warning-color);
  font-variant-numeric: tabular-nums;
}

/* === 排名徽章 === */

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: var(--ed-radius-sm, 6px);
  font-size: var(--ed-fs-xs, 12px);
  font-weight: var(--ed-fw-bold, 700);
  background: var(--bg-secondary);
  color: var(--text-secondary);
}

.rank-badge--gold {
  background: var(
    --ed-rank-gold-bg,
    linear-gradient(135deg, var(--warning-color), var(--warning-light))
  );
  color: var(--ed-rank-gold-text, var(--text-on-primary));
}

.rank-badge--silver {
  background: var(
    --ed-rank-silver-bg,
    linear-gradient(135deg, var(--info-color), var(--info-light))
  );
  color: var(--ed-rank-silver-text, var(--text-on-primary));
}

.rank-badge--bronze {
  background: var(
    --ed-rank-bronze-bg,
    linear-gradient(135deg, var(--danger-color), var(--danger-light))
  );
  color: var(--ed-rank-bronze-text, var(--text-on-primary));
}

/* === 移动端卡片 === */

.mobile-cards {
  display: flex;
  flex-direction: column;
  gap: var(--ed-mobile-card-gap, 8px);
}

.mobile-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: var(--ed-mobile-card-padding, 12px 14px);
  background: var(--ed-mobile-card-bg, var(--bg-secondary));
  border-radius: var(--ed-mobile-card-radius, var(--ed-radius-lg, 12px));
  cursor: pointer;
  flex-wrap: wrap;
  transition: background var(--ed-transition-fast, 0.2s ease);
}

.mobile-card:hover {
  background: var(--ed-mobile-card-hover-bg, var(--bg-section));
}

.mc-title {
  font-size: var(--ed-fs-md, 14px);
  font-weight: var(--ed-fw-medium, 500);
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-primary);
}

.mc-meta {
  font-size: var(--ed-fs-xs, 12px);
  color: var(--text-secondary);
}

.mc-rank {
  font-weight: var(--ed-fw-bold, 700);
  color: var(--primary-color);
  width: 32px;
  font-size: var(--ed-fs-sm, 13px);
}

.mc-rank--gold {
  color: var(--ed-rank-gold-color, var(--warning-color));
}
.mc-rank--silver {
  color: var(--ed-rank-silver-color, var(--info-color));
}
.mc-rank--bronze {
  color: var(--ed-rank-bronze-color, var(--danger-color));
}

.mc-name {
  flex: 1;
  font-size: var(--ed-fs-md, 14px);
  font-weight: var(--ed-fw-medium, 500);
  color: var(--text-primary);
}

.mc-score {
  color: var(--warning-color);
  font-weight: var(--ed-fw-semibold, 600);
  font-size: var(--ed-fs-sm, 13px);
  font-variant-numeric: tabular-nums;
}

.rank-card {
  flex-wrap: nowrap;
}

/* === 响应式 === */

@media (max-width: 768px) {
  .bottom-grid {
    grid-template-columns: 1fr;
  }

  .section-card {
    padding: var(--ed-section-padding-mobile, 16px);
  }
}
</style>

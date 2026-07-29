<template>
  <div class="showcase-container">
    <div class="page-header">
      <h3 class="page-title">
        <el-icon :size="22"><Trophy /></el-icon>
        优秀作品展示墙
      </h3>
      <span class="page-subtitle">向优秀学习，共同进步</span>
    </div>

    <!-- 本周之星 -->
    <div v-if="weeklyStars.length" class="weekly-stars">
      <div class="stars-title">
        <el-icon :size="18"><Trophy /></el-icon>
        本周之星
      </div>
      <div class="stars-list">
        <div
          v-for="(star, i) in weeklyStars"
          :key="star.id"
          class="star-item"
          @click="openDetail(star)"
        >
          <span class="star-rank">{{ i + 1 }}</span>
          <el-avatar :size="36" class="star-avatar">{{ (star.studentName || '?')[0] }}</el-avatar>
          <span class="star-name">{{ star.studentName }}</span>
          <span class="star-title-text">{{ star.title }}</span>
          <span class="star-likes">
            <el-icon :size="14"><StarFilled /></el-icon>
            {{ star.likeCount || 0 }}
          </span>
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-button v-if="isMobile" class="filter-trigger" @click="filterDrawer = true">
        <el-icon><Filter /></el-icon>
        筛选
        <span v-if="activeFilterCount" class="filter-badge">{{ activeFilterCount }}</span>
      </el-button>
      <div v-show="!isMobile" class="filter-row">
        <el-select
          v-model="filters.sourceType"
          placeholder="作品类型"
          clearable
          size="default"
          style="width: 140px"
          @change="onFilterChange"
        >
          <el-option label="全部类型" value="" />
          <el-option label="作业" value="HOMEWORK" />
          <el-option label="考试" value="EXAM" />
          <el-option label="实训" value="PRACTICAL" />
          <el-option label="任务" value="TASK" />
        </el-select>
        <el-select
          v-model="filters.subject"
          placeholder="学科"
          clearable
          size="default"
          style="width: 160px"
          @change="onFilterChange"
        >
          <el-option label="全部学科" value="" />
          <el-option
            v-for="s in subjectOptions"
            :key="s.id"
            :value="s.subjectName"
            :label="s.subjectName"
          />
        </el-select>
        <el-select
          v-model="filters.grade"
          placeholder="年级"
          clearable
          size="default"
          style="width: 130px"
          @change="onGradeChange"
        >
          <el-option label="全部年级" value="" />
          <el-option
            v-for="g in gradeOptions"
            :key="g"
            :value="g"
            :label="g"
          />
        </el-select>
        <el-select
          v-model="filters.classId"
          placeholder="班级"
          clearable
          size="default"
          style="width: 180px"
          @change="onFilterChange"
        >
          <el-option label="全部班级" :value="''" />
          <el-option
            v-for="c in filteredClassOptions"
            :key="c.id"
            :value="c.id"
            :label="(c.grade || '') + c.className"
          />
        </el-select>
      </div>
    </div>

    <!-- 卡片网格 -->
    <ShowcaseGrid :works="works" :loading="loading" @open-detail="openDetail" />

    <!-- 右侧滑出详情面板 -->
    <ShowcaseDetailPanel
      :visible="detailVisible"
      :detail="detail"
      :loading="detailLoading"
      @close="detailVisible = false"
    />

    <!-- 移动端筛选抽屉 -->
    <el-drawer
      v-if="isMobile"
      v-model="filterDrawer"
      direction="btt"
      size="60%"
      title="筛选条件"
    >
      <div class="drawer-filter-row">
        <el-select
          v-model="filters.sourceType"
          placeholder="作品类型"
          clearable
          @change="onFilterChange"
        >
          <el-option label="全部类型" value="" />
          <el-option label="作业" value="HOMEWORK" />
          <el-option label="考试" value="EXAM" />
          <el-option label="实训" value="PRACTICAL" />
          <el-option label="任务" value="TASK" />
        </el-select>
        <el-select
          v-model="filters.subject"
          placeholder="学科"
          clearable
          @change="onFilterChange"
        >
          <el-option label="全部学科" value="" />
          <el-option
            v-for="s in subjectOptions"
            :key="s.id"
            :value="s.subjectName"
            :label="s.subjectName"
          />
        </el-select>
        <el-select
          v-model="filters.grade"
          placeholder="年级"
          clearable
          @change="onGradeChange"
        >
          <el-option label="全部年级" value="" />
          <el-option
            v-for="g in gradeOptions"
            :key="g"
            :value="g"
            :label="g"
          />
        </el-select>
        <el-select
          v-model="filters.classId"
          placeholder="班级"
          clearable
          @change="onFilterChange"
        >
          <el-option label="全部班级" :value="''" />
          <el-option
            v-for="c in filteredClassOptions"
            :key="c.id"
            :value="c.id"
            :label="(c.grade || '') + c.className"
          />
        </el-select>
      </div>
    </el-drawer>

    <!-- 分页 -->
    <div v-if="total > pageSize" class="pagination-wrap">
      <el-pagination
        v-model:current-page="filters.pageNum"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="loadData"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { getShowcaseList, getShowcaseDetail, getWeeklyStars } from '@/api/showcase';
import { getClassList } from '@/api/classes';
import { getSubjects, getGrades } from '@/api/settings';
import { Trophy, Filter } from '@element-plus/icons-vue';
import { useIsMobile } from '@/composables/useIsMobile';
import ShowcaseGrid from './ShowcaseGrid.vue';
import ShowcaseDetailPanel from './ShowcaseDetailPanel.vue';

const loading = ref(false);
const works = ref([]);
const total = ref(0);
const pageSize = 12;
const subjectOptions = ref([]);
const classOptions = ref([]);

const filters = reactive({ pageNum: 1, sourceType: '', subject: '', grade: '', classId: null });
const { isMobile } = useIsMobile();
const filterDrawer = ref(false);
const activeFilterCount = computed(() => {
  let n = 0;
  if (filters.sourceType) n++;
  if (filters.subject) n++;
  if (filters.grade) n++;
  if (filters.classId) n++;
  return n;
});

const gradeOptions = ref([]);
const filteredClassOptions = computed(() => {
  if (!filters.grade) return classOptions.value;
  return classOptions.value.filter((c) => c.grade === filters.grade);
});

const onGradeChange = () => {
  filters.classId = null;
  filters.pageNum = 1;
  loadData();
};

// 本周之星
const weeklyStars = ref([]);

// 详情面板
const detailVisible = ref(false);
const detailLoading = ref(false);
const detail = ref(null);

const openDetail = async (work) => {
  detailVisible.value = true;
  detailLoading.value = true;
  detail.value = null;
  try {
    const detailRes = await getShowcaseDetail(work.id);
    if (detailRes.code === 200) detail.value = detailRes.data;
  } catch {
    /* */
  } finally {
    detailLoading.value = false;
  }
};

const onFilterChange = () => {
  filters.pageNum = 1;
  loadData();
};

const loadData = async () => {
  loading.value = true;
  try {
    const params = { pageNum: filters.pageNum, pageSize };
    if (filters.sourceType) params.sourceType = filters.sourceType;
    if (filters.subject) params.subject = filters.subject;
    if (filters.grade) params.grade = filters.grade;
    if (filters.classId) params.classId = filters.classId;
    const res = await getShowcaseList(params);
    if (res.code === 200) {
      works.value = res.data.records || [];
      total.value = res.data.total || 0;
    }
  } catch {
    /* */
  } finally {
    loading.value = false;
  }
};

onMounted(async () => {
  loadData();
  try {
    const [subRes, clsRes, gradeRes, starsRes] = await Promise.all([
      getSubjects(),
      getClassList(),
      getGrades(),
      getWeeklyStars(),
    ]);
    if (subRes.code === 200) subjectOptions.value = subRes.data || [];
    if (clsRes.code === 200)
      classOptions.value = (clsRes.data.records || []).map((c) => ({
        id: c.id,
        className: c.className,
        grade: c.grade || '',
      }));
    if (gradeRes.code === 200) gradeOptions.value = (gradeRes.data || []).map((g) => g.gradeName);
    if (starsRes.code === 200) weeklyStars.value = starsRes.data || [];
  } catch {
    /* */
  }
});
</script>

<style scoped lang="scss">
.showcase-container {
  margin: 0 auto;
  padding: var(--spacing-lg, 24px);
}

.page-header {
  margin-bottom: 24px;

  .page-title {
    font-size: var(--fs-2xl, 22px);
    color: var(--text-primary, #303133);
    margin: 0 0 4px;
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .page-subtitle {
    font-size: var(--fs-sm, 13px);
    color: var(--text-secondary, var(--el-color-info));
  }
}

/* 本周之星 */
.weekly-stars {
  background: var(--bg-card, #fff);
  border-radius: var(--radius-lg, 12px);
  padding: 16px 20px;
  margin-bottom: 20px;
  box-shadow: var(--shadow-sm, 0 1px 3px rgba(0, 0, 0, 0.08));

  .stars-title {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: var(--fs-md);
    font-weight: 600;
    color: var(--text-primary, #303133);
    margin-bottom: 12px;
  }

  .stars-list {
    display: flex;
    gap: 24px;
    flex-wrap: wrap;
  }

  .star-item {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    padding: 6px 12px;
    border-radius: var(--radius-md, 8px);
    transition: background 0.2s;

    &:hover {
      background: var(--bg-section, #f5f7fa);
    }
  }

  .star-rank {
    font-size: var(--fs-md);
    font-weight: 700;
    color: var(--text-secondary, var(--el-color-info));
    min-width: 16px;
    text-align: center;
  }

  .star-avatar {
    flex-shrink: 0;
  }

  .star-name {
    font-size: var(--fs-sm);
    font-weight: 500;
    color: var(--text-primary, #303133);
    white-space: nowrap;
  }

  .star-title-text {
    font-size: var(--fs-xs);
    color: var(--text-secondary, var(--el-color-info));
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 140px;
  }

  .star-likes {
    display: flex;
    align-items: center;
    gap: 3px;
    font-size: var(--fs-xs);
    color: var(--warning-color, #e6a23c);
    white-space: nowrap;
  }
}

/* 筛选栏 */
.filter-bar {
  margin-bottom: 20px;

  .filter-row {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
  }
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

/* 详情面板 */
.detail-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 2000;
}

.detail-panel {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  width: 480px;
  max-width: 100vw;
  background: var(--bg-card, #fff);
  box-shadow: var(--shadow-lg, 0 8px 24px rgba(0, 0, 0, 0.12));
  display: flex;
  flex-direction: column;
  z-index: 2001;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  border-bottom: 1px solid var(--border-light, #ebeef5);
  flex-shrink: 0;

  h3 {
    font-size: var(--fs-lg);
    font-weight: 600;
    color: var(--text-primary, #303133);
    margin: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    flex: 1;
    margin-right: 12px;
  }
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.panel-slide-enter-active {
  transition: transform 0.3s ease-out;

  .detail-panel {
    transition: transform 0.3s ease-out;
  }
}
.panel-slide-leave-active {
  transition: transform 0.2s ease-in;

  .detail-panel {
    transition: transform 0.2s ease-in;
  }
}
.panel-slide-enter-from .detail-panel,
.panel-slide-leave-to .detail-panel {
  transform: translateX(100%);
}

// 详情内容
.detail-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}

.detail-type {
  font-size: var(--fs-xs);
  padding: 2px 10px;
  border-radius: var(--radius-xs, 4px);
  font-weight: 500;

  &.type-homework {
    background: var(--primary-light, #e8f0fe);
    color: var(--primary-color, #409eff);
  }
  &.type-exam {
    background: var(--bg-warning-light, #fef0e5);
    color: var(--warning-color, #e6a23c);
  }
  &.type-practical {
    background: var(--bg-success-light, #e8f8e8);
    color: var(--success-color, #67c23a);
  }
}

.detail-subject {
  font-size: var(--fs-sm);
  color: var(--text-secondary, var(--el-color-info));
  padding: 2px 8px;
  background: var(--bg-section, #f5f7fa);
  border-radius: var(--radius-xs, 4px);
}

.detail-scope {
  font-size: var(--fs-sm);
  color: var(--text-secondary, var(--el-color-info));
  display: flex;
  align-items: center;
  gap: 4px;
}

.detail-credit {
  color: var(--warning-color, #e6a23c);
  font-weight: 600;
  font-size: var(--fs-md);
}

.detail-desc {
  margin-top: 8px;
}

// 作品内容
.submission-detail {
  margin-top: 16px;
}

.submission-label {
  font-size: var(--fs-sm, 13px);
  font-weight: 500;
  color: var(--text-secondary, var(--el-color-info));
  margin-bottom: 8px;
}

.submission-content {
  background: var(--bg-section, #f5f7fa);
  padding: 12px 16px;
  border-radius: var(--radius-md, 8px);
  white-space: pre-wrap;
  line-height: 1.7;
  font-size: var(--fs-sm, 13px);
  max-height: 200px;
  overflow-y: auto;
  margin-bottom: 8px;
}

.submission-file {
  margin-bottom: 6px;
}

// 点赞按钮 + 荣誉海报
.detail-actions {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--border-light, #ebeef5);
  display: flex;
  align-items: center;
  gap: 12px;
}

// 评论区
.comment-section {
  margin-top: 24px;
  border-top: 1px solid var(--border-light, #ebeef5);
  padding-top: 16px;

  h4 {
    font-size: var(--fs-md);
    font-weight: 600;
    color: var(--text-primary, #303133);
    margin: 0 0 12px;
  }
}

.comment-empty {
  font-size: var(--fs-sm);
  color: var(--text-secondary, var(--el-color-info));
  padding: 16px 0;
  text-align: center;
}

.comment-item {
  display: flex;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid var(--border-light, #ebeef5);

  &:last-child {
    border-bottom: none;
  }
}

.comment-avatar {
  flex-shrink: 0;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-top {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.comment-name {
  font-size: var(--fs-sm);
  font-weight: 500;
  color: var(--text-primary, #303133);
}

.comment-time {
  font-size: var(--fs-xs);
  color: var(--text-secondary, var(--el-color-info));
}

.comment-text {
  font-size: var(--fs-sm);
  color: var(--text-regular, #606266);
  margin-top: 4px;
  line-height: 1.5;
  word-break: break-word;
}

.comment-input {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-end;
}

// 移动端
@media (max-width: 768px) {
  .showcase-container {
    padding: var(--spacing-md, 16px);
  }

  .filter-row {
    :deep(.el-select) {
      flex: 1;
      min-width: 0;
    }
  }

  .detail-panel {
    width: 100vw;
  }

  .weekly-stars .stars-list {
    gap: 8px;
  }

  .star-title-text {
    display: none;
  }

  .filter-trigger {
    width: 100%;
    justify-content: center;
    margin-bottom: 12px;
  }
  .filter-badge {
    margin-left: 6px;
    background: var(--primary-color);
    color: #fff;
    border-radius: 10px;
    padding: 0 6px;
    font-size: var(--fs-xs);
    min-width: 18px;
    display: inline-flex;
    justify-content: center;
  }
}

/* 移动端筛选抽屉 */
.drawer-filter-row {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.drawer-filter-row :deep(.el-select) {
  width: 100%;
}
</style>

<template>
  <div class="page-card">
    <div class="page-header"><h3 class="page-title">我的任务</h3></div>
    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="⏳ 待完成" name="pending" />
      <el-tab-pane label="⏰ 已逾期" name="overdue" />
      <el-tab-pane label="✅ 已完成" name="done" />
      <el-tab-pane label="📋 备考区" name="retake" />
    </el-tabs>

    <!-- 备考区内容 -->
    <template v-if="activeTab === 'retake'">
      <EmptyState v-if="!retakeTasks.length" title="暂无待处理重测" :icon="Calendar" />
      <div
        v-for="t in retakeTasks"
        :key="t.taskId || t.id"
        class="task-card"
        style="
          border: 1px solid var(--el-color-warning);
          border-radius: 8px;
          padding: 12px;
          margin-bottom: 8px;
        "
      >
        <div style="display: flex; justify-content: space-between; align-items: center">
          <div>
            <strong>{{ t.title }}</strong>
            <div style="font-size: 12px; color: var(--text-secondary); margin-top: 4px">
              得分: {{ t.score }}% 🔄 可再练 · {{ t.remainingAttempts }}次机会
            </div>
            <div style="font-size: 12px; color: var(--el-color-warning)">
              重测截止: {{ t.retakeDeadline || '待确认' }}
            </div>
          </div>
          <el-button type="primary" size="small" @click="startRetake(t)">开始重测</el-button>
        </div>
      </div>
      <div v-if="total > pageSize" class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          layout="prev,next"
          @change="loadData"
        />
      </div>
    </template>

    <!-- 筛选栏 -->
    <template v-if="activeTab !== 'retake'">
      <div class="filter-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索任务标题"
          clearable
          size="small"
          style="width: 200px; margin-right: 8px"
          @input="page = 1"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select
          v-model="filterSubject"
          placeholder="学科筛选"
          clearable
          size="small"
          style="width: 140px"
          @change="page = 1"
        >
          <el-option
            v-for="s in subjectList"
            :key="s"
            :value="s"
            :label="s"
          />
        </el-select>
        <span
          v-if="filteredTasks.length"
          class="filter-count"
        >共 {{ filteredTasks.length }} 项</span>
      </div>

      <!-- 考前突击包横幅 -->
      <template v-if="activeTab === 'pending'">
        <ExamPrepBanner
          v-for="t in urgentExamTasks"
          :key="'epb-' + t.id"
          :task-id="t.id"
          :task-title="t.title"
          :deadline="t.deadline"
          :task-type="t.taskType"
        />
      </template>

      <!-- 骨架加载 -->
      <div v-if="loading && !tasks.length" class="st-sk-wrap">
        <el-skeleton :rows="4" animated />
      </div>

      <!-- 移动端卡片 -->
      <template v-else-if="isMobile">
        <div class="card-list">
          <EmptyState
            v-if="!filteredTasks.length"
            :title="emptyDesc"
            :description="activeTab === 'pending' || activeTab === 'overdue' ? '教师还没有发布任务，稍后再来看看吧' : ''"
            :icon="Document"
          />
          <MobileDataCard
            v-for="t in filteredTasks"
            :key="t.id"
            :title="t.title"
            :icon="typeIcon(t.taskType)"
            :badge="urgencyBadge(t)"
            :variant="
              t.submissionStatus === 'GRADED'
                ? 'success'
                : activeTab === 'overdue'
                  ? 'danger'
                  : 'default'
            "
            :meta-items="[subjectBadge(t)]"
            @click="openTask(t)"
          >
            <template #footer>
              <span class="st-deadline"><el-icon><Clock /></el-icon>{{ relativeDeadline(t.deadline) }}</span>
              <template v-if="activeTab === 'done'">
                <span
                  v-if="t.score != null"
                  class="st-score"
                  :class="scoreLevel(t.score, t.totalScore)"
                >{{ renderScore(t) }}</span>
                <el-tag
                  v-if="t.submissionStatus === 'RETURNED'"
                  type="warning"
                  size="small"
                >
                  已退回
                </el-tag>
              </template>
              <template v-else-if="activeTab === 'overdue'">
                <el-tag
                  v-if="t.extraSubmitAllowed === 1"
                  type="info"
                  size="small"
                  effect="plain"
                >
                  可补交
                </el-tag>
              </template>
            </template>
            <template #actions>
              <el-tag :type="TASK_TYPE_TAG[t.taskType] || ''" size="small">
                {{
                  TASK_TYPE_LABEL[t.taskType] || t.taskType
                }}
              </el-tag>
              <el-button size="small" type="primary">
                {{
                  t.submissionStatus === 'GRADED' || t.submissionStatus === 'SUBMITTED'
                    ? '查看'
                    : '去完成'
                }}
              </el-button>
            </template>
          </MobileDataCard>
        </div>
      </template>

      <!-- 桌面端表格 -->
      <el-table
        v-else
        v-loading="loading"
        :data="filteredTasks"
        stripe
      >
        <template #empty><EmptyState :title="emptyDesc" :icon="Document" /></template>
        <el-table-column label="" width="36">
          <template #default="{ row }">
            <span class="st-table-icon">{{ typeIcon(row.taskType) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="任务标题" min-width="180">
          <template #default="{ row }">
            <span>{{ row.title }}</span>
            <el-tag
              v-if="getUrgency(row) === 'urgent'"
              type="danger"
              size="small"
              effect="dark"
              style="margin-left: 6px"
            >
              紧急
            </el-tag>
            <el-tag
              v-else-if="getUrgency(row) === 'warning'"
              type="warning"
              size="small"
              style="margin-left: 6px"
            >
              即将到期
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="学科" width="90">
          <template #default="{ row }">
            <span
              class="st-subject-tag"
              :style="{
                background: subjectColor(row.subject) + '18',
                color: subjectColor(row.subject),
              }"
            >{{ row.subject || '通用' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="TASK_TYPE_TAG[row.taskType] || ''" size="small">
              {{
                TASK_TYPE_LABEL[row.taskType] || row.taskType
              }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag
              v-if="row.submissionStatus === 'RETURNED'"
              type="warning"
              size="small"
            >
              已退回
            </el-tag>
            <el-tag
              v-else-if="row.submissionStatus === 'GRADED'"
              type="success"
              size="small"
            >
              已评分
            </el-tag>
            <el-tag v-else-if="row.submissionStatus === 'SUBMITTED'" size="small">已提交</el-tag>
            <el-tag
              v-else-if="row.submissionStatus === 'PENDING'"
              type="info"
              size="small"
            >
              进行中
            </el-tag>
            <el-tag
              v-else-if="row.submissionStatus === 'EXEMPTED'"
              type="info"
              size="small"
            >
              已豁免
            </el-tag>
            <span v-else class="text-secondary">未开始</span>
          </template>
        </el-table-column>
        <el-table-column label="截止" width="160">
          <template #default="{ row }">
            <span
              :style="{
                color:
                  getUrgency(row) === 'urgent'
                    ? 'var(--el-color-danger)'
                    : getUrgency(row) === 'warning'
                      ? 'var(--el-color-warning)'
                      : 'var(--text-secondary)',
              }"
            >
              {{ relativeDeadline(row.deadline) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="得分" width="80">
          <template #default="{ row }">{{ renderScore(row) }}</template>
        </el-table-column>
        <el-table-column label="评语" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ getComment(row) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openTask(row)">
              {{
                row.submissionStatus === 'GRADED' || row.submissionStatus === 'SUBMITTED'
                  ? '查看'
                  : '去完成'
              }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="total > pageSize" class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          layout="prev,next"
          @change="loadData"
        />
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Clock, Calendar, Document } from '@element-plus/icons-vue';
import { getStudentTasks, getStudentCompletedTasks } from '@/api/task';
import { TASK_TYPE_LABEL, TASK_TYPE_TAG } from '@/constants/taskType';
import { formatDeadline } from '@/utils/taskUtils';
import { useIsMobile } from '@/composables/useIsMobile';
import MobileDataCard from '@/components/common/MobileDataCard.vue';
import ExamPrepBanner from '@/components/knowledge/ExamPrepBanner.vue';
import EmptyState from '@/components/common/EmptyState.vue';

const { isMobile } = useIsMobile();

const router = useRouter();
const route = useRoute();
const tasks = ref([]),
  loading = ref(false),
  activeTab = ref('pending');
const page = ref(1),
  pageSize = ref(20),
  total = ref(0);
const filterSubject = ref('');
const searchKeyword = ref('');
const retakeTasks = ref([]);

// ── 辅助 ────────────────────────────────
const now = computed(() => new Date());
const isOverdue = (t) =>
  t.deadline &&
  new Date(t.deadline) < now.value &&
  (!t.submissionStatus || t.submissionStatus === 'PENDING' || t.submissionStatus === 'RETURNED');

const getUrgency = (t) => {
  if (!t.deadline || isOverdue(t)) return null;
  const days = (new Date(t.deadline) - now.value) / 86400000;
  if (days <= 2) return 'urgent';
  if (days <= 7) return 'warning';
  return null;
};

const isExamType = (t) => ['FORMATIVE', 'SUMMATIVE'].includes(t.taskType);
const isWithin3Days = (t) => {
  if (!t.deadline) return false;
  const days = Math.ceil((new Date(t.deadline) - now.value) / 86400000);
  return days >= 0 && days <= 3;
};
const urgentExamTasks = computed(() =>
  tasks.value.filter(
    (t) =>
      isExamType(t) && isWithin3Days(t) && (!t.submissionStatus || t.submissionStatus === 'PENDING')
  )
);

// ── MobileDataCard 辅助 ──
const urgencyBadge = (t) => {
  const u = getUrgency(t);
  if (u === 'urgent') return { text: '紧急', type: 'danger' };
  if (u === 'warning') return { text: '即将到期', type: 'warning' };
  return null;
};
const subjectBadge = (t) => t.subject || '通用';

const emptyDesc = computed(() => {
  if (activeTab.value === 'pending') return '暂无待完成的任务';
  if (activeTab.value === 'overdue') return '暂无已逾期的任务';
  return '暂无已完成的任务';
});

const subjectList = computed(() => [...new Set(tasks.value.map((t) => t.subject).filter(Boolean))]);

// ── 类型图标 ────────────────────────────
const TYPE_ICONS = {
  PRE_CLASS: '📖',
  IN_CLASS: '📝',
  AFTER_CLASS: '📚',
  FORMATIVE: '📋',
  SUMMATIVE: '🏆',
  PRACTICE: '💻',
  MORAL: '❤️',
  LABOR: '🔧',
  SURVEY: '📊',
};
const typeIcon = (t) => TYPE_ICONS[t] || '📋';

// ── 学科色系 ────────────────────────────
import { getSubjectColor } from '@/utils/subjectColors';
const subjectColor = getSubjectColor;

// ── 相对截止时间 ────────────────────────
const relativeDeadline = (d) => {
  if (!d) return '无截止时间';
  const t = new Date(d) - now.value;
  const abs = Math.abs(t);
  const mins = Math.round(abs / 60000);
  const hours = Math.round(abs / 3600000);
  const days = Math.round(abs / 86400000);
  if (t < 0) {
    if (mins < 60) return `已逾期 ${mins} 分钟`;
    if (hours < 24) return `已逾期 ${hours} 小时`;
    return `已逾期 ${days} 天`;
  }
  if (mins < 60) return `还剩 ${mins} 分钟`;
  if (hours < 24) return `还剩 ${hours} 小时`;
  return formatDeadline(d);
};

// ── 分数渲染 ────────────────────────────
const renderScore = (row) => {
  if (row.score == null && row.gradeLevel === undefined) return '-';
  if (row.gradeLevel === 'PASS') return '通过';
  if (row.gradeLevel === 'FAIL') return '未通过';
  if (row.score != null) return `${row.score}/${row.totalScore || '?'}`;
  return '-';
};
const scoreLevel = (score, total) => {
  if (score == null || !total) return '';
  const pct = score / total;
  if (pct >= 0.8) return 'score-high';
  if (pct >= 0.6) return 'score-mid';
  return 'score-low';
};

// ── 排序和过滤 ──────────────────────────
const URGENCY_ORDER = { urgent: 0, warning: 1 };
const sortedTasks = computed(() => {
  const arr = [...tasks.value];
  if (activeTab.value === 'pending') {
    arr.sort((a, b) => {
      const ua = URGENCY_ORDER[getUrgency(a)] ?? 2;
      const ub = URGENCY_ORDER[getUrgency(b)] ?? 2;
      if (ua !== ub) return ua - ub;
      return new Date(a.deadline || 0) - new Date(b.deadline || 0);
    });
  }
  if (activeTab.value === 'overdue')
    arr.sort((a, b) => new Date(b.deadline || 0) - new Date(a.deadline || 0));
  return arr;
});

const filteredTasks = computed(() => {
  let arr = sortedTasks.value;
  const kw = (searchKeyword.value || '').trim().toLowerCase();
  if (kw) arr = arr.filter((t) => (t.title || '').toLowerCase().includes(kw));
  if (filterSubject.value) arr = arr.filter((t) => t.subject === filterSubject.value);
  // 待完成/已逾期标签页：排除已提交/已评分/已豁免和备考区任务
  if (activeTab.value === 'pending' || activeTab.value === 'overdue') {
    arr = arr.filter(
      (t) =>
        !t.needsRetake &&
        (!t.submissionStatus || !['SUBMITTED', 'GRADED', 'EXEMPTED'].includes(t.submissionStatus))
    );
  }
  if (activeTab.value === 'overdue') arr = arr.filter(isOverdue);
  else if (activeTab.value === 'pending') arr = arr.filter((t) => !isOverdue(t));
  return arr;
});

const onTabChange = () => {
  page.value = 1;
  loadData();
};
const loadData = async () => {
  loading.value = true;
  try {
    if (activeTab.value === 'retake') {
      const r = await getStudentTasks({ page: page.value, size: pageSize.value });
      if (r.code === 200) {
        const items = r.data?.records || r.data || [];
        retakeTasks.value = items.filter((t) => t.needsRetake);
        total.value = r.data?.total || retakeTasks.value.length;
      }
      loading.value = false;
      return;
    }
    const api = activeTab.value === 'done' ? getStudentCompletedTasks : getStudentTasks;
    const r = await api({ page: page.value, size: pageSize.value });
    if (r.code === 200) {
      tasks.value = r.data?.records || r.data || [];
      total.value = r.data?.total || tasks.value.length;
      // 分离待重测任务
      retakeTasks.value = tasks.value.filter((t) => t.needsRetake);
    }
  } catch {
    ElMessage.error('加载失败');
  } finally {
    loading.value = false;
  }
};
const startRetake = (t) => {
  if (t.taskType === 'PRACTICE') router.push(`/training/${t.id}/do`);
  else router.push({ name: 'StudentTaskDetail', params: { id: t.id }, query: { retake: '1' } });
};
const openTask = (row) => {
  if (row.taskType === 'PRACTICE') router.push(`/training/${row.id}/do`);
  else router.push({ name: 'StudentTaskDetail', params: { id: row.id } });
};
const getComment = (row) => {
  try {
    return JSON.parse(row.scoreJson || '{}').comment || '';
  } catch {
    return '';
  }
};

onMounted(loadData);

// 每次通过通知等跳转到任务页时自动刷新
watch(
  () => route.fullPath,
  () => {
    if (route.name === 'StudentTasks') loadData();
  }
);
</script>

<style scoped>
.filter-bar {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.filter-count {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}

/* ── 移动端卡片新样式 ── */
.card-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.st-card {
  padding: 14px;
  background: var(--bg-card);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
  cursor: pointer;
  transition: all 0.15s;
}
.st-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
.st-card:active {
  background: var(--bg-secondary);
}
.st-overdue {
  border-left: 3px solid var(--el-color-danger);
}
.st-graded {
  border-left: 3px solid var(--el-color-success);
}
.st-card-top {
  display: flex;
  gap: 10px;
  margin-bottom: 8px;
}
.st-card-icon {
  font-size: 22px;
  flex-shrink: 0;
  line-height: 1.4;
}
.st-card-body {
  flex: 1;
  min-width: 0;
}
.st-card-title {
  font-size: var(--fs-base);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
  line-height: 1.4;
}
.st-card-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.st-card-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: var(--fs-xs);
}
.st-deadline {
  color: var(--text-secondary);
  display: inline-flex;
  align-items: center;
  gap: 3px;
}
.st-deadline .el-icon {
  font-size: var(--fs-xs);
}
.st-score {
  font-weight: 700;
  font-size: var(--fs-md);
}
.score-high {
  color: var(--el-color-success);
}
.score-mid {
  color: var(--el-color-warning);
}
.score-low {
  color: var(--el-color-danger);
}
.st-type-tag {
  margin-left: auto;
}

/* ── 学科标签 ── */
.st-subject-tag {
  display: inline-block;
  padding: 1px 8px;
  border-radius: 10px;
  font-size: var(--fs-xs);
  font-weight: 500;
  line-height: 1.8;
}
.st-table-icon {
  font-size: var(--fs-lg);
}

/* ── 骨架加载 ── */
.st-sk-wrap {
  padding: 16px 0;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}
.text-secondary {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}

.task-card {
  background: var(--bg-card);
}
.task-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

@media (max-width: 768px) {
  :deep(.el-tabs__item) {
    font-size: var(--fs-sm);
    padding: 0 12px !important;
  }
  :deep(.el-tabs__nav-wrap) {
    overflow-x: auto;
  }
}
</style>

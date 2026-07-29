<template>
  <div class="parent-home">
    <div class="page-header">
      <h2>我的孩子</h2>
      <p class="subtitle">查看孩子的学习情况</p>
      <el-button
        type="primary"
        size="small"
        class="bind-btn"
        @click="showBind = true"
      >
        + 绑定孩子
      </el-button>
    </div>

    <el-row v-loading="loading" :gutter="20">
      <el-col
        v-for="child in children"
        :key="child.studentId"
        :xs="24"
        :sm="12"
        :md="8"
        :lg="6"
      >
        <el-card class="child-card" shadow="hover" @click="viewChild(child)">
          <div class="child-avatar">
            <el-avatar :size="64" style="font-size: 28px">
              {{ child.realName?.charAt(0) }}
            </el-avatar>
          </div>
          <div class="child-info">
            <div class="child-name">{{ child.realName }}</div>
            <div class="child-detail">
              <el-tag size="small" type="info">{{ child.className }}</el-tag>
            </div>
            <div class="child-detail">
              <span class="label">学号：</span>{{ child.studentNumber }}
            </div>
            <div v-if="child.grade" class="child-detail">
              <span class="label">年级：</span>{{ child.grade }}
            </div>
            <div v-if="child.relation" class="child-detail">
              <span class="label">关系：</span>{{ relationLabel(child.relation) }}
            </div>
          </div>
          <div class="child-actions">
            <template v-if="!isMobile">
              <el-button
                size="small"
                type="primary"
                @click.stop="viewGrades(child)"
              >
                查看成绩
              </el-button>
              <el-button size="small" @click.stop="viewHomework(child)">作业列表</el-button>
              <el-button size="small" @click.stop="viewTimeline(child)">成长足迹</el-button>
              <el-button size="small" @click.stop="viewPractices(child)">实训记录</el-button>
              <el-button size="small" type="info" @click.stop="goMessages">消息</el-button>
            </template>
            <el-dropdown v-else trigger="click" @command="(cmd) => handleMobileAction(cmd, child)">
              <el-button
                size="small"
                type="primary"
              >
                操作 <el-icon><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="grades">查看成绩</el-dropdown-item>
                  <el-dropdown-item command="homework">作业列表</el-dropdown-item>
                  <el-dropdown-item command="timeline">成长足迹</el-dropdown-item>
                  <el-dropdown-item command="practices">实训记录</el-dropdown-item>
                  <el-dropdown-item command="messages">消息</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && children.length === 0" description="暂未关联孩子" :image-size="80">
      <template #default>
        <div
          style="
            color: var(--text-secondary);
            font-size: var(--fs-sm);
            text-align: center;
            line-height: 1.8;
          "
        >
          <p>绑定孩子后，您可以查看孩子的学习情况。</p>
          <p style="color: var(--text-placeholder)">
            提示：数据内容取决于教师是否通过本系统布置任务。<br />如无数据，建议通过班级群等渠道直接与老师沟通。
          </p>
        </div>
      </template>
    </el-empty>

    <el-alert
      v-if="!loading && children.length > 0"
      type="info"
      :closable="false"
      style="margin-bottom: 16px"
    >
      <template #default>
        <span style="font-size: var(--fs-sm)">📌
          以下数据来源于教师通过本系统发布的任务和记录。如某项为空，可能教师暂未使用本系统相关功能，建议关注班级群通知。</span>
      </template>
    </el-alert>

    <!-- 待填问卷提醒 -->
    <el-card v-if="pendingForms.length > 0" class="feedback-section" shadow="never">
      <template #header>
        <span><el-icon><EditPen /></el-icon> 待填写问卷
          <el-badge
            :value="pendingForms.length"
            class="alert-badge"
          /></span>
      </template>
      <div v-for="f in pendingForms" :key="f.formId" class="feedback-item">
        <div class="feedback-info">
          <span class="feedback-title">{{ f.title }}</span>
          <span class="feedback-class">{{ f.className }}</span>
          <span class="feedback-period">{{ f.period }}</span>
        </div>
        <el-button size="small" type="primary" @click="goFeedback(f.formId)">去填写</el-button>
      </div>
    </el-card>

    <!-- 预警通知区域 -->
    <el-card v-if="alerts.length > 0" class="alert-section" shadow="never">
      <template #header>
        <span>
          <el-icon><Bell /></el-icon> 预警通知
          <el-badge :value="alerts.length" class="alert-badge" />
        </span>
      </template>
      <div v-for="alert in alerts" :key="alert.id" class="alert-item">
        <div class="alert-header">
          <el-tag :type="alert.ruleName?.includes('不及格') ? 'danger' : 'warning'" size="small">
            {{ alert.ruleName }}
          </el-tag>
          <span class="alert-student">{{ alert.studentName }}</span>
          <span class="alert-time">{{ alert.createTime ? fmt(alert.createTime) : '' }}</span>
          <el-button
            size="small"
            text
            type="primary"
            :loading="acknowledging[alert.id]"
            @click="doAcknowledge(alert.id)"
          >
            标记已读
          </el-button>
        </div>
        <div class="alert-summary">{{ alert.alertSummary }}</div>
      </div>
    </el-card>
    <el-card v-else class="alert-section" shadow="never">
      <template #header>
        <span><el-icon><Bell /></el-icon> 预警通知</span>
      </template>
      <el-empty description="暂无预警" :image-size="48" />
    </el-card>

    <el-dialog v-model="showBind" title="绑定孩子" width="400px">
      <el-form :model="bindForm" label-width="80px">
        <el-form-item label="学号" required>
          <el-input v-model="bindForm.studentNumber" placeholder="输入孩子的学号" />
        </el-form-item>
        <el-form-item label="学生姓名" required>
          <el-input v-model="bindForm.studentName" placeholder="输入孩子的姓名" />
        </el-form-item>
        <el-form-item label="关系">
          <el-select v-model="bindForm.relation" style="width: 100%">
            <el-option label="父亲" value="FATHER" />
            <el-option label="母亲" value="MOTHER" />
            <el-option label="监护人" value="GUARDIAN" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showBind = false">取消</el-button>
        <el-button type="primary" :loading="binding" @click="doBind">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getMyChildren, bindChild, acknowledgeAlert } from '@/api/parent';
import { getChildAlerts } from '@/api/alert';
import { getPendingFeedbackForms } from '@/api/parentFeedback';
import { useIsMobile } from '@/composables/useIsMobile';
import { ArrowDown } from '@element-plus/icons-vue';

const router = useRouter();
const { isMobile } = useIsMobile();
const children = ref([]);
const alerts = ref([]);
const pendingForms = ref([]);
const loading = ref(false);

const RELATION_MAP = { FATHER: '父亲', MOTHER: '母亲', GUARDIAN: '监护人' };
const relationLabel = (r) => RELATION_MAP[r] || r;

const showBind = ref(false);
const binding = ref(false);
const bindForm = ref({ studentNumber: '', studentName: '', relation: 'FATHER' });
const acknowledging = ref({});

async function doBind() {
  if (!bindForm.value.studentNumber || !bindForm.value.studentName) {
    ElMessage.warning('请填写学号和姓名');
    return;
  }
  binding.value = true;
  try {
    const res = await bindChild(bindForm.value);
    if (res.code === 200) {
      ElMessage.success('绑定成功');
      showBind.value = false;
      bindForm.value = { studentNumber: '', studentName: '', relation: 'FATHER' };
      // 刷新孩子列表
      const r = await getMyChildren();
      if (r.code === 200) children.value = r.data || [];
    } else {
      ElMessage.error(res.message || '绑定失败');
    }
  } catch (e) {
    ElMessage.error(e.message || '绑定失败');
  } finally {
    binding.value = false;
  }
}

const fmt = (s) => {
  if (!s) return '-';
  const d = new Date(s);
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
};

onMounted(async () => {
  loading.value = true;
  try {
    const [childrenRes, alertsRes, formsRes] = await Promise.all([
      getMyChildren(),
      getChildAlerts(),
      getPendingFeedbackForms(),
    ]);
    if (childrenRes.code === 200) children.value = childrenRes.data || [];
    if (alertsRes.code === 200) alerts.value = alertsRes.data || [];
    if (formsRes.code === 200) pendingForms.value = formsRes.data || [];
  } finally {
    loading.value = false;
  }
});

const childQuery = (child) => ({ name: child.realName });
const viewChild = (child) => {
  router.push({ path: `/parent/children/${child.studentId}/grades`, query: childQuery(child) });
};
const viewGrades = (child) => {
  router.push({ path: `/parent/children/${child.studentId}/grades`, query: childQuery(child) });
};
const viewTimeline = (child) => {
  router.push({ path: `/parent/children/${child.studentId}/timeline`, query: childQuery(child) });
};
const viewHomework = (child) => {
  router.push({ path: `/parent/children/${child.studentId}/homework`, query: childQuery(child) });
};
const viewPractices = (child) => {
  router.push({ path: `/parent/children/${child.studentId}/practice`, query: childQuery(child) });
};
const handleMobileAction = (cmd, child) => {
  const actions = {
    grades: viewGrades,
    homework: viewHomework,
    timeline: viewTimeline,
    practices: viewPractices,
    messages: goMessages,
  };
  actions[cmd]?.(child);
};

const goMessages = () => {
  router.push('/messages');
};
const goFeedback = (formId) => {
  router.push(`/parent/feedback/${formId}`);
};
async function doAcknowledge(alertId) {
  acknowledging.value[alertId] = true;
  try {
    const res = await acknowledgeAlert(alertId);
    if (res.code === 200) {
      alerts.value = alerts.value.filter((a) => a.id !== alertId);
      ElMessage.success('已标记已读');
    } else {
      ElMessage.error(res.message || '操作失败');
    }
  } catch (e) {
    ElMessage.error(e.message || '操作失败');
  } finally {
    acknowledging.value[alertId] = false;
  }
}
</script>

<style scoped>
.parent-home {
  max-width: 1200px;
  margin: 0 auto;
  padding: 8px;
}
.page-header {
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
.page-header h2 {
  margin: 0;
  font-size: 22px;
}
.subtitle {
  margin: 0;
  color: var(--text-secondary);
  font-size: var(--fs-md);
}
.bind-btn {
  margin-left: auto;
}

.child-card {
  cursor: pointer;
  margin-bottom: 16px;
  text-align: center;
  transition: transform 0.2s;
}
.child-card:hover {
  transform: translateY(-2px);
}
.child-avatar {
  margin: 8px 0 12px;
}
.child-info {
  margin-bottom: 16px;
}
.child-name {
  font-size: var(--fs-lg);
  font-weight: 600;
  margin-bottom: 6px;
}
.child-detail {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  margin-bottom: 4px;
}
.child-detail .label {
  color: var(--text-regular);
}
.child-actions {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.alert-section {
  margin-top: 32px;
}
.alert-section :deep(.el-card__header) {
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
}
.alert-badge {
  margin-left: 8px;
}
.alert-item {
  padding: 10px 0;
  border-bottom: 1px solid var(--border-light);
}
.alert-item:last-child {
  border-bottom: none;
}
.alert-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.alert-student {
  font-weight: 500;
  font-size: var(--fs-md);
}
.alert-time {
  margin-left: auto;
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}
.alert-summary {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  padding-left: 4px;
}

.feedback-section {
  margin-top: 24px;
}
.feedback-section :deep(.el-card__header) {
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
}
.feedback-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid var(--border-light);
}
.feedback-item:last-child {
  border-bottom: none;
}
.feedback-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.feedback-title {
  font-weight: 500;
  font-size: var(--fs-md);
}
.feedback-class {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}
.feedback-period {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}

@media (max-width: 768px) {
  :deep(.el-form-item .el-input),
  :deep(.el-form-item .el-select) {
    width: 100%;
  }
}
</style>

<template>
  <el-dialog
    v-model="visible"
    title="发布实训任务"
    width="640px"
    destroy-on-close
  >
    <div v-if="loading" style="text-align: center; padding: 20px">
      <el-icon class="is-loading"><Loading /></el-icon> 正在解析...
    </div>
    <template v-else>
      <el-radio-group v-model="publishMode" style="margin-bottom: 16px">
        <el-radio value="ai">从AI输出创建方案</el-radio>
        <el-radio value="plan">从已有方案发布</el-radio>
      </el-radio-group>

      <!-- 从已有方案发布 -->
      <template v-if="publishMode === 'plan'">
        <el-form label-width="80px">
          <el-form-item label="选择方案">
            <el-select
              v-model="selectedPlanId"
              filterable
              placeholder="选择实训方案"
              style="width: 100%"
              @change="onPlanSelect"
            >
              <el-option
                v-for="p in planList"
                :key="p.id"
                :label="p.title"
                :value="p.id"
              />
            </el-select>
          </el-form-item>
          <div v-if="previewPlan" class="plan-preview">
            <h4>{{ previewPlan.title }}</h4>
            <p>{{ previewPlan.description }}</p>
            <el-tag
              v-for="r in previewRubrics"
              :key="r.id"
              size="small"
              style="margin: 2px"
            >
              {{ r.dimensionLabel }}({{ r.weight }})
            </el-tag>
          </div>
        </el-form>
      </template>

      <!-- 从AI输出创建方案 -->
      <template v-else-if="meta">
        <el-form :model="form" label-width="90px" size="default">
          <el-form-item label="任务标题" required>
            <el-input v-model="form.title" maxlength="50" show-word-limit />
          </el-form-item>
          <el-row :gutter="12">
            <el-col :xs="24" :sm="12">
              <el-form-item label="年级" required>
                <el-select v-model="form.gradeId" style="width: 100%">
                  <el-option
                    v-for="g in grades"
                    :key="g.id"
                    :label="g.gradeName || g.name || g.id"
                    :value="g.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="满分">
                <el-input-number
                  v-model="form.totalScore"
                  :min="10"
                  :max="200"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="目标班级" required>
            <el-select
              v-model="form.classIds"
              multiple
              placeholder="选择班级"
              style="width: 100%"
            >
              <el-option
                v-for="c in classes"
                :key="c.id"
                :label="c.className || c.name || c.id"
                :value="c.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="截止时间">
            <el-date-picker
              v-model="form.deadline"
              type="datetime"
              placeholder="选择截止时间"
              style="width: 100%"
              value-format="YYYY-MM-DDTHH:mm:ss"
            />
          </el-form-item>
          <el-form-item label="其他设置">
            <el-checkbox v-model="form.allowCustomSteps">允许学生自定义步骤</el-checkbox>
          </el-form-item>
          <el-divider />
          <el-form-item label="包含步骤">
            <div style="width: 100%">
              <div
                v-for="(s, i) in editSteps"
                :key="i"
                style="display: flex; align-items: center; gap: 6px; margin-bottom: 6px"
              >
                <span style="font-weight: 600; min-width: 20px">{{ s.seq }}</span>
                <el-input
                  v-model="s.name"
                  size="small"
                  placeholder="步骤名称"
                  style="flex: 1"
                />
                <el-input-number
                  v-model="s.minutes"
                  :min="0"
                  :max="120"
                  size="small"
                  style="width: 80px"
                  placeholder="分钟"
                />
                <el-button size="small" circle @click="editSteps.splice(i, 1)">×</el-button>
              </div>
              <el-button
                v-if="!editSteps.length"
                size="small"
                @click="editSteps.push({ seq: editSteps.length + 1, name: '', minutes: 15 })"
              >
                + 添加步骤
              </el-button>
            </div>
          </el-form-item>
        </el-form>
      </template>
      <el-empty v-else-if="publishMode === 'ai' && !meta" description="未找到任务元数据" />
    </template>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button
        v-if="publishMode === 'plan'"
        type="primary"
        :loading="publishing"
        :disabled="!selectedPlanId || !form.classIds.length"
        @click="doPublishFromPlan"
      >
        发布方案
      </el-button>
      <el-button
        v-else
        type="primary"
        :loading="publishing"
        :disabled="!canPublish"
        @click="doPublish"
      >
        发布为任务
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { Loading } from '@element-plus/icons-vue';
import { getGrades } from '@/api/settings';
import { getMyClasses } from '@/api/classes';
import { listPlans, getPlan, getRubrics, publishPlan, createPlan } from '@/api/practice';

const emit = defineEmits(['published']);

const visible = ref(false);
const loading = ref(false);
const publishing = ref(false);
const meta = ref(null);
const grades = ref([]);
const classes = ref([]);
const outputId = ref(null);
const editSteps = ref([]);
const publishMode = ref('ai');

const planList = ref([]);
const selectedPlanId = ref(null);
const previewPlan = ref(null);
const previewRubrics = ref([]);

const form = ref({
  title: '',
  gradeId: null,
  classIds: [],
  deadline: null,
  totalScore: 100,
  allowCustomSteps: false,
  subject: '',
});

const canPublish = computed(
  () => form.value.title && form.value.gradeId && form.value.classIds.length > 0
);

const loadRefs = async () => {
  try {
    const [gRes, cRes] = await Promise.all([getGrades(), getMyClasses()]);
    if (gRes.code === 200) grades.value = gRes.data || [];
    if (cRes.code === 200) classes.value = cRes.data || [];
  } catch {
    /* */
  }
};

const open = (item) => {
  outputId.value = item.id;
  loading.value = true;
  meta.value = null;
  publishMode.value = 'ai';

  const content = item.content || '';
  const m = content.match(/```json\s*(\{[^`]+\})\s*```/s);
  if (m) {
    try {
      meta.value = JSON.parse(m[1]);
    } catch {
      /* */
    }
  }

  if (meta.value) {
    form.value.title = meta.value.title || item.title || '';
    form.value.totalScore =
      (meta.value.scoringItems || []).reduce((s, i) => s + (i.maxScore || 0), 0) || 100;
    form.value.subject = item.subject || '';
    editSteps.value = (meta.value.steps || []).map((s) => ({ ...s }));
    if (!editSteps.value.length) editSteps.value.push({ seq: 1, name: '', minutes: 15 });

    // 从Markdown内容中提取每个步骤的描述
    if (content && editSteps.value.length) {
      editSteps.value.forEach((step, i) => {
        const pattern = new RegExp(
          `###\\s*(?:任务|步骤)\\s*${step.seq}[：:]\\s*${escapeRegex(step.name)}[\\s\\S]*?(?=###\\s*(?:任务|步骤)|###\\s*评分|\`\`\`json|$)`,
          'i'
        );
        const m = content.match(pattern);
        if (m) {
          const desc = m[0].replace(/^###.*\n/, '').trim();
          if (desc) step.description = desc.substring(0, 500);
        }
      });
    }
  }
  loading.value = false;
  if (!meta.value) {
    ElMessage.warning('未找到任务元数据');
    visible.value = false;
    return;
  }

  loadRefs();
  visible.value = true;
};

const openFromPlan = async () => {
  outputId.value = null;
  publishMode.value = 'plan';
  selectedPlanId.value = null;
  previewPlan.value = null;
  previewRubrics.value = [];
  loading.value = true;
  try {
    const [gRes, cRes, pRes] = await Promise.all([getGrades(), getMyClasses(), listPlans()]);
    if (gRes.code === 200) grades.value = gRes.data || [];
    if (cRes.code === 200) classes.value = cRes.data || [];
    if (pRes.code === 200) planList.value = pRes.data || [];
  } catch {
    /* */
  }
  loading.value = false;
  visible.value = true;
};

const onPlanSelect = async (planId) => {
  if (!planId) return;
  try {
    const [planRes, rubricsRes] = await Promise.all([getPlan(planId), getRubrics(planId)]);
    if (planRes.code === 200) previewPlan.value = planRes.data;
    if (rubricsRes.code === 200) previewRubrics.value = rubricsRes.data || [];
  } catch {
    /* */
  }
};

const doPublishFromPlan = async () => {
  if (!selectedPlanId.value || !form.value.classIds.length) return;
  publishing.value = true;
  try {
    const res = await publishPlan(selectedPlanId.value, { classIds: form.value.classIds });
    if (res.code === 200) {
      ElMessage.success('实训方案已发布为任务');
      visible.value = false;
      emit('published', res.data);
    } else ElMessage.error(res.message || '发布失败');
  } catch (e) {
    const msg = e?.response?.data?.message || e?.message || '发布失败';
    ElMessage.error('发布实训任务失败');
    ElMessage.error(msg);
  } finally {
    publishing.value = false;
  }
};

const doPublish = async () => {
  if (!canPublish.value) return;
  publishing.value = true;
  try {
    // 从AI输出创建方案 → 发布为任务
    const rubrics = (meta.value?.scoringItems || []).map((item, i) => ({
      dimension: 'process_' + (item.item || '').toLowerCase().replace(/\s+/g, '_'),
      dimensionLabel: item.item || '维度' + (i + 1),
      weight: item.maxScore ? Math.min(1, (item.maxScore || 10) / 50) : 0.5,
      criteria: JSON.stringify(
        Array.isArray(item.criteria)
          ? item.criteria.map((c, l) => ({ level: l, label: c, description: c }))
          : item.criteria
            ? [{ level: 0, label: item.criteria, description: item.criteria }]
            : []
      ),
      sortOrder: i,
    }));

    const planData = {
      title: form.value.title,
      description: meta.value
        ? JSON.stringify({
            steps: editSteps.value.map((s) => ({
              seq: s.seq,
              name: s.name,
              minutes: s.minutes,
              description: s.description || '',
            })),
            scoringItems: meta.value.scoringItems,
          })
        : '',
      scoringModel: 'DUAL_DIMENSION',
      rubrics,
    };
    const planRes = await createPlan(planData);
    if (planRes.code !== 200) {
      ElMessage.error(planRes.message || '创建方案失败');
      publishing.value = false;
      return;
    }

    const planId = planRes.data?.id;
    if (!planId) {
      ElMessage.error('创建方案失败');
      publishing.value = false;
      return;
    }

    const pubRes = await publishPlan(planId, { classIds: form.value.classIds });
    if (pubRes.code === 200) {
      ElMessage.success('实训任务已创建');
      visible.value = false;
      emit('published', pubRes.data);
    } else {
      ElMessage.error(pubRes.message || '发布失败');
    }
  } catch (e) {
    const msg = e?.response?.data?.message || e?.message || '发布失败';
    ElMessage.error('发布实训任务失败');
    ElMessage.error(msg);
  }
  publishing.value = false;
};

defineExpose({ open, openFromPlan });

function escapeRegex(s) {
  return (s || '').replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
</script>

<style scoped>
.plan-preview {
  background: var(--bg-card);
  padding: 12px;
  border-radius: 6px;
  margin-top: 8px;
}
.plan-preview h4 {
  margin: 0 0 4px;
  font-size: var(--fs-md);
}
.plan-preview p {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  margin: 0 0 8px;
  white-space: pre-wrap;
}
</style>

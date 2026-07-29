<template>
  <div class="task-editor">
    <div class="te-header">
      <el-button text @click="$router.back()">← 返回</el-button>
      <h3>{{ isEdit ? '编辑实训任务' : '创建实训任务' }}</h3>
      <el-button
        v-if="!isEdit"
        type="primary"
        plain
        size="small"
        @click="showTemplateDialog = true"
      >
        <el-icon><FolderOpened /></el-icon> 从模板创建
      </el-button>
    </div>

    <!-- 基本信息 -->
    <el-card shadow="never" class="te-section">
      <template #header>基本信息</template>
      <el-form :model="form" label-width="80px">
        <el-form-item label="任务标题" required>
          <el-input v-model="form.title" placeholder="输入任务标题" />
        </el-form-item>
        <el-form-item label="学科">
          <el-select v-model="form.subject" placeholder="选择学科">
            <el-option
              v-for="s in subjectOptions"
              :key="s"
              :label="s"
              :value="s"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="任务描述（可选）"
          />
        </el-form-item>
        <el-form-item label="目标班级" required>
          <el-select
            v-model="selectedClassIds"
            multiple
            placeholder="选择班级（可多选）"
            style="width: 100%"
          >
            <el-option
              v-for="c in classOptions"
              :key="c.id"
              :label="c.className"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 步骤管线 -->
    <el-card shadow="never" class="te-section">
      <template #header>
        <span>实训步骤</span>
        <el-button
          type="primary"
          text
          size="small"
          style="margin-left: 12px"
          @click="addStep"
        >
          + 添加步骤
        </el-button>
      </template>

      <el-empty v-if="steps.length === 0" description="暂无步骤，点击上方添加" :image-size="60" />

      <div v-for="(step, i) in steps" :key="i" class="step-row">
        <div class="step-row-header">
          <span class="step-index">Step {{ i + 1 }}</span>
          <el-input
            v-model="step.title"
            placeholder="步骤标题"
            size="small"
            style="width: 200px"
          />
          <el-select
            v-model="step.type"
            size="small"
            style="width: 120px"
            @change="onTypeChange(i)"
          >
            <el-option
              v-for="t in STEP_TYPES"
              :key="t.value"
              :label="t.label"
              :value="t.value"
            />
          </el-select>
          <span style="flex: 1"></span>
          <el-button-group>
            <el-button :disabled="i === 0" size="small" @click="moveStep(i, -1)">↑</el-button>
            <el-button
              :disabled="i === steps.length - 1"
              size="small"
              @click="moveStep(i, 1)"
            >
              ↓
            </el-button>
          </el-button-group>
          <el-button
            size="small"
            type="danger"
            text
            @click="removeStep(i)"
          >
            删除
          </el-button>
        </div>
        <el-input
          v-model="step.description"
          placeholder="步骤说明（可选）"
          size="small"
          style="margin-top: 4px"
        />
        <div class="step-config">
          <el-input-number
            v-model="step.scoreMax"
            :min="0"
            :max="100"
            size="small"
            placeholder="满分"
          />
          <span class="config-hint">分</span>
        </div>
      </div>
    </el-card>

    <!-- 底部操作 -->
    <div class="te-footer">
      <el-checkbox v-model="saveToLibrary">保存到任务库</el-checkbox>
      <span style="flex: 1"></span>
      <el-button :loading="saving" @click="saveDraft">保存草稿</el-button>
      <el-button type="primary" :loading="publishing" @click="publish">发布任务</el-button>
    </div>

    <!-- 模板选择对话框 -->
    <el-dialog v-model="showTemplateDialog" title="从模板创建实训任务" width="700px">
      <el-form :inline="true" size="small">
        <el-form-item label="学科">
          <el-select
            v-model="templateFilter.subject"
            placeholder="全部学科"
            clearable
            @change="loadTemplates"
          >
            <el-option
              v-for="s in subjectOptions"
              :key="s"
              :label="s"
              :value="s"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <el-table
        :data="templates"
        max-height="380"
        highlight-current-row
        @row-click="applyTemplate"
      >
        <el-table-column prop="title" label="模板名称" min-width="180" />
        <el-table-column prop="subject" label="学科" width="140" />
        <el-table-column prop="category" label="类别" width="100" />
        <el-table-column label="步骤数" width="70">
          <template #default="{ row }">
            {{ parseStepCount(row.stepsJson) }}步
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button
              size="small"
              type="primary"
              text
              @click.stop="applyTemplate(row)"
            >
              使用
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!templates.length" description="暂无可用模板" :image-size="60" />
      <template #footer>
        <el-button @click="showTemplateDialog = false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { FolderOpened } from '@element-plus/icons-vue';
import {
  createTrainingTask,
  updateTrainingTask,
  publishTrainingTask,
  copyFromLibrary,
  aiGenerateSteps,
} from '@/api/training';
import { listTemplates } from '@/api/practice';
import { getMyClasses } from '@/api/classes';

const route = useRoute();
const router = useRouter();
const isEdit = !!route.params.id;
const saving = ref(false);
const publishing = ref(false);
const saveToLibrary = ref(false);
const showTemplateDialog = ref(false);
const classOptions = ref([]);
const selectedClassIds = ref([]);
watch(showTemplateDialog, (v) => {
  if (v) loadTemplates();
});
const templates = ref([]);
const templateFilter = ref({ subject: '' });

async function loadTemplates() {
  try {
    const params = templateFilter.value.subject ? { subject: templateFilter.value.subject } : {};
    const res = await listTemplates(params);
    if (res.code === 200) templates.value = res.data || [];
  } catch {
    templates.value = [];
  }
}

function parseStepCount(stepsJson) {
  if (!stepsJson) return 0;
  try {
    const arr = typeof stepsJson === 'string' ? JSON.parse(stepsJson) : stepsJson;
    return Array.isArray(arr) ? arr.length : 0;
  } catch {
    return 0;
  }
}

async function applyTemplate(row) {
  try {
    let parsedSteps = [];
    if (row.stepsJson) {
      parsedSteps = typeof row.stepsJson === 'string'
        ? JSON.parse(row.stepsJson)
        : row.stepsJson;
    }
    form.value = {
      title: row.title || '',
      subject: row.subject || '',
      description: row.description || '',
    };
    steps.value = (parsedSteps || []).map((s) => ({
      title: s.title || '',
      type: s.type || 'text',
      description: s.description || '',
      scoreMax: s.score?.max || 10,
      config: s.config,
      resourceFile: s.resourceFile,
      sampleImages: s.sampleImages,
    }));
    showTemplateDialog.value = false;
    ElMessage.success('模板已加载，请按需调整后发布');
  } catch {
    ElMessage.error('加载模板失败');
  }
}

const STEP_TYPES = [
  { label: '文字论述', value: 'text' },
  { label: '文件提交', value: 'file' },
  { label: '仿真操作', value: 'sim' },
  { label: 'Word 文档', value: 'office' },
  { label: 'Excel 表格', value: 'excel' },
  { label: 'PPT 演示', value: 'ppt' },
  { label: '网页制作', value: 'web' },
  { label: 'SQL 查询', value: 'sql' },
  { label: '选择题', value: 'choice' },
];

const subjectOptions = ['信息技术应用基础', '网络应用基础', '办公应用基础', 'Access', '其他'];

const form = ref({ title: '', subject: '', description: '' });
const steps = ref([]);

function addStep() {
  steps.value.push({ title: '', type: 'text', description: '', scoreMax: 10 });
}

function removeStep(i) {
  steps.value.splice(i, 1);
}
function moveStep(i, dir) {
  const arr = steps.value;
  const item = arr.splice(i, 1)[0];
  arr.splice(i + dir, 0, item);
}
function onTypeChange(i) {
  const s = steps.value[i];
  if (s) {
    delete s.config;
    s.description = '';
  }
}

async function loadFromQuery() {
  const from = route.query.from;
  if (from === 'library' && route.query.id) {
    try {
      const res = await copyFromLibrary(route.query.id);
      if (res.code === 200 && res.data) {
        form.value = {
          title: res.data.title,
          subject: res.data.subject,
          description: res.data.description,
        };
        steps.value = res.data.steps || [];
      }
    } catch {
      /* 静默 */
    }
  } else if (from === 'syllabus' && route.query.nodeId) {
    try {
      const res = await aiGenerateSteps({
        nodeId: route.query.nodeId,
        subject: form.value.subject,
        stepCount: 5,
      });
      if (res.code === 200 && res.data) {
        steps.value = res.data.steps || [];
      }
    } catch {
      /* 静默 */
    }
  }
}

function buildSaveData() {
  return {
    ...form.value,
    targetIds: selectedClassIds.value,
    steps: steps.value.map((s) => {
      const step = {
        title: s.title,
        type: s.type,
        description: s.description,
        score: {
          method:
            s.type === 'sim' ||
            s.type === 'choice' ||
            s.type === 'excel' ||
            s.type === 'ppt' ||
            s.type === 'sql'
              ? 'auto'
              : 'manual',
          max: s.scoreMax,
        },
      };
      if (s.config) step.config = s.config;
      if (s.resourceFile) step.resourceFile = s.resourceFile;
      if (s.sampleImages) step.sampleImages = s.sampleImages;
      return step;
    }),
    saveToLibrary: saveToLibrary.value,
  };
}

async function saveDraft() {
  if (!form.value.title) {
    ElMessage.warning('请输入任务标题');
    return;
  }
  if (!selectedClassIds.value.length) {
    ElMessage.warning('请选择目标班级');
    return;
  }
  saving.value = true;
  try {
    const data = buildSaveData();
    let res;
    if (isEdit) {
      res = await updateTrainingTask(route.params.id, data);
    } else {
      res = await createTrainingTask(data);
    }
    if (res.code === 200) {
      ElMessage.success('已保存');
      router.push({ name: 'TrainingHubTeacher' });
    } else {
      ElMessage.error(res.message || '保存失败');
    }
  } catch {
    ElMessage.error('保存失败');
  } finally {
    saving.value = false;
  }
}

async function publish() {
  if (!form.value.title) {
    ElMessage.warning('请输入任务标题');
    return;
  }
  if (!selectedClassIds.value.length) {
    ElMessage.warning('请选择目标班级');
    return;
  }
  publishing.value = true;
  try {
    const data = buildSaveData();
    let res;
    if (isEdit) {
      res = await updateTrainingTask(route.params.id, data);
    } else {
      res = await createTrainingTask(data);
    }
    if (res.code === 200) {
      const taskId = res.data?.id || route.params.id;
      await publishTrainingTask(taskId);
      ElMessage.success('已发布');
      router.push({ name: 'TrainingHubTeacher' });
    } else {
      ElMessage.error(res.message || '保存失败');
    }
  } catch {
    ElMessage.error('发布失败');
  } finally {
    publishing.value = false;
  }
}

async function loadClasses() {
  try {
    const res = await getMyClasses();
    if (res.code === 200) {
      classOptions.value = (res.data?.records || res.data || []).map((c) => ({
        id: c.id,
        className: c.className,
      }));
    }
  } catch (e) {
    console.error('加载班级列表失败:', e);
    ElMessage.warning('班级列表加载失败，请刷新重试');
  }
}

onMounted(() => {
  loadFromQuery();
  loadClasses();
});
</script>

<style scoped>
.task-editor {
  max-width: 800px;
  margin: 0 auto;
  padding: 16px;
}
.te-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.te-header h3 {
  margin: 0;
}
.te-section {
  margin-bottom: 16px;
  border-radius: var(--radius-md);
}
.step-row {
  padding: 12px;
  margin-bottom: 8px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
}
.step-row-header {
  display: flex;
  align-items: center;
  gap: 8px;
}
.step-index {
  font-weight: 600;
  color: var(--primary-color);
  min-width: 50px;
  font-size: var(--fs-sm);
}
.step-config {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
}
.config-hint {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}
.te-footer {
  display: flex;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid var(--border-light);
}
</style>

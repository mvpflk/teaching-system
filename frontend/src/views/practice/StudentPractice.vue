<template>
  <div class="sp-page">
    <div class="sp-header">
      <el-button text @click="$router.back()">← 返回</el-button>
      <h3>{{ task?.title || '实训任务' }}</h3>
      <el-tag v-if="submitted" type="success" size="small">已提交</el-tag>
    </div>

    <!-- 实训概览面板 -->
    <div class="practice-overview">
      <div class="overview-card">
        <span class="ov-label">实训任务</span><span class="ov-value">{{ task?.title || '加载中...' }}</span>
      </div>
      <div class="overview-card">
        <span class="ov-label">总步骤</span><span class="ov-value ov-big">{{ steps.length }}</span>
      </div>
      <div class="overview-card">
        <span class="ov-label">已完成</span><span class="ov-value ov-big ov-green">{{ completedSteps }}</span>
      </div>
      <div class="overview-card">
        <span class="ov-label">状态</span><span class="ov-value" :class="statusClass">{{ statusText }}</span>
      </div>
    </div>
    <!-- 步骤进度条 -->
    <div class="step-progress">
      <div class="progress-track">
        <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
      </div>
      <div class="step-nodes">
        <div
          v-for="(step, i) in steps"
          :key="step.stepId || i"
          :class="[
            'step-node',
            {
              'node-done': step._completed,
              'node-current': i === currentStepIndex && !step._completed,
              'node-returned': step._returned,
            },
          ]"
          @click="currentStepIndex = i"
        >
          <span class="node-icon">{{ step._completed ? '✓' : step._returned ? '!' : i + 1 }}</span>
          <span class="node-label">步骤{{ i + 1 }}</span>
        </div>
      </div>
    </div>

    <!-- 骨架屏 -->
    <div v-if="loading" class="sk-list">
      <div class="sk-card">
        <div class="sk-line w-90" style="height: 12px; margin-bottom: 8px"></div>
        <div class="sk-line w-70" style="height: 12px"></div>
      </div>
      <div v-for="i in 3" :key="i" class="sk-card">
        <div class="sk-line w-30" style="height: 16px; margin-bottom: 10px"></div>
        <div class="sk-line w-90" style="height: 12px; margin-bottom: 6px"></div>
        <div class="sk-line w-60" style="height: 12px"></div>
      </div>
    </div>

    <template v-else>
      <!-- 评分结果（置顶） -->
      <div v-if="gradedInfo" class="sp-result">
        <div class="result-header">
          <el-tag type="success" size="large">已评分</el-tag>
          <span class="result-score">总分：{{ gradedInfo.overallScore }}</span>
          <span v-if="gradedInfo.gradeLevel" class="result-level">{{ gradedInfo.gradeLevel }}</span>
          <span
            v-if="gradedInfo.overallComment"
            class="result-comment-top"
          >— {{ gradedInfo.overallComment }}</span>
        </div>
        <RadarChart
          v-if="radarData.length"
          :dimensions="radarData"
          title="能力雷达图"
          size="small"
          style="margin-top: 8px"
        />
      </div>

      <!-- 任务描述 + 参考图片 -->
      <div v-if="task" class="sp-task-info">
        <div class="sp-desc" v-html="taskDescHtml"></div>
        <div v-if="refImages.length" class="sp-ref-imgs">
          <el-image
            v-for="(url, i) in refImages"
            :key="i"
            :src="url"
            fit="cover"
            class="sp-ref-img"
            :preview-src-list="refImages"
            :initial-index="i"
          />
        </div>
      </div>

      <!-- 步骤列表 -->
      <div class="sp-steps" :class="{ 'sp-steps-scroll': isMobile }">
        <div
          v-for="(step, idx) in steps"
          :key="step.stepId || idx"
          class="step-card"
          :class="{ dragging: dragIdx === idx }"
        >
          <div class="step-header" @mousedown="onDragStart(idx)" @mouseup="onDragEnd">
            <span class="step-idx">步骤 {{ pad(step.stepIndex + 1) }}</span>
            <div class="step-actions">
              <el-button
                size="small"
                :disabled="idx === 0"
                text
                @click="moveStep(idx, -1)"
              >
                ↑
              </el-button>
              <el-button
                size="small"
                :disabled="idx === steps.length - 1"
                text
                @click="moveStep(idx, 1)"
              >
                ↓
              </el-button>
              <el-button
                size="small"
                type="danger"
                text
                @click="removeStep(idx)"
              >
                删除
              </el-button>
            </div>
          </div>
          <div class="step-body">
            <!-- 操作指引 -->
            <div v-if="step.description" class="step-guide">
              <div class="guide-title">💡 操作指引</div>
              <div class="guide-content" v-html="renderMarkdown(step.description)"></div>
            </div>
            <el-input
              v-model="step.title"
              placeholder="步骤标题"
              size="small"
              class="step-title"
              @change="debouncedSaveStep(step)"
            />
            <!-- 描述：有内容时渲染HTML，点击可编辑 -->
            <div
              v-if="!step._editing && step.description"
              class="step-desc-html"
              @dblclick="step._editing = true"
              v-html="md2html(step.description)"
            ></div>
            <el-input
              v-if="step._editing || !step.description"
              v-model="step.description"
              type="textarea"
              :rows="4"
              placeholder="操作说明..."
              size="small"
              @change="debouncedSaveStep(step)"
              @blur="step._editing = false"
            />
            <el-button
              v-if="isMobile"
              size="large"
              style="width: 100%; margin-bottom: 12px"
              @click="showUploadSheet = true"
            >
              <el-icon><Camera /></el-icon> 拍照或选择图片
            </el-button>
            <div class="step-uploads">
              <span class="upload-label">配图:</span>
              <el-upload
                :action="UPLOAD_ACTION"
                :headers="uploadHeaders"
                :file-list="step._imgList"
                :on-success="(r) => onImgSuccess(r, step)"
                :on-remove="(f) => onImgRemove(f, step)"
                :before-upload="beforeImgUpload"
                list-type="picture-card"
                accept=".jpg,.jpeg,.png,.gif"
                multiple
              >
                <el-icon><Plus /></el-icon>
              </el-upload>
            </div>
            <div class="step-uploads">
              <span class="upload-label">附件:</span>
              <el-upload
                :action="UPLOAD_ACTION"
                :headers="uploadHeaders"
                :file-list="step._fileList"
                :on-success="(r, f) => onFileSuccess(r, f, step)"
                :on-remove="(f) => onFileRemove(f, step)"
                :before-upload="beforeFileUpload"
                accept=".pdf,.docx,.xlsx,.zip,.rar"
              >
                <el-button size="small">上传附件</el-button>
              </el-upload>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部操作 -->
      <div class="sp-footer">
        <div class="footer-status">
          <span v-if="submitted && !gradedInfo">📤 已提交，等待教师评分</span>
          <span v-else-if="gradedInfo">✅ 已评分 — {{ gradedInfo.overallScore }}分</span>
          <span v-else>⚠ 尚未提交</span>
        </div>
        <div class="footer-actions">
          <el-button v-if="submitted && !gradedInfo" type="warning" @click="handleWithdraw">
            撤回已提交
          </el-button>
          <el-button @click="handleDraftSave">暂存</el-button>
          <el-button
            v-if="!submitted"
            type="success"
            :disabled="steps.length === 0"
            @click="handleSubmitAll"
          >
            提交全部步骤
          </el-button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import { useIsMobile } from '@/composables/useIsMobile';
import { usePracticeDraft } from '@/composables/usePracticeDraft';
import { renderMarkdown } from '@/utils/markdown';
import {
  createStep,
  updateStep,
  deleteStep,
  reorderSteps,
  listSteps,
  submitPractice,
  withdrawPractice,
} from '@/api/practice';
import { getTask } from '@/api/task';
import { getUploadHeaders } from '@/api/task';
import RadarChart from '@/components/common/RadarChart.vue';

const route = useRoute();
const router = useRouter();
const { isMobile } = useIsMobile();
const showUploadSheet = ref(false);
const taskId = Number(route.params.taskId);

const { saveDraft, loadDraft, clearAllDrafts } = usePracticeDraft(route.params.taskId);
const currentStepIndex = ref(0);
const loading = ref(false);
const submitting = ref(false);
const submitted = ref(false);
const task = ref(null);
const steps = ref([]);
const gradedInfo = ref(null);
const radarData = ref([]);
const dragIdx = ref(-1);

const UPLOAD_ACTION = '/api/upload/actions/practice';
const uploadHeaders = getUploadHeaders();

const refImages = computed(() => {
  if (!task.value?.referenceImages) return [];
  try {
    return typeof task.value.referenceImages === 'string'
      ? JSON.parse(task.value.referenceImages)
      : task.value.referenceImages;
  } catch {
    return [];
  }
});

const completedSteps = computed(() => steps.value.filter((s) => s._completed).length);
const progressPercent = computed(() =>
  steps.value.length === 0 ? 0 : Math.round((completedSteps.value / steps.value.length) * 100)
);
const statusText = computed(() => {
  if (gradedInfo.value) return '已评分';
  if (submitted.value) return '已提交';
  if (completedSteps.value > 0) return '进行中';
  return '未开始';
});
const statusClass = computed(() => ({
  'status-graded': gradedInfo.value,
  'status-submitted': submitted.value && !gradedInfo.value,
  'status-progress': !submitted.value && completedSteps.value > 0,
  'status-idle': !submitted.value && completedSteps.value === 0,
}));

function md2html(raw) {
  if (!raw) return '';
  let text = raw;
  // 先处理字面 \\n → 转为真实换行，再统一处理
  text = text.replace(/\\n/g, '\n');
  // HTML转义
  text = text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  // 标题
  text = text.replace(/^### (.+)$/gm, '<h4 class="md-h4">$1</h4>');
  text = text.replace(/^## (.+)$/gm, '<h3 class="md-h3">$1</h3>');
  // 粗体
  text = text.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
  // 行内代码
  text = text.replace(/`([^`]+)`/g, '<code class="md-code">$1</code>');
  // 水平线
  text = text.replace(/^---$/gm, '<hr class="md-hr">');
  // 编号列表
  text = text.replace(/^(\d+)\.\s+(.+)$/gm, '<li class="md-li">$2</li>');
  text = text.replace(/((?:<li class="md-li">.*<\/li>\n?)+)/g, '<ol class="md-ol">$1</ol>');
  // 高亮标签
  text = text.replace(
    /<strong>(任务描述|操作步骤|预期结果|检查点|Step-by-step)<\/strong>[：:]\s*/g,
    '<strong class="md-label">$1：</strong>'
  );
  // 双换行 → 段落
  text = text.replace(/\n\n/g, '</p><p class="md-p">');
  // 单换行 → <br>
  text = text.replace(/\n/g, '<br>');
  return '<p class="md-p">' + text + '</p>';
}

const taskDescHtml = computed(() => {
  if (!task.value?.description) return '';
  if (task.value.description.startsWith('{"steps"'))
    return '<p style="color:var(--text-secondary)">请重新发布实训方案以查看格式化的任务描述</p>';
  return md2html(task.value.description);
});

function pad(n) {
  return n < 10 ? '0' + n : String(n);
}

// 标记步骤完成状态
watch(
  steps,
  (val) => {
    val.forEach((s) => {
      s._completed = !!(s.title || s.images?.length || s.files?.length);
      s._returned = false;
    });
    const firstIncomplete = val.findIndex((s) => !s._completed);
    if (firstIncomplete >= 0 && currentStepIndex.value === 0)
      currentStepIndex.value = firstIncomplete;
  },
  { immediate: true }
);

// ── 加载 ──
async function load() {
  loading.value = true;
  try {
    const [tRes, sRes] = await Promise.all([getTask(taskId), listSteps(taskId)]);
    if (tRes.code === 200) task.value = tRes.data;
    if (sRes.code === 200) {
      const raw = sRes.data || [];
      if (raw.length > 0 && raw[0]._submission) {
        gradedInfo.value = raw[0]._submission;
        raw.shift();
        submitted.value = true;
        const gi = gradedInfo.value;
        if (gi && gi.skillScore !== undefined) {
          radarData.value = [
            { name: '技能水平', score: gi.skillScore || 0, max: 10 },
            { name: '职业素养', score: gi.profScore || 0, max: 10 },
            { name: '应用价值', score: gi.valueScore || 0, max: 10 },
            { name: '创新创意', score: gi.innovScore || 0, max: 10 },
            { name: '团队合作', score: gi.teamScore || 0, max: 10 },
          ];
        }
      } else {
        submitted.value = false;
      }
      steps.value = raw.map((s) => ({
        ...s,
        _imgList: (s.images || []).map((url, i) => ({ url, name: `img_${i}`, status: 'success' })),
        _fileList: (s.files || []).map((f, i) => ({
          url: f.url,
          name: f.name || f.url,
          status: 'success',
        })),
      }));
      for (const step of steps.value) {
        const draft = loadDraft(step.stepIndex);
        if (draft) {
          if (draft.title) step.title = draft.title;
          if (draft.description) step.description = draft.description;
          if (draft.images?.length) {
            step._imgList = draft.images.map((url, i) => ({
              url,
              name: `img_${i}`,
              status: 'success',
            }));
            step.images = draft.images;
          }
          if (draft.files?.length) {
            step._fileList = draft.files.map((f, i) => ({
              url: f.url,
              name: f.name || f.url,
              status: 'success',
            }));
            step.files = draft.files;
          }
        }
      }
    }
  } catch {
    ElMessage.error('加载失败');
  } finally {
    loading.value = false;
  }
}

// ── 步骤操作 ──
async function addStep() {
  try {
    const res = await createStep({ taskId, title: '', description: '', images: [], files: [] });
    if (res.code === 200) {
      steps.value.push({
        stepId: res.data.stepId,
        stepIndex: res.data.stepIndex,
        title: '',
        description: '',
        images: [],
        files: [],
        _imgList: [],
        _fileList: [],
      });
    }
  } catch {
    ElMessage.error('新增失败');
  }
}

let saveTimer = null;
function debouncedSaveStep(step) {
  clearTimeout(saveTimer);
  saveTimer = setTimeout(() => saveStep(step), 300);
}

async function saveStep(step) {
  if (!step.stepId) return;
  try {
    await updateStep(step.stepId, {
      title: step.title,
      description: step.description,
      images: step._imgList?.map((f) => f.url || f.response?.data?.url).filter(Boolean) || [],
      files:
        step._fileList
          ?.map((f) => ({ url: f.url || f.response?.data?.url, name: f.name, size: f.size }))
          .filter((f) => f.url) || [],
    });
  } catch {
    ElMessage.warning('保存失败，请稍后重试');
  }
}

async function removeStep(idx) {
  const step = steps.value[idx];
  if (!step.stepId) {
    steps.value.splice(idx, 1);
    return;
  }
  try {
    await ElMessageBox.confirm('确定删除该步骤？', '确认', { type: 'warning' });
    await deleteStep(step.stepId);
    steps.value.splice(idx, 1);
    // 重排后续 index
    steps.value.forEach((s, i) => (s.stepIndex = i));
    ElMessage.success('已删除');
  } catch {
    /* 取消 */
  }
}

async function moveStep(idx, dir) {
  const newIdx = idx + dir;
  if (newIdx < 0 || newIdx >= steps.value.length) return;
  const arr = [...steps.value];
  [arr[idx], arr[newIdx]] = [arr[newIdx], arr[idx]];
  steps.value = arr;
  steps.value.forEach((s, i) => (s.stepIndex = i));
  try {
    await reorderSteps(
      taskId,
      steps.value.map((s) => s.stepId)
    );
  } catch {
    /* */
  }
}

// ── 上传 ──
function beforeImgUpload(file) {
  return file.size <= 5 * 1024 * 1024 ? true : (ElMessage.warning('图片≤5MB'), false);
}
function beforeFileUpload(file) {
  return file.size <= 20 * 1024 * 1024 ? true : (ElMessage.warning('文件≤20MB'), false);
}
function onImgSuccess(res, step) {
  if (res.code === 200 && res.data?.url) {
    step._imgList = [...(step._imgList || []), { url: res.data.url, status: 'success' }];
    saveStep(step);
  }
}
function onImgRemove(file, step) {
  step._imgList = (step._imgList || []).filter(
    (f) => (f.url || f.response?.data?.url) !== (file.url || file.response?.data?.url)
  );
  saveStep(step);
}
function onFileSuccess(res, uploadFile, step) {
  if (res.code === 200 && res.data?.url) {
    step._fileList = [
      ...(step._fileList || []),
      { url: res.data.url, name: uploadFile?.name || '', status: 'success' },
    ];
    saveStep(step);
  }
}
function onFileRemove(file, step) {
  step._fileList = (step._fileList || []).filter(
    (f) => (f.url || f.response?.data?.url) !== (file.url || file.response?.data?.url)
  );
  saveStep(step);
}

// ── 拖拽占位 ──
function onDragStart(idx) {
  dragIdx.value = idx;
}
function onDragEnd() {
  dragIdx.value = -1;
}

// ── 提交 ──
async function doSubmit() {
  submitting.value = true;
  try {
    const res = await submitPractice(taskId);
    if (res.code === 200) {
      submitted.value = true;
      ElMessage.success('提交成功');
    } else ElMessage.error(res.message || '提交失败');
  } catch {
    ElMessage.error('提交失败');
  } finally {
    submitting.value = false;
  }
}

// ── 新课操作（暂存/撤回/提交全部）──
const handleDraftSave = () => {
  const step = steps.value[currentStepIndex.value];
  if (!step) return;
  saveDraft(currentStepIndex.value, {
    title: step.title,
    description: step.description,
    images: step.images,
    files: step.files,
  });
  ElMessage.success('已暂存');
};

const handleWithdraw = async () => {
  try {
    await ElMessageBox.confirm('撤回后需重新提交。', '确认撤回', { type: 'warning' });
    await withdrawPractice(route.params.taskId);
    submitted.value = false;
    ElMessage.success('已撤回');
  } catch {
    /* 取消 */
  }
};

const handleSubmitAll = async () => {
  try {
    await ElMessageBox.confirm('提交后未被评分前可以撤回修改。确定提交吗？', '确认提交', {
      type: 'warning',
    });
    for (const step of steps.value) {
      if (step.stepId) {
        await updateStep(step.stepId, {
          title: step.title,
          description: step.description,
          images: step._imgList?.map((f) => f.url || f.response?.data?.url).filter(Boolean) || [],
          files:
            step._fileList
              ?.map((f) => ({ url: f.url || f.response?.data?.url, name: f.name, size: f.size }))
              .filter((f) => f.url) || [],
        });
      }
    }
    await submitPractice(route.params.taskId);
    submitted.value = true;
    clearAllDrafts();
    ElMessage.success('实训已提交');
    load();
  } catch {
    /* 取消 */
  }
};

onMounted(() => load());
</script>

<style scoped>
.sp-page {
  max-width: 780px;
  margin: 0 auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: calc(100vh - 64px);
}
.sp-header {
  display: flex;
  align-items: center;
  gap: 12px;
}
.sp-header h3 {
  margin: 0;
  flex: 1;
}
.sp-task-info {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 14px;
}
.sp-desc {
  font-size: var(--fs-md);
  color: var(--text-secondary);
  line-height: 1.6;
}
.sp-desc :deep(.md-h3) {
  font-size: 17px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 20px 0 8px;
  padding-bottom: 6px;
  border-bottom: 2px solid var(--primary-color);
}
.sp-desc :deep(.md-h4) {
  font-size: var(--fs-md);
  font-weight: 600;
  color: var(--text-primary);
  margin: 16px 0 6px;
}
.sp-desc :deep(.md-p) {
  margin: 0 0 8px;
}
.sp-desc :deep(.md-label) {
  color: var(--primary-color);
}
.sp-desc :deep(.md-code) {
  background: var(--bg-section);
  padding: 1px 5px;
  border-radius: 3px;
  font-family: monospace;
  font-size: var(--fs-sm);
}
.sp-desc :deep(.md-hr) {
  border: none;
  border-top: 1px dashed var(--border-color);
  margin: 12px 0;
}
.sp-desc :deep(.md-ol) {
  margin: 4px 0 8px;
  padding-left: 24px;
}
.sp-desc :deep(.md-li) {
  margin-bottom: 3px;
}
.sp-ref-imgs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 10px;
}
.sp-ref-img {
  width: 100px;
  height: 80px;
  border-radius: 6px;
  object-fit: cover;
  cursor: pointer;
}
.sp-steps {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.sp-result {
  background: var(--bg-card);
  border: 1px solid var(--el-color-success);
  border-radius: var(--radius-md);
  padding: 16px;
}
.result-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.result-score {
  font-size: var(--fs-lg);
  font-weight: 700;
  color: var(--text-primary);
}
.result-level {
  font-size: var(--fs-md);
  font-weight: 600;
  color: var(--primary-color);
  margin-left: 4px;
}
.result-comment-top {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  margin-left: 8px;
}
.result-comment {
  font-size: var(--fs-md);
  color: var(--text-secondary);
  line-height: 1.6;
}
.step-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  overflow: hidden;
}
.step-card.dragging {
  border-color: var(--primary-color);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
.step-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 14px;
  background: var(--bg-section);
  border-bottom: 1px solid var(--border-light);
  cursor: grab;
}
.step-idx {
  font-size: var(--fs-md);
  font-weight: 600;
}
.step-actions {
  display: flex;
  gap: 2px;
}
.step-body {
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.step-title {
  max-width: 400px;
}
.step-desc-html {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  line-height: 1.7;
  padding: 6px 0;
  cursor: pointer;
}
.step-desc-html :deep(strong) {
  color: var(--primary-color);
  font-weight: 600;
}
.step-desc-html :deep(.md-label) {
  color: var(--primary-color);
  font-weight: 700;
}
.step-desc-html :deep(ol) {
  margin: 4px 0;
  padding-left: 20px;
}
.step-desc-html :deep(li) {
  margin-bottom: 2px;
}
.step-desc-html :deep(code) {
  background: var(--bg-section);
  padding: 1px 4px;
  border-radius: 2px;
  font-size: var(--fs-xs);
}
.step-desc-html :deep(h4) {
  font-size: var(--fs-md);
  margin: 8px 0 4px;
}
.step-uploads {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}
.upload-label {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  white-space: nowrap;
  padding-top: 6px;
  width: 36px;
}
.sp-footer {
  display: flex;
  justify-content: center;
  gap: 12px;
  padding: 8px 0 24px;
}

.sk-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 8px 0;
}
.sk-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 16px;
}
.sk-line {
  height: 14px;
  background: var(--bg-secondary);
  border-radius: var(--radius-xs);
  position: relative;
  overflow: hidden;
}
.sk-line::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.4), transparent);
  animation: sk-shimmer 1.6s infinite;
}
@keyframes sk-shimmer {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}
.w-30 {
  width: 30%;
}
.w-60 {
  width: 60%;
}
.w-70 {
  width: 70%;
}
.w-90 {
  width: 90%;
}

@media (max-width: 768px) {
  .sp-steps-scroll {
    display: flex;
    gap: 8px;
    overflow-x: auto;
    padding: 8px 0;
    -webkit-overflow-scrolling: touch;
  }
}

@media (max-width: 768px) {
  :deep(.el-form-item .el-input),
  :deep(.el-form-item .el-select) {
    width: 100%;
  }
}

.practice-overview {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
.overview-card {
  flex: 1;
  min-width: 100px;
  padding: 14px;
  background: var(--el-bg-color);
  border-radius: 8px;
  border: 1px solid var(--el-border-color);
  text-align: center;
}
.ov-label {
  display: block;
  font-size: 0.7rem;
  color: var(--el-text-color-secondary);
  text-transform: uppercase;
  margin-bottom: 4px;
}
.ov-value {
  font-weight: 600;
  font-size: 0.9rem;
}
.ov-big {
  font-size: 1.5rem;
}
.ov-green {
  color: #15803d;
}
.status-graded {
  color: #15803d;
}
.status-submitted {
  color: #f59e0b;
}
.status-progress {
  color: var(--primary-color);
}
.status-idle {
  color: var(--el-text-color-secondary);
}
.step-progress {
  margin-bottom: 24px;
}
.progress-track {
  width: 100%;
  height: 6px;
  background: var(--el-border-color);
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 12px;
}
.progress-fill {
  height: 100%;
  background: var(--primary-color);
  border-radius: 3px;
  transition: width 0.3s;
}
.step-nodes {
  display: flex;
  justify-content: space-between;
}
.step-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
}
.node-icon {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 700;
  background: #e5e7eb;
  color: #9ca3af;
}
.node-label {
  font-size: 0.7rem;
  margin-top: 4px;
  color: var(--el-text-color-secondary);
}
.node-done .node-icon {
  background: #15803d;
  color: #fff;
}
.node-done .node-label {
  color: #15803d;
}
.node-current .node-icon {
  background: var(--primary-color);
  color: #fff;
  box-shadow: 0 0 0 3px rgba(67, 97, 238, 0.2);
}
.node-current .node-label {
  color: var(--primary-color);
  font-weight: 600;
}
.node-returned .node-icon {
  background: #f59e0b;
  color: #fff;
}
.step-guide {
  padding: 12px 16px;
  background: #eff6ff;
  border-radius: 8px;
  border-left: 3px solid var(--primary-color);
  margin-bottom: 16px;
}
.guide-title {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--primary-color);
  margin-bottom: 4px;
}
.guide-content {
  font-size: 0.8rem;
  color: var(--el-text-color-regular);
  line-height: 1.8;
}
.sp-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding: 16px;
  background: var(--el-bg-color);
  border-radius: 8px;
  border: 1px solid var(--el-border-color);
}
.footer-status {
  font-size: 0.8rem;
  color: var(--el-text-color-secondary);
}
.footer-actions {
  display: flex;
  gap: 8px;
}
@media (max-width: 640px) {
  .practice-overview {
    gap: 8px;
  }
  .overview-card {
    min-width: 70px;
    padding: 10px;
  }
  .step-nodes {
    overflow-x: auto;
    gap: 12px;
    justify-content: flex-start;
    padding-bottom: 8px;
  }
  .step-node {
    flex-shrink: 0;
  }
  .sp-footer {
    flex-direction: column;
    gap: 10px;
    position: sticky;
    bottom: 0;
    z-index: 10;
  }
  .footer-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>

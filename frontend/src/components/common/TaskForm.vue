<template>
  <el-form
    ref="formRef"
    :model="form"
    :rules="rules"
    label-position="top"
    class="task-form"
  >
    <el-row :gutter="16">
      <el-col :span="24">
        <el-form-item label="任务标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入任务标题" maxlength="200" />
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :sm="12">
        <el-form-item label="任务类型" prop="taskType">
          <el-select
            v-model="form.taskType"
            placeholder="选择任务类型"
            style="width:100%"
            @change="onTaskTypeChange"
          >
            <el-option
              v-for="(label, key) in TASK_TYPE_FILTER_LABEL"
              :key="key"
              :value="key"
              :label="label"
            />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="评分体系" prop="scoreType">
          <el-select v-model="form.scoreType" placeholder="选择评分体系" style="width:100%">
            <el-option
              v-for="(label, key) in SCORE_TYPE_LABEL"
              :key="key"
              :value="key"
              :label="label"
            />
          </el-select>
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :sm="8">
        <el-form-item label="学科">
          <el-select
            v-model="form.subject"
            placeholder="选择学科"
            filterable
            clearable
            style="width:100%"
          >
            <el-option
              v-for="s in subjectOptions"
              :key="s.id"
              :value="s.subjectName"
              :label="s.subjectName"
            />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-form-item label="年级" prop="gradeId">
          <el-select
            v-model="form.gradeId"
            placeholder="选择年级"
            style="width:100%"
            @change="onGradeFilter"
          >
            <el-option
              v-for="g in gradeOptions"
              :key="g.id"
              :value="g.id"
              :label="g.gradeName"
            />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-form-item label="目标班级" prop="targetIds">
          <el-select
            v-model="form.targetIds"
            multiple
            placeholder="可多选班级"
            style="width:100%"
            :disabled="!form.gradeId"
          >
            <el-option
              v-for="c in filteredClassOptions"
              :key="c.id"
              :value="c.id"
              :label="c.className"
            />
          </el-select>
        </el-form-item>
      </el-col>
    </el-row>

    <el-form-item label="任务描述" prop="description">
      <MarkdownEditor v-model="form.description" placeholder="请输入任务描述&#10;支持：**加粗** *斜体* ##标题 -列表 `代码` [链接](url)" :rows="8" />
      <div class="char-hint">{{ (form.description || '').length }}/5000</div>
    </el-form-item>

    <el-row :gutter="16">
      <el-col :xs="24" :sm="12">
        <el-form-item label="截止时间">
          <el-date-picker
            v-model="form.deadline"
            type="datetime"
            placeholder="选择截止时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width:100%"
          />
        </el-form-item>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="满分">
          <el-input-number
            v-model="form.totalScore"
            :min="1"
            :max="300"
            :precision="1"
            style="width:100%"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 类型特有配置 -->
    <template v-if="isExamType">
      <el-divider content-position="left">考试配置</el-divider>
      <el-row :gutter="16">
        <el-col :xs="24" :sm="8">
          <el-form-item label="时长(分钟)">
            <el-input-number
              v-model="examConfig.durationMinutes"
              :min="10"
              :max="300"
              style="width:100%"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-form-item label="通过分数线">
            <el-input-number
              v-model="examConfig.passingScore"
              :min="0"
              :max="300"
              :precision="1"
              style="width:100%"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-form-item label="最大切屏次数">
            <el-input-number
              v-model="examConfig.maxWarnings"
              :min="0"
              :max="10"
              style="width:100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :xs="12" :sm="8">
          <el-form-item label="随机打乱题目">
            <el-switch v-model="examConfig.shuffleQuestions" />
          </el-form-item>
        </el-col>
        <el-col :xs="12" :sm="8">
          <el-form-item label="随机打乱选项">
            <el-switch v-model="examConfig.shuffleOptions" />
          </el-form-item>
        </el-col>
        <el-col :xs="12" :sm="8">
          <el-form-item label="允许重考">
            <el-switch v-model="examConfig.allowRetake" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-divider content-position="left">🛡 防作弊</el-divider>
      <el-row :gutter="16">
        <el-col :xs="12" :sm="8">
          <el-form-item label="全屏锁定">
            <el-switch v-model="examConfig.fullscreenLock" />
          </el-form-item>
        </el-col>
        <el-col :xs="12" :sm="8">
          <el-form-item label="禁用右键">
            <el-switch v-model="examConfig.disableContextMenu" />
          </el-form-item>
        </el-col>
        <el-col :xs="12" :sm="8">
          <el-form-item label="禁止复制粘贴">
            <el-switch v-model="examConfig.disableCopyPaste" />
          </el-form-item>
        </el-col>
      </el-row>
      <!-- 选题面板（仅考试类任务） -->
      <el-divider content-position="left">选题（题库）</el-divider>
      <TaskQuestionPicker v-model="selectedQuestionIds" />
    </template>

    <template v-if="isHomeworkType">
      <el-divider content-position="left">作业配置</el-divider>
      <el-row :gutter="16" align="middle">
        <el-col :xs="12" :sm="12">
          <el-form-item label="扣分比例">
            <el-input-number
              v-model="homeworkConfig.latePenaltyRatio"
              :min="0"
              :max="1"
              :step="0.1"
              :precision="1"
              style="width:130px"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="12" :sm="12">
          <el-form-item label="最大迟交">
            <div style="display:flex;align-items:center;gap:6px;">
              <el-input-number
                v-model="homeworkConfig.maxLateHours"
                :min="0"
                :max="168"
                style="width:130px"
              />
              <span style="font-size:var(--fs-sm);color:var(--text-secondary);white-space:nowrap;">小时</span>
            </div>
          </el-form-item>
        </el-col>
      </el-row>
    </template>

    <el-divider />
    <el-row :gutter="16" align="middle">
      <el-col :xs="12" :sm="8">
        <el-form-item>
          <el-switch v-model="homeworkConfig.allowLateSubmit" active-text="允许迟交" />
        </el-form-item>
      </el-col>
      <el-col :xs="12" :sm="8">
        <el-form-item prop="isRequired">
          <el-switch
            v-model="form.isRequired"
            :active-value="1"
            :inactive-value="0"
            active-text="必做"
            inactive-text="选做"
          />
        </el-form-item>
      </el-col>
      <el-col :xs="12" :sm="8">
        <el-form-item>
          <el-switch
            v-model="form.notifyParents"
            :active-value="1"
            :inactive-value="0"
            active-text="通知家长"
          />
        </el-form-item>
      </el-col>
    </el-row>
    <el-row :gutter="16" align="middle">
      <el-col :xs="12" :sm="12">
        <el-form-item>
          <el-switch
            v-model="form.allowResubmit"
            :active-value="1"
            :inactive-value="0"
            active-text="允许重交"
          />
        </el-form-item>
      </el-col>
      <el-col :xs="12" :sm="12">
        <el-form-item>
          <el-switch
            v-model="form.isCompetitionMode"
            :active-value="1"
            :inactive-value="0"
            active-text="竞赛模式"
          />
        </el-form-item>
      </el-col>
    </el-row>
    <el-row :gutter="16">
      <el-col :xs="24" :sm="10">
        <el-form-item label="五育标签">
          <el-select
            v-model="form.wuyuTag"
            placeholder="选择五育"
            clearable
            style="width:100%"
          >
            <el-option
              v-for="t in wuyuTags"
              :key="t.key"
              :value="t.key"
              :label="t.label"
            />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :xs="24" :sm="14">
        <el-form-item label="学期">
          <el-select
            v-model="form.termId"
            placeholder="选择学期"
            clearable
            style="width:100%"
          >
            <el-option
              v-for="t in termList"
              :key="t.id"
              :value="t.id"
              :label="t.name"
            />
          </el-select>
        </el-form-item>
      </el-col>
    </el-row>
    <el-row v-if="form.scoreType === 'CUSTOM_RUBRIC'" :gutter="16">
      <el-col :xs="24" :sm="12">
        <el-form-item label="量规预设">
          <el-select
            v-model="form.rubricId"
            placeholder="选择量规"
            clearable
            style="width:100%"
          >
            <el-option
              v-for="r in rubricList"
              :key="r.id"
              :value="r.id"
              :label="r.name"
            />
          </el-select>
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { TASK_TYPE_FILTER_LABEL, SCORE_TYPE_LABEL } from '@/constants/taskType'
import { useTaskForm } from '@/composables/useTaskForm'
import MarkdownEditor from './MarkdownEditor.vue'
import { getWuyuTags, getTermList, getRubricList } from '@/api/settings'
import TaskQuestionPicker from './TaskQuestionPicker.vue'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  modelValue: { type: Object, default: () => ({}) },
  subjectOptions: { type: Array, default: () => [] },
  gradeOptions: { type: Array, default: () => [] },
  classOptions: { type: Array, default: () => [] },
})

const emit = defineEmits(['update:modelValue'])

const subjectOpt = computed(() => props.subjectOptions)
const gradeOpt = computed(() => props.gradeOptions)
const classOpt = computed(() => props.classOptions)

const {
  formRef, form, examConfig, homeworkConfig,
  isExamType, isHomeworkType, rules, filteredClassOptions,
  onGradeFilter,
  onTaskTypeChange: _onTaskTypeChange, validate,
} = useTaskForm({ subjectOptions: subjectOpt, gradeOptions: gradeOpt, classOptions: classOpt })

const selectedQuestionIds = ref([])
const wuyuTags = ref([])
const termList = ref([])
const rubricList = ref([])
const userStore = useUserStore()
const schoolId = computed(() => userStore.schoolId || 1)

const loadWuyuTags = async () => {
  try { const r = await getWuyuTags(); if (r.code === 200) wuyuTags.value = r.data || [] }
  catch { /* */ }
}

const loadTerms = async () => {
  try { const r = await getTermList(schoolId.value); if (r.code === 200) termList.value = r.data || [] }
  catch { /* */ }
}

const loadRubrics = async () => {
  try { const r = await getRubricList(schoolId.value); if (r.code === 200) rubricList.value = r.data || [] }
  catch { /* */ }
}

onMounted(() => { loadWuyuTags(); loadTerms(); loadRubrics() })

const onTaskTypeChange = () => {
  _onTaskTypeChange()
  if (isExamType.value) selectedQuestionIds.value = []
}

watch([form, examConfig, homeworkConfig, selectedQuestionIds],
  () => emit('update:modelValue', { ...form, examConfig: { ...examConfig }, homeworkConfig: { ...homeworkConfig }, selectedQuestionIds: [...selectedQuestionIds.value] }),
  { deep: true })

defineExpose({ validate })
</script>

<style scoped>
.task-form { max-width: 900px; }
@media (max-width: 768px) {
  .task-form { padding: 0; }
  :deep(.el-form-item__label) { font-size: var(--fs-xs); }
}
</style>

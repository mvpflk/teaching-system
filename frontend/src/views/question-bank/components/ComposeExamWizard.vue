<template>
  <el-dialog
    v-model="visible"
    title="从试题篮组卷"
    width="700px"
    :close-on-click-modal="false"
    destroy-on-close
    append-to-body
  >
    <el-steps :active="step" simple style="margin-bottom:20px">
      <el-step title="配置赋分" icon="Edit" />
      <el-step title="预览确认" icon="Check" />
    </el-steps>

    <!-- Step 1: 配置赋分 -->
    <div v-show="step === 0" class="compose-body">
      <!-- 篮内题目分组 -->
      <div class="basket-review">
        <div class="section-title">试题篮题目（{{ basket.count }} 题）</div>
        <div v-for="(qs, type) in basket.byType" :key="type" class="type-group">
          <div class="type-group__header">
            <el-tag size="small" effect="plain">{{ QUESTION_TYPE_LABEL[type] || type }}</el-tag>
            <span class="type-group__count">{{ qs.length }} 题</span>
          </div>
          <div class="type-group__items">
            <div v-for="q in qs" :key="q.id" class="q-chip">
              <span class="q-chip__text">{{ q.questionText?.substring(0, 50) }}{{ q.questionText?.length > 50 ? '…' : '' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 基本配置 -->
      <el-form
        ref="formRef"
        :model="examForm"
        :rules="formRules"
        label-position="top"
      >
        <el-row :gutter="16">
          <el-col :span="14"><el-form-item label="试卷标题" prop="title"><el-input v-model="examForm.title" placeholder="如：期中测试" /></el-form-item></el-col>
          <el-col :span="10">
            <el-form-item label="学科">
              <el-select
                v-model="examForm.subjectId"
                filterable
                placeholder="选择学科"
                style="width:100%"
                @change="onSubjectIdChange"
              >
                <el-option
                  v-for="s in subjectOptions"
                  :key="s.subjectId"
                  :value="s.subjectId"
                  :label="s.subjectName"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="目标班级" prop="classIds" required>
              <div class="class-select-wrap">
                <el-select
                  v-model="examForm.classIds"
                  multiple
                  placeholder="可多选班级"
                  style="flex:1"
                >
                  <el-option
                    v-for="c in filteredClassOptions"
                    :key="c.id"
                    :value="c.id"
                    :label="c.grade ? c.grade + ' ' + c.className : c.className"
                  />
                </el-select>
                <el-button v-if="filteredClassOptions.length > 1" size="small" text type="primary" style="flex-shrink:0" @click="selectAllClasses">全选</el-button>
              </div>
              <div class="class-hint" :class="{ 'class-hint--warn': examForm.classIds.length === 0 }">
                {{ examForm.classIds.length ? `已选 ${examForm.classIds.length} 个班级` : '⚠ 未选择班级，发布后学生将看不到任务' }}
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="6"><el-form-item label="考试时长(分)"><el-input-number v-model="examForm.durationMinutes" :min="10" :max="180" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="及格分数"><el-input-number v-model="examForm.passingScore" :min="0" :max="200" /></el-form-item></el-col>
        </el-row>

        <el-divider />
        <div class="section-title">题型赋分（{{ basket.count }} 题）</div>
        <div v-for="(count, type) in typeCounts" :key="type" class="score-type-row">
          <span class="st-label">{{ QUESTION_TYPE_LABEL[type] || type }} ({{ count }}题)</span>
          <template v-if="type === 'ESSAY'">
            <span class="st-hint">每题单独赋分：</span>
          </template>
          <template v-else>
            <span class="st-hint">每题</span>
            <el-input-number
              v-model="typeScores[type]"
              :min="1"
              :max="50"
              size="small"
              style="width:80px"
            />
            <span class="st-hint">分 × {{ count }} = {{ (typeScores[type] || 0) * count }}分</span>
          </template>
        </div>
        <div v-if="typeCounts['ESSAY']" class="essay-scores">
          <div class="st-label mt-8">作答题逐题赋分：</div>
          <div v-for="q in essayQuestions" :key="q.id" class="essay-row">
            <span class="essay-text">{{ q.questionText?.substring(0, 40) }}{{ q.questionText?.length > 40 ? '…' : '' }}</span>
            <el-input-number
              v-model="essayScores[q.id]"
              :min="1"
              :max="50"
              size="small"
              style="width:80px"
            /> 分
          </div>
        </div>
        <div class="total-line">总分：<b>{{ computedTotal }}</b> 分</div>
      </el-form>
    </div>

    <!-- Step 2: 预览确认 -->
    <div v-show="step === 1" class="compose-body">
      <div class="compose-preview">
        <h4>{{ examForm.title || '新试卷' }}</h4>
        <p class="preview-meta">共 {{ basket.count }} 题 | 总分 {{ computedTotal }} 分 | {{ examForm.durationMinutes }} 分钟 | 及格 {{ examForm.passingScore }} 分</p>
        <div v-for="(q, qi) in selectedQuestions" :key="qi" class="preview-q">
          <span class="q-num">{{ qi + 1 }}.</span>
          <span class="q-type">[{{ QUESTION_TYPE_LABEL[q.questionType] }}]</span>
          <span class="q-text">{{ q.questionText }}</span>
          <span class="q-score">{{ getQScore(q) }}分</span>
        </div>
      </div>
    </div>

    <template #footer>
      <span v-if="step === 0">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :disabled="basket.count === 0" @click="goPreview">预览确认</el-button>
      </span>
      <span v-if="step === 1">
        <el-button @click="step = 0">上一步</el-button>
        <el-button type="primary" :loading="composing" @click="submitCompose">
          <el-icon style="margin-right:4px"><Check /></el-icon>确认并创建任务
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useQuestionBasketStore } from '@/stores/questionBasket'
import { useFormRules } from '@/composables/useFormRules'
import { getMyTeachingAssignments, getSubjects, getMySubjects } from '@/api/settings'
import { createTask } from '@/api/task'
import { getClassList } from '@/api/classes'
import { QUESTION_TYPE_LABEL } from '@/constants/questionTypes'

const props = defineProps({
  modelValue: Boolean,
})
const emit = defineEmits(['update:modelValue'])

const router = useRouter()
const userStore = useUserStore()
const basket = useQuestionBasketStore()
const { required: req } = useFormRules()
const formRules = {
  title: [req('试卷标题')],
  classIds: [{ type: 'array', required: true, message: '请至少选择一个目标班级', trigger: 'change' }],
}
const formRef = ref(null)

const visible = computed({ get: () => props.modelValue, set: (v) => emit('update:modelValue', v) })

const step = ref(0)
const composing = ref(false)
const examForm = reactive({ title: '', subjectId: null, durationMinutes: 60, passingScore: 60, classIds: [] })
const teachingAssignments = ref([])

// 超管：全部学科 + 全部班级
const allSubjects = ref([])
const allClasses = ref([])

// 学科选项（对齐标准 TaskCreatePage）
const subjectOptions = computed(() => {
  if (userStore.isSuperAdmin) return allSubjects.value
  const seen = new Set()
  const result = []
  for (const a of teachingAssignments.value) {
    const val = a.subjectId || a.subject
    if (!val || seen.has(val)) continue
    seen.add(val)
    result.push({ subjectId: val, subjectName: a.subject })
  }
  return result
})

// 班级选项（根据选中学科过滤）
const filteredClassOptions = computed(() => {
  if (userStore.isSuperAdmin) {
    return allClasses.value.map(c => ({ id: c.id, className: c.className, grade: c.grade || '' }))
  }
  if (!examForm.subjectId) {
    return teachingAssignments.value.map(a => ({ id: a.classId, className: a.className, grade: a.grade }))
  }
  return teachingAssignments.value
    .filter(a => {
      const aSubId = a.subjectId || a.subject
      return aSubId && aSubId === examForm.subjectId
    })
    .map(a => ({ id: a.classId, className: a.className, grade: a.grade }))
})

const onSubjectIdChange = () => { examForm.classIds = [] }

const selectAllClasses = () => {
  examForm.classIds = filteredClassOptions.value.map(c => c.id)
}

/** 进入预览步骤前验证表单 */
const goPreview = async () => {
  if (!formRef.value) { step.value = 1; return }
  try {
    await formRef.value.validate()
    step.value = 1
  } catch { /* 验证失败，el-form 会自动展示错误 */ }
}

// 赋分：从 basket store 读取题目
const typeScores = reactive({ SINGLE_CHOICE: 2, MULTI_CHOICE: 3, TRUE_FALSE: 1, FILL_IN: 1, DRAG_SORT: 5, MATCHING: 5, CLOZE: 10 })
const essayScores = reactive({})

// 篮内已选题目（已 hydrated）
const selectedQuestions = computed(() =>
  basket.ids.map(id => basket.hydrated[id]).filter(Boolean)
)

// 按题型统计
const typeCounts = computed(() => {
  const counts = {}
  for (const q of selectedQuestions.value) {
    const t = q.questionType || 'UNKNOWN'
    // 初始化未在 typeScores 中出现的题型默认分
    if (!(t in typeScores)) typeScores[t] = t === 'ESSAY' ? 10 : 5
    counts[t] = (counts[t] || 0) + 1
  }
  return counts
})

const essayQuestions = computed(() => selectedQuestions.value.filter(q => q.questionType === 'ESSAY'))

const computedTotal = computed(() => {
  let t = 0
  for (const q of selectedQuestions.value) {
    if (q.questionType === 'ESSAY') t += Number(essayScores[q.id] || 0)
    else t += Number(typeScores[q.questionType] || 0)
  }
  return t
})

const getQScore = (q) => q.questionType === 'ESSAY' ? (essayScores[q.id] || 0) : (typeScores[q.questionType] || 0)

// 初始化：加载任教数据 + 确保篮内数据已 hydrated
const init = async () => {
  // 守卫：空篮自动关闭
  if (!basket.count) {
    ElMessage.warning('试题篮为空，请先在题库中选择题目')
    visible.value = false
    return
  }

  step.value = 0
  examForm.title = ''; examForm.subjectId = null; examForm.durationMinutes = 60; examForm.passingScore = 60; examForm.classIds = []
  Object.keys(essayScores).forEach(k => delete essayScores[k])

  // 确保篮内题目数据已加载
  await basket.hydrate()

  // 初始化 ESSAY 逐题分值
  for (const q of essayQuestions.value) {
    if (!essayScores[q.id]) essayScores[q.id] = 10
  }

  // 自动填充学科（从篮内第一道题推断）
  if (!examForm.subjectId && selectedQuestions.value.length) {
    const firstSubject = selectedQuestions.value[0].subject
    const match = subjectOptions.value.find(s => s.subjectName === firstSubject)
    if (match) examForm.subjectId = match.subjectId
  }

  // 超管：加载全部学科+全部班级
  if (userStore.isSuperAdmin) {
    try {
      const [subRes, clsRes] = await Promise.all([getSubjects(), getClassList()])
      if (subRes.code === 200) allSubjects.value = (subRes.data || []).map(s => ({ subjectId: s.id, subjectName: s.subjectName }))
      if (clsRes.code === 200) allClasses.value = (clsRes.data.records || []).map(c => ({ id: c.id, className: c.className, grade: c.grade || '' }))
    } catch { /* */ }
    return
  }
  // 教师：加载任教配置
  try {
    const res = await getMyTeachingAssignments()
    if (res.code === 200) teachingAssignments.value = res.data || []
  } catch { /* */ }
}

// 提交创建任务
const submitCompose = async () => {
  if (composing.value || !basket.count) return

  // 最终校验：班级必选
  if (!examForm.classIds || examForm.classIds.length === 0) {
    ElMessage.warning('请至少选择一个目标班级，否则学生无法看到任务')
    return
  }

  composing.value = true
  try {
    const qids = [...basket.ids]

    // 构建 scorePresets（题型 → 分值）— 修复：必须传入，否则后端用硬编码默认值
    const scorePresets = {}
    for (const [type, score] of Object.entries(typeScores)) {
      if (typeCounts.value[type]) scorePresets[type] = score
    }
    // ESSAY 逐题赋分：存入 taskConfig，同时给后端一个默认 ESSAY 分值
    const essayScoresSnapshot = {}
    for (const q of essayQuestions.value) {
      essayScoresSnapshot[q.id] = essayScores[q.id] || 10
    }

    const matched = subjectOptions.value.find(s => s.subjectId === examForm.subjectId)
    const subjectName = matched ? matched.subjectName : ''

    const taskData = {
      title: examForm.title || '组卷试卷',
      subject: subjectName,
      subjectId: examForm.subjectId,
      taskBehavior: 'EXAM',
      scoreType: 'POINT_100',
      totalScore: computedTotal.value,
      targetType: 'CLASS',
      targetIds: examForm.classIds,
      questionIds: qids,
      scorePresets,
      taskConfig: JSON.stringify({
        durationMinutes: examForm.durationMinutes,
        passingScore: examForm.passingScore,
        essayScores: Object.keys(essayScoresSnapshot).length ? essayScoresSnapshot : undefined,
      }),
      description: `组卷创建，共${qids.length}题，总分${computedTotal.value}分`,
    }

    const res = await createTask(taskData)
    if (res.code === 200) {
      const taskId = res.data?.id || res.data?.taskId
      basket.clear()
      ElMessage.success('组卷成功！试题篮已清空，即将跳转任务编辑页')
      visible.value = false
      if (taskId) {
        router.push(`/teacher/tasks/${taskId}/edit`)
      } else {
        router.push('/teacher/tasks/list')
      }
    } else {
      ElMessage.error(res.message || '创建失败')
    }
  } catch {
    ElMessage.error('创建失败，请重试')
  } finally {
    composing.value = false
  }
}

// 弹窗打开时初始化
watch(() => props.modelValue, (v) => { if (v) init() }, { immediate: true })

defineExpose({ init })
</script>

<style scoped>
.compose-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 篮内题目预览区 */
.basket-review {
  max-height: 180px;
  overflow-y: auto;
  padding: 12px;
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  border: 0.5px solid var(--border-light);
}
.type-group {
  margin-bottom: 8px;
}
.type-group:last-child {
  margin-bottom: 0;
}
.type-group__header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.type-group__count {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}
.type-group__items {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  padding-left: 4px;
}
.q-chip {
  font-size: var(--fs-xs);
  color: var(--text-regular);
  background: var(--bg-card);
  border: 0.5px solid var(--border-light);
  border-radius: var(--radius-xs);
  padding: 2px 8px;
  max-width: 280px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.q-chip__text {
  white-space: nowrap;
}

/* 预览区 */
.compose-preview {
  max-height: 350px;
  overflow-y: auto;
}
.compose-preview .preview-meta {
  color: var(--text-secondary);
  font-size: var(--fs-sm);
  margin-bottom: 12px;
}
.compose-preview .preview-q {
  display: flex;
  gap: 6px;
  padding: 6px 0;
  border-bottom: 0.5px solid var(--border-light);
  font-size: var(--fs-sm);
  align-items: flex-start;
}
.compose-preview .q-num {
  color: var(--text-secondary);
  min-width: 24px;
}
.compose-preview .q-type {
  color: var(--primary-color);
  white-space: nowrap;
  font-size: var(--fs-xs);
}
.compose-preview .q-text {
  flex: 1;
}
.compose-preview .q-score {
  color: var(--warning-color);
  white-space: nowrap;
  font-size: var(--fs-xs);
}

/* 赋分区 */
.section-title {
  font-size: var(--fs-sm);
  font-weight: 600;
  margin-bottom: 8px;
}
.score-type-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.st-label {
  font-size: var(--fs-sm);
  font-weight: 500;
  min-width: 100px;
}
.st-hint {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}
.essay-scores {
  margin-top: 4px;
}
.essay-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.essay-text {
  flex: 1;
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}
.mt-8 {
  margin-top: 8px;
}
.total-line {
  text-align: right;
  font-size: var(--fs-base);
  margin-top: 8px;
  color: var(--primary-color);
}
.total-line b {
  font-size: var(--fs-xl);
}

.class-select-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}
.class-hint {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  margin-top: 4px;
  line-height: 1.4;
}
.class-hint--warn {
  color: var(--warning-color);
  font-weight: 500;
}

@media (max-width: 768px) {
  :deep(.el-form-item .el-input),
  :deep(.el-form-item .el-select) {
    width: 100%;
  }
}
</style>

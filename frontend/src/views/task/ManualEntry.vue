<template>
  <div v-loading="loading" class="manual-entry-page">
    <!-- 头部 -->
    <el-page-header class="mb-16" @back="$router.back()">
      <template #content>
        <span class="page-title">纸质答题卡录入</span>
        <el-tag v-if="task.title" size="small" class="ml-8" type="info">{{ task.title }}</el-tag>
      </template>
      <template #extra>
        <el-alert type="warning" :closable="false" show-icon class="header-alert">
          对照班专用 — 将学生纸质试卷答案逐题录入系统，用于后续数据对比分析
        </el-alert>
      </template>
    </el-page-header>

    <!-- 学生选择 -->
    <el-card shadow="never" class="mb-16">
      <el-row :gutter="16" align="middle">
        <el-col :span="8">
          <el-form-item label="选择学生" label-width="80px">
            <el-select
              v-model="selectedStudentId"
              filterable
              placeholder="搜索学生姓名"
              style="width:100%"
              @change="onStudentChange"
            >
              <el-option
                v-for="s in students"
                :key="s.id"
                :label="`${s.realName || s.name} (${s.className || ''})`"
                :value="s.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="4">
          <el-button
            type="primary"
            :disabled="!selectedStudentId || questions.length === 0"
            :loading="submitting"
            @click="handleSubmit"
          >
            提交录入
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 题目列表 -->
    <el-empty v-if="questions.length === 0" description="加载题目中..." />

    <div v-else class="question-list">
      <el-card
        v-for="(q, idx) in questions"
        :key="q.questionId || q.id"
        shadow="never"
        class="question-card"
      >
        <template #header>
          <div class="q-header">
            <span class="q-no">第 {{ idx + 1 }} 题</span>
            <el-tag size="small" :type="qTypeTag(q.questionType)">{{ qTypeLabel(q.questionType) }}</el-tag>
            <span class="q-score">({{ q.score || 0 }}分)</span>
          </div>
        </template>

        <!-- 题干 -->
        <div class="q-body" v-html="renderMarkdown(q.title || q.content || '')"></div>

        <!-- 客观题：答案录入 -->
        <div v-if="isObjective(q.questionType)" class="q-input">
          <!-- 单选题 -->
          <template v-if="q.questionType === 'SINGLE_CHOICE'">
            <el-radio-group v-model="answers[idx]" class="choice-group">
              <el-radio
                v-for="opt in parseOptions(q.options)"
                :key="opt.key"
                :value="opt.key"
              >{{ opt.key }}. {{ opt.text }}</el-radio>
              <el-radio value="">未答</el-radio>
            </el-radio-group>
          </template>

          <!-- 多选题 -->
          <template v-else-if="q.questionType === 'MULTI_CHOICE'">
            <el-checkbox-group v-model="answers[idx]" class="choice-group">
              <el-checkbox
                v-for="opt in parseOptions(q.options)"
                :key="opt.key"
                :value="opt.key"
              >{{ opt.key }}. {{ opt.text }}</el-checkbox>
            </el-checkbox-group>
            <div class="multi-hint">多选题 — 勾选所有正确选项</div>
          </template>

          <!-- 判断题 -->
          <template v-else-if="q.questionType === 'TRUE_FALSE'">
            <el-radio-group v-model="answers[idx]" class="choice-group">
              <el-radio value="对">✓ 对</el-radio>
              <el-radio value="错">✗ 错</el-radio>
              <el-radio value="">未答</el-radio>
            </el-radio-group>
          </template>

          <!-- 填空题 -->
          <template v-else-if="q.questionType === 'FILL_IN'">
            <el-input
              v-model="answers[idx]"
              placeholder="输入学生填写的答案"
              clearable
              style="max-width:400px"
            />
          </template>
        </div>

        <!-- 主观题：跳过错行，仅输入总分 -->
        <div v-else class="q-subjective">
          <el-alert type="info" :closable="false" show-icon class="subjective-alert">
            ← 此题为<strong>{{ qTypeLabel(q.questionType) }}</strong>，需手动批阅纸质试卷后在此输入总分
          </el-alert>
          <el-input-number
            v-model="subjectiveScores[idx]"
            :min="0"
            :max="q.score || 100"
            :precision="0.5"
            :step="0.5"
            placeholder="总分"
            style="width:140px;margin-top:8px"
          />
          <span class="score-max">/ {{ q.score || '--' }} 分</span>
        </div>
      </el-card>
    </div>

    <!-- 底部固定提交栏 -->
    <div v-if="questions.length > 0" class="bottom-bar">
      <el-button
        type="primary"
        size="large"
        :disabled="!selectedStudentId"
        :loading="submitting"
        @click="handleSubmit"
      >
        提交录入
      </el-button>
      <span class="bottom-hint">
        已录入 {{ answeredCount() }} / {{ questions.length }} 题
      </span>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTask, getTaskQuestions } from '@/api/task'
import { getStudents } from '@/api/classes'
import { manualEntryAnswers } from '@/api/research'
import { renderMarkdown } from '@/utils/markdown'

const route = useRoute()
const taskId = Number(route.params.taskId)

const loading = ref(true)
const submitting = ref(false)
const task = ref({})
const questions = ref([])
const students = ref([])
const selectedStudentId = ref(null)

// 客观题答案: answers[idx] = String (单选题/判断题/填空题) 或 Array (多选题)
const answers = reactive({})
// 主观题总分
const subjectiveScores = reactive({})

// ── 加载 ──
onMounted(async () => {
  try {
    const [taskRes, qRes] = await Promise.all([
      getTask(taskId),
      getTaskQuestions(taskId)
    ])
    task.value = taskRes.data || {}
    questions.value = (qRes.data || []).map((q, i) => ({
      ...q,
      _idx: i
    }))

    // 加载班级学生
    const classId = task.value.targetId
    if (classId) {
      const sRes = await getStudents(classId)
      students.value = (sRes.data || []).map(s => ({
        id: s.studentId || s.id,
        realName: s.realName || s.name || s.studentName,
        className: s.className || ''
      }))
    }
  } catch (e) {
    ElMessage.error('加载失败: ' + (e?.message || e))
  } finally {
    loading.value = false
  }
})

// ── 题型判断 ──
const objectiveTypes = ['SINGLE_CHOICE', 'MULTI_CHOICE', 'TRUE_FALSE', 'FILL_IN']
const isObjective = (type) => objectiveTypes.includes(type)

const qTypeLabel = (type) => ({
  SINGLE_CHOICE: '单选题',
  MULTI_CHOICE: '多选题',
  TRUE_FALSE: '判断题',
  FILL_IN: '填空题',
  SHORT_ANSWER: '简答题',
  COMPOSITION: '作文',
  CALCULATION: '计算题',
  PROOF: '证明题',
  CLOZE: '完形填空',
  READING_COMPREHENSION: '阅读理解'
}[type] || type || '未知')

const qTypeTag = (type) => {
  if (objectiveTypes.includes(type)) return 'success'
  return 'warning'
}

// ── 选项解析 ──
const parseOptions = (raw) => {
  if (!raw) return []
  if (Array.isArray(raw)) {
    return raw.map((o, i) => ({
      key: typeof o === 'string' ? String.fromCharCode(65 + i) : (o.key || o.label),
      text: typeof o === 'string' ? o : (o.text || o.label || o.value)
    }))
  }
  if (typeof raw === 'string') {
    try {
      const arr = JSON.parse(raw)
      return parseOptions(arr)
    } catch { return [] }
  }
  return []
}

// ── 交互 ──
const onStudentChange = () => {
  // 切换学生时清空已填答案
  Object.keys(answers).forEach(k => delete answers[k])
  Object.keys(subjectiveScores).forEach(k => delete subjectiveScores[k])
}

const answeredCount = () => {
  let count = 0
  questions.value.forEach((q, i) => {
    if (isObjective(q.questionType)) {
      const v = answers[i]
      if (v !== undefined && v !== null && v !== '' && (!Array.isArray(v) || v.length > 0)) count++
    } else {
      if (subjectiveScores[i] !== undefined && subjectiveScores[i] !== null) count++
    }
  })
  return count
}

const handleSubmit = async () => {
  if (!selectedStudentId.value) {
    ElMessage.warning('请先选择学生')
    return
  }

  // 构建答案列表
  const answerList = questions.value.map((q, i) => {
    const entry = {
      questionNo: i + 1,
      questionId: q.questionId || q.id
    }
    if (isObjective(q.questionType)) {
      const v = answers[i]
      entry.answer = Array.isArray(v) ? v.join(',') : (v || '')
    } else {
      // 主观题：answer 填总分
      entry.answer = subjectiveScores[i] !== undefined ? String(subjectiveScores[i]) : ''
    }
    return entry
  })

  submitting.value = true
  try {
    const res = await manualEntryAnswers(taskId, selectedStudentId.value, answerList)
    if (res.code === 200) {
      const data = res.data || {}
      const correct = data.autoGradeResult?.correct || 0
      const total = data.autoGradeResult?.total || 0
      ElMessage.success(`录入成功！客观题自动判分: ${correct}/${total}`)
    } else {
      ElMessage.error(res.message || '录入失败')
    }
  } catch (e) {
    ElMessage.error('录入失败: ' + (e?.message || e))
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.manual-entry-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 16px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
}

.header-alert {
  max-width: 420px;
}

.mb-16 { margin-bottom: 16px; }
.ml-8 { margin-left: 8px; }

.question-card {
  margin-bottom: 12px;
  border: 0.5px solid var(--border-color);
}

.q-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.q-no {
  font-weight: 600;
  color: var(--primary-color);
}

.q-score {
  color: var(--text-secondary);
  font-size: var(--fs-sm);
}

.q-body {
  margin-bottom: 12px;
  line-height: 1.6;
  font-size: var(--fs-md);
}

.q-body :deep(p) { margin: 0 0 4px; }

.choice-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.choice-group :deep(.el-radio), .choice-group :deep(.el-checkbox) {
  margin-right: 0;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.15s;
}

.choice-group :deep(.el-radio:hover), .choice-group :deep(.el-checkbox:hover) {
  background: var(--bg-section);
}

.multi-hint {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  margin-top: 4px;
}

.q-subjective {
  padding: 12px;
  background: var(--bg-warning-light);
  border-radius: 8px;
  border: 0.5px solid var(--border-color);
}

.score-max {
  margin-left: 8px;
  color: var(--text-secondary);
  font-size: var(--fs-sm);
}

.bottom-bar {
  position: sticky;
  bottom: 0;
  background: var(--bg-card);
  border-top: 0.5px solid var(--border-color);
  padding: 12px 16px;
  margin-top: 16px;
  display: flex;
  align-items: center;
  gap: 16px;
  z-index: 10;
}

.bottom-hint {
  color: var(--text-secondary);
  font-size: var(--fs-sm);
}
</style>

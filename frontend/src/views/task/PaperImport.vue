<template>
  <div class="page-card paper-import">
    <!-- ═══ 页面头部 ═══ -->
    <div class="page-header">
      <div class="header-left">
        <el-button text class="back-btn" @click="$router.push('/teacher/tasks/list')">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <h3 class="page-title">导入试卷</h3>
      </div>
    </div>

    <!-- ═══ 步骤1: 上传文件 ═══ -->
    <el-card shadow="never" class="import-card">
      <template #header><span>① 选择试卷文件</span></template>
      <el-form label-position="top" class="import-form">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="试卷标题">
              <el-input v-model="form.title" placeholder="自动取文件名" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="学科">
              <el-input v-model="form.subject" placeholder="如：计算机基础" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label=" ">
              <el-upload
                :show-file-list="false"
                :http-request="handleFileSelect"
                accept=".txt,.doc,.docx,.xlsx,.xls"
                :disabled="uploading"
              >
                <el-button type="primary" :loading="uploading" size="large">
                  <el-icon><Upload /></el-icon>
                  {{ uploading ? '解析中...' : '选择文件并上传' }}
                </el-button>
              </el-upload>
              <span class="upload-hint">支持 .txt / .docx / .xlsx</span>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- ═══ 步骤2: 预览（解析完成后显示） ═══ -->
    <template v-if="parsed">
      <!-- 题型统计 -->
      <el-card shadow="never" class="import-card">
        <template #header>
          <span>② 题型统计</span>
          <el-tag type="info" style="margin-left:12px">共 {{ parsed.totalCount }} 题</el-tag>
        </template>
        <div class="type-stats">
          <div v-for="(count, type) in parsed.typeStats" :key="type" class="type-stat-item">
            <span class="type-label">{{ typeLabel(type) }}</span>
            <span class="type-count">{{ count }} 题</span>
          </div>
        </div>
      </el-card>

      <!-- 题目预览（可折叠） -->
      <el-card shadow="never" class="import-card">
        <template #header>
          <span>③ 题目预览</span>
          <el-button text size="small" @click="showPreview = !showPreview">
            {{ showPreview ? '收起' : '展开' }}
          </el-button>
        </template>
        <div v-if="showPreview" class="preview-list">
          <div v-for="(q, i) in parsed.questions" :key="i" class="preview-item">
            <div class="preview-index">{{ i + 1 }}</div>
            <div class="preview-body">
              <el-tag size="small" :type="typeTag(q.questionType)">{{ typeLabel(q.questionType) }}</el-tag>
              <span class="preview-text">{{ truncate(q.questionText, 80) }}</span>
              <div v-if="q.options" class="preview-options">
                <span v-for="o in q.options" :key="o.label" class="preview-opt">{{ o.label }}. {{ o.text }}</span>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="preview-collapsed">
          共 {{ parsed.questions.length }} 道题目，<el-button text @click="showPreview = true">点击展开预览</el-button>
        </div>
      </el-card>

      <!-- ═══ 步骤3: 赋分设置 ═══ -->
      <el-card shadow="never" class="import-card">
        <template #header><span>④ 按题型设置分值</span></template>
        <div class="score-presets">
          <div v-for="(count, type) in parsed.typeStats" :key="type" class="score-row">
            <span class="score-label">{{ typeLabel(type) }}（{{ count }}题）</span>
            <el-input-number
              v-model="scorePresets[type]"
              :min="0.5"
              :max="100"
              :step="0.5"
              :precision="1"
              size="small"
              style="width:140px"
              @change="calcTotal"
            />
            <span class="score-subtotal">{{ (scorePresets[type] || 0) * count }} 分</span>
          </div>
          <el-divider />
          <div class="score-total">
            <strong>总分：{{ totalScore.toFixed(1) }} 分</strong>
            <el-tag v-if="Math.abs(totalScore - 100) < 0.01" type="success" size="small">满分100</el-tag>
            <el-tag v-else type="warning" size="small">非标准满分</el-tag>
          </div>
        </div>
      </el-card>

      <!-- ═══ 步骤4: 考试配置 ═══ -->
      <el-card shadow="never" class="import-card">
        <template #header><span>⑤ 考试配置</span></template>
        <el-form label-position="top" class="config-form">
          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="考试时长（分钟）">
                <el-input-number
                  v-model="examConfig.durationMinutes"
                  :min="5"
                  :max="300"
                  :step="5"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="及格线">
                <el-input-number v-model="examConfig.passingScore" :min="0" :max="100" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="6">
              <el-checkbox v-model="examConfig.shuffle">题目乱序</el-checkbox>
            </el-col>
            <el-col :span="6">
              <el-checkbox v-model="examConfig.antiCheat">开启防作弊</el-checkbox>
            </el-col>
            <el-col :span="6">
              <el-checkbox v-model="examConfig.showResult">交卷后显示成绩</el-checkbox>
            </el-col>
          </el-row>
        </el-form>
      </el-card>

      <!-- ═══ 步骤5: 选择目标班级 ═══ -->
      <el-card shadow="never" class="import-card">
        <template #header><span>⑥ 选择目标班级</span></template>
        <el-select
          v-model="form.targetIds"
          multiple
          filterable
          placeholder="请选择班级"
          style="width:100%"
        >
          <el-option
            v-for="c in classes"
            :key="c.id"
            :label="c.className || c.name"
            :value="c.id"
          />
        </el-select>
      </el-card>

      <!-- 提交按钮 -->
      <div class="form-actions">
        <div class="action-left">
          <el-checkbox v-model="saveToLibrary">保存到试卷库</el-checkbox>
          <el-checkbox v-model="markAsStandard">标记为标准化试卷</el-checkbox>
          <el-select
            v-if="markAsStandard"
            v-model="standardRole"
            size="small"
            style="width:140px;margin-left:8px"
          >
            <el-option label="前测试卷" value="PRETEST" />
            <el-option label="后测试卷" value="POSTTEST" />
            <el-option label="中期测试卷" value="MIDTEST" />
            <el-option label="通用标准化" value="COMMON" />
          </el-select>
        </div>
        <div class="action-buttons">
          <el-button @click="resetForm">重置</el-button>
          <el-button
            type="primary"
            size="large"
            :loading="submitting"
            @click="handleSubmit(false)"
          >
            创建任务（草稿）
          </el-button>
          <el-button
            type="success"
            size="large"
            :loading="submitting"
            @click="handleSubmit(true)"
          >
            创建并发布
          </el-button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { parsePaper, createPaperTask } from '@/api/paperImport'
import { markStandardized } from '@/api/research'
import { getMyClasses } from '@/api/classes'

// ── 状态 ──
const uploading = ref(false)
const submitting = ref(false)
const parsed = ref(null)
const showPreview = ref(false)
const saveToLibrary = ref(true)
const markAsStandard = ref(false)
const standardRole = ref('COMMON')
const classes = ref([])

const form = reactive({
  title: '',
  subject: '',
  targetIds: []
})

const scorePresets = reactive({})
const examConfig = reactive({
  durationMinutes: 120,
  passingScore: 60,
  shuffle: false,
  antiCheat: true,
  showResult: true
})

const totalScore = ref(100)

// ── 生命周期 ──
onMounted(async () => {
  try {
    const res = await getMyClasses()
    if (res.code === 200) classes.value = res.data || []
    else classes.value = []
  } catch { classes.value = [] }
})

// ── 方法 ──

/** 上传并解析 */
const handleFileSelect = async (options) => {
  const file = options.file
  if (!file) return
  if (!form.subject) { ElMessage.warning('请先填写学科'); return }

  const fd = new FormData()
  fd.append('file', file)
  if (form.title) fd.append('title', form.title)
  fd.append('subject', form.subject)

  uploading.value = true
  try {
    const res = await parsePaper(fd)
    if (res.code === 200) {
      parsed.value = res.data
      // 初始化分值预设
      const stats = res.data.typeStats || {}
      Object.keys(stats).forEach(k => {
        if (!(k in scorePresets)) scorePresets[k] = defaultScore(k)
      })
      if (!form.title) form.title = res.data.title || file.name.replace(/\.[^.]+$/, '')
      calcTotal()
      showPreview.value = true
      ElMessage.success(`解析成功，共识别 ${res.data.totalCount} 道题目`)
    } else {
      ElMessage.error(res.message || '解析失败')
    }
  } catch (e) {
    ElMessage.error('解析失败: ' + (e.message || e))
  } finally {
    uploading.value = false
  }
}

/** 计算总分 */
const calcTotal = () => {
  const stats = parsed.value?.typeStats || {}
  let t = 0
  Object.entries(scorePresets).forEach(([type, score]) => {
    const count = stats[type] || 0
    t += (Number(score) || 0) * count
  })
  totalScore.value = t
}

/** 提交 */
const handleSubmit = async (publishNow) => {
  if (!parsed.value) { ElMessage.warning('请先上传并解析试卷'); return }
  if (!form.targetIds.length) { ElMessage.warning('请选择目标班级'); return }

  submitting.value = true
  try {
    const payload = {
      title: form.title || '导入试卷',
      subject: form.subject,
      questions: parsed.value.questions,
      scorePresets: { ...scorePresets },
      examConfig: { ...examConfig },
      targetIds: form.targetIds,
      saveToLibrary: saveToLibrary.value,
      publishNow
    }
    const res = await createPaperTask(payload)
    if (res.code === 200) {
      const msg = publishNow ? '任务已创建并发布' : '任务已创建（草稿）'
      ElMessage.success(msg)

      // 如果勾选了标准化标记，创建成功后标记试卷
      if (markAsStandard.value && res.data.paperId) {
        try {
          await markStandardized(res.data.paperId, standardRole.value)
          ElMessage.success('已标记为标准化试卷')
        } catch (e) {
          ElMessage.warning('试卷已保存但标记失败：' + (e?.message || e))
        }
      }

      // 跳转到任务管理页
      setTimeout(() => { window.location.href = '/#/teacher/tasks/list' }, 800)
    } else {
      ElMessage.error(res.message || '创建失败')
    }
  } catch (e) {
    ElMessage.error('创建失败: ' + (e.message || e))
  } finally {
    submitting.value = false
  }
}

/** 重置 */
const resetForm = () => {
  ElMessageBox.confirm('将清空所有已填内容，确定？', '确认重置').then(() => {
    parsed.value = null
    Object.keys(scorePresets).forEach(k => delete scorePresets[k])
    Object.assign(examConfig, { durationMinutes: 120, passingScore: 60, shuffle: false, antiCheat: true, showResult: true })
    form.title = ''
    form.subject = ''
    form.targetIds = []
    saveToLibrary.value = true
    markAsStandard.value = false
    standardRole.value = 'COMMON'
    totalScore.value = 100
    showPreview.value = false
  }).catch(() => {})
}

// ── 工具函数 ──

const typeLabel = (type) => ({
  SINGLE_CHOICE: '单选题',
  MULTI_CHOICE: '多选题',
  TRUE_FALSE: '判断题',
  FILL_IN: '填空题',
  SHORT_ANSWER: '简答题',
  ESSAY: '论述题',
  DRAG_SORT: '排序题',
  MATCHING: '匹配题',
  CLOZE: '完形填空'
}[type] || type || '未知')

const typeTag = (type) => ({
  SINGLE_CHOICE: 'primary',
  MULTI_CHOICE: 'warning',
  TRUE_FALSE: 'success',
  FILL_IN: 'info',
  SHORT_ANSWER: '',
  ESSAY: 'danger'
}[type] || '')

const defaultScore = (type) => ({
  SINGLE_CHOICE: 2,
  MULTI_CHOICE: 3,
  TRUE_FALSE: 1,
  FILL_IN: 1,
  SHORT_ANSWER: 6,
  ESSAY: 10,
  DRAG_SORT: 4,
  MATCHING: 4,
  CLOZE: 1
}[type] || 2)

const truncate = (text, max) => {
  if (!text) return ''
  return text.length > max ? text.slice(0, max) + '...' : text
}
</script>

<style scoped>
.paper-import { max-width: 960px; margin: 0 auto; }
.import-card { margin-bottom: 16px; }
.import-form .upload-hint { display: block; color: var(--text-secondary); font-size: var(--fs-xs); margin-top: 4px; }
.type-stats { display: flex; flex-wrap: wrap; gap: 12px; }
.type-stat-item { display: flex; align-items: center; gap: 8px; padding: 8px 16px; background: var(--bg-section); border-radius: 6px; }
.type-label { font-weight: 500; }
.type-count { color: var(--primary-color); font-weight: 600; }
.preview-list { max-height: 400px; overflow-y: auto; }
.preview-item { display: flex; gap: 10px; padding: 8px 0; border-bottom: 1px solid var(--border-light); }
.preview-index { width: 28px; text-align: center; color: var(--text-secondary); font-weight: 600; flex-shrink: 0; }
.preview-body { flex: 1; min-width: 0; }
.preview-text { margin-left: 6px; }
.preview-options { margin-top: 4px; display: flex; flex-wrap: wrap; gap: 8px; }
.preview-opt { font-size: var(--fs-sm); color: var(--text-regular); }
.preview-collapsed { color: var(--text-secondary); }
.score-presets { max-width: 500px; }
.score-row { display: flex; align-items: center; gap: 16px; margin-bottom: 10px; }
.score-label { width: 140px; flex-shrink: 0; }
.score-subtotal { color: var(--text-secondary); font-size: var(--fs-sm); }
.score-total { font-size: var(--fs-lg); }
.config-form { max-width: 600px; }
.form-actions { display: flex; justify-content: space-between; align-items: center; padding: 20px 0; }
.action-left { display: flex; align-items: center; gap: 8px; }
.action-buttons { display: flex; gap: 10px; }
</style>

<template>
  <div class="compose-tab">
    <!-- 知识点选取 + 题型数量 -->
    <div class="ckp-builder-card">
      <div class="ckp-cascade">
        <div class="ckp-cascade__item">
          <span class="ckp-cascade__label">学科</span>
          <el-select
            v-model="composeDraft.l1"
            placeholder="选择学科/单元"
            size="small"
            class="ckp-cascade__select"
            @change="onCompL1"
          >
            <el-option
              v-for="n in composeL1"
              :key="n.id"
              :label="n.name"
              :value="n.id"
            />
          </el-select>
        </div>
        <el-icon class="ckp-cascade__arrow"><ArrowRight /></el-icon>
        <div class="ckp-cascade__item">
          <span class="ckp-cascade__label">章节</span>
          <el-select
            v-model="composeDraft.l2"
            placeholder="选择章节"
            size="small"
            :disabled="!composeDraft.l1"
            class="ckp-cascade__select"
            @change="onCompL2"
          >
            <el-option
              v-for="n in composeL2"
              :key="n.id"
              :label="n.name"
              :value="n.id"
            />
          </el-select>
        </div>
        <el-icon class="ckp-cascade__arrow"><ArrowRight /></el-icon>
        <div class="ckp-cascade__item">
          <span class="ckp-cascade__label">任务</span>
          <el-select
            v-model="composeDraft.l3"
            placeholder="选择任务"
            size="small"
            :disabled="!composeDraft.l2"
            class="ckp-cascade__select"
            @change="onCompL3"
          >
            <el-option
              v-for="n in composeL3Task"
              :key="n.id"
              :label="n.name"
              :value="n.id"
            />
          </el-select>
        </div>
        <el-icon class="ckp-cascade__arrow"><ArrowRight /></el-icon>
        <div class="ckp-cascade__item">
          <span class="ckp-cascade__label">知识点</span>
          <el-select
            v-model="composeDraft.catId"
            placeholder="选择知识点"
            size="small"
            :disabled="!composeDraft.l3"
            class="ckp-cascade__select"
          >
            <el-option
              v-for="n in composeL4"
              :key="n.id"
              :label="n.name"
              :value="n.id"
            />
          </el-select>
        </div>
      </div>
      <el-divider style="margin:8px 0" />
      <div class="ckp-type-row">
        <span class="ckp-type-row__label">题型数量</span>
        <div class="ckp-type-items">
          <span class="ckp-type-pill">单选<el-input-number
            v-model="composeDraft.ns"
            :min="0"
            :max="50"
            size="small"
            controls-position="right"
          /></span>
          <span class="ckp-type-pill">多选<el-input-number
            v-model="composeDraft.nm"
            :min="0"
            :max="50"
            size="small"
            controls-position="right"
          /></span>
          <span class="ckp-type-pill">判断<el-input-number
            v-model="composeDraft.nt"
            :min="0"
            :max="50"
            size="small"
            controls-position="right"
          /></span>
          <span class="ckp-type-pill">填空<el-input-number
            v-model="composeDraft.nf"
            :min="0"
            :max="50"
            size="small"
            controls-position="right"
          /></span>
        </div>
        <el-select
          v-model="composeDraft.knowledgeDim"
          size="small"
          style="width:90px;margin-left:8px"
          placeholder="考纲维度"
        >
          <el-option label="全部" value="BOTH" />
          <el-option label="应知" value="THEORY" />
          <el-option label="应会" value="PRACTICE" />
        </el-select>
        <el-button
          type="primary"
          size="small"
          :loading="composeLoading"
          :disabled="composeDraftTotal===0 || !hasCategory"
          @click="doComposeMatch"
        >
          <el-icon><Search /></el-icon> 从题库匹配
        </el-button>
        <el-button
          size="small"
          :loading="composeLoading"
          :disabled="composeDraftTotal===0 || !hasCategory"
          @click="doComposeAi"
        >
          <el-icon><MagicStick /></el-icon> AI补充
        </el-button>
      </div>
    </div>

    <!-- AI 进度指示 -->
    <div v-if="composeAiProgress" class="ai-progress-bar">
      <el-progress
        :percentage="composeAiProgress.percent"
        :stroke-width="16"
        :text-inside="true"
        :status="composeAiProgress.status"
      />
      <div class="ai-progress-text">{{ composeAiProgress.text }}</div>
    </div>

    <!-- 最近生成结果（可预览/编辑/移除） -->
    <div v-if="lastResults.length" class="compose-results">
      <div class="compose-group-hdr">
        最近生成 ({{ lastResults.length }}题 · 已自动加入试卷)
        <el-button
          size="small"
          text
          type="danger"
          style="margin-left:8px"
          @click="clearLastResults"
        >
          清除
        </el-button>
      </div>
      <div
        v-for="q in lastResults"
        :key="q.id || q._tmpId"
        class="ai-card"
        :class="{ 'ai-similar': q.similarity === 'low' }"
      >
        <div class="ai-card-header">
          <el-tag size="small" :type="q.source==='bank'?'success':'warning'">{{ q.source==='bank'?'题库':'AI' }}</el-tag>
          <el-tag size="small" :type="q.status===1?'success':q.status===0?'warning':'info'">{{ STATUS_LABELS[q.status] || '未知' }}</el-tag>
          <el-tag size="small" type="info">{{ TYPE_LABELS[q.questionType] || q.questionType }}</el-tag>
          <span class="ckp-src">{{ q._kpLabel }}</span>
        </div>
        <!-- 编辑模式 -->
        <template v-if="q._editing">
          <div class="ai-edit-input">
            <el-input
              v-model="q.questionText"
              size="small"
              type="textarea"
              :rows="2"
            />
          </div>
          <div v-if="q.options" class="ai-edit-opts">
            <div v-for="(opt, oi) in parseComposeOpts(q.options)" :key="oi" class="ai-edit-opt">
              <span class="opt-letter">{{ String.fromCharCode(65+oi) }}.</span>
              <el-input v-model="q.options[oi]" size="small" style="width:100%" />
            </div>
          </div>
          <div class="ai-edit-footer">
            <el-button size="small" type="success" @click="saveComposeEdit(q)">保存</el-button>
            <el-button size="small" @click="q._editing=false">取消</el-button>
          </div>
        </template>
        <!-- 查看模式 -->
        <template v-else>
          <div class="ai-card-body">{{ q.questionText }}</div>
          <div v-if="q.options" class="ai-card-options">
            <span
              v-for="(opt, oi) in parseComposeOpts(q.options)"
              :key="oi"
              class="ai-opt"
              :class="{ 'ai-opt-correct': q.correctAnswer && String.fromCharCode(65+oi) === String(q.correctAnswer).toUpperCase() }"
            >
              {{ String.fromCharCode(65+oi) }}. {{ stripOptPrefix(opt) }}
            </span>
          </div>
        </template>
      </div>
    </div>

    <!-- 空状态引导 -->
    <div v-if="!lastResults.length && !composeAiProgress" class="compose-guide">
      <div class="compose-guide__icon">
        <el-icon><Search /></el-icon>
      </div>
      <div class="compose-guide__title">按知识点组题</div>
      <div class="compose-guide__text">选择学科 → 章节 → 任务 → 知识点，设置各题型数量，点击"从题库匹配"或"AI补充"</div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowRight, Search, MagicStick } from '@element-plus/icons-vue'
import { matchQuestions } from '@/api/questionBank'
import { getMySubjects } from '@/api/settings'
import { pollAiTask } from '@/api/ai'
import { submitAiOutput, getAiOutputResult } from '@/api/aiOutput'
import { getNodeTree } from '@/api/knowledgeNode'

defineOptions({ name: 'FreeQuestionComposer' })

const props = defineProps({ modelValue: { type: Array, default: () => [] } })
const emit = defineEmits(['update:modelValue'])

const TYPE_SHORT = { SINGLE_CHOICE: '单选', MULTI_CHOICE: '多选', TRUE_FALSE: '判断', FILL_IN: '填空' }
const TYPE_LABELS = { SINGLE_CHOICE: '单选题', MULTI_CHOICE: '多选题', TRUE_FALSE: '判断题', FILL_IN: '填空题' }
const STATUS_LABELS = { 0: 'AI草稿', 1: '正式', 2: '已修改' }

const composeL1 = ref([])        // 学科
const composeL2 = ref([])        // 章节
const composeL3Task = ref([])    // 任务（新增第3级）
const composeL4 = ref([])        // 知识点（原composeL3）
const composeDraft = reactive({ l1: null, l2: null, l3: null, catId: null, ns: 0, nm: 0, nt: 0, nf: 0, knowledgeDim: 'BOTH' })
const composeDraftTotal = computed(() => composeDraft.ns + composeDraft.nm + composeDraft.nt + composeDraft.nf)
const composeLoading = ref(false)
const composeAiProgress = ref(null)
const lastResults = ref([])

/** 是否有任一分类级别被选中 */
const hasCategory = computed(() => !!(composeDraft.catId || composeDraft.l3 || composeDraft.l2 || composeDraft.l1))

const onCompL1 = (id) => {
  composeDraft.l2 = null; composeDraft.l3 = null; composeDraft.catId = null
  composeL2.value = []; composeL3Task.value = []; composeL4.value = []
  if (id) { const node = composeL1.value.find(n => n.id === id); composeL2.value = node?.children || [] }
}
const onCompL2 = (id) => {
  composeDraft.l3 = null; composeDraft.catId = null
  composeL3Task.value = []; composeL4.value = []
  if (id) { const node = composeL2.value.find(n => n.id === id); composeL3Task.value = node?.children || [] }
}
const onCompL3 = (id) => {
  composeDraft.catId = null
  composeL4.value = []
  if (id) { const node = composeL3Task.value.find(n => n.id === id); composeL4.value = node?.children || [] }
}

// 构建知识点对象 —— categoryId 取最深一级
const buildKp = () => {
  const tc = {}
  if (composeDraft.ns > 0) tc.SINGLE_CHOICE = composeDraft.ns
  if (composeDraft.nm > 0) tc.MULTI_CHOICE = composeDraft.nm
  if (composeDraft.nt > 0) tc.TRUE_FALSE = composeDraft.nt
  if (composeDraft.nf > 0) tc.FILL_IN = composeDraft.nf
  if (!Object.keys(tc).length) return null
  const categoryId = composeDraft.catId || composeDraft.l3 || composeDraft.l2 || composeDraft.l1
  if (!categoryId) return null
  const cat = [...composeL4.value, ...composeL3Task.value, ...composeL2.value, ...composeL1.value].find(c => c.id === categoryId)
  return { categoryId, typeCounts: tc, _label: cat?.name || '', _idx: 0, knowledgeDim: composeDraft.knowledgeDim || 'BOTH' }
}

// 将题目ID加入试卷
const addToExam = (ids) => {
  const current = [...props.modelValue]
  let added = 0
  ids.forEach(id => { if (!current.includes(id)) { current.push(id); added++ } })
  emit('update:modelValue', current)
  return added
}

// 从题库匹配
const doComposeMatch = async () => {
  const kp = buildKp()
  if (!kp) return
  composeLoading.value = true; lastResults.value = []
  try {
    const res = await matchQuestions({ knowledgePoints: [kp] })
    if (res.code === 200) {
      const questions = res.data?.questions || res.data || []
      questions.forEach((q, i) => {
        q.source = 'bank'
        q._kpLabel = kp._label
        q._kpIdx = 0
      })
      lastResults.value = questions
      const ids = questions.map(q => q.id).filter(Boolean)
      const added = addToExam(ids)
      ElMessage.success(`已匹配 ${questions.length} 题，${added} 题已加入试卷`)
    }
  } catch { ElMessage.error('匹配失败') }
  finally { composeLoading.value = false }
}

// AI补充
const doComposeAi = async () => {
  const kp = buildKp()
  if (!kp) return
  composeLoading.value = true; lastResults.value = []
  const typeNames = []
  if (kp.typeCounts.SINGLE_CHOICE) typeNames.push(`单选${kp.typeCounts.SINGLE_CHOICE}题`)
  if (kp.typeCounts.MULTI_CHOICE) typeNames.push(`多选${kp.typeCounts.MULTI_CHOICE}题`)
  if (kp.typeCounts.TRUE_FALSE) typeNames.push(`判断${kp.typeCounts.TRUE_FALSE}题`)
  if (kp.typeCounts.FILL_IN) typeNames.push(`填空${kp.typeCounts.FILL_IN}题`)

  try {
    const { result } = await pollAiTask(() => submitAiOutput({
      contentType: 'COMPREHENSIVE_EXERCISES',
      categoryId: kp.categoryId,
      knowledgePoint: kp._label,
      typeCounts: kp.typeCounts
    }), { getResult: getAiOutputResult })
    const questions = result?.questions || result || []
    if (Array.isArray(questions) && questions.length) {
      questions.forEach((q, i) => {
        q.source = 'ai'
        q._kpLabel = kp._label
        q._kpIdx = 0
        q._tmpId = 'ai_' + Date.now() + '_' + i
      })
      lastResults.value = questions
      const ids = questions.map(q => q.id).filter(Boolean)
      const added = addToExam(ids)
      ElMessage.success(`AI已生成 ${questions.length} 题，${added} 题已加入试卷`)
    } else {
      ElMessage.warning('AI未能生成有效题目，请重试')
    }
  } catch (e) {
    ElMessage.error('AI生成失败：' + (e.message || '请重试'))
  }
  finally { composeLoading.value = false }
}

const clearLastResults = () => { lastResults.value = [] }
const parseComposeOpts = (opts) => { try { return JSON.parse(typeof opts === 'string' ? opts : '[]') } catch { return Array.isArray(opts) ? opts : [] } }
const stripOptPrefix = (opt) => { if (!opt) return ''; return opt.replace(/^[A-D][.、)]?\s*/, '') }
const saveComposeEdit = (q) => { q._editing = false; ElMessage.success('已保存') }

onMounted(async () => {
  try {
    const [treeRes, subjRes] = await Promise.all([getNodeTree(), getMySubjects()])
    // getNodeTree 返回树形结构 List<{id,name,children:[...]}>
    const treeData = treeRes.code === 200 ? (treeRes.data || []) : (treeRes || [])
    // 按教师任教科目过滤一级分类（学科）
    const mySubjects = subjRes.code === 200 ? (subjRes.data || []).map(s => (s.subjectName || '').trim()) : []
    let allL1 = treeData
    if (mySubjects.length) {
      allL1 = treeData.filter(c => mySubjects.some(s => s && (c.name || '').includes(s) || s.includes(c.name || '')))
      if (!allL1.length) allL1 = treeData // 无匹配时保留全部，避免空白
    }
    composeL1.value = allL1
  } catch { /* */ }
})
</script>

<style scoped>
/* 构建卡片 */
.ckp-builder-card {
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: var(--bg-section);
  padding: 12px 14px 10px;
}

/* 四级级联 */
.ckp-cascade { display: flex; align-items: flex-end; gap: 6px; }
.ckp-cascade__item { display: flex; flex-direction: column; gap: 3px; flex: 1; min-width: 0; }
.ckp-cascade__label { font-size: 10px; color: var(--text-secondary); font-weight: 500; padding-left: 2px; }
.ckp-cascade__select { width: 100%; }
.ckp-cascade__arrow { color: var(--text-secondary); font-size: var(--fs-lg); flex-shrink: 0; margin-bottom: 3px; }

/* 题型数量行 */
.ckp-type-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.ckp-type-row__label { font-size: var(--fs-xs); color: var(--text-secondary); font-weight: 500; white-space: nowrap; }
.ckp-type-items { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.ckp-type-pill {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 3px 6px 3px 10px; border-radius: 14px;
  border: 1px solid var(--border-light); background: var(--bg-card);
  font-size: var(--fs-xs); color: var(--text-secondary);
}
.ckp-type-pill :deep(.el-input-number) { width: 60px; }
.ckp-type-pill :deep(.el-input-number .el-input__wrapper) { background: var(--bg-card); }
.ckp-type-pill :deep(.el-input-number .el-input__inner) { font-size: var(--fs-xs); }

/* 结果区 */
.compose-results { margin-top: 8px; }
.compose-group-hdr {
  font-size: var(--fs-xs); font-weight: 600; color: var(--text-primary);
  margin: 8px 0 4px; padding-bottom: 3px;
  border-bottom: 1px solid var(--border-light);
  display: flex; align-items: center;
}

.ai-progress-bar { padding: 8px 0; }
.ai-progress-text { font-size: var(--fs-xs); color: var(--text-secondary); margin-top: 4px; }

.ai-card {
  border: 1px solid var(--border-light); border-radius: 6px;
  padding: 8px 10px; margin-bottom: 6px; background: var(--bg-card);
}
.ai-card-header { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
.ai-card-body { font-size: var(--fs-sm); line-height: 1.6; margin: 4px 0; }
.ai-card-options { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 4px; }
.ai-opt {
  font-size: var(--fs-xs); padding: 2px 8px; background: var(--bg-section);
  border-radius: 4px; color: var(--text-secondary);
}
.ai-opt-correct { background: var(--bg-success-light); color: var(--el-color-success); font-weight: 600; }
.ai-similar { border-left: 3px solid var(--el-color-warning); }

.ai-edit-input { margin-bottom: 4px; }
.ai-edit-opts { display: flex; flex-direction: column; gap: 4px; margin: 4px 0; }
.ai-edit-opt { display: flex; align-items: center; gap: 4px; }
.opt-letter { font-weight: 600; font-size: var(--fs-xs); width: 14px; color: var(--text-secondary); }
.ai-edit-footer { display: flex; gap: 4px; margin-top: 4px; justify-content: flex-end; }

.ckp-src { font-size: var(--fs-xs); color: var(--text-secondary); margin-left: auto; }

/* 空状态引导 */
.compose-guide {
  text-align: center; padding: 32px 20px;
  color: var(--text-secondary);
}
.compose-guide__icon { font-size: 32px; margin-bottom: 10px; opacity: 0.3; }
.compose-guide__title { font-size: var(--fs-md); font-weight: 600; color: var(--text-primary); margin-bottom: 6px; }
.compose-guide__text { font-size: var(--fs-xs); line-height: 1.6; }

@media (max-width: 768px) {
  :deep(.el-form-item .el-input),
  :deep(.el-form-item .el-select) {
    width: 100%;
  }
}
</style>

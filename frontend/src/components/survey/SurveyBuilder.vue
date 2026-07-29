<template>
  <div class="survey-builder">
    <!-- 顶部工具栏 -->
    <div class="sb-toolbar">
      <div class="sb-toolbar-left">
        <el-select
          v-model="selectedTemplate"
          placeholder="从模板载入…"
          size="small"
          clearable
          class="desktop-width"
          style="width:180px"
          @change="loadTemplate"
        >
          <el-option
            v-for="t in templates"
            :key="t.key"
            :value="t.key"
            :label="t.label"
          />
        </el-select>
        <el-button size="small" text @click="previewVisible = !previewVisible">
          {{ previewVisible ? '关闭预览' : '预览问卷' }}
        </el-button>
      </div>
      <div class="sb-toolbar-right">
        <span class="sb-count">{{ questions.length }} 题</span>
      </div>
    </div>

    <!-- 主体：左(题型面板) + 中(题目列表) -->
    <div class="sb-body">
      <!-- 左侧题型面板 -->
      <div class="sb-palette">
        <div class="sb-palette-title">题型</div>
        <div
          v-for="qt in questionTypes"
          :key="qt.type"
          class="sb-palette-item"
          draggable="true"
          @dragstart="onPaletteDragStart($event, qt.type)"
          @click="addQuestion(qt.type)"
        >
          <span class="sb-p-icon" v-html="qt.icon"></span>
          <span class="sb-p-label">{{ qt.label }}</span>
        </div>
      </div>

      <!-- 中间编辑区 -->
      <div class="sb-editor" @drop.prevent="onDropToEditor" @dragover.prevent>
        <div v-if="!questions.length" class="sb-empty">
          <el-empty description="从左侧选择题型添加题目，或从模板载入" :image-size="60" />
        </div>
        <div v-else ref="listRef" class="sb-question-list">
          <div
            v-for="(q, i) in questions"
            :key="q.id"
            class="sb-question-card"
            :class="{ active: selectedIndex === i }"
            @click="selectedIndex = i"
          >
            <!-- 拖拽手柄 -->
            <span class="sb-drag-handle" @mousedown.stop>
              <svg width="16" height="16" viewBox="0 0 16 16"><circle
                cx="5"
                cy="4"
                r="1.5"
                fill="#999"
              /><circle
                cx="11"
                cy="4"
                r="1.5"
                fill="#999"
              /><circle
                cx="5"
                cy="8"
                r="1.5"
                fill="#999"
              /><circle
                cx="11"
                cy="8"
                r="1.5"
                fill="#999"
              /><circle
                cx="5"
                cy="12"
                r="1.5"
                fill="#999"
              /><circle
                cx="11"
                cy="12"
                r="1.5"
                fill="#999"
              /></svg>
            </span>
            <!-- 题目编号+类型 -->
            <span class="sb-q-num">{{ i + 1 }}.</span>
            <el-tag size="small" class="sb-q-type-tag">{{ typeLabel(q.type) }}</el-tag>
            <!-- 标题编辑 -->
            <el-input
              v-if="selectedIndex === i"
              v-model="q.title"
              placeholder="输入题目"
              size="small"
              class="sb-q-title-input"
              @click.stop
            />
            <span v-else class="sb-q-title-text">{{ q.title || '（未填写的题目）' }}</span>
            <!-- 必填标记 -->
            <el-checkbox
              v-if="selectedIndex === i"
              v-model="q.required"
              size="small"
              @click.stop
            >
              必填
            </el-checkbox>
            <!-- 选项编辑 -->
            <div v-if="selectedIndex === i && hasOptions(q.type)" class="sb-options-editor">
              <div v-for="(opt, oi) in q.options" :key="oi" class="sb-opt-row">
                <span class="sb-opt-letter">{{ optionLetter(oi) }}.</span>
                <el-input
                  v-model="q.options[oi]"
                  size="small"
                  placeholder="选项"
                  @click.stop
                />
                <el-button
                  size="small"
                  text
                  type="danger"
                  @click.stop="removeOption(q, oi)"
                >
                  ✕
                </el-button>
              </div>
              <el-button size="small" text @click.stop="addOption(q)">+ 添加选项</el-button>
            </div>
            <!-- 非编辑时展示选项预览 -->
            <div v-if="selectedIndex !== i && q.options?.length" class="sb-opt-preview">
              <el-tag
                v-for="(opt, oi) in q.options.slice(0, 3)"
                :key="oi"
                size="small"
                class="sb-opt-preview-tag"
              >
                {{ opt }}
              </el-tag>
              <span v-if="q.options.length > 3" class="sb-opt-more">+{{ q.options.length - 3 }}</span>
            </div>
            <!-- 删除按钮（hover 显示） -->
            <span class="sb-q-del" @click.stop="removeQuestion(i)">✕</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 预览弹窗 -->
    <el-dialog
      v-model="previewVisible"
      title="问卷预览"
      width="640px"
      append-to-body
      destroy-on-close
    >
      <div class="survey-preview">
        <div v-for="(q, i) in questions" :key="q.id" class="sp-q">
          <div class="sp-q-label">{{ i + 1 }}. {{ q.title }}<span v-if="q.required" class="sp-required">*</span></div>
          <el-radio-group v-if="q.type === 'radio'" disabled>
            <el-radio v-for="opt in q.options" :key="opt" :value="opt">{{ opt }}</el-radio>
          </el-radio-group>
          <el-checkbox-group v-else-if="q.type === 'checkbox'" disabled>
            <el-checkbox v-for="opt in q.options" :key="opt" :value="opt">{{ opt }}</el-checkbox>
          </el-checkbox-group>
          <el-select
            v-else-if="q.type === 'dropdown'"
            disabled
            placeholder="请选择…"
            style="width:100%"
          />
          <el-rate v-else-if="q.type === 'rating'" disabled />
          <el-slider
            v-else-if="q.type === 'scale'"
            disabled
            :max="10"
            :value="5"
          />
          <el-date-picker
            v-else-if="q.type === 'date'"
            disabled
            type="date"
            placeholder="选择日期"
            style="width:100%"
          />
          <el-input
            v-else-if="q.type === 'textarea'"
            disabled
            type="textarea"
            :rows="2"
            placeholder="输入回答…"
          />
        </div>
        <el-empty v-if="!questions.length" description="暂无题目" :image-size="50" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { SURVEY_TEMPLATES } from '@/constants/taskType'

const emit = defineEmits(['update:schema'])
const props = defineProps({ schema: { type: String, default: '' } })

// ── 可用题型 ──
const questionTypes = [
  { type: 'radio',    label: '单选',   icon: '◉' },
  { type: 'checkbox', label: '多选',   icon: '☑' },
  { type: 'dropdown', label: '下拉',   icon: '▼' },
  { type: 'rating',   label: '评分星', icon: '★' },
  { type: 'scale',    label: '量表',   icon: '━' },
  { type: 'date',     label: '日期',   icon: '📅' },
  { type: 'textarea', label: '文本',   icon: 'Aa' },
]

let idCounter = 0
function uid() { return 'q_' + (++idCounter) }

const templates = SURVEY_TEMPLATES
const selectedTemplate = ref('')
const previewVisible = ref(false)
const questions = ref([])
const selectedIndex = ref(-1)

// 从模板载入
function loadTemplate(key) {
  const tpl = templates.find(t => t.key === key)
  if (!tpl) return
  questions.value = tpl.questions.map(q => ({
    id: uid(),
    type: q.type === 'text' ? 'textarea' : q.type,
    title: q.title,
    options: q.options || [],
    required: q.required !== undefined ? !!q.required : true
  }))
  selectedIndex.value = 0
  emitSchema()
  ElMessage.success(`已载入「${tpl.label}」`)
}

// 从已有 schema 初始化
watch(() => props.schema, (val) => {
  if (val && questions.value.length === 0) {
    try {
      const parsed = typeof val === 'string' ? JSON.parse(val) : val
      if (Array.isArray(parsed) && parsed.length) {
        questions.value = parsed.map(q => ({
          id: uid(),
          type: q.type || q.questionType,
          title: q.label || q.title || '',
          options: q.options || [],
          required: q.required !== undefined ? !!q.required : true
        }))
        selectedIndex.value = 0
      }
    } catch {}
  }
}, { immediate: true })

// 添加题目
function addQuestion(type) {
  const q = { id: uid(), type, title: '', options: [], required: true }
  if (['radio', 'checkbox', 'dropdown'].includes(type)) {
    q.options = ['选项A', '选项B', '选项C']
  }
  if (type === 'rating') q.options = []
  if (type === 'scale') q.options = []
  if (type === 'date') q.options = []
  questions.value.push(q)
  selectedIndex.value = questions.value.length - 1
  emitSchema()
}

function removeQuestion(idx) {
  questions.value.splice(idx, 1)
  if (selectedIndex.value >= questions.value.length) selectedIndex.value = questions.value.length - 1
  emitSchema()
}

function addOption(q) { q.options.push('选项' + optionLetter(q.options.length)) }
function removeOption(q, oi) { q.options.splice(oi, 1) }

function hasOptions(type) { return ['radio', 'checkbox', 'dropdown'].includes(type) }
function optionLetter(i) { return String.fromCharCode(65 + i) }
function typeLabel(t) { const m = questionTypes.find(qt => qt.type === t); return m ? m.label : t }

// 拖入
function onPaletteDragStart(e, type) {
  e.dataTransfer.setData('text/plain', type)
}
function onDropToEditor(e) {
  const type = e.dataTransfer.getData('text/plain')
  if (type) addQuestion(type)
}

function emitSchema() {
  const schema = questions.value.map(q => ({
    type: q.type,
    title: q.title,
    options: q.type === 'rating' || q.type === 'scale' || q.type === 'date' || q.type === 'textarea' ? undefined : q.options,
    required: q.required ? 1 : 0
  }))
  emit('update:schema', JSON.stringify(schema))
}

watch(questions, emitSchema, { deep: true })
</script>

<style scoped>
.survey-builder { border: 1px solid #e2e8f0; border-radius: var(--radius-lg); overflow: hidden; background: #fff; box-shadow: 0 1px 3px rgba(0,0,0,.04); }
.sb-toolbar { display: flex; justify-content: space-between; align-items: center; padding: 10px 16px; background: linear-gradient(135deg, #f0f9ff, #f5f3ff); border-bottom: 1px solid #dbeafe; }
.sb-toolbar-left, .sb-toolbar-right { display: flex; align-items: center; gap: 8px; }
.sb-count { font-size: var(--fs-sm); font-weight: 600; color: #6366f1; background: #eef2ff; padding: 2px 10px; border-radius: 12px; }
.sb-body { display: flex; min-height: 300px; background: #fafbff; }
.sb-palette { width: 96px; flex-shrink: 0; padding: 12px 8px; background: linear-gradient(180deg, #f8fafc, #f1f5f9); border-right: 1px solid #e2e8f0; }
.sb-palette-title { font-size: var(--fs-xs); font-weight: 600; color: #64748b; text-align: center; margin-bottom: 10px; text-transform: uppercase; letter-spacing: 1px; }
.sb-palette-item { display: flex; flex-direction: column; align-items: center; padding: 10px 6px; border-radius: var(--radius-md); cursor: grab; transition: all .2s; margin-bottom: 4px; border: 1px solid transparent; }
.sb-palette-item:hover { background: #fff; border-color: #c7d2fe; box-shadow: 0 1px 3px rgba(99,102,241,.1); transform: translateY(-1px); }
.sb-p-icon { font-size: 22px; line-height: 1; margin-bottom: 3px; }
.sb-p-label { font-size: var(--fs-xs); color: #475569; font-weight: 500; white-space: nowrap; }
.sb-editor { flex: 1; padding: 14px; overflow-y: auto; max-height: 460px; }
.sb-empty { padding: 60px 0; }
.sb-question-list { display: flex; flex-direction: column; gap: 8px; }
.sb-question-card { position: relative; display: flex; flex-wrap: wrap; align-items: center; gap: 8px; padding: 12px 14px; border: 1px solid #e2e8f0; border-left: 3px solid #e2e8f0; border-radius: var(--radius-md); background: #fff; cursor: pointer; transition: all .2s; }
.sb-question-card:hover { border-color: #c7d2fe; border-left-color: #818cf8; box-shadow: 0 2px 6px rgba(0,0,0,.04); }
.sb-question-card.active { border-color: #818cf8; border-left-color: #6366f1; background: #eef2ff; box-shadow: 0 0 0 3px rgba(99,102,241,.08); }
.sb-drag-handle { cursor: grab; display: flex; align-items: center; opacity: .3; margin-right: 2px; }
.sb-drag-handle:hover { opacity: .7; }
.sb-q-num { font-size: var(--fs-md); font-weight: 700; color: #6366f1; min-width: 22px; }
.sb-q-type-tag { flex-shrink: 0; }
.sb-q-title-input { flex: 1; min-width: 120px; }
.sb-q-title-text { flex: 1; font-size: var(--fs-md); color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; min-width: 80px; }
.sb-q-del { position: absolute; top: 6px; right: 10px; font-size: var(--fs-md); color: #94a3b8; cursor: pointer; opacity: 0; transition: all .15s; width: 22px; height: 22px; display: flex; align-items: center; justify-content: center; border-radius: 50%; }
.sb-question-card:hover .sb-q-del { opacity: 1; }
.sb-q-del:hover { color: #fff; background: var(--el-color-danger); opacity: 1 !important; }
.sb-options-editor { width: 100%; padding: 8px 0 0 30px; display: flex; flex-direction: column; gap: 5px; }
.sb-opt-row { display: flex; align-items: center; gap: 6px; }
.sb-opt-letter { font-size: var(--fs-sm); font-weight: 600; color: #6366f1; min-width: 18px; text-align: right; }
.sb-opt-preview { width: 100%; padding-left: 30px; display: flex; flex-wrap: wrap; gap: 4px; }
.sb-opt-preview-tag { font-size: var(--fs-xs); background: #f1f5f9; border-color: #e2e8f0; }
.sb-opt-more { font-size: var(--fs-xs); color: #94a3b8; line-height: 22px; }

/* 预览 */
.survey-preview { display: flex; flex-direction: column; gap: 18px; padding: 8px 0; }
.sp-q { padding: 16px; background: #f8fafc; border-radius: var(--radius-md); border: 1px solid #e2e8f0; }
.sp-q-label { font-weight: 600; margin-bottom: 12px; font-size: var(--fs-md); }
.sp-required { color: var(--el-color-danger); margin-left: 4px; font-size: var(--fs-lg); }

@media (max-width: 768px) {
  :deep(.el-form-item .el-input),
  :deep(.el-form-item .el-select) {
    width: 100%;
  }
}
</style>

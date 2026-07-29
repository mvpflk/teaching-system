<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑题目' : '添加题目'"
    width="680px"
    destroy-on-close
    append-to-body
  >
    <div class="form-body">
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-position="top"
      >
        <el-form-item label="分类">
          <el-space wrap>
            <el-select
              v-model="formSubjectId"
              placeholder="选学科"
              style="width:160px"
              clearable
              @change="onSubjectChange"
            >
              <el-option
                v-for="s in categoryTree"
                :key="s.id"
                :value="s.id"
                :label="s.name"
              />
            </el-select>
            <el-select
              v-model="formChapterId"
              placeholder="选章节"
              style="width:180px"
              :disabled="!formSubjectId"
              clearable
              @change="onChapterChange"
            >
              <el-option
                v-for="c in chapters"
                :key="c.id"
                :value="c.id"
                :label="c.name"
              />
            </el-select>
            <el-select
              v-model="formTaskId"
              placeholder="选任务"
              style="width:180px"
              :disabled="!formChapterId"
              clearable
              @change="onTaskChange"
            >
              <el-option
                v-for="t in tasks"
                :key="t.id"
                :value="t.id"
                :label="t.name"
              />
            </el-select>
            <el-select
              v-model="formKpId"
              placeholder="知识点"
              style="width:180px"
              :disabled="!formTaskId || kps.length === 0"
              clearable
              @change="onKpChange"
            >
              <el-option
                v-for="k in kps"
                :key="k.id"
                :value="k.id"
                :label="k.name"
              />
            </el-select>
          </el-space>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="10">
            <el-form-item label="题型">
              <el-select
                v-model="form.questionType"
                filterable
                style="width:100%"
                @change="onTypeChange"
              >
                <el-option
                  v-for="(label, key) in QUESTION_TYPE_LABEL"
                  :key="key"
                  :value="key"
                  :label="label"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="分值">
              <template v-if="FIXED_ONE_POINT_TYPES.includes(form.questionType)">
                <span class="score-fixed">1 分</span>
              </template>
              <el-select v-else-if="form.questionType === 'SINGLE_CHOICE'" v-model="form.score" style="width:100%">
                <el-option :value="1" label="1 分" /><el-option :value="2" label="2 分" />
              </el-select>
              <el-select v-else-if="form.questionType === 'MULTI_CHOICE'" v-model="form.score" style="width:100%">
                <el-option :value="2" label="2 分" /><el-option :value="3" label="3 分" />
              </el-select>
              <el-input-number
                v-else
                v-model="form.score"
                :min="1"
                :max="30"
                style="width:100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="学科">
              <div class="subject-readonly">{{ form.subject || '选择分类后自动填充' }}</div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="题目内容" prop="questionText">
          <el-input
            v-model="form.questionText"
            type="textarea"
            :rows="3"
            placeholder="输入题目内容..."
          />
        </el-form-item>

        <!-- 选择题选项 -->
        <template v-if="OPTION_TYPES.includes(form.questionType)">
          <div v-for="(opt, oi) in optionList" :key="oi" class="option-row">
            <span class="option-label">{{ optionLetters[oi] }}.</span>
            <span class="opt-sort-btns">
              <el-button
                size="small"
                text
                :disabled="oi===0"
                class="arr-btn"
                @click="swapOpts(oi, oi-1)"
              >▲</el-button>
              <el-button
                size="small"
                text
                :disabled="oi===optionList.length-1"
                class="arr-btn"
                @click="swapOpts(oi, oi+1)"
              >▼</el-button>
            </span>
            <el-input
              v-model="optionList[oi]"
              placeholder="选项内容"
              size="small"
              class="opt-input"
            />
            <span class="opt-actions">
              <el-button
                v-show="oi === optionList.length - 1 && oi < 7"
                text
                size="small"
                @click="optionList.push('')"
              >＋</el-button>
              <el-button
                v-show="optionList.length > 2"
                text
                size="small"
                type="danger"
                @click="optionList.splice(oi, 1)"
              >✕</el-button>
            </span>
          </div>
        </template>

        <!-- 拖拽排序：正确顺序的项目列表 -->
        <template v-if="form.questionType === 'DRAG_SORT'">
          <el-form-item label="排序项目（按正确顺序输入，非最后一行回车添加）">
            <div v-for="(item, i) in sortItems" :key="i" class="sort-row">
              <div class="sort-arrows">
                <el-button
                  size="small"
                  text
                  :disabled="i===0"
                  class="arr-btn"
                  @click="swapSortItems(i,i-1)"
                >
                  ▲
                </el-button>
                <span class="sort-idx">{{ i + 1 }}</span>
                <el-button
                  size="small"
                  text
                  :disabled="i===sortItems.length-1"
                  class="arr-btn"
                  @click="swapSortItems(i,i+1)"
                >
                  ▼
                </el-button>
              </div>
              <el-input
                v-model="sortItems[i]"
                placeholder="项目内容"
                size="small"
                class="sort-input"
                @keyup.enter="i===sortItems.length-1 && sortItems.length<10 && sortItems.push('')"
              />
              <div class="sort-actions">
                <el-button
                  v-if="i===sortItems.length-1 && sortItems.length<10"
                  size="small"
                  text
                  type="primary"
                  @click="sortItems.push('')"
                >
                  ＋
                </el-button>
                <el-button
                  v-if="sortItems.length>2"
                  size="small"
                  text
                  type="danger"
                  @click="sortItems.splice(i,1)"
                >
                  ✕
                </el-button>
              </div>
            </div>
          </el-form-item>
        </template>

        <!-- 连线匹配：左右配对 -->
        <template v-if="form.questionType === 'MATCHING'">
          <el-form-item label="匹配对（左列→右列）">
            <div v-for="(pair, i) in matchPairs" :key="i" class="option-row">
              <el-input
                v-model="pair.left"
                placeholder="左项"
                size="small"
                style="width:130px"
              />
              <span style="color:var(--text-secondary)">→</span>
              <el-input
                v-model="pair.right"
                placeholder="右项"
                size="small"
                style="width:130px"
              />
              <el-button v-if="i === matchPairs.length - 1 && i < 7" text @click="matchPairs.push({left:'',right:''})">+</el-button>
              <el-button
                v-if="i > 1"
                text
                type="danger"
                @click="matchPairs.splice(i, 1)"
              >
                ✕
              </el-button>
            </div>
          </el-form-item>
        </template>

        <!-- 完形填空：题干中用 ___ 标记，下方填答案 -->
        <template v-if="form.questionType === 'CLOZE'">
          <el-form-item label="完形填空（用 ___ 标记空格）">
            <el-input
              v-model="form.questionText"
              type="textarea"
              :rows="4"
              placeholder="例如：The capital of France is ___, and its largest city is ___."
            />
          </el-form-item>
          <el-form-item label="空格答案（按顺序，逗号分隔）">
            <el-input v-model="clozeAnswers" placeholder="Paris,Paris" />
            <div class="form-hint">空格数 = 答案数，如题干中2个 ___ 则此处填2个答案用逗号隔开</div>
          </el-form-item>
        </template>

        <!-- 编程题：语言选择 + 参考代码 -->
        <template v-if="form.questionType === 'PROGRAMMING'">
          <el-form-item label="编程语言">
            <el-select v-model="codeLang" style="width:160px">
              <el-option
                v-for="l in ['java','python','cpp','c','javascript','typescript','go','rust']"
                :key="l"
                :value="l"
                :label="l"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="参考解答（选填，仅教师可见）">
            <el-input
              v-model="form.correctAnswer"
              type="textarea"
              :rows="5"
              class="code-input"
              placeholder="输入参考代码或评分要点…"
            />
          </el-form-item>
        </template>

        <el-form-item v-if="form.questionType !== 'CLOZE' && form.questionType !== 'DRAG_SORT' && form.questionType !== 'MATCHING'" label="正确答案/要点">
          <template v-if="form.questionType === 'SINGLE_CHOICE'">
            <el-select v-model="form.correctAnswer" style="width:200px">
              <el-option
                v-for="(_, oi) in optionList.filter(o => o)"
                :key="oi"
                :value="optionLetters[oi]"
                :label="optionLetters[oi] + '. ' + optionList[oi]"
              />
            </el-select>
          </template>
          <template v-else-if="form.questionType === 'MULTI_CHOICE'">
            <el-checkbox-group v-model="multiAnswer">
              <el-checkbox
                v-for="(_, oi) in optionList.filter(o => o)"
                :key="oi"
                :value="optionLetters[oi]"
                :label="optionLetters[oi] + '. ' + optionList[oi]"
              />
            </el-checkbox-group>
          </template>
          <template v-else-if="form.questionType === 'TRUE_FALSE'">
            <el-radio-group v-model="form.correctAnswer">
              <el-radio value="T">正确</el-radio>
              <el-radio value="F">错误</el-radio>
            </el-radio-group>
          </template>
          <template v-else>
            <el-input
              v-model="form.correctAnswer"
              type="textarea"
              :rows="3"
              placeholder="输入参考答案或评分要点（教师评阅时参考）"
              style="width:100%"
            />
          </template>
        </el-form-item>

        <el-form-item label="解析">
          <el-input
            v-model="form.explanation"
            type="textarea"
            :rows="2"
            placeholder="选填，答题后显示"
          />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="难度">
              <el-rate
                v-model="form.difficultyLevel"
                :max="5"
                :low-threshold="2"
                :high-threshold="4"
                show-text
                :texts="['很简单','简单','中等','较难','困难']"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="知识点">
              <el-input v-model="form.knowledgePoints" placeholder="硬件、反比例函数、语法……" maxlength="200" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="题目配图（可选，仅图片）">
          <div v-if="form.attachmentUrl" class="attach-preview">
            <img :src="form.attachmentUrl" style="max-width:300px;max-height:150px;border-radius:var(--radius-sm)" />
            <el-button
              size="small"
              type="danger"
              link
              @click="form.attachmentUrl = ''"
            >
              移除
            </el-button>
          </div>
          <el-upload
            v-else
            :action="UPLOAD_ACTION"
            :headers="uploadHeaders"
            :before-upload="beforeUpload"
            :on-success="onUploadSuccess"
            :on-error="onUploadError"
            :show-file-list="false"
            accept=".jpg,.jpeg,.png,.gif,.bmp,.webp"
          >
            <el-button size="small" plain><el-icon><Plus /></el-icon> 上传图片</el-button>
            <template #tip><div class="upload-tip">支持 jpg/png/gif，≤2MB</div></template>
          </el-upload>
        </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">{{ isEdit ? '保存' : '添加' }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useFormRules } from '@/composables/useFormRules'
import { useCategoryCascade } from '@/composables/useCategoryCascade'
import { createQuestion, updateQuestion } from '@/api/questionBank'
import { QUESTION_TYPE_LABEL, OPTION_TYPES, FIXED_ONE_POINT_TYPES } from '@/constants/questionTypes'
import { UPLOAD_ACTION, getUploadHeaders } from '@/api/task'
import { parseOptions } from '@/utils/question'

const { required: req } = useFormRules()
const formRules = { questionText: [req('题目内容')] }

const props = defineProps({
  modelValue: Boolean,
  categoryTree: { type: Array, default: () => [] },
  editData: { type: Object, default: null },
})
const emit = defineEmits(['update:modelValue', 'saved'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const formRef = ref(null)
const isEdit = computed(() => !!props.editData?.id)
const editId = computed(() => props.editData?.id || null)
const saving = ref(false)
const optionLetters = ['A','B','C','D','E','F','G','H']
const uploadHeaders = getUploadHeaders()

const form = reactive({
  subject: '', questionType: 'SINGLE_CHOICE',
  questionText: '', correctAnswer: '', explanation: '', categoryId: null,
  score: 2, difficultyLevel: 3, knowledgePoints: '', attachmentUrl: '',
})

const cascade = useCategoryCascade(computed(() => props.categoryTree))
const { selectedSubjectId: formSubjectId, selectedChapterId: formChapterId, selectedTaskId: formTaskId, selectedKpId: formKpId, chapters, tasks, kps } = cascade

const optionList = ref(['', '', '', ''])
const multiAnswer = ref([])
const sortItems = ref(['', '', '', ''])
const matchPairs = ref([{left:'',right:''},{left:'',right:''},{left:'',right:''}])
const clozeAnswers = ref('')
const codeLang = ref('java')

const IMAGE_EXTS = '.jpg,.jpeg,.png,.gif,.bmp,.webp'
const beforeUpload = (file) => {
  const ext = '.' + file.name.split('.').pop()?.toLowerCase()
  if (!IMAGE_EXTS.includes(ext)) { ElMessage.warning('仅支持图片格式：' + IMAGE_EXTS); return false }
  if (file.size > 2 * 1024 * 1024) { ElMessage.warning('图片不能超过2MB'); return false }
  return true
}
const onUploadSuccess = (res) => {
  const url = res.data?.url || res.data
  if (url) { form.attachmentUrl = url; ElMessage.success('上传成功') }
  else ElMessage.error('上传失败')
}
const onUploadError = () => { ElMessage.error('上传失败') }

const swapSortItems = (i, j) => {
  const arr = sortItems.value; const t = arr[i]; arr[i] = arr[j]; arr[j] = t
  sortItems.value = [...arr]
}
const swapOpts = (i, j) => {
  const arr = optionList.value; const t = arr[i]; arr[i] = arr[j]; arr[j] = t
  optionList.value = [...arr]
}

const buildSubjectPath = (val) => {
  if (!val || !props.categoryTree) return ''
  for (const s of props.categoryTree) {
    if (s.id === val) return s.name
    for (const c of (s.children||[])) {
      if (c.id === val) return s.name + ' > ' + c.name
      for (const t of (c.children||[])) {
        if (t.id === val) return s.name + ' > ' + c.name + ' > ' + t.name
        for (const k of (t.children||[])) {
          if (k.id === val) return s.name + ' > ' + c.name + ' > ' + t.name + ' > ' + k.name
        }
      }
    }
  }
  return ''
}

const onSubjectChange = (val) => {
  cascade.onSubjectChange(val)
  form.categoryId = val; form.subject = val ? buildSubjectPath(val) : ''
}
const onChapterChange = (val) => {
  cascade.onChapterChange(val)
  form.categoryId = val; form.subject = val ? buildSubjectPath(val) : ''
}
const onTaskChange = (val) => {
  cascade.onTaskChange(val)
  form.categoryId = val; form.subject = val ? buildSubjectPath(val) : ''
}
const onKpChange = (val) => {
  form.categoryId = val; form.subject = val ? buildSubjectPath(val) : ''
}

const onTypeChange = (type) => {
  form.correctAnswer = ''; optionList.value = []; sortItems.value = ['', '', '', '']; matchPairs.value = [{left:'',right:''},{left:'',right:''},{left:'',right:''}]; clozeAnswers.value = ''
  if (type === 'TRUE_FALSE') { form.score = 1 }
  else if (type === 'FILL_IN') { form.score = 1 }
  else if (type === 'SINGLE_CHOICE') { form.score = 2; optionList.value = ['', '', '', ''] }
  else if (type === 'MULTI_CHOICE') { form.score = 3; optionList.value = ['', '', '', ''] }
  else if (type === 'DRAG_SORT' || type === 'MATCHING') { form.score = 5 }
  else if (type === 'CLOZE') { form.score = 10 }
  else { form.score = 10 }
}

const parseOpts = (opts) => parseOptions(opts)

// Watch editData to populate form
watch(() => props.editData, (row) => {
  if (!row) {
    form.subject = ''; form.questionType = 'SINGLE_CHOICE'
    form.questionText = ''; form.correctAnswer = ''; form.explanation = ''
    form.categoryId = null; form.score = 2
    form.difficultyLevel = 3; form.knowledgePoints = ''; form.attachmentUrl = ''
    cascade.reset()
    optionList.value = ['', '', '', '']; multiAnswer.value = []
    sortItems.value = ['', '', '', '']; matchPairs.value = [{left:'',right:''},{left:'',right:''},{left:'',right:''}]; clozeAnswers.value = ''; codeLang.value = 'java'
    return
  }
  form.subject = row.subject; form.questionType = row.questionType
  form.questionText = row.questionText; form.score = row.score || 2
  form.correctAnswer = row.correctAnswer; form.explanation = row.explanation || ''
  form.categoryId = row.categoryId || null
  form.difficultyLevel = row.difficultyLevel || 3
  form.knowledgePoints = row.knowledgePoints || ''
  form.attachmentUrl = row.attachmentUrl || ''
  cascade.reset()
  if (OPTION_TYPES.includes(row.questionType)) {
    optionList.value = parseOpts(row.options).map(o => String(o).replace(/^([A-Z]|[0-9]+|[①②③④⑤⑥⑦⑧]|[一二三四五六七八九十]+)[.、．)]\s*/, ''))
    while (optionList.value.length < 4) optionList.value.push('')
  } else if (row.questionType === 'DRAG_SORT') {
    sortItems.value = parseOpts(row.options)
    if (!sortItems.value.length) sortItems.value = row.correctAnswer ? row.correctAnswer.split(',') : ['']
    while (sortItems.value.length < 2) sortItems.value.push('')
  } else if (row.questionType === 'MATCHING') {
    const pairs = parseOpts(row.options)
    matchPairs.value = pairs.length ? pairs : (row.correctAnswer || '').split(',').filter(Boolean).map(s => { const [l,r] = s.split('-'); return {left:l||'',right:r||''} })
    while (matchPairs.value.length < 2) matchPairs.value.push({left:'',right:''})
  } else if (row.questionType === 'CLOZE') {
    clozeAnswers.value = row.correctAnswer || ''
  } else if (row.questionType === 'PROGRAMMING') {
    try { const s = JSON.parse(row.answerSchema || '{}'); codeLang.value = s.language || 'java' } catch { codeLang.value = 'java' }
  }
  if (row.questionType === 'MULTI_CHOICE') {
    multiAnswer.value = (row.correctAnswer || '').split(',').filter(Boolean)
  } else { multiAnswer.value = [] }
}, { immediate: true })

const handleSave = async () => {
  if (saving.value) return
  if (!formRef.value) return
  try { await formRef.value.validate() } catch { return }
  saving.value = true
  try {
    const data = {
      subject: form.subject || '通用',
      categoryId: form.categoryId,
      questionType: form.questionType,
      questionText: form.questionText,
      correctAnswer: form.questionType === 'MULTI_CHOICE' ? multiAnswer.value.sort().join(',') : form.correctAnswer,
      explanation: form.explanation,
      score: form.score,
      difficultyLevel: form.difficultyLevel,
      knowledgePoints: form.knowledgePoints,
      attachmentUrl: form.attachmentUrl,
    }
    if (OPTION_TYPES.includes(form.questionType)) {
      const opts = optionList.value.filter(o => o.trim())
      if (opts.length === 0) { ElMessage.warning('请填写选项'); return }
      data.options = JSON.stringify(opts.map((t, i) => optionLetters[i] + '. ' + t.trim()))
    } else if (form.questionType === 'DRAG_SORT') {
      const items = sortItems.value.filter(s => s.trim())
      if (items.length < 2) { ElMessage.warning('请至少填写2个排序项目'); return }
      data.options = JSON.stringify(items)
      data.correctAnswer = items.join(',')
    } else if (form.questionType === 'MATCHING') {
      const pairs = matchPairs.value.filter(p => p.left.trim() && p.right.trim())
      if (pairs.length < 2) { ElMessage.warning('请至少填写2个匹配对'); return }
      data.options = JSON.stringify(pairs)
      data.correctAnswer = pairs.map(p => p.left.trim() + '-' + p.right.trim()).join(',')
    } else if (form.questionType === 'CLOZE') {
      const blanks = (form.questionText.match(/_{3,}|【.+?】/g) || []).length
      const answers = clozeAnswers.value.split(',').filter(a => a.trim())
      if (blanks === 0) { ElMessage.warning('请用 ___ 在题干中标记空格'); return }
      if (answers.length !== blanks) { ElMessage.warning(`题干有 ${blanks} 个空格，但填了 ${answers.length} 个答案，请保持一致`); return }
      data.correctAnswer = answers.map(a => a.trim()).join(',')
      data.options = '[]'
    } else if (form.questionType === 'PROGRAMMING') {
      data.options = '[]'
      data.answerSchema = JSON.stringify({ language: codeLang.value })
    } else {
      data.options = '[]'
    }
    if (isEdit.value) {
      await updateQuestion(editId.value, data)
      ElMessage.success('已更新')
    } else {
      await createQuestion(data)
      ElMessage.success('添加成功')
    }
    visible.value = false
    emit('saved')
  } finally { saving.value = false }
}

// Reset function for parent to call
const reset = () => {
  form.subject = ''; form.questionType = 'SINGLE_CHOICE'
  form.questionText = ''; form.correctAnswer = ''; form.explanation = ''
  form.categoryId = null; form.score = 2
  form.difficultyLevel = 3; form.knowledgePoints = ''; form.attachmentUrl = ''
  cascade.reset()
  optionList.value = ['', '', '', '']; multiAnswer.value = []
  sortItems.value = ['', '', '', '']; matchPairs.value = [{left:'',right:''},{left:'',right:''},{left:'',right:''}]; clozeAnswers.value = ''; codeLang.value = 'java'
}

defineExpose({ reset })
</script>

<style scoped>
.form-body { max-height: 68vh; overflow-y: auto; padding-right: 4px; }
.option-row { display: flex; align-items: center; gap: 6px; margin-bottom: 8px; }
.option-label { font-weight: 600; width: 22px; flex-shrink: 0; }
.opt-input { flex: 1; }
.opt-sort-btns { display: flex; flex-direction: column; align-items: center; gap: 0; width: 20px; flex-shrink: 0; }
.opt-actions { width: 40px; flex-shrink: 0; display: flex; align-items: center; justify-content: flex-end; }
.sort-row { display: flex; align-items: center; gap: 6px; margin-bottom: 6px; padding: 4px 8px; background: var(--bg-section); border-radius: var(--radius-sm); }
.sort-arrows { display: flex; flex-direction: column; align-items: center; gap: 0; width: 28px; }
.arr-btn { padding: 0 !important; height: 18px; font-size: 10px; color: var(--text-secondary); }
.sort-idx { font-size: var(--fs-xs); font-weight: 700; color: var(--primary-color); }
.sort-input { flex: 1; }
.sort-actions { display: flex; align-items: center; gap: 0; width: 36px; }
.score-fixed { font-size: var(--fs-md); color: var(--text-secondary); line-height: 32px; }
.subject-readonly { font-size: var(--fs-sm); color: var(--text-secondary); line-height: 32px; }
.code-input :deep(textarea) { font-family: 'Consolas','Monaco','Courier New',monospace; font-size: var(--fs-sm); }
.form-hint { font-size: var(--fs-xs); color: var(--text-secondary); margin-top: 4px; }
.attach-preview { display: flex; align-items: flex-start; gap: 8px; }
.upload-tip { font-size: var(--fs-xs); color: var(--text-secondary); margin-top: 4px; }
@media (max-width: 768px) { .form-body { max-height: calc(100dvh - 120px); } }
</style>

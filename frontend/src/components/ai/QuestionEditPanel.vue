<template>
  <div class="qep-editor">
    <h4 class="qep-editor-title">编辑第 {{ questionIndex + 1 }} 题</h4>
    <el-form label-width="72px" size="default">
      <el-form-item label="题型">
        <el-select v-model="localQuestion.questionType" style="width:160px" @change="onTypeChange">
          <el-option
            v-for="t in typeOptions"
            :key="t.value"
            :label="t.label"
            :value="t.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="题干">
        <el-input v-model="localQuestion.questionText" type="textarea" :rows="3" />
      </el-form-item>

      <el-form-item v-if="hasOptions" label="选项">
        <div class="qep-options">
          <div v-for="(opt, oi) in localQuestion.options" :key="oi" class="qep-option-row">
            <span class="qep-opt-label">{{ ['A','B','C','D','E','F'][oi] }}.</span>
            <el-input v-model="localQuestion.options[oi]" size="small" style="flex:1" />
            <el-button
              v-if="localQuestion.options.length > 2"
              size="small"
              text
              type="danger"
              @click="removeOption(oi)"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
          <el-button
            v-if="localQuestion.options.length < 6"
            size="small"
            text
            type="primary"
            @click="addOption"
          >
            + 添加选项
          </el-button>
        </div>
      </el-form-item>

      <el-form-item label="正确答案">
        <el-select v-if="localQuestion.questionType === 'SINGLE_CHOICE'" v-model="localQuestion.correctAnswer" style="width:120px">
          <el-option
            v-for="(opt, oi) in (localQuestion.options || [])"
            :key="oi"
            :label="['A','B','C','D','E','F'][oi]"
            :value="['A','B','C','D','E','F'][oi]"
          />
        </el-select>
        <el-checkbox-group v-else-if="localQuestion.questionType === 'MULTI_CHOICE'" v-model="multiAnswer">
          <el-checkbox
            v-for="(opt, oi) in (localQuestion.options || [])"
            :key="oi"
            :label="['A','B','C','D','E','F'][oi]"
            :value="['A','B','C','D','E','F'][oi]"
          />
        </el-checkbox-group>
        <el-select v-else-if="localQuestion.questionType === 'TRUE_FALSE'" v-model="localQuestion.correctAnswer" style="width:120px">
          <el-option label="A (正确)" value="A" />
          <el-option label="B (错误)" value="B" />
        </el-select>
        <el-input v-else v-model="localQuestion.correctAnswer" />
      </el-form-item>

      <el-form-item label="解析">
        <el-input
          v-model="localQuestion.explanation"
          type="textarea"
          :rows="2"
          placeholder="题目解析（可选）"
        />
      </el-form-item>

      <el-form-item label="难度">
        <el-rate
          v-model="localQuestion.difficultyLevel"
          :max="5"
          show-text
          :texts="['极易','容易','中等','较难','困难']"
        />
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { reactive, computed, watch } from 'vue'
import { Delete } from '@element-plus/icons-vue'

const props = defineProps({
  question: { type: Object, default: null },
  questionIndex: { type: Number, default: 0 }
})

const emit = defineEmits(['update:question'])

const typeOptions = [
  { value: 'SINGLE_CHOICE', label: '单选' },
  { value: 'MULTI_CHOICE', label: '多选' },
  { value: 'TRUE_FALSE', label: '判断' },
  { value: 'FILL_IN', label: '填空' },
  { value: 'SHORT_ANSWER', label: '简答' }
]

const localQuestion = reactive({ questionType: 'SINGLE_CHOICE', options: [], correctAnswer: '', explanation: '', difficultyLevel: 2 })

watch(() => props.question, (q) => {
  if (q) Object.assign(localQuestion, JSON.parse(JSON.stringify(q)))
}, { immediate: true })

watch(localQuestion, () => {
  emit('update:question', JSON.parse(JSON.stringify(localQuestion)))
}, { deep: true })

const hasOptions = computed(() => {
  const qt = localQuestion.questionType
  return qt === 'SINGLE_CHOICE' || qt === 'MULTI_CHOICE' || qt === 'TRUE_FALSE'
})

const multiAnswer = computed({
  get: () => {
    const ans = localQuestion.correctAnswer || ''
    return ans.split('').filter(c => c >= 'A' && c <= 'D')
  },
  set: (arr) => {
    localQuestion.correctAnswer = arr.sort().join('')
  }
})

const onTypeChange = () => {
  const qt = localQuestion.questionType
  if (qt === 'SINGLE_CHOICE' || qt === 'MULTI_CHOICE') {
    if (!localQuestion.options?.length) {
      localQuestion.options = ['', '', '', '']
    }
  } else if (qt === 'TRUE_FALSE') {
    localQuestion.options = ['正确', '错误']
  } else {
    localQuestion.options = []
  }
}

const addOption = () => {
  if (localQuestion.options.length < 6) {
    localQuestion.options.push('')
  }
}

const removeOption = (oi) => {
  localQuestion.options.splice(oi, 1)
  if (localQuestion.questionType === 'SINGLE_CHOICE') {
    const maxIdx = localQuestion.options.length - 1
    const maxChar = String.fromCharCode(65 + maxIdx)
    if (localQuestion.correctAnswer > maxChar) {
      localQuestion.correctAnswer = maxChar
    }
  }
}
</script>

<style scoped>
.qep-editor {
  flex: 1;
  overflow-y: auto;
  padding-right: 4px;
}
.qep-editor-title { margin: 0 0 16px; font-size: var(--fs-md); color: var(--text-primary); }
.qep-options { display: flex; flex-direction: column; gap: 6px; }
.qep-option-row { display: flex; align-items: center; gap: 6px; }
.qep-opt-label { font-weight: 600; min-width: 20px; color: var(--primary-color); }
</style>

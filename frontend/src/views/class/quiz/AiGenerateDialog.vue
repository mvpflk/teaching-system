<template>
  <el-dialog
    v-model="visible"
    title="AI即时出题"
    width="480px"
    destroy-on-close
  >
    <el-form label-position="top">
      <el-form-item label="学科">
        <el-select
          v-model="form.subject"
          style="width:100%"
          placeholder="选择学科"
          filterable
        >
          <el-option
            v-for="s in subjects"
            :key="s"
            :label="s"
            :value="s"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="知识点">
        <el-input v-model="form.knowledgePoint" placeholder="输入知识点名称" @keyup.enter="handleGenerate" />
      </el-form-item>
      <el-form-item label="教学目标">
        <el-select
          v-model="form.teachingGoal"
          style="width:100%"
          placeholder="选择教学目标"
        >
          <el-option label="复习旧知" value="REVIEW" />
          <el-option label="预习新知" value="PREVIEW" />
          <el-option label="课堂巩固" value="CONSOLIDATE" />
          <el-option label="拓展延伸" value="EXTEND" />
          <el-option label="诊断学情" value="DIAGNOSE" />
        </el-select>
      </el-form-item>
      <el-form-item label="难度级别">
        <el-radio-group v-model="form.difficulty">
          <el-radio :value="1">基础（记忆）</el-radio>
          <el-radio :value="2">进阶（理解）</el-radio>
          <el-radio :value="3">挑战（应用）</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="题目类型">
        <el-radio-group v-model="form.questionType">
          <el-radio value="SHORT_ANSWER">简答题</el-radio>
          <el-radio value="MULTIPLE_CHOICE">单选题</el-radio>
          <el-radio value="TRUE_FALSE">判断题</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="题目数量">
        <el-slider
          v-model="form.count"
          :min="3"
          :max="8"
          show-stops
          :step="1"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleGenerate">生成</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { aiGenerateQuiz } from '@/api/classroom'

const props = defineProps({
  modelValue: Boolean,
  subjects: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:modelValue', 'generated'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const form = reactive({ subject: '', knowledgePoint: '', count: 5, teachingGoal: '', difficulty: 2, questionType: 'SHORT_ANSWER' })
const loading = ref(false)

const handleGenerate = async () => {
  if (!form.knowledgePoint.trim()) { ElMessage.warning('请输入知识点'); return }
  loading.value = true
  try {
    const res = await aiGenerateQuiz({ ...form })
    if (res.code === 200) {
      const qs = res.data || []
      emit('generated', qs)
      visible.value = false
      ElMessage.success(`AI生成了${qs.length}道题目`)
    } else { ElMessage.error(res.message || '生成失败') }
  } catch { ElMessage.error('AI出题失败') } finally { loading.value = false }
}
</script>

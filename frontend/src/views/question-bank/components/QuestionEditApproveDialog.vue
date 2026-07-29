<template>
  <el-dialog :model-value="visible" title="修正AI题目" width="640px" :close-on-click-modal="false" @update:model-value="$emit('update:visible', $event)">
    <template v-if="question">
      <el-form label-width="80px" size="small">
        <el-form-item label="题型">
          <el-select v-model="question.questionType">
            <el-option v-for="(label, key) in typeLabels" :key="key" :value="key" :label="label" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-rate v-model="question.difficultyLevel" :max="5" show-text :texts="['很简单','简单','中等','较难','困难']" />
        </el-form-item>
        <el-form-item label="题目内容">
          <el-input v-model="question.questionText" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="选项">
          <el-input v-model="question.options" type="textarea" :rows="3" placeholder="JSON格式选项" />
        </el-form-item>
        <el-form-item label="正确答案">
          <el-input v-model="question.correctAnswer" />
        </el-form-item>
        <el-form-item label="解析">
          <el-input v-model="question.explanation" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
    </template>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" @click="$emit('confirm')">保存并通过</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
defineProps({ visible: Boolean, question: Object, typeLabels: Object })
defineEmits(['update:visible', 'confirm'])
</script>

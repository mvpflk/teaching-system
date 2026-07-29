<template>
  <el-dialog
    v-model="visible"
    title="导入课堂抽问题目"
    width="420px"
    destroy-on-close
  >
    <el-form label-position="top">
      <el-form-item label="所属任务（关联知识节点树）">
        <div style="display:flex;gap:8px;flex-wrap:wrap">
          <el-select v-model="form.subjectId" placeholder="学科" style="width:130px" @change="$emit('subjectChange', $event)">
            <el-option v-for="s in subjects" :key="s.id" :label="s.subjectName" :value="s.id" />
          </el-select>
          <el-select v-model="form.chapterId" placeholder="章节" style="width:130px" :disabled="!form.subjectId" @change="$emit('chapterChange', $event)">
            <el-option v-for="ch in chapters" :key="ch.id" :label="ch.name" :value="ch.id" />
          </el-select>
          <el-select v-model="form.taskId" placeholder="任务" style="width:130px" :disabled="!form.chapterId">
            <el-option v-for="t in tasks" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </div>
      </el-form-item>
      <el-form-item label="题目文件 (.txt)">
        <el-upload :show-file-list="false" :http-request="handleUpload" accept=".txt" style="display:inline-block">
          <el-button type="primary" :loading="uploading" :disabled="!form.taskId">
            <el-icon><Upload /></el-icon> 选择 .txt 文件
          </el-button>
        </el-upload>
        <span style="color:var(--text-secondary);font-size:var(--fs-sm);margin-left:10px">每行一道题目</span>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'
import { Upload } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: Boolean,
  subjects: { type: Array, default: () => [] },
  chapters: { type: Array, default: () => [] },
  tasks: { type: Array, default: () => [] },
  form: Object,
  uploading: Boolean
})
const emit = defineEmits(['update:modelValue', 'subjectChange', 'chapterChange', 'upload'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const handleUpload = (options) => {
  emit('upload', options)
}
</script>

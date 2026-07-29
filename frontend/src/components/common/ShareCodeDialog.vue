<template>
  <el-dialog
    v-model="visible"
    title="分享试卷"
    width="500px"
    :close-on-click-modal="false"
    append-to-body
  >
    <el-form :model="form" label-position="top">
      <el-form-item label="选择试卷">
        <el-select
          v-model="form.taskId"
          placeholder="请选择要分享的试卷"
          filterable
          style="width:100%"
        >
          <el-option
            v-for="t in taskList"
            :key="t.id"
            :value="t.id"
            :label="t.title"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <div v-if="result" class="share-result">
      <p>分享成功！分享码：</p>
      <div class="share-code-box">
        <span class="share-code">{{ result.shareCode }}</span>
        <el-button size="small" type="primary" @click="copyCode">复制</el-button>
      </div>
      <p class="share-expires">有效期至：{{ result.expiresAt }}</p>
    </div>
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button
        v-if="!result"
        type="primary"
        :disabled="!form.taskId"
        :loading="sharing"
        @click="handleShare"
      >
        生成分享码
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createShare } from '@/api/examShare'
import { listTasks } from '@/api/task'

const props = defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue', 'shared'])

const visible = ref(false)
watch(() => props.modelValue, v => { visible.value = v; if (v) init() })
watch(visible, v => emit('update:modelValue', v))

const form = ref({ taskId: null })
const taskList = ref([])
const sharing = ref(false)
const result = ref(null)

const init = async () => {
  form.value = { taskId: null }
  result.value = null
  try {
    const res = await listTasks({ page: 1, size: 200 })
    if (res.code === 200) taskList.value = res.data.records || []
  } catch { taskList.value = [] }
}

const handleShare = async () => {
  sharing.value = true
  try {
    const res = await createShare(form.value.taskId)
    if (res.code === 200) {
      result.value = res.data
      ElMessage.success('分享成功')
      emit('shared')
    } else {
      ElMessage.error(res.message || '分享失败')
    }
  } catch { ElMessage.error('分享失败') }
  finally { sharing.value = false }
}

const copyCode = () => {
  navigator.clipboard.writeText(result.value.shareCode).then(() => ElMessage.success('已复制'))
}
</script>

<style scoped>
.share-result { text-align: center; padding: 16px 0; }
.share-result p { margin: 8px 0; color: var(--text-secondary); font-size: var(--fs-md); }
.share-code-box { display: flex; align-items: center; justify-content: center; gap: 12px; margin: 12px 0; }
.share-code { font-size: 32px; font-weight: 700; letter-spacing: 4px; color: var(--primary-color); font-family: monospace; }
.share-expires { font-size: var(--fs-xs); color: var(--text-secondary); }

@media (max-width: 768px) {
  :deep(.el-form-item .el-input),
  :deep(.el-form-item .el-select) {
    width: 100%;
  }
}
</style>

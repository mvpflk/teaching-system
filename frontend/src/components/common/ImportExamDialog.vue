<template>
  <el-dialog
    v-model="visible"
    title="导入试卷"
    width="420px"
    :close-on-click-modal="false"
    append-to-body
  >
    <div class="import-body">
      <p class="import-hint">输入分享码并选择目标班级</p>
      <el-input
        v-model="shareCode"
        placeholder="请输入6位分享码"
        maxlength="6"
        style="font-size:var(--fs-xl);text-align:center;letter-spacing:4px;font-family:monospace;margin-bottom:12px"
        @keyup.enter="handleImport"
      />
      <el-select
        v-model="targetClassId"
        placeholder="选择目标班级"
        filterable
        style="width:100%"
      >
        <el-option
          v-for="c in classOptions"
          :key="c.id"
          :value="c.id"
          :label="(c.grade||'')+c.className"
        />
      </el-select>
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button
        type="primary"
        :disabled="shareCode.length !== 6"
        :loading="importing"
        @click="handleImport"
      >
        导入
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { importShared } from '@/api/examShare'
import { getClassList } from '@/api/classes'

const props = defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue', 'imported'])

const visible = ref(false)
watch(() => props.modelValue, v => { visible.value = v; if (v) init() })
watch(visible, v => emit('update:modelValue', v))

const shareCode = ref('')
const targetClassId = ref(null)
const classOptions = ref([])
const importing = ref(false)

const init = () => { shareCode.value = ''; targetClassId.value = null }

onMounted(async () => {
  try {
    const r = await getClassList()
    if (r.code === 200) classOptions.value = (r.data?.records || r.data || []).map(c => ({ id: c.id, className: c.className, grade: c.grade }))
  } catch { /* */ }
})

const handleImport = async () => {
  if (shareCode.value.length !== 6) { ElMessage.warning('请输入6位分享码'); return }
  if (!targetClassId.value) { ElMessage.warning('请选择目标班级'); return }
  importing.value = true
  try {
    const res = await importShared(shareCode.value.toUpperCase(), targetClassId.value)
    if (res.code === 200) {
      ElMessage.success(`导入成功！「${res.data.title}」(共${res.data.questionCount}题)`)
      visible.value = false
      emit('imported')
    } else {
      ElMessage.error(res.message || '导入失败')
    }
  } catch { ElMessage.error('导入失败，请检查分享码') }
  finally { importing.value = false }
}
</script>

<style scoped>
.import-body { padding: 8px 0; }
.import-hint { color: var(--text-secondary); font-size: var(--fs-md); margin-bottom: 16px; text-align: center; }

@media (max-width: 768px) {
  :deep(.el-form-item .el-input),
  :deep(.el-form-item .el-select) {
    width: 100%;
  }
}
</style>

<template>
  <el-dialog
    v-model="visible"
    title="导入实训方案"
    width="450px"
    @update:model-value="v => $emit('update:modelValue', v)"
  >
    <el-radio-group v-model="importType" style="margin-bottom:16px">
      <el-radio value="zip">ZIP 导入</el-radio>
      <el-radio value="excel">Excel 导入</el-radio>
    </el-radio-group>

    <el-upload
      drag
      :auto-upload="false"
      :show-file-list="false"
      :on-change="handleFile"
      accept=".zip,.xlsx,.xls"
    >
      <el-icon :size="32"><UploadFilled /></el-icon>
      <div>拖拽文件到此处，或点击选择</div>
      <template #tip>
        <div class="el-upload__tip">
          ZIP: plan.json + rubric.json + steps/*.md<br>
          Excel: 标题/描述/步骤序号/步骤名称/步骤描述/评分维度/维度权重
        </div>
      </template>
    </el-upload>

    <div v-if="selectedFile" class="file-info">
      已选择: {{ selectedFile.name }}
      <el-button
        size="small"
        type="primary"
        :loading="importing"
        style="margin-left:12px"
        @click="doImport"
      >
        开始导入
      </el-button>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { importPlanZip, importPlanExcel } from '@/api/practice'

const props = defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue', 'done'])

const visible = ref(false)
const importType = ref('zip')
const selectedFile = ref(null)
const importing = ref(false)

watch(() => props.modelValue, v => { visible.value = v; if (!v) { selectedFile.value = null; importType.value = 'zip' } })
watch(() => visible.value, v => emit('update:modelValue', v))

function handleFile(file) { selectedFile.value = file.raw || file }

async function doImport() {
  if (!selectedFile.value) return
  importing.value = true
  try {
    let res
    if (importType.value === 'zip') {
      res = await importPlanZip(selectedFile.value)
    } else {
      res = await importPlanExcel(selectedFile.value)
    }
    if (res.code === 200) {
      ElMessage.success('导入成功')
      visible.value = false
      emit('done')
    } else ElMessage.error(res.message || '导入失败')
  } catch { ElMessage.error('导入失败') }
  finally { importing.value = false; selectedFile.value = null }
}
</script>

<style scoped>
.file-info { margin-top: 12px; display: flex; align-items: center; }
</style>

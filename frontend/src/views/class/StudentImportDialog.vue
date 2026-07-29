<template>
  <el-dialog
    v-model="visible"
    title="批量导入学生"
    width="550px"
    append-to-body
  >
    <div style="margin-bottom:12px">
      <el-button text type="primary" @click="downloadTemplate">
        <el-icon><Download /></el-icon>下载Excel模板
      </el-button>
    </div>
    <el-upload
      drag
      :auto-upload="false"
      :on-change="onFileChange"
      accept=".xlsx,.xls"
      :limit="1"
    >
      <el-icon class="el-icon--upload" size="40"><UploadFilled /></el-icon>
      <div class="el-upload__text">拖拽 Excel 文件到此处，或<em>点击选择</em></div>
      <template #tip>
        <div class="el-upload__tip">
          仅支持 .xlsx / .xls 格式<br>
          模板列：<br>
          <b>学号</b>(必填) · 姓名 · 用户名 · 性别 · 出生日期 · 年级 · <b>班级</b> · 手机号 · 邮箱<br>
          「年级+班级」联合匹配可区分同名班级<br>
          默认密码为随机8位字符，首次登录需修改
        </div>
      </template>
    </el-upload>
    <div v-if="importResult" class="import-result">
      <el-divider />
      <el-alert :title="importResult.success + ' 人导入成功'" type="success" :closable="false" />
      <div v-if="importResult.classMatched !== undefined" style="margin-top:8px;font-size:var(--fs-sm)">
        <span style="color:var(--success-color)">✅ {{ importResult.classMatched }} 人已分配班级</span>
        <span v-if="importResult.success - importResult.classMatched > 0" style="color:var(--warning-color);margin-left:12px">
          ⚠️ {{ importResult.success - importResult.classMatched }} 人未分配班级
        </span>
      </div>
      <div v-if="importResult.debug && importResult.debug.length > 0" style="margin-top:8px">
        <el-tag size="small" type="info">{{ importResult.debug[0] }}</el-tag>
      </div>
      <div v-if="importResult.errors && importResult.errors.length > 0" style="margin-top:8px">
        <el-alert
          :title="importResult.errors.length + ' 条警告'"
          type="warning"
          :closable="false"
          :description="importResult.errors.join('\n')"
          show-icon
        />
      </div>
    </div>
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button v-if="importResult" type="primary" @click="done">完成</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { batchImportStudents } from '@/api/student'

const props = defineProps({
  modelValue: Boolean,
})

const emit = defineEmits(['update:modelValue', 'imported'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const importResult = ref(null)

const downloadTemplate = () => {
  const token = localStorage.getItem('token')
  const xhr = new XMLHttpRequest()
  xhr.open('GET', '/api/student/actions/template/download', true)
  xhr.setRequestHeader('Authorization', `Bearer ${token}`)
  xhr.responseType = 'blob'
  xhr.onload = function() {
    if (xhr.status === 200) {
      const url = window.URL.createObjectURL(xhr.response)
      const a = document.createElement('a')
      a.href = url; a.download = '学生导入模板.xlsx'; a.click()
      window.URL.revokeObjectURL(url)
      ElMessage.success('模板已下载')
    } else { ElMessage.error('下载失败') }
  }
  xhr.onerror = () => ElMessage.error('下载失败')
  xhr.send()
}

const onFileChange = async (uploadFile) => {
  const formData = new FormData()
  formData.append('file', uploadFile.raw)
  try {
    const res = await batchImportStudents(formData)
    if (res.code === 200) {
      importResult.value = res.data
      ElMessage.success(res.message || '导入成功')
    } else {
      ElMessage.error(res.message || '导入失败')
    }
  } catch (e) {
    ElMessage.error('导入失败: ' + (e.message || '未知错误'))
  }
}

const done = () => {
  visible.value = false
  emit('imported')
}

const reset = () => {
  importResult.value = null
}

defineExpose({ reset })
</script>

<style scoped>

@media (max-width: 768px) {
  :deep(.el-form-item .el-input),
  :deep(.el-form-item .el-select) {
    width: 100%;
  }
}
</style>

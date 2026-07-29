<template>
  <div class="inspector-issue-create">
    <div class="page-header"><h3 class="page-title">新建问题</h3></div>
    <el-form
      ref="formRef"
      v-loading="loading"
      :model="form"
      :rules="rules"
      label-width="110px"
    >
      <el-form-item label="问题标题" prop="title">
        <el-input v-model="form.title" placeholder="请输入问题标题" maxlength="200" />
      </el-form-item>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="问题分类" prop="category">
            <el-select v-model="form.category" placeholder="选择分类" style="width:100%">
              <el-option
                v-for="c in categoryOptions"
                :key="c.value"
                :value="c.value"
                :label="c.label"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="严重程度" prop="severity">
            <el-select v-model="form.severity" placeholder="选择程度" style="width:100%">
              <el-option label="低" value="LOW" /><el-option label="中" value="MEDIUM" /><el-option label="高" value="HIGH" /><el-option label="严重" value="CRITICAL" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="关联班级">
            <el-select
              v-model="form.assignedClassId"
              placeholder="选择班级"
              clearable
              filterable
              style="width:100%"
            >
              <el-option
                v-for="c in classOptions"
                :key="c.id"
                :value="c.id"
                :label="(c.grade||'') + ' ' + c.className"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="关联巡视记录">
            <el-select
              v-model="form.recordId"
              placeholder="选择记录"
              clearable
              filterable
              style="width:100%"
            >
              <el-option
                v-for="r in recordOptions"
                :key="r.id"
                :value="r.id"
                :label="r.title"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="问题描述">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="5"
          placeholder="描述问题的具体情况"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">创建</el-button>
        <el-button @click="$router.back()">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createIssue, getRecords } from '@/api/inspectorManage'
import { getClassList } from '@/api/classes'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const submitting = ref(false)
const classOptions = ref([])
const recordOptions = ref([])

const form = reactive({ title: '', category: '', severity: 'MEDIUM', assignedClassId: null, recordId: null, description: '' })

const categoryOptions = [
  { value: 'TEACHING_QUALITY', label: '教学质量' }, { value: 'CLASSROOM_DISCIPLINE', label: '课堂纪律' },
  { value: 'HOMEWORK_PROCRASTINATION', label: '作业拖拉' }, { value: 'ATTENDANCE', label: '出勤' },
  { value: 'MORAL_EDUCATION', label: '德育' }, { value: 'EXAM_IRREGULARITY', label: '考试违纪' }, { value: 'OTHER', label: '其他' }
]

const rules = { title: [{ required: true, message: '请输入标题', trigger: 'blur' }], category: [{ required: true, message: '请选择分类', trigger: 'change' }] }

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => {})
  if (!valid) return
  submitting.value = true
  try {
    const res = await createIssue(form)
    if (res.code === 200) { ElMessage.success('创建成功'); router.push(`/inspector/issues/${res.data.id}`) }
    else ElMessage.error(res.message)
  } catch { ElMessage.error('创建失败') }
  finally { submitting.value = false }
}

onMounted(async () => {
  loading.value = true
  try {
    const [clsRes, recRes] = await Promise.all([getClassList(), getRecords({ size: 999 })])
    if (clsRes.code === 200) classOptions.value = (clsRes.data.records || clsRes.data || []).map(c => ({ id: c.id, className: c.className, grade: c.grade }))
    if (recRes.code === 200) recordOptions.value = (recRes.data.records || []).map(r => ({ id: r.id, title: r.title }))
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
})
</script>

<style scoped lang="scss">
.inspector-issue-create { max-width: 800px; margin: 0 auto; padding: var(--spacing-lg, 24px); }
.page-header { margin-bottom: 20px; }
.page-title { font-size: var(--fs-2xl, 22px); margin: 0; }
@media (max-width: 768px) { .inspector-issue-create { padding: var(--spacing-md, 16px); } }
</style>

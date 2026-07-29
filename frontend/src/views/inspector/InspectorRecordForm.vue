<template>
  <div class="inspector-record-form">
    <div class="page-header">
      <h3 class="page-title">{{ isEdit ? '编辑巡视记录' : '新建巡视记录' }}</h3>
    </div>

    <el-form
      ref="formRef"
      v-loading="loading"
      :model="form"
      :rules="rules"
      label-width="100px"
    >
      <el-form-item label="标题" prop="title">
        <el-input v-model="form.title" placeholder="请输入巡视标题" maxlength="200" />
      </el-form-item>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="巡视类型" prop="recordType">
            <el-select v-model="form.recordType" placeholder="选择类型" style="width:100%">
              <el-option label="日常巡视" value="CASUAL" />
              <el-option label="课堂巡视" value="CLASSROOM" />
              <el-option label="德育巡视" value="MORAL" />
              <el-option label="专项巡视" value="SPECIAL" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="巡视日期" prop="recordDate">
            <el-date-picker
              v-model="form.recordDate"
              type="date"
              placeholder="选择日期"
              value-format="YYYY-MM-DD"
              style="width:100%"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="严重程度" prop="severity">
            <el-select v-model="form.severity" placeholder="选择程度" style="width:100%">
              <el-option label="信息" value="INFO" />
              <el-option label="警告" value="WARNING" />
              <el-option label="严重" value="CRITICAL" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="地点">
            <el-input v-model="form.location" placeholder="巡视地点" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="关联班级">
            <el-select
              v-model="form.targetClassId"
              placeholder="选择班级"
              clearable
              filterable
              style="width:100%"
            >
              <el-option
                v-for="c in classOptions"
                :key="c.id"
                :value="c.id"
                :label="(c.grade || '') + ' ' + c.className"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="关联教师">
            <el-select
              v-model="form.targetTeacherId"
              placeholder="选择教师"
              clearable
              filterable
              style="width:100%"
            >
              <el-option
                v-for="t in teacherOptions"
                :key="t.id"
                :value="t.id"
                :label="t.realName || t.name"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="描述">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="4"
          placeholder="请输入巡视描述"
        />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ isEdit ? '保存' : '创建' }}</el-button>
        <el-button @click="$router.back()">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getRecord, createRecord, updateRecord } from '@/api/inspectorManage'
import { getClassList } from '@/api/classes'
import { getTeacherList } from '@/api/teacher'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.params.id)
const formRef = ref(null)
const loading = ref(false)
const submitting = ref(false)
const classOptions = ref([])
const teacherOptions = ref([])

const form = reactive({
  title: '', recordType: 'CASUAL', recordDate: '', severity: 'INFO',
  location: '', targetClassId: null, targetTeacherId: null, description: ''
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  recordType: [{ required: true, message: '请选择巡视类型', trigger: 'change' }],
  recordDate: [{ required: true, message: '请选择巡视日期', trigger: 'change' }],
  severity: [{ required: true, message: '请选择严重程度', trigger: 'change' }]
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => {})
  if (!valid) return
  submitting.value = true
  try {
    let res
    if (isEdit.value) res = await updateRecord(route.params.id, form)
    else res = await createRecord(form)
    if (res.code === 200) { ElMessage.success(isEdit.value ? '保存成功' : '创建成功'); router.push('/inspector/records') }
    else ElMessage.error(res.message)
  } catch { ElMessage.error('操作失败') }
  finally { submitting.value = false }
}

onMounted(async () => {
  loading.value = true
  try {
    const [clsRes, tchRes] = await Promise.all([getClassList(), getTeacherList({ size: 999 })])
    if (clsRes.code === 200) classOptions.value = (clsRes.data.records || clsRes.data || []).map(c => ({ id: c.id, className: c.className, grade: c.grade }))
    if (tchRes.code === 200) teacherOptions.value = ((tchRes.data && (tchRes.data.records || tchRes.data)) || []).map(t => ({ id: t.id, realName: t.realName || t.name }))
    if (isEdit.value) {
      const res = await getRecord(route.params.id)
      if (res.code === 200) Object.assign(form, res.data)
    }
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
})
</script>

<style scoped lang="scss">
.inspector-record-form { max-width: 800px; margin: 0 auto; padding: var(--spacing-lg, 24px); }
.page-header { margin-bottom: 20px; }
.page-title { font-size: var(--fs-2xl, 22px); margin: 0; }
@media (max-width: 768px) {
  .inspector-record-form { padding: var(--spacing-md, 16px); }
}
</style>

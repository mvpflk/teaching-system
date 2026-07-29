<template>
  <div class="template-manager">
    <div class="page-header">
      <h3><el-icon><Tickets /></el-icon> 任务模板</h3>
      <span class="header-sub">一键复用已有任务配置</span>
      <el-button type="primary" @click="openCreate">新建模板</el-button>
    </div>

    <div class="filter-bar">
      <el-radio-group v-model="filter.scope" size="small" @change="loadList">
        <el-radio-button value="ALL">全部</el-radio-button>
        <el-radio-button value="PRIVATE">我的模板</el-radio-button>
        <el-radio-button value="LESSON_PREP">备课组</el-radio-button>
        <el-radio-button value="TEACHING_GROUP">教研组</el-radio-button>
        <el-radio-button value="SCHOOL_WIDE">全校</el-radio-button>
        <el-radio-button value="HEAD_TEACHER_GROUP">班主任群</el-radio-button>
      </el-radio-group>
      <el-select
        v-model="filter.category"
        placeholder="用途分类"
        clearable
        size="small"
        style="width:130px"
        @change="loadList"
      >
        <el-option value="ALL" label="全部分类" />
        <el-option value="TEACHING" label="教学类" />
        <el-option value="CLASS_MGMT" label="班级管理类" />
        <el-option value="SCHOOL_NOTICE" label="全校通知类" />
      </el-select>
      <el-select
        v-model="filter.subject"
        placeholder="学科"
        clearable
        size="small"
        style="width:120px"
        @change="loadList"
      >
        <el-option
          v-for="s in subjects"
          :key="s"
          :value="s"
          :label="s"
        />
      </el-select>
      <el-select
        v-model="filter.taskType"
        placeholder="任务类型"
        clearable
        size="small"
        style="width:110px"
        @change="loadList"
      >
        <el-option
          v-for="(l,k) in TASK_TYPE_FILTER_LABEL"
          :key="k"
          :value="k"
          :label="l"
        />
      </el-select>
    </div>

    <el-empty v-if="!loading && list.length===0" description="暂无模板，去任务列表保存一个吧" />
    <div v-loading="loading" class="template-grid">
      <div v-for="t in list" :key="t.id" class="tpl-card">
        <div class="tpl-header">
          <span class="tpl-name">{{ t.name }}</span>
          <el-dropdown v-if="t.createdBy===userId" trigger="click" @command="(cmd)=>onCommand(cmd,t)">
            <el-button text size="small"><el-icon><MoreFilled /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="scope">修改共享范围</el-dropdown-item>
                <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <div class="tpl-tags">
          <el-tag size="small" :type="categoryTag(t.category)">{{ categoryLabel(t.category) }}</el-tag>
          <el-tag size="small" type="primary">{{ t.subject || '通用' }}</el-tag>
          <el-tag size="small">{{ label(t.taskType) }}</el-tag>
          <el-tag size="small" type="info">{{ t.questionCount || 0 }}题</el-tag>
          <el-tag v-if="t.scope!=='PRIVATE'" size="small" type="success">{{ scopeLabel(t.scope) }}</el-tag>
        </div>
        <div class="tpl-meta">
          <span>{{ t.creatorName }}</span> <span>{{ fmt(t.updatedAt) }}</span>
          <span v-if="t.useCount">已用{{ t.useCount }}次</span>
        </div>
        <div class="tpl-actions">
          <el-button size="small" type="primary" @click="useTemplate(t)">使用</el-button>
          <el-button size="small" @click="previewTemplate(t)">预览</el-button>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="previewVisible"
      title="模板预览"
      width="640px"
      destroy-on-close
      append-to-body
    >
      <div v-if="preview" class="preview-body">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="学科">{{ preview.subject||'通用' }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ label(preview.taskType) }}</el-descriptions-item>
          <el-descriptions-item label="满分">{{ preview.totalScore||100 }}</el-descriptions-item>
          <el-descriptions-item label="题目数">{{ preview.questionCount }}题</el-descriptions-item>
        </el-descriptions>
        <div v-if="preview.description" class="preview-desc" v-html="sanitizeHtml(preview.description)"></div>
      </div>
      <template #footer>
        <el-button @click="previewVisible=false">关闭</el-button>
        <el-button type="primary" @click="previewVisible=false;useTemplate(preview)">使用此模板</el-button>
      </template>
    </el-dialog>

    <!-- 新建模板 -->
    <el-dialog
      v-model="createVisible"
      title="新建模板"
      width="680px"
      destroy-on-close
      append-to-body
    >
      <el-form :model="createForm" label-position="top">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12"><el-form-item label="模板名称" required><el-input v-model="createForm.name" maxlength="200" placeholder="输入模板名称" /></el-form-item></el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="任务类型">
              <el-select v-model="createForm.taskType" style="width:100%">
                <el-option
                  v-for="(l,k) in TASK_TYPE_FILTER_LABEL"
                  :key="k"
                  :value="k"
                  :label="l"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12"><el-form-item label="用途分类"><el-select v-model="createForm.category" style="width:100%"><el-option value="TEACHING" label="教学类" /><el-option value="CLASS_MGMT" label="班级管理类" /><el-option value="SCHOOL_NOTICE" label="全校通知类" /></el-select></el-form-item></el-col>
          <el-col :xs="24" :sm="12"><el-form-item label="共享范围"><el-select v-model="createForm.scope" style="width:100%"><el-option value="PRIVATE" label="仅自己可见" /><el-option value="LESSON_PREP" label="备课组" /><el-option value="TEACHING_GROUP" label="教研组" /><el-option v-if="isAdmin" value="SCHOOL_WIDE" label="全校教师" /><el-option value="HEAD_TEACHER_GROUP" label="班主任群组" /></el-select></el-form-item></el-col>
        </el-row>
        <el-form-item label="任务说明">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="3"
            placeholder="输入任务说明..."
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :xs="24" :sm="8">
            <el-form-item label="学科">
              <el-select
                v-model="createForm.subject"
                filterable
                clearable
                style="width:100%"
                placeholder="选择学科"
              >
                <el-option
                  v-for="s in subjects"
                  :key="s"
                  :value="s"
                  :label="s"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-form-item label="总分">
              <el-input-number
                v-model="createForm.totalScore"
                :min="1"
                :max="300"
                style="width:100%"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="评分方式"><el-select v-model="createForm.scoreType" style="width:100%"><el-option value="POINT_100" label="百分制" /><el-option value="GRADE_5" label="五级制" /><el-option value="PASS_FAIL" label="通过/不通过" /></el-select></el-form-item></el-col>
        </el-row>
        <el-form-item label="选题(可选)"><TaskQuestionPicker v-model="createForm.questionIds" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible=false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="doCreate">保存模板</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="scopeVisible"
      title="修改共享范围"
      width="400px"
      destroy-on-close
      append-to-body
    >
      <el-radio-group v-model="scopeTarget.scope" style="display:flex;flex-direction:column;gap:12px">
        <el-radio value="PRIVATE">仅自己可见</el-radio>
        <el-radio value="LESSON_PREP">共享给备课组</el-radio>
        <el-radio value="TEACHING_GROUP">共享给教研组</el-radio>
        <el-radio value="HEAD_TEACHER_GROUP">共享给班主任群组</el-radio>
        <el-radio v-if="isAdmin" value="SCHOOL_WIDE">共享给全校教师</el-radio>
      </el-radio-group>
      <template #footer>
        <el-button @click="scopeVisible=false">取消</el-button>
        <el-button type="primary" @click="doUpdateScope">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Tickets } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { TASK_TYPE_FILTER_LABEL } from '@/constants/taskType'
import { getTemplateList, getTemplate, createTaskFromTemplate, updateTemplateScope, deleteTemplate, createTemplate } from '@/api/taskTemplate'
import TaskQuestionPicker from '@/components/common/TaskQuestionPicker.vue'
import { getMySubjects } from '@/api/settings'
import { sanitizeHtml } from '@/utils/markdown'
import dayjs from 'dayjs'

const router = useRouter()
const userStore = useUserStore()
const userId = userStore.userInfo?.id
const isAdmin = userStore.isAdmin
const list = ref([]), loading = ref(false), subjects = ref([])
const filter = reactive({ scope: 'ALL', category: 'ALL', subject: '', taskType: '' })
const categoryLabel = (c) => ({ TEACHING: '教学', CLASS_MGMT: '班级管理', SCHOOL_NOTICE: '全校通知' }[c] || '教学')
const categoryTag = (c) => ({ TEACHING: '', CLASS_MGMT: 'warning', SCHOOL_NOTICE: 'danger' }[c] || '')
const scopeLabel = (s) => ({ LESSON_PREP: '备课组', TEACHING_GROUP: '教研组', SCHOOL_WIDE: '全校', HEAD_TEACHER_GROUP: '班主任群' }[s] || s)
const previewVisible = ref(false), preview = ref(null)
const createVisible = ref(false), creating = ref(false)
const createForm = reactive({ name: '', taskType: 'AFTER_CLASS', category: 'TEACHING', scope: 'PRIVATE', description: '', subject: '', totalScore: 100, scoreType: 'POINT_100', questionIds: [] })
const scopeVisible = ref(false), scopeTarget = ref({})
const label = (k) => TASK_TYPE_FILTER_LABEL[k] || k
const fmt = (d) => d ? dayjs(d).format('MM-DD HH:mm') : ''

const loadList = async () => {
  loading.value = true
  try {
    const params = { scope: filter.scope, category: filter.category }
    if (filter.subject) params.subject = filter.subject
    if (filter.taskType) params.taskType = filter.taskType
    const r = await getTemplateList(params); if (r.code===200) list.value = r.data||[]
  } catch { /* */ } finally { loading.value = false }
}

const useTemplate = async (t) => {
  try {
    const r = await createTaskFromTemplate(t.id)
    if (r.code === 200) { ElMessage.success('草稿已创建，请选择班级'); router.push(`/teacher/tasks/list`) }
  } catch { ElMessage.error('创建失败') }
}

const previewTemplate = async (t) => {
  try { const r = await getTemplate(t.id); if (r.code===200) { preview.value=r.data; previewVisible.value=true } }
  catch { ElMessage.error('加载失败') }
}

const openCreate = () => { Object.assign(createForm, { name: '', taskType: 'AFTER_CLASS', category: 'TEACHING', scope: 'PRIVATE', description: '', subject: '', totalScore: 100, scoreType: 'POINT_100', questionIds: [] }); createVisible.value = true }
const doCreate = async () => {
  if (!createForm.name) return ElMessage.warning('请输入模板名称')
  creating.value = true
  try {
    const r = await createTemplate({ ...createForm, questionIds: JSON.stringify(createForm.questionIds) })
    if (r.code === 200) { ElMessage.success('模板已创建'); createVisible.value = false; loadList() }
    else ElMessage.error(r.message || '创建失败')
  } catch { ElMessage.error('创建失败') } finally { creating.value = false }
}

const onCommand = (cmd, t) => {
  if (cmd==='scope') { scopeTarget.value = { ...t }; scopeVisible.value = true }
  else if (cmd==='delete') ElMessageBox.confirm('删除后不可恢复','确认删除',{type:'warning'}).then(async()=>{await deleteTemplate(t.id);ElMessage.success('已删除');loadList()}).catch(()=>{})
}

const doUpdateScope = async () => {
  try { await updateTemplateScope(scopeTarget.value.id, scopeTarget.value.scope); ElMessage.success('已更新'); scopeVisible.value=false; loadList() }
  catch { ElMessage.error('更新失败') }
}

const onResize = () => {}
onMounted(async () => { window.addEventListener('resize',onResize); loadList(); const r=await getMySubjects(); if(r.code===200) subjects.value=(r.data||[]).map(s=>s.subjectName) })
onUnmounted(() => window.removeEventListener('resize',onResize))
</script>

<style scoped>
.template-manager { max-width: 1100px; margin: 0 auto; padding: var(--spacing-lg); }
.page-header { margin-bottom: 10px; display: flex; align-items: baseline; gap: 10px; }
.page-header h3 { font-size: var(--fs-xl); margin: 0; }
.header-sub { font-size: var(--fs-sm); color: var(--text-secondary); }
.filter-bar { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; margin-bottom: 18px; }
.template-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 14px; }
.tpl-card { background: var(--bg-card); border-radius: var(--radius-md); padding: 16px; box-shadow: var(--shadow-sm); display: flex; flex-direction: column; gap: 10px; }
.tpl-card:hover { box-shadow: var(--shadow-base); }
.tpl-header { display: flex; justify-content: space-between; align-items: flex-start; }
.tpl-name { font-size: var(--fs-md); font-weight: 600; color: var(--text-primary); flex: 1; overflow: hidden; text-overflow: ellipsis; }
.tpl-tags { display: flex; flex-wrap: wrap; gap: 4px; }
.tpl-meta { display: flex; gap: 12px; font-size: var(--fs-xs); color: var(--text-secondary); }
.tpl-actions { display: flex; gap: 8px; }
.preview-body { max-height: 50vh; overflow-y: auto; }
.preview-desc { margin-top: 14px; padding: 12px; background: var(--bg-section); border-radius: var(--radius-sm); font-size: var(--fs-sm); line-height: 1.8; }
@media (max-width: 768px) {
  .template-manager { padding: var(--spacing-md); }
  .template-grid { grid-template-columns: 1fr; }
}
</style>

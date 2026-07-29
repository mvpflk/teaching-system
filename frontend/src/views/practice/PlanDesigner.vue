<template>
  <div class="plan-designer">
    <div class="pd-header">
      <h3>{{ editingId ? '编辑实训方案' : '实训方案设计' }}</h3>
      <div class="pd-actions">
        <el-button type="primary" @click="resetForm">＋ 新建方案</el-button>
        <el-button @click="showImport = true">📥 导入方案</el-button>
        <el-button @click="showBatchImport = true">📝 批量导入</el-button>
        <el-button :type="editingId ? 'warning' : 'primary'" :loading="saving" @click="doSave">
          {{ editingId ? '更新方案' : '保存方案' }}
        </el-button>
        <el-button
          type="success"
          :loading="publishing"
          :disabled="!editingId"
          @click="doPublish"
        >
          发布为任务
        </el-button>
      </div>
    </div>

    <el-form label-width="100px" class="pd-form">
      <el-form-item label="方案标题" required>
        <el-input v-model="plan.title" placeholder="输入实训方案标题" />
      </el-form-item>
      <el-form-item label="方案描述">
        <el-input
          v-model="plan.description"
          type="textarea"
          :rows="3"
          placeholder="方案整体描述"
        />
      </el-form-item>
      <el-form-item label="提交模式">
        <el-switch v-model="plan.simpleMode" active-text="简易" inactive-text="标准" />
        <span style="margin-left:8px;font-size:var(--fs-xs);color:var(--text-secondary)">
          简易模式：学生只需1步上传作品截图+描述即可提交；标准模式：学生按多步骤流程完成
        </span>
      </el-form-item>
    </el-form>

    <el-collapse v-model="activePanels" class="pd-sections">
      <el-collapse-item v-if="!plan.simpleMode" title="📚 前置知识" name="prereqs">
        <div class="tag-editor">
          <el-tag
            v-for="(p, i) in prereqs"
            :key="i"
            closable
            style="margin:2px"
            @close="prereqs.splice(i, 1)"
          >
            {{ p.name }}
          </el-tag>
          <el-input
            v-model="newPrereq"
            size="small"
            style="width:160px"
            placeholder="输入知识名称"
            @keydown.enter.prevent="addPrereq"
          />
          <el-button size="small" @click="addPrereq">添加</el-button>
        </div>
      </el-collapse-item>

      <el-collapse-item v-if="!plan.simpleMode" title="🛠️ 环境要求" name="env">
        <el-table :data="envItems" size="small" style="width:100%">
          <el-table-column label="工具" width="160">
            <template #default="{ row }"><el-input v-model="row.tool" size="small" /></template>
          </el-table-column>
          <el-table-column label="版本" width="100">
            <template #default="{ row }"><el-input v-model="row.version" size="small" /></template>
          </el-table-column>
          <el-table-column label="用途" min-width="200">
            <template #default="{ row }"><el-input v-model="row.purpose" size="small" /></template>
          </el-table-column>
          <el-table-column width="50">
            <template #default="{ $index }"><el-button text type="danger" @click="envItems.splice($index,1)">✕</el-button></template>
          </el-table-column>
        </el-table>
        <el-button size="small" style="margin-top:6px" @click="envItems.push({tool:'',version:'',purpose:''})">+ 添加环境</el-button>
      </el-collapse-item>

      <el-collapse-item v-if="!plan.simpleMode" title="⚠️ 安全规范" name="safety">
        <el-input
          v-model="plan.safetyNotes"
          type="textarea"
          :rows="4"
          placeholder="安全注意事项"
        />
      </el-collapse-item>

      <el-collapse-item v-if="!plan.simpleMode" title="🔧 排错指南" name="trouble">
        <el-input
          v-model="plan.troubleshooting"
          type="textarea"
          :rows="4"
          placeholder="常见问题与排故方法"
        />
      </el-collapse-item>

      <el-collapse-item title="📊 评分模型" name="scoring">
        <el-form-item label="评分模型">
          <el-select v-model="plan.scoringModel" style="width:200px">
            <el-option label="双维度评分 (过程+成果)" value="DUAL_DIMENSION" />
            <el-option label="竞赛制评分" value="COMPETITION" />
          </el-select>
        </el-form-item>
        <div class="rubric-header"><span>评分细则</span><el-button size="small" @click="addRubric">+ 添加维度</el-button></div>
        <el-table :data="rubrics" size="small" style="width:100%">
          <el-table-column label="维度代码" width="140">
            <template #default="{ row }"><el-input v-model="row.dimension" size="small" placeholder="如 process_quality" /></template>
          </el-table-column>
          <el-table-column label="维度名称" width="140">
            <template #default="{ row }"><el-input v-model="row.dimensionLabel" size="small" /></template>
          </el-table-column>
          <el-table-column label="权重" width="100">
            <template #default="{ row }">
              <el-input-number
                v-model="row.weight"
                :min="0"
                :max="1"
                :step="0.1"
                size="small"
                style="width:80px"
              />
            </template>
          </el-table-column>
          <el-table-column label="评分标准" min-width="200">
            <template #default="{ row }">
              <el-input
                v-model="row.criteria"
                type="textarea"
                :rows="2"
                placeholder="JSON格式: [{level:0-5,label,description}]"
              />
            </template>
          </el-table-column>
          <el-table-column width="50">
            <template #default="{ $index }"><el-button text type="danger" @click="rubrics.splice($index,1)">✕</el-button></template>
          </el-table-column>
        </el-table>
      </el-collapse-item>
    </el-collapse>

    <!-- 方案列表 -->
    <div class="pd-list">
      <el-tabs v-model="planTab">
        <el-tab-pane label="我的方案" name="mine">
          <el-table
            v-loading="listLoading"
            :data="planList"
            stripe
            @row-click="selectPlan"
          >
            <el-table-column prop="title" label="标题" min-width="160" />
            <el-table-column prop="subject" label="学科" width="120" />
            <el-table-column label="共享" width="80">
              <template #default="{ row }">
                <el-switch
                  v-model="row.shared"
                  size="small"
                  @change="(val) => toggleShare(row)"
                  @click.stop
                />
              </template>
            </el-table-column>
            <el-table-column prop="scoringModel" label="评分模型" width="120">
              <template #default="{ row }">{{ row.scoringModel === 'DUAL_DIMENSION' ? '双维度' : '竞赛制' }}</template>
            </el-table-column>
            <el-table-column label="创建时间" width="160">
              <template #default="{ row }">{{ row.createTime ? fmt(row.createTime) : '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-popconfirm title="确认删除?" @confirm.stop="doDelete(row.id)">
                  <template #reference><el-button size="small" type="danger" @click.stop>删除</el-button></template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="学科共享库" name="shared">
          <el-table
            v-loading="sharedLoading"
            :data="sharedPlans"
            stripe
            @row-click="selectSharedPlan"
          >
            <el-table-column prop="title" label="标题" min-width="200" />
            <el-table-column prop="subject" label="学科" width="120" />
            <el-table-column label="创建时间" width="160">
              <template #default="{ row }">{{ row.createTime ? fmt(row.createTime) : '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click.stop="selectSharedPlan(row)">编辑</el-button>
                <el-button size="small" type="success" @click.stop="publishSharedPlan(row)">发布</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!sharedLoading && sharedPlans.length === 0" description="学科共享库暂无方案" :image-size="60" />
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 发布对话框 -->
    <el-dialog v-model="showPublish" title="发布为任务" width="450px">
      <el-form label-width="80px">
        <el-form-item label="目标班级">
          <el-select
            v-model="publishClassIds"
            multiple
            placeholder="选择班级"
            style="width:100%"
          >
            <el-option
              v-for="c in classOptions"
              :key="c.id"
              :label="c.className"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPublish = false">取消</el-button>
        <el-button type="success" :loading="publishing" @click="confirmPublish">发布</el-button>
      </template>
    </el-dialog>

    <!-- 导入对话框 -->
    <PlanImportDialog v-model="showImport" @done="loadList" />
    <BatchImportDialog v-model="showBatchImport" @done="loadList" />
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listPlans, getRubrics, createPlan, updatePlan, deletePlan, publishPlan, listSharedPlans, updatePlanShareStatus } from '@/api/practice'
import { getClassList } from '@/api/classes'
import PlanImportDialog from '@/components/practice/PlanImportDialog.vue'
import BatchImportDialog from '@/components/practice/BatchImportDialog.vue'

const editingId = ref(null)
const saving = ref(false)
const publishing = ref(false)
const showPublish = ref(false)
const showImport = ref(false)
const publishClassIds = ref([])
const classOptions = ref([])
const listLoading = ref(false)
const planList = ref([])
const activePanels = ref([])
const planTab = ref('mine')
const showBatchImport = ref(false)
const sharedPlans = ref([])
const sharedLoading = ref(false)

const plan = ref({ title: '', description: '', safetyNotes: '', troubleshooting: '', scoringModel: 'DUAL_DIMENSION', simpleMode: true })
const prereqs = ref([])
const newPrereq = ref('')
const envItems = ref([])
const rubrics = ref([])

function addPrereq() {
  const v = newPrereq.value.trim()
  if (!v) return
  prereqs.value.push({ name: v })
  newPrereq.value = ''
}

function addRubric() {
  rubrics.value.push({ dimension: '', dimensionLabel: '', weight: 0, criteria: '', sortOrder: rubrics.value.length })
}

function resetForm() {
  editingId.value = null
  plan.value = { title: '', description: '', safetyNotes: '', troubleshooting: '', scoringModel: 'DUAL_DIMENSION', simpleMode: true }
  prereqs.value = []
  envItems.value = []
  rubrics.value = []
  activePanels.value = []
}

const fmt = (s) => {
  if (!s) return '-'
  const d = new Date(s)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function loadList() {
  listLoading.value = true
  try {
    const res = await listPlans()
    if (res.code === 200) planList.value = res.data || []
  } finally { listLoading.value = false }
}

async function selectPlan(row) {
  editingId.value = row.id
  plan.value = { title: row.title, description: row.description || '', safetyNotes: row.safetyNotes || '', troubleshooting: row.troubleshooting || '', scoringModel: row.scoringModel || 'DUAL_DIMENSION', simpleMode: row.simpleMode || false }
  try {
    if (row.prerequisites) { try { prereqs.value = JSON.parse(row.prerequisites) || [] } catch { prereqs.value = [] } } else prereqs.value = []
    if (row.environment) { try { envItems.value = JSON.parse(row.environment) || [] } catch { envItems.value = [] } } else envItems.value = []
    if (row.teamRoles) { /* skip */ }
    const rRes = await getRubrics(row.id)
    if (rRes.code === 200) rubrics.value = (rRes.data || []).map(r => ({ ...r, weight: r.weight || 0 }))
  } catch { /* */ }
}

async function doSave() {
  if (!plan.value.title) { ElMessage.warning('请输入方案标题'); return }
  saving.value = true
  try {
    const data = {
      ...plan.value,
      prerequisites: prereqs.value.length ? JSON.stringify(prereqs.value) : null,
      environment: envItems.value.length ? JSON.stringify(envItems.value) : null,
      rubrics: rubrics.value.map(r => ({ ...r, weight: r.weight || 0 }))
    }
    let res
    if (editingId.value) {
      res = await updatePlan(editingId.value, data)
    } else {
      res = await createPlan(data)
    }
    if (res.code === 200) {
      ElMessage.success('保存成功')
      editingId.value = res.data?.id || editingId.value
      loadList()
    } else ElMessage.error(res.message || '保存失败')
  } catch { ElMessage.error('保存失败') }
  finally { saving.value = false }
}

async function doPublish() {
  if (!editingId.value) { ElMessage.warning('请先保存方案'); return }
  try {
    const res = await getClassList()
    if (res.code === 200) classOptions.value = res.data || []
  } catch { /* */ }
  showPublish.value = true
}

async function confirmPublish() {
  if (!publishClassIds.value.length) { ElMessage.warning('请选择班级'); return }
  publishing.value = true
  try {
    const res = await publishPlan(editingId.value, { classIds: publishClassIds.value })
    if (res.code === 200) {
      ElMessage.success('已发布')
      showPublish.value = false
    } else ElMessage.error(res.message || '发布失败')
  } catch { ElMessage.error('发布失败') }
  finally { publishing.value = false }
}

async function doDelete(id) {
  try {
    await deletePlan(id)
    ElMessage.success('已删除')
    if (editingId.value === id) { editingId.value = null; plan.value = { title: '', description: '', safetyNotes: '', troubleshooting: '', scoringModel: 'DUAL_DIMENSION' }; prereqs.value = []; envItems.value = []; rubrics.value = [] }
    loadList()
  } catch { ElMessage.error('删除失败') }
}

async function toggleShare(row) {
  try {
    const res = await updatePlanShareStatus(row.id, { shared: row.shared })
    if (res.code !== 200) {
      row.shared = !row.shared
      ElMessage.error(res.message || '操作失败')
    } else {
      ElMessage.success(row.shared ? '已共享到学科库' : '已取消共享')
    }
  } catch {
    row.shared = !row.shared
    ElMessage.error('操作失败')
  }
}

async function loadShared() {
  sharedLoading.value = true
  try {
    const res = await listSharedPlans()
    if (res.code === 200) sharedPlans.value = res.data || []
  } finally { sharedLoading.value = false }
}

async function selectSharedPlan(row) {
  editingId.value = row.id
  plan.value = { title: row.title, description: row.description || '', safetyNotes: row.safetyNotes || '', troubleshooting: row.troubleshooting || '', scoringModel: row.scoringModel || 'DUAL_DIMENSION', simpleMode: row.simpleMode || false }
  try {
    if (row.prerequisites) { try { prereqs.value = JSON.parse(row.prerequisites) || [] } catch { prereqs.value = [] } } else prereqs.value = []
    if (row.environment) { try { envItems.value = JSON.parse(row.environment) || [] } catch { envItems.value = [] } } else envItems.value = []
    const rRes = await getRubrics(row.id)
    if (rRes.code === 200) rubrics.value = (rRes.data || []).map(r => ({ ...r, weight: r.weight || 0 }))
  } catch { /* */ }
  planTab.value = 'mine'
}

async function publishSharedPlan(row) {
  editingId.value = row.id
  await selectSharedPlan(row)
  doPublish()
}

watch(planTab, (val) => {
  if (val === 'shared') loadShared()
})

onMounted(() => loadList())
</script>

<style scoped lang="scss">
.plan-designer { max-width: 1100px; margin: 0 auto; padding: 16px; }
.pd-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; h3 { margin: 0; } }
.pd-actions { display: flex; gap: 8px; }
.pd-form { max-width: 700px; margin-bottom: 16px; }
.pd-sections { margin-bottom: 24px; }
.tag-editor { display: flex; flex-wrap: wrap; gap: 4px; align-items: center; }
.rubric-header { display: flex; justify-content: space-between; align-items: center; font-weight: 600; margin-bottom: 8px; }
.pd-list { margin-top: 24px; h4 { margin: 0 0 12px; } }
</style>

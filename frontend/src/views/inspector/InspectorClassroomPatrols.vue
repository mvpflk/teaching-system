<template>
  <div class="inspector-patrols">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>课堂巡课记录</span>
          <el-button type="primary" @click="showForm = true">新建巡课记录</el-button>
        </div>
      </template>

      <el-form :model="filters" inline class="filter-bar">
        <el-form-item label="班级">
          <el-select
            v-model="filters.classId"
            placeholder="全部班级"
            clearable
            filterable
            style="width:160px"
          >
            <el-option
              v-for="c in classList"
              :key="c.id"
              :label="c.className"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="教师">
          <el-select
            v-model="filters.teacherId"
            placeholder="全部教师"
            clearable
            filterable
            style="width:160px"
          >
            <el-option
              v-for="t in teacherList"
              :key="t.id"
              :label="t.realName || t.name"
              :value="t.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="科目">
          <el-input
            v-model="filters.subject"
            placeholder="科目"
            clearable
            style="width:140px"
          />
        </el-form-item>
        <el-form-item label="开始">
          <el-date-picker
            v-model="filters.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="开始日期"
            style="width:140px"
          />
        </el-form-item>
        <el-form-item label="结束">
          <el-date-picker
            v-model="filters.endDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="结束日期"
            style="width:140px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>

      <template v-if="!isMobile">
        <el-table v-loading="loading" :data="list" stripe>
          <el-table-column prop="patrolDate" label="巡课日期" width="120" />
          <el-table-column label="班级" width="120"><template #default="{ row }">{{ getClassName(row.classId) }}</template></el-table-column>
          <el-table-column label="教师" width="120"><template #default="{ row }">{{ getTeacherName(row.teacherId) }}</template></el-table-column>
          <el-table-column prop="subject" label="科目" width="100" />
          <el-table-column prop="period" label="节次" width="80" />
          <el-table-column label="纪律评分" width="100"><template #default="{ row }"><el-rate :model-value="row.disciplineScore" disabled show-score score-template="{value}" /></template></el-table-column>
          <el-table-column label="教学规范" width="100"><template #default="{ row }"><el-rate :model-value="row.teachingScore" disabled show-score score-template="{value}" /></template></el-table-column>
          <el-table-column label="师生互动" width="100"><template #default="{ row }"><el-rate :model-value="row.interactionScore" disabled show-score score-template="{value}" /></template></el-table-column>
          <el-table-column prop="note" label="备注" min-width="180" show-overflow-tooltip />
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="editRow(row)">编辑</el-button>
              <el-popconfirm title="确认删除?" @confirm="deleteRow(row.id)"><template #reference><el-button size="small" type="danger">删除</el-button></template></el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </template>
      <!-- 移动端卡片 -->
      <div v-else class="mobile-list">
        <div v-for="row in list" :key="row.id" class="mobile-card">
          <div class="mc-header"><span class="mc-title">{{ getClassName(row.classId) }} - {{ row.subject }}</span><span class="mc-period">{{ row.period }}</span></div>
          <div class="mc-body">
            <div class="mc-meta"><span class="mc-lbl">日期</span><span>{{ row.patrolDate }}</span><span class="mc-lbl" style="margin-left:12px">教师</span><span>{{ getTeacherName(row.teacherId) }}</span></div>
            <div class="mc-meta"><span class="mc-lbl">纪律</span><el-rate :model-value="row.disciplineScore" disabled show-score score-template="{value}" size="small" /></div>
            <div class="mc-meta"><span class="mc-lbl">教学</span><el-rate :model-value="row.teachingScore" disabled show-score score-template="{value}" size="small" /></div>
            <div class="mc-meta"><span class="mc-lbl">互动</span><el-rate :model-value="row.interactionScore" disabled show-score score-template="{value}" size="small" /></div>
            <div v-if="row.note" class="mc-desc">{{ row.note }}</div>
          </div>
          <div class="mc-actions">
            <el-button size="small" @click="editRow(row)">编辑</el-button>
            <el-popconfirm title="确认删除?" @confirm="deleteRow(row.id)"><template #reference><el-button size="small" type="danger">删除</el-button></template></el-popconfirm>
          </div>
        </div>
        <el-empty v-if="!loading && list.length === 0" description="暂无巡课记录" :image-size="60" />
      </div>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          :page-size="size"
          :total="total"
          layout="prev,pager,next,total"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <el-dialog v-model="showForm" :title="editingId ? '编辑巡课记录' : '新建巡课记录'" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="巡课日期" required>
          <el-date-picker
            v-model="form.patrolDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="班级" required>
          <el-select v-model="form.classId" filterable style="width:100%">
            <el-option
              v-for="c in classList"
              :key="c.id"
              :label="c.className"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="授课教师" required>
          <el-select v-model="form.teacherId" filterable style="width:100%">
            <el-option
              v-for="t in teacherList"
              :key="t.id"
              :label="t.realName || t.name"
              :value="t.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="科目">
          <el-input v-model="form.subject" style="width:100%" />
        </el-form-item>
        <el-form-item label="节次">
          <el-select v-model="form.period" placeholder="选择节次" style="width:100%">
            <el-option
              v-for="p in 8"
              :key="p"
              :label="'第' + p + '节'"
              :value="'第' + p + '节'"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="纪律评分">
          <el-rate
            v-model="form.disciplineScore"
            :max="5"
            show-score
            score-template="{value}"
          />
        </el-form-item>
        <el-form-item label="教学规范">
          <el-rate
            v-model="form.teachingScore"
            :max="5"
            show-score
            score-template="{value}"
          />
        </el-form-item>
        <el-form-item label="师生互动">
          <el-rate
            v-model="form.interactionScore"
            :max="5"
            show-score
            score-template="{value}"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="form.note"
            type="textarea"
            :rows="3"
            style="width:100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showForm = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getClassList } from '@/api/classes'
import { getTeacherList } from '@/api/teacher'
import { useIsMobile } from '@/composables/useIsMobile'
import {
  getClassroomPatrols, createClassroomPatrol, updateClassroomPatrol, deleteClassroomPatrol
} from '@/api/inspectorManage'

const list = ref([])
const { isMobile } = useIsMobile()
const loading = ref(false)
const saving = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const showForm = ref(false)
const editingId = ref(null)
const classList = ref([])
const teacherList = ref([])

const filters = ref({ classId: null, teacherId: null, subject: '', startDate: '', endDate: '' })
const form = ref({ patrolDate: '', classId: null, teacherId: null, subject: '', period: '', disciplineScore: null, teachingScore: null, interactionScore: null, note: '' })

function resetFilters() {
  filters.value = { classId: null, teacherId: null, subject: '', startDate: '', endDate: '' }
  page.value = 1
  loadData()
}

function getClassName(id) {
  const c = classList.value.find(x => x.id === id)
  return c ? c.className : '未知'
}

function getTeacherName(id) {
  const t = teacherList.value.find(x => x.id === id)
  return t ? (t.realName || t.name) : '未知'
}

async function loadData() {
  loading.value = true
  try {
    const res = await getClassroomPatrols({ ...filters.value, page: page.value, size: size.value })
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function editRow(row) {
  editingId.value = row.id
  form.value = { ...row }
  showForm.value = true
}

async function save() {
  saving.value = true
  try {
    if (editingId.value) {
      await updateClassroomPatrol(editingId.value, form.value)
    } else {
      await createClassroomPatrol(form.value)
    }
    showForm.value = false
    loadData()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    saving.value = false
  }
}

async function deleteRow(id) {
  try {
    await ElMessageBox.confirm('确定删除此巡课记录？', '提示', { type: 'warning' })
    await deleteClassroomPatrol(id)
    loadData()
  } catch {
    ElMessage.error('操作失败')
  }
}

onMounted(async () => {
  const [cls, tch] = await Promise.all([getClassList(), getTeacherList()])
  classList.value = cls.data || []
  teacherList.value = tch.data || []
  loadData()
})
</script>

<style scoped lang="scss">
.card-header { display: flex; justify-content: space-between; align-items: center; }
.filter-bar { margin-bottom: 16px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: center; }

@media (max-width: 768px) {
  .inspector-patrols { padding: var(--spacing-md, 16px); }
  .filter-bar { flex-direction: column; align-items: stretch; }
  :deep(.el-table) { font-size: var(--fs-xs); }
}

.mobile-list { display: flex; flex-direction: column; gap: 10px; }
.mobile-card { padding: 14px; background: var(--bg-card); border-radius: var(--radius-md); border: 1px solid var(--border-light); }
.mc-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 8px; margin-bottom: 8px; }
.mc-title { font-weight: 600; font-size: var(--fs-md); color: var(--text-primary); flex: 1; }
.mc-period { font-size: var(--fs-sm); color: var(--text-secondary); flex-shrink: 0; }
.mc-body { display: flex; flex-direction: column; gap: 4px; }
.mc-meta { display: flex; align-items: center; gap: 8px; font-size: var(--fs-sm); }
.mc-lbl { color: var(--text-secondary); min-width: 36px; }
.mc-desc { font-size: var(--fs-sm); color: var(--text-regular); margin: 4px 0; }
.mc-actions { display: flex; gap: 8px; }
</style>

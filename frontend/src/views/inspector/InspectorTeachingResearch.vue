<template>
  <div class="inspector-research">
    <el-row :gutter="16" class="mb-16">
      <el-col v-for="s in stats" :key="s.type" :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">{{ s.label }}</div>
            <div class="stat-value">{{ s.count }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>教研活动记录</span>
          <el-button type="primary" @click="showForm = true">新建教研活动</el-button>
        </div>
      </template>

      <el-form :model="filters" inline class="filter-bar">
        <el-form-item label="教研组">
          <el-select
            v-model="filters.teachingGroupId"
            placeholder="全部教研组"
            clearable
            filterable
            style="width:180px"
          >
            <el-option
              v-for="g in groupList"
              :key="g.id"
              :label="g.name"
              :value="g.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="活动类型">
          <el-select
            v-model="filters.activityType"
            placeholder="全部类型"
            clearable
            style="width:160px"
          >
            <el-option
              v-for="(l, k) in typeMap"
              :key="k"
              :label="l"
              :value="k"
            />
          </el-select>
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

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="activityDate" label="活动日期" width="110" />
        <el-table-column label="教研组" width="140">
          <template #default="{ row }">{{ getGroupName(row.teachingGroupId) }}</template>
        </el-table-column>
        <el-table-column label="活动类型" width="120">
          <template #default="{ row }">{{ typeMap[row.activityType] || row.activityType }}</template>
        </el-table-column>
        <el-table-column
          prop="title"
          label="活动标题"
          min-width="200"
          show-overflow-tooltip
        />
        <el-table-column prop="participantCount" label="参与人数" width="100" />
        <el-table-column prop="totalMembers" label="组成员总数" width="100" />
        <el-table-column label="参与率" width="80">
          <template #default="{ row }">
            <template v-if="row.participationRate != null">
              {{ Math.round(row.participationRate * 100) }}%
            </template>
            <template v-else>-</template>
          </template>
        </el-table-column>
        <el-table-column
          prop="summary"
          label="纪要"
          min-width="200"
          show-overflow-tooltip
        />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="editRow(row)">编辑</el-button>
            <el-popconfirm title="确认删除?" @confirm="deleteRow(row.id)">
              <template #reference>
                <el-button size="small" type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
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

    <el-dialog v-model="showForm" :title="editingId ? '编辑教研活动' : '新建教研活动'" width="580px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="活动日期" required>
          <el-date-picker
            v-model="form.activityDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="教研组" required>
          <el-select v-model="form.teachingGroupId" filterable style="width:100%">
            <el-option
              v-for="g in groupList"
              :key="g.id"
              :label="g.name"
              :value="g.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="活动类型" required>
          <el-select v-model="form.activityType" style="width:100%">
            <el-option
              v-for="(l, k) in typeMap"
              :key="k"
              :label="l"
              :value="k"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="活动标题" required>
          <el-input v-model="form.title" style="width:100%" />
        </el-form-item>
        <el-form-item label="参与人数">
          <el-input-number v-model="form.participantCount" :min="0" style="width:100%" />
          <div class="form-tip">
            <template v-if="form.teachingGroupId && getGroupMemberCount(form.teachingGroupId) > 0">
              该教研组共 {{ getGroupMemberCount(form.teachingGroupId) }} 名成员
            </template>
            <template v-else>请先选择教研组</template>
          </div>
        </el-form-item>
        <el-form-item label="活动纪要">
          <el-input
            v-model="form.summary"
            type="textarea"
            :rows="4"
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
import { ElMessage } from 'element-plus'
import { getTeachingGroups } from '@/api/teachingGroups'
import {
  getResearchActivities, createResearchActivity, updateResearchActivity, deleteResearchActivity, getResearchActivityStats
} from '@/api/inspectorManage'

const typeMap = {
  GROUP_LESSON_PREP: '集体备课', PUBLIC_CLASS: '公开课',
  TEACHING_COMPETITION: '教学竞赛', SEMINAR: '研讨会'
}

const list = ref([])
const stats = ref([])
const loading = ref(false)
const saving = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const showForm = ref(false)
const editingId = ref(null)
const groupList = ref([])

const filters = ref({ teachingGroupId: null, activityType: '', startDate: '', endDate: '' })
const form = ref({ activityDate: '', teachingGroupId: null, activityType: '', title: '', participantCount: 0, summary: '' })

function resetFilters() {
  filters.value = { teachingGroupId: null, activityType: '', startDate: '', endDate: '' }
  page.value = 1
  loadData()
}

function getGroupMemberCount(id) {
  const g = groupList.value.find(x => x.id === id)
  return g ? (g.memberCount || 0) : 0
}

function getGroupName(id) {
  const g = groupList.value.find(x => x.id === id)
  return g ? g.name : '未知'
}

async function loadData() {
  loading.value = true
  try {
    const [listRes, statsRes] = await Promise.all([
      getResearchActivities({ ...filters.value, page: page.value, size: size.value }),
      getResearchActivityStats()
    ])
    list.value = listRes.data.records || []
    total.value = listRes.data.total || 0
    stats.value = statsRes.data || []
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
      await updateResearchActivity(editingId.value, form.value)
    } else {
      await createResearchActivity(form.value)
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
    await deleteResearchActivity(id)
    loadData()
  } catch {
    ElMessage.error('操作失败')
  }
}

onMounted(async () => {
  try {
    const res = await getTeachingGroups()
    groupList.value = res.data || []
  } catch {
    groupList.value = []
  }
  loadData()
})
</script>

<style scoped lang="scss">
.card-header { display: flex; justify-content: space-between; align-items: center; }
.filter-bar { margin-bottom: 16px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: center; }
.mb-16 { margin-bottom: 16px; }
.stat-item { text-align: center; }
.stat-label { font-size: var(--fs-sm); color: var(--el-color-info); }
.stat-value { font-size: 28px; font-weight: 700; color: #303133; margin-top: 4px; }

.form-tip { font-size: var(--fs-xs); color: var(--text-secondary, var(--el-color-info)); margin-top: 4px; }
@media (max-width: 768px) {
  .inspector-research { padding: var(--spacing-md, 16px); }
  .filter-bar { flex-direction: column; align-items: stretch; }
  :deep(.el-table) { font-size: var(--fs-xs); }
}
</style>

<template>
  <div class="inspector-moral">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>德育巡视记录</span>
          <el-button type="primary" @click="showForm = true">新建德育记录</el-button>
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
        <el-form-item label="类别">
          <el-select
            v-model="filters.category"
            placeholder="全部类别"
            clearable
            style="width:160px"
          >
            <el-option
              v-for="(l, k) in categoryMap"
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

      <template v-if="!isMobile">
        <el-table v-loading="loading" :data="list" stripe>
          <el-table-column prop="inspectionDate" label="检查日期" width="120" />
          <el-table-column label="班级" width="120">
            <template #default="{ row }">{{ getClassName(row.classId) }}</template>
          </el-table-column>
          <el-table-column label="类别" width="140">
            <template #default="{ row }">{{ categoryMap[row.category] || row.category }}</template>
          </el-table-column>
          <el-table-column label="评分" width="120">
            <template #default="{ row }"><el-rate :model-value="row.score" disabled show-score score-template="{value}" /></template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="240" show-overflow-tooltip />
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
          <div class="mc-header"><span class="mc-title">{{ getClassName(row.classId) }}</span><el-tag size="small">{{ categoryMap[row.category] || row.category }}</el-tag></div>
          <div class="mc-body">
            <div class="mc-meta"><span class="mc-lbl">日期</span><span>{{ row.inspectionDate }}</span></div>
            <div class="mc-meta"><span class="mc-lbl">评分</span><el-rate :model-value="row.score" disabled show-score score-template="{value}" /></div>
            <div class="mc-desc">{{ row.description }}</div>
          </div>
          <div class="mc-actions">
            <el-button size="small" @click="editRow(row)">编辑</el-button>
            <el-popconfirm title="确认删除?" @confirm="deleteRow(row.id)"><template #reference><el-button size="small" type="danger">删除</el-button></template></el-popconfirm>
          </div>
        </div>
        <el-empty v-if="!loading && list.length === 0" description="暂无德育记录" :image-size="60" />
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

    <el-dialog v-model="showForm" :title="editingId ? '编辑德育记录' : '新建德育记录'" width="540px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="检查日期" required>
          <el-date-picker
            v-model="form.inspectionDate"
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
        <el-form-item label="类别" required>
          <el-select v-model="form.category" style="width:100%">
            <el-option
              v-for="(l, k) in categoryMap"
              :key="k"
              :label="l"
              :value="k"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="评分">
          <el-rate
            v-model="form.score"
            :max="5"
            show-score
            score-template="{value}"
          />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="form.description"
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
import { ElMessage } from 'element-plus'
import { getClassList } from '@/api/classes'
import { useIsMobile } from '@/composables/useIsMobile'
import {
  getMoralInspections, createMoralInspection, updateMoralInspection, deleteMoralInspection
} from '@/api/inspectorManage'

const categoryMap = {
  HYGIENE: '卫生检查', APPEARANCE: '仪容仪表', BREAK_DISCIPLINE: '课间纪律',
  MORNING_READING: '早读', EYE_EXERCISE: '眼保健操'
}

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

const filters = ref({ classId: null, category: '', startDate: '', endDate: '' })
const form = ref({ inspectionDate: '', classId: null, category: '', score: null, description: '' })

function resetFilters() {
  filters.value = { classId: null, category: '', startDate: '', endDate: '' }
  page.value = 1
  loadData()
}

function getClassName(id) {
  const c = classList.value.find(x => x.id === id)
  return c ? c.className : '未知'
}

async function loadData() {
  loading.value = true
  try {
    const res = await getMoralInspections({ ...filters.value, page: page.value, size: size.value })
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
      await updateMoralInspection(editingId.value, form.value)
    } else {
      await createMoralInspection(form.value)
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
    await deleteMoralInspection(id)
    loadData()
  } catch {
    ElMessage.error('操作失败')
  }
}

onMounted(async () => {
  const res = await getClassList()
  classList.value = res.data || []
  loadData()
})
</script>

<style scoped lang="scss">
.card-header { display: flex; justify-content: space-between; align-items: center; }
.filter-bar { margin-bottom: 16px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: center; }

@media (max-width: 768px) {
  .inspector-moral { padding: var(--spacing-md, 16px); }
  .filter-bar { flex-direction: column; align-items: stretch; }
  :deep(.el-table) { font-size: var(--fs-xs); }
}

.mobile-list { display: flex; flex-direction: column; gap: 10px; }
.mobile-card { padding: 14px; background: var(--bg-card); border-radius: var(--radius-md); border: 1px solid var(--border-light); }
.mc-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 8px; margin-bottom: 8px; }
.mc-title { font-weight: 600; font-size: var(--fs-md); color: var(--text-primary); flex: 1; }
.mc-body { display: flex; flex-direction: column; gap: 4px; }
.mc-desc { font-size: var(--fs-sm); color: var(--text-regular); margin: 4px 0; }
.mc-meta { display: flex; align-items: center; gap: 8px; font-size: var(--fs-sm); }
.mc-lbl { color: var(--text-secondary); min-width: 36px; }
.mc-actions { display: flex; gap: 8px; margin-top: 8px; }
</style>

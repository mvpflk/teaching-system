<template>
  <div class="page-card">
    <h3 class="page-title" style="margin-bottom:20px">积分管理后台</h3>

    <el-tabs v-model="tab">
      <el-tab-pane label="👤 学生积分" name="students">
        <div class="toolbar">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索学生姓名/用户名"
            clearable
            class="desktop-width"
            style="width:240px"
            @keyup.enter="loadStudents"
          />
          <el-button type="primary" @click="loadStudents">查询</el-button>
        </div>
        <el-table
          v-loading="loadingStudents"
          :data="studentList"
          stripe
          size="small"
        >
          <el-table-column prop="studentNumber" label="学号" width="120" />
          <el-table-column prop="realName" label="姓名" width="100" />
          <el-table-column prop="totalCredits" label="当前积分" width="100">
            <template #default="{ row }"><span class="text-warning fw-bold">{{ row.totalCredits || 0 }}</span></template>
          </el-table-column>
          <el-table-column prop="titleName" label="称号" width="80">
            <template #default="{ row }"><el-tag size="small">{{ row.titleName }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <el-button size="small" type="success" @click="showAdjust(row, 1)">加分</el-button>
              <el-button size="small" type="danger" @click="showAdjust(row, -1)">扣分</el-button>
              <el-button size="small" @click="viewTransactions(row)">记录</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-dialog
          v-model="adjustVisible"
          :title="(adjustType > 0 ? '加' : '扣') + '积分 - ' + adjustStudent?.realName"
          width="400px"
          destroy-on-close
          append-to-body
        >
          <el-form label-position="top">
            <el-form-item :label="adjustType > 0 ? '加分值' : '扣分值'">
              <el-input-number v-model="adjustAmount" :min="1" :max="1000" />
            </el-form-item>
            <el-form-item label="原因">
              <el-input v-model="adjustReason" placeholder="如：作业优秀奖励/迟到扣分" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="adjustVisible = false">取消</el-button>
            <el-button type="primary" :loading="adjusting" @click="handleAdjust">确认</el-button>
          </template>
        </el-dialog>

        <el-dialog
          v-model="txnVisible"
          :title="'积分记录 - ' + txnStudent?.realName"
          width="600px"
          destroy-on-close
          append-to-body
        >
          <el-table :data="txnList" size="small" stripe>
            <el-table-column prop="description" label="说明" min-width="180" />
            <el-table-column label="类型" width="70">
              <template #default="{ row }">
                <el-tag :type="row.transactionType === 'earn' ? 'success' : 'danger'" size="small">
                  {{ row.transactionType === 'earn' ? '获得' : '消费' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="creditAmount" label="积分" width="70" />
            <el-table-column prop="balanceAfter" label="余额" width="70" />
            <el-table-column prop="createTime" label="时间" width="160" />
          </el-table>
        </el-dialog>
      </el-tab-pane>

      <el-tab-pane label="🛒 商城管理" name="shop">
        <AdminShopPanel @changed="loadData" />
      </el-tab-pane>

      <el-tab-pane label="📜 积分规则" name="rules">
        <AdminRulesPanel @changed="loadData" />
      </el-tab-pane>

      <el-tab-pane label="📦 实物交付" name="deliveries">
        <div class="toolbar">
          <el-radio-group v-model="deliveryFilter" size="small" @change="loadDeliveries">
            <el-radio-button value="pending">待交付</el-radio-button>
            <el-radio-button value="delivered">已交付</el-radio-button>
            <el-radio-button value="all">全部</el-radio-button>
          </el-radio-group>
        </div>
        <el-table
          v-loading="loadingDelivery"
          :data="deliveryList"
          stripe
          size="small"
        >
          <el-table-column prop="studentName" label="学生" width="100" />
          <el-table-column prop="itemName" label="兑换物品" width="180" />
          <el-table-column prop="creditCost" label="消耗积分" width="80" />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 'pending' ? 'warning' : 'success'" size="small">
                {{ row.status === 'pending' ? '待交付' : '已交付' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="兑换时间" width="160" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 'pending'"
                size="small"
                type="success"
                @click="markDelivered(row)"
              >
                确认交付
              </el-button>
              <span v-else class="text-muted">已完成</span>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="deliveryList.length === 0" style="text-align:center;padding:30px;color:var(--text-secondary)">
          暂无{{ deliveryFilter === 'pending' ? '待交付' : '' }}记录
        </div>
      </el-tab-pane>

      <el-tab-pane label="🏆 称号管理" name="titles">
        <el-table
          v-loading="loadingTitles"
          :data="titles"
          stripe
          size="small"
        >
          <el-table-column label="等级" width="60">
            <template #default="{ row }">Lv.{{ row.levelNumber }}</template>
          </el-table-column>
          <el-table-column prop="levelName" label="称号名称" width="100" />
          <el-table-column label="积分区间" width="180">
            <template #default="{ row }">{{ row.minCredits }} ~ {{ row.maxCredits ?? '∞' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <el-button size="small" @click="editTitle(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-dialog
          v-model="titleFormVisible"
          title="编辑称号"
          width="400px"
          destroy-on-close
          append-to-body
        >
          <el-form :model="titleForm" label-position="top">
            <el-form-item label="称号名称"><el-input v-model="titleForm.levelName" /></el-form-item>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="最低积分"><el-input-number v-model="titleForm.minCredits" :min="0" /></el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="最高积分"><el-input-number v-model="titleForm.maxCredits" :min="0" /></el-form-item>
              </el-col>
            </el-row>
          </el-form>
          <template #footer>
            <el-button @click="titleFormVisible = false">取消</el-button>
            <el-button type="primary" @click="saveTitle">保存</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminDeliveries, deliverItem, getAdminStudents, adjustCredit, getCreditTransactions, getAdminTitles, updateAdminTitle } from '@/api/credit'
import AdminShopPanel from './AdminShopPanel.vue'
import AdminRulesPanel from './AdminRulesPanel.vue'

const tab = ref('students')

const searchKeyword = ref('')
const studentList = ref([])
const loadingStudents = ref(false)
const adjustVisible = ref(false)
const adjustStudent = ref(null)
const adjustType = ref(1)
const adjustAmount = ref(10)
const adjustReason = ref('')
const adjusting = ref(false)
const txnVisible = ref(false)
const txnStudent = ref(null)
const txnList = ref([])

const deliveryFilter = ref('pending')
const deliveryList = ref([])
const loadingDelivery = ref(false)

const titles = ref([])
const loadingTitles = ref(false)
const titleFormVisible = ref(false)
const titleForm = ref({ levelNumber: 1, levelName: '', minCredits: 0, maxCredits: null })
const editingTitle = ref(null)

const loadData = () => { loadStudents() }

const loadStudents = async () => {
  loadingStudents.value = true
  try {
    const res = await getAdminStudents(searchKeyword.value || undefined)
    if (res.code === 200) studentList.value = res.data
  } finally { loadingStudents.value = false }
}

const showAdjust = (student, type) => {
  adjustStudent.value = student
  adjustType.value = type
  adjustAmount.value = 10
  adjustReason.value = ''
  adjustVisible.value = true
}

const handleAdjust = async () => {
  if (adjusting.value) return
  adjusting.value = true
  try {
    const amount = adjustType.value > 0 ? adjustAmount.value : -adjustAmount.value
    const res = await adjustCredit({ studentId: adjustStudent.value.studentId, amount, reason: adjustReason.value || (adjustType.value > 0 ? '管理员加分' : '管理员扣分') })
    if (res.code === 200) {
      ElMessage.success(`操作成功，新余额: ${res.data.newBalance}`)
      adjustVisible.value = false
      await loadStudents()
    }
  } finally { adjusting.value = false }
}

const viewTransactions = async (student) => {
  txnStudent.value = student
  txnList.value = []
  txnVisible.value = true
  const res = await getCreditTransactions({ studentId: student.studentId, limit: 50 })
  if (res.code === 200) txnList.value = res.data
}

const loadDeliveries = async () => {
  loadingDelivery.value = true
  try {
    const res = await getAdminDeliveries({ status: deliveryFilter.value })
    if (res.code === 200) deliveryList.value = res.data
  } finally { loadingDelivery.value = false }
}

const markDelivered = async (row) => {
  try {
    await ElMessageBox.confirm(`确认「${row.itemName}」已交付给 ${row.studentName}？`, '确认交付', { type: 'warning' })
  } catch { return }
  const res = await deliverItem(row.id)
  if (res.code === 200) {
    ElMessage.success('已标记为已交付')
    await loadDeliveries()
  }
}

const loadTitles = async () => {
  loadingTitles.value = true
  try {
    const res = await getAdminTitles()
    if (res.code === 200) titles.value = res.data
  } finally { loadingTitles.value = false }
}

const editTitle = (t) => {
  editingTitle.value = t
  titleForm.value = { levelNumber: t.levelNumber, levelName: t.levelName, minCredits: t.minCredits, maxCredits: t.maxCredits }
  titleFormVisible.value = true
}

const saveTitle = async () => {
  const res = await updateAdminTitle(editingTitle.value.id, titleForm.value)
  if (res.code === 200) { ElMessage.success('称号已更新'); titleFormVisible.value = false; await loadTitles() }
}

onMounted(() => {
  loadStudents()
  loadDeliveries()
  loadTitles()
})
</script>

<style scoped>
.toolbar { margin-bottom: 16px; display: flex; gap: 10px; }
.text-muted { color: var(--text-secondary); font-size: var(--fs-sm); }
.text-warning { color: var(--warning-color); }
.fw-bold { font-weight: 700; }

@media (max-width: 768px) {
  .toolbar { flex-direction: column; align-items: stretch; }
  .toolbar :deep(.el-input) { width: 100%; }
  .toolbar :deep(.el-button) { width: 100%; margin-left: 0; }
  :deep(.el-tabs__item) { font-size: var(--fs-xs); padding: 0 10px !important; }
  :deep(.el-tabs__nav-wrap) { overflow-x: auto; }
  :deep(.el-table) { font-size: var(--fs-xs); }
}
</style>

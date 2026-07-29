<template>
  <div class="credit-index">
    <CreditStatsCards :info="info" :title-levels="titleLevels" @signed="loadData" />

    <div class="page-card mb-20">
      <div class="card-header-custom">
        <h3>🏆 成就</h3>
      </div>
      <div v-if="achievements.length === 0" style="text-align:center;padding:20px;color:var(--text-secondary)">
        暂无成就数据
      </div>
      <div v-else class="achievement-grid">
        <div
          v-for="ach in achievements"
          :key="ach.name"
          class="achievement-card"
          :class="{ achieved: ach.achieved }"
        >
          <div class="ach-icon">{{ ach.achieved ? '⭐' : '☆' }}</div>
          <div class="ach-body">
            <div class="ach-name">{{ ach.name }}</div>
            <div class="ach-desc">{{ ach.description }}</div>
          </div>
          <el-tag
            v-if="ach.achieved"
            type="success"
            size="small"
            effect="dark"
          >
            已达成
          </el-tag>
          <el-tag
            v-else
            type="info"
            size="small"
            effect="plain"
          >
            未达成
          </el-tag>
        </div>
      </div>
    </div>

    <div v-if="certificates.length > 0" class="page-card mb-20">
      <div class="card-header-custom">
        <h3>📜 荣誉证书</h3>
      </div>
      <div class="cert-grid">
        <div v-for="cert in certificates" :key="cert.id" class="cert-card">
          <div class="cert-badge">🏅</div>
          <div class="cert-title">荣誉证书</div>
          <div class="cert-student">{{ userName }}</div>
          <div class="cert-desc">凭借优异表现获得</div>
          <div class="cert-date">{{ formatDate(cert.createTime) }}</div>
        </div>
      </div>
    </div>

    <div class="page-card">
      <div class="card-header-custom">
        <h3>积分记录</h3>
      </div>
      <el-table
        v-loading="loading"
        :data="transactions"
        stripe
        empty-text="暂无积分记录"
      >
        <el-table-column prop="description" label="说明" min-width="200" />
        <el-table-column prop="transactionType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.transactionType === 'earn'" type="success">获得</el-tag>
            <el-tag v-else type="danger">消费</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="creditAmount" label="积分" width="100">
          <template #default="{ row }">
            <span :class="row.transactionType === 'earn' ? 'text-success' : 'text-danger'">
              {{ row.transactionType === 'earn' ? '+' : '-' }}{{ row.creditAmount }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="balanceAfter" label="余额" width="100" />
        <el-table-column prop="createTime" label="时间" width="170" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getCreditInfo, getCreditTransactions, getTitleLevels, getAchievements } from '@/api/credit'
import { getProfile } from '@/api/profile'
import { useUserStore } from '@/stores/user'
import CreditStatsCards from './CreditStatsCards.vue'

const userStore = useUserStore()
const info = ref({})
const transactions = ref([])
const titleLevels = ref([])
const achievements = ref([])
const certificates = ref([])
const userName = ref('')

const formatDate = (d) => d ? d.substring(0, 10) : ''
const loading = ref(false)

const loadData = async () => {
  loading.value = true
  try {
    if (userStore.isStudent) {
      const [infoRes, txnRes, achRes, userRes] = await Promise.all([
        getCreditInfo(),
        getCreditTransactions({ limit: 20 }),
        getAchievements(),
        getProfile()
      ])
      if (infoRes.code === 200) info.value = infoRes.data
      if (txnRes.code === 200) {
        transactions.value = txnRes.data
        certificates.value = (txnRes.data || []).filter(t => {
          const desc = t.description || ''
          return desc.includes('荣誉证书') || (t.sourceType === 'redeem' && desc.includes('HONOR'))
        })
      }
      if (achRes.code === 200) achievements.value = achRes.data
      if (userRes.code === 200) userName.value = userRes.data.realName || ''
    }
    const titleRes = await getTitleLevels()
    if (titleRes.code === 200) titleLevels.value = titleRes.data
  } catch { /* */ } finally {
    loading.value = false
  }
}

onMounted(() => loadData())
</script>

<style scoped lang="scss">
.card-header-custom {
  display: flex; justify-content: space-between; align-items: center;
  padding-bottom: 15px; border-bottom: 1px solid var(--border-light); margin-bottom: 15px;
  h3 { margin: 0; font-size: var(--fs-lg); }
}

.mb-20 { margin-bottom: 20px; }

.cert-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}
.cert-card {
  background: var(--bg-warning-light);
  border: 1px solid var(--el-color-warning);
  border-radius: var(--radius-lg);
  padding: 20px;
  text-align: center;
  .cert-badge { font-size: var(--fs-3xl); margin-bottom: 8px; }
  .cert-title { font-size: var(--fs-lg); font-weight: 500; color: var(--text-primary); }
  .cert-student { font-size: var(--fs-lg); font-weight: bold; color: var(--text-primary); margin: 8px 0; }
  .cert-desc { font-size: var(--fs-xs); color: var(--text-secondary); }
  .cert-date { font-size: var(--fs-xs); color: var(--text-secondary); margin-top: 8px; }
}

.achievement-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 10px;
}
.achievement-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border-radius: var(--radius-md);
  border: 0.5px solid var(--border-light);
  transition: all var(--transition-fast);
  &.achieved { background: var(--success-light); border-color: var(--success-color); }
  &:not(.achieved) { background: var(--bg-secondary); opacity: 0.7; }
  .ach-icon { font-size: var(--fs-2xl); flex-shrink: 0; }
  .ach-body { flex: 1; min-width: 0; }
  .ach-name { font-size: var(--fs-sm); font-weight: 500; color: var(--text-primary); }
  .ach-desc { font-size: var(--fs-xs); color: var(--text-secondary); margin-top: 2px; }
}

.text-success { color: var(--success-color); }
.text-danger { color: var(--danger-color); }

@media (max-width: 768px) {
  :deep(.el-table) { font-size: var(--fs-xs); }
  .achievement-grid { grid-template-columns: 1fr; }
  .cert-grid { grid-template-columns: 1fr; }
}
</style>

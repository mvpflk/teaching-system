<template>
  <div class="shop-container">
    <div class="credit-bar mb-24">
      <div class="credit-info">
        <el-icon size="22" style="color:var(--warning-color)"><Coin /></el-icon>
        <span>我的积分</span>
        <strong class="credit-amount">{{ myCredits }}</strong>
      </div>
      <el-button text type="primary" @click="router.push('/credit/index')">查看明细 <el-icon><ArrowRight /></el-icon></el-button>
    </div>

    <div v-if="items.length === 0" class="empty-state-box">
      <div class="empty-icon" style="opacity:0.3;font-size:40px">🎁</div>
      <div class="empty-text">暂无商品上架</div>
    </div>

    <div v-else class="shop-grid">
      <div v-for="item in items" :key="item.id" class="shop-card card-hover">
        <div class="item-icon-wrapper" :style="{ background: getItemBg(item.itemType) }">
          <el-icon :size="36" :color="getItemColor(item.itemType)">
            <component :is="getItemIcon(item.itemType)" />
          </el-icon>
        </div>
        <h4 class="item-name">{{ item.itemName }}</h4>
        <p class="item-desc">{{ item.description || '无描述' }}</p>

        <div class="item-meta">
          <div class="item-price">
            <el-icon size="14" style="color:var(--warning-color)"><Coin /></el-icon>
            <span>{{ item.creditPrice }}</span>
          </div>
          <div class="item-stock">
            <span v-if="item.stockCount === -1" style="color:var(--text-secondary)">无限</span>
            <span v-else :class="getStockClass(item)">{{ item.stockCount - item.soldCount }} 件</span>
          </div>
        </div>

        <el-button
          class="redeem-btn"
          :type="myCredits >= item.creditPrice ? 'warning' : 'info'"
          :disabled="myCredits < item.creditPrice"
          :loading="redeemingId === item.id"
          @click="handleRedeem(item)"
        >
          {{ myCredits >= item.creditPrice ? '立即兑换' : '积分不足' }}
        </el-button>
      </div>
    </div>

    <div class="page-card" style="margin-top:20px">
      <div class="card-header-custom">
        <h4>📋 我的兑换记录</h4>
        <el-button
          text
          size="small"
          :loading="loadingTxns"
          @click="loadMyRedeems"
        >
          刷新
        </el-button>
      </div>
      <template v-if="myRedeems.length > 0">
        <el-table
          v-if="!isMobile"
          :data="myRedeems"
          size="small"
          stripe
        >
          <el-table-column prop="description" label="兑换内容" min-width="200" />
          <el-table-column label="消耗积分" width="100">
            <template #default="{ row }"><span class="text-danger">-{{ row.creditAmount }}</span></template>
          </el-table-column>
          <el-table-column prop="createTime" label="时间" width="170" />
        </el-table>
        <div v-if="isMobile" class="redeem-cards">
          <div v-for="(tx, idx) in myRedeems" :key="idx" class="redeem-card-item">
            <div class="redeem-card-desc">{{ tx.description }}</div>
            <div class="redeem-card-meta">
              <span class="text-danger">-{{ tx.creditAmount }} 积分</span>
              <span class="redeem-card-time">{{ tx.createTime }}</span>
            </div>
          </div>
        </div>
      </template>
      <div v-else style="text-align:center;padding:24px;color:var(--text-secondary);font-size:var(--fs-sm)">
        暂无兑换记录
      </div>
    </div>

    <CustomTitleDialog v-model="customTitleVisible" :setting-title="settingTitle" @confirm="handleSetCustomTitle" />
    <RedeemResultDialog v-model="resultVisible" :redeem-result="redeemResult" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getShopItems, redeemItem, getCreditInfo, setCustomTitle, getCreditTransactions } from '@/api/credit'
import { useIsMobile } from '@/composables/useIsMobile'
import CustomTitleDialog from './CustomTitleDialog.vue'
import RedeemResultDialog from './RedeemResultDialog.vue'

const router = useRouter()
const { isMobile } = useIsMobile()

const items = ref([])
const myCredits = ref(0)
const redeemingId = ref(null)
const resultVisible = ref(false)
const redeemResult = ref({})
const myRedeems = ref([])
const loadingTxns = ref(false)
const customTitleVisible = ref(false)
const settingTitle = ref(false)
const pendingRedeemItem = ref(null)

const getItemIcon = (type) => {
  const icons = { physical: 'Present', virtual: 'Ticket', badge: 'Medal', coupon: 'Discount' }
  return icons[type] || 'Present'
}
const getItemColor = (type) => {
  const colors = { physical: 'var(--primary-color)', virtual: 'var(--el-color-success)', badge: 'var(--deco-purple)', coupon: 'var(--deco-orange)' }
  return colors[type] || 'var(--primary-color)'
}
const getItemBg = (type) => {
  const bg = { physical: 'var(--primary-light)', virtual: 'var(--bg-success-light)', badge: 'var(--bg-deco-purple-light)', coupon: 'var(--bg-deco-orange-light)' }
  return bg[type] || 'var(--primary-light)'
}
const getStockClass = (item) => {
  const remaining = item.stockCount - item.soldCount
  return remaining <= 0 ? 'stock-out' : remaining <= 5 ? 'stock-low' : ''
}

const loadData = async () => {
  const [r1, r2] = await Promise.all([
    getShopItems().catch(() => ({ code: 500 })),
    getCreditInfo().catch(() => ({ code: 500 }))
  ])
  if (r1.code === 200) items.value = r1.data || []
  if (r2.code === 200) myCredits.value = r2.data.totalCredits || 0
  loadMyRedeems()
}

const loadMyRedeems = async () => {
  loadingTxns.value = true
  try {
    const res = await getCreditTransactions({ limit: 50 })
    if (res.code === 200) {
      myRedeems.value = (res.data || []).filter(t => t.sourceType === 'redeem' || t.description?.includes('兑换'))
    }
  } finally { loadingTxns.value = false }
}

const handleRedeem = async (item) => {
  try {
    await ElMessageBox.confirm(`确定要兑换「${item.itemName}」吗？需要 ${item.creditPrice} 积分`, '确认兑换', {
      confirmButtonText: '确认兑换', cancelButtonText: '再想想', type: 'warning'
    })
  } catch { return }

  redeemingId.value = item.id
  try {
    const res = await redeemItem({ itemId: item.id })
    if (res.code === 200) {
      if (item.itemCode === 'CUSTOM_TITLE') {
        customTitleVisible.value = true
        pendingRedeemItem.value = item
        redeemingId.value = null
        return
      }
      redeemResult.value = { itemName: item.itemName, creditPrice: item.creditPrice, ...res.data }
      resultVisible.value = true
      myCredits.value = res.data.remainingCredits || myCredits.value - item.creditPrice
    }
  } catch { ElMessage.error('兑换失败') }
  finally { redeemingId.value = null }
}

const handleSetCustomTitle = async (title) => {
  settingTitle.value = true
  try {
    const res = await setCustomTitle(title)
    if (res.code === 200) {
      ElMessage.success(res.message || '设置成功')
      customTitleVisible.value = false
      myCredits.value -= (pendingRedeemItem.value?.creditPrice || 300)
      loadMyRedeems()
    }
  } catch { ElMessage.error('设置失败') }
  finally { settingTitle.value = false; pendingRedeemItem.value = null }
}

onMounted(() => loadData())
</script>

<style scoped lang="scss">
.credit-bar {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: var(--shadow-sm);
  .credit-info { display: flex; align-items: center; gap: 10px; font-size: var(--fs-md); color: var(--text-regular); }
  .credit-amount { font-size: 22px; font-weight: 500; color: var(--warning-color); }
}

.shop-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.shop-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 24px 20px 20px;
  text-align: center;
  box-shadow: var(--shadow-sm);
  border: 0.5px solid var(--border-light);
  display: flex;
  flex-direction: column;
  align-items: center;

  .item-icon-wrapper {
    width: 64px; height: 64px;
    border-radius: var(--radius-xl);
    display: flex; align-items: center; justify-content: center;
    margin-bottom: 12px;
  }
  .item-name { font-size: var(--fs-md); font-weight: 500; color: var(--text-primary); margin: 0 0 4px; }
  .item-desc { font-size: var(--fs-xs); color: var(--text-secondary); margin: 0 0 12px; line-height: 1.4; }
  .item-meta { display: flex; gap: 16px; margin-bottom: 14px; font-size: var(--fs-sm);
    .item-price { display: flex; align-items: center; gap: 4px; font-weight: 500; color: var(--text-primary); }
    .item-stock { color: var(--text-secondary); .stock-low { color: var(--warning-color); } .stock-out { color: var(--danger-color); } }
  }
  .redeem-btn { width: 100%; border-radius: var(--radius-md) !important; }
}

.mb-24 { margin-bottom: 24px; }
.text-danger { color: var(--danger-color) !important; }

.card-header-custom {
  display: flex; justify-content: space-between; align-items: center;
  padding-bottom: 12px; border-bottom: 1px solid var(--border-light); margin-bottom: 12px;
  h4 { margin: 0; font-size: var(--fs-md); font-weight: 500; }
}

@media (max-width: 768px) {
  .shop-grid { grid-template-columns: repeat(2, 1fr); gap: 12px; }
  .shop-card { padding: 16px; }
  .credit-bar { padding: 12px 16px; flex-wrap: wrap; gap: 8px; .credit-amount { font-size: var(--fs-lg); } }
  .redeem-cards { display: flex; flex-direction: column; gap: 8px; }
  .redeem-card-item {
    display: flex; justify-content: space-between; align-items: center;
    padding: 10px 12px; background: var(--bg-section); border-radius: var(--radius-sm);
    border: 0.5px solid var(--border-light);
    &:active { background: var(--bg-hover-light); }
  }
  .redeem-card-desc { font-size: var(--fs-sm); font-weight: 500; color: var(--text-primary); flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .redeem-card-meta { display: flex; flex-direction: column; align-items: flex-end; gap: 2px; flex-shrink: 0; margin-left: 8px; font-size: var(--fs-xs); }
  .redeem-card-time { color: var(--text-secondary); font-size: var(--fs-xs); }
}
</style>

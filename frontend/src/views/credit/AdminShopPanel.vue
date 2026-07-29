<template>
  <div>
    <div class="toolbar">
      <el-button type="primary" @click="showAddItem">添加商品</el-button>
    </div>
    <el-table
      v-loading="loadingShop"
      :data="shopItems"
      stripe
      size="small"
    >
      <el-table-column prop="itemName" label="商品名" width="150" />
      <el-table-column prop="itemType" label="类型" width="80">
        <template #default="{ row }">{{ row.itemType === 'physical' ? '实物' : '虚拟' }}</template>
      </el-table-column>
      <el-table-column prop="creditPrice" label="价格" width="80" />
      <el-table-column prop="stock" label="库存" width="70" />
      <el-table-column prop="soldCount" label="已售" width="70" />
      <el-table-column prop="description" label="描述" min-width="150" />
      <el-table-column label="状态" width="70">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '上架' : '下架' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button size="small" @click="showEditItem(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteItem(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="itemFormVisible"
      :title="editingItem ? '编辑商品' : '添加商品'"
      width="500px"
      destroy-on-close
      append-to-body
    >
      <el-form
        ref="itemFormRef"
        :model="itemForm"
        :rules="itemRules"
        label-position="top"
      >
        <el-form-item label="商品名称"><el-input v-model="itemForm.itemName" /></el-form-item>
        <el-form-item label="商品编码"><el-input v-model="itemForm.itemCode" :disabled="!!editingItem" /></el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="类型">
              <el-select v-model="itemForm.itemType" style="width:100%">
                <el-option label="实物" value="physical" /><el-option label="虚拟" value="virtual" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="价格(积分)">
              <el-input-number v-model="itemForm.creditPrice" :min="1" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="库存">
              <el-input-number v-model="itemForm.stock" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="itemForm.sortOrder" :min="0" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述"><el-input v-model="itemForm.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="图标"><el-input v-model="itemForm.iconClass" placeholder="如: Coin" /></el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="itemForm.status"
            :active-value="1"
            :inactive-value="0"
            active-text="上架"
            inactive-text="下架"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingItem" @click="saveItem">{{ editingItem ? '保存' : '添加' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminShop, adminCreateShop, adminUpdateShop, adminDeleteShop } from '@/api/credit'

const emit = defineEmits(['changed'])

const shopItems = ref([])
const loadingShop = ref(false)
const savingItem = ref(false)
const itemFormVisible = ref(false)
const editingItem = ref(null)

const initItemForm = () => ({
  itemName: '', itemCode: '', itemType: 'virtual', creditPrice: 10,
  stock: 0, sortOrder: 0, description: '', iconClass: '', status: 1,
})

const itemForm = ref(initItemForm())

const itemFormRef = ref(null)
const itemRules = {
  itemName: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  itemCode: [{ required: true, message: '请输入商品编码', trigger: 'blur' }],
  creditPrice: [{ required: true, message: '请输入积分价格', trigger: 'blur' }],
}

const loadShop = async () => {
  loadingShop.value = true
  try {
    const res = await getAdminShop()
    if (res.code === 200) shopItems.value = res.data
  } finally { loadingShop.value = false }
}

const showAddItem = () => {
  editingItem.value = null
  itemForm.value = initItemForm()
  itemFormVisible.value = true
}

const showEditItem = (item) => {
  editingItem.value = item
  itemForm.value = { ...item }
  itemFormVisible.value = true
}

const saveItem = async () => {
  if (savingItem.value) return
  try { await itemFormRef.value?.validate() } catch { return }
  savingItem.value = true
  try {
    const data = { ...itemForm.value }
    if (editingItem.value) {
      const res = await adminUpdateShop(editingItem.value.id, data)
      if (res.code === 200) { ElMessage.success('已更新'); itemFormVisible.value = false; await loadShop(); emit('changed') }
    } else {
      const res = await adminCreateShop(data)
      if (res.code === 200) { ElMessage.success('已添加'); itemFormVisible.value = false; await loadShop(); emit('changed') }
    }
  } finally {
    savingItem.value = false
  }
}

const deleteItem = async (item) => {
  try {
    await ElMessageBox.confirm(`确定删除商品「${item.itemName}」？`, '确认', { type: 'warning' })
    const res = await adminDeleteShop(item.id)
    if (res.code === 200) { ElMessage.success('已删除'); await loadShop(); emit('changed') }
  } catch { /* cancelled */ }
}

</script>

<style scoped>
.toolbar { margin-bottom: 16px; display: flex; gap: 10px; }
@media (max-width: 768px) {
  .toolbar { flex-direction: column; align-items: stretch; }
  .toolbar :deep(.el-input), .toolbar :deep(.el-select) { width: 100%; }
  .toolbar :deep(.el-button) { width: 100%; margin-left: 0; }
  :deep(.el-table) { overflow-x: auto; display: block; }
}
</style>

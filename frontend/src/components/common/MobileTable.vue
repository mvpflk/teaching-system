<template>
  <div class="mt-container">
    <!-- 桌面端: el-table -->
    <div v-if="!isMobile" class="mt-desktop">
      <el-table
        :data="data"
        :row-key="rowKey"
        v-bind="$attrs"
        @row-click="onRowClick"
      >
        <template v-for="col in visibleColumns" :key="col.prop || col.label">
          <el-table-column v-bind="col">
            <template v-if="col.slot" #default="scope">
              <slot :name="col.slot" v-bind="scope" />
            </template>
          </el-table-column>
        </template>
      </el-table>
    </div>

    <!-- 移动端: 卡片列表 -->
    <div v-else class="mt-mobile">
      <div
        v-for="row in data"
        :key="row[rowKey] || row.id"
        class="mt-card"
        @click="onRowClick(row)"
      >
        <div class="mt-card-header">
          <slot name="card-header" :row="row">
            <span class="mt-card-title">{{ getCardTitle(row) }}</span>
          </slot>
        </div>
        <div class="mt-card-body">
          <slot name="card" :row="row">
            <div v-for="col in mobileColumns" :key="col.prop" class="mt-card-field">
              <span class="mt-card-label">{{ col.label }}</span>
              <span class="mt-card-value">{{ row[col.prop] }}</span>
            </div>
          </slot>
        </div>
        <div v-if="$slots['card-footer']" class="mt-card-footer">
          <slot name="card-footer" :row="row" />
        </div>
      </div>
      <EmptyState v-if="!data || data.length === 0" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useIsMobile } from '@/composables/useIsMobile'
import EmptyState from './EmptyState.vue'

const { isMobile } = useIsMobile()

const props = defineProps({
  columns: { type: Array, required: true },
  data: { type: Array, default: () => [] },
  rowKey: { type: String, default: 'id' },
  cardTitle: { type: String, default: 'title' }
})

const emit = defineEmits(['row-click'])

const visibleColumns = computed(() =>
  props.columns.filter(c => !c.mobileHidden)
)

const mobileColumns = computed(() =>
  props.columns.filter(c => !c.mobileHidden && c.prop !== props.cardTitle)
)

function getCardTitle(row) {
  return row[props.cardTitle] || row.title || row.name || ''
}

function onRowClick(row) {
  emit('row-click', row)
}
</script>

<style scoped>
.mt-container { width: 100%; }
.mt-mobile { display: flex; flex-direction: column; gap: var(--mobile-card-gap, 8px); }
.mt-card {
  padding: 14px;
  background: var(--bg-card);
  border: 0.5px solid var(--border-color);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background 0.15s;
}
.mt-card:active { background: var(--bg-hover); transform: scale(0.98); }
.mt-card-header { margin-bottom: 8px; }
.mt-card-title { font-size: var(--fs-md); font-weight: 600; color: var(--text-primary); }
.mt-card-field { display: flex; justify-content: space-between; padding: 4px 0; font-size: var(--fs-sm); }
.mt-card-label { color: var(--text-secondary); }
.mt-card-value { color: var(--text-regular); text-align: right; }
.mt-card-footer { margin-top: 10px; padding-top: 8px; border-top: 0.5px solid var(--border-light); }
</style>

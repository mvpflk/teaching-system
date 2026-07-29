<template>
  <div class="mcl-container">
    <!-- 搜索栏（折叠） -->
    <div v-if="searchable" class="mcl-search">
      <el-input
        v-if="showSearch"
        ref="searchInputRef"
        v-model="searchText"
        :placeholder="searchPlaceholder"
        size="large"
        clearable
        @input="onSearchInput"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button
        v-else
        text
        circle
        @click="toggleSearch"
      >
        <el-icon size="20"><Search /></el-icon>
      </el-button>
    </div>

    <!-- 卡片列表 -->
    <div v-if="filteredItems.length > 0" class="mcl-list">
      <slot name="cards" :items="filteredItems" />
    </div>

    <!-- 空态 -->
    <EmptyState v-else-if="!loading" :title="emptyTitle" :description="emptyDesc" />

    <!-- 加载骨架屏 -->
    <div v-if="loading" class="mcl-loading">
      <SkeletonCard v-for="i in 3" :key="i" :rows="2" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { Search } from '@element-plus/icons-vue'
import SkeletonCard from './SkeletonCard.vue'
import EmptyState from './EmptyState.vue'

const props = defineProps({
  items: { type: Array, default: () => [] },
  searchable: { type: Boolean, default: true },
  searchPlaceholder: { type: String, default: '搜索...' },
  searchKeys: { type: Array, default: () => ['title', 'name', 'description'] },
  filterFn: { type: Function, default: null },
  emptyTitle: { type: String, default: '暂无数据' },
  emptyDesc: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  debounceMs: { type: Number, default: 300 }
})

const showSearch = ref(false)
const searchText = ref('')
const searchInputRef = ref(null)
let debounceTimer = null

const filteredItems = computed(() => {
  if (!searchText.value.trim()) return props.items
  const keyword = searchText.value.trim().toLowerCase()
  if (props.filterFn) return props.items.filter(item => props.filterFn(item, keyword))
  return props.items.filter(item =>
    props.searchKeys.some(key => {
      const val = item[key]
      return val && String(val).toLowerCase().includes(keyword)
    })
  )
})

function toggleSearch() {
  showSearch.value = !showSearch.value
  if (showSearch.value) {
    nextTick(() => searchInputRef.value?.focus())
  } else {
    searchText.value = ''
  }
}

function onSearchInput() {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => { /* filteredItems 自动响应 */ }, props.debounceMs)
}
</script>

<style scoped>
.mcl-search { margin-bottom: 12px; display: flex; justify-content: flex-end; }
.mcl-list { display: flex; flex-direction: column; gap: var(--mobile-card-gap, 8px); }
.mcl-loading { margin-top: 8px; }
</style>

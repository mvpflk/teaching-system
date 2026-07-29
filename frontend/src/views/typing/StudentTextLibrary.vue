<template>
  <div class="library-page">
    <div class="page-header">
      <el-button text class="back-btn" @click="router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <h3>练习素材库</h3>
      <span v-if="total > 0" class="header-count">共 {{ total }} 篇</span>
    </div>

    <!-- 筛选+排序 -->
    <div class="filters">
      <el-select
        v-model="filters.language"
        placeholder="语言"
        clearable
        size="small"
        class="filter-sel"
        @change="loadData(true)"
      >
        <el-option label="全部" value="" />
        <el-option label="英文" value="en" />
        <el-option label="中文" value="zh" />
        <el-option label="混合" value="mixed" />
      </el-select>
      <el-select
        v-model="filters.difficulty"
        placeholder="难度"
        clearable
        size="small"
        class="filter-sel"
        @change="loadData(true)"
      >
        <el-option label="全部" :value="null" />
        <el-option
          v-for="n in 5"
          :key="n"
          :label="'⭐'.repeat(n)"
          :value="n"
        />
      </el-select>
      <el-select
        v-model="filters.category"
        placeholder="分类"
        clearable
        size="small"
        class="filter-sel"
        @change="loadData(true)"
      >
        <el-option label="全部" value="" />
        <el-option
          v-for="c in categories"
          :key="c"
          :label="c"
          :value="c"
        />
      </el-select>
      <el-select
        v-model="sortBy"
        placeholder="排序"
        size="small"
        class="filter-sel sort-sel"
        @change="loadData(true)"
      >
        <el-option label="最新" value="newest" />
        <el-option label="最难" value="hard" />
        <el-option label="最简" value="easy" />
        <el-option label="最长" value="longest" />
      </el-select>
      <el-input
        v-model="filters.keyword"
        placeholder="搜索标题"
        clearable
        size="small"
        class="filter-search"
        @keyup.enter="loadData(true)"
        @clear="loadData(true)"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
    </div>

    <!-- 骨架屏 -->
    <div v-if="loading" class="sk-grid">
      <div v-for="i in 6" :key="i" class="sk-card">
        <div class="sk-line w-60" /><div class="sk-line w-40" /><div class="sk-line w-80" />
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="list.length === 0" class="empty">
      <el-empty description="暂无练习素材" :image-size="80">
        <el-button v-if="hasFilter" size="small" @click="resetFilters">清除筛选</el-button>
      </el-empty>
    </div>

    <!-- 素材卡片 -->
    <div v-else class="card-grid">
      <div
        v-for="t in list"
        :key="t.id"
        class="text-card"
        :class="{ practiced: practicedIds.has(t.id) }"
        @click="startPractice(t.id)"
      >
        <div class="card-header">
          <span class="card-title">{{ t.title }}</span>
          <span v-if="practicedIds.has(t.id)" class="card-practiced">已练</span>
        </div>
        <div class="card-tags">
          <el-tag size="small">{{ langLabel(t.language) }}</el-tag>
          <el-tag size="small" type="warning">{{ '⭐'.repeat(t.difficulty || 1) }}</el-tag>
          <el-tag v-if="t.category" size="small" type="success">{{ t.category }}</el-tag>
          <span class="card-chars">{{ t.content?.length || 0 }}字</span>
        </div>
        <div class="card-preview">{{ (t.content || '').slice(0, 120) }}{{ t.content?.length > 120 ? '…' : '' }}</div>
        <div class="card-footer">
          <el-button
            size="small"
            type="primary"
            text
            class="card-start"
          >
            开始练习 →
          </el-button>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="total > pageSize" class="pagination-wrap">
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next, jumper"
        background
        @current-change="loadData()"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getStudentTexts, getStudentTextCategories } from '@/api/typing'
import { ArrowLeft, Search } from '@element-plus/icons-vue'

const router = useRouter()
const list = ref([])
const categories = ref([])
const loading = ref(true)
const page = ref(1)
const pageSize = ref(12)
const total = ref(0)
const sortBy = ref('newest')
const practicedIds = ref(new Set())
const filters = reactive({ language: '', difficulty: null, category: '', keyword: '' })

const hasFilter = computed(() => filters.language || filters.difficulty || filters.category || filters.keyword)

const langLabel = (l) => ({ en: '英文', zh: '中文', mixed: '混合' }[l] || l)

async function loadData(resetPage = false) {
  loading.value = true
  if (resetPage) page.value = 1
  try {
    const params = { page: page.value, size: pageSize.value }
    if (filters.language) params.language = filters.language
    if (filters.difficulty) params.difficulty = filters.difficulty
    if (filters.category) params.category = filters.category
    if (filters.keyword) params.keyword = filters.keyword
    if (sortBy.value === 'hard') { params.difficulty = params.difficulty || 5; params.orderBy = 'difficulty' }
    else if (sortBy.value === 'easy') { params.orderBy = 'difficulty_asc' }
    else if (sortBy.value === 'longest') { params.orderBy = 'length' }
    const res = await getStudentTexts(params)
    if (res.code === 200) {
      list.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch {}
  loading.value = false
}

async function loadCategories() {
  try {
    const res = await getStudentTextCategories()
    if (res.code === 200) categories.value = res.data || []
  } catch {}
}

function loadPracticedIds() {
  try {
    const stored = localStorage.getItem('typing-practiced-ids')
    if (stored) practicedIds.value = new Set(JSON.parse(stored))
  } catch {}
}

function markPracticed(id) {
  practicedIds.value.add(id)
  localStorage.setItem('typing-practiced-ids', JSON.stringify([...practicedIds.value]))
}

function startPractice(textId) {
  markPracticed(textId)
  router.push(`/typing?textId=${textId}`)
}

function resetFilters() {
  filters.language = ''
  filters.difficulty = null
  filters.category = ''
  filters.keyword = ''
  loadData(true)
}

onMounted(() => { loadCategories(); loadData(); loadPracticedIds() })
</script>

<style scoped>
.library-page { max-width: 1100px; margin: 0 auto; padding: 16px; }
.page-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.page-header h3 { margin: 0; }
.back-btn { color: var(--text-secondary); font-size: var(--fs-sm); }
.back-btn:hover { color: var(--text-primary); }
.header-count { font-size: var(--fs-sm); color: var(--text-secondary); }

.filters { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 16px; align-items: center; }
.filter-sel { width: 120px; }
.sort-sel { width: 110px; }
.filter-search { width: 180px; }

.card-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 12px; }
.text-card { background: var(--bg-card); border: 1px solid var(--border-color); border-radius: var(--radius-md); padding: 14px; cursor: pointer; transition: all var(--transition-fast); display: flex; flex-direction: column; }
.text-card:hover { box-shadow: var(--shadow-base); border-color: var(--primary-color); transform: translateY(-1px); }
.text-card:active { transform: translateY(0); }
.text-card.practiced { border-left: 3px solid var(--el-color-success); }
.card-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.card-title { font-weight: 600; font-size: var(--fs-md); color: var(--text-primary); }
.card-practiced { font-size: var(--fs-xs); padding: 1px 6px; background: var(--el-color-success-light-9); color: var(--el-color-success); border-radius: 8px; }
.card-tags { display: flex; gap: 4px; align-items: center; flex-wrap: wrap; margin-bottom: 8px; }
.card-chars { font-size: var(--fs-xs); color: var(--text-secondary); margin-left: auto; }
.card-preview { font-size: var(--fs-sm); color: var(--text-secondary); line-height: 1.5; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; flex: 1; }
.card-footer { margin-top: 10px; }
.card-start { font-size: var(--fs-sm); }

.empty { padding: 40px 0; }

.sk-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 12px; }
.sk-card { background: var(--bg-card); border-radius: var(--radius-md); padding: 14px; display: flex; flex-direction: column; gap: 8px; }
.sk-line { height: 14px; border-radius: 4px; background: linear-gradient(90deg, var(--bg-section, #f5f7fa) 25%, var(--bg-card, #fff) 50%, var(--bg-section, #f5f7fa) 75%); background-size: 200% 100%; animation: sk-shimmer 1.5s infinite; }
@keyframes sk-shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
.w-40 { width: 40%; } .w-60 { width: 60%; } .w-80 { width: 80%; }

.pagination-wrap { display: flex; justify-content: center; margin-top: 20px; }

@media (max-width: 768px) {
  .library-page { padding: 8px; }
  .card-grid { grid-template-columns: 1fr; }
  .filters { flex-direction: column; align-items: stretch; }
  .filter-sel, .filter-search { width: 100% !important; }
  .page-header { flex-wrap: wrap; }
}
</style>

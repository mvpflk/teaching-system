<template>
  <teleport to="body">
    <transition name="palette-fade">
      <div
        v-if="visible"
        class="command-palette-overlay"
        @click.self="close"
        @keydown="handleKeydown"
      >
        <div class="command-palette-panel">
          <!-- 搜索框 -->
          <div class="palette-search">
            <el-icon class="palette-search-icon" :size="18"><Search /></el-icon>
            <input
              ref="inputRef"
              v-model="query"
              type="text"
              class="palette-search-input"
              :placeholder="placeholderText"
              @input="onQueryChange"
            />
            <kbd class="palette-kbd-hint">Esc</kbd>
          </div>

          <!-- 结果列表 -->
          <div ref="resultsRef" class="palette-results">
            <template v-if="filteredCommands.length === 0">
              <div class="palette-empty">无匹配结果</div>
            </template>
            <template v-for="group in filteredCommands" v-else :key="group.group">
              <div class="palette-group-label">{{ group.group }}</div>
              <div
                v-for="(item, idx) in group.items"
                :key="item.label"
                :ref="el => setItemRef(group.group, idx, el)"
                class="palette-item"
                :class="{ 'palette-item--active': isActive(group, idx) }"
                @click="execute(item)"
                @mouseenter="onHover(group, idx)"
              >
                <el-icon class="palette-item-icon" :size="18">
                  <component :is="iconMap[item.icon]" />
                </el-icon>
                <span class="palette-item-label">{{ item.label }}</span>
                <span v-if="item.desc" class="palette-item-desc">{{ item.desc }}</span>
              </div>
            </template>
          </div>

          <!-- 底部提示 -->
          <div class="palette-footer">
            <span class="palette-footer-hint">
              <kbd>&uarr;</kbd><kbd>&darr;</kbd> 导航
            </span>
            <span class="palette-footer-hint">
              <kbd>&#8629;</kbd> 执行
            </span>
            <span class="palette-footer-hint">
              <kbd>Esc</kbd> 关闭
            </span>
          </div>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import {
  List, Plus, Collection, Checked, HomeFilled,
  Coin, School, ChatDotRound, Files, Search
} from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'createTask'])

const router = useRouter()

// ---- 图标映射 ----
const iconMap = {
  List, Plus, Collection, Checked, HomeFilled,
  Coin, School, ChatDotRound, Files
}

// ---- 命令列表 ----
const COMMANDS = [
  {
    group: '任务',
    items: [
      { label: '任务列表', icon: 'List', desc: '查看所有教学任务', action: () => router.push('/teacher/tasks/list') },
      { label: '创建新考试', icon: 'Plus', desc: '新建形成性/终结性评价', action: () => emit('createTask', 'EXAM') },
      { label: '布置新作业', icon: 'Plus', desc: '新建课前/课中/课后任务', action: () => emit('createTask', 'HOMEWORK') },
      { label: '题库管理', icon: 'Collection', desc: '管理试题和知识点', action: () => router.push('/teacher/tasks/question-bank') },
      { label: '待审核', icon: 'Checked', desc: '查看待审核任务', action: () => router.push('/teacher/tasks/pending-review') }
    ]
  },
  {
    group: '页面',
    items: [
      { label: '首页仪表板', icon: 'HomeFilled', desc: '教学数据总览', action: () => router.push('/home') },
      { label: '积分中心', icon: 'Coin', desc: '积分签到/商城/排行', action: () => router.push('/credit') },
      { label: '班级管理', icon: 'School', desc: '班级和学生管理', action: () => router.push('/class') },
      { label: 'BBS论坛', icon: 'ChatDotRound', desc: '师生互动社区', action: () => router.push('/bbs') },
      { label: '题库管理', icon: 'Files', desc: '试题库和组卷', action: () => router.push('/teacher/tasks/question-bank') }
    ]
  }
]

// ---- 状态 ----
const query = ref('')
const visible = ref(false)
const inputRef = ref(null)
const resultsRef = ref(null)

// 扁平化索引：[{ group, idx }]
const activeIndex = ref(0)
const itemRefs = {}

// ---- 计算属性 ----
const placeholderText = computed(() => {
  const cmds = COMMANDS.flatMap(g => g.items.map(i => i.label))
  const idx = Math.floor(Math.random() * cmds.length)
  return `搜索命令... 试试 "${cmds[idx]}"`
})

const filteredCommands = computed(() => {
  if (!query.value.trim()) return COMMANDS
  const q = query.value.toLowerCase()
  return COMMANDS.map(group => ({
    ...group,
    items: group.items.filter(item =>
      item.label.toLowerCase().includes(q) ||
      (item.desc || '').toLowerCase().includes(q)
    )
  })).filter(g => g.items.length > 0)
})

// 获取扁平化列表
const flatItems = computed(() => {
  const result = []
  for (const group of filteredCommands.value) {
    for (let i = 0; i < group.items.length; i++) {
      result.push({ group: group.group, idx: i })
    }
  }
  return result
})

const maxIndex = computed(() => Math.max(0, flatItems.value.length - 1))

// ---- 方法 ----
const isActive = (group, idx) => {
  const item = flatItems.value[activeIndex.value]
  return item && item.group === group.group && item.idx === idx
}

const setItemRef = (group, idx, el) => {
  if (el) {
    itemRefs[`${group}:${idx}`] = el
  }
}

const onHover = (group, idx) => {
  const flatIdx = flatItems.value.findIndex(f => f.group === group.group && f.idx === idx)
  if (flatIdx !== -1) {
    activeIndex.value = flatIdx
  }
}

const onQueryChange = () => {
  activeIndex.value = 0
}

const execute = (item) => {
  if (item.action) {
    item.action()
  }
  close()
}

const close = () => {
  visible.value = false
}

const scrollToActive = () => {
  const item = flatItems.value[activeIndex.value]
  if (!item) return
  const el = itemRefs[`${item.group}:${item.idx}`]
  if (el && resultsRef.value) {
    el.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
  }
}

const handleKeydown = (e) => {
  switch (e.key) {
    case 'ArrowDown':
      e.preventDefault()
      activeIndex.value = Math.min(activeIndex.value + 1, maxIndex.value)
      nextTick(() => scrollToActive())
      break
    case 'ArrowUp':
      e.preventDefault()
      activeIndex.value = Math.max(activeIndex.value - 1, 0)
      nextTick(() => scrollToActive())
      break
    case 'Enter':
      e.preventDefault()
      const item = flatItems.value[activeIndex.value]
      if (item) {
        const group = filteredCommands.value.find(g => g.group === item.group)
        if (group) {
          execute(group.items[item.idx])
        }
      }
      break
    case 'Escape':
      e.preventDefault()
      close()
      break
  }
}

// ---- 全局快捷键 Ctrl+K ----
const globalHandler = (e) => {
  if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
    e.preventDefault()
    visible.value = true
  }
}

// ---- watchers ----
watch(visible, (val) => {
  emit('update:modelValue', val)
  if (val) {
    query.value = ''
    activeIndex.value = 0
    nextTick(() => {
      inputRef.value?.focus()
    })
  }
})

watch(() => props.modelValue, (val) => {
  if (val !== visible.value) {
    visible.value = val
  }
})

// ---- 生命周期 ----
onMounted(() => {
  window.addEventListener('keydown', globalHandler)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', globalHandler)
})
</script>

<style scoped>
/* ==================== 遮罩 ==================== */
.command-palette-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 3000;
  display: flex;
  justify-content: center;
  padding-top: 20vh;
}

/* ==================== 面板 ==================== */
.command-palette-panel {
  width: 560px;
  max-width: calc(100vw - 32px);
  max-height: 480px;
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--border-light);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  align-self: flex-start;
}

/* ==================== 搜索框 ==================== */
.palette-search {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 20px;
  border-bottom: 1px solid var(--border-light);
}

.palette-search-icon {
  color: var(--text-secondary);
  flex-shrink: 0;
}

.palette-search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: var(--fs-md);
  color: var(--text-primary);
  background: transparent;
  font-family: inherit;
  line-height: 1.4;
  min-width: 0;
}

.palette-search-input::placeholder {
  color: var(--text-placeholder);
}

.palette-kbd-hint {
  flex-shrink: 0;
}

/* ==================== 结果列表 ==================== */
.palette-results {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  overscroll-behavior: contain;
}

.palette-group-label {
  padding: 8px 12px 4px;
  font-size: var(--fs-xs);
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.palette-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background var(--transition-fast);
}

.palette-item:hover,
.palette-item--active {
  background: var(--bg-section);
}

.palette-item-icon {
  color: var(--text-secondary);
  flex-shrink: 0;
  transition: color var(--transition-fast);
}

.palette-item--active .palette-item-icon {
  color: var(--primary-color);
}

.palette-item-label {
  font-size: var(--fs-base);
  color: var(--text-primary);
  font-weight: 500;
  white-space: nowrap;
}

.palette-item-desc {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  margin-left: auto;
  white-space: nowrap;
}

.palette-empty {
  padding: 32px 16px;
  text-align: center;
  font-size: var(--fs-sm);
  color: var(--text-secondary);
}

/* ==================== 底部提示 ==================== */
.palette-footer {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 20px;
  border-top: 1px solid var(--border-light);
  background: var(--bg-section);
  flex-shrink: 0;
}

.palette-footer-hint {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}

/* ==================== kbd 快捷键样式 ==================== */
kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 2px 6px;
  font-size: var(--fs-xs);
  font-family: inherit;
  line-height: 1.4;
  color: var(--text-secondary);
  background: var(--bg-section);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xs);
  min-width: 20px;
  box-shadow: 0 1px 0 var(--border-color);
}

/* ==================== 过渡动画 ==================== */
.palette-fade-enter-active {
  transition: opacity 0.15s ease;
}
.palette-fade-leave-active {
  transition: opacity 0.1s ease;
}
.palette-fade-enter-from,
.palette-fade-leave-to {
  opacity: 0;
}

/* ==================== 暗色模式适配 ==================== */
.dark .command-palette-overlay {
  background: rgba(0, 0, 0, 0.6);
}

.dark .command-palette-panel {
  background: var(--bg-card);
  border-color: var(--border-color);
}

.dark .palette-item:hover,
.dark .palette-item--active {
  background: rgba(67, 97, 238, 0.1);
}

.dark kbd {
  background: var(--bg-secondary);
  border-color: var(--border-color);
  box-shadow: 0 1px 0 var(--border-color);
}

/* ==================== 响应式 ==================== */
@media (max-width: 768px) {
  .command-palette-overlay {
    padding-top: 8vh;
  }

  .command-palette-panel {
    width: calc(100vw - 24px);
    max-height: 56vh;
    border-radius: var(--radius-lg);
  }

  .palette-search {
    padding: 14px 16px;
  }

  .palette-search-input {
    font-size: var(--fs-base);
  }

  .palette-item-desc {
    display: none;
  }

  .palette-footer {
    padding: 8px 16px;
    gap: 12px;
  }
}
</style>

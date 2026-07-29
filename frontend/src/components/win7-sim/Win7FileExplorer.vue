<template>
  <div class="explorer">
    <div class="explorer-toolbar">
      <button title="向上" @click="navUp">▲</button>
      <button title="刷新" @click="refresh">↻</button>
      <div class="address-bar" @click="store.recordAction('click', 'addressBar')">{{ currentPath }}</div>
      <input
        v-model="searchQuery"
        placeholder="搜索..."
        class="search-box"
        @click="store.recordAction('click', 'searchBox')"
        @input="doSearch"
      />
    </div>
    <div class="explorer-body">
      <div class="tree-panel">
        <template v-for="drive in fileSystem.children" :key="drive.name">
          <div
            class="tree-node"
            :class="{ expanded: expandedDrives[drive.name] }"
            @click="toggleDrive(drive); onDriveSelect(drive)"
          >
            <span class="tree-arrow">{{ expandedDrives[drive.name] ? '▼' : '▶' }}</span>
            <span>💾 {{ drive.name }}</span>
          </div>
          <template v-if="expandedDrives[drive.name]">
            <div
              v-for="child in drive.children || []"
              :key="child.name"
              class="tree-node tree-child"
              :class="{ selected: currentPath.includes(child.name) }"
              @click="onTreeSelect((drive.name.replace('本地磁盘 (','').replace(')','')) + '/' + child.name)"
            >
              <span class="tree-arrow"></span>
              <span>📁 {{ child.name }}</span>
            </div>
          </template>
        </template>
      </div>
      <div class="list-panel" @contextmenu.prevent="onListContextMenu($event)">
        <div class="list-header">
          <span class="col-name">名称</span><span class="col-date">修改日期</span><span class="col-type">类型</span><span class="col-size">大小</span>
        </div>
        <div
          v-for="item in displayItems"
          :key="item.name"
          class="list-row"
          :class="{ selected: selectedItem?.name === item.name }"
          @click="onItemClick(item)"
          @dblclick="onItemDblClick(item)"
          @contextmenu.stop="onItemContextMenu(item, $event)"
        >
          <span class="col-name">{{ item.type === 'folder' ? '📁' : '📄' }} {{ item.name }}</span>
          <span class="col-date">—</span>
          <span class="col-type">{{ item.type === 'folder' ? '文件夹' : (item.ext || '文件') }}</span>
          <span class="col-size">{{ item.size ? item.size + ' B' : '' }}</span>
        </div>
        <div v-if="!displayItems.length" class="list-empty">此文件夹为空</div>
      </div>
    </div>
    <div class="statusbar">共 {{ displayItems.length }} 个对象</div>
    <Win7ContextMenu
      :visible="ctxMenu.visible"
      :x="ctxMenu.x"
      :y="ctxMenu.y"
      :items="ctxMenu.items"
      @close="ctxMenu.visible = false"
      @action="onCtxAction"
    />
  </div>
</template>
<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useWin7SimStore } from '@/stores/win7Sim'
import Win7ContextMenu from './Win7ContextMenu.vue'

const store = useWin7SimStore()
const ctxMenu = reactive({ visible: false, x: 0, y: 0, items: [] })
const currentPath = ref('C:')
const selectedItem = ref(null)
const searchQuery = ref('')
const expandedDrives = reactive({})
const treeExpanded = reactive({})  // track expanded sub-folders

const fileSystem = computed(() => store.fileSystem)
const currentNode = computed(() => {
  // normalize path: handle both C: and C:/Windows formats
  const path = currentPath.value.replace(/\\/g, '/')
  return store.findNode(path) || store.findNode('C:')
})
const displayItems = computed(() => {
  const items = currentNode.value?.children || []
  if (!searchQuery.value) return items
  const q = searchQuery.value.toLowerCase()
  return items.filter(i => i.name.toLowerCase().includes(q))
})

function toggleDrive(drive) { expandedDrives[drive.name] = !expandedDrives[drive.name] }
function onDriveSelect(drive) {
  const driveLetter = drive.name.replace('本地磁盘 (', '').replace(')', '')
  onTreeSelect(driveLetter)
  store.recordAction('click', `drive:${driveLetter}`)
}
function onTreeSelect(path) {
  const normalizedPath = path.replace(/\\/g, '/')
  currentPath.value = normalizedPath
  selectedItem.value = null
  searchQuery.value = ''
  store.setSelectedFile(null)
  store.recordAction('click', `tree:${path}`)
}
function navUp() {
  const parts = currentPath.value.replace(/\\/g, '/').split('/')
  if (parts.length > 1) { parts.pop(); currentPath.value = parts.join('/') }
  store.recordAction('click', 'navUp')
}
function refresh() { searchQuery.value = ''; store.recordAction('click', 'refresh') }
function doSearch() { store.recordAction('input', 'search', { value: searchQuery.value }) }
function onItemClick(item) {
  selectedItem.value = item
  const itemPath = currentPath.value + '/' + item.name
  store.setSelectedFile(itemPath)
  store.recordAction('click', `file:${item.name}`)
}
function onItemDblClick(item) {
  if (item.type === 'folder' || item.type === 'drive') {
    const newPath = currentPath.value + '/' + item.name
    currentPath.value = newPath
    selectedItem.value = null
  }
  store.recordAction('dblClick', `file:${item.name}`)
}
function onListContextMenu(e) {
  store.recordAction('rightClick', 'panel:empty')
  store.setSelectedFile(null)
  ctxMenu.visible = true; ctxMenu.x = e.clientX; ctxMenu.y = e.clientY
  ctxMenu.items = [
    { label: '查看', key: 'view' },
    { label: '排序方式', key: 'sortBy' },
    { label: '', divider: true },
    { label: '新建', children: true, key: 'new' },
    { label: '', divider: true },
    { label: '粘贴', key: 'paste', disabled: !store.clipboard }
  ]
}
function onItemContextMenu(item, e) {
  store.recordAction('rightClick', `file:${item.name}`)
  const itemPath = currentPath.value + '/' + item.name
  store.setSelectedFile(itemPath)
  ctxMenu.visible = true; ctxMenu.x = e.clientX; ctxMenu.y = e.clientY
  ctxMenu.items = [
    { label: '打开', key: 'open' },
    { label: '', divider: true },
    { label: '复制', key: 'copy' },
    { label: '剪切', key: 'cut' },
    { label: '重命名', key: 'rename' },
    { label: '删除', key: 'delete' },
    { label: '', divider: true },
    { label: '发送到', children: true, key: 'sendTo' },
    { label: '属性', key: 'properties' }
  ]
}

function onCtxAction(key) {
  store.recordAction('menuSelect', key)
  const item = selectedItem.value
  const itemPath = item ? (currentPath.value + '/' + item.name) : null

  switch (key) {
    case 'open': if (item?.type === 'folder') { currentPath.value = itemPath; selectedItem.value = null } break
    case 'copy': if (itemPath) store.copyToClipboard([itemPath]); break
    case 'cut': if (itemPath) store.cutToClipboard([itemPath]); break
    case 'paste': store.pasteFromClipboard(currentPath.value); break
    case 'delete': if (itemPath) { store.setSelectedFile(itemPath); store.deleteSelectedFile(); selectedItem.value = null } break
    case 'rename': store.recordAction('keyDown', 'F2'); break
    case 'properties':
      selectedItem.value = item  // keep selected for properties
      store.recordAction('click', 'checkbox:readonly')
      store.recordAction('click', 'confirm')
      break
    case 'new':
      // 新建文件夹后刷新
      setTimeout(() => { selectedItem.value = null }, 100)
      break
    case 'sendTo':
      store.recordAction('menuSelect', 'sendToDesktop')
      break
    case 'view':
      store.recordAction('menuSelect', 'viewDetails')
      break
    case 'sortBy':
      store.recordAction('menuSelect', 'sortByName')
      break
    case 'addToLibrary':
      store.recordAction('menuSelect', 'addToLibrary')
      break
  }
  ctxMenu.visible = false
}
function onItemDelete(item) {
  const itemPath = currentPath.value + '/' + item.name
  store.setSelectedFile(itemPath)
  store.deleteSelectedFile()
  selectedItem.value = null
}
function onItemCopy(item) {
  const itemPath = currentPath.value + '/' + item.name
  store.copyToClipboard([itemPath])
}
function onItemCut(item) {
  const itemPath = currentPath.value + '/' + item.name
  store.cutToClipboard([itemPath])
}
function onItemPaste() {
  store.pasteFromClipboard(currentPath.value)
  store.recordAction('keyDown', 'Ctrl+V')
}

// ── 键盘快捷键（仅当该窗口是最顶层时执行 VFS 操作，事件记录由全局 handler 负责）──
function isTopmostExplorer() {
  const wins = [...store.openWindows]
  if (!wins.length) return true // fallback: first explorer loaded
  const top = wins.reduce((a, b) => (b.zIndex > a.zIndex ? b : a), wins[0])
  return top?.app === 'explorer'
}

function onExplorerKeyDown(e) {
  if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return
  if (!isTopmostExplorer()) return

  if (e.key === 'Delete' && !e.shiftKey) {
    e.preventDefault()
    if (store.getSelectedFile()) {
      store.deleteSelectedFile()
      selectedItem.value = null
    }
    return
  }
  if (e.shiftKey && e.key === 'Delete') {
    e.preventDefault()
    if (store.getSelectedFile()) {
      store.permanentDeleteSelectedFile()
      selectedItem.value = null
    }
    return
  }
  if (e.ctrlKey && e.key === 'c') {
    e.preventDefault()
    const path = store.getSelectedFile()
    if (path) store.copyToClipboard([path])
    return
  }
  if (e.ctrlKey && e.key === 'v') {
    e.preventDefault()
    store.pasteFromClipboard(currentPath.value)
    return
  }
  // F2: record action only; actual rename triggered by global handler
}

onMounted(() => {
  document.addEventListener('keydown', onExplorerKeyDown)
})
onBeforeUnmount(() => {
  document.removeEventListener('keydown', onExplorerKeyDown)
})
</script>
<style scoped>
.explorer { display: flex; flex-direction: column; height: 100%; }
.explorer-toolbar { display: flex; align-items: center; gap: 6px; padding: 4px 8px; background: #f0f4f8; border-bottom: 1px solid #e0e0e0; }
.explorer-toolbar button { background: #fff; border: 1px solid #ccc; border-radius: 3px; padding: 2px 8px; cursor: pointer; font-size: var(--fs-xs); }
.address-bar { flex: 1; background: #fff; border: 1px solid #c0c0c0; border-radius: 2px; padding: 2px 6px; font-size: var(--fs-xs); color: #333; }
.search-box { width: 150px; border: 1px solid #c0c0c0; border-radius: 2px; padding: 2px 6px; font-size: var(--fs-xs); }
.explorer-body { display: flex; flex: 1; overflow: hidden; }
.tree-panel { width: 200px; overflow-y: auto; border-right: 1px solid #e0e0e0; padding: 4px 0; font-size: var(--fs-xs); background: #fafbfc; }
.tree-node { padding: 3px 8px; cursor: pointer; white-space: nowrap; display: flex; align-items: center; gap: 4px; }
.tree-node:hover { background: #e8f0fe; }
.tree-node.selected { background: #cce5ff; }
.tree-child { padding-left: 22px; }
.tree-arrow { width: 14px; font-size: 9px; color: #888; flex-shrink: 0; }
.list-panel { flex: 1; overflow-y: auto; }
.list-header { display: flex; padding: 4px 8px; font-size: 10px; color: #888; border-bottom: 1px solid #eee; background: var(--bg-hover); position: sticky; top: 0; }
.list-row { display: flex; padding: 2px 8px; font-size: var(--fs-xs); cursor: pointer; border-bottom: 1px solid #f5f5f5; }
.list-row:hover { background: #e8f0fe; }
.list-row.selected { background: #cce5ff; }
.col-name { flex: 2; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.col-date { flex: 1; color: #999; }
.col-type { flex: 1; color: #666; }
.col-size { flex: 0.5; color: #999; text-align: right; }
.list-empty { padding: 20px; text-align: center; color: #999; font-size: var(--fs-xs); }
.statusbar { background: #f0f4f8; border-top: 1px solid #e0e0e0; padding: 2px 8px; font-size: 10px; color: #888; }
</style>

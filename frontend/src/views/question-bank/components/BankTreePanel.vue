<template>
  <div class="bank-tree">
    <el-radio-group :model-value="mode" size="small" class="bank-tree__mode"
      @update:model-value="$emit('update:mode', $event)">
      <el-radio-button value="chapter">章节</el-radio-button>
      <el-radio-button value="kp">知识点</el-radio-button>
    </el-radio-group>
    <el-tree
      ref="treeRef"
      :data="displayTree"
      :props="{ label: 'name', children: 'children' }"
      node-key="id"
      highlight-current
      :expand-on-click-node="false"
      :default-expanded-keys="expandedKeys"
      :current-node-key="selectedNodeId"
      @node-click="onNodeClick"
    />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  tree: { type: Array, default: () => [] },
  mode: { type: String, default: 'chapter' },
  selectedNodeId: { type: [Number, String], default: null },
  expandedKeys: { type: Array, default: () => [] },
})
const emit = defineEmits(['update:mode', 'select'])
const treeRef = ref(null)

const prune = (nodes) => nodes
  .filter(n => (n.level || 1) < 4)
  .map(n => ({ ...n, children: n.children ? prune(n.children) : [] }))

const displayTree = ref([])

/** 只保留 level=1 的学科节点作为树根，过滤掉游离的 level>1 孤儿节点 */
const filterRoots = (nodes) => {
  if (!nodes || !nodes.length) return []
  return nodes.filter(n => {
    // 保留 level=1（学科根节点）；对于无法确定层级的，检查是否有 children 且 parentId 为空（视为根）
    if (n.level === 1) return true
    if (n.level == null && !n.parentId) return true
    return false
  })
}

const findInTree = (nodes, id, key = 'id') => {
  for (const n of nodes) {
    if (n[key] === id) return n
    if (n.children) { const f = findInTree(n.children, id, key); if (f) return f }
  }
  return null
}

watch([() => props.tree, () => props.mode], ([tree, mode]) => {
  // 先只保留学科根节点，再按模式裁剪层级
  const rooted = filterRoots(tree)
  displayTree.value = mode === 'chapter' ? prune(rooted) : rooted
  if (mode === 'chapter' && props.selectedNodeId) {
    const exists = findInTree(displayTree.value, props.selectedNodeId, 'id')
    if (!exists) emit('select', null)
  }
}, { immediate: true, deep: true })

const onNodeClick = (node) => emit('select', node)

const locate = (nodeId) => {
  treeRef.value?.setCurrentKey(nodeId)
  emit('select', { id: nodeId })
}
defineExpose({ locate })
</script>

<style scoped>
.bank-tree__mode { margin-bottom: 8px; }
</style>

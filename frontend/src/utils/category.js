/** 在树结构中查找指定节点的直接子节点列表 */
export function findChildren(nodes, id, childrenKey = 'children') {
  if (!nodes || !id) return []
  for (const n of nodes) {
    if (n.id === id) return n[childrenKey] || []
    const kids = n[childrenKey]
    if (kids && kids.length) {
      const f = findChildren(kids, id, childrenKey)
      if (f.length) return f
    }
  }
  return []
}

/**
 * 预计算分类树路径 Map（id → "学科 > 章节 > 任务 > 知识点"）
 * 替代5处 O(n⁴) 嵌套循环，一次遍历生成全部路径
 */
export function buildPathMap(tree) {
  const map = new Map()
  if (!tree) return map
  function walk(nodes, path) {
    for (const n of nodes) {
      const currentPath = path ? path + ' > ' + n.name : n.name
      map.set(n.id, currentPath)
      if (n.children?.length) walk(n.children, currentPath)
    }
  }
  walk(tree, '')
  return map
}

/** 根据路径 Map 获取节点的完整路径文本，未找到返回 '通用' */
export function getCategoryPath(catId, pathMap) {
  if (!catId || !pathMap) return '通用'
  return pathMap.get(catId) || '通用'
}

/** 递归展平分类树为带缩进的选项列表（用于 el-select） */
export function flattenCategoryTree(tree) {
  const result = []
  if (!tree) return result
  const prefixes = ['', '　└ ', '　　　└ ', '　　　　　└ ']
  function walk(nodes, depth) {
    for (const n of nodes) {
      result.push({ id: n.id, name: (prefixes[depth] || '') + n.name })
      if (n.children?.length) walk(n.children, depth + 1)
    }
  }
  walk(tree, 0)
  return result
}

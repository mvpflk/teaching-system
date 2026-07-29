import { ref } from 'vue'
import { getNodeTree, getNodeTreeVersion } from '@/api/knowledgeNode'
import { getMySubjects } from '@/api/settings'

const CACHE_KEY_VERSION = 'ktree_version'
const CACHE_KEY_DATA = 'ktree_data'
const CACHE_KEY_TIME = 'ktree_time'
const CACHE_TTL_MS = 3600_000

const treeData = ref([])
const subjects = ref([])
const loaded = ref(false)

export function useKnowledgeTree() {
  const loadData = async () => {
    try {
      const [sRes, vRes] = await Promise.all([getMySubjects(), getNodeTreeVersion().catch(() => ({ code: 0 }))])

      let tree = null
      const cachedVersion = localStorage.getItem(CACHE_KEY_VERSION)
      const serverVersion = vRes?.code === 200 ? String(vRes.data) : null
      const cacheTime = parseInt(localStorage.getItem(CACHE_KEY_TIME) || '0')
      const cacheExpired = Date.now() - cacheTime > CACHE_TTL_MS
      if (serverVersion && cachedVersion === serverVersion && !cacheExpired) {
        try {
          tree = JSON.parse(localStorage.getItem(CACHE_KEY_DATA) || 'null')
        } catch { /* */ }
      }
      if (!tree) {
        const tRes = await getNodeTree()
        if (tRes.code === 200) {
          tree = tRes.data || []
          if (serverVersion) {
            localStorage.setItem(CACHE_KEY_VERSION, serverVersion)
            localStorage.setItem(CACHE_KEY_DATA, JSON.stringify(tree))
            localStorage.setItem(CACHE_KEY_TIME, String(Date.now()))
          }
        }
      }

      if (sRes.code === 200) subjects.value = sRes.data || []
      if (tree) {
        treeData.value = tree
        if (subjects.value.length === 0) {
          const seen = new Map()
          for (const n of tree) {
            if (n.level === 1 && n.subjectId && !seen.has(n.subjectId)) {
              seen.set(n.subjectId, true)
              subjects.value.push({ id: n.subjectId, subjectName: n.name })
            }
          }
        }
        const markContent = (nodes) => {
          for (const n of nodes) {
            n.hasContent = !!n.content
            if (n.children) markContent(n.children)
          }
        }
        markContent(tree)
      }
      loaded.value = true
    } catch { /* */ }
  }

  return { treeData, subjects, loaded, loadData }
}

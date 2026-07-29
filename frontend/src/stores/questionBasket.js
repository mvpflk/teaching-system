import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { getQuestionsByIds } from '@/api/questionBank'

export const useQuestionBasketStore = defineStore('questionBasket', () => {
  const ids = ref([])
  const hydrated = ref({})
  const hydrateError = ref(false)
  let userId = null
  let loaded = false

  const storageKey = () => `qb_basket:${userId}`
  const persist = () => { if (userId != null) localStorage.setItem(storageKey(), JSON.stringify(ids.value)) }

  const init = (uid) => {
    if (uid == null) return
    userId = uid
    ids.value = []
    hydrated.value = {}
    try {
      const raw = localStorage.getItem(storageKey())
      if (raw) ids.value = JSON.parse(raw).filter(n => Number.isFinite(n))
    } catch { ids.value = [] }
    loaded = true
  }

  const initialized = computed(() => loaded && userId != null)

  const has = (id) => ids.value.includes(id)
  const add = (id) => { if (!has(id)) { ids.value.push(id); persist() } }
  const remove = (id) => { ids.value = ids.value.filter(x => x !== id); persist() }
  const toggle = (id) => { has(id) ? remove(id) : add(id) }
  const clear = () => { ids.value = []; hydrated.value = {}; hydrateError.value = false; persist() }

  const count = computed(() => ids.value.length)
  const byType = computed(() => {
    const g = {}
    for (const id of ids.value) {
      const t = hydrated.value[id]?.questionType || 'UNKNOWN'
      g[t] = g[t] || []
      g[t].push(hydrated.value[id] || { id })
    }
    return g
  })
  const difficultyDist = computed(() => {
    const dist = { 1: 0, 2: 0, 3: 0 }
    for (const id of ids.value) {
      const lv = hydrated.value[id]?.difficultyLevel
      if (lv >= 1 && lv <= 3) dist[lv]++
    }
    return dist
  })

  const hydrate = async () => {
    const missing = ids.value.filter(id => !hydrated.value[id])
    if (!missing.length) { hydrateError.value = false; return }
    try {
      const res = await getQuestionsByIds(missing.slice(0, 200))
      if (res.code === 200) {
        for (const q of res.data || []) hydrated.value[q.id] = q
        const alive = new Set((res.data || []).map(q => q.id))
        const dead = missing.filter(id => !alive.has(id))
        if (dead.length) { ids.value = ids.value.filter(id => !dead.includes(id)); persist() }
        hydrateError.value = false
      } else {
        hydrateError.value = true
      }
    } catch { hydrateError.value = true }
  }

  /** 保存为命名草稿 */
  const saveDraft = (name) => {
    if (!userId || !name) return false
    try {
      localStorage.setItem(`qb_draft:${userId}:${name}`, JSON.stringify({ ids: ids.value, savedAt: Date.now() }))
      return true
    } catch { return false }
  }

  /** 加载命名草稿（替换当前篮） */
  const loadDraft = (name) => {
    if (!userId || !name) return false
    try {
      const raw = localStorage.getItem(`qb_draft:${userId}:${name}`)
      if (!raw) return false
      const data = JSON.parse(raw)
      ids.value = data.ids || []
      hydrated.value = {}
      hydrateError.value = false
      persist()
      return true
    } catch { return false }
  }

  /** 删除命名草稿 */
  const deleteDraft = (name) => {
    if (!userId || !name) return
    localStorage.removeItem(`qb_draft:${userId}:${name}`)
  }

  /** 列出所有已保存草稿名（按保存时间倒序） */
  const listDrafts = () => {
    if (!userId) return []
    const drafts = []
    const prefix = `qb_draft:${userId}:`
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i)
      if (key && key.startsWith(prefix)) {
        try {
          const data = JSON.parse(localStorage.getItem(key))
          drafts.push({ name: key.slice(prefix.length), count: data.ids?.length || 0, savedAt: data.savedAt || 0 })
        } catch { /* skip corrupt */ }
      }
    }
    return drafts.sort((a, b) => b.savedAt - a.savedAt)
  }

  /** 拖拽调序 */
  const reorder = (fromIdx, toIdx) => {
    if (fromIdx === toIdx || fromIdx < 0 || toIdx < 0 || fromIdx >= ids.value.length || toIdx >= ids.value.length) return
    const item = ids.value.splice(fromIdx, 1)[0]
    ids.value.splice(toIdx, 0, item)
    persist()
  }

  return { ids, hydrated, hydrateError, initialized, count, byType, difficultyDist, init, has, add, remove, toggle, clear, hydrate, reorder, saveDraft, loadDraft, deleteDraft, listDrafts }
})

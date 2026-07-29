/**
 * useAutoSave — 基于 IndexedDB 的实时草稿保存/恢复 composable。
 * 约束: 不阻塞提交、异常静默 log、单条 ≤500KB。
 */
import { ref, watch, onMounted, onUnmounted } from 'vue'

const DB = 'teaching_drafts'
const VER = 1
const STORE = 'drafts'
const MAX = 500 * 1024

function openDB() {
  return new Promise((resolve, reject) => {
    const r = indexedDB.open(DB, VER)
    r.onupgradeneeded = () => { r.result.createObjectStore(STORE) }
    r.onsuccess = () => resolve(r.result)
    r.onerror = () => reject(r.error)
  })
}

const timers = {}
export function autoSave(key, data) {
  if (!key) return
  clearTimeout(timers[key])
  timers[key] = setTimeout(async () => {
    try {
      const json = JSON.stringify(JSON.parse(JSON.stringify(data)))
      if (json.length > MAX) { console.warn('[autoSave] >500KB skipped'); return }
      const db = await openDB()
      db.transaction(STORE, 'readwrite').objectStore(STORE).put({ key, data: json, ts: Date.now() }, key)
    } catch (e) { console.warn('[autoSave] fail:', e.message) }
  }, 3000)
}

export async function restoreDraft(key) {
  if (!key) return null
  try {
    const db = await openDB()
    return new Promise(resolve => {
      const r = db.transaction(STORE).objectStore(STORE).get(key)
      r.onsuccess = () => { const row = r.result; resolve(row ? JSON.parse(row.data) : null) }
      r.onerror = () => resolve(null)
    })
  } catch (e) { return null }
}

export async function clearDraft(key) {
  if (!key) return
  try {
    const db = await openDB()
    db.transaction(STORE, 'readwrite').objectStore(STORE).delete(key)
  } catch (e) { /* silent */ }
}

/** composable: 挂载时恢复草稿，监听变化自动保存 */
export function useAutoSave(keyRef, getData, watchSources) {
  const draft = ref(null)
  const isRestoring = ref(false)

  const doRestore = async () => {
    const k = typeof keyRef === 'function' ? keyRef() : keyRef?.value ?? keyRef
    if (!k) return
    isRestoring.value = true
    const d = await restoreDraft(k)
    if (d) draft.value = d
    isRestoring.value = false
  }

  onMounted(doRestore)

  if (watchSources?.length) {
    watch(watchSources, () => {
      const k = typeof keyRef === 'function' ? keyRef() : keyRef?.value ?? keyRef
      const data = getData?.()
      if (k && data) autoSave(k, data)
    }, { deep: true })
  }

  onUnmounted(() => {
    const k = typeof keyRef === 'function' ? keyRef() : keyRef?.value ?? keyRef
    if (k && timers[k]) { clearTimeout(timers[k]); delete timers[k] }
  })

  const clear = () => {
    const k = typeof keyRef === 'function' ? keyRef() : keyRef?.value ?? keyRef
    clearDraft(k)
  }

  return { draft, isRestoring, clearDraft: clear, restore: doRestore }
}

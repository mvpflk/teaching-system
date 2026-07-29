/**
 * 实训步骤暂存 — localStorage 草稿
 * key: practice_draft_{taskId}_step_{stepIndex}
 */
const PREFIX = 'practice_draft_'

function buildKey(taskId, stepIndex) {
  return `${PREFIX}${taskId}_step_${stepIndex}`
}

export function usePracticeDraft(taskId) {
  function saveDraft(stepIndex, data) {
    try {
      localStorage.setItem(buildKey(taskId, stepIndex), JSON.stringify(JSON.parse(JSON.stringify({
        ...data,
        savedAt: new Date().toISOString()
      }))))
    } catch { /* quota exceeded */ }
  }

  function loadDraft(stepIndex) {
    try {
      const raw = localStorage.getItem(buildKey(taskId, stepIndex))
      return raw ? JSON.parse(raw) : null
    } catch { return null }
  }

  function removeDraft(stepIndex) {
    localStorage.removeItem(buildKey(taskId, stepIndex))
  }

  function clearAllDrafts() {
    const keys = Object.keys(localStorage).filter(k => k.startsWith(`${PREFIX}${taskId}_`))
    keys.forEach(k => localStorage.removeItem(k))
  }

  return { saveDraft, loadDraft, removeDraft, clearAllDrafts }
}

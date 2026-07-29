import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'

const DEFAULT_VFS = {
  name: '计算机', type: 'root', children: [
    { name: '本地磁盘 (C:)', type: 'drive', children: [
      { name: 'Windows', type: 'folder', children: [{ name: 'System32', type: 'folder', children: [] }] },
      { name: 'Program Files', type: 'folder', children: [] },
      { name: 'Users', type: 'folder', children: [
        { name: 'Student', type: 'folder', children: [
          { name: '桌面', type: 'folder', children: [
            { name: '笔记.txt', type: 'file', ext: 'txt', size: 1234 },
            { name: '课程资料', type: 'folder', children: [] },
            { name: '临时文件.txt', type: 'file', ext: 'txt', size: 256 },
            { name: '截图.png', type: 'file', ext: 'png', size: 45000 },
            { name: '新建文件夹', type: 'folder', children: [] }
          ]},
          { name: '文档', type: 'folder', children: [
            { name: '作业要求.docx', type: 'file', ext: 'docx', size: 15000 }
          ]},
          { name: '下载', type: 'folder', children: [] }
        ]}
      ]}
    ]},
    { name: '本地磁盘 (D:)', type: 'drive', children: [
      { name: '备份', type: 'folder', children: [] }
    ]}
  ]
}

let _vfs = null

export const useWin7SimStore = defineStore('win7Sim', () => {
  if (!_vfs) _vfs = JSON.parse(JSON.stringify(DEFAULT_VFS))
  const fileSystem = reactive(_vfs)
  const currentPath = ref('C:/Users/Student/桌面')
  const clipboard = ref(null)

  // ── VFS 操作 ──
  function findNode(path) {
    const parts = path.replace(/\\/g, '/').replace(/^\/+/, '').split('/').filter(Boolean)
    let node = fileSystem
    for (const part of parts) {
      if (part === 'C:' || part === 'D:') {
        const drive = node.children?.find(c => c.name === `本地磁盘 (${part})`)
        if (!drive) return null
        node = drive
      } else {
        if (!node.children) return null
        const found = node.children.find(c => c.name === part)
        if (!found) return null
        node = found
      }
    }
    return node
  }

  function listDir(path) {
    const node = findNode(path)
    return node?.children || []
  }

  function createFolder(parentPath, name) {
    const parent = findNode(parentPath)
    if (!parent || parent.type === 'file') return false
    if (!parent.children) parent.children = []
    if (parent.children.some(c => c.name === name)) return false
    parent.children.push({ name, type: 'folder', children: [] })
    return true
  }

  function createFile(parentPath, name, ext = 'txt') {
    const parent = findNode(parentPath)
    if (!parent || parent.type === 'file') return false
    if (!parent.children) parent.children = []
    if (parent.children.some(c => c.name === name)) return false
    parent.children.push({ name, type: 'file', ext, size: 0 })
    return true
  }

  function deleteFile(path) {
    const parts = path.replace(/\\/g, '/').replace(/^\/+/, '').split('/').filter(Boolean)
    const name = parts.pop()
    const parent = findNode(parts.join('/'))
    if (!parent?.children) return false
    const idx = parent.children.findIndex(c => c.name === name)
    if (idx === -1) return false
    parent.children.splice(idx, 1)
    return true
  }

  function renameFile(path, newName) {
    const node = findNode(path)
    if (!node) return false
    node.name = newName
    return true
  }

  function copyFile(srcPath, dstPath) {
    const src = findNode(srcPath)
    const dst = findNode(dstPath)
    if (!src || !dst || dst.type === 'file') return false
    if (!dst.children) dst.children = []
    // 防环：不能复制到自己内部
    if (dstPath.startsWith(srcPath + '/')) return false
    dst.children.push(JSON.parse(JSON.stringify(src)))
    return true
  }

  function moveFile(srcPath, dstPath) {
    if (!copyFile(srcPath, dstPath)) return false
    return deleteFile(srcPath)
  }

  function cutToClipboard(paths) { clipboard.value = { files: paths, mode: 'cut' } }
  function copyToClipboard(paths) { clipboard.value = { files: paths, mode: 'copy' } }

  function pasteFromClipboard(dstPath) {
    if (!clipboard.value || !clipboard.value.files.length) return false
    const dst = findNode(dstPath) || { children: [] }
    const dstDir = dst.type === 'file' ? dstPath.substring(0, dstPath.lastIndexOf('/')) : dstPath
    for (const src of clipboard.value.files) {
      if (clipboard.value.mode === 'cut') moveFile(src, dstDir)
      else copyFile(src, dstDir)
    }
    if (clipboard.value.mode === 'cut') clipboard.value = null  // 剪切后清空
    return true
  }

  // ── 当前选中文件（供键盘操作使用）──
  const selectedFilePath = ref(null)

  function setSelectedFile(path) {
    selectedFilePath.value = path
  }

  function getSelectedFile() {
    return selectedFilePath.value
  }

  /** 删除选中文件（到回收站） */
  function deleteSelectedFile() {
    const path = selectedFilePath.value
    if (!path) return false
    const node = findNode(path)
    if (!node) return false
    deleteToRecycle(path)
    selectedFilePath.value = null
    recordAction('click', 'confirmDelete')
    return true
  }

  /** 永久删除选中文件 */
  function permanentDeleteSelectedFile() {
    const path = selectedFilePath.value
    if (!path) return false
    if (!deleteFile(path)) return false
    selectedFilePath.value = null
    recordAction('click', 'confirmDelete')
    return true
  }

  /** 获取选中文件所在目录 */
  function getSelectedFileDir() {
    const path = selectedFilePath.value
    if (!path) return null
    const lastSlash = path.lastIndexOf('/')
    return lastSlash > 0 ? path.substring(0, lastSlash) : path
  }

  // ── 回收站 ──
  const recycleBin = ref([])

  function deleteToRecycle(path) {
    const node = findNode(path)
    if (!node) return false
    recycleBin.value.push({ path, node: JSON.parse(JSON.stringify(node)), deletedAt: Date.now() })
    return deleteFile(path)
  }

  function restoreFromRecycle(index) {
    const item = recycleBin.value[index]
    if (!item) return false
    const parts = item.path.replace(/\\/g, '/').replace(/^\/+/, '').split('/').filter(Boolean)
    const name = parts.pop()
    const parent = findNode(parts.join('/'))
    if (!parent) return false
    if (!parent.children) parent.children = []
    parent.children.push(item.node)
    recycleBin.value.splice(index, 1)
    return true
  }

  function emptyRecycleBin() { recycleBin.value = [] }

  // ── 桌面 ──
  const desktopIcons = ref([
    { id: 'computer', name: '计算机', icon: '🖥️' },
    { id: 'documents', name: '我的文档', icon: '📁' }
  ])
  const wallpaper = ref('default')

  // ── 窗口管理 ──
  const openWindows = ref([])
  let windowIdCounter = 0

  function openWindow(app, title, options = {}) {
    windowIdCounter++
    const win = reactive({
      id: windowIdCounter, app, title,
      x: options.x || 80 + openWindows.value.length * 30,
      y: options.y || 60 + openWindows.value.length * 30,
      w: options.w || 700, h: options.h || 480,
      minimized: false, maximized: false,
      zIndex: openWindows.value.length + 1
    })
    openWindows.value.push(win)
    return win.id
  }

  function closeWindow(id) { openWindows.value = openWindows.value.filter(w => w.id !== id) }
  function focusWindow(id) {
    const maxZ = Math.max(...openWindows.value.map(w => w.zIndex), 0)
    const win = openWindows.value.find(w => w.id === id)
    if (win) win.zIndex = maxZ + 1
  }
  function toggleMinimize(id) {
    const win = openWindows.value.find(w => w.id === id)
    if (win) win.minimized = !win.minimized
  }
  function toggleMaximize(id) {
    const win = openWindows.value.find(w => w.id === id)
    if (win) win.maximized = !win.maximized
  }

  // ── 操作日志（增量上报） ──
  const actionLog = ref([])
  let _recording = false, _startTime = 0, _lastUploadIdx = 0

  function startRecording() { _recording = true; _startTime = Date.now(); actionLog.value = []; _lastUploadIdx = 0 }
  function stopRecording() { _recording = false }
  function recordAction(type, target, detail = {}) {
    if (!_recording) return
    actionLog.value.push({ seq: actionLog.value.length + 1, ts: Date.now() - _startTime, type, target, detail })
  }
  /** 获取自上次上报以来的增量事件 */
  function getIncrementalEvents() {
    const events = actionLog.value.slice(_lastUploadIdx)
    _lastUploadIdx = actionLog.value.length
    return events
  }

  function autoCheckStep() {
    if (!currentTask.value) return
    const nextIdx = taskSteps.value.findIndex(s => !s.completed)
    if (nextIdx < 0) return
    const step = taskSteps.value[nextIdx]
    if (validateStep(step)) {
      step.completed = true
      showFeedback('success', '✅ ' + step.name + ' — 完成！')
    } else {
      // 调试：显示最近操作和期望的操作
      const lastAction = actionLog.value.length > 0 ? actionLog.value[actionLog.value.length - 1] : null
      const lastType = lastAction ? (lastAction.type + ':' + (lastAction.target || '')) : '(无操作)'
      const expectType = (step.validate?.event || '?') + ':' + (step.validate?.target || '')
      // 只有当用户有操作且不匹配时才提示
      // (静默失败，但记录到控制台 — 开发时可打开 F12 查看)
      if (typeof console !== 'undefined') {
        console.debug('[Win7Sim] 步骤' + (nextIdx+1) + '未通过 | 最近操作:', lastType, '| 期望:', expectType, '| 操作日志:', actionLog.value.slice(-3).map(a => a.type+':'+(a.target||'')).join(', '))
      }
    }
  }

  // ── 网络配置（网络实训专用）──
  const networkConfig = reactive({
    localIP: '192.168.1.100',
    subnetMask: '255.255.255.0',
    gateway: '192.168.1.1',
    dns: '8.8.8.8',
    dhcp: true,
    mac: '00-1A-2B-3C-4D-5E',
    remoteHosts: {}
  })

  // ── 任务状态 ──
  const currentTask = ref(null)
  const taskSteps = ref([])
  const usedHints = ref([])
  const taskTimer = ref(0)
  const scoreFeedback = ref(null) // { type: 'success'|'error', message: string }

  function loadTask(taskJson) {
    currentTask.value = taskJson
    taskSteps.value = (taskJson.steps || []).map(s => ({ ...s, completed: false, score: 0 }))
    usedHints.value = []
    scoreFeedback.value = null
    // 从任务JSON加载网络配置（网络实训专用）
    if (taskJson.networkConfig) {
      Object.assign(networkConfig, taskJson.networkConfig)
    }
    startRecording() // 练习模式自动开始记录操作
  }

  function showFeedback(type, message) {
    scoreFeedback.value = { type, message }
    setTimeout(() => { scoreFeedback.value = null }, 2000)
  }

  function validateStep(step) {
    if (!step.validate) return true
    const v = step.validate
    if (v.event) {
      // 检查最近 5 条操作记录（而非仅最后一条），容忍多余操作
      const recent = actionLog.value.slice(-5)
      const matched = recent.some(a => {
        if (!a) return false
        const typeMatch = a.type === v.event
          || (v.event === 'click' && (a.type === 'dblClick' || a.type === 'launch'))
          || (v.event === 'dblClick' && a.type === 'click')
          // rightClick 与 menuSelect 互认（因为很多操作这两个动作等价）
          || (v.event === 'rightClick' && a.type === 'menuSelect')
          || (v.event === 'menuSelect' && a.type === 'rightClick')
          // input 步骤允许 click+input 组合
          || (v.event === 'input' && a.type === 'click')
        if (!typeMatch) return false
        if (v.target) {
          // 双向包含匹配
          if (a.target.includes(v.target) || v.target.includes(a.target)) return true
          // 模糊匹配：file:xxx 与 xxx 互认
          const cleanV = v.target.replace(/^(file|desktop_icon|tree|drive|cp|menu):/, '')
          const cleanA = a.target.replace(/^(file|desktop_icon|tree|drive|cp|menu):/, '')
          if (cleanV && cleanA && (cleanA.includes(cleanV) || cleanV.includes(cleanA))) return true
          // input:search 与 search:*.txt 互认
          if (a.type === 'input' && v.event === 'input') {
            if (cleanA === 'search' || cleanA === 'address') return true
            if (cleanA === 'notepad' && cleanV === 'notepad') return true
            if (a.detail && a.detail.value && v.target.includes(a.detail.value)) return true
          }
          return false
        }
        return true
      })
      if (!matched) return false
    }
    if (v.vfs) {
      const node = findNode(v.vfs.path)
      if (!node) return false
      if (v.vfs.type && node.type !== v.vfs.type) return false
    }
    return true
  }

  // ── 编辑距离评分（考试模式）──
  function calcLevenshteinScore(studentLog, expectedSeq, config = {}) {
    const deductionExtra = config.deductionPerExtra || 10
    const deductionMiss = config.deductionPerMiss || 15
    const studentsOps = studentLog.map(a => `${a.type}:${a.target || ''}`)
    const expectedOps = expectedSeq.map(s => {
      if (typeof s.validate === 'string') return s.validate
      return `${s.validate?.event || ''}:${s.validate?.target || ''}`
    })
    // 简化编辑距离：统计匹配/多余/缺失操作数
    let matched = 0, extra = 0, missing = 0
    const used = new Set()
    for (const exp of expectedOps) {
      const idx = studentsOps.findIndex((s, i) => !used.has(i) && s === exp)
      if (idx >= 0) { used.add(idx); matched++ } else missing++
    }
    extra = studentsOps.length - matched
    const totalOps = expectedOps.length
    const rawScore = Math.max(0, 100 - extra * deductionExtra - missing * deductionMiss)
    return Math.round(rawScore * 100) / 100
  }

  // ── 考试模式 ──
  const examMode = ref(false)
  const examSubmitted = ref(false)
  const _uploadTimer = null

  function startExam(taskJson, submissionId) {
    resetAll()
    examMode.value = true
    examSubmitted.value = false
    currentTask.value = taskJson
    taskSteps.value = (taskJson.steps || []).map(s => ({ ...s, completed: false, score: 0 }))
    _submissionId = submissionId
    startRecording()
  }

  function submitExam() {
    stopRecording()
    examSubmitted.value = true
    const log = [...actionLog.value]
    const expected = currentTask.value?.steps || []
    const score = calcLevenshteinScore(log, expected)
    return { events: log, eventCount: log.length, autoScore: score, success: score >= 60 }
  }

  let _submissionId = null
  function getSubmissionId() { return _submissionId }
  function setSubmissionId(id) { _submissionId = id }

  function resetAll() {
    Object.assign(fileSystem, JSON.parse(JSON.stringify(DEFAULT_VFS)))
    openWindows.value.length = 0
    actionLog.value = []
    currentTask.value = null
    taskSteps.value = []
    recycleBin.value = []
    clipboard.value = null
    scoreFeedback.value = null
    _recording = false
    Object.assign(networkConfig, {
      localIP: '192.168.1.100',
      subnetMask: '255.255.255.0',
      gateway: '192.168.1.1',
      dns: '8.8.8.8',
      dhcp: true,
      mac: '00-1A-2B-3C-4D-5E',
      remoteHosts: {}
    })
  }

  return {
    fileSystem, currentPath, clipboard,
    selectedFilePath, setSelectedFile, getSelectedFile,
    deleteSelectedFile, permanentDeleteSelectedFile, getSelectedFileDir,
    findNode, listDir, createFolder, createFile, deleteFile, renameFile, copyFile, moveFile,
    cutToClipboard, copyToClipboard, pasteFromClipboard,
    recycleBin, deleteToRecycle, restoreFromRecycle, emptyRecycleBin,
    desktopIcons, wallpaper,
    openWindows, openWindow, closeWindow, focusWindow, toggleMinimize, toggleMaximize,
    actionLog, startRecording, stopRecording, recordAction,
    networkConfig,
    currentTask, taskSteps, usedHints, taskTimer, scoreFeedback,
    loadTask, showFeedback, validateStep, resetAll,
    calcLevenshteinScore, examMode, examSubmitted, startExam, submitExam, getSubmissionId, setSubmissionId,
    getIncrementalEvents
  }
})

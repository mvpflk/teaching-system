import { ref, toRef } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { listSessions, getSessionMessages, submitFeedback as apiSubmitFeedback, confirmWrite as apiConfirmWrite } from '@/api/agent'

function safeParse(str) {
  try { return JSON.parse(str) } catch { return str }
}

// 还原后端编码的换行符（U+E000 → \n），SSE 传输会吞掉原始 \n
function restoreNewlines(data) {
  if (typeof data === 'string') return data.replace(//g, '\n')
  return data
}

/**
 * 创建标准化的工具调用回调（toolStart / toolEnd）
 * 供 AgentPage 和 AgentAssistant 共享，消除 handleSend 中的重复回调逻辑
 *
 * @param {import('vue').Ref<Array>} toolsVisible 工具列表 ref
 * @param {Function} [onThinking] 可选：回调工具状态文本（用于 AgentAssistant 的 thinkingText）
 */
/** 工具名 → 友好中文描述（后端 toolProgressDescription 的补充） */
const TOOL_LABEL_MAP = {
  teaching_knowledge_search: '正在搜索知识库…',
  teaching_syllabus_lookup: '正在查询考纲…',
  teaching_similar_questions: '正在从题库匹配习题…',
  teaching_search_tasks: '正在搜索任务…',
  teaching_create_task: '正在创建任务…',
  teaching_send_notification: '正在发送通知…',
  teaching_student_wrong_book: '正在查询错题本…',
  teaching_student_mastery: '正在获取掌握度…',
  teaching_student_submissions: '正在查看提交记录…',
  teaching_question_explain: '正在查看题目解析…',
  teaching_class_analytics: '正在分析班级成绩…',
  teaching_knowledge_trend: '正在查看知识点趋势…',
  teaching_student_growth: '正在查看学生成长…',
  teaching_my_classes: '正在获取班级信息…',
  teaching_class_students: '正在获取学生名单…',
  teaching_generate_ppt: '正在生成PPT课件…',
  teaching_task_submission_status: '正在查询提交情况…',
  teaching_expand_node: '正在展开知识节点…',
  teaching_aggregate_questions: '正在聚合组卷…',
}

function resolveToolLabel(toolInfo) {
  // 1. 优先用后端下发的 progress 字段（中文描述）
  if (toolInfo?.progress) return toolInfo.progress
  const name = toolInfo?.tool || toolInfo?.name || ''
  // 2. 用前端映射表
  if (TOOL_LABEL_MAP[name]) return TOOL_LABEL_MAP[name]
  // 3. 兜底：去掉 teaching_ 前缀
  return name.replace(/^teaching_/, '') + '…'
}

/** 工具完成态描述 */
function resolveDoneLabel(toolInfo) {
  if (toolInfo?.progress) {
    // "正在搜索知识库…" → "已搜索知识库"
    return toolInfo.progress.replace(/^正在/, '已').replace(/…$/, '')
  }
  const name = toolInfo?.tool || toolInfo?.name || ''
  if (TOOL_LABEL_MAP[name]) {
    return TOOL_LABEL_MAP[name].replace(/^正在/, '已').replace(/…$/, '')
  }
  return name.replace(/^teaching_/, '') + '完成'
}

export function createToolCallbacks(toolsVisible, onThinking) {
  return {
    onToolStart(toolInfo) {
      const name = toolInfo?.tool || toolInfo?.name || ''
      const label = resolveToolLabel(toolInfo)
      if (onThinking) onThinking(label)
      toolsVisible.value.push({ name, status: 'running', label })
    },
    onToolEnd(toolInfo) {
      const name = toolInfo?.tool || toolInfo?.name || ''
      const doneLabel = resolveDoneLabel(toolInfo)
      if (onThinking) onThinking(doneLabel)
      const idx = toolsVisible.value.findIndex((t) => t.name === name)
      if (idx >= 0) toolsVisible.value[idx] = { ...toolsVisible.value[idx], status: 'done' }
    },
  }
}

export function useAgentChat(agentTypeRef) {
  const userStore = useUserStore()
  const agentType = toRef(agentTypeRef)
  const sessions = ref([])
  const currentSessionId = ref(null)
  const messages = ref([])
  const isStreaming = ref(false)
  const error = ref(null)
  const showError = ref(null)
  const pendingConfirm = ref(null)
  let abortController = null

  /** 从后端加载指定会话的历史消息 */
  async function loadSessionMessages(sessionId) {
    if (!sessionId) return
    try {
      const res = await getSessionMessages(sessionId)
      if (res.code === 200 && Array.isArray(res.data)) {
        messages.value = res.data.map(m => ({ ...m, time: Date.now() }))
      }
    } catch (e) {
      console.error('[Agent] 加载会话消息失败:', e)
    }
  }

  async function loadSessions() {
    try {
      const res = await listSessions()
      if (res.code === 200) sessions.value = res.data || []
    } catch (e) {
      console.error('[Agent] 加载会话失败:', e)
    }
  }

  function resetMessages() {
    stopGeneration()
    messages.value = []
    currentSessionId.value = null
    error.value = null
    showError.value = null
    feedbackState.value = {}
  }

  function stopGeneration() {
    if (abortController) {
      abortController.abort()
      abortController = null
    }
    isStreaming.value = false
  }

  async function sendMessage(text, onToolStart, onToolEnd, onText, onDone, onError, onThinking) {
    if (!text || !text.trim()) return
    // 清除上一次可能残留的流式状态（防卡死）
    if (streamTimeoutId) { clearTimeout(streamTimeoutId); streamTimeoutId = null }
    // 创建新的 AbortController
    abortController = new AbortController()
    isStreaming.value = true
    const userMsg = { role: 'user', content: text, time: Date.now() }
    messages.value = [...messages.value, userMsg]
    error.value = null
    showError.value = null

    let resp
    try {
      const token = userStore.token
      console.log('[Agent] 发送消息:', { agentType, sessionId: currentSessionId.value, msgLen: text.length })
      resp = await fetch('/api/agent/chat', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': token ? `Bearer ${token}` : ''
        },
        body: JSON.stringify({
          agentType: agentType.value,
          sessionId: currentSessionId.value,
          message: text
        }),
        signal: abortController.signal
      })
      console.log('[Agent] 响应状态:', resp.status, resp.statusText)
    } catch (e) {
      if (e.name === 'AbortError') {
        console.log('[Agent] 用户手动停止生成')
        isStreaming.value = false
        onDone && onDone()
        return
      }
      console.error('[Agent] 网络请求失败:', e)
      const msg = '无法连接到服务器，请确认后端已启动 (http://localhost:8080)'
      showError.value = msg
      isStreaming.value = false
      onError && onError(msg)
      return
    }

    if (!resp.ok) {
      let errBody = ''
      try { errBody = await resp.text() } catch (_) {}
      console.error('[Agent] HTTP错误:', resp.status, errBody)
      const msg = resp.status === 401 ? '登录已过期，请重新登录'
        : resp.status === 403 ? '无权使用Agent功能'
        : `服务器错误 (${resp.status})`
      showError.value = msg
      isStreaming.value = false
      onError && onError(msg)
      return
    }

    try {
      const reader = resp.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      let lastEvent = null
      let chunkCount = 0
      // 启动流式超时保护
      resetStreamTimeout()
      // 流式文本累积 + 防抖：减少 re-render 次数
      let textBuf = ''
      let textTimer = null
      const flushText = () => {
        if (!textBuf) return
        const last = messages.value[messages.value.length - 1]
        if (last && last.role === 'assistant') {
          last.content += textBuf
          messages.value = [...messages.value]
        } else {
          messages.value = [...messages.value, { role: 'assistant', content: textBuf, time: Date.now() }]
        }
        onText && onText(textBuf)
        textBuf = ''
      }

      readLoop: while (true) {
        const { done, value } = await reader.read()
        if (done) break
        chunkCount++

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('event:')) {
            lastEvent = line.slice(6).trim()
            continue
          }
          if (!line.startsWith('data:')) continue
          const dataStr = line.slice(5).trim()
          if (!dataStr) continue

          const eventData = restoreNewlines(safeParse(dataStr))
          const event = lastEvent
          lastEvent = null

          if (event === 'done') {
            clearTimeout(textTimer); flushText()
            console.log('[Agent] 完成, 收到chunk数:', chunkCount)
            if (eventData && eventData.sessionId) currentSessionId.value = eventData.sessionId
            onDone && onDone()
            break readLoop // 立即退出流读取循环，不等待连接关闭
          } else if (event === 'thinking') {
            const t = typeof eventData === 'string' ? eventData : ''
            onThinking && onThinking(t)
          } else if (event === 'error') {
            clearTimeout(textTimer); flushText()
            const msg = typeof eventData === 'string' ? eventData
              : (eventData?.message || eventData?.data || '未知错误')
            showError.value = msg
            console.error('[Agent] 服务端错误:', msg)
            onError && onError(msg)
          } else if (event === 'api_key_missing') {
            clearTimeout(textTimer); flushText()
            const msg = typeof eventData === 'string' ? eventData : 'AI 服务未配置'
            showError.value = '⚠️ ' + msg
            console.error('[Agent] API Key 未配置')
            onError && onError(msg)
          } else if (event === 'api_key_invalid') {
            clearTimeout(textTimer); flushText()
            const msg = typeof eventData === 'string' ? eventData : 'API Key 无效或已过期'
            showError.value = '⚠️ ' + msg
            console.error('[Agent] API Key 无效')
            onError && onError(msg)
          } else if (event === 'api_limit') {
            clearTimeout(textTimer); flushText()
            const msg = typeof eventData === 'string' ? eventData : '今日调用次数已用完'
            showError.value = '⚠️ ' + msg
            console.warn('[Agent] 调用次数超限')
            onError && onError(msg)
          } else if (event === 'warning') {
            const msg = typeof eventData === 'string' ? eventData : ''
            console.warn('[Agent] 警告:', msg)
            onThinking && onThinking(msg)
          } else if (event === 'schema') {
            clearTimeout(textTimer); flushText()
            const content = typeof eventData === 'string' ? eventData : JSON.stringify(eventData)
            if (content) {
              // 替换上一条流式文本消息（避免 raw text + structured card 重复显示）
              const msgs = [...messages.value]
              const last = msgs[msgs.length - 1]
              if (last && last.role === 'assistant' && !last._structured) {
                msgs[msgs.length - 1] = { role: 'assistant', content, time: Date.now(), _structured: true }
              } else {
                msgs.push({ role: 'assistant', content, time: Date.now(), _structured: true })
              }
              messages.value = msgs
            }
          } else if (event === 'text') {
            const content = typeof eventData === 'string' ? eventData : (eventData?.data || eventData?.content || '')
            textBuf += content
            if (textTimer) clearTimeout(textTimer)
            textTimer = setTimeout(flushText, 80)  // 80ms 防抖
          } else if (event === 'tool_start') {
            clearTimeout(textTimer); flushText()
            const info = eventData?.data || eventData || {}
            console.log('[Agent] 工具开始:', info.tool || info.name)
            onToolStart && onToolStart(info)
          } else if (event === 'tool_end') {
            const info = eventData?.data || eventData || {}
            console.log('[Agent] 工具完成:', info.tool || info.name)
            onToolEnd && onToolEnd(info)
          } else if (event === 'confirm_write') {
            // G-4: 写操作确认 — 弹窗等待用户确认
            clearTimeout(textTimer); flushText()
            console.log('[Agent] 写操作确认:', eventData)
            const data = typeof eventData === 'object' && eventData ? eventData : { message: '即将执行写操作', tools: [] }
            pendingConfirm.value = data
            onThinking && onThinking('⏳ 等待确认…')
          } else if (event === 'answer_warning') {
            // 3.1: 答案泄露警告
            const msg = typeof eventData === 'string' ? eventData : (eventData?.message || '')
            console.warn('[Agent] 答案保护警告:', msg)
          } else if (event === 'crisis_warning') {
            // 3.6: 学生心理健康危机提醒
            const msg = typeof eventData === 'string' ? eventData : (eventData?.message || '')
            console.warn('[Agent] 心理健康危机提醒:', msg)
            showError.value = '💙 ' + msg
            setTimeout(() => { showError.value = null }, 10000)
          }
        }
      }
      console.log('[Agent] 流结束, 共', chunkCount, '个chunk')
    } catch (e) {
      console.error('[Agent] 流解析失败:', e)
      const msg = '响应解析失败: ' + (e.message || '')
      showError.value = msg
      onError && onError(msg)
    } finally {
      // 清除超时保护
      if (streamTimeoutId) { clearTimeout(streamTimeoutId); streamTimeoutId = null }
      // 5.8: 确保 textBuf 中残留的文本在流结束时一定被刷新
      clearTimeout(textTimer)
      if (textBuf) {
        const last = messages.value[messages.value.length - 1]
        if (last && last.role === 'assistant') {
          last.content += textBuf
          messages.value = [...messages.value]
        } else if (textBuf) {
          messages.value = [...messages.value, { role: 'assistant', content: textBuf, time: Date.now() }]
        }
        textBuf = ''
      }
      isStreaming.value = false
    }
  }

  // 反馈状态：记录每条消息的评分 { messageIndex: rating }
  const feedbackState = ref({})
  let streamTimeoutId = null

  // 流式超时保护：若 120 秒内未完成则强制释放 isStreaming，防止 UI 卡死
  function resetStreamTimeout() {
    if (streamTimeoutId) clearTimeout(streamTimeoutId)
    streamTimeoutId = setTimeout(() => {
      console.warn('[Agent] 流式响应超时(120s)，强制释放 isStreaming')
      isStreaming.value = false
      streamTimeoutId = null
    }, 120_000)
  }

  async function submitFeedback(messageIndex, rating, tags, comment, userQuestion, answerSnippet, toolsUsed) {
    // 防重复：已评价则跳过
    if (feedbackState.value[messageIndex] !== undefined) return

    try {
      await apiSubmitFeedback({
        sessionId: currentSessionId.value,
        messageIndex,
        rating,
        tags: tags || '',
        comment: comment || '',
        userQuestion: (userQuestion || '').substring(0, 500),
        answerSnippet: (answerSnippet || '').substring(0, 200),
        toolsUsed: toolsUsed || ''
      })
      feedbackState.value = { ...feedbackState.value, [messageIndex]: rating }
      ElMessage.success({ message: rating === 5 ? '感谢好评 🎉' : '已记录反馈', duration: 2000 })
      console.log('[Agent] 反馈已提交:', { messageIndex, rating, tags })
    } catch (e) {
      console.error('[Agent] 提交反馈失败:', e)
      // 失败时显示错误提示，不做 UI 状态变更
      showError.value = '反馈提交失败，请稍后重试'
      setTimeout(() => { showError.value = null }, 3000)
    }
  }

  async function confirmWrite(confirmed) {
    try {
      await apiConfirmWrite({ sessionId: currentSessionId.value, confirmed })
    } catch (e) {
      console.error('[Agent] 确认失败:', e)
    } finally {
      pendingConfirm.value = null
    }
  }

  return {
    sessions,
    currentSessionId,
    messages,
    isStreaming,
    error,
    showError,
    feedbackState,
    pendingConfirm,
    loadSessions,
    loadSessionMessages,
    resetMessages,
    sendMessage,
    stopGeneration,
    submitFeedback,
    confirmWrite
  }
}

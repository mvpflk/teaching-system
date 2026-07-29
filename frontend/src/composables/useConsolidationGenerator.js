import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { requestConsolidation } from '@/api/simulation'
import { createEventSource } from '@/utils/sseTicket'
import request from '@/utils/request'

/**
 * 巩固材料生成 — SSE 实时推送 + 轮询兜底
 * @param {Object} options
 * @param {number} options.taskId
 * @param {number[]} options.knowledgeNodeIds
 * @param {string} options.subject
 * @param {string} [options.commonMistakes]
 * @param {number} [options.timeout=305000] SSE 超时 ms
 * @returns {{ generating: import('vue').Ref<boolean>, generate: () => Promise<number|null> }}
 */
export function useConsolidationGenerator({ taskId, knowledgeNodeIds, subject, commonMistakes = '', timeout = 305_000 } = {}) {
  const generating = ref(false)

  async function generate(extra = {}) {
    generating.value = true
    try {
      const res = await requestConsolidation({
        taskId: extra.taskId ?? taskId,
        knowledgeNodeIds: extra.knowledgeNodeIds ?? knowledgeNodeIds,
        subject: extra.subject ?? subject,
        commonMistakes: extra.commonMistakes ?? commonMistakes
      })
      if (res.code !== 200) {
        ElMessage.error(res.message || '提交失败')
        return null
      }
      const asyncTaskId = res.data.asyncTaskId
      ElMessage.info('巩固材料生成中...')

      let outputId = null
      try {
        const es = await createEventSource(`/api/ai-output/stream/${asyncTaskId}`)
        await new Promise((resolve, reject) => {
          let settled = false
          const doResolve = (v) => { if (!settled) { settled = true; resolve(v) } }
          const doReject = (e) => { if (!settled) { settled = true; reject(e) } }
          const cleanup = () => { clearTimeout(t); es.close() }
          const t = setTimeout(() => { cleanup(); doReject(new Error('超时')) }, timeout)
          es.addEventListener('task-result', (e) => {
            if (settled) return
            clearTimeout(t)
            try {
              const d = JSON.parse(e.data)
              if (d.status === 'COMPLETED') { outputId = d.result?.outputId || d.result?.id; es.close(); doResolve() }
              else if (d.status === 'FAILED') { cleanup(); doReject(new Error(d.error || '生成失败')) }
            } catch (err) { cleanup(); doReject(err) }
          })
          es.onerror = () => { cleanup(); doReject(new Error('SSE连接中断')) }
        })
      } catch {
        const start = Date.now()
        let interval = 3000
        while (Date.now() - start < 300_000) {
          await new Promise(r => setTimeout(r, interval))
          interval = Math.min(interval * 1.5, 8000)
          try {
            const pollRes = await request.get(`/api/ai-output/result/${asyncTaskId}`)
            if (pollRes.code !== 200) continue
            const { status, result, error } = pollRes.data || {}
            if (status === 'COMPLETED') { outputId = result?.outputId || result?.id; break }
            if (status === 'FAILED' || status === 'TIMEOUT') throw new Error(error || 'AI生成超时')
          } catch (pollErr) {
            if (pollErr.message?.includes('AI生成超时') || pollErr.message?.includes('生成失败')) throw pollErr
          }
        }
      }

      if (outputId) {
        ElMessage.success('巩固材料已生成')
        return outputId
      } else {
        ElMessage.warning('生成超时（5分钟），AI 可能在后台继续处理，请在历史记录中查看结果')
        return null
      }
    } catch (e) {
      ElMessage.error('生成失败: ' + (e.message || '未知错误'))
      return null
    } finally {
      generating.value = false
    }
  }

  return { generating, generate }
}

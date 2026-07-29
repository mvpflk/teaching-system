import { ref, onUnmounted } from 'vue'
import { ElNotification } from 'element-plus'
import { createEventSourceWithReconnect } from '@/utils/sseTicket'
import { getCurrentCompetition, reportProgress } from '@/api/typing'

export function useTypingCompetition() {
  const ranking = ref([])
  const countdown = ref('')
  const compEndTime = ref(null)
  const competitionId = ref(null)
  const inCompetition = ref(false)

  let rankingSSE = null
  let sseClosed = false
  let announceSSE = null
  let compPollTimer = null
  let progressTimer = null
  let countdownTimer = null

  function startCountdown() {
    clearInterval(countdownTimer)
    countdownTimer = setInterval(updateCountdown, 1000)
  }

  function updateCountdown() {
    if (!compEndTime.value) return
    const diff = new Date(compEndTime.value).getTime() - Date.now()
    if (diff <= 0) { countdown.value = '已结束'; return }
    const m = Math.floor(diff / 60000)
    const s = Math.floor((diff % 60000) / 1000)
    countdown.value = `${m}:${String(s).padStart(2, '0')}`
  }

  async function connectRankingSSE() {
    if (!competitionId.value) return
    const token = localStorage.getItem('token')
    if (!token) return
    disconnectRankingSSE()
    sseClosed = false
    rankingSSE = await createEventSourceWithReconnect(
      `/api/typing/competitions/${competitionId.value}/subscribe`,
      {
        ranking: (e) => { if (!sseClosed) try { ranking.value = JSON.parse(e.data) } catch { /* 静默降级：SSE 数据解析失败跳过 */ } }
      }
    )
  }

  function disconnectRankingSSE() {
    sseClosed = true
    if (rankingSSE) { rankingSSE.close(); rankingSSE = null }
  }

  function connectAnnounceSSE() {
    announceSSE = createEventSourceWithReconnect(
      '/api/typing/announcements/subscribe',
      {
        competition_started: (e) => {
          try {
            const data = JSON.parse(e.data)
            if (inCompetition.value && competitionId.value === data.competitionId) return
            ElNotification({
              title: '🏆 打字竞赛已开始',
              message: `「${data.title}」${data.durationMinutes ? ' · ' + data.durationMinutes + '分钟' : ''} — 点击加入`,
              type: 'success',
              duration: 0,
              onClick: () => {
                // parent should handle mode switch
              }
            })
          } catch { /* 静默降级：竞赛通知解析失败不影响轮询 */ }
        }
      }
    )
  }

  function disconnectAnnounceSSE() {
    if (announceSSE) { announceSSE.close(); announceSSE = null }
  }

  function startCompPolling(onCompetitionDetected) {
    compPollTimer = setInterval(async () => {
      if (inCompetition.value) return
      try {
        const res = await getCurrentCompetition()
        if (res.code === 200 && res.data) {
          ElNotification({
            title: '🏆 检测到进行中的打字竞赛',
            message: `「${res.data.textTitle || res.data.title || ''}」— 点击切换竞赛模式`,
            type: 'success',
            duration: 15000,
            onClick: () => onCompetitionDetected?.()
          })
        }
      } catch { /* 静默降级：竞赛检测请求失败下次重试 */ }
    }, 30000)
  }

  function startProgressReport(correctCount, textContent, speedWpm, accuracy, backspaceCount, progressPercent, isFinished) {
    clearInterval(progressTimer)
    let hiddenSkip = 0
    progressTimer = setInterval(() => {
      if (!inCompetition.value || isFinished.value || !competitionId.value) return
      // #10 修复：隐藏标签页时降低上报频率（每15秒一次），而非完全停止
      if (document.hidden) {
        hiddenSkip++
        if (hiddenSkip < 3) return // 跳过前2次（5s×2=10s），第3次（15s）才上报
        hiddenSkip = 0
      }
      reportProgress({
        competitionId: competitionId.value,
        progress: {
          correctCount: correctCount.value,
          totalCount: textContent.value.length,
          speedWpm: speedWpm.value,
          accuracy: accuracy.value,
          backspaceCount: backspaceCount.value,
          progressPercent: progressPercent.value
        }
      }).catch(() => {})
    }, 5000)
  }

  function cleanup() {
    sseClosed = true
    if (rankingSSE) { rankingSSE.close(); rankingSSE = null }
    if (announceSSE) { announceSSE.close(); announceSSE = null }
    clearInterval(countdownTimer)
    clearInterval(progressTimer)
    clearInterval(compPollTimer)
  }

  onUnmounted(() => { cleanup() })

  return {
    ranking, countdown, compEndTime, competitionId, inCompetition,
    connectRankingSSE, disconnectRankingSSE,
    connectAnnounceSSE, disconnectAnnounceSSE,
    startCompPolling, startProgressReport,
    updateCountdown, startCountdown, cleanup
  }
}

import { onUnmounted } from 'vue'

export function useQuizAudio() {
  let audioCtx = null
  const getAudioCtx = () => {
    if (!audioCtx) audioCtx = new (window.AudioContext || window.webkitAudioContext)()
    return audioCtx
  }
  const closeAudioCtx = () => {
    if (audioCtx && audioCtx.state !== 'closed') {
      audioCtx.close().then(() => { audioCtx = null }).catch(() => {})
    }
  }
  const playTone = (freq, duration, type = 'sine', volume = 0.15) => {
    try {
      const ctx = getAudioCtx()
      const osc = ctx.createOscillator()
      const gain = ctx.createGain()
      osc.type = type
      osc.frequency.value = freq
      gain.gain.value = volume
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + duration)
      osc.connect(gain)
      gain.connect(ctx.destination)
      osc.start()
      osc.stop(ctx.currentTime + duration)
    } catch { /* 静默失败 */ }
  }
  const playTick = () => playTone(800, 0.05, 'square', 0.08)
  const playDing = () => { playTone(880, 0.15, 'sine', 0.2); setTimeout(() => playTone(1100, 0.2, 'sine', 0.15), 150) }
  const playCorrect = () => { playTone(523, 0.1, 'sine', 0.2); setTimeout(() => playTone(659, 0.1, 'sine', 0.18), 100); setTimeout(() => playTone(784, 0.15, 'sine', 0.15), 200) }
  const playWrong = () => { playTone(300, 0.15, 'sawtooth', 0.12); setTimeout(() => playTone(250, 0.2, 'sawtooth', 0.1), 150) }
  const playBuzz = () => { playTone(200, 0.3, 'square', 0.1) }
  onUnmounted(() => { closeAudioCtx() })
  return { playTick, playDing, playCorrect, playWrong, playBuzz, closeAudioCtx }
}
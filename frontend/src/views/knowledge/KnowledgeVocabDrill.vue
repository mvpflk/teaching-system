<template>
  <div class="kvd">
    <div class="kvd-header">
      <el-button text @click="goBack">← 返回</el-button>
      <h3>单词记忆</h3>
      <span class="kvd-count" v-if="words.length">{{ currentIdx + 1 }} / {{ words.length }}</span>
    </div>

    <div class="kvd-progress" v-if="words.length">
      <div class="kvd-progress-fill" :style="{ width: progressPct + '%' }"></div>
    </div>

    <div v-if="loading" style="padding:60px 0;text-align:center">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <p style="color:var(--text-secondary);margin-top:12px">加载今日复习词汇...</p>
    </div>

    <el-empty v-else-if="error" :description="error">
      <el-button @click="loadWords">重试</el-button>
    </el-empty>

    <el-empty v-else-if="!words.length" description="今日无待复习单词 🎉">
      <template #image>
        <div style="font-size:64px;padding:20px">🌿</div>
      </template>
      <p style="color:var(--text-secondary);margin-bottom:12px">所有单词已掌握，明天再来看看吧</p>
      <el-button @click="loadWords">刷新</el-button>
    </el-empty>

    <div v-else class="kvd-body">
      <div class="kvd-card" :class="{ flipped: cardFlipped }" @click="flipCard">
        <div class="kvd-face kvd-front">
          <div class="kvd-word">
            {{ currentWord.word }}
            <el-button class="kvd-speak-btn" text :icon="null" @click.stop="speakWord" title="朗读发音">
              🔊
            </el-button>
          </div>
          <div v-if="currentWord.phonetic" class="kvd-phonetic">{{ currentWord.phonetic }}</div>
          <div class="kvd-flip-hint">点击翻转查看释义</div>
        </div>
        <div class="kvd-face kvd-back">
          <div class="kvd-meaning">{{ currentWord.meaning }}</div>
          <div v-if="currentWord.example" class="kvd-example">{{ currentWord.example }}</div>
        </div>
      </div>

      <div v-if="cardFlipped && !rated" class="kvd-spelling">
        <el-input
          v-model="spelling"
          :placeholder="spellPlaceholder"
          @keyup.enter="checkSpelling"
        >
          <template #append>
            <el-button @click="checkSpelling">检查</el-button>
          </template>
        </el-input>
        <div v-if="spellingResult !== null" class="kvd-spelling-feedback" :class="{ correct: spellingResult }">
          <template v-if="spellingResult">✅ 正确</template>
          <template v-else>❌ 正确答案：{{ correctSpelling }}</template>
        </div>
      </div>

      <div v-if="cardFlipped && !rated" class="kvd-rating">
        <p class="kvd-rating-label">这个词你记住了吗？</p>
        <div class="kvd-rating-btns">
          <el-button type="success" :icon="Check" @click="rate(3)">认识</el-button>
          <el-button @click="rate(2)">不确定</el-button>
          <el-button type="danger" plain :icon="Close" @click="rate(1)">不认识</el-button>
        </div>
      </div>

      <div v-if="submitting" class="kvd-submitting">
        <el-icon class="is-loading"><Loading /></el-icon> 提交中...
      </div>

      <div v-if="done" class="kvd-done">
        <el-alert
          :title="`本节完成 — ${knownCount} 认识 · ${unsureCount} 不确定 · ${unknownCount} 不认识`"
          :type="knownCount >= words.length / 2 ? 'success' : 'warning'"
          show-icon
          :closable="false"
        />
        <div style="margin-top:16px;text-align:center">
          <el-button @click="loadWords">再来一组</el-button>
          <el-button type="primary" @click="goBack">返回知识库</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading, Check, Close } from '@element-plus/icons-vue'
import { getDailyTask, submitDrillAnswer, completeDrill } from '@/api/precisionEnglish'

const router = useRouter()

const loading = ref(true)
const error = ref('')
const words = ref([])
const currentIdx = ref(0)
const cardFlipped = ref(false)
const spelling = ref('')
const spellingResult = ref(null)
const rated = ref(false)
const submitting = ref(false)
const done = ref(false)
const results = ref([])
const startTime = ref(0)

const currentWord = computed(() => words.value[currentIdx.value])
const progressPct = computed(() => words.value.length ? Math.round(((currentIdx.value + (rated.value || done.value ? 1 : 0)) / words.value.length) * 100) : 0)
const knownCount = computed(() => results.value.filter(r => r.rating === 3).length)
const unsureCount = computed(() => results.value.filter(r => r.rating === 2).length)
const unknownCount = computed(() => results.value.filter(r => r.rating === 1).length)

function speakWord() {
  const engWord = currentWord.value?.word
  if (!engWord) return
  try {
    const u = new SpeechSynthesisUtterance(engWord)
    u.lang = 'en-US'; u.rate = 0.85
    speechSynthesis.cancel()
    speechSynthesis.speak(u)
  } catch { /* Speech API unavailable */ }
}

const correctSpelling = computed(() => {
  // 翻转看释义确认含义后，输入英文单词拼写巩固记忆
  return currentWord.value?.word || ''
})

const spellPlaceholder = computed(() => '输入英文拼写巩固记忆...')

function flipCard() {
  if (!cardFlipped.value) cardFlipped.value = true
}

function checkSpelling() {
  if (!spelling.value.trim()) return
  const expected = correctSpelling.value
  if (!expected) { spellingResult.value = false; return }
  const correct = spelling.value.trim().toLowerCase() === expected.toLowerCase()
  spellingResult.value = correct
}

async function rate(rating) {
  if (submitting.value) return
  rated.value = true
  submitting.value = true
  const word = currentWord.value
  try {
    let answer = ''
    let hintLevel = 0
    if (rating === 3) {
      answer = word.word || ''
      hintLevel = 0
    } else if (rating === 2) {
      answer = ''
      hintLevel = 0
    } else {
      answer = ''
      hintLevel = 3
    }
    await submitDrillAnswer({
      questionId: word.questionId || -(currentIdx.value + 1),
      answer,
      hintLevel,
      questionType: 'FILL_IN',
      subject: '英语[职高]',
      word: word.word,
      direction: word.direction || 'en2cn'
    })
  } catch {
    ElMessage.warning('提交失败，本地已记录')
  } finally {
    submitting.value = false
  }
  results.value.push({ word: word.word, rating })
  nextWord()
}

async function nextWord() {
  if (currentIdx.value < words.value.length - 1) {
    currentIdx.value++
    cardFlipped.value = false
    spelling.value = ''
    spellingResult.value = null
    rated.value = false
  } else {
    submitting.value = true
    try {
      await completeDrill({
        answers: results.value.map(r => ({
          questionId: -(results.value.indexOf(r) + 1),
          correct: r.rating === 3,
          hintLevel: r.rating === 1 ? 3 : 0
        })),
        groupSeq: 1,
        elapsedSeconds: Math.round((Date.now() - startTime.value) / 1000)
      })
    } catch (e) {
      console.error('completeDrill 失败', e)
    } finally {
      submitting.value = false
    }
    done.value = true
  }
}

function goBack() {
  router.push('/knowledge-base')
}

async function loadWords() {
  loading.value = true
  error.value = ''
  words.value = []
  currentIdx.value = 0
  cardFlipped.value = false
  spelling.value = ''
  spellingResult.value = null
  rated.value = false
  done.value = false
  results.value = []
  startTime.value = 0
  try {
    const res = await getDailyTask()
    const task = res.data || {}
    const vocab = (task.vocabQuestions || []).map(q => ({
      ...q,
      type: 'vocab',
      direction: q.direction || 'en2cn'
    }))
    words.value = vocab
    startTime.value = Date.now()
  } catch (e) {
    error.value = '加载失败，请检查网络后重试'
  } finally {
    loading.value = false
  }
}

onMounted(loadWords)
</script>

<style scoped>
.kvd {
  max-width: 640px;
  margin: 0 auto;
  padding: 8px 0 32px;
}
.kvd-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.kvd-header h3 {
  margin: 0;
  font-size: var(--fs-lg);
  flex: 1;
}
.kvd-count {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
}
.kvd-progress {
  height: 4px;
  background: var(--bg-secondary);
  border-radius: 2px;
  margin-bottom: 20px;
  overflow: hidden;
}
.kvd-progress-fill {
  height: 100%;
  background: var(--primary-color);
  border-radius: 2px;
  transition: width 0.3s ease;
}
.kvd-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.kvd-card {
  height: 220px;
  perspective: 800px;
  cursor: pointer;
  position: relative;
}
.kvd-face {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  background: var(--bg-card);
  backface-visibility: hidden;
  transition: transform 0.4s ease;
}
.kvd-front {
  transform: rotateY(0deg);
}
.kvd-card.flipped .kvd-front {
  transform: rotateY(180deg);
}
.kvd-card.flipped .kvd-back {
  transform: rotateY(0deg);
}
.kvd-back {
  transform: rotateY(-180deg);
}
.kvd-word {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
}
.kvd-phonetic {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  margin-bottom: 4px;
}
.kvd-meaning {
  font-size: 24px;
  font-weight: 600;
  color: var(--primary-color);
  margin-bottom: 8px;
}
.kvd-example {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  max-width: 400px;
  text-align: center;
  line-height: 1.5;
}
.kvd-speak-btn {
  font-size: 20px;
  margin-left: 8px;
  vertical-align: middle;
  cursor: pointer;
  opacity: 0.7;
  transition: opacity 0.2s;
}
.kvd-speak-btn:hover { opacity: 1; }
.kvd-flip-hint {
  font-size: var(--fs-xs);
  color: var(--text-tertiary);
  margin-top: 16px;
  padding: 4px 12px;
  border: 1px dashed var(--border-color);
  border-radius: 20px;
}
.kvd-spelling {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.kvd-spelling-feedback {
  padding: 8px 12px;
  border-radius: 6px;
  font-size: var(--fs-sm);
  background: var(--bg-danger-light);
  color: var(--el-color-danger);
}
.kvd-spelling-feedback.correct {
  background: var(--bg-success-light);
  color: var(--el-color-success);
}
.kvd-rating {
  text-align: center;
}
.kvd-rating-label {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  margin: 0 0 10px;
}
.kvd-rating-btns {
  display: flex;
  gap: 12px;
  justify-content: center;
}
.kvd-submitting {
  text-align: center;
  padding: 16px;
  color: var(--text-secondary);
}
.kvd-done {
  padding: 16px 0;
}
</style>

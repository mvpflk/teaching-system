<template>
  <div class="hardware-gallery">
    <div class="hg-header">
      <h4>🖥️ 硬件认知图鉴</h4>
      <el-tag size="small" type="info">信息技术应用基础 — 单元2</el-tag>
    </div>

    <!-- 模式切换 -->
    <el-radio-group v-model="mode" size="small" style="margin-bottom:16px">
      <el-radio-button value="learn">学习模式</el-radio-button>
      <el-radio-button value="quiz">识别测验</el-radio-button>
    </el-radio-group>

    <!-- 学习模式：部件卡片浏览 -->
    <div v-if="mode === 'learn'" class="learn-grid">
      <el-card v-for="hw in hardware" :key="hw.id" shadow="never" class="hw-card">
        <div class="hw-icon">{{ hw.icon }}</div>
        <h5>{{ hw.name }}</h5>
        <p class="hw-desc">{{ hw.desc }}</p>
        <div class="hw-tags">
          <el-tag v-for="t in hw.tags" :key="t" size="small" :type="t.includes('输入') || t.includes('输出') ? 'warning' : ''">{{ t }}</el-tag>
        </div>
        <div class="hw-params" v-if="hw.params">
          <span v-for="p in hw.params" :key="p" class="param-chip">{{ p }}</span>
        </div>
      </el-card>
    </div>

    <!-- 测验模式：名称匹配 -->
    <div v-else-if="quizStarted" class="quiz-panel">
      <div class="quiz-progress">
        <span>第 {{ currentQuiz + 1 }} / {{ quizQuestions.length }} 题</span>
        <span>得分: {{ quizScore }}</span>
      </div>

      <div class="quiz-question">
        <div class="quiz-icon">{{ quizQuestions[currentQuiz]?.icon }}</div>
        <p class="quiz-prompt">这是什么硬件部件？</p>
        <div class="quiz-options">
          <el-button
            v-for="(opt, i) in quizQuestions[currentQuiz]?.options || []"
            :key="i"
            :type="quizAnswered !== null ? (i === quizAnswer ? 'success' : (i === quizAnswered ? 'danger' : '')) : ''"
            :disabled="quizAnswered !== null"
            size="large"
            @click="answerQuiz(i)"
          >
            {{ opt }}
          </el-button>
        </div>
        <div v-if="quizAnswered !== null" class="quiz-feedback">
          <el-alert
            :title="quizAnswered === quizAnswer ? '✅ 正确！' : `❌ 正确答案是「${quizQuestions[currentQuiz]?.options[quizAnswer]}」`"
            :type="quizAnswered === quizAnswer ? 'success' : 'error'"
            :closable="false"
          />
          <el-button size="small" style="margin-top:8px" @click="nextQuiz">
            {{ currentQuiz < quizQuestions.length - 1 ? '下一题' : '完成测验' }}
          </el-button>
        </div>
      </div>

      <!-- 错题回顾 -->
      <div v-if="quizFinished && wrongAnswers.length" class="wrong-review">
        <h5>错题回顾</h5>
        <div v-for="(w, i) in wrongAnswers" :key="i" class="wrong-item">
          <span class="wrong-icon">{{ w.icon }}</span>
          <span>正确答案: <strong>{{ w.correct }}</strong></span>
          <span style="color:var(--el-color-danger, #f56c6c)">你的选择: {{ w.yours }}</span>
        </div>
      </div>

      <!-- 完成 -->
      <div v-if="quizFinished" class="quiz-result">
        <el-result :icon="quizScore >= quizQuestions.length * 0.7 ? 'success' : 'warning'"
          :title="`得分: ${quizScore}/${quizQuestions.length}`"
          :sub-title="quizScore >= quizQuestions.length * 0.7 ? '掌握得很好！' : '多看看学习模式吧'"
        >
          <template #extra>
            <el-button type="primary" @click="restartQuiz">重新测验</el-button>
            <el-button @click="mode = 'learn'">去学习</el-button>
          </template>
        </el-result>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const hardware = [
  { id: 1, name: 'CPU 中央处理器', icon: '🔲', desc: '计算机的核心运算和控制部件，负责执行指令和处理数据。', tags: ['核心部件', '运算控制'], params: ['主频(GHz)', '核心数', '缓存L1/L2/L3'] },
  { id: 2, name: '主板 Motherboard', icon: '📋', desc: '连接和承载所有硬件的基础电路板，提供数据传输通道。', tags: ['核心部件', '连接中枢'], params: ['芯片组', '插槽类型', '板型ATX/M-ATX'] },
  { id: 3, name: '内存 RAM', icon: '📏', desc: '临时存储运行中的程序和数据，断电后数据丢失。', tags: ['存储', '易失性'], params: ['容量(GB)', '频率(MHz)', 'DDR4/DDR5'] },
  { id: 4, name: '硬盘 SSD/HDD', icon: '💾', desc: '永久存储操作系统、程序和用户数据。SSD速度快，HDD容量大。', tags: ['存储', '非易失性'], params: ['容量(TB)', '读写速度', 'SATA/NVMe'] },
  { id: 5, name: '显卡 GPU', icon: '🎮', desc: '处理图像和视频输出，独立显卡适合游戏和图形设计。', tags: ['输出', '图形处理'], params: ['显存(GB)', '核心频率', 'CUDA核心数'] },
  { id: 6, name: '电源 PSU', icon: '🔋', desc: '将市电转换为计算机所需的直流电，供给各部件使用。', tags: ['供电'], params: ['功率(W)', '80+认证', '模组化'] },
  { id: 7, name: '网卡 NIC', icon: '🌐', desc: '实现计算机与网络之间的数据传输，可以是有线或无线。', tags: ['网络', '通信'], params: ['速率(Mbps)', '有线/无线', 'MAC地址'] },
  { id: 8, name: '声卡', icon: '🔊', desc: '处理音频输入输出，将数字音频信号转换为模拟声音。', tags: ['音频', '输入输出'], params: ['声道数', '采样率'] },
  { id: 9, name: '机箱', icon: '📦', desc: '容纳和保护所有硬件部件的外壳，提供散热风道。', tags: ['外壳', '散热'], params: ['板型兼容', '风道设计'] },
  { id: 10, name: '散热器', icon: '🌀', desc: '为CPU/GPU等发热部件降温，分风冷和水冷两种。', tags: ['散热', '冷却'], params: ['风冷/水冷', 'TDP(W)'] },
  { id: 11, name: '显示器', icon: '🖥️', desc: '输出设备，将计算机处理的图像信息显示给用户。', tags: ['输出'], params: ['分辨率', '刷新率', '面板类型IPS/TN/VA'] },
  { id: 12, name: '键盘/鼠标', icon: '⌨️', desc: '最基本的人机输入设备，键盘输入文字，鼠标控制光标。', tags: ['输入'], params: ['机械/薄膜', '有线/无线', 'DPI'] },
]

const mode = ref('learn')

// 测验逻辑
const quizQuestions = computed(() => {
  const selected = [...hardware].sort(() => Math.random() - 0.5).slice(0, 8)
  return selected.map(hw => {
    const others = hardware.filter(h => h.id !== hw.id).sort(() => Math.random() - 0.5).slice(0, 3)
    const options = [...others.map(o => o.name), hw.name].sort(() => Math.random() - 0.5)
    return { icon: hw.icon, options, answer: hw.name }
  })
})

const currentQuiz = ref(0)
const quizAnswered = ref(null)
const quizAnswer = ref(-1)
const quizScore = ref(0)
const quizFinished = ref(false)
const questions = ref([])
const quizStarted = ref(false)
const wrongAnswers = ref([])

function startQuiz() {
  questions.value = quizQuestions.value
  currentQuiz.value = 0
  quizScore.value = 0
  quizFinished.value = false
  quizAnswered.value = null
  quizStarted.value = true
  wrongAnswers.value = []
  if (questions.value.length > 0) {
    quizAnswer.value = questions.value[0]?.options.indexOf(questions.value[0]?.answer) ?? -1
  }
}

function answerQuiz(i) {
  quizAnswered.value = i
  if (i === quizAnswer.value) quizScore.value++
  else wrongAnswers.value.push({ icon: questions.value[currentQuiz.value]?.icon, correct: questions.value[currentQuiz.value]?.options[quizAnswer.value], yours: questions.value[currentQuiz.value]?.options[i] })
}

function nextQuiz() {
  if (currentQuiz.value < questions.value.length - 1) {
    currentQuiz.value++
    quizAnswered.value = null
    quizAnswer.value = questions.value[currentQuiz.value]?.options.indexOf(questions.value[currentQuiz.value]?.answer) ?? -1
  } else {
    quizFinished.value = true
  }
}

function restartQuiz() {
  startQuiz()
}

// 监听模式切换自动开始测验
watch(mode, (m) => { if (m === 'quiz') startQuiz() })
</script>

<style scoped>
.hardware-gallery { padding: 16px; background: var(--bg-card); border-radius: var(--radius-md); border: 1px solid var(--border-color); }
.hg-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.hg-header h4 { margin: 0; }

.learn-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 12px; }
.hw-card { text-align: center; }
.hw-icon { font-size: 40px; margin-bottom: 8px; }
.hw-card h5 { margin: 0 0 4px; font-size: var(--fs-sm); }
.hw-desc { font-size: var(--fs-xs); color: var(--text-secondary); margin: 0 0 8px; }
.hw-tags { display: flex; gap: 4px; justify-content: center; flex-wrap: wrap; margin-bottom: 6px; }
.hw-params { display: flex; gap: 4px; justify-content: center; flex-wrap: wrap; }
.param-chip { font-size: 11px; padding: 2px 6px; background: var(--bg-page); border-radius: 4px; color: var(--text-secondary); }

.quiz-panel { text-align: center; }
.quiz-progress { display: flex; justify-content: space-between; font-size: var(--fs-sm); margin-bottom: 16px; color: var(--text-secondary); }
.quiz-icon { font-size: 64px; margin-bottom: 12px; }
.quiz-prompt { font-size: var(--fs-md); font-weight: 600; margin: 0 0 16px; }
.quiz-options { display: flex; flex-wrap: wrap; gap: 10px; justify-content: center; }
.quiz-feedback { margin-top: 16px; }
.quiz-result { margin-top: 24px; }
</style>

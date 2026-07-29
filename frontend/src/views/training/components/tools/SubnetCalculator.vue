<template>
  <div class="subnet-calc">
    <div class="sc-header">
      <h4>🔢 子网划分练习</h4>
      <el-tag size="small" type="info">网络应用基础 — 单元2任务2</el-tag>
    </div>

    <div class="sc-body">
      <!-- 练习模式 -->
      <div class="exercise-panel">
        <div class="question-card">
          <h5>📝 题目</h5>
          <p>IP地址: <strong>{{ question.ip }}</strong></p>
          <p>子网掩码: <strong>{{ question.mask }}</strong></p>
        </div>

        <div class="answer-form">
          <el-form label-position="top" size="small">
            <el-row :gutter="12">
              <el-col :xs="24" :sm="8">
                <el-form-item label="网络地址">
                  <el-input v-model="answers.network" placeholder="如 192.168.1.0" @change="checkAnswer" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="8">
                <el-form-item label="广播地址">
                  <el-input v-model="answers.broadcast" placeholder="如 192.168.1.255" @change="checkAnswer" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="8">
                <el-form-item label="可用IP范围(首)">
                  <el-input v-model="answers.firstHost" placeholder="如 192.168.1.1" @change="checkAnswer" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="12">
              <el-col :xs="12" :sm="6">
                <el-form-item label="可用IP范围(末)">
                  <el-input v-model="answers.lastHost" placeholder="如 192.168.1.254" />
                </el-form-item>
              </el-col>
              <el-col :xs="12" :sm="6">
                <el-form-item label="子网数量">
                  <el-input-number v-model="answers.subnetCount" :min="1" controls-position="right" />
                </el-form-item>
              </el-col>
              <el-col :xs="12" :sm="6">
                <el-form-item label="每子网主机数">
                  <el-input-number v-model="answers.hostCount" :min="1" controls-position="right" />
                </el-form-item>
              </el-col>
              <el-col :xs="12" :sm="6">
                <el-form-item label="CIDR前缀">
                  <el-input v-model="answers.cidr" placeholder="如 /24" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </div>

        <div class="actions">
          <el-button type="primary" @click="submitAnswer" :disabled="!canSubmit">提交验证</el-button>
          <el-button @click="nextQuestion">换一题</el-button>
          <el-button text @click="showHint = !showHint">{{ showHint ? '隐藏提示' : '查看提示' }}</el-button>
        </div>
      </div>

      <!-- 结果反馈 -->
      <div v-if="submitted" class="result-panel">
        <el-alert :title="resultTitle" :type="allCorrect ? 'success' : 'warning'" :closable="false" show-icon />
        <div v-if="!allCorrect" class="corrections">
          <div v-for="c in corrections" :key="c.field" class="corr-item">
            <span class="corr-field">{{ c.field }}</span>
            <span class="corr-wrong">❌ {{ c.yours }}</span>
            <span class="corr-right">✅ {{ c.correct }}</span>
          </div>
        </div>
      </div>

      <!-- 提示面板 -->
      <div v-if="showHint" class="hint-panel">
        <h5>💡 计算提示</h5>
        <ul>
          <li>网络地址 = IP地址 AND 子网掩码（逐位）</li>
          <li>广播地址 = 网络地址 OR (NOT 子网掩码)</li>
          <li>可用主机范围 = 网络地址+1 ~ 广播地址-1</li>
          <li>子网数量 = 2^(借位数)</li>
          <li>每子网主机 = 2^(主机位数) - 2</li>
          <li>CIDR前缀 = 子网掩码中1的个数</li>
        </ul>
      </div>

      <!-- 得分统计 -->
      <div class="score-panel">
        <span>正确: <strong style="color:var(--el-color-success, #67c23a)">{{ correctCount }}</strong></span>
        <span>错误: <strong style="color:var(--el-color-danger, #f56c6c)">{{ wrongCount }}</strong></span>
        <span>正确率: <strong>{{ correctRate }}%</strong></span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'

// 题目生成
const QUESTIONS = [
  { ip: '192.168.1.100', mask: '255.255.255.0' },
  { ip: '10.0.0.50', mask: '255.0.0.0' },
  { ip: '172.16.5.30', mask: '255.255.0.0' },
  { ip: '192.168.10.25', mask: '255.255.255.240' },
  { ip: '10.20.30.40', mask: '255.255.255.128' },
  { ip: '172.31.100.200', mask: '255.255.224.0' },
  { ip: '192.168.200.150', mask: '255.255.255.192' },
  { ip: '10.100.50.10', mask: '255.255.252.0' },
]

const ANSWERS = {
  '192.168.1.100:255.255.255.0': { network: '192.168.1.0', broadcast: '192.168.1.255', firstHost: '192.168.1.1', lastHost: '192.168.1.254', subnetCount: 1, hostCount: 254, cidr: '/24' },
  '10.0.0.50:255.0.0.0': { network: '10.0.0.0', broadcast: '10.255.255.255', firstHost: '10.0.0.1', lastHost: '10.255.255.254', subnetCount: 1, hostCount: 16777214, cidr: '/8' },
  '172.16.5.30:255.255.0.0': { network: '172.16.0.0', broadcast: '172.16.255.255', firstHost: '172.16.0.1', lastHost: '172.16.255.254', subnetCount: 1, hostCount: 65534, cidr: '/16' },
  '192.168.10.25:255.255.255.240': { network: '192.168.10.16', broadcast: '192.168.10.31', firstHost: '192.168.10.17', lastHost: '192.168.10.30', subnetCount: 16, hostCount: 14, cidr: '/28' },
  '10.20.30.40:255.255.255.128': { network: '10.20.30.0', broadcast: '10.20.30.127', firstHost: '10.20.30.1', lastHost: '10.20.30.126', subnetCount: 2, hostCount: 126, cidr: '/25' },
  '172.31.100.200:255.255.224.0': { network: '172.31.96.0', broadcast: '172.31.127.255', firstHost: '172.31.96.1', lastHost: '172.31.127.254', subnetCount: 8, hostCount: 8190, cidr: '/19' },
  '192.168.200.150:255.255.255.192': { network: '192.168.200.128', broadcast: '192.168.200.191', firstHost: '192.168.200.129', lastHost: '192.168.200.190', subnetCount: 4, hostCount: 62, cidr: '/26' },
  '10.100.50.10:255.255.252.0': { network: '10.100.48.0', broadcast: '10.100.51.255', firstHost: '10.100.48.1', lastHost: '10.100.51.254', subnetCount: 64, hostCount: 1022, cidr: '/22' },
}

const question = ref(QUESTIONS[0])
const answers = reactive({ network: '', broadcast: '', firstHost: '', lastHost: '', subnetCount: null, hostCount: null, cidr: '' })
const submitted = ref(false)
const allCorrect = ref(false)
const corrections = ref([])
const resultTitle = ref('')
const showHint = ref(false)
const correctCount = ref(0)
const wrongCount = ref(0)
const correctRate = computed(() => {
  const total = correctCount.value + wrongCount.value
  return total ? Math.round(correctCount.value / total * 100) : 0
})
const canSubmit = computed(() => answers.network.trim() || answers.broadcast.trim())

function getKey() { return question.value.ip + ':' + question.value.mask }

function submitAnswer() {
  const expected = ANSWERS[getKey()]
  if (!expected) return
  submitted.value = true
  corrections.value = []
  let ok = true

  const fields = [
    { field: '网络地址', key: 'network', yours: answers.network, correct: expected.network },
    { field: '广播地址', key: 'broadcast', yours: answers.broadcast, correct: expected.broadcast },
    { field: '可用IP首位', key: 'firstHost', yours: answers.firstHost, correct: expected.firstHost },
    { field: '可用IP末位', key: 'lastHost', yours: answers.lastHost, correct: expected.lastHost },
    { field: '子网数量', key: 'subnetCount', yours: String(answers.subnetCount ?? ''), correct: String(expected.subnetCount) },
    { field: '主机数', key: 'hostCount', yours: String(answers.hostCount ?? ''), correct: String(expected.hostCount) },
    { field: 'CIDR', key: 'cidr', yours: answers.cidr, correct: expected.cidr },
  ]

  for (const f of fields) {
    if (f.yours !== f.correct) {
      ok = false
      corrections.value.push(f)
    }
  }

  allCorrect.value = ok
  resultTitle.value = ok ? '🎉 全部正确！' : `❌ ${corrections.value.length} 项有误`
  if (ok) correctCount.value++
  else wrongCount.value++
}

function checkAnswer() { if (submitted.value) submitAnswer() }

function nextQuestion() {
  submitted.value = false
  corrections.value = []
  const idx = Math.floor(Math.random() * QUESTIONS.length)
  question.value = QUESTIONS[idx]
  answers.network = answers.broadcast = answers.firstHost = answers.lastHost = answers.cidr = ''
  answers.subnetCount = answers.hostCount = null
}

onMounted(() => nextQuestion())
</script>

<style scoped>
.subnet-calc { padding: 16px; background: var(--bg-card); border-radius: var(--radius-md); border: 1px solid var(--border-color); }
.sc-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.sc-header h4 { margin: 0; }
.question-card { padding: 12px; background: var(--bg-page); border-radius: var(--radius-sm); margin-bottom: 16px; }
.question-card h5 { margin: 0 0 8px; }
.question-card p { margin: 4px 0; font-size: var(--fs-md); }
.answer-form { margin-bottom: 12px; }
.actions { display: flex; gap: 8px; margin-bottom: 12px; }
.result-panel { margin-bottom: 12px; }
.corrections { margin-top: 8px; }
.corr-item { display: flex; gap: 12px; font-size: var(--fs-sm); padding: 4px 0; }
.corr-field { font-weight: 600; min-width: 80px; }
.corr-wrong { color: var(--el-color-danger, #f56c6c); }
.corr-right { color: var(--el-color-success, #67c23a); }
.hint-panel { padding: 12px; background: var(--el-color-info-light-9); border-radius: var(--radius-sm); margin-bottom: 12px; }
.hint-panel h5 { margin: 0 0 8px; }
.hint-panel ul { margin: 0; padding-left: 20px; font-size: var(--fs-sm); }
.hint-panel li { margin: 2px 0; }
.score-panel { display: flex; gap: 24px; font-size: var(--fs-sm); padding-top: 12px; border-top: 1px solid var(--border-color); }
</style>

<template>
  <div class="buzz-panel">
    <div class="bp-header">
      <el-button text @click="$emit('back')"><el-icon><ArrowLeft /></el-icon>返回</el-button>
      <span class="bp-title"><el-icon><Lightning /></el-icon> 抢答</span>
      <el-tag
        v-if="active"
        type="warning"
        size="small"
        effect="dark"
      >
        进行中
      </el-tag>
    </div>

    <!-- 设置态 -->
    <div v-if="!active" class="bp-setup">
      <el-input
        v-model="questionText"
        type="textarea"
        :rows="3"
        placeholder="输入抢答题目..."
        size="large"
      />
      <div class="bp-reward">
        <span>奖励积分：</span>
        <el-input-number
          v-model="reward"
          :min="1"
          :max="10"
          size="small"
        />
        <span>分</span>
      </div>
      <el-button
        type="warning"
        size="large"
        :loading="starting"
        class="bp-start-btn"
        @click="start"
      >
        <el-icon><Lightning /></el-icon> 开始抢答
      </el-button>
    </div>

    <!-- 进行态 -->
    <div v-else class="bp-active">
      <div class="bp-question">{{ questionText }}</div>

      <div v-if="!winner" class="bp-waiting">
        <div class="bp-spinner"></div>
        <div class="bp-wait-text">等待学生抢答中...</div>
        <div class="bp-timer">{{ elapsed }}s</div>
        <el-button
          v-if="elapsed >= 30"
          type="danger"
          class="bp-close-btn"
          @click="close"
        >
          <el-icon><CircleClose /></el-icon> 超时关闭
        </el-button>
      </div>

      <div v-else class="bp-winner">
        <div class="bpw-badge">
          <el-icon class="bpw-trophy"><TrophyBase /></el-icon>
          <div class="bpw-badge-text">抢到！</div>
        </div>
        <div class="bpw-name">{{ winner.studentName }}</div>
        <div class="bpw-time">{{ winner.buzzTime ? (winner.buzzTime + 'ms') : '' }}</div>
        <div class="bp-actions">
          <el-button
            type="success"
            size="large"
            :loading="grading"
            @click="gradeBuzz(1)"
          >
            <el-icon><CircleCheck /></el-icon> 正确 +{{ reward }}分
          </el-button>
          <el-button
            type="danger"
            size="large"
            :loading="grading"
            @click="gradeBuzz(0)"
          >
            <el-icon><CircleClose /></el-icon> 错误 → 错题本
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onBeforeUnmount } from 'vue'
import { ArrowLeft, Lightning, CircleCheck, CircleClose, TrophyBase } from '@element-plus/icons-vue'
import { startBuzz, gradeBuzz as gradeBuzzApi } from '@/api/classroom'
import { ElMessage } from 'element-plus'

const props = defineProps({ classId: [String, Number], sseConn: Object })
const emit = defineEmits(['back', 'scored'])

const questionText = ref('')
const reward = ref(3)
const starting = ref(false)
const active = ref(false)
const winner = ref(null)
const sessionId = ref(null)
const elapsed = ref(0)
const grading = ref(false)
let timer = null

const onBuzzEnd = (e) => {
  const d = JSON.parse(e.data)
  if (d.winnerStudentId) {
    winner.value = { studentId: d.winnerStudentId, studentName: d.winnerName, buzzTime: d.buzzTime }
    clearInterval(timer)
  }
}

const start = async () => {
  if (!questionText.value.trim()) return ElMessage.warning('请输入题目')
  starting.value = true
  try {
    const res = await startBuzz({ classId: props.classId, questionText: questionText.value, scoreReward: reward.value })
    if (res.code === 200) {
      sessionId.value = res.data.sessionId
      active.value = true
      elapsed.value = 0
      winner.value = null
      if (props.sseConn) {
        props.sseConn.removeEventListener('buzz:end', onBuzzEnd)
        props.sseConn.addEventListener('buzz:end', onBuzzEnd)
      }
      timer = setInterval(() => {
        elapsed.value++
        if (elapsed.value >= 30 && !winner.value) {
          ElMessage.warning('抢答超时，已自动关闭')
          close()
        }
      }, 1000)
    }
  } finally { starting.value = false }
}

const gradeBuzz = async (result) => {
  if (grading.value) return
  grading.value = true
  try {
    const res = await gradeBuzzApi({ sessionId: sessionId.value, studentId: winner.value.studentId, result })
    if (res.code === 200) {
      ElMessage.success(result ? `回答正确 +${reward.value}分` : '回答错误，已记录错题本')
      emit('scored', { studentId: winner.value.studentId, ...res.data })
    }
  } finally {
    grading.value = false
    close()
  }
}

const close = () => {
  clearInterval(timer)
  active.value = false
  winner.value = null
  questionText.value = ''
  if (props.sseConn) { props.sseConn.removeEventListener('buzz:end', onBuzzEnd) }
}

onBeforeUnmount(() => {
  clearInterval(timer)
  if (props.sseConn) { props.sseConn.removeEventListener('buzz:end', onBuzzEnd) }
})
</script>

<style scoped lang="scss">
.buzz-panel { display: flex; flex-direction: column; height: 100%; }

.bp-header {
  display: flex; align-items: center; gap: 10px;
  padding-bottom: 14px; border-bottom: 0.5px solid var(--border-light); margin-bottom: var(--spacing-md);
}

.bp-title {
  font-size: var(--fs-lg); font-weight: 700; flex: 1;
  display: flex; align-items: center; gap: var(--spacing-xs);
  color: var(--text-primary);
}

// 设置态
.bp-setup { flex: 1; display: flex; flex-direction: column; gap: 18px; }

.bp-reward {
  display: flex; align-items: center; gap: 10px;
  font-size: var(--fs-md); color: var(--text-regular);
}

.bp-start-btn {
  width: 100%; font-size: var(--fs-lg); padding: 18px;
  border-radius: var(--radius-lg); height: auto;
}

// 进行态
.bp-active { flex: 1; text-align: center; display: flex; flex-direction: column; }

.bp-question {
  font-size: var(--fs-xl); font-weight: 700; padding: var(--spacing-lg);
  background: var(--bg-section); border-radius: var(--radius-lg);
  border: 0.5px solid var(--border-light); margin-bottom: var(--spacing-xl);
  line-height: 1.6; color: var(--text-primary);
}

.bp-waiting { display: flex; flex-direction: column; align-items: center; gap: 12px; }

.bp-spinner {
  width: 48px; height: 48px;
  border: 3px solid var(--border-color);
  border-top-color: var(--el-color-warning);
  border-radius: var(--radius-full);
  animation: spin 0.7s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.bp-wait-text { font-size: var(--fs-lg); color: var(--text-secondary); }
.bp-timer { font-size: 40px; font-weight: 800; color: var(--el-color-warning); font-family: 'JetBrains Mono', monospace; }
.bp-close-btn { margin-top: 12px; }

.bp-winner { margin-top: var(--spacing-sm); display: flex; flex-direction: column; align-items: center; gap: 10px; flex: 1; }

.bpw-badge { text-align: center; }
.bpw-trophy { font-size: 48px; color: var(--el-color-warning); }
.bpw-badge-text { font-size: var(--fs-md); color: var(--text-secondary); }

.bpw-name { font-size: 36px; font-weight: 800; color: var(--el-color-warning); }
.bpw-time { font-size: var(--fs-sm); color: var(--text-secondary); font-family: monospace; }

.bp-actions {
  display: flex; gap: var(--spacing-md); margin-top: var(--spacing-lg); flex-wrap: wrap; justify-content: center;
  :deep(.el-button) { min-width: 150px; padding: 14px var(--spacing-lg); font-size: var(--fs-md); }
}

@media (max-width: 768px) {
  .bpw-name { font-size: var(--fs-2xl); }
  .bp-actions { flex-direction: column; align-items: stretch; }
}
</style>

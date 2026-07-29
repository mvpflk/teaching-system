<template>
  <div class="vote-panel">
    <div class="vp-header">
      <el-button text @click="$emit('back')"><el-icon><ArrowLeft /></el-icon>返回</el-button>
      <span class="vp-title"><el-icon><TrendCharts /></el-icon> 即时投票</span>
      <el-tag :type="sceneMode === 'LAB' ? 'primary' : 'info'" size="small" effect="dark">
        {{ sceneMode === 'LAB' ? '在线' : '手动' }}
      </el-tag>
    </div>

    <!-- 教师发起 -->
    <div v-if="!active" class="vp-setup">
      <el-input v-model="question" placeholder="投票题目，如：今天讲的OSI模型大家理解了吗？" size="large" />
      <div class="vp-options">
        <div class="vp-opt-label">选项设置</div>
        <div v-for="(opt, i) in options" :key="i" class="vp-opt-row">
          <span class="vp-opt-letter">{{ String.fromCharCode(65 + i) }}.</span>
          <el-input v-model="options[i]" size="small" :placeholder="`选项${i+1}`" />
          <el-button
            v-if="options.length > 2"
            size="small"
            circle
            @click="options.splice(i,1)"
          >
            ×
          </el-button>
        </div>
        <el-button v-if="options.length < 4" size="small" @click="options.length < 4 && options.push('')">+ 添加选项</el-button>
      </div>
      <div class="vp-settings">
        <span>时长：</span>
        <el-select v-model="duration" size="small" style="width:100px">
          <el-option
            v-for="s in [30,60,120]"
            :key="s"
            :value="s"
            :label="s+'秒'"
          />
        </el-select>
        <el-checkbox v-if="sceneMode === 'LAB'" v-model="anonymous">匿名投票</el-checkbox>
      </div>
      <el-button
        type="primary"
        size="large"
        :loading="starting"
        class="vp-start-btn"
        @click="start"
      >
        <el-icon><TrendCharts /></el-icon> 发起投票
      </el-button>
    </div>

    <!-- 投票进行中 -->
    <div v-else class="vp-active">
      <div class="vp-question">{{ question }}</div>

      <!-- 在线模式：实时柱状图 -->
      <div v-if="sceneMode === 'LAB'" class="vp-online">
        <div v-for="(r, i) in pollResults" :key="i" class="vp-result-row">
          <div class="vp-r-header">
            <span class="vp-r-opt">{{ String.fromCharCode(65 + i) }}. {{ options[i] }}</span>
            <span class="vp-r-count">{{ r.count }}票</span>
          </div>
          <div class="vp-r-bar">
            <div class="vp-r-fill" :style="{ width: r.pct + '%' }">
              <span v-if="r.pct > 8" class="vp-r-pct">{{ r.pct }}%</span>
            </div>
            <span v-if="r.pct <= 8" class="vp-r-pct-outside">{{ r.pct }}%</span>
          </div>
        </div>
        <div class="vp-footer">
          <span>已投 <strong>{{ totalVotes }}</strong> 票</span>
          <span>{{ remaining }}s</span>
        </div>
      </div>

      <!-- 教室模式：手动计数器 -->
      <div v-else class="vp-manual">
        <div v-for="(opt, i) in options" :key="i" class="vp-manual-opt">
          <span class="vp-m-label">{{ String.fromCharCode(65 + i) }}. {{ opt }}</span>
          <div class="vp-counter">
            <el-button size="small" circle @click="counts[i] = Math.max(0, (counts[i]||0) - 1)">−</el-button>
            <span class="vp-count-val">{{ counts[i] || 0 }}</span>
            <el-button size="small" circle @click="counts[i] = (counts[i]||0) + 1">+</el-button>
          </div>
        </div>
        <div class="vp-footer">
          <span>总计 <strong>{{ totalManual }}</strong> 票</span>
        </div>
      </div>

      <el-button
        type="danger"
        size="large"
        class="vp-end-btn"
        @click="end"
      >
        <el-icon><CircleClose /></el-icon> 结束投票
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onBeforeUnmount } from 'vue'
import { ArrowLeft, TrendCharts, CircleClose } from '@element-plus/icons-vue'
import { startPoll, endPoll } from '@/api/classroom'
import { ElMessage } from 'element-plus'

const props = defineProps({ classId: [String, Number], sceneMode: String, sseConn: Object })
const emit = defineEmits(['back', 'scored'])

const question = ref('')
const options = ref(['完全理解', '基本理解', '不太理解'])
const duration = ref(30)
const anonymous = ref(true)
const starting = ref(false)
const active = ref(false)
const sessionId = ref(null)
const pollResults = ref([])
const remaining = ref(0)
const totalVotes = ref(0)
const counts = ref([0, 0, 0, 0])
let timer = null

const totalManual = computed(() => counts.value.reduce((a, b) => (a||0) + (b||0), 0))

const onPollUpdate = (e) => {
  const d = JSON.parse(e.data)
  if (!d.options) return
  const total = d.totalVotes || 0
  // U2: 就地更新避免数组重建导致动画跳变
  for (let i = 0; i < d.options.length; i++) {
    const count = d.options[i].count || 0
    const pct = total > 0 ? Math.round((count / total) * 100) : 0
    if (pollResults.value[i]) {
      pollResults.value[i].count = count
      pollResults.value[i].pct = pct
    } else {
      pollResults.value.push({ count, pct })
    }
  }
  totalVotes.value = total
}

const start = async () => {
  if (!question.value.trim() || options.value.filter(o => o.trim()).length < 2) {
    return ElMessage.warning('请输入题目和至少2个选项')
  }
  starting.value = true
  try {
    const cleanOpts = options.value.filter(o => o.trim())
    const res = await startPoll({
      classId: props.classId,
      questionText: question.value,
      options: cleanOpts,
      durationSeconds: duration.value,
      anonymous: anonymous.value
    })
    if (res.code === 200) {
      sessionId.value = res.data.sessionId
      active.value = true
      pollResults.value = cleanOpts.map(() => ({ count: 0, pct: 0 }))
      totalVotes.value = 0
      if (props.sceneMode === 'LAB') {
        remaining.value = duration.value
        if (props.sseConn) {
          props.sseConn.removeEventListener('poll:update', onPollUpdate)
          props.sseConn.addEventListener('poll:update', onPollUpdate)
        }
        timer = setInterval(() => {
          remaining.value--
          if (remaining.value <= 0) end()
        }, 1000)
      }
    }
  } finally { starting.value = false }
}

const end = async () => {
  clearInterval(timer)
  if (props.sseConn) { props.sseConn.removeEventListener('poll:update', onPollUpdate) }
  if (sessionId.value) {
    try {
      const res = await endPoll(
        sessionId.value,
        props.sceneMode === 'CLASSROOM' ? counts.value.filter(c => c != null) : null
      )
      if (res.code === 200) {
        const d = res.data
        if (d && d.options) {
          const total = d.totalVotes || 0
          pollResults.value = d.options.map(o => ({
            count: o.count || 0,
            pct: total > 0 ? Math.round(((o.count || 0) / total) * 100) : 0
          }))
          totalVotes.value = total
        }
        emit('scored')
      }
    } catch { /* */ }
  } else if (props.sceneMode === 'CLASSROOM') {
    emit('scored')
  }
  active.value = false
  question.value = ''
  counts.value = [0, 0, 0, 0]
}

onBeforeUnmount(() => {
  clearInterval(timer)
  if (props.sseConn) { props.sseConn.removeEventListener('poll:update', onPollUpdate) }
})
</script>

<style scoped lang="scss">
.vote-panel { display: flex; flex-direction: column; height: 100%; }

.vp-header {
  display: flex; align-items: center; gap: 10px;
  padding-bottom: 14px; border-bottom: 0.5px solid var(--border-light); margin-bottom: var(--spacing-md);
}

.vp-title {
  font-size: var(--fs-lg); font-weight: 700; flex: 1;
  display: flex; align-items: center; gap: var(--spacing-xs);
  color: var(--text-primary);
}

// 设置态
.vp-setup { flex: 1; display: flex; flex-direction: column; gap: 18px; }

.vp-options { display: flex; flex-direction: column; gap: 10px; }
.vp-opt-label { font-size: var(--fs-sm); font-weight: 600; color: var(--text-secondary); }
.vp-opt-row { display: flex; align-items: center; gap: 10px; }
.vp-opt-letter { font-weight: 700; width: 26px; font-size: var(--fs-md); color: var(--text-regular); }

.vp-settings { display: flex; align-items: center; gap: 14px; font-size: var(--fs-sm); color: var(--text-regular); }

.vp-start-btn { width: 100%; font-size: var(--fs-lg); padding: var(--spacing-md); border-radius: var(--radius-lg); height: auto; }

// 进行态
.vp-active { flex: 1; display: flex; flex-direction: column; }

.vp-question {
  font-size: var(--fs-xl); font-weight: 700; padding: 22px;
  background: var(--bg-section); border-radius: var(--radius-lg);
  border: 0.5px solid var(--border-light); margin-bottom: var(--spacing-lg);
  line-height: 1.6; color: var(--text-primary);
}

// 在线结果
.vp-online { flex: 1; }
.vp-result-row { margin-bottom: 18px; }
.vp-r-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.vp-r-opt { font-size: var(--fs-md); font-weight: 500; color: var(--text-regular); }
.vp-r-count { font-size: var(--fs-sm); font-weight: 600; color: var(--text-primary); }

.vp-r-bar {
  height: 32px;
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
  overflow: hidden;
  position: relative;
}

.vp-r-fill {
  height: 100%;
  background: var(--primary-color);
  border-radius: var(--radius-sm);
  transition: width 0.4s ease;
  display: flex; align-items: center; justify-content: flex-end;
  min-width: 0;
}

.vp-r-pct {
  font-size: var(--fs-xs); font-weight: 700; color: #fff; padding: 0 10px;
}

.vp-r-pct-outside {
  font-size: var(--fs-xs); color: var(--text-secondary); padding-left: var(--spacing-sm); line-height: 32px;
}

// 手动模式
.vp-manual { flex: 1; }
.vp-manual-opt {
  display: flex; justify-content: space-between; align-items: center;
  padding: var(--spacing-md);
  background: var(--bg-section); border-radius: var(--radius-md);
  border: 0.5px solid var(--border-light); margin-bottom: 10px;
}

.vp-m-label { font-size: var(--fs-md); font-weight: 500; color: var(--text-regular); }
.vp-counter { display: flex; align-items: center; gap: 10px; }
.vp-count-val { font-size: var(--fs-2xl); font-weight: 800; min-width: 40px; text-align: center; color: var(--text-primary); }

// 底部
.vp-footer {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 0; font-size: var(--fs-md); color: var(--text-secondary);
  border-top: 0.5px solid var(--border-light); margin-top: var(--spacing-sm);
  strong { color: var(--text-primary); }
}

.vp-end-btn { width: 100%; margin-top: 12px; font-size: var(--fs-md); padding: 14px; border-radius: var(--radius-lg); height: auto; }

@media (max-width: 768px) {
  .vp-header { flex-wrap: wrap; }
}
</style>

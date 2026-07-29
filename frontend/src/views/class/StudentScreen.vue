<template>
  <div class="student-screen">
    <div class="sss-header">
      <span class="sss-header-title"><el-icon><Monitor /></el-icon> 课堂互动</span>
      <span class="sss-score">本堂 +{{ sessionScore }}分</span>
    </div>

    <!-- U4: 断连提示（R112: 区分状态 + 显示当前模式） -->
    <div v-if="sseStatus === 'error' || sseStatus === 'reconnecting'" class="sss-disconnect">
      <el-icon><WarningFilled /></el-icon>
      <template v-if="pollMode">已切换到轮询模式（SSE重连中...）</template>
      <template v-else-if="sseStatus === 'reconnecting'">
        连接已断开，正在重连({{ pollFailCount }}/3)...
      </template>
      <template v-else>连接已断开，正在重连...</template>
    </div>
    <!-- 轮询模式提示 -->
    <div v-if="pollMode && sseStatus === 'connected'" class="sss-poll-recovered">
      <el-icon><CircleCheck /></el-icon> SSE已恢复，切回实时模式
    </div>

    <!-- 等待态 -->
    <div v-if="state === 'waiting'" class="sss-state sss-waiting">
      <div class="sssw-icon">
        <el-icon class="sssw-big-icon"><Monitor /></el-icon>
      </div>
      <div class="sssw-text">等待教师发起互动...</div>
      <div class="sssw-hint">教师发起后会在此显示</div>
    </div>

    <!-- 观战态（R112v3）：全班可见题目，仅目标学生可作答 -->
    <div v-else-if="state === 'watching'" class="sss-state sss-quizzed">
      <div class="sssq-banner watching">
        <el-icon><View /></el-icon> {{ quizTargetName }} 正在作答...
      </div>
      <div class="sssq-question">{{ questionText }}</div>
      <div v-if="quizOptions.length > 0" class="sss-options">
        <div v-for="(opt, i) in quizOptions" :key="i" class="sss-opt readonly">
          <span class="sss-opt-letter">{{ getOptionLetter(i) }}</span>
          <span class="sss-opt-text">{{ opt }}</span>
        </div>
      </div>
      <div class="sssq-wait">等待 {{ quizTargetName }} 提交答案...</div>
    </div>

    <!-- 被抽中（目标学生） -->
    <div v-else-if="state === 'quizzed'" class="sss-state sss-quizzed">
      <div class="sssq-banner">
        <el-icon><Aim /></el-icon> 你被选中了！
      </div>
      <div class="sssq-question">{{ questionText }}</div>

      <!-- 选择题/判断题：可点击选项 -->
      <div v-if="quizOptions.length > 0" class="sss-options">
        <div
          v-for="(opt, i) in quizOptions"
          :key="i"
          class="sss-opt"
          :class="{ selected: selectedOptionIndex === i }"
          @click="selectQuizOption(i)"
        >
          <span class="sss-opt-letter">{{ getOptionLetter(i) }}</span>
          <span class="sss-opt-text">{{ opt }}</span>
          <el-icon v-if="selectedOptionIndex === i" class="sss-opt-check"><CircleCheck /></el-icon>
        </div>
      </div>

      <!-- 简答/填空题：文本框 -->
      <el-input
        v-else
        v-model="answer"
        type="textarea"
        :rows="3"
        placeholder="在此输入你的回答..."
        size="large"
        class="sss-textarea"
      />

      <el-button
        type="primary"
        size="large"
        :loading="submitting"
        :disabled="quizOptions.length > 0 && selectedOptionIndex === null"
        class="sss-submit-btn"
        @click="submitAnswer"
      >
        提交回答
      </el-button>
      <div v-if="submitted" class="sssq-done">
        <el-icon><CircleCheck /></el-icon> 回答已提交，等待评分...
      </div>
    </div>

    <!-- R112v3: 评分结果 — 全班可见 -->
    <div
      v-if="quizResult"
      class="sss-quiz-result"
      :class="{ correct: quizResultCorrect, wrong: !quizResultCorrect }"
    >
      <el-icon><component :is="quizResultCorrect ? 'CircleCheck' : 'WarningFilled'" /></el-icon>
      {{ quizResult }}
    </div>

    <!-- 抢答 -->
    <div v-else-if="state === 'buzzing'" class="sss-state sss-buzz">
      <div class="sssb-question">{{ questionText }}</div>
      <el-button
        type="warning"
        size="large"
        :loading="buzzing"
        :disabled="buzzClosed"
        class="sssb-btn"
        @click="doBuzz"
      >
        <el-icon><Lightning /></el-icon> 抢答！
      </el-button>
      <div
        v-if="buzzResult"
        class="sssb-result"
        :class="{ won: buzzResult.includes('抢到了'), lost: buzzResult.includes('抢走') }"
      >
        {{ buzzResult }}
      </div>
      <div v-if="buzzClosed && !buzzResult" class="sssb-result lost">抢答已结束</div>
    </div>

    <!-- 投票 -->
    <div v-else-if="state === 'voting'" class="sss-state sss-vote">
      <div class="sssv-question">{{ questionText }}</div>
      <div class="sss-options">
        <div
          v-for="(opt, i) in voteOptions"
          :key="i"
          class="sss-opt"
          :class="{ selected: votedIndex === i }"
          @click="doVote(i)"
        >
          <span class="sss-opt-letter">{{ getOptionLetter(i) }}</span>
          <span class="sss-opt-text">{{ opt }}</span>
          <el-icon v-if="votedIndex === i" class="sss-opt-check"><CircleCheck /></el-icon>
        </div>
      </div>
      <div v-if="votedIndex !== null" class="sssq-done">
        <el-icon><CircleCheck /></el-icon> 已投票，等待结果...
      </div>

      <!-- 投票结束后显示结果 -->
      <div v-if="voteEnded && pollResult" class="sssv-results">
        <div class="sssv-results-title">投票结果</div>
        <div v-for="(r, i) in pollResult.options" :key="i" class="sssv-result-row">
          <span class="sssv-r-label">{{ getOptionLetter(i) }}. {{ voteOptions[i] }}</span>
          <div class="sssv-r-bar">
            <div
              class="sssv-r-fill"
              :style="{ width: calcPct(r.count, pollResult.totalVotes) + '%' }"
            ></div>
          </div>
          <span class="sssv-r-count">{{ r.count }}票</span>
        </div>
        <div class="sssv-r-total">共 {{ pollResult.totalVotes }} 票</div>
      </div>
    </div>

    <!-- 随堂速答 -->
    <div v-else-if="liveQuizState === 'answering' || liveQuizState === 'submitted' || liveQuizState === 'revealed'"
         class="sss-state ssl-live-quiz">
      <template v-if="liveQuizState === 'answering'">
        <div class="sssq-banner">
          <el-icon><Aim /></el-icon> 随堂速答
        </div>
        <div class="sssq-question">{{ liveQuizQuestion }}</div>
        <div v-if="liveQuizOptions.length > 0" class="sss-options">
          <div
            v-for="(opt, i) in liveQuizOptions" :key="i"
            class="sss-opt"
            :class="{ selected: liveQuizSelected === i }"
            @click="liveQuizSelected = i"
          >
            <span class="sss-opt-letter">{{ getOptionLetter(i) }}</span>
            <span class="sss-opt-text">{{ opt }}</span>
            <el-icon v-if="liveQuizSelected === i" class="sss-opt-check"><CircleCheck /></el-icon>
          </div>
        </div>
        <div v-else class="sss-options">
          <div
            v-for="(opt, i) in ['对', '错']" :key="i"
            class="sss-opt"
            :class="{ selected: liveQuizSelected === i }"
            @click="liveQuizSelected = i"
          >
            <span class="sss-opt-text">{{ opt }}</span>
            <el-icon v-if="liveQuizSelected === i" class="sss-opt-check"><CircleCheck /></el-icon>
          </div>
        </div>
        <el-button
          type="primary" size="large" class="sss-submit-btn"
          :loading="liveQuizSubmitting"
          :disabled="liveQuizSelected === null"
          @click="submitLiveAnswer"
        >
          提交答案
        </el-button>
      </template>

      <template v-else-if="liveQuizState === 'submitted'">
        <div class="sssq-done">
          <el-icon><CircleCheck /></el-icon> 答案已提交，等待教师公布结果...
        </div>
      </template>

      <template v-else-if="liveQuizState === 'revealed'">
        <div class="sss-quiz-result" :class="{ correct: liveQuizResult?.correct, wrong: !liveQuizResult?.correct }">
          <el-icon><component :is="liveQuizResult?.correct ? 'CircleCheck' : 'WarningFilled'" /></el-icon>
          <template v-if="liveQuizResult?.correct">
            正确！+{{ liveQuizResult?.scoreEarned }}分
          </template>
          <template v-else>
            正确答案 {{ liveQuizResult?.correctAnswer }}
          </template>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue';
import { useRoute } from 'vue-router';
import { submitQuizAnswer, submitBuzz, submitVote, pollClassState, submitLiveQuizAnswer } from '@/api/classroom';
import { useClassroomSSE } from '@/composables/useClassroomSSE';
import { ElMessage } from 'element-plus';
import { Monitor, Aim, CircleCheck, Lightning, WarningFilled, View } from '@element-plus/icons-vue';

const route = useRoute();
const classId = route.params.id;
const state = ref('waiting'); // waiting | watching | quizzed | buzzing | voting
const sessionScore = ref(0);
const questionText = ref('');
const answer = ref('');
const sessionId = ref(null);
const submitting = ref(false);
const submitted = ref(false);
const buzzing = ref(false);
const buzzClosed = ref(false);
const buzzResult = ref('');
const voteOptions = ref([]);
const votedIndex = ref(null);
const voteEnded = ref(false);
const pollResult = ref(null);

const quizOptions = ref([]);
const selectedOptionIndex = ref(null);
const quizTargetName = ref(''); // R112v3: 被抽中学生姓名（观战态显示）
const quizResult = ref(''); // R112v3: 评分结果文案（全班可见）
const quizResultCorrect = ref(false);

// R112: 短轮询降级
const pollMode = ref(false);
const pollFailCount = ref(0);
// ── 随堂速答 ──
const liveQuizState = ref('idle')  // idle | answering | submitted | revealed
const liveQuizQuestion = ref('')
const liveQuizMode = ref('choice')
const liveQuizOptions = ref([])
const liveQuizSelected = ref(null)
const liveQuizResult = ref(null)   // {correct, correctAnswer, scoreEarned}
const liveQuizSubmitting = ref(false)

let pollTimer = null;
let lastPollSessionId = null;

// R112v3: myStudentId从SSE connected事件获取，100%可靠
const {
  connect: sseConnect,
  on: sseOn,
  onError: sseOnError,
  close: sseClose,
  status: sseStatus,
  myStudentId,
} = useClassroomSSE(classId);

const calcPct = (count, total) => (total > 0 ? Math.round((count / total) * 100) : 0);

const getOptionLetter = (index) => {
  if (index < 26) return String.fromCharCode(65 + index);
  return String(index + 1);
};

const parseOptions = (raw) => {
  if (!raw) return [];
  if (Array.isArray(raw)) return raw;
  try {
    return JSON.parse(raw);
  } catch {
    return [];
  }
};

// R112: 短轮询 — 每2秒查询活动状态
const pollState = async () => {
  try {
    const json = await pollClassState(classId);
    if (json.code !== 200 || !json.data) return;
    const d = json.data;
    if (!d.hasActivity) return;

    // 避免重复处理同一活动
    const sid = d.data?.sessionId;
    if (sid && sid === lastPollSessionId) return;
    lastPollSessionId = sid;

    // 根据活动类型还原学生端状态
    const type = d.sessionType;
    if (type === 'QUIZ') {
      const isTarget = myStudentId.value && d.data?.studentId === myStudentId.value;
      questionText.value = d.data.questionText || '';
      sessionId.value = d.data.sessionId;
      quizTargetName.value = d.data.studentName || '同学';
      quizResult.value = '';
      if (isTarget) {
        // 目标学生 → 可作答
        state.value = 'quizzed';
        answer.value = '';
        submitted.value = false;
        quizOptions.value = parseOptions(d.data.options);
        selectedOptionIndex.value = null;
      } else {
        // 其他学生 → 观战
        state.value = 'watching';
        quizOptions.value = parseOptions(d.data.options);
      }
      quizOptions.value = parseOptions(d.data.options);
      selectedOptionIndex.value = null;
    } else if (type === 'BUZZ') {
      state.value = 'buzzing';
      questionText.value = d.data.questionText || '';
      sessionId.value = d.data.sessionId;
      buzzClosed.value = false;
      buzzResult.value = '';
    } else if (type === 'POLL') {
      state.value = 'voting';
      questionText.value = d.data.questionText || '';
      voteOptions.value = (d.data.pollData?.options || []).map((o) => o.option || o);
      sessionId.value = d.data.sessionId;
      votedIndex.value = null;
      voteEnded.value = false;
      pollResult.value = null;
    }
  } catch {
    /* 轮询静默失败 */
  }
};

const startPolling = () => {
  if (pollMode.value) return;
  pollMode.value = true;
  lastPollSessionId = null;
  pollTimer = setInterval(pollState, 2000);
  pollState(); // 立即查询一次
};

const stopPolling = () => {
  pollMode.value = false;
  lastPollSessionId = null;
  clearInterval(pollTimer);
  pollTimer = null;
};

// R112: 监听SSE状态变化 → 连续失败3次切轮询，恢复后切回
watch(sseStatus, (newStatus) => {
  if (newStatus === 'error' || newStatus === 'reconnecting') {
    pollFailCount.value++;
    if (pollFailCount.value >= 3 && !pollMode.value) {
      startPolling();
    }
  } else if (newStatus === 'connected') {
    pollFailCount.value = 0;
    if (pollMode.value) {
      stopPolling();
      ElMessage.success('实时连接已恢复');
      // 短暂显示恢复提示后自动消失
      setTimeout(() => {
        /* 恢复提示自动消失 */
      }, 3000);
    }
  }
});

onMounted(async () => {
  await sseConnect();
  // R112v3: quiz:selected全班广播 → 目标学生作答，其余学生观战
  sseOn('quiz:selected', (e) => {
    const d = JSON.parse(e.data);
    const isTarget = myStudentId.value && d.studentId === myStudentId.value;
    questionText.value = d.questionText || '';
    sessionId.value = d.sessionId;
    quizTargetName.value = d.studentName || '同学';
    quizResult.value = '';
    quizOptions.value = parseOptions(d.options);
    if (isTarget) {
      state.value = 'quizzed';
      answer.value = '';
      submitted.value = false;
      selectedOptionIndex.value = null;
    } else {
      state.value = 'watching';
    }
  });
  // R112v3: quiz:result全班可见
  sseOn('quiz:result', (e) => {
    const d = JSON.parse(e.data);
    sessionScore.value += d.scoreEarned || 0;
    const name = quizTargetName.value || '同学';
    if (d.result > 0) {
      quizResult.value = `${name} 回答正确 +${d.scoreEarned}分`;
      quizResultCorrect.value = true;
    } else {
      quizResult.value = `${name} 回答错误，已记录错题本`;
      quizResultCorrect.value = false;
    }
    setTimeout(() => {
      state.value = 'waiting';
      quizResult.value = '';
    }, 3000);
  });
  sseOn('buzz:start', (e) => {
    const d = JSON.parse(e.data);
    state.value = 'buzzing';
    questionText.value = d.questionText;
    sessionId.value = d.sessionId;
    buzzClosed.value = false;
    buzzResult.value = '';
  });
  sseOn('buzz:end', (e) => {
    const d = JSON.parse(e.data);
    buzzClosed.value = true;
    if (!buzzResult.value) {
      buzzResult.value = d.winnerStudentId ? '已被其他同学抢走' : '无人抢答';
    }
    setTimeout(() => {
      state.value = 'waiting';
    }, 3000);
  });
  sseOn('poll:start', (e) => {
    const d = JSON.parse(e.data);
    state.value = 'voting';
    questionText.value = d.questionText;
    voteOptions.value = (d.pollData?.options || []).map((o) => o.option || o);
    sessionId.value = d.sessionId;
    votedIndex.value = null;
    voteEnded.value = false;
    pollResult.value = null;
  });
  sseOn('poll:end', (e) => {
    const d = JSON.parse(e.data);
    voteEnded.value = true;
    pollResult.value = d.pollData;
    setTimeout(() => {
      state.value = 'waiting';
    }, 5000);
  });
  sseOn('live-quiz:start', (e) => {
    const d = JSON.parse(e.data)
    liveQuizState.value = 'answering'
    liveQuizQuestion.value = d.questionText || ''
    liveQuizMode.value = d.mode || 'choice'
    liveQuizOptions.value = d.options || []
    liveQuizSelected.value = null
    liveQuizResult.value = null
  })
  sseOn('live-quiz:result', (e) => {
    const d = JSON.parse(e.data)
    liveQuizState.value = 'revealed'
    // 使用提交时本地缓存的结果（已含 correct/correctAnswer/scoreEarned）
    if (liveQuizResult.value === null) {
      liveQuizResult.value = { correct: false, correctAnswer: (d.results?.correctAnswer) || '', scoreEarned: 0 }
    }
    setTimeout(() => { liveQuizState.value = 'waiting' }, 5000)
  })
  sseOn('live-quiz:pick', (e) => {
    const d = JSON.parse(e.data)
    ElNotification({
      title: '👤 被点名',
      message: `${d.studentName} 被老师点到`,
      type: 'success',
      duration: 3000,
    })
  })
  sseOn('score:update', (e) => {
    const d = JSON.parse(e.data);
    sessionScore.value += d.scoreEarned || 0;
  });
  sseOn('task:started', (e) => {
    const d = JSON.parse(e.data);
    ElMessage.info(`课堂任务已启动：${d.title || '任务'}`);
  });
});

const selectQuizOption = (i) => {
  if (submitted.value) return;
  selectedOptionIndex.value = i;
  answer.value = getOptionLetter(i) + '. ' + quizOptions.value[i];
};

const submitAnswer = async () => {
  if (quizOptions.value.length > 0) {
    if (selectedOptionIndex.value === null) return ElMessage.warning('请选择一个选项');
  } else {
    if (!answer.value.trim()) return ElMessage.warning('请输入回答');
  }
  submitting.value = true;
  try {
    await submitQuizAnswer({ sessionId: sessionId.value, answerText: answer.value });
    submitted.value = true;
    ElMessage.success('回答已提交');
  } catch {
    ElMessage.error('提交失败');
  } finally {
    submitting.value = false;
  }
};

const doBuzz = async () => {
  if (buzzClosed.value) return;
  buzzing.value = true;
  try {
    const res = await submitBuzz({ sessionId: sessionId.value });
    if (res.code === 200 && res.data) {
      if (res.data.won) {
        buzzResult.value = '你抢到了！等待老师评分...';
        buzzClosed.value = true;
      } else {
        buzzResult.value = res.data.message || '已被其他同学抢走';
        buzzClosed.value = true;
      }
    }
  } catch {
    buzzResult.value = '网络异常，请重试';
  } finally {
    buzzing.value = false;
  }
};

const doVote = async (i) => {
  if (votedIndex.value !== null) return;
  try {
    await submitVote({ sessionId: sessionId.value, optionIndex: i });
    votedIndex.value = i;
  } catch {
    /* */
  }
};

async function submitLiveAnswer() {
  if (liveQuizSelected.value === null) return ElMessage.warning('请选择一个选项')
  liveQuizSubmitting.value = true
  try {
    const res = await submitLiveQuizAnswer({
      sessionId: sessionId.value,
      answer: liveQuizMode.value === 'choice' ? getOptionLetter(liveQuizSelected.value) : (liveQuizSelected.value === 0 ? '对' : '错'),
    })
    if (res.code === 200) {
      liveQuizState.value = 'submitted'
      liveQuizResult.value = {
        correct: res.data.correct,
        correctAnswer: res.data.correctAnswer,
        scoreEarned: res.data.scoreEarned,
      }
      sessionScore.value += res.data.scoreEarned || 0
    }
  } catch {
    ElMessage.error('提交失败')
  } finally {
    liveQuizSubmitting.value = false
  }
}

onBeforeUnmount(() => {
  stopPolling();
  sseClose();
});
</script>

<style scoped lang="scss">
.student-screen {
  max-width: 560px;
  margin: 0 auto;
  padding: var(--spacing-lg) var(--spacing-lg);
}

.sss-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
  font-size: var(--fs-lg);
  font-weight: 700;
  color: var(--text-primary);
}

.sss-header-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

.sss-score {
  font-size: var(--fs-md);
  font-weight: 600;
  color: var(--el-color-success);
}

.sss-disconnect {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px;
  margin-bottom: 16px;
  background: var(--el-color-warning-light-9);
  border: 1px solid var(--el-color-warning-light-5);
  border-radius: var(--radius-md);
  color: var(--el-color-warning);
  font-size: var(--fs-sm);
  font-weight: 600;
}

.sss-poll-recovered {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px;
  margin-bottom: 16px;
  background: var(--el-color-success-light-9);
  border: 1px solid var(--el-color-success-light-5);
  border-radius: var(--radius-md);
  color: var(--el-color-success);
  font-size: var(--fs-sm);
  font-weight: 600;
}

// 状态容器
.sss-state {
  display: flex;
  flex-direction: column;
}

// ===== 等待态 =====
.sss-waiting {
  text-align: center;
  padding: 80px 0;
}

.sssw-icon {
  margin-bottom: var(--spacing-md);
}

.sssw-big-icon {
  font-size: 72px;
  color: var(--text-disabled);
}

.sssw-text {
  font-size: var(--fs-lg);
  color: var(--text-secondary);
  margin-top: var(--spacing-md);
}

.sssw-hint {
  font-size: var(--fs-sm);
  color: var(--text-disabled);
  margin-top: var(--spacing-xs);
}

// ===== 被抽中 =====
.sssq-banner {
  font-size: 22px;
  font-weight: 800;
  color: var(--el-color-warning);
  text-align: center;
  margin-bottom: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);

  &.watching {
    color: var(--primary-color);
    font-size: var(--fs-lg);
    font-weight: 600;
  }
}

.sssq-question {
  font-size: var(--fs-lg);
  padding: 20px;
  background: var(--bg-section);
  border-radius: var(--radius-lg);
  margin-bottom: 18px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--text-primary);
  border: 0.5px solid var(--border-light);
}

// 选项（抽问 + 投票共用）
.sss-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: var(--spacing-md);
}

.sss-opt {
  padding: var(--spacing-md);
  border: 0.5px solid var(--border-color);
  border-radius: var(--radius-lg);
  cursor: pointer;
  font-size: var(--fs-lg);
  display: flex;
  align-items: center;
  gap: 10px;
  transition:
    border-color var(--transition-base),
    background var(--transition-base);
  color: var(--text-primary);

  &:hover {
    border-color: var(--primary-color);
    background: var(--primary-light);
  }

  &.selected {
    border-color: var(--primary-color);
    background: var(--primary-light);
  }
}

.sss-opt-letter {
  font-weight: 800;
  color: var(--primary-color);
  width: 24px;
  flex-shrink: 0;
}

.sss-opt-text {
  flex: 1;
}

.sss-opt-check {
  color: var(--primary-color);
  font-size: var(--fs-lg);
  flex-shrink: 0;
}

// R112v3: 只读选项（观战态）
.sss-opt.readonly {
  cursor: default;
  opacity: 0.7;
  &:hover {
    border-color: var(--border-color);
    background: transparent;
  }
}

.sssq-wait {
  text-align: center;
  color: var(--text-secondary);
  font-size: var(--fs-sm);
  margin-top: var(--spacing-lg);
}

// R112v3: 评分结果横幅（全班可见）
.sss-quiz-result {
  margin-top: 20px;
  padding: 14px 20px;
  border-radius: var(--radius-lg);
  text-align: center;
  font-size: var(--fs-md);
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;

  &.correct {
    background: var(--el-color-success-light-9);
    color: var(--el-color-success);
    border: 0.5px solid var(--el-color-success-light-5);
  }
  &.wrong {
    background: var(--el-color-warning-light-9);
    color: var(--el-color-warning);
    border: 0.5px solid var(--el-color-warning-light-5);
  }
}

.sss-textarea {
  margin-bottom: var(--spacing-md);
}

.sss-submit-btn {
  width: 100%;
  margin-top: var(--spacing-md);
}

.sssq-done {
  text-align: center;
  margin-top: var(--spacing-md);
  color: var(--el-color-success);
  font-weight: 600;
  font-size: var(--fs-md);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-xs);
}

// ===== 抢答 =====
.sssb-question {
  font-size: var(--fs-xl);
  font-weight: 700;
  text-align: center;
  margin-bottom: 28px;
  line-height: 1.6;
  color: var(--text-primary);
}

.sssb-btn {
  width: 100%;
  height: 72px;
  font-size: var(--fs-2xl);
  border-radius: var(--radius-xl);
}

.sssb-result {
  text-align: center;
  margin-top: var(--spacing-md);
  font-size: var(--fs-lg);
  font-weight: 600;
  color: var(--text-secondary);

  &.won {
    color: var(--el-color-success);
  }
  &.lost {
    color: var(--el-color-warning);
  }
}

// ===== 投票 =====
.sssv-question {
  font-size: var(--fs-lg);
  font-weight: 700;
  margin-bottom: 18px;
  line-height: 1.5;
  color: var(--text-primary);
}

.sssv-results {
  margin-top: 20px;
  padding: 20px;
  background: var(--bg-section);
  border-radius: var(--radius-lg);
  border: 0.5px solid var(--border-light);
}

.sssv-results-title {
  font-size: var(--fs-md);
  font-weight: 700;
  margin-bottom: var(--spacing-md);
  color: var(--text-primary);
}

.sssv-result-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  font-size: var(--fs-sm);
}

.sssv-r-label {
  width: 200px;
  flex-shrink: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--text-regular);
}

.sssv-r-bar {
  flex: 1;
  height: 20px;
  background: var(--bg-secondary);
  border-radius: var(--radius-xs);
  overflow: hidden;
}

.sssv-r-fill {
  height: 100%;
  background: var(--primary-color);
  border-radius: var(--radius-xs);
  min-width: 2px;
  transition: width 0.4s ease;
}

.sssv-r-count {
  width: 40px;
  text-align: right;
  font-weight: 600;
  color: var(--text-secondary);
  flex-shrink: 0;
}

.sssv-r-total {
  text-align: center;
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  margin-top: var(--spacing-sm);
}

@media (max-width: 768px) {
  .student-screen {
    padding: var(--spacing-md);
  }

  .sssv-r-label {
    width: 100px;
  }
}
</style>

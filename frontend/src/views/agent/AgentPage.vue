<template>
  <div class="agent-page">
    <div class="agent-container">
      <SessionList
        v-if="!isMobile"
        :sessions="sessions"
        :current-session-id="currentSessionId"
        @select="selectSession"
        @new="handleNewChat"
        @delete="handleDeleteSession"
      />
      <ChatPanel
        :title="currentModeLabel"
        :mode-label="currentModeLabel"
        :modes="availableModes"
        :current-mode="agentMode"
        :messages="chatMessages"
        :is-streaming="isStreaming"
        :tools-visible="toolsVisible"
        :suggestions="currentSuggestions"
        :error="showError"
        :feedback-state="feedbackState"
        :is-mobile="isMobile"
        :thinking="thinkingText"
        api-status="connected"
        api-model-name="DeepSeek V4 Pro"
        :daily-usage="dailyUsage"
        :daily-limit="dailyLimit"
        :has-custom-key="hasCustomKey"
        @send="handleSend"
        @new-chat="handleNewChat"
        @feedback="handleFeedback"
        @mode-change="switchMode"
        @toggle-sessions="toggleMobileSessions"
        @stop="handleStop"
      />
    </div>
    <!-- Mobile session drawer -->
    <el-drawer
      v-model="mobileDrawerVisible"
      size="260px"
      direction="ltr"
      :with-header="false"
      destroy-on-close
    >
      <SessionList
        :sessions="sessions"
        :current-session-id="currentSessionId"
        @select="
          (id) => {
            selectSession(id);
            mobileDrawerVisible = false;
          }
        "
        @new="
          () => {
            handleNewChat();
            mobileDrawerVisible = false;
          }
        "
        @delete="handleDeleteSession"
      />
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useUserStore } from '@/stores/user';
import { useIsMobile } from '@/composables/useIsMobile';
import { useAgentModes } from '@/composables/useAgentModes';
import SessionList from '@/components/ai/SessionList.vue';
import ChatPanel from '@/components/ai/ChatPanel.vue';
import { useAgentChat, createToolCallbacks } from '@/composables/useAgentChat';
import { getApiKeys } from '@/api/apiKeys';
import { deleteSession, getDailyUsage } from '@/api/agent';
import { ElMessageBox, ElMessage } from 'element-plus';

const userStore = useUserStore();
const { isMobile } = useIsMobile();
const { availableModes, getModeLabel } = useAgentModes();
const mobileDrawerVisible = ref(false);
const toolsVisible = ref([]);
const thinkingText = ref('');
const hasCustomKey = ref(false);
const dailyUsage = ref(0);
const dailyLimit = ref(20);

const agentMode = ref(availableModes.value[0]?.value || 'STUDY_BUDDY');

const {
  sessions,
  messages,
  isStreaming,
  currentSessionId,
  loadSessions,
  loadSessionMessages,
  sendMessage,
  resetMessages,
  stopGeneration,
  showError,
  submitFeedback,
  feedbackState,
  pendingConfirm,
  confirmWrite,
} = useAgentChat(agentMode);

onMounted(async () => {
  loadSessions();
  try {
    const res = await getApiKeys();
    if (res.code === 200 && Array.isArray(res.data)) {
      hasCustomKey.value = res.data.some((k) => k.active !== false);
    }
  } catch (e) {
    /* BYOK 查询非关键路径 */
  }
  // 获取每日用量
  try {
    const usageRes = await getDailyUsage();
    if (usageRes.code === 200 && usageRes.data) {
      dailyUsage.value = usageRes.data.used || 0;
      dailyLimit.value = usageRes.data.limit || 20;
    }
  } catch (e) {
    /* 用量查询非关键路径 */
  }
});

const chatMessages = computed(() => messages.value);
const currentModeLabel = computed(() => getModeLabel(agentMode.value));

// G-4: 写操作确认弹窗
watch(pendingConfirm, (val) => {
  if (!val) return
  const tools = Array.isArray(val.tools) ? val.tools : []
  const toolNames = tools.map(t => {
    const map = { teaching_create_task: '创建任务', teaching_send_notification: '发送通知', teaching_generate_ppt: '生成PPT' }
    return map[t] || t
  }).join('、')
  ElMessageBox.confirm(
    `AI 将要${toolNames ? '执行以下操作：' + toolNames : '执行写操作'}，是否允许？`,
    '操作确认',
    { confirmButtonText: '允许', cancelButtonText: '取消', type: 'warning', closeOnClickModal: false }
  ).then(() => confirmWrite(true)).catch(() => confirmWrite(false))
})

const currentSuggestions = computed(() => {
  switch (agentMode.value) {
    case 'LESSON_PREP':
      return ['出一份随堂练习', '分析这次考试', '搜索网络知识点'];
    case 'STUDY_BUDDY':
      return ['帮我讲解这道题', '我上次错了哪些', '出类似题练练'];
    case 'ANALYTICS':
      return ['这个班考试怎么样', '谁需要重点关注', '知识点掌握趋势'];
    default:
      return [];
  }
});

function selectSession(id) {
  currentSessionId.value = id;
  loadSessionMessages(id);
}

async function handleDeleteSession(sessionId) {
  try {
    const res = await deleteSession(sessionId)
    if (res.code === 200) {
      ElMessage.success('已删除')
      // 如果删除的是当前会话，重置消息
      if (sessionId === currentSessionId.value) {
        resetMessages()
      }
      // 刷新会话列表
      loadSessions()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch {
    ElMessage.error('删除失败')
  }
}

function switchMode(val) {
  agentMode.value = val;
  resetMessages();
  clearToolState();
}

function handleNewChat() {
  resetMessages();
  clearToolState();
}

function toggleMobileSessions() {
  mobileDrawerVisible.value = !mobileDrawerVisible.value;
}

function clearToolState() {
  toolsVisible.value = [];
  thinkingText.value = '';
}

function handleFeedback(ev) {
  submitFeedback(
    ev.messageIndex,
    ev.rating,
    '',
    '',
    ev.userQuestion,
    ev.answerSnippet,
    ev.toolsUsed
  );
}

function handleStop() {
  stopGeneration();
}

function handleSend(text) {
  clearToolState();
  thinkingText.value = '';
  const { onToolStart, onToolEnd } = createToolCallbacks(toolsVisible, (t) => { thinkingText.value = t });
  sendMessage(
    text,
    onToolStart,
    onToolEnd,
    () => { thinkingText.value = '' },
    () => { thinkingText.value = '' },
    (err) => { console.error('Agent error:', err) },
    (thinkMsg) => { thinkingText.value = thinkMsg }
  );
}
</script>

<style scoped>
/* ══════════════════════════════════════════════
   AgentPage — 外层容器 · 两栏卡片布局
   ══════════════════════════════════════════════ */

.agent-page {
  padding: 24px 0;
  height: calc(100vh - 60px);
  box-sizing: border-box;
  background: var(--bg-page);
}

/* ── 白色卡片容器 ── */
.agent-container {
  display: flex;
  background: var(--bg-card);
  border: 0.5px solid var(--border-base);
  border-radius: 12px;
  overflow: hidden;
  height: calc(100vh - 108px);
}

/* ── 大屏幕居中约束（防止左右拉太长） ── */
@media (min-width: 1441px) {
  .agent-container {
    max-width: 1400px;
    margin: 0 auto;
  }
}

/* ── 移动端适配 ── */
@media (max-width: 768px) {
  .agent-page {
    padding: 0;
    height: calc(100vh - 60px - 48px);
  }
  .agent-container {
    margin: 0;
    border: none;
    border-radius: 0;
    height: calc(100vh - 60px);
  }
}
</style>

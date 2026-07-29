<template>
  <div>
    <AiFloatButton :active="drawerVisible" @click="toggleDrawer" />
    <el-drawer
      v-model="drawerVisible"
      :title="currentModeLabel"
      size="480px"
      direction="rtl"
      destroy-on-close
    >
      <template #header>
        <div class="agent-header">
          <el-select
            v-model="agentMode"
            size="small"
            style="width: 130px"
            @change="switchMode"
          >
            <el-option
              v-for="m in availableModes"
              :key="m.value"
              :label="m.label"
              :value="m.value"
            />
          </el-select>
          <span class="agent-title">{{ currentModeLabel }}</span>
        </div>
      </template>

      <div class="agent-drawer-body">
        <ChatPanel
          :title="currentModeLabel"
          :mode-label="currentModeLabel"
          :modes="availableModes"
          :current-mode="agentMode"
          :messages="chatMessages"
          :is-streaming="isStreaming"
          :tools-visible="toolsVisible"
          :suggestions="currentSuggestions"
          :thinking="thinkingText"
          :error="showError"
          :feedback-state="feedbackState"
          :is-mobile="false"
          api-status="connected"
          api-model-name="DeepSeek V4 Pro"
          @send="handleSend"
          @new-chat="handleNewChat"
          @feedback="handleFeedback"
          @mode-change="handleModeChange"
        />
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { useUserStore } from '@/stores/user';
import { useAgentModes } from '@/composables/useAgentModes';
import AiFloatButton from './AiFloatButton.vue';
import ChatPanel from './ChatPanel.vue';
import { useAgentChat, createToolCallbacks } from '@/composables/useAgentChat';
import { ElMessageBox } from 'element-plus';

const props = defineProps({
  defaultMode: { type: String, default: '' },
  context: { type: String, default: '' },
});

const userStore = useUserStore();
const { availableModes, getModeLabel } = useAgentModes();
const drawerVisible = ref(false);
const thinkingText = ref('');
const toolsVisible = ref([]);

const agentMode = ref(props.defaultMode || availableModes.value[0]?.value || 'STUDY_BUDDY');

const {
  messages,
  isStreaming,
  sendMessage,
  resetMessages,
  showError,
  submitFeedback,
  feedbackState,
  pendingConfirm,
  confirmWrite,
} = useAgentChat(agentMode);

const chatMessages = computed(() => messages.value);
const currentModeLabel = computed(() => getModeLabel(agentMode.value));
const currentSuggestions = computed(() => {
  switch (agentMode.value) {
    case 'LESSON_PREP':
      return ['帮我出一份随堂练习', '帮我写一份教案', '搜索知识点'];
    case 'STUDY_BUDDY':
      return ['这道题我不会做', '我上次错了哪些', '帮我出一道类似的题'];
    case 'ANALYTICS':
      return ['这个班这次考试怎么样', '哪些学生需要重点关注', '知识点掌握趋势'];
    default:
      return [];
  }
});

// 写操作确认弹窗
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

function toggleDrawer() {
  drawerVisible.value = !drawerVisible.value;
}

function switchMode() {
  resetMessages();
  toolsVisible.value = [];
  thinkingText.value = '';
}

function handleNewChat() {
  resetMessages();
  toolsVisible.value = [];
  thinkingText.value = '';
}

function handleFeedback({ messageIndex, rating, userQuestion, answerSnippet, toolsUsed }) {
  submitFeedback(messageIndex, rating, '', '', userQuestion, answerSnippet, toolsUsed);
}

function handleModeChange(val) {
  agentMode.value = val;
  switchMode();
}

function handleSend(text) {
  const msg = props.context ? `${props.context}\n${text}` : text;
  toolsVisible.value = [];
  thinkingText.value = '';

  const { onToolStart, onToolEnd } = createToolCallbacks(toolsVisible, (t) => { thinkingText.value = t });
  sendMessage(
    msg,
    onToolStart,
    onToolEnd,
    () => { thinkingText.value = '' },
    () => { thinkingText.value = '' },
    (err) => { thinkingText.value = `错误: ${err}`; console.error('Agent error:', err) },
    (thinkMsg) => { thinkingText.value = thinkMsg }
  );
}
</script>

<style scoped>
.agent-header {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}
.agent-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
}
.agent-drawer-body {
  height: 100%;
  display: flex;
  flex-direction: column;
}
</style>

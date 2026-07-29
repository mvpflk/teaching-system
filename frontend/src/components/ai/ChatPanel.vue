<template>
  <div class="chat-panel">
    <!-- ── Header ── -->
    <div class="chat-header">
      <div class="header-left">
        <el-button
          v-if="isMobile"
          text
          size="small"
          @click="$emit('toggleSessions')"
        >
          <el-icon><List /></el-icon>
        </el-button>
        <span class="header-title">{{ title }}</span>
        <span v-if="modeLabel" class="header-badge">{{ modeLabel }}</span>
      </div>
      <div class="header-right">
        <el-select
          v-if="modes.length"
          v-model="currentModeModel"
          size="small"
          style="width: 120px"
        >
          <el-option
            v-for="m in modes"
            :key="m.value"
            :label="m.label"
            :value="m.value"
          />
        </el-select>
        <el-button text size="small" @click="handleNewChat">＋ 新对话</el-button>
      </div>
    </div>

    <!-- ── 消息列表 ── -->
    <div ref="msgContainer" class="chat-messages" @scroll="onScroll">
      <!-- 错误提示 -->
      <div v-if="error" class="chat-error">
        <el-alert
          :title="error"
          type="error"
          show-icon
          :closable="false"
        />
      </div>

      <!-- 空状态（首屏） -->
      <div v-if="messages.length === 0 && !isStreaming && !error" class="chat-empty">
        <div class="empty-icon-wrap">
          <span class="empty-icon">🤖</span>
        </div>
        <h2 class="empty-title">有什么可以帮你的？</h2>
        <p class="empty-desc">备课、出题、分析成绩、搜知识点——随时问我</p>
        <div v-if="suggestions.length" class="empty-suggestions">
          <button
            v-for="(s, i) in suggestions"
            :key="i"
            class="suggestion-pill"
            @click="handleSuggestion(s)"
          >
            {{ s }}
          </button>
        </div>
      </div>

      <!-- 消息列表 -->
      <div
        v-for="(msg, idx) in messages"
        :key="idx"
        :class="['chat-msg', msg.role]"
        @mouseenter="hoveredIdx = idx"
        @mouseleave="hoveredIdx = null"
      >
        <!-- 头像 -->
        <div class="msg-avatar">
          <div v-if="msg.role === 'user'" class="avatar-user">我</div>
          <div v-else class="avatar-ai">🤖</div>
        </div>

        <div class="msg-main">
          <!-- 气泡 -->
          <div class="msg-bubble">
            <ContentRenderer v-if="msg._structured" :content="msg.content" />
            <div v-else class="msg-text" v-html="renderMarkdown(msg.content)" />

            <!-- UX #1 消息复制按钮（AI 气泡 hover 时出现；移动端常显） -->
            <button
              v-if="
                msg.role === 'assistant' && !msg._structured && (isMobile || hoveredIdx === idx)
              "
              class="msg-copy-btn"
              title="复制"
              @click.stop="copyMessage(msg)"
            >
              📋
            </button>
          </div>

          <!-- 反馈按钮 -->
          <div v-if="msg.role === 'assistant'" class="msg-feedback">
            <button
              :class="['fb-btn', { active: feedbackState[idx] === 5 }]"
              title="有帮助"
              @click="handleFeedback(idx, 5, msg)"
            >
              👍
            </button>
            <button
              :class="['fb-btn', { active: feedbackState[idx] === 1 }]"
              title="没帮助"
              @click="handleFeedback(idx, 1, msg)"
            >
              👎
            </button>
          </div>

          <!-- 时间戳 -->
          <div class="msg-time">{{ fmtTime(msg.time) }}</div>

          <!-- UX #3 流式输出增强：脉冲条 + 停止按钮 -->
          <div v-if="isStreaming && isLastAssistantMsg(idx)" class="streaming-indicator">
            <div class="streaming-bar"></div>
            <button class="streaming-stop-btn" @click="$emit('stop')">停止生成</button>
          </div>
        </div>
      </div>

      <!-- T8: 流式等待/思考指示（用户发送后、首段回复前的间隙） -->
      <div v-if="isStreaming && !hasAssistantMessage" class="thinking-indicator">
        <span class="thinking-dot"></span>
        <span class="thinking-dot"></span>
        <span class="thinking-dot"></span>
        <span class="thinking-label">{{ thinking || 'AI 正在思考…' }}</span>
      </div>

      <!-- UX #2 滚动到底部按钮 -->
      <Transition name="fade">
        <button v-if="showScrollBtn" class="scroll-bottom-btn" @click="scrollToBottom()">
          ↓ 回到最新
        </button>
      </Transition>

      <!-- 新消息提示横条（与滚动按钮互斥） -->
      <Transition name="fade">
        <div
          v-if="!showScrollBtn && showNewMsgNotice"
          class="new-msg-notice"
          @click="scrollToBottom()"
        >
          📩 新消息 ↓
        </div>
      </Transition>

      <!-- ── 工具调用状态栏（独立于消息，即时显示） ── -->
      <div v-if="toolsVisible.length" class="tools-status-bar">
        <span
          v-for="t in toolsVisible"
          :key="t.name"
          :class="['tool-badge', 'tool-' + t.status]"
        >
          <span v-if="t.status === 'running'" class="tool-spinner"></span>
          <span v-else-if="t.status === 'done'" class="tool-check">✓</span>
          <span v-else class="tool-cross">✗</span>
          {{ t.label || t.name }}
        </span>
      </div>
    </div>

    <!-- ── 输入区 ── -->
    <div class="chat-input-area">
      <div class="input-row">
        <!-- UX #5 输入框自动伸缩 -->
        <el-input
          ref="inputRef"
          v-model="inputText"
          type="textarea"
          :autosize="{ minRows: isMobile ? 1 : 2, maxRows: 4 }"
          :placeholder="currentPlaceholder"
          :disabled="isStreaming"
          @keydown.enter.exact.prevent="handleSend"
        />
        <el-button
          class="send-btn"
          :class="{ 'is-streaming': isStreaming }"
          type="primary"
          :disabled="!inputText.trim() || isStreaming"
          @click="handleSend"
        >
          ↵
        </el-button>
      </div>

      <!-- 快捷操作条 -->
      <div class="quick-actions">
        <span
          v-for="act in visibleQuickActions"
          :key="act.label"
          class="quick-action"
          @click="handleSuggestion(act.prompt)"
        >{{ act.icon }} {{ act.label }}</span>
      </div>

      <!-- API 状态栏 -->
      <ApiStatusBar
        :status="apiStatus"
        :model-name="apiModelName"
        :daily-usage="dailyUsage"
        :daily-limit="dailyLimit"
        :has-custom-key="hasCustomKey"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue';
import { renderMarkdown } from '@/utils/markdown';
import ContentRenderer from '@/components/ai/ContentRenderer.vue';
import ApiStatusBar from '@/components/ai/ApiStatusBar.vue';
import { ElMessageBox, ElMessage } from 'element-plus';
import { List } from '@element-plus/icons-vue';

const props = defineProps({
  title: { type: String, default: 'AI 助手' },
  modeLabel: { type: String, default: '' },
  modes: { type: Array, default: () => [] },
  currentMode: { type: String, default: '' },
  suggestions: { type: Array, default: () => [] },
  messages: { type: Array, default: () => [] },
  isStreaming: { type: Boolean, default: false },
  toolsVisible: { type: Array, default: () => [] },
  error: { type: String, default: '' },
  feedbackState: { type: Object, default: () => ({}) },
  isMobile: { type: Boolean, default: false },
  apiStatus: { type: String, default: 'connected' },
  apiModelName: { type: String, default: 'DeepSeek V4 Pro' },
  dailyUsage: { type: Number, default: 0 },
  dailyLimit: { type: Number, default: 20 },
  hasCustomKey: { type: Boolean, default: false },
  thinking: { type: String, default: '' },
});

const emit = defineEmits(['send', 'newChat', 'toggleSessions', 'feedback', 'stop', 'modeChange']);

// ── 本地状态 ──
const inputText = ref('');
const msgContainer = ref(null);
const inputRef = ref(null);
const hoveredIdx = ref(null);
const showScrollBtn = ref(false);
const userScrolledUp = ref(false);
const showNewMsgNotice = ref(false);

// ── UX #7 占位符轮换 ──
const placeholders = ['输入消息…', '问一道题…', '分析一个班级…', '搜索知识点…', '出随堂练习…'];
const placeholderIdx = ref(0);
const currentPlaceholder = computed(() => placeholders[placeholderIdx.value]);
let placeholderTimer = null;

onMounted(() => {
  placeholderTimer = setInterval(() => {
    placeholderIdx.value = (placeholderIdx.value + 1) % placeholders.length;
  }, 5000);
});

onUnmounted(() => {
  if (placeholderTimer) clearInterval(placeholderTimer);
});

// ── 快捷操作 ──
const quickActions = [
  { icon: '📝', label: '出题', prompt: '帮我出几道题' },
  { icon: '📊', label: '分析', prompt: '帮我分析一下成绩' },
  { icon: '🔍', label: '搜索', prompt: '搜索知识点' },
];
const visibleQuickActions = computed(() =>
  props.isMobile ? quickActions.slice(0, 2) : quickActions
);

// ── 模式切换双向绑定 ──
const currentModeModel = computed({
  get: () => props.currentMode,
  set: (val) => emit('modeChange', val),
});

// ── 是否已有 AI 回复（用于"思考中"指示器）──
const hasAssistantMessage = computed(() => props.messages.some((m) => m.role === 'assistant'));

// ── 是否为最后一条 AI 消息 ──
function isLastAssistantMsg(idx) {
  const msgs = props.messages;
  if (msgs.length === 0) return false;
  return idx === msgs.length - 1 && msgs[idx]?.role === 'assistant';
}
// 工具状态栏独立显示在输入区上方，不再绑定到最后一条消息


// ── 发送 ──
function handleSend() {
  if (!inputText.value.trim()) return;
  emit('send', inputText.value.trim());
  inputText.value = '';
  // 强制 autosize 收缩回 minRows
  nextTick(() => {
    inputRef.value?.resize?.();
  });
}

// ── UX #4 误操作保护：新对话确认 ──
async function handleNewChat() {
  if (props.messages.length > 0) {
    try {
      await ElMessageBox.confirm('当前对话记录将丢失，确定要开始新对话吗？', '确认新对话', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      });
    } catch {
      return;
    }
  }
  inputText.value = '';
  nextTick(() => {
    inputRef.value?.resize?.();
  });
  emit('newChat');
}

// ── 建议词/快捷操作点击 ──
function handleSuggestion(text) {
  inputText.value = text;
  handleSend();
}

// ── UX #1 复制消息 ──
async function copyMessage(msg) {
  try {
    const text = typeof msg.content === 'string' ? msg.content : JSON.stringify(msg.content);
    await navigator.clipboard.writeText(text);
    ElMessage.success({ message: '已复制', duration: 1500 });
  } catch (e) {
    console.warn('[ChatPanel] 复制失败:', e);
  }
}

// ── 反馈 ──
function handleFeedback(msgIndex, rating, msg) {
  if (props.feedbackState[msgIndex] !== undefined) return;
  let userQuestion = '';
  for (let i = msgIndex - 1; i >= 0; i--) {
    if (props.messages[i]?.role === 'user') {
      userQuestion = props.messages[i].content || '';
      break;
    }
  }
  const answerSnippet = (msg.content || '').substring(0, 200);
  const tools = [...new Set(props.toolsVisible.map((t) => t.name))].join(',');
  emit('feedback', {
    messageIndex: msgIndex,
    rating,
    userQuestion,
    answerSnippet,
    toolsUsed: tools,
  });
}

// ── 时间格式化 ──
function fmtTime(ts) {
  if (!ts) return '';
  const d = new Date(ts);
  return (
    d.getHours().toString().padStart(2, '0') + ':' + d.getMinutes().toString().padStart(2, '0')
  );
}

// ── UX #2 / UX #6 滚动逻辑 ──
function onScroll() {
  const el = msgContainer.value;
  if (!el) return;
  const distFromBottom = el.scrollHeight - el.scrollTop - el.clientHeight;
  showScrollBtn.value = distFromBottom > 200;
  userScrolledUp.value = distFromBottom > 50;
  if (distFromBottom > 200) {
    showNewMsgNotice.value = false;
  }
}

function scrollToBottom() {
  const el = msgContainer.value;
  if (!el) return;
  el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' });
  showScrollBtn.value = false;
  userScrolledUp.value = false;
  showNewMsgNotice.value = false;
}

// UX #6 强制滚底：新消息到达时自动滚底（除非用户手动上滚）
watch(
  () => props.messages.length,
  () => {
    nextTick(() => {
      if (!userScrolledUp.value) {
        scrollToBottom();
      } else {
        showNewMsgNotice.value = true;
      }
    });
  }
);

// 流式内容变化时也尝试滚底
watch(
  () => {
    const msgs = props.messages;
    if (!msgs.length) return '';
    const last = msgs[msgs.length - 1];
    return last?.role === 'assistant' ? last.content : '';
  },
  () => {
    nextTick(() => {
      if (!userScrolledUp.value) {
        scrollToBottom();
      }
    });
  }
);

// 工具标签由 createToolCallbacks 统一解析（来自后端 progress / 前端映射表），
// toolsVisible 中每个元素自带 label 字段；独立工具状态栏显示在输入区上方
</script>

<style scoped>
/* ── 面板容器 ── */
.chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--bg-color);
  border-radius: var(--border-radius-base, 8px);
  overflow: hidden;
}

/* ── Header ── */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 0.5px solid var(--border-base, var(--border-color));
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.header-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.header-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 500;
  color: var(--primary-color);
  background: var(--primary-light, rgba(67, 97, 238, 0.08));
  border-radius: 10px;
  white-space: nowrap;
  flex-shrink: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

/* ── 消息容器 ── */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  position: relative;
}

/* ── 错误 ── */
.chat-error {
  margin-bottom: 12px;
}

/* ── 空状态 ── */
.chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 32px 16px;
}

.empty-icon-wrap {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: linear-gradient(
    135deg,
    var(--primary-light, rgba(67, 97, 238, 0.12)),
    var(--bg-secondary)
  );
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

.empty-icon {
  font-size: 28px;
  line-height: 1;
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px;
}

.empty-desc {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0 0 20px;
  text-align: center;
  max-width: 280px;
  line-height: 1.6;
}

.empty-suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.suggestion-pill {
  padding: 6px 16px;
  font-size: 13px;
  color: var(--text-secondary);
  background: var(--bg-color);
  border: 0.5px solid var(--border-base, var(--border-color));
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
}

.suggestion-pill:hover {
  color: var(--primary-color);
  border-color: var(--primary-color);
  background: var(--primary-light, rgba(67, 97, 238, 0.04));
}

/* ── 消息行 ── */
.chat-msg {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.chat-msg.user {
  flex-direction: row-reverse;
}

/* ── 头像 ── */
.msg-avatar {
  flex-shrink: 0;
}

.avatar-user {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: var(--primary-color);
  color: var(--el-color-white);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
}

.avatar-ai {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--bg-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  line-height: 1;
}

/* ── 消息主体 ── */
.msg-main {
  max-width: 85%;
  min-width: 0;
}

/* ── 气泡 ── */
.msg-bubble {
  position: relative;
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
}

.chat-msg.user .msg-bubble {
  background: var(--primary-color);
  color: var(--el-color-white);
  border-radius: 12px 12px 4px 12px;
}

.chat-msg.assistant .msg-bubble {
  background: var(--bg-color);
  color: var(--text-primary);
  border: 0.5px solid var(--border-base, var(--border-color));
  border-radius: 12px 12px 12px 4px;
}

/* ── UX #1 复制按钮 ── */
.msg-copy-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: var(--bg-secondary);
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.7;
  transition: opacity 0.15s;
  line-height: 1;
  padding: 0;
}

.msg-copy-btn:hover {
  opacity: 1;
}

/* ── 工具调用徽标 ── */
.msg-tools {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
}

.tool-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 10px;
  font-size: 12px;
  color: var(--text-secondary);
  background: var(--bg-secondary);
  border-radius: 12px;
  border: 0.5px solid var(--border-base, var(--border-color));
  line-height: 1.4;
}

.tool-badge.tool-done .tool-check {
  color: var(--el-color-success);
  font-weight: 700;
}

.tool-badge.tool-running {
  color: var(--primary-color);
}

.tool-badge.tool-denied .tool-cross,
.tool-badge.tool-error .tool-cross,
.tool-badge.tool-failed .tool-cross {
  color: var(--el-color-danger);
}

.tool-spinner {
  display: inline-block;
  width: 10px;
  height: 10px;
  border: 2px solid var(--border-base, var(--border-color));
  border-top-color: var(--primary-color);
  border-radius: 50%;
  animation: tool-spin 0.7s linear infinite;
}

@keyframes tool-spin {
  to {
    transform: rotate(360deg);
  }
}

/* ── 独立工具状态栏（消息区与输入区之间） ── */
.tools-status-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 8px 16px;
  flex-shrink: 0;
  background: var(--bg-page);
  border-top: 0.5px solid var(--border-base);
}

/* ── 反馈按钮 ── */
.msg-feedback {
  display: flex;
  gap: 6px;
  margin-top: 4px;
  padding: 0 4px;
}

.fb-btn {
  background: transparent;
  border: 0.5px solid var(--border-base, var(--border-color));
  border-radius: 4px;
  padding: 2px 8px;
  cursor: pointer;
  font-size: 13px;
  opacity: 0.4;
  transition: all 0.15s;
  line-height: 1.4;
  font-family: inherit;
}

.fb-btn:hover {
  opacity: 0.85;
  border-color: var(--primary-color);
}

.fb-btn.active {
  opacity: 1;
  border-color: var(--primary-color);
  background: var(--primary-light, rgba(67, 97, 238, 0.08));
}

/* ── 时间戳 ── */
.msg-time {
  font-size: 11px;
  color: var(--text-secondary);
  margin-top: 2px;
  padding: 0 4px;
}

.chat-msg.user .msg-time {
  text-align: right;
}

/* ── UX #3 流式输出增强 ── */
.streaming-indicator {
  margin-top: 8px;
  padding: 0 4px;
}

.streaming-bar {
  width: 100%;
  height: 3px;
  background: var(--bg-secondary);
  border-radius: 2px;
  overflow: hidden;
}

.streaming-bar::after {
  content: '';
  display: block;
  width: 36%;
  height: 100%;
  background: var(--primary-light, rgba(67, 97, 238, 0.3));
  border-radius: 2px;
  animation: pulse-bar 1.5s ease-in-out infinite;
}

@keyframes pulse-bar {
  0%,
  100% {
    transform: translateX(-60%);
    opacity: 0.25;
  }
  50% {
    transform: translateX(220%);
    opacity: 1;
  }
}

.streaming-stop-btn {
  display: inline-flex;
  align-items: center;
  padding: 4px 14px;
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-color-danger);
  background: var(--el-color-danger-light-9, #fef0f0);
  border: none;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
}

.streaming-stop-btn:hover {
  background: var(--el-color-danger-light-8, #fde0e0);
}

/* T8: 流式等待指示 — 跳动圆点 */
.thinking-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 24px 0;
}
.thinking-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--primary-color);
  opacity: 0.4;
  animation: thinking-bounce 1.2s ease-in-out infinite;
}
.thinking-dot:nth-child(2) {
  animation-delay: 0.2s;
}
.thinking-dot:nth-child(3) {
  animation-delay: 0.4s;
}
@keyframes thinking-bounce {
  0%,
  80%,
  100% {
    transform: scale(0.8);
    opacity: 0.3;
  }
  40% {
    transform: scale(1.2);
    opacity: 1;
  }
}
.thinking-label {
  margin-left: 8px;
  font-size: 13px;
  color: var(--text-secondary);
}

/* ── UX #2 滚动到底部按钮 ── */
.scroll-bottom-btn {
  position: sticky;
  bottom: 8px;
  left: 50%;
  transform: translateX(-50%);
  padding: 6px 16px;
  font-size: 12px;
  color: var(--primary-color);
  background: var(--bg-color);
  border: 0.5px solid var(--border-base, var(--border-color));
  border-radius: 16px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  z-index: 10;
  transition: all 0.15s;
  font-family: inherit;
}

.scroll-bottom-btn:hover {
  color: var(--el-color-white);
  background: var(--primary-color);
}

/* ── UX #6 新消息提示横条 ── */
.new-msg-notice {
  position: sticky;
  bottom: 8px;
  left: 50%;
  transform: translateX(-50%);
  width: fit-content;
  padding: 6px 16px;
  font-size: 12px;
  color: var(--primary-color);
  background: var(--bg-color);
  border: 0.5px solid var(--primary-light, var(--primary-color));
  border-radius: 16px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  z-index: 10;
}

/* ── 过渡动画 ── */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* ── 输入区 ── */
.chat-input-area {
  flex-shrink: 0;
  border-top: 0.5px solid var(--border-base, var(--border-color));
  background: var(--bg-color);
}

.input-row {
  display: flex;
  gap: 10px;
  padding: 12px 16px 8px;
  align-items: flex-end;
}

.input-row :deep(.el-textarea__inner) {
  resize: none;
  background: var(--bg-page);
}

.send-btn {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  font-size: 16px;
  padding: 0;
  border-radius: 8px;
}

/* T8: 流式状态时发送按钮脉冲 */
.send-btn.is-streaming {
  animation: send-btn-pulse 1.2s ease-in-out infinite;
}

@keyframes send-btn-pulse {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(67, 97, 238, 0.3);
  }
  50% {
    box-shadow: 0 0 0 6px rgba(67, 97, 238, 0);
  }
}

/* ── 快捷操作 ── */
.quick-actions {
  display: flex;
  gap: 4px;
  padding: 0 16px 4px;
  flex-wrap: wrap;
}

.quick-action {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 3px 10px;
  font-size: 12px;
  color: var(--text-secondary);
  background: var(--bg-page);
  border: 0.5px solid var(--border-base, var(--border-color));
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.15s;
  user-select: none;
}

.quick-action:hover {
  color: var(--primary-color);
  border-color: var(--primary-color);
  background: var(--primary-light, rgba(67, 97, 238, 0.04));
}

/* ── 移动端适配 ── */
@media (max-width: 768px) {
  .avatar-user,
  .avatar-ai {
    width: 24px !important;
    height: 24px !important;
  }
  .chat-input-area {
    padding-bottom: 48px;
  }
  .msg-bubble.user {
    border-radius: 10px 10px 4px 10px;
  }
  .msg-bubble.ai {
    border-radius: 10px 10px 10px 4px;
  }
}
</style>
<style scoped src="./chat-markdown.css"></style>

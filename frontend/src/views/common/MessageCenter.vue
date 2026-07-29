<template>
  <div class="message-center">
    <div class="mc-sidebar">
      <div class="mc-sidebar-header">
        <h3>消息</h3>
        <el-badge
          :value="totalUnread"
          :hidden="totalUnread === 0"
          type="danger"
          class="mc-badge"
        />
      </div>
      <div class="mc-conversations">
        <div
          v-for="c in conversations"
          :key="c.otherUserId"
          class="mc-conv-item"
          :class="{ active: c.otherUserId === activeConv }"
          @click="openConversation(c.otherUserId)"
        >
          <div class="mc-avatar">{{ c.otherUserName?.charAt(0) }}</div>
          <div class="mc-conv-info">
            <div class="mc-conv-name">
              {{ c.otherUserName }}
              <el-badge
                :value="c.unreadCount"
                :hidden="!c.unreadCount"
                type="danger"
                class="conv-badge"
              />
            </div>
            <div class="mc-conv-preview">{{ c.lastMessage }}</div>
            <div class="mc-conv-time">{{ fmt(c.lastTime) }}</div>
          </div>
        </div>
        <el-empty v-if="conversations.length === 0" description="暂无会话" :image-size="40" />
      </div>
    </div>

    <div class="mc-main">
      <div v-if="!activeConv" class="mc-empty">
        <el-icon :size="48" color="#ccc"><ChatDotSquare /></el-icon>
        <p>选择左侧会话开始聊天</p>
      </div>
      <template v-else>
        <div class="mc-chat-header">
          <h4>{{ activeUserName }}</h4>
        </div>
        <div ref="msgBox" class="mc-messages">
          <div
            v-for="m in messages"
            :key="m.id"
            class="mc-msg"
            :class="{ 'mc-msg-mine': m.senderId === currentUserId, 'mc-msg-theirs': m.senderId !== currentUserId }"
          >
            <div class="mc-msg-bubble">{{ m.content }}</div>
            <div class="mc-msg-time">{{ fmtTime(m.createdAt) }}</div>
          </div>
        </div>
        <div class="mc-input-area">
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="2"
            placeholder="输入消息..."
            @keydown.enter.prevent="doSend"
          />
          <el-button
            type="primary"
            :loading="sending"
            style="margin-left:8px;align-self:flex-end"
            @click="doSend"
          >
            发送
          </el-button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { getConversations, getConversationMessages, sendMessage, getUnreadMessageCount } from '@/api/messages'

const currentUserId = ref(null)
const conversations = ref([])
const totalUnread = ref(0)
const activeConv = ref(null)
const activeUserName = ref('')
const messages = ref([])
const inputText = ref('')
const sending = ref(false)
const msgBox = ref(null)

const scrollToBottom = async () => {
  await nextTick()
  if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight
}

const fmt = (s) => {
  if (!s) return ''
  const d = new Date(s)
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  if (d.toDateString() === now.toDateString()) return `${pad(d.getHours())}:${pad(d.getMinutes())}`
  return `${d.getMonth() + 1}/${d.getDate()}`
}

const fmtTime = (s) => {
  if (!s) return ''
  const d = new Date(s)
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(d.getMonth() + 1)}/${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const loadConversations = async () => {
  const res = await getConversations()
  if (res.code === 200) conversations.value = res.data || []
}

const loadUnread = async () => {
  const res = await getUnreadMessageCount()
  if (res.code === 200) totalUnread.value = res.data.count || 0
}

const openConversation = async (otherUserId) => {
  activeConv.value = otherUserId
  const c = conversations.value.find(x => x.otherUserId === otherUserId)
  activeUserName.value = c?.otherUserName || ''
  const res = await getConversationMessages(otherUserId)
  if (res.code === 200) {
    messages.value = res.data || []
    await scrollToBottom()
  }
  await loadConversations()
  await loadUnread()
}

const doSend = async () => {
  const text = inputText.value.trim()
  if (!text || !activeConv.value) return
  sending.value = true
  try {
    const res = await sendMessage({ receiverId: activeConv.value, content: text })
    if (res.code === 200) {
      inputText.value = ''
      messages.value.push({ id: res.data.id, senderId: currentUserId.value, receiverId: activeConv.value, content: text, createdAt: res.data.createdAt })
      await scrollToBottom()
      const idx = conversations.value.findIndex(c => c.otherUserId === activeConv.value)
      if (idx >= 0) conversations.value[idx].lastMessage = text.length > 50 ? text.slice(0, 50) + '…' : text
    } else ElMessage.error(res.message || '发送失败')
  } catch { ElMessage.error('发送失败') }
  finally { sending.value = false }
}

let _pollTimer = null

onMounted(async () => {
  try {
    const u = JSON.parse(sessionStorage.getItem('userInfo') || '{}')
    currentUserId.value = u.id
  } catch { /* sessionStorage parse error — ignore */ }
  await Promise.all([loadConversations(), loadUnread()])
  _pollTimer = setInterval(loadUnread, 30000)
})

onUnmounted(() => {
  if (_pollTimer) { clearInterval(_pollTimer); _pollTimer = null }
})
</script>

<style scoped lang="scss">
.message-center {
  display: flex; height: calc(100vh - 120px); margin: 0 auto;
  border: 1px solid var(--border-color, #ebeef5); border-radius: var(--radius-md); overflow: hidden;
}
.mc-sidebar {
  width: 300px; flex-shrink: 0; border-right: 1px solid var(--border-color, #ebeef5);
  display: flex; flex-direction: column; background: var(--bg-hover);
}
.mc-sidebar-header {
  display: flex; align-items: center; gap: 8px; padding: 16px; border-bottom: 1px solid var(--border-color, #ebeef5);
  h3 { margin: 0; font-size: var(--fs-lg); }
}
.mc-conversations { flex: 1; overflow-y: auto; }
.mc-conv-item {
  display: flex; align-items: center; gap: 10px; padding: 12px 16px; cursor: pointer; transition: background 0.15s;
  &:hover { background: var(--el-color-primary-light-9, #ecf5ff); }
  &.active { background: var(--el-color-primary-light-8, #d9ecff); }
}
.mc-avatar {
  width: 36px; height: 36px; border-radius: 50%; background: var(--el-color-primary); color: #fff;
  display: flex; align-items: center; justify-content: center; font-size: var(--fs-md); flex-shrink: 0;
}
.mc-conv-info { flex: 1; overflow: hidden; }
.mc-conv-name { font-size: var(--fs-md); font-weight: 500; color: var(--text-primary); display: flex; align-items: center; gap: 4px; }
.mc-conv-preview { font-size: var(--fs-xs); color: var(--text-secondary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-top: 2px; }
.mc-conv-time { font-size: var(--fs-xs); color: var(--text-disabled); margin-top: 2px; }
.conv-badge { margin-left: auto; }
.mc-badge { margin-left: auto; }

.mc-main { flex: 1; display: flex; flex-direction: column; }
.mc-empty { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; color: var(--text-secondary); font-size: var(--fs-md); }
.mc-chat-header { padding: 12px 16px; border-bottom: 1px solid var(--border-color, #ebeef5); h4 { margin: 0; font-size: var(--fs-md); } }
.mc-messages { flex: 1; overflow-y: auto; padding: 16px; display: flex; flex-direction: column; gap: 12px; }
.mc-msg { display: flex; flex-direction: column; max-width: 70%; }
.mc-msg-mine { align-self: flex-end; align-items: flex-end; }
.mc-msg-theirs { align-self: flex-start; align-items: flex-start; }
.mc-msg-bubble {
  padding: 10px 14px; border-radius: 12px; font-size: var(--fs-md); line-height: 1.5; word-break: break-word;
  .mc-msg-mine & { background: var(--el-color-primary); color: #fff; border-bottom-right-radius: 4px; }
  .mc-msg-theirs & { background: var(--el-color-info-light-8, #f0f0f0); color: var(--text-primary); border-bottom-left-radius: 4px; }
}
.mc-msg-time { font-size: var(--fs-xs); color: var(--text-disabled); margin-top: 4px; }
.mc-input-area {
  display: flex; padding: 12px 16px; border-top: 1px solid var(--border-color, #ebeef5);
  :deep(.el-textarea__inner) { min-height: 50px !important; }
}

@media (max-width: 768px) {
  .message-center { height: calc(100vh - 100px); }
  .mc-sidebar { width: 100px; }
  .mc-conv-info .mc-conv-preview, .mc-conv-info .mc-conv-time { display: none; }
}
</style>

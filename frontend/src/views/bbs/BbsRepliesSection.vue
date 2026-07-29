<template>
  <!-- 回复区 -->
  <el-card shadow="never" class="reply-card">
    <h3>全部回复（{{ total }}）</h3>

    <div v-if="loading" class="reply-loading">
      <el-skeleton :rows="3" animated />
    </div>

    <div v-else-if="replies.length === 0" class="empty-reply">
      <div class="empty-illustration">💬</div>
      <div class="empty-title">暂无回复</div>
      <div class="empty-desc">来做第一个回复的人吧</div>
    </div>

    <div
      v-for="reply in replies"
      :id="'reply-' + reply.id"
      :key="reply.id"
      class="reply-item"
    >
      <div class="reply-author">
        <el-avatar :size="32" :src="reply.authorAvatar || ''">{{ reply.authorName?.charAt(0) }}</el-avatar>
        <span class="author-name">{{ reply.authorName }}</span>
        <span v-if="reply.authorClassName" class="author-class">{{ reply.authorClassName }}</span>
        <el-tag
          v-if="isNonActive(reply.authorStatus)"
          :type="statusTagType(reply.authorStatus)"
          size="small"
          effect="plain"
        >
          {{ statusLabel(reply.authorStatus) }}
        </el-tag>
        <span class="reply-time">{{ formatTime(reply.createTime) }}</span>
        <el-button
          v-if="isTeacher || reply.authorId === userId"
          text
          size="small"
          type="danger"
          style="margin-left:auto"
          @click="handleDeleteReply(reply.id)"
        >
          删除
        </el-button>
      </div>
      <div class="reply-content" v-html="renderBbsContent(reply.content)"></div>
      <div class="reply-actions">
        <el-button text size="small" @click="replyTo(reply)">
          <el-icon><ChatLineRound /></el-icon>回复
        </el-button>
        <el-button text size="small" @click="quoteReply(reply)">引用</el-button>
        <el-button
          text
          size="small"
          :type="reply.liked ? 'primary' : ''"
          @click="handleLike(reply.id)"
        >
          <el-icon><StarFilled v-if="reply.liked" /><Star v-else /></el-icon>{{ reply.likeCount || 0 }}
        </el-button>
      </div>
    </div>

    <div v-if="hasMore" class="load-more-wrap">
      <el-button text :loading="loadingMore" @click="loadMore">加载更多回复</el-button>
    </div>

    <!-- 回复输入框 -->
    <div class="reply-input-area">
      <div v-if="replyTarget" class="reply-to">
        回复 @{{ replyTarget.authorName }}
        <el-button text size="small" @click="replyTarget = null">取消</el-button>
      </div>
      <el-input
        ref="replyInputRef"
        v-model="replyContent"
        type="textarea"
        :rows="3"
        placeholder="输入回复内容，@用户名 可提及他人"
        maxlength="2000"
        show-word-limit
      />
      <div class="reply-submit">
        <el-button type="primary" :loading="replying" @click="handleReply">{{ replyTarget ? '回复' : '发表回复' }}</el-button>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReplies, createReply, deleteReply, toggleLike } from '@/api/bbs'
import { isNonActive, statusLabel, statusTagType } from '@/utils/student'
import { renderBbsContent } from '@/utils/markdown'
import dayjs from 'dayjs'

const props = defineProps({
  postId: { type: Number, required: true },
  isTeacher: { type: Boolean, default: false },
  userId: { type: Number, default: null }
})

const emit = defineEmits(['replied', 'delete-reply'])

const replies = ref([])
const replyContent = ref('')
const replying = ref(false)
const replyTarget = ref(null)
const replyInputRef = ref(null)
const loading = ref(false)
const loadingMore = ref(false)
const page = ref(1)
const total = ref(0)
const hasMore = ref(false)

const formatTime = (t) => t ? dayjs(t).format('MM-DD HH:mm') : ''

const loadReplies = async (append = false) => {
  if (append) {
    loadingMore.value = true
  } else {
    loading.value = true
    page.value = 1
  }
  try {
    const res = await getReplies(props.postId, { page: page.value, pageSize: 20 })
    if (res.code === 200) {
      const records = res.data.records || []
      if (append) {
        replies.value = [...replies.value, ...records]
      } else {
        replies.value = records
      }
      total.value = res.data.total || 0
      hasMore.value = res.data.hasMore || false
    }
  } catch { ElMessage.error('加载回复失败') } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const loadMore = () => {
  page.value++
  loadReplies(true)
}

const handleLike = async (replyId) => {
  try {
    const res = await toggleLike({ targetId: replyId, targetType: 'reply' })
    if (res.code === 200) {
      const r = replies.value.find(r => r.id === replyId)
      if (r) { r.liked = res.data.liked; r.likeCount = res.data.count }
    }
  } catch { ElMessage.error('操作失败') }
}

const handleDeleteReply = async (replyId) => {
  try {
    await ElMessageBox.confirm('确定删除该回复吗？', '确认', { type: 'warning' })
    const res = await deleteReply(replyId)
    if (res.code === 200) {
      ElMessage.success('已删除')
      replies.value = replies.value.filter(r => r.id !== replyId)
      total.value--
      emit('delete-reply', replyId)
    }
  } catch { /* cancelled */ }
}

const replyTo = (reply) => {
  replyTarget.value = reply
  replyContent.value = `@${reply.authorName} `
}

const quoteReply = (reply) => {
  replyContent.value = '> ' + reply.authorName + '：' + reply.content.slice(0, 100) + '\n\n' + replyContent.value
  nextTick(() => {
    replyInputRef.value?.focus()
  })
}

const handleReply = async () => {
  if (!replyContent.value.trim()) { ElMessage.warning('请输入回复内容'); return }
  replying.value = true
  try {
    const res = await createReply(props.postId, {
      content: replyContent.value,
      parentId: replyTarget.value?.id || null
    })
    if (res.code === 200) {
      ElMessage.success('回复成功')
      replyContent.value = ''
      replyTarget.value = null
      await loadReplies()
      emit('replied')
    }
  } finally { replying.value = false }
}

onMounted(() => loadReplies())
watch(() => props.postId, () => { replies.value = []; total.value = 0; hasMore.value = false; loadReplies() })
</script>

<style scoped lang="scss">
.reply-card {
  h3 { font-size: var(--fs-lg); margin: 0 0 16px; }
}
.reply-loading { padding: 16px 0; }
.reply-item {
  padding: 16px 0; border-bottom: 1px solid var(--border-light);
  transition: background var(--transition-fast);
  animation: replyFadeIn 0.3s ease;
  &:last-child { border-bottom: none; }
  &:hover { background: var(--bg-secondary); margin: 0 -16px; padding: 16px; border-radius: var(--radius-md); }
}
@keyframes replyFadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
.reply-author {
  display: flex; align-items: center; gap: 8px; margin-bottom: 8px; flex-wrap: wrap;
  .author-name { font-weight: 500; font-size: var(--fs-md); }
  .author-class { font-size: var(--fs-xs); color: var(--text-secondary); background: var(--bg-secondary); padding: 1px 6px; border-radius: var(--radius-xs); }
  .reply-time { font-size: var(--fs-xs); color: var(--text-secondary); }
}
.reply-content {
  font-size: var(--fs-md); line-height: 1.7; color: var(--text-regular); padding-left: 40px;
  :deep(.mention) { color: var(--primary-color); background: var(--primary-light); padding: 1px 4px; border-radius: var(--radius-xs); }
}
.reply-actions {
  display: flex; gap: 8px; padding-left: 40px; margin-top: 8px;
}
.reply-input-area {
  position: sticky;
  bottom: 0;
  background: var(--bg-card);
  padding: 12px 0;
  border-top: 1px solid var(--border-light);
  z-index: 10;
  margin-top: 20px;
}
.reply-to {
  font-size: var(--fs-sm); color: var(--primary-color); margin-bottom: 8px; display: flex; align-items: center; gap: 8px;
}
.reply-submit { display: flex; justify-content: flex-end; margin-top: 8px; }
.load-more-wrap { text-align: center; padding: 16px 0; }
.empty-reply { text-align: center; padding: 32px 0; }
.empty-illustration { font-size: 48px; margin-bottom: 12px; }
.empty-title { font-size: var(--fs-md); font-weight: 500; color: var(--text-primary); margin-bottom: 4px; }
.empty-desc { font-size: var(--fs-sm); color: var(--text-secondary); }

@media (max-width: 768px) {
  .reply-content { padding-left: 24px; font-size: var(--fs-sm); }
  .reply-actions { padding-left: 24px; }
  .reply-item:hover { margin: 0 -8px; padding: 12px 8px; }
  .reply-author .author-name { font-size: var(--fs-sm); }
}
</style>

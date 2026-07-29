<template>
  <div class="post-detail-container">
    <!-- 骨架屏 -->
    <div v-if="loading" class="sk-list">
      <div class="bbs-top-nav">
        <el-button text class="back-btn"><el-icon><ArrowLeft /></el-icon>返回论坛</el-button>
      </div>
      <el-skeleton :rows="6" animated />
      <el-skeleton :rows="3" animated style="margin-top:16px" />
    </div>

    <div v-else>
      <div class="bbs-top-nav">
        <el-button text class="back-btn" @click="router.push('/bbs')">
          <el-icon><ArrowLeft /></el-icon>返回论坛
        </el-button>
        <el-breadcrumb separator=">">
          <el-breadcrumb-item :to="{ path: '/bbs' }">师生论坛</el-breadcrumb-item>
          <el-breadcrumb-item>帖子详情</el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <el-card v-if="post" shadow="never" class="detail-card">
        <div class="post-header">
          <div class="post-tags">
            <el-tag v-if="post.isSticky" size="small" type="warning">置顶</el-tag>
            <el-tag v-if="post.isHighlighted" size="small" type="danger">精华</el-tag>
            <el-tag size="small">{{ post.categoryIcon || '' }} {{ post.categoryName }}</el-tag>
          </div>
          <h2 class="post-title">{{ post.title }}</h2>
          <div class="post-author">
            <el-avatar :size="40" :src="post.authorAvatar || ''">{{ post.authorName?.charAt(0) }}</el-avatar>
            <span class="author-name">{{ post.authorName }}</span>
            <span v-if="post.authorClassName" class="author-class">{{ post.authorClassName }}</span>
            <el-tag
              v-if="isNonActive(post.authorStatus)"
              :type="statusTagType(post.authorStatus)"
              size="small"
              effect="plain"
            >
              {{ statusLabel(post.authorStatus) }}
            </el-tag>
            <el-tag
              v-if="post.authorCustomTitle"
              size="small"
              type="warning"
              effect="dark"
              style="margin-left:6px;font-size:var(--fs-xs)"
            >
              <el-icon style="margin-right:2px"><Medal /></el-icon>{{ post.authorCustomTitle }}
            </el-tag>
            <el-tag
              v-if="post.authorCertCount > 0"
              size="small"
              type="success"
              effect="plain"
              style="margin-left:4px;font-size:var(--fs-xs)"
            >
              <el-icon style="margin-right:2px"><Trophy /></el-icon> x{{ post.authorCertCount }}
            </el-tag>
            <span class="post-time">{{ formatTime(post.createTime) }}</span>
            <span class="post-views"><el-icon size="14"><View /></el-icon>{{ post.viewCount || 0 }} 次阅读</span>
          </div>
        </div>

        <div class="post-content" v-html="renderBbsContent(post.content)"></div>

        <div v-if="post.images && JSON.parse(post.images).length > 0" class="post-images">
          <el-image
            v-for="(img, i) in JSON.parse(post.images)"
            :key="i"
            :src="img"
            :preview-src-list="JSON.parse(post.images)"
            fit="cover"
            preview-teleported
          />
        </div>

        <div class="post-actions">
          <el-button
            text
            :type="post.liked ? 'primary' : ''"
            class="like-btn"
            @click="handleLike"
          >
            <el-icon><StarFilled v-if="post.liked" /><Star v-else /></el-icon>
            {{ post.likeCount || 0 }}
          </el-button>
          <el-button text :type="post.bookmarked ? 'warning' : ''" @click="handleBookmark">
            <el-icon><CollectionTagFilled v-if="post.bookmarked" /><CollectionTag v-else /></el-icon>
            {{ post.bookmarked ? '已收藏' : '收藏' }}
          </el-button>
          <el-button
            v-if="isTeacher"
            text
            type="warning"
            @click="handleSticky"
          >
            <el-icon><Top /></el-icon>{{ post.isSticky ? '取消置顶' : '置顶' }}
          </el-button>
          <el-button
            v-if="post.authorId === userId && !isTeacher && !post.isSticky"
            text
            type="warning"
            :loading="stickyCouponLoading"
            @click="handleStickyWithCoupon"
          >
            <el-icon><Top /></el-icon>置顶(使用券)
          </el-button>
          <el-button
            v-if="isTeacher"
            text
            type="danger"
            @click="handleHighlight"
          >
            <el-icon><Orange /></el-icon>{{ post.isHighlighted ? '取消加精' : '加精' }}
          </el-button>
          <el-button
            v-if="isTeacher || post.authorId === userId"
            text
            type="danger"
            @click="handleDelete"
          >
            <el-icon><Delete /></el-icon>删除
          </el-button>
        </div>
      </el-card>

      <div v-if="post?.relatedPosts?.length > 0" class="related-section">
        <h3 class="related-title"><el-icon><Connection /></el-icon>相关推荐</h3>
        <div class="related-list">
          <div
            v-for="rp in post.relatedPosts"
            :key="rp.id"
            class="related-item"
            @click="router.push(`/bbs/post/${rp.id}`)"
          >
            <span class="related-post-title">{{ rp.title }}</span>
            <span class="related-post-meta">{{ rp.replyCount || 0 }}回复 · {{ rp.viewCount || 0 }}阅读</span>
          </div>
        </div>
      </div>

      <BbsRepliesSection
        v-if="post"
        :post-id="post.id"
        :is-teacher="isTeacher"
        :user-id="userId"
        @replied="onReplied"
      />

      <div class="bbs-bottom-nav">
        <el-button @click="router.push('/bbs')">
          <el-icon><ArrowLeft /></el-icon>返回论坛首页
        </el-button>
        <el-button text @click="scrollToTop">
          <el-icon><Top /></el-icon>回到顶部
        </el-button>
      </div>

      <div v-if="isMobile" class="bbs-reply-bar" :style="{ paddingBottom: 'var(--safe-bottom)' }">
        <el-input
          v-model="replyText"
          size="large"
          placeholder="写下你的回复..."
          clearable
        >
          <template #append>
            <el-button type="primary" :loading="replying" @click="doReply">发送</el-button>
          </template>
        </el-input>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getPostDetail, toggleLike, toggleBookmark, toggleSticky, toggleHighlight, useStickyCoupon, deletePost, createReply } from '@/api/bbs'
import { isNonActive, statusLabel, statusTagType } from '@/utils/student'
import { useIsMobile } from '@/composables/useIsMobile'
import { renderBbsContent } from '@/utils/markdown'
import BbsRepliesSection from './BbsRepliesSection.vue'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const post = ref(null)
const loading = ref(true)
const justLiked = ref(false)
const stickyCouponLoading = ref(false)
const replyText = ref('')
const replying = ref(false)

const { isMobile } = useIsMobile()

const userId = computed(() => userStore.userInfo?.id)
const isTeacher = computed(() => userStore.isTeacher || userStore.isAdmin)

const formatTime = (t) => t ? dayjs(t).format('MM-DD HH:mm') : ''

const loadPost = async () => {
  loading.value = true
  try {
    const res = await getPostDetail(route.params.id)
    if (res.code === 200) post.value = res.data
  } finally { loading.value = false }
}

const handleLike = async () => {
  try {
    const res = await toggleLike({ targetId: post.value.id, targetType: 'post' })
    if (res.code === 200) {
      post.value.liked = res.data.liked
      post.value.likeCount = res.data.count
      justLiked.value = true
      setTimeout(() => { justLiked.value = false }, 400)
    }
  } catch { ElMessage.error('操作失败') }
}

const handleBookmark = async () => {
  try {
    const res = await toggleBookmark({ postId: post.value.id })
    if (res.code === 200) {
      post.value.bookmarked = res.data.bookmarked
      if (post.value.bookmarked) {
        ElMessage.success('已收藏')
      } else {
        ElMessage.info('已取消收藏')
      }
    }
  } catch { ElMessage.error('操作失败') }
}

const handleSticky = async () => {
  try {
    await toggleSticky(post.value.id)
    post.value.isSticky = !post.value.isSticky
    ElMessage.success(post.value.isSticky ? '已置顶' : '已取消置顶')
  } catch { ElMessage.error('操作失败') }
}

const handleStickyWithCoupon = async () => {
  stickyCouponLoading.value = true
  try {
    const res = await useStickyCoupon(post.value.id)
    if (res.code === 200) {
      post.value.isSticky = 1
      ElMessage.success(res.message || '已使用置顶券，帖子已置顶')
    }
  } catch {
    ElMessage.error('使用失败，请先在积分商城兑换置顶券')
  } finally { stickyCouponLoading.value = false }
}

const handleHighlight = async () => {
  try {
    await toggleHighlight(post.value.id)
    post.value.isHighlighted = !post.value.isHighlighted
    ElMessage.success(post.value.isHighlighted ? '已加精' : '已取消加精')
  } catch { ElMessage.error('操作失败') }
}

const handleDelete = async () => {
  try {
    await ElMessageBox.confirm('确定删除该帖子吗？', '确认', { type: 'warning' })
    const res = await deletePost(post.value.id)
    if (res.code === 200) { ElMessage.success('已删除'); router.push('/bbs') }
  } catch { /* cancelled */ }
}

const onReplied = () => {
  if (post.value) post.value.replyCount = (post.value.replyCount || 0) + 1
}

const doReply = async () => {
  const text = replyText.value.trim()
  if (!text) return
  replying.value = true
  try {
    const res = await createReply(post.value.id, { content: text })
    if (res.code === 200) {
      replyText.value = ''
      ElMessage.success('回复成功')
      onReplied()
    } else {
      ElMessage.error(res.message || '回复失败')
    }
  } catch { ElMessage.error('回复失败') }
  finally { replying.value = false }
}

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(loadPost)
</script>

<style scoped lang="scss">
.post-detail-container { max-width: 860px; margin: 0 auto; }

.sk-list { padding: 8px 0; }

.bbs-top-nav {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  padding: 12px 0;
  .back-btn {
    font-size: var(--fs-md);
    color: var(--text-secondary);
    &:hover { color: var(--primary-color); }
  }
}

.bbs-bottom-nav {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 24px;
  padding: 20px 0 32px;
}

.post-views {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: 8px;
}

.detail-card {
  margin-bottom: 16px;
  &.sticky-post { border-left: 4px solid var(--warning-color); }
  &.highlighted-post { border-left: 4px solid var(--danger-color); }
}
.post-header { margin-bottom: 20px; }
.post-tags { display: flex; gap: 6px; margin-bottom: 10px; flex-wrap: wrap; }
.post-title { font-size: var(--fs-2xl); font-weight: 600; margin: 0 0 12px; color: var(--text-primary); line-height: 1.4; }
.post-author {
  display: flex; align-items: center; gap: 10px; font-size: var(--fs-md); color: var(--text-regular); flex-wrap: wrap;
  .author-class { font-size: var(--fs-xs); color: var(--text-secondary); background: var(--bg-secondary); padding: 1px 8px; border-radius: var(--radius-xs); }
  .post-time { color: var(--text-secondary); font-size: var(--fs-sm); }
}
.post-content {
  font-size: var(--fs-md); line-height: 1.8; color: var(--text-primary); padding: 16px 0;
  :deep(.mention) { color: var(--primary-color); background: var(--primary-light); padding: 1px 4px; border-radius: var(--radius-xs); }
  :deep(img) { max-width: 100%; border-radius: var(--radius-md); }
}
.post-images {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 4px;
  margin: 12px 0;
  .el-image { width: 100%; height: auto; aspect-ratio: 1; border-radius: var(--radius-md); cursor: pointer;
    border: 1px solid var(--border-light); }
}

@media (min-width: 769px) {
  .post-images {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  }
}
.post-actions {
  display: flex; gap: 8px; padding-top: 16px; border-top: 1px solid var(--border-light); flex-wrap: wrap;
}

.like-btn:active { animation: like-bounce 0.4s ease; }
.related-section {
  margin-bottom: 16px;
  .related-title { font-size: var(--fs-md); font-weight: 600; margin: 0 0 12px; display: flex; align-items: center; gap: 6px; color: var(--text-primary); }
  .related-list { display: flex; flex-direction: column; gap: 8px; }
  .related-item {
    display: flex; justify-content: space-between; align-items: center;
    padding: 10px 14px; background: var(--bg-card); border-radius: var(--radius-md);
    border: 0.5px solid var(--border-light); cursor: pointer;
    transition: all 0.2s;
    &:hover { border-color: var(--primary-color); box-shadow: var(--shadow-sm); }
  }
  .related-post-title { font-size: var(--fs-sm); color: var(--text-primary); flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .related-post-meta { font-size: var(--fs-xs); color: var(--text-secondary); flex-shrink: 0; margin-left: 12px; }
}

@keyframes like-bounce {
  0% { transform: scale(1); }
  30% { transform: scale(0.85); }
  60% { transform: scale(1.15); }
  100% { transform: scale(1); }
}

@media (max-width: 768px) {
  .bbs-reply-bar { position: fixed; bottom: 56px; left: 0; right: 0; padding: 10px 16px; background: var(--bg-card); border-top: 0.5px solid var(--border-color); z-index: 100; }
}

@media (max-width: 768px) {
  .bbs-top-nav { gap: 8px; margin-bottom: 12px; padding: 8px 0; .back-btn { font-size: var(--fs-sm); } }
  .bbs-bottom-nav { margin-top: 16px; padding: 12px 0 24px; gap: 12px; }
  .post-title { font-size: var(--fs-lg); }
  .post-content { font-size: var(--fs-md); padding: 12px 0; }
  .post-actions { gap: 4px; }
  .post-views { display: none; }
}
</style>

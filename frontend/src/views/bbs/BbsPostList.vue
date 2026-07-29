<template>
  <div v-loading="loading && posts.length === 0" class="post-list">
    <!-- 骨架屏 -->
    <el-skeleton v-if="loading && posts.length === 0" :rows="3" animated />

    <div v-if="!loading && posts.length === 0" class="empty-state-box">
      <div class="empty-icon">
        <el-icon :size="40"><ChatDotSquare /></el-icon>
      </div>
      <div class="empty-text">暂无帖子</div>
    </div>

    <template v-for="(group, gIdx) in groupedPosts" :key="gIdx">
      <div v-if="group.label" class="section-label">{{ group.label }}</div>
      <div
        v-for="post in group.items"
        :key="post.id"
        class="post-card card-hover bbs-post-item"
        :class="{ 'sticky-post': post.isSticky, 'highlighted-post': post.isHighlighted && !post.isSticky }"
        :style="{ borderLeft: `3px solid ${getCatColor(post.categoryId)}` }"
        @click="goDetail(post.id)"
      >
        <div v-if="post.isSticky" class="sticky-ribbon">
          <el-tag type="warning" size="small"><el-icon><Top /></el-icon> 置顶</el-tag>
        </div>
        <div v-if="post.isHighlighted && !post.isSticky" class="highlight-ribbon">
          <el-tag type="danger" size="small"><el-icon><Medal /></el-icon> 精华</el-tag>
        </div>

        <div class="post-main">
          <div class="post-avatar">
            <el-avatar :size="40">{{ post.authorName?.charAt(0) }}</el-avatar>
          </div>

          <div class="post-body">
            <div class="post-header">
              <div class="post-tags">
                <span class="category-tag">{{ getCatInfo(post.categoryId).icon || '' }} {{ post.categoryName }}</span>
                <el-tag
                  v-if="post.replyCount >= 10"
                  type="warning"
                  size="small"
                  effect="dark"
                  class="hot-badge"
                >
                  <el-icon><TrendCharts /></el-icon> 热帖
                </el-tag>
              </div>
              <h3 class="post-title bbs-post-title">{{ post.title }}</h3>
            </div>

            <p v-if="post.content" class="post-excerpt bbs-post-summary">{{ truncate(post.content, 120) }}</p>

            <div v-if="post.images && getImages(post.images).length > 0" class="post-images">
              <el-image
                v-for="(img, i) in getImages(post.images).slice(0, 3)"
                :key="i"
                :src="img"
                fit="cover"
                :preview-src-list="getImages(post.images)"
                style="width:72px;height:72px;border-radius:var(--radius-md);flex-shrink:0;border:0.5px solid var(--border-light)"
              >
                <template #error>
                  <div class="img-placeholder">
                    <el-icon><PictureFilled /></el-icon>
                  </div>
                </template>
              </el-image>
              <div v-if="getImages(post.images).length > 3" class="more-images">+{{ getImages(post.images).length - 3 }}</div>
            </div>

            <div class="post-footer bbs-post-meta">
              <span class="meta-item"><el-icon size="14"><User /></el-icon>{{ post.authorName }}</span>
              <span v-if="post.authorClassName" class="meta-item meta-class">{{ post.authorClassName }}</span>
              <el-tag
                v-if="isNonActive(post.authorStatus)"
                :type="statusTagType(post.authorStatus)"
                size="small"
                effect="plain"
                class="meta-status-tag"
              >
                {{ statusLabel(post.authorStatus) }}
              </el-tag>
              <span class="meta-item"><el-icon size="14"><ChatDotSquare /></el-icon>{{ post.replyCount || 0 }}</span>
              <span class="meta-item"><el-icon size="14"><View /></el-icon>{{ post.viewCount || 0 }}</span>
              <span class="meta-time">{{ formatTime(post.createTime) }}</span>
              <!-- 教师管理按钮 -->
              <span v-if="isTeacher" class="post-actions" @click.stop>
                <el-button
                  text
                  size="small"
                  :type="post.isSticky ? 'warning' : ''"
                  @click="$emit('toggleSticky', post)"
                >
                  <el-icon><Top /></el-icon>{{ post.isSticky ? '已置顶' : '置顶' }}
                </el-button>
                <el-button
                  text
                  size="small"
                  :type="post.isHighlighted ? 'danger' : ''"
                  @click="$emit('toggleHighlight', post)"
                >
                  <el-icon><Medal /></el-icon>{{ post.isHighlighted ? '已加精' : '加精' }}
                </el-button>
                <el-popconfirm title="确定删除此帖？" @confirm="$emit('deletePost', post)">
                  <template #reference>
                    <el-button text size="small" type="danger"><el-icon><Delete /></el-icon></el-button>
                  </template>
                </el-popconfirm>
              </span>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 无限滚动哨兵 -->
    <div ref="sentinelRef" class="scroll-sentinel" style="height:1px"></div>

    <!-- 分页：仅当全部加载完毕时显示 -->
    <div v-if="total > 0 && posts.length >= total" class="pagination-wrap">
      <el-pagination
        :current-page="page"
        :page-size="20"
        :total="total"
        layout="prev, pager, next"
        @current-change="onPageChange"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { isNonActive, statusLabel, statusTagType } from '@/utils/student'
import { useIsMobile } from '@/composables/useIsMobile'
import dayjs from 'dayjs'
import { primaryColor, elDanger, elInfo } from '@/utils/theme'

const props = defineProps({
  posts: { type: Array, required: true },
  loading: { type: Boolean, required: true },
  total: { type: Number, required: true },
  page: { type: Number, required: true },
  hasMore: { type: Boolean, default: false },
  categories: { type: Array, default: () => [] },
  isTeacher: { type: Boolean, default: false }
})

const emit = defineEmits(['update:page', 'go-detail', 'loadMore', 'toggleSticky', 'toggleHighlight', 'deletePost'])

const { isMobile } = useIsMobile()

const PALETTE = [primaryColor, '#4a9e2e', '#ff6b35', elDanger, '#e63946', '#457b9d', '#2a9d8f', '#e76f51']

const categoryColorMap = computed(() => {
  const map = {}
  props.categories.forEach((cat, i) => { map[cat.id] = PALETTE[i % PALETTE.length] })
  return map
})

const getCatColor = (id) => categoryColorMap.value[id] || elInfo
const getCatInfo = (id) => props.categories.find(c => c.id === id) || {}

const groupedPosts = computed(() => {
  const sticky = props.posts.filter(p => p.isSticky)
  const normal = props.posts.filter(p => !p.isSticky)
  const groups = []
  if (sticky.length > 0) groups.push({ label: '置顶帖子', items: sticky })
  if (normal.length > 0) groups.push({ label: sticky.length > 0 ? '全部帖子' : null, items: normal })
  return groups
})

const formatTime = (t) => t ? dayjs(t).format('MM-DD HH:mm') : ''
const stripHtml = (html) => { const d = document.createElement('div'); d.innerHTML = html; return d.textContent || '' }
const truncate = (text, len) => { const plain = stripHtml(text); return plain.length > len ? plain.slice(0, len) + '...' : plain }

const getImages = (images) => {
  if (!images) return []
  try { const arr = JSON.parse(images); return Array.isArray(arr) ? arr : [] } catch { return [] }
}

const goDetail = (id) => { emit('go-detail', id) }
const onPageChange = (val) => { emit('update:page', val) }

// 无限滚动
const sentinelRef = ref(null)
let observer = null
onMounted(() => {
  observer = new IntersectionObserver(([entry]) => {
    if (entry.isIntersecting && !props.loading && props.hasMore) {
      emit('loadMore')
    }
  }, { rootMargin: '200px' })
  if (sentinelRef.value) observer.observe(sentinelRef.value)
})
onBeforeUnmount(() => observer?.disconnect())
</script>

<style scoped lang="scss">
.section-label {
  font-size: var(--fs-sm);
  font-weight: 500;
  color: var(--text-secondary);
  padding: 8px 0 4px;
  &:first-child { padding-top: 0; }
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.post-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 20px;
  cursor: pointer;
  border: 0.5px solid var(--border-color);
  transition: border-color var(--transition-fast), transform var(--transition-fast);
  position: relative; overflow: hidden;
  animation: cardFadeIn 0.35s ease;

  &:hover {
    border-color: var(--primary-color);
    transform: translateY(-1px);
  }

  &.sticky-post {
    border-color: var(--sticky-border, #ffd580);
    background: linear-gradient(135deg, var(--sticky-bg, #fffdf5) 0%, var(--bg-card) 30%);
    .sticky-ribbon {
      position: absolute; top: 0; right: 0;
      font-weight: 500;
    }
  }

  &.highlighted-post {
    border-color: var(--highlight-border, #ffb3b3);
    background: linear-gradient(135deg, var(--highlight-bg, #fff8f8) 0%, var(--bg-card) 30%);
    .highlight-ribbon {
      position: absolute; top: 0; right: 0;
      font-weight: 500;
    }
  }
}

.hot-badge {
  font-weight: 600;
}

.post-main {
  display: flex;
  gap: 14px;
}

.post-avatar {
  flex-shrink: 0;
  :deep(.el-avatar) {
    background: var(--primary-light);
    color: var(--primary-color);
    font-weight: 500;
  }
}

.post-body { flex: 1; min-width: 0; }
.post-header { margin-bottom: 8px; }

.post-tags {
  display: flex;
  gap: 6px;
  align-items: center;
  margin-bottom: 6px;
}

.category-tag {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  background: var(--bg-secondary);
  padding: 2px 8px;
  border-radius: var(--radius-xs);
}

.post-title {
  font-size: var(--fs-lg);
  font-weight: 500;
  color: var(--text-primary);
  margin: 0;
  line-height: 1.4;
}

.post-excerpt {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  margin: 0 0 10px;
  line-height: 1.6;
}

.post-images {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 4px;
  margin-bottom: 10px;

  :deep(.el-image) {
    width: 100% !important;
    height: auto !important;
    aspect-ratio: 1;
  }

  .more-images {
    aspect-ratio: 1;
    border-radius: var(--radius-md);
    background: var(--bg-secondary);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: var(--fs-sm);
    color: var(--text-secondary);
  }
}

@media (min-width: 769px) {
  .post-images {
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  }
}

.post-footer {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: var(--fs-xs);
  color: var(--text-secondary);

  .meta-item { display: flex; align-items: center; gap: 4px; }
  .meta-class {
    font-size: var(--fs-xs);
    color: var(--text-secondary);
    background: var(--bg-secondary);
    padding: 1px 6px;
    border-radius: var(--radius-xs);
  }
  .meta-status-tag { flex-shrink: 0; }
  .meta-time { margin-left: auto; font-size: var(--fs-xs); }
  .post-actions { display: flex; gap: 2px; margin-left: 8px; flex-shrink: 0; }
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.img-placeholder {
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
}

@keyframes cardFadeIn {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 768px) {
  .bbs-post-item { padding: 14px; }
  .bbs-post-title { font-size: var(--fs-lg); line-height: 1.4; }
  .bbs-post-summary { font-size: var(--fs-sm); color: var(--text-secondary); display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
  .bbs-post-meta { font-size: var(--fs-xs); gap: 12px; }
}

@media (max-width: 768px) {
  .section-label { font-size: var(--fs-xs); padding: 6px 0 2px; }
  .post-card {
    padding: 14px;
  }
  .post-avatar { display: none; }
  .post-title { font-size: var(--fs-md); }
  .post-excerpt { font-size: var(--fs-xs); }
}
</style>

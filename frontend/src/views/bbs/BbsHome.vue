<template>
  <div class="bbs-layout">
    <div class="bbs-main">
      <div class="bbs-container">
        <div class="category-nav">
          <div class="category-pills">
            <div class="pill" :class="{ active: !currentCategory && !highlightOnly }" @click="switchCategory(null); highlightOnly = false">
              <el-icon size="16"><HomeFilled /></el-icon>
              <span>全部</span>
            </div>
            <div class="pill pill-elite" :class="{ active: highlightOnly }" @click="highlightOnly = !highlightOnly; currentCategory = null; page = 1; loadPosts()">
              <el-icon size="14"><Medal /></el-icon>
              <span>精华</span>
            </div>
            <div
              v-for="cat in categories"
              :key="cat.id"
              class="pill"
              :class="{ active: currentCategory === cat.id }"
              @click="switchCategory(cat.id)"
            >
              <span class="pill-emoji">{{ cat.icon || '📋' }}</span>
              <span>{{ cat.name }}</span>
              <span v-if="cat.postCount > 0" class="pill-count">{{ cat.postCount }}</span>
            </div>
            <div class="pill pill-mine" :class="{ active: showMyContent }" @click="toggleMyContent">
              <el-icon size="14"><UserFilled /></el-icon>
              <span>我的</span>
            </div>
          </div>
        </div>

        <!-- 我的内容区 -->
        <div v-if="showMyContent" class="my-section">
          <div class="my-tabs">
            <button class="my-tab" :class="{ active: myTab === 'posts' }" @click="myTab = 'posts'; loadMyContent()">我的帖子</button>
            <button class="my-tab" :class="{ active: myTab === 'replies' }" @click="myTab = 'replies'; loadMyContent()">我的回复</button>
            <button class="my-tab" :class="{ active: myTab === 'bookmarks' }" @click="myTab = 'bookmarks'; loadMyContent()">我的收藏</button>
          </div>

          <!-- 我的帖子 -->
          <template v-if="myTab === 'posts'">
            <div v-if="myLoading" class="my-loading"><el-skeleton :rows="3" animated /></div>
            <div v-else-if="myData.length === 0" class="my-empty">还没有发布过帖子</div>
            <div
              v-for="item in myData"
              v-else
              :key="item.id"
              class="my-item"
              @click="goDetail(item.id)"
            >
              <span class="my-item-title">{{ item.title }}</span>
              <span class="my-item-meta">{{ item.replyCount || 0 }}回复 · {{ item.viewCount || 0 }}阅读</span>
            </div>
          </template>

          <!-- 我的回复 -->
          <template v-if="myTab === 'replies'">
            <div v-if="myLoading" class="my-loading"><el-skeleton :rows="3" animated /></div>
            <div v-else-if="myData.length === 0" class="my-empty">还没有回复过帖子</div>
            <div
              v-for="item in myData"
              v-else
              :key="item.id"
              class="my-item"
              @click="item.postExists ? goDetail(item.postId) : null"
            >
              <span class="my-item-title">{{ item.postTitle }}</span>
              <span class="my-item-excerpt">{{ truncateText(item.content, 60) }}</span>
            </div>
          </template>

          <!-- 我的收藏 -->
          <template v-if="myTab === 'bookmarks'">
            <div v-if="myLoading" class="my-loading"><el-skeleton :rows="3" animated /></div>
            <div v-else-if="myData.length === 0" class="my-empty">还没有收藏帖子</div>
            <div
              v-for="item in myData"
              v-else
              :key="item.id"
              class="my-item"
              @click="goDetail(item.id)"
            >
              <span class="my-item-title">{{ item.title }}</span>
              <span class="my-item-meta">{{ item.replyCount || 0 }}回复 · {{ item.viewCount || 0 }}阅读</span>
            </div>
          </template>
        </div>

        <div v-if="!showMyContent" class="action-bar">
          <div class="action-left">
            <el-input
              v-model="keyword"
              placeholder="搜索帖子..."
              clearable
              prefix-icon="Search"
              style="width:260px"
              @keyup.enter="search"
            />
            <div class="sort-tabs">
              <button class="sort-btn" :class="{ active: sort === 'latest' }" @click="switchSort('latest')">
                <el-icon size="14"><Timer /></el-icon>最新回复
              </button>
              <button class="sort-btn" :class="{ active: sort === 'hottest' }" @click="switchSort('hottest')">
                <el-icon size="14"><TrendCharts /></el-icon>最热帖子
              </button>
            </div>
          </div>
          <el-button type="primary" @click="router.push('/bbs/create')">
            <el-icon><Plus /></el-icon>发布帖子
          </el-button>
        </div>

        <div v-if="!showMyContent && total > 0" class="stats-bar">
          <span class="stats-text">共 {{ total }} 篇帖子</span>
        </div>

        <BbsPostList
          v-if="!showMyContent"
          v-model:page="page"
          :posts="posts"
          :loading="loading"
          :total="total"
          :has-more="hasMore"
          :categories="categories"
          :is-teacher="isTeacher"
          @go-detail="goDetail"
          @load-more="loadMore"
          @toggle-sticky="onToggleSticky"
          @toggle-highlight="onToggleHighlight"
          @delete-post="onDeletePost"
        />
      </div>
    </div>
    <BbsSidebar
      class="bbs-side"
      :hot-posts="hotPosts"
      :active-users="activeUsers"
      :categories="categories"
      @go-detail="goDetail"
      @switch-category="switchCategory"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCategories, getPosts, getHotPosts, getActiveUsers, getMyPosts, getMyReplies, getMyBookmarks, toggleSticky, toggleHighlight, deletePost } from '@/api/bbs'
import { useUserStore } from '@/stores/user'
import BbsPostList from './BbsPostList.vue'
import BbsSidebar from './BbsSidebar.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isTeacher = computed(() => userStore.isTeacher || userStore.isAdmin)

const categories = ref([])
const posts = ref([])
const loading = ref(false)
const currentCategory = ref(null)
const keyword = ref('')
const sort = ref('latest')
const highlightOnly = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const hotPosts = ref([])
const activeUsers = ref([])
const showMyContent = ref(false)
const myTab = ref('posts')
const myData = ref([])
const myLoading = ref(false)

const hasMore = computed(() => posts.value.length < total.value)

const switchCategory = (id) => { currentCategory.value = id; highlightOnly.value = false; page.value = 1; loadPosts() }
const search = () => { page.value = 1; loadPosts() }
const switchSort = (s) => { sort.value = s; page.value = 1; loadPosts() }

// 教师管理操作
const onToggleSticky = async (post) => {
  try {
    const res = await toggleSticky(post.id)
    if (res.code === 200) { ElMessage.success(res.message || '已切换置顶'); loadPosts() }
    else ElMessage.error(res.message || '操作失败')
  } catch { ElMessage.error('操作失败') }
}
const onToggleHighlight = async (post) => {
  try {
    const res = await toggleHighlight(post.id)
    if (res.code === 200) { ElMessage.success(res.message || '已切换精华'); loadPosts() }
    else ElMessage.error(res.message || '操作失败')
  } catch { ElMessage.error('操作失败') }
}
const onDeletePost = async (post) => {
  try {
    const res = await deletePost(post.id)
    if (res.code === 200) { ElMessage.success('已删除'); loadPosts() }
    else ElMessage.error(res.message || '删除失败')
  } catch { ElMessage.error('删除失败') }
}

const loadPosts = async (append = false) => {
  loading.value = true
  try {
    const res = await getPosts({
      categoryId: currentCategory.value,
      keyword: keyword.value || undefined,
      page: page.value,
      pageSize: pageSize.value,
      sort: sort.value,
      highlightOnly: highlightOnly.value || undefined
    })
    if (res.code === 200) {
      const records = res.data.records || []
      if (append) {
        posts.value = [...posts.value, ...records]
      } else {
        posts.value = records
      }
      total.value = res.data.total || 0
    }
  } catch { ElMessage.error('加载帖子失败，请稍后重试') } finally { loading.value = false }
}

const loadMore = () => {
  page.value++
  loadPosts(true)
}

const loadCategories = async () => {
  try {
    const res = await getCategories()
    if (res.code === 200) categories.value = res.data
  } catch { ElMessage.error('加载版块失败') }
}

const loadSidebarData = async () => {
  try {
    const [hotRes, userRes] = await Promise.all([
      getHotPosts(5),
      getActiveUsers(5)
    ])
    if (hotRes.code === 200) hotPosts.value = hotRes.data
    if (userRes.code === 200) activeUsers.value = userRes.data
  } catch { /* 侧边栏数据非关键路径 */ }
}

const toggleMyContent = () => {
  showMyContent.value = !showMyContent.value
  if (showMyContent.value) { myTab.value = 'posts'; loadMyContent() }
  else { currentCategory.value = null; page.value = 1; loadPosts() }
}

const loadMyContent = async () => {
  myLoading.value = true
  try {
    let res
    if (myTab.value === 'posts') res = await getMyPosts()
    else if (myTab.value === 'replies') res = await getMyReplies()
    else res = await getMyBookmarks()
    if (res.code === 200) myData.value = res.data || []
  } catch { ElMessage.error('加载失败') } finally { myLoading.value = false }
}

const truncateText = (text, len) => {
  if (!text) return ''
  const d = document.createElement('div'); d.innerHTML = text
  const plain = d.textContent || ''
  return plain.length > len ? plain.slice(0, len) + '...' : plain
}

const goDetail = (id) => { router.push(`/bbs/post/${id}`) }

onMounted(() => {
  loadCategories()
  loadSidebarData()
  if (route.params.id) currentCategory.value = Number(route.params.id)
  loadPosts()
})
</script>

<style scoped lang="scss">
.bbs-layout { display: flex; gap: 24px; max-width: 1200px; margin: 0 auto; align-items: flex-start; }
.bbs-main { flex: 1; min-width: 0; }
.bbs-side { width: 280px; flex-shrink: 0; position: sticky; top: 76px; }

.bbs-container {
  max-width: 960px;
  margin: 0 auto;
}

.category-nav { margin-bottom: 16px; }
.category-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.pill {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: var(--radius-xl);
  cursor: pointer;
  font-size: var(--fs-sm);
  transition: all 0.2s;
  background: var(--bg-secondary);
  color: var(--text-regular);
  .pill-emoji { font-size: var(--fs-md); line-height: 1; }
  &:hover { background: var(--primary-light); color: var(--primary-color); }
  &.active { background: var(--primary-color); color: var(--bg-card); }
  .pill-count {
    font-size: var(--fs-xs);
    background: rgba(255,255,255,0.2);
    padding: 0 6px;
    border-radius: var(--radius-md);
  }
  &.active .pill-count { background: rgba(255,255,255,0.25); }
}
.pill-elite {
  &.active { background: linear-gradient(135deg, #f093fb, #f5576c); color: #fff; }
}
.pill-mine {
  margin-left: auto;
  &.active { background: var(--primary-color); color: var(--bg-card); }
}

.my-section {
  margin-bottom: 20px;
}
.my-tabs {
  display: flex;
  gap: 2px;
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  padding: 3px;
  margin-bottom: 16px;
  width: fit-content;
}
.my-tab {
  padding: 6px 14px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: var(--fs-sm);
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: all 0.2s;
  &:hover { color: var(--text-primary); }
  &.active { background: var(--bg-card); color: var(--primary-color); font-weight: 500; box-shadow: var(--shadow-sm); }
}
.my-loading { padding: 16px 0; }
.my-empty { text-align: center; padding: 40px 0; color: var(--text-secondary); font-size: var(--fs-md); }
.my-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 16px; background: var(--bg-card); border-radius: var(--radius-md);
  border: 0.5px solid var(--border-light); cursor: pointer; margin-bottom: 8px;
  transition: all 0.2s;
  &:hover { border-color: var(--primary-color); box-shadow: var(--shadow-sm); }
}
.my-item-title { font-size: var(--fs-md); color: var(--text-primary); flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.my-item-meta { font-size: var(--fs-xs); color: var(--text-secondary); flex-shrink: 0; margin-left: 12px; }
.my-item-excerpt { font-size: var(--fs-xs); color: var(--text-secondary); flex-shrink: 0; margin-left: 12px; max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 12px;
}
.action-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.sort-tabs {
  display: flex;
  gap: 2px;
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  padding: 3px;
}
.sort-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: var(--fs-xs);
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: all 0.2s;
  white-space: nowrap;
  &:hover { color: var(--text-primary); }
  &.active {
    background: var(--bg-card);
    color: var(--primary-color);
    font-weight: 500;
    box-shadow: var(--shadow-sm);
  }
}

.stats-bar {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  .stats-text { font-size: var(--fs-sm); color: var(--text-secondary); }
}

@media (max-width: 768px) {
  .stats-bar { margin-bottom: 8px; }
  .action-bar {
    flex-direction: column; gap: 10px; align-items: stretch;
    .el-input { width: 100% !important; }
  }
  .action-left { flex-direction: column; align-items: stretch; gap: 10px; }
  .sort-tabs { align-self: flex-start; }
  .category-pills { gap: 6px; }
  .pill { padding: 5px 10px; font-size: var(--fs-xs); }
}

@media (max-width: 1024px) {
  .bbs-side { display: none; }
  .bbs-layout { max-width: 100%; }
}
</style>

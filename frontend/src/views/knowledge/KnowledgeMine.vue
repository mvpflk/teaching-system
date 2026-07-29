<template>
  <div class="mine-page">
    <div class="page-header">
      <h2>📊 我的学习</h2>
      <span class="subject-tag" v-if="subjectName">{{ subjectName }}</span>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card stat-card--streak">
        <div class="stat-icon">🔥</div>
        <div class="stat-value">{{ dailyStats.streak || 0 }}<small> 天</small></div>
        <div class="stat-label">连续学习</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">📖</div>
        <div class="stat-value">{{ progress.studiedArticles || 0 }}</div>
        <div class="stat-label">已学文章</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">✅</div>
        <div class="stat-value">{{ progress.masteredCards || 0 }}</div>
        <div class="stat-label">已掌握卡片</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">📝</div>
        <div class="stat-value">{{ dailyStats.todayQuizzes || 0 }}</div>
        <div class="stat-label">今日自测</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">🎯</div>
        <div class="stat-value">{{ dailyStats.avgScore || '--' }}</div>
        <div class="stat-label">近5次均分</div>
      </div>
      <div class="stat-card stat-card--review">
        <div class="stat-icon">🕐</div>
        <div class="stat-value">{{ progress.todayReview || 0 }}</div>
        <div class="stat-label">待复习卡片</div>
      </div>
    </div>

    <!-- 薄弱标签 + 推荐文章 -->
    <div v-if="isStudent" class="dashboard-sections">
      <div class="section-card section-card--weak">
        <div class="section-header">
          <h3>🎯 薄弱知识点</h3>
          <span class="section-hint">基于自测错题聚合</span>
        </div>
        <div v-if="weakTags.length" class="tag-cloud">
          <span
            v-for="t in weakTags"
            :key="t.tag"
            class="tag-item"
            :style="{ fontSize: getTagSize(t.count), opacity: getTagOpacity(t.count) }"
            :title="`${t.tag}（${t.count} 次错误）`"
          >{{ t.tag }}</span>
        </div>
        <el-empty v-else description="暂无薄弱数据，多做自测题吧" :image-size="60" />
      </div>

      <div v-if="reviewItems.length" class="section-card section-card--review">
        <div class="section-header"><h3>🔄 需复习</h3><span class="section-hint">之前得分较低，建议重做</span></div>
        <div class="recommend-list">
          <div v-for="a in reviewItems" :key="a.id" class="recommend-item" @click="$router.push(`/knowledge-base/article/${a.id}`)">
            <div class="rec-title">{{ a.title }} <el-tag size="small" type="danger" effect="dark" round>需复习</el-tag></div>
            <div class="rec-meta">
              <el-tag :type="difficultyType(a.difficulty)" size="small" round>{{ difficultyLabel(a.difficulty) }}</el-tag>
              <span class="rec-chapter">{{ a.chapter }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="section-card section-card--recommend">
        <div class="section-header">
          <h3>📚 推荐阅读</h3>
          <span v-if="recommendReason" class="section-hint">{{ recommendReason }}</span>
        </div>
        <div v-if="normalRecs.length" class="recommend-list">
          <div
            v-for="a in normalRecs"
            :key="a.id"
            class="recommend-item"
            @click="$router.push(`/knowledge-base/article/${a.id}`)"
          >
            <div class="rec-title">{{ a.title }}</div>
            <div class="rec-meta">
              <el-tag :type="difficultyType(a.difficulty)" size="small" round>
                {{ difficultyLabel(a.difficulty) }}
              </el-tag>
              <span class="rec-chapter">{{ a.chapter }}</span>
              <span v-if="a.matchCount" class="rec-match">匹配度 +{{ a.matchCount }}</span>
            </div>
            <div class="rec-excerpt">{{ a.excerpt || '暂无摘要' }}</div>
          </div>
        </div>
        <el-empty v-else description="暂无推荐，浏览全部文章吧" :image-size="60" />
      </div>
    </div>

    <!-- 收藏 Tab -->
    <div class="fav-section">
      <el-tabs v-model="tab">
        <el-tab-pane label="⭐ 我的收藏" name="favs">
          <div v-if="favorites.length" class="fav-grid">
            <ArticleCard v-for="a in favorites" :key="a.id" :article="a" />
          </div>
          <el-empty v-else description="还没有收藏文章" :image-size="80" />
          <div v-if="favorites.length" class="fav-footer">
            <el-button text type="primary" @click="$router.push('/knowledge-base/discover')">
              去发现更多文章 →
            </el-button>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProgress, getFavorites } from '@/api/knowledgeBase'
import { getDailyStats, getWeakAnalysis, getRecommendations } from '@/api/knowledgeBase'
import { useUserStore } from '@/stores/user'
import ArticleCard from '@/components/knowledge/ArticleCard.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isStudent = computed(() => userStore.isStudent)
const SUBJECT_ID = computed(() => Number(route.query.subjectId) || 24)

const tab = ref('favs')
const progress = ref({})
const favorites = ref([])
const dailyStats = ref({})
const weakTags = ref([])
const recommendations = ref([])
const recommendReason = ref('')
const reviewItems = computed(() => recommendations.value.filter(a => a.needsReview))
const normalRecs = computed(() => recommendations.value.filter(a => !a.needsReview))

const subjectName = computed(() => {
  const names = { 24: '英语[职高]' }
  return names[SUBJECT_ID.value] || ''
})

function difficultyType(v) {
  return ['', '', 'warning', 'danger', ''][v] || 'info'
}
function difficultyLabel(v) {
  return ['', '基础', '中等', '困难', '挑战'][v] || '未知'
}
function getTagSize(count) {
  const c = Math.min(count, 10)
  return `${12 + c * 1.2}px`
}
function getTagOpacity(count) {
  return Math.min(0.5 + count * 0.05, 1)
}

async function loadData() {
  if (!isStudent.value) return
  try {
    const [pR, dR, wR, rR] = await Promise.allSettled([
      getProgress(SUBJECT_ID.value),
      getDailyStats(SUBJECT_ID.value),
      getWeakAnalysis(SUBJECT_ID.value),
      getRecommendations(SUBJECT_ID.value),
    ])
    if (pR.status === 'fulfilled') progress.value = pR.value.data || {}
    else console.error('进度加载失败:', pR.reason)
    if (dR.status === 'fulfilled') dailyStats.value = dR.value.data || {}
    else console.error('dailyStats加载失败:', dR.reason)
    if (wR.status === 'fulfilled') {
      weakTags.value = wR.value.data?.weakTags || []
    } else console.error('weakAnalysis加载失败:', wR.reason)
    if (rR.status === 'fulfilled') {
      const data = rR.value.data || []
      recommendations.value = data.slice(0, 5)
      recommendReason.value = weakTags.value.length
        ? '基于薄弱知识点推荐'
        : '推荐基础文章'
    } else console.error('recommendations加载失败:', rR.reason)
  } catch (e) {
    console.error('加载学习数据失败:', e)
  }
  try {
    const r = await getFavorites()
    favorites.value = r.data || []
  } catch (e) {
    console.error('加载收藏失败:', e)
  }
}

onMounted(loadData)
watch(() => route.query.subjectId, loadData)
</script>

<style scoped>
.mine-page {
  max-width: 960px;
  margin: 0 auto;
}
.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}
.page-header h2 {
  margin: 0;
  font-size: var(--fs-xl, 20px);
}
.subject-tag {
  padding: 2px 10px;
  background: var(--primary-light, var(--primary-light));
  color: var(--primary-color, var(--primary-color));
  border-radius: 12px;
  font-size: var(--fs-xs, 12px);
  font-weight: 500;
}
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}
.stat-card {
  background: var(--bg-card, #fff);
  border: 1px solid var(--border-light, #eee);
  border-radius: var(--radius-md, 12px);
  padding: 18px 16px;
  text-align: center;
  transition: transform 0.2s, box-shadow 0.2s;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}
.stat-card--streak {
  background: var(--bg-warning-light);
  border-color: var(--el-color-warning);
}
.stat-card--review {
  background: var(--bg-danger-light);
  border-color: var(--el-color-danger);
}
.stat-icon { font-size: 24px; margin-bottom: 6px; }
.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: var(--primary-color, var(--primary-color));
}
.stat-value small { font-size: 14px; font-weight: 400; color: var(--text-secondary, #666); }
.stat-card--streak .stat-value { color: var(--el-color-warning); }
.stat-card--review .stat-value { color: var(--el-color-danger); }
.stat-label {
  font-size: var(--fs-xs, 12px);
  color: var(--text-secondary, #666);
  margin-top: 4px;
}

.dashboard-sections {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 24px;
}
@media (max-width: 768px) {
  .dashboard-sections { grid-template-columns: 1fr; }
}
.section-card {
  background: var(--bg-card, #fff);
  border: 1px solid var(--border-light, #eee);
  border-radius: var(--radius-md, 12px);
  padding: 16px;
}
.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}
.section-header h3 {
  margin: 0;
  font-size: var(--fs-md, 15px);
}
.section-hint {
  font-size: var(--fs-xs, 11px);
  color: var(--text-secondary, #999);
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  padding: 4px 0;
  align-items: center;
  min-height: 60px;
}
.tag-item {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  background: var(--primary-light, var(--primary-light));
  color: var(--primary-color, var(--primary-color));
  cursor: default;
  transition: all 0.2s;
  font-weight: 500;
  line-height: 1.4;
}
.tag-item:hover {
  transform: scale(1.05);
  box-shadow: 0 2px 8px rgba(67, 97, 238, 0.15);
}

.recommend-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.recommend-item {
  padding: 12px;
  border-radius: var(--radius-sm, 8px);
  border: 1px solid var(--border-light, #eee);
  cursor: pointer;
  transition: all 0.2s;
}
.recommend-item:hover {
  border-color: var(--primary-color, var(--primary-color));
  background: var(--primary-light, #f5f6ff);
}
.rec-title {
  font-size: var(--fs-sm, 14px);
  font-weight: 600;
  color: var(--text-primary, #333);
  margin-bottom: 4px;
}
.rec-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  flex-wrap: wrap;
}
.rec-chapter {
  font-size: var(--fs-xs, 11px);
  color: var(--text-secondary, #999);
}
.rec-match {
  font-size: var(--fs-xs, 11px);
  color: var(--el-color-success, #67c23a);
}
.rec-excerpt {
  font-size: var(--fs-xs, 12px);
  color: var(--text-secondary, #999);
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.fav-section {
  margin-top: 8px;
}
.fav-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}
.fav-footer {
  text-align: center;
  margin-top: 16px;
}
@media (max-width: 768px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .fav-grid { grid-template-columns: 1fr; }
}
@media (max-width: 480px) {
  .stats-grid { grid-template-columns: 1fr; }
}
</style>

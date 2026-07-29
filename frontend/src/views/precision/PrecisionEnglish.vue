<template>
  <div class="pe-page">
    <div class="pe-header">
      <el-button text @click="$router.back()">← 返回</el-button>
      <h2>英语提分</h2>
    </div>

    <!-- 火苗条 -->
    <StreakFlame :streak="dash.streak" :freeze-cards="dash.freezeCards" @use-freeze="handleUseFreeze" />

    <!-- 七阶段进度 -->
    <EnglishStageBar :stage="dash.stage" :progress="dash.stageProgress" />

    <!-- 今日任务卡片 -->
    <EnglishDailyTaskCard v-if="dash.dailyTask" :task="dash.dailyTask" :stage-name="dash.stageName">
      <el-button
        type="primary"
        size="large"
        style="margin-top:12px;width:100%"
        @click="startDrill"
      >
        开始今日练习 → ({{ dash.dailyTask.totalQuestions }}题)
      </el-button>
    </EnglishDailyTaskCard>

    <!-- 月学习日历 -->
    <LearningCalendar :practiced="recentPracticeDates" />

    <!-- 班级周榜（阶段3+） -->
    <ClassEnglishRanking v-if="dash.stage >= 3" :ranking="ranking" />

    <!-- 统计行 -->
    <div class="pe-stats">
      <div class="pe-stat"><span class="pe-sn">{{ dash.vocabKnown }}</span><span class="pe-sl">词汇量</span></div>
      <div class="pe-stat"><span class="pe-sn">{{ dash.totalPractices }}</span><span class="pe-sl">练习次数</span></div>
      <div class="pe-stat"><span class="pe-sn">{{ dash.longestStreak }}</span><span class="pe-sl">最长连续</span></div>
    </div>

    <!-- 我的词汇本入口 -->
    <div class="pe-vocab-entry" @click="showVocabBook = true">
      📒 我的词汇本（{{ dash.vocabKnown }} 词）
    </div>

    <!-- 成就墙 -->
    <AchievementWall v-if="dash.achievements && dash.achievements.length" :achievements="dash.achievements" />

    <!-- AI 故事（阶段6+） -->
    <AiStoryCard v-if="dash.stage >= 6" :story="aiStory" />

    <el-empty v-if="!dash.dailyTask" description="请先完成英语诊断测试" :image-size="60" />

    <!-- 词汇本抽屉 -->
    <VocabBookDrawer v-model="showVocabBook" :groups="vocabBookGroups" />
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getEnglishDashboard, useFreezeCard, getEnglishRanking, getVocabBook } from '@/api/precisionEnglish'
import StreakFlame from '@/components/precision/StreakFlame.vue'
import EnglishStageBar from '@/components/precision/EnglishStageBar.vue'
import EnglishDailyTaskCard from '@/components/precision/EnglishDailyTaskCard.vue'
import LearningCalendar from '@/components/precision/LearningCalendar.vue'
import ClassEnglishRanking from '@/components/precision/ClassEnglishRanking.vue'
import AchievementWall from '@/components/precision/AchievementWall.vue'
import VocabBookDrawer from '@/components/precision/VocabBookDrawer.vue'
import AiStoryCard from '@/components/precision/AiStoryCard.vue'

const router = useRouter()
const dash = ref({ stage: 1, stageName: '', vocabKnown: 0, vocabTotal: 500, streak: 0,
  freezeCards: 0, longestStreak: 0, totalPractices: 0, dailyTask: null, achievements: [],
  stageProgress: {} })
const ranking = ref([])
const recentPracticeDates = ref([])
const showVocabBook = ref(false)
const vocabBookGroups = ref([])
const aiStory = ref(null)

const loadAll = async () => {
  try {
    const r = await getEnglishDashboard()
    if (r.code === 200) dash.value = r.data
  } catch (e) {
    console.error('加载英语仪表盘失败:', e)
  }
  // R112修复：Dashboard 加载后立即派生非依赖数据，并行请求 ranking
  const promises = []
  if (dash.value.stage >= 3) {
    promises.push(
      getEnglishRanking().then(r2 => { if (r2.code === 200) ranking.value = r2.data || [] }).catch(() => {})
    )
  }
  await Promise.allSettled(promises)
  // 练习日期：优先后端真实数据，兜底前端估算
  const realDates = dash.value.practiceDates
  if (realDates && realDates.length) {
    recentPracticeDates.value = realDates
  } else {
    const dates = []
    const today = new Date()
    for (let i = 0; i < (dash.value.totalPractices || 0); i++) {
      const d = new Date(today); d.setDate(d.getDate() - i)
      dates.push(d.toISOString().slice(0, 10))
    }
    recentPracticeDates.value = dates
  }
}

const handleUseFreeze = async () => {
  try {
    await ElMessageBox.confirm('确定使用一张冰冻卡保住火苗吗？', '使用冰冻卡', { confirmButtonText: '使用 🧊' })
    const r = await useFreezeCard()
    if (r.code === 200) {
      dash.value.freezeCards = r.data.freezeCards
      dash.value.streak = r.data.streak
      ElMessage.success('火苗保住了！')
    } else {
      ElMessage.error(r.message || '使用失败')
    }
  } catch (e) {
    // R112修复：区分用户取消和API错误
    if (e !== 'cancel' && e !== 'close' && e?.message !== 'cancel') {
      ElMessage.error('操作失败，请重试')
    }
  }
}

const loadVocabBook = async () => {
  try {
    const r = await getVocabBook()
    if (r.code === 200 && r.data) {
      vocabBookGroups.value = r.data.groups || []
    }
  } catch (e) {
    console.error('加载词汇书失败:', e)
  }
}

const startDrill = () => {
  if (dash.value.dailyTask) {
    sessionStorage.setItem('english_daily_task', JSON.stringify({ ...dash.value.dailyTask, _ts: Date.now() }))
    router.push('/precision/english/drill')
  }
}

watch(showVocabBook, (v) => { if (v) loadVocabBook() })

onMounted(loadAll)
</script>

<style scoped>
.pe-page { margin: 0 auto; padding: 16px; min-height: 100vh; background: var(--bg-page, var(--bg-page)); }
.pe-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.pe-header h2 { margin: 0; font-size: var(--fs-xl); font-weight: 600; }
.pe-stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-bottom: 14px; }
.pe-stat { text-align: center; padding: 10px; background: var(--bg-card, #fff); border: 1px solid var(--border-base, #e8e8ed); border-radius: var(--radius-md, 8px); }
.pe-sn { display: block; font-size: var(--fs-xl); font-weight: 700; color: var(--text-primary, var(--text-primary)); }
.pe-sl { display: block; font-size: var(--fs-xs); color: var(--text-secondary, var(--text-secondary)); margin-top: 2px; }
.pe-vocab-entry { padding: 12px 16px; background: var(--bg-card, #fff); border: 1px solid var(--border-base, #e8e8ed); border-radius: 8px; margin-bottom: 14px; cursor: pointer; font-size: var(--fs-md); color: var(--primary-color); font-weight: 500; }
</style>

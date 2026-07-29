<template>
  <div class="sg-page">
    <!-- Header -->
    <div class="sg-header">
      <h2 class="sg-title">成长记录</h2>
      <div class="sg-header-actions">
        <el-select
          v-model="subject"
          placeholder="选择学科"
          size="default"
          style="width:180px"
          @change="loadAll"
        >
          <el-option
            v-for="s in subjectList"
            :key="s"
            :label="s"
            :value="s"
          />
        </el-select>
        <el-button text type="primary" @click="$router.push('/student/precision')">
          偏科提分 <span class="sg-arrow">→</span>
        </el-button>
      </div>
    </div>

    <!-- Empty state — 无数据时显示 -->
    <div v-if="!hasPrecisionData" class="sg-empty-hero">
      <div class="sg-empty-icon">· · ·</div>
      <div class="sg-empty-text">还没有学习数据</div>
      <div class="sg-empty-hint">完成偏科提分诊断后，这里将展示你的成长轨迹</div>
      <el-button size="default" type="primary" @click="$router.push('/student/precision')">
        开始第一次诊断
      </el-button>
    </div>

    <!-- 有数据时 — 完整布局 -->
    <template v-if="hasPrecisionData">
      <!-- 🏆 成就墙 — 顶部突出展示 -->
      <div v-if="highlightAchievements.length > 0" class="sg-achievement-wall">
        <div class="sg-aw-header">
          <span class="sg-aw-title">🏆 成长里程碑</span>
          <span class="sg-aw-sub">{{ earnedCount }}/{{ totalCount }} 项已解锁</span>
        </div>
        <div class="sg-aw-grid">
          <div
            v-for="a in displayAchievements"
            :key="a.name"
            :class="['sg-aw-card', { 'sg-aw-earned': a.earned, 'sg-aw-new': a.isNew }]"
          >
            <div class="sg-aw-icon-wrap">
              <span class="sg-aw-icon">{{ achievementIcon(a) }}</span>
              <span v-if="a.isNew" class="sg-aw-badge">NEW</span>
            </div>
            <div class="sg-aw-info">
              <div class="sg-aw-name">{{ a.name }}</div>
              <div class="sg-aw-desc">{{ a.description }}</div>
            </div>
            <div v-if="a.earned" class="sg-aw-check">✅</div>
            <div v-else class="sg-aw-lock">🔒</div>
          </div>
        </div>
      </div>

      <!-- 里程碑时间线 (SIGNATURE) -->
      <div class="sg-timeline-wrap">
        <div ref="timelineRef" class="sg-timeline">
          <div
            v-for="(m, i) in milestones"
            :key="i"
            class="sg-milestone"
            :class="{ reached: m.reached, current: m.current }"
            :style="{ animationDelay: (i * 80) + 'ms' }"
          >
            <div class="sg-milestone-dot">
              <span v-if="m.reached" class="sg-milestone-check">✓</span>
            </div>
            <div class="sg-milestone-label">{{ m.label }}</div>
            <div class="sg-milestone-detail">{{ m.detail || '—' }}</div>
          </div>
        </div>
      </div>

      <!-- Hero Stats — 无边框数字集群 -->
      <div class="sg-hero">
        <div class="sg-hero-main">
          <div class="sg-hero-number">
            <span ref="heroNumRef">{{ displayPercent }}</span>
            <span class="sg-hero-number-unit">%</span>
          </div>
          <div class="sg-hero-label">知识点掌握率</div>
          <div class="sg-hero-sub">
            {{ summary.masteredNodes || 0 }} / {{ summary.totalNodes || 0 }} 个知识点
          </div>
        </div>
        <div class="sg-hero-divider"></div>
        <div class="sg-hero-stats">
          <div class="sg-hero-stat">
            <div class="sg-hero-stat-num">{{ summary.totalPractices || 0 }}</div>
            <div class="sg-hero-stat-label">次练习</div>
          </div>
          <div class="sg-hero-stat">
            <div class="sg-hero-stat-num">{{ summary.streakWeeks || 0 }}</div>
            <div class="sg-hero-stat-label">连续周</div>
          </div>
          <div class="sg-hero-stat">
            <div class="sg-hero-stat-num">{{ summary.lastDiagnoseScore || 0 }}</div>
            <div class="sg-hero-stat-label">诊断分</div>
          </div>
        </div>
      </div>

      <!-- 双图表 -->
      <div v-loading="chartLoading" class="sg-charts">
        <div class="sg-chart-block">
          <div class="sg-section-label">成长曲线</div>
          <GrowthCurveChart :subject="subject" />
        </div>
        <div class="sg-chart-block">
          <div class="sg-section-label">知识雷达</div>
          <KnowledgeRadarChart :subject="subject" />
        </div>
      </div>

      <!-- 成就徽章 · 行内紧凑排列 -->
      <div v-if="allAchievements.length > 0" v-loading="achievementsLoading" class="sg-achievements">
        <div class="sg-section-label">
          已获徽章
          <span class="sg-achievements-count">{{ earnedCount }}/{{ totalCount }}</span>
        </div>
        <div class="sg-achievements-row">
          <div
            v-for="a in sortedAchievements"
            :key="a.name"
            class="sg-ach-item"
            :class="{ earned: a.earned }"
          >
            <span class="sg-ach-icon">{{ categoryIcon(a.category) }}</span>
            <span class="sg-ach-name">{{ a.name }}</span>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import confetti from 'canvas-confetti'
import GrowthCurveChart from '@/components/analytics/GrowthCurveChart.vue'
import KnowledgeRadarChart from '@/components/analytics/KnowledgeRadarChart.vue'
import { getStudentAchievements, getStudentDailyEncouragement, getStudentSummary, getStudentAvailableSubjects } from '@/api/analytics'
import { primaryColor, elSuccess, elWarning, elDanger } from '@/utils/theme'

const router = useRouter()

const subject = ref('')
const subjectList = ref([])

const chartLoading = ref(true)
const achievementsLoading = ref(true)
const encouragement = ref(null)
const allAchievements = ref([])
const summary = ref({})

const heroNumRef = ref(null)
const timelineRef = ref(null)
const displayPercent = ref(0)
const prevMasteredNodes = ref(0) // track previous mastery for breakthrough detection
const firedConfetti = ref(false) // prevent double-firing

const earnedCount = computed(() => allAchievements.value.filter(a => a.earned).length)
const totalCount = computed(() => allAchievements.value.length)
const hasPrecisionData = computed(() => summary.value.totalNodes > 0 || summary.value.totalPractices > 0)

// 顶部突出展示的成就（4个偏科提分专项）
const displayAchievements = computed(() => {
  const practiceCats = ['practice', 'photo']
  return allAchievements.value.filter(a => practiceCats.includes(a.category))
})
// 底部原有成就徽章（保持兼容）
const highlightAchievements = computed(() => displayAchievements.value.filter(a => a.earned))


const percentValue = computed(() => {
  if (!summary.value.totalNodes || summary.value.totalNodes === 0) return 0
  return Math.round((summary.value.masteredNodes || 0) / summary.value.totalNodes * 100)
})

// 成就：已达成前置
const sortedAchievements = computed(() => {
  return [...allAchievements.value].sort((a, b) => (b.earned ? 1 : 0) - (a.earned ? 1 : 0))
})

// 里程碑：从 summary 数据推导
const milestones = computed(() => {
  const s = summary.value
  if (!s) return []

  const items = [
    { label: '首次练习', reached: s.totalPractices >= 1, detail: s.totalPractices >= 1 ? `${s.totalPractices} 次` : '' },
    { label: '持续一周', reached: s.streakWeeks >= 1, detail: s.streakWeeks >= 1 ? `${s.streakWeeks} 周` : '' },
    { label: '掌握首个知识点', reached: s.masteredNodes >= 1, detail: s.masteredNodes >= 1 ? `${s.masteredNodes} 个` : '' },
    { label: '诊断分达标', reached: s.lastDiagnoseScore >= 60, detail: s.lastDiagnoseScore >= 60 ? `${s.lastDiagnoseScore} 分` : '' },
    { label: '掌握过半', reached: s.masteredNodes > 0 && s.totalNodes > 0 && (s.masteredNodes / s.totalNodes) >= 0.5, detail: s.totalNodes ? `${Math.round(s.masteredNodes / s.totalNodes * 100)}%` : '' }
  ]
  // current = 第一个未达成的节点
  const firstUnreached = items.findIndex(m => !m.reached)
  return items.map((m, i) => ({
    ...m,
    current: firstUnreached >= 0 ? i === firstUnreached : i === items.length - 1
  }))
})

function categoryIcon(cat) {
  return { credit: '💰', task: '📝', sign: '🔥', grade: '⭐', wrong: '🎯', practice: '📚', photo: '📷' }[cat] || '🏆'
}

function achievementIcon(a) {
  const map = {
    '坚持不懈': '🔥', '初露锋芒': '🚀', '融会贯通': '🧠', '科技达人': '📷',
    '积分新手': '💰', '积分达人': '💰', '积分大师': '🏆', '积分传奇': '👑',
    '初次任务': '📝', '勤奋好学': '📚', '学霸': '🎓',
    '初来乍到': '🔥', '签到王者': '👑', 'A+达人': '⭐',
    '半壁江山': '🎯', '横扫千军': '🏅'
  }
  return map[a.name] || '🌟'
}

// 检测突破：某知识点掌握率首次达到 >=80%
function checkBreakthrough(newSummary) {
  if (firedConfetti.value) return
  const prev = prevMasteredNodes.value
  const curr = newSummary.masteredNodes || 0
  const total = newSummary.totalNodes || 1
  const currRate = curr / total
  const prevRate = prev / total

  // 突破条件: 掌握率从 <60% 跨越到 >=80% (或直接首次 >=80%)
  if (currRate >= 0.8 && prevRate < 0.8) {
    fireConfetti()
  }
  prevMasteredNodes.value = curr
}

function fireConfetti() {
  firedConfetti.value = true
  const duration = 3000
  const end = Date.now() + duration
  const colors = [primaryColor, elDanger, '#4cc9f0', elSuccess, elWarning]

  ;(function frame() {
    confetti({
      particleCount: 3,
      angle: 60,
      spread: 55,
      origin: { x: 0, y: 0.6 },
      colors: colors,
    })
    confetti({
      particleCount: 3,
      angle: 120,
      spread: 55,
      origin: { x: 1, y: 0.6 },
      colors: colors,
    })
    if (Date.now() < end) requestAnimationFrame(frame)
  })()

  // 中心爆发
  setTimeout(() => {
    confetti({ particleCount: 100, spread: 100, origin: { y: 0.4 }, colors: colors })
  }, 500)
}

// 数字 count-up 动画
function animateHeroNumber(target) {
  const duration = 200
  const start = performance.now()
  const from = displayPercent.value
  function step(now) {
    const elapsed = now - start
    const progress = Math.min(elapsed / duration, 1)
    const eased = 1 - Math.pow(1 - progress, 3) // ease-out cubic
    displayPercent.value = Math.round(from + (target - from) * eased)
    if (progress < 1) requestAnimationFrame(step)
  }
  requestAnimationFrame(step)
}

async function loadSubjects() {
  try {
    const res = await getStudentAvailableSubjects()
    if (res.code === 200 && res.data?.length > 0) {
      subjectList.value = res.data
      if (!subject.value) subject.value = res.data[0]
    } else {
      // 兜底：从 summary 数据推断学科（延迟加载）
      try {
        const sumRes = await getStudentSummary()
        if (sumRes.code === 200 && sumRes.data?.lastSubject) {
          subjectList.value = [sumRes.data.lastSubject]
          if (!subject.value) subject.value = sumRes.data.lastSubject
        }
      } catch {}
    }
  } catch {}
}

async function loadAll() {
  chartLoading.value = true
  achievementsLoading.value = true

  let sumRes = null, encRes = null, achRes = null
  try {
    [sumRes, encRes, achRes] = await Promise.all([
      getStudentSummary(subject.value),
      getStudentDailyEncouragement(),
      getStudentAchievements()
    ])
    if (sumRes.code === 200) {
      summary.value = sumRes.data || {}
      await nextTick()
      animateHeroNumber(percentValue.value)
      checkBreakthrough(sumRes.data || {})
    }
    if (encRes.code === 200) encouragement.value = encRes.data
    if (achRes.code === 200 && achRes.data?.all) {
      const prevEarned = new Set(
        allAchievements.value.filter(a => a.earned).map(a => a.name)
      )
      allAchievements.value = achRes.data.all.map(a => ({
        ...a,
        isNew: a.earned && !prevEarned.has(a.name)
      }))
      // 检查是否有新解锁的突破成就 → 触发彩带
      const newBreakthrough = allAchievements.value.find(a =>
        a.isNew && (a.name === '融会贯通' || a.name === '初露锋芒')
      )
      if (newBreakthrough && !firedConfetti.value) {
        fireConfetti()
      }
    }
  } catch {} finally {
    chartLoading.value = false
    achievementsLoading.value = false
  }
}

onMounted(() => { loadSubjects(); loadAll() })
watch(() => subject.value, loadAll)
</script>

<style scoped>
/* ========== 页面容器 ========== */
.sg-page { max-width: 960px; margin: 0 auto; padding: 24px; }

/* ========== Header ========== */
.sg-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; margin-bottom: 24px; }
.sg-title { margin: 0; font-size: var(--fs-xl); font-weight: 600; color: var(--text-primary, var(--text-primary)); letter-spacing: -0.01em; }
.sg-header-actions { display: flex; align-items: center; gap: 10px; }
.sg-arrow { font-family: -apple-system, BlinkMacSystemFont, sans-serif; }

/* ========== 空状态 ========== */
.sg-empty-hero { text-align: center; padding: 64px 24px; }
.sg-empty-icon { font-size: 32px; color: var(--text-disabled, var(--text-disabled)); letter-spacing: 8px; margin-bottom: 12px; }
.sg-empty-text { font-size: var(--fs-lg); font-weight: 600; color: var(--text-primary, var(--text-primary)); margin-bottom: 6px; }
.sg-empty-hint { font-size: var(--fs-sm); color: var(--text-secondary, var(--text-secondary)); margin-bottom: 20px; }

/* ========== 里程碑时间线 SIGNATURE ========== */
.sg-timeline-wrap { margin-bottom: 28px; }
.sg-timeline {
  display: flex; gap: 0; overflow-x: auto; padding: 8px 0 12px;
  scrollbar-width: thin; scrollbar-color: var(--border-input) transparent;
  position: relative;
}
.sg-timeline::-webkit-scrollbar { height: 4px; }
.sg-timeline::-webkit-scrollbar-thumb { background: var(--border-input); border-radius: 2px; }

/* 连接线 — 时间线容器的水平中线 */
.sg-timeline::before {
  content: ''; position: absolute; top: 16px; left: 0; right: 0; height: 1px;
  background: var(--border-base, var(--border-color));
  z-index: 0; min-width: 100%;
}

.sg-milestone {
  flex: 0 0 auto; width: 80px; text-align: center; position: relative; z-index: 1;
  opacity: 0; animation: sg-fade-up 0.2s ease-out forwards;
}
@keyframes sg-fade-up {
  from { opacity: 0; transform: translateY(6px); }
  to   { opacity: 1; transform: translateY(0); }
}

.sg-milestone-dot {
  width: 32px; height: 32px; margin: 0 auto 8px; border-radius: 50%;
  background: var(--bg-card, #fff);
  border: 1.5px solid var(--border-base, var(--border-color));
  display: flex; align-items: center; justify-content: center; transition: all 0.2s;
}
.sg-milestone.reached .sg-milestone-dot {
  background: var(--primary-color, var(--primary-color)); border-color: var(--primary-color, var(--primary-color));
}
.sg-milestone.current:not(.reached) .sg-milestone-dot {
  border-color: var(--primary-color, var(--primary-color));
  animation: sg-pulse 2s ease-in-out infinite;
}
@keyframes sg-pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(67,97,238,0.2); }
  50%      { box-shadow: 0 0 0 6px rgba(67,97,238,0); }
}

.sg-milestone-check { font-size: var(--fs-md); color: #fff; line-height: 1; }

.sg-milestone-label { font-size: var(--fs-xs); font-weight: 600; color: var(--text-primary, var(--text-primary)); margin-bottom: 2px; white-space: nowrap; }
.sg-milestone-detail { font-size: 10px; color: var(--text-secondary, var(--text-secondary)); white-space: nowrap; }
.sg-milestone:not(.reached) .sg-milestone-label { color: var(--text-secondary, var(--text-secondary)); }

/* ========== Hero Stats ========== */
.sg-hero {
  display: flex; align-items: center; gap: 32px; padding: 28px 0;
  border-bottom: 1px solid var(--border-light, var(--border-light));
  margin-bottom: 28px;
}
.sg-hero-main { flex-shrink: 0; }
.sg-hero-number { font-size: 56px; font-weight: 700; color: var(--primary-color, var(--primary-color)); line-height: 1; letter-spacing: -0.02em; font-feature-settings: "tnum"; font-variant-numeric: tabular-nums; }
.sg-hero-number-unit { font-size: 28px; font-weight: 500; margin-left: 2px; }
.sg-hero-label { font-size: var(--fs-sm); font-weight: 600; color: var(--text-primary, var(--text-primary)); margin-top: 4px; }
.sg-hero-sub { font-size: var(--fs-xs); color: var(--text-secondary, var(--text-secondary)); margin-top: 2px; }

.sg-hero-divider { width: 1px; height: 56px; background: var(--border-light, var(--border-light)); flex-shrink: 0; }

.sg-hero-stats { display: flex; gap: 32px; flex: 1; }
.sg-hero-stat { min-width: 60px; }
.sg-hero-stat-num { font-size: var(--fs-2xl); font-weight: 600; color: var(--text-primary, var(--text-primary)); line-height: 1.2; font-feature-settings: "tnum"; font-variant-numeric: tabular-nums; }
.sg-hero-stat-label { font-size: var(--fs-xs); color: var(--text-secondary, var(--text-secondary)); margin-top: 2px; }

/* ========== Section Label（板块标题 — 无 emoji） ========== */
.sg-section-label { font-size: var(--fs-xs); font-weight: 600; color: var(--text-secondary, var(--text-secondary)); text-transform: uppercase; letter-spacing: 0.03em; margin-bottom: 12px; }

/* ========== 双图表 ========== */
.sg-charts { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 28px; }
.sg-chart-block { background: var(--bg-card, #fff); padding: 16px 16px 8px; border: 1px solid var(--border-light, var(--border-light)); border-radius: 8px; }

/* ========== 成就墙 — 顶部突出展示 ========== */
.sg-achievement-wall {
  background: var(--primary-light);
  border: 1px solid var(--border-light, var(--border-light));
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 24px;
}
.sg-aw-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px;
}
.sg-aw-title { font-size: var(--fs-md); font-weight: 600; color: var(--text-primary, var(--text-primary)); }
.sg-aw-sub { font-size: var(--fs-xs); color: var(--text-secondary, var(--text-secondary)); }

.sg-aw-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.sg-aw-card {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 16px;
  background: var(--bg-card, #fff);
  border: 1px solid var(--border-light, var(--border-light));
  border-radius: 10px;
  opacity: 0.45;
  transition: all 0.3s ease;
  position: relative; overflow: hidden;
}
.sg-aw-card.sg-aw-earned {
  opacity: 1;
  border-color: rgba(67,97,238,0.25);
  background: var(--bg-card);
  box-shadow: 0 2px 8px rgba(67,97,238,0.06);
}
.sg-aw-card.sg-aw-new {
  animation: aw-glow 1.5s ease-in-out 3;
  border-color: var(--primary-color);
}
@keyframes aw-glow {
  0%, 100% { box-shadow: 0 0 0 0 rgba(67,97,238,0.2); }
  50%      { box-shadow: 0 0 0 8px rgba(67,97,238,0); }
}
.sg-aw-icon-wrap { position: relative; flex-shrink: 0; }
.sg-aw-icon { font-size: 28px; line-height: 1; }
.sg-aw-badge {
  position: absolute; top: -8px; right: -12px;
  font-size: 9px; font-weight: 700; color: #fff;
  background: var(--el-color-danger); border-radius: 3px; padding: 1px 4px;
  animation: aw-pulse 0.8s ease-in-out infinite;
}
@keyframes aw-pulse {
  0%, 100% { transform: scale(1); }
  50%      { transform: scale(1.15); }
}
.sg-aw-info { flex: 1; min-width: 0; }
.sg-aw-name { font-size: var(--fs-sm); font-weight: 600; color: var(--text-primary, var(--text-primary)); white-space: nowrap; }
.sg-aw-desc { font-size: var(--fs-xs); color: var(--text-secondary, var(--text-secondary)); margin-top: 2px; }
.sg-aw-check, .sg-aw-lock { font-size: var(--fs-lg); flex-shrink: 0; }

/* Canvas for confetti overlay */
.sg-confetti-canvas {
  position: fixed; top: 0; left: 0; width: 100%; height: 100%;
  pointer-events: none; z-index: 9999;
}

/* ========== 成就徽章（原有底部区域） ========== */
.sg-achievements { padding: 0 0 24px; }
.sg-achievements-count { font-weight: 400; color: var(--text-disabled, var(--text-disabled)); margin-left: 6px; font-size: var(--fs-xs); }
.sg-achievements-row { display: flex; gap: 10px; flex-wrap: wrap; }
.sg-ach-item {
  display: flex; align-items: center; gap: 5px; padding: 6px 12px;
  border: 1px solid var(--border-light, var(--border-light)); border-radius: 6px;
  background: var(--bg-card, #fff); opacity: 0.4; transition: all 0.15s;
}
.sg-ach-item.earned { opacity: 1; border-color: var(--primary-color, var(--primary-color)); background: var(--primary-light, var(--primary-light)); }
.sg-ach-item:hover { transform: translateY(-1px); }
.sg-ach-icon { font-size: var(--fs-md); flex-shrink: 0; }
.sg-ach-name { font-size: var(--fs-xs); font-weight: 500; color: var(--text-primary, var(--text-primary)); white-space: nowrap; }

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .sg-page { padding: 12px; }
  .sg-header { flex-direction: column; align-items: flex-start; }

  /* 移动端里程碑 — 纵向列表 */
  .sg-timeline { flex-direction: column; gap: 0; overflow-x: visible; padding: 0; }
  .sg-timeline::before { top: 0; bottom: 0; left: 15px; right: auto; width: 1px; height: auto; min-width: unset; }
  .sg-milestone { width: 100%; flex-direction: row; align-items: flex-start; gap: 14px; text-align: left; display: flex; padding: 10px 0; }
  .sg-milestone-dot { margin: 0; flex-shrink: 0; width: 28px; height: 28px; }
  .sg-milestone-label { margin-top: 0; }
  .sg-milestone:first-child { padding-top: 0; }

  /* Hero 竖排 */
  .sg-hero { flex-direction: column; gap: 20px; align-items: stretch; padding: 20px 0; }
  .sg-hero-divider { width: auto; height: 1px; }
  .sg-hero-stats { gap: 16px; }
  .sg-hero-number { font-size: 44px; }
  .sg-hero-number-unit { font-size: 22px; }

  /* 图表单列 */
  .sg-charts { grid-template-columns: 1fr; }

  /* 成就 2 列 */
  .sg-achievements-row { display: grid; grid-template-columns: 1fr 1fr; }
}
/* 成就墙移动端响应式 */
  @media (max-width: 768px) {
    .sg-aw-grid { grid-template-columns: repeat(2, 1fr); }
    .sg-aw-card { padding: 10px 12px; gap: 8px; }
    .sg-aw-icon { font-size: 22px; }
    .sg-aw-name { font-size: var(--fs-xs); }
    .sg-aw-desc { font-size: 10px; }
  }
</style>

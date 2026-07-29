<template>
  <div class="ranking-container">
    <RankingChallengeBanner :text="challengeText" />

    <div class="page-card">
      <div class="page-header">
        <h3 class="page-title">🎯 积分排行榜</h3>
        <div class="header-controls">
          <el-select
            v-if="isStudent"
            v-model="scope"
            class="desktop-width"
            style="width:150px"
            @change="onScopeChange"
          >
            <el-option value="myclass" label="🔒 我的班级" />
            <el-option value="grademajor" label="📚 同年级" />
            <el-option value="all" label="🌐 全校" />
          </el-select>
          <el-select
            v-if="isTeacher"
            v-model="scope"
            class="desktop-width"
            style="width:150px"
            @change="onScopeChange"
          >
            <el-option value="class" label="🏫 单个班级" />
            <el-option value="grademajor" label="📚 同年级" />
            <el-option value="all" label="🌐 全校" />
          </el-select>
          <el-select
            v-if="isTeacher && scope === 'class'"
            v-model="selectedClassId"
            placeholder="选择班级"
            class="desktop-width"
            style="width:140px"
            @change="loadData"
          >
            <el-option
              v-for="c in teachingClasses"
              :key="c.classId"
              :value="c.classId"
              :label="c.className"
            />
          </el-select>
          <el-radio-group v-model="rankType" size="small" @change="loadData">
            <el-radio-button value="total">🏅 总榜</el-radio-button>
            <el-radio-button value="weekly">🔥 周榜</el-radio-button>
            <el-radio-button value="monthly">📅 月榜</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <RankingPodium :ranking="ranking" />

      <el-table
        v-if="!isMobile"
        v-loading="loading"
        :data="ranking"
        stripe
        empty-text="暂无排行数据"
        class="ranking-table"
      >
        <el-table-column label="排名" width="70" align="center">
          <template #default="{ $index }">
            <span v-if="$index < 3" class="rank-medal">{{ ['🥇','🥈','🥉'][$index] }}</span>
            <span v-else class="rank-num">{{ $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="学生" min-width="120">
          <template #default="{ row }">
            <div class="student-cell">
              <el-avatar :size="36" shape="circle" class="student-avatar">{{ row.realName?.charAt(0) }}</el-avatar>
              <span class="student-name">{{ row.realName }}</span>
              <span v-if="row.currentStreak >= 7" class="streak-star" title="连续签到7天+">⚡</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="称号" width="160">
          <template #default="{ row }">
            <el-tag :type="getTagType(row.titleLevel)" size="small" effect="dark">{{ row.titleName || '初出茅庐' }}</el-tag>
            <el-tag
              v-if="row.customTitle"
              size="small"
              type="warning"
              effect="plain"
              class="custom-tag"
            >
              🎭 {{ row.customTitle }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="积分" width="110" align="center">
          <template #default="{ row }">
            <div class="credit-cell">
              <span class="credit-value">{{ row.totalCredits || 0 }}</span>
              <span class="credit-unit">分</span>
            </div>
            <div v-if="row.nextTitleName" class="title-progress-mini">
              <div class="progress-bar-mini">
                <div class="progress-fill-mini" :style="{ width: (row.titleProgress || 0) + '%' }"></div>
              </div>
              <span class="progress-label">{{ row.nextTitleName }} {{ row.titleProgress || 0 }}%</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="连续签到" width="110" align="center">
          <template #default="{ row }">
            <span v-if="row.currentStreak" class="streak-display">
              <span class="streak-fire">🔥</span>
              <span class="streak-days" :class="{ 'streak-hot': row.currentStreak >= 7 }">{{ row.currentStreak }}</span>
              <span class="streak-unit">天</span>
            </span>
            <span v-else class="no-streak">—</span>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="isMobile" v-loading="loading" class="ranking-cards">
        <div
          v-for="(row, idx) in ranking"
          :key="row.userId || idx"
          class="rank-card"
          :class="{ 'top-three': idx < 3 }"
        >
          <span class="card-rank">{{ idx < 3 ? ['🥇','🥈','🥉'][idx] : idx + 1 }}</span>
          <el-avatar :size="40" shape="circle" class="card-avatar">{{ row.realName?.charAt(0) }}</el-avatar>
          <div class="card-body">
            <div class="card-name-line">
              <span class="card-name">{{ row.realName }}</span>
              <span v-if="row.currentStreak >= 7" class="streak-star">⚡</span>
            </div>
            <div class="card-title-line">
              <span class="card-title-tag">{{ row.titleName || '初出茅庐' }}</span>
              <span v-if="row.customTitle" class="card-custom-tag">🎭 {{ row.customTitle }}</span>
            </div>
          </div>
          <div class="card-credits">
            <span class="card-credit-val">{{ row.totalCredits || 0 }}</span>
            <span class="card-credit-unit">分</span>
          </div>
        </div>
      </div>

      <RankingMotivation />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getCreditRanking } from '@/api/credit'
import { useUserStore } from '@/stores/user'
import { useIsMobile } from '@/composables/useIsMobile'
import RankingChallengeBanner from './RankingChallengeBanner.vue'
import RankingPodium from './RankingPodium.vue'
import RankingMotivation from './RankingMotivation.vue'

const userStore = useUserStore()
const { isMobile } = useIsMobile()
const isStudent = computed(() => userStore.isStudent)
const isTeacher = computed(() => userStore.isTeacher || userStore.isAdmin)

const ranking = ref([])
const loading = ref(false)
const rankType = ref('total')
const scope = ref('all')
const selectedClassId = ref(null)

const challengeText = computed(() => {
  const day = new Date().getDay()
  const challenges = [
    '周日冲刺！完成一套模拟试卷获得双倍积分',
    '新的一周开始啦！连续签到7天解锁⚡闪电加成',
    '今天在论坛帮助同学解答问题，可获得额外积分奖励',
    '完成本周作业任务，积分排名冲刺前十！',
    '参与在线考试，满分可获50积分奖励！',
    '周末大作战！在积分商城兑换你的第一个奖励吧',
    '周末狂欢！排行榜本周前三将获得特别称号'
  ]
  return challenges[day] || challenges[1]
})

const teachingClasses = computed(() => userStore.teacherSummary?.teachingClasses || [])
const studentClassId = computed(() => userStore.teacherSummary?.headClassId || null)

const onScopeChange = () => {
  if (scope.value !== 'class') selectedClassId.value = null
  loadData()
}

const getTagType = (level) => (['', 'info', 'success', 'warning', 'danger', ''][level] || 'info')

const loadData = async () => {
  loading.value = true
  try {
    const params = { type: rankType.value, limit: 50 }
    if (scope.value === 'class') {
      const cid = selectedClassId.value || (teachingClasses.value[0]?.classId)
      if (cid) params.classId = cid
    } else if (scope.value === 'myclass') {
      params.classId = studentClassId.value
    } else if (scope.value === 'grademajor') {
      if (isTeacher.value && teachingClasses.value.length > 0) {
        params.grade = teachingClasses.value[0]?.grade || ''
      }
    }
    const res = await getCreditRanking(params)
    if (res.code === 200) ranking.value = res.data
  } catch { /* */ }
  finally { loading.value = false }
}

onMounted(() => { loadData() })

</script>

<style scoped lang="scss">
.header-controls {
  display: flex; align-items: center; flex-wrap: wrap; gap: 8px;
}
.rank-medal { font-size: var(--fs-2xl); }
.rank-num { font-size: var(--fs-md); font-weight: 700; color: var(--text-secondary); }
.custom-tag { margin-left: 4px; font-size: var(--fs-xs); }
.student-cell { display: flex; align-items: center; gap: 8px; }
.student-name { font-weight: 500; }
.streak-star { font-size: var(--fs-md); animation: sparkle 1s ease-in-out infinite; }
@keyframes sparkle {
  0%, 100% { opacity: 0.6; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.2); }
}
.credit-cell {
  display: flex; align-items: baseline; justify-content: center; gap: 2px;
  .credit-value { font-size: var(--fs-lg); font-weight: 700; color: var(--warning-color); }
  .credit-unit { font-size: var(--fs-xs); color: var(--text-secondary); }
}
.title-progress-mini {
  display: flex; align-items: center; gap: 4px; margin-top: 4px;
  .progress-bar-mini {
    flex: 1; height: 4px; background: var(--border-color);
    border-radius: 2px; overflow: hidden;
    .progress-fill-mini {
      height: 100%; background: var(--primary-gradient);
      border-radius: 2px; transition: width 0.6s ease;
    }
  }
  .progress-label { font-size: var(--fs-xs); color: var(--text-secondary); white-space: nowrap; }
}
.streak-display {
  display: flex; align-items: center; justify-content: center; gap: 2px;
  .streak-fire { font-size: var(--fs-lg); }
  .streak-days { font-size: var(--fs-lg); font-weight: 600; color: var(--text-primary); }
  .streak-days.streak-hot { color: var(--warning-color); }
  .streak-unit { font-size: var(--fs-xs); color: var(--text-secondary); }
}
.no-streak { color: var(--text-placeholder); }

.ranking-cards {
  display: flex; flex-direction: column; gap: 12px;
}
.rank-card {
  display: flex; align-items: center; gap: 10px;
  background: var(--bg-card); border-radius: var(--radius-md);
  padding: 12px; box-shadow: var(--shadow-sm);
  transition: transform 0.2s;
  &.top-three {
    background: var(--bg-warning-light);
    border: 1px solid var(--warning-color);
  }
  &:active { transform: scale(0.98); }
  .card-rank { font-size: var(--fs-lg); font-weight: 700; min-width: 32px; text-align: center; color: var(--text-secondary); }
  .card-avatar { flex-shrink: 0; }
  .card-body {
    flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3px;
    .card-name-line { display: flex; align-items: center; gap: 4px; }
    .card-name { font-size: var(--fs-md); font-weight: 600; color: var(--text-primary); }
    .streak-star { font-size: var(--fs-sm); flex-shrink: 0; }
    .card-title-line { display: flex; align-items: center; gap: 4px; flex-wrap: wrap; }
    .card-title-tag { font-size: var(--fs-xs); background: var(--bg-section); color: var(--text-regular); padding: 1px 6px; border-radius: var(--radius-xs); }
    .card-custom-tag { font-size: 10px; color: var(--warning-color); background: var(--bg-warning-light); padding: 1px 6px; border-radius: var(--radius-xs); }
  }
  .card-credits { flex-shrink: 0; text-align: right; line-height: 1; }
  .card-credit-val { font-size: var(--fs-lg); font-weight: 700; color: var(--warning-color); display: block; }
  .card-credit-unit { font-size: 10px; color: var(--text-secondary); }
}

@media (max-width: 768px) {
  .page-header { flex-direction: column; align-items: flex-start; gap: 8px; }
  .header-controls { width: 100%; flex-direction: column; gap: 6px;
    :deep(.el-select) { width: 100% !important; }
    :deep(.el-radio-button__inner) { padding: 4px 8px; font-size: var(--fs-xs); }
  }
  .ranking-table { display: none; }
}
</style>

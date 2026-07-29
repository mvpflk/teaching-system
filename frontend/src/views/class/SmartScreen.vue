<template>
  <div class="smart-screen">
    <SmartScreenToolbar
      v-model:scene-mode="sceneMode"
      :conn-status-class="connStatusClass"
      :online-count="onlineCount"
      :class-name="className"
      :now="now"
      :conn-status-label="connStatusLabel"
      :conn-status-title="connStatusText"
      :show-reconnect="sseStatus === 'error'"
      @reconnect="manualReconnect"
      @back="goBack"
      @open-analytics="analyticsPanelRef?.open()"
      @open-task="taskPanelRef?.open()"
    />

    <div v-if="activePanel" class="ss-status-strip" :class="statusClass"></div>

    <div class="ss-body">
      <div class="ss-main">
        <template v-if="!activePanel">
          <SmartScreenHero
            :scene-mode="sceneMode"
            :student-count="studentScores.length"
            :stats="stats"
            @select-panel="activePanel = $event"
          />
          <SmartScreenLogPanel v-model:log-filter="logFilter" :logs="logs" />

          <div class="ss-quick-review-bar">
            <el-button type="primary" size="default" @click="activePanel = 'review'">
              课前三分钟复习
            </el-button>
            <el-button type="success" size="default" @click="activePanel = 'livequiz'">
              <el-icon><Promotion /></el-icon> 随堂速答
            </el-button>
          </div>
        </template>

        <keep-alive v-else>
          <QuizPanel
            v-if="activePanel === 'quiz'"
            :class-id="classId"
            :scene-mode="sceneMode"
            :sse-conn="sseConn"
            @back="activePanel = null"
            @scored="onScored"
          />
          <BuzzPanel
            v-else-if="activePanel === 'buzz'"
            :class-id="classId"
            :sse-conn="sseConn"
            @back="activePanel = null"
            @scored="onScored"
          />
          <VotePanel
            v-else-if="activePanel === 'vote'"
            :class-id="classId"
            :scene-mode="sceneMode"
            :sse-conn="sseConn"
            @back="activePanel = null"
            @scored="onScored"
          />
          <QuickReviewPanel
            v-else-if="activePanel === 'review'"
            :class-id="classId"
            :student-scores="studentScores"
            :sse-conn="sseConn"
            @back="activePanel = null"
            @scored="onScored"
          />
          <LiveQuizPanel
            v-else-if="activePanel === 'livequiz'"
            :class-id="classId"
            :sse-conn="sseConn"
            @back="activePanel = null"
            @scored="onScored"
          />
        </keep-alive>
      </div>

      <div v-if="!(sceneMode === 'CLASSROOM' && activePanel === 'quiz')" class="ss-rail">
        <SmartScreenRanking
          :scene-mode="sceneMode"
          :ranked-scores="rankedScores"
          :top-five="topFive"
          :absent-set="absentSet"
          :student-scores="studentScores"
          :stats="stats"
          @toggle-absent="toggleAbsent"
        />
        <SmartScreenAbsentPanel
          v-if="sceneMode === 'CLASSROOM'"
          :student-scores="studentScores"
          :absent-set="absentSet"
          @toggle-absent="toggleAbsent"
        />
      </div>
    </div>

    <AnalyticsPanel ref="analyticsPanelRef" :class-id="classId" :is-mobile="isMobile" />
    <ClassTaskPanel
      ref="taskPanelRef"
      :class-id="classId"
      :is-mobile="isMobile"
      @task-started="refreshData"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useIsMobile } from '@/composables/useIsMobile';
import { useClassroomSSE } from '@/composables/useClassroomSSE';
import { useSmartScreenConnection } from '@/composables/useSmartScreenConnection';
import { getSessions, getClassroomScores, markAbsent, getAbsentStudents } from '@/api/classroom';
import { ElMessage } from 'element-plus';
import QuizPanel from './QuizPanel.vue';
import BuzzPanel from './BuzzPanel.vue';
import VotePanel from './VotePanel.vue';
import AnalyticsPanel from '@/components/class/AnalyticsPanel.vue';
import ClassTaskPanel from '@/components/class/ClassTaskPanel.vue';
import SmartScreenToolbar from '@/components/class/SmartScreenToolbar.vue';
import SmartScreenHero from '@/components/class/SmartScreenHero.vue';
import SmartScreenLogPanel from '@/components/class/SmartScreenLogPanel.vue';
import SmartScreenRanking from '@/components/class/SmartScreenRanking.vue';
import SmartScreenAbsentPanel from '@/components/class/SmartScreenAbsentPanel.vue';
import QuickReviewPanel from '@/components/class/QuickReviewPanel.vue';
import LiveQuizPanel from './LiveQuizPanel.vue';
import dayjs from 'dayjs';
import { Promotion } from '@element-plus/icons-vue';

const route = useRoute();
const router = useRouter();

const classId = computed(() => route.params.id);
const className = ref(route.query.className || '');
const sceneMode = ref('LAB');
const activePanel = ref(null);
const logFilter = ref('ALL');
const { isMobile } = useIsMobile();

const analyticsPanelRef = ref(null);
const taskPanelRef = ref(null);

const logs = ref([]);
const studentScores = ref([]);
const absentSet = ref(new Set());
const highlightTimers = new Map();
const {
  connect: sseConnect,
  on: sseOn,
  close: sseClose,
  status: sseStatus,
  conn: sseConnRef,
  onlineCount,
} = useClassroomSSE(classId.value);
const { connStatusClass, connStatusLabel, connStatusText, manualReconnect } =
  useSmartScreenConnection(sseStatus, sseConnect);
const sseConn = computed(() => sseConnRef.value);
let clockTimer = null;

const now = ref(dayjs().format('HH:mm'));

watch(sceneMode, () => {
  if (sceneMode.value === 'CLASSROOM' && activePanel.value === 'buzz') {
    activePanel.value = null;
  }
});

const statusClass = computed(() => {
  if (!activePanel.value) return '';
  return { quiz: 'status-quiz', buzz: 'status-buzz', vote: 'status-vote' }[activePanel.value] || '';
});

const rankedScores = computed(() => {
  return [...studentScores.value].sort((a, b) => (b.sessionScore || 0) - (a.sessionScore || 0));
});

const topFive = computed(() => rankedScores.value.slice(0, 5));

const stats = computed(() => {
  const quizLogs = logs.value.filter((l) => l.sessionType === 'QUIZ');
  const correctCount = quizLogs.filter((l) => l.scoreEarned > 0).length;
  const gradedCount = quizLogs.filter((l) => l.result != null).length;
  const activeIds = new Set(logs.value.map((l) => l.winnerStudentId).filter(Boolean));
  return {
    quizCount: quizLogs.length,
    correctRate: gradedCount ? Math.round((correctCount / gradedCount) * 100) : 0,
    activeStudents: activeIds.size,
  };
});

const refreshData = async () => {
  try {
    const [sRes, lRes] = await Promise.all([
      getClassroomScores(classId.value),
      getSessions(classId.value),
    ]);
    if (sRes.code === 200) studentScores.value = sRes.data || [];
    if (lRes.code === 200) logs.value = lRes.data || [];
  } catch (e) {
    console.error('SmartScreen refreshData failed:', e);
  }
};

const onScored = (data) => {
  const sid = data?.studentId;
  if (sid) {
    if (highlightTimers.has(sid)) clearTimeout(highlightTimers.get(sid));
  }
  refreshData().then(() => {
    if (!sid) return;
    const idx = studentScores.value.findIndex((s) => s.studentId === sid);
    if (idx >= 0) {
      studentScores.value[idx] = { ...studentScores.value[idx], _justScored: true };
      const timer = setTimeout(() => {
        const i = studentScores.value.findIndex((s) => s.studentId === sid);
        if (i >= 0) {
          studentScores.value[i] = { ...studentScores.value[i], _justScored: false };
        }
        highlightTimers.delete(sid);
      }, 1200);
      highlightTimers.set(sid, timer);
    }
  });
};

const toggleAbsent = (studentId) => {
  const s = new Set(absentSet.value);
  if (s.has(studentId)) {
    s.delete(studentId);
  } else {
    s.add(studentId);
  }
  absentSet.value = s;
  markAbsent(classId.value, [...s])
    .then((res) => {
      if (res.code === 200) {
        ElMessage.success(s.has(studentId) ? '已标记缺席' : '已取消缺席');
      }
    })
    .catch(() => ElMessage.error('操作失败'));
};

const goBack = () => router.push(`/class/${classId.value}/home`);

onMounted(async () => {
  refreshData();
  getAbsentStudents(classId.value)
    .then((res) => {
      if (res.code === 200 && res.data) absentSet.value = new Set(res.data);
    })
    .catch(() => {});
  try {
    await sseConnect();
    sseOn('score:update', () => {
      getClassroomScores(classId.value)
        .then((res) => {
          if (res.code === 200) studentScores.value = res.data || [];
        })
        .catch(() => {});
    });
  } catch (e) {
    console.error('SSE connect failed:', e);
  }
  clockTimer = setInterval(() => {
    now.value = dayjs().format('HH:mm');
  }, 5000);
});

onBeforeUnmount(() => {
  sseClose();
  if (clockTimer) clearInterval(clockTimer);
});
</script>

<style scoped lang="scss">
.smart-screen {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-section);
  overflow: hidden;
}

.ss-status-strip {
  height: 3px;
  flex-shrink: 0;
  transition: background 0.6s ease;
  z-index: 19;

  &.status-quiz {
    background: var(--primary-color);
  }
  &.status-buzz {
    background: var(--el-color-warning);
  }
  &.status-vote {
    background: var(--color-vote);
  }
}

.ss-body {
  flex: 1;
  display: flex;
  overflow: hidden;
  min-height: 0;
}

.ss-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  padding: 28px;
  min-width: 0;
}

.ss-rail {
  width: 280px;
  background: var(--bg-card);
  border-left: 0.5px solid var(--border-color);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  flex-shrink: 0;
}

@media (max-width: 900px) {
  .ss-body {
    flex-direction: column;
  }
  .ss-main {
    padding: 16px;
  }

  .ss-rail {
    width: 100%;
    max-height: 240px;
    border-left: none;
    border-top: 0.5px solid var(--border-color);
    flex-shrink: 0;
  }
}

@media (min-width: 1400px) {
  .ss-main {
    padding: 32px 40px;
  }
  .ss-rail {
    width: 320px;
  }
}

.ss-quick-review-bar {
  text-align: center;
  margin-top: 16px;
}
</style>

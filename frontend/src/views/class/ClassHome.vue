<template>
  <div v-loading="loading" class="class-home">
    <!-- 头部：班级基本信息 -->
    <div v-if="data.basicInfo" class="home-header">
      <div class="header-left">
        <h2 class="class-name">{{ data.basicInfo.className }}</h2>
        <div class="class-tags">
          <el-tag v-if="data.basicInfo.classTypeName" size="small" type="primary">{{ data.basicInfo.classTypeName }}</el-tag>
          <el-tag v-if="data.basicInfo.grade" size="small" type="success">{{ data.basicInfo.grade }}</el-tag>
          <el-tag v-if="data.basicInfo.majorName" size="small" type="warning">{{ data.basicInfo.majorName }}</el-tag>
        </div>
      </div>
      <div class="header-right">
        <span v-if="data.basicInfo.headTeacherName" class="head-teacher">👨‍🏫 班主任：{{ data.basicInfo.headTeacherName }}</span>
        <span class="student-total">👨‍🎓 {{ data.basicInfo.studentCount }} 人</span>
        <el-tag
          v-if="isClassStudent"
          size="small"
          type="success"
          style="margin-left:8px"
        >
          我的班级
        </el-tag>
        <el-button
          v-if="isTeacher && smartScreenEnabled"
          size="small"
          type="success"
          @click="goSmartScreen"
        >
          📺 智慧大屏
        </el-button>
        <span v-if="isHeadTeacher" class="action-btns">
          <el-button size="small" type="primary" @click="goCreateTask">发布任务</el-button>
          <el-button size="small" @click="goPendingGrade">待批改</el-button>
          <el-button size="small" @click="goScoreAnalysis">成绩分析</el-button>
        </span>
      </div>
    </div>

    <div class="home-body">
      <!-- 左侧：学生列表 + 任务动态 -->
      <div class="home-left">
        <!-- 学生列表 -->
        <div class="home-card">
          <div class="card-title">🏆 积分排行</div>
          <el-empty v-if="!data.students || data.students.length === 0" description="暂无学生" :image-size="60" />
          <div v-else class="student-scroll">
            <div
              v-for="s in topStudents"
              :key="s.studentId"
              class="student-row"
              @click="openStudentScore(s)"
            >
              <span class="s-avatar">{{ (s.name || '?')[0] }}</span>
              <div class="s-info">
                <span class="s-name">{{ s.name }}</span>
                <span class="s-number">{{ s.studentNumber }}</span>
              </div>
              <el-tag v-if="s.title" size="small" :type="titleColor(s.title)">{{ s.title }}</el-tag>
              <span class="s-credits">{{ s.credits }} 分</span>
            </div>
            <el-button
              v-if="data.students.length > 5"
              text
              size="small"
              style="margin-top:6px"
              @click="showAllStudents = !showAllStudents"
            >
              {{ showAllStudents ? '收起' : `查看全部 ${data.students.length} 人` }}
            </el-button>
            <div v-if="showAllStudents">
              <div
                v-for="s in data.students.slice(5)"
                :key="s.studentId"
                class="student-row"
                @click="openStudentScore(s)"
              >
                <span class="s-avatar">{{ (s.name || '?')[0] }}</span>
                <div class="s-info">
                  <span class="s-name">{{ s.name }}</span>
                  <span class="s-number">{{ s.studentNumber }}</span>
                </div>
                <el-tag v-if="s.title" size="small" :type="titleColor(s.title)">{{ s.title }}</el-tag>
                <span class="s-credits">{{ s.credits }} 分</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 任务动态（仅班主任/管理员） -->
        <div v-if="!isClassStudent" class="home-card">
          <div class="card-title">📝 任务动态</div>
          <el-empty v-if="!data.tasks || data.tasks.length === 0" description="暂无任务" :image-size="60" />
          <div
            v-for="t in data.tasks"
            :key="t.taskId"
            class="task-item"
            @click="goTask(t.taskId)"
          >
            <div class="task-top">
              <span class="task-title">{{ t.title }}</span>
              <el-tag size="small">{{ taskTypeLabel(t.taskType) }}</el-tag>
            </div>
            <div class="task-meta">{{ t.subject || '通用' }} · {{ t.teacherName }} · 截止 {{ fmtDate(t.deadline) }}</div>
            <div class="task-bars">
              <div class="tb-row">
                <span class="tb-label">提交 {{ t.submitted }}/{{ t.totalStudents }}</span>
                <div class="tb-track"><div class="tb-fill green" :style="{ width: (t.submitRate || 0) + '%' }"></div></div><span class="tb-pct">{{ t.submitRate }}%</span>
              </div>
              <div class="tb-row">
                <span class="tb-label">批改 {{ t.graded }}/{{ t.submitted }}</span>
                <div class="tb-track"><div class="tb-fill blue" :style="{ width: (t.gradeRate || 0) + '%' }"></div></div><span class="tb-pct">{{ t.gradeRate }}%</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：成绩概览 + 活动记录 + 荣誉墙 -->
      <div class="home-right">
        <!-- 成绩概览（仅班主任/管理员） -->
        <div v-if="!isClassStudent" class="home-card">
          <div class="card-title">📊 成绩概览</div>
          <template v-if="data.scoreOverview && data.scoreOverview.hasData">
            <div class="score-title">{{ data.scoreOverview.taskTitle }}</div>
            <div class="score-stats">
              <div class="ss-item"><span class="ss-val">{{ fmtScore(data.scoreOverview.avgScore, 1) }}</span><span class="ss-lbl">平均分</span></div>
              <div class="ss-item"><span class="ss-val">{{ fmtScore(data.scoreOverview.maxScore, 0) }}</span><span class="ss-lbl">最高</span></div>
              <div class="ss-item"><span class="ss-val">{{ fmtScore(data.scoreOverview.minScore, 0) }}</span><span class="ss-lbl">最低</span></div>
              <div class="ss-item"><span class="ss-val">{{ fmtScore(data.scoreOverview.passRate, 1) }}%</span><span class="ss-lbl">及格率</span></div>
            </div>
            <ScoreDistBar v-if="data.scoreOverview.scoreDistribution" :distribution="data.scoreOverview.scoreDistribution" />
          </template>
          <el-empty v-else description="暂无考试数据" :image-size="60" />
        </div>

        <!-- 活动记录 -->
        <div v-if="data.activities" class="home-card">
          <div class="card-title">📢 活动记录</div>
          <template v-if="data.activities.bbsPosts && data.activities.bbsPosts.length">
            <div class="act-subtitle">💬 BBS 帖子</div>
            <div v-for="p in data.activities.bbsPosts" :key="'b'+p.id" class="act-item">
              <span class="act-text">{{ p.title }}</span>
              <span class="act-meta">{{ p.authorName }} · {{ fmtTime(p.createTime) }}</span>
            </div>
          </template>
          <template v-if="data.activities.showcaseWorks && data.activities.showcaseWorks.length">
            <div class="act-subtitle">🎨 优秀作品</div>
            <div v-for="w in data.activities.showcaseWorks" :key="'s'+w.id" class="act-item">
              <span class="act-text">{{ w.title }}</span>
              <span class="act-meta">{{ w.authorName }} · {{ fmtTime(w.createTime) }}</span>
            </div>
          </template>
          <el-empty v-if="(!data.activities.bbsPosts || !data.activities.bbsPosts.length) && (!data.activities.showcaseWorks || !data.activities.showcaseWorks.length)" description="暂无活动" :image-size="60" />
        </div>

        <!-- 荣誉墙 -->
        <div class="home-card">
          <div class="card-title">🎖 荣誉墙</div>
          <template v-if="data.honorWall && data.honorWall.length">
            <div v-for="h in data.honorWall" :key="h.time" class="honor-item">
              <span class="honor-badge">{{ h.type === '德育表扬' ? '🏅' : '⭐' }}</span>
              <span class="honor-name">{{ h.studentName }}</span>
              <span class="honor-reason">{{ h.reason }}</span>
              <span class="honor-time">{{ fmtTime(h.time) }}</span>
            </div>
          </template>
          <el-empty v-else description="暂无荣誉记录" :image-size="60" />
        </div>
      </div>
    </div>

    <!-- 班主任寄语（仅班主任可见） -->
    <div v-if="isHeadTeacher" class="home-card" style="margin-top:16px">
      <div class="card-title">✏️ 班主任寄语 <span class="card-hint">{{ remarkSemester }}</span></div>
      <div v-loading="remarkLoading" class="remark-body">
        <div v-for="rs in remarkStudents" :key="rs.studentId" class="remark-row">
          <span class="remark-name">{{ rs.name }}</span>
          <el-input
            v-model="rs.remark"
            size="small"
            placeholder="输入寄语..."
            @blur="saveRemark(rs)"
          />
        </div>
        <el-empty v-if="!remarkStudents.length" description="暂无学生" :image-size="40" />
      </div>
    </div>

    <!-- 班级相册 -->
    <div class="home-card album-section" style="margin-top:16px">
      <ClassAlbum :class-id="classId" />
    </div>

    <StudentScoreDialog
      v-model="scoreDialogVisible"
      :class-id="classId"
      :student-id="selectedStudent?.studentId"
      :student-name="selectedStudent?.name"
    />
    <TaskDetailPanel v-model="taskDetailVisible" :class-id="classId" :task-id="taskDetailTaskId" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useSettingsStore } from '@/stores/settings'
import request from '@/utils/request'
import StudentScoreDialog from './StudentScoreDialog.vue'
import ScoreDistBar from '@/components/common/ScoreDistBar.vue'
import ClassAlbum from './ClassAlbum.vue'
import TaskDetailPanel from './TaskDetailPanel.vue'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const settingsStore = useSettingsStore()
const loading = ref(false)
const data = ref({})
const showAllStudents = ref(false)
const scoreDialogVisible = ref(false)
const selectedStudent = ref(null)
const classId = computed(() => route.params.id)

const isHeadTeacher = computed(() => data.value.isHeadTeacher === true)
const isTeacher = computed(() => userStore.isTeacher || userStore.isHeadTeacher || userStore.isAdmin)
const isClassStudent = computed(() => data.value.isClassStudent === true)
const smartScreenEnabled = computed(() => settingsStore.isEnabled('feature.smart_screen_enabled'))

const topStudents = computed(() => (data.value.students || []).slice(0, 5))

const taskTypeLabel = (t) => ({
  FORMATIVE: '形成性', SUMMATIVE: '终结性', PRE_CLASS: '课前', IN_CLASS: '课中', AFTER_CLASS: '课后', PRACTICE: '实训', PRACTICAL: '实训'
}[t] || t)

const titleColor = (t) => {
  if (!t) return ''
  if (t.includes('学霸') || t.includes('之星')) return 'danger'
  if (t.includes('优秀') || t.includes('标兵')) return 'warning'
  return 'success'
}

const fmtDate = (d) => d ? dayjs(d).format('MM-DD HH:mm') : '-'
const fmtTime = (t) => t ? dayjs(t).format('MM-DD') : '-'
const fmtScore = (v, decimals = 1) => v != null ? Number(v).toFixed(decimals) : '-'

const goCreateTask = () => router.push({ path: '/teacher/tasks/list', query: { classId: classId.value } })
const goSmartScreen = () => router.push({ path: `/class/${classId.value}/smart-screen`, query: { className: data.value.basicInfo?.className || '' } })
const goPendingGrade = () => router.push({ path: '/teacher/tasks/list', query: { classId: classId.value, tab: 'grading' } })
const goScoreAnalysis = () => router.push({ path: '/inspector/exams', query: { classId: classId.value } })
const openStudentScore = (s) => { selectedStudent.value = s; scoreDialogVisible.value = true }
const taskDetailVisible = ref(false)
const taskDetailTaskId = ref(null)
const goTask = (id) => { taskDetailTaskId.value = id; taskDetailVisible.value = true }

const loadData = async () => {
  if (!classId.value) return
  loading.value = true
  try {
    const res = await request({ url: `/class/${classId.value}/actions/home`, method: 'get' })
    if (res.code === 200) data.value = res.data || {}
  } catch { /* */ }
  finally { loading.value = false }
}

const remarkLoading = ref(false)
const remarkStudents = ref([])
const remarkSemester = computed(() => {
  const d = new Date(); const y = d.getFullYear(); const m = d.getMonth() + 1
  if (m >= 2 && m <= 7) return y + '-' + (y + 1) + '-2'
  else if (m >= 9) return y + '-' + (y + 1) + '-1'
  else return (y - 1) + '-' + y + '-1'
})

const loadRemarks = async () => {
  if (!isHeadTeacher.value || !classId.value) return
  remarkLoading.value = true
  try {
    const r = await request({ url: `/class/${classId.value}/students-with-remarks`, method: 'get' })
    if (r.code === 200) remarkStudents.value = r.data || []
  } catch { /* */ }
  finally { remarkLoading.value = false }
}

const saveRemark = async (rs) => {
  try {
    await request({ url: `/student/${rs.studentId}/remark`, method: 'put', data: { remark: rs.remark || '' } })
  } catch { /* */ }
}

watch([classId, isHeadTeacher], () => { if (isHeadTeacher.value) loadRemarks() })
onMounted(() => { loadData(); if (isHeadTeacher.value) loadRemarks() })
</script>

<style scoped lang="scss">
.class-home { max-width: 1280px; margin: 0 auto; padding: var(--spacing-lg, 24px); }

.home-header {
  display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px;
  margin-bottom: 20px; padding: 20px 24px;
  background: var(--bg-card); border-radius: var(--radius-lg); box-shadow: var(--shadow-sm);
  .class-name { font-size: var(--fs-2xl); font-weight: 700; color: var(--text-primary); margin: 0; }
  .class-tags { display: flex; gap: 6px; margin-top: 6px; }
  .header-right { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
  .head-teacher { font-size: var(--fs-sm); color: var(--text-secondary); }
  .student-total { font-size: var(--fs-sm); color: var(--text-regular); font-weight: 500; }
  .action-btns { display: flex; gap: 6px; }
}

.home-body { display: flex; gap: 20px; }
.home-left { flex: 3; min-width: 0; display: flex; flex-direction: column; gap: 16px; }
.home-right { flex: 2; min-width: 0; display: flex; flex-direction: column; gap: 16px; }

.home-card {
  background: var(--bg-card); border-radius: var(--radius-lg); padding: 18px; box-shadow: var(--shadow-sm);
}

.card-title { font-size: var(--fs-md); font-weight: 600; color: var(--text-primary); margin-bottom: 12px; }

// 学生列表
.student-scroll { max-height: 360px; overflow-y: auto; }
.student-row {
  display: flex; align-items: center; gap: 8px; padding: 8px 6px;
  border-bottom: 1px solid var(--bg-section); font-size: var(--fs-sm); cursor: pointer;
  &:hover { background: var(--bg-section); border-radius: var(--radius-sm); }
}
.s-avatar {
  width: 32px; height: 32px; border-radius: 50%; background: var(--primary-light);
  display: flex; align-items: center; justify-content: center;
  font-size: var(--fs-md); font-weight: 600; color: var(--primary-color); flex-shrink: 0;
}
.s-info { flex: 1; display: flex; flex-direction: column; gap: 1px; }
.s-name { color: var(--text-primary); font-weight: 500; }
.s-number { font-size: var(--fs-xs); color: var(--text-secondary); }
.s-credits { color: var(--warning-color); font-weight: 600; font-size: var(--fs-xs); }

// 任务动态
.task-item {
  padding: 12px 0; border-bottom: 1px solid var(--bg-section); cursor: pointer;
  &:hover { background: var(--bg-section); margin: 0 -8px; padding: 12px 8px; border-radius: var(--radius-sm); }
}
.task-top { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.task-title { font-size: var(--fs-sm); font-weight: 600; color: var(--text-primary); flex: 1; }
.task-meta { font-size: var(--fs-xs); color: var(--text-secondary); margin-bottom: 8px; }
.task-bars { display: flex; flex-direction: column; gap: 4px; }
.tb-row { display: flex; align-items: center; gap: 8px; font-size: var(--fs-xs); }
.tb-label { width: 80px; flex-shrink: 0; color: var(--text-secondary); }
.tb-track { flex: 1; height: 10px; background: var(--bg-secondary); border-radius: var(--radius-xs); overflow: hidden; }
.tb-fill { height: 100%; border-radius: var(--radius-xs); transition: width 0.5s; }
.tb-fill.green { background: var(--el-color-success); }
.tb-fill.blue { background: var(--primary-color); }
.tb-pct { width: 40px; text-align: right; font-weight: 600; color: var(--text-primary); }

// 成绩概览
.score-title { font-size: var(--fs-sm); color: var(--text-secondary); margin-bottom: 8px; }
.score-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; margin-bottom: 14px; }
.ss-item { text-align: center;
  .ss-val { display: block; font-size: var(--fs-xl); font-weight: 700; color: var(--text-primary); }
  .ss-lbl { display: block; font-size: var(--fs-xs); color: var(--text-secondary); }
}
// 活动记录
.act-subtitle { font-size: var(--fs-xs); color: var(--text-secondary); font-weight: 500; margin: 6px 0 4px; }
.act-item { display: flex; justify-content: space-between; align-items: center; padding: 6px 0; border-bottom: 1px solid var(--bg-section); }
.act-text { font-size: var(--fs-sm); color: var(--text-primary); flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.act-meta { font-size: var(--fs-xs); color: var(--text-secondary); flex-shrink: 0; margin-left: 8px; }

// 荣誉墙
.honor-item { display: flex; align-items: center; gap: 6px; padding: 6px 0; border-bottom: 1px solid var(--bg-section); font-size: var(--fs-sm); }
.honor-badge { font-size: var(--fs-lg); flex-shrink: 0; }
.honor-name { font-weight: 600; color: var(--text-primary); flex-shrink: 0; }
.honor-reason { flex: 1; color: var(--text-secondary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.honor-time { font-size: var(--fs-xs); color: var(--text-secondary); flex-shrink: 0; }

// 班主任寄语
.card-hint { font-size: var(--fs-xs); color: var(--text-secondary); font-weight: 400; margin-left: 8px; }
.remark-body { max-height: 400px; overflow-y: auto; }
.remark-row { display: flex; align-items: center; gap: 10px; padding: 6px 0; border-bottom: 1px solid var(--bg-section); }
.remark-name { font-size: var(--fs-sm); font-weight: 500; color: var(--text-primary); min-width: 60px; white-space: nowrap; }

// 移动端
@media (max-width: 768px) {
  .class-home { padding: var(--spacing-md, 16px); }
  .home-header { padding: 14px 16px; flex-direction: column; align-items: flex-start; }
  .home-body { flex-direction: column; }
  .home-left, .home-right { flex: none; width: 100%; }
  .score-stats { grid-template-columns: repeat(2, 1fr); }
  .student-scroll { max-height: 280px; }
  .task-item { padding: 10px 0; }
  :deep(.el-button) { min-height: 36px; font-size: var(--fs-sm); }
}
</style>

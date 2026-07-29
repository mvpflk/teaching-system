<template>
  <div class="twm-page">
    <div class="page-header">
      <h2 class="twm-title">错题监督</h2>
      <el-button plain :loading="loading" @click="loadAll">刷新</el-button>
    </div>

    <div v-loading="loading" class="twm-cards">
      <div class="twm-card">
        <div class="twm-num">{{ summary.studentCount }}</div>
        <div class="twm-label">覆盖学生</div>
      </div>
      <div class="twm-card twm-card--warn">
        <div class="twm-num">{{ summary.totalWrong }}</div>
        <div class="twm-label">总错题数</div>
      </div>
      <div class="twm-card twm-card--danger">
        <div class="twm-num">{{ summary.unmasteredWrong }}</div>
        <div class="twm-label">未掌握</div>
      </div>
      <div class="twm-card">
        <div class="twm-num">{{ summary.recentActiveStudents }}</div>
        <div class="twm-label">近7天练习</div>
      </div>
      <div class="twm-card" :class="{ 'twm-card--danger': summary.needNudgeStudents > 0 }">
        <div class="twm-num">{{ summary.needNudgeStudents }}</div>
        <div class="twm-label">需催促</div>
      </div>
    </div>

    <div class="twm-toolbar">
      <el-input
        v-model="searchKey"
        placeholder="搜索学生姓名或班级"
        clearable
        style="width:220px"
        size="default"
      />
      <el-select
        v-model="classFilter"
        placeholder="全部班级"
        clearable
        style="width:160px"
        size="default"
      >
        <el-option
          v-for="c in classOptions"
          :key="c"
          :label="c"
          :value="c"
        />
      </el-select>
      <el-select
        v-model="statusFilter"
        placeholder="全部状态"
        clearable
        style="width:180px"
        size="default"
      >
        <el-option label="需关注（未掌握≥10）" value="needAttention" />
        <el-option label="超7天未练习" value="stale" />
        <el-option label="从未练习" value="never" />
      </el-select>
      <span class="twm-count">共 {{ filteredStudents.length }} 人</span>
    </div>

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="学生总览" name="students">
        <el-table
          :data="filteredStudents"
          size="default"
          style="width:100%"
          class="twm-table"
        >
          <el-table-column label="姓名" width="90">
            <template #default="{row}">
              <span class="twm-name-cell" @click="openStudentDetail(row)">{{ row.studentName }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="className" label="班级" width="100" />
          <el-table-column
            label="错题数"
            width="70"
            sortable
            prop="total"
          />
          <el-table-column
            label="未掌握"
            width="95"
            sortable
            prop="unmastered"
          >
            <template #default="{row}">
              <el-tag
                v-if="row.unmastered === 0"
                type="success"
                size="small"
                effect="plain"
              >
                已全掌握
              </el-tag>
              <el-tag
                v-else-if="row.unmastered > 10"
                type="danger"
                size="small"
                effect="plain"
              >
                {{ row.unmastered }}
              </el-tag>
              <el-tag
                v-else-if="row.unmastered > 5"
                type="warning"
                size="small"
                effect="plain"
              >
                {{ row.unmastered }}
              </el-tag>
              <el-tag v-else size="small" effect="plain">{{ row.unmastered }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column
            label="最近练习"
            width="130"
            sortable
            prop="daysSincePractice"
          >
            <template #default="{row}">
              <span v-if="!row.lastPracticeTime" class="twm-never">从未练习</span>
              <span v-else>
                <span class="twm-date">{{ formatPracticeDate(row.lastPracticeTime) }}</span>
                <span :class="{ 'twm-stale': row.daysSincePractice > 7 }" class="twm-days">{{ row.daysSincePractice }}天前</span>
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{row}">
              <el-button
                v-if="row.unmastered >= 5"
                type="warning"
                size="small"
                plain
                @click="doNotifyStudent(row)"
              >
                提醒
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="薄弱知识点" name="weakpoints">
        <div v-if="trendData.length" class="twm-trend-bar">
          <span class="twm-trend-label">趋势对比 · 近{{ trendWeeks }}周</span>
          <el-tag
            v-for="t in trendSummary"
            :key="t.label"
            :type="t.type"
            size="small"
            effect="plain"
          >
            {{ t.label }} {{ t.count }}个
          </el-tag>
        </div>
        <div v-if="weakPoints.length" ref="twChartRef" class="twm-chart"></div>
        <el-table
          v-if="weakPoints.length"
          :data="weakPoints"
          stripe
          border
          size="default"
          style="width:100%"
        >
          <el-table-column prop="knowledgeNodeName" label="薄弱知识点" min-width="160" />
          <el-table-column
            label="错误次数"
            width="90"
            sortable
            prop="errorCount"
          >
            <template #default="{row}"><el-tag type="danger" size="small" effect="plain">{{ row.errorCount }}</el-tag></template>
          </el-table-column>
          <el-table-column label="涉及学生" width="80" prop="studentCount" />
          <el-table-column label="趋势" width="80">
            <template #default="{row}">
              <span v-if="row.trend==='up'" class="twm-trend-up">↑{{ row.deltaPercent }}%</span>
              <span v-else-if="row.trend==='down'" class="twm-trend-down">↓{{ Math.abs(row.deltaPercent) }}%</span>
              <span v-else class="twm-trend-flat">—</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="110">
            <template #default="{row}">
              <el-button type="primary" size="small" @click="composeForWeakPoint(row)">组卷补强</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="twm-empty-weak">暂无薄弱数据</div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog
      v-model="detailVisible"
      :title="`${detailStudentName} 的错题明细`"
      width="720px"
      top="3vh"
    >
      <div class="twm-detail-filters">
        <span :class="['twm-detail-pill', { active: detailFilter==='all' }]" @click="detailFilter='all'">全部</span>
        <span :class="['twm-detail-pill', { active: detailFilter==='unmastered' }]" @click="detailFilter='unmastered'">未掌握</span>
        <span :class="['twm-detail-pill', { active: detailFilter==='mastered' }]" @click="detailFilter='mastered'">已掌握</span>
      </div>
      <el-table
        v-loading="detailLoading"
        :data="filteredDetailList"
        size="default"
        max-height="50vh"
        class="twm-table"
      >
        <el-table-column
          label="题目"
          min-width="200"
          show-overflow-tooltip
          prop="questionText"
        />
        <el-table-column label="题型" width="70">
          <template #default="{row}">{{ typeLabelMap[row.questionType] || row.questionType }}</template>
        </el-table-column>
        <el-table-column label="错次" width="55" prop="wrongCount" />
        <el-table-column label="维度" width="70">
          <template #default="{row}">
            <el-tag
              v-if="row.knowledgeDim"
              :type="row.knowledgeDim==='THEORY'?'':'success'"
              size="small"
              effect="plain"
            >
              {{ row.knowledgeDim==='THEORY'?'应知':'应会' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="掌握" width="70">
          <template #default="{row}">
            <el-tag :type="row.isMastered?'success':'danger'" size="small" effect="plain">{{ row.isMastered?'已掌握':'未掌握' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近错误" width="100">
          <template #default="{row}">{{ (row.lastWrongTime||'').substring(0,10) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, onUnmounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { getTeacherSummary, getTeacherStudentList, getTeacherWeakPoints, getTeacherStudentWrongDetail, notifyStudentReview, getTeacherWeakPointsTrend } from '@/api/wrong'
import { QUESTION_TYPE_LABEL } from '@/constants/questionTypes'
import echarts from '@/utils/echarts'
import { textSecondary, elDanger } from '@/utils/theme'

const router = useRouter()
const typeLabelMap = QUESTION_TYPE_LABEL
const loading = ref(false), activeTab = ref('students')
const summary = ref({ studentCount:0, totalWrong:0, unmasteredWrong:0, recentActiveStudents:0, needNudgeStudents:0 })
const students = ref([]), weakPoints = ref([])
const searchKey = ref('')
const classFilter = ref('')
const statusFilter = ref('')
const classOptions = computed(() => {
  const set = new Set(students.value.map(s => s.className).filter(Boolean))
  return Array.from(set).sort()
})
const filteredStudents = computed(() => {
  let list = students.value
  if (searchKey.value) {
    const kw = searchKey.value.toLowerCase()
    list = list.filter(s => (s.studentName||'').toLowerCase().includes(kw) || (s.className||'').toLowerCase().includes(kw))
  }
  if (classFilter.value) {
    list = list.filter(s => s.className === classFilter.value)
  }
  if (statusFilter.value === 'needAttention') {
    list = list.filter(s => (s.unmastered||0) >= 10)
  } else if (statusFilter.value === 'stale') {
    list = list.filter(s => s.daysSincePractice > 7)
  } else if (statusFilter.value === 'never') {
    list = list.filter(s => !s.lastPracticeTime)
  }
  return list
})
const twChartRef = ref(null); let twChartInstance = null
const detailVisible = ref(false), detailStudentName = ref(''), detailLoading = ref(false), detailList = ref([])
const detailFilter = ref('all')
const filteredDetailList = computed(() => {
  if (detailFilter.value === 'unmastered') return detailList.value.filter(d => !d.isMastered)
  if (detailFilter.value === 'mastered') return detailList.value.filter(d => d.isMastered)
  return detailList.value
})

const trendData = ref([]), trendWeeks = ref(4)
const trendSummary = computed(() => {
  const up = trendData.value.filter(t => t.trend==='up').length
  const down = trendData.value.filter(t => t.trend==='down').length
  return [{label:'恶化',count:up,type:'danger'},{label:'改善',count:down,type:'success'}]
})

async function loadAll() {
  loading.value = true
  try {
    const [sRes, stRes, wpRes] = await Promise.all([getTeacherSummary(), getTeacherStudentList(), getTeacherWeakPoints()])
    if (sRes.code===200) summary.value = sRes.data
    if (stRes.code===200) students.value = stRes.data
    if (wpRes.code===200) weakPoints.value = wpRes.data
  } catch { ElMessage.error('加载失败') }
  loading.value = false; await nextTick(); renderChart(); loadTrend()
}

async function loadTrend() {
  try {
    const res = await getTeacherWeakPointsTrend(trendWeeks.value)
    if (res.code===200) { trendData.value = res.data?.current||[]; const m={}; trendData.value.forEach(t=>{m[t.knowledgeNodeId]=t}); weakPoints.value.forEach(w=>{const t=m[w.knowledgeNodeId]; if(t){w.deltaPercent=t.deltaPercent; w.trend=t.trend}}) }
  } catch {}
}

function renderChart() {
  if (!twChartRef.value||!weakPoints.value.length) return
  if (twChartInstance) twChartInstance.dispose()
  twChartInstance = echarts.init(twChartRef.value)
  const names = weakPoints.value.map(w=>(w.knowledgeNodeName||'').length>10?(w.knowledgeNodeName||'').substring(0,10)+'...':w.knowledgeNodeName)
  twChartInstance.setOption({
    tooltip:{trigger:'axis',axisPointer:{type:'shadow'}},
    grid:{left:'3%',right:'4%',bottom:'12%',top:'8%',containLabel:true},
    xAxis:{type:'category',data:names,axisLabel:{rotate:30,fontSize:11,color:textSecondary}},
    yAxis:{type:'value',name:'错误次数',axisLabel:{fontSize:10,color:textSecondary}},
    series:[{type:'bar',data:weakPoints.value.map(w=>w.errorCount),
      itemStyle:{color:new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:elDanger},{offset:1,color:'#f5a0a0'}])},
      barWidth:24}]
  })
  window.addEventListener('resize',_twChartResize)
}
const _twChartResize = ()=>twChartInstance?.resize()
onUnmounted(()=>{window.removeEventListener('resize',_twChartResize);twChartInstance?.dispose()})

async function openStudentDetail(row) {
  detailFilter.value = 'all'
  detailVisible.value=true; detailStudentName.value=row.studentName; detailLoading.value=true
  try { const res = await getTeacherStudentWrongDetail(row.studentId,-1); if (res.code===200) detailList.value=res.data||[] } catch { ElMessage.error('加载失败') }
  detailLoading.value=false
}
async function doNotifyStudent(row) {
  try { const res = await notifyStudentReview(row.studentId); if (res.code===200) ElMessage.success(res.data||'已提醒'); else ElMessage.error(res.message||'失败') } catch { ElMessage.error('失败') }
}
function formatPracticeDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getMonth()+1}月${d.getDate()}日`
}
function composeForWeakPoint(row) { router.push({path:'/teacher/tasks/question-bank',query:{action:'compose',kpId:row.knowledgeNodeId,kpName:row.knowledgeNodeName}}) }
function onTabChange(name) { if (name==='weakpoints') { nextTick(renderChart); loadTrend() } }
onMounted(loadAll)
</script>

<style scoped>
.twm-page { max-width: 1140px; margin: 0 auto; padding: 24px 16px; }
.twm-title { font-size: var(--fs-2xl, 24px); font-weight: 600; color: var(--text-primary, var(--text-primary)); margin: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }

.twm-cards { display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; margin-bottom: 20px; }
.twm-card { text-align: center; padding: 16px 12px; background: var(--bg-card, #fff); border: 0.5px solid var(--border-color); border-radius: var(--radius-md, 8px); }
.twm-card--warn .twm-num { color: var(--el-color-warning); }
.twm-card--danger .twm-num { color: var(--el-color-danger); }
.twm-num { font-size: var(--fs-2xl); font-weight: 700; color: var(--text-primary, var(--text-primary)); }
.twm-label { font-size: var(--fs-xs); color: var(--text-secondary, var(--text-secondary)); margin-top: 4px; }

.twm-table { border: none; }
.twm-table :deep(.el-table__header-wrapper) { border-bottom: 0.5px solid var(--border-color); }
.twm-table :deep(.el-table__body-wrapper .el-table__row) { border-bottom: 0.5px solid var(--border-light); }
.twm-table :deep(.el-table__body-wrapper .el-table__row:last-child) { border-bottom: none; }
.twm-table :deep(.el-table td) { padding: 10px 0; }

.twm-name-cell { cursor: pointer; color: var(--text-primary); font-weight: 600; }
.twm-name-cell::after { content: ' →'; color: transparent; font-weight: 400; transition: color 0.15s; }
.twm-name-cell:hover { color: var(--primary-color); }
.twm-name-cell:hover::after { color: var(--primary-color); }

.twm-date { color: var(--text-secondary, var(--text-secondary)); font-size: var(--fs-xs); }
.twm-days { font-size: var(--fs-xs); margin-left: 4px; color: var(--text-secondary, var(--text-secondary)); }

.twm-never { font-size: var(--fs-xs); color: var(--el-color-danger); font-weight: 500; }
.twm-stale { font-size: var(--fs-xs); color: var(--el-color-warning); font-weight: 500; }

.twm-trend-bar { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.twm-trend-label { font-size: var(--fs-sm); color: var(--text-secondary, var(--text-secondary)); }
.twm-trend-up { color: var(--el-color-danger); font-size: var(--fs-sm); font-weight: 500; }
.twm-trend-down { color: var(--el-color-success); font-size: var(--fs-sm); font-weight: 500; }
.twm-trend-flat { color: var(--text-disabled, var(--text-disabled)); font-size: var(--fs-sm); }
.twm-chart { width: 100%; height: 320px; margin-bottom: 12px; }
.twm-empty-weak { padding: 24px; text-align: center; color: var(--text-secondary, var(--text-secondary)); font-size: var(--fs-sm); background: var(--bg-card, #fff); border: 1px solid var(--border-base, #e8e8ed); border-radius: var(--radius-md, 8px); }

.twm-toolbar { display: flex; gap: 10px; align-items: center; margin-bottom: 12px; flex-wrap: wrap; }

.twm-detail-filters { display: flex; gap: 8px; margin-bottom: 12px; }
.twm-detail-pill { display: inline-block; padding: 4px 14px; border-radius: 14px; font-size: var(--fs-xs); cursor: pointer; color: var(--text-regular, var(--text-regular)); background: var(--bg-card, #fff); border: 0.5px solid var(--border-color); transition: all 0.15s; }
.twm-detail-pill.active { background: var(--primary-light); color: var(--primary-color); border-color: var(--primary-color); }
.twm-detail-pill:hover:not(.active) { background: var(--bg-hover, var(--bg-hover)); }
.twm-count { font-size: var(--fs-xs); color: var(--text-secondary, var(--text-secondary)); white-space: nowrap; margin-left: auto; }

@media (max-width: 768px) { .twm-cards { grid-template-columns: repeat(2, 1fr); } .twm-page { padding: 16px 8px; } }
</style>

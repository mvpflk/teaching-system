<template>
  <div class="syllabus-coverage">
    <div class="sc-header">
      <h4>📊 考纲覆盖度</h4>
      <el-select v-model="selectedSubject" placeholder="选择学科" size="small" style="width:200px" @change="loadData">
        <el-option label="信息技术应用基础" :value="4" />
        <el-option label="网络应用基础" :value="5" />
        <el-option label="办公应用基础" :value="6" />
        <el-option label="Access" :value="17" />
      </el-select>
    </div>

    <div v-loading="loading" class="sc-body">
      <!-- 图例 -->
      <div class="legend">
        <span class="legend-item"><span class="dot red"></span> 无覆盖</span>
        <span class="legend-item"><span class="dot yellow"></span> 仅有题库</span>
        <span class="legend-item"><span class="dot green"></span> 有实训</span>
      </div>

      <!-- 热力图 -->
      <div v-if="coverageData.length" class="heatmap">
        <div v-for="unit in coverageData" :key="unit.name" class="unit-row">
          <div class="unit-name">{{ unit.name }}</div>
          <div class="task-grid">
            <el-popover
              v-for="task in unit.tasks"
              :key="task.name"
              placement="top"
              :width="280"
              trigger="hover"
            >
              <template #reference>
                <div
                  class="task-cell"
                  :class="'level-' + task.level"
                  @click="showTaskDetail(task)"
                >
                  <span class="task-label">{{ task.name }}</span>
                  <span class="task-badge">{{ task.kpCount }}</span>
                </div>
              </template>
              <div class="cell-popover">
                <strong>{{ task.name }}</strong>
                <div class="pop-stats">
                  <span>📝 知识点: {{ task.kpCount }}个</span>
                  <span v-if="task.practiceCount > 0">💻 实训: {{ task.practiceCount }}个</span>
                  <span v-if="task.questionCount > 0">📋 题目: {{ task.questionCount }}道</span>
                  <span v-if="task.practiceCount === 0 && task.questionCount === 0" class="no-data">暂无覆盖</span>
                </div>
                <div v-if="task.practiceCount > 0" class="pop-actions">
                  <el-button size="small" type="primary" @click="goToPractices(task)">查看实训</el-button>
                </div>
                <div v-else class="pop-actions">
                  <el-button size="small" @click="createPractice(task)">创建实训</el-button>
                </div>
              </div>
            </el-popover>
          </div>
        </div>
      </div>

      <el-empty v-else description="请选择学科查看考纲覆盖情况" :image-size="80" />
    </div>

    <!-- 整体统计 -->
    <div v-if="coverageData.length" class="sc-summary">
      <div class="summary-item"><span class="s-label">总单元</span><strong>{{ totalUnits }}</strong></div>
      <div class="summary-item"><span class="s-label">总任务</span><strong>{{ totalTasks }}</strong></div>
      <div class="summary-item"><span class="s-label">总知识点</span><strong>{{ totalKps }}</strong></div>
      <div class="summary-item covered"><span class="s-label">实训覆盖</span><strong>{{ coveredTasks }}/{{ totalTasks }}</strong></div>
      <div class="summary-item"><span class="s-label">覆盖率</span><strong>{{ coveragePercent }}%</strong></div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()

const selectedSubject = ref(null)
const loading = ref(false)
const coverageData = ref([])

const totalUnits = computed(() => coverageData.value.length)
const totalTasks = computed(() => coverageData.value.reduce((s, u) => s + u.tasks.length, 0))
const totalKps = computed(() => coverageData.value.reduce((s, u) => s + u.tasks.reduce((ss, t) => ss + t.kpCount, 0), 0))
const coveredTasks = computed(() => coverageData.value.reduce((s, u) => s + u.tasks.filter(t => t.practiceCount > 0).length, 0))
const coveragePercent = computed(() => totalTasks.value ? Math.round(coveredTasks.value / totalTasks.value * 100) : 0)

async function loadData() {
  if (!selectedSubject.value) return
  loading.value = true
  try {
    const res = await request({
      url: '/analytics/syllabus-coverage',
      method: 'get',
      params: { subjectId: selectedSubject.value }
    })
    if (res.code === 200) {
      coverageData.value = res.data || []
    }
  } catch {
    ElMessage.error('加载考纲数据失败')
  }
  loading.value = false
}

function showTaskDetail(task) { /* popover已展示详情 */ }

function getSubjectName(id) { return {4:'信息技术应用基础',5:'网络应用基础',6:'办公应用基础',17:'Access'}[id] || '' }

function goToPractices(task) {
  router.push({ name: 'TrainingHubTeacher', query: { subject: getSubjectName(selectedSubject.value), task: task.name } })
}

function createPractice(task) {
  router.push({ name: 'TrainingTaskCreate', query: { subject: getSubjectName(selectedSubject.value), topic: task.name } })
}
</script>

<style scoped>
.syllabus-coverage { padding: 16px; background: var(--bg-card); border-radius: var(--radius-md); border: 1px solid var(--border-color); }
.sc-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.sc-header h4 { margin: 0; }
.sc-body { min-height: 120px; }
.legend { display: flex; gap: 16px; margin-bottom: 16px; font-size: var(--fs-xs); }
.legend-item { display: flex; align-items: center; gap: 4px; }
.dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }
.dot.red { background: var(--el-color-danger, #f56c6c); }
.dot.yellow { background: var(--el-color-warning, #e6a23c); }
.dot.green { background: var(--el-color-success, #67c23a); }

.heatmap { display: flex; flex-direction: column; gap: 10px; }
.unit-row { display: flex; gap: 10px; align-items: flex-start; }
.unit-name { width: 140px; flex-shrink: 0; font-size: var(--fs-sm); font-weight: 600; color: var(--text-primary); padding-top: 2px; }
.task-grid { display: flex; flex-wrap: wrap; gap: 6px; flex: 1; }
.task-cell { padding: 6px 10px; border-radius: var(--radius-sm); cursor: pointer; font-size: var(--fs-xs); display: flex; align-items: center; gap: 6px; transition: transform 0.15s; }
.task-cell:hover { transform: scale(1.03); }
.task-cell.level-0 { background: #fde2e2; color: #c45656; }  /* 红色: 无覆盖 */
.task-cell.level-1 { background: #fdf6e2; color: #b88230; }  /* 黄色: 仅有题库 */
.task-cell.level-2 { background: #e1f3e1; color: #3a7a3a; }  /* 绿色: 有实训 */
.task-label { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 120px; }
.task-badge { font-weight: 700; }

.cell-popover strong { display: block; margin-bottom: 6px; }
.pop-stats { font-size: var(--fs-xs); display: flex; flex-direction: column; gap: 2px; margin-bottom: 8px; }
.pop-stats .no-data { color: var(--el-color-danger); }
.pop-actions { margin-top: 8px; }

.sc-summary { display: flex; gap: 24px; margin-top: 16px; padding-top: 16px; border-top: 1px solid var(--border-color); }
.summary-item { display: flex; flex-direction: column; gap: 2px; }
.s-label { font-size: var(--fs-xs); color: var(--text-muted); }
.summary-item strong { font-size: var(--fs-lg); }
.summary-item.covered strong { color: var(--el-color-success, #67c23a); }
</style>

<template>
  <div class="baseline-page">
    <div class="page-header">
      <div class="header-left">
        <el-button text class="back-btn" @click="$router.push('/teacher/research')">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <h3 class="page-title">学生基线快照</h3>
      </div>
    </div>

    <!-- 操作卡片 -->
    <el-card shadow="never" class="action-card">
      <template #header><span>拍摄基线快照</span></template>
      <el-alert type="info" :closable="false" show-icon style="margin-bottom:16px">
        基线快照会<strong>自动过滤</strong>：仅拍摄班级管理中已设置<strong>课题组别（实验班/对照班）</strong>的学生数据。
        非参研班级（未设置课题组别的班级）的学生将被自动排除，不会混入实验数据。
      </el-alert>

      <el-row :gutter="16" align="middle">
        <el-col :span="6">
          <el-form-item label="快照标签">
            <el-select v-model="captureLabel" style="width:100%">
              <el-option label="前测 (PRETEST)" value="PRETEST" />
              <el-option label="中期 (MIDTEST)" value="MIDTEST" />
              <el-option label="后测 (POSTTEST)" value="POSTTEST" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-button
            type="primary"
            size="large"
            :loading="capturing"
            @click="handleCapture"
          >
            <el-icon><Camera /></el-icon> 拍摄快照
          </el-button>
        </el-col>
      </el-row>

      <!-- 拍摄结果 -->
      <el-result
        v-if="captureResult"
        icon="success"
        :title="'快照拍摄成功'"
        :sub-title="`共保存 ${captureResult.totalRecords} 条记录，覆盖 ${captureResult.studentCount} 名参研学生${captureResult.excludedNonResearch ? '（已自动排除 ' + captureResult.excludedNonResearch + ' 条非参研班级数据）' : ''}`"
      >
        <template #extra>
          <el-button type="primary" @click="handleExport(captureResult.snapshotLabel)">
            <el-icon><Download /></el-icon> 导出CSV
          </el-button>
        </template>
      </el-result>
    </el-card>

    <!-- 已有快照摘要 -->
    <el-card shadow="never" class="summary-card">
      <template #header><span>快照摘要</span></template>

      <el-form :inline="true" class="filter-bar">
        <el-form-item label="快照标签">
          <el-select v-model="summaryLabel" style="width:160px" @change="loadSummary">
            <el-option label="前测 (PRETEST)" value="PRETEST" />
            <el-option label="中期 (MIDTEST)" value="MIDTEST" />
            <el-option label="后测 (POSTTEST)" value="POSTTEST" />
          </el-select>
        </el-form-item>
        <el-form-item label="课题组别">
          <el-select v-model="summaryGroup" style="width:130px" @change="loadSummary" clearable placeholder="全部">
            <el-option label="实验班" value="EXPERIMENT" />
            <el-option label="对照班" value="CONTROL" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="small" @click="loadSummary">查询</el-button>
          <el-button size="small" @click="handleExport(summaryLabel)">
            <el-icon><Download /></el-icon> 导出CSV
          </el-button>
        </el-form-item>
      </el-form>

      <div v-loading="summaryLoading">
        <el-empty v-if="!summary" description="暂无快照数据，请先拍摄快照" />
        <template v-else>
          <el-row :gutter="24" class="summary-stats">
            <el-col :span="6">
              <div class="stat-box">
                <div class="stat-value">{{ summary.studentCount || 0 }}</div>
                <div class="stat-label">学生人数</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-box">
                <div class="stat-value">{{ summary.totalRecords || 0 }}</div>
                <div class="stat-label">记录条数</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-box">
                <div class="stat-value">{{ summary.avgMastery || 0 }}%</div>
                <div class="stat-label">平均掌握度</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-box">
                <div class="stat-value">{{ formatTime(summary.snapshotTime) }}</div>
                <div class="stat-label">拍摄时间</div>
              </div>
            </el-col>
          </el-row>

          <!-- 按学科分布 -->
          <div v-if="summary.bySubject" class="dist-section">
            <h4>按学科分布</h4>
            <div class="dist-tags">
              <el-tag v-for="(count, subj) in summary.bySubject" :key="subj" size="small" type="info">
                {{ subj }}: {{ count }}条
              </el-tag>
            </div>
          </div>

          <!-- 按课题组别分布 -->
          <div v-if="summary.byResearchGroup" class="dist-section">
            <h4>按课题组别分布</h4>
            <div class="dist-tags">
              <el-tag
                v-for="(count, group) in summary.byResearchGroup"
                :key="group"
                size="small"
                :type="group === 'EXPERIMENT' ? 'success' : group === 'CONTROL' ? 'primary' : 'info'"
              >
                {{ groupLabel(group) }}: {{ count }}条
              </el-tag>
            </div>
          </div>
        </template>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Camera, Download } from '@element-plus/icons-vue'
import { captureBaseline, exportBaselineCsv, getBaselineSummary } from '@/api/research'

const captureLabel = ref('PRETEST')
const capturing = ref(false)
const captureResult = ref(null)

const summaryLabel = ref('PRETEST')
const summaryGroup = ref('')
const summaryLoading = ref(false)
const summary = ref(null)

const handleCapture = async () => {
  try {
    await ElMessageBox.confirm(
      `确定拍摄"${captureLabel.value}"基线快照？此操作将冻结当前所有学生的掌握度数据。`,
      '确认拍摄',
      { type: 'warning' }
    )
  } catch { return }

  capturing.value = true
  try {
    const res = await captureBaseline(captureLabel.value)
    if (res.code === 200) {
      captureResult.value = res.data
      ElMessage.success('快照拍摄成功')
      loadSummary()
    } else {
      ElMessage.error(res.message || '拍摄失败')
    }
  } catch (e) {
    ElMessage.error('拍摄失败: ' + (e?.message || e))
  } finally {
    capturing.value = false
  }
}

const loadSummary = async () => {
  summaryLoading.value = true
  try {
    const res = await getBaselineSummary(summaryLabel.value, summaryGroup.value || undefined)
    if (res.code === 200) {
      summary.value = res.data
    } else {
      summary.value = null
    }
  } catch {
    summary.value = null
  } finally {
    summaryLoading.value = false
  }
}

const handleExport = async (label) => {
  try {
    const res = await exportBaselineCsv(label, summaryGroup.value || undefined)
    const blob = new Blob([res], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `baseline-${(label || 'pretest').toLowerCase()}.csv`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败: ' + (e?.message || e))
  }
}

const groupLabel = (g) => ({
  EXPERIMENT: '实验班',
  CONTROL: '对照班',
  UNSET: '未分组'
}[g] || g)

const formatTime = (t) => {
  if (!t) return '--'
  const d = new Date(t)
  return `${d.getMonth()+1}/${d.getDate()} ${d.getHours()}:${String(d.getMinutes()).padStart(2,'0')}`
}

onMounted(() => { loadSummary() })
</script>

<style scoped>
.baseline-page { max-width: 960px; margin: 0 auto; padding: 16px; }
.page-header { margin-bottom: 16px; }
.header-left { display: flex; align-items: center; gap: 8px; }
.page-title { margin: 0; font-size: 18px; }
.action-card { margin-bottom: 16px; }
.summary-card { margin-bottom: 16px; }
.filter-bar { margin-bottom: 16px; }
.summary-stats { margin-bottom: 24px; }
.stat-box { text-align: center; padding: 16px; background: var(--bg-section, #f5f7fa); border-radius: 8px; }
.stat-value { font-size: 24px; font-weight: 700; color: var(--primary-color); }
.stat-label { font-size: var(--fs-sm); color: var(--text-secondary); margin-top: 4px; }
.dist-section { margin-top: 16px; }
.dist-section h4 { margin: 0 0 8px; font-size: 14px; color: var(--text-regular); }
.dist-tags { display: flex; gap: 8px; flex-wrap: wrap; }
</style>

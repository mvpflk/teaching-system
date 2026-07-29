<template>
  <div class="page-card">
    <div class="page-header">
      <h3 class="page-title">教师活跃度排行</h3>
      <el-input
        v-model="search"
        placeholder="搜索教师..."
        clearable
        size="default"
        class="desktop-width"
        style="width:200px"
      />
    </div>

    <!-- 移动端卡片 -->
    <template v-if="isMobile">
      <div v-loading="loading" class="mobile-card-list">
        <div
          v-for="(row, idx) in filteredList"
          :key="row.teacherId || idx"
          class="mobile-teacher-card"
        >
          <div class="mtc-header">
            <span class="mtc-rank">#{{ idx + 1 }}</span>
            <span class="mtc-name">{{ row.teacherName }}</span>
          </div>
          <div class="mtc-stats">
            <div class="mtc-stat"><span class="mtc-stat-val">{{ row.tasksCreated || 0 }}</span><span class="mtc-stat-lbl">创建任务</span></div>
            <div class="mtc-stat"><span class="mtc-stat-val">{{ row.submissionsReceived || 0 }}</span><span class="mtc-stat-lbl">收到提交</span></div>
            <div class="mtc-stat"><span class="mtc-stat-val">{{ row.submissionsGraded || 0 }}</span><span class="mtc-stat-lbl">已批改</span></div>
            <div class="mtc-stat primary"><span class="mtc-stat-val">{{ row.activityScore || 0 }}</span><span class="mtc-stat-lbl">活跃度</span></div>
          </div>
          <div class="mtc-bar">
            <div class="mtc-bar-track"><div class="mtc-bar-fill" :style="{ width: barWidth(row.activityScore) + '%' }"></div></div>
          </div>
        </div>
        <el-empty v-if="!loading && filteredList.length === 0" :description="search ? '未找到匹配的教师' : '暂无教师数据'" :image-size="60" />
      </div>
    </template>

    <!-- 桌面端表格 -->
    <template v-else>
      <el-table
        v-loading="loading"
        :data="filteredList"
        stripe
        empty-text="暂无教师数据"
      >
        <el-table-column label="排名" width="60">
          <template #default="{ $index }"><strong>#{{ $index + 1 }}</strong></template>
        </el-table-column>
        <el-table-column prop="teacherName" label="教师" width="120" />
        <el-table-column
          prop="tasksCreated"
          label="创建任务"
          width="90"
          align="center"
        />
        <el-table-column
          prop="submissionsReceived"
          label="收到提交"
          width="100"
          align="center"
        />
        <el-table-column
          prop="submissionsGraded"
          label="已批改"
          width="90"
          align="center"
        />
        <el-table-column label="活跃度分" width="160" align="center">
          <template #default="{ row }">
            <div class="activity-bar"><div class="activity-fill" :style="{ width: barWidth(row.activityScore) + '%' }"></div></div>
            <span style="font-size:var(--fs-xs);color:var(--text-secondary)">{{ row.activityScore || 0 }} 分</span>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && filteredList.length === 0" class="empty-state">
        <el-empty :description="search ? '未找到匹配的教师' : '暂无教师数据'" :image-size="80" />
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTeacherActivity } from '@/api/inspector'
import { useIsMobile } from '@/composables/useIsMobile'

const { isMobile } = useIsMobile()
const list = ref([])
const loading = ref(false)
const search = ref('')
const maxScore = ref(200)

const filteredList = computed(() => {
  if (!search.value) return list.value
  const kw = search.value.toLowerCase()
  return list.value.filter(r => (r.teacherName || '').toLowerCase().includes(kw))
})

const barWidth = (score) => {
  if (!maxScore.value) return 0
  return Math.min(100, Math.round((score || 0) / maxScore.value * 100))
}

onMounted(async () => {
  loading.value = true
  try {
    const r = await getTeacherActivity()
    if (r.code === 200) {
      list.value = r.data || []
      const scores = list.value.map(t => t.activityScore || 0)
      if (scores.length) maxScore.value = Math.max(...scores, 1)
    }
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
})
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-title { font-size: 22px; margin: 0; }
.activity-bar { width: 100px; height: 6px; background: var(--bg-secondary); border-radius: var(--radius-xs); overflow: hidden; display: inline-block; vertical-align: middle; margin-right: 6px; }
.activity-fill { height: 100%; background: var(--primary-gradient); border-radius: var(--radius-xs); transition: width 0.6s; }
.empty-state { padding: 40px 0; }

/* 移动端卡片 */
.mobile-card-list { display: flex; flex-direction: column; gap: 10px; }
.mobile-teacher-card {
  padding: 14px; background: var(--bg-card); border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
}
.mtc-header { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.mtc-rank { width: 24px; height: 24px; border-radius: 50%; background: var(--primary-color); color: #fff; display: flex; align-items: center; justify-content: center; font-size: var(--fs-xs); font-weight: 600; flex-shrink: 0; }
.mtc-name { font-weight: 600; font-size: var(--fs-md); color: var(--text-primary); }
.mtc-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; margin-bottom: 10px; }
.mtc-stat { text-align: center; }
.mtc-stat-val { display: block; font-size: var(--fs-lg); font-weight: 700; color: var(--text-primary); }
.mtc-stat-lbl { display: block; font-size: var(--fs-xs); color: var(--text-secondary); margin-top: 2px; }
.mtc-stat.primary .mtc-stat-val { color: var(--primary-color); }
.mtc-bar-track { height: 6px; background: var(--bg-secondary); border-radius: var(--radius-xs); overflow: hidden; }
.mtc-bar-fill { height: 100%; background: var(--primary-gradient); border-radius: var(--radius-xs); transition: width 0.6s; }

@media (max-width: 768px) {
  :deep(.el-table) { font-size: var(--fs-xs); }
  .page-header { flex-direction: column; align-items: stretch; gap: 8px; }
  .mtc-stats { grid-template-columns: repeat(2, 1fr); }
}
</style>

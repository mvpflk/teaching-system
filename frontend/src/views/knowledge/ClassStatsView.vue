<template>
  <div class="class-stats">
    <div class="header">
      <h3>📊 全班知识学习统计</h3>
      <el-button @click="$router.back()">← 返回</el-button>
    </div>
    <div v-if="loading" class="loading"><el-skeleton :rows="6" animated /></div>
    <div v-else-if="classes.length === 0"><el-empty description="暂无教学班级数据" /></div>
    <div v-else class="class-list">
      <div v-for="cls in classes" :key="cls.classId" class="class-card">
        <div class="class-title">
          <span class="class-name">{{ cls.className }}</span>
          <span class="class-counts">{{ cls.activeStudentCount }}/{{ cls.studentCount }} 人已参与</span>
        </div>
        <div class="class-summary">
          <div class="summary-item"><span class="label">平均完成率</span><span class="val">{{ calcCompletion(cls) }}%</span></div>
          <div class="summary-item"><span class="label">全校均分</span><span class="val">{{ calcClassAvg(cls) }}</span></div>
          <div class="summary-item"><span class="label">未参与学生</span><span class="val warn">{{ cls.inactiveStudents.length }} 人</span></div>
        </div>
        <div v-if="cls.classWeakTags.length" class="weak-tags">
          <span class="tag-label">薄弱标签：</span>
          <el-tag v-for="t in cls.classWeakTags.slice(0, 5)" :key="t.tag" size="small" type="danger" round>{{ t.tag }}×{{ t.count }}</el-tag>
        </div>
        <div v-if="cls.articleStats.length" class="article-table">
          <table>
            <tr><th>文章</th><th>章节</th><th>答题人次</th><th>均分</th></tr>
            <tr v-for="a in cls.articleStats" :key="a.articleId"
              :class="{ 'low-score': a.avgScore < 60 }">
              <td class="art-title">{{ a.title }}</td>
              <td>{{ a.chapter }}</td>
              <td>{{ a.attemptedCount }}/{{ a.totalStudents }}</td>
              <td><el-tag :type="scoreType(a.avgScore)" size="small">{{ a.avgScore }}</el-tag></td>
            </tr>
          </table>
        </div>
        <div v-if="cls.inactiveStudents.length" class="inactive">
          <span class="tag-label">未参与学生：</span>
          <span v-for="s in cls.inactiveStudents" :key="s.studentId" class="inactive-tag">{{ s.studentNumber }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getClassStats } from '@/api/knowledgeBase'

import { useRoute } from 'vue-router'

const route = useRoute()
const loading = ref(true)
const classes = ref([])

const SUBJECT_ID = computed(() => Number(route.query.subjectId) || 24)

function calcCompletion(cls) {
  if (!cls.articleStats.length || !cls.studentCount) return 0
  const totalPossible = cls.articleStats.length * cls.studentCount
  const totalAttempts = cls.articleStats.reduce((s, a) => s + a.attemptedCount, 0)
  return Math.round(totalAttempts / totalPossible * 100)
}
function calcClassAvg(cls) {
  const scores = cls.articleStats.filter(a => a.avgScore > 0)
  if (!scores.length) return '--'
  const avg = scores.reduce((s, a) => s + a.avgScore, 0) / scores.length
  return Math.round(avg * 10) / 10
}
function scoreType(v) { return v >= 80 ? 'success' : v >= 60 ? 'warning' : 'danger' }

onMounted(async () => {
  try {
    const r = await getClassStats(SUBJECT_ID.value)
    classes.value = r.data || []
  } catch (e) {
    console.error('加载班级统计失败:', e)
    ElMessage.error('加载班级统计失败')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.class-stats { margin: 0 auto; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.header h3 { margin: 0; }
.loading { padding: 40px; }
.class-list { display: flex; flex-direction: column; gap: 20px; }
.class-card { background: var(--bg-card, #fff); border: 1px solid var(--border-light, #eee); border-radius: var(--radius-md, 12px); padding: 20px; }
.class-title { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.class-name { font-size: var(--fs-lg, 16px); font-weight: 600; }
.class-counts { font-size: var(--fs-xs, 12px); color: var(--text-secondary, #999); }
.class-summary { display: flex; gap: 16px; margin-bottom: 12px; }
.summary-item { display: flex; flex-direction: column; align-items: center; background: var(--bg-section, #f9fafb); padding: 10px 20px; border-radius: 8px; }
.summary-item .label { font-size: var(--fs-xs, 11px); color: var(--text-secondary, #999); }
.summary-item .val { font-size: 22px; font-weight: 700; color: var(--primary-color, var(--primary-color)); }
.summary-item .val.warn { color: var(--el-color-danger, #f56c6c); }
.weak-tags { display: flex; align-items: center; gap: 6px; margin-bottom: 12px; flex-wrap: wrap; }
.tag-label { font-size: var(--fs-xs, 12px); color: var(--text-secondary, #999); white-space: nowrap; }
.article-table { margin-bottom: 12px; }
.article-table table { width: 100%; border-collapse: collapse; font-size: var(--fs-sm, 13px); }
.article-table th { text-align: left; padding: 8px 6px; border-bottom: 2px solid var(--border-light, #eee); color: var(--text-secondary, #666); font-weight: 500; }
.article-table td { padding: 8px 6px; border-bottom: 1px solid var(--border-light, #eee); }
.article-table .art-title { max-width: 220px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.article-table .low-score { background: var(--bg-danger-light); }
.inactive { display: flex; flex-wrap: wrap; gap: 4px; align-items: center; }
.inactive-tag { padding: 2px 8px; background: var(--bg-section, #f3f4f6); border-radius: 4px; font-size: var(--fs-xs, 11px); color: var(--text-secondary, #999); }
</style>

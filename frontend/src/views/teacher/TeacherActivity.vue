 <template>
   <div v-loading="loading" class="activity-page">
     <h3>我的使用记录</h3>
     <el-timeline v-if="activities.length">
       <el-timeline-item
         v-for="a in activities"
         :key="a.id"
         :timestamp="a.createdAt?.replace('T', ' ')"
         placement="top"
       >
         <el-tag :type="actionColor(a.action)" size="small">{{ actionLabel(a.action) }}</el-tag>
         <span class="target-info" v-if="a.targetType"> — {{ a.targetType }}#{{ a.targetId }}</span>
       </el-timeline-item>
     </el-timeline>
     <el-empty v-else description="暂无使用记录" />
   </div>
 </template>
 
 <script setup>
 import { ref, onMounted } from 'vue'
 import { getMyActivities } from '@/api/teacherActivity'
 
 const loading = ref(false)
 const activities = ref([])
 
 const actionLabel = (a) => {
   const map = { CREATE_TASK: '创建任务', GRADE: '批改任务', AI_GENERATE: 'AI生成', AI_QUESTION: 'AI出题', VIEW_ANALYTICS: '查看分析', DIAGNOSE: '偏科诊断' }
   return map[a] || a
 }
 const actionColor = (a) => {
   const map = { CREATE_TASK: '', GRADE: 'success', AI_GENERATE: 'warning', AI_QUESTION: 'warning', VIEW_ANALYTICS: 'info', DIAGNOSE: 'danger' }
   return map[a] || 'info'
 }
 
 onMounted(async () => {
   loading.value = true
   try {
     const res = await getMyActivities()
     if (res?.code === 200) activities.value = res.data || []
   } finally { loading.value = false }
 })
 </script>
 
 <style scoped>
 .activity-page { padding: 20px; max-width: 600px; margin: 0 auto; }
 .target-info { color: var(--text-secondary); font-size: 12px; }
 </style>

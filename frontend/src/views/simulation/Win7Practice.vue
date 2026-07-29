<template>
  <div class="practice-page">
    <div v-if="loading" class="loading">加载任务中...</div>
    <div v-else-if="error" class="error">{{ error }} <button @click="loadTask">重试</button></div>
    <Win7Simulation v-else mode="practice" />
  </div>
</template>
<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useWin7SimStore } from '@/stores/win7Sim'
import { getSimTaskDefinition } from '@/api/simulation'
import Win7Simulation from '@/components/win7-sim/Win7Simulation.vue'

// ── Props（内嵌模式由 SimStep 传入）──
const props = defineProps({
  mode: { type: String, default: 'practice' },
  taskJson: { type: Object, default: null },
  initialVfs: { type: Object, default: null },
  timeLimit: { type: Number, default: 300 }
})
const emit = defineEmits(['complete', 'exit'])

// ── 兼容独立路由模式 ──
const route = useRoute()
const router = useRouter()
const isEmbedded = !!props.taskJson  // 有 taskJson prop → 内嵌模式

const store = useWin7SimStore()
const loading = ref(!isEmbedded)
const error = ref(null)

const isNetwork = !isEmbedded && route.path.includes('/network/')

async function loadTask() {
  if (isEmbedded) return  // 内嵌模式不需要 API 加载
  loading.value = true; error.value = null
  try {
    const id = route.params.id
    const res = await getSimTaskDefinition(id)
    if (res.code === 200) {
      store.resetAll()
      const data = res.data
      if (data.initialVfs) Object.assign(store.fileSystem, data.initialVfs)
      if (data.taskJson?.networkConfig) Object.assign(store.networkConfig, data.taskJson.networkConfig)
      store.loadTask(data.taskJson)
      if (isNetwork) {
        await new Promise(r => setTimeout(r, 200))
        store.openWindow('cmd', '命令提示符')
        store.recordAction('launch', 'cmd')
      }
    } else {
      error.value = res.message || '加载失败'
    }
  } catch (e) {
    error.value = '网络错误: ' + (e.message || e)
  } finally {
    loading.value = false
  }
}

function handleBeforeUnload(e) {
  e.preventDefault()
  e.returnValue = ''
}

async function handlePopState() {
  if (isEmbedded) {
    emit('exit')
  }
  if (store.actionLog.value?.length > 0) {
    try {
      await ElMessageBox.confirm('退出将丢失本次练习进度，确定离开吗？', '提示', {
        confirmButtonText: '离开', cancelButtonText: '留下', type: 'warning'
      })
    } catch { history.pushState(null, '', route.href) }
  }
}

onMounted(() => {
  if (isEmbedded && props.taskJson) {
    store.resetAll()
    if (props.initialVfs) Object.assign(store.fileSystem, props.initialVfs)
    if (props.taskJson.networkConfig) Object.assign(store.networkConfig, props.taskJson.networkConfig)
    store.loadTask(props.taskJson)
    loading.value = false
  } else {
    loadTask()
  }
  window.addEventListener('beforeunload', handleBeforeUnload)
  window.addEventListener('popstate', handlePopState)
})
onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
  window.removeEventListener('popstate', handlePopState)
})
</script>
<style scoped>
.practice-page { width: 100%; height: 100vh; overflow: hidden; }
.loading, .error { display: flex; align-items: center; justify-content: center; height: 100%; color: #fff; font-size: var(--fs-lg); flex-direction: column; gap: 12px; }
.error button { padding: 8px 20px; border: 1px solid #fff; border-radius: 4px; background: rgba(255,255,255,0.1); color: #fff; cursor: pointer; }
</style>

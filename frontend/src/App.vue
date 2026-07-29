<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, watch } from 'vue'
import { useNotificationStore } from '@/stores/notification'

let lastCount = 0

const requestPermission = async () => {
  if (!('Notification' in window)) return
  if (Notification.permission === 'default') {
    await Notification.requestPermission()
  }
}

const notifyStore = useNotificationStore()

// 桌面通知：监听未读数变化
watch(() => notifyStore.unreadCount, (count) => {
  if (!('Notification' in window) || Notification.permission !== 'granted') return
  if (count > lastCount && lastCount > 0) {
    new Notification('教学管理系统', {
      body: `您有 ${count - lastCount} 条新通知，共 ${count} 条未读`,
      icon: '/favicon.ico',
      tag: 'teaching-notify'
    })
  }
  lastCount = count
})

onMounted(() => {
  requestPermission()
  if (localStorage.getItem('token')) {
    lastCount = notifyStore.unreadCount
    notifyStore.connectSSE()
  }
})
onUnmounted(() => {
  notifyStore.disconnectSSE()
  notifyStore.stopPolling()
})
</script>

<style>
#app {
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 0;
}
</style>

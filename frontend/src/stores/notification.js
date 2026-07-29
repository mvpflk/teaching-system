import { defineStore } from 'pinia'
import { getNotificationList, markRead, markAllRead } from '@/api/notification'
import request from '@/utils/request'
import { createEventSource } from '@/utils/sseTicket'

export const useNotificationStore = defineStore('notification', {
  state: () => ({
    unreadCount: 0,
    latestList: [],
    lastFetchTime: null,
    polling: null,
    pollingInterval: 30000,
    eventSource: null,
    sseConnected: false
  }),

  actions: {
    async fetchUnreadCount() {
      try {
        const res = await request({ url: '/notification/unread-count', method: 'get' })
        if (res.code === 200) {
          this.unreadCount = res.data?.count ?? res.data ?? 0
        }
      } catch { /* silent */ }
    },

    async fetchLatestList() {
      try {
        const res = await getNotificationList({ page: 1, pageSize: 8 })
        if (res.code === 200) {
          this.latestList = res.data.records || []
          this.unreadCount = res.data.unreadCount ?? this.unreadCount
          this.lastFetchTime = Date.now()
        }
      } catch { /* silent */ }
    },

    async markOneRead(id) {
      try {
        await markRead(id)
        const item = this.latestList.find(n => n.id === id)
        if (item) item.isRead = 1
        this.unreadCount = Math.max(0, this.unreadCount - 1)
      } catch { /* silent */ }
    },

    async markAllRead() {
      try {
        const res = await markAllRead()
        if (res.code === 200) {
          this.unreadCount = 0
          this.latestList.forEach(n => { n.isRead = 1 })
        }
      } catch { /* silent */ }
    },

    startPolling(interval) {
      if (interval) this.pollingInterval = interval
      this.stopPolling()
      this.fetchUnreadCount()
      this.polling = setInterval(() => this.fetchUnreadCount(), this.pollingInterval)
    },

    stopPolling() {
      if (this.polling) {
        clearInterval(this.polling)
        this.polling = null
      }
    },

    /** SSE 实时连接 — 断开时自动回退到轮询 */
    async connectSSE() {
      this.disconnectSSE()
      const token = localStorage.getItem('token')
      if (!token) return

      // 立即拉取未读数，不等 SSE 回调
      this.fetchUnreadCount()

      const es = await createEventSource('/api/sse/subscribe')

      es.addEventListener('notification', (event) => {
        try {
          const data = JSON.parse(event.data)
          this.unreadCount++
          this.latestList.unshift({
            id: data.id,
            title: data.title,
            content: data.content,
            type: data.type,
            relatedId: data.relatedId,
            createTime: data.createTime,
            isRead: 0
          })
          if (this.latestList.length > 8) this.latestList.pop()
        } catch { /* parse error */ }
      })

      es.onopen = () => {
        this.sseConnected = true
        if (this.polling) {
          this.stopPolling()
          this.startPolling(300000)
        }
      }

      es.onerror = () => {
        this.sseConnected = false
        this.stopPolling()
        this.startPolling(30000)
      }

      this.eventSource = es
    },

    disconnectSSE() {
      if (this.eventSource) {
        this.eventSource.close()
        this.eventSource = null
        this.sseConnected = false
      }
    }
  }
})

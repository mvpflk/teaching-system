import { defineStore } from 'pinia'
import { getFeatureFlags } from '@/api/settings'

export const useSettingsStore = defineStore('settings', {
  state: () => ({
    features: {},
    loaded: false
  }),
  getters: {
    /** 检查某项功能是否启用（未加载时返回 false） */
    isEnabled: (state) => (key) => state.features[key] === true,
    /** 所有 feature flags */
    allFeatures: (state) => state.features
  },
  actions: {
    async fetchFeatureFlags() {
      try {
        const res = await getFeatureFlags()
        if (res.code === 200 && res.data) {
          this.features = res.data
        }
      } catch {
        // 网络异常：所有功能默认关闭
        this.features = {}
      }
      this.loaded = true
    }
  }
})

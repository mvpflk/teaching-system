<template>
  <div class="typing-header">
    <el-button text class="back-btn" @click="$emit('go-back')">
      <el-icon><ArrowLeft /></el-icon> 返回
    </el-button>
    <div class="level-bar">
      <span class="lv">Lv.{{ level.levelId }}</span>
      <el-progress
        :percentage="expPercent"
        :stroke-width="10"
        :show-text="false"
        class="exp-bar"
      />
      <span class="exp-text">{{ level.exp }}/{{ expToNext }}</span>
    </div>
    <el-button text class="material-btn" @click="$emit('go-materials')">
      <el-icon><Document /></el-icon> 选择素材
    </el-button>
    <el-button
      text
      circle
      :aria-label="isDark ? '切换到亮色主题' : '切换到暗色主题'"
      class="theme-btn"
      @click="$emit('toggle-theme')"
    >
      <el-icon size="18"><Moon v-if="isDark" /><Sunny v-else /></el-icon>
    </el-button>
  </div>
</template>

<script setup>
import { ArrowLeft, Document, Moon, Sunny } from '@element-plus/icons-vue'

defineProps({
  level: { type: Object, required: true },
  expPercent: { type: Number, required: true },
  expToNext: { type: Number, required: true },
  isDark: { type: Boolean, required: true }
})

defineEmits(['toggle-theme', 'go-back', 'go-materials'])
</script>

<style scoped>
.typing-header { display: flex; align-items: center; gap: 12px; }
.back-btn { color: var(--typing-pending); font-size: var(--fs-md); }
.back-btn:hover { color: var(--typing-text); }
.level-bar { display: flex; align-items: center; gap: 8px; flex: 1; max-width: 280px; }
.lv { font-weight: 800; color: var(--typing-cursor); white-space: nowrap; font-size: var(--fs-lg); letter-spacing: 1px; }
.exp-bar { flex: 1; }
.exp-text { font-size: var(--fs-xs); color: var(--typing-pending); white-space: nowrap; font-variant-numeric: tabular-nums; }
.material-btn { color: var(--typing-pending); font-size: var(--fs-sm); }
.material-btn:hover { color: var(--typing-text); }
.theme-btn { color: var(--typing-pending); }
</style>

<template>
  <div class="qp-body qp-pick-stage">
    <div class="qp-question-preview">
      <div class="qp-qp-label">当前题目</div>
      <div class="qp-qp-text">{{ questionText }}</div>
      <button class="qp-link" @click="$emit('goBack')"><el-icon><ArrowLeft /></el-icon> 换题</button>
    </div>
    <div class="qp-settings">
      <el-checkbox v-model="settings.excludeAbsent" size="large">排除缺勤学生</el-checkbox>
      <el-checkbox v-model="settings.downWeightPicked" size="large">已抽过降权 (×0.3)</el-checkbox>
      <el-checkbox v-model="settings.downWeightCorrect" size="large">已答对降权 (×0.2)</el-checkbox>
    </div>
    <div class="qp-draw-area">
      <el-button type="warning" size="large" :loading="picking" class="qp-draw-btn" @click="$emit('pick')">
        <el-icon><Aim /></el-icon> 随机抽人
      </el-button>
      <Transition name="pop">
        <div v-if="pickedStudent" class="qp-picked">
          <div class="qp-picked-name">{{ pickedStudent.studentName }}</div>
          <div class="qp-picked-actions">
            <el-button type="primary" size="large" @click="$emit('confirm')">确认，开始评分 →</el-button>
            <el-button size="small" @click="$emit('removeFromPool', pickedStudent)"><el-icon><Minus /></el-icon> 手动移除</el-button>
            <button class="qp-link" @click="$emit('clearPicked')">重新抽人</button>
          </div>
        </div>
      </Transition>
    </div>
  </div>
</template>

<script setup>
import { ArrowLeft, Aim, Minus } from '@element-plus/icons-vue'

defineProps({
  questionText: String,
  picking: Boolean,
  pickedStudent: Object,
  settings: { type: Object, default: () => ({ excludeAbsent: true, downWeightPicked: true, downWeightCorrect: true }) }
})

defineEmits(['goBack', 'pick', 'confirm', 'removeFromPool', 'clearPicked'])
</script>

<style scoped>
.qp-body { flex: 1; overflow-y: auto; }
.qp-pick-stage { text-align: center; }
.qp-question-preview { background: var(--bg-section); border-radius: var(--radius-lg); border: 0.5px solid var(--border-light); padding: 20px; margin-bottom: var(--spacing-lg); text-align: left; }
.qp-qp-label { font-size: var(--fs-xs); color: var(--text-secondary); margin-bottom: var(--spacing-sm); font-weight: 600; }
.qp-qp-text { font-size: var(--fs-lg); font-weight: 600; color: var(--text-primary); line-height: 1.6; white-space: pre-wrap; word-break: break-word; }
.qp-settings { display: flex; gap: 20px; justify-content: center; margin-bottom: var(--spacing-xl); flex-wrap: wrap; color: var(--text-regular); }
.qp-draw-area { margin: 20px 0; }
.qp-draw-btn { font-size: var(--fs-xl); padding: 18px 48px; border-radius: var(--radius-lg); height: auto; }
.qp-picked { margin-top: var(--spacing-lg); display: flex; flex-direction: column; align-items: center; }
.qp-picked-name { font-size: 36px; font-weight: 800; color: var(--el-color-warning); }
.qp-picked-actions { display: flex; flex-direction: column; align-items: center; gap: var(--spacing-xs); margin-top: var(--spacing-md); }
.qp-link { display: inline-flex; align-items: center; gap: 5px; font-size: var(--fs-sm); font-family: inherit; color: var(--text-secondary); padding: 6px 14px; border-radius: var(--radius-md); border: 0.5px solid transparent; background: transparent; cursor: pointer; transition: all var(--transition-base); margin-top: 10px; }
.qp-link:hover { color: var(--primary-color); background: var(--primary-light); border-color: var(--primary-color); }
</style>
<template>
  <div class="dcs">
    <div class="dcs-score">{{ result.correctCount }}/{{ result.totalQuestions }} 正确</div>
    <p v-if="result.creditsEarned" class="dcs-credits">+{{ result.creditsEarned }} 积分</p>
    <p class="dcs-streak">🔥 连续 {{ result.streak || 0 }} 天</p>
    <p class="dcs-owl">{{ result.owlMessage || '👍' }}</p>
    <div style="display:flex;gap:10px;justify-content:center;margin-top:14px">
      <el-button size="small" @click="$emit('again')">再来一组</el-button>
      <el-button size="small" type="primary" @click="$emit('back')">返回仪表盘</el-button>
    </div>
  </div>
</template>
<script setup>defineProps({ result: Object }); defineEmits(['again', 'back'])</script>
<style scoped>
.dcs { text-align: center; padding: 20px; }
.dcs-score { font-size: var(--fs-2xl); font-weight: 700; color: var(--primary-color); }
.dcs-credits { font-size: var(--fs-lg); color: var(--text-primary, var(--text-primary)); }
.dcs-streak { color: var(--el-color-warning); margin: 8px 0; }
.dcs-owl { font-size: var(--fs-sm); color: var(--text-secondary, var(--text-secondary)); }
</style>

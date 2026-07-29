<template>
  <div class="behavior-picker">
    <h4 class="behavior-picker__title">选择任务类型</h4>
    <p class="behavior-picker__sub">3 种行为覆盖全部教学场景，系统会自动处理细节配置</p>
    <div class="behavior-cards">
      <div v-for="b in cards" :key="b.behavior" class="behavior-card" @click="$emit('select', b.behavior)">
        <div class="behavior-card__icon">{{ b.icon }}</div>
        <div class="behavior-card__name">{{ b.name }}</div>
        <div class="behavior-card__desc">{{ b.desc }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({ cards: { type: Array, default: () => [] } })
defineEmits(['select'])
</script>

<style scoped>
.behavior-picker { text-align: center; padding: 40px 16px; }
.behavior-picker__title { font-size: var(--fs-xl); font-weight: 600; color: var(--text-primary); margin: 0 0 8px; }
.behavior-picker__sub { font-size: var(--fs-md); color: var(--text-secondary); margin: 0 0 32px; }
.behavior-cards { display: flex; gap: 20px; justify-content: center; flex-wrap: wrap; }
.behavior-card { width: 240px; padding: 32px 20px; border: 1.5px solid var(--border-light); border-radius: var(--radius-lg); cursor: pointer; transition: all 0.2s; background: var(--bg-card); }
.behavior-card:hover { border-color: var(--primary-color); transform: translateY(-2px); }
.behavior-card__icon { font-size: 40px; margin-bottom: 12px; }
.behavior-card__name { font-size: 17px; font-weight: 600; color: var(--text-primary); margin-bottom: 8px; }
.behavior-card__desc { font-size: var(--fs-sm); color: var(--text-secondary); line-height: 1.5; }
</style>

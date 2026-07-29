<template>
  <div class="owl" :class="'owl-' + mood">
    <span class="owl-icon">🦉</span>
    <span class="owl-msg">{{ message }}</span>
  </div>
</template>
<script setup>
defineProps({ message: String, mood: { type: String, default: 'normal' } })
</script>
<style scoped>
.owl { display: flex; align-items: center; gap: 8px; padding: 10px 14px; border-radius: 8px; margin: 10px 0; font-size: var(--fs-md); transition: all .3s; }
.owl-correct { background: #f6ffed; }
.owl-hint { background: #fff8e1; }
.owl-reveal { background: #fff2f0; }
.owl-normal { background: var(--bg-secondary, var(--bg-secondary)); }
.owl-icon { font-size: var(--fs-2xl); }
.owl-msg { color: var(--text-primary, var(--text-primary)); }
</style>

<template>
  <div class="basket-float" :class="{ 'basket-float--empty': basket.count === 0 }" @click="$emit('open')">
    <el-badge :value="basket.count" :hidden="basket.count === 0" :max="99">
      <span class="basket-float__text">试题篮</span>
    </el-badge>
  </div>
</template>

<script setup>
import { useQuestionBasketStore } from '@/stores/questionBasket'
defineEmits(['open'])
const basket = useQuestionBasketStore()
</script>

<style scoped>
.basket-float {
  position: fixed; right: 0; top: 50%; transform: translateY(-50%);
  z-index: 900; cursor: pointer;
  background: var(--primary-color); color: var(--el-color-white);
  padding: 14px 8px; border-radius: var(--radius-md) 0 0 var(--radius-md);
  box-shadow: var(--el-box-shadow-light);
  transition: opacity .2s;
}
.basket-float--empty { opacity: .75; }
.basket-float__text { writing-mode: vertical-lr; letter-spacing: 4px; font-size: var(--fs-sm); font-weight: 600; }
</style>

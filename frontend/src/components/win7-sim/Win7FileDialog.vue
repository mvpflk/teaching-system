<template>
  <div class="file-dialog">
    <div class="fd-header">{{ title }}</div>
    <div class="fd-body">
      <div class="fd-row"><label>名称:</label><span>{{ file?.name }}</span></div>
      <div class="fd-row"><label>类型:</label><span>{{ typeLabel }}</span></div>
      <div class="fd-row"><label>大小:</label><span>{{ file?.size || 0 }} 字节</span></div>
      <div class="fd-row"><label>路径:</label><span class="fd-path">{{ path }}</span></div>
      <div v-if="file?.type === 'file'" class="fd-row">
        <label><input v-model="readonly" type="checkbox" /> 只读</label>
        <label><input v-model="hidden" type="checkbox" /> 隐藏</label>
      </div>
    </div>
    <div class="fd-footer">
      <button @click="$emit('close')">确定</button>
      <button @click="$emit('close')">取消</button>
    </div>
  </div>
</template>
<script setup>
import { ref, computed } from 'vue'
const props = defineProps({ file: Object, path: String })
defineEmits(['close'])
const readonly = ref(false), hidden = ref(false)
const title = computed(() => props.file?.name + ' 属性')
const typeLabel = computed(() => props.file?.type === 'folder' ? '文件夹' : (props.file?.ext || '文件'))
</script>
<style scoped>
.file-dialog { width: 380px; background: #fff; border: 1px solid #999; border-radius: 6px; box-shadow: 0 4px 16px rgba(0,0,0,0.15); overflow: hidden; }
.fd-header { padding: 10px 14px; font-weight: 600; font-size: var(--fs-sm); border-bottom: 1px solid #e0e0e0; }
.fd-body { padding: 14px; }
.fd-row { display: flex; gap: 10px; margin-bottom: 10px; font-size: var(--fs-xs); align-items: center; }
.fd-row label { width: 50px; font-weight: 500; color: #555; }
.fd-path { color: #999; font-size: 10px; }
.fd-footer { display: flex; justify-content: flex-end; gap: 8px; padding: 10px 14px; border-top: 1px solid #e0e0e0; background: #f5f5f5; }
.fd-footer button { padding: 4px 20px; font-size: var(--fs-xs); border: 1px solid #ccc; border-radius: 3px; background: #fff; cursor: pointer; }
.fd-footer button:first-child { background: var(--primary-color); color: #fff; border-color: var(--primary-color); }
</style>

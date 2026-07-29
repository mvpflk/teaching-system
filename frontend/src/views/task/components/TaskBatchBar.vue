<template>
  <div v-if="selectedRows.length" class="batch-bar">
    <span class="batch-count">已选 {{ selectedRows.length }} 项</span>
    <el-button size="small" type="success" :disabled="!canPublish" @click="$emit('batch-publish')">
      批量发布
    </el-button>
    <el-button size="small" type="warning" :disabled="!canClose" @click="$emit('batch-close')">
      批量关闭
    </el-button>
    <el-button size="small" type="danger" @click="$emit('batch-delete')">批量删除</el-button>
    <el-button size="small" @click="$emit('clear-selection')">取消选择</el-button>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  selectedRows: { type: Array, default: () => [] },
})

defineEmits(['batch-publish', 'batch-close', 'batch-delete', 'clear-selection'])

const canPublish = computed(() => props.selectedRows.some((r) => r.status === 'DRAFT'))
const canClose = computed(() => props.selectedRows.some((r) => r.status === 'PUBLISHED' || r.status === 'ONGOING'))
</script>

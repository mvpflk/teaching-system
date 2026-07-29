<template>
  <el-dialog :model-value="visible" title="管理快捷评语" width="480px" destroy-on-close append-to-body @close="$emit('close')">
    <div class="quick-mgmt">
      <div class="quick-add-row">
        <el-input v-model="newCommentText" placeholder="输入新评语（最多500字）" maxlength="500" size="small" @keyup.enter="handleAdd" />
        <el-button type="primary" size="small" :loading="adding" @click="handleAdd">添加</el-button>
      </div>
      <el-table v-if="comments.length" :data="comments" size="small" style="margin-top:12px" max-height="300">
        <el-table-column prop="commentText" label="评语内容" min-width="280" show-overflow-tooltip />
        <el-table-column label="操作" width="60" align="center">
          <template #default="{ row }">
            <el-button size="small" text type="danger" @click="$emit('delete', row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!comments.length" description="暂无快捷评语" :image-size="60" />
    </div>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  visible: { type: Boolean, default: false },
  adding: { type: Boolean, default: false },
  comments: { type: Array, default: () => [] },
})

const emit = defineEmits(['close', 'add', 'delete'])

const newCommentText = ref('')

const handleAdd = () => {
  const text = newCommentText.value.trim()
  if (!text) return
  emit('add', text)
  newCommentText.value = ''
}
</script>

<style scoped>
.quick-mgmt { min-height: 200px; }
.quick-add-row { display: flex; gap: 8px; }
</style>

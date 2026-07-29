<template>
  <div v-show="!isMobile" class="save-bar">
    <div class="save-bar-left">
      <el-button plain size="default" @click="$emit('save-draft')">
        <el-icon><FolderOpened /></el-icon> 保存草稿
      </el-button>
    </div>
    <div class="save-bar-right">
      <el-button size="default" @click="$emit('cancel')">取消</el-button>
      <el-button-group>
        <el-button type="primary" size="default" :loading="saving" @click="$emit('publish')">
          {{ isEdit ? '保存修改' : '发布任务' }}
        </el-button>
        <el-dropdown v-if="!isEdit" trigger="click" @command="$emit('publish-continue')">
          <el-button type="primary" size="default" :disabled="saving">
            <el-icon><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="publish-and-new">发布并继续创建</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-button-group>
    </div>
  </div>
</template>

<script setup>
import { FolderOpened, ArrowDown } from '@element-plus/icons-vue'
defineProps({ saving: Boolean, isEdit: Boolean, isMobile: Boolean })
defineEmits(['save-draft', 'cancel', 'publish', 'publish-continue'])
</script>

<style scoped>
.save-bar { margin-top: 24px; padding-top: 16px; border-top: 1px solid var(--border-light); display: flex; align-items: center; justify-content: space-between; }
.save-bar-left, .save-bar-right { display: flex; align-items: center; gap: 8px; }
</style>

<template>
  <div class="page-header">
    <h3 class="page-title">任务管理</h3>
    <div class="header-actions" style="display: flex; gap: 8px; flex-wrap: wrap">
      <template v-if="isMobile">
        <el-button
          type="primary"
          size="small"
          style="flex: 1; min-width: 0"
          @click="$emit('create')"
        >
          <el-icon><Plus /></el-icon>创建
        </el-button>
        <el-button
          size="small"
          @click="$router.push('/teacher/tasks/paper-library')"
        >
          <el-icon><Folder /></el-icon>试卷库
        </el-button>
        <el-button
          size="small"
          type="success"
          @click="$router.push('/teacher/tasks/paper-import')"
        >
          <el-icon><Upload /></el-icon>导入
        </el-button>
      </template>
      <template v-else>
        <el-button
          type="success"
          @click="$router.push('/teacher/tasks/paper-import')"
        >
          <el-icon><Upload /></el-icon>导入试卷
        </el-button>
        <el-button @click="$router.push('/teacher/tasks/paper-library')">
          <el-icon><Folder /></el-icon>试卷库
        </el-button>
        <el-button @click="$emit('refresh')">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
        <el-button
          type="primary"
          @click="$emit('create')"
        >
          <el-icon><Plus /></el-icon>创建任务
        </el-button>
      </template>
    </div>
  </div>
</template>

<script setup>
import { Plus, Folder, Upload, Refresh } from '@element-plus/icons-vue';

defineProps({
  isMobile: { type: Boolean, default: false },
});

defineEmits(['create', 'refresh']);
</script>

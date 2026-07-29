<template>
  <div class="page-header">
    <h3 class="page-title">题库管理</h3>
    <div class="header-actions">
      <div class="header-actions__primary">
        <el-button type="primary" @click="$emit('create')"><el-icon><Plus /></el-icon>添加题目</el-button>
        <el-button type="success" @click="$emit('compose')"><el-icon><DocumentCopy /></el-icon>组卷</el-button>
      </div>
      <div class="header-actions__secondary">
        <el-button text size="small" @click="$emit('download-template','word')"><el-icon><Download /></el-icon>Word模板</el-button>
        <el-button text size="small" @click="$emit('download-template','excel')"><el-icon><Download /></el-icon>Excel模板</el-button>
        <el-button text size="small" type="warning" @click="$emit('import-word')"><el-icon><Upload /></el-icon>导入Word</el-button>
        <el-button text size="small" type="success" @click="$emit('import-excel')"><el-icon><Upload /></el-icon>导入Excel</el-button>
        <el-button v-if="isAdmin" text size="small" type="danger" @click="$emit('clear')"><el-icon><Delete /></el-icon>清空</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Plus, DocumentCopy, Download, Upload, Delete } from '@element-plus/icons-vue'
defineProps({ isAdmin: { type: Boolean, default: false } })
defineEmits(['create', 'compose', 'download-template', 'import-word', 'import-excel', 'clear'])
</script>

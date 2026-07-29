<template>
  <div>
    <el-tabs v-model="activeTypeModel" class="type-filter-tabs" @tab-change="$emit('type-change', activeTypeModel)">
      <el-tab-pane name="ALL"><template #label><span class="tab-label">全部</span></template></el-tab-pane>
      <el-tab-pane name="EXAM"><template #label><TaskIcon type="SUMMATIVE" :size="16" class="tab-icon" /> 考试</template></el-tab-pane>
      <el-tab-pane name="HOMEWORK"><template #label><TaskIcon type="AFTER_CLASS" :size="16" class="tab-icon" /> 作业</template></el-tab-pane>
      <el-tab-pane name="PRACTICE"><template #label><TaskIcon type="PRACTICE" :size="16" class="tab-icon" /> 实训</template></el-tab-pane>
      <el-tab-pane name="SIMULATION"><template #label><TaskIcon type="SIMULATION" :size="16" class="tab-icon" /> 仿真</template></el-tab-pane>
      <el-tab-pane name="SURVEY"><template #label><TaskIcon type="SURVEY" :size="16" class="tab-icon" /> 问卷</template></el-tab-pane>
    </el-tabs>

    <div v-if="isMobile" class="mobile-search-bar">
      <el-input v-model="filters.search" placeholder="搜索任务标题..." clearable size="small" class="mobile-search-input" @keyup.enter="doSearch" @clear="doSearch">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button size="small" :type="showMobileFilter ? 'primary' : ''" @click="$emit('update:show-mobile-filter', !showMobileFilter)">
        <el-icon><Filter /></el-icon>筛选<span v-if="activeFilterCount" class="filter-badge">{{ activeFilterCount }}</span>
      </el-button>
    </div>

    <el-form v-show="!isMobile || showMobileFilter" :inline="!isMobile" class="search-form">
      <el-form-item label="类型">
        <el-select v-model="filters.taskType" placeholder="全部类型" clearable size="small" @change="onSelectType">
          <el-option v-for="(label, key) in TASK_TYPE_FILTER_LABEL" :key="key" :value="key" :label="label" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="filters.status" placeholder="全部状态" clearable size="small" @change="doSearch">
          <el-option v-for="(label, key) in TASK_STATUS_LABEL" :key="key" :value="key" :label="label" />
        </el-select>
      </el-form-item>
      <el-form-item label="年级">
        <el-select v-model="filters.grade" placeholder="全部年级" clearable size="small" @change="onGradeChange">
          <el-option v-for="g in gradeList" :key="g" :value="g" :label="g" />
        </el-select>
      </el-form-item>
      <el-form-item label="班级">
        <el-select v-model="filters.className" placeholder="全部班级" clearable size="small" :disabled="!filters.grade" @change="doSearch">
          <el-option v-for="c in gradeClassOptions" :key="c" :value="c" :label="c" />
        </el-select>
      </el-form-item>
      <el-form-item label="搜索">
        <el-input v-model="filters.search" placeholder="标题" clearable size="small" @keyup.enter="doSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="small" @click="doSearch">搜索</el-button>
        <el-button size="small" @click="$emit('reset')">重置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Search, Filter } from '@element-plus/icons-vue'
import TaskIcon from '@/components/common/TaskIcon.vue'
import { TASK_TYPE_FILTER_LABEL, TASK_STATUS_LABEL } from '@/constants/taskType'

const props = defineProps({
  filters: { type: Object, required: true },
  activeType: { type: String, default: 'ALL' },
  showMobileFilter: { type: Boolean, default: false },
  isMobile: { type: Boolean, default: false },
  gradeList: { type: Array, default: () => [] },
  gradeClassOptions: { type: Array, default: () => [] },
  activeFilterCount: { type: Number, default: 0 },
})

const emit = defineEmits([
  'update:activeType', 'update:show-mobile-filter',
  'search', 'reset', 'select-type',
])

const activeTypeModel = computed({
  get: () => props.activeType,
  set: (val) => emit('update:activeType', val),
})

const doSearch = () => emit('search')
const onSelectType = () => emit('select-type')
const onGradeChange = () => { props.filters.className = ''; doSearch() }
</script>

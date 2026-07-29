<template>
  <div style="max-width:800px">
    <div style="margin-bottom:20px">
      <h3 style="margin:0 0 8px">偏科提分 — 配置</h3>
      <span style="color:var(--el-color-info);font-size:var(--fs-sm)">管理偏科提分模块的班级授权与阈值参数</span>
    </div>

    <!-- 阈值配置 -->
    <el-card v-loading="thLoading" shadow="never" style="margin-bottom:16px">
      <template #header><span>⚙️ 参数设置</span></template>
      <el-form label-width="200px" size="default">
        <el-form-item label="自动分组阈值（分）">
          <el-input-number
            v-model="threshold"
            :min="10"
            :max="90"
            :step="5"
          />
          <span style="margin-left:8px;color:#909399;font-size:var(--fs-xs)">诊断分低于此值的学生自动加入偏科组</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="thSaving" @click="saveThreshold">保存阈值</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 班级配置 -->
    <el-card v-loading="loading" shadow="never">
      <template #header><span>🏫 班级授权</span></template>
      <div style="margin-bottom:16px;color:#909399;font-size:var(--fs-sm)">选择允许使用偏科提分模块的班级。不选任何班级 = 全部班级可用。</div>
      <div style="margin-bottom:16px">
        <el-checkbox v-model="selectAll" :indeterminate="isIndeterminate" @change="handleSelectAll">
          全选（留空 = 所有班级可用）
        </el-checkbox>
      </div>

      <el-checkbox-group v-model="selectedIds">
        <div v-for="cls in classes" :key="cls.id" style="margin-bottom:6px">
          <el-checkbox :value="cls.id">
            {{ cls.className }}
            <span style="color:#909399;font-size:var(--fs-xs)">({{ cls.grade }})</span>
            <el-tag
              v-if="cls.remedialEnabled"
              size="small"
              type="success"
              style="margin-left:6px"
            >
              已开启
            </el-tag>
          </el-checkbox>
        </div>
      </el-checkbox-group>

      <el-empty v-if="!loading && classes.length === 0" description="暂无班级数据" />

      <div style="margin-top:20px">
        <el-button type="primary" :loading="saving" @click="saveClasses">保存班级授权</el-button>
        <el-button @click="loadAll">重置</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getRemedialClasses, updateRemedialClasses, getSettings, updateSettings } from '@/api/settings'

const loading = ref(false), saving = ref(false)
const thLoading = ref(false), thSaving = ref(false)
const classes = ref([]), selectedIds = ref([])
const threshold = ref(50)

const selectAll = computed({
  get: () => classes.value.length > 0 && selectedIds.value.length === classes.value.length,
  set: () => {}
})
const isIndeterminate = computed(() =>
  selectedIds.value.length > 0 && selectedIds.value.length < classes.value.length
)

const loadAll = async () => {
  loading.value = true; thLoading.value = true
  try {
    const [clsRes, setRes] = await Promise.all([getRemedialClasses(), getSettings()])
    if (clsRes.code === 200) {
      classes.value = clsRes.data.allClasses || []
      selectedIds.value = clsRes.data.enabledClassIds || []
    }
    if (setRes.code === 200) {
      const all = setRes.data || {}
      threshold.value = parseInt(all['remedial.auto_group_threshold'] || '50', 10)
    }
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false; thLoading.value = false }
}

const handleSelectAll = (val) => {
  selectedIds.value = val ? classes.value.map(c => c.id) : []
}

const saveClasses = async () => {
  saving.value = true
  try {
    const res = await updateRemedialClasses({ classIds: selectedIds.value })
    if (res.code === 200) { ElMessage.success('班级授权已保存'); await loadAll() }
    else { ElMessage.error(res.message || '保存失败') }
  } catch { ElMessage.error('保存失败') }
  finally { saving.value = false }
}

const saveThreshold = async () => {
  thSaving.value = true
  try {
    const res = await updateSettings({ 'remedial.auto_group_threshold': String(threshold.value) })
    if (res.code === 200) { ElMessage.success('阈值已更新') }
    else { ElMessage.error(res.message || '保存失败') }
  } catch { ElMessage.error('保存失败') }
  finally { thSaving.value = false }
}

onMounted(loadAll)
</script>

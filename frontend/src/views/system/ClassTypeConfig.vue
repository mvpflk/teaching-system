<template>
  <div>
    <div class="page-header">
      <h3 class="page-title">🏷️ 班级类型配置</h3>
      <span class="page-subtitle">普高班/职高班 两个固定类型，配置职高班的可选专业</span>
    </div>

    <el-row v-loading="loading" :gutter="24">
      <!-- 普高班 -->
      <el-col :xs="24" :sm="12">
        <el-card
          class="type-card"
          :class="{ active: activeType === 'general' }"
          shadow="hover"
          @click="activeType = 'general'"
        >
          <template #header>
            <div class="card-header">
              <el-tag type="" size="small">普高</el-tag>
              <span class="card-type-name">普高班</span>
              <el-icon v-if="activeType === 'general'" color="var(--primary-color)"><CircleCheckFilled /></el-icon>
            </div>
          </template>
          <p class="card-desc">面向普通高考的班级，无需配置专业。</p>
          <el-form-item label="类型名称">
            <el-input v-model="generalType.typeName" placeholder="普高班" @blur="saveType('general')" />
          </el-form-item>
        </el-card>
      </el-col>

      <!-- 职高班 -->
      <el-col :xs="24" :sm="12">
        <el-card
          class="type-card"
          :class="{ active: activeType === 'vocational' }"
          shadow="hover"
          @click="activeType = 'vocational'"
        >
          <template #header>
            <div class="card-header">
              <el-tag type="warning" size="small">职高</el-tag>
              <span class="card-type-name">职高班</span>
              <el-icon v-if="activeType === 'vocational'" color="var(--primary-color)"><CircleCheckFilled /></el-icon>
            </div>
          </template>
          <p class="card-desc">面向职业教育的班级，可配置默认专业。</p>
          <el-form-item label="类型名称">
            <el-input v-model="vocationalType.typeName" placeholder="职高班" @blur="saveType('vocational')" />
          </el-form-item>
          <el-form-item label="默认专业">
            <el-select
              v-model="vocationalType.defaultMajor"
              placeholder="选择默认专业"
              filterable
              clearable
              style="width:100%"
              @change="saveType('vocational')"
            >
              <el-option
                v-for="m in majorOptions"
                :key="m.id"
                :value="m.majorName"
                :label="m.majorName"
              />
            </el-select>
          </el-form-item>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { CircleCheckFilled } from '@element-plus/icons-vue'
import { getTypeConfigList, updateTypeConfig } from '@/api/classTypeConfig'
import { getMajors } from '@/api/settings'

const loading = ref(false)
const activeType = ref('general')
const majorOptions = ref([])
const generalType = reactive({ id: null, typeName: '普高班', typeCode: 'general', defaultMajor: '' })
const vocationalType = reactive({ id: null, typeName: '职高班', typeCode: 'vocational', defaultMajor: '' })

const loadTypes = async () => {
  loading.value = true
  try {
    const res = await getTypeConfigList()
    if (res.code === 200 && res.data) {
      const general = res.data.find(t => t.typeCode === 'general')
      const vocational = res.data.find(t => t.typeCode === 'vocational')
      if (general) { generalType.id = general.id; generalType.typeName = general.typeName || '普高班'; generalType.defaultMajor = general.defaultMajor || '' }
      if (vocational) { vocationalType.id = vocational.id; vocationalType.typeName = vocational.typeName || '职高班'; vocationalType.defaultMajor = vocational.defaultMajor || '' }
    }
  } catch { /* */ }
  finally { loading.value = false }
}

const loadMajors = async () => {
  try { const r = await getMajors(); if (r.code === 200) majorOptions.value = r.data || [] } catch { /* */ }
}

const saveType = async (code) => {
  const t = code === 'general' ? generalType : vocationalType
  if (!t.id) return
  try {
    await updateTypeConfig(t.id, { typeName: t.typeName, defaultMajor: t.defaultMajor || '' })
  } catch { /* */ }
}

onMounted(() => { loadTypes(); loadMajors() })
</script>

<style scoped>
.type-card { cursor: pointer; border: 2px solid transparent; transition: border-color 0.2s; margin-bottom: 16px; }
.type-card.active { border-color: var(--primary-color); }
.type-card :deep(.el-card__header) { padding: 12px 16px; }
.type-card :deep(.el-card__body) { padding: 16px; }
.card-header { display: flex; align-items: center; gap: 10px; }
.card-type-name { font-weight: 600; font-size: var(--fs-lg); flex: 1; }
.card-desc { color: var(--text-secondary); font-size: var(--fs-sm); margin: 0 0 16px 0; }

@media (max-width: 768px) {
  .type-card { margin-bottom: 12px; }
}
</style>

<template>
  <div class="rubric-panel">
    <div class="rp-header">
      <span class="rp-title">评分量规：{{ rubric?.rubricName || '' }}</span>
      <span class="rp-total">总分：{{ computedTotal }} / {{ maxTotal }}</span>
    </div>
    <div v-if="rubric?.dimensions?.length" class="rp-dimensions">
      <div v-for="dim in rubric.dimensions" :key="dim.dimensionId" class="rp-dim">
        <div class="rp-dim-header">
          <span class="rp-dim-name">{{ dim.name }}</span>
          <el-tag size="small" type="info">权重 {{ (dim.weight * 100).toFixed(0) }}%</el-tag>
        </div>
        <div v-if="dim.description" class="rp-dim-desc">{{ dim.description }}</div>
        <div class="rp-levels">
          <el-radio-group
            :model-value="getLevel(dim.dimensionId)"
            :disabled="disabled"
            @change="(v) => setLevel(dim.dimensionId, v)"
          >
            <el-radio-button v-for="lv in dim.levels" :key="lv.level" :value="lv.level">
              {{ lv.label || lv.level }}
            </el-radio-button>
          </el-radio-group>
        </div>
        <el-input
          v-if="!disabled"
          v-model="comments[dim.dimensionId]"
          placeholder="评语（可选）"
          size="small"
          class="rp-comment"
          maxlength="200"
        />
      </div>
    </div>
    <el-empty v-else description="该量规未配置评分维度" :image-size="60" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'

const props = defineProps({
  rubric: { type: Object, default: null },
  disabled: { type: Boolean, default: false },
})
const emit = defineEmits(['update:scores'])

const levels = reactive({})
const comments = reactive({})

const getLevel = (dimId) => levels[dimId] ?? null
const setLevel = (dimId, v) => { levels[dimId] = v; emitScores() }

function emitScores() {
  if (!props.rubric?.dimensions) return
  const dims = props.rubric.dimensions.map((dim) => ({
    dimensionId: dim.dimensionId, dimensionName: dim.name, weight: dim.weight,
    level: levels[dim.dimensionId] ?? 0,
    levelLabel: dim.levels?.find((l) => l.level === (levels[dim.dimensionId] ?? 0))?.label || '',
    comment: comments[dim.dimensionId] || '',
  }))
  emit('update:scores', { rubricId: props.rubric.rubricId, dimensions: dims })
}

const maxTotal = computed(() => (props.rubric?.dimensions?.length || 1) * 10)
const computedTotal = computed(() => {
  if (!props.rubric?.dimensions) return 0
  let total = 0
  for (const dim of props.rubric.dimensions) {
    const lvl = levels[dim.dimensionId] ?? 0
    const maxLvl = dim.levels?.length ? Math.max(...dim.levels.map((l) => l.level)) : 4
    if (maxLvl > 0) total += (lvl / maxLvl) * (dim.weight || 0) * 10
  }
  return total.toFixed(1)
})

watch(() => props.rubric?.rubricId, () => {
  Object.keys(levels).forEach((k) => delete levels[k])
  Object.keys(comments).forEach((k) => delete comments[k])
})
</script>

<style scoped>
.rubric-panel{border:1px solid var(--border-light);border-radius:var(--radius-md);padding:16px;margin-top:16px}
.rp-header{display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;padding-bottom:12px;border-bottom:1px solid var(--border-light)}
.rp-title{font-size:var(--fs-md);font-weight:600;color:var(--text-primary)}
.rp-total{font-size:var(--fs-lg);font-weight:700;color:var(--primary-color)}
.rp-dim{margin-bottom:16px;padding-bottom:16px;border-bottom:1px dashed var(--border-light)}
.rp-dim:last-child{border-bottom:none;margin-bottom:0;padding-bottom:0}
.rp-dim-header{display:flex;align-items:center;gap:8px;margin-bottom:6px}
.rp-dim-name{font-size:var(--fs-sm);font-weight:600;color:var(--text-primary)}
.rp-dim-desc{font-size:var(--fs-xs);color:var(--text-secondary);margin-bottom:8px;line-height:1.5}
.rp-levels{margin-bottom:6px}
.rp-comment{margin-top:6px}
</style>

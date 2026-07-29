<template>
  <div class="cd-kp-card" :class="{ 'cd-kp-card--hl': highlighted }" style="cursor:pointer" @click="handleClick">
    <div class="cd-kp-head">
      <div class="cd-kp-head-left">
        <span class="cd-kp-name">{{ kp.kpName }}</span>
        <el-tag size="small" effect="plain" type="info">{{ (kp.questionCount || 0) }}题</el-tag>
        <el-tooltip
          v-if="kp.mappingQuality === 'estimated' || kp.mappingQuality === 'mixed'"
          content="部分题目由算法从三级节点轮询分配，非精确标注"
          placement="top"
        >
          <span class="cd-kp-quality-dot" style="display:inline-block;width:8px;height:8px;border-radius:50%;background:var(--el-color-warning, #e6a23c)" />
        </el-tooltip>
        <el-tag v-if="highlighted" type="danger" size="small" effect="dark">差异 {{ kp._delta }}%</el-tag>
        <el-tag v-else size="small" effect="plain" type="info">差异 {{ kp._delta }}%</el-tag>
        <el-tag v-if="highlighted && weakClassCount" size="small" type="warning" effect="plain">{{ weakClassCount }}个班级需加强</el-tag>
      </div>
      <el-button v-if="highlighted" size="small" type="primary" @click.stop="$emit('generate-material', kp)">
        <el-icon><MagicStick /></el-icon> 生成巩固材料
      </el-button>
    </div>
    <div class="cd-kp-ranks">
      <div v-for="(c, ci) in kp._sorted" :key="c.classId" class="cd-kp-rank-item" :class="{ 'cd-kp-rank-item--weak': c._gap > 20 }" @click.stop="openClassDetail(c)">
        <div class="cd-kp-rank-left">
          <span class="cd-kp-rank-pos">{{ ci + 1 }}</span>
          <span class="cd-kp-rank-cls">{{ getClassName(c.classId) }}</span>
          <el-tag v-if="ci === 0" size="small" type="success" effect="plain">最佳</el-tag>
          <el-tag v-if="ci === kp._sorted.length - 1 && ci > 0" size="small" type="danger" effect="plain">需加强</el-tag>
        </div>
        <div class="cd-kp-rank-right">
          <span v-if="ci > 0" class="cd-kp-rank-delta" :style="{color:c._gap <= 10 ? 'var(--el-color-success, #67c23a)' : c._gap <= 20 ? 'var(--el-color-warning, #e6a23c)' : 'var(--el-color-danger, #f56c6c)'}">−{{ c._gap }}%</span>
          <div class="cd-kp-rank-track"><div class="cd-kp-rank-fill" :style="{width:Math.max(c.correctRate,2)+'%',background:rankBarColor(c.correctRate)}" /></div>
          <span class="cd-kp-rank-pct" :style="{color:rankBarColor(c.correctRate),fontWeight:700}">{{ c.correctRate }}%</span>
        </div>
      </div>
    </div>

    <el-dialog v-model="detailVisible" :title="'班级详情 — ' + (detailClass?.className || '')" width="400px" append-to-body @click.stop>
      <div v-if="detailClass">
        <div style="margin-bottom:12px">
          <span style="font-weight:600">知识点：</span>{{ kp.kpName }}
        </div>
        <div style="margin-bottom:8px">
          <span style="font-weight:600">正确率：</span>
          <span :style="{color:rankBarColor(detailClass.correctRate),fontWeight:700}">{{ detailClass.correctRate }}%</span>
        </div>
        <div style="margin-bottom:8px">
          <span style="font-weight:600">与最佳班级差距：</span>
          <span :style="{color:detailClass._gap <= 10 ? 'var(--el-color-success, #67c23a)' : detailClass._gap <= 20 ? 'var(--el-color-warning, #e6a23c)' : 'var(--el-color-danger, #f56c6c)',fontWeight:700}">{{ detailClass._gap }}%</span>
        </div>
        <el-divider />
        <div style="font-size:var(--fs-sm);color:var(--text-secondary)">
          点击下方按钮查看该知识点对应题目，了解具体薄弱点。
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="detailVisible = false; $emit('drill', kp.kpId)">查看题目</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { MagicStick } from '@element-plus/icons-vue'

const props = defineProps({
  kp: { type: Object, default: () => ({}) },
  highlighted: { type: Boolean, default: false }
})
const emit = defineEmits(['drill', 'generate-material'])

const detailVisible = ref(false)
const detailClass = ref(null)

const weakClassCount = computed(() => {
  return (props.kp._sorted || []).filter(c => c._gap > 20).length
})

function handleClick() {
  if (props.highlighted && props.kp._sorted?.length > 1) {
    // 高亮卡片：让用户点击具体班级行查看详情，或点击空白处跳转到题目
    emit('drill', props.kp.kpId)
  } else {
    emit('drill', props.kp.kpId)
  }
}

function openClassDetail(c) {
  detailClass.value = c
  detailVisible.value = true
}

function getClassName(cid) {
  const cls = props.kp._sorted || []
  return cls.find(c => c.classId === cid)?.className || ('班级' + cid)
}

function rankBarColor(r) {
  return r >= 80 ? 'var(--el-color-success, #67c23a)' : r >= 60 ? 'var(--el-color-primary, #409eff)' : r >= 40 ? 'var(--el-color-warning, #e6a23c)' : 'var(--el-color-danger, #f56c6c)'
}
</script>

<style scoped>
@import '@/styles/knowledge-point-card.css';
.cd-kp-rank-item--weak { background: var(--el-color-danger-light-9, #fef0f0); border-radius: 4px; cursor: pointer; }
.cd-kp-rank-item--weak:hover { background: var(--el-color-danger-light-8, #fde2e2); }
.cd-kp-rank-item { cursor: pointer; transition: background var(--transition-base); border-radius: 4px; padding: 2px 0; }
.cd-kp-rank-item:hover { background: var(--bg-hover-light, #f5f7fa); }
</style>

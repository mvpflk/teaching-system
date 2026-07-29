<template>
  <el-dialog
    :model-value="modelValue"
    title="AI 教学助手 · 快速入门"
    width="540px"
    :close-on-click-modal="false"
    :show-close="false"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div class="aia-guide">
      <div class="aia-guide-steps">
        <span :class="['aia-guide-dot', { active: guideStep >= 0 }]" />
        <span :class="['aia-guide-line', { active: guideStep >= 1 }]" />
        <span :class="['aia-guide-dot', { active: guideStep >= 1 }]" />
        <span :class="['aia-guide-line', { active: guideStep >= 2 }]" />
        <span :class="['aia-guide-dot', { active: guideStep >= 2 }]" />
      </div>
      <template v-if="guideStep === 0">
        <div class="aia-guide-icon"><el-icon :size="36"><MagicStick /></el-icon></div>
        <h4>第一步：生成教学方案</h4>
        <p>① 在"选择知识点"中依次选择<strong>学科 → 章节 → 任务 → 知识点</strong></p>
        <p>② 在"选择生成类型"中挑选所需类型（教学设计/知识清单/实训方案等）</p>
        <p>③ 如果是教学设计，可调整<strong>风格</strong>（精简/标准/详细）和<strong>侧重</strong>（应知/应会）</p>
        <p>④ 点击"开始生成"，等待 AI 完成后即可查看结果</p>
      </template>
      <template v-else-if="guideStep === 1">
        <div class="aia-guide-icon"><el-icon :size="36"><Clock /></el-icon></div>
        <h4>第二步：查看历史记录</h4>
        <p>① 页面下方<strong>"历史记录"</strong>区域展示所有已生成的 AI 产出</p>
        <p>② 可按类型筛选（教学设计/知识清单/实训方案）</p>
        <p>③ 输入<strong>关键词</strong>回车搜索，快速定位需要的产出</p>
        <p>④ 点击<strong>"查看"</strong>可重新加载，<strong>"发布"</strong>后可供学生使用</p>
      </template>
      <template v-else>
        <div class="aia-guide-icon"><el-icon :size="36"><StarFilled /></el-icon></div>
        <h4>第三步：评分与反馈</h4>
        <p>① 生成结果顶部有<strong>星星评分</strong>（1-5星），点击即可评价</p>
        <p>② 评分后还可点击<strong>"反馈"</strong>按钮，输入文字建议</p>
        <p>③ 您的评分和反馈将帮助我们<strong>持续优化</strong> AI 生成质量</p>
        <p>④ 不满意的产出可点击"重新生成"或"归档"丢弃</p>
      </template>
    </div>
    <template #footer>
      <el-button v-if="guideStep > 0" @click="guideStep--">上一步</el-button>
      <el-button v-if="guideStep < 2" type="primary" @click="guideStep++">下一步</el-button>
      <el-button v-else type="primary" @click="close">开始使用</el-button>
      <el-button text @click="close">跳过教程</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { MagicStick, Clock, StarFilled } from '@element-plus/icons-vue'

const GUIDE_KEY = 'aia_guide_viewed'

const props = defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue'])

const guideStep = ref(0)

const close = () => {
  emit('update:modelValue', false)
  guideStep.value = 0
  localStorage.setItem(GUIDE_KEY, '1')
}

watch(() => props.modelValue, (v) => {
  if (v) guideStep.value = 0
})
</script>

<style scoped>
.aia-guide { text-align: center; padding: 8px 0; }
.aia-guide-steps { display: flex; align-items: center; justify-content: center; margin-bottom: 20px; }
.aia-guide-dot { width: 12px; height: 12px; border-radius: 50%; background: var(--text-disabled); transition: background 0.3s; }
.aia-guide-dot.active { background: var(--primary-color); }
.aia-guide-line { width: 48px; height: 2px; background: var(--text-disabled); transition: background 0.3s; }
.aia-guide-line.active { background: var(--primary-color); }
.aia-guide-icon { margin-bottom: 8px; color: var(--primary-color); }
.aia-guide h4 { margin: 0 0 12px; font-size: var(--fs-lg); color: var(--text-primary); }
.aia-guide p { font-size: var(--fs-sm); color: #666; line-height: 1.8; margin: 4px 0; text-align: left; }
.aia-guide strong { color: var(--primary-color); }
</style>

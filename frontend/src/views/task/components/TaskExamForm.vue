<!-- 考试行为专属表单段：考试配置 + 题目管理（题库/自由/主观） -->
<template>
  <div class="form-section">
    <div class="form-section__header"><el-icon><Setting /></el-icon> 考试配置</div>
    <div class="form-section__body">
      <el-row :gutter="12">
        <el-col :xs="12" :sm="8">
          <el-form-item label="满分">
            <el-input-number
              v-model="form.totalScore"
              :min="1"
              :max="300"
              class="u-w-full"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="12" :sm="8">
          <el-form-item label="时长(分钟)">
            <el-input-number
              v-model="config.durationMinutes"
              :min="10"
              :max="300"
              class="u-w-full"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="12" :sm="8">
          <el-form-item label=" ">
            <!-- 空占位 -->
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="选项" class="switches-row">
        <el-switch v-model="config.shuffleQuestions" active-text="随机题目" size="small" />
        <el-switch v-model="config.shuffleOptions" active-text="随机选项" size="small" />
        <el-switch v-model="config.allowRetake" active-text="可重考" size="small" />
        <el-switch
          v-model="form.isRequired"
          :active-value="1"
          :inactive-value="0"
          active-text="必做"
          size="small"
        />
        <el-switch
          v-model="form.autoWrongbook"
          :active-value="1"
          :inactive-value="0"
          active-text="错题收录"
          size="small"
        />
        <el-switch
          v-model="form.notifyParents"
          :active-value="1"
          :inactive-value="0"
          active-text="通知家长"
          size="small"
        />
      </el-form-item>
      <el-form-item label="防作弊设置" class="switches-row">
        <el-switch v-model="config.fullscreenLock" active-text="全屏锁定" size="small" />
        <el-switch v-model="config.disableContextMenu" active-text="禁用右键" size="small" />
        <el-switch v-model="config.disableCopyPaste" active-text="禁止复制粘贴" size="small" />
        <span class="cheat-warn-label">切屏上限</span>
        <el-input-number
          v-model="config.maxWarnings"
          :min="0"
          :max="10"
          size="small"
          style="width:80px"
        />
      </el-form-item>
      <!-- 达标设置（2026-07-03 新增） -->
      <el-divider content-position="left">达标设置</el-divider>
      <el-row :gutter="12">
        <el-col :span="24">
          <el-form-item label="开启达标模式">
            <el-switch
              :model-value="config.passRate > 0"
              @update:model-value="v => { config.passRate = v ? 60 : 0 }"
            />
            <span style="font-size:var(--fs-xs);color:var(--text-secondary);margin-left:8px">
              开启后学生未达到得分率将自动安排重测
            </span>
          </el-form-item>
        </el-col>
      </el-row>
      <template v-if="config.passRate > 0">
        <el-row :gutter="12">
          <el-col :xs="12" :sm="8">
            <el-form-item label="达标得分率">
              <el-slider v-model="config.passRate" :min="50" :max="100" :step="5" show-input style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :xs="12" :sm="8">
            <el-form-item label="最大重测次数（含首次）">
              <el-select v-model="config.maxAttempts" style="width:100%">
                <el-option :value="2" label="1次重测" />
                <el-option :value="3" label="2次重测" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="12" :sm="8">
            <el-form-item label="重测截止（小时）">
              <el-input-number v-model="config.retakeDeadlineHours" :min="1" :max="720" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="24">
            <el-form-item label="重测策略">
              <el-radio-group v-model="config.passMode">
                <el-radio value="objective">仅客观题判定（推荐）</el-radio>
                <el-radio value="all">客观+主观全判定</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-alert type="info" :closable="false" show-icon style="margin-bottom:12px">
          <template #title>
            学生得分低于 {{ config.passRate }}% 将自动安排重测，首次成绩计入统计
          </template>
        </el-alert>
      </template>
    </div>
  </div>
  <div class="form-section">
    <div class="form-section__header">
      <el-icon><List /></el-icon> 题目管理
      <el-tag size="small" style="margin-left:8px">{{ modelValue.length }} 题</el-tag>
    </div>
    <div class="form-section__body">
      <div class="global-score-bar">
        <span class="gsb-label">每题默认分值</span>
        <span class="gsb-pill">单选<el-input-number
          v-model="form.scorePresets.single"
          :min="1"
          :max="20"
          size="small"
          controls-position="right"
        /></span>
        <span class="gsb-pill">多选<el-input-number
          v-model="form.scorePresets.multi"
          :min="1"
          :max="20"
          size="small"
          controls-position="right"
        /></span>
        <span class="gsb-pill">判断<el-input-number
          v-model="form.scorePresets.judge"
          :min="1"
          :max="20"
          size="small"
          controls-position="right"
        /></span>
        <span class="gsb-pill">填空<el-input-number
          v-model="form.scorePresets.fill"
          :min="1"
          :max="20"
          size="small"
          controls-position="right"
        /></span>
        <span class="gsb-pill gsb-pill--other">其他<el-input-number
          v-model="form.scorePresets.other"
          :min="1"
          :max="20"
          size="small"
          controls-position="right"
        /></span>
      </div>
      <el-tabs v-model="tabModel" class="exam-source-tabs">
        <el-tab-pane name="picker">
          <template #label><el-icon><List /></el-icon> 题库选题</template>
          <TaskQuestionPicker v-model="selectedQuestionIds" />
        </el-tab-pane>
        <el-tab-pane name="compose">
          <template #label><el-icon><Grid /></el-icon> 自由组题</template>
          <FreeQuestionComposer v-model="selectedQuestionIds" />
        </el-tab-pane>
        <el-tab-pane name="subjective">
          <template #label><el-icon><EditPen /></el-icon> 主观题</template>
          <SubjectiveQuestions v-model:questions="subjectiveQuestionsModel" />
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import TaskQuestionPicker from '@/components/common/TaskQuestionPicker.vue'
import FreeQuestionComposer from './FreeQuestionComposer.vue'
import SubjectiveQuestions from './SubjectiveQuestions.vue'

const props = defineProps({
  form: { type: Object, required: true },
  config: { type: Object, required: true },
  tab: { type: String, default: 'picker' },
  subjectiveQuestions: { type: Array, default: () => [] },
  modelValue: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:modelValue', 'update:tab', 'update:subjectiveQuestions'])

const tabModel = computed({
  get: () => props.tab,
  set: (v) => emit('update:tab', v)
})

const selectedQuestionIds = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const subjectiveQuestionsModel = computed({
  get: () => props.subjectiveQuestions,
  set: (v) => emit('update:subjectiveQuestions', v)
})
</script>

<style scoped>
.cheat-warn-label { font-size: var(--fs-xs); color: var(--text-secondary); margin-right: 4px; }
</style>

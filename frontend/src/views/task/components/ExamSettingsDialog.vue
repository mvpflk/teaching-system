<template>
  <el-dialog
    v-model="visible"
    title="考试设置"
    width="520px"
    append-to-body
    destroy-on-close
  >
    <div class="esd-body">
      <el-form label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="考试时长（分钟）">
              <el-input-number
                v-model="form.durationMinutes"
                :min="10"
                :max="300"
                style="width:100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="通过线（分）">
              <el-input-number
                v-model="form.passingScore"
                :min="0"
                :max="300"
                :precision="1"
                style="width:100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="防作弊设置">
          <div class="esd-switches">
            <el-switch v-model="form.fullscreenLock" active-text="全屏锁定" />
            <el-switch v-model="form.disableContextMenu" active-text="禁用右键菜单" />
            <el-switch v-model="form.disableCopyPaste" active-text="禁止复制粘贴" />
          </div>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="切屏警告次数（0=不限制）">
              <el-input-number
                v-model="form.maxWarnings"
                :min="0"
                :max="10"
                style="width:100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider />
        <el-form-item label="难度等级">
          <el-select
            v-model="form.difficultyLevel"
            placeholder="不限制"
            clearable
            style="width:100%"
          >
            <el-option label="简单" value="EASY" />
            <el-option label="中等" value="MEDIUM" />
            <el-option label="难" value="HARD" />
          </el-select>
        </el-form-item>
        <el-form-item label="题目选项">
          <div class="esd-switches">
            <el-switch v-model="form.shuffleQuestions" active-text="随机题目顺序" />
            <el-switch v-model="form.shuffleOptions" active-text="随机选项顺序" />
            <el-switch v-model="form.allowRetake" active-text="允许重考" />
          </div>
        </el-form-item>
        <el-form-item label="任务选项">
          <div class="esd-switches">
            <el-switch
              v-model="form.isRequired"
              :active-value="1"
              :inactive-value="0"
              active-text="必做"
              title="学生必须完成此任务"
            />
            <el-switch
              v-model="form.allowResubmit"
              :active-value="1"
              :inactive-value="0"
              active-text="可重交"
              title="批改后学生可再次提交"
            />
            <el-checkbox
              v-model="form.autoWrongbook"
              :true-value="1"
              :false-value="0"
              title="答错的题目自动加入错题本"
            >
              自动收录错题本
            </el-checkbox>
          </div>
        </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="apply">应用设置</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, watch, computed } from 'vue'

const props = defineProps({
  modelValue: Boolean,
  config: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:modelValue', 'apply'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const form = reactive({
  durationMinutes: 120,
  passingScore: 60,
  fullscreenLock: false,
  disableContextMenu: false,
  disableCopyPaste: false,
  maxWarnings: 3,
  shuffleQuestions: false,
  shuffleOptions: false,
  allowRetake: false,
  difficultyLevel: '',
  isRequired: 1,
  allowResubmit: 0,
  autoWrongbook: 1
})

watch(() => props.config, (cfg) => {
  if (cfg && Object.keys(cfg).length) {
    Object.keys(form).forEach(k => {
      if (cfg[k] !== undefined) form[k] = cfg[k]
    })
  }
}, { immediate: true })

const apply = () => {
  emit('apply', { ...form })
  visible.value = false
}
</script>

<style scoped>
.esd-body { padding: 4px 0; }
.esd-switches { display: flex; flex-wrap: wrap; gap: 16px; }
.esd-switches .el-switch { margin-right: 0; }

@media (max-width: 768px) {
  :deep(.el-form-item .el-input),
  :deep(.el-form-item .el-select) {
    width: 100%;
  }
}
</style>

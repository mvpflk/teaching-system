<template>
  <div class="sim-editor">
    <div class="sim-editor__header">
      <el-button text @click="$router.back()"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
      <h3>{{ isEdit ? '编辑仿真任务' : '创建仿真任务' }}</h3>
    </div>

    <!-- 基本设置 -->
    <div class="sim-section">
      <div class="sim-section__title"><el-icon><Setting /></el-icon> 基本设置</div>
      <el-form label-position="top" class="sim-form">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="任务标题" required>
              <el-input v-model="form.title" placeholder="如：在桌面新建文件夹" />
            </el-form-item>
          </el-col>
          <el-col :xs="12" :sm="6">
            <el-form-item label="类别">
              <el-select v-model="form.category" style="width:100%" @change="onCategoryChange">
                <el-option
                  v-for="c in categories"
                  :key="c.value"
                  :label="c.label"
                  :value="c.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="12" :sm="6">
            <el-form-item label="难度">
              <el-select v-model="form.difficulty" style="width:100%">
                <el-option
                  v-for="i in 5"
                  :key="i"
                  :label="'⭐'.repeat(i)"
                  :value="i"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :xs="24" :sm="8">
            <el-form-item label="学科">
              <el-input v-model="form.subject" />
            </el-form-item>
          </el-col>
          <el-col :xs="12" :sm="8">
            <el-form-item label="时限(秒)">
              <el-input-number
                v-model="form.timeLimit"
                :min="30"
                :max="3600"
                :step="30"
                style="width:100%"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="12" :sm="8">
            <el-form-item label="截止日期">
              <el-date-picker
                v-model="form.deadline"
                type="datetime"
                placeholder="选填，不填则长期有效"
                value-format="YYYY-MM-DDTHH:mm:ss"
                style="width:100%"
                clearable
              />
            </el-form-item>
          </el-col>
          <el-col :xs="12" :sm="8">
            <el-form-item label="模式">
              <el-radio-group v-model="form.mode">
                <el-radio v-for="m in modes" :key="m.value" :value="m.value">{{ m.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="任务描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            placeholder="告诉学生要做什么"
          />
        </el-form-item>
        <el-form-item label="关联知识点">
          <el-input v-model="form.nodeId" placeholder="knowledge_nodes.id，如 987（文件管理）" />
          <span class="sim-hint">对应考纲 knowledge_nodes 的 ID</span>
        </el-form-item>
      </el-form>
    </div>

    <!-- Network 快速生成器 -->
    <div v-if="form.category === 'network'" class="sim-section">
      <div class="sim-section__title">
        <el-icon><Monitor /></el-icon> 网络命令快速生成
        <el-badge :value="selectedCommands.length" :hidden="!selectedCommands.length" style="margin-left:8px" />
      </div>
      <el-collapse v-model="expandedCmdGroups" class="cmd-collapse">
        <el-collapse-item v-for="group in networkCommandGroups" :key="group.label" :name="group.label">
          <template #title>
            <span class="cmd-group-title">{{ group.label }}</span>
            <el-tag size="small" type="info" style="margin-left:8px">
              {{ group.commands.filter(c => selectedCommands.includes(c.name)).length }}/{{ group.commands.length }}
            </el-tag>
          </template>
          <el-checkbox-group v-model="selectedCommands" class="cmd-checkbox-group">
            <el-checkbox
              v-for="cmd in group.commands"
              :key="cmd.name"
              :value="cmd.name"
              class="cmd-item"
            >
              <span class="cmd-name">{{ cmd.name }}</span>
              <span class="cmd-desc">{{ cmd.desc }}</span>
            </el-checkbox>
          </el-checkbox-group>
        </el-collapse-item>
      </el-collapse>
      <div class="cmd-actions">
        <el-button size="small" @click="selectedCommands = []">清空选择</el-button>
        <el-button
          type="primary"
          size="small"
          :disabled="!selectedCommands.length"
          @click="quickGenerate"
        >
          从勾选的命令生成步骤
        </el-button>
      </div>
    </div>

    <!-- 操作步骤 -->
    <div class="sim-section">
      <div class="sim-section__title">
        <el-icon><List /></el-icon> 操作步骤
        <el-tag size="small" style="margin-left:8px">{{ form.steps.length }} 步</el-tag>
        <el-button
          size="small"
          type="primary"
          class="sim-section__action"
          @click="addStep"
        >
          + 添加步骤
        </el-button>
      </div>

      <TransitionGroup name="step-list" tag="div" class="step-list">
        <div v-for="(step, i) in form.steps" :key="step._key" class="step-card">
          <div class="step-card__head">
            <span class="step-card__idx">{{ i + 1 }}</span>
            <el-input
              v-model="step.name"
              placeholder="步骤名称（如：打开 CMD）"
              size="small"
              class="step-card__name"
            />
            <div class="step-card__actions">
              <el-button
                :disabled="i === 0"
                text
                size="small"
                title="上移"
                @click="moveStep(i, -1)"
              >
                <el-icon><Top /></el-icon>
              </el-button>
              <el-button
                :disabled="i === form.steps.length - 1"
                text
                size="small"
                title="下移"
                @click="moveStep(i, 1)"
              >
                <el-icon><Bottom /></el-icon>
              </el-button>
              <el-button
                text
                type="danger"
                size="small"
                title="删除"
                @click="removeStep(i)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>

          <div class="step-card__body">
            <el-row :gutter="12">
              <el-col :xs="24" :sm="12">
                <el-input v-model="step.hint" placeholder="提示文本（学生可看到）" size="small" />
              </el-col>
              <el-col :xs="12" :sm="6">
                <el-select
                  v-model="step.validateType"
                  size="small"
                  placeholder="验证类型"
                  @change="onValidateTypeChange(step)"
                >
                  <el-option
                    v-for="vt in validateTypes"
                    :key="vt.value"
                    :label="vt.label"
                    :value="vt.value"
                    :disabled="vt.win7Only && form.category !== 'win7'"
                  />
                </el-select>
              </el-col>
              <el-col :xs="12" :sm="6">
                <template v-if="step.validateType === 'event'">
                  <el-select
                    v-model="step.validateEvent"
                    size="small"
                    placeholder="事件类型"
                    filterable
                    allow-create
                  >
                    <el-option
                      v-for="evt in availableEvents"
                      :key="evt.value"
                      :label="evt.label"
                      :value="evt.value"
                    />
                  </el-select>
                </template>
                <template v-else-if="step.validateType === 'vfs'">
                  <el-input v-model="step.validatePath" size="small" placeholder="VFS 路径">
                    <template #prepend>路径</template>
                  </el-input>
                </template>
                <template v-else-if="step.validateType === 'window'">
                  <el-select v-model="step.validateEvent" size="small" placeholder="操作">
                    <el-option label="启动应用" value="launch" />
                    <el-option label="关闭应用" value="windowClose" />
                  </el-select>
                </template>
              </el-col>
            </el-row>
            <el-row v-if="step.validateType === 'event' || step.validateType === 'window'" :gutter="12" style="margin-top:8px">
              <el-col :span="24">
                <el-select
                  v-model="step.validateTarget"
                  size="small"
                  placeholder="目标（可搜索或手动输入）"
                  filterable
                  allow-create
                >
                  <el-option
                    v-for="tgt in availableTargets"
                    :key="tgt.value"
                    :label="tgt.label"
                    :value="tgt.value"
                  />
                </el-select>
              </el-col>
            </el-row>
          </div>
        </div>
      </TransitionGroup>

      <el-empty v-if="!form.steps.length" description="暂无步骤，点击「+ 添加步骤」开始" :image-size="60" />
    </div>

    <!-- 底部操作 -->
    <div class="sim-actions">
      <el-button size="large" :loading="saving" @click="handleSave(false)">保存草稿</el-button>
      <el-button
        type="primary"
        size="large"
        :loading="saving"
        @click="handleSave(true)"
      >
        保存并发布
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Setting, List, Top, Bottom, Delete, Monitor } from '@element-plus/icons-vue'
import { createSimTask, getSimTaskDefinition, updateSimTask } from '@/api/simulation'
import {
  SIM_CATEGORIES, SIM_MODES, WIN7_EVENTS, NETWORK_EVENTS,
  WIN7_TARGETS, NETWORK_TARGETS, NETWORK_COMMAND_GROUPS, VALIDATE_TYPES
} from '@/constants/simTasks'

const route = useRoute()
const router = useRouter()
const saving = ref(false)
const isEdit = computed(() => !!route.params.id)

const categories = SIM_CATEGORIES
const modes = SIM_MODES
const validateTypes = VALIDATE_TYPES

let stepKeySeq = 0
const form = reactive({
  title: '', description: '', subject: '信息技术应用基础',
  category: 'win7', nodeId: null, difficulty: 1, timeLimit: 120, mode: 'practice',
  deadline: '',
  steps: []
})

// ── 验证选项 ──
const availableEvents = computed(() => form.category === 'network' ? NETWORK_EVENTS : WIN7_EVENTS)
const availableTargets = computed(() => form.category === 'network' ? NETWORK_TARGETS : WIN7_TARGETS)

// ── Network 快速生成 ──
const selectedCommands = ref([])
const expandedCmdGroups = ref([])
const networkCommandGroups = NETWORK_COMMAND_GROUPS

function quickGenerate() {
  if (!selectedCommands.value.length) { ElMessage.warning('请至少勾选一个命令'); return }
  form.steps = []
  for (const cmdName of selectedCommands.value) {
    let cmd = null
    for (const group of networkCommandGroups) {
      cmd = group.commands.find(c => c.name === cmdName)
      if (cmd) break
    }
    if (!cmd) continue
    form.steps.push(
      { _key: ++stepKeySeq, name: '启动 CMD', hint: '开始菜单 → 所有程序 → 命令提示符', validateType: 'event', validateEvent: 'launch', validateTarget: 'cmd', validatePath: '' },
      { _key: ++stepKeySeq, name: cmdName, hint: `输入 ${cmd.cmd} 后按回车`, validateType: 'event', validateEvent: 'cmdExecute', validateTarget: cmd.cmd.split(' ')[0], validatePath: '' }
    )
  }
  ElMessage.success(`已生成 ${form.steps.length} 个步骤（${selectedCommands.value.length} 个命令）`)
}

// ── 类别切换 ──
function onCategoryChange(cat) {
  const found = categories.find(c => c.value === cat)
  if (found) form.subject = found.subject
  if (cat === 'network' && form.steps.length === 0) {
    form.steps.push({ _key: ++stepKeySeq, name: '启动 CMD', hint: '开始菜单 → 命令提示符', validateType: 'event', validateEvent: 'launch', validateTarget: 'cmd', validatePath: '' })
  }
}

// ── 步骤管理 ──
function addStep() {
  const isNetwork = form.category === 'network'
  form.steps.push({
    _key: ++stepKeySeq, name: '', hint: '',
    validateType: 'event', validateEvent: isNetwork ? 'cmdExecute' : 'click',
    validateTarget: '', validatePath: ''
  })
}

function removeStep(idx) { form.steps.splice(idx, 1) }

function moveStep(idx, dir) {
  const target = idx + dir
  if (target < 0 || target >= form.steps.length) return
  const tmp = form.steps[idx]
  form.steps[idx] = form.steps[target]
  form.steps[target] = tmp
}

function onValidateTypeChange(step) {
  if (step.validateType === 'vfs') { step.validateEvent = ''; step.validateTarget = '' }
}

// ── 保存 ──
async function handleSave(publish) {
  if (!form.title) { ElMessage.warning('请填写任务标题'); return }
  if (!form.steps.length) { ElMessage.warning('请至少添加一个步骤'); return }

  const taskJson = {
    title: form.title, description: form.description,
    difficulty: form.difficulty, timeLimit: form.timeLimit,
    steps: form.steps.map(s => ({
      name: s.name, hint: s.hint,
      validate: s.validateType === 'vfs'
        ? { vfs: { path: s.validatePath || '', type: 'folder' } }
        : { event: s.validateEvent || 'click', target: s.validateTarget || undefined }
    }))
  }

  const payload = {
    title: form.title, subject: form.subject, category: form.category,
    nodeId: form.nodeId ? Number(form.nodeId) : null,
    taskJson, mode: form.mode, difficulty: form.difficulty, timeLimit: form.timeLimit,
    deadline: form.deadline || null, publish
  }

  saving.value = true
  try {
    const res = isEdit.value
      ? await updateSimTask(route.params.id, payload)
      : await createSimTask(payload)
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '已更新' : '创建成功')
      router.push('/teacher/simulation/tasks')
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.message || e))
  } finally { saving.value = false }
}

// ── 编辑模式加载 ──
onMounted(async () => {
  if (!isEdit.value) return
  try {
    const res = await getSimTaskDefinition(route.params.id)
    if (res.code === 200) {
      const data = res.data
      const json = data.taskJson || {}
      form.title = json.title || ''
      form.description = json.description || ''
      form.difficulty = json.difficulty || 1
      form.timeLimit = json.timeLimit || 120
      form.subject = data.subject || ''
      form.category = data.category || 'win7'
      form.mode = data.mode || 'practice'
      form.nodeId = data.nodeId || null
      form.steps = (json.steps || []).map(s => {
        const v = s.validate || {}
        return {
          _key: ++stepKeySeq, name: s.name || '', hint: s.hint || '',
          validateType: v.vfs ? 'vfs' : 'event',
          validateEvent: v.event || '', validateTarget: v.target || '',
          validatePath: v.vfs?.path || ''
        }
      })
    }
  } catch (e) {
    ElMessage.error('加载任务失败: ' + (e.message || e))
  }
})
</script>

<style scoped>
.sim-editor { max-width: 900px; margin: 0 auto; padding: 24px 16px; }

.sim-editor__header { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; }
.sim-editor__header h3 { margin: 0; font-size: var(--fs-lg); color: var(--text-primary); }

.sim-section {
  background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-md);
  padding: 16px; margin-bottom: 16px;
}
.sim-section__title {
  display: flex; align-items: center; gap: 8px; font-size: var(--fs-md); font-weight: 600;
  color: var(--text-primary); padding-bottom: 12px; margin-bottom: 12px;
  border-bottom: 1px solid var(--border-light);
}
.sim-section__title .el-icon { color: var(--primary-color); }
.sim-section__action { margin-left: auto; }

.sim-form { --el-form-item-margin-bottom: 12px; }
.sim-hint { font-size: var(--fs-xs); color: var(--text-secondary); margin-left: 8px; }

/* 网络命令折叠 */
.cmd-collapse { margin-bottom: 12px; }
.cmd-group-title { font-size: var(--fs-sm); font-weight: 600; color: var(--text-primary); }
.cmd-checkbox-group { display: flex; flex-direction: column; gap: 4px; }
.cmd-item { --el-checkbox-margin-right: 0; }
.cmd-name { font-size: var(--fs-sm); color: var(--text-primary); margin-right: 8px; }
.cmd-desc { font-size: var(--fs-xs); color: var(--text-secondary); }
.cmd-actions { display: flex; justify-content: flex-end; gap: 8px; padding-top: 8px; border-top: 1px solid var(--border-light); }

/* 步骤卡片 */
.step-list { display: flex; flex-direction: column; gap: 10px; }
.step-card {
  background: var(--bg-hover); border: 1px solid var(--border-base); border-radius: var(--radius-md);
  padding: 12px; transition: border-color 0.2s, box-shadow 0.2s;
}
.step-card:hover { border-color: var(--primary-color); box-shadow: 0 0 0 1px var(--primary-color); }

.step-card__head { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.step-card__idx {
  width: 24px; height: 24px; border-radius: 50%; background: var(--primary-color); color: #fff;
  font-size: var(--fs-xs); font-weight: 700; display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.step-card__name { flex: 1; }
.step-card__actions { display: flex; gap: 2px; flex-shrink: 0; }
.step-card__body { padding-left: 32px; }

/* 步骤列表动画 */
.step-list-enter-active, .step-list-leave-active { transition: all 0.2s ease; }
.step-list-enter-from { opacity: 0; transform: translateY(-8px); }
.step-list-leave-to { opacity: 0; transform: translateX(-20px); }
.step-list-move { transition: transform 0.2s ease; }

/* 底部操作 */
.sim-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 20px; padding-top: 16px; border-top: 1px solid var(--border-light); }

@media (max-width: 768px) {
  .sim-editor { padding: 12px 8px; }
  .sim-section { padding: 12px; }
  .step-card__body { padding-left: 0; }
  .step-card__idx { width: 20px; height: 20px; font-size: var(--fs-xs); }
  .sim-actions { flex-direction: column; }
  .sim-actions .el-button { width: 100%; }
}
</style>

<template>
  <div class="comp-mgr">
    <div class="page-header">
      <h3>打字竞赛管理</h3>
      <el-button type="primary" @click="openCreate">创建竞赛</el-button>
    </div>

    <!-- 骨架屏 -->
    <div v-if="loading" class="sk-list">
      <div v-for="i in 4" :key="i" class="sk-row">
        <div class="sk-line w-10" style="height:14px"></div>
        <div class="sk-line w-25" style="height:14px"></div>
        <div class="sk-line w-20" style="height:14px"></div>
        <div class="sk-line w-20" style="height:14px"></div>
        <div class="sk-line w-20" style="height:14px"></div>
      </div>
    </div>

    <el-table
      v-else
      :data="records"
      stripe
      border
    >
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="title" label="标题" min-width="150" />
      <el-table-column prop="textTitle" label="文本" min-width="140" />
      <el-table-column label="状态" width="100">
        <template #default="{row}">
          <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="startTime" label="开始时间" width="160" />
      <el-table-column prop="endTime" label="结束时间" width="160" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{row}">
          <el-button
            v-if="row.status==='pending'"
            size="small"
            type="success"
            @click="handleStart(row)"
          >
            开始
          </el-button>
          <el-button
            v-if="row.status==='ongoing'"
            size="small"
            type="danger"
            @click="handleFinish(row)"
          >
            结束
          </el-button>
          <el-button
            v-if="row.status!=='ongoing'"
            size="small"
            type="danger"
            plain
            @click="handleDelete(row)"
          >
            删除
          </el-button>
          <el-button size="small" @click="router.push(`/teacher/typing/monitor?compId=${row.id}`)">驾驶舱</el-button>
          <el-button size="small" @click="handleExport(row)">导出</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 创建弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="创建打字竞赛"
      width="560px"
      @close="resetForm"
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="竞赛标题" required>
          <el-input v-model="form.title" placeholder="例如：期中打字大赛" />
        </el-form-item>
        <el-form-item label="文本来源">
          <el-radio-group v-model="textSource">
            <el-radio value="library">从文本库选择</el-radio>
            <el-radio value="custom">自定义输入</el-radio>
          </el-radio-group>
        </el-form-item>

        <template v-if="textSource === 'library'">
          <el-form-item label="选择文本" required>
            <el-select
              v-model="form.textId"
              placeholder="选择一篇打字文本"
              filterable
              style="width:100%"
              :loading="loadingTexts"
            >
              <el-option
                v-for="t in texts"
                :key="t.id"
                :label="`${t.title} (${t.content?.length||0}字)`"
                :value="t.id"
              />
            </el-select>
          </el-form-item>
        </template>

        <template v-else>
          <el-form-item label="文本标题" required>
            <el-input v-model="customTitle" placeholder="给这段文本起个名字" />
          </el-form-item>
          <el-form-item label="文本内容" required>
            <el-input
              v-model="customContent"
              type="textarea"
              :rows="6"
              placeholder="在此输入或粘贴打字原文..."
            />
          </el-form-item>
        </template>
        <el-form-item label="参赛班级">
          <el-select
            v-model="form.allowedClassIds"
            multiple
            placeholder="选择班级（留空则全部班级可参加）"
            style="width:100%"
            :loading="loadingClasses"
          >
            <el-option
              v-for="c in myClasses"
              :key="c.id"
              :label="`${c.className} (${c.grade||''})`"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="竞赛时长">
          <div style="display:flex;gap:8px;flex-wrap:wrap">
            <el-radio-group v-model="form.durationMinutes" size="small">
              <el-radio-button :value="15">15分钟</el-radio-button>
              <el-radio-button :value="20">20分钟</el-radio-button>
              <el-radio-button :value="25">25分钟</el-radio-button>
              <el-radio-button :value="30">30分钟</el-radio-button>
            </el-radio-group>
            <el-button
              size="small"
              text
              type="info"
              @click="form.durationMinutes = null"
            >
              不限时
            </el-button>
          </div>
          <div style="color:var(--typing-pending);font-size:var(--fs-xs);margin-top:4px">
            选择时长后，竞赛开始即自动倒计时，到时自动结束。可不选，由教师手动控制。
          </div>
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            placeholder="指定具体时间（优先级高于时长）"
            style="width:100%"
          />
          <div style="color:var(--typing-pending);font-size:var(--fs-xs);margin-top:2px">留空则按上方时长自动计算，两者均不设则由教师手动结束</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCompetitions, createCompetition, startCompetition, finishCompetition, deleteCompetition, exportResults, getTexts, createText } from '@/api/typing'
import { getMyClasses } from '@/api/classes'
import { exportCsv, TYPING_RESULT_COLUMNS } from '@/utils/export'

const router = useRouter()
const loading = ref(false)
const records = ref([])
const dialogVisible = ref(false)
const saving = ref(false)
const loadingTexts = ref(false)
const loadingClasses = ref(false)
const form = ref({ title: '', textId: null, allowedClassIds: [], endTime: null, durationMinutes: null })
const texts = ref([])
const myClasses = ref([])
const textSource = ref('library')
const customTitle = ref('')
const customContent = ref('')

function statusTag(s) { return { pending: '', ongoing: 'warning', finished: 'info' }[s] }
function statusLabel(s) { return { pending: '待开始', ongoing: '进行中', finished: '已结束' }[s] }

async function loadData() {
  loading.value = true
  try {
    const res = await getCompetitions({ page: 1, size: 50 })
    if (res.code === 200) records.value = res.data.records || []
  } catch { ElMessage.error('加载失败') }
  loading.value = false
}

async function openCreate() {
  dialogVisible.value = true
  loadingTexts.value = true
  loadingClasses.value = true
  // 分开请求，互不影响
  try {
    const tRes = await getTexts({ page: 1, size: 200 })
    if (tRes.code === 200) texts.value = tRes.data?.records || []
  } catch { /* getTexts 失败时 texts 保持空数组 */ }
  loadingTexts.value = false

  try {
    const cRes = await getMyClasses()
    if (cRes.code === 200) myClasses.value = (cRes.data || []).map(c => ({
      id: c.id, className: c.className, grade: c.grade
    }))
  } catch { /* getMyClasses 失败时 myClasses 保持空数组 */ }
  loadingClasses.value = false
}

function resetForm() { form.value = { title: '', textId: null, allowedClassIds: [], endTime: null, durationMinutes: null }; textSource.value = 'library'; customTitle.value = ''; customContent.value = '' }

async function handleCreate() {
  if (!form.value.title) { ElMessage.warning('请输入竞赛标题'); return }
  if (textSource.value === 'custom' && (!customTitle.value || !customContent.value)) {
    ElMessage.warning('请输入自定义文本的标题和内容'); return
  }
  if (textSource.value === 'library' && !form.value.textId) {
    ElMessage.warning('请从文本库选择一篇文本'); return
  }
  saving.value = true
  try {
    let textId = form.value.textId
    // 自定义文本：先保存到 typing_texts
    if (textSource.value === 'custom') {
      const tRes = await createText({
        title: customTitle.value,
        content: customContent.value,
        language: 'mixed',
        difficulty: 1,
        type: 'competition'
      })
      if (tRes.code === 200 && tRes.data) textId = tRes.data.id
      else { ElMessage.error('保存文本失败'); saving.value = false; return }
    }
    await createCompetition({
      title: form.value.title,
      textId: textId,
      allowedClassIds: JSON.stringify(form.value.allowedClassIds),
      endTime: form.value.endTime ? new Date(form.value.endTime).toISOString() : null,
      durationMinutes: form.value.durationMinutes || null
    })
    ElMessage.success('已创建')
    dialogVisible.value = false
    loadData()
  } catch { ElMessage.error('创建失败') }
  saving.value = false
}

async function handleStart(row) {
  await ElMessageBox.confirm(`确定开始「${row.title}」？开始后学生将可参与。`, '确认')
  try {
    await startCompetition(row.id)
    ElMessage.success('竞赛已开始')
    loadData()
  } catch {}
}

async function handleFinish(row) {
  await ElMessageBox.confirm(`确定结束「${row.title}」？`, '确认', { type: 'warning' })
  try {
    await finishCompetition(row.id)
    ElMessage.success('竞赛已结束')
    loadData()
  } catch {}
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除「${row.title}」？关联成绩将一并清除。`, '确认删除', { type: 'warning', confirmButtonText: '删除' })
  try {
    await deleteCompetition(row.id)
    ElMessage.success('已删除')
    loadData()
  } catch {}
}

async function handleExport(row) {
  try {
    const res = await exportResults(row.id)
    if (res.code === 200 && res.data) {
      exportCsv(res.data, `typing_results_${row.id}.csv`, TYPING_RESULT_COLUMNS)
      ElMessage.success('已导出')
    }
  } catch { ElMessage.error('导出失败') }
}

onMounted(loadData)
</script>

<style scoped>
.comp-mgr { max-width: 1200px; margin: 0 auto; padding: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { margin: 0; }

.sk-list { padding: 8px 0; }
.sk-row { display: flex; gap: 12px; margin-bottom: 8px; }
.sk-line { height: 14px; border-radius: 4px; background: linear-gradient(90deg, var(--bg-section, #f5f7fa) 25%, var(--bg-card, #fff) 50%, var(--bg-section, #f5f7fa) 75%); background-size: 200% 100%; animation: sk-shimmer 1.5s infinite; }
@keyframes sk-shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
.w-10 { width: 10% } .w-20 { width: 20% } .w-25 { width: 25% }

@media (max-width: 768px) { .comp-mgr { padding: 8px; } }
</style>

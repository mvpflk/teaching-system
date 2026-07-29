<template>
  <div class="page-card">
    <div class="page-header">
      <h3 class="page-title">共享试卷</h3>
      <div style="display:flex;gap:8px">
        <el-button size="small" type="primary" @click="showImport = true">输入分享码导入</el-button>
      </div>
    </div>

    <FilterBar :model="search" @search="filterList" @reset="resetFilter">
      <el-input
        v-model="search.keyword"
        placeholder="搜索试卷名称"
        clearable
        style="width:200px"
        @input="filterList"
      />
      <el-select
        v-model="search.subject"
        placeholder="学科"
        clearable
        style="width:140px"
        @change="filterList"
      >
        <el-option
          v-for="s in subjectOptions"
          :key="s.id"
          :value="s.subjectName"
          :label="s.subjectName"
        />
      </el-select>
    </FilterBar>

    <!-- 移动端卡片 -->
    <template v-if="isMobile">
      <div v-loading="loading" class="card-list">
        <el-empty v-if="!filtered.length" description="暂无试卷" />
        <MobileDataCard
          v-for="r in paginated"
          :key="r.id"
          :title="r.examTitle"
          :badge="{ text: r.expired ? '已过期' : '有效', type: r.expired ? 'danger' : 'success' }"
          :meta-items="[r.examSubject, r.grade || '-', r.questionCount + '题']"
          @click="handlePreview(r.shareCode)"
        >
          <template #footer>
            <el-tag
              size="small"
              type="success"
              style="cursor:pointer"
              @click.stop="copyCode(r.shareCode)"
            >
              {{ r.shareCode }}
            </el-tag>
            <span>{{ r.creatorName }}</span>
          </template>
          <template #actions>
            <el-button size="small" @click.stop="handlePreview(r.shareCode)">预览</el-button>
            <el-button
              size="small"
              type="primary"
              :loading="importing === r.shareCode"
              @click.stop="handleImport(r.shareCode)"
            >
              导入
            </el-button>
          </template>
        </MobileDataCard>
      </div>
    </template>

    <!-- 桌面端表格 -->
    <el-table
      v-else
      v-loading="loading"
      :data="paginated"
      stripe
      size="small"
    >
      <template #empty><el-empty description="暂无试卷" /></template>
      <el-table-column
        prop="examTitle"
        label="试卷名称"
        min-width="160"
        show-overflow-tooltip
      />
      <el-table-column
        prop="examSubject"
        label="学科"
        width="100"
        align="center"
      />
      <el-table-column
        prop="creatorName"
        label="分享者"
        width="100"
        align="center"
      />
      <el-table-column
        prop="grade"
        label="年级"
        width="70"
        align="center"
      >
        <template #default="{ row }">{{ row.grade || '-' }}</template>
      </el-table-column>
      <el-table-column
        prop="questionCount"
        label="题数"
        width="55"
        align="center"
      />
      <el-table-column label="分享码" width="110" align="center">
        <template #default="{ row }">
          <el-tag
            size="small"
            type="success"
            style="cursor:pointer"
            @click="copyCode(row.shareCode)"
          >
            {{ row.shareCode }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="70" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.expired" size="small" type="danger">已过期</el-tag>
          <el-tag v-else size="small" type="success">有效</el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="操作"
        width="140"
        align="center"
        fixed="right"
      >
        <template #default="{ row }">
          <el-button size="small" @click="handlePreview(row.shareCode)">预览</el-button>
          <el-button
            size="small"
            type="primary"
            link
            :loading="importing === row.shareCode"
            @click="handleImport(row.shareCode)"
          >
            导入
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="filtered.length > pageSize" class="pagination-wrap">
      <el-pagination
        v-model:current-page="page"
        layout="total,prev,next"
        :total="filtered.length"
        :page-size="pageSize"
      />
    </div>

    <!-- 班级选择弹窗 -->
    <el-dialog
      v-model="classDialogVisible"
      title="选择导入班级"
      width="360px"
      append-to-body
    >
      <el-select
        v-model="selectedClassId"
        placeholder="选择目标班级"
        filterable
        style="width:100%"
        :loading="classLoading"
      >
        <el-option
          v-for="c in classOptions"
          :key="c.id"
          :value="c.id"
          :label="(c.grade||'')+c.className"
        />
      </el-select>
      <template #footer>
        <el-button @click="classDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedClassId" @click="confirmImport">确认导入</el-button>
      </template>
    </el-dialog>

    <!-- 预览弹窗 -->
    <el-dialog
      v-model="previewVisible"
      title="试卷预览"
      width="600px"
      append-to-body
    >
      <div v-loading="previewLoading">
        <template v-if="previewData">
          <el-descriptions
            :column="2"
            border
            size="small"
            class="pv-desc"
          >
            <el-descriptions-item label="试卷名称">{{ previewData.title }}</el-descriptions-item>
            <el-descriptions-item label="学科">{{ previewData.subject || '-' }}</el-descriptions-item>
            <el-descriptions-item label="题目数" :span="2">{{ previewData.questionCount }} 题</el-descriptions-item>
          </el-descriptions>
          <div v-if="previewData.questions?.length" class="pv-questions">
            <div
              v-for="(q, i) in previewData.questions"
              :key="q.id"
              class="pv-q"
              :class="{ 'pv-q-expanded': expandedQs.has(i) }"
              @click="expandedQs.has(i) ? expandedQs.delete(i) : expandedQs.add(i)"
            >
              <div class="pv-q-num">
                <span>{{ i + 1 }}.</span>
                <el-tag size="small" type="info">{{ QUESTION_TYPE_LABEL[q.questionType] || q.questionType }}</el-tag>
                <el-tag v-if="q.difficultyLevel" size="small" :type="q.difficultyLevel>=4?'danger':q.difficultyLevel>=3?'warning':''">难度{{ q.difficultyLevel }}</el-tag>
                <span v-if="q.score" class="pv-q-score">{{ q.score }}分</span>
                <el-icon class="pv-q-arrow" :class="{ 'pv-q-arrow-open': expandedQs.has(i) }"><ArrowDown /></el-icon>
              </div>
              <div class="pv-q-text">{{ q.questionText }}</div>
              <!-- 展开详情 -->
              <div v-if="expandedQs.has(i)" class="pv-q-detail" @click.stop>
                <div v-if="q.options" class="pv-q-opts">
                  <div class="pv-q-opts-title">选项：</div>
                  <div
                    v-for="(opt, j) in (typeof q.options === 'string' ? parseOptsStr(q.options) : q.options)"
                    :key="j"
                    class="pv-q-opt"
                    :class="{ 'pv-q-opt-correct': q.correctAnswer && String.fromCharCode(65+j) === String(q.correctAnswer).toUpperCase() }"
                  >
                    {{ String.fromCharCode(65+j) }}. {{ typeof opt === 'string' ? opt.replace(/^[A-Z][.、．]\s*/, '') : opt }}
                  </div>
                </div>
                <div v-if="q.correctAnswer" class="pv-q-answer">答案：<b>{{ q.correctAnswer }}</b></div>
                <div v-if="q.explanation" class="pv-q-explain">解析：{{ q.explanation }}</div>
              </div>
            </div>
          </div>
        </template>
      </div>
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
        <el-button v-if="previewData" type="primary" @click="previewVisible = false; handleImport(previewCode)">导入此试卷</el-button>
      </template>
    </el-dialog>

    <ImportExamDialog v-model="showImport" @imported="refreshList" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { useIsMobile } from '@/composables/useIsMobile'
import MobileDataCard from '@/components/common/MobileDataCard.vue'
import { getLibrary, importShared, previewShare } from '@/api/examShare'
import { getMySubjects } from '@/api/settings'
import { getClassList } from '@/api/classes'
import { QUESTION_TYPE_LABEL } from '@/constants/questionTypes'
import FilterBar from '@/components/common/FilterBar.vue'
import ImportExamDialog from '@/components/common/ImportExamDialog.vue'

const router = useRouter()
const list = ref([])
const loading = ref(false)
const search = reactive({ keyword: '', subject: '' })
const showImport = ref(false)
const page = ref(1)
const pageSize = 10
const importing = ref(null)
const subjectOptions = ref([])
const { isMobile } = useIsMobile()
// 班级选择（行内导入）
const classDialogVisible = ref(false)
const pendingShareCode = ref('')
const selectedClassId = ref(null)
const classOptions = ref([])
const classLoading = ref(false)

const paginated = computed(() => {
  const start = (page.value - 1) * pageSize
  return filtered.value.slice(start, start + pageSize)
})
const loadSubjects = async () => {
  try { const r = await getMySubjects(); if (r.code === 200) subjectOptions.value = r.data || [] }
  catch { /* */ }
}
const filtered = computed(() => {
  let r = list.value
  if (search.keyword) { const q = search.keyword.toLowerCase(); r = r.filter(x => (x.examTitle || '').toLowerCase().includes(q)) }
  if (search.subject) r = r.filter(x => x.examSubject === search.subject)
  return r
})

const refreshList = async () => {
  loading.value = true
  try { const res = await getLibrary(); if (res.code === 200) list.value = res.data || [] }
  catch { ElMessage.error('加载共享试卷失败') } finally { loading.value = false }
}
const filterList = () => { page.value = 1 }
const resetFilter = () => { search.keyword = ''; search.subject = ''; page.value = 1 }

const copyCode = async (code) => {
  try { await navigator.clipboard.writeText(code); ElMessage.success('分享码已复制') }
  catch { ElMessage.error('复制失败') }
}

const handleImport = async (shareCode) => {
  if (importing.value) return
  pendingShareCode.value = shareCode
  // 懒加载班级列表
  if (!classOptions.value.length) {
    classLoading.value = true
    try {
      const r = await getClassList()
      if (r.code === 200) classOptions.value = (r.data?.records || r.data || []).map(c => ({ id: c.id, className: c.className, grade: c.grade }))
    } catch { /* */ }
    classLoading.value = false
  }
  selectedClassId.value = null
  classDialogVisible.value = true
}

const confirmImport = async () => {
  if (!selectedClassId.value) { ElMessage.warning('请选择目标班级'); return }
  importing.value = pendingShareCode.value
  classDialogVisible.value = false
  try {
    const res = await importShared(pendingShareCode.value, selectedClassId.value)
    if (res.code === 200) {
      refreshList()
      const taskId = res.data?.taskId
      try {
        await ElMessageBox.confirm(
          '导入成功！是否立即编辑此试卷？',
          '导入成功',
          { confirmButtonText: '去编辑', cancelButtonText: '稍后', type: 'success' }
        )
        if (taskId) router.push(`/teacher/tasks/${taskId}/edit`)
      } catch { /* 用户选择稍后 */ }
    } else ElMessage.error(res.message || '导入失败')
  } catch { ElMessage.error('导入失败') } finally { importing.value = null }
}

const previewVisible = ref(false)
const previewLoading = ref(false)
const previewData = ref(null)
const previewCode = ref('')
const expandedQs = ref(new Set())
const parseOptsStr = (v) => { try { return JSON.parse(v) } catch { return [] } }
const handlePreview = async (code) => {
  previewVisible.value = true; previewLoading.value = true; previewData.value = null; previewCode.value = code; expandedQs.value = new Set()
  try {
    const res = await previewShare(code)
    if (res.code === 200) previewData.value = res.data
    else ElMessage.error(res.message || '加载失败')
  } catch { ElMessage.error('预览失败') } finally { previewLoading.value = false }
}

	onMounted(() => { refreshList(); loadSubjects() })

</script>

<style scoped>
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
.card-list { display: flex; flex-direction: column; gap: 12px; }
.eb-card { padding: 16px; background: var(--bg-card); border-radius: var(--radius-md); border: 1px solid var(--border-light); }
.eb-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.eb-title { font-weight: 600; color: var(--text-primary); font-size: var(--fs-sm); }
.eb-meta { display: flex; align-items: center; gap: 8px; font-size: var(--fs-xs); color: var(--text-secondary); margin-bottom: 4px; }
.eb-author { color: var(--text-secondary); }
.eb-actions { margin-top: 8px; display: flex; gap: 6px; justify-content: flex-end; }
.pv-desc { margin-bottom: 16px; }
.pv-questions { max-height: 400px; overflow-y: auto; }
.pv-q { padding: 8px 0; border-bottom: 1px solid var(--border-light); cursor: pointer; transition: background var(--transition-base); }
.pv-q:hover { background: var(--bg-hover-light); }
.pv-q-expanded { background: var(--bg-section); }
.pv-q-num { font-weight: 500; font-size: var(--fs-sm); margin-bottom: 4px; display: flex; align-items: center; gap: 6px; }
.pv-q-text { font-size: var(--fs-sm); color: var(--text-regular); line-height: 1.6; }
.pv-q-arrow { margin-left: auto; transition: transform var(--transition-base); font-size: var(--fs-xs); color: var(--text-secondary); }
.pv-q-arrow-open { transform: rotate(180deg); }
.pv-q-score { font-size: var(--fs-xs); color: var(--primary-color); margin-left: 4px; }
.pv-q-detail { margin-top: 8px; padding: 10px 12px; background: var(--bg-card); border-radius: var(--radius-sm); border: 0.5px solid var(--border-light); }
.pv-q-opts { margin-bottom: 6px; }
.pv-q-opts-title { font-size: var(--fs-xs); color: var(--text-secondary); margin-bottom: 2px; }
.pv-q-opt { font-size: var(--fs-sm); color: var(--text-regular); padding: 2px 0; }
.pv-q-opt-correct { color: var(--success-color); font-weight: 600; }
.pv-q-answer { font-size: var(--fs-sm); color: var(--success-color); margin-top: 4px; }
.pv-q-explain { font-size: var(--fs-sm); color: var(--text-secondary); margin-top: 4px; }
@media (max-width: 768px) {
  :deep(.el-table) { font-size: var(--fs-xs); }
}
</style>

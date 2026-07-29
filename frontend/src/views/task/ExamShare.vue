<template>
  <div class="page-card">
    <div class="page-header">
      <h3 class="page-title">我的分享</h3>
      <div class="header-actions">
        <el-button size="small" @click="showImport = true">通过分享码导入</el-button>
        <el-button size="small" type="primary" @click="showShareDialog = true">分享试卷</el-button>
      </div>
    </div>

    <!-- 移动端卡片 -->
    <template v-if="isMobile">
      <div v-loading="loading" class="card-list">
        <el-empty v-if="!list.length" description="暂无分享" />
        <div v-for="r in paginated" :key="r.id" class="es-card">
          <div class="es-card-header">
            <span class="es-title">{{ r.examTitle }}</span>
            <el-tag :type="tagType(r)" size="small">{{ tagText(r) }}</el-tag>
          </div>
          <div class="es-meta">
            <span>{{ r.examSubject }}</span>
            <span>{{ r.questionCount }}题</span>
            <span>已用{{ r.useCount || 0 }}次</span>
          </div>
          <div class="es-meta">
            <el-tag
              size="small"
              type="success"
              style="cursor:pointer"
              @click="copyCode(r.shareCode)"
            >
              {{ r.shareCode }}
            </el-tag>
          </div>
          <div class="es-actions">
            <el-button size="small" type="danger" @click="handleDelete(r)">取消分享</el-button>
          </div>
        </div>
      </div>
    </template>
    <el-table
      v-else
      v-loading="loading"
      :data="paginated"
      stripe
      size="small"
    >
      <el-table-column
        prop="examTitle"
        label="试卷名称"
        min-width="180"
        show-overflow-tooltip
      />
      <el-table-column
        prop="examSubject"
        label="学科"
        width="120"
        align="center"
      />
      <el-table-column label="分享码" width="120" align="center">
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
      <el-table-column
        prop="questionCount"
        label="题数"
        width="60"
        align="center"
      />
      <el-table-column
        prop="useCount"
        label="已用"
        width="60"
        align="center"
      />
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.expired" size="small" type="danger">已过期</el-tag>
          <el-tag v-else-if="isExpiringSoon(row)" size="small" type="warning">即将过期</el-tag>
          <el-tag v-else size="small" type="success">有效</el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="操作"
        width="180"
        align="center"
        fixed="right"
      >
        <template #default="{ row }">
          <el-button size="small" @click="goTask(row)">查看任务</el-button>
          <el-button size="small" type="primary" @click="handlePreview(row.shareCode)">预览</el-button>
          <el-button
            size="small"
            type="danger"
            link
            @click="handleDelete(row)"
          >
            取消分享
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="list.length > pageSize" class="pagination-wrap">
      <el-pagination
        v-model:current-page="page"
        layout="total,prev,next"
        :total="list.length"
        :page-size="pageSize"
      />
    </div>

    <ShareCodeDialog v-model="showShareDialog" @shared="loadList" />
    <ImportExamDialog v-model="showImport" @imported="loadList" />

    <!-- 预览弹窗 -->
    <el-dialog
      v-model="previewVisible"
      title="试卷预览"
      width="700px"
      destroy-on-close
      append-to-body
    >
      <div v-loading="previewLoading">
        <el-empty v-if="!previewData && !previewLoading" description="加载失败" />
        <template v-else-if="previewData">
          <div class="preview-header">
            <span><b>{{ previewData.title || previewData.examTitle }}</b></span>
            <span class="preview-count">{{ previewData.questionCount || previewData.questions?.length || 0 }} 题</span>
          </div>
          <div
            v-for="(q, i) in (previewData.questions || [])"
            :key="i"
            class="preview-item"
            :class="{ 'preview-item-expanded': previewExpanded.has(i) }"
            @click="previewExpanded.has(i) ? previewExpanded.delete(i) : previewExpanded.add(i)"
          >
            <div class="preview-q-title">
              <span>{{ i + 1 }}. </span>
              <span>{{ q.questionText }}</span>
              <el-icon class="preview-q-arrow" :class="{ 'preview-q-arrow-open': previewExpanded.has(i) }"><ArrowDown /></el-icon>
            </div>
            <!-- 展开详情 -->
            <div v-if="previewExpanded.has(i)" class="preview-q-detail" @click.stop>
              <div v-if="q.options" class="preview-q-opts">
                <div
                  v-for="(opt, j) in (typeof q.options === 'string' ? parseOptions(q.options) : q.options)"
                  :key="j"
                  class="preview-opt"
                  :class="{ 'preview-opt-correct': q.correctAnswer && String.fromCharCode(65+j) === String(q.correctAnswer).toUpperCase() }"
                >
                  {{ String.fromCharCode(65+j) }}. {{ typeof opt === 'string' ? opt.replace(/^[A-Z][.、．]\s*/, '') : opt }}
                </div>
              </div>
              <div v-if="q.correctAnswer" class="preview-q-answer">答案：<b>{{ q.correctAnswer }}</b></div>
              <div v-if="q.explanation" class="preview-q-explain">解析：{{ q.explanation }}</div>
            </div>
          </div>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useIsMobile } from '@/composables/useIsMobile'
import { getMyShares, deleteShare, previewShare } from '@/api/examShare'
import ShareCodeDialog from '@/components/common/ShareCodeDialog.vue'
import ImportExamDialog from '@/components/common/ImportExamDialog.vue'

const router = useRouter()
const list = ref([])
const loading = ref(false)
const showShareDialog = ref(false)
const showImport = ref(false)
const page = ref(1)
const pageSize = 10
const { isMobile } = useIsMobile()

const paginated = computed(() => {
  const start = (page.value - 1) * pageSize
  return list.value.slice(start, start + pageSize)
})

const loadList = async () => {
  loading.value = true
  try {
    const res = await getMyShares()
    if (res.code === 200) list.value = res.data || []
  } catch { ElMessage.error('加载分享列表失败') } finally { loading.value = false }
}

const copyCode = async (code) => {
  try {
    await navigator.clipboard.writeText(code)
    ElMessage.success('分享码已复制')
  } catch {
    // HTTP 回退：创建隐藏 textarea
    const ta = document.createElement('textarea')
    ta.value = code; ta.style.position = 'fixed'; ta.style.opacity = '0'
    document.body.appendChild(ta); ta.select()
    document.execCommand('copy'); document.body.removeChild(ta)
    ElMessage.success('分享码已复制')
  }
}

const goTask = (row) => { if (row.taskId) router.push('/teacher/tasks/' + row.taskId + '/grade') }

const previewVisible = ref(false)
const previewData = ref(null)
const previewLoading = ref(false)
const previewExpanded = ref(new Set())
const handlePreview = async (code) => {
  previewVisible.value = true; previewLoading.value = true; previewData.value = null; previewExpanded.value = new Set()
  try {
    const res = await previewShare(code)
    if (res.code === 200) previewData.value = res.data
    else ElMessage.error(res.message || '加载失败')
  } catch { ElMessage.error('预览失败') } finally { previewLoading.value = false }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('取消分享后，试卷将回到任务列表中作为草稿。确定取消？', '提示', { type: 'warning' })
    await deleteShare(row.id)
    ElMessage.success('已取消分享，试卷已回到任务列表')
    loadList()
  } catch { /* cancelled */ }
}

const parseOptions = (opts) => { try { return JSON.parse(opts) } catch { return [] } }

function isExpiringSoon(row) {
  if (row.expired || !row.expiresAt) return false
  const now = Date.now()
  const expires = new Date(row.expiresAt).getTime()
  const daysLeft = (expires - now) / (1000 * 60 * 60 * 24)
  return daysLeft < 3 && daysLeft > 0
}
function tagType(row) {
  if (row.expired) return 'danger'
  if (isExpiringSoon(row)) return 'warning'
  return 'success'
}
function tagText(row) {
  if (row.expired) return '已过期'
  if (isExpiringSoon(row)) return '即将过期'
  return '有效'
}

onMounted(() => { loadList() })

</script>

<style scoped>
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
.card-list { display: flex; flex-direction: column; gap: 12px; }
.es-card { padding: 16px; background: var(--bg-card); border-radius: var(--radius-md); border: 1px solid var(--border-light); }
.es-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.es-title { font-weight: 600; color: var(--text-primary); font-size: var(--fs-sm); }
.es-meta { display: flex; align-items: center; gap: 8px; font-size: var(--fs-xs); color: var(--text-secondary); margin-bottom: 4px; }
.es-actions { margin-top: 8px; text-align: right; }
.preview-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; padding-bottom: 10px; border-bottom: 1px solid var(--border-light); }
.preview-count { color: var(--text-secondary); font-size: var(--fs-sm); }
.preview-item { margin-bottom: 14px; padding: 8px 0; border-bottom: 1px solid var(--border-light); cursor: pointer; transition: background var(--transition-base); }
.preview-item:hover { background: var(--bg-hover-light); }
.preview-item-expanded { background: var(--bg-section); }
.preview-q-title { font-weight: 500; margin-bottom: 6px; display: flex; align-items: center; gap: 6px; }
.preview-q-arrow { margin-left: auto; transition: transform var(--transition-base); font-size: var(--fs-xs); color: var(--text-secondary); flex-shrink: 0; }
.preview-q-arrow-open { transform: rotate(180deg); }
.preview-q-detail { margin-top: 8px; padding: 10px 12px; background: var(--bg-card); border-radius: var(--radius-sm); border: 0.5px solid var(--border-light); }
.preview-q-opts { padding-left: 20px; margin-bottom: 6px; }
.preview-opt { font-size: var(--fs-sm); color: var(--text-secondary); padding: 2px 0; }
.preview-opt-correct { color: var(--success-color); font-weight: 600; }
.preview-q-answer { font-size: var(--fs-sm); color: var(--success-color); margin-top: 4px; }
.preview-q-explain { font-size: var(--fs-sm); color: var(--text-secondary); margin-top: 4px; }
@media (max-width: 768px) {
  :deep(.el-table) { font-size: var(--fs-xs); }
}
</style>

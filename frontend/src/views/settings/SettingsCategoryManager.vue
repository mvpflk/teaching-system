<template>
  <div class="category-manager">
    <div class="cm-header">
      <div class="cm-header__text">
        <h3 class="cm-header__title">知识点分类管理</h3>
        <p class="cm-header__desc">管理各学科下的章节、任务和知识点层级结构</p>
      </div>
      <el-button size="small" @click="impVisible = true">
        <el-icon><Upload /></el-icon>
        模板导入
      </el-button>
    </div>

    <div class="cm-panels">
      <div class="cm-panel cm-panel--subject">
        <div class="cm-panel__head">
          <span class="cm-panel__label">学科</span>
          <span class="cm-panel__count">{{ categoryTree.length }}</span>
        </div>
        <div v-loading="treeLoading" class="cm-panel__body">
          <div v-if="!categoryTree.length && !treeLoading" class="cm-empty">
            暂无学科数据
          </div>
          <div
            v-for="s in categoryTree"
            :key="s.id"
            class="cm-item"
            :class="{ 'is-active': selSubject?.id === s.id }"
            @click="onSelectSubject(s)"
          >
            <span class="cm-item__name">{{ s.name }}</span>
            <span v-if="s.children?.length" class="cm-item__badge">{{ s.children.length }}</span>
          </div>
        </div>
      </div>

      <div class="cm-panel cm-panel--chapter">
        <div class="cm-panel__head">
          <span class="cm-panel__label">章节</span>
          <button
            class="cm-panel__add"
            :disabled="!selSubject"
            @click="showDialog(null, 1)"
          >
            +
          </button>
        </div>
        <div class="cm-panel__body">
          <div v-if="!selSubject" class="cm-empty cm-empty--hint">请先选择学科</div>
          <div v-else-if="!chapters.length" class="cm-empty">暂无章节</div>
          <div
            v-for="c in chapters"
            :key="c.id"
            class="cm-item"
            :class="{ 'is-active': selChapter?.id === c.id }"
            @click="onSelectChapter(c)"
          >
            <span class="cm-item__name">{{ c.name }}</span>
            <span class="cm-item__actions">
              <button class="cm-action" title="编辑" @click.stop="showDialog(c, 1)">✎</button>
              <button class="cm-action cm-action--danger" title="删除" @click.stop="delCat(c)">✕</button>
            </span>
          </div>
        </div>
      </div>

      <div class="cm-panel cm-panel--task">
        <div class="cm-panel__head">
          <span class="cm-panel__label">任务</span>
          <button
            class="cm-panel__add"
            :disabled="!selChapter"
            @click="showDialog(null, 2)"
          >
            +
          </button>
        </div>
        <div class="cm-panel__body">
          <div v-if="!selChapter" class="cm-empty cm-empty--hint">请先选择章节</div>
          <div v-else-if="!tasks.length" class="cm-empty">暂无任务</div>
          <div
            v-for="t in tasks"
            :key="t.id"
            class="cm-item"
            :class="{ 'is-active': selTask?.id === t.id }"
            @click="onSelectTask(t)"
          >
            <span class="cm-item__name">{{ t.name }}</span>
            <span class="cm-item__actions">
              <button class="cm-action" title="编辑" @click.stop="showDialog(t, 2)">✎</button>
              <button class="cm-action cm-action--danger" title="删除" @click.stop="delCat(t)">✕</button>
            </span>
          </div>
        </div>
      </div>

      <div class="cm-panel cm-panel--knowledge">
        <div class="cm-panel__head">
          <span class="cm-panel__label">知识点</span>
          <button
            class="cm-panel__add"
            :disabled="!selTask"
            @click="showDialog(null, 3)"
          >
            +
          </button>
        </div>
        <div class="cm-panel__body">
          <div v-if="!selTask" class="cm-empty cm-empty--hint">请先选择任务</div>
          <div v-else-if="!kps.length" class="cm-empty">暂无知识点</div>
          <div
            v-for="k in kps"
            :key="k.id"
            class="cm-item cm-item--kp"
          >
            <div class="cm-item__main">
              <span class="cm-item__name cm-item__name--ellip">{{ k.name }}</span>
              <span
                v-if="k.resourceStatus"
                class="cm-status"
                :class="'cm-status--' + k.resourceStatus.toLowerCase()"
              >{{ statusLabel(k.resourceStatus) }}</span>
            </div>
            <span class="cm-item__actions">
              <button
                v-if="!k.resourceStatus || k.resourceStatus === 'REJECTED'"
                class="cm-action cm-action--ai"
                title="AI生成学习资源"
                @click.stop="genResource(k)"
              >🤖</button>
              <button
                v-if="k.resourceStatus"
                class="cm-action cm-action--review"
                title="审核资源"
                @click.stop="openReview(k)"
              >✓</button>
              <button class="cm-action" title="编辑" @click.stop="showDialog(k, 3)">✎</button>
              <button class="cm-action cm-action--danger" title="删除" @click.stop="delCat(k)">✕</button>
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editing ? '编辑' : '新增'"
      width="480px"
      append-to-body
    >
      <el-form ref="formRef" :rules="rules" @submit.prevent="save">
        <el-form-item label="名称" prop="name">
          <el-input
            ref="nameInput"
            v-model="form.name"
            placeholder="输入名称"
            @keyup.enter="save"
          />
        </el-form-item>
        <el-form-item v-if="form._level === 3" label="讲解内容">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
            placeholder="知识点讲解文字（可选），AI出题时作为参考资料注入"
          />
          <div class="cm-form-hint">用于AI出题的RAG私有知识注入；留空则AI仅根据名称生成</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">确定</el-button>
      </template>
    </el-dialog>

    <!-- 审核对话框 -->
    <el-dialog
      v-model="reviewVisible"
      title="审核学习资源"
      width="480px"
      append-to-body
    >
      <div v-if="reviewLoading" v-loading="reviewLoading" style="min-height:120px" />
      <template v-else>
        <div class="cm-review-info">
          <div class="cm-review-row">
            <span class="cm-review-label">知识点</span>
            <span class="cm-review-value">{{ reviewNode?.name }}</span>
          </div>
          <div class="cm-review-row">
            <span class="cm-review-label">当前状态</span>
            <span
              class="cm-status"
              :class="'cm-status--' + (reviewNode?.resourceStatus || '').toLowerCase()"
            >{{ statusLabel(reviewNode?.resourceStatus) }}</span>
          </div>
        </div>
        <div class="cm-review-actions">
          <span class="cm-review-label">审核操作</span>
          <el-radio-group v-model="reviewAction" size="small">
            <el-radio-button value="APPROVED">通过</el-radio-button>
            <el-radio-button value="REJECTED">拒绝</el-radio-button>
          </el-radio-group>
        </div>
        <el-input
          v-if="reviewAction === 'REJECTED'"
          v-model="reviewReason"
          type="textarea"
          :rows="3"
          placeholder="请输入拒绝原因（学生可见）"
          style="margin-top:12px"
        />
      </template>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewSubmitting" @click="doReview">确认审核</el-button>
      </template>
    </el-dialog>

    <!-- Excel导入弹窗 -->
    <el-dialog
      v-model="impVisible"
      title="导入知识点"
      width="480px"
      append-to-body
      @close="impFile = null"
    >
      <div class="cm-import-help">
        <p>Excel列顺序:</p>
        <p><strong>5列新格式:</strong> 学科名 | 章节名 | 任务名 | 知识点名 | 内容</p>
        <p><strong>4列旧格式:</strong> 学科名 | 章节名 | 知识点名 | 内容</p>
        <p class="cm-form-hint">学科不存在时自动创建，章节/任务重复时复用</p>
      </div>
      <div style="margin-bottom:12px">
        <el-button
          size="small"
          type="primary"
          link
          @click="downloadTemplate"
        >
          下载导入模板
        </el-button>
      </div>
      <el-upload
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        :on-change="onImpFileChange"
        drag
      >
        <el-icon class="upload-icon"><UploadFilled /></el-icon>
        <div class="upload-text">点击或拖拽上传Excel</div>
      </el-upload>
      <template #footer>
        <el-button @click="impVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="impLoading"
          :disabled="!impFile"
          @click="doImport"
        >
          开始导入
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useFormRules } from '@/composables/useFormRules'
import request from '@/utils/request'
import { getNodeTree, createNode, updateNode, deleteNode, generateResources, reviewResource } from '@/api/knowledgeNode'

const { required: req } = useFormRules()
const rules = { name: [req('名称')] }
const formRef = ref(null)
const nameInput = ref(null)

const categoryTree = ref([])
const treeLoading = ref(false)
const selSubject = ref(null)
const selChapter = ref(null)
const selTask = ref(null)
const dialogVisible = ref(false)
const editing = ref(null)
const form = ref({ name: '', content: '' })

const chapters = computed(() => selSubject.value?.children || [])
const tasks = computed(() => selChapter.value?.children || [])
const kps = computed(() => selTask.value?.children || [])

const statusLabel = (s) => {
  const map = { PENDING: '待审', APPROVED: '已通过', REJECTED: '已拒绝' }
  return map[s] || s || ''
}

const onSelectSubject = (s) => { selSubject.value = s; selChapter.value = null; selTask.value = null }
const onSelectChapter = (c) => { selChapter.value = c; selTask.value = null }
const onSelectTask = (t) => { selTask.value = t }

const loadTree = async () => {
  treeLoading.value = true
  try {
    const res = await getNodeTree()
    if (res.code === 200) categoryTree.value = res.data
  } catch { categoryTree.value = [] }
  finally { treeLoading.value = false }
}

const findInTree = (nodes, id) => {
  for (const n of nodes) {
    if (n.id === id) return n
    if (n.children) { const f = findInTree(n.children, id); if (f) return f }
  }
  return null
}

const showDialog = (item, level) => {
  editing.value = item?.id ? item : null
  form.value = { name: item?.name || '', content: item?.content || '', _level: level }
  dialogVisible.value = true
  nextTick(() => nameInput.value?.focus())
}

const save = async () => {
  if (!formRef.value) return
  try { await formRef.value.validate() } catch { return }
  const level = form.value._level
  const data = { name: form.value.name, content: form.value.content, level, subjectId: selSubject.value?.subjectId }
  if (level === 2) data.parentId = selChapter.value?.id
  if (level === 3) data.parentId = selTask.value?.id
  try {
    if (editing.value) {
      await updateNode(editing.value.id, data)
      ElMessage.success('已更新')
    } else {
      await createNode(data)
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    await loadTree()
    if (selSubject.value) {
      const updated = findInTree(categoryTree.value, selSubject.value.id)
      selSubject.value = updated
      selChapter.value = null; selTask.value = null
    }
  } catch { ElMessage.error('操作失败') }
}

const delCat = async (item) => {
  try {
    await ElMessageBox.confirm(`确定删除「${item.name}」及其所有子级吗？`, '确认', { type: 'warning' })
    await deleteNode(item.id)
    ElMessage.success('已删除')
    selChapter.value = null; selTask.value = null
    await loadTree()
    if (selSubject.value) {
      selSubject.value = findInTree(categoryTree.value, selSubject.value.id)
    }
  } catch { /* cancelled */ }
}

const impVisible = ref(false)
const impFile = ref(null)
const impLoading = ref(false)
const onImpFileChange = (f) => { impFile.value = f.raw }
const doImport = async () => {
  if (!impFile.value) return
  impLoading.value = true
  try {
    const fd = new FormData(); fd.append('file', impFile.value)
    const r = await request({ url: '/knowledge-node/actions/import', method: 'post', data: fd, headers: { 'Content-Type': 'multipart/form-data' } })
    if (r.code === 200) { ElMessage.success(r.message || '导入成功'); impVisible.value = false; loadTree() }
    else ElMessage.error(r.message || '导入失败')
  } catch { ElMessage.error('导入失败') }
  finally { impLoading.value = false }
}

const genLoading = ref(false)
const reviewVisible = ref(false)
const reviewNode = ref(null)
const reviewAction = ref('APPROVED')
const reviewReason = ref('')
const reviewLoading = ref(false)
const reviewSubmitting = ref(false)

const genResource = async (k) => {
  genLoading.value = true
  try {
    const r = await generateResources(k.id)
    if (r.code === 200) {
      ElMessage.success(`「${k.name}」学习资源已生成，待审核`)
      await loadTree()
      if (selTask.value) selTask.value = findInTree(categoryTree.value, selTask.value.id)
    } else {
      ElMessage.error(r.message || '生成失败')
    }
  } catch { ElMessage.error('生成失败，请稍后重试') }
  finally { genLoading.value = false }
}

const openReview = (k) => {
  reviewNode.value = k
  reviewAction.value = 'APPROVED'
  reviewReason.value = ''
  reviewVisible.value = true
}

const doReview = async () => {
  if (!reviewNode.value) return
  reviewSubmitting.value = true
  try {
    const r = await reviewResource(reviewNode.value.id, reviewAction.value, reviewReason.value || null)
    if (r.code === 200) {
      ElMessage.success(reviewAction.value === 'APPROVED' ? '已通过审核' : '已拒绝')
      reviewVisible.value = false
      await loadTree()
      if (selTask.value) selTask.value = findInTree(categoryTree.value, selTask.value.id)
    } else {
      ElMessage.error(r.message || '审核失败')
    }
  } catch { ElMessage.error('审核失败') }
  finally { reviewSubmitting.value = false }
}

const downloadTemplate = () => {
  const a = document.createElement('a')
  a.href = '/api/knowledge-node/actions/template/download'
  a.download = '知识点导入模板.xlsx'
  a.click()
}

onMounted(() => { loadTree() })
</script>

<style scoped>
.category-manager {
  max-width: 1200px;
}

.cm-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--spacing-lg);
}
.cm-header__title {
  font-size: var(--fs-xl);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 4px;
}
.cm-header__desc {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  margin: 0;
}

/* Panels container */
.cm-panels {
  display: grid;
  grid-template-columns: 1.2fr 1fr 0.8fr 1.2fr;
  gap: 1px;
  background: var(--border-light);
  border-radius: var(--radius-lg);
  border: 0.5px solid var(--border-color);
  overflow: hidden;
}

.cm-panel {
  background: var(--bg-card);
  display: flex;
  flex-direction: column;
  min-height: 480px;
}
.cm-panel__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 0.5px solid var(--border-light);
  background: var(--bg-section);
}
.cm-panel__label {
  font-size: var(--fs-sm);
  font-weight: 600;
  color: var(--text-regular);
  letter-spacing: 0.02em;
}
.cm-panel__count {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  background: var(--bg-secondary);
  padding: 1px 8px;
  border-radius: var(--radius-full);
  font-variant-numeric: tabular-nums;
}
.cm-panel__add {
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: var(--bg-secondary);
  color: var(--text-secondary);
  border-radius: var(--radius-xs);
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all var(--transition-fast);
}
.cm-panel__add:hover:not(:disabled) {
  background: var(--primary-light);
  color: var(--primary-color);
}
.cm-panel__add:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
.cm-panel__body {
  flex: 1;
  overflow-y: auto;
  padding: 6px;
}

/* Items */
.cm-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 10px;
  margin: 2px 0;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: var(--fs-sm);
  color: var(--text-regular);
  transition: all var(--transition-fast);
  position: relative;
}
.cm-item:hover {
  background: var(--bg-hover);
}
.cm-item.is-active {
  background: var(--primary-color);
  color: #fff;
}
.cm-item.is-active .cm-item__badge {
  background: rgba(255,255,255,0.2);
  color: #fff;
}
.cm-item.is-active .cm-item__actions {
  display: flex;
}
.cm-item.is-active .cm-action {
  color: rgba(255,255,255,0.8);
}
.cm-item.is-active .cm-action:hover {
  color: #fff;
  background: rgba(255,255,255,0.15);
}
.cm-item.is-active .cm-action--danger:hover {
  background: rgba(255,255,255,0.2);
}

.cm-item__name {
  flex: 1;
  min-width: 0;
}
.cm-item__name--ellip {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cm-item__badge {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  background: var(--bg-secondary);
  padding: 1px 7px;
  border-radius: var(--radius-full);
  margin-left: 6px;
  flex-shrink: 0;
  font-variant-numeric: tabular-nums;
}

.cm-item__actions {
  display: none;
  gap: 2px;
  margin-left: 4px;
  flex-shrink: 0;
}
.cm-item:hover .cm-item__actions {
  display: flex;
}

.cm-action {
  width: 22px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  border-radius: var(--radius-xs);
  cursor: pointer;
  font-size: 12px;
  transition: all var(--transition-fast);
  padding: 0;
}
.cm-action:hover {
  background: var(--bg-secondary);
  color: var(--text-primary);
}
.cm-action--danger:hover {
  background: var(--bg-danger-light);
  color: var(--danger);
}
.cm-action--ai:hover {
  background: var(--primary-light);
  color: var(--primary-color);
}
.cm-action--review:hover {
  background: var(--bg-success-light);
  color: var(--success);
}

/* Knowledge point specific */
.cm-item--kp {
  flex-wrap: wrap;
}
.cm-item__main {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  min-width: 0;
}

/* Status tags */
.cm-status {
  display: inline-flex;
  align-items: center;
  font-size: var(--fs-xs);
  padding: 1px 6px;
  border-radius: var(--radius-xs);
  flex-shrink: 0;
  font-weight: 500;
}
.cm-status--pending {
  color: var(--el-color-warning);
  background: var(--bg-warning-light);
}
.cm-status--approved {
  color: var(--el-color-success);
  background: var(--bg-success-light);
}
.cm-status--rejected {
  color: var(--el-color-danger);
  background: var(--bg-danger-light);
}

/* Empty states */
.cm-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 80px;
  font-size: var(--fs-sm);
  color: var(--text-disabled);
}
.cm-empty--hint {
  color: var(--text-secondary);
}

/* Form hint */
.cm-form-hint {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  margin-top: 4px;
  line-height: 1.5;
}

/* Import dialog */
.cm-import-help {
  margin-bottom: 12px;
  font-size: var(--fs-sm);
  color: var(--text-regular);
  line-height: 1.8;
}
.cm-import-help p { margin: 0; }

/* Review dialog */
.cm-review-info {
  margin-bottom: 16px;
}
.cm-review-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 0.5px solid var(--border-light);
}
.cm-review-row:last-child { border-bottom: none; }
.cm-review-label {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  width: 64px;
  flex-shrink: 0;
}
.cm-review-value {
  font-size: var(--fs-sm);
  color: var(--text-primary);
  font-weight: 500;
}
.cm-review-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* Responsive */
@media (max-width: 1100px) {
  .cm-panels {
    grid-template-columns: 1fr 1fr;
  }
}
@media (max-width: 768px) {
  .cm-panels {
    grid-template-columns: 1fr;
  }
  .cm-panel {
    min-height: auto;
  }
  .cm-panel__body {
    max-height: 200px;
  }
}
</style>

<template>
  <div class="basic-settings">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="年级字典" name="grades">
        <div class="tab-toolbar">
          <el-button type="primary" size="default" @click="openGradeDialog()">新增年级</el-button>
          <el-button size="default" @click="openBatchDialog('grades')">批量导入</el-button>
        </div>
        <el-table
          v-loading="gLoading"
          :data="grades"
          stripe
          empty-text="暂无年级数据"
        >
          <el-table-column prop="gradeName" label="年级名称" min-width="120" />
          <el-table-column
            prop="sortOrder"
            label="排序"
            width="80"
            align="center"
          />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" align="center">
            <template #default="{ row }">
              <el-button
                size="small"
                text
                type="primary"
                @click="openGradeDialog(row)"
              >
                编辑
              </el-button>
              <el-button
                size="small"
                text
                type="danger"
                @click="deleteGradeRow(row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="学科字典" name="subjects">
        <div class="tab-toolbar">
          <el-button type="primary" size="default" @click="openSubjectDialog()">新增学科</el-button>
          <el-button size="default" @click="openBatchDialog('subjects')">批量导入</el-button>
        </div>
        <el-table
          v-loading="sLoading"
          :data="subjects"
          stripe
          empty-text="暂无学科数据"
        >
          <el-table-column prop="subjectName" label="学科名称" min-width="140" />
          <el-table-column
            prop="sortOrder"
            label="排序"
            width="80"
            align="center"
          />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" align="center">
            <template #default="{ row }">
              <el-button
                size="small"
                text
                type="primary"
                @click="openSubjectDialog(row)"
              >
                编辑
              </el-button>
              <el-button
                size="small"
                text
                type="danger"
                @click="deleteSubjectRow(row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="专业设置" name="majors">
        <div class="tab-toolbar">
          <el-button type="primary" size="default" @click="openMajorDialog()">新增专业</el-button>
          <el-button size="default" @click="openBatchDialog('majors')">批量导入</el-button>
        </div>
        <el-table
          v-loading="mjLoading"
          :data="majors"
          stripe
          empty-text="暂无专业数据"
        >
          <el-table-column prop="majorName" label="专业名称" min-width="140" />
          <el-table-column
            prop="sortOrder"
            label="排序"
            width="80"
            align="center"
          />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="240" align="center">
            <template #default="{ row }">
              <el-button
                size="small"
                text
                type="primary"
                @click="openMajorDialog(row)"
              >
                编辑
              </el-button>
              <el-button
                size="small"
                text
                type="primary"
                @click="openMajorSubjectDialog(row)"
              >
                管理学科
              </el-button>
              <el-button
                size="small"
                text
                type="danger"
                @click="deleteMajorRow(row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="📚 知识点分类" name="category">
        <SettingsCategoryManager />
      </el-tab-pane>

      <el-tab-pane label="🎨 站点外观" name="appearance">
        <div class="appearance-section">
          <h4 style="margin:0 0 4px;font-size:var(--fs-md)">学校 Logo</h4>
          <p style="font-size:var(--fs-sm);color:var(--text-secondary);margin:0 0 20px">
            上传学校 Logo，将在登录页面顶部显示。建议使用正方形图片，自动裁剪为圆形 80×80px。
          </p>

          <div class="logo-preview-row">
            <div class="logo-preview-box">
              <img
                v-if="logoUrl"
                :src="logoUrl"
                alt="学校Logo"
                class="logo-preview-img"
                @error="onLogoError"
              />
              <div v-else class="logo-preview-placeholder">
                <svg
                  width="40"
                  height="40"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.5"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <path d="M22 10v6M2 10l10-5 10 5M6 12v5h12v-5" />
                  <circle cx="12" cy="15" r="2" />
                </svg>
              </div>
            </div>
            <div class="logo-actions">
              <el-upload
                :auto-upload="false"
                :show-file-list="false"
                accept=".jpg,.jpeg,.png,.gif,.bmp"
                :on-change="handleLogoSelect"
              >
                <el-button type="primary" :loading="logoUploading">
                  {{ logoUrl ? '更换 Logo' : '上传 Logo' }}
                </el-button>
              </el-upload>
              <el-button
                v-if="logoUrl"
                type="danger"
                plain
                :loading="logoDeleting"
                @click="handleDeleteLogo"
              >
                恢复默认
              </el-button>
            </div>
          </div>

          <el-divider />

          <h4 style="margin:0 0 4px;font-size:var(--fs-md)">站点标题</h4>
          <p style="font-size:var(--fs-sm);color:var(--text-secondary);margin:0 0 12px">
            显示在浏览器标签页和登录页面
          </p>
          <div style="display:flex;gap:12px;max-width:480px">
            <el-input v-model="siteTitle" placeholder="智慧教育管理系统" maxlength="50" />
            <el-button type="primary" :loading="titleSaving" @click="saveSiteTitle">保存</el-button>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 年级对话框 -->
    <el-dialog
      v-model="gradeVisible"
      :title="gradeForm.id ? '编辑年级' : '新增年级'"
      width="420px"
      destroy-on-close
      append-to-body
    >
      <el-form
        ref="gradeFormRef"
        :model="gradeForm"
        :rules="dictRules"
        label-position="top"
      >
        <el-form-item label="年级名称" prop="gradeName"><el-input v-model="gradeForm.gradeName" maxlength="20" /></el-form-item>
        <el-form-item label="排序">
          <el-input-number
            v-model="gradeForm.sortOrder"
            :min="1"
            :max="99"
            controls-position="right"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="gradeForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="gradeVisible = false">取消</el-button>
        <el-button type="primary" :loading="gradeSaving" @click="saveGrade">保存</el-button>
      </template>
    </el-dialog>

    <!-- 学科对话框 -->
    <el-dialog
      v-model="subjectVisible"
      :title="subjectForm.id ? '编辑学科' : '新增学科'"
      width="420px"
      destroy-on-close
      append-to-body
    >
      <el-form
        ref="subjectFormRef"
        :model="subjectForm"
        :rules="dictRules"
        label-position="top"
      >
        <el-form-item label="学科名称" prop="subjectName"><el-input v-model="subjectForm.subjectName" maxlength="30" /></el-form-item>
        <el-form-item label="排序">
          <el-input-number
            v-model="subjectForm.sortOrder"
            :min="1"
            :max="99"
            controls-position="right"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="subjectForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="subjectVisible = false">取消</el-button>
        <el-button type="primary" :loading="subjectSaving" @click="saveSubject">保存</el-button>
      </template>
    </el-dialog>

    <!-- 专业对话框 -->
    <el-dialog
      v-model="majorVisible"
      :title="majorForm.id ? '编辑专业' : '新增专业'"
      width="420px"
      destroy-on-close
      append-to-body
    >
      <el-form
        ref="majorFormRef"
        :model="majorForm"
        :rules="majorRules"
        label-position="top"
      >
        <el-form-item label="专业名称" prop="majorName"><el-input v-model="majorForm.majorName" maxlength="30" /></el-form-item>
        <el-form-item label="排序">
          <el-input-number
            v-model="majorForm.sortOrder"
            :min="1"
            :max="99"
            controls-position="right"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="majorForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="majorVisible = false">取消</el-button>
        <el-button type="primary" :loading="majorSaving" @click="saveMajor">保存</el-button>
      </template>
    </el-dialog>

    <!-- 专业-学科关联对话框 -->
    <el-dialog
      v-model="majorSubjectVisible"
      :title="'管理学科 — ' + currentMajorName"
      width="480px"
      destroy-on-close
      append-to-body
    >
      <p style="color:var(--text-secondary);font-size:var(--fs-sm);margin-bottom:16px">
        勾选该专业需要关联的学科，未勾选的学科将解除关联。
      </p>
      <el-checkbox-group v-model="selectedSubjectIds" style="display:flex;flex-direction:column;gap:8px">
        <el-checkbox
          v-for="subj in availableSubjects"
          :key="subj.id"
          :label="subj.id"
          :value="subj.id"
        >
          {{ subj.subjectName }}
        </el-checkbox>
      </el-checkbox-group>
      <div v-if="availableSubjects.length === 0" style="text-align:center;color:var(--text-disabled);padding:20px">
        暂无可用学科，请先在"学科字典"中添加启用的学科
      </div>
      <template #footer>
        <el-button @click="majorSubjectVisible = false">取消</el-button>
        <el-button type="primary" :loading="majorSubjectSaving" @click="saveMajorSubjects">保存</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入弹窗 -->
    <el-dialog
      v-model="batchVisible"
      :title="'批量导入' + batchTitle"
      width="500px"
      append-to-body
      @close="batchPreview=[];batchText=''"
    >
      <p style="color:var(--text-secondary);font-size:var(--fs-sm);margin-bottom:12px">
        每行一条记录，格式: <b>名称,排序</b>（排序可选，默认0）
      </p>
      <el-input
        v-model="batchText"
        type="textarea"
        :rows="8"
        placeholder="语文,1&#10;数学,2&#10;英语,3&#10;计算机,4"
        @input="parseBatchPreview"
      />
      <div v-if="batchPreview.length" style="margin-top:12px">
        <p style="font-size:var(--fs-sm);color:var(--text-secondary)">预览（共 {{ batchPreview.length }} 条）：</p>
        <div style="max-height:160px;overflow-y:auto;border:1px solid var(--border-light);border-radius:4px;padding:8px">
          <div v-for="(item,i) in batchPreview" :key="i" style="font-size:var(--fs-sm);padding:2px 0;border-bottom:1px solid var(--border-light)">
            {{ item.name }} <span v-if="item.sortOrder" style="color:var(--text-secondary)">排序:{{ item.sortOrder }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="batchVisible=false">取消</el-button>
        <el-button
          type="primary"
          :loading="batchSaving"
          :disabled="!batchPreview.length"
          @click="doBatchImport"
        >
          确认导入 {{ batchPreview.length }} 条
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useFormRules } from '@/composables/useFormRules'
import request from '@/utils/request'
import { getGrades, createGrade, updateGrade, deleteGrade, getSubjects, createSubject, updateSubject, deleteSubject, getMajors, createMajor, updateMajor, deleteMajor, getMajorSubjects, setMajorSubjects } from '@/api/settings'
import { getSystemLogo, uploadLogo, deleteLogo } from '@/api/system'
import SettingsCategoryManager from '@/views/settings/SettingsCategoryManager.vue'

const { required: req } = useFormRules()

// ── 站点外观 ──
const logoUrl = ref('')
const logoUploading = ref(false)
const logoDeleting = ref(false)
const siteTitle = ref('')
const titleSaving = ref(false)

const loadLogo = async () => {
  try {
    const r = await getSystemLogo()
    if (r.code === 200 && r.data?.url) logoUrl.value = r.data.url
  } catch { /* 加载失败不影响使用 */ }
}

const loadSiteTitle = async () => {
  try {
    const r = await request({ url: '/settings/actions/config', method: 'get', params: { key: 'system.site_title' } })
    if (r.code === 200 && r.data) {
      siteTitle.value = r.data['system.site_title'] || ''
    }
  } catch { /* */ }
}

const handleLogoSelect = async (uploadFile) => {
  const file = uploadFile.raw
  if (!file) return
  logoUploading.value = true
  try {
    const r = await uploadLogo(file)
    if (r.code === 200) {
      logoUrl.value = r.data.url
      // 加时间戳防缓存
      logoUrl.value += '?t=' + Date.now()
      ElMessage.success('Logo 上传成功')
    } else {
      ElMessage.error(r.message || '上传失败')
    }
  } catch {
    ElMessage.error('上传失败，请重试')
  } finally {
    logoUploading.value = false
  }
}

const handleDeleteLogo = async () => {
  try { await ElMessageBox.confirm('确定恢复默认 Logo？原 Logo 将被删除。', '确认') } catch { return }
  logoDeleting.value = true
  try {
    const r = await deleteLogo()
    if (r.code === 200) {
      logoUrl.value = ''
      ElMessage.success('已恢复默认 Logo')
    } else {
      ElMessage.error(r.message || '操作失败')
    }
  } catch {
    ElMessage.error('操作失败')
  } finally {
    logoDeleting.value = false
  }
}

const onLogoError = () => {
  logoUrl.value = ''
}

const saveSiteTitle = async () => {
  titleSaving.value = true
  try {
    const r = await request({ url: '/settings/actions/update-config', method: 'put', data: { 'system.site_title': siteTitle.value } })
    if (r.code === 200) ElMessage.success('站点标题已更新')
    else ElMessage.error(r.message || '保存失败')
  } catch { ElMessage.error('保存失败') }
  finally { titleSaving.value = false }
}
const dictRules = { gradeName: [req('名称')], subjectName: [req('名称')] }

const activeTab = ref('grades')
const grades = ref([]); const subjects = ref([])
const gLoading = ref(false); const sLoading = ref(false)

// ── 年级 CRUD ──
const gradeFormRef = ref(null)
const gradeVisible = ref(false); const gradeSaving = ref(false)
const gradeForm = ref({})

const openGradeDialog = (row) => {
  gradeForm.value = row ? { ...row } : { gradeName: '', sortOrder: 1, status: 1 }
  gradeVisible.value = true
}
const loadGrades = async () => {
  gLoading.value = true
  try { const r = await getGrades(); if (r.code === 200) grades.value = r.data || [] }
  catch { ElMessage.error('加载年级失败') } finally { gLoading.value = false }
}
const saveGrade = async () => {
  if (gradeSaving.value) return
  if (!gradeFormRef.value) return
  try { await gradeFormRef.value.validate() } catch { return }
  gradeSaving.value = true
  try {
    const f = gradeForm.value
    const r = f.id ? await updateGrade(f.id, f) : await createGrade(f)
    if (r.code === 200) { ElMessage.success(f.id ? '已更新' : '已创建'); gradeVisible.value = false; loadGrades() }
    else ElMessage.error(r.message || '操作失败')
  } finally { gradeSaving.value = false }
}
const deleteGradeRow = async (row) => {
  try { await ElMessageBox.confirm(`删除年级"${row.gradeName}"？`, '确认') } catch { return }
  try {
    const r = await deleteGrade(row.id)
    if (r.code === 200) { ElMessage.success('已删除'); loadGrades() }
  } catch { /* */ }
}

// ── 学科 CRUD ──
const subjectFormRef = ref(null)
const subjectVisible = ref(false); const subjectSaving = ref(false)
const subjectForm = ref({})

const openSubjectDialog = (row) => {
  subjectForm.value = row ? { ...row } : { subjectName: '', sortOrder: 1, status: 1 }
  subjectVisible.value = true
}
const loadSubjects = async () => {
  sLoading.value = true
  try { const r = await getSubjects(); if (r.code === 200) subjects.value = r.data || [] }
  catch { ElMessage.error('加载学科失败') } finally { sLoading.value = false }
}
const saveSubject = async () => {
  if (subjectSaving.value) return
  if (!subjectFormRef.value) return
  try { await subjectFormRef.value.validate() } catch { return }
  subjectSaving.value = true
  try {
    const f = subjectForm.value
    const r = f.id ? await updateSubject(f.id, f) : await createSubject(f)
    if (r.code === 200) { ElMessage.success(f.id ? '已更新' : '已创建'); subjectVisible.value = false; loadSubjects() }
    else ElMessage.error(r.message || '操作失败')
  } finally { subjectSaving.value = false }
}
const deleteSubjectRow = async (row) => {
  try { await ElMessageBox.confirm(`删除学科"${row.subjectName}"？`, '确认') } catch { return }
  try {
    const r = await deleteSubject(row.id)
    if (r.code === 200) { ElMessage.success('已删除'); loadSubjects() }
  } catch { /* */ }
}

// ── 专业 CRUD ──
const majorFormRef = ref(null)
const majorVisible = ref(false); const majorSaving = ref(false)
const majorForm = ref({})
const majors = ref([]); const mjLoading = ref(false)
const majorRules = { majorName: [req('专业名称')] }

const openMajorDialog = (row) => {
  majorForm.value = row ? { ...row } : { majorName: '', sortOrder: 1, status: 1 }
  majorVisible.value = true
}
const loadMajors = async () => {
  mjLoading.value = true
  try { const r = await getMajors(); if (r.code === 200) majors.value = r.data || [] }
  catch { ElMessage.error('加载专业失败') } finally { mjLoading.value = false }
}
const saveMajor = async () => {
  if (majorSaving.value) return
  if (!majorFormRef.value) return
  try { await majorFormRef.value.validate() } catch { return }
  majorSaving.value = true
  try {
    const f = majorForm.value
    const r = f.id ? await updateMajor(f.id, f) : await createMajor(f)
    if (r.code === 200) { ElMessage.success(f.id ? '已更新' : '已创建'); majorVisible.value = false; loadMajors() }
    else ElMessage.error(r.message || '操作失败')
  } finally { majorSaving.value = false }
}
const deleteMajorRow = async (row) => {
  try { await ElMessageBox.confirm(`删除专业"${row.majorName}"？`, '确认') } catch { return }
  try {
    const r = await deleteMajor(row.id)
    if (r.code === 200) { ElMessage.success('已删除'); loadMajors() }
  } catch { /* */ }
}

// ── 专业-学科关联 ──
const majorSubjectVisible = ref(false)
const majorSubjectSaving = ref(false)
const currentMajorId = ref(null)
const currentMajorName = ref('')
const availableSubjects = ref([])
const selectedSubjectIds = ref([])

const openMajorSubjectDialog = async (row) => {
  currentMajorId.value = row.id
  currentMajorName.value = row.majorName
  selectedSubjectIds.value = []
  majorSubjectVisible.value = true
  try {
    const sRes = await getSubjects()
    if (sRes.code === 200) availableSubjects.value = sRes.data || []
    else availableSubjects.value = []
    const r = await getMajorSubjects(row.id)
    if (r.code === 200 && r.data) {
      selectedSubjectIds.value = r.data.map(s => s.id)
    }
  } catch {
    ElMessage.error('加载学科数据失败')
  }
}

const saveMajorSubjects = async () => {
  majorSubjectSaving.value = true
  try {
    const r = await setMajorSubjects(currentMajorId.value, { subjectIds: selectedSubjectIds.value })
    if (r.code === 200) {
      ElMessage.success('学科关联已更新')
      majorSubjectVisible.value = false
    } else {
      ElMessage.error(r.message || '保存失败')
    }
  } catch {
    ElMessage.error('保存失败')
  } finally {
    majorSubjectSaving.value = false
  }
}

// 批量导入
const batchVisible = ref(false); const batchText = ref(''); const batchType = ref('')
const batchPreview = ref([]); const batchSaving = ref(false)
const batchTitles = { grades: '年级', subjects: '学科', majors: '专业' }
const batchTitle = computed(() => batchTitles[batchType.value] || '')
const openBatchDialog = (type) => { batchType.value = type; batchText.value = ''; batchPreview.value = []; batchVisible.value = true }
const parseBatchPreview = () => {
  batchPreview.value = batchText.value.split('\n').filter(l => l.trim()).map(line => {
    const parts = line.split(',').map(s => s.trim())
    return { name: parts[0] || '', sortOrder: parseInt(parts[1]) || 0 }
  }).filter(item => item.name)
}
const doBatchImport = async () => {
  if (!batchPreview.value.length) return
  batchSaving.value = true
  try {
    const list = batchPreview.value.map(item => ({ name: item.name, sortOrder: item.sortOrder }))
    const r = await request({ url: `/settings/${batchType.value}/batch`, method: 'post', data: list })
    if (r.code === 200) { ElMessage.success(`成功导入 ${r.data?.count || batchPreview.value.length} 条`); batchVisible.value = false
      if (batchType.value === 'grades') loadGrades()
      else if (batchType.value === 'subjects') loadSubjects()
      else loadMajors()
    } else ElMessage.error(r.message || '导入失败')
  } catch { ElMessage.error('导入失败') }
  finally { batchSaving.value = false }
}

onMounted(() => { loadGrades(); loadSubjects(); loadMajors(); loadLogo(); loadSiteTitle() })
</script>

<style scoped lang="scss">
.basic-settings { max-width: 900px; }
.tab-toolbar { margin-bottom: 14px; }

// ── 站点外观 ──
.appearance-section { max-width: 600px; }

.logo-preview-row {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 8px;
}

.logo-preview-box {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  border: 1px solid var(--border-base);
  background: var(--bg-section);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.logo-preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.logo-preview-placeholder {
  color: var(--text-disabled);
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

@media (max-width: 768px) {
  :deep(.el-tabs__item) { font-size: var(--fs-sm); padding: 0 12px !important; }
  :deep(.el-tabs__nav-wrap) { overflow-x: auto; }
  .logo-preview-row { flex-direction: column; align-items: flex-start; }
}
</style>

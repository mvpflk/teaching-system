<template>
  <div class="create-post-container create-post-page">
    <!-- 顶部导航栏 -->
    <div class="bbs-top-nav">
      <el-button text class="back-btn" @click="router.push('/bbs')">
        <el-icon><ArrowLeft /></el-icon>返回论坛
      </el-button>
      <el-breadcrumb separator=">">
        <el-breadcrumb-item :to="{ path: '/bbs' }">师生论坛</el-breadcrumb-item>
        <el-breadcrumb-item>{{ isEdit ? '编辑帖子' : '发布新帖' }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="create-post-form">
      <el-card shadow="never">
        <h2>{{ isEdit ? '编辑帖子' : '发布新帖' }}</h2>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
        >
          <el-form-item label="版块" prop="categoryId">
            <el-select v-model="form.categoryId" placeholder="选择版块" style="width:100%">
              <el-option
                v-for="cat in categories"
                :key="cat.id"
                :value="cat.id"
                :label="cat.name"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="标题" prop="title">
            <el-input
              v-model="form.title"
              placeholder="请输入标题"
              maxlength="100"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="内容" prop="content">
            <div v-if="isMobile" class="bbs-mobile-toolbar">
              <el-button text circle @click="insertFormat('**')"><b>B</b></el-button>
              <el-button text circle @click="triggerUpload"><el-icon><Picture /></el-icon></el-button>
              <el-button text circle @click="insertFormat('[]()')"><el-icon><Link /></el-icon></el-button>
            </div>
            <MarkdownEditor
              ref="markdownEditorRef"
              v-model="form.content"
              placeholder="写下你想说的话...&#10;使用 @用户名 可以提及他人"
              :rows="10"
            />
          </el-form-item>

          <el-form-item label="图片">
            <div class="upload-area">
              <el-upload
                ref="uploadRef"
                v-model:file-list="fileList"
                :action="uploadUrl"
                :headers="uploadHeaders"
                list-type="picture-card"
                :on-success="handleUploadSuccess"
                :on-remove="handleUploadRemove"
                multiple
              >
                <el-icon><Plus /></el-icon>
              </el-upload>
            </div>
          </el-form-item>

          <!-- 德育行为表扬（仅教师可见） -->
          <template v-if="isTeacher && !isEdit">
            <el-divider />
            <el-form-item>
              <el-checkbox v-model="isMoralBehavior" size="default">
                <span style="font-weight:500;color:var(--el-color-warning);"><el-icon style="vertical-align:middle"><Medal /></el-icon> 记录为德育行为</span>
                <span style="font-size:var(--fs-xs);color:var(--text-secondary);margin-left:4px;">（表扬学生行为，自动发放德育积分）</span>
              </el-checkbox>
            </el-form-item>

            <template v-if="isMoralBehavior">
              <el-form-item label="任教班级">
                <el-select v-model="moralClassId" placeholder="选择班级" style="width:100%">
                  <el-option
                    v-for="c in teachingClasses"
                    :key="c.classId"
                    :value="c.classId"
                    :label="(c.grade||'') + c.className + ' · ' + (c.subject||'')"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="表扬学生">
                <el-select
                  v-model="praisedStudentId"
                  placeholder="选择学生"
                  style="width:100%"
                  :loading="loadingStudents"
                  filterable
                >
                  <el-option
                    v-for="s in classStudents"
                    :key="s.id"
                    :value="s.id"
                    :label="(s.realName||s.username) + (s.studentNumber ? ' (' + s.studentNumber + ')' : '')"
                  />
                </el-select>
              </el-form-item>
            </template>
          </template>
        </el-form>
      </el-card>
    </div>
    <div v-if="isMobile" class="bbs-mobile-submit" :style="{ paddingBottom: 'var(--safe-bottom)' }">
      <el-button
        type="primary"
        size="large"
        style="width:100%"
        :loading="submitting"
        @click="handleSubmit"
      >
        发布
      </el-button>
    </div>
    <div v-else class="create-post-footer">
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        {{ isMoralBehavior ? '发布德育表扬' : isEdit ? '保存编辑' : '发布帖子' }}
      </el-button>
      <el-button @click="router.back()">取消</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useFormRules } from '@/composables/useFormRules'
import MarkdownEditor from '@/components/common/MarkdownEditor.vue'
import { getCategories, createPost, updatePost, getPostDetail, createMoralPost } from '@/api/bbs'
import { getStudents } from '@/api/classes'
import { useIsMobile } from '@/composables/useIsMobile'
import { useKeyboardFix } from '@/composables/useKeyboardFix'
import { Picture } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isEdit = computed(() => !!route.params.id)
const categories = ref([])
const submitting = ref(false)
const fileList = ref([])
const uploadedImages = ref([])
const { required: req, selectRequired } = useFormRules()

const formRef = ref(null)
const rules = {
  categoryId: [selectRequired('版块')],
  title: [req('标题')],
  content: [req('内容')],
}

// 德育行为相关
const isMoralBehavior = ref(false)
const praisedStudentId = ref(null)
const moralClassId = ref(null)
const classStudents = ref([])
const loadingStudents = ref(false)
const isTeacher = computed(() => userStore.isTeacher)
const { isMobile } = useIsMobile()
const teachingClasses = computed(() => userStore.teacherSummary?.teachingClasses || [])

const markdownEditorRef = ref(null)
const uploadRef = ref(null)
const uploadUrl = '/api/upload/actions/bbs'
const uploadHeaders = { Authorization: `Bearer ${localStorage.getItem('token')}` }

const form = ref({
  categoryId: null,
  title: '',
  content: ''
})

const loadClassStudents = async (classId) => {
  if (!classId) { classStudents.value = []; return }
  loadingStudents.value = true
  try {
    const res = await getStudents(classId)
    if (res.code === 200) classStudents.value = res.data || []
  } finally { loadingStudents.value = false }
}

watch(moralClassId, (val) => { praisedStudentId.value = null; loadClassStudents(val) })

const loadCategories = async () => {
  try {
    const res = await getCategories()
    if (res.code === 200) categories.value = res.data
  } catch { ElMessage.error('加载版块失败') }
}

const loadPost = async () => {
  if (!route.params.id) return
  try {
    const res = await getPostDetail(route.params.id)
    if (res.code === 200) {
      form.value.categoryId = res.data.categoryId
      form.value.title = res.data.title
      form.value.content = res.data.content
      if (res.data.images) {
        try {
          uploadedImages.value = JSON.parse(res.data.images)
        } catch { uploadedImages.value = [] }
      }
    }
  } catch { ElMessage.error('加载帖子失败') }
}

const handleUploadSuccess = (res) => {
  if (res.code === 200 && res.data) {
    uploadedImages.value.push(res.data.url || res.data)
  }
}

const handleUploadRemove = (file) => {
  const url = file.url || file.response?.data?.url || file.response?.data
  if (url) {
    uploadedImages.value = uploadedImages.value.filter(u => u !== url)
  }
}

function insertFormat(format) {
  markdownEditorRef.value?.focus()
  if (format === '**') {
    document.execCommand('bold')
  } else if (format === '[]()') {
    const url = prompt('请输入链接地址：', 'https://')
    if (url) document.execCommand('createLink', false, url)
  }
}
function triggerUpload() {
  const el = uploadRef.value?.$el
  if (el) {
    const input = el.querySelector('input[type="file"]')
    if (input) input.click()
  }
}

const handleSubmit = async () => {
  if (submitting.value) return
  if (!formRef.value) return
  try { await formRef.value.validate() } catch { return }
  if (isMoralBehavior.value) {
    if (!moralClassId.value) { ElMessage.warning('请选择班级'); return }
    if (!praisedStudentId.value) { ElMessage.warning('请选择受表扬学生'); return }
  }
  submitting.value = true
  try {
    let res
    if (isMoralBehavior.value) {
      res = await createMoralPost({
        title: form.value.title,
        content: form.value.content,
        praisedStudentId: praisedStudentId.value,
        classId: moralClassId.value,
        categoryId: form.value.categoryId
      })
    } else if (isEdit.value) {
      res = await updatePost(route.params.id, {
        ...form.value,
        images: uploadedImages.value.length > 0 ? JSON.stringify(uploadedImages.value) : null
      })
    } else {
      res = await createPost({
        ...form.value,
        images: uploadedImages.value.length > 0 ? JSON.stringify(uploadedImages.value) : null
      })
    }
    if (res.code === 200) {
      ElMessage.success(isMoralBehavior.value ? '德育表扬发布成功' : isEdit.value ? '编辑成功' : '发帖成功')
      router.push('/bbs')
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } finally { submitting.value = false }
}

onMounted(() => {
  loadCategories()
  if (isEdit.value) loadPost()
})
useKeyboardFix()
</script>

<style scoped lang="scss">
.create-post-container { max-width: 800px; margin: 0 auto; padding: 0 var(--spacing-md, 16px); }

.bbs-top-nav {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  padding: 12px 0;
  .back-btn {
    font-size: var(--fs-md);
    color: var(--text-secondary);
    &:hover { color: var(--primary-color); }
  }
}

h2 { font-size: var(--fs-xl); margin: 0 0 24px; }
.upload-area { width: 100%; }

@media (max-width: 768px) {
  .bbs-top-nav { gap: 8px; margin-bottom: 12px; padding: 8px 0; .back-btn { font-size: var(--fs-sm); } }
  .create-post-container { padding: 0 var(--spacing-sm, 12px); }
  h2 { font-size: var(--fs-lg); }

  .create-post-page {
    height: calc(100dvh - 120px);
    display: flex;
    flex-direction: column;
  }
  .create-post-form {
    flex: 1;
    overflow-y: auto;
  }
  .create-post-footer {
    position: sticky;
    bottom: 0;
    background: var(--bg-card);
    padding: 12px 0;
    border-top: 1px solid var(--border-light);
  }
  .bbs-mobile-toolbar { display: flex; gap: 4px; padding: 4px 0 8px; border-bottom: 1px solid var(--border-light); margin-bottom: 8px; }
  .bbs-mobile-submit { position: fixed; bottom: 56px; left: 0; right: 0; padding: 12px 16px; background: var(--bg-card); border-top: 0.5px solid var(--border-color); z-index: 100; }
}
</style>

<template>
  <div class="editor-page">
    <el-page-header :content="isNew ? '新建文章' : '编辑文章'" @back="$router.back()" />
    <el-form :model="form" label-width="80px" style="margin-top:20px;">
      <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
      <el-form-item label="章节"><el-input v-model="form.chapter" /></el-form-item>
      <el-form-item label="任务"><el-input v-model="form.task" /></el-form-item>
      <el-form-item label="难度">
        <el-rate v-model="form.difficulty" :max="3" />
      </el-form-item>
      <el-form-item label="标签">
        <el-input v-model="tagsStr" placeholder="逗号分隔" @change="updateTags" />
      </el-form-item>
      <el-form-item label="内容（Markdown）">
        <el-input v-model="form.contentMd" type="textarea" :rows="15" />
      </el-form-item>
      <el-form-item label="记忆口诀"><el-input v-model="form.memoryTips" type="textarea" :rows="3" /></el-form-item>
      <el-form-item label="考试重点"><el-input v-model="form.examFocus" type="textarea" :rows="3" /></el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio value="DRAFT">草稿</el-radio>
          <el-radio value="PUBLISHED">发布</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
        <el-button @click="$router.back()">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAdminArticle, createArticle, updateArticle } from '@/api/knowledgeBase'

const route = useRoute()
const router = useRouter()
const isNew = computed(() => route.params.id === 'new' || !route.params.id)
const saving = ref(false)

const form = ref({ title: '', chapter: '', task: '', difficulty: 1, contentMd: '', memoryTips: '', examFocus: '', status: 'DRAFT', subjectId: 1 })
const tagsStr = ref('')

onMounted(async () => {
  if (!isNew.value) {
    try {
      const r = await getAdminArticle(route.params.id)
      const a = r.data?.article || r.data
      form.value = { ...a }
      if (a.tags) {
        try { tagsStr.value = JSON.parse(a.tags).join(',') } catch (e) { console.warn('Failed to parse tags:', e) }
      }
    } catch (e) { console.error('加载文章失败:', e) }
  }
})

function updateTags() {
  const arr = tagsStr.value.split(',').map(t => t.trim()).filter(Boolean)
  form.value.tags = JSON.stringify(arr)
}

async function save() {
  saving.value = true
  try {
    if (isNew.value) {
      await createArticle(form.value)
      ElMessage.success('创建成功')
    } else {
      await updateArticle(route.params.id, form.value)
      ElMessage.success('更新成功')
    }
    router.push('/knowledge-base/admin/articles')
  } catch (e) {
    console.error('保存失败:', e)
    ElMessage.error('保存失败，请重试')
  }
  finally { saving.value = false }
}
</script>

<style scoped>
.editor-page { max-width: 800px; margin: 0 auto; }
</style>

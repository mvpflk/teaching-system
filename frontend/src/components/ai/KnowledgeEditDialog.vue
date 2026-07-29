<template>
  <el-dialog
    v-model="visible"
    title="编辑知识库内容"
    width="700px"
    destroy-on-close
    @open="loadContent"
  >
    <div v-if="loading" v-loading="loading" style="min-height:200px"></div>
    <template v-else>
      <div class="ked-hint">支持 Markdown 格式，内容将作为 AI 生成教学产出的参考资料 · <kbd>Ctrl+S</kbd> 保存</div>

      <!-- 移动端 Tab 切换 -->
      <div v-if="isMobile" class="edit-tabs">
        <span :class="{ active: editTab === 'edit' }" @click="editTab = 'edit'">编辑</span>
        <span :class="{ active: editTab === 'preview' }" @click="editTab = 'preview'">预览</span>
      </div>

      <!-- 编辑器 -->
      <el-input
        v-show="!isMobile || editTab === 'edit'"
        v-model="content"
        type="textarea"
        :rows="16"
        placeholder="在此编辑或粘贴 Markdown 内容..."
        class="ked-editor"
        @keydown.ctrl.s.prevent="save"
      />

      <!-- 移动端预览 -->
      <div v-show="isMobile && editTab === 'preview'" class="ked-preview">
        <div v-if="!content" style="color:#999;text-align:center;padding:40px 0">暂无内容</div>
        <div v-else v-html="renderMarkdown(content)" />
      </div>
    </template>
    <template #footer>
      <!-- 移动端固定底部保存栏 -->
      <div v-if="isMobile" class="edit-bottom-bar" :style="{ paddingBottom: 'var(--safe-bottom)' }">
        <el-button
          type="primary"
          size="large"
          style="width:100%"
          @click="save"
        >
          保存
        </el-button>
      </div>
      <template v-else>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { getNodeContent, updateNodeContent } from '@/api/knowledgeNode'
import { ElMessage } from 'element-plus'
import { renderMarkdown } from '@/utils/markdown'
import { useIsMobile } from '@/composables/useIsMobile'
const { isMobile } = useIsMobile()
const editTab = ref('edit')

const props = defineProps({ categoryId: [Number, String] })
const emit = defineEmits(['saved'])

const visible = ref(false)
const loading = ref(false)
const saving = ref(false)
const content = ref('')

const loadContent = async () => {
  if (!props.categoryId) return
  loading.value = true
  try {
    const res = await getNodeContent(props.categoryId)
    if (res.code === 200) content.value = res.data?.content || ''
  } catch { /* */ } finally { loading.value = false }
}

const save = async () => {
  saving.value = true
  try {
    const res = await updateNodeContent(props.categoryId, content.value)
    if (res.code === 200) {
      ElMessage.success('已保存')
      visible.value = false
      emit('saved')
    } else { ElMessage.error(res.message || '保存失败') }
  } catch { ElMessage.error('请求失败') } finally { saving.value = false }
}

const open = () => { visible.value = true }
defineExpose({ open })
</script>

<style scoped>
.ked-hint { font-size: var(--fs-xs); color: #999; margin-bottom: 12px; }
.ked-hint kbd { background: #f0f2f5; border: 1px solid #d0d5dd; border-radius: 3px; padding: 1px 5px; font-size: var(--fs-xs); }
.ked-editor :deep(textarea) { font-family: 'JetBrains Mono', 'SF Mono', 'Cascadia Code', Consolas, monospace; font-size: var(--fs-sm); line-height: 1.7; }
.ked-preview { min-height: 200px; padding: 8px 0; font-size: var(--fs-md); line-height: 1.8; word-break: break-word; }
@media (max-width: 768px) {
  .edit-tabs { display: flex; border-bottom: 1px solid var(--border-light); margin-bottom: 12px; }
  .edit-tabs span { flex: 1; text-align: center; padding: 10px; font-size: var(--fs-md); cursor: pointer; color: var(--text-secondary); }
  .edit-tabs span.active { color: var(--primary-color); border-bottom: 2px solid var(--primary-color); font-weight: 500; }
  .edit-bottom-bar { position: fixed; bottom: 0; left: 0; right: 0; padding: 12px 16px; background: var(--bg-card); border-top: 0.5px solid var(--border-color); z-index: 100; }
}
</style>

<template>
  <div class="fb-wrap">
    <el-tooltip content="反馈 Bug / 建议" placement="left">
      <div class="fb-btn" @click="open">
        <svg
          width="20"
          height="20"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" />
        </svg>
      </div>
    </el-tooltip>

    <el-dialog
      v-model="visible"
      title="反馈 Bug / 建议"
      width="480px"
      destroy-on-close
    >
      <el-form label-width="80px" label-position="top" size="default">
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio-button value="BUG">Bug 反馈</el-radio-button>
            <el-radio-button value="SUGGESTION">功能建议</el-radio-button>
            <el-radio-button value="OTHER">其他</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input
            v-model="form.title"
            placeholder="简要概括问题或建议"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="详细描述">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="5"
            placeholder="详细描述你遇到的问题或建议内容..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="sending" @click="submitFeedback">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const visible = ref(false)
const sending = ref(false)
const form = reactive({ type: 'BUG', title: '', content: '' })

const open = () => {
  form.type = 'BUG'; form.title = ''; form.content = ''
  visible.value = true
}

const submitFeedback = async () => {
  if (!form.title.trim()) return ElMessage.warning('请填写标题')
  sending.value = true
  try {
    const res = await request({
      url: '/feedback', method: 'post',
      data: {
        type: form.type,
        title: form.title.trim(),
        content: form.content.trim(),
        pageUrl: window.location.href,
        browserInfo: navigator.userAgent
      }
    })
    if (res.code === 200) {
      ElMessage.success('感谢反馈，我们会尽快处理')
      visible.value = false
    }
  } catch { ElMessage.error('提交失败，请重试') } finally { sending.value = false }
}
</script>

<style scoped>
.fb-wrap { position: fixed; right: 20px; bottom: 88px; z-index: 1000; }
.fb-btn {
  width: 40px; height: 40px; border-radius: 50%;
  background: var(--bg-card, #fff); color: var(--text-secondary, #6b7a8a);
  display: flex; align-items: center; justify-content: center;
  cursor: pointer;
  border: 0.5px solid var(--border-light, #e0e3e8);
  transition: all 0.2s;
}
.fb-btn:hover {
  color: var(--primary-color, var(--primary-color));
  background: var(--primary-light, rgba(67,97,238,0.06));
  border-color: var(--primary-color, var(--primary-color));
}
.fb-btn:active { transform: scale(0.95); }
@media (max-width: 768px) { .fb-wrap { right: 8px; bottom: 70px; } }
</style>

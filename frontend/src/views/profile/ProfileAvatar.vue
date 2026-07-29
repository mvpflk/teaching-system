<template>
  <div class="avatar-wrapper" @click="triggerUpload">
    <el-avatar :size="100" :src="avatarUrl" class="user-avatar">
      {{ userName?.charAt(0) }}
    </el-avatar>
    <div class="avatar-overlay">
      <el-icon size="18"><Camera /></el-icon>
      <span>更换</span>
    </div>
  </div>
  <input
    ref="fileInput"
    type="file"
    accept="image/*"
    style="display:none"
    @change="handleUpload"
  />
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Camera } from '@element-plus/icons-vue'
import { uploadAvatar } from '@/api/profile'

const props = defineProps({
  avatarUrl: { type: String, default: '' },
  userName: { type: String, default: '' }
})
const emit = defineEmits(['updated'])

const fileInput = ref(null)

const triggerUpload = () => fileInput.value?.click()

const handleUpload = async (e) => {
  const file = e.target.files[0]
  if (!file) return
  const formData = new FormData()
  formData.append('file', file)
  try {
    const res = await uploadAvatar(formData)
    if (res.code === 200) {
      emit('updated', res.data.avatarUrl + '?t=' + Date.now())
      ElMessage.success('头像已更新')
    }
  } catch { ElMessage.error('上传失败') }
  e.target.value = ''
}
</script>

<style scoped lang="scss">
.avatar-wrapper {
  display: inline-block;
  position: relative;
  cursor: pointer;
  border-radius: var(--radius-full);

  .user-avatar {
    border: 3px solid var(--bg-card);
    background: var(--primary-light);
    color: var(--primary-color);
    font-weight: 500;
  }

  .avatar-overlay {
    position: absolute;
    inset: 0;
    border-radius: var(--radius-full);
    background: rgba(0,0,0,0.45);
    color: var(--bg-card);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 2px;
    opacity: 0;
    transition: opacity 0.2s;
    font-size: var(--fs-xs);
  }
  &:hover .avatar-overlay { opacity: 1; }
}

@media (max-width: 768px) {
  :deep(.el-form-item .el-input),
  :deep(.el-form-item .el-select) {
    width: 100%;
  }
}
</style>

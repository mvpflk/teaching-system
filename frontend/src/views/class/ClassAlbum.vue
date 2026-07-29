<template>
  <div class="album-page">
    <div class="album-header">
      <h4 class="album-title">📸 班级相册</h4>
      <div style="display:flex;gap:8px">
        <el-button
          v-if="isTeacher"
          size="small"
          :type="tab === 'pending' ? 'warning' : ''"
          @click="tab = 'pending'"
        >
          待审核{{ pendingCount > 0 ? ` (${pendingCount})` : '' }}
        </el-button>
        <el-button size="small" :type="tab === 'all' ? 'primary' : ''" @click="tab = 'all'">全部照片</el-button>
        <el-button type="primary" size="small" @click="showUpload = true">
          <el-icon><Plus /></el-icon> 上传照片
        </el-button>
      </div>
    </div>

    <!-- 图片网格 -->
    <div v-if="photos.length" class="album-grid">
      <div
        v-for="p in photos"
        :key="p.id"
        class="album-card"
        @click="previewPhoto = p"
      >
        <img
          :src="p.imageUrl"
          :alt="cleanCaption(p.caption)"
          class="album-img"
          loading="lazy"
        />
        <div v-if="p.status === 'PENDING'" class="status-badge pending">待审核</div>
        <div v-if="p.status === 'REJECTED'" class="status-badge rejected">已拒绝</div>
        <div class="album-card-footer">
          <span class="caption-text">{{ cleanCaption(p.caption) || '无描述' }}</span>
          <span class="like-area" :class="{ liked: p.likedByCurrentUser }" @click.stop="handleLike(p)">
            {{ p.likedByCurrentUser ? '❤️' : '🤍' }} {{ p.likeCount || 0 }}
          </span>
        </div>
        <div v-if="isTeacher && p.status === 'PENDING'" class="review-actions" @click.stop>
          <el-button size="small" type="success" @click="handleReview(p.id, 'approve')">通过</el-button>
          <el-button size="small" type="danger" @click="handleReview(p.id, 'reject')">拒绝</el-button>
        </div>
        <div v-if="isTeacher && p.status !== 'PENDING'" class="review-actions" @click.stop>
          <el-button
            size="small"
            type="danger"
            plain
            @click="handleDelete(p.id)"
          >
            删除
          </el-button>
        </div>
      </div>
    </div>
    <el-empty v-else description="暂无照片" :image-size="80" />

    <el-pagination
      v-if="total > pageSize"
      v-model:current-page="page"
      :page-size="pageSize"
      :total="total"
      layout="prev, pager, next"
      small
      class="album-pager"
      @current-change="loadPhotos"
    />

    <!-- 上传对话框 -->
    <el-dialog
      v-model="showUpload"
      title="上传照片"
      width="400px"
      append-to-body
    >
      <el-form label-position="top">
        <el-form-item label="选择图片">
          <el-upload
            :auto-upload="false"
            :limit="1"
            accept="image/*"
            :on-change="onFileChange"
            drag
          >
            <el-icon class="upload-icon"><Plus /></el-icon>
            <div class="upload-text">点击或拖拽上传</div>
          </el-upload>
        </el-form-item>
        <el-form-item label="描述（可选）">
          <el-input v-model="uploadCaption" placeholder="照片描述..." maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUpload = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">上传</el-button>
      </template>
    </el-dialog>

    <!-- 预览对话框 -->
    <el-dialog
      :model-value="!!previewPhoto"
      :title="cleanCaption(previewPhoto?.caption)"
      width="600px"
      append-to-body
      @update:model-value="(v) => { if (!v) previewPhoto = null }"
    >
      <img v-if="previewPhoto" :src="previewPhoto.imageUrl" style="width:100%;border-radius:8px" />
      <div style="margin-top:12px;display:flex;align-items:center;gap:12px">
        <el-button size="small" :disabled="previewPhoto?.likedByCurrentUser" @click="handleLike(previewPhoto)">
          {{ previewPhoto?.likedByCurrentUser ? '❤️' : '🤍' }} {{ previewPhoto?.likeCount || 0 }}
        </el-button>
      </div>
      <div v-if="previewPhoto" style="margin-top:16px">
        <div v-for="c in currentComments" :key="c.id" class="comment-item">
          <span class="comment-user">{{ maskName(c.username) || '用户' + (c.userId || '').toString().slice(-4) }}</span>
          <span class="comment-text">{{ c.content }}</span>
          <span class="comment-time">{{ c.createdAt?.substring(0,16) }}</span>
        </div>
        <div style="display:flex;gap:8px;margin-top:8px">
          <el-input
            v-model="commentText"
            placeholder="写评论..."
            size="small"
            @keyup.enter="handleComment"
          />
          <el-button size="small" type="primary" @click="handleComment">发送</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getAlbumPhotos, uploadPhoto, likePhoto, commentPhoto, getComments, reviewPhoto, getPendingPhotos, deletePhoto } from '@/api/classAlbum'

const props = defineProps({ classId: { type: [Number, String], required: true } })
const userStore = useUserStore()
const isTeacher = userStore.isTeacher || userStore.isAdmin || userStore.isSuperAdmin

const photos = ref([])
const page = ref(1)
const pageSize = 12
const total = ref(0)
const showUpload = ref(false)
const uploading = ref(false)
const uploadCaption = ref('')
const pendingFile = ref(null)
const previewPhoto = ref(null)
const commentText = ref('')
const currentComments = ref([])
const tab = ref('all')
const pendingCount = ref(0)

const loadPhotos = async () => {
  try {
    let r
    if (tab.value === 'pending' && isTeacher) {
      r = await getPendingPhotos()
      if (r.code === 200) {
        photos.value = (r.data || []).map(p => ({ ...p, likedByCurrentUser: p.likedByCurrentUser ?? false }))
        total.value = photos.value.length
        pendingCount.value = photos.value.length
      }
    } else {
      r = await getAlbumPhotos(props.classId, page.value, pageSize)
      if (r.code === 200) {
        photos.value = (r.data.records || []).map(p => ({ ...p, likedByCurrentUser: p.likedByCurrentUser ?? false }))
        total.value = r.data.total || 0
      }
    }
  } catch { /* */ }
}

const handleReview = async (id, action) => {
  try {
    const r = await reviewPhoto(id, action)
    if (r.code === 200) {
      ElMessage.success(action === 'approve' ? '已通过' : '已拒绝')
      loadPhotos()
    } else ElMessage.error(r.message || '操作失败')
  } catch { ElMessage.error('操作失败') }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这张照片吗？', '确认删除', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
    const r = await deletePhoto(id)
    if (r.code === 200) {
      ElMessage.success('已删除')
      loadPhotos()
    } else ElMessage.error(r.message || '删除失败')
  } catch { /* 取消 */ }
}

const handleLike = async (p) => {
  if (!p) return
  if (p.likedByCurrentUser) { ElMessage.warning('已经点过赞了'); return }
  try {
    const r = await likePhoto(p.id)
    if (r.code === 200) {
      p.likeCount = r.data.likeCount
      p.likedByCurrentUser = true
    } else if (r.code === 409) {
      ElMessage.warning(r.message || '已经点过赞了')
    }
  } catch { /* */ }
}

const onFileChange = (file) => { pendingFile.value = file.raw }
const cleanCaption = (caption) => {
  if (!caption) return ''
  const idx = caption.indexOf('|uploader:')
  return idx >= 0 ? caption.substring(0, idx) : caption
}

const handleUpload = async () => {
  if (!pendingFile.value) { ElMessage.warning('请选择图片'); return }
  uploading.value = true
  try {
    const r = await uploadPhoto(pendingFile.value, props.classId, uploadCaption.value)
    if (r.code === 200) {
      ElMessage.success('上传成功')
      showUpload.value = false; uploadCaption.value = ''; pendingFile.value = null
      page.value = 1; loadPhotos()
    } else ElMessage.error(r.message || '上传失败')
  } catch { ElMessage.error('上传失败') }
  finally { uploading.value = false }
}

const handleComment = async () => {
  if (!commentText.value.trim() || !previewPhoto.value) return
  try {
    await commentPhoto(previewPhoto.value.id, commentText.value.trim())
    commentText.value = ''
    await loadComments(previewPhoto.value.id)
  } catch { /* */ }
}

const maskName = (name) => {
  if (!name) return ''
  return name[0] + '*'.repeat(Math.max(1, name.length - 1))
}

const loadComments = async (photoId) => {
  try {
    const r = await getComments(photoId)
    if (r.code === 200) currentComments.value = r.data || []
  } catch { currentComments.value = [] }
}

const previewWatcher = ref(false)
import { watch } from 'vue'
watch(() => previewPhoto.value, (v) => {
  if (v) { currentComments.value = []; commentText.value = ''; loadComments(v.id) }
})
watch(tab, () => { page.value = 1; loadPhotos() })

onMounted(async () => {
  loadPhotos()
  if (isTeacher) {
    try {
      const r = await getPendingPhotos()
      if (r.code === 200) pendingCount.value = (r.data || []).length
    } catch {}
  }
})
</script>

<style scoped>
.album-page { padding: 8px 0; }
.album-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.album-title { font-size: var(--fs-lg); font-weight: 600; margin: 0; }
.album-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 12px; }
.album-card { border-radius: var(--radius-md); overflow: hidden; background: var(--bg-card); border: 1px solid var(--border-light); cursor: pointer; transition: transform 0.2s; }
.album-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px var(--border-color); }
.album-img { width: 100%; height: 160px; object-fit: cover; display: block; }
.album-card-footer { display: flex; justify-content: space-between; align-items: center; padding: 8px 10px; font-size: var(--fs-sm); }
.caption-text { color: var(--text-regular); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex:1; }
.like-area { cursor: pointer; color: var(--text-secondary); white-space: nowrap; font-size: var(--fs-xs); }
.like-area:hover { color: var(--el-color-danger); }
.like-area.liked { color: var(--el-color-danger); }
.album-pager { margin-top: 16px; justify-content: center; }
.upload-icon { font-size: 28px; color: var(--text-secondary); }
.upload-text { font-size: var(--fs-md); color: var(--text-regular); margin-top: 8px; }
.comment-item { padding: 6px 0; border-bottom: 1px solid var(--border-light); font-size: var(--fs-sm); }
.comment-user { font-weight: 500; color: var(--primary-color); margin-right: 8px; }
.comment-text { color: var(--text-regular); }
.comment-time { float: right; color: var(--text-secondary); font-size: var(--fs-xs); }

.status-badge { position: absolute; top: 6px; right: 6px; padding: 2px 8px; border-radius: var(--radius-xs); font-size: var(--fs-xs); font-weight: 600; color: #fff; }
.status-badge.pending { background: var(--el-color-warning); }
.status-badge.rejected { background: var(--el-color-danger); }
.review-actions { display: flex; gap: 6px; padding: 6px 10px; border-top: 1px solid var(--border-light); }
.album-card { position: relative; }

@media (max-width: 768px) {
  .album-grid { grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: 8px; }
  .album-img { height: 120px; }
}
</style>

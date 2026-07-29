<template>
  <transition name="panel-slide">
    <div v-if="visible" class="detail-overlay" @click.self="$emit('close')">
      <div class="detail-panel">
        <div class="panel-header">
          <h3>{{ detail?.title || '作品详情' }}</h3>
          <el-button
            :icon="Close"
            text
            circle
            @click="$emit('close')"
          />
        </div>
        <div class="panel-body">
          <el-skeleton :loading="loading" animated :count="3">
            <template #default>
              <div v-if="detail">
                <div class="detail-meta">
                  <span
                    class="detail-type"
                    :class="'type-' + (detail.sourceType || '').toLowerCase()"
                  >
                    {{ sourceTypeLabel(detail.sourceType) }}
                  </span>
                  <span v-if="detail.subject" class="detail-subject">{{ detail.subject }}</span>
                  <span v-if="detail.showScope" class="detail-scope">
                    <el-icon :size="14"><component :is="scopeIcon(detail.showScope)" /></el-icon>
                    {{ scopeLabel(detail.showScope) }}
                  </span>
                </div>

                <el-descriptions
                  :column="isMobile ? 1 : 2"
                  border
                  size="small"
                  class="detail-desc"
                >
                  <el-descriptions-item label="作者" :span="1">
                    {{
                      detail.studentName || '未知'
                    }}
                  </el-descriptions-item>
                  <el-descriptions-item label="班级" :span="1">
                    {{
                      (detail.grade || '') + (detail.className || '') || '-'
                    }}
                  </el-descriptions-item>
                  <el-descriptions-item label="推荐教师" :span="1">
                    {{
                      detail.teacherName || '未知'
                    }}
                  </el-descriptions-item>
                  <el-descriptions-item label="推荐时间" :span="1">
                    {{
                      formatTime(detail.createTime)
                    }}
                  </el-descriptions-item>
                  <el-descriptions-item label="展示范围" :span="1">
                    {{
                      scopeLabel(detail.showScope)
                    }}
                  </el-descriptions-item>
                  <el-descriptions-item label="获得积分" :span="1">
                    <span class="detail-credit">+{{ detail.creditAwarded || 0 }}</span>
                  </el-descriptions-item>
                  <el-descriptions-item label="推荐语" :span="2">
                    <span style="white-space: pre-wrap; line-height: 1.6">{{
                      detail.teacherComment || '暂无推荐语'
                    }}</span>
                  </el-descriptions-item>
                  <el-descriptions-item v-if="detail.submissionScore" label="得分" :span="1">
                    <span class="detail-score">{{ detail.submissionScore }}</span>
                  </el-descriptions-item>
                </el-descriptions>

                <div
                  v-if="detail.submissionContent || submissionFiles.length"
                  class="submission-detail"
                >
                  <div class="submission-label">学生提交内容</div>
                  <div v-if="detail.submissionContent" class="submission-content">
                    {{ detail.submissionContent }}
                  </div>
                  <FilePreview
                    v-for="(url, i) in submissionFiles"
                    :key="'f' + i"
                    :src="url"
                    :filename="getFileName(url)"
                    class="submission-file"
                  />
                </div>

                <div class="detail-actions">
                  <el-button
                    :type="liked ? 'primary' : 'default'"
                    :icon="liked ? StarFilled : Star"
                    size="default"
                    @click="handleLike"
                  >
                    {{ likeCount }}
                  </el-button>
                  <HonorPoster :work="detail" />
                </div>

                <div class="comment-section">
                  <h4>评论 ({{ comments.length }})</h4>
                  <div v-if="comments.length === 0" class="comment-empty">暂无评论</div>
                  <div v-for="c in comments" :key="c.id" class="comment-item">
                    <el-avatar :size="28" class="comment-avatar">
                      {{
                        (c.userName || '?')[0]
                      }}
                    </el-avatar>
                    <div class="comment-body">
                      <div class="comment-top">
                        <span class="comment-name">{{ c.userName }}</span>
                        <span class="comment-time">{{ formatTime(c.createdAt) }}</span>
                      </div>
                      <p class="comment-text">{{ c.content }}</p>
                    </div>
                  </div>
                  <div class="comment-input">
                    <el-input
                      v-model="newComment"
                      placeholder="写下你的评论..."
                      :rows="2"
                      type="textarea"
                      maxlength="500"
                      show-word-limit
                    />
                    <el-button
                      type="primary"
                      size="small"
                      :loading="submittingComment"
                      :disabled="!newComment.trim()"
                      @click="submitComment"
                    >
                      发表
                    </el-button>
                  </div>
                </div>
              </div>
            </template>
          </el-skeleton>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { useIsMobile } from '@/composables/useIsMobile';
import { Close, StarFilled, Star, School, Connection, User } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import dayjs from 'dayjs';
import { toggleLike, getComments, addComment } from '@/api/showcase';
import { SOURCE_TYPE_LABEL } from '@/constants/taskType';
import FilePreview from '@/components/renderers/FilePreview.vue';
import HonorPoster from './HonorPoster.vue';

const props = defineProps({
  visible: { type: Boolean, default: false },
  detail: { type: Object, default: null },
  loading: { type: Boolean, default: false },
});

defineEmits(['close']);
const { isMobile } = useIsMobile();

const sourceTypeLabel = (t) => SOURCE_TYPE_LABEL[t] || t;
const scopeLabel = (s) => ({ CLASS: '班级', MULTI_CLASS: '跨班', SCHOOL: '全校' })[s] || s;
const scopeIcon = (s) => {
  const map = { SCHOOL: School, MULTI_CLASS: Connection, CLASS: User };
  return map[s] || School;
};
const formatTime = (t) => (t ? dayjs(t).format('MM-DD HH:mm') : '');

const submissionFiles = computed(() => {
  try {
    return JSON.parse(props.detail?.submissionAttachments || '[]');
  } catch {
    return [];
  }
});
const getFileName = (url) => {
  const p = (url || '').split('/');
  return p[p.length - 1] || '文件';
};

const liked = ref(false);
const likeCount = ref(0);
watch(
  () => props.detail,
  (d) => {
    if (d) {
      likeCount.value = d.likeCount || 0;
      liked.value = false;
    }
  }
);

const handleLike = async () => {
  if (!props.detail) return;
  try {
    const res = await toggleLike(props.detail.id);
    if (res.code === 200) {
      liked.value = res.data.liked;
      likeCount.value = res.data.likeCount;
    }
  } catch {
    /* */
  }
};

const comments = ref([]);
const newComment = ref('');
const submittingComment = ref(false);

const submitComment = async () => {
  if (!newComment.value.trim() || !props.detail) return;
  submittingComment.value = true;
  try {
    const res = await addComment(props.detail.id, newComment.value.trim());
    if (res.code === 200) {
      ElMessage.success('评论已提交' + (res.data.status === 'PENDING' ? '，等待审核' : ''));
      newComment.value = '';
      const cr = await getComments(props.detail.id);
      if (cr.code === 200) comments.value = cr.data || [];
    }
  } catch {
    /* */
  } finally {
    submittingComment.value = false;
  }
};

watch(
  () => props.detail?.id,
  async (id) => {
    if (!id) {
      comments.value = [];
      return;
    }
    try {
      const cr = await getComments(id);
      if (cr.code === 200) comments.value = cr.data || [];
    } catch {
      comments.value = [];
    }
  }
);
</script>

<style scoped>
.detail-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  z-index: 2000;
  display: flex;
  justify-content: flex-end;
}
.detail-panel {
  width: 560px;
  max-width: 100vw;
  height: 100%;
  background: var(--bg-white, #fff);
  display: flex;
  flex-direction: column;
  box-shadow: -4px 0 30px rgba(0, 0, 0, 0.12);
}
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-light);
  flex-shrink: 0;
}
.panel-header h3 {
  margin: 0;
  font-size: var(--fs-lg);
  font-weight: 600;
}
.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}
.detail-meta {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.detail-type {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: var(--fs-xs);
  font-weight: 500;
}
.detail-type.type-homework {
  background: #e8f5e9;
  color: var(--el-color-success);
}
.detail-type.type-exam {
  background: #fce4ec;
  color: #c62828;
}
.detail-type.type-practical {
  background: #e3f2fd;
  color: #1565c0;
}
.detail-type.type-task {
  background: #fff3e0;
  color: #e65100;
}
.detail-subject {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
}
.detail-scope {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: auto;
}
.detail-desc {
  margin-bottom: 16px;
}
.detail-credit {
  font-weight: 600;
  color: var(--el-color-success);
}
.detail-score {
  font-weight: 600;
  color: var(--primary-color);
  font-size: var(--fs-lg);
}
.submission-detail {
  margin: 16px 0;
  padding: 12px;
  background: var(--bg-section);
  border-radius: var(--radius-md);
}
.submission-label {
  font-size: var(--fs-xs);
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 8px;
}
.submission-content {
  padding: 10px;
  background: var(--bg-white);
  border-radius: 6px;
  white-space: pre-wrap;
  line-height: 1.6;
  margin-bottom: 8px;
}
.submission-file {
  margin-bottom: 8px;
}
.detail-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 16px 0;
}
.comment-section {
  margin-top: 20px;
  border-top: 1px solid var(--border-light);
  padding-top: 16px;
}
.comment-section h4 {
  font-size: var(--fs-sm);
  font-weight: 600;
  margin: 0 0 12px;
}
.comment-empty {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
}
.comment-item {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}
.comment-avatar {
  flex-shrink: 0;
}
.comment-body {
  flex: 1;
}
.comment-top {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}
.comment-name {
  font-size: var(--fs-sm);
  font-weight: 500;
}
.comment-time {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}
.comment-text {
  font-size: var(--fs-sm);
  color: var(--text-regular);
  margin: 0;
  line-height: 1.5;
}
.comment-input {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  margin-top: 12px;
}
.comment-input .el-textarea {
  flex: 1;
}
.comment-input .el-button {
  flex-shrink: 0;
  margin-top: 4px;
}

/* 移动端全屏沉浸式适配 */
@media (max-width: 768px) {
  .detail-panel { width: 100vw; }
  .panel-header { padding: 12px 16px; }
  .panel-header h3 { font-size: var(--fs-md); }
  .panel-body { padding: 16px; }
  .detail-actions {
    position: sticky; bottom: 0; background: var(--bg-card, #fff);
    padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
    margin: 12px -16px 0; border-top: 1px solid var(--border-light);
    z-index: 10;
  }
  .comment-input { flex-direction: column; align-items: stretch; }
  .comment-input .el-button { margin-top: 0; align-self: flex-end; }
  .submission-content { max-height: none; }
}
</style>

<template>
  <el-skeleton :loading="loading" animated :count="6">
    <template #default>
      <div v-if="works.length === 0" class="empty-state">
        <el-icon :size="48"><PictureFilled /></el-icon>
        <p>暂无展示作品</p>
      </div>
      <div v-else class="masonry-grid">
        <div
          v-for="work in works"
          :key="work.id"
          class="masonry-item"
          @click="emit('open-detail', work)"
        >
          <div class="showcase-card">
            <!-- 封面图 -->
            <div class="card-cover">
              <img
                v-if="work.firstImageUrl"
                :src="work.firstImageUrl"
                :alt="work.title"
                class="cover-img"
                loading="lazy"
              />
              <div v-else class="cover-placeholder" :style="{ background: subjectColor(work.subject) }">
                <span class="placeholder-char">{{ (work.subject || work.title || '?')[0] }}</span>
              </div>
            </div>
            <!-- 类型标签+学科 -->
            <div class="card-header">
              <span class="card-type-tag" :class="'type-' + (work.sourceType || '').toLowerCase()">
                {{ sourceTypeLabel(work.sourceType) }}
              </span>
              <span v-if="work.subject" class="card-subject">{{ work.subject }}</span>
            </div>
            <!-- 标题 -->
            <h4 class="card-title">{{ work.title }}</h4>
            <!-- 内容摘要 -->
            <p v-if="work.submissionContent" class="card-summary">{{ work.submissionContent }}</p>
            <!-- 底部：学生+头像+点赞+评论+时间 -->
            <div class="card-footer">
              <div class="footer-student">
                <div class="student-avatar">{{ (work.studentName || '?')[0] }}</div>
                <span class="student-name">{{ work.studentName || '未知' }}</span>
              </div>
              <div class="footer-stats">
                <span class="stat-item">
                  <el-icon :size="14"><StarFilled /></el-icon>
                  {{ work.likeCount || 0 }}
                </span>
                <span class="stat-item">
                  <el-icon :size="14"><ChatDotSquare /></el-icon>
                  {{ work.commentCount || 0 }}
                </span>
                <span class="stat-time">{{ formatTime(work.createTime) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </el-skeleton>
</template>

<script setup>
import { PictureFilled, StarFilled, ChatDotSquare } from '@element-plus/icons-vue'

const props = defineProps({
  works: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})
const emit = defineEmits(['open-detail'])

import { SOURCE_TYPE_LABEL } from '@/constants/taskType'
import { elInfo } from '@/utils/theme'
const sourceTypeLabel = (t) => SOURCE_TYPE_LABEL[t] || t

const subjectColors = [
  'var(--el-color-primary, #409eff)', 'var(--el-color-success, #67c23a)', 'var(--el-color-warning, #e6a23c)', 'var(--el-color-danger, #f56c6c)', elInfo,
  '#b37feb', '#36cfc9', '#597ef7', '#ff7a45', '#73d13d'
]
const subjectColorMap = {}
const subjectColor = (subject) => {
  if (!subject) return subjectColors[0]
  if (!subjectColorMap[subject]) {
    let hash = 0
    for (let i = 0; i < subject.length; i++) hash = subject.charCodeAt(i) + ((hash << 5) - hash)
    subjectColorMap[subject] = subjectColors[Math.abs(hash) % subjectColors.length]
  }
  return subjectColorMap[subject]
}

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${mm}-${dd} ${hh}:${min}`
}
</script>

<style scoped lang="scss">
.masonry-grid {
  column-count: 4;
  column-gap: 16px;
}

@media (max-width: 1200px) {
  .masonry-grid { column-count: 3; }
}
@media (max-width: 768px) {
  .masonry-grid { column-count: 2; }
}
@media (max-width: 480px) {
  .masonry-grid { column-count: 1; }
}

.masonry-item {
  break-inside: avoid;
  margin-bottom: 16px;
}

.showcase-card {
  background: var(--bg-card, #fff);
  border-radius: var(--radius-lg, 12px);
  overflow: hidden;
  box-shadow: var(--shadow-sm, 0 1px 3px rgba(0,0,0,.08));
  display: flex;
  flex-direction: column;
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease;

  &:hover {
    transform: translateY(-4px);
    box-shadow: var(--shadow-lg, 0 8px 24px rgba(0,0,0,.12));
  }
}

/* 封面 */
.card-cover {
  width: 100%;
  overflow: hidden;

  .cover-img {
    width: 100%;
    height: auto;
    max-height: 200px;
    object-fit: cover;
    display: block;
    transition: transform 0.3s ease;
  }

  .showcase-card:hover .cover-img {
    transform: scale(1.03);
  }

  .cover-placeholder {
    width: 100%;
    height: 100px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .placeholder-char {
    font-size: 36px;
    font-weight: 700;
    color: rgba(255, 255, 255, 0.85);
  }
}

/* 类型标签+学科 */
.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px 0;

  .card-type-tag {
    font-size: var(--fs-xs);
    padding: 2px 8px;
    border-radius: var(--radius-xs, 4px);
    font-weight: 500;

    &.type-homework { background: var(--primary-light, #e8f0fe); color: var(--primary-color, #409eff); }
    &.type-exam { background: var(--bg-warning-light, #fef0e5); color: var(--warning-color, #e6a23c); }
    &.type-practical { background: var(--bg-success-light, #e8f8e8); color: var(--success-color, #67c23a); }
    &.type-task { background: var(--bg-primary-light, #e8f0fe); color: var(--primary-color, #409eff); }
  }

  .card-subject {
    font-size: var(--fs-xs);
    color: var(--text-secondary, var(--el-color-info));
  }
}

/* 标题 */
.card-title {
  font-size: var(--fs-lg);
  font-weight: 600;
  color: var(--text-primary, #303133);
  margin: 8px 14px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 内容摘要 */
.card-summary {
  font-size: var(--fs-sm);
  color: var(--text-secondary, var(--el-color-info));
  margin: 6px 14px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.5;
}

/* 底部 */
.card-footer {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 10px 14px 12px;
  margin-top: 10px;
  border-top: 1px solid var(--border-light, #ebeef5);
}

.footer-student {
  display: flex;
  align-items: center;
  gap: 8px;

  .student-avatar {
    width: 24px;
    height: 24px;
    border-radius: 50%;
    background: var(--primary-color, #409eff);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: var(--fs-xs);
    font-weight: 600;
    flex-shrink: 0;
  }

  .student-name {
    font-size: var(--fs-sm);
    color: var(--text-primary, #303133);
    font-weight: 500;
  }
}

.footer-stats {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: var(--fs-xs);
  color: var(--text-secondary, var(--el-color-info));

  .stat-item {
    display: flex;
    align-items: center;
    gap: 3px;
  }

  .stat-time {
    margin-left: auto;
    white-space: nowrap;
  }
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--text-secondary, var(--el-color-info));
  column-span: all;

  p {
    font-size: var(--fs-base, 14px);
    margin-top: 12px;
  }
}
</style>

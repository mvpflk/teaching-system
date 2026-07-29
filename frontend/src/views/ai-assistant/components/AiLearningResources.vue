<template>
  <el-card v-if="nodeId" shadow="never" class="aia-card lr-card">
    <template #header>
      <div class="aia-card-title">
        <el-icon class="lr-title-icon"><Reading /></el-icon>
        学习资源 · {{ nodeLabel }}
        <el-tag
          v-if="lrStatus === 'PENDING'"
          size="small"
          type="warning"
          effect="plain"
          class="lr-status-tag"
        >
          待审核
        </el-tag>
        <el-tag
          v-else-if="lrStatus === 'APPROVED'"
          size="small"
          type="success"
          effect="plain"
          class="lr-status-tag"
        >
          已通过
        </el-tag>
        <el-tag
          v-else-if="lrStatus === 'REJECTED'"
          size="small"
          type="danger"
          effect="plain"
          class="lr-status-tag"
        >
          已拒绝
        </el-tag>
        <span v-else class="lr-status-none">尚未生成</span>
      </div>
    </template>
    <div class="lr-body">
      <p class="lr-desc">
        为「{{ nodeLabel }}」生成配套学习资源：视频推荐、例题解析、练习题、常见误区提醒
      </p>
      <div class="lr-actions">
        <el-button
          type="primary"
          :loading="lrGenerating"
          :disabled="lrStatus === 'PENDING'"
          @click="genLr"
        >
          <el-icon><MagicStick /></el-icon> {{ lrStatus ? '重新生成' : 'AI 生成学习资源' }}
        </el-button>
        <el-button
          v-if="lrStatus === 'PENDING' && isAdmin"
          size="small"
          type="success"
          plain
          @click="reviewLr('APPROVED')"
        >
          <el-icon><Check /></el-icon> 通过
        </el-button>
        <el-button
          v-if="lrStatus === 'PENDING' && isAdmin"
          size="small"
          type="danger"
          plain
          @click="reviewDialogVisible = true"
        >
          <el-icon><Close /></el-icon> 拒绝
        </el-button>
      </div>
      <div v-if="lrData" class="lr-preview">
        <el-tabs v-model="lrTab" class="lr-tabs">
          <el-tab-pane name="videos">
            <template #label>
              <span class="lr-tab-label"><el-icon><VideoCamera /></el-icon> 视频</span>
            </template>
            <div v-if="!(lrData.videoUrls || []).length" class="lr-tab-empty">暂无视频资源</div>
            <div v-for="(v, i) in lrData.videoUrls || []" :key="i" class="lr-video-item">
              <el-tag size="small" :type="v.platform === 'bilibili' ? 'danger' : 'info'">
                {{ v.platform === 'bilibili' ? 'B站' : v.platform }}
              </el-tag>
              <a :href="v.url" target="_blank" class="lr-video-link">{{ v.title }}</a>
            </div>
          </el-tab-pane>
          <el-tab-pane name="examples">
            <template #label>
              <span class="lr-tab-label"><el-icon><EditPen /></el-icon> 例题</span>
            </template>
            <div v-if="!(lrData.examples || []).length" class="lr-tab-empty">暂无例题</div>
            <div v-for="(e, i) in lrData.examples || []" :key="i" class="lr-item">
              <p class="lr-q">
                <strong>例题{{ i + 1 }}：</strong><span v-html="renderMath(e.question)" />
              </p>
              <el-collapse>
                <el-collapse-item title="答案与解析">
                  <p><strong>答案：</strong><span v-html="renderMath(e.answer)" /></p>
                  <p><strong>解析：</strong><span v-html="renderMath(e.explanation)" /></p>
                </el-collapse-item>
              </el-collapse>
            </div>
          </el-tab-pane>
          <el-tab-pane name="practices">
            <template #label>
              <span class="lr-tab-label"><el-icon><Document /></el-icon> 练习</span>
            </template>
            <div v-if="!(lrData.practices || []).length" class="lr-tab-empty">暂无练习题</div>
            <div v-for="(p, i) in lrData.practices || []" :key="i" class="lr-item">
              <p class="lr-q">
                <el-tag
                  size="small"
                  :type="p.difficulty === 1 ? 'success' : p.difficulty === 2 ? 'warning' : 'danger'"
                >
                  {{ ['', '基础', '进阶', '挑战'][p.difficulty] }}
                </el-tag>
                <strong>练习{{ i + 1 }}：</strong><span v-html="renderMath(p.question)" />
              </p>
              <el-collapse>
                <el-collapse-item title="答案">
                  <p v-html="renderMath(p.answer)" />
                </el-collapse-item>
              </el-collapse>
            </div>
          </el-tab-pane>
          <el-tab-pane name="mistakes">
            <template #label>
              <span class="lr-tab-label"><el-icon><WarningFilled /></el-icon> 误区</span>
            </template>
            <div class="lr-text">{{ lrData.commonMistakes || '暂无' }}</div>
          </el-tab-pane>
          <el-tab-pane name="tips">
            <template #label>
              <span class="lr-tab-label"><el-icon><Sunny /></el-icon> 建议</span>
            </template>
            <div class="lr-text">{{ lrData.studyTips || '暂无' }}</div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </el-card>

  <el-dialog
    v-model="reviewDialogVisible"
    title="拒绝学习资源"
    width="400px"
    append-to-body
  >
    <el-input
      v-model="reviewReason"
      type="textarea"
      :rows="3"
      placeholder="拒绝原因（学生可见）"
    />
    <template #footer>
      <el-button @click="reviewDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="reviewLr('REJECTED')">确认拒绝</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import {
  MagicStick,
  Check,
  Close,
  Reading,
  VideoCamera,
  EditPen,
  Document,
  WarningFilled,
  Sunny,
} from '@element-plus/icons-vue';
import { renderMath } from '@/composables/useQuestionHelpers';
import { generateResources, reviewResource, getNodeLearningResources } from '@/api/knowledgeNode';

const props = defineProps({
  nodeId: { type: [Number, String], default: null },
  nodeLabel: { type: String, default: '' },
  isAdmin: { type: Boolean, default: false },
});

const lrStatus = ref(null);
const lrData = ref(null);
const lrGenerating = ref(false);
const lrTab = ref('videos');
const reviewDialogVisible = ref(false);
const reviewReason = ref('');

watch(
  () => props.nodeId,
  async (nodeId) => {
    lrStatus.value = null;
    lrData.value = null;
    if (!nodeId) return;
    try {
      const r = await getNodeLearningResources(nodeId);
      if (r.code === 200 && r.data) {
        lrStatus.value = r.data.resourceStatus || null;
        if (r.data.learningResources) lrData.value = r.data.learningResources;
      }
    } catch {
      /* */
    }
  },
  { immediate: true }
);

const genLr = async () => {
  if (!props.nodeId) return;
  lrGenerating.value = true;
  try {
    const r = await generateResources(props.nodeId);
    if (r.code === 200) {
      ElMessage.success('学习资源已生成，请等待审核');
      lrStatus.value = 'PENDING';
      lrData.value = r.data?.learningResources || null;
    } else {
      ElMessage.error(r.message || '生成失败');
    }
  } catch {
    ElMessage.error('生成失败');
  } finally {
    lrGenerating.value = false;
  }
};

const reviewLr = async (status) => {
  if (!props.nodeId) return;
  try {
    const reason = status === 'REJECTED' ? reviewReason.value : null;
    const r = await reviewResource(props.nodeId, status, reason);
    if (r.code === 200) {
      ElMessage.success(status === 'APPROVED' ? '已通过' : '已拒绝');
      lrStatus.value = status;
      reviewDialogVisible.value = false;
      reviewReason.value = '';
    } else {
      ElMessage.error(r.message || '操作失败');
    }
  } catch {
    ElMessage.error('操作失败');
  }
};
</script>

<style scoped>
.lr-card :deep(.el-card__body) {
  padding-top: 8px;
}
.lr-title-icon {
  color: var(--primary-color);
  font-size: var(--fs-lg);
}
.lr-status-tag {
  margin-left: 8px;
}
.lr-status-none {
  margin-left: 8px;
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}
.lr-body {
  padding: 4px 0;
}
.lr-desc {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  margin: 0 0 12px;
}
.lr-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
.lr-preview {
  margin-top: 8px;
}
.lr-tab-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.lr-tab-empty {
  padding: 24px;
  text-align: center;
  color: var(--text-disabled);
  font-size: var(--fs-sm);
}
.lr-video-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.lr-video-link {
  font-size: var(--fs-sm);
  color: var(--primary-color);
  text-decoration: none;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.lr-video-link:hover {
  text-decoration: underline;
}
.lr-item {
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-light);
}
.lr-q {
  font-size: var(--fs-sm);
  line-height: var(--lh-relaxed);
  margin-bottom: 4px;
}
.lr-text {
  font-size: var(--fs-sm);
  line-height: var(--lh-relaxed);
  white-space: pre-wrap;
}

@media (max-width: 767px) {
  .lr-actions .el-button {
    flex: 1;
    min-width: calc(50% - 4px);
  }
  .lr-actions .el-button:first-child {
    flex: 1 1 100%;
  }
  .lr-tabs :deep(.el-tabs__nav-wrap) {
    overflow-x: auto;
    overflow-y: hidden;
  }
  .lr-tabs :deep(.el-tabs__nav) {
    flex-wrap: nowrap;
  }
  .lr-tab-label .el-icon {
    font-size: 14px;
  }
  .lr-video-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
}
</style>

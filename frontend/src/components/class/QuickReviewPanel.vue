<template>
  <div class="qrp-panel">
    <div class="qrp-header">
      <el-button
        text
        @click="$emit('back')"
      >
        <el-icon><ArrowLeft /></el-icon>返回
      </el-button>
      <span class="qrp-title">课前三分钟</span>
      <span class="qrp-progress">{{ currentIndex + 1 }} / {{ cards.length }}</span>
    </div>

    <div v-if="!started" class="qrp-setup">
      <div class="qrp-setup-title">选择复习进度</div>
      <div class="qrp-setup-section">
        <div class="qrp-setup-label">复习范围</div>
        <el-radio-group v-model="reviewScope" @change="onScopeChange">
          <el-radio value="all">全部知识点</el-radio>
          <el-radio value="progress">按进度复习</el-radio>
        </el-radio-group>
      </div>
      <div v-if="reviewScope === 'progress'" class="qrp-setup-section">
        <div class="qrp-setup-label">学科</div>
        <el-select v-model="subjectId" placeholder="选择学科" @change="onSubjectChange">
          <el-option
            v-for="s in subjects"
            :key="s.id"
            :label="s.name"
            :value="s.id"
          />
        </el-select>
      </div>
      <div v-if="reviewScope === 'progress' && knowledgeTree.length > 0" class="qrp-setup-section">
        <div class="qrp-setup-label">复习到该节点之前的内容</div>
        <el-tree
          v-model="selectedNodeId"
          :data="knowledgeTree"
          :props="treeProps"
          node-key="id"
          highlight-current
          default-expand-all
          class="qrp-tree"
        />
      </div>
      <el-button type="primary" :loading="loading" @click="startReview">开始复习</el-button>
    </div>

    <div v-else-if="loading" class="qrp-loading">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
    </div>

    <template v-else-if="finished">
      <div class="qrp-done">
        <el-icon :size="48" color="var(--el-color-success, #67c23a)"><CircleCheckFilled /></el-icon>
        <div class="qrp-done-title">复习完成！</div>
        <div class="qrp-done-stats">
          <span>答对 {{ correctCount }} / {{ cards.length }}</span>
          <span>总积分 +{{ correctCount }}</span>
        </div>
        <el-button type="primary" @click="$emit('back')">返回大屏</el-button>
      </div>
    </template>

    <template v-else>
      <div class="qrp-card-stage">
        <div class="qrp-card" :class="{ revealed: revealed }" @click="reveal">
          <div class="qrp-card-front">
            <div class="qrp-card-label">📖 复习卡片</div>
            <div class="qrp-card-text">{{ currentCard.frontText }}</div>
            <div v-if="!revealed" class="qrp-card-hint">点击显示答案</div>
          </div>
          <div v-if="revealed" class="qrp-card-back">
            <div class="qrp-card-label">✅ 答案</div>
            <div class="qrp-card-text">{{ currentCard.backText }}</div>
          </div>
        </div>

        <div v-if="revealed && !picking" class="qrp-actions">
          <el-button size="large" type="danger" @click="startPick(false)">✗ 答错</el-button>
          <el-button size="large" type="success" @click="startPick(true)">✓ 答对 (+1分)</el-button>
        </div>

        <div v-if="picking" class="qrp-pick">
          <div class="qrp-pick-title">{{ pickCorrect ? '谁答对了？' : '谁答错了？' }}</div>
          <div class="qrp-pick-list">
            <div
              v-for="s in studentList"
              :key="s.studentId"
              class="qrp-pick-item"
              @click="record(s.studentId)"
            >
              <span class="qrp-pick-name">{{ s.name }}</span>
              <span class="qrp-pick-score">+{{ s.sessionScore || 0 }}</span>
            </div>
          </div>
          <el-button size="small" text @click="picking = false">取消</el-button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { ArrowLeft, Loading, CircleCheckFilled } from '@element-plus/icons-vue';
import { startQuickReview, recordQuickReview } from '@/api/classroom';
import { getNodeTree } from '@/api/knowledgeNode';
import { getMySubjects } from '@/api/settings';

const props = defineProps({
  classId: { type: [Number, String], required: true },
  studentScores: { type: Array, default: () => [] },
  sseConn: { type: Object, default: null },
});

const emit = defineEmits(['back', 'scored']);

const loading = ref(false);
const cards = ref([]);
const sessionId = ref('');
const currentIndex = ref(0);
const revealed = ref(false);
const correctCount = ref(0);
const finished = ref(false);
const picking = ref(false);
const pickCorrect = ref(true);
const pendingCorrect = ref(false);
const started = ref(false);
const reviewScope = ref('all');
const subjectId = ref(null);
const selectedNodeId = ref(null);
const subjects = ref([]);
const knowledgeTree = ref([]);

const treeProps = {
  label: 'name',
  children: 'children',
};

const currentCard = computed(() => cards.value[currentIndex.value] || {});

const studentList = computed(() => {
  return [...props.studentScores]
    .filter((s) => s.name)
    .sort((a, b) => (b.sessionScore || 0) - (a.sessionScore || 0))
    .slice(0, 20);
});

const reveal = () => {
  if (!revealed.value) revealed.value = true;
};

const startPick = (correct) => {
  pickCorrect.value = correct;
  pendingCorrect.value = correct;
  picking.value = true;
};

const record = async (studentId) => {
  picking.value = false;
  const correct = pendingCorrect.value;
  try {
    await recordQuickReview(sessionId.value, studentId, currentCard.value.index, correct);
    if (correct) {
      correctCount.value++;
      ElMessage.success('+1分');
      emit('scored', { studentId });
    }
  } catch {
    /* 静默 */
  }
  nextCard();
};

const nextCard = () => {
  if (currentIndex.value + 1 >= cards.value.length) {
    finished.value = true;
  } else {
    currentIndex.value++;
    revealed.value = false;
  }
};

const onScopeChange = () => {
  if (reviewScope.value === 'progress') {
    loadSubjects();
  } else {
    subjectId.value = null;
    selectedNodeId.value = null;
    knowledgeTree.value = [];
  }
};

const onSubjectChange = () => {
  if (subjectId.value) {
    loadKnowledgeTree(subjectId.value);
  } else {
    knowledgeTree.value = [];
    selectedNodeId.value = null;
  }
};

const loadSubjects = async () => {
  try {
    const res = await getMySubjects();
    if (res.code === 200) {
      subjects.value = res.data || [];
    }
  } catch {
    /* 静默 */
  }
};

const loadKnowledgeTree = async (sid) => {
  try {
    const res = await getNodeTree({ subjectId: sid });
    if (res.code === 200) {
      knowledgeTree.value = res.data || [];
    }
  } catch {
    /* 静默 */
  }
};

const startReview = async () => {
  loading.value = true;
  try {
    const params = { limit: 5 };
    if (reviewScope.value === 'progress' && selectedNodeId.value) {
      params.nodeId = selectedNodeId.value; // highlight-current → 单值
      params.subjectId = subjectId.value; // 后端 nodeId 优先时忽略,传了不冲突
    }
    const r = await startQuickReview(props.classId, params);
    if (r.code === 200 && r.data) {
      started.value = true;
      sessionId.value = r.data.sessionId;
      cards.value = r.data.cards || [];
      if (cards.value.length === 0) {
        ElMessage.warning('该进度之前暂无复习卡片');
        started.value = false;
      }
    }
  } catch {
    ElMessage.error('复习卡片加载失败');
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped lang="scss">
.qrp-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 0;
}

.qrp-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  border-bottom: 0.5px solid var(--border-light);
}

.qrp-title {
  flex: 1;
  font-size: var(--fs-xl);
  font-weight: 700;
  color: var(--text-primary);
}

.qrp-progress {
  font-size: var(--fs-md);
  color: var(--text-secondary);
}

.qrp-setup {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  gap: 24px;
}

.qrp-setup-title {
  font-size: var(--fs-2xl);
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.qrp-setup-section {
  width: 100%;
  max-width: 500px;
}

.qrp-setup-label {
  font-size: var(--fs-md);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.qrp-tree {
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 12px;
}

.qrp-loading {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.qrp-card-stage {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 24px;
  padding: 24px;
}

.qrp-card {
  width: 100%;
  max-width: 600px;
  min-height: 200px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-xl);
  padding: 32px;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover:not(.revealed) {
    border-color: var(--primary-color);
  }
}

.qrp-card-label {
  font-size: var(--fs-xs);
  color: var(--primary-color);
  font-weight: 600;
  margin-bottom: 16px;
  text-align: center;
}

.qrp-card-text {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  text-align: center;
  line-height: 1.6;
}

.qrp-card-hint {
  margin-top: 20px;
  font-size: var(--fs-sm);
  color: var(--text-disabled);
  text-align: center;
}

.qrp-card-back {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed var(--border-color);
}

.qrp-actions {
  display: flex;
  gap: 16px;
}

.qrp-done {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.qrp-done-title {
  font-size: var(--fs-2xl);
  font-weight: 700;
  color: var(--text-primary);
}

.qrp-done-stats {
  display: flex;
  gap: 24px;
  font-size: var(--fs-md);
  color: var(--text-secondary);
}

.qrp-pick {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  width: 100%;
  max-width: 400px;
}

.qrp-pick-title {
  font-size: var(--fs-lg);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.qrp-pick-list {
  width: 100%;
  max-height: 300px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.qrp-pick-item {
  display: flex;
  justify-content: space-between;
  padding: 12px 16px;
  background: var(--bg-card);
  border: 0.5px solid var(--border-color);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background 0.15s;
  &:hover {
    background: var(--bg-hover);
  }
}

.qrp-pick-name {
  font-size: var(--fs-md);
  font-weight: 500;
  color: var(--text-primary);
}

.qrp-pick-score {
  font-size: var(--fs-md);
  color: var(--el-color-success);
  font-weight: 600;
}
</style>

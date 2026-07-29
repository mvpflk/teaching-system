<template>
  <div class="kb-hub" :class="{ 'kb-mobile': isMobile }">
    <!-- PC 端：左侧边栏 -->
    <aside v-if="!isMobile" class="kb-sidebar">
      <div class="kb-sidebar-header">
        <h3>📚 知识库</h3>
        <span v-if="majorName" class="major-badge">{{ majorName }}</span>
      </div>

      <!-- 学科分组 -->
      <div class="subject-groups">
        <!-- 公共基础课 -->
        <div v-if="publicSubjects.length" class="subject-group">
          <div class="group-title">📖 公共基础课</div>
          <div
            v-for="subject in publicSubjects"
            :key="subject.id"
            class="subject-item"
            :class="{ active: kbStore.selectedSubjectId.value === subject.id }"
            @click="selectSubject(subject.id)"
          >
            <span class="subject-name">{{ subject.name }}</span>
            <span class="subject-count">{{ subject.articleCount }}</span>
          </div>
        </div>

        <!-- 专业课 -->
        <div v-if="majorSubjects.length" class="subject-group">
          <div class="group-title">🔧 {{ majorName }}专业课</div>
          <div
            v-for="subject in majorSubjects"
            :key="subject.id"
            class="subject-item"
            :class="{ active: kbStore.selectedSubjectId.value === subject.id }"
            @click="selectSubject(subject.id)"
          >
            <span class="subject-name">{{ subject.name }}</span>
            <span class="subject-count">{{ subject.articleCount }}</span>
          </div>
        </div>
      </div>

      <!-- 章节树 -->
      <div v-if="kbStore.chapterTree.value.length" class="kb-chapter-tree">
        <div class="tree-title">📋 章节</div>
        <el-tree
          :data="kbStore.chapterTree.value"
          :props="{ label: 'name', children: 'tasks' }"
          node-key="name"
          default-expand-all
          highlight-current
          :current-node-key="activeNodeKey"
          @node-click="onNodeClick"
        >
          <template #default="{ data }">
            <span class="tree-node">
              <span class="tree-node-name">{{ data.name }}</span>
              <el-tag v-if="data.count" size="small" round>{{ data.count }}</el-tag>
            </span>
          </template>
        </el-tree>
      </div>

      <div v-if="kbStore.todayCount.value > 0" class="kb-review-prompt">
        <div class="prompt-text">🕐 今日待复习</div>
        <div class="prompt-count">{{ kbStore.todayCount.value }} 张卡片</div>
        <el-button type="primary" size="small" @click="$router.push('/knowledge-base/review')">
          开始复习 →
        </el-button>
      </div>
      <div v-if="isStudent && (dailyStats.todayQuizzes > 0 || dailyStats.streak > 0)" class="kb-mini-stats">
        <div class="mini-stat">
          <span class="mini-stat-icon">📝</span>
          <span class="mini-stat-val">{{ dailyStats.todayQuizzes || 0 }}</span>
          <span class="mini-stat-label">今日自测</span>
        </div>
        <div class="mini-stat">
          <span class="mini-stat-icon">🔥</span>
          <span class="mini-stat-val">{{ dailyStats.streak || 0 }}</span>
          <span class="mini-stat-label">连续天数</span>
        </div>
        <div class="mini-stat">
          <span class="mini-stat-icon">🎯</span>
          <span class="mini-stat-val">{{ dailyStats.avgScore || '--' }}</span>
          <span class="mini-stat-label">均分</span>
        </div>
      </div>

      <!-- 知识清单入口（学生专有） -->
      <div
        v-if="isStudent"
        class="kb-checklist-entry"
        :class="{ active: route.path.startsWith('/knowledge-base/checklist') }"
        @click="$router.push('/knowledge-base/checklists')"
      >
        <el-icon><Notebook /></el-icon>
        <span>知识清单</span>
      </div>
    </aside>

    <!-- 主内容区 -->
    <main class="kb-main">
      <!-- 移动端：学科分组展示 -->
      <div
        v-if="isMobile && (publicSubjects.length || majorSubjects.length)"
        class="mobile-subjects"
      >
        <div v-if="publicSubjects.length" class="subject-section">
          <h3 class="section-title">📖 公共基础课</h3>
          <div class="subject-grid">
            <div
              v-for="subject in publicSubjects"
              :key="subject.id"
              class="subject-card"
              @click="selectSubject(subject.id)"
            >
              <div class="card-name">{{ subject.name }}</div>
              <div class="card-count">{{ subject.articleCount }} 篇</div>
            </div>
          </div>
        </div>
        <div v-if="majorSubjects.length" class="subject-section">
          <h3 class="section-title">🔧 {{ majorName }}专业课</h3>
          <div class="subject-grid">
            <div
              v-for="subject in majorSubjects"
              :key="subject.id"
              class="subject-card major"
              @click="selectSubject(subject.id)"
            >
              <div class="card-name">{{ subject.name }}</div>
              <div class="card-count">{{ subject.articleCount }} 篇</div>
            </div>
          </div>
        </div>
      </div>

      <router-view v-slot="{ Component, route: r }">
        <transition name="kb-fade">
          <component :is="Component" :key="r.fullPath" />
        </transition>
      </router-view>
    </main>

    <!-- 移动端：底部 Tab -->
    <nav v-if="isMobile" class="kb-bottom-tab">
      <div
        class="tab-item"
        :class="{ active: route.name === 'KnowledgeDiscover' }"
        @click="$router.push('/knowledge-base/discover')"
      >
        <el-icon><Search /></el-icon><span>发现</span>
      </div>
      <div
        class="tab-item"
        :class="{ active: route.name === 'KnowledgeReview' }"
        @click="$router.push('/knowledge-base/review')"
      >
        <div class="tab-icon-wrap">
          <el-icon><Clock /></el-icon>
          <span v-if="kbStore.todayCount.value > 0" class="badge pulse">{{
            kbStore.todayCount.value
          }}</span>
        </div>
        <span>复习</span>
      </div>
      <div
        class="tab-item"
        :class="{ active: route.name === 'KnowledgeMine' }"
        @click="$router.push('/knowledge-base/mine')"
      >
        <el-icon><User /></el-icon><span>我的</span>
      </div>
      <div
        class="tab-item"
        :class="{ active: route.path.startsWith('/knowledge-base/checklist') }"
        @click="$router.push('/knowledge-base/checklists')"
      >
        <el-icon><Notebook /></el-icon><span>清单</span>
      </div>
    </nav>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useIsMobile } from '@/composables/useIsMobile';
import { useUserStore } from '@/stores/user';
import { useKnowledgeBaseStore } from '@/composables/useKnowledgeBaseStore';
import { getTodayReview, getSubjectsGrouped, getDailyStats } from '@/api/knowledgeBase';
import { Search, Clock, User, Notebook } from '@element-plus/icons-vue';

const route = useRoute();
const router = useRouter();
const { isMobile } = useIsMobile();
const userStore = useUserStore();
const kbStore = useKnowledgeBaseStore();
const isStudent = computed(() => userStore.isStudent);

const publicSubjects = ref([]);
const majorSubjects = ref([]);
const majorName = ref('');
const activeNodeKey = ref('');
const dailyStats = ref({});

const SUBJECT_ID = computed(() => Number(route.query.subjectId) || 1);

function selectSubject(subjectId) {
  kbStore.selectedSubjectId.value = subjectId;
  kbStore.loadTree(subjectId);
  // 学生跳转到发现页，教师/管理员留在当前页
  if (isStudent.value) {
    router.push({ path: '/knowledge-base/discover', query: { subjectId } });
  }
}

function onNodeClick(data, node) {
  const nodeName = data.name || '';
  const renderType = data.renderType;
  const isChapter = !!(data.tasks && data.tasks.length > 0);

  // v151: 自定义渲染节点 → 路由到对应视图
  if (renderType === 'vocab_drill') {
    router.push({ path: '/knowledge-base/english/vocab' });
    return;
  }

  activeNodeKey.value = nodeName;
  if (isChapter) {
    kbStore.selectedChapter.value = nodeName;
    kbStore.selectedTask.value = '';
  } else {
    let found = false;
    for (const ch of kbStore.chapterTree.value) {
      if (ch.tasks && ch.tasks.some((t) => t.name === nodeName)) {
        kbStore.selectedChapter.value = ch.name || '';
        kbStore.selectedTask.value = nodeName;
        found = true;
        break;
      }
    }
    if (!found) {
      kbStore.selectedChapter.value = nodeName;
      kbStore.selectedTask.value = '';
    }
  }
  // 学生跳转到发现页，教师/管理员留在当前页（与 selectSubject 保持一致）
  if (isStudent.value && route.name !== 'KnowledgeDiscover') {
    router.push({
      path: '/knowledge-base/discover',
      query: { subjectId: kbStore.selectedSubjectId.value },
    });
  }
}

onMounted(async () => {
  try {
    const { data } = await getSubjectsGrouped();
    publicSubjects.value = data.publicSubjects || [];
    majorSubjects.value = data.majorSubjects || [];
    majorName.value = data.majorName || '';

    // 确定默认学科：路由参数 > 第一个可用学科 > 1
    const routeSubjectId = Number(route.query.subjectId) || 0;
    if (routeSubjectId > 0) {
      kbStore.selectedSubjectId.value = routeSubjectId;
    } else if (publicSubjects.value.length > 0) {
      kbStore.selectedSubjectId.value = publicSubjects.value[0].id;
    } else if (majorSubjects.value.length > 0) {
      kbStore.selectedSubjectId.value = majorSubjects.value[0].id;
    }
  } catch (e) {
    console.error('加载学科分组失败', e);
  }

  const treeSubjectId = kbStore.selectedSubjectId.value || SUBJECT_ID.value;
  await kbStore.loadTree(treeSubjectId);

  if (isStudent.value) {
    try {
      const res = await getTodayReview();
      kbStore.todayCount.value = (res.data || []).length;
    } catch (e) {
      console.error('getTodayReview failed:', e);
    }
    try {
      const r = await getDailyStats(kbStore.selectedSubjectId.value || SUBJECT_ID.value);
      dailyStats.value = r.data || {};
    } catch (e) {
      console.error('getDailyStats failed:', e);
    }
  }
});
</script>

<style scoped>
.kb-hub {
  display: flex;
  min-height: calc(100vh - 64px);
}

/* 侧边栏 */
.kb-sidebar {
  width: 260px;
  border-right: 1px solid var(--border-light);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex-shrink: 0;
  overflow-y: auto;
}
.kb-sidebar-header {
  display: flex;
  align-items: center;
  gap: 8px;
}
.kb-sidebar-header h3 {
  margin: 0;
  font-size: var(--fs-lg);
}
.major-badge {
  padding: 2px 8px;
  background: var(--primary-light);
  color: var(--primary-color);
  border-radius: var(--radius-sm);
  font-size: var(--fs-xs);
}

/* 学科分组 */
.subject-groups {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.subject-group {
}
.group-title {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  margin-bottom: 6px;
  font-weight: 500;
}
.subject-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.15s ease;
}
.subject-item:hover {
  background: var(--bg-hover);
}
.subject-item.active {
  background: var(--primary-light);
  color: var(--primary-color);
}
.subject-name {
  font-size: var(--fs-sm);
}
.subject-count {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  background: var(--bg-secondary);
  padding: 1px 6px;
  border-radius: 10px;
}

/* 章节树 */
.tree-title {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  margin-bottom: 6px;
  font-weight: 500;
}
.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
}
.tree-node-name {
  font-size: var(--fs-sm);
}
:deep(.el-tree-node.is-current > .el-tree-node__content) {
  background: var(--primary-light, var(--primary-light));
  color: var(--primary-color);
}
:deep(.el-tree-node__content:hover) {
  background: var(--bg-hover, #f5f7fa);
}

.kb-review-prompt {
  background: var(--primary-light);
  border-radius: 12px;
  padding: 16px;
  text-align: center;
}
.prompt-text {
  font-size: var(--fs-md);
  color: var(--text-primary);
}
.prompt-count {
  font-size: var(--fs-2xl);
  font-weight: 700;
  color: var(--primary-color);
  margin: 4px 0 8px;
}

/* 主内容区 */
.kb-main {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.kb-mini-stats {
  display: flex;
  gap: 4px;
  background: var(--bg-card, #fff);
  border: 1px solid var(--border-light, #eee);
  border-radius: 10px;
  padding: 10px 8px;
}
.mini-stat {
  flex: 1;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}
.mini-stat + .mini-stat {
  border-left: 1px solid var(--border-light, #eee);
}
.mini-stat-icon { font-size: 16px; }
.mini-stat-val { font-size: 16px; font-weight: 700; color: var(--primary-color, var(--primary-color)); }
.mini-stat-label { font-size: 10px; color: var(--text-secondary, #999); }

/* 知识清单入口 */
.kb-checklist-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding: 12px 16px;
  background: var(--bg-card, #fff);
  border: 1px solid var(--border-light, #eee);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s;
  font-size: var(--fs-sm);
  color: var(--text-primary);
}
.kb-checklist-entry:hover {
  border-color: var(--primary-color);
  background: var(--primary-light);
}
.kb-checklist-entry.active {
  background: var(--primary-light);
  color: var(--primary-color);
  border-color: var(--primary-color);
}

/* 移动端学科卡片 */
.mobile-subjects {
  margin-bottom: 20px;
}
.subject-section {
  margin-bottom: 16px;
}
.section-title {
  font-size: var(--fs-md);
  color: var(--text-primary);
  margin-bottom: 12px;
}
.subject-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}
.subject-card {
  padding: 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  cursor: pointer;
  text-align: center;
  transition: all 0.2s ease;
}
.subject-card:hover {
  border-color: var(--primary-color);
  box-shadow: var(--shadow-sm);
}
.subject-card.major {
  border-left: 3px solid var(--primary-color);
}
.card-name {
  font-size: var(--fs-sm);
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 4px;
}
.card-count {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}

/* 底部 Tab */
.kb-bottom-tab {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-around;
  background: var(--bg-card);
  border-top: 1px solid var(--border-light);
  padding: 6px 0 env(safe-area-inset-bottom, 8px);
  z-index: 100;
}
.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  font-size: 10px;
  color: var(--text-secondary);
  cursor: pointer;
}
.tab-item.active {
  color: var(--primary-color);
}
.tab-icon-wrap {
  position: relative;
  display: inline-flex;
}
.badge {
  position: absolute;
  top: -6px;
  right: -10px;
  background: var(--el-color-danger);
  color: #fff;
  border-radius: 50%;
  min-width: 16px;
  height: 16px;
  font-size: 10px;
  text-align: center;
  line-height: 16px;
  padding: 0 3px;
}
.badge.pulse {
  animation: badge-pulse 2s ease-in-out infinite;
}
@keyframes badge-pulse {
  0%,
  100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.15);
  }
}
.kb-fade-enter-active,
.kb-fade-leave-active {
  transition: opacity 0.15s ease;
}
.kb-fade-enter-from,
.kb-fade-leave-to {
  opacity: 0;
}

@media (max-width: 768px) {
  .kb-sidebar {
    display: none;
  }
  .kb-main {
    padding: 12px;
    padding-bottom: 64px;
  }
  .subject-grid {
    grid-template-columns: 1fr;
  }
}
</style>

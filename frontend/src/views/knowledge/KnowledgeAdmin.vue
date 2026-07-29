<template>
  <div class="admin-page">
    <div class="header">
      <h3>📋 知识库管理</h3>
      <div class="header-actions">
        <el-select
          v-model="kbStore.selectedSubjectId"
          placeholder="全部学科"
          clearable
          class="subject-filter"
        >
          <el-option-group label="公共基础课">
            <el-option
              v-for="s in publicSubjects"
              :key="s.id"
              :label="s.name"
              :value="s.id"
            />
          </el-option-group>
          <el-option-group
            v-for="group in majorGroups"
            :key="group.majorName"
            :label="group.majorName + '专业课'"
          >
            <el-option
              v-for="s in group.subjects"
              :key="s.id"
              :label="s.name"
              :value="s.id"
            />
          </el-option-group>
        </el-select>
        <el-button
          type="success"
          plain
          :loading="batchGenerating"
          @click="handleBatchGenerateCards"
        >
          🃏 批量生成全部卡片
        </el-button>
        <el-button type="primary" @click="$router.push('/knowledge-base/admin/articles/new')">
          新建文章
        </el-button>
        <el-button plain @click="$router.push(`/knowledge-base/admin/class-stats?subjectId=${kbStore.selectedSubjectId || 24}`)">
          📊 全班统计
        </el-button>
      </div>
    </div>
    <el-table
      v-loading="loading"
      :data="articles"
      stripe
      @row-click="(row) => $router.push(`/knowledge-base/admin/articles/${row.id}`)"
    >
      <el-table-column prop="title" label="标题" min-width="200" />
      <el-table-column prop="chapter" label="章节" width="150" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'warning'" size="small">
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="viewCount" label="浏览" width="80" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button
            text
            type="primary"
            size="small"
            @click.stop="$router.push(`/knowledge-base/admin/articles/${row.id}`)"
          >
            编辑
          </el-button>
          <el-button
            text
            type="success"
            size="small"
            @click.stop="handleGenerateCards(row.id)"
          >
            🃏生成卡片
          </el-button>
          <el-popconfirm title="确定删除？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button
                text
                type="danger"
                size="small"
                @click.stop
              >
                删除
              </el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <div v-if="total > 20" class="pagination">
      <el-pagination
        v-model:current-page="page"
        layout="prev, pager, next"
        :total="total"
        :page-size="20"
        background
        @current-change="loadData"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue';
import {
  getAdminArticles,
  deleteArticle,
  generateFlashcards,
  generateAllFlashcards,
  getSubjectsGrouped,
} from '@/api/knowledgeBase';
import { useKnowledgeBaseStore } from '@/composables/useKnowledgeBaseStore';
import { ElMessage } from 'element-plus';

const kbStore = useKnowledgeBaseStore();

const articles = ref([]);
const loading = ref(false);
const batchGenerating = ref(false);
const page = ref(1);
const total = ref(0);

// 学科分组（selectedSubjectId 由 store 共享，侧边栏和下拉框联动）
const publicSubjects = ref([]);
const majorGroups = ref([]);

onMounted(async () => {
  await loadSubjectGroups();
  await loadData();
});

async function loadSubjectGroups() {
  try {
    const { data } = await getSubjectsGrouped();
    publicSubjects.value = data.publicSubjects || [];
    // 教师端后端直接返回分组好的 majorGroups；学生端返回扁平 majorSubjects
    if (data.majorGroups && data.majorGroups.length) {
      majorGroups.value = data.majorGroups;
    } else if (data.majorSubjects && data.majorSubjects.length) {
      majorGroups.value = [
        {
          majorName: data.majorName || '专业课',
          subjects: data.majorSubjects,
        },
      ];
    }
  } catch (e) {
    console.error('加载学科分组失败', e);
  }
}

watch(
  () => kbStore.selectedChapter.value,
  () => {
    page.value = 1;
    loadData();
  }
);
watch(
  () => kbStore.selectedTask.value,
  () => {
    page.value = 1;
    loadData();
  }
);
watch(kbStore.selectedSubjectId, () => {
  page.value = 1;
  loadData();
});

let fetchSeq = 0;
async function loadData() {
  const seq = ++fetchSeq;
  loading.value = true;
  try {
    const r = await getAdminArticles({
      page: page.value,
      size: 20,
      subjectId: kbStore.selectedSubjectId.value || undefined,
      chapter: kbStore.selectedChapter.value || undefined,
      task: kbStore.selectedTask.value || undefined,
    });
    if (seq !== fetchSeq) return;
    if (r.code === 200) {
      articles.value = r.data?.records || [];
      total.value = r.data?.total || 0;
    }
  } catch (e) {
    if (seq !== fetchSeq) return;
    console.error('loadData failed:', e);
  } finally {
    if (seq === fetchSeq) loading.value = false;
  }
}

async function handleDelete(id) {
  try {
    await deleteArticle(id);
    articles.value = articles.value.filter((a) => a.id !== id);
    ElMessage.success('已删除');
  } catch (e) {
    console.error('删除失败:', e);
    ElMessage.error('删除失败，请重试');
  }
}

async function handleGenerateCards(id) {
  try {
    const r = await generateFlashcards(id);
    if (r.code === 200) ElMessage.success(r.message || '卡片已生成');
  } catch (e) {
    console.error('生成卡片失败:', e);
    ElMessage.error('生成卡片失败');
  }
}

async function handleBatchGenerateCards() {
  batchGenerating.value = true;
  try {
    const r = await generateAllFlashcards();
    if (r.code === 200) {
      ElMessage.success(r.message || '批量生成完成');
      loadData();
    }
  } catch (e) {
    console.error('批量生成失败:', e);
    ElMessage.error('批量生成失败');
  } finally {
    batchGenerating.value = false;
  }
}
</script>

<style scoped>
.admin-page {
  max-width: 1000px;
  margin: 0 auto;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}
.header h3 {
  margin: 0;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.subject-filter {
  width: 200px;
}
.pagination {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>

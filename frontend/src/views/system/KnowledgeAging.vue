<template>
  <div class="ka-page">
    <div class="page-header">
      <h3>知识时效性管理</h3>
      <div>
        <el-select
          v-model="filterSubject"
          placeholder="筛选学科"
          clearable
          size="small"
          style="width: 200px; margin-right: 8px"
          @change="loadData"
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
        <el-select
          v-model="filterStatus"
          placeholder="筛选状态"
          clearable
          size="small"
          style="width: 120px; margin-right: 8px"
          @change="loadData"
        >
          <el-option label="正常" value="ACTIVE" />
          <el-option label="陈旧" value="LEGACY" />
          <el-option label="过时" value="DEPRECATED" />
          <el-option label="淘汰" value="OBSOLETE" />
        </el-select>
        <el-button size="small" :loading="loading" @click="loadData">刷新</el-button>
      </div>
    </div>

    <el-table
      v-loading="loading"
      :data="nodes"
      stripe
      border
      max-height="65vh"
    >
      <el-table-column type="selection" width="40" />
      <el-table-column prop="name" label="知识点" min-width="160">
        <template #default="{ row }">
          <span :class="{ 'text-dim': (row.relevanceLevel || 5) <= 3 }">{{ row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column label="层级" width="80">
        <template #default="{ row }">{{ levelLabel[row.level] || row.level }}</template>
      </el-table-column>
      <el-table-column
        label="相关度"
        width="85"
        sortable
        prop="relevanceLevel"
      >
        <template #default="{ row }">
          <el-rate
            v-model="row.relevanceLevel"
            :max="10"
            :low-threshold="4"
            :high-threshold="7"
            disabled
            show-score
            text-color="var(--text-regular)"
            size="small"
          />
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-select v-model="row.status" size="small" @change="(v) => saveRow(row)">
            <el-option label="正常" value="ACTIVE" />
            <el-option label="陈旧" value="LEGACY" />
            <el-option label="过时" value="DEPRECATED" />
            <el-option label="淘汰" value="OBSOLETE" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="陈旧说明" min-width="180">
        <template #default="{ row }">
          <el-input
            v-model="row.deprecationNote"
            size="small"
            placeholder="如：软盘已被U盘取代"
            @blur="saveRow(row)"
            @keyup.enter="saveRow(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="最后审核" width="110">
        <template #default="{ row }">
          {{
            row.lastReviewedAt?.substring(0, 10) || '未审核'
          }}
        </template>
      </el-table-column>
    </el-table>

    <!-- 批量操作 -->
    <div v-if="selectedNodes.length" class="batch-bar">
      <span>已选 {{ selectedNodes.length }} 个</span>
      <el-select
        v-model="batchStatus"
        placeholder="设为状态"
        size="small"
        style="width: 120px; margin: 0 8px"
        clearable
      >
        <el-option label="正常" value="ACTIVE" />
        <el-option label="陈旧" value="LEGACY" />
        <el-option label="过时" value="DEPRECATED" />
        <el-option label="淘汰" value="OBSOLETE" />
      </el-select>
      <el-input-number
        v-model="batchRelevance"
        :min="1"
        :max="10"
        size="small"
        style="width: 100px; margin-right: 8px"
        placeholder="相关度"
      />
      <el-button size="small" type="primary" @click="applyBatch">应用</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { getNodeTree, updateNode } from '@/api/knowledgeNode';
import { getSubjectsGrouped } from '@/api/knowledgeBase';

const levelLabel = { 1: '学科', 2: '章节', 3: '任务', 4: '知识点' };

const loading = ref(false);
const nodes = ref([]);
const subjects = ref([]);
const filterSubject = ref(null);
const filterStatus = ref(null);
const batchStatus = ref(null);
const batchRelevance = ref(5);

// 学科分组
const publicSubjects = ref([]);
const majorGroups = ref([]);

// 从知识树提取所有非根节点
function flattenTree(treeNodes, level = 1) {
  const result = [];
  for (const n of treeNodes) {
    if (level >= 2) {
      result.push({
        id: n.id,
        name: n.name,
        level,
        status: n.status || 'ACTIVE',
        relevanceLevel: n.relevanceLevel || 5,
        deprecationNote: n.deprecationNote || '',
        lastReviewedAt: n.lastReviewedAt || null,
      });
    }
    if (n.children?.length) result.push(...flattenTree(n.children, level + 1));
  }
  return result;
}

async function loadData() {
  loading.value = true;
  try {
    const res = await getNodeTree();
    if (res.code === 200) {
      let all = flattenTree(res.data || []);
      // 提取学科列表
      subjects.value = (res.data || []).map((n) => ({ id: n.id, name: n.name }));
      if (filterSubject.value) {
        // 从指定学科的子树中提取
        const subj = (res.data || []).find((n) => n.id === filterSubject.value);
        all = subj ? flattenTree([subj]) : [];
      }
      if (filterStatus.value) all = all.filter((n) => n.status === filterStatus.value);
      nodes.value = all;
    }
  } catch {
    ElMessage.error('加载失败');
  }
  loading.value = false;
}

async function saveRow(row) {
  try {
    await updateNode(row.id, {
      status: row.status,
      relevanceLevel: row.relevanceLevel,
      deprecationNote: row.deprecationNote,
      lastReviewedAt: new Date().toISOString(),
    });
    ElMessage.success('已保存');
  } catch {
    ElMessage.error('保存失败');
  }
}

const selectedNodes = ref([]);

async function applyBatch() {
  if (!selectedNodes.value.length) return;
  const updates = {};
  if (batchStatus.value) updates.status = batchStatus.value;
  if (batchRelevance.value) updates.relevanceLevel = batchRelevance.value;
  if (!Object.keys(updates).length) return ElMessage.warning('请选择要更新的属性');

  loading.value = true;
  let ok = 0;
  for (const n of selectedNodes.value) {
    try {
      await updateNode(n.id, { ...updates, lastReviewedAt: new Date().toISOString() });
      n.status = updates.status || n.status;
      n.relevanceLevel = updates.relevanceLevel || n.relevanceLevel;
      ok++;
    } catch {
      /* skip */
    }
  }
  ElMessage.success(`已更新 ${ok} 个知识点`);
  loading.value = false;
}

// selection
const tableRef = ref(null);

onMounted(async () => {
  await loadSubjectGroups();
  await loadData();
});

async function loadSubjectGroups() {
  try {
    const { data } = await getSubjectsGrouped();
    publicSubjects.value = data.publicSubjects || [];
    if (data.majorSubjects && data.majorSubjects.length) {
      majorGroups.value = [
        {
          majorName: data.majorName || '专业',
          subjects: data.majorSubjects,
        },
      ];
    }
  } catch (e) {
    console.error('加载学科分组失败', e);
  }
}
</script>

<style scoped>
.ka-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 16px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.page-header h3 {
  margin: 0;
}
.text-dim {
  opacity: 0.5;
}
.batch-bar {
  display: flex;
  align-items: center;
  margin-top: 12px;
  padding: 8px 12px;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  font-size: var(--fs-sm);
}
</style>

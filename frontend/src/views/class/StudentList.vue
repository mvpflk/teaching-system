<template>
  <div class="page-card">
    <div class="page-header">
      <h3 class="page-title">学生管理</h3>
      <div class="header-actions">
        <el-button
          v-if="isAdmin"
          type="danger"
          :disabled="selectedIds.length === 0"
          @click="handleBatchDelete"
        >
          <el-icon><Delete /></el-icon>批量删除 ({{ selectedIds.length }})
        </el-button>
        <el-button v-if="isAdmin" type="warning" @click="showImport">
          <el-icon><Upload /></el-icon>批量导入
        </el-button>
        <el-button v-if="isAdmin" type="primary" @click="showCreate">
          <el-icon><Plus /></el-icon>添加学生
        </el-button>
      </div>
    </div>

    <el-card shadow="never" class="search-card">
      <el-form :model="search" inline>
        <el-form-item label="搜索">
          <el-input
            v-model="search.keyword"
            placeholder="姓名/用户名"
            clearable
            @clear="onSearchChange"
            @keyup.enter="onSearchChange"
          />
        </el-form-item>
        <el-form-item v-if="showGradeFilter" label="年级">
          <el-select
            v-model="search.grade"
            placeholder="全部年级"
            clearable
            style="width: 150px"
            @change="onSearchChange"
          >
            <el-option
              v-for="g in displayGradeList"
              :key="g"
              :value="g"
              :label="g"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="showClassFilter" label="班级">
          <el-select
            v-model="search.classId"
            placeholder="全部班级"
            clearable
            style="width: 180px"
            @change="onSearchChange"
          >
            <el-option
              v-for="c in displayClassList"
              :key="c.id"
              :value="c.id"
              :label="c.className"
            >
              <span style="float: left">{{ c.className }}</span>
              <span style="float: right; color: var(--text-secondary); font-size: var(--fs-xs)">{{
                c.major
              }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select
            v-model="search.currentType"
            placeholder="全部"
            clearable
            style="width: 150px"
            @change="onSearchChange"
          >
            <el-option
              v-for="t in typeOptions"
              :key="t.typeCode"
              :value="t.typeCode"
              :label="t.typeName"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearchChange">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table
      v-loading="loading"
      :data="list"
      stripe
      @selection-change="onSelectionChange"
    >
      <el-table-column type="selection" width="45" />
      <el-table-column prop="studentNumber" label="学号" width="120" />
      <el-table-column prop="realName" label="姓名" width="100" />
      <el-table-column prop="username" label="用户名" width="130" />
      <el-table-column label="性别" width="60">
        <template #default="{ row }">
          {{ row.gender === 1 ? '男' : row.gender === 2 ? '女' : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="grade" label="年级" width="100" />
      <el-table-column label="班级" min-width="100">
        <template #default="{ row }">
          <span>{{ row.className }}</span>
        </template>
      </el-table-column>
      <el-table-column label="当前类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.currentType" size="small" :type="typeTagColor(row.currentType)">
            {{ typeName(row.currentType) }}
          </el-tag>
          <span v-else style="color: var(--text-secondary)">-</span>
        </template>
      </el-table-column>
      <el-table-column label="入学类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.enrollmentType" size="small" :type="typeTagColor(row.enrollmentType)">
            {{ typeName(row.enrollmentType) }}
          </el-tag>
          <span v-else style="color: var(--text-secondary)">-</span>
        </template>
      </el-table-column>
      <el-table-column label="专业" min-width="100">
        <template #default="{ row }">{{ row.major || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="85" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">
            {{ statusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="操作"
        width="70"
        align="center"
        fixed="right"
      >
        <template #default="{ row }">
          <el-dropdown trigger="click" @command="(cmd) => onStudentCmd(cmd, row)">
            <el-button size="small" circle :icon="'MoreFilled'" />
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="canEdit(row)" command="edit">
                  <el-icon><Edit /></el-icon> 编辑
                </el-dropdown-item>
                <el-dropdown-item v-if="canEdit(row)" command="status" divided>
                  <el-icon><Document /></el-icon> 变更状态
                </el-dropdown-item>
                <el-dropdown-item v-if="isAdmin" command="resetPwd">
                  <el-icon><Lock /></el-icon> 重置密码
                </el-dropdown-item>
                <el-dropdown-item v-if="isAdmin" command="delete" divided>
                  <el-icon><Delete /></el-icon> 删除
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>

    <div style="margin-top: 16px; display: flex; justify-content: flex-end">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="onPageChange"
        @size-change="onPageSizeChange"
      />
    </div>

    <StudentFormDialog
      v-model="formVisible"
      :edit-data="editData"
      :class-list="classList"
      :grade-list="gradeList"
      @saved="onStudentSaved"
    />
    <StudentImportDialog v-model="importVisible" @imported="onImported" />

    <el-dialog
      v-model="statusVisible"
      title="变更学生状态"
      width="400px"
      destroy-on-close
      append-to-body
    >
      <p style="margin: 0 0 12px; font-size: var(--fs-md)">
        学生：<strong>{{ statusTarget?.realName }}</strong>
      </p>
      <el-select
        v-model="newStatus"
        placeholder="选择新状态"
        size="default"
        style="width: 100%"
      >
        <el-option
          v-for="s in STATUS_OPTIONS"
          :key="s.value"
          :value="s.value"
          :label="s.label"
        />
      </el-select>
      <template #footer>
        <el-button @click="statusVisible = false">取消</el-button>
        <el-button type="primary" :loading="statusSaving" @click="doChangeStatus">
          确认变更
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useUserStore } from '@/stores/user';
import {
  getStudentList,
  deleteStudent,
  batchDeleteStudents,
  updateStudentStatus,
} from '@/api/student';
import { getClassList } from '@/api/classes';
import { adminResetPassword } from '@/api/profile';
import { statusLabel, statusTagType, STATUS_OPTIONS } from '@/utils/student';
import { getGrades } from '@/api/settings';
import { getTypeConfigList } from '@/api/classTypeConfig';
import StudentFormDialog from './StudentFormDialog.vue';
import StudentImportDialog from './StudentImportDialog.vue';

const typeOptions = ref([]);
const typeTagColor = (code) => {
  if (!code) return 'info';
  const t = typeOptions.value.find((o) => o.typeCode === code);
  return t ? { GENERAL: 'info', PUGAO: '', VOCATIONAL: 'warning' }[t.category] || 'info' : 'info';
};
const typeName = (code) => {
  if (!code) return '-';
  const t = typeOptions.value.find((o) => o.typeCode === code);
  return t ? t.typeName : code;
};

const userStore = useUserStore();
const isAdmin = computed(() => userStore.isAdmin);
const isTeacher = computed(() => userStore.isTeacher);
const isHeadTeacher = computed(() => userStore.isHeadTeacher);
const headClassId = computed(() => userStore.teacherSummary?.headClassId);
const teachingClassIds = computed(() => userStore.teachingClassIds);
const loading = ref(false);
const list = ref([]);
const classList = ref([]);
const gradeList = ref([]);
const selectedIds = ref([]);

// 非管理员教师：年级/班级下拉只显示任教范围
const displayClassList = computed(() => {
  if (isAdmin.value) return classList.value;
  return classList.value.filter((c) => teachingClassIds.value.includes(c.id));
});
const displayGradeList = computed(() => {
  if (isAdmin.value) return gradeList.value;
  const ids = teachingClassIds.value;
  return [
    ...new Set(
      classList.value
        .filter((c) => ids.includes(c.id))
        .map((c) => c.grade)
        .filter(Boolean)
    ),
  ];
});
const showGradeFilter = computed(() => displayGradeList.value.length > 1);
const showClassFilter = computed(() => displayClassList.value.length > 1);

const canEdit = (row) => {
  return isAdmin.value || (isHeadTeacher.value && headClassId.value === row.classId);
};

const search = reactive({ keyword: '', grade: '', classId: null, currentType: '' });
const pageNum = ref(1);
const pageSize = ref(20);
const total = ref(0);

const formVisible = ref(false);
const editData = ref(null);
const importVisible = ref(false);

const showCreate = () => {
  editData.value = null;
  formVisible.value = true;
};
const showImport = () => {
  importVisible.value = true;
};

const onStudentSaved = () => {
  formVisible.value = false;
  pageNum.value = 1;
  loadList();
  loadClasses();
};
const onImported = () => {
  importVisible.value = false;
  pageNum.value = 1;
  loadList();
  loadClasses();
};

const loadList = async () => {
  loading.value = true;
  try {
    const params = { page: pageNum.value, pageSize: pageSize.value };
    if (search.keyword) params.keyword = search.keyword;
    if (search.classId) params.classId = search.classId;
    if (search.grade) params.grade = search.grade;
    if (search.currentType) params.currentType = search.currentType;
    const res = await getStudentList(params);
    if (res.code === 200) {
      list.value = res.data.records;
      total.value = res.data.total;
    }
  } finally {
    loading.value = false;
  }
};

const onPageChange = (page) => {
  pageNum.value = page;
  loadList();
};
const onPageSizeChange = (size) => {
  pageSize.value = size;
  pageNum.value = 1;
  loadList();
};

const loadClasses = async () => {
  const res = await getClassList();
  if (res.code === 200) classList.value = res.data.records;
};

const loadGradeList = async () => {
  const res = await getGrades();
  if (res.code === 200) gradeList.value = res.data.map((g) => g.gradeName);
};

const resetSearch = () => {
  search.keyword = '';
  search.grade = '';
  search.classId = null;
  search.currentType = '';
  pageNum.value = 1;
  loadList();
};

const onSearchChange = () => {
  pageNum.value = 1;
  loadList();
};

const onSelectionChange = (rows) => {
  selectedIds.value = rows.map((r) => r.id);
};

const handleBatchDelete = async () => {
  if (selectedIds.value.length === 0) return;
  try {
    await ElMessageBox.confirm(
      `确定批量删除选中的 ${selectedIds.value.length} 名学生吗？\n将同时删除其账号及所有关联数据，不可恢复！`,
      '批量删除',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    );
    const res = await batchDeleteStudents(selectedIds.value);
    if (res.code === 200) {
      ElMessage.success(res.message || `成功删除 ${res.data.success} 人`);
      selectedIds.value = [];
      await loadList();
      await loadClasses();
    }
  } catch {
    /* cancelled */
  }
};

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除学生「${row.realName}」吗？`, '确认删除', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    });
    const res = await deleteStudent(row.id);
    if (res.code === 200) {
      ElMessage.success('已删除');
      await loadList();
      await loadClasses();
    }
  } catch {
    /* cancelled */
  }
};

const onStudentCmd = (cmd, row) => {
  if (cmd === 'edit') {
    editData.value = row;
    formVisible.value = true;
  } else if (cmd === 'status') openStatusDialog(row);
  else if (cmd === 'resetPwd') handleResetPassword(row);
  else if (cmd === 'delete') handleDelete(row);
};

const handleResetPassword = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt(
      `为「${row.realName}」重置密码（留空则自动生成）`,
      '重置密码',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPlaceholder: '输入新密码，留空则自动生成',
      }
    );
    const res = await adminResetPassword(row.userId, { newPassword: value || '' });
    if (res.code === 200) {
      ElMessage.success(`密码已重置为: ${res.data.newPassword}`);
    } else {
      ElMessage.error(res.message || '重置失败');
    }
  } catch {
    /* cancelled */
  }
};

const statusVisible = ref(false);
const statusTarget = ref(null);
const newStatus = ref('active');
const statusSaving = ref(false);

const openStatusDialog = (row) => {
  statusTarget.value = row;
  newStatus.value = row.status || 'active';
  statusVisible.value = true;
};

const doChangeStatus = async () => {
  statusSaving.value = true;
  try {
    const res = await updateStudentStatus(statusTarget.value.id, newStatus.value);
    if (res.code === 200) {
      ElMessage.success(res.data || '状态已更新');
      statusVisible.value = false;
      loadList();
    } else {
      ElMessage.error(res.message || '操作失败');
    }
  } catch {
    ElMessage.error('请求失败');
  } finally {
    statusSaving.value = false;
  }
};

const loadTypeOptions = async () => {
  try {
    const r = await getTypeConfigList();
    if (r.code === 200) typeOptions.value = r.data || [];
  } catch {
    /* */
  }
};

onMounted(() => {
  loadList();
  loadClasses();
  loadGradeList();
  loadTypeOptions();
});
</script>

<style scoped>
.search-card {
  margin-bottom: 16px;
}
.search-card :deep(.el-card__body) {
  padding: 16px 20px;
}

@media (max-width: 768px) {
  :deep(.el-table) {
    font-size: var(--fs-xs);
  }
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  .header-actions {
    flex-wrap: wrap;
    gap: 8px;
  }
  :deep(.el-form--inline .el-form-item) {
    margin-right: 0;
    width: 100%;
    margin-bottom: 8px;
  }
  :deep(.el-form--inline .el-select),
  :deep(.el-form--inline .el-input) {
    width: 100%;
  }
  :deep(.el-form--inline .el-button) {
    width: 100%;
    margin-left: 0;
  }
}
</style>

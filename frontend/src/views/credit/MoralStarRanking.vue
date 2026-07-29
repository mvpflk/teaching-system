<template>
  <div class="moral-ranking-container">
    <div class="page-header">
      <h3 class="page-title">🌟 德育行为积分榜</h3>
      <span class="page-subtitle">每一个善举都值得被看见</span>
      <div class="top-n-selector">
        <span style="font-size: var(--fs-sm); color: var(--text-secondary); margin-right: 8px">显示：</span>
        <el-select
          v-model="topN"
          size="small"
          class="desktop-width"
          style="width: 100px"
          @change="onTopNChange"
        >
          <el-option :value="10" label="前10名" />
          <el-option :value="20" label="前20名" />
          <el-option :value="50" label="前50名" />
          <el-option :value="0" label="全部" />
        </el-select>
      </div>
    </div>

    <!-- 三级筛选：全校 / 年级 / 班级 -->
    <div class="filter-bar">
      <div class="scope-tabs">
        <button :class="{ active: scope === 'school' }" @click="switchScope('school')">
          🌍 全校
        </button>
        <button :class="{ active: scope === 'grade' }" @click="switchScope('grade')">
          🏫 年级
        </button>
        <button :class="{ active: scope === 'class' }" @click="switchScope('class')">
          📋 班级
        </button>
      </div>
      <!-- 非学生显示下拉选择菜单 -->
      <div v-if="scope !== 'school' && !isStudent" class="scope-selects">
        <el-select
          v-if="scope === 'grade' || scope === 'class'"
          v-model="filterGrade"
          placeholder="选择年级"
          size="default"
          @change="onGradeChange"
        >
          <el-option
            v-for="g in gradeOptions"
            :key="g"
            :value="g"
            :label="g"
          />
        </el-select>
        <el-select
          v-if="scope === 'class'"
          v-model="filterClassId"
          placeholder="选择班级"
          size="default"
          @change="loadData"
        >
          <el-option
            v-for="c in filteredClassOptions"
            :key="c.id"
            :value="c.id"
            :label="(c.grade || '') + c.className"
          />
        </el-select>
      </div>
    </div>

    <div class="ranking-area">
      <!-- 前三名领奖台 -->
      <div v-if="list.length >= 3" class="podium">
        <div class="podium-item second">
          <div class="podium-avatar">{{ list[1].studentName?.[0] || '?' }}</div>
          <div class="podium-name">{{ list[1].studentName }}</div>
          <div class="podium-class">{{ (list[1].grade || '') + list[1].className }}</div>
          <div class="podium-score">{{ list[1].moralScore }} 分</div>
          <div class="podium-badge">🥈</div>
        </div>
        <div class="podium-item first">
          <div class="podium-avatar">{{ topN >= 1 ? list[0].studentName?.[0] || '?' : '?' }}</div>
          <div class="podium-name">{{ list[0].studentName }}</div>
          <div class="podium-class">{{ (list[0].grade || '') + list[0].className }}</div>
          <div class="podium-score">{{ list[0].moralScore }} 分</div>
          <div class="podium-badge">🥇</div>
        </div>
        <div class="podium-item third">
          <div class="podium-avatar">{{ list[2].studentName?.[0] || '?' }}</div>
          <div class="podium-name">{{ list[2].studentName }}</div>
          <div class="podium-class">{{ (list[2].grade || '') + list[2].className }}</div>
          <div class="podium-score">{{ list[2].moralScore }} 分</div>
          <div class="podium-badge">🥉</div>
        </div>
      </div>

      <!-- 完整榜单 -->
      <div v-loading="loading" class="rank-table-wrap">
        <div v-if="list.length === 0 && !loading" class="empty-state">
          <el-empty description="暂无德育积分记录" :image-size="80">
            <span style="font-size: var(--fs-xs); color: var(--text-secondary)">教师通过班级圈表扬学生后，积分将自动累计</span>
          </el-empty>
        </div>
        <div v-else class="rank-list">
          <div
            v-for="(item, i) in list"
            :key="item.studentId"
            class="rank-row"
            :class="{ 'top-three': i < 3 }"
          >
            <span class="rank-num" :class="'rank-' + (i + 1)">
              {{ i + 1 }}
            </span>
            <div class="rank-avatar">{{ item.studentName?.[0] || '?' }}</div>
            <div class="rank-info">
              <span class="rank-name">{{ i < topN ? item.studentName : '匿名同学' }}</span>
              <span class="rank-class">{{ (item.grade || '') + item.className }}</span>
            </div>
            <span class="rank-score">{{ item.moralScore }} <span class="score-unit">德育分</span></span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useUserStore } from '@/stores/user';
import { getMoralRanking } from '@/api/credit';
import { getUserInfo } from '@/api/user';
import { getGrades } from '@/api/settings';
import { getClassList } from '@/api/classes';

const userStore = useUserStore();
const isStudent = computed(() => userStore.isStudent);

const list = ref([]);
const loading = ref(false);

// 三级筛选
const topN = ref(20);

const scope = ref('school');
const filterGrade = ref('');
const filterClassId = ref(null);
const studentClassName = ref('');
const gradeOptions = ref([]);
const classOptions = ref([]);

// 学生默认看自己班级
const studentClassId = computed(() => {
  if (isStudent.value) {
    const cls = userStore.teacherSummary?.teachingClasses;
    return null; // 学生身份从 auth/info 获取 classId
  }
  return null;
});

const filteredClassOptions = computed(() => {
  if (!filterGrade.value) return classOptions.value;
  return classOptions.value.filter((c) => c.grade === filterGrade.value);
});

const switchScope = (s) => {
  scope.value = s;
  if (s === 'school') {
    filterGrade.value = '';
    filterClassId.value = null;
  } else if (s === 'grade') {
    filterClassId.value = null;
  }
  loadData();
};

const onTopNChange = () => loadData();

const onGradeChange = () => {
  filterClassId.value = null;
  if (scope.value === 'grade') loadData();
};

const loadData = async () => {
  loading.value = true;
  try {
    const params = { limit: 50 };
    if (scope.value === 'class' && filterClassId.value) params.classId = filterClassId.value;
    if ((scope.value === 'grade' || scope.value === 'class') && filterGrade.value)
      params.grade = filterGrade.value;
    const res = await getMoralRanking(params);
    if (res.code === 200) list.value = (res.data || []).slice(0, topN.value || undefined);
  } finally {
    loading.value = false;
  }
};

onMounted(async () => {
  // 学生默认定位到班级
  if (isStudent.value) {
    try {
      const infoRes = await getUserInfo();
      if (infoRes.code === 200 && infoRes.data.classId) {
        scope.value = 'class';
        filterClassId.value = infoRes.data.classId;
        filterGrade.value = infoRes.data.grade || '';
        studentClassName.value = infoRes.data.className || '';
      }
    } catch {
      /* */
    }
  }
  loadData();
  // 加载筛选选项
  try {
    const [gradeRes, clsRes] = await Promise.all([getGrades(), getClassList()]);
    if (gradeRes.code === 200) gradeOptions.value = (gradeRes.data || []).map((g) => g.gradeName);
    if (clsRes.code === 200)
      classOptions.value = (clsRes.data.records || []).map((c) => ({
        id: c.id,
        className: c.className,
        grade: c.grade || '',
      }));
  } catch {
    /* */
  }
});
</script>

<style scoped lang="scss">
.moral-ranking-container {
  max-width: 800px;
  margin: 0 auto;
  padding: var(--spacing-lg, 24px);
}

.page-header {
  text-align: center;
  margin-bottom: 24px;
  .page-title {
    font-size: var(--fs-2xl, 22px);
    color: var(--text-primary);
    margin: 0 0 4px;
  }
  .page-subtitle {
    font-size: var(--fs-sm, 13px);
    color: var(--text-secondary);
  }
  .top-n-selector {
    margin-top: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

.filter-bar {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.scope-tabs {
  display: flex;
  gap: 6px;
  button {
    padding: 8px 20px;
    border: 1.5px solid var(--border-light, #e4e7ed);
    background: var(--bg-card, var(--bg-card));
    border-radius: var(--radius-md, 8px);
    font-size: var(--fs-md);
    color: var(--text-secondary);
    cursor: pointer;
    transition: all 0.2s;
    &:hover {
      border-color: var(--primary-color);
      color: var(--primary-color);
    }
    &.active {
      background: var(--primary-color);
      color: var(--bg-card);
      border-color: var(--primary-color);
      font-weight: 600;
    }
  }
}

.scope-selects {
  display: flex;
  gap: 10px;
}
.student-scope-tag {
  font-size: var(--fs-md);
  color: var(--primary-color);
  font-weight: 600;
  background: var(--primary-light);
  padding: 8px 16px;
  border-radius: var(--radius-md, 8px);
}

// 领奖台
.podium {
  display: flex;
  justify-content: center;
  align-items: flex-end;
  gap: 12px;
  margin-bottom: 28px;
  .podium-item {
    text-align: center;
    padding: 16px 12px 12px;
    background: var(--bg-card);
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-sm);
    width: 160px;
    transition: transform 0.3s;
    .podium-avatar {
      width: 48px;
      height: 48px;
      border-radius: 50%;
      background: var(--primary-gradient);
      color: var(--bg-card);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: var(--fs-xl);
      font-weight: 700;
      margin: 0 auto 8px;
    }
    .podium-name {
      font-size: var(--fs-md);
      font-weight: 600;
      color: var(--text-primary);
    }
    .podium-class {
      font-size: var(--fs-xs);
      color: var(--text-secondary);
      margin: 2px 0;
    }
    .podium-score {
      font-size: var(--fs-lg);
      font-weight: 700;
      color: var(--el-color-warning);
    }
    .podium-badge {
      font-size: 28px;
      margin-top: 4px;
    }
    &.first {
      width: 180px;
      padding: 20px 16px 16px;
      order: 1;
      .podium-avatar {
        width: 56px;
        height: 56px;
        font-size: var(--fs-2xl);
      }
    }
    &.second {
      order: 0;
    }
    &.third {
      order: 2;
    }
  }
}

// 榜单列表
.rank-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.rank-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--bg-card);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  &.top-three {
    background: var(--bg-warning-light);
  }
  .rank-num {
    width: 28px;
    height: 28px;
    border-radius: var(--radius-full);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: var(--fs-sm);
    font-weight: 700;
    color: var(--text-secondary);
    background: var(--bg-secondary);
    &.rank-1 {
      background: var(--gold, #ffd700);
      color: var(--bg-card);
    }
    &.rank-2 {
      background: var(--silver, #c0c0c0);
      color: var(--bg-card);
    }
    &.rank-3 {
      background: var(--bronze, #cd7f32);
      color: var(--bg-card);
    }
  }
  .rank-avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background: var(--primary-color);
    color: var(--text-on-primary);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: var(--fs-md);
    font-weight: 600;
    flex-shrink: 0;
  }
  .rank-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    .rank-name {
      font-size: var(--fs-md);
      font-weight: 500;
      color: var(--text-primary);
    }
    .rank-class {
      font-size: var(--fs-xs);
      color: var(--text-secondary);
    }
  }
  .rank-score {
    font-size: var(--fs-lg);
    font-weight: 700;
    color: var(--el-color-warning);
    .score-unit {
      font-size: var(--fs-xs);
      font-weight: 400;
      color: var(--text-secondary);
    }
  }
}

.empty-state {
  padding: 40px 0;
}

@media (max-width: 768px) {
  .moral-ranking-container {
    padding: var(--spacing-md, 16px);
  }
  .scope-tabs button {
    padding: 6px 14px;
    font-size: var(--fs-sm);
  }
  .podium {
    gap: 6px;
    .podium-item {
      width: 100px;
      padding: 12px 8px;
      .podium-avatar {
        width: 36px;
        height: 36px;
        font-size: var(--fs-lg);
      }
      .podium-name {
        font-size: var(--fs-sm);
      }
      .podium-score {
        font-size: var(--fs-md);
      }
      .podium-badge {
        font-size: 22px;
      }
      &.first {
        width: 110px;
        padding: 14px 8px;
        .podium-avatar {
          width: 42px;
          height: 42px;
          font-size: var(--fs-lg);
        }
      }
    }
  }
  .rank-row {
    padding: 10px 12px;
    gap: 8px;
  }
  .scope-selects {
    flex-direction: column;
    width: 100%;
  }
  .scope-selects :deep(.el-select) {
    width: 100%;
  }
}
</style>

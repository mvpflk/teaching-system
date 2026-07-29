/**
 * 用户状态管理 — 刷新后从 localStorage 恢复角色信息，避免竞态导致权限误判。
 */
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { getUserInfo, login as apiLogin, logout as apiLogout } from '@/api/user';

// 持久化的 key
const USER_INFO_KEY = 'user_info';

// 从 localStorage 恢复（仅存储 role/realName 等非敏感字段，不含密码）
function loadFromStorage() {
  try {
    const raw = localStorage.getItem(USER_INFO_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

function saveToStorage(data) {
  try {
    // 只持久化角色和基本信息，不存 token（token 单独存）
    localStorage.setItem(
      USER_INFO_KEY,
      JSON.stringify({
        role: data.role,
        realName: data.realName,
        teacherSummary: data.teacherSummary || null,
        classId: data.classId || null,
        ts: Date.now(),
      })
    );
  } catch {
    /* 静默 */
  }
}

function clearStorage() {
  localStorage.removeItem(USER_INFO_KEY);
}

export const useUserStore = defineStore('user', () => {
  // 从 localStorage 恢复角色（避免刷新后 role 为空导致 isStudent/isTeacher 误判）
  const saved = loadFromStorage();

  const token = ref(localStorage.getItem('token') || '');
  const userInfo = ref(null);
  const role = ref(saved?.role || '');
  const teacherSummary = ref(saved?.teacherSummary || null);
  const isTeachingGroupLeader = ref(false);
  const isLessonPrepGroupLeader = ref(false);
  const canAccessRemedial = ref(false);

  // 计算属性
  const isLoggedIn = computed(() => !!token.value);
  const isSuperAdmin = computed(() => role.value === 'SUPER_ADMIN');
  const isAdmin = computed(() => role.value === 'SUPER_ADMIN' || role.value === 'ADMIN');
  const isRegionAdmin = computed(() => role.value === 'REGION_ADMIN');
  const isRegionAdminOrAbove = computed(
    () => role.value === 'REGION_ADMIN' || role.value === 'ADMIN' || role.value === 'SUPER_ADMIN'
  );
  const isInspector = computed(() => role.value === 'INSPECTOR');
  const isTeacher = computed(
    () =>
      role.value === 'TEACHER' ||
      role.value === 'HEAD_TEACHER' ||
      role.value === 'ADMIN' ||
      role.value === 'SUPER_ADMIN'
  );
  const isStudent = computed(() => role.value === 'STUDENT');
  const isParent = computed(() => role.value === 'PARENT');
  const isHeadTeacher = computed(() => teacherSummary.value?.isHeadTeacher === true);
  const isComputerMajor = computed(() => userInfo.value?.subjectId === 4);
  const teachingClassIds = computed(() => {
    const classes = teacherSummary.value?.teachingClasses || [];
    const headClassId = teacherSummary.value?.headClassId;
    const ids = classes.map((c) => c.classId);
    if (headClassId) ids.push(headClassId);
    return [...new Set(ids)];
  });

  // 是否显示实训方案：仅职高非文化课（语数英）教师
  const showPracticePlans = computed(() => {
    const classes = teacherSummary.value?.teachingClasses || [];
    for (const c of classes) {
      const subject = c.subject || '';
      // 判断是否职高学段：科目含[职高]标记，或无学段后缀(专业科目默认职高)
      const isVocational =
        subject.includes('[职高]') ||
        (!subject.includes('[普高]') && !subject.includes('[初中]') && !subject.includes('[小学]'));
      if (!isVocational) continue;
      // 排除文化课
      const bareName = subject.replace(/\[.*?\]/, '');
      if (['语文', '数学', '英语'].includes(bareName)) continue;
      return true;
    }
    return false;
  });

  // 是否显示 AI 组卷/诊断/质量分析：任意职高学科教师或管理员均可访问
  const showAiCultureModules = computed(() => {
    if (isAdmin.value) return true;
    const classes = teacherSummary.value?.teachingClasses || [];
    for (const c of classes) {
      const subject = c.subject || '';
      const isVocational =
        subject.includes('[职高]') ||
        (!subject.includes('[普高]') && !subject.includes('[初中]') && !subject.includes('[小学]'));
      if (isVocational) return true;
    }
    return false;
  });

  // 当前教师的所有任教学科列表
  const teachingSubjects = computed(() => {
    const classes = teacherSummary.value?.teachingClasses || [];
    return [...new Set(classes.map((c) => c.subject).filter(Boolean))];
  });

  // 登录
  async function login(username, password) {
    const res = await apiLogin({ username, password });
    if (res.code === 200) {
      token.value = res.data.token;
      role.value = res.data.role;
      localStorage.setItem('token', res.data.token);
      if (res.data.teacherSummary) {
        teacherSummary.value = res.data.teacherSummary;
      }
      saveToStorage({
        role: res.data.role,
        realName: res.data.realName,
        teacherSummary: res.data.teacherSummary,
      });
      await getInfo();
      return res.data;
    }
    throw new Error(res.message || '登录失败');
  }

  // 获取用户信息
  async function getInfo() {
    if (!token.value) return;
    try {
      const res = await getUserInfo();
      if (res.code === 200) {
        userInfo.value = res.data;
        role.value = res.data.role;
        if (res.data.teacherSummary) {
          teacherSummary.value = res.data.teacherSummary;
        }
        // 持久化到 localStorage，确保刷新后角色立即可用
        saveToStorage({
          role: res.data.role,
          realName: res.data.realName,
          teacherSummary: res.data.teacherSummary,
          classId: res.data.classId,
        });
      }
    } catch (error) {
      // 网络异常时从 localStorage 兜底，不阻塞页面渲染
      console.error('获取用户信息失败:', error);
    }
  }

  async function checkTeachingGroupLeader() {
    if (!isTeacher.value) {
      isTeachingGroupLeader.value = false;
      return;
    }
    try {
      const { getMyResearchGroup } = await import('@/api/teacherResearch');
      const res = await getMyResearchGroup();
      isTeachingGroupLeader.value = res.data != null && res.code === 200;
    } catch {
      isTeachingGroupLeader.value = false;
    }
  }

  async function checkLessonPrepGroupLeader() {
    if (!isTeacher.value) {
      isLessonPrepGroupLeader.value = false;
      return;
    }
    try {
      const { getMyLessonPrepGroup } = await import('@/api/teacherLessonPrep');
      const res = await getMyLessonPrepGroup();
      isLessonPrepGroupLeader.value = res.data != null && res.code === 200;
    } catch {
      isLessonPrepGroupLeader.value = false;
    }
  }

  // 登出
  async function logout() {
    try {
      await apiLogout();
    } catch (e) {
      /* 忽略 */
    }
    token.value = '';
    userInfo.value = null;
    role.value = '';
    teacherSummary.value = null;
    isTeachingGroupLeader.value = false;
    isLessonPrepGroupLeader.value = false;
    localStorage.removeItem('token');
    clearStorage();
  }

  async function fetchRemedialAccess() {
    try {
      const { checkRemedialAccess } = await import('@/api/precision');
      const res = await checkRemedialAccess();
      canAccessRemedial.value = res?.data?.canAccess === true;
    } catch {
      canAccessRemedial.value = false;
    }
  }

  return {
    token,
    userInfo,
    role,
    teacherSummary,
    isTeachingGroupLeader,
    isLessonPrepGroupLeader,
    canAccessRemedial,
    isLoggedIn,
    isSuperAdmin,
    isTeacher,
    isStudent,
    isAdmin,
    isRegionAdmin,
    isRegionAdminOrAbove,
    isInspector,
    isParent,
    isHeadTeacher,
    isComputerMajor,
    teachingClassIds,
    showPracticePlans,
    showAiCultureModules,
    teachingSubjects,
    login,
    getInfo,
    logout,
    checkTeachingGroupLeader,
    checkLessonPrepGroupLeader,
    fetchRemedialAccess,
  };
});

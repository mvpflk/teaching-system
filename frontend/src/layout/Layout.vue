<template>
  <el-container class="layout-container">
    <div v-if="mobileVisible && isMobile" class="aside-overlay" @click="closeMobileMenu" />

    <el-aside
      :width="asideWidth"
      class="aside"
      :class="{ 'aside-mobile': isMobile && mobileVisible, 'aside-tablet': isTablet }"
      @mouseenter="onSidebarEnter"
      @mouseleave="onSidebarLeave"
    >
      <div class="logo" :class="{ collapse: effectiveCollapse }">
        <div class="logo-icon">
          <svg
            width="28"
            height="28"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M22 10v6M2 10l10-5 10 5M6 12v5h12v-5" />
            <circle cx="12" cy="15" r="2" />
          </svg>
        </div>
        <span v-if="!effectiveCollapse" class="logo-text">教学管理</span>
      </div>
      <SidebarMenu
        :active-menu="activeMenu"
        :collapse="effectiveCollapse"
        :is-teacher="isTeacher"
        :is-student="isStudent"
        :is-admin="isAdmin"
        :is-inspector="isInspector"
        :is-super-admin="isSuperAdmin"
        :is-parent="isParent"
        :is-head-teacher="isHeadTeacher"
        :is-teaching-group-leader="userStore.isTeachingGroupLeader"
        :is-lesson-prep-group-leader="userStore.isLessonPrepGroupLeader"
        :show-practice-plans="userStore.showPracticePlans"
        :show-ai-culture-modules="userStore.showAiCultureModules"
        :can-access-remedial="userStore.canAccessRemedial"
        :features="features"
        @select="onMenuSelect"
      />
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-button text class="collapse-btn" @click="toggleCollapse">
            <el-icon size="20"><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
          </el-button>
          <el-breadcrumb separator="›" class="breadcrumb">
            <el-breadcrumb-item :to="{ path: '/home' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta.title !== '首页'">
              {{ route.meta.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-button
            text
            circle
            class="icon-btn"
            @click="toggleDark"
          >
            <el-icon size="20"><Moon v-if="!isDark" /><Sunny v-else /></el-icon>
          </el-button>

          <NotificationPopover />

          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" :src="userInfo?.avatar" class="user-avatar">
                {{ userInfo?.realName?.charAt(0) }}
              </el-avatar>
              <span class="username">{{ userInfo?.realName }}</span>
              <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item command="password">
                  <el-icon><Lock /></el-icon>修改密码
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main" :class="route.meta.layoutWidth ? `layout-${route.meta.layoutWidth}` : ''">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
    <FeedbackButton />
    <AgentAssistant v-if="!isAgentChatPage" />

    <MobileBottomNav
      v-if="isMobile"
      :is-teacher="isTeacher"
      :is-student="isStudent"
      :is-admin="isAdmin"
      :is-parent="isParent"
      :is-inspector="isInspector"
      @navigate="closeMobileMenu"
    />
    <el-dialog
      v-model="pwdVisible"
      title="修改密码"
      width="420px"
      append-to-body
      destroy-on-close
    >
      <el-form
        ref="pwdFormRef"
        :model="pwdForm"
        :rules="pwdRules"
        label-position="top"
      >
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input
            v-model="pwdForm.oldPassword"
            type="password"
            show-password
            placeholder="输入当前密码"
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="pwdForm.newPassword"
            type="password"
            show-password
            placeholder="至少6位"
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="pwdForm.confirmPassword"
            type="password"
            show-password
            placeholder="再次输入新密码"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdSubmitting" @click="doChangePassword">确定修改</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { updatePassword } from '@/api/profile'
import { useUserStore } from '@/stores/user'
import { useSettingsStore } from '@/stores/settings'
import SidebarMenu from '@/components/layout/SidebarMenu.vue'
import NotificationPopover from '@/components/layout/NotificationPopover.vue'
import MobileBottomNav from '@/components/layout/MobileBottomNav.vue'
import FeedbackButton from '@/components/common/FeedbackButton.vue'
import AgentAssistant from '@/components/ai/AgentAssistant.vue'
import { useIsMobile } from '@/composables/useIsMobile'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const settingsStore = useSettingsStore()

const isCollapse = ref(false)
const mobileVisible = ref(false)
const { isMobile, isTablet } = useIsMobile()

const sidebarHover = ref(false)
const onSidebarEnter = () => { if (isTablet.value && isCollapse.value) sidebarHover.value = true }
const onSidebarLeave = () => { sidebarHover.value = false }
const effectiveCollapse = computed(() => {
  if (isTablet.value && sidebarHover.value) return false
  if (isTablet.value) return true
  return isCollapse.value
})

if (isTablet.value) isCollapse.value = true

const isDark = ref(localStorage.getItem('theme') === 'dark')
const applyTheme = () => {
  if (isDark.value) document.documentElement.classList.add('dark')
  else document.documentElement.classList.remove('dark')
}
const toggleDark = () => {
  isDark.value = !isDark.value
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
  applyTheme()
}
applyTheme()

const userInfo = computed(() => userStore.userInfo)
const isTeacher = computed(() => userStore.isTeacher)
const isStudent = computed(() => userStore.isStudent)
const isAdmin = computed(() => userStore.isAdmin)
const isInspector = computed(() => userStore.isInspector)
const isSuperAdmin = computed(() => userStore.isSuperAdmin)
const isParent = computed(() => userStore.isParent)
const isHeadTeacher = computed(() => userStore.isHeadTeacher)
const features = computed(() => settingsStore.features)
const activeMenu = computed(() => route.path)
const isAgentChatPage = computed(() => route.path === '/teacher/agent/chat')

const asideWidth = computed(() => {
  if (isMobile.value && mobileVisible.value) return '200px'
  if (effectiveCollapse.value) return '64px'
  return '200px'
})

onMounted(() => {
  userStore.getInfo()
  userStore.checkTeachingGroupLeader()
  userStore.checkLessonPrepGroupLeader()
  settingsStore.fetchFeatureFlags()
  if (userStore.isStudent || userStore.isTeacher) {
    userStore.fetchRemedialAccess()
  }
})

const toggleCollapse = () => {
  if (isMobile.value) mobileVisible.value = !mobileVisible.value
  else isCollapse.value = !isCollapse.value
}

const closeMobileMenu = () => { mobileVisible.value = false }
const onMenuSelect = () => { if (isMobile.value) mobileVisible.value = false }

const handleCommand = async (command) => {
  switch (command) {
    case 'profile': router.push('/profile'); break
    case 'password': pwdForm.oldPassword = ''; pwdForm.newPassword = ''; pwdForm.confirmPassword = ''; pwdVisible.value = true; break
    case 'logout':
      try {
        await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
        await userStore.logout()
        router.push('/login')
        ElMessage.success('已退出登录')
      } catch { /* 取消 */ }
      break
  }
}

const pwdVisible = ref(false)
const pwdSubmitting = ref(false)
const pwdFormRef = ref(null)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请确认新密码', trigger: 'blur' }],
}

const doChangePassword = async () => {
  if (pwdSubmitting.value) return
  if (!pwdFormRef.value) return
  try { await pwdFormRef.value.validate() } catch { return }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    ElMessage.warning('两次密码不一致'); return
  }
  pwdSubmitting.value = true
  try {
    const res = await updatePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    if (res.code === 200) {
      ElMessage.success('密码已修改，请重新登录')
      pwdVisible.value = false
      await userStore.logout()
      router.push('/login')
    } else {
      ElMessage.error(res.message || '修改失败')
    }
  } catch { ElMessage.error('修改失败') }
  finally { pwdSubmitting.value = false }
}
</script>

<style scoped lang="scss" src="./Layout.css"></style>
<style lang="scss">
.el-popper.is-light.el-menu-popper {
  background: var(--bg-card) !important;
  border: 1px solid var(--border-light) !important;
  box-shadow: 0 4px 16px rgba(0,0,0,0.08) !important;
}
.el-menu--popup {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  padding: 4px !important;
  .el-menu-item {
    height: 38px !important; line-height: 38px !important;
    color: var(--text-regular) !important;
    border-radius: var(--radius-sm) !important;
    margin: 1px 0 !important;
    padding: 0 32px !important;
    &:hover { background: var(--bg-secondary) !important; color: var(--primary-color) !important; }
    &.is-active {
      background: var(--primary-light) !important;
      color: var(--primary-color) !important;
      font-weight: 500 !important;
      position: relative;
      &::before {
        content: ''; position: absolute; left: 0;
        top: 2px; bottom: 2px; width: 4px;
        background: var(--primary-color);
        border-radius: 0 4px 4px 0;
      }
    }
  }
}
</style>

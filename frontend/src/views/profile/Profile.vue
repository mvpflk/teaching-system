<template>
  <div class="page-container">
    <div class="profile-layout">
      <div class="profile-sidebar">
        <div class="user-card">
          <div class="user-card-bg" />
          <div class="user-card-body">
            <ProfileAvatar :avatar-url="avatarUrl" :user-name="userInfo?.realName" @updated="onAvatarUpdated" />

            <h3 class="user-name">{{ userInfo?.realName }}</h3>
            <el-tag :type="role === 'ADMIN' ? 'danger' : role === 'TEACHER' ? 'warning' : 'success'" size="small" effect="plain">
              {{ { ADMIN: '管理员', TEACHER: '教师', HEAD_TEACHER: '班主任', STUDENT: '学生' }[role] || role }}
            </el-tag>
            <div v-if="customTitle" style="margin-top:8px">
              <el-tag
                type="warning"
                size="small"
                effect="dark"
                style="font-size:var(--fs-sm);padding:2px 8px"
              >
                🎭 {{ customTitle }}
              </el-tag>
            </div>

            <el-divider style="margin:16px 0" />

            <div class="info-list">
              <div class="info-row">
                <span class="info-label">用户名</span>
                <span class="info-value">{{ userInfo?.username }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">邮箱</span>
                <span class="info-value">{{ userInfo?.email || '未设置' }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">手机</span>
                <span class="info-value">{{ userInfo?.phone || '未设置' }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="security-card">
          <h4>账号安全</h4>
          <div class="security-item">
            <div class="security-left">
              <el-icon size="16" style="color:var(--success-color)"><Lock /></el-icon>
              <span>登录密码</span>
            </div>
            <el-tag size="small" type="success" effect="plain">已设置</el-tag>
          </div>
          <div class="security-item">
            <div class="security-left">
              <el-icon size="16" style="color:var(--text-secondary)"><Message /></el-icon>
              <span>手机绑定</span>
            </div>
            <el-tag size="small" :type="userInfo?.phone ? 'success' : 'info'" effect="plain">
              {{ userInfo?.phone ? '已绑定' : '未绑定' }}
            </el-tag>
          </div>
        </div>
      </div>

      <div class="profile-main">
        <el-tabs v-model="activeTab" class="profile-tabs">
          <el-tab-pane label="✏️ 编辑资料" name="info">
            <el-form
              ref="infoFormRef"
              :model="infoForm"
              :rules="infoRules"
              label-position="top"
              class="profile-form"
            >
              <el-form-item v-if="role !== 'STUDENT'" label="真实姓名" prop="realName">
                <el-input v-model="infoForm.realName" placeholder="请输入真实姓名" />
              </el-form-item>
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="infoForm.email" placeholder="example@school.edu" />
              </el-form-item>
              <el-form-item label="手机号" prop="phone">
                <el-input v-model="infoForm.phone" placeholder="13800138000" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="saving" @click="saveProfile">保存修改</el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="🔑 修改密码" name="password">
            <el-form
              ref="pwdFormRef"
              :model="pwdForm"
              :rules="pwdRules"
              label-position="top"
              class="profile-form"
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
              <el-form-item>
                <el-button type="primary" :loading="changingPwd" @click="changePassword">修改密码</el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useFormRules } from '@/composables/useFormRules'
import { getProfile, updateProfile, updatePassword } from '@/api/profile'
import { getCustomTitle } from '@/api/credit'
import ProfileAvatar from './ProfileAvatar.vue'

const userStore = useUserStore()
const router = useRouter()
const userInfo = computed(() => userStore.userInfo)
const role = computed(() => userStore.role)
const avatarUrl = ref('')
const route = useRoute()
const activeTab = ref(route.query.tab || 'info')
const saving = ref(false)
const changingPwd = ref(false)
const { required, email: emailRule, phone: phoneRule, minLength } = useFormRules()

const infoFormRef = ref(null)
const pwdFormRef = ref(null)

const infoRules = {
  realName: [required('真实姓名')],
  email: [required('邮箱'), emailRule()],
  phone: [required('手机号'), phoneRule()],
}
const pwdRules = {
  oldPassword: [required('当前密码')],
  newPassword: [required('新密码'), minLength(6, '新密码')],
  confirmPassword: [required('确认新密码')],
}

const infoForm = reactive({ realName: '', email: '', phone: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const customTitle = ref('')

const loadProfile = async () => {
  const res = await getProfile()
  if (res.code === 200) {
    infoForm.realName = res.data.realName || ''
    infoForm.email = res.data.email || ''
    infoForm.phone = res.data.phone || ''
    avatarUrl.value = res.data.avatarUrl || ''
  }
}

const onAvatarUpdated = (url) => { avatarUrl.value = url; userStore.getInfo() }

const saveProfile = async () => {
  if (saving.value) return
  if (!infoFormRef.value) return
  try { await infoFormRef.value.validate() } catch { return }
  saving.value = true
  try {
    const res = await updateProfile({ ...infoForm })
    if (res.code === 200) { ElMessage.success('已保存'); await userStore.getInfo() }
    else { ElMessage.error(res.message || '保存失败') }
  } catch { ElMessage.error('保存失败') }
  finally { saving.value = false }
}

const changePassword = async () => {
  if (changingPwd.value) return
  if (!pwdFormRef.value) return
  try {
    await pwdFormRef.value.validate()
    if (pwdForm.newPassword !== pwdForm.confirmPassword) {
      ElMessage.warning('两次密码不一致'); return
    }
  } catch { return }
  changingPwd.value = true
  try {
    const res = await updatePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    if (res.code === 200) {
      ElMessage.success('密码已修改，请重新登录')
      await userStore.logout()
      router.push('/login')
    } else {
      ElMessage.error(res.message || '修改失败')
    }
  } catch { ElMessage.error('修改失败') }
  finally { changingPwd.value = false }
}

onMounted(() => {
  loadProfile()
  getCustomTitle().then(r => {
    if (r.code === 200 && r.data.valid) customTitle.value = r.data.customTitle
  }).catch(() => {})
})
</script>

<style scoped lang="scss">
.profile-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 20px;
  max-width: 960px;
  margin: 0 auto;
}

.user-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  overflow: hidden;
  border: 0.5px solid var(--border-color);
  .user-card-bg { height: 80px; background: var(--primary-gradient); }
  .user-card-body { text-align: center; padding: 0 20px 20px; margin-top: -44px; }
}

.user-name { font-size: var(--fs-lg); font-weight: 500; margin: 10px 0 6px; color: var(--text-primary); }

.info-list { text-align: left;
  .info-row {
    display: flex; justify-content: space-between; padding: 8px 0; font-size: var(--fs-sm);
    + .info-row { border-top: 1px solid var(--border-light); }
    .info-label { color: var(--text-secondary); }
    .info-value { color: var(--text-primary); font-weight: 500; }
  }
}

.security-card {
  background: var(--bg-card); border-radius: var(--radius-lg); padding: 16px 20px;
  border: 0.5px solid var(--border-color); margin-top: 16px;
  h4 { font-size: var(--fs-md); font-weight: 500; margin: 0 0 12px; color: var(--text-primary); }
  .security-item {
    display: flex; justify-content: space-between; align-items: center; padding: 8px 0;
    + .security-item { border-top: 1px solid var(--border-light); }
    .security-left { display: flex; align-items: center; gap: 8px; font-size: var(--fs-sm); color: var(--text-regular); }
  }
}

.profile-main { background: var(--bg-card); border-radius: var(--radius-lg); padding: 24px; border: 0.5px solid var(--border-color); }
.profile-tabs { :deep(.el-tabs__item) { font-size: var(--fs-md); } }
.profile-form { max-width: 480px; margin-top: 16px; }

@media (max-width: 768px) {
  .profile-layout { grid-template-columns: 1fr; }
  :deep(.el-tabs__item) { font-size: var(--fs-sm); padding: 0 12px !important; }
  :deep(.el-tabs__nav-wrap) { overflow-x: auto; }
}
</style>

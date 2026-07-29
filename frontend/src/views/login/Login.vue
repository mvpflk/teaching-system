<template>
  <div class="login-page">
    <div class="login-wrapper">
      <LoginBrand />

      <div class="login-section">
        <div class="login-card">
          <div class="login-header">
            <h2>欢迎回来</h2>
            <p>请登录您的账号</p>
          </div>

          <el-form
            ref="formRef"
            :model="loginForm"
            :rules="rules"
            class="login-form"
            @submit.prevent="handleLogin"
          >
            <el-form-item prop="username">
              <el-input
                ref="usernameRef"
                v-model="loginForm.username"
                placeholder="请输入用户名"
                size="large"
                clearable
              >
                <template #prefix>
                  <el-icon><User /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                size="large"
                show-password
                clearable
                @keyup.enter="handleLogin"
              >
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                :loading="loading"
                size="large"
                class="login-btn"
                @click="handleLogin"
              >
                {{ loading ? '登录中...' : '登 录' }}
              </el-button>
            </el-form-item>
          </el-form>

          <div class="login-footer">
            <p class="login-forgot">忘记密码？请联系管理员重置</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useUserStore } from '@/stores/user';
import { useKeyboardFix } from '@/composables/useKeyboardFix';
import LoginBrand from './LoginBrand.vue';

const router = useRouter();
const userStore = useUserStore();

const formRef = ref(null);
const usernameRef = ref(null);
onMounted(() => nextTick(() => usernameRef.value?.focus()));
useKeyboardFix();
const loading = ref(false);

const loginForm = reactive({
  username: '',
  password: '',
});

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
};

const handleLogin = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    loading.value = true;
    try {
      await userStore.login(loginForm.username, loginForm.password);
      ElMessage.success('登录成功');
      router.push('/home');
    } catch (error) {
      ElMessage.error(error.message || '登录失败，请检查用户名和密码');
    } finally {
      loading.value = false;
    }
  });
};
</script>

<style scoped lang="scss">
.login-page {
  width: 100%;
  min-height: 100vh;
  background: var(--bg-section);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.login-wrapper {
  display: flex;
  width: 960px;
  max-width: 100%;
  min-height: 560px;
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  /* B4 新增：主色氛围光晕（品牌色专业信任感） */
  box-shadow:
    0 8px 32px rgba(67, 97, 238, 0.08),
    var(--shadow-lg);
  overflow: hidden;
  position: relative;
  animation: login-enter var(--dur-slow) var(--ease-enter);
}
/* B4 新增：左下角装饰圆（只在视觉底层起氛围作用，不遮挡内容） */
.login-wrapper::before {
  content: '';
  position: absolute;
  width: 320px;
  height: 320px;
  background: radial-gradient(circle, rgba(67, 97, 238, 0.06) 0%, transparent 70%);
  border-radius: 50%;
  bottom: -160px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 0;
  pointer-events: none;
}

@keyframes login-enter {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-section {
  width: 420px;
  max-width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  position: relative;
  z-index: 1;
}

.login-card {
  width: 100%;
  max-width: 340px;
}

.login-header {
  margin-bottom: 32px;
  h2 {
    font-size: var(--fs-2xl);
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 8px;
    letter-spacing: -0.01em;
  }
  p {
    font-size: var(--fs-md);
    color: var(--text-secondary);
    line-height: var(--lh-normal);
  }
}

.login-form {
  :deep(.el-input__wrapper) {
    padding: 4px 12px;
    border-radius: var(--radius-md);
    transition:
      box-shadow var(--transition-fast),
      border-color var(--transition-fast);
  }
  /* B4 新增：输入框 focus 外发光强化（3px → 4px） */
  :deep(.el-input__wrapper.is-focus) {
    box-shadow: 0 0 0 4px rgba(67, 97, 238, 0.15) !important;
    border-color: var(--primary-color);
  }
  :deep(.el-input__prefix) {
    margin-right: 8px;
    .el-icon {
      font-size: var(--fs-lg);
      color: var(--text-placeholder);
    }
  }
  :deep(.el-form-item) {
    margin-bottom: 22px;
  }
}

.login-btn {
  width: 100%;
  height: 46px;
  font-size: var(--fs-md);
  font-weight: 500;
  border-radius: var(--radius-md) !important;
  letter-spacing: 2px;
  position: relative;
  overflow: hidden;
  /* B4 新增：按钮加载 shimmer 骨架效果 */
  &.is-loading::after {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(
      90deg,
      transparent 0%,
      rgba(255, 255, 255, 0.25) 50%,
      transparent 100%
    );
    animation: shimmer 1.2s ease-in-out infinite;
    border-radius: inherit;
    pointer-events: none;
  }
}
@keyframes shimmer {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}

.login-footer {
  text-align: center;
  margin-top: 24px;
  p {
    font-size: var(--fs-xs);
    color: var(--text-placeholder);
  }
}
.login-forgot {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  margin-bottom: 12px;
}

/* B4 新增：reduced-motion 用户降级（登录不做动画，按钮不 shimmer） */
@media (prefers-reduced-motion: reduce) {
  .login-wrapper {
    animation: none !important;
  }
  .login-btn.is-loading::after {
    animation: none !important;
    display: none;
  }
}

@media (max-width: 768px) {
  .login-page {
    padding: 0;
    align-items: flex-start;
  }
  .login-wrapper {
    min-height: 100vh;
    border-radius: 0;
    box-shadow: none;
    flex-direction: column;
  }
  .login-wrapper::before {
    left: 50%;
    transform: translateX(-50%);
    bottom: -200px;
    opacity: 0.8;
  }
  .login-section {
    width: 100%;
    padding: 32px 24px;
  }
  .login-card {
    max-width: 100%;
  }
  .login-header h2 {
    font-size: var(--fs-xl);
  }
}

@media (min-width: 769px) and (max-width: 960px) {
  .login-section {
    width: 50%;
    padding: 32px 24px;
  }
  .login-card {
    max-width: 100%;
  }
  .login-wrapper::before {
    left: auto;
    right: -100px;
  }
}
</style>

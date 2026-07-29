<template>
  <div class="brand-section">
    <div class="brand-content">
      <!-- Logo 区域：有自定义 Logo 显示图片，否则显示默认 SVG -->
      <div class="brand-logo" :class="{ 'has-logo': logoUrl }">
        <img
          v-if="logoUrl"
          :src="logoUrl"
          alt="学校 Logo"
          class="logo-img"
          @error="logoUrl = ''"
        />
        <svg
          v-else
          width="42"
          height="42"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.5"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M22 10v6M2 10l10-5 10 5M6 12v5h12v-5" />
          <circle cx="12" cy="15" r="2" />
          <path d="M12 3v3" />
          <path d="M10 6h4" />
          <path d="M9 21l3-3 3 3" />
        </svg>
      </div>
      <h1 class="brand-title">南部县综合高级中学智慧教育管理系统</h1>
      <p class="brand-subtitle">教学管理 · 在线考试 · 作业批改 · 积分激励</p>
      <div class="brand-features">
        <div class="feature-item">
          <span class="feature-dot" />
          <span>一站式教学管理平台</span>
        </div>
        <div class="feature-item">
          <span class="feature-dot" />
          <span>支持在线考试与智能批改</span>
        </div>
        <div class="feature-item">
          <span class="feature-dot" />
          <span>师生论坛互动交流</span>
        </div>
      </div>
      <!-- C4 新增：登录品牌插画（线性风，与深色模式适配）
           BUGFIX-2：必须用 import + :src 绑定，Vite 不会解析静态 src 中的 @ alias，会 404 -->
      <img
        v-if="loginBrandSvg"
        class="brand-illustration"
        :src="loginBrandSvg"
        alt=""
        loading="lazy"
        aria-hidden="true"
      />
    </div>
    <div class="brand-bg-shapes">
      <div class="shape s1" />
      <div class="shape s2" />
      <div class="shape s3" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { getSystemLogo } from '@/api/system';
// BUGFIX-2：静态 import SVG → 打包后拿到真实 URL，避免 Vite 静态 src 的 @ alias 解析失败
import loginBrandSvg from '@/assets/illustrations/login-brand.svg';

const logoUrl = ref('');

onMounted(async () => {
  try {
    const r = await getSystemLogo();
    if (r.code === 200 && r.data?.url) {
      logoUrl.value = r.data.url;
    }
  } catch {
    // 加载失败，使用默认图标
  }
});
</script>

<style scoped lang="scss">
.brand-section {
  flex: 1;
  position: relative;
  background: var(--primary-color);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  overflow: hidden;

  .brand-content {
    position: relative;
    z-index: 2;
    color: var(--bg-card);
    text-align: center;
  }

  // 兼容旧版 brand-icon，新版用 brand-logo（圆形 80×80）
  .brand-logo {
    width: 80px;
    height: 80px;
    margin: 0 auto 24px;
    background: rgba(255, 255, 255, 0.15);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;

    &.has-logo {
      background: transparent;
      border: 2px solid rgba(255, 255, 255, 0.3);
    }

    .logo-img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  .brand-title {
    font-size: var(--fs-2xl);
    font-weight: 600;
    margin-bottom: 8px;
    letter-spacing: 0.02em;
    word-break: break-word;
  }

  .brand-subtitle {
    font-size: var(--fs-md);
    opacity: 0.8;
    margin-bottom: 36px;
  }

  .brand-features {
    display: flex;
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
    max-width: 260px;
    margin: 0 auto;
  }

  .feature-item {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: var(--fs-md);
    opacity: 0.9;

    .feature-dot {
      width: 8px;
      height: 8px;
      border-radius: var(--radius-full);
      background: var(--bg-card);
      flex-shrink: 0;
    }
  }
}

.brand-bg-shapes {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;

  .shape {
    position: absolute;
    border-radius: var(--radius-full);
    background: rgba(255, 255, 255, 0.06);
  }
  .s1 {
    width: 300px;
    height: 300px;
    top: -80px;
    right: -80px;
  }
  .s2 {
    width: 200px;
    height: 200px;
    bottom: -40px;
    left: -40px;
  }
  .s3 {
    width: 120px;
    height: 120px;
    bottom: 80px;
    right: 60px;
    background: rgba(255, 255, 255, 0.04);
  }
}

/* C4 新增：品牌插画（主色深底 → 反色变白/浅色线条） */
.brand-illustration {
  display: block;
  width: 100%;
  max-width: 320px;
  height: auto;
  margin: 28px auto 0;
  /* 主色深色背景下，靛蓝SVG全部转白，保持线性风且对比度足够 */
  filter: invert(1) saturate(0) brightness(1.2);
  opacity: 0.95;
  user-select: none;
  pointer-events: none;
}

@media (max-width: 768px) {
  .brand-section {
    padding: 32px 24px;
    .brand-logo {
      width: 64px;
      height: 64px;
      margin-bottom: 16px;
    }
    .brand-title {
      font-size: var(--fs-xl);
      line-height: 1.4;
      word-break: break-word;
    }
    .brand-subtitle {
      font-size: var(--fs-xs);
      margin-bottom: 20px;
    }
    .brand-features {
      display: none;
    }
    .brand-illustration {
      display: none;
    } /* 移动端隐藏插画，节省空间 */
  }
}
</style>

# 前端 UI/UX 美化实施计划（方案 B + C1/C4）

> 生成时间: 2026-07-29
> 选定方案: 方案 B（平衡优化）+ C1（全局动效体系）+ C4（品牌插画系统）
> 预估总工时: 约 10 天
> 实施模式: 分 4 期上线，每期独立验证可回滚

---

## 一、选择说明

| 模块                             | 是否纳入 | 说明                  |
| -------------------------------- | -------- | --------------------- |
| 方案 A（基础规范化 8 项）        | ✅ 全部  | B 方案包含 A 全部内容 |
| 方案 B1（首页响应式+卡片美化）   | ✅       | 高频页                |
| 方案 B2（统计卡片色条+数字优化） | ✅       | 高频页                |
| 方案 B3（批阅工作台三栏+快捷键） | ✅       | 教师核心工作流        |
| 方案 B4（登录页品牌光晕）        | ✅       | 全局第一印象          |
| 方案 B5（空状态 4 场景化）       | ✅       | 转化率提升            |
| 方案 B6（移动端底部导航质感）    | ✅       | 移动端入口            |
| 方案 C1（全局动效 Token 体系）   | ✅       | 统一微动效语义        |
| 方案 C2（骨架屏覆盖率）          | ❌       | 延后迭代              |
| 方案 C3（桌面沉浸布局）          | ❌       | 延后迭代              |
| 方案 C4（品牌插画系统 6 场景）   | ✅       | 记忆点+品牌感         |
| 方案 C5（表格行增强）            | ❌       | 延后迭代              |
| 方案 C6（移动端 H5 质感）        | ❌       | 延后迭代              |
| 方案 C7（暗色模式精修）          | ❌       | 延后迭代              |

---

## 二、分 4 期实施路线图

### 🔹 Phase 0：方案 A 基础规范化（2 天 · 先打底）

> 纯样式层改动，**不涉及任何业务逻辑**，可一次性合并。

| 编号 | 任务                                                                            | 涉及文件                                                       | 验收标准                                                    |
| ---- | ------------------------------------------------------------------------------- | -------------------------------------------------------------- | ----------------------------------------------------------- |
| A1   | 修复 page-container padding:0 → 24px(桌面)/16px(平板)                           | `src/styles/index.scss#L818`                                   | 所有页面两侧留白一致，不再贴边                              |
| A2   | 统一 el-card：移除 shadow-sm，对齐 theme.css 的 border-only + hover-shadow 策略 | `src/styles/index.scss#L314-324` + `src/styles/theme.css#L184` | 全站卡片视觉一致：平时 0.5px 边框，hover 时升起 shadow-base |
| A3   | 消灭所有内联 `style="color:rgb"`，替换为 CSS 变量                               | 全局 grep 约 15-20 处                                          | `grep -rn "style=\"color:rgb" src/` 无匹配                  |
| A4   | 字号规范化：移除 0px/11px/13.3333px，对齐 8 级系统                              | 积分排行页、系统设置页等                                       | 字号只允许 12/13/14/16/18/20/24/28                          |
| A5   | 新增行高变量 `--lh-tight/normal/relaxed`                                        | `src/styles/theme.css`                                         | 表单用 normal(1.5)、正文 relaxed(1.7)、标题 tight(1.35)     |
| A6   | 硬编码间距 → `var(--spacing-*)` 迁移（8-12 组件）                               | TrainingHub/GradingWorkbench/HomeTodoCard                      | 组件内不再出现 `padding: 24px 16px` 这类硬编码              |
| A7   | 断点统一：`@media (max-width:768px)` → 引用 breakpoints 常量                    | 全局约 200 处 → SASS 变量化                                    | iPad mini(768px 横屏)不再误触手机布局                       |
| A8   | 移动端补全：BBS 工具栏、ExamPrepBanner、弹窗安全区                              | `src/styles/mobile.css`                                        | iPhone SE/iPhone 15 Pro 无溢出                              |

**Phase 0 验证命令：**

```bash
cd frontend
npm run dev
# 手动检查：桌面/平板(iPad mini 横屏 768px)/手机(390px) 三档切换无异常
```

---

### 🔹 Phase 1：方案 B5 + B6（空状态 + 底部导航，1.5 天）

> **独立组件级改动**，风险最低，可先合并。

#### B5：空状态场景化

**文件：** `src/components/common/EmptyState.vue`

**改动内容：**

```vue
<!-- 新增 props -->
defineProps({ variant: { type: String, default: 'no-data', // 4 种场景 validator: (v) => ['no-data',
'empty-search', 'no-permission', 'loading-error'].includes(v) }, actionText: { type: String,
default: '' }, secondaryActionText: { type: String, default: '' }, // 新增双 CTA })
defineEmits(['action', 'secondary-action']) /* 对应文案预设（variant → 图标+标题+描述） */ const
presetMap = { 'no-data': { icon: FolderOpened, title: '暂无数据', desc: '还没有内容，试试创建一个吧'
}, 'empty-search': { icon: Search, title: '未找到匹配项', desc: '换个关键词或筛选条件试试' },
'no-permission': { icon: Lock, title: '暂无访问权限', desc: '如需访问，请联系管理员开通权限' },
'loading-error': { icon: Warning, title: '加载失败', desc: '网络异常，请检查后重试' }, }
```

**验收：** 4 种 variant 分别预览 + 双 CTA 按钮正常触发事件

---

#### B6：移动端底部导航质感提升

**文件：** `src/components/layout/MobileBottomNav.vue`

**改动内容：**

```scss
.mobile-bottom-nav {
  /* 毛玻璃（降级为纯色兼容不支持的设备） */
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  @supports not (backdrop-filter: blur(20px)) {
    background: var(--bg-card); // 降级
  }
}
.nav-item {
  font-size: 11px; // 10px → 11px
  font-weight: 500; // +字重
  transition: transform var(--transition-fast);
}
.nav-item.active {
  position: relative;
  transform: scale(1.05);
  /* 小圆点指示器 */
  &::after {
    content: '';
    position: absolute;
    bottom: -2px;
    width: 4px;
    height: 4px;
    background: var(--primary-color);
    border-radius: 50%;
  }
  .el-icon {
    transform: scale(1.1);
  }
}
```

**验收：** 5 种角色（学生/教师/管理员/巡视员/家长）底部导航正常，激活态小圆点可见，毛玻璃在 Safari/Chrome 正常

---

### 🔹 Phase 2：方案 B1 + B2 + C1（首页+统计卡+动效体系，2.5 天）

#### C1：全局动效 Token（先做，给后续页面提供基建）

**文件：** `src/styles/theme.css` → 新增 20-30 行

```css
:root {
  /* ===== 动效时长（三档语义化） ===== */
  --dur-fast: 120ms; /* 触控反馈、hover */
  --dur-base: 220ms; /* 页面切换、卡片升起 */
  --dur-slow: 380ms; /* 弹窗入场、列表 stagger */

  /* ===== 缓动函数 ===== */
  --ease-standard: cubic-bezier(0.4, 0, 0.2, 1); /* 标准 */
  --ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1); /* 弹性（按钮、选中） */
  --ease-enter: cubic-bezier(0, 0, 0.2, 1); /* 入场减速 */
  --ease-leave: cubic-bezier(0.4, 0, 1, 1); /* 退场加速 */

  /* ===== 统一过渡 ===== */
  --transition-fast: var(--dur-fast) var(--ease-standard);
  --transition-base: var(--dur-base) var(--ease-standard);
  --transition-spring: var(--dur-base) var(--ease-spring);
}

/* 全局列表 stagger 入场基础类 */
.stagger-enter > * {
  opacity: 0;
  transform: translateY(8px);
  animation: stagger-in var(--dur-slow) var(--ease-enter) forwards;
}
.stagger-enter > *:nth-child(1) {
  animation-delay: 0ms;
}
.stagger-enter > *:nth-child(2) {
  animation-delay: 60ms;
}
.stagger-enter > *:nth-child(3) {
  animation-delay: 120ms;
}
.stagger-enter > *:nth-child(4) {
  animation-delay: 180ms;
}
.stagger-enter > *:nth-child(5) {
  animation-delay: 240ms;
}
.stagger-enter > *:nth-child(n + 6) {
  animation-delay: 300ms;
}
@keyframes stagger-in {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 统一卡片 hover：抬起 + 阴影升档（替换各组件重复实现） */
.card-hover {
  transition:
    transform var(--transition-base),
    box-shadow var(--transition-base),
    border-color var(--transition-fast);
}
.card-hover:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
}

/* 可点击项的触控反馈：按下微缩（桌面+移动端通用） */
.press-feedback {
  transition: transform var(--dur-fast) var(--ease-spring);
}
.press-feedback:active {
  transform: scale(0.97);
}
```

**验收：** 新增 Token 在 Home.vue、EmptyState.vue、MobileBottomNav.vue 中复用生效，reduced-motion 用户需降级（后面做）

---

#### B1：首页响应式重构 + 卡片美化

**文件：** `src/views/home/Home.vue`

```scss
/* 待办卡片 grid：3 列固定 → fluid 自适应 */
.todo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--spacing-md);
}
/* （删除）原 768/1024 两个断点的硬切换，让 auto-fill 自然处理 */

/* 统计卡片：1024px 平板增加 2×2 中间态 */
.stat-card-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-md);
  @media (max-width: 1024px) {
    grid-template-columns: repeat(2, 1fr); /* 平板 2×2 */
  }
  @media (max-width: 480px) {
    grid-template-columns: 1fr; /* 小屏手机单列 */
  }
}

/* pending-stat-card 图标：加圆形主色浅底徽章 */
.pending-stat-card .el-icon {
  background: var(--primary-light);
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: var(--primary-color);
  margin-left: auto; /* 保持右侧 */
}

/* 欢迎 banner：左侧主色渐变装饰 */
.welcome-banner {
  background: linear-gradient(135deg, var(--primary-light) 0%, transparent 50%);
  padding-left: var(--spacing-lg);
  border-radius: var(--radius-lg);
}
```

---

#### B2：统计卡片色条 + 数字对齐

**文件：** `src/views/home/HomeStatCards.vue`

```scss
.stat-card {
  /* 左上 3px 语义色条（根据 props 传入状态） */
  position: relative;
  overflow: hidden;
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 3px;
    height: 100%;
    background: var(--bar-color, var(--primary-color));
  }
}
.stat-card.success::before {
  --bar-color: var(--el-color-success);
}
.stat-card.warning::before {
  --bar-color: var(--el-color-warning);
}
.stat-card.danger::before {
  --bar-color: var(--el-color-danger);
}

/* 数字：tabular-nums 防跳动 + 微字距收紧 */
.stat-value {
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.02em;
  font-feature-settings: 'tnum';
}

/* 趋势彩色 Chip */
.trend-chip {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: var(--fs-xs);
  font-weight: 500;
  &.up {
    background: var(--bg-success-light);
    color: var(--el-color-success);
  }
  &.down {
    background: var(--bg-danger-light);
    color: var(--el-color-danger);
  }
}
```

---

### 🔹 Phase 3：方案 B3 + B4 + C4（批阅工作流 + 登录 + 插画系统，4 天）

> **工作量最大的一期**，建议分两个子批次：(B4 + C4 插画先上) → (B3 批阅三栏独立测试后上)

---

#### B4：登录页品牌光晕

**文件：** `src/views/login/Login.vue`

```scss
.login-wrapper {
  /* 微妙主色光晕阴影（只在浅色模式明显） */
  box-shadow:
    0 8px 32px rgba(67, 97, 238, 0.08),
    var(--shadow-lg);
  position: relative;
  overflow: hidden;
}
/* 左下角装饰圆：主色 2% 不透明度的大圆，只在视觉底层起氛围作用 */
.login-wrapper::before {
  content: '';
  position: absolute;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(67, 97, 238, 0.06), transparent 70%);
  border-radius: 50%;
  bottom: -150px;
  left: -150px;
  z-index: 0;
  pointer-events: none;
}
.login-section {
  position: relative;
  z-index: 1;
}

/* 输入框 focus 强化：当前 3px → 4px 外发光更明显 */
.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 4px rgba(67, 97, 238, 0.15) !important;
  border-color: var(--primary-color);
}

/* 登录按钮 shimmer 骨架加载动画 */
.login-btn.is-loading .el-icon {
  /* Element Plus 自带 spinner，补充骨架 shimmer： */
}
.login-btn.is-loading::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.25), transparent);
  animation: shimmer 1.2s infinite;
  border-radius: inherit;
}
@keyframes shimmer {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}
```

**并移除：** 登录页 `版本：Beta 1.0`（迁移到个人中心 → 关于页面）

---

#### C4：品牌插画系统（6 个场景 SVG）

> **统一风格：线性插画 · 线条 1.5px · 端点圆角 · 主色 #4361ee · 辅色 #4cc9f0 · 无填充或极浅填充**

**输出文件清单：**

```
src/assets/illustrations/
├── empty-no-data.svg          # 无数据（文件夹空）  → 对应 EmptyState variant="no-data"
├── empty-search.svg           # 空搜索（放大镜）    → 对应 EmptyState variant="empty-search"
├── empty-no-permission.svg    # 无权限（锁+盾牌）   → 对应 EmptyState variant="no-permission"
├── empty-error.svg            # 加载错误（警示云朵） → 对应 EmptyState variant="loading-error"
├── login-brand.svg            # 登录页左侧品牌区：教室+黑板+学生（抽象几何风）
└── getting-started.svg        # 新人引导：火箭+学习路径
```

**SVG 规范（所有文件必须遵守）：**

```svg
<!-- 所有插画统一 viewBox="0 0 400 300"，方便统一缩放 -->
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 300" fill="none">
  <!-- 线条：1.5px 粗，端点圆角 stroke-linecap="round" -->
  <!-- 主色用 currentColor 或 var(--primary-color)，确保暗色模式仍可读 -->
  <!-- 示例： -->
  <path d="M40 200 L200 80 L360 200 Z" stroke="var(--primary-color)" stroke-width="1.5" stroke-linecap="round"/>
  <circle cx="200" cy="180" r="40" stroke="var(--primary-color)" stroke-width="1.5"/>
  <path d="M170 180 L195 205 L240 160" stroke="#4cc9f0" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
</svg>
```

**接入方式：**

```vue
<!-- LoginBrand.vue -->
<template>
  <div class="login-brand">
    <img src="@/assets/illustrations/login-brand.svg" class="brand-illustration" alt="" />
    <!-- 原有文字... -->
  </div>
</template>
<style scoped>
.brand-illustration {
  width: 80%;
  height: auto;
  max-width: 320px;
  /* 暗色模式降亮度 */
  .dark & {
    opacity: 0.85;
    filter: brightness(0.95);
  }
}
</style>
```

---

#### B3：批阅工作台三栏布局 + 快捷键

**文件：** `src/views/task/TeacherGradingWorkbench.vue`（或对应实际路径）

**三栏布局断点：**

```
┌────────────────────────────────────────────────────────────────┐
│  ≥1280px（三栏）:                                                │
│  ┌──────────┬───────────────────────────────┬──────────────┐   │
│  │ 学生列表  │     当前学生提交内容            │   评分面板   │   │
│  │ (240px)  │      (flex:1, 可滚动)         │  (320px,sticky)│ │
│  └──────────┴───────────────────────────────┴──────────────┘   │
│                                                                 │
│  768-1279px（双栏）:                                             │
│  ┌──────────────────────────────────────────────┐              │
│  │ [☰ 抽屉按钮]  提交内容 + 评分上下排布          │              │
│  │ (学生列表变成左侧抽屉，点击按钮呼出)            │              │
│  └──────────────────────────────────────────────┘              │
│                                                                 │
│  <768px（手机单列）:                                             │
│  ┌──────────────────────────┐                                   │
│  │ 顶部：切学生上一位/下一位 │                                   │
│  ├──────────────────────────┤                                   │
│  │      学生提交内容         │                                   │
│  ├──────────────────────────┤                                   │
│  │      底部固定评分栏       │                                   │
│  └──────────────────────────┘                                   │
└────────────────────────────────────────────────────────────────┘
```

**快捷键系统（仅桌面端 ≥1024px 启用）：**

```js
// composables/useGradingShortcuts.js
import { onMounted, onUnmounted } from 'vue';

export function useGradingShortcuts({
  prevStudent,
  nextStudent,
  focusStep,
  submit,
  isMobile, // 移动端直接禁用
}) {
  if (isMobile?.value) return;

  function onKey(e) {
    // 输入框中不触发
    if (['INPUT', 'TEXTAREA'].includes(e.target.tagName) && e.key !== 'Enter') return;

    switch (e.key.toLowerCase()) {
      case 'j':
        prevStudent?.();
        break;
      case 'k':
        nextStudent?.();
        break;
      case '1':
      case '2':
      case '3':
      case '4':
      case '5':
      case '6':
      case '7':
      case '8':
      case '9':
        focusStep?.(parseInt(e.key) - 1);
        break;
      case 'enter':
        if (e.ctrlKey || e.metaKey) {
          e.preventDefault();
          submit?.();
          break;
        }
    }
  }

  // 首次挂载时显示快捷键提示（Element Plus el-tooltip 绑到评分按钮上）
  onMounted(() => window.addEventListener('keydown', onKey));
  onUnmounted(() => window.removeEventListener('keydown', onKey));
}
```

**步骤卡色条：**

```scss
.step-card {
  position: relative;
  padding-left: 16px;
  /* 左侧 3px 色条：未评=灰色，已评=绿色，自动评=蓝色 */
  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 8px;
    bottom: 8px;
    width: 3px;
    border-radius: 2px;
    background: var(--step-bar-color, var(--border-color));
  }
  &.ungraded::before {
    --step-bar-color: var(--text-disabled);
  }
  &.graded::before {
    --step-bar-color: var(--el-color-success);
  }
  &.auto-graded::before {
    --step-bar-color: var(--primary-color);
  }
}
```

---

## 三、提交前强制审计脚本（每次合并前必跑）

```bash
cd frontend

# 1. 查残留内联颜色硬编码
echo "=== 残留内联颜色 ==="
grep -rn "style=\"color:rgb\|style=\"background.*rgb" src/ --include="*.vue" --include="*.js" | head -20

# 2. 查硬编码间距
echo "=== 硬编码间距 (未用 var(--spacing) ==="
grep -rn "padding: 24\|padding: 16\|margin: 24\|margin: 16" src/views/ --include="*.vue" \
  | grep -v "var(--spacing" | head -20

# 3. 查非标准字号
echo "=== 非标准字号（允许 12/13/14/16/18/20/24/28）==="
grep -rnE "font-size:\s*(0|11|15|17|19|21|22|23|25|26|27|30)px" src/ --include="*.vue" --include="*.scss" | head -20

# 4. 查新增的 el-card shadow/border 冲突
echo "=== el-card 是否同时有 shadow 和 border ==="
grep -A3 "\.el-card" src/styles/index.scss src/styles/theme.css | head -30

# 5. eslint + 构建验证
npm run lint
npm run build  # 必须能过
```

---

## 四、暗色模式 & reduced-motion 兜底检查清单

### 暗色模式（每一期完工必查）

```
□ 登录页装饰圆 ::before 暗色模式下是否降低不透明度?
□ 6 张 SVG 插画 currentColor 在暗色下是否仍可读?
□ status-bar 色条在暗 card 背景上对比度 ≥ 3:1?
□ 毛玻璃 backdrop-filter 暗色下 rgba 背景色是否同步调暗?
```

### reduced-motion（C1 动效体系必须覆盖）

```scss
/* C1 动效必须加这一段降级： */
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.001ms !important;
    transition-duration: 0.001ms !important;
  }
  .stagger-enter > * {
    animation: none !important;
    opacity: 1 !important;
    transform: none !important;
  }
  .card-hover:hover {
    transform: none !important;
  } // 不抬升
}
```

---

## 五、关键文件索引（供快速定位）

| 模块                              | 文件路径                                              |
| --------------------------------- | ----------------------------------------------------- |
| 全局样式主入口                    | `frontend/src/styles/index.scss`                      |
| 设计 Token（颜色/字号/间距/圆角） | `frontend/src/styles/theme.css`                       |
| 移动端专属样式                    | `frontend/src/styles/mobile.css`                      |
| 布局容器（侧边栏+顶栏）           | `frontend/src/layout/Layout.vue` + `Layout.css`       |
| 首页主文件                        | `frontend/src/views/home/Home.vue`                    |
| 登录页主文件                      | `frontend/src/views/login/Login.vue`                  |
| 批阅工作台（需确认实际文件名）    | `frontend/src/views/task/TeacherGradingWorkbench.vue` |
| 空状态组件                        | `frontend/src/components/common/EmptyState.vue`       |
| 移动端底部导航                    | `frontend/src/components/layout/MobileBottomNav.vue`  |
| 响应式断点常量                    | `frontend/src/constants/breakpoints.js`               |
| 历史审计参考                      | `frontend/UX_UI_OPTIMIZATION_PLAN.md`                 |
| 本计划文档                        | `frontend/UI_UX_BEAUTIFICATION_PLAN.md`               |

---

## 六、完成标准

每一期完成后，以下 5 项都通过才算 OK：

1. ✅ **桌面端（1920×1080）** 视觉无错位，三档窗口宽度切换流畅
2. ✅ **平板端（768×1024 / 1024×768）** 无水平滚动条，批阅工作台触发双栏
3. ✅ **移动端（390×844 iPhone 15 Pro / 375×667 iPhone SE）** 底部导航不遮挡内容，弹窗全屏不溢出
4. ✅ **命令审计脚本** 5 项全部 0 警告（或解释说明合理的例外）
5. ✅ **人工走查**：学生端首页 → 学习 Tab → 做 1 题；教师端首页 → 批阅作业 → 切学生 → 提交评分 全流程无异常

---

> **文档维护约定：** 实际实施中如发现某个环节的实际工时预估偏差 >30%，请更新本文件对应行，用于后续计划校准。

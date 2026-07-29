# 前端全面审计报告

> **审计时间**: 2026-06-17  
> **审计范围**: 全部前端模块 — views(29个模块)、components(12个目录)、composables(19个)、utils(13个)、stores(6个)、api(48个)、router(15个)、styles(4个)  
> **审计角色**: UI设计师 · UX研究员 · 前端架构师 · 可访问性专家 · 安全审计员  
> **审计方法**: 9 个并行审计子代理，覆盖 200+ Vue 文件、50+ JS 文件、4 个 CSS/SCSS 文件

---

## 一、审计总览

| 维度 | 评分 | 说明 |
|------|------|------|
| **设计系统一致性** | ⭐⭐⭐☆☆ | theme.css 定义完善，但大量文件绕过 CSS 变量体系 |
| **UI/UX 交互质量** | ⭐⭐⭐☆☆ | 核心流程良好，部分模块缺少 loading/error/empty 状态 |
| **代码质量** | ⭐⭐⭐⭐☆ | Composition API 统一使用，少量 dead code 和 bug |
| **可访问性 (A11y)** | ⭐⭐☆☆☆ | 几乎无 aria-label、键盘导航、屏幕阅读器支持 |
| **响应式/移动端** | ⭐⭐⭐⭐☆ | mobile.css + 断点体系完善，个别模块缺失 |
| **安全性** | ⭐⭐⭐⭐☆ | v-html 均走 renderMarkdown/DOMPurify，少量风险点 |

---

## 二、致命问题 (P0 — 必须修复)

### 2.1 运行时 Bug

| 文件 | 行号 | 问题 | 影响 |
|------|------|------|------|
| `class/QuizPanel.vue` | 95-96 | `editLocalQuestion(q)` 和 `deleteLocalQuestion(q)` 在模板中调用但 **script 中从未定义** | 点击时运行时报错 |
| `class/QuizPanel.vue` | 494-513 | `showEditDialog` 和 `editingQuestion` 引用但 **未声明** | 编辑弹窗完全不可用 |
| `student/StudentGrowth.vue` | 330 | `sumRes` 在 `finally` 块中引用，但变量声明在 `try` 块内 | 运行时 ReferenceError |
| `student/StudentGrowth.vue` | 146-147 | `chartLoading`/`achievementsLoading` 设置后 **模板中从未消费** | loading 指示器失效 |
| `ai/GenerationResult.vue` | 185 vs 17 | `defineEmits` 声明 `'export'`，但模板 emit `'exportWord'` | emit 名称不匹配 |
| `ai/QuestionEditPanel.vue` | 99 | `multiAnswer` 过滤仅 A-D，但选项支持到 F | E/F 多选答案被静默丢弃 |
| `precision/PrecisionPractice.vue` | 137 | `getPracticeQuestions` 被调用但 **未导入** | 运行时 ReferenceError |
| `credit/AdminRulesPanel.vue` | 50 | `savingRule` ref 声明但 **从未绑定到任何按钮的 :loading** | 保存按钮无 loading 指示 |

### 2.2 API 层功能不可用

| 文件 | 问题 | 影响 |
|------|------|------|
| `api/parent.js` | 所有 URL 带 `/api/` 前缀，但 request.js baseURL 已是 `/api`，导致 **双重 `/api/api/`** | 请求 404，家长端完全不可用 |
| `api/alert.js` | 同上，所有 URL 带 `/api/` 前缀 | 告警模块完全不可用 |

---

## 三、高优先级问题 (P1)

### 3.1 文件超长 (违反 AGENTS.md ≤300 行规则)

| 文件 | 行数 | script 行数 | 建议 |
|------|------|------------|------|
| `class/QuizPanel.vue` | **1865** | ~740 | 拆分为 QuizPanelLab + QuizPanelClassroom |
| `class/SmartScreen.vue` | **1240** | ~253 | 拆分样式和逻辑 |
| `ai-assistant/AiAssistant.vue` | **1168** | ~660 | 拆分 SSE 逻辑/SSE 重复代码提取为 composable |
| `task/WrongBook.vue` | **896** | ~372 | 拆分重做弹窗/薄弱分析/学科分组 |
| `task/TaskGrading.vue` | **887** | ~390 | 拆分提交列表/看板/质量分析/问卷统计 |
| `question-bank/QuestionBank.vue` | **783** | ~354 | 提取批量操作/类型切换逻辑 |
| `task/TaskList.vue` | **712** | ~287 | 提取筛选逻辑/批量操作 |
| `task/TaskCreatePage.vue` | **759** | ~377 | 提取表单逻辑到 composable |
| `question-bank/QuestionFormDialog.vue` | **643** | ~227 | 提取 8+ 题型的 payload builder |
| `task/TeacherGradingWorkbench.vue` | **833** | ~275 | 拆分快捷评论/自动批改/键盘快捷键 |
| `ai-exam/ExamPaperCreate.vue` | **578** | ~346 | 提取 SSE 逻辑（与 AiAssistant 重复） |
| `question-bank/ComposeExamWizard.vue` | **584** | ~280 | 提取初始化/提交逻辑 |
| `ai-exam/DiagnosisReport.vue` | **531** | ~186 | 提取 echarts 配色为常量 |
| `teacher/AlertManagement.vue` | **580** | ~222 | 拆分为仪表盘/规则/通知/学生详情 |
| `student/StudentGrowth.vue` | **549** | ~207 | 拆分动画/图表/成就 |
| `inspector/InspectorDashboard.vue` | **465** | ~111 | 提取统计卡片子组件 |
| `inspector/InspectorParentFeedback.vue` | **429** | ~171 | 提取表单/汇总子组件 |
| `inspector/InspectorTaskAnalysis.vue` | **420** | ~104 | 提取考试/作业分析子组件 |
| `bbs/BbsHome.vue` | **416** | ~138 | 提取侧边栏/内容区 |
| `bbs/BbsPostDetail.vue` | **402** | ~126 | 提取回复区/图片处理 |
| `teacher/SimTaskEditor.vue` | **406** | ~157 | 提取步骤卡片组件 |
| `task/StudentTaskDetail.vue` | **401** | ~172 | 提取考试逻辑/作弊检测 |
| `class/ClassHome.vue` | **390** | ~86 | 提取各区块子组件 |
| `settings/SettingsCategoryManager.vue` | **396** | ~160 | 提取导入/审核弹窗 |
| `home/Home.vue` | **399** | ~126 | 提取偏科提分/质量预警/待办统计 |
| `bbs/BbsPostList.vue` | **373** | ~64 | 略超限 |
| `bbs/BbsCreatePost.vue` | **328** | ~159 | 略超限 |
| `task/PaperImport.vue` | **383** | ~175 | 提取导入步骤 |
| `question-bank/WordImportDialog.vue` | **370** | ~191 | 提取模糊匹配算法 |
| `practice/StudentPractice.vue` | **633** | ~311 | 拆分步骤列表/进度/报告/上传 |
| `practice/PlanDesigner.vue` | **418** | ~169 | 提取计划编辑/前置条件 |
| `practice/TeacherPracticeGrading.vue` | **382** | ~181 | 提取左侧列表/右侧评分 |
| `credit/MoralStarRanking.vue` | **322** | ~93 | 略超限 |
| `credit/AdminCredit.vue` | **317** | ~116 | 略超限 |
| 另有 **10+** 文件在 300-400 行之间 | — | — | — |

### 3.2 硬编码颜色 (违反「禁止硬编码颜色」铁律)

**最高频违规 TOP 10:**

| 文件 | 硬编码颜色 | 位置 |
|------|-----------|------|
| `class/QuizPanel.vue` | `#999`, `#8b5cf6`, `#06b6d4`, `#d97706`, `#059669`, `#6c5ce7`, `#ff9800` 等 20+ 处 | SCSS 区域 |
| `class/SmartScreen.vue` | `#4361ee`, `#f59e0b`, `#06b6d4`, `#d97706`, `#b45309` 等 10+ 处 | SCSS + JS |
| `teacher/AlertManagement.vue` | `#f56c6c`, `#e6a23c`, `#409eff` (echarts + CSS) | 多处 |
| `student/StudentGrowth.vue` | `#eef0ff`, `#f5f5ff`, `#fff8f0`, `#fff`, `#4361ee`, `#f72585` | 渐变 + 内联 |
| `home/Home.vue` | `#4361ee`, `#e6a23c`, `#fef7e8`, `#303133`, `#909399` | 内联 style |
| `inspector/PracticeMonitor.vue` | `#409EFF`, `#67C23A`, `#E6A23C`, `#F56C6C` | 内联 style |
| `inspector/InspectorDashboard.vue` | `#909399`, `#E6A23C`, `#409EFF`, `#F56C6C`, `#67C23A` | 内联 + JS |
| `ai/GenerationResult.vue` | `#f0f0f0`, `#999`, `#fafbfc`, `#e8eaed`, `#2d5cf6`, `#666` | CSS |
| `ai/KnowledgePreview.vue` | `#999`, `#f5a623`, `#fafbfc`, `#e8eaed`, `#f5f7fa` | CSS |
| `knowledge/FlashcardItem.vue` | `#fef2f2`, `#dc2626`, `#fff7ed`, `#ea580c`, `#eef0ff`, `#3651d4` | hover 样式 |

**echarts 配置中的硬编码颜色**（需抽取为 JS 常量 `THEME_COLORS`）:
- `home/HomeDashboardCharts.vue`: `#06d6a0`, `#e0e0e0`
- `student/StudentLearningProfile.vue`: `#67c23a`, `#e6a23c`, `#f56c6c`
- `inspector/InspectorClassProfile.vue`: `#409EFF`, `#67C23A`
- `analytics/GrowthCurveChart.vue`: `#4361ee` (6处)
- `analytics/KnowledgeRadarChart.vue`: `#4361ee`, `#86868b`

### 3.3 font-size 未使用 CSS 变量

**全局统计**: 300+ 处 `font-size` 直接写像素值，未使用 `var(--fs-*)` 体系。

| 违规模式 | 出现次数 | 涉及文件数 |
|----------|----------|-----------|
| `font-size: 12px` → 应为 `var(--fs-xs)` | 80+ | 40+ |
| `font-size: 13px` → 应为 `var(--fs-sm)` | 60+ | 30+ |
| `font-size: 14px` → 应为 `var(--fs-md)` | 40+ | 20+ |
| `font-size: 11px` (低于设计系统最小值) | 15+ | 8 |
| `font-size: 28px/32px/36px` (超出设计系统) | 5+ | 3 |

---

## 四、中优先级问题 (P2)

### 4.1 UX 反模式

| 类别 | 文件 | 问题 |
|------|------|------|
| **破坏性操作无确认** | `inspector/InspectorClassroomPatrols.vue` | `deleteRow()` 无 ElMessageBox.confirm |
| **破坏性操作无确认** | `class/ClassAlbum.vue:180` | 照片审核通过/拒绝无确认 |
| **破坏性操作无确认** | `class/VotePanel.vue:74` | 结束投票无确认 |
| **破坏性操作无确认** | `teacher/SimTaskEditor.vue:124` | 删除步骤无确认 |
| **破坏性操作无确认** | `practice/StepEditor.vue:21` | 步骤删除无确认 |
| **破坏性操作无确认** | `task/TeacherGradingWorkbench.vue:451` | `handleDeleteQuick` 无确认 |
| **破坏性操作无确认** | `task/PendingReview.vue:85` | 审核通过无确认 |
| **破坏性操作无确认** | `task/SubjectiveQuestions.vue:104` | 删除主观题无确认 |
| **表单无验证规则** | `teacher/ResearchWorkbench.vue:133` | el-form-item 有 required 但无 rules |
| **表单无验证规则** | `teacher/LessonPrepWorkbench.vue:133` | 同上 |
| **表单无验证规则** | `inspector/InspectorTeachingResearch.vue` | 弹窗表单无 rules |
| **表单无验证规则** | `inspector/InspectorMoralInspections.vue` | 仅 HTML required |
| **表单无验证规则** | `inspector/InspectorNotices.vue` | 手动 if 检查 |
| **表单无验证规则** | `credit/AdminShopPanel.vue` | 商品创建/编辑无 required rules |
| **表单无验证规则** | `credit/AdminCredit.vue` | 调整弹窗无验证 |
| **表单无验证规则** | `practice/PracticeWizard.vue` | 向导步骤切换无验证 |
| **全页刷新** | `inspector/InspectorRecords.vue:117` | `window.location.href` 而非 `router.push()` |
| **全页刷新** | `task/PaperImport.vue:292` | `window.location.href` 而非 `router.push()` |
| **404 页面无导航** | `error/NotFound.vue` | 无返回首页按钮 |
| **密码明文展示** | `teacher/TeacherFormDialog.vue:299` | 随机密码在成功消息中明文显示 |
| **无二次确认** | `profile/Profile.vue` | 修改密码后直接登出 |
| **触屏误操作** | `class/SmartScreen.vue:452` | 点击学生切换缺勤无确认 |
| **组件库混用** | `precision/PrecisionHub.vue` | 同时使用 Vant 和 Element Plus |
| **重复 markdown 渲染** | `practice/StudentPractice.vue` | 同时导入 `renderMarkdown` 和定义本地 `md2html` |

### 4.2 代码重复

| 涉及文件 | 重复度 | 说明 |
|----------|--------|------|
| `teacher/ResearchWorkbench.vue` ↔ `teacher/LessonPrepWorkbench.vue` | ~80% | 结构几乎相同，应提取 GroupWorkbench 共享组件 |
| `inspector/InspectorExams.vue` ↔ `inspector/InspectorTaskAnalysis.vue` 考试 tab | ~200行 | 考试列表逻辑重复 |
| `inspector/InspectorNotices.vue` ↔ `inspector/InspectorParentFeedback.vue` | 结构相似 | 通知/反馈表单可共享 |

### 4.3 Dead Code (未使用的文件)

| 文件 | 说明 |
|------|------|
| `composables/useFormFilter.js` | 未被任何组件导入 |
| `composables/useCrudList.js` | 未被任何组件导入 |
| `composables/useAiGeneration.js` | 未被任何组件导入 |
| `composables/useFormDialog.js` | 未被任何组件导入 |
| `utils/chartColors.js` | 未被任何文件导入 |
| `ai/ImportZipDialog.vue` L100 | `onFormatChange = () => {}` 死代码 |
| `composables/useTaskForm.js` L34 | `isPracticeType = computed(() => false)` 永远返回 false |
| `composables/useSubmissionStatus.js` L16 | `statusDotClass` 与 `statusTag` 完全重复 |

### 4.4 console.log 残留

| 文件 | 行号 | 内容 |
|------|------|------|
| `ai/PublishPracticeTaskDialog.vue` | 272, 317 | `console.error()` |
| `utils/request.js` | 25, 59, 79 | `console.error/warn()` |
| `composables/useAutoSave.js` | 28, 31 | `console.warn()` |
| `composables/useFormDialog.js` | 82 | `console.error()` |
| `utils/exportPdf.js` | 16 | `console.warn()` |

### 4.5 Pinia Store 问题

| 文件 | 问题 |
|------|------|
| `stores/precision.js` | **未使用 `defineStore`**，用手写 reactive 代替，丢失 devtools/热重载 |
| `stores/win7Sim.js` | 模块级 `let` 变量管理状态，448 行需拆分 |
| `stores/user.js` | 登录无 loading state，getInfo 失败不清理 token |
| `stores/notification.js` | markOneRead 本地 -1 不可靠，应重新拉取 |

---

## 五、低优先级问题 (P3)

### 5.1 可访问性 (A11y) 缺失

**全局统计**: 几乎所有可交互元素缺少无障碍支持。

| 问题类型 | 出现次数 | 示例 |
|----------|----------|------|
| 可点击 div 无 `role="button"` + `tabindex` | 50+ | Home.vue 偏科提分卡片、错题本卡片、待办统计卡片 |
| 图标按钮无 `aria-label` | 30+ | SettingsCategoryManager 删除/编辑按钮用 emoji |
| 表单 input 无 `aria-label` | 20+ | Login.vue 用户名/密码靠 placeholder |
| echarts 图表无 `aria-label`/`<title>` | 15+ | 所有图表组件 |
| 图片无 `alt` 属性 | 5+ | 部分装饰图 |
| 缺少 `aria-live` 动态区域 | 10+ | 实时数据更新区域 |

### 5.2 内联 style 绕过设计系统

**全局统计**: 250+ 处内联 `style=` 绕过 CSS 类体系。

高频违规文件:
- `settings/SettingsCategoryManager.vue`: 11 处
- `precision/PrecisionHub.vue`: 10+ 处
- `precision/TeacherPrecisionMonitor.vue`: 8+ 处
- `teacher/TeacherFormDialog.vue`: 5 处
- `teacher/MyRectification.vue`: 3 处
- `ai/AiAssistant.vue`: 5+ 处
- `ai-exam/ExamPaperCreate.vue`: 6+ 处
- `student/StudentLearningProfile.vue`: 4 处

### 5.3 缺少响应式设计的模块

以下模块 **完全没有** `@media` 查询，移动端体验可能异常：

| 模块 | 文件 | 风险 |
|------|------|------|
| `practice/` | `PracticeWizard.vue`, `PlanDesigner.vue`, `AutoGradeResult.vue`, `PreviewPublish.vue`, `RubricEditor.vue`, `StepEditor.vue`, `StepAttachmentUploader.vue`, `TemplateLibrary.vue`, `StepStartPicker.vue` | **高** — 教师端管理页面 |
| `precision/` | `PrecisionHub.vue`, `PrecisionPractice.vue`, `PrecisionEnglishDrill.vue`, `PrecisionEnglish.vue`, `CheckpointPlay.vue`, `MemoryCardDashboard.vue`, `CheckpointMixed.vue`, `CheckpointBoss.vue`, `CheckpointOverview.vue`, `StudentGrowth.vue` | **高** — 学生端核心页面 |
| `inspector/` | `InspectorParentFeedback.vue` | 中 |

### 5.4 `throw new Error` 违反规则

| 文件 | 行号 | 内容 |
|------|------|------|
| `composables/useCrudList.js` | 15 | `throw new Error('useCrudList: fetchList is required')` |
| `composables/useConsolidationGenerator.js` | 67 | `throw new Error(error \|\| 'AI生成超时')` |
| `utils/sseTicket.js` | 29 | `throw new Error('Failed to get SSE ticket')` |

### 5.4 API 调用风格不一致

| 文件 | 问题 |
|------|------|
| `api/questionBank.js` | 混用 `request({})` 对象式和 `request.get()` 方法式 |
| `api/simulation.js` | 全部使用 `request.get()/post()` 方法式 |
| `api/precision.js` | 混用两种风格 |
| `api/precisionEnglish.js` | 全部箭头函数声明 |
| `composables/useConsolidationGenerator.js` | 绕过 `@/api/*` 直接调用 `request.get()` |
| `precision/StudentGrowth.vue:43` | 直接 `request.get()` 而非通过 API 模块 |
| `precision/ComparisonHub.vue:281` | 直接 `request.get()` 而非通过 API 模块 |
| `precision/ComparisonDashboard.vue:303` | 直接 `request.get()` 而非通过 API 模块 |

### 5.5 路由守卫问题

| 问题 | 说明 |
|------|------|
| Token 过期不自动刷新 | 路由守卫只检查 token 存在性，不验证有效性 |
| getInfo 失败则踢出 | 角色检查依赖 getInfo() 成功，失败则合法用户被误拒 |
| 硬编码路径检查 | AI 组卷/偏科提分路由检查硬编码路径前缀 |
| 移动端屏蔽硬编码 | 6 个路径手动维护 |

---

## 六、设计系统审计

### 6.0 暗色模式变量冲突 (严重)

**`theme.css` 和 `index.scss` 定义了冲突的 `.dark` 变量**，后者覆盖前者：

| 变量 | theme.css (.dark) | index.scss (.dark) | 冲突 |
|------|-------------------|-------------------|------|
| `--primary-light` | `rgba(67,97,238,0.15)` | `#1e2a5e` | ✅ 不同值 |
| `--bg-page` | `#1a1a1e` | `#0f1117` | ✅ 不同值 |
| `--bg-card` | (未重定义) | `#1a1c2b` | ⚠️ 仅 index.scss 定义 |
| `--text-primary` | `#f0f0f3` | `#e8eaf0` | ✅ 不同值 |
| `--border-color` | `rgba(255,255,255,0.08)` | `#2a2d42` | ✅ 不同值 |
| `--shadow-sm` | `none` | `0 1px 3px rgba(0,0,0,0.3)` | ✅ 完全相反 |

由于 `index.scss` 通过 `@import './theme.css'` 后加载，**index.scss 的值最终生效**。应统一为单一来源。

### 6.1 断点不一致 (767px vs 768px)

`mobile.css` 使用 `max-width: 767px`，而 `index.scss` 和 60+ 组件使用 `max-width: 768px`，导致 **1px 间隙**。

| 使用 767px 的文件 | 行号 |
|------------------|------|
| `src/styles/mobile.css` | 24 |
| `views/task/TeacherGradingWorkbench.vue` | 779 |
| `views/task/WrongBook.vue` | 893 |
| `views/practice/TeacherPracticeGrading.vue` | 378 |
| `views/practice/StudentPractice.vue` | 586 |
| `views/bbs/BbsPostList.vue` | 357 |
| `views/bbs/BbsPostDetail.vue` | 390 |
| `components/ai/KnowledgeEditDialog.vue` | 96 |

**建议**: 全部统一为 `768px`。

### 6.2 theme.css 定义 vs 实际使用

| 设计 Token | 定义 | 实际使用率 |
|-----------|------|-----------|
| `--primary-color` (#4361ee) | ✅ | ~70% (echarts/内联 style 大量绕过) |
| `--text-primary/regular/secondary/disabled` | ✅ | ~60% (多处直接写 #303133/#909399) |
| `--bg-page/card/secondary/section` | ✅ | ~80% |
| `--fs-xs/sm/md/lg/xl/2xl` | ✅ | ~15% (绝大多数 font-size 直写) |
| `--spacing-xs/sm/md/lg/xl/2xl` | ✅ | ~30% (大量 padding/margin 直写) |
| `--radius-xs/sm/md/lg/xl` | ✅ | ~60% |
| `--shadow-sm/base/lg` | ✅ | ~50% |
| `--border-light/color/input` | ✅ | ~70% |
| `--transition-fast/base` | ✅ | ~40% |

### 6.2 暗色模式覆盖

- `theme.css` 定义了完整的 `.dark` 变量覆盖 ✅
- `index.scss` 有大量 `!important` 暗色覆盖 ⚠️ (15+ 处)
- `Layout.css` 有暗色模式侧边栏样式 ✅
- **问题**: 部分硬编码颜色的文件在暗色模式下不会自适应

### 6.3 响应式断点一致性

| 断点 | 使用情况 |
|------|---------|
| `≤768px` (移动端) | ✅ 全局一致 |
| `769px-1024px` (平板) | ✅ 全局一致 |
| `≤380px` (小屏) | ✅ index.scss 有覆盖 |
| `InspectorParentFeedback.vue` | ❌ **完全缺失** 响应式样式 |

---

## 七、安全审计

### 7.1 v-html 使用

| 文件 | 行号 | 消毒方式 | 风险 |
|------|------|---------|------|
| `ai/GenerationResult.vue` | 65 | `renderMarkdown()` (DOMPurify) | ✅ 安全 |
| `ai/KnowledgeEditDialog.vue` | 33 | `renderMarkdown()` | ✅ 安全 |
| `ai/KnowledgePreview.vue` | 12 | `renderMarkdown()` | ✅ 安全 |

### 7.2 潜在安全风险

| 风险 | 文件 | 说明 |
|------|------|------|
| JWT 存 localStorage | `stores/user.js` | XSS 可窃取 token (架构决策，已知) |
| HTML strip regex 脆弱 | `ai/QuestionImportDialog.vue:66` | `/<[^>]+>/g` 不处理属性中 > |
| 密码明文展示 | `teacher/TeacherFormDialog.vue:299` | 随机密码在 ElMessage 中 |
| echarts tooltip XSS | `analytics/KnowledgeRadarChart.vue:38` | 字符串模板拼接 nodeName (低风险) |

---

## 八、性能审计

### 8.1 关注点

| 问题 | 文件 | 说明 |
|------|------|------|
| ECharts dispose+init | `analytics/GrowthCurveChart.vue`, `KnowledgeRadarChart.vue` | 每次数据变更完全销毁重建，应改用 `setOption(merge)` |
| Deep watcher 每次按键触发 | `ai/QuestionEditPanel.vue:87-89` | `JSON.stringify` 在每次按键时执行 |
| N+1 API 调用 | `ai/QuestionEditor.vue:183-215` | `saveAll` 逐题保存，应批量 |
| regex in loop | `ai/PublishPracticeTaskDialog.vue:216-228` | 大 Markdown 内容中循环创建 RegExp |
| 模块级 reactive 单例 | `stores/precision.js`, `composables/useKnowledgeBaseStore.js` | 非 Pinia store，失去 devtools |

---

## 九、修复优先级矩阵

### 第一批：P0 (本周修复)

1. **修复 QuizPanel.vue 未定义函数/引用** — 运行时崩溃
2. **修复 StudentGrowth.vue ReferenceError** — 运行时崩溃
3. **修复 GenerationResult.vue emit 不匹配** — 功能异常
4. **修复 QuestionEditPanel.vue 多选答案丢失** — 数据错误
5. **修复 parent.js / alert.js 双重 /api/ 前缀** — 功能不可用

### 第二批：P1 (两周内)

6. 拆分 10+ 个超 300 行的 Vue 文件
7. 统一 echarts 颜色为 JS 常量 `THEME_COLORS`
8. 全局替换 `font-size: Npx` → `var(--fs-*)` (自动化脚本)
9. 修复 InspectorClassroomPatrols 删除无确认
10. 迁移 precision.js 到 defineStore

### 第三批：P2 (一个月内)

11. 提取 GroupWorkbench 共享组件
12. 清理 5 个 dead code 文件
13. 清理 console.log 残留
14. 统一 API 调用风格
15. 修复路由守卫 getInfo 失败处理
16. 补充关键页面的表单验证规则

### 第四批：P3 (持续改进)

17. 为可交互元素添加 aria-label
18. 为可点击 div 添加 role + tabindex
19. 为 echarts 添加 aria-label
20. 内联 style 迁移到 CSS 类
21. InspectorParentFeedback.vue 补充响应式样式

---

## 十、最佳实践示范文件

以下文件设计系统使用规范，可作为团队参考：

| 文件 | 亮点 |
|------|------|
| `home/HomeWelcomeBanner.vue` | 完全使用 CSS 变量，57 行精简 |
| `home/HomeStatCards.vue` | 完全符合设计系统 |
| `settings/SettingsAnnouncement.vue` | 表单验证、loading、删除确认齐全 |
| `composables/useFormRules.js` | 表单规则工厂，被 12+ 组件使用 |
| `utils/markdown.js` | DOMPurify 安全渲染 |
| `utils/category.js` | O(n) 树遍历替代 O(n⁴) 嵌套循环 |
| `utils/echarts.js` | 按需引入，减包 500KB |
| `composables/useClassroomSSE.js` | SSE 生命周期管理典范 |
| `composables/useCheatMonitor.js` | 338 行但清理完备、职责清晰 |

---

## 十一、数据统计

| 指标 | 数值 |
|------|------|
| 审计 Vue 文件总数 | ~200 |
| 超 300 行的 Vue 文件 | 35+ |
| 超 200 行 script setup | 20+ |
| 硬编码颜色实例 | 200+ |
| font-size 未用 CSS 变量 | 300+ |
| 内联 style 绕过类体系 | 250+ |
| 缺少 aria-label 的交互元素 | 100+ |
| dead code 文件 | 5 |
| console.log 残留 | 8+ 处 |
| v-html 使用 (均安全) | 39 处 |
| 运行时 Bug | 8 个 |
| 功能不可用 (API 前缀) | 2 个模块 |
| 缺少响应式设计的模块 | 11+ 个 |
| 破坏性操作无确认 | 10+ 处 |
| 表单无验证规则 | 10+ 处 |

---

*报告由 6 个并行审计代理 + 3 个专项审计代理联合生成，覆盖设计系统、UI/UX、代码质量、可访问性、安全性、性能 6 大维度。*

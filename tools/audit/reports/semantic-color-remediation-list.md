# 语义色甄别清单 — 任务② 整改记录

> 2026-07-17 · 基于 AGENT-PROMPTS-UIUX.md 任务② 产出

## 规则

搜索 `frontend/src/` 下 `#67c23a` / `#e6a23c` / `#f56c6c` + 二审补充 `#409eff`。

- **裸写** → 替换为 `var(--el-color-*, #xxx)` 回退写法
- **var() 回退**（含 `var(--el-color-*)` 或 `var(--primary-color)` 等自定义变量）→ 保留
- **ECharts canvas 配置** → 用 `cssVar()` 取值
- **chartColors.js / grading.js** → 集中色板源，保留

## 甄别清单

| # | 文件:行号 | 原值 | 判定 | 处置 | 二审追认 |
|---|-----------|------|------|------|---------|
| 1 | `KpComparisonCard.vue:31,49` | `'#67c23a'/'#e6a23c'/'#f56c6c'` 三元内联 | 裸写 | → `var(--el-color-*)` | ✅ |
| 2 | `KpComparisonCard.vue:101` | `'#409eff'` 三元内联 | 裸写 | → `var(--el-color-primary, #409eff)` | ✅ 二审追加 |
| 3 | `ExamPaperHub.vue:50` | `background:#67c23a` | 裸写 | → `var(--el-color-success, #67c23a)` | ✅ |
| 4 | `ExamPaperHub.vue:101` | `dashed #e6a23c` | 裸写 | → `var(--el-color-warning, #e6a23c)` | ✅ |
| 5 | `ExamPaperHub.vue:109` | `dashed #67c23a` | 裸写 | → `var(--el-color-success, #67c23a)` | ✅ |
| 6 | `ExamPaperHub.vue:116` | `background:#e6a23c` | 裸写 | → `var(--el-color-warning, #e6a23c)` | ✅ |
| 7 | `MonitorDashboard.vue:236,259,246,284` | `'#f56c6c'/'#e6a23c'/'#67c23a'` ECharts | 裸写 | → `cssVar()` | ✅ |
| 8 | `MonitorDashboard.vue:366-367` | `.text-danger/warning { color }` | 裸写 | → `var(--el-color-*)` | ✅ |
| 9 | `StepSolutionPanel.vue:39-40` | `.step-icon { color }` | 裸写 | → `var(--el-color-*)` | ✅ |
| 10 | `PerStudentTab.vue:48,63` | `color:#67c23a` + `'#409eff'` 三元 | 裸写 | → `var(--el-color-*)` | ✅ 二审追加 #409eff |
| 11 | `PerQuestionTab.vue:67` | `'#409eff'` 三元内联 | 裸写 | → `var(--el-color-primary, #409eff)` | ✅ 二审追加 |
| 12 | `useDiagnosisData.js:34,36,37` | 分布数据 `color: '#xxx'` | 裸写 | → CSS var 字符串 | ✅ |
| 13 | `useDiagnosisData.js:109,113` | `'#409eff'` 三元 | 裸写 | → `var(--el-color-primary, #409eff)` | ✅ 二审追加 |
| 14 | `QuestionDetailRow.vue:21,49` | `color:#f56c6c` + `'#409eff'` 三元 | 裸写 | → `var(--el-color-*)` | ✅ 二审追加 #409eff |
| 15 | `GradingWorkbench.vue:82,102` | `'#67c23a'/'#f56c6c'` 三元内联 | 裸写 | → CSS var 字符串 | ✅ |
| 16 | `ComparisonDashboard.vue:299,304` | `style="color:#..."` | 裸写 | → `var(--el-color-*)` | ✅ |
| 17 | `DiagnosisHub.vue:50` | `'#67c23a'/'#e6a23c'/'#f56c6c'` 三元 | 裸写 | → CSS var 字符串 | ✅ |
| 18 | `PptStep.vue:35` / `OfficeStep.vue:26` / `ExcelStep.vue:41` | 三元内联 | 裸写 | → CSS var 字符串 | ✅ |
| 19 | `SyllabusCoverage.vue:134-136,159` | `.dot.red/yellow/green` + `.covered strong` | 裸写 | → `var(--el-color-*)` | ✅ |
| 20 | `SubnetCalculator.vue:95-96,203-204` | `color:#xxx` + `.corr-right/wrong` | 裸写 | → `var(--el-color-*)` | ✅ |
| 21 | `HardwareGallery.vue:69` | `color:#f56c6c` | 裸写 | → `var(--el-color-danger)` | ✅ |
| 22 | `ShowcaseGrid.vue:78` | `'#409eff'` 数组 | 裸写 | → `var(--el-color-primary, #409eff)` | ✅ 二审追加 |
| 23 | `QuickReviewPanel.vue:15` | `<el-icon color="#67c23a">` | 裸写 | → `var(--el-color-success)` | ✅ |
| 24 | `PenetrationCheckin.vue:5` | `<el-icon color="#e6a23c">` | 裸写 | → `var(--el-color-warning)` | ✅ |
| 25 | `ExamPrepBanner.vue:77-78,89` | `border/color: #e6a23c` | 裸写 | → `var(--el-color-warning)` | ✅ |
| 26 | `SidebarMenu.vue:476,480` | `background: #f56c6c/#e6a23c` | 裸写 | → `var(--el-color-*)` | ✅ |
| 27 | `MobileBottomNav.vue:183-184` | `background: #F56C6C/#E6A23C` | 裸写 | → `var(--el-color-*)` | ✅ |
| 28 | `TopologyDesigner.vue:235` | `.del-icon { color: #f56c6c }` | 裸写 | → `var(--el-color-danger)` | ✅ |
| 29 | `ExamPaperCreate.vue:1060` | `.epc-error { color: #f56c6c }` | 裸写 | → `var(--el-color-danger)` | ✅ |
| 30 | `InspectorTeacherProfile.vue:116` | `'#67C23A'` ECharts | 裸写 | → `cssVar()` | ✅ |

### 合法保留（var() 回退写法，不改）

`DiagnoseResult.vue:415,418,421,435,438,441` · `markdown-body.css:30,53` · `AiCallStatus.vue:118,123` · `ExamAnswerPanel.vue:363,365` · `TaskSubmitPanel.vue:618,620` · `ClassStatsView.vue:99` · `QuestionDetailRow.vue:11` · `KpComparisonCard.vue:12` · `ShowcaseGrid.vue:187-188` · `Showcase.vue:325,436,440,461` · `TextStep.vue:41` · `StepPlayer.vue:270,274` · `KnowledgeMine.vue:355`

### 集中色板源保留（不改）

- `chartColors.js` — ECharts 图表色板
- `grading.js` — 评分等级色板
- `TaskIcon.vue:322` — 任务状态色映射

### 已预置 var() 回退（不是裸 hex，不改）

- `ShowcaseGrid.vue:186,189,240` — `var(--primary-color, #409eff)` 等回退写法
- `Showcase.vue:432` — `var(--primary-color, #409eff)`

# 前端 UI/UX 一致性审计（修订版 v3）

> 版本: v3（v2 经主控二轮审查后精准修订:§5 门禁重构为棘轮制、§6.4 消歧、§4 补护栏,新增 §7 主题体系规划）
> 基准资产: `tools/audit/reports/baseline-20260704.md`、本仓 `theme.css` / `index.scss` / `main.js` / `vite.config.js` / `index.html` / `DESIGN.md`
> 口径声明: 真实 UI 面 = `src/views`(306) + `src/components`(107) + `App.vue`/`Layout.vue`(2) = **415** 个 `.vue`（实测 2026-07-17）；`api/composables/utils/router` 等非 UI 目录不计入分母。

---

## 0. 修订说明（相对 v1 的变化）

v1 报告被主控审查退回,核准确认:**三大 P0 核心证据全部坐实,事实核准率约 8 成**;但存在数据口径虚高、两处例证误报、且作为行动方案缺失"实验冻结期约束 / 误判 Vant 选型动机 / 忽略在途治理资产"三处与现实脱节。本版:

1. 撤回并修正所有失实数值(见 §1 对账表);
2. 采纳三项"结论修正":暗色模式不计入语义色分裂、token 属"命名错位"非"值错"、治理脚本逻辑写反;
3. 采纳三项"现实脱节":冻结期时间锚、Vant 为刻意移动端选型、在途治理资产应复用;
4. 所有行动方案均带时间锚,且冻结期内明确"不做"。

**v3 增量（2026-07-17 主控二轮审查后）**:

5. §5 门禁脚本整体重构:废弃 diff-grep 白名单方案（存在挂起/误杀/漏杀/自相矛盾四缺陷），改为**棘轮机制 + 两条高信噪比增量规则**，明确 husky + deploy.sh 双接入;
6. §6.4 消歧:与 P0-1 定论严格对齐——禁止的是"全局双注册"，不是"双库共存";
7. §4 补两条护栏:④与 PRETEST 基线的先后硬约束、函数式组件样式验收标准与 bundle 实测要求;
8. 三处口径修正（分母 415 / useIsMobile 实测口径 / "32 文件"去重说明）;
9. 新增 §7 主题体系规划:token 治理的直接红利——暗色之外扩展高对比/护眼/大屏主题包（全部实验后）。

---

## 1. 与主控审查的逐条对账

### 1.1 坐实的核心主张（v1 证据有效）

| v1 主张 | 实测复核 | 判定 |
|--------|---------|------|
| main.js 全局注册 Vant + 全量 CSS | `main.js:10` 引入 `vant/lib/index.css`；`main.js:35-37` 注释 + `app.use(Vant)` | ✅ 属实（CSS 行号原写 36-37，实为 10） |
| precision 模块整套用 Vant | 11 个文件、92 个开标签（含闭合 156） | ✅ 属实 |
| 语义色存在多套定义 | `theme.css:30-32`、`index.scss:321-324`、`index.scss:93-95` 三组值逐字符吻合 | ✅ 属实（但范围需收窄，见 §2.1 修正） |
| 字体设计未落地 | `index.html:10` 系统栈、`index.scss:167` Inter、全仓无 `@font-face`、`DESIGN.md:18` 规定 Geist | ✅ 完全属实 |
| 内联样式数量级 | 实测 **1204** 处 | ✅ 精确（v1 报 1205） |
| el-button 采用量 | 实测 **1097** 处 | ✅ 精确（v1 报 1093） |
| kb-bottom-tab 自造 tab | `KnowledgeBaseHub.vue:135` | ✅ 属实 |
| 圆角 sm 偏差 | `theme.css:49`、`index.scss:43` 均为 6px | ✅ 属实（性质见 §2.4 修正） |
| .flex / .u-flex 双轨 | `index.scss:380-385` vs `1054-1057` | ✅ 属实 |
| echarts 无全量引入 / Mobile* 5 组件 / win7-sim 隔离 | 全部确认 | ✅ 属实 |

### 1.2 失实或夸大的部分（v1 已撤回）

| v1 主张 | 实测 | 修正 |
|--------|------|------|
| "563 个 .vue 文件""其余 540+ 全用 EP" | 真实 UI 面 = **415**（views 306 + components 107 + App/Layout 2，实测 2026-07-17）；precision 明确用 Vant | 作废"563"与"540+ 全用 EP"；以 415 为分母 |
| "page-container/page-card 已被 200+ 视图采用"（正面基线） | `page-container` 视图层仅 **4** 处（Home/Profile/NotFound/ClassList，另 2 处为定义端 index.scss/mobile.css）；`page-card` ≈ 34–40 视图 | 撤回"200+"；正面基线论据弱化（见 §2.3） |
| `DiagnoseResult.vue:415-441` 列为硬编码反例 | 实为 `var(--el-color-success, #67c23a)` 带回退值的安全写法 | 撤回；真反例为 `KpComparisonCard.vue:31,49` 裸三元 hex |
| "ChildGrades.vue 用 22px" | 实为 `var(--fs-lg)` + token，属正面案例 | 撤回该例证（大论点"49 视图本地重写 .page-header"仍成立） |
| el-page-header 24 处 | 实测 **12** 处（24 为含闭合标签的匹配） | 改为 12 |
| 硬编码色 1075 处 | `.vue` 纯 hex 854 / 含 rgb 全口径 1236 / 基线口径 900 | 口径注明（见 §2.2） |
| "审计脚本未固化 CI（否则不会累积）" | `tools/audit/` 已有 8 个检查脚本 + `replace-hardcoded-colors.js`，基线 1209 引用已两轮治理降至 ~900 | 改为"工具已齐、缺 CI 挂钩、存量在降"，撤回"从未治理"暗示 |

---

## 2. 修订后的核心问题清单（去伪存真）

优先级定义:P0 = 影响跨模块统一观感、必须治理;P1 = 一致性缺口;P2 = 治理层碎片化。

### 2.1 P0 — 组件库与视觉底座（3 项，范围已收窄）

**P0-1 · Vant 全局注册 + 全量 CSS（保留库、改按需引入）**
- 证据:`main.js:10` `import 'vant/lib/index.css'`、`main.js:35-37` `app.use(Vant)` + 全量 CSS。
- 风险:整库运行时与 CSS 注入每个页面,拖累首屏;全局组件命名空间污染。
- **修订处置（采纳现实脱节②）**:Vant 是 `main.js:35` 注释明示的"移动端组件库(偏科提分模块专用)",学生端以手机为主(R114 移动优化),Element Plus 桌面优先、触控体验不及 Vant。**不删库、不重写业务**。改为:
  - **按需引入**:实测 `vite.config.js:5-19` 已有 `unplugin-auto-import` + `unplugin-vue-components`（ElementPlusResolver 在用），仅需追加 Vant 4 官方 `@vant/auto-import-resolver`、移除 `app.use(Vant)` 与全量 CSS——基础设施现成,真·低成本;
  - **⚠️ 函数式组件暗雷（实测 2026-07-17）**:precision 有 **27 处**函数式调用（`showToast/showDialog/showConfirmDialog/showNotify`，分布 7 个文件）。unplugin 只处理模板组件,函数式组件样式**不会**被自动引入,必须手动补 `import 'vant/es/toast/style'` 等——否则功能正常、样式裸奔,且本地 dev 因 HMR/缓存可能测不出（部署铁律#2）。**验收标准:构建产物中逐一人工验证 toast/dialog/notify 实际样式**。（全仓无 `:is="'van-…"` 动态组件,模板侧按需解析安全。）
  - **收益必须实测**:`main.js:9` 还全量引入了 `element-plus/dist/index.css`（EP 组件已按需、CSS 却全量）——首屏包袱并非 Vant 独担。改动前后必须留存 bundle 体积对比数字,避免"优化了个寂寞";
  - 用 Vant CSS 变量主题对齐主色与圆角:`--van-primary-color: #4361ee`、`--van-radius-*` 等,收敛视觉割裂（并入 §7 主题包统一管理）。

**P0-2 · 语义功能色存在"真实冲突"两处（暗色不计入）**
- 采纳修正①:暗色模式 `index.scss:93-95` 用更亮 `#4caf50/#ff9800/#ef5350` 是深底提亮惯例,**不算分裂**。
- 真实冲突 A — `index.scss:321-324` 对 `el-tag--*` 的文字色覆写:`#0f6e56/#854f0b/#a32d2d`,与 `theme.css:30-32` 的 `--el-color-*`（#2e7d32/#ed6c02/#d32f2f）不一致。这三个深色很可能是"浅底(--success-light)深字"的对比度设计,**直接删覆写前必须先做 WCAG 对比度评估**,不能无脑删。
- 真实冲突 B — 部分文件**裸写 Element Plus 默认色**而非引用 token:如 `KpComparisonCard.vue:31,49` 三元表达式 `'#67c23a':'#e6a23c':'#f56c6c'`。基线报告将其归类为"EP 内部色",但裸写会绕过 token 体系,属应收敛项（**≤32 文件**——该计数含 `var(--el-color-*, #67c23a)` 回退写法的文件,执行前必须先甄别"裸写"与"var() 回退",真实需改文件数小于 32）。

**P0-3 · 字体设计完全未落地（建议改 DESIGN.md 而非加载字体）**
- 证据:`index.html:10` 系统栈、`index.scss:167` Inter、全仓无 `@font-face`、DESIGN.md 规定 Geist。
- **修订处置**:Geist **无中文字形**,中文为主的界面自托管收益极低。建议修订 `DESIGN.md` 正式承认"系统字体栈(-apple-system / PingFang SC / Microsoft YaHei)"为主,并统一三处字体栈(index.html / index.scss / DESIGN.md)为同一声明,消除"声称 Geist 实际无"的错位。

### 2.2 P1 — 一致性缺口（口径澄清）

- **硬编码颜色**:基线(2026-07-04)总引用 **1209**,其中已映射 token ~420+、EP 内部 ~350+、孤儿 ~90+、未知 ~300+;经两轮治理(replace-hardcoded-colors.js)降至 ~900。v1 的 1075 为 .vue 含 rgb 口径。处置:继续用现有 `replace-hardcoded-colors.js` 批量跑 + `check-orphan-colors.sh` 验收,**不重新发明**。
- **内联样式 1204 处**:`style="color/background"` 与布局类内联为主。处置:优先迁"颜色类"内联到 token;布局类内联可保留(动态宽度等)。
- **自定义按钮样式 72+ 处**:`start-btn/login-btn/redeem-btn/qp-go-btn/qpc-an-btn/sort-btn/rating-btn/cm-action/co-btn` 等绕开 `el-button`,同一"主操作"出现多种外观。处置:收敛为 `el-button` + 少量语义变体(实验后阶段)。
- **页面标题双模式 + 视图级重写**:`el-page-header` **12** 处,自定义 `<div class="page-header">` 广泛;**49 个视图本地重写 `.page-header`** 导致标题字号不一(全局 token 被架空)。处置:抽离 `PageHeader` 公共组件锁死字号(实验后阶段)。

### 2.3 P2 — 治理层碎片化

- **移动端三方案并存**:`useIsMobile()`（实测 2026-07-17:31 个文件引入,含 `isMobile` 引用全口径 220 处;v1 的"138 处"来源不明,作废）+ 响应式 CSS(主流)、`Mobile*` 独立组件(5 个)、`kb-bottom-tab` 自造(Vue:135)。处置:选定单一范式(响应式 + `useIsMobile` + 通用 `MobileBottomNav`),废弃模块自造 tab。
- **工具类双轨**:`.flex-*`(`index.scss:380`)与 `.u-*`(`1054`)并存 + 旧数字类 `.mt-4/.mt-8`。处置:保留一套(建议 `.u-*`),删另一套。
- **token 命名错位(非值错,采纳修正②)**:`--fs-base:14px` 即正文(符合 DESIGN.md 14px),`--fs-md:16px` 为小标题;DESIGN.md 的"md"映射代码"base"。`--radius-xs:4px` 已存在,DESIGN.md 的 sm≈代码 xs。**处置:改 DESIGN.md 命名对齐代码,勿改 token 值**(改 `--radius-sm` 6→4 会触发全站视觉回归)。rem 仅 99 处,"放大 rem 预期"言过其实。
- **win7-sim 平行视觉世界**:`components/win7-sim/*` 为刻意 Windows 7 仿真,须明确隔离为"仿真上下文",不当通用组件复用、不污染全局类名。

---

## 3. 已具备的在途治理资产（采纳现实脱节③,复用而非重造）

`tools/audit/` 已有成熟工具链,阶段 2/4 应挂接而非重写:

| 资产 | 路径 | 用途 |
|------|------|------|
| 检查脚本集合 | `tools/audit/check-hardcoded-colors.sh`、`check-inline-styles.sh`、`check-orphan-colors.sh`、`check-mobile-gaps.sh`、`check-gradient-legacy.sh`、`check-ai-patterns.sh` | 各类一致性扫描 |
| 汇总入口 | `tools/audit/run-all.sh` | 一键全量审计 |
| 批量修复 | `tools/replace-hardcoded-colors.js` | 硬编码色→token 批量替换(已两轮,1209→~900) |
| 基线报告 | `tools/audit/reports/baseline-20260704.md` | 首次全面扫描基准,含 Top30 高频色与映射表 |

**缺口仅为**:上述工具未挂钩 `lint:check`/CI,且存量仍在下降中。

---

## 4. 时间锚定排期（采纳现实脱节①:实验冻结期）

> 课题实验 **9 月 1 日冻结**,冻结至 **2027-01-20**,期间仅 `--hotfix` 可用。
> ⚠️ 偏科提分(precision)是实验核心干预工具,**冻结前/冻结期不得重写其 UI**,以免引入 bug 只能 hotfix、并威胁"干预条件一致性"课题纪律。

| 时间窗 | 做什么 | 对应条目 | 约束 |
|--------|--------|---------|------|
| **冻结前(~6 周)** | ① CI 门禁:按 §5 棘轮方案落地,husky + deploy.sh 双接入（半天工作量,置顶） ② 语义色收尾:先甄别 ≤32 文件中的真裸写（排除 var() 回退）再替换;el-tag 覆写先做 WCAG 再决定 ③ 字体决策:修订 DESIGN.md 承认系统栈,统一三处字体栈（纯文档,零风险） ④ Vant 改按需引入* | P0-2、P0-3、P0-1(按需部分) | **④硬约束:必须在 PRETEST 基线拍摄前完成并通过验收（含 27 处函数式组件样式人工验证 + bundle 前后体积对比）,否则整体推迟到实验后**;冻结前主线是触达点①-⑤部署 + PRETEST,若人力吃紧,仅保①③（①半天、③纯文档,无论如何都做） |
| **冻结期(9.1–1.20)** | 不做 UI 统一改动(课题纪律) | — | 仅 --hotfix |
| **实验后(1.20 起)** | ① van-→el- 与否重新评估(建议保留 Vant + 主题对齐) ② 72+ 自定义按钮收敛为 el-button ③ PageHeader 公共组件(锁死标题字号) ④ 移动端范式统一(kb-bottom-tab 等废弃) ⑤ token 命名与 DESIGN.md 对齐 | P1、P2 | 可自由改动 |

---

## 5. CI 门禁方案（v3 重构:棘轮制为主 + 高信噪比增量规则为辅）

### 5.1 为什么废弃 v2 的 diff-grep 白名单方案

v2 脚本经主控二轮审查确认存在四缺陷,整体废弃:

| # | 缺陷 | 后果 |
|---|------|------|
| 1 | **误杀**:检查[1]是全文件 grep 非增量 diff——按需引入落地前,precision 11 文件全是合法 `from 'vant'` import,改任意一行提交即炸;冻结前 6 周正是 precision 可能 hotfix 的窗口 | 门禁上线即被 `--no-verify` 绕过,名存实亡 |
| 2 | **挂起**:`xargs` 缺 `-r`——纯后端提交 staged 列表为空时,grep 读 stdin 挂死 pre-commit/CI | 提交流程卡死 |
| 3 | **漏杀**:检查[3]白名单按整行排除——一行同现白名单色与违规色(如 `color:#4361ee;background:#ff0000`)被整行放过 | 违规色搭白名单便车 |
| 4 | **自相矛盾**:白名单收录 `#909399/#303133`——正是 §2.2 点名要治理的 EP 中性色,CI 却放行**新增** | 门禁与治理目标打架,存量回涨 |

（另有小项:`ROOT` 死变量、`^\+` 误匹配 `+++ b/...` diff 头、`main.js:.*VantResolver` 排除项永不命中——VantResolver 配置在 vite.config.js 且 Vant 4 官方包为 `@vant/auto-import-resolver`。）

### 5.2 v3 方案:棘轮 + 两条增量规则

**核心思想**:全量口径交给棘轮——计数只许降不许升,零误杀零漏杀,天然留痕治理进度;diff 口径只保留两条不会误伤存量代码的高信噪比规则。

```bash
#!/usr/bin/env bash
# tools/audit/ci-gate-ui.sh —— UI 一致性门禁（v3:棘轮 + 增量规则）
set -euo pipefail
TOP="$(git rev-parse --show-toplevel)"

# ---------- 规则 1:硬编码色棘轮（只许降不许升） ----------
# 前提:check-hardcoded-colors.sh 增加 --count 模式只输出整数计数（由执行 agent 实现）
BASELINE_FILE="$TOP/tools/audit/color-ratchet.txt"   # 基线计数,提交进仓库
CURRENT=$(bash "$TOP/tools/audit/check-hardcoded-colors.sh" --count)
BASELINE=$(cat "$BASELINE_FILE")
if [ "$CURRENT" -gt "$BASELINE" ]; then
  echo "❌ 硬编码色计数上升:$BASELINE → $CURRENT（棘轮只许降,请把新增色改为 var(--*)）"
  exit 1
elif [ "$CURRENT" -lt "$BASELINE" ]; then
  echo "🎉 计数下降 $BASELINE → $CURRENT,请同步更新 color-ratchet.txt 锁定成果"
fi

# ---------- 增量口径:只看本次提交新增行（排除 +++ 文件头） ----------
ADDED=$(git diff --cached -U0 | grep -E '^\+[^+]' || true)

# ---------- 规则 2:禁止新增 Vant 全量注册 ----------
if echo "$ADDED" | grep -qE "app\.use\(Vant\)|import Vant from 'vant'|vant/lib/index\.css"; then
  echo "❌ 新增 Vant 全量注册/全量 CSS,请用 @vant/auto-import-resolver 按需引入"
  exit 1
fi

# ---------- 规则 3:禁止新增裸写 EP 默认语义色 ----------
# var(--el-color-*, #67c23a) 回退写法合法,按行排除;此三色合法出现场景仅此一种,行级排除风险可接受
if echo "$ADDED" | grep -E "#67c23a|#e6a23c|#f56c6c" | grep -vE "var\(--el-color-(success|warning|danger)"; then
  echo "❌ 新增裸写 Element Plus 默认语义色,请改用 var(--el-color-*)"
  exit 1
fi

echo "✅ UI 一致性门禁通过（当前计数 $CURRENT / 基线 $BASELINE）"
```

### 5.3 双接入点（缺一不可）

| 接入点 | 方式 | 作用 |
|--------|------|------|
| husky pre-commit | `package.json` 已有 `"prepare": "husky"`（实测 2026-07-17）,新增 pre-commit 钩子调用本脚本 | 开发时即时反馈 |
| **deploy.sh** | 在 `.review` 门禁旁增加调用（全量棘轮判定） | **兜底**:本项目纪律中枢是 deploy.sh,pre-commit 一个 `--no-verify` 就绕过;deploy 侧绕过须与 `--skip-review` 同级显式审批 |

> `tools/audit/run-all.sh` 保持人工深度审计用途不变;门禁只跑上述轻量三则,秒级完成。`check-orphan-colors.sh` 仍作为人工复核兜底。

---

## 6. 附录:外部 Vue3 组件库借鉴参考（统一组件库策略依据）

> 目的:为本报告"统一组件库"定调提供外部参照。结论先说:**不必换库**,Element Plus 已是正确选型;以下仅作为"借鉴其设计机制"的参考,而非替换目标。

### 6.1 结论:保留 Element Plus,不引入第二个全量库

| 项目特征 | Element Plus 契合度 |
|---------|-------------------|
| 多角色后台管理(教师/管理员/巡视员) | ⭐⭐⭐⭐⭐ 表单/表格/弹窗/权限交互是强项 |
| 中国职教 SaaS,需中文文档/社区 | ⭐⭐⭐⭐⭐ 国产库,文档中文,微信/钉钉生态友好 |
| 已建设计 token + 暗色模式 | ⭐⭐⭐⭐⭐ EP 的 `--el-*` 变量体系可被完全覆写(本报告已验证) |
| "Refined Professional / Linear·Notion" 观感 | ⭐⭐⭐⭐ 经 token 覆写可达(去阴影、0.5px 边框、靛蓝主色) |
| 论坛/社区内容区(BBS) | ⭐⭐⭐ 偏重,需自定义轻量组件补足 |

换到任何别的库都要重写 415 个 UI 文件、1097 处 `el-button`、所有 `el-table/el-dialog`——成本极高、收益近零。EP 的观感上限足够支撑 DESIGN.md 方向。

### 6.2 若需"备选/借鉴"评估:成熟 Vue3 库横向对比

| 库 | 定位 | 对本项目 | 备注 |
|----|------|---------|------|
| **Element Plus** | 中后台全家桶 | ✅ 当前主力,建议保留 | 已深度集成 |
| **Naive UI** | Vue3 原生、极简、强主题化 | 🟡 最适合"借鉴" | `n-config-provider` 单层注入主题,token 架构干净,值得学 |
| **Arco Design** | 字节企业级、设计系统级 | 🟡 适合"借鉴" | 完整 Design Token 文档 + Figma 资源,适合参考 token 命名/层级 |
| **TDesign** | 腾讯企业级 | 🟡 可参考 | 与 Arco 类似,组件全、token 规范好 |
| **Ant Design Vue** | 企业级、视觉偏重 | ⚔️ **不推荐** | 默认观感偏商务厚重,与 DESIGN.md 轻盈感冲突;换它=设计倒退 |
| **PrimeVue** | 多主题引擎(Aura/Unstyled) | 🟡 适合学"无样式"思路 | `Unstyled` 模式可完全自定义 |
| **Vant** | 移动端专用 | ✅ 保留,限定移动触控面 | 定论=按需引入+token 对齐(§2.1 P0-1);桌面观感问题实验后另议(§6.4) |
| **Reka UI / Radix Vue**(Headless) | 无样式原语 | 🟢 长期可借鉴 | 只提供交互+无障碍,视觉全自定义;未来想彻底脱离"组件库既视感"的方向 |

### 6.3 "借鉴"而非"替换"——可落地 3 点（时间锚:全部实验后,2027-01-20 起）

1. **学 Naive UI 的 token 注入方式**:用单层配置把主题当数据传,而非散落 CSS 覆写。可把现有 `theme.css` 收敛为**单一 token 清单**(primary/success/warning/danger/中性/间距/字号/圆角),消灭"语义色多定义"(§2.1 P0-2)。此项同时是 §7 主题体系的技术前提。
2. **学 Arco 的 token 文档化**:把 DESIGN.md 色板/字号/间距做成**带代码取值的权威表**,并让 CI(§5 门禁)校验色值只能源于该表。
3. **BBS/知识库等内容区用轻量自定义补足**:参考 PrimeVue Unstyled / Headless 思路,为社区区写少量无 EP 外观的自定义组件(卡片、楼主层、富文本),形成 DESIGN.md 说的"Hybrid Grid"分工(后台 EP、内容区 Notion 化)。

### 6.4 明确反推荐（v3 消歧:与 §2.1 P0-1 定论严格对齐）

- **不换 Ant Design Vue**:默认视觉与"克制、微妙层次、靛蓝信任感"相反,且 415 文件重写代价巨大。
- **不搞"全局双注册"**:被禁止的是 `app.use(Vant)` + 全量 CSS 注入每个页面（本次审计最大结构性 debt）——**不是"双库共存"本身**。本报告定论恰恰是**双库共存、各管一端**:EP 管桌面工具面,Vant 管移动触控面,经同一套 token 对齐视觉（§2.1 P0-1、§7 主题包）。
- **precision 桌面观感问题不在本轮动**:precision 页面在桌面浏览器打开时呈移动观感,是否为桌面提供 EP 版布局属产品决策,留待实验后评估。**执行 agent 不得据本节改写任何 van-* 业务代码**——那正是 v1 被退回的方案。

---

## 7. 主题体系规划（v3 新增:token 治理的直接红利,实验后实施）

> 定位:P0-2/token 收敛完成后,全站颜色只剩"token 一层",**主题 = 换一组变量,零业务改动**。这是治理工作的正向收益——统一不只是还债,还解锁能力。本节规划先行,实施全部排在实验后。

### 7.1 主题包提案

| 主题 | 受众/场景 | 说明 |
|------|----------|------|
| 亮色（默认） | 全体 | 现状保持,`#f5f5f7` 暖灰底 |
| 暗色 | 全体 | 已有（`.dark` + prefers-color-scheme）,并入统一 `data-theme` 机制 |
| **高对比度** | 教室投影、低视力学生 | 目标 WCAG AA→AAA;与 P0-2 的 el-tag 对比度评估**共用同一套检查工具**,一次投入两处收益 |
| **护眼模式** | 学生端晚自习 | 暖色温、降低底色亮度;只动中性色 token,语义色不动 |
| **大屏模式** | 智慧大屏/教室投影（SmartScreen 首用） | 字号+间距 token 整体上浮一档,非新配色——服务后排学生可读性 |

**克制决定——学科色不做主题**:学科色已由 `subjectColors.js` 集中管理（R115v2）,仅作数据着色;主色恒为 `#4361ee` 保品牌一致。主题包只动中性色与密度,避免"每科一个 APP"的割裂。

### 7.2 实现约束（给执行 agent 的硬边界）

1. **单属性驱动**:`<html data-theme="light|dark|hc|eye|screen">`,主题包只在 `theme.css` 集中定义;**禁止组件内写 `[data-theme]` 选择器**——防散落,重蹈"语义色多定义"覆辙;
2. **防 FOUC**:`index.html` 内联脚本在 app mount 前读 localStorage 设置属性,避免首帧闪白/闪黑;
3. **⚠️ ECharts 陷阱（R115v2 既有教训）**:canvas 不响应 CSS 变量变化,主题切换必须触发 `cssVar()` 重读 + chart 重渲染——R115v2 已有 cssVar() 工具与 15 文件改造经验,需补一个主题变更事件（mitt/watch）驱动重绘,否则切主题后图表还是旧配色;
4. **Vant 同步换肤**:`--van-*` 变量并入同一主题包定义,移动端跟随切换（承接 §2.1 P0-1 的主题对齐动作）;
5. **偏好持久化**:localStorage 起步;跨设备同步（写 users 设置表）另行评估,涉及 DB 变更,不在首期;
6. **排期纪律**:全部实验后。冻结前唯一相关动作 = P0-2 token 收敛本身（为主题包扫清前提）,**不新增任何主题代码**。

---

## 8. 结论与可信度自评

- **事实核准率**:约 8 成。三大 P0 核心证据(双组件库并存、语义色多定义、字体未落地)全部坐实;数据口径(vue 数、page-container 采用数、el-page-header 数)部分虚高已修正;两处具体例证(DiagnoseResult、ChildGrades)误报已撤回。
- **行动方案可信度**:v1 缺失冻结期约束、误判 Vant 选型动机、忽略在途治理,直接执行有实质风险;v2 的门禁脚本存在挂起/误杀/漏杀/自相矛盾四缺陷、§6.4 与正文自相矛盾,已在 v3 全部修正;本版按 §4 排期+§5 棘轮方案执行。
- **核心定调**:不必换组件库——Element Plus 已是正确选型且占绝对主导;统一工作的本质是"去 Vant 全量注册 + 收敛语义色/字体/token 命名 + 复用现有治理工具",且必须尊重实验冻结期。token 收敛完成后顺势解锁 §7 主题体系(高对比/护眼/大屏),让治理产出面向师生的可感知价值。外部借鉴见 §6。

---
*修订:2026-07-17 · v3 = v2 经主控二轮审查后精准修订(§4 补护栏 / §5 重构为棘轮制 / §6.4 消歧 / §7 主题体系新增 / 三处口径修正) · 下一轮审查对象:§5 脚本的落地实现(--count 模式、husky/deploy.sh 挂接)与冻结前四项的执行产物*

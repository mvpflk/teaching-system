# AGENTS.md — 教学管理系统

> 职高教学管理系统。Spring Boot 3.2 + Java 17 + MyBatis-Plus + Vue 3 + Element Plus + DeepSeek AI。

## 快速启动

```bash
# 后端 (端口 :8080/api, Swagger: /api/doc.html)
cd backend && export DB_PASSWORD=root123
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 前端 (端口 :3000)
cd frontend && npm install && npm run dev

# 后端测试（排除慢/不稳定的测试）
cd backend && mvn test -Dtest="!CoreApiSmokeTest,!CreditServiceTest,!ClassroomServiceImplTest,!AiQuestionGeneratorServiceTest"
```

默认管理员: `admin`

## 项目结构

```
teaching-system/
├── backend/       # Spring Boot → com.school.teaching.*
├── frontend/      # Vue 3 + Vite + Element Plus
├── database/      # DDL (v2~v159) + init.sql
├── tests/         # E2E 测试 (puppeteer-core)
├── tools/         # MCP server (Node.js)
├── monitoring/    # Prometheus + Grafana dashboards
├── deploy.sh      # 生产部署脚本
└── CLAUDE.md      # 完整架构文档（61条规则）
```

## ⚠️ 改动前强制检查清单（来自 30+ bug 根因，跳过 = 制造 bug）

**每次改代码，完成改动后必须逐条确认。任何一条不通过 = 不能提交。**

| # | 检查项 | 反例 |
|---|--------|------|
| 1 | **API 调用签名是否匹配后端？** | `ocrAnswerSheet(file)` 漏传 `studentId` → 400 |
| 2 | **DB 查询是否可能有重复行？** | `selectOne(PENDING)` 遇2行 → 500 |
| 3 | **异常类型是否正确？** | `RuntimeException` 代 `BusinessException` → 前端收不到错误码 |
| 4 | **定时器/回调是否产生副作用？** | `flushPending` 每30s调API → 伪装作弊 |
| 5 | **状态变更是否区分原因？** | 超时也设 `cheat_terminated=1` → 误显作弊 |
| 6 | **新增缓存/Map 有无清理机制？** | `cheatRateLimiter` 无限增长 → 内存泄漏 |
| 7 | **改方案后旧代码是否删除？** | `TeacherResearchService` 168行 @Service 无人调用 |
| + | **改动前是否读了上下游代码？** | 改前端不读后端API → 签名不匹配 |
| + | **单次改动是否 ≤2 个模块？** | 一次改5个文件跨3层 → 失控 |

**1-3 改完自查 · 4-5 推演调用链 · 6-7 部署前 grep 确认 · 最后两条动手前确认**

---

## 改动前强制分析

**每次修改任何文件前，必须先回答以下三个问题，否则禁止动手：**

1. **谁依赖我？** — 这个文件被哪些文件 import/调用？改动后它们会受影响吗？
2. **我依赖谁？** — 这个文件 import 了哪些模块？改动是否会影响它们的行为？
3. **影响范围？** — 这次改动涉及几个模块？跨模块改动必须拆分为多次独立任务。

**执行规则：**
- 单次会话只允许改动 **≤2 个模块**，超过必须拆任务
- 每个任务完成后运行 `mvn test`（后端）验证
- 如果改动涉及公共组件/工具函数，必须先检查所有调用方
- 改动前用 `grep` / `glob` 搜索相关引用，不要凭记忆判断
- **任何 agent 产出代码禁止自行部署**，必须经主控 Claude review

---

## 设计审查机制（新增 —— 来自测试任务多轮审查实践经验）

**在「设计阶段」和「编码阶段」之间增加一层审查，避免设计文档中的遗漏直接漏到代码里。**

### 第一步：变更分类

每次设计变更，先确定类型，不同类型用不同的审查清单：

| 类型 | 标识 | 典型场景 | 审查重点 |
|------|------|---------|---------|
| **TYPE_DB** | 涉及表/列/索引变更 | 新增字段、改表结构、加唯一索引 | 历史数据兼容 + 全库查询审计 |
| **TYPE_API** | 涉及 Controller 出入参 | 新增端点、改返回值、改请求体 | 前端调用方 + 签名一致 |
| **TYPE_LOGIC** | 涉及业务流程变化 | 状态机、评分规则、权限逻辑 | 边界条件 + 状态穷举 |
| **TYPE_UI** | 涉及前端组件/页面 | 新页面、改布局、交互变化 | 移动端适配 + 空态加载态 + 权限态 |
| **TYPE_MIXED** | 同时涉及多个类型 | 上述场景的组合 | 全量审计，跨层验证 |

### 第二步：类型专用审查清单

#### TYPE_DB 专用清单（新增列/表/索引时）

```markdown
## TYPE_DB 审查清单
- [ ] **默认值污染**：新列 DEFAULT 不能影响历史数据
  - 反例：`DEFAULT 60` → 历史几千条任务突然进入达标模式 ✓
  - 正解：`DEFAULT 0` 或 `DEFAULT NULL`，业务层按需覆写
- [ ] **已有查询兼容**：grep 所有 `selectList` / `selectOne` / `selectCount` 操作该表的位置
  - 反例：`selectOne` 假设"每人每任务一条" → 数据模型允许多条后抛异常 ✓
  - 正解：每条 `selectOne` 加 `.last("LIMIT 1")` 或改为 `selectList`
- [ ] **唯一索引不影响已有数据**：新加唯一索引前确认现有数据不冲突
- [ ] **全库 audit 清单**：列出所有操作该表的代码位置并逐条确认
  - 反例：声称"StudentTaskController 无需改动"但未读其代码 → 实际需改 ✓
  - 正解：声称"无需改动"前必须读过该文件核心逻辑（至少 50 行）
```

#### TYPE_API 专用清单（新增/修改端点时）

```markdown
## TYPE_API 审查清单
- [ ] **前端调用方确认**：grep 前端对该端点的调用，确保所有调用方适配
- [ ] **返回结构扩展兼容**：新增字段不应破坏前端现有解构
  - 安全：`{原有的不变, 新增的附加}` → 前端不报错
  - 危险：改字段名、改类型、删字段
- [ ] **错误码覆盖面**：新增业务异常是否映射为 `BusinessException(code, msg)`？
- [ ] **权限检查**：端点是否需要 `@PreAuthorize` 或方法内 `SecurityUtils` 校验？
```

#### TYPE_LOGIC 专用清单（业务流程变化时）

```markdown
## TYPE_LOGIC 审查清单
- [ ] **状态机穷举**：列出所有可能的状态转换，确认无遗漏
  - 方法：画状态图，标记每个转换的触发条件和副作用
- [ ] **并发竞态**：同一数据被多线程/多请求同时操作时如何防重？
  - 工具：唯一索引 / 乐观锁 / 分布式锁 / synchronized
- [ ] **边界条件表**：列出至少 5 个边界场景并注明处理方式
  - 示例：totalScore=0、passRate=0、重测次数耗尽、截止已过期
- [ ] **不要假设前端行为**：所有校验必须后端兜底
  - 反例："前端不会传这个值所以后端不校验" → 直接调 API 就绕过了
- [ ] **幂等性**：重复调用同一 API 不应产生副作用
```

#### TYPE_UI 专用清单（前端变化时）

```markdown
## TYPE_UI 审查清单
- [ ] **空态/加载态/错误态**：数据为空时显示什么？加载中？接口报错？
- [ ] **移动端适配**：桌面排版在手机屏幕能否正常显示？
- [ ] **权限态**：不同角色（管理员/教师/学生/巡视员）看到的内容是否不同？
- [ ] **无权限时**：显示空内容还是友好提示？还是直接 403？
```

### 第三步：「无需改动」声明的纪律

声称"某文件无需改动"之前，**必须读过该文件的相关代码**（至少 50 行核心逻辑），并注明确认依据。

```markdown
## 无需改动声明格式
- `TaskController.create()` ✅ 已读 30 行，只调 service.create，不受新字段影响
- `TaskSchedulerService` ✅ 已读 50 行核心逻辑，只操作 tasks.status，不查 task_submissions
- `StudentTaskController` ❌ **未读，移出清单** → 实际需要改 selectOne
```

**禁止**不读代码就声称"无需改动"。

### 第四步：设计文档中的伪代码规范

设计文档中的伪代码**必须标注实体字段的来源表**，避免实现时发现字段不存在。

```
× 不可编译的设计文档伪代码：
    lastByStudent.merge(a.getStudentId(), a, ...)

✓ 标注数据来源的设计文档伪代码：
    // StudentAnswer 只有 submissionId，没有 studentId
    // 需要 student_answers.submission_id → task_submissions.id → task_submissions.student_id
    // 即：一次 JOIN 才能拿到 studentId
```

### 第五步：审查轮次模型（复杂变更必用）

对于 TYPE_MIXED 或高风险的变更，按以下轮次组织审查：

```
轮次      | 角色        | 关注维度         | 这次实践中发现的问题
──────────┼─────────────┼─────────────────┼──────────────────────
第 1 轮   | 架构师      | 竞态/性能/数据一致性 | 唯一索引、selectOne 崩
第 2 轮   | 教育专家    | 用语/心理/教学有效性 | "冲刺中"歧义、同卷重测效度
第 3 轮   | 一线教师    | 操作路径/工作负担   | 学生无入口、主观题策略
第 4 轮   | 开发工程师  | 实体字段/查询语句   | StudentAnswer 无 studentId
```

**每个角色只审查自己的维度**，不越界。这样可以：
- 架构师不会漏掉教育语言问题（因为他不需要关注这个维度）
- 教育专家不需要理解唯一索引（自然也不会漏掉）

### 第六步：预防不是万能——已知盲区

以上机制能覆盖**大部分**设计遗漏，但仍有以下盲区需要意识：

| 盲区 | 原因 | 缓解 |
|------|------|------|
| **未知的未知** | 不知道某个隐藏依赖存在，所以不会去检查它 | 增大 grep 搜索范围，搜模式而非搜表名 |
| **假设惯性** | "这个功能很简单，肯定没问题" | 强制分类，TYPE_MIXED 自动触发全审查 |
| **角色盲区** | 缺少某个角色的输入（如缺少运维视角） | 复杂变更审查轮次至少覆盖 3 个角色 |
| **时序耦合** | 两个看似无关的模块通过时序紧耦合（先 A 后 B） | 流程图审查，标记时序依赖 |
| **存量数据** | 忘记了数据库里已经有上万条数据 | DDL 三问强制回答"历史数据会怎样" |

### 流程总览

```
[需求] → [变更分类] → [类型专用清单] → [设计稿] → [多角色审查] → [实现] → [改完自查]
    ↑ 确定 TYPE            │                       │                    ↑
    │                      │ 1. DDL 三问            │ 发现遗漏           │ 走现有 7 条清单
    └──────────────────────┴───────────────────────┘  ← 迭代修正         └────────────
```

**关键门槛**：任何 TYPE_MIXED 变更，**设计稿必须经过至少 2 个角色的审查**才能进入编码。

---

### 第七步：实现后的审计脚本——减少手工验证循环

设计审查做再好，实现时也会引入新问题。以下审计脚本在**每次代码提交前**运行，能自动捕获大部分 P0 遗漏：

```bash
# ═══════════════════════════════════════════════
# 提交前审计脚本（前端/后端各自运行）
# ═══════════════════════════════════════════════

# ── 1. 查 selectOne 是否存在"多行风险" ──
# 搜所有 selectOne 查询，确认都没有 "LIMIT 1" 保护
# 输出中如果没有 .last("LIMIT 1") 的行需要人工确认
echo "=== selectOne 未加 LIMIT 1 的查询 ==="
git diff --cached --name-only | xargs grep -n "selectOne" 2>/dev/null \
  | grep -v "LIMIT 1\|last(\"LIMIT 1\")"

# ── 2. 查新增的实体字段是否被下游使用但误解 ──
# 如果涉及 DB 新列，检查所有 setter 调用方是否使用了正确的类型
echo "=== 本次变更涉及的新实体字段 ==="
git diff --cached -U0 | grep -E '^\+.*private.*//.*新增' || echo "(无新增字段)"

# ── 3. 查新增的 Controller 端点是否缺少权限校验 ──
echo "=== 新增 Controller 方法缺少权限校验 ==="
git diff --cached --name-only | grep 'Controller.java$' | xargs grep -n 'public R\|public ResponseEntity' \
  | grep -v '@PreAuthorize\|SecurityUtils\|permitAll\|@PostMapping\|@GetMapping' || true

# ── 4. 查 RuntimeException 是否潜入 ──
echo "=== 新增 RuntimeException ==="
git diff --cached -U0 | grep '^\+.*throw new RuntimeException' || echo "(无新增 RuntimeException)"

# ── 5. 查硬编码颜色的潜入 ──
echo "=== 新增硬编码颜色(前端) ==="
git diff --cached -U0 | grep '^\+.*color\|^\+.*background' | grep -E '#[0-9a-fA-F]{3,6}|rgb(a)?\(' || echo "(无新增硬编码颜色)"
```

> **注意**：脚本只做初步筛选，**不能完全替代人工审查**。每个匹配行仍需人工确认。

---

### 第八步：防复发机制——从 Bug 到清单的闭环

**这是整个框架中最重要的一步。** 没有这一步，框架只会越来越臃肿但不会越来越聪明。

每次修复 Bug 或审查发现遗漏后，必须完成以下闭环：

```markdown
## 后见之明日志

| 日期 | 触发事件 | 根因 | 新增规则 | 所属清单 |
|------|---------|------|---------|---------|
| 2026-07-03 | 测试重测设计遗漏 | DB 默认值 60 污染历史数据 | DDL 三问：默认值选 0/NULL | TYPE_DB 清单 |
| 2026-07-03 | 同上 | `StudentAnswer` 无 `studentId` | 伪代码必须标注字段来源表 | 通用纪律 |
| 2026-07-03 | 同上 | `selectOne` 假设"只有一条" | 全库审计：搜 selectOne + LIMIT 1 | TYPE_DB 清单 |
| 2026-07-03 | 同上 | "冲刺中" 被学生误解为还有机会 | 发展性语言校验：所有用户可见文案走审 | TYPE_LOGIC 清单 |
| 2026-07-03 | 同上 | 声称"无需改动"但未读代码 | "无需改动"声明必须读过 50 行核心逻辑 | 通用纪律 |
```

**规则**：
- 每次修 Bug，更新一行日志
- 每次审查发现遗漏，更新一行日志
- 每个季度 Review 日志，看哪些规则反复触发 → 考虑自动化

这个日志放在 `docs/superpowers/recurrence-prevention-log.md`。

---

### 第九步：压缩验证循环——"一次完美"不现实，"快速纠偏"是目标

承认"一次性完美"不成立后，策略应该从**减少 Bug 数**转向**缩短 Bug 发现周期**：

```
没有框架:  设计(1x) → 编码(3x) → 测试发现Bug(5x) → 修复(3x) → 再测试(5x) → ...
有框架:    设计审查(1.5x) → 编码(2x) → 审计脚本(0.1x) → 测试(2x) → ...
                                                         ↑
                                                  Bug 在提交前就被截住了
```

具体到这个项目的实践：

| 阶段 | 发现问题 | 修复成本 | 如果没有审查 |
|------|---------|---------|------------|
| 设计审查 | `selectOne` 崩 | 改 1 行代码 | 线上 500 错误 |
| 设计审查 | DDL 默认值 | 改 1 行 SQL | 几千条历史任务异常 |
| 设计审查 | `StudentAnswer` 无 studentId | 改 10 行 + JOIN | 编译不过→调试 2 小时 |
| 设计审查 | 学生无入口 | 加弹窗按钮 | 上线后学生投诉 |
| 编码后审计 | 审计脚本捕获遗漏 | 秒级 | 人工检查 30 分钟 |

**核心理念转变**：

> 不要追求"一次写对"——这不可能。要追求"每次偏离都能在 5 分钟内被自动发现"。

落地行动：
1. 每次提交前跑审计脚本（耗时 <10 秒）
2. 审计脚本捕获的问题修复在 <5 分钟
3. 审计脚本无法覆盖的才走人工测试
4. 人工测试发现的问题 → 反哺审计脚本（下次就自动化了）

---

### 第⼗步：实现后验证——设计⽂档逐条追踪（2026-07-03 新增，来⾃审核漏掉 passMode/retakeDeadline）

设计审查再完善，实现时也会有遗漏。**编码完成后，必须逐条验证设计⽂档的每个需求在代码中真实存在。**

```markdown
## 实现后验证清单

逐条翻阅设计⽂档，对每个 § 标出：
- §2.1 tasks 表 3 列 → database/v200_task_retake.sql ✅
- §3.2 达标判断+重测创建 → ExamTaskHandler.onSubmit() ✅
- §5.1 达标得分率滑块 → TaskExamForm.vue 达标设置区块 ✅
- §5.1 重测策略单选组 → TaskExamForm.vue passMode ⚠️ 后端⽆对应字段/逻辑 ← 遗漏
```

**规则**：
- 设计⽂档中每个功能点必须有唯⼀的代码位置对应
- 发现"设计有但代码⽆" → 要么实现它，要么更新设计⽂档标注"延期"
- **禁⽌**前端存在选择项而后端⽆对应处理（如 passMode 单选组背后⽆逻辑）

**审计脚本增强**（追加到第七步脚本末尾）：

```bash
# ── 6. 查前端占位符 TODO 是否混⼊提交 ──
echo "=== 新增 TODO/FIXME 占位符 ==="
git diff --cached -U0 | grep '^\+.*TODO\|^\+.*FIXME\|^\+.*// 需要\|^\+.*// 待实现' \
  || echo "(⽆新增占位符)"

# ── 7. 查前端 el-radio/el-select 选项在后端是否有对应字段 ──
# ⼈⼯审查：前端每个选项必须有后端字段或逻辑处理
echo "=== ⼈⼯确认：前端选项与后端字段映射 ==="
git diff --cached --name-only | grep '\.vue$' | xargs grep -n 'el-radio\|el-select' 2>/dev/null \
  | grep -v 'size\|placeholder\|clearable\|filterable' || true
```

---

### 第⼗⼀步：⽅法内⼝径⼀致性——同⼀⽅法的多个统计必须使⽤同⼀过滤条件（2026-07-03 新增）

同⼀⽅法中，如果同时计算 avg/max/min/count/passCount 等多个统计值，**所有统计必须使⽤相同的过滤条件**。
违反会产⽣"均分⽤首次、最⾼分⽤全部"这类难于发现的逻辑⽭盾。

```java
// ❌ 错误：avg 过滤了 isOfficial，max/min 没有
double avg = list.stream().filter(s -> isOfficial).mapToDouble(...).avg();
double max = list.stream().mapToDouble(...).max();  // 未过滤！

// ✅ 正确：所有统计复⽤同⼀个过滤后的集合
List<X> filtered = list.stream().filter(s -> isOfficial).toList();
double avg = filtered.stream().mapToDouble(...).avg();
double max = filtered.stream().mapToDouble(...).max();
```

**审查要点**：
- grep 搜索 `\.stream()` 后跟 `mapToDouble|mapToInt|count|filter` 的⽅法
- 确认同⼀⽅法内多个 stream 链使⽤了相同的过滤条件
- 建议做法：提前把过滤后的集合存为局部变量，各统计复⽤

---

### 第⼗⼆步：显式优于默认——不依赖 DB 默认值，业务字段必须显式赋值（2026-07-03 新增）

MyBatis-Plus 插⼊实体后，依赖 `DEFAULT` 的字段在 Java 对象中为 `null`。
后续代码靠 null-safe 兜底（`getX() != null ? getX() : defaultValue`）是脆弱的。

```java
// ❌ 脆弱：依赖 DB DEFAULT 1，Java 对象中 attemptNumber=null
TaskSubmission sub = new TaskSubmission();
sub.setTaskId(taskId);
submissionMapper.insert(sub);
// sub.getAttemptNumber() → null，靠下游 null-safe 兜底

// ✅ 健壮：显式赋值，Java 对象中 attemptNumber=1
TaskSubmission sub = new TaskSubmission();
sub.setTaskId(taskId);
sub.setAttemptNumber(1);
sub.setIsOfficial(true);
submissionMapper.insert(sub);
// sub.getAttemptNumber() → 1，下游可直接使⽤
```

**规则**：
- `@TableId(type = IdType.AUTO)` 的主键可依赖 DB ⾃增
- 所有业务字段（`status`、`attemptNumber`、`isOfficial`、`score` 等）**必须显式 set**
- 审计脚本检查：grep `new TaskSubmission()` 后⾯ 10 ⾏内没有 `setAttemptNumber` / `setIsOfficial` 的，报警告

## 编码铁律（违反必被拒）

### 后端

- **禁止 `throw new RuntimeException()`** → 用 `BusinessException(code, msg)`
- **Controller 禁止注入 Mapper** → 所有 DB 操作走 Service（实验后统一修复，新增代码必须遵守）
- **≥2 表写入必须 `@Transactional`**，且事务必须能感知异常（禁止 try-catch 吞噬）
- **禁止循环内查数据库** (N+1) → 批量查询 + 内存组装
- **selectOne 查询前确认字段无重复可能**，有风险加 `.last("LIMIT 1")`
- **状态字段必须区分原因**（如 TERMINATED 区分超时/作弊，用不同字段或不同标记位）
- **定时器/回调/Scheduled 任务必须评估副作用**，确认不会产生意外计数/写入
- **改方案后旧代码/旧文件必须删除**，grep 确认无残留引用
- **单方法 ≤50 行**，超过拆分子方法
- **认证统一用 `SecurityUtils`**，禁止手动解析 JWT
- **school_id 写死为 1**，异步线程用 `SchoolContext.set(1L)` + finally clear
- **实体类必须实现 `Serializable`**（Redis 序列化会崩）
- **teacher_classes.subject 逗号分隔** → `split("[,，、]")`
- **含中文 SQL 必须 utf8mb4**: `mysql --default-character-set=utf8mb4`
- **EncodingUtils.fix(text)**: 数据库中文出站前修复 latin1 双重编码
- **统一响应**: `R<T>` — `R.ok(data, msg)` / `R.error(code, msg)`

### 前端

- **所有 API 调用走 `@/api/*`**，禁止组件内直接调 axios/fetch
- **禁止硬编码颜色** → 必须用 `var(--primary-color)` 等 CSS 变量
- **v-html 必须消毒** → `renderMarkdown()` / `sanitizeHtml()` (DOMPurify)
- **echarts 按需引入** → `import('@/utils/echarts')` (300KB)，禁止全量 (827KB)
- **SSE 必须用 `useClassroomSSE` composable**
- **单 Vue 文件 ≤300 行**，`<script setup>` ≤200 行
- **el-upload 必须写完整路径 + 手动传 headers token**
- **findChildren 统一用 `@/utils/category.js`**

## 架构模式

| 模式 | 实现 |
|------|------|
| 统一响应 | `R<T>` |
| 异常处理 | `BusinessException` → `GlobalExceptionHandler` |
| 异步任务 | `taskStore.create()` → `@Async executeAsync` → `taskStore.complete/fail()` |
| AI Prompt | `PromptBuilder.build()` → `executeAsync()` → `DeepSeekGateway.buildPrompt()` 全链路审计 |
| SSE 安全 | `sseTicket.js` 获取 60s 一次性 ticket 后建 EventSource，主 JWT 不暴露 |
| 权限三层 | SecurityConfig(filter) → @PreAuthorize(AOP) → SecurityUtils(方法体) |
| Feature 开关 | `useSettingsStore().isEnabled(key)` — SQL 写入 → 路由 meta.featureKey → 侧边栏 v-if |

## 角色

| role_name | 权限 |
|-----------|------|
| SUPER_ADMIN / ADMIN | 全部 / 业务管理 |
| INSPECTOR | 全局巡视 + 预警 |
| TEACHER / HEAD_TEACHER | 自己的班级/任务 |
| STUDENT / PARENT | 自己的数据 |

## 部署

```bash
./deploy.sh           # 全量 (~45s，前后端并行)
./deploy.sh backend   # 仅后端 (~40s)
./deploy.sh frontend  # 仅前端 (~15s)
```

生产服务器: `your-server-ip` (腾讯云 2核4G)，容器化部署 (Docker Compose)
监控: `/prometheus/graph` · `/grafana/` (admin/your-grafana-password)

## 关键约定

- **永远用中文回复**
- **AI Prompt 新增内容类型必须全链路审计** (PromptBuilder → DeepSeekGateway → 实际送达)
- **诊断报告用发展性语言**: 禁"差""不及格"，用"发展中"/"起步期"/"已达标"
- **AI 评分低置信度(<0.85)**: 前端红框 + 脉冲动画 + 建议复核横幅
- **Vue3 TDZ 陷阱**: `<script setup>` 中 `const` 声明必须在 `watch` 之前

## 参考文档

- `CLAUDE.md` — 完整架构 (61 条规则，含代码模式、AI Prompt 全链路、题型判分)
- `CONVENTIONS.md` — 编码铁律 (前端/后端不可违反项)
- `DESIGN.md` — 设计系统 (Refined Professional, #4361ee, Geist 字体)

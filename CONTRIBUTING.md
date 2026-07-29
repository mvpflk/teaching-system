# 贡献指南

感谢你考虑为职高教学管理系统贡献代码或想法！

## 关于这个项目

- 作者是一线中职教师，利用业余时间从零开发
- 项目的目标是：**让每一所中职学校都能用上免费、好用的教学管理系统**
- 无论你是教师、学生还是开发者，欢迎以各种方式参与

---

## 如何参与

### 🐛 报告 Bug

请使用 [Bug 报告模板](https://github.com/mvpflk/teaching-system/issues/new?template=bug_report.md) 提交 Issue，并尽可能提供：

- 你的部署环境（Docker / 直接部署）
- 复现步骤
- 错误日志（注意隐藏个人隐私和敏感信息）

### 💡 提出功能建议

请使用 [功能建议模板](https://github.com/mvpflk/teaching-system/issues/new?template=feature_request.md) 提交 Issue。

如果你是一线教师，有实际教学场景中的需求，欢迎直接提——这是项目最需要的声音。

### 🚀 提交代码

#### 1. 环境准备

```bash
# 后端
cd backend
# 确保 Java 17+ 和 Maven 已安装
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 前端（另一个终端）
cd frontend
npm install
npm run dev
```

#### 2. 分支策略

- `master` 分支始终保持稳定
- 功能开发从 `master` 切出特性分支：`feat/你的功能名`
- Bug 修复分支：`fix/你的修复名`

#### 3. Commit 规范

使用语义化的 commit message：

```
feat: 新增 AI 出题的知识点选择
fix: 修复成绩分析页白屏问题
docs: 更新部署指南
refactor: 重构积分计算逻辑
chore: 升级依赖版本
style: 格式化代码（无逻辑变化）
test: 添加提交批改测试
```

#### 4. 代码规范

**后端**：
- 禁止 `throw new RuntimeException()` → 用 `BusinessException(code, msg)`
- Controller 禁止注入 Mapper → 所有 DB 操作走 Service
- ≥2 表写入必须 `@Transactional`
- 禁止循环内查数据库（N+1）

**前端**：
- 所有 API 调用走 `@/api/*`，禁止组件内直接调 axios
- 禁止硬编码颜色 → 用 CSS 变量 `var(--primary-color)`
- 单 Vue 文件 ≤300 行

#### 5. 提交 PR 前确认

- [ ] 后端测试通过：`mvn test -Dtest="!CoreApiSmokeTest,!CreditServiceTest,!ClassroomServiceImplTest,!AiQuestionGeneratorServiceTest"`
- [ ] 前端测试通过：`npm run test`
- [ ] 前端构建通过：`npm run build`
- [ ] ESLint 无报错：`npm run lint:check`
- [ ] 本次改动涉及 ≤2 个模块
- [ ] 检查清单（见 PR 模板）逐条确认

#### 6. PR 流程

1. 确保你的 fork 与上游同步
2. 创建 PR，填写 PR 模板
3. 等待 CI 通过
4. 项目维护者 review 后合并

---

## 非代码贡献

### 📖 文档改进

发现文档有错别字、表述不清或遗漏？直接提 PR 修改即可。

### 🎨 UI/UX 反馈

如果你对界面设计、交互流程有改进建议，欢迎提交 Issue 或 PR。

### 🌐 推广

- 在你的学校或朋友圈推荐这个项目
- 在知乎、V2EX 等平台分享使用体验
- 在 GitHub 上点 ⭐ Star——这对开源项目是很大的鼓励

---

## 行为准则

本项目采用 [贡献者公约](CODE_OF_CONDUCT.md)。我们承诺为所有贡献者提供友善、包容的环境。

---

## 联系作者

- 在 GitHub Issues 中提问（公开，方便后人搜索）
- 加入项目交流群（见仓库主页）

再次感谢你的参与 ❤️

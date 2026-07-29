package com.school.teaching.service;

/**
 * 学科分组感知的卡片生成 Prompt 构建器。
 * 纯逻辑工具类，无 Spring 依赖，遵循 {@link TeachingContentPromptBuilder} 的模式。
 *
 * <p>路由由 {@code DictSubject.cardProfileGroup} 驱动：
 * <ul>
 *   <li>{@code public-math}     — 数学，公式定理基础优先(≥40% DEFINITION)</li>
 *   <li>{@code public-language} — 语文/英语，字词/语法/默写基础优先(≥50% DEFINITION)</li>
 *   <li>{@code major}           — 专业课，AI 根据知识节点 content 自主判断应知/应会配比</li>
 *   <li>{@code null}            — 兜底为 major</li>
 * </ul>
 *
 * <p>防超纲：每套模板均以知识节点 {@code content} 为唯一准确来源，
 * 明确禁止 AI 编造节点内容和考纲范围外的信息。
 *
 * @since v169
 */
public final class CardPromptBuilder {

    private CardPromptBuilder() {}

    /**
     * @param cardProfileGroup DictSubject.cardProfileGroup，nullable(兜底 major)
     * @param nodeContent      知识节点 content（【一句话定义】→【具体例子】→【常见错误】→【出题方向】）
     * @param syllabusContext  考纲上下文（来自 ExamSyllabusService）
     * @param articleTitle     文章标题
     * @param articleContent   文章 Markdown 正文
     * @param nodePath         知识点完整路径，如 "数学[职高] > 函数 > 定义域"
     * @param subjectName      L1 根节点名（如 "数学[职高]"）
     * @return PromptResult 包含最终 prompt 文本和建议 maxTokens
     */
    public static PromptResult build(
            String cardProfileGroup,
            String nodeContent,
            String syllabusContext,
            String articleTitle,
            String articleContent,
            String nodePath,
            String subjectName) {

        String group = (cardProfileGroup == null || cardProfileGroup.isBlank())
                ? "major" : cardProfileGroup.trim();

        // 卡片正面只标注知识点名称（路径最后一级），完整路径存 context_path
        String leafName = leafName(nodePath);

        return switch (group) {
            case "public-math"     -> buildPublicMath(syllabusContext, nodeContent,
                    articleTitle, articleContent, nodePath, leafName, subjectName);
            case "public-language" -> buildPublicLanguage(syllabusContext, nodeContent,
                    articleTitle, articleContent, nodePath, leafName, subjectName);
            default                -> buildMajor(syllabusContext, nodeContent,
                    articleTitle, articleContent, nodePath, leafName, subjectName);
        };
    }

    /** 提取知识点名称（路径最后一级），如 "任务5-维护Windows" */
    private static String leafName(String fullPath) {
        if (fullPath == null || fullPath.isEmpty()) return "";
        String[] parts = fullPath.split(" > ");
        return parts[parts.length - 1];
    }

    // ═══════════════════════════════════════════════════════════════
    // 公共-数理: 数学[职高]
    // ═══════════════════════════════════════════════════════════════

    private static PromptResult buildPublicMath(
            String syllabus, String nodeContent,
            String title, String content, String path, String leafName, String subject) {

        String prompt = """
                你是四川省中职对口升学数学学科的命题专家。你的任务是为职高学生生成高质量知识卡片，\
                帮助学生牢固掌握公式定理基础并灵活应用于解题。

                ═══ 考纲范围（所有卡片内容不得超出以下范围）═══
                %s

                ═══ 知识点内容（唯一准确来源，不得编造或扩展以下内容）═══
                %s

                ═══ 知识点路径 ═══
                %s

                ═══ 文章标题 ═══
                %s

                ═══ 文章内容（参考）═══
                %s

                ═══ 任务 ═══
                为上述知识点生成 4-6 张知识卡片。数学学科必须侧重公式定理基础：

                【基础必选 — 占比 ≥ 40%%】DEFINITION 类型，聚焦公式定理本身：
                  - 正面：以"[%s]"作为模块前缀，然后写"写出XXX的公式"或"XXX的定义是什么？"
                  - 背面：完整公式 + 每个符号含义 + 适用条件/范围
                  - 如果知识点涉及多个公式，每个公式独立成卡

                【解题应用 — 至少 1 张】APPLICATION 类型：
                  - 正面：一道可直接代入公式求解的具体计算题（数值不超出考纲范围）
                  - 背面：分步解题过程(1→2→3) + 代入的公式 + 最终答案 + 易错点提示

                【对比辨析 — 可选】COMPARISON 类型，用于易混淆概念（如 ∈ vs ⊆、sin vs cos）

                其他类型按内容自动决定。

                【严格要求】
                1. 知识点内容为唯一准确来源，不得超出此范围编造任何内容
                2. 所有卡片内容必须在考纲范围内
                3. 正面文字（不含模块前缀）≤ 30 字，背面 ≤ 100 字，语言适合中职学生
                4. 避免"差""不及格"等否定词汇，使用"需加强""再练练"等发展性语言
                5. 正面以短模块路径"[%s]"开头

                返回纯JSON数组（不要markdown代码块）：
                [{"cardType":"DEFINITION","frontText":"[%s] 问题","backText":"答案"}, ...]
                """.formatted(
                syllabus, nodeContent, path,
                title, content,
                leafName, leafName, leafName);

        return new PromptResult(prompt, 2500);
    }

    // ═══════════════════════════════════════════════════════════════
    // 公共-语言: 语文[职高]、英语[职高]
    // ═══════════════════════════════════════════════════════════════

    private static PromptResult buildPublicLanguage(
            String syllabus, String nodeContent,
            String title, String content, String path, String leafName, String subject) {

        String prompt = """
                你是四川省中职对口升学%s学科的命题专家。你的任务是为职高学生生成高质量知识卡片，\
                帮助学生牢固掌握基础知识（字词/语法/默写/文学常识）并能在考试中准确应用。

                ═══ 考纲范围（所有卡片内容不得超出以下范围）═══
                %s

                ═══ 知识点内容（唯一准确来源，不得编造或扩展以下内容）═══
                %s

                ═══ 知识点路径 ═══
                %s

                ═══ 文章标题 ═══
                %s

                ═══ 文章内容（参考）═══
                %s

                ═══ 任务 ═══
                为上述知识点生成 4-6 张知识卡片。语言学科必须侧重基础知识的记忆与辨析：

                【基础必选 — 占比 ≥ 50%%】DEFINITION 类型，聚焦基础要素：
                  - 语文：字音字形辨析、词语释义、名篇名句默写、文学常识、修辞手法识别
                    注意：涉及多音字/形近字时，标注四川方言易混点（平翘舌、鼻边音、前后鼻音）
                  - 英语：单词释义、词组搭配、语法规则、句型结构、时态语态用法
                    每张背面必须包含至少 1 个完整例句
                  - 正面：以"[%s]"作为模块前缀，然后写问题

                【对比辨析 — 建议 1-2 张】COMPARISON 类型：
                  - 相近字词/语法点对比（如 "的/地/得"、"borrow/lend"）
                  - 背面：对比要点 + 各自例句 + 记忆技巧

                其他类型按内容自动决定。

                【严格要求】
                1. 知识点内容为唯一准确来源，不得超出此范围编造任何内容
                2. 所有卡片内容必须在考纲范围内
                3. 正面文字（不含模块前缀）≤ 30 字，背面 ≤ 100 字，语言适合中职学生
                4. 避免"差""不及格"等否定词汇，使用"需加强""再练练"等发展性语言
                5. 正面以短模块路径"[%s]"开头

                返回纯JSON数组（不要markdown代码块）：
                [{"cardType":"DEFINITION","frontText":"[%s] 问题","backText":"答案"}, ...]
                """.formatted(
                subject, syllabus, nodeContent, path,
                title, content,
                leafName, leafName, leafName);

        return new PromptResult(prompt, 2500);
    }

    // ═══════════════════════════════════════════════════════════════
    // 专业课: 农学/建筑/网络/信息/教育等
    // ═══════════════════════════════════════════════════════════════

    private static PromptResult buildMajor(
            String syllabus, String nodeContent,
            String title, String content, String path, String leafName, String subject) {

        String prompt = """
                你是四川省中职对口升学%s学科的命题专家。你的任务是为职高学生生成高质量知识卡片，\
                帮助学生系统掌握专业知识，应对升学考试。

                ═══ 考纲范围（所有卡片内容不得超出以下范围）═══
                %s

                ═══ 知识点内容（唯一准确来源，不得编造或扩展以下内容）═══
                %s

                ═══ 知识点路径 ═══
                %s

                ═══ 文章标题 ═══
                %s

                ═══ 文章内容（参考）═══
                %s

                ═══ 任务 ═══
                为上述知识点生成 4-6 张知识卡片。根据知识点内容性质自行决定卡片类型分布。

                五种类型说明：
                - DEFINITION：概念定义、术语解释、原理阐述
                - PROCEDURE：操作步骤、工作流程、工艺方法
                - COMPARISON：相近概念/方法对比辨析、易混淆点区分
                - APPLICATION：案例分析、计算应用、实际问题解决
                - SCENARIO：场景判断、故障诊断、方案选择

                【类型分配原则 — 严格根据知识点内容性质决定】
                1. 先分析知识点内容属于哪类：理论知识为主？实践技能为主？还是两者兼顾？
                2. 理论知识为主（概念/原理/定义/术语）→ 侧重 DEFINITION + COMPARISON
                3. 实践技能为主（操作/流程/方法/步骤）→ 侧重 PROCEDURE + APPLICATION
                4. 考试中常以分析题/应用题形式出现 → 侧重 APPLICATION + SCENARIO
                5. 均衡型知识点 → 各类型均匀分布

                【严格要求】
                1. 知识点内容为唯一准确来源，不得超出此范围编造任何内容
                2. 所有卡片内容必须在考纲范围内
                3. 正面文字（不含模块前缀）≤ 30 字，背面 ≤ 100 字，语言适合中职学生
                4. 避免"差""不及格"等否定词汇，使用"需加强""再练练"等发展性语言
                5. 正面以短模块路径"[%s]"开头

                返回纯JSON数组（不要markdown代码块）：
                [{"cardType":"DEFINITION","frontText":"[%s] 问题","backText":"答案"}, ...]
                """.formatted(
                subject, syllabus, nodeContent, path,
                title, content, leafName, leafName);

        return new PromptResult(prompt, 2500);
    }

    // ═══════════════════════════════════════════════════════════════

    /** Prompt 构建结果 */
    public record PromptResult(String prompt, int maxTokens) {
        public PromptResult {
            if (prompt == null || prompt.isBlank()) {
                throw new IllegalArgumentException("prompt must not be blank");
            }
            if (maxTokens <= 0) {
                maxTokens = 2000;
            }
        }
    }
}

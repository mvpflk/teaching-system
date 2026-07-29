package com.school.teaching.service.impl;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * DeepSeek Prompt 构建器 — 负责 System Prompt 和 User Prompt 的构建。
 * 包含：角色设定、考纲约束、难度配比、题型数量、输出格式、RAG 段落拆分。
 */
@Slf4j
public class DeepSeekPromptBuilder {

    private final DeepSeekResponseParser parser;

    public DeepSeekPromptBuilder(DeepSeekResponseParser parser) {
        this.parser = parser;
    }

    String buildSystemPrompt(Map<String, Object> params) {
        String subject = String.valueOf(params.getOrDefault("subject", "通用"));
        String stageHint = String.valueOf(params.getOrDefault("stageHint", "中职"));
        String bareSubject = subject.replaceAll("\\[.*?\\]", "").trim();
        if (bareSubject.isEmpty()) bareSubject = "通用";

        StringBuilder sb = new StringBuilder();
        sb.append("你是").append(stageHint).append(bareSubject).append("教师，擅长根据知识库内容生成高质量教学资源。");

        String syllabusTitle = String.valueOf(params.getOrDefault("syllabusTitle", ""));
        String syllabusScope = String.valueOf(params.getOrDefault("syllabusScope", ""));
        String syllabusContext = String.valueOf(params.getOrDefault("syllabusContext", ""));
        if (!"null".equals(syllabusTitle) && !syllabusTitle.isEmpty()) {
            sb.append("你正在为「").append(syllabusTitle).append("」生成内容，所有题目和知识点必须严格限定在该考纲范围内。");
        }
        if (!"null".equals(syllabusScope) && !syllabusScope.isEmpty()) {
            sb.append("考纲覆盖知识点：").append(syllabusScope).append("。");
        }
        if (!"null".equals(syllabusContext) && !syllabusContext.isEmpty()) {
            sb.append("以下为本次出题/备课必须遵循的对口升学考纲，仅在此范围内出题：\n").append(syllabusContext).append("\n");
        }
        sb.append("仅针对四川省对口升学考试考点出题，不涉及考纲外内容。");

        boolean isQuestionGen = params.containsKey("questionType")
            || String.valueOf(params.getOrDefault("prompt", "")).contains("出题");
        if (isQuestionGen) {
            sb.append("出题难度系数严格控制在 0.40~0.70（即中等难度，通过率 40%~70%），")
              .append("不得出过难(>0.70)或过易(<0.40)的题目。")
              .append("题型以选择题和填空题为主，每道题需标注所属考纲知识点。");
        }

        sb.append("严格遵循知识库中的知识点、概念和范例，不编造不存在的知识点。");
        sb.append("输出结构严谨、内容准确、语言简洁，适合").append(stageHint).append("学生理解。");
        sb.append("如果知识库信息不足以回答，请说明缺少哪些信息，不要猜测。");

        return sb.toString();
    }

    String buildPrompt(Map<String, Object> p) {
        String point = String.valueOf(p.getOrDefault("knowledgePoint", p.getOrDefault("knowledgePoints", "通用")));
        String rawType = String.valueOf(p.getOrDefault("questionType", p.getOrDefault("questionTypes", "MIXED")));
        String categoryPath = p.containsKey("categoryPath") ? String.valueOf(p.get("categoryPath")) : null;
        int refDifficulty = p.get("difficultyLevel") instanceof Number n ? n.intValue() : 2;
        String stageHint = String.valueOf(p.getOrDefault("stageHint", ""));
        String referenceMaterial = p.containsKey("referenceMaterial") ? String.valueOf(p.get("referenceMaterial")) : null;
        String studentMajor = p.containsKey("studentMajor") ? String.valueOf(p.get("studentMajor")) : null;
        String alreadyCovered = p.containsKey("_alreadyCovered") ? String.valueOf(p.get("_alreadyCovered")) : null;

        @SuppressWarnings("unchecked")
        Map<String, Integer> typeCounts;
        Object tcObj = p.get("typeCounts");
        if (tcObj instanceof Map<?,?> m && !m.isEmpty()) {
            typeCounts = new LinkedHashMap<>();
            m.forEach((k, v) -> typeCounts.put(String.valueOf(k), v instanceof Number n ? n.intValue() : 0));
        } else {
            typeCounts = Map.of(rawType.equals("MIXED") ? "MIXED" : rawType,
                p.get("candidateCount") instanceof Number n ? n.intValue() : 4);
        }

        int totalCount = typeCounts.values().stream().mapToInt(Integer::intValue).sum();
        if (totalCount == 0) totalCount = 4;

        String roleSubject = String.valueOf(p.getOrDefault("subject", ""));
        if (roleSubject.isEmpty() && categoryPath != null && !categoryPath.isEmpty()) {
            String top = categoryPath.contains(" → ") ? categoryPath.split(" → ")[0].trim() : categoryPath.trim();
            roleSubject = top.replaceAll("\\[.*?\\]", "").trim();
        }
        if (roleSubject.isEmpty()) roleSubject = "通用";

        String effectiveStage = stageHint.isEmpty() ? "" : stageHint;
        String roleDesc = effectiveStage + roleSubject + "教师";

        boolean classroomQuestions = p.get("classroomQuestions") instanceof Boolean b && b;
        boolean comprehensive = p.get("comprehensive") instanceof Boolean b && b;

        String instructionPrompt = String.valueOf(p.getOrDefault("_instructionPrompt", ""));
        boolean hasInstructionPrompt = !"null".equals(instructionPrompt) && !instructionPrompt.isEmpty();
        boolean skipGenericFormat = p.get("_skipGenericFormat") instanceof Boolean b && b;
        String tierFocus = String.valueOf(p.getOrDefault("tierFocus", ""));
        String knowledgeDim = String.valueOf(p.getOrDefault("knowledgeDim", ""));
        String syllabusContext = String.valueOf(p.getOrDefault("syllabusContext", ""));
        @SuppressWarnings("unchecked")
        Map<String, Integer> scorePresets = p.get("scorePresets") instanceof Map<?,?> sm && !sm.isEmpty()
            ? (Map<String, Integer>) sm : null;

        List<String> ragParagraphs = splitRagParagraphs(referenceMaterial);

        StringBuilder sb = new StringBuilder();

        // ── 专用 Prompt 优先 ──
        if (hasInstructionPrompt) {
            sb.append(instructionPrompt);
            sb.append("\n\n");
            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> cpList =
                p.get("categoryPaths") instanceof java.util.List ? (java.util.List<java.util.Map<String, Object>>) p.get("categoryPaths") : null;
            if (cpList != null && !cpList.isEmpty()) {
                sb.append("【知识点ID映射】（每题必须标注knowledgeNodeId）\n");
                for (int i = 0; i < cpList.size(); i++) {
                    java.util.Map<String, Object> cpItem = cpList.get(i);
                    sb.append("节点").append(i + 1).append("：ID=").append(cpItem.get("nodeId")).append("（").append(cpItem.get("path")).append("）\n");
                }
                sb.append("\n");
            }
            if (classroomQuestions) {
                sb.append("请严格按纯JSON数组输出，包含questionText/questionType/correctAnswer/explanation/intent/category字段。\n");
            }
        } else {
            // ── 通用出题模式 ──
            sb.append("你是").append(roleDesc).append("，请按以下精确数量生成题目，知识点：「").append(point).append("」。");
            if (categoryPath != null && !categoryPath.isEmpty()) {
                sb.append(" 归类路径：").append(categoryPath).append("。");
            }
        }

        // 多节点命题范围
        @SuppressWarnings("unchecked")
        java.util.List<java.util.Map<String, Object>> categoryPaths =
            p.get("categoryPaths") instanceof java.util.List ? (java.util.List<java.util.Map<String, Object>>) p.get("categoryPaths") : null;
        if (categoryPaths != null && !categoryPaths.isEmpty()) {
            sb.append("\n\n【命题范围 — 共").append(categoryPaths.size()).append("个节点，请均匀分配题量】\n");
            for (int i = 0; i < categoryPaths.size(); i++) {
                java.util.Map<String, Object> cp = categoryPaths.get(i);
                sb.append("节点").append(i + 1).append("：").append(cp.get("path")).append("（ID:").append(cp.get("nodeId")).append("）\n");
            }
            sb.append("\n每题JSON中请带上 \"knowledgeNodeId\" 字段指向对应的节点ID。");
        }

        if (studentMajor != null && !studentMajor.isEmpty())
            sb.append(" 学生专业：").append(studentMajor).append("。");
        sb.append(" 难度：").append(refDifficulty).append("/5");
        if (!effectiveStage.isEmpty()) sb.append("（").append(effectiveStage).append("水平）");
        sb.append("。\n");

        // ── 考纲约束 ──
        if ((comprehensive || classroomQuestions) && !"null".equals(syllabusContext) && !syllabusContext.isEmpty() && !hasInstructionPrompt) {
            sb.append("\n【升学考试考纲对接】。请参照以下考试大纲要求出题：\n");
            sb.append(syllabusContext).append("\n");
        }

        // ── 难度配比 ──
        if (comprehensive && !hasInstructionPrompt && !tierFocus.isEmpty() && !"null".equals(tierFocus)) {
            String tierDesc = switch (tierFocus) {
                case "BASIC" -> "以基础题为主(70%)，中等题(30%)，不出难题。";
                case "ADVANCED" -> "基础题(20%)，中等题(40%)，难题(40%)。";
                default -> "中等题(50%)，较难题(30%)，综合题(20%)。";
            };
            sb.append("\n【难度配比】").append(tierDesc).append(" 请在JSON的tier字段中标注每题的层级(BASIC/MEDIUM/ADVANCED)。\n");
        }

        // ── 综合练习：维度关注 ──
        if (comprehensive && !hasInstructionPrompt && !knowledgeDim.isEmpty() && !"null".equals(knowledgeDim)) {
            String dimDesc = switch (knowledgeDim) {
                case "THEORY" -> "侧重理论(应知)，实操(应会)为辅。";
                case "PRACTICE" -> "侧重实操(应会)，理论(应知)为辅。";
                default -> "兼顾理论(应知)与实操(应会)，侧重综合应用。";
            };
            sb.append("【出题维度】").append(dimDesc).append(" 请在JSON的knowledgeDim字段标注(THEORY/PRACTICE)。\n");
        }

        // ── 综合练习：跨知识点与真实场景 ──
        if (comprehensive && !hasInstructionPrompt) {
            sb.append("\n【综合要求】题目应模拟真实场景，综合运用多个关联知识点。");
            sb.append("至少1道简答题需要跨知识点分析，体现知识间的融会贯通，不可孤立考查单一知识点。\n");
        }

        // ── 分值权重提示 ──
        if (scorePresets != null) {
            sb.append("\n【分值参考】各题型每道分值如下（高分值题型应包含更丰富的考查内容或更深度的分析）：");
            for (Map.Entry<String, Integer> e : scorePresets.entrySet()) {
                String label = parser.typeLabel(e.getKey());
                sb.append(label).append("每题").append(e.getValue()).append("分、 ");
            }
            sb.delete(sb.length() - 2, sb.length());
            sb.append("。\n");
        }

        // 数量清单
        if (!hasInstructionPrompt) {
            sb.append("【各题型数量】\n");
            sb.append(formatTypeCounts(typeCounts)).append("\n");
        }

        // 防同质化
        sb.append("【防同质化要求】\n");
        sb.append("1)每题覆盖不同子维度：定义/概念、参数/数据、应用场景、常见误区——不可重复同一个子维度\n");
        sb.append("2)相邻题目考查点必须不同——不能两题都问时间、都不能问同一个数值\n");
        if (alreadyCovered != null && !alreadyCovered.isEmpty()) {
            sb.append("3)以下考查点已出过，绝对不要重复：\n").append(alreadyCovered).append("\n");
        }

        // RAG 注入
        if (!ragParagraphs.isEmpty()) {
            sb.append("【参考资料】（共").append(ragParagraphs.size()).append("段）\n");
            for (int i = 0; i < ragParagraphs.size(); i++) {
                sb.append("第").append(i + 1).append("段：").append(ragParagraphs.get(i)).append("\n");
            }
            sb.append("要求：所有题目应当使用上述参考资料中的素材，均匀覆盖不同的知识点子维度，避免重复。\n");
        } else if (referenceMaterial != null && !referenceMaterial.isEmpty()
            && !referenceMaterial.equals("请根据知识点名称生成题目。")) {
            sb.append("参考资料：\n").append(referenceMaterial).append("\n");
            sb.append("请从不同角度出题，每次从资料不同位置选取素材。\n");
        }

        // 输出格式 + 示例
        if (!skipGenericFormat) {
            sb.append("\n【输出格式】请严格按上述数量生成，不要减少或增加任何题型。纯JSON数组，按题型分组（单选在前→多选→判断→填空在后）：\n[\n");
            if (typeCounts.getOrDefault("SINGLE_CHOICE", 0) > 0 || typeCounts.containsKey("MIXED"))
                sb.append("  {\"questionText\":\"…\",\"options\":[\"选项内容（不可含A.等前缀）\",\"选项内容\",\"选项内容\",\"选项内容\"],\"correctAnswer\":\"A\",\"questionType\":\"SINGLE_CHOICE\",\"explanation\":\"…\",\"quality\":85,\"difficultyLevel\":").append(refDifficulty).append("},\n");
            if (typeCounts.getOrDefault("MULTI_CHOICE", 0) > 0 || typeCounts.containsKey("MIXED"))
                sb.append("  {\"questionText\":\"…\",\"options\":[\"选项内容1\",\"选项内容2\",\"选项内容3\",\"选项内容4\"],\"correctAnswer\":\"AB\",\"questionType\":\"MULTI_CHOICE\",\"explanation\":\"…\",\"quality\":85},\n");
            if (typeCounts.getOrDefault("TRUE_FALSE", 0) > 0 || typeCounts.containsKey("MIXED"))
                sb.append("  {\"questionText\":\"…\",\"options\":[\"正确\",\"错误\"],\"correctAnswer\":\"A\",\"questionType\":\"TRUE_FALSE\",\"explanation\":\"…\"},\n");
            if (typeCounts.getOrDefault("FILL_IN", 0) > 0 || typeCounts.containsKey("MIXED"))
                sb.append("  {\"questionText\":\"…____…\",\"options\":[],\"correctAnswer\":\"答案\",\"questionType\":\"FILL_IN\",\"explanation\":\"…\"},\n");
            if (typeCounts.getOrDefault("ESSAY", 0) > 0)
                sb.append("  {\"questionText\":\"…\",\"options\":[],\"correctAnswer\":\"参考答案要点\",\"questionType\":\"SHORT_ANSWER\",\"explanation\":\"评分要点…\"}\n");
            if (typeCounts != null && typeCounts.getOrDefault("CLOZE", 0) > 0)
                sb.append("  {\"questionText\":\"[短文含10空·用__标注]\",\"questionType\":\"CLOZE\",\"blanks\":[{\"blankIndex\":1,\"options\":[\"A\",\"B\",\"C\",\"D\"],\"correctAnswer\":\"A\"}]},\n");
            if (typeCounts != null && (typeCounts.getOrDefault("READING_COMPREHENSION", 0) > 0 || typeCounts.getOrDefault("READING", 0) > 0))
                sb.append("  {\"questionText\":\"导读问题\",\"questionType\":\"READING_COMPREHENSION\",\"passage\":\"[阅读短文全文]\",\"questions\":[{\"questionText\":\"\",\"options\":[\"A\",\"B\",\"C\",\"D\"],\"correctAnswer\":\"A\"}]},\n");
            if (typeCounts != null && typeCounts.getOrDefault("CALCULATION", 0) > 0)
                sb.append("  {\"questionText\":\"\",\"questionType\":\"CALCULATION\",\"steps\":[\"步骤1\"],\"correctAnswer\":\"答案\"},\n");
            if (typeCounts != null && typeCounts.getOrDefault("PROOF", 0) > 0)
                sb.append("  {\"questionText\":\"\",\"questionType\":\"PROOF\",\"keyPoints\":[\"关键步骤\"],\"correctAnswer\":\"完整证明\"},\n");
            if (typeCounts != null && typeCounts.getOrDefault("COMPOSITION", 0) > 0)
                sb.append("  {\"questionText\":\"[作文材料]\",\"questionType\":\"COMPOSITION\",\"wordLimit\":600,\"scoringRubric\":\"评分标准\"}\n");
            sb.append("]\n");
            sb.append("【规则】1)单选/判断=A/B/C/D单字母 2)多选=AB/ABC无分隔符 3)判断options=[\"正确\",\"错误\"]，A=对B=错 4)填空options=[]答案文字 5)简答options=[]，correctAnswer写参考答案要点 6)禁止「以上都是」7)每题explanation必填 8)选项内容绝对不能包含A. B. C. D.等字母前缀，即写[\"光合作用\",\"呼吸作用\"]而非[\"A. 光合作用\",\"B. 呼吸作用\"] 9)不用```json``` 10)每道选择题严格只含4个选项(A/B/C/D)，不得超过4个，多选题答案仅限A-D 11)每题必填\"quality\":数字(0-100)，自评本题质量——题干清晰度+答案准确性+选项合理性综合分，低于60分请自行修正后再输出\n\n");
            // ── 答案自审校验 ──
            sb.append("【答案自审 — 生成前必须逐题执行以下检查】\n");
            sb.append("1) 回读每道题的题干+选项+答案，确认答案与题干逻辑一致\n");
            sb.append("2) 对于涉及年代/发明人/历史事实的题目，核实选项与题干的时间线是否匹配（如FORTRAN是1957年→第二代计算机时期，不是第三代）\n");
            sb.append("3) 多选题正确答案是否确实包含了所有正确选项（不能漏选也不能多选干扰项）\n");
            sb.append("4) 判断题的\"正确/错误\"是否与题干事实一致\n");
            sb.append("5) 如有不确定或存疑的题目，在explanation末尾追加（【待复核】），不要强行编造答案\n");
        }

        // 非重试且非批量模式时，加一句完整性提示，减少因数量不足触发的自动重试
        boolean isRetry = p.get("_retry") instanceof Number n && n.intValue() > 0;
        boolean isBatchMode = Boolean.TRUE.equals(p.get("_batchMode"));
        if (!isRetry && !isBatchMode) {
            sb.append("\n【完整性要求】如果输出题目数不足请求数，系统将要求你补充剩余题目，请尽量一次输出完整。\n");
        }

        // 重试时加入随机扰动
        Object retry = p.get("_retry");
        if (retry instanceof Number n && n.intValue() > 0) {
            sb.append(" 9)本次为第").append(n.intValue()).append("次重试，请务必严格遵循JSON格式，不要省略任何必填字段");
        }

        return sb.toString();
    }

    // ── 辅助方法 ──

    List<String> splitRagParagraphs(String material) {
        if (material == null || material.isBlank()) return List.of();
        String[] parts = material.split("(?=【)");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty() && trimmed.length() > 20) {
                result.add(trimmed.length() > 400 ? trimmed.substring(0, 400) : trimmed);
            }
        }
        return result;
    }

    String formatTypeCounts(Map<String, Integer> typeCounts) {
        StringBuilder sb = new StringBuilder();
        typeCounts.forEach((type, cnt) -> {
            if (cnt > 0) {
                String name = parser.typeLabel(type) + "×" + cnt;
                if (!sb.isEmpty()) sb.append("、");
                sb.append(name);
            }
        });
        if (sb.isEmpty()) sb.append("单选题×2、多选题×2");
        sb.append("（共").append(typeCounts.values().stream().mapToInt(Integer::intValue).sum()).append("题）");
        return sb.toString();
    }
}

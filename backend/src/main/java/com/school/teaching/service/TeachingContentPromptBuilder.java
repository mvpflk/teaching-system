package com.school.teaching.service;







import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;







/**



 * AI教学产出 Prompt 构建器 — 6种产出类型各有专用模板。



 * 纯工具类，无 Spring 依赖，按需实例化。



 *



 * 支持按学段适配国家课程标准：



 * 中职 → 《中等职业学校专业教学标准》(理实一体/做中学)



 * 高中 → 《普通高中课程标准(2017年版2020年修订)》(学科核心素养)



 * 初中 → 《义务教育课程标准(2022年版)》(核心素养导向)



 * 小学 → 《义务教育课程标准(2022年版)》(活动化/游戏化/生活化)



 */



public class TeachingContentPromptBuilder {
    private static final Logger log = LoggerFactory.getLogger(TeachingContentPromptBuilder.class);







    public static final String TYPE_TEACHING_DESIGN = "TEACHING_DESIGN";



    public static final String TYPE_KNOWLEDGE_CHECKLIST = "KNOWLEDGE_CHECKLIST";



    public static final String TYPE_PRACTICE_PLAN = "PRACTICE_PLAN";





    public static final String TYPE_COMPREHENSIVE_EXERCISES = "COMPREHENSIVE_EXERCISES";



    public static final String TYPE_CLASSROOM_QUESTIONS = "CLASSROOM_QUESTIONS";
    public static final String TYPE_EXAM_PAPER = "EXAM_PAPER";
    public static final String TYPE_DIAGNOSIS = "DIAGNOSIS";
    public static final String TYPE_KNOWLEDGE_PRACTICE = "KNOWLEDGE_PRACTICE";







    private static final String STAGE_ZZ = "中职";

    private static final com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();

    // 命题质量控制规则 — 追加到所有组卷/出题Prompt末尾
    private static final String QUALITY_RULES = "\n【命题质量控制——必须严格遵守】\n"
        + "1. 选项唯一性：同一道题的所有选项内容必须互不相同，去除字母前缀后不能出现重复选项。\n"
        + "2. 题干差异性：同一试卷中禁止生成多道题干高度相似的题目，每道题考察不同知识点。\n"
        + "3. 选项格式一致：同一题的所有选项必须使用统一的数值表示法。如十六进制题全部用十六进制数码(A/B/C/D)，禁止混用十进制数字(10/11/12/13)。\n"
        + "4. 答案字母精确：correctAnswer的字母必须严格对应选项数组的索引位置（A→options[0], B→options[1], C→options[2], D→options[3]）。\n"
        + "5. 数学验算：涉及计算的题目必须验算答案正确性后再输出。公式转换必须按标准步骤推导，禁止凭感觉填答案。\n"
        + "6. 生成前自检：输出每道题前，先确认选项有无重复、答案字母是否越界、数字格式是否一致，确认无误后再输出。\n"
        + "7. 每题自评质量分：在每道题的JSON中追加 \"_quality\":<分数> 字段，按0~100分自评本题的命题质量（题目清晰度、选项区分度、答案准确性、无歧义性），低于60分的题目表示存在质量问题。请如实评分，宁低勿高。\n";

    private static final String DIAGRAM_INSTRUCTIONS = "\n【几何图形支持——仅当题目涉及图形时使用】\n"
        + "如果题目涉及几何图形（三角形、四边形、圆、坐标系、函数图像等），请在题目JSON中附加 \"diagram\" 字段，遵循以下规范：\n"
        + "1. 点：用 x, y 坐标定义定点，或用 constraints 定义约束点\n"
        + "2. 线段/直线/射线：用点 ID 定义\n"
        + "3. 多边形：用顶点列表定义\n"
        + "4. 圆形：用圆心 + 半径 或 直径端点定义\n"
        + "5. 标注：用 label 标记点名和数值\n"
        + "6. 函数图：用 function-plot + expr 字符串定义（支持 x^2, sin(x) 等）\n"
        + "7. 角度：用 angle-arc 标记角度值，right-angle 标记直角\n"
        + "8. 边长：用 dimension 标注线段长度\n"
        + "9. 坐标系：用 grid + axis 定义网格\n"
        + "示例（三角形的 diagram）：\n"
        + "{\"elements\": [{\"type\":\"point\",\"id\":\"A\",\"x\":0,\"y\":0},{\"type\":\"point\",\"id\":\"B\",\"x\":6,\"y\":0},{\"type\":\"point\",\"id\":\"C\",\"constraints\":[{\"distance\":[\"A\",5]},{\"distance\":[\"B\",4]}]},{\"type\":\"segment\",\"from\":\"A\",\"to\":\"B\"},{\"type\":\"segment\",\"from\":\"A\",\"to\":\"C\"},{\"type\":\"segment\",\"from\":\"B\",\"to\":\"C\"},{\"type\":\"label\",\"at\":\"A\",\"text\":\"A\",\"offset\":[-8,12]},{\"type\":\"label\",\"at\":\"B\",\"text\":\"B\",\"offset\":[8,12]},{\"type\":\"label\",\"at\":\"C\",\"text\":\"C\",\"offset\":[0,-12]},{\"type\":\"dimension\",\"segment\":\"AB\",\"label\":\"6\",\"offset\":[0,18]}]}\n"
        + "注意：\n"
        + "- 只有涉及图形的题目才需要输出 diagram 字段\n"
        + "- 单纯的代数/概率/数列题不需要 diagram\n";


    private static final String STAGE_GZ = "高中";



    private static final String STAGE_CZ = "初中";



    private static final String STAGE_XX = "小学";

    private static final Set<String> CULTURE_SUBJECTS = new java.util.LinkedHashSet<>();
    static { for (String s : new String[]{"语文[职高]","数学[职高]","英语[职高]","语文[普高]","数学[普高]","英语[普高]","语文[初中]","数学[初中]","英语[初中]"}) CULTURE_SUBJECTS.add(s); }

    private static boolean isCultureSubject(String subject) {
        if (subject == null) return false;
        if (CULTURE_SUBJECTS.contains(subject)) return true;
        String bare = subject.replaceAll("\\[.*?\\]", "").trim();
        return bare.equals("语文") || bare.equals("数学") || bare.equals("英语");
    }

    /** V055: 计算机专业学科集合（subject_id=4/5/6/17） */
    private static final Set<String> COMPUTER_MAJOR_SUBJECTS = new java.util.LinkedHashSet<>();
    static {
        for (String s : new String[]{"信息技术应用基础","网络应用基础","办公应用基础","Access"})
            COMPUTER_MAJOR_SUBJECTS.add(s);
    }

    private static boolean isComputerMajor(String subject) {
        if (subject == null) return false;
        if (COMPUTER_MAJOR_SUBJECTS.contains(subject)) return true;
        String bare = subject.replaceAll("\\[.*?\\]", "").trim();
        return bare.equals("信息技术应用基础") || bare.equals("网络应用基础")
            || bare.equals("办公应用基础") || bare.equals("Access");
    }

    /** V059: 计算机专业课出题风格约束（基于2026年对口高考真题命题规律提取） */
    private static final String COMPUTER_MAJOR_STYLE_RULES = """
        【计算机专业课出题风格 — 对口升学五大命题手法（基于真题统计分析）】

        ## 一、场景嵌入（每3题中至少1题使用）
        禁止裸问概念。必须将知识点包装为真实操作/使用场景。
        ❌ 差："什么是输入掩码？"
        ✅ 好："在设计学生信息表时，需要确保电话号码字段只能输入11位数字，应该使用Access的什么功能？"
        句式模板：
        - "某用户/管理员/学生在……时，需要……，以下做法正确的是（）"
        - "在配置/安装/调试……的过程中，遇到了……问题，应如何解决？"
        - "下列关于……在实际应用中的描述，正确的是（）"

        ## 二、实操导向（操作题必须落实到菜单/快捷键/命令）
        操作类知识点不能只描述功能，必须指向具体的操作路径。
        句式模板：
        - "在XXX软件中，实现YYY功能的快捷键是（）"
        - "在XXX 2010中，为ZZZ添加数字签名，应在下列哪个选项卡中操作（）"
        - "使用netstat命令显示网络连接时，能显示连接和侦听接口的选项是（）"
        - "在Windows CMD中查看目录内容使用___命令，功能类似于Linux的ls"

        ## 三、跨工具/跨平台对比（辨析题首选手法）
        将不同系统、不同软件、不同版本的同一类功能进行对比。
        句式模板：
        - "以下关于XXX与YYY的区别，正确的是（）"
        - "XXX 2010中功能A位于【插入】选项卡，而在XXX 2016中该功能被调整到了（）"
        - "Windows中dir命令的功能类似于Linux的___命令"

        ## 四、故障排查链（综合题/难题专用）
        描述一个包含矛盾现象的故障场景，让学生反向推理原因。
        必须包含：现象描述 + 已尝试的操作 + 矛盾点 + 排除法选项
        句式模板：
        - "在……配置后，发现……异常，但此时……却正常。造成此现象最可能的原因是（）"
        - "某网络管理员……，检查了A和B均正常，但C仍然失败，最可能的原因是（）"

        ## 五、多知识点融合（杜绝孤立考点）
        每3题中至少1题串联2-3个相关知识点。
        示例（融合UEFI+4TB+GPT+分区表）：
        "某用户准备在一台新电脑(UEFI启动模式)上安装操作系统，需要初始化一块全新的4TB机械硬盘。
         关于分区表的选择，以下说法正确的是（）"

        ## 通用格式要求
        - 单选题严格4个选项，选项内容不含A/B/C/D字母前缀，干扰项与正确项同属一个概念域
        - 多选题4-5个选项，答案通常2-3个
        - 判断题使用"可以/不可以""只能/不只""一定/不一定"等辨析句式
        - 涉及软件操作的必须标注软件名称+版本号（如Word 2010、PowerPoint 2010）
        - 涉及命令的必须给出完整命令格式（含参数）
        - 所有题目以"（）。"结尾
        - 答案解析30-80字，指出正确选项的原因和其他选项的错误点
        - 难度配比：BASIC 30% / MEDIUM 50% / ADVANCED 20%

        """;







    /**



     * 构建完整 Prompt。返回 (prompt, maxTokens) 元组。



     * contentTypes 为前3种返回 Markdown 类；questionTypes 为后3种。



     */



    public static PromptResult build(String contentType, Map<String, Object> params) {



        String knowledgePoint = (String) params.getOrDefault("knowledgePoint", "");



        String taskName = (String) params.get("taskName");



        // displayName始终用当前选中节点的名称(选KP=KP名, 选任务=任务名)



        String displayName = knowledgePoint.isEmpty() && taskName != null ? taskName : knowledgePoint;



        String categoryPath = (String) params.getOrDefault("categoryPath", "");



        String subject = (String) params.getOrDefault("subject", "通用");



        String ragContext = (String) params.getOrDefault("ragContext", "");







        String stageHint = (String) params.getOrDefault("stageHint", "");



        if (stageHint == null || stageHint.isEmpty()) {



            stageHint = STAGE_ZZ; // 默认中职



        }







        String syllabusContext = (String) params.getOrDefault("syllabusContext", "");



        String style = (String) params.getOrDefault("style", "STANDARD");



        String focus = (String) params.getOrDefault("focus", "BALANCED");







        // 文化课组卷路由
        if (TYPE_EXAM_PAPER.equals(contentType)) {
            return buildExamPaper(stageHint, subject, displayName, categoryPath, ragContext, syllabusContext, params);
        }
        if (TYPE_DIAGNOSIS.equals(contentType)) {
            return buildDiagnosis(subject, params);
        }
        // 巩固材料：prompt 已由 ConsolidationMaterialService 经 buildConsolidationPrompt 存入 _instructionPrompt
        // （R116 拆分时 DIAGNOSIS 接进了路由，CONSOLIDATION_MATERIAL 被漏掉 → 之前每次生成必抛"不支持的内容类型"）
        if ("CONSOLIDATION_MATERIAL".equals(contentType)) {
            String ip = String.valueOf(params.getOrDefault("_instructionPrompt", ""));
            if (ip.isBlank()) throw new IllegalArgumentException("巩固材料缺少 _instructionPrompt");
            int mt = params.get("_maxTokens") instanceof Number n ? n.intValue() : 4000;
            return new PromptResult(ip, mt);
        }
        // 文化课教学设计——使用高中/义务教育标准模板
        if (isCultureSubject(subject) && TYPE_TEACHING_DESIGN.equals(contentType)) {
            return buildCultureTeachingDesign(stageHint, subject, displayName, categoryPath, ragContext, syllabusContext, params);
        }
        // 文化课禁止使用实训方案
        if (isCultureSubject(subject) && TYPE_PRACTICE_PLAN.equals(contentType)) {
            throw new IllegalArgumentException("文化课不支持实训方案，请使用综合练习或组卷功能");
        }

        return switch (contentType) {



            case TYPE_TEACHING_DESIGN -> buildTeachingDesign(stageHint, subject, displayName, categoryPath, ragContext, syllabusContext, style, focus);



            case TYPE_KNOWLEDGE_CHECKLIST -> buildKnowledgeChecklist(stageHint, subject, displayName, categoryPath, ragContext);



            case TYPE_PRACTICE_PLAN -> buildPracticePlan(stageHint, subject, displayName, categoryPath, ragContext);





            case TYPE_COMPREHENSIVE_EXERCISES -> buildComprehensiveExercises(stageHint, subject, displayName, categoryPath, ragContext, syllabusContext, params);



            case TYPE_CLASSROOM_QUESTIONS -> buildClassroomQuestions(stageHint, subject, displayName, categoryPath, ragContext, syllabusContext, params);



            case TYPE_KNOWLEDGE_PRACTICE -> buildKnowledgePractice(stageHint, subject, displayName, categoryPath, ragContext, syllabusContext, params);



            default -> throw new IllegalArgumentException("不支持的内容类型: " + contentType);



        };



    }







    // ═══════════════════════════════════════════════════════════════════



    // 教学设计 — 按学段适配国家课程标准



    // ═══════════════════════════════════════════════════════════════════







    private static PromptResult buildTeachingDesign(String stageHint, String subject,



            String kp, String path, String rag, String syllabus, String style, String focus) {



        String stageTemplate = switch (stageHint) {



            case STAGE_ZZ -> switch (style != null ? style : "STANDARD") {



                case "CONCISE" -> buildVocationalCompactTemplate(subject, kp, rag, syllabus, focus);



                case "DETAILED" -> buildVocationalTemplate(subject, kp, rag);



                default -> buildVocationalStandardTemplate(subject, kp, rag, syllabus, focus);



            };



            case STAGE_GZ -> buildSeniorHighTemplate(subject, kp, rag);



            case STAGE_CZ -> buildJuniorHighTemplate(subject, kp, rag);



            case STAGE_XX -> buildPrimaryTemplate(subject, kp, rag);



            default -> buildVocationalTemplate(subject, kp, rag);



        };



        int maxTokens = switch (style != null ? style : "STANDARD") {



            case "CONCISE" -> 4000;



            case "DETAILED" -> 8000;



            default -> 6000;



        };



        return new PromptResult(stageTemplate, maxTokens);



    }







    /** 中职专业课 — 依据《中等职业学校专业教学标准》：做中学、做中教、理实一体 */



    private static String buildVocationalTemplate(String subject, String kp, String rag) {



        return """



            你是中职%s教师。请根据提供的知识库内容，依据《中等职业学校专业教学标准》，撰写一份理实一体化的教学设计方案。







            【课程标准依据】



            - 教学理念：做中学、做中教，理论与实践一体化



            - 对接职业标准：课程内容对接职业岗位能力要求，融入职业技能大赛标准和1+X证书内容



            - 突出能力本位：以典型工作任务为载体，注重职业能力培养







            【输出结构要求】严格按以下Markdown格式输出：



            # 教学设计：《%s》



            ## 一、教学分析



            ### 1. 教材分析（选用教材、单元定位、前后关联）



            ### 2. 学情分析（学生专业/年级、已有知识基础、学习特点）



            ### 3. 职业能力分析（对接岗位能力、技能标准）



            ## 二、教学目标



            ### 1. 知识目标



            ### 2. 技能目标（可操作、可测量）



            ### 3. 素养目标（职业素养、工匠精神）



            ## 三、教学重点与难点



            ### 重点



            ### 难点及突破策略



            ## 四、教学资源与环境



            ### 教具/设备/软件/实训环境



            ## 五、教学过程（理实一体，总时长45分钟）



            ### 环节一：创设情境（3分钟）



            **教师活动**：展示真实职业场景案例或工作任务，激发学习动机



            **学生活动**：观察、思考、讨论情境中的问题



            **设计意图**：以岗位实际需求驱动学习，明确学习目标



            ### 环节二：任务引入（5分钟）



            **教师活动**：呈现典型工作任务，拆解任务步骤，讲解关键知识



            **学生活动**：理解任务要求，记录操作要点



            **设计意图**：以任务为导向，将知识学习融入工作任务



            ### 环节三：技能示范（10分钟）



            **教师活动**：演示操作流程，讲解关键技术点，强调安全规范



            **学生活动**：观察操作规范，模仿关键步骤



            **设计意图**：示范引领，直观呈现操作标准



            ### 环节四：分组实训（18分钟）



            **教师活动**：巡回指导，及时发现并纠正典型问题



            **学生活动**：分组操作练习，互帮互学，记录操作过程



            **设计意图**：在"做"中巩固知识和技能，培养团队协作



            ### 环节五：成果展示与评价（6分钟）



            **教师活动**：组织小组展示、自评互评，提炼总结



            **学生活动**：展示实训成果，参与评价，反思改进



            **设计意图**：多元评价促进反思，强化职业规范意识



            ### 环节六：课堂小结与作业（3分钟）



            **教师活动**：梳理知识技能要点，布置拓展任务



            **学生活动**：整理笔记，明确课后练习方向



            **设计意图**：巩固学习成果，延伸课堂学习



            ## 六、板书设计



            ## 七、教学评价设计



            | 评价维度 | 评价标准 | 权重 |



            |----------|----------|------|



            | 知识掌握 | 能准确复述核心概念 | 30%% |



            | 技能操作 | 操作规范、步骤完整 | 40%% |



            | 职业素养 | 安全规范、团队协作 | 30%% |



            ## 八、教学反思







            【要求】



            1. 所有内容必须基于提供的知识库材料，不可凭空编造



            2. 教学过程每个环节必须包含：**教师活动**、**学生活动**、**设计意图**三个子标题



            3. 技能操作步骤具体可操作，符合中职学生认知特点



            4. 体现"岗课赛证"融通理念（工作岗位→课程内容→技能大赛→职业证书）



            5. 知识库未覆盖的内容请标注[需补充]







            【参考资料 — 知识库内容】



            %s



            """.formatted(subject, kp, rag);



    }







    /** 中职精简实用版 — 去冗余、增实操字段(关键提问/巡视要点/常见错误预案)，~2000字 */



    private static String buildVocationalCompactTemplate(String subject, String kp, String rag, String syllabus, String focus) {



        String syllabusBlock = buildSyllabusBlock(syllabus);



        String focusHint = buildFocusHint(focus);



        return """



            你是中职%s教师。请根据提供的知识库内容，撰写一份可直接用于课堂教学的精炼教案。去掉空话套话，动词优先。



            %s



            %s







            【输出结构要求】严格按以下Markdown格式输出，控制在2000字以内：



            # %s







            ## 一、教学目标（3条，每条一句话）



            - 知识：...



            - 技能：...（可操作、可测量的动作描述）



            - 素养：...（职业规范/安全意识/团队协作，1句话）







            ## 二、重难点



            - 重点：...



            - 难点：... | 突破方法：...







            ## 三、教学准备



            | 类别 | 内容 |



            |------|------|



            | 软件/工具 | ... |



            | 教具/材料 | ... |



            | 学生预备知识 | ... |







            ## 四、教学过程（45分钟）



            ### 1. 情境导入（3分钟）



            - **教师活动**：展示{具体场景/案例}，提问：{1个具体问题}



            - **学生活动**：观察/思考/简短回答



            - **设计意图**：{1句话}







            ### 2. 新知讲授+示范（12分钟）



            - **教师活动**：{具体讲解步骤1→2→3}，示范操作要点{具体是什么}



            - **关键提问**：{1-2个检查理解的具体问题}



            - **学生活动**：听讲、记录、模仿操作



            - **巡视要点**：检查学生{具体操作/理解情况}



            - **设计意图**：{1句话}







            ### 3. 分组实训（20分钟）



            - **任务**：{具体可操作的实训任务}



            - **步骤**：1. ... 2. ... 3. ...



            - **教师活动**：巡回指导，重点关注{具体内容}



            - **巡视要点**：{2-3个具体检查项}



            - **学生活动**：分组操作、互帮互学



            - **常见错误预案**：{学生可能犯的1-2个错误及纠正方法}



            - **设计意图**：{1句话}







            ### 4. 成果展示与点评（7分钟）



            - **教师活动**：抽取{1-2组}展示，点评{具体维度}



            - **学生活动**：展示成果、互评



            - **设计意图**：{1句话}







            ### 5. 课堂小结（3分钟）



            - **教师活动**：用{1-2个问题}回顾本节要点



            - **学生活动**：回答问题、整理笔记







            ## 五、课后任务



            - 必做：...（约15分钟）



            - 选做：...（拓展提高）







            ## 六、教学反思（课后填写）



            （留空）







            【核心要求】



            1. 所有具体内容(案例/任务/步骤/问题)必须基于知识库材料



            2. "关键提问"写具体句子，不要写"提问相关概念"



            3. "巡视要点"写具体检查什么操作/什么理解



            4. "常见错误预案"写具体错误现象和纠正方法



            5. 每个环节"设计意图"仅1句话，说明对达成教学目标的作用



            6. 去掉独立板书设计和教学评价表格，融入教学过程



            7. 语言去修饰化，用展示/演示/操作/检查/点评等动词，少用形容词



            8. 知识库未覆盖的内容标注[需补充]







            【参考资料 — 知识库内容】



            %s



            """.formatted(subject, syllabusBlock, focusHint, kp, rag);



    }







    /** 中职标准版 — 结构与详细版相近但去多余章节，增加关键提问和巡视要点 */



    private static String buildVocationalStandardTemplate(String subject, String kp, String rag, String syllabus, String focus) {



        String syllabusBlock = buildSyllabusBlock(syllabus);



        String focusHint = buildFocusHint(focus);



        return """



            你是中职%s教师。请根据提供的知识库内容，依据《中等职业学校专业教学标准》，撰写一份理实一体化的教学设计方案。



            %s



            %s







            【课程标准依据】



            - 教学理念：做中学、做中教，理论与实践一体化



            - 对接职业标准：课程内容对接职业岗位能力要求







            【输出结构要求】严格按以下Markdown格式输出：



            # 教学设计：《%s》



            ## 一、教学分析



            ### 1. 教材分析（1句话：选用教材、单元定位）



            ### 2. 学情分析（1句话：学生专业/年级、已有基础）



            ### 3. 职业能力分析（1句话：对接岗位能力）



            ## 二、教学目标



            - 知识：...



            - 技能：...（可操作、可测量）



            - 素养：...



            ## 三、教学重点与难点



            - 重点：...



            - 难点：... | 突破策略：...



            ## 四、教学资源



            | 类别 | 内容 |



            |------|------|



            | 软件/工具 | ... |



            | 教具/设备 | ... |



            ## 五、教学过程（45分钟）



            ### 环节一：创设情境（3分钟）



            **教师活动**：展示真实职业场景案例，激发学习动机



            **关键提问**：{1个具体问题}



            **学生活动**：观察、思考



            **设计意图**：以岗位实际需求驱动学习



            ### 环节二：任务引入+示范（12分钟）



            **教师活动**：呈现典型工作任务，拆解步骤，演示关键操作



            **关键提问**：{1-2个检查理解的问题}



            **学生活动**：理解任务要求，观察操作规范



            **巡视要点**：检查学生{具体操作/理解情况}



            **设计意图**：任务导向，直观呈现操作标准



            ### 环节三：分组实训（20分钟）



            **教师活动**：巡回指导，及时纠正典型问题



            **学生活动**：分组操作练习，互帮互学



            **巡视要点**：{2个具体检查项}



            **常见错误预案**：{1-2个典型错误及纠正}



            **设计意图**：在"做"中巩固知识和技能



            ### 环节四：成果展示与评价（7分钟）



            **教师活动**：组织小组展示、自评互评，提炼总结



            **学生活动**：展示成果，参与评价



            **设计意图**：多元评价促进反思



            ### 环节五：课堂小结（3分钟）



            **教师活动**：梳理要点，布置拓展任务



            **学生活动**：整理笔记，明确方向



            ## 六、教学反思







            【要求】



            1. 所有内容必须基于知识库材料



            2. 每个环节必须包含教师活动、学生活动、设计意图，部分环节含关键提问和巡视要点



            3. "关键提问"写具体句子；"巡视要点"写具体检查内容



            4. 技能操作步骤具体可操作，符合中职学生认知特点



            5. 知识库未覆盖的内容标注[需补充]







            【参考资料 — 知识库内容】



            %s



            """.formatted(subject, syllabusBlock, focusHint, kp, rag);



    }







    /** 构建考纲注入块 */



    /** 从 params 解析题型数量，支持用户自定义覆盖默认值（R112修复） */
    @SuppressWarnings("unchecked")
    private static Map<String, Integer> resolveTypeCounts(Map<String, Object> params, Map<String, Integer> defaults) {
        Object tcObj = params.get("typeCounts");
        if (tcObj instanceof Map<?,?> m && !m.isEmpty()) {
            Map<String, Integer> result = new java.util.LinkedHashMap<>();
            m.forEach((k, v) -> {
                int cnt = v instanceof Number n ? n.intValue() : 0;
                if (cnt > 0) result.put(String.valueOf(k), cnt);
            });
            if (!result.isEmpty()) return result;
        }
        return new java.util.LinkedHashMap<>(defaults);
    }

    private static String buildSyllabusBlock(String syllabus) {



        if (syllabus == null || syllabus.isEmpty()) return "";



        return """



            【升学考试考纲对接】



            请参照以下考试大纲要求组织教学内容，确保覆盖考纲规定的应知应会知识点：



            %s



            """.formatted(syllabus);



    }







    /** 构建侧重提示 */



    private static String buildFocusHint(String focus) {



        if (focus == null || focus.isEmpty() || "BALANCED".equals(focus)) return "";



        if ("THEORY".equals(focus)) return "【侧重提示】本次教学偏重理论知识（应知），在概念讲授和原理解释上适当增加篇幅，实操部分精简。";



        if ("PRACTICE".equals(focus)) return "【侧重提示】本次教学偏重实操技能（应会），在实训步骤和操作细节上增加篇幅，理论部分精简。";



        return "";



    }







    private static String typeLabel(String key) {



        return switch (key) {



            case "SINGLE_CHOICE" -> "单选题";



            case "MULTI_CHOICE" -> "多选题";



            case "TRUE_FALSE" -> "判断题";



            case "FILL_IN" -> "填空题";



            case "ESSAY" -> "简答题";



            default -> key;



        };



    }







    /** 普通高中 — 依据《普通高中课程标准(2017年版2020年修订)》：学科核心素养导向 */



    private static String buildSeniorHighTemplate(String subject, String kp, String rag) {



        return """



            你是高中%s教师。请根据提供的知识库内容，依据《普通高中课程标准(2017年版2020年修订)》，撰写一份学科核心素养导向的教学设计方案。







            【课程标准依据】



            - 教学理念：以学科核心素养为导向，促进深度学习



            - 注重学科大概念统领下的单元教学设计



            - 强调真实情境中的问题解决和迁移应用







            【输出结构要求】严格按以下Markdown格式输出：



            # 教学设计：《%s》



            ## 一、单元/课时定位



            ### 所属大单元及课时在单元中的位置



            ## 二、学科核心素养目标



            ### 素养维度1（如：语言建构与运用/数学抽象/科学探究等）：



            ### 素养维度2：



            ### 素养维度3：



            ## 三、教学重点与难点



            ### 重点



            ### 难点及突破策略



            ## 四、教学准备



            ## 五、教学过程（总时长45分钟）



            ### 环节一：情境导入（5分钟）



            **教师活动**：创设真实、复杂的情境问题，引发认知冲突



            **学生活动**：感知情境，发现问题，产生探究欲望



            **设计意图**：以情境驱动学习，激活已有认知，明确探究方向



            ### 环节二：问题探究（12分钟）



            **教师活动**：呈现层层递进的核心问题链，引导自主探究、合作讨论



            **学生活动**：独立思考后小组讨论，尝试提出假设和解决方案



            **设计意图**：通过问题链引导深度学习，培养高阶思维



            ### 环节三：知识建构（13分钟）



            **教师活动**：点拨关键概念，归纳方法规律，搭建知识框架



            **学生活动**：总结规律，建构概念图/思维导图，形成结构化认知



            **设计意图**：帮助学生从具体到抽象，形成学科思维方式



            ### 环节四：迁移应用（10分钟）



            **教师活动**：提供变式练习或新情境问题，评价学生迁移能力



            **学生活动**：运用所学解决新问题，展示解题思路



            **设计意图**：检验知识迁移能力，促进深度学习



            ### 环节五：总结反思（5分钟）



            **教师活动**：引导学生反思学习过程，梳理核心素养达成情况



            **学生活动**：回顾学习过程，反思收获与不足



            **设计意图**：培养元认知能力，形成学科核心素养



            ## 六、板书设计



            ## 七、作业设计



            ### 基础作业（必做）



            ### 拓展作业（选做）



            ## 八、教学反思







            【要求】



            1. 所有内容必须基于提供的知识库材料



            2. 教学目标要对应具体学科核心素养维度，不可泛泛而谈



            3. 问题链设计要有梯度，从识记→理解→应用→分析→评价→创造



            4. 每个环节需包含：**教师活动**、**学生活动**、**设计意图**三个子标题



            5. 知识库未覆盖的内容请标注[需补充]







            【参考资料 — 知识库内容】



            %s



            """.formatted(subject, kp, rag);



    }







    /** 义务教育初中 — 依据《义务教育课程标准(2022年版)》：核心素养导向 */



    private static String buildJuniorHighTemplate(String subject, String kp, String rag) {



        return """



            你是初中%s教师。请根据提供的知识库内容，依据《义务教育课程标准(2022年版)》，撰写一份核心素养导向的教学设计方案。







            【课程标准依据】



            - 教学理念：以核心素养为导向，强化学科实践



            - 推进综合学习，注重知识关联和结构化



            - 注重"教—学—评"一致性







            【输出结构要求】严格按以下Markdown格式输出：



            # 教学设计：《%s》



            ## 一、内容要求与学业要求



            ### 课标内容要求



            ### 本课时对应的学业要求



            ## 二、核心素养目标



            ### 1. （对应课标核心素养维度，如科学观念/空间观念等）



            ### 2.



            ### 3.



            ## 三、教学重点与难点



            ### 重点



            ### 难点及突破策略



            ## 四、教学准备



            ## 五、教学过程（总时长45分钟）



            ### 环节一：创设情境（5分钟）



            **教师活动**：呈现贴近学生生活的真实情境或现象，提出驱动性问题



            **学生活动**：观察现象，激活已有经验，提出初步猜想



            **设计意图**：以生活情境引发认知需求，激发探究兴趣



            ### 环节二：探究新知（15分钟）



            **教师活动**：设计探究任务或实验活动，搭建学习支架，适时点拨引导



            **学生活动**：动手操作/小组讨论/实验观察，记录数据和发现



            **设计意图**：在实践探究中建构概念，培养学科思维



            ### 环节三：归纳建构（10分钟）



            **教师活动**：引导学生归纳规律，辨析易错概念，建立知识结构



            **学生活动**：总结核心概念，绘制知识结构图，修正错误认知



            **设计意图**：从具体经验上升为抽象认知，形成结构化知识



            ### 环节四：迁移应用（10分钟）



            **教师活动**：提供分层练习和综合性问题，关注不同水平学生表现



            **学生活动**：应用所学解决实际问题，尝试解释生活现象



            **设计意图**：在应用中巩固深化，体验学以致用



            ### 环节五：总结评价（5分钟）



            **教师活动**：组织学生总结交流，实施多元评价



            **学生活动**：回顾学习收获，自我评价和同伴评价



            **设计意图**：发展反思能力，落实"教—学—评"一致性



            ## 六、板书设计



            ## 七、作业设计



            ### 基础巩固类



            ### 实践探究类



            ## 八、教学反思







            【要求】



            1. 所有内容必须基于提供的知识库材料



            2. 核心素养目标要具体、可观测、可评价，对应课标要求



            3. 探究活动要有明确的步骤和观察点/记录要求



            4. 每个环节需包含：**教师活动**、**学生活动**、**设计意图**三个子标题



            5. 练习设计要分层，兼顾不同学习水平的学生



            6. 知识库未覆盖的内容请标注[需补充]







            【参考资料 — 知识库内容】



            %s



            """.formatted(subject, kp, rag);



    }







    /** 义务教育小学 — 依据《义务教育课程标准(2022年版)》：活动化、游戏化、生活化 */



    private static String buildPrimaryTemplate(String subject, String kp, String rag) {



        return """



            你是小学%s教师。请根据提供的知识库内容，依据《义务教育课程标准(2022年版)》，撰写一份符合小学生认知特点的教学设计方案。







            【课程标准依据】



            - 教学理念：活动化、游戏化、生活化



            - 注重幼小衔接，关注学生的年龄特点和认知规律



            - 强化学科实践，推进综合学习



            - "教学评"一体化，关注学习过程







            【输出结构要求】严格按以下Markdown格式输出：



            # 教学设计：《%s》



            ## 一、教材与学情分析



            ### 教学内容在单元中的位置



            ### 学情分析（学生已有经验、认知水平、兴趣特点）



            ## 二、教学目标



            ### 1. （具体、可观测、符合学生年龄特点）



            ### 2.



            ### 3.



            ## 三、教学重点与难点



            ### 重点



            ### 难点及趣味化突破策略



            ## 四、教学准备



            ### 教具、学具、多媒体资源



            ## 五、教学过程（总时长40分钟）



            ### 环节一：趣味导入（5分钟）



            **教师活动**：通过游戏、儿歌、故事、谜语等趣味方式引入新课



            **学生活动**：参与游戏/活动，在趣味体验中发现问题



            **设计意图**：通过游戏化活动激发学习兴趣，自然过渡到学习内容



            ### 环节二：活动探究（12分钟）



            **教师活动**：组织小组合作活动，提供直观教具/学具，引导发现规律



            **学生活动**：动手操作学具，小组讨论交流，尝试发现规律



            **设计意图**：在动手操作中感知和理解，培养合作学习习惯



            ### 环节三：归纳点拨（8分钟）



            **教师活动**：用简明生动的语言总结规律，借助儿歌/口诀帮助记忆



            **学生活动**：跟随老师归纳要点，朗读口诀，初步形成概念



            **设计意图**：将操作经验提炼为知识，用直观方式帮助理解记忆



            ### 环节四：趣味练习（10分钟）



            **教师活动**：设计闯关、竞赛、角色扮演等趣味练习活动



            **学生活动**：积极参与游戏化练习，在趣味中巩固知识



            **设计意图**：以游戏形式巩固新知，保持学习热情



            ### 环节五：分享收获（5分钟）



            **教师活动**：引导学生说出"今天我学到了什么"，给予积极评价



            **学生活动**：分享学习收获和快乐体验，展示学习成果



            **设计意图**：培养反思意识和表达能力，体验学习成就感



            ## 六、板书设计



            （图文并茂，适合小学生识记）



            ## 七、作业设计



            ### 基础练习



            ### 趣味实践（与生活联系的小任务）



            ## 八、教学反思







            【要求】



            1. 所有内容必须基于提供的知识库材料



            2. 语言要生动有趣，适合小学生理解水平



            3. 活动设计要具体可操作，明确活动规则和步骤



            4. 每个环节需包含：**教师活动**、**学生活动**、**设计意图**三个子标题



            5. 善于运用儿歌、口诀、游戏等形式帮助理解和记忆



            6. 知识库未覆盖的内容请标注[需补充]







            【参考资料 — 知识库内容】



            %s



            """.formatted(subject, kp, rag);



    }







    // ═══════════════════════════════════════════════════════════════════



    // 知识清单



    // ═══════════════════════════════════════════════════════════════════







    private static PromptResult buildKnowledgeChecklist(String stageHint, String subject, String kp, String path, String rag) {



        String prompt = """



            你是%s%s教师。请根据提供的知识库内容，整理一份结构化的知识清单。







            【输出结构要求】严格按以下Markdown格式输出：



            # 知识清单：《%s》



            ## 一、核心概念



            | 概念 | 定义 | 关键词 |



            |------|------|--------|



            | ... | ... | ... |



            ## 二、关键知识点



            ### 2.1 {知识点1}



            **定义**：... **要点**：... **示例**：...



            ### 2.2 {知识点2}



            ...



            ## 三、常见误区与注意事项



            ## 四、记忆口诀或知识关联图



            ## 五、自测题（3道选择题，含答案和解析）







            【要求】



            1. 核心概念以表格形式呈现，每个概念配定义和关键词



            2. 每个知识点需说明"为什么重要"



            3. 使用表格、列表提升可读性



            4. 自测题要有代表性，覆盖不同考点



            5. 基于知识库材料，不编造内容







            【参考资料 — 知识库内容】



            %s



            """.formatted(stageHint, subject, kp, rag);



        return new PromptResult(prompt, 8000);



    }







    // ═══════════════════════════════════════════════════════════════════



    // 实训方案



    // ═══════════════════════════════════════════════════════════════════







    private static PromptResult buildPracticePlan(String stageHint, String subject, String kp, String path, String rag) {



        String prompt = """



            你是%s%s教师。请根据提供的知识库内容，设计一份机房/实训室实训方案。







            【输出结构要求】严格按以下Markdown格式输出：



            # 实训方案：《%s》







            ## 一、实训目标



            （2-3句话，说明本次实训要达成的技能目标）







            ## 二、实训环境与工具



            | 类别 | 内容 |



            |------|------|



            | 软件/工具 | ... |



            | 硬件设备 | ... |



            | 素材/文件 | ... |







            ## 三、实训任务（设计3-5个递进任务，基础→进阶→综合）



            ### 任务1：{任务名称}（基础）



            **任务描述**：...



            **操作步骤**(Step-by-step)：1. ... 2. ... 3. ...



            **预期结果**：...



            **检查点**：...







            ### 任务2：{任务名称}（进阶）



            ...







            ### 任务N：{任务名称}（综合）



            ...







            ## 四、评分标准



            | 评分项 | 分值 | 评分标准 |



            |--------|------|----------|



            | 任务完成度 | 40 | 所有步骤按预期执行 |



            | 操作规范性 | 30 | 符合行业操作规范 |



            | 作品/成果质量 | 30 | 输出符合要求 |







            ## 五、常见问题与排错指南



            （列举2-3个学生最可能遇到的问题及解决方法）







            ## 六、任务元数据JSON（供系统自动发布为实训任务用）



            ```json



            {



              "title": "实训方案标题（≤30字）",



              "passCriteria": "学生通过本次实训的标准（1句话）",



              "steps": [



                {"seq":1, "name":"任务名称（≤15字）"},



                {"seq":2, "name":"任务名称"},



                {"seq":3, "name":"任务名称"}



              ],



              "scoringItems": [



                {"item":"评分项", "maxScore":40, "criteria":"评分标准"},



                {"item":"评分项", "maxScore":30, "criteria":"评分标准"},



                {"item":"评分项", "maxScore":30, "criteria":"评分标准"}



              ]



            }



            ```







            【要求】



            1. 步骤具体可操作，适合%s学生在机房/实训室独立完成



            2. 每个任务包含：目标、Step-by-step步骤、预期结果、检查点



            3. 包含截图或代码片段的位置标注[此处插入截图]



            4. 排错指南覆盖学生最常遇到的2-3个问题



            5. 末尾JSON元数据必须严格按格式输出，步骤数3-5个，评分项2-4个



            6. JSON中步骤的seq从1开始，name与Markdown中任务名称一致







            【参考资料 — 知识库内容】



            %s



            """.formatted(stageHint, subject, kp, stageHint, rag);



        return new PromptResult(prompt, 8000);



    }







    // ═══════════════════════════════════════════════════════════════════





    // =============================================================



    // 综合练习 — 跨知识点综合应用，模拟真实场景



    // =============================================================






    private static PromptResult buildComprehensiveExercises(String stageHint, String subject, String kp, String path, String rag, String syllabusContext, Map<String, Object> params) {



        // 综合练习：默认与前一致 — 单选×5、多选×3、判断×3、填空×3、简答×1



        Map<String, Integer> typeCounts = new java.util.LinkedHashMap<>();



        typeCounts.put("SINGLE_CHOICE", 5);



        typeCounts.put("MULTI_CHOICE", 3);



        typeCounts.put("TRUE_FALSE", 3);



        typeCounts.put("FILL_IN", 3);



        typeCounts.put("ESSAY", 1);



        // 允许调用方覆盖



        Object tcObj = params.get("typeCounts");



        if (tcObj instanceof Map<?,?> m && !m.isEmpty()) {



            typeCounts.clear();



            m.forEach((k, v) -> {



                int cnt = v instanceof Number n ? n.intValue() : 0;



                if (cnt > 0) typeCounts.put(String.valueOf(k), cnt);



            });



        }



        int totalCount = typeCounts.values().stream().mapToInt(Integer::intValue).sum();



        String typeConfig = typeCounts.entrySet().stream()



            .map(e -> typeLabel(e.getKey()) + "×" + e.getValue())



            .reduce((a, b) -> a + "、 " + b).orElse("混合题型");






        // 动态难度配比 — 根据前端用户选择的 tierFocus 决定
        String tierFocus = String.valueOf(params.getOrDefault("tierFocus", "BALANCED"));
        String tierHint = switch (tierFocus) {
            case "BASIC" -> "基础题(70%), 中等题(20%), 难题(10%)。";
            case "ADVANCED" -> "基础题(20%), 中等题(40%), 难题(40%)。";
            default -> "中等题(50%), 较难题(30%), 综合题(20%)。";
        };



        // 动态维度关注 — 根据前端用户选择的 knowledgeDim 决定
        String knowledgeDim = String.valueOf(params.getOrDefault("knowledgeDim", "BOTH"));
        String dimHint = switch (knowledgeDim) {
            case "THEORY" -> "侧重理论(应知)，实操(应会)为辅。";
            case "PRACTICE" -> "侧重实操(应会)，理论(应知)为辅。";
            default -> "兼顾理论(应知)与实操(应会)，侧重综合应用。";
        };



        String extraRule = "题目应模拟真实场景，综合运用多个关联知识点。至少1道简答题需要跨知识点分析，体现知识间的融会贯通，不可孤立考查单一知识点。";






        String subKpListJson = (String) params.get("_subKpList");



        String kpMappingHint = "";



        if (subKpListJson != null && !subKpListJson.isEmpty()) {



            try {



                java.util.List<java.util.Map<String, Object>> kpList =



                    new com.fasterxml.jackson.databind.ObjectMapper().readValue(subKpListJson,



                        new com.fasterxml.jackson.core.type.TypeReference<java.util.List<java.util.Map<String, Object>>>() {});



                StringBuilder sb = new StringBuilder("\n【知识点ID映射】（每题必须标注knowledgeNodeId，跨知识点题目可标注多个）\n");



                for (java.util.Map<String, Object> subKp : kpList) {



                    sb.append("- ").append(subKp.get("name")).append(" → knowledgeNodeId=").append(subKp.get("id")).append("\n");



                }



                kpMappingHint = sb.toString();



            } catch (Exception e) { log.warn("语文学科专用Prompt categoryPaths 构建失败: {}", e.getMessage()); }

        }






String syllabusBlock = buildSyllabusBlock(syllabusContext);
	String majorStyleBlock = isComputerMajor(subject) ? COMPUTER_MAJOR_STYLE_RULES : "";

String prompt = """



            你是%s%s教师。请根据知识库参考资料生成综合练习题。%s






            知识点：%s（%s）






            题型分布：%s   难度：3级（%s水平）



            难度配比：%s   出题维度：%s






            要求：



            1. %s



            2. 题目必须基于参考资料中的内容，不可编造



            3. 选项要有干扰性，不能一眼看出答案



            4. 每题需含答案解析(explanation字段)



            5. 输出JSON中每题增加"tier"字段(值为BASIC/MEDIUM/ADVANCED)和"knowledgeDim"字段(值为THEORY/PRACTICE)



            6. 如果参考资料包含多个子知识点，题目必须均匀分布到每个子知识点，不可集中在某一个上



            7. 模拟真实考试场景，至少1道简答题需要综合运用多个知识点进行分析







            %s
            【参考资料 — 知识库内容】



            %s



            %s



            """.formatted(stageHint, subject, syllabusBlock, kp,



                path != null && !path.isEmpty() ? path : subject,



                typeConfig, stageHint,



                tierHint, dimHint, extraRule, majorStyleBlock, rag, kpMappingHint) + QUALITY_RULES;

        // V055-fix: 注入真题参考样题（Few-shot 风格对齐）— 与 buildChineseExam 等保持一致
        String refQ = (String) params.get("_referenceQuestions");
        if (refQ != null && !refQ.isEmpty()) {
            prompt += "\n\n【参考样题（仅做风格参考，不得照抄）】\n" + refQ;
        }

        Map<String, Object> questionParams = new java.util.LinkedHashMap<>();



        questionParams.put("knowledgePoint", kp);



        questionParams.put("categoryPath", path);



        questionParams.put("subject", subject);



        questionParams.put("stageHint", stageHint);



        questionParams.put("difficultyLevel", 3);



        questionParams.put("typeConfig", typeConfig);



        questionParams.put("typeCounts", typeCounts);



        questionParams.put("tierFocus", tierFocus);



        questionParams.put("knowledgeDim", knowledgeDim);



        questionParams.put("comprehensive", true);



        questionParams.put("referenceMaterial", rag);



        // 考纲上下文注入 questionParams，使 DeepSeekGateway.buildPrompt 也能使用
        if (syllabusContext != null && !syllabusContext.isEmpty()) {
            questionParams.put("syllabusContext", syllabusContext);
        }

        // 分值预设传递 — 用户设置的每题型分值
        Object scoreObj = params.get("scorePresets");
        if (scoreObj instanceof Map<?,?> sm && !sm.isEmpty()) {
            questionParams.put("scorePresets", sm);
        }

        // 将综合练习专用 Prompt 文本注入，供 DeepSeekGateway 优先使用
        questionParams.put("_instructionPrompt", prompt);

        // 构建结构化知识点映射（供 DeepSeekPromptBuilder 使用）
        if (subKpListJson != null && !subKpListJson.isEmpty()) {
            try {
                List<Map<String, Object>> kps = om.readValue(subKpListJson,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
                List<Map<String, Object>> cpList = new java.util.ArrayList<>();
                for (Map<String, Object> kpItem : kps) {
                    Object kpId = kpItem.get("id");
                    Object kpName = kpItem.get("name");
                    if (kpId != null && kpName != null) {
                        cpList.add(Map.of("nodeId", kpId, "path",
                            (path != null && !path.isEmpty() ? path + " > " : "") + kpName));
                    }
                }
                if (!cpList.isEmpty()) {
                    questionParams.put("categoryPaths", cpList);
                }
            } catch (Exception e) { log.warn("categoryPaths 解析失败: {}", e.getMessage()); }
        }

        Object nodeId = params.get("nodeId");



        if (nodeId != null) questionParams.put("categoryId", nodeId);



        else if (params.get("categoryId") != null) questionParams.put("categoryId", params.get("categoryId"));



        questionParams.put("_skipGenericFormat", Boolean.TRUE);
        questionParams.put("candidateCount", Math.max(totalCount, 4));

        // 传播 _subKpList 供 saveQuestions 白名单校验
        if (subKpListJson != null && !subKpListJson.isEmpty()) {
            questionParams.put("_subKpList", subKpListJson);
        }


        questionParams.put("_maxTokens", questionTokenBudget(totalCount));






        return new PromptResult(prompt, Math.min(500 + totalCount * 50, 1500), questionParams);



    }
    // 课堂提问



    // ═══════════════════════════════════════════════════════════════════







    private static PromptResult buildClassroomQuestions(String stageHint, String subject, String kp, String path, String rag, String syllabusContext, Map<String, Object> params) {


	String syllabusBlock = buildSyllabusBlock(syllabusContext);


        // 读取子知识点列表并构建 ID 映射
        String subKpListJson = (String) params.get("_subKpList");
        String kpMappingHint = "";
        List<Map<String, Object>> cpList = null;
        if (subKpListJson != null && !subKpListJson.isEmpty()) {
            try {
                List<Map<String, Object>> kps = om.readValue(subKpListJson,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
                cpList = new java.util.ArrayList<>();
                StringBuilder sb2 = new StringBuilder("\n【知识点ID映射】每题必须标注knowledgeNodeId：\n");
                for (Map<String, Object> kpItem : kps) {
                    sb2.append("- ").append(kpItem.get("name")).append(" → knowledgeNodeId=").append(kpItem.get("id")).append("\n");
                    cpList.add(Map.of("nodeId", kpItem.get("id"), "path", kpItem.get("name")));
                }
                kpMappingHint = sb2.toString();
            } catch (Exception e) { log.warn("静默降级异常: {}", e.getMessage()); }
        }

        String teachingGoal = params.get("teachingGoal") instanceof String s ? s : "";
        Integer difficulty = params.get("difficulty") instanceof Number n ? n.intValue() : 2;
        String questionType = params.get("questionType") instanceof String s ? s : "SHORT_ANSWER";

        String goalHint = "";
        if (!teachingGoal.isEmpty()) {
            goalHint = "\n\n【教学目标】" + teachingGoal + "\n\n";
            switch (teachingGoal) {
                case "REVIEW" -> goalHint += "重点考察已学知识点的记忆和理解，题目难度适中\n";
                case "PREVIEW" -> goalHint += "引导学生思考新内容，不要涉及未学细节，注重概念引入\n";
                case "CONSOLIDATE" -> goalHint += "结合课堂实例，强化核心概念应用，注重知识迁移\n";
                case "EXTEND" -> goalHint += "联系实际场景，培养综合应用能力，题目可以有一定挑战性\n";
                case "DIAGNOSE" -> goalHint += "针对易错点设计，快速了解学生掌握情况，注重基础概念\n";
                default -> goalHint += "根据教学目标灵活调整题目难度和类型\n";
            }
        }

        String difficultyHint = "";
        switch (difficulty) {
            case 1 -> difficultyHint = "\n【难度级别】基础 — 侧重记忆和简单理解，适合入门\n";
            case 2 -> difficultyHint = "\n【难度级别】进阶 — 侧重理解和应用，适合课堂巩固\n";
            case 3 -> difficultyHint = "\n【难度级别】挑战 — 侧重综合应用和拓展，适合学有余力的学生\n";
            default -> difficultyHint = "\n【难度级别】进阶 — 侧重理解和应用\n";
        }

        String typeHint = "";
        if (!questionType.equals("SHORT_ANSWER")) {
            switch (questionType) {
                case "MULTIPLE_CHOICE" -> typeHint = "\n【题目类型】单选题 — 每题提供3-4个选项，只有一个正确答案\n";
                case "TRUE_FALSE" -> typeHint = "\n【题目类型】判断题 — 每题只有对/错两个选项\n";
                case "DISCUSSION" -> typeHint = "\n【题目类型】讨论题 — 适合课堂小组讨论，答案开放\n";
                default -> typeHint = "\n【题目类型】简答题 — 适合口头回答，答案简洁明了\n";
            }
        }

        String prompt = """

            你是%s%s教师。请根据知识库参考资料，生成适合课堂口头提问的问题。%s
%s
%s
%s

            知识点：%s


            请按以下4类问题生成，共8-10道：


            【回忆型(1-2道)】检查知识记忆，如"什么是XX？""XX由哪些部分组成？"


            【理解型(2-3道)】检查深层理解，如"为什么说XX是可靠的？""XX和YY有什么区别？"


            【应用型(2-3道)】联系实际操作，如"如果XX不通，你第一步查什么？"


            【拓展型(1-2道)】开放思考，如"XX相比YY解决了哪些YY无法解决的问题？"


            输出JSON数组，每题结构：


            {


              "questionText":"问题文本",


              "questionType":"%s",


              "correctAnswer":"参考答案(简洁)或A/B",


              "explanation":"简短解析",


              "intent":"提问意图(教师用，如'检查IP概念理解')",


              "category":"RECALL/COMPREHEND/APPLY/EXTEND"


            }


            要求：


            - 所有问题基于参考资料，不编造


            - 理解型和应用型问题要有思维深度，不要只问"对不对""是不是"


            - 参考答案要准确


            - 提问意图(intent)写清楚此问想检验什么


            【参考资料 — 知识库内容】


            %s


            """.formatted(stageHint, subject, syllabusBlock, goalHint, difficultyHint, typeHint, kp, questionType, rag);

        // V055-fix: 注入真题参考样题（Few-shot 风格对齐）
        String refQ = (String) params.get("_referenceQuestions");
        if (refQ != null && !refQ.isEmpty()) {
            prompt += "\n\n【参考样题（仅做风格参考，不得照抄）】\n" + refQ;
        }

        Map<String, Object> questionParams = new java.util.LinkedHashMap<>();



        questionParams.put("knowledgePoint", kp);



        questionParams.put("categoryPath", path);



        questionParams.put("subject", subject);



        questionParams.put("stageHint", stageHint);



        questionParams.put("difficultyLevel", 1);



        questionParams.put("candidateCount", 10);



        questionParams.put("typeConfig", "SHORT_ANSWER为主, TRUE_FALSE为辅");



        questionParams.put("referenceMaterial", rag);



        questionParams.put("classroomQuestions", true);

        // 传播 _subKpList 供 saveQuestions 白名单校验
        if (subKpListJson != null && !subKpListJson.isEmpty()) {
            questionParams.put("_subKpList", subKpListJson);
        }
        // 构建结构化知识点映射
        if (cpList != null && !cpList.isEmpty()) {
            questionParams.put("categoryPaths", cpList);
        }

        questionParams.put("_maxTokens", 2500);



        // 考纲上下文注入
        if (syllabusContext != null && !syllabusContext.isEmpty()) {
            questionParams.put("syllabusContext", syllabusContext);
        }

        // 将课堂提问专用 Prompt 文本注入，供 DeepSeekGateway 使用
        questionParams.put("_instructionPrompt", prompt);







        return new PromptResult(prompt, 2500, questionParams);



    }







    /** Prompt 构建结果：prompt 文本 + maxTokens + 附加参数(题目类型用) */



    // ==================== AI 智能组卷 Prompt ====================

    /** 三级回退读取考纲结构化数据<br>
     *  L1: params["syllabusMeta"] JSON key lookup<br>
     *  L3: hardcoded default + log.warn<br> */
    private static String readSyllabusData(Map<String, Object> params, String key, String defaultVal) {
        Object metaObj = params.get("syllabusMeta");
        if (metaObj instanceof String metaStr && !metaStr.isEmpty() && !"null".equals(metaStr)) {
            try {
                com.fasterxml.jackson.databind.JsonNode meta = new com.fasterxml.jackson.databind.ObjectMapper().readTree(metaStr);
                if (meta.has(key) && !meta.get(key).isNull()) {
                    String val = meta.get(key).asText();
                    if (!val.isEmpty()) return val;
                }
            } catch (Exception e) { /* L1 parse fail → L3 fallback */ }
        }
        return defaultVal;
    }

    private static PromptResult buildExamPaper(String stageHint, String subject, String kp,
            String path, String rag, String syllabus, Map<String, Object> params) {
        String mode = String.valueOf(params.getOrDefault("examMode", "exam"));
        boolean isTraining = "training".equals(mode);
        String bareSubject = (subject != null ? subject.replaceAll("\\[.*?\\]", "").trim() : "");
        if (bareSubject.contains("语文")) return isTraining ? buildChineseTraining(stageHint, subject, kp, path, rag, syllabus, params) : buildChineseExam(stageHint, subject, kp, path, rag, syllabus, params);
        else if (bareSubject.contains("数学")) return isTraining ? buildMathTraining(stageHint, subject, kp, path, rag, syllabus, params) : buildMathExam(stageHint, subject, kp, path, rag, syllabus, params);
        else if (bareSubject.contains("英语")) return isTraining ? buildEnglishTraining(stageHint, subject, kp, path, rag, syllabus, params) : buildEnglishExam(stageHint, subject, kp, path, rag, syllabus, params);
        return buildComprehensiveExercises(stageHint, subject, kp, path, rag, syllabus, params);
    }

    // 语文仿真组卷 — 对标《四川省对口招生职业技能考试语文大纲》
    // 试卷结构: 第I卷客观题30分 + 第II卷主观题120分 = 150分/150分钟
    // 第I卷: 基础知识5题×3分(字音字形/词语/病句/修辞/文常) + 社科文阅读3-4题×3分 + 文言文阅读2-3题×3分
    // 第II卷: 名句默写约6分 + 文言文翻译约6-10分 + 古诗词鉴赏约8-12分 + 现代文简答约15-20分 + 语言运用约6-10分 + 应用文约10-15分 + 大作文50-60分
    private static PromptResult buildChineseExam(String stageHint, String subject, String kp,
            String path, String rag, String syllabus, Map<String, Object> params) {
        String reciteList = readSyllabusData(params, "reciteList",
            "静女/采薇/寡人之于国也/劝学/师说/将进酒/琵琶行/念奴娇·赤壁怀古");
        String themes = readSyllabusData(params, "compositionThemes",
            "工匠精神/积累坚持/责任担当/创新突破");
        // 动态题型数量（R112修复：原硬编码数量与用户设置不一致）
        Map<String, Integer> tc = resolveTypeCounts(params,
            Map.of("SINGLE_CHOICE",10,"FILL_IN",3,"SHORT_ANSWER",4,"COMPOSITION",2));
        int sc = tc.getOrDefault("SINGLE_CHOICE", 10);
        int fi = tc.getOrDefault("FILL_IN", 3);
        int sa = tc.getOrDefault("SHORT_ANSWER", 4);
        int cp = tc.getOrDefault("COMPOSITION", 2);
        // 单选题子类按比例分配
        int scBase = Math.max(1, sc * 5 / 10); // 基础知识 ~50%
        int scSocial = Math.max(1, sc * 3 / 10); // 社科文 ~30%
        int scClassical = sc - scBase - scSocial; // 文言文 剩余
        if (scClassical < 1) { scBase = Math.max(1, sc - 2); scSocial = 1; scClassical = 1; }
        int saTrans = Math.max(1, sa / 2); // 翻译
        int saPoetry = Math.max(1, (sa - saTrans) / 2); // 古诗词
        int saModern = sa - saTrans - saPoetry; // 现代文
        if (saModern < 1) { saTrans = Math.max(1, sa - 2); saPoetry = 1; saModern = 1; }

        StringBuilder sb = new StringBuilder();
        sb.append("你是四川省对口升学考试语文命题教师，依据《中等职业学校语文课程标准》和《四川省对口招生职业技能考试语文大纲》命题。\n\n");
        sb.append("【试卷标准】满分150分，考试时间150分钟。第I卷客观题30分+第II卷主观题120分。\n");
        sb.append("容易题约40%、较易题约30%、中等题约20%、较难题约10%。\n\n");
        sb.append("【第I卷·客观题 共30分】（全部为选择题，每题3分，共").append(sc).append("题）\n");
        sb.append("一、基础知识(").append(scBase).append("题×3分=").append(scBase*3).append("分)：字音字形/词语运用/病句辨析/修辞手法/文学常识，各1题。\n");
        sb.append("二、社科文阅读(").append(scSocial).append("题×3分=").append(scSocial*3).append("分)：信息筛选/内容理解/推断判断。\n");
        sb.append("三、文言文阅读(").append(scClassical).append("题×3分=").append(scClassical*3).append("分)：实词释义/虚词用法各1题。\n\n");
        sb.append("【第II卷·主观题 共120分】\n");
        sb.append("四、名句默写(约6分，").append(fi).append("题)：背诵篇目——").append(reciteList).append("\n");
        sb.append("五、文言文翻译(约8分，").append(saTrans).append("题)：将浅易文言文句子译为现代汉语。\n");
        sb.append("六、古诗词鉴赏(约10分，").append(saPoetry).append("题)：语言特点/思想感情/表现手法。\n");
        sb.append("七、现代文阅读·简答题(约18分，").append(saModern).append("题)：含义理解/思路梳理/要点概括/写法分析。\n");
        sb.append("八、语言运用(约8分，1-2题)：仿写/句式变换/修辞辨析与运用。\n");
        sb.append("九、应用文写作(约10分)：通知/启事/书信/条据/计划/总结/求职信/应聘书——格式规范，正文约100字。\n");
        sb.append("十、材料作文(约60分)：议论文≥600字，切合题意/中心明确/内容充实/结构完整。主题方向——").append(themes).append("。\n\n");
        sb.append("【输出要求】\n");
        sb.append("1. 纯JSON数组格式，不要Markdown代码块包裹，不要输出任何非JSON文本。\n");
        sb.append("2. 题干的questionText用中文书写，选项options用中文书写（不可含A.B.C.D.字母前缀）。\n");
        sb.append("3. questionType字段用英文：SINGLE_CHOICE(选择,4选项)/FILL_IN(名句默写/语言运用)/SHORT_ANSWER(古诗词鉴赏/现代文简答/翻译,含expectedPoints)/COMPOSITION(作文,含wordLimit和scoringRubric)。\n");
        sb.append("4. 禁止在题干、选项、答案等用户可见文本中出现SINGLE_CHOICE/COMPOSITION/FILL_IN/SHORT_ANSWER等题型英文代码。\n");
        sb.append("5. 每题含knowledgeNodeId和difficultyLevel(1-5)。\n");
        sb.append("6. 题型分布——SINGLE_CHOICE共").append(sc).append("题(基础知识").append(scBase).append("+社科文").append(scSocial).append("+文言文").append(scClassical)
            .append("), FILL_IN共").append(fi).append("题(名句默写), SHORT_ANSWER共").append(sa).append("题(文言文翻译").append(saTrans).append("+古诗词鉴赏").append(saPoetry)
            .append("+现代文简答").append(saModern).append("), COMPOSITION共").append(cp).append("题(应用文1+大作文").append(cp > 1 ? cp-1 : 1).append(")。");
        sb.append(QUALITY_RULES);
        String ref = (String) params.get("_referenceQuestions");
        if (ref != null && !ref.isEmpty()) {
            sb.append("\n【参考样题（仅做风格参考，不得照抄）】\n").append(ref).append("\n");
        }
        String prompt = sb.toString();
        Map<String,Object> qp = new java.util.LinkedHashMap<>();
        qp.put("_instructionPrompt", prompt); qp.put("comprehensive", false);
        qp.put("_maxTokens", questionTokenBudget(tc.values().stream().mapToInt(Integer::intValue).sum()));
        qp.put("temperature", 0.6);
        qp.put("typeCounts", tc);
        // 构建结构化知识点映射
        String subKpListJson = (String) params.get("_subKpList");
        if (subKpListJson != null && !subKpListJson.isEmpty()) {
            try {
                List<Map<String, Object>> kps = om.readValue(subKpListJson,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
                List<Map<String, Object>> cpList = new java.util.ArrayList<>();
                for (Map<String, Object> kpItem : kps) {
                    cpList.add(Map.of("nodeId", kpItem.get("id"), "path", kpItem.get("name")));
                }
                if (!cpList.isEmpty()) {
                    qp.put("categoryPaths", cpList);
                }
            } catch (Exception e) { log.warn("静默降级异常: {}", e.getMessage()); }
        }
        qp.put("_skipGenericFormat", Boolean.TRUE);
        return new PromptResult(prompt, 3000, qp);
    }

    private static PromptResult buildMathExam(String stageHint, String subject, String kp,
            String path, String rag, String syllabus, Map<String, Object> params) {
        String answerOrder = readSyllabusData(params, "answerOrder",
            "函数→三角→数列→解析几何→立几→概率/导数应用");
        String scoreDist = readSyllabusData(params, "scoreDistribution",
            "集合约5%/不等式约7%/函数约12%/指对数约5%/三角约14%/数列约9%/向量约7%/立几约10%/解析几何约18%/概率约7%/导数选考约5%");
        // 动态题型数量（R112修复）
        Map<String, Integer> tc = resolveTypeCounts(params,
            Map.of("SINGLE_CHOICE",15,"FILL_IN",5,"CALCULATION",6));
        int sc = tc.getOrDefault("SINGLE_CHOICE", 15);
        int fi = tc.getOrDefault("FILL_IN", 5);
        int calc = tc.getOrDefault("CALCULATION", 6);
        StringBuilder sb = new StringBuilder();
        sb.append("你是四川省对口升学考试数学命题教师，依据《中等职业学校数学课程标准》和《四川省对口招生职业技能考试数学大纲》命题。\n\n");
        sb.append("【试卷标准】满分150分，考试时间120分钟。容易题约40%/较易题约30%/中等题约20%/较难题约10%。\n\n");
        sb.append("【题型与分值】\n");
        sb.append("一、选择题(").append(sc).append("题×4分=").append(sc*4).append("分)：4选项单选题，考查基础概念和公式直接应用。\n");
        sb.append("二、填空题(").append(fi).append("题×4分=").append(fi*4).append("分)：直接填写数值或表达式结果，结果为整数或简分数。\n");
        sb.append("三、解答题(").append(calc).append("题共").append(150 - sc*4 - fi*4).append("分)：每题10-14分不等，须写出完整步骤。每题分解为2-3小问(xx题第1问→第2问→第3问)。\n");
        sb.append("  解答题顺序：").append(answerOrder).append("。\n\n");
        sb.append("【内容分布】").append(scoreDist).append("。生活中的数学应用类题目约占15%。\n\n");
        sb.append("【输出】纯JSON数组。可用题型：\n");
        sb.append("SINGLE_CHOICE(选择,4选项,每题difficultyLevel1-5)\n");
        sb.append("FILL_IN(填空,答案含数值或表达式)\n");
        sb.append("CALCULATION(解答题,含steps数组各步骤得分点,含subQuestions数组每题对应小问)\n");
        sb.append("【重要】选择题共").append(sc).append("题、填空题共").append(fi).append("题、解答题共").append(calc)
            .append("题。每题含knowledgeNodeId和difficultyLevel。干扰选项须设为该专题特有常见计算错误结果。");
        sb.append(QUALITY_RULES);
        sb.append(DIAGRAM_INSTRUCTIONS);
        String ref = (String) params.get("_referenceQuestions");
        if (ref != null && !ref.isEmpty()) {
            sb.append("\n【参考样题（仅做风格参考，不得照抄）】\n").append(ref).append("\n");
        }
        String prompt = sb.toString();
        Map<String,Object> qp = new java.util.LinkedHashMap<>();
        qp.put("_instructionPrompt", prompt); qp.put("comprehensive", false);
        qp.put("_maxTokens", questionTokenBudget(tc.values().stream().mapToInt(Integer::intValue).sum()));
        qp.put("temperature", 0.6);
        qp.put("typeCounts", tc);
        // 构建结构化知识点映射
        String subKpListJson = (String) params.get("_subKpList");
        if (subKpListJson != null && !subKpListJson.isEmpty()) {
            try {
                List<Map<String, Object>> kps = om.readValue(subKpListJson,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
                List<Map<String, Object>> cpList = new java.util.ArrayList<>();
                for (Map<String, Object> kpItem : kps) {
                    cpList.add(Map.of("nodeId", kpItem.get("id"), "path", kpItem.get("name")));
                }
                if (!cpList.isEmpty()) {
                    qp.put("categoryPaths", cpList);
                }
            } catch (Exception e) { log.warn("静默降级异常: {}", e.getMessage()); }
        }
        qp.put("_skipGenericFormat", Boolean.TRUE);
        return new PromptResult(prompt, 2000, qp);
    }

    private static PromptResult buildEnglishExam(String stageHint, String subject, String kp,
            String path, String rag, String syllabus, Map<String, Object> params) {
        String choiceOrder = readSyllabusData(params, "singleChoiceOrder",
            "情景交际→冠词→代词→介词→主谓一致→动词短语→连词→反意/感叹→动词辨析→情态→非谓语→名从→定从→虚拟/倒装→综合");
        String readTopics = readSyllabusData(params, "readingTopics",
            "校园→社会→科技→人物→实用");
        String langNote = readSyllabusData(params, "languageAppNote",
            "多模态语篇(标识牌/广告/票务/地图)，通过文字描述场景即可");
        // 动态题型数量（R112修复）
        Map<String, Integer> tc = resolveTypeCounts(params,
            Map.of("SINGLE_CHOICE",30,"READING_COMPREHENSION",5,"FILL_IN",2,"SHORT_ANSWER",3,"COMPOSITION",1));
        int sc = tc.getOrDefault("SINGLE_CHOICE", 30);
        int read = tc.getOrDefault("READING_COMPREHENSION", 5);
        int fi = tc.getOrDefault("FILL_IN", 2);
        int sa = tc.getOrDefault("SHORT_ANSWER", 3);
        int cp = tc.getOrDefault("COMPOSITION", 1);
        // 单选子类按比例分配
        int scChoice = Math.max(1, sc / 2); // 单项选择 ~50%
        int scLang = Math.max(1, sc / 3); // 语言应用 ~33%
        int scDialog = sc - scChoice - scLang; // 补全对话 剩余
        if (scDialog < 1) { scChoice = Math.max(1, sc - 2); scLang = 1; scDialog = 1; }

        StringBuilder sb = new StringBuilder();
        sb.append("你是四川省对口升学考试英语命题教师，依据《中等职业学校英语课程标准》和《四川省对口招生职业技能考试英语大纲》命题。\n\n");
        sb.append("【试卷标准】满分100分，考试时间120分钟。容易题约40%/较易题约30%/中等题约20%/较难题约10%。\n\n");
        sb.append("【第I卷·客观题 共70分】\n");
        sb.append("一、单项选择(").append(scChoice).append("题×1分=").append(scChoice).append("分)：考点排布顺序——").append(choiceOrder).append("。\n");
        sb.append("二、语言应用(").append(scLang).append("题×1.5分=").append(Math.round(scLang*1.5)).append("分)：根据文字场景描述（如路标/广告/通知/日程表/图表等）选择正确答案。").append(langNote).append("。注意：文本描述场景即可，不依赖真实图片。\n");
        sb.append("三、阅读理解(").append(read).append("篇×4题×2分=").append(read*8).append("分)：").append(read).append("篇文章，每篇设4道选择题。话题顺序——").append(readTopics).append("。体裁：记叙文/说明文/应用文/议论文/新闻报道各一篇。\n\n");
        sb.append("【第II卷·主观题 共30分】\n");
        sb.append("四、补全对话(").append(scDialog).append("题×1分=").append(scDialog).append("分)：给出一段不完整对话，从选项中选择正确的句子填入空白处（SINGLE_CHOICE题型）。\n");
        sb.append("五、短文改错(").append(sa + fi).append("题×2分=").append((sa+fi)*2).append("分)：一篇约100词短文，标有5处错误（动词时态/名词单复数/冠词/介词/主谓一致等），逐处写出正确形式（SHORT_ANSWER题型）。\n");
        sb.append("六、书面表达(").append(cp).append("题×15分=").append(cp*15).append("分)：80词左右应用文（邮件/书信/通知/值日报告等），开头已给出。\n\n");
        sb.append("【输出】纯JSON数组。可用题型：\n");
        sb.append("SINGLE_CHOICE(单选/语言应用/补全对话,4选项,每题difficultyLevel1-5)\n");
        sb.append("READING_COMPREHENSION(阅读理解,每篇含passage+subQuestions数组含4道选择小题)\n");
        sb.append("FILL_IN(语法词汇填空,options=[],correctAnswer=补全后的单词/短语)\n");
        sb.append("SHORT_ANSWER(短文改错,correctAnswer=改正后的词/短语)\n");
        sb.append("COMPOSITION(书面表达,含wordLimit=80,scoringRubric,给出首句)\n");
        sb.append("【重要】题型分布——SINGLE_CHOICE共").append(sc).append("题(单项选择").append(scChoice).append("+语言应用").append(scLang).append("+补全对话").append(scDialog)
            .append("), READING_COMPREHENSION共").append(read).append("篇(每篇4小题), FILL_IN共").append(fi).append("题(语法词汇填空), SHORT_ANSWER共").append(sa).append("题(短文改错), COMPOSITION共").append(cp).append("题(书面表达)。");
        sb.append(QUALITY_RULES);
        String ref = (String) params.get("_referenceQuestions");
        if (ref != null && !ref.isEmpty()) {
            sb.append("\n【参考样题（仅做风格参考，不得照抄）】\n").append(ref).append("\n");
        }
        String prompt = sb.toString();
        Map<String,Object> qp = new java.util.LinkedHashMap<>();
        qp.put("_instructionPrompt", prompt); qp.put("comprehensive", false);
        qp.put("_maxTokens", questionTokenBudget(tc.values().stream().mapToInt(Integer::intValue).sum()));
        qp.put("temperature", 0.6);
        qp.put("typeCounts", tc);
        // 构建结构化知识点映射
        String subKpListJson = (String) params.get("_subKpList");
        if (subKpListJson != null && !subKpListJson.isEmpty()) {
            try {
                List<Map<String, Object>> kps = om.readValue(subKpListJson,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
                List<Map<String, Object>> cpList = new java.util.ArrayList<>();
                for (Map<String, Object> kpItem : kps) {
                    cpList.add(Map.of("nodeId", kpItem.get("id"), "path", kpItem.get("name")));
                }
                if (!cpList.isEmpty()) {
                    qp.put("categoryPaths", cpList);
                }
            } catch (Exception e) { log.warn("静默降级异常: {}", e.getMessage()); }
        }
        qp.put("_skipGenericFormat", Boolean.TRUE);
        return new PromptResult(prompt, 2500, qp);
    }

    // 语文专题训练 — 动态题量+token预算+逐题型格式说明
    private static PromptResult buildChineseTraining(String stageHint, String subject, String kp,
            String path, String rag, String syllabus, Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        Map<String, Integer> tc = params.get("typeCounts") instanceof Map<?,?> m && !m.isEmpty()
            ? (Map<String, Integer>) m : Map.of("SINGLE_CHOICE",10);
        int total = tc.values().stream().mapToInt(Integer::intValue).sum();
        int dynTokens = questionTokenBudget(total);
        StringBuilder typeStr = new StringBuilder();
        tc.forEach((k, v) -> { if (v > 0) typeStr.append(typeName(k)).append("×").append(v).append("、"); });
        if (typeStr.length() > 0) typeStr.setLength(typeStr.length() - 1);

        // 构建每种题型的 JSON 格式说明（关键：让 AI 知道每种题型的具体结构）
        StringBuilder fmt = new StringBuilder();
        for (Map.Entry<String, Integer> e : tc.entrySet()) {
            if (e.getValue() <= 0) continue;
            fmt.append("  ").append(typeName(e.getKey())).append("(").append(e.getKey()).append(")：");
            fmt.append(formatExample(e.getKey())).append("\n");
        }

        String prompt = "你是中职语文练习命题专家。围绕指定知识点出题，按难度递进排列（基础→中等→提高）。\n"
            + "本次需生成" + total + "题，必须严格按以下数量分配，禁止把所有题目集中到一种题型：\n"
            + typeStr + "。\n\n"
            + "【各题型 JSON 格式】\n" + fmt + "\n"
            + "精讲解析须指出错误选项错因+正确解法。每题含knowledgeNodeId/difficultyLevel(1-5)。输出纯JSON数组。\n"
            + (rag != null && !rag.isEmpty() ? "【参考资料】\n" + rag + "\n" : "") + buildSyllabusBlock(syllabus);
        Map<String,Object> qp = new java.util.LinkedHashMap<>();
        qp.put("_instructionPrompt", prompt); qp.put("_maxTokens", dynTokens); qp.put("temperature", 0.8);
        qp.put("subject", subject); qp.put("stageHint", stageHint); qp.put("knowledgePoint", kp);
        qp.put("typeCounts", tc);
        qp.put("_skipGenericFormat", Boolean.TRUE);  // 跳过 buildPrompt 中的通用格式，避免冲突
        return new PromptResult(prompt, total > 20 ? 2000 : 1000, qp);
    }

    // 数学专题训练 — 动态题量+token预算+逐题型格式说明
    private static PromptResult buildMathTraining(String stageHint, String subject, String kp,
            String path, String rag, String syllabus, Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        Map<String, Integer> tc = params.get("typeCounts") instanceof Map<?,?> m && !m.isEmpty()
            ? (Map<String, Integer>) m : Map.of("SINGLE_CHOICE",8,"CALCULATION",3);
        int total = tc.values().stream().mapToInt(Integer::intValue).sum();
        int dynTokens = questionTokenBudget(total);
        StringBuilder typeStr = fmtTypeCounts(tc);
        String prompt = "你是中职数学练习命题专家。所有题目围绕核心公式，按难度递进(直接代入→变形代入→综合应用)。\n"
            + "本次需生成" + total + "题，必须严格按以下数量分配，禁止把所有题目集中到一种题型：\n"
            + typeStr + "。\n\n"
            + "【各题型 JSON 格式】\n" + fmtExamples(tc) + "\n"
            + "干扰项设为该专题特有的常见计算错误结果。\n"
            + "精讲解析：指出错误选项+正确步骤+记忆口诀。每题含knowledgeNodeId/difficultyLevel。输出纯JSON数组。\n"
            + DIAGRAM_INSTRUCTIONS
            + (rag != null && !rag.isEmpty() ? "【参考资料】\n" + rag + "\n" : "") + buildSyllabusBlock(syllabus);
        Map<String,Object> qp = new java.util.LinkedHashMap<>();
        qp.put("_instructionPrompt", prompt); qp.put("_maxTokens", dynTokens); qp.put("temperature", 0.8);
        qp.put("subject", subject); qp.put("stageHint", stageHint); qp.put("knowledgePoint", kp);
        qp.put("typeCounts", tc);
        qp.put("_skipGenericFormat", Boolean.TRUE);
        return new PromptResult(prompt, total > 20 ? 2000 : 1000, qp);
    }

    // 英语专题训练 — 动态题量+token预算+逐题型格式说明
    private static PromptResult buildEnglishTraining(String stageHint, String subject, String kp,
            String path, String rag, String syllabus, Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        Map<String, Integer> tc = params.get("typeCounts") instanceof Map<?,?> m && !m.isEmpty()
            ? (Map<String, Integer>) m : Map.of("SINGLE_CHOICE",10,"SHORT_ANSWER",3,"FILL_IN",2);
        int total = tc.values().stream().mapToInt(Integer::intValue).sum();
        int dynTokens = questionTokenBudget(total);
        StringBuilder typeStr = fmtTypeCounts(tc);
        String prompt = "你是中职英语练习命题专家。围绕指定语法点或词汇出题。\n"
            + "本次需生成" + total + "题，必须严格按以下数量分配，禁止把所有题目集中到一种题型：\n"
            + typeStr + "。\n\n"
            + "【各题型 JSON 格式】\n" + fmtExamples(tc) + "\n"
            + "精讲解析须指出错误选项错因+正确用法+例句。每题含knowledgeNodeId/difficultyLevel。输出纯JSON数组。\n"
            + (rag != null && !rag.isEmpty() ? "【参考资料】\n" + rag + "\n" : "") + buildSyllabusBlock(syllabus);
        Map<String,Object> qp = new java.util.LinkedHashMap<>();
        qp.put("_instructionPrompt", prompt); qp.put("_maxTokens", dynTokens); qp.put("temperature", 0.8);
        qp.put("subject", subject); qp.put("stageHint", stageHint); qp.put("knowledgePoint", kp);
        qp.put("typeCounts", tc);
        qp.put("_skipGenericFormat", Boolean.TRUE);
        return new PromptResult(prompt, total > 20 ? 2000 : 1000, qp);
    }

    // 知识清单配套练习 — 动态题量+token预算+逐题型格式说明
    private static PromptResult buildKnowledgePractice(String stageHint, String subject, String kp,
            String path, String rag, String syllabus, Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        Map<String, Integer> tc = params.get("typeCounts") instanceof Map<?,?> m && !m.isEmpty()
            ? (Map<String, Integer>) m : Map.of("FILL_IN",7,"TRUE_FALSE",4,"SINGLE_CHOICE",5);
        // CLOZE 默认不出：QuestionTypeEnum 将 CLOZE 归为主观题需教师手批，保证默认发布即全自动评分
        int total = tc.values().stream().mapToInt(Integer::intValue).sum();
        int dynTokens = questionTokenBudget(total);
        StringBuilder typeStr = new StringBuilder();
        tc.forEach((k, v) -> { if (v > 0) typeStr.append(typeName(k)).append("×").append(v).append("、"); });
        if (typeStr.length() > 0) typeStr.setLength(typeStr.length() - 1);

        StringBuilder fmt = new StringBuilder();
        for (Map.Entry<String, Integer> e : tc.entrySet()) {
            if (e.getValue() <= 0) continue;
            fmt.append("  ").append(typeName(e.getKey())).append("(").append(e.getKey()).append(")：");
            fmt.append(formatExample(e.getKey())).append("\n");
        }

        String checklistContent = (String) params.get("_checklistContent");
        String reference = checklistContent != null && !checklistContent.isBlank() ? checklistContent : rag;
        if (reference != null && reference.length() > 6000) {
            int cut = reference.lastIndexOf("\n", 6000);
            reference = reference.substring(0, cut > 0 ? cut : 6000) + "\n...(内容已按段落边界截断)";
        }

        String subKpListJson = (String) params.get("_subKpList");
        StringBuilder categoryPaths = new StringBuilder();
        if (subKpListJson != null && !subKpListJson.isEmpty()) {
            try {
                List<Map<String, Object>> kps = om.readValue(subKpListJson,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> kpItem : kps) {
                    categoryPaths.append(kpItem.get("name")).append("、");
                }
                if (categoryPaths.length() > 0) categoryPaths.setLength(categoryPaths.length() - 1);
            } catch (Exception e) { log.warn("categoryPaths解析失败: {}", e.getMessage()); }
        }

        String prompt = "你是" + stageHint + subject + "学科教师，为学生编制一套基于\u201C知识清单\u201D的配套练习。\n\n"
            + "【参考资料】以下是本次出题必须依据的知识清单内容（或知识库资料）：\n"
            + (reference != null && !reference.isBlank() ? reference : "暂无参考资料") + "\n"
            + "知识点范围：" + (categoryPaths.length() > 0 ? categoryPaths : (kp != null ? kp : "通用")) + "\n\n"
            + "【出题要求】总计" + total + "题：" + typeStr + "\n"
            + "1. 填空题(FILL_IN)：挖空必须落在清单核心概念/关键术语/关键数据上，一题一空；\n"
            + "    等价答案（同义词/不同写法）在answer中用竖线|分隔列出全部；\n"
            + "    挖空后题干仍能唯一确定答案，禁止\u201C重要的是____\u201D这类无信息量挖空。\n"
            + "2. 判断题(TRUE_FALSE)：优先从清单\u201C常见误区\u201D改编错误说法，正确说法直接取自清单；\n"
            + "    错误说法仅一处错误且清单可证伪。\n"
            + "3. 单选题(SINGLE_CHOICE)：4选项，干扰项取清单中相近易混淆概念，不编造清单外知识。\n"
            // CLOZE 默认0不出题：数量>0 才注入完形指令，避免 AI 因看到指令而多生成（CLOZE 是主观题需教师手批）
            + (tc.getOrDefault("CLOZE", 0) > 0
                ? "4. 完形填空(CLOZE)：从清单选一段概括性文字改写为3-5空短文，占位符统一【____】；\n"
                + "    answer用逗号按空的先后顺序分隔，答案个数必须与【____】个数严格一致。\n"
                : "")
            + "【共同约束】每题仅凭清单内容可作答，禁\u201C如上文/如清单所述\u201D指代；\n"
            + "覆盖清单主要小节不重复；每题含knowledgeNodeId/difficultyLevel(1-5)。\n"
            + "【输出格式】\n" + fmt + "\n输出纯JSON数组。";

        // V055-fix: 注入真题参考样题（Few-shot 风格对齐）
        String refQ = (String) params.get("_referenceQuestions");
        if (refQ != null && !refQ.isEmpty()) {
            prompt += "\n\n【参考样题（仅做风格参考，不得照抄）】\n" + refQ;
        }

        Map<String,Object> qp = new java.util.LinkedHashMap<>();
        qp.put("_instructionPrompt", prompt); qp.put("_maxTokens", dynTokens); qp.put("temperature", 0.8);
        qp.put("subject", subject); qp.put("stageHint", stageHint); qp.put("knowledgePoint", kp);
        qp.put("typeCounts", tc);
        qp.put("_skipGenericFormat", Boolean.TRUE);
        // 构建结构化知识点映射（复用现有解析模式）
        if (subKpListJson != null && !subKpListJson.isEmpty()) {
            try {
                List<Map<String, Object>> kps = om.readValue(subKpListJson,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
                List<Map<String, Object>> cpList = new java.util.ArrayList<>();
                for (Map<String, Object> kpItem : kps) {
                    cpList.add(Map.of("nodeId", kpItem.get("id"), "path", kpItem.get("name")));
                }
                if (!cpList.isEmpty()) {
                    qp.put("categoryPaths", cpList);
                }
            } catch (Exception e) { log.warn("静默降级异常: {}", e.getMessage()); }
        }
        return new PromptResult(prompt, total > 20 ? 2000 : 1000, qp);
    }

    private static StringBuilder fmtTypeCounts(Map<String, Integer> tc) {
        StringBuilder sb = new StringBuilder();
        tc.forEach((k, v) -> { if (v > 0) sb.append(typeName(k)).append("×").append(v).append("、"); });
        if (sb.length() > 0) sb.setLength(sb.length() - 1);
        return sb;
    }
    private static String fmtExamples(Map<String, Integer> tc) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : tc.entrySet()) {
            if (e.getValue() <= 0) continue;
            sb.append("  ").append(typeName(e.getKey())).append("(").append(e.getKey()).append(")：");
            sb.append(formatExample(e.getKey())).append("\n");
        }
        return sb.toString();
    }

    /** 题型中文映射（训练 Prompt 内联用） */
    private static String typeName(String key) {
        return switch (key) {
            case "SINGLE_CHOICE" -> "单选题";
            case "MULTI_CHOICE" -> "多选题";
            case "TRUE_FALSE" -> "判断题";
            case "FILL_IN" -> "填空题";
            case "ESSAY", "SHORT_ANSWER" -> "简答题";
            case "CLOZE" -> "完形填空";
            case "READING_COMPREHENSION", "READING" -> "阅读理解";
            case "CALCULATION" -> "计算题";
            case "PROOF" -> "证明题";
            case "COMPOSITION" -> "作文";
            default -> key;
        };
    }

    /** 每种题型的具体 JSON 格式示例（训练 Prompt 用） */
    private static String formatExample(String key) {
        return switch (key) {
            case "SINGLE_CHOICE" -> "{\"questionType\":\"SINGLE_CHOICE\",\"questionText\":\"题干\",\"options\":[\"选项A内容\",\"选项B内容\",\"选项C内容\",\"选项D内容\"],\"correctAnswer\":\"A\",\"explanation\":\"解析\",\"difficultyLevel\":3,\"knowledgeNodeId\":1}";
            case "MULTI_CHOICE" -> "{\"questionType\":\"MULTI_CHOICE\",\"questionText\":\"题干\",\"options\":[\"选项A内容\",\"选项B内容\",\"选项C内容\",\"选项D内容\"],\"correctAnswer\":\"AB\",\"explanation\":\"解析\"}";
            case "TRUE_FALSE" -> "{\"questionType\":\"TRUE_FALSE\",\"questionText\":\"题干\",\"options\":[\"正确\",\"错误\"],\"correctAnswer\":\"A\",\"explanation\":\"解析\"}";
            case "FILL_IN" -> "{\"questionType\":\"FILL_IN\",\"questionText\":\"题干含____空白\",\"options\":[],\"correctAnswer\":\"填入答案\",\"explanation\":\"解析\"}";
            case "SHORT_ANSWER", "ESSAY" -> "{\"questionType\":\"SHORT_ANSWER\",\"questionText\":\"题干\",\"options\":[],\"correctAnswer\":\"参考答案要点\",\"explanation\":\"评分要点\"}";
            case "CALCULATION" -> "{\"questionType\":\"CALCULATION\",\"questionText\":\"题干\",\"steps\":[\"步骤1\",\"步骤2\"],\"subQuestions\":[{\"questionText\":\"(1) 小问1\",\"score\":5},{\"questionText\":\"(2) 小问2\",\"score\":5}],\"correctAnswer\":\"结果\",\"explanation\":\"解析\"}";
            case "PROOF" -> "{\"questionType\":\"PROOF\",\"questionText\":\"题干\",\"keyPoints\":[\"关键步骤1\",\"关键步骤2\"],\"correctAnswer\":\"完整证明\"}";
            case "CLOZE" -> "{\"questionType\":\"CLOZE\",\"questionText\":\"短文开头【____】中间内容【____】结尾\",\"options\":[],\"correctAnswer\":\"答案1,答案2\",\"explanation\":\"解析\"}";
            case "COMPOSITION" -> "{\"questionType\":\"COMPOSITION\",\"questionText\":\"作文题目/材料\",\"correctAnswer\":\"评分要点\",\"scoringRubric\":\"评分标准\",\"wordLimit\":600}";
            case "READING_COMPREHENSION", "READING" -> "{\"questionType\":\"READING_COMPREHENSION\",\"questionText\":\"导读\",\"passage\":\"短文全文\",\"questions\":[{\"questionText\":\"小题1\",\"options\":[\"A\",\"B\",\"C\",\"D\"],\"correctAnswer\":\"A\"}]}";
            default -> "{\"questionType\":\"" + key + "\",\"questionText\":\"题干\",\"correctAnswer\":\"答案\",\"explanation\":\"解析\"}";
        };
    }

    // 文化课专属教学设计模板
    private static PromptResult buildCultureTeachingDesign(String stageHint, String subject, String kp,
            String path, String rag, String syllabus, Map<String, Object> params) {
        String bareSubject = (subject != null ? subject.replaceAll("\\[.*?\\]", "").trim() : "");
        String template = switch (bareSubject) {
            case "语文" -> "你是高中语文教师。请设计45分钟教学设计：教学目标(语言建构/思维发展/审美鉴赏/文化传承)、教学重难点、教学过程(导入5'→整体感知10'→深入研读15'→拓展延伸10'→课堂小结5')、板书设计、课后作业。不使用\"理实一体\"\"做中学做中教\"\"岗课赛证\"等职业术语。";
            case "数学" -> "你是高中数学教师。请设计45分钟教学设计：教学目标(数学抽象/逻辑推理/数学建模/直观想象/数学运算/数据分析)、教学重难点、教学过程(情境导入5'→概念形成10'→例题讲解10'→巩固练习15'→课堂小结5')、板书设计、课后作业。不使用\"理实一体\"\"做中学做中教\"\"岗课赛证\"等职业术语。";
            case "英语" -> "你是高中英语教师。请设计45分钟教学设计：教学目标(语言能力/文化意识/思维品质/学习能力)、教学重难点、教学过程(Warm-up5'→Presentation10'→Practice10'→Production15'→Summary+Homework5')、板书设计。不使用\"理实一体\"\"做中学做中教\"\"岗课赛证\"等职业术语。";
            default -> "你是高中教师。请设计45分钟教学设计：包含教学目标/重难点/教学过程/板书/作业。不使用\"理实一体\"\"做中学做中教\"等职业术语。";
        };
        return new PromptResult(template + "\n\n【知识点】" + (kp != null ? kp : "通用") + "\n" + (rag != null && !rag.isEmpty() ? "【参考资料】\n" + rag : ""), 6000);
    }

    // 诊断 Prompt
    private static String safeToJson(Object obj) {
        try { return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj); }
        catch (Exception e) { return "{}"; }
    }

    private static PromptResult buildDiagnosis(String subject, Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) params.get("diagnosisData");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = (List<Map<String, Object>>) data.get("questions");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> students = (List<Map<String, Object>>) data.get("students");

        // 只发送弱项题目和薄弱学生，减少 token
        StringBuilder weakQuestions = new StringBuilder();
        if (questions != null) {
            for (Map<String, Object> q : questions) {
                double rate = q.get("correctRate") instanceof Number n ? n.doubleValue() : 100;
                if (rate < 70) {
                    weakQuestions.append("- Q").append(q.get("questionIndex"))
                        .append(": ").append(q.get("questionText")).append(" (正确率").append(Math.round(rate)).append("%)\n");
                }
            }
        }
        StringBuilder topStudents = new StringBuilder();
        if (students != null) {
            students = new ArrayList<>(students);
            students.sort((a, b) -> {
                Double sa = a.get("score") instanceof Number na ? na.doubleValue() : -1.0;
                Double sb = b.get("score") instanceof Number nb ? nb.doubleValue() : -1.0;
                return sa.compareTo(sb);
            });
            int show = Math.min(8, students.size());
            for (int i = 0; i < show; i++) {
                Map<String, Object> s = students.get(i);
                topStudents.append(i + 1).append(". ").append(s.get("name"))
                    .append(" ").append(s.get("score")).append("分\n");
            }
        }

        // 提取考纲元数据作为诊断参考（试题权重、核心考点等）
        String syllabusMeta = params.get("syllabusMeta") instanceof String s && !s.isEmpty() ? s : null;
        String syllabusHint = "";
        if (syllabusMeta != null) {
            syllabusHint = "【考纲参考】\n" + syllabusMeta + "\n请基于考纲标注的知识点权重和核心考点，优先关注高权重知识点的掌握情况。\n";
        }

        String prompt = "你是" + (subject != null ? subject : "学科") + "教师。以下是学生答题数据摘要：\n\n"
            + "【薄弱题目（正确率<70%）】\n" + (weakQuestions.length() > 0 ? weakQuestions : "无（所有题目正确率均≥70%）") + "\n"
            + "【分数最低的" + Math.min(8, students != null ? students.size() : 0) + "名学生】\n" + topStudents + "\n"
            + syllabusHint
            + "【输出格式 — 纯文本Markdown，不用JSON】\n"
            + "## 班级学情总结\n2-3句话概括整体表现（不重复数据，只说结论）\n\n"
            + "## 共性薄弱点\n列出2-3个最突出的共性问题，每个说明原因和教学建议（每条50-80字）\n\n"
            + "## 重点学生关注\n对得分最低的3-5名学生，每人一句针对性鼓励+一句具体建议\n\n"
            + "【要求】1.发展性语言禁止负面词 2.建议具体可操作 3.纯Markdown不用JSON 4.总字数≤500字";

        return new PromptResult(prompt, 1500);
    }

    public static PromptResult buildConsolidationPrompt(String subject, java.util.List<String> kpNames,
            java.util.List<Double> errorRates, String commonMistakes, String ragContext) {
        StringBuilder kpList = new StringBuilder();
        for (int i = 0; i < kpNames.size(); i++) {
            kpList.append(i + 1).append(". ").append(kpNames.get(i));
            if (i < (errorRates != null ? errorRates.size() : 0))
                kpList.append("（错误率 ").append(Math.round(errorRates.get(i))).append("%）");
            kpList.append("\n");
        }
        String bareSubject = subject != null ? subject.replaceAll("\\[.*?\\]", "").trim() : "通用";

        String prompt;
        if (isCultureSubject(subject)) {
            // 文化课：学术型模板（知识回顾→例题→易错→练习）
            prompt = "你是" + bareSubject + "教师。请基于以下信息生成一份巩固学习材料。\n\n"
                + "【薄弱知识点】\n" + kpList + "\n"
                + "【常见错误】\n" + (commonMistakes != null && !commonMistakes.isEmpty() ? commonMistakes : "根据知识点推断常见错误")
                + "\n\n"
                + "【输出结构——严格按以下Markdown格式：】\n"
                + "# " + bareSubject + " 巩固材料\n\n"
                + "## 一、错误分析\n分析学生错误的根本原因（概念混淆/公式记错/步骤遗漏），100-200字。\n\n"
                + "## 二、核心公式/规则\n列出最关键的公式定理，每个知识点2-3条。数学公式用$$公式$$格式。\n\n"
                + "## 三、例题精讲（3道）\n每道：**题目**(易→难) → **错误解法**(展示典型错误) → **正确解法**(Step-by-step+依据) → **避坑提示**(一句话口诀)\n\n"
                + "## 四、巩固练习（5道）\n与例题同类型，难度递进。末尾给参考答案和简要解析。\n\n"
                + "【要求】1.语言简洁适合中职学生 2.例题错误解法须基于上述错误描述 3.数学结果用整数或简分数 4.输出纯Markdown";
        } else {
            // 专业课：技能型模板（操作步骤→故障排查→实操练习→案例分析）
            prompt = "你是" + bareSubject + "专业课教师。请基于以下信息生成一份技能巩固材料。\n\n"
                + "【薄弱知识点】\n" + kpList + "\n"
                + "【常见错误】\n" + (commonMistakes != null && !commonMistakes.isEmpty() ? commonMistakes : "根据知识点推断常见操作错误")
                + "\n\n"
                + "【输出结构——严格按以下Markdown格式：】\n"
                + "# " + bareSubject + " 巩固材料\n\n"
                + "## 一、操作步骤拆解\n将核心技能拆分为3-5步清晰的操作流程，每步说明**做什么**和**为什么这样做**。用『步骤1/2/3...』标记。100-200字。\n\n"
                + "## 二、常见错误与排查\n列出3个典型错误（基于上述错误描述），每个格式：**错误现象** → **原因** → **解决方法**。\n\n"
                + "## 三、实操练习（3道）\n模拟真实场景的操作题，每道包含：**场景描述** → **要求** → **操作要点提示**。难度递进。\n\n"
                + "## 四、案例分析\n一个150-200字的综合案例：某学生遇到XX问题→分析原因→操作纠错→总结口诀（一句话记忆技巧）。\n\n"
                + "【要求】1.语言贴近实际工作岗位 2.步骤可操作性强 3.避免纯理论灌输 4.输出纯Markdown";
        }

        if (ragContext != null && !ragContext.isEmpty()) prompt += "\n\n【知识库参考】\n" + ragContext;

        java.util.Map<String, Object> extra = new java.util.LinkedHashMap<>();
        extra.put("_instructionPrompt", prompt);
        extra.put("_maxTokens", 4000);
        extra.put("subject", subject);
        extra.put("contentType", "CONSOLIDATION_MATERIAL");
        return new PromptResult(prompt, 2000, extra);
    }

    /**
     * 统一题目 token 预算计算（与 DeepSeekGateway.resolveMaxTokens v2 对齐）
     * 500/题(非主观) + 1200/题(主观) + 最低8000 + 上限32768
     */
    static int questionTokenBudget(int count) {
        return Math.max(8000, Math.min(600 + count * 500, 32768));
    }

    public record PromptResult(String prompt, int maxTokens, Map<String, Object> extraParams) {



        public PromptResult(String prompt, int maxTokens) {



            this(prompt, maxTokens, null);



        }



        public boolean isQuestionType() { return extraParams != null; }



    }



}




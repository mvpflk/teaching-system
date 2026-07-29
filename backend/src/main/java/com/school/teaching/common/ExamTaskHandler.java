package com.school.teaching.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import com.school.teaching.service.SubmitLockService;
import com.school.teaching.exception.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.school.teaching.service.NotificationService;
import com.school.teaching.common.NotificationType;
import org.springframework.dao.DuplicateKeyException;

/**
 * 考试/测验任务处理器 — 覆盖 FORMATIVE 和 SUMMATIVE 两种任务类型。
 *
 * 核心流程：加锁 → 批量加载题目 → 客观题自动评分 → 错题收录 → 写回答记录 → 计算总分。
 * 约束：全程批量操作，严禁循环内查库；所有 DB 写入由上层 TaskServiceImpl @Transactional 管控。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExamTaskHandler implements TaskTypeHandler {

    private final TaskQuestionMapper taskQuestionMapper;
    private final QuestionBankMapper questionBankMapper;
    private final TaskSubmissionMapper submissionMapper;
    private final StudentAnswerMapper answerMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final SubmitLockService lockService;
    private final com.school.teaching.service.StudentEventLogService eventLogService;
    private final PrecisionProgressMapper progressMapper;
    private final StudentMapper studentMapper;
    private final NotificationService notificationService;
    private final SqlSessionFactory sqlSessionFactory;

    private static final Set<String> OBJECTIVE_TYPES = QuestionTypeEnum.OBJECTIVE_TYPES.stream()
        .map(QuestionTypeEnum::name).collect(Collectors.toSet());
    private static final Set<String> OPTION_TYPES = QuestionTypeEnum.OPTION_TYPES.stream()
        .map(QuestionTypeEnum::name).collect(Collectors.toSet());

    @Override
    public Set<TaskCategory> getCategories() {
        return Set.of(TaskCategory.FORMATIVE, TaskCategory.SUMMATIVE);
    }

    @Override @Transactional
    public TaskSubmission onSubmit(TaskContext ctx) {
        Long taskId = ctx.taskId();
        Long studentId = ctx.studentId();
        String lockKey = "task:" + taskId + ":" + studentId;

        try (SubmitLockService.SubmitLock lock = lockService.tryLockString(lockKey)) {
            if (lock == null) throw new BusinessException(409, "请勿重复提交，正在处理中");

            Task task = (Task) ctx.extras().get("task");
            if (task == null) throw new BusinessException(500, "任务上下文缺失");
            if (!"PUBLISHED".equals(task.getStatus()) && !"ONGOING".equals(task.getStatus()))
                throw new BusinessException(400, "任务状态不可提交");

            // 加载提交记录（校验用）
            Long submissionId = ctx.extras() != null ? (Long) ctx.extras().get("submissionId") : null;
            TaskSubmission sub = submissionId != null ? submissionMapper.selectById(submissionId) : null;
            if (sub == null) throw new BusinessException(404, "提交记录不存在");

            // 校验 PENDING 状态（防御性：防止直接调用 handler 绕过）
            if (!"PENDING".equals(sub.getStatus())) {
                throw new BusinessException(400, "提交记录状态异常（需为 PENDING），当前状态: " + sub.getStatus());
            }

            // 检查作弊终止（特许重做除外）
            if (sub.getCheatTerminated() != null && sub.getCheatTerminated() == 1
                && (sub.getExtraSubmitAllowed() == null || sub.getExtraSubmitAllowed() != 1)) {
                throw new BusinessException(410, "考试已被终止，不可提交。如需重考请联系教师特许。");
            }

            // 考试时长校验（服务端兜底，防止前端篡改 localStorage 超时）
            if (task.getTaskConfig() != null && !task.getTaskConfig().isBlank()) {
                try {
                    Map<String, Object> config = new ObjectMapper().readValue(task.getTaskConfig(),
                        new TypeReference<Map<String, Object>>() {});
                    Object durRaw = config.get("durationMinutes");
                    if (durRaw != null) {
                        int durationMinutes = Integer.parseInt(durRaw.toString());
                        if (durationMinutes > 0 && sub.getCreatedAt() != null) {
                            long elapsedMinutes = Duration.between(sub.getCreatedAt(), LocalDateTime.now()).toMinutes();
                            if (elapsedMinutes > durationMinutes + 1) { // 1分钟宽容期
                                throw new BusinessException(410, "考试已超时，不可提交");
                            }
                        }
                    }
                } catch (BusinessException e) {
                    throw e;
                } catch (Exception ignored) { /* 配置解析失败时不拦截提交 */ }
            }

            // 竞赛模式: 切屏限制降为1次
            boolean competition = task.getIsCompetitionMode() != null && task.getIsCompetitionMode() == 1;
            if (competition) {
                ctx.extras().put("maxCheatWarnings", 1);
                ctx.extras().put("scoringMode", "deduction");
                eventLogService.log(ctx.studentId(), "TASK_COMPETITION_STARTED",
                    Map.of("taskId", taskId, "mode", "competition"), "TASK");
            }

            List<TaskQuestion> tqList = taskQuestionMapper.selectList(
                new LambdaQueryWrapper<TaskQuestion>().eq(TaskQuestion::getTaskId, taskId));
            if (tqList.isEmpty()) throw new BusinessException(400, "任务无题目");

            // 批量加载题目
            Set<Long> qIds = tqList.stream().map(TaskQuestion::getQuestionId).collect(Collectors.toSet());
            Map<Long, QuestionBank> qMap = questionBankMapper.selectBatchIds(qIds).stream()
                .collect(Collectors.toMap(QuestionBank::getId, q -> q, (a, b) -> a));

            // 解析学生答案 {"1":"A","2":"B,C",...}
            @SuppressWarnings("unchecked")
            Map<String, Object> answers = (Map<String, Object>) ctx.submission().get("answers");
            if (answers == null) throw new BusinessException(400, "缺少答案数据");

            // 逐题评分 → 收集 student_answers + wrong_questions
            List<StudentAnswer> answerList = new ArrayList<>();
            List<WrongQuestion> wrongList = new ArrayList<>();
            BigDecimal totalAuto = BigDecimal.ZERO;

            for (TaskQuestion tq : tqList) {
                QuestionBank q = qMap.get(tq.getQuestionId());
                if (q == null) continue;
                String studentAns = Objects.toString(answers.get(String.valueOf(tq.getQuestionId())), "");
                BigDecimal qScore = gradeOne(q, studentAns, answerList, wrongList, ctx, tq);
                totalAuto = totalAuto.add(qScore);
            }

            // 上限截断：不超过任务设定总分（题目分值之和可能略超 totalScore）
            if (task.getTotalScore() != null && totalAuto.compareTo(task.getTotalScore()) > 0) {
                totalAuto = task.getTotalScore();
            }

            // 批量写入（分批避免单次SQL过大）
            if (!answerList.isEmpty()) {
                for (List<StudentAnswer> batch : partition(answerList, 50)) {
                    try (org.apache.ibatis.session.SqlSession sqlSession = sqlSessionFactory.openSession(org.apache.ibatis.session.ExecutorType.BATCH)) {
                        StudentAnswerMapper batchMapper = sqlSession.getMapper(StudentAnswerMapper.class);
                        for (StudentAnswer a : batch) batchMapper.insert(a);
                        sqlSession.commit();
                    }
                }
            }
            if (!wrongList.isEmpty()) batchUpsertWrong(wrongList);

            // 更新 PrecisionProgress（加权移动平均，与 PrecisionHelper 一致）
            Set<Long> updatedNodes = new HashSet<>();
            for (StudentAnswer sa : answerList) {
                if (sa.getIsCorrect() == null || sa.getIsCorrect() == 2) continue; // 主观题跳过
                QuestionBank q = qMap.get(sa.getQuestionId());
                if (q == null || q.getCategoryId() == null) continue;
                if (!updatedNodes.add(q.getCategoryId())) continue; // 同一节点只更新一次
                updatePrecisionProgress(studentId, q.getCategoryId(), q.getSubject(), sa.getIsCorrect() == 1);
            }

            // 更新提交记录（sub 已在前面加载，此处直接使用）
            sub.setScore(totalAuto);
            sub.setStatus("SUBMITTED");
            sub.setExtraSubmitAllowed(null); // 消耗额外提交权限，防止无限重交
            sub.setSubmittedAt(LocalDateTime.now());
            // 评分摘要：告知学生客观题已批改，主观题待教师评分
            long subjectiveCount = answerList.stream().filter(a -> a.getIsCorrect() != null && a.getIsCorrect() == 2).count();
            sub.setObjectiveScore(totalAuto);
            sub.setSubjectivePendingCount((int) subjectiveCount);
            sub.setGradingMessage(subjectiveCount > 0
                ? "客观题已自动批改(" + totalAuto + "分)，" + subjectiveCount + "道主观题等待教师评分"
                : "全部客观题已自动批改，共" + totalAuto + "分");
            submissionMapper.updateById(sub);

            // ════════════════════════════════════════════════════════
            // 达标判断 + 重测（2026-07-03 新增）
            // ════════════════════════════════════════════════════════
            // 仅在启用达标模式时走重测逻辑
            if (task.getPassRate() == null || task.getPassRate() <= 0) return sub;
            if (task.getMaxAttempts() == null || task.getMaxAttempts() <= 1) return sub;

            int passRate = task.getPassRate();
            int maxAttempts = task.getMaxAttempts();
            int attemptNumber = sub.getAttemptNumber() != null ? sub.getAttemptNumber() : 1;

            // 重测截止时间检查：仅重测提交（attemptNumber > 1）需检查截止时间
            if (attemptNumber > 1 && task.getRetakeDeadlineHours() != null) {
                TaskSubmission firstSub = submissionMapper.selectOne(
                    new LambdaQueryWrapper<TaskSubmission>()
                        .eq(TaskSubmission::getTaskId, task.getId())
                        .eq(TaskSubmission::getStudentId, studentId)
                        .eq(TaskSubmission::getAttemptNumber, 1)
                        .last("LIMIT 1"));
                if (firstSub != null && firstSub.getSubmittedAt() != null) {
                    LocalDateTime deadline = firstSub.getSubmittedAt()
                        .plusHours(task.getRetakeDeadlineHours());
                    if (LocalDateTime.now().isAfter(deadline)) {
                        throw new BusinessException(409,
                            "重测截止时间已过（首次提交于 " + firstSub.getSubmittedAt().toLocalDate()
                                + "，截止 " + task.getRetakeDeadlineHours() + " 小时后）");
                    }
                }
            }

            // 已达最大次数 → 不再重测
            if (attemptNumber >= maxAttempts) return sub;

            BigDecimal total = task.getTotalScore() != null ? task.getTotalScore() : BigDecimal.valueOf(100);
            if (total.compareTo(BigDecimal.ZERO) <= 0) return sub;

            // 根据 passMode 选择判定分数：objective=仅客观题，all=全判定
            String passMode = task.getPassMode();
            BigDecimal judgmentScore;
            if ("all".equals(passMode)) {
                judgmentScore = sub.getScore();
            } else {
                // objective 或 null（兜底）：优先使用客观题分数
                judgmentScore = sub.getObjectiveScore() != null ? sub.getObjectiveScore() : sub.getScore();
            }
            double rate = judgmentScore != null
                ? judgmentScore.doubleValue() / total.doubleValue() * 100 : 0;

            // 达标 → 通过
            if (rate >= passRate) return sub;

            // 安全检查：如果该生已有任意轮次达标，不创建重测
            if (hasAnyAttemptPassed(task.getId(), studentId, task, total)) return sub;

            // 未达标 + 还有机会 → 创建重测
            // 快照当前 passRate 到 scoreJson
            java.util.Map<String, Object> meta = new java.util.LinkedHashMap<>();
            meta.put("passRateAtCreation", task.getPassRate());
            meta.put("originalSubmissionId", sub.getResubmissionOf() != null
                ? sub.getResubmissionOf() : sub.getId());
            String scoreJsonSnapshot = null;
            try {
                scoreJsonSnapshot = new ObjectMapper().writeValueAsString(meta);
            } catch (Exception ignored) {
                log.warn("scoreJsonSnapshot 序列化失败，将使用 null: taskId={}", task.getId());
            }

            TaskSubmission retake = new TaskSubmission();
            retake.setTaskId(task.getId());
            retake.setStudentId(studentId);
            retake.setAttemptNumber(attemptNumber + 1);
            retake.setIsOfficial(false);
            retake.setResubmissionOf(sub.getResubmissionOf() != null
                ? sub.getResubmissionOf() : sub.getId());
            retake.setStatus("PENDING");
            retake.setSchoolId(sub.getSchoolId());
            retake.setStageId(sub.getStageId());
            retake.setScoreJson(scoreJsonSnapshot);
            try {
                submissionMapper.insert(retake);
            } catch (DuplicateKeyException e) {
                log.info("重测记录已存在，跳过创建: taskId={}, studentId={}, attempt={}",
                    task.getId(), studentId, attemptNumber + 1);
                return sub;
            }

            // 发送重测通知
            Student retakeStudent = studentMapper.selectById(studentId);
            Long retakeUserUserId = retakeStudent != null ? retakeStudent.getUserId() : null;
            if (retakeUserUserId != null) {
                int remaining = maxAttempts - attemptNumber;
                try {
                    notificationService.notify(retakeUserUserId, NotificationType.RETEST_REQUIRED,
                        "📝 《" + task.getTitle() + "》需重新作答",
                        "你在《" + task.getTitle() + "》中得分 " + (int)rate + "%，低于达标线 "
                            + passRate + "%。请在截止时间前完成重测，剩余 "
                            + remaining + " 次机会。",
                        task.getId());
                } catch (Exception nfEx) {
                    log.warn("重测通知发送失败: taskId={}, studentId={}", task.getId(), studentId, nfEx);
                }
            }

            return sub;
        }
    }

    /** 评一道题，返回该题得分。副作用：向 answerList / wrongList 追加记录 */
    private BigDecimal gradeOne(QuestionBank q, String studentAns,
                                 List<StudentAnswer> answerList, List<WrongQuestion> wrongList,
                                 TaskContext ctx, TaskQuestion tq) {
        StudentAnswer sa = new StudentAnswer();
        sa.setSubmissionId(ctx.extras() != null ? (Long) ctx.extras().get("submissionId") : null);
        sa.setTaskId(ctx.taskId());
        sa.setQuestionId(q.getId());
        sa.setStudentAnswer(studentAns);
        sa.setSchoolId(ctx.extras() != null ? (Long) ctx.extras().get("schoolId") : null);
        sa.setStageId(ctx.extras() != null ? (Long) ctx.extras().get("stageId") : null);

        BigDecimal score = tq.getScore() != null ? tq.getScore() : BigDecimal.ONE;

        // DRAG_SORT/MATCHING — 复杂客观题无法自动判分，标记为待教师评分
        if ("DRAG_SORT".equals(q.getQuestionType()) || "MATCHING".equals(q.getQuestionType())) {
            sa.setIsCorrect(2);
            sa.setAutoScore(null);
            answerList.add(sa);
            return BigDecimal.ZERO;
        }

        if (OBJECTIVE_TYPES.contains(q.getQuestionType())) {
            // 修正教师正确答案：选项截断后，正确答案可能超出当前选项范围（如原5选项答案为E）
            String correctAns = normalizeAnswerToOptions(q.getCorrectAnswer(), q.getOptions(), q.getQuestionType());
            boolean correct = answersMatch(q.getQuestionType(), correctAns, studentAns);
            sa.setIsCorrect(correct ? 1 : 0);
            sa.setAutoScore(correct ? score : BigDecimal.ZERO);
            answerList.add(sa);
            if (!correct) {
                // 检查 autoWrongbook 标志（默认开启），关闭时不收录错题
                Task task = (Task) ctx.extras().get("task");
                if (task == null || task.getAutoWrongbook() == null || task.getAutoWrongbook() == 1) {
                    WrongQuestion wq = new WrongQuestion();
                    wq.setStudentId(ctx.studentId());
                    wq.setQuestionId(q.getId());
                    wq.setWrongCount(1);
                    wq.setLastWrongTime(LocalDateTime.now());
                    wq.setIsMastered(0);
                    wq.setSourceTaskId(task != null ? task.getId() : null);
                    wq.setSourceType(task != null ? inferSourceFromCategory(task.getTaskType()) : null);
                    wrongList.add(wq);
                }
            }
            return correct ? score : BigDecimal.ZERO;
        } else {
            // 主观题 — 待教师评分，2=主观待评分，null=尚未评分
            sa.setIsCorrect(2);
            sa.setAutoScore(null);
            answerList.add(sa);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 答案匹配 — 兼容 SINGLE_CHOICE/MULTI_CHOICE/TRUE_FALSE/FILL_IN。
     * MULTI_CHOICE: 排序后比较（"A,C" == "C,A"）
     */
    public static boolean answersMatch(String questionType, String correct, String student) {
        if (correct == null || student == null) return false;
        String sc = correct.trim();
        String ss = normalizeStudentAnswer(student);
        // 空答案不应被判对（防止双方都为空或空白时误判）
        if (sc.isEmpty() || ss.isEmpty()) return false;
        if ("MULTI_CHOICE".equals(questionType) || "MATCHING".equals(questionType)) {
            // 规范化：去除逗号/顿号/空格，按字母排序去重后比较
            return sortTokens(sc).equals(sortTokens(ss));
        }
        if ("TRUE_FALSE".equals(questionType)) {
            // 双向语义匹配：无论 correctAnswer 是 A/B/T/F/对/错/True/False/√/×，
            // 无论 studentAnswer 是 A/B/T/F/对/错... 都能正确匹配
            String ssc = sc.toUpperCase();
            String sss = ss.toUpperCase();
            boolean correctIsTrue = ssc.equals("A") || ssc.equals("T") || ssc.equals("TRUE") || ssc.equals("YES")
                || ssc.equals("对") || ssc.equals("正确") || ssc.equals("√");
            boolean correctIsFalse = ssc.equals("B") || ssc.equals("F") || ssc.equals("FALSE") || ssc.equals("NO")
                || ssc.equals("错") || ssc.equals("错误") || ssc.equals("×");
            boolean studentIsTrue = sss.equals("A") || sss.equals("T") || sss.equals("TRUE") || sss.equals("YES")
                || sss.equals("对") || sss.equals("正确") || sss.equals("√");
            boolean studentIsFalse = sss.equals("B") || sss.equals("F") || sss.equals("FALSE") || sss.equals("NO")
                || sss.equals("错") || sss.equals("错误") || sss.equals("×");
            if (correctIsTrue && studentIsTrue) return true;
            if (correctIsFalse && studentIsFalse) return true;
            return false;
        }
        // DRAG_SORT / MATCHING — 复杂题型无法自动判分，由教师手动评分
        if ("DRAG_SORT".equals(questionType) || "MATCHING".equals(questionType)) {
            return false;
        }
        // CLOZE / FILL_IN: 模糊匹配 — 多答案拆分 + 包含匹配 + 括号内容剥离
        if ("FILL_IN".equals(questionType) || "CLOZE".equals(questionType)) {
            String[] alts = sc.split("[;,，；/|、]");
            for (String alt : alts) {
                String a = alt.trim();
                if (a.isEmpty()) continue;
                // 精确匹配
                if (a.equalsIgnoreCase(ss)) return true;
                // 剥离标准答案中的括号注释后匹配（如 "CPU（中央处理器）" → "CPU"）
                String aCore = a.replaceAll("[（(][^）)]*[）)]", "").trim();
                if (!aCore.isEmpty() && aCore.equalsIgnoreCase(ss)) return true;
                // 包含匹配：要求较短字符串长度 ≥ 较长字符串的 90% 才算正确
                // 防止"光合作用" vs "光合作" 之类不完整答案误判
                if (ss.length() >= 2 && aCore.length() >= 2) {
                    int minLen = Math.min(aCore.length(), ss.length());
                    int maxLen = Math.max(aCore.length(), ss.length());
                    if (minLen * 1.0 / maxLen >= 0.9) {
                        if (aCore.contains(ss) || ss.contains(aCore)) return true;
                    }
                }
            }
            // 反方向：用学生答案剥离括号后与标准答案匹配
            String ssCore = ss.replaceAll("[（(][^）)]*[）)]", "").trim();
            if (!ssCore.equals(ss) && !ssCore.isEmpty()) {
                for (String alt : alts) {
                    String a = alt.trim();
                    if (!a.isEmpty() && a.equalsIgnoreCase(ssCore)) return true;
                }
            }
            return false;
        }
        return sc.equalsIgnoreCase(ss);
    }

    /** 规范化学生答案：去前后空格、去结尾标点、提取纯字母 */
    private static String normalizeStudentAnswer(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        // 去除结尾标点: A. A、 A) A） . 。 、 )
        s = s.replaceAll("[.。、)）]$", "").trim();
        // 如果是 "选项A" / "答案B" 格式，提取字母
        if (s.length() >= 2 && (s.startsWith("选项") || s.startsWith("答案"))) {
            String letter = s.substring(2).trim();
            if (letter.length() == 1 && Character.isLetter(letter.charAt(0)))
                return letter.toUpperCase();
        }
        return s;
    }

    /** 根据任务类型推断错题来源标签 */
    private String inferSourceFromCategory(String taskType) {
        if (taskType == null) return null;
        return switch (taskType) {
            case "FORMATIVE", "SUMMATIVE" -> "EXAM";
            case "PRE_CLASS", "IN_CLASS", "AFTER_CLASS", "MORAL", "LABOR" -> "HOMEWORK";
            default -> null;
        };
    }

    private static String sortTokens(String s) {
        // 先去掉所有分隔符(逗号/顿号/空格)，再逐字母排序
        String cleaned = s.replaceAll("[,，、\\s]", "");
        char[] chars = cleaned.toUpperCase().toCharArray();
        Arrays.sort(chars);
        return new String(chars);
    }

    /** 批量 upsert wrong_questions：利用唯一索引 + INSERT ON DUPLICATE KEY UPDATE 原子操作 */
    private void batchUpsertWrong(List<WrongQuestion> list) {
        if (list.isEmpty()) return;
        // 分批写入，避免单次 SQL 过长
        List<WrongQuestion> batch = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            batch.add(list.get(i));
            if (batch.size() >= 50 || i == list.size() - 1) {
                wrongQuestionMapper.batchUpsert(batch);
                batch.clear();
            }
        }
    }

    /**
     * 将学生答案归一化到当前题目的合法选项字母范围内。
     * 场景：AI 生成了 >4 个选项（A-F），修复后只剩 4 个（A-D）。
     * 学生答案中超出选项范围的字母被剥离，避免误判。
     */
    private String normalizeAnswerToOptions(String studentAnswer, String optionsJson, String questionType) {
        if (studentAnswer == null || studentAnswer.isBlank()) return "";
        if (optionsJson == null || optionsJson.isBlank()) return studentAnswer;

        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.List<?> opts = om.readValue(optionsJson, java.util.List.class);
            int validCount = opts != null ? opts.size() : 0;
            if (validCount == 0) return studentAnswer;

            // 选项数已确定，剥离学生答案中超出范围的字母
            char maxLetter = (char) ('A' + validCount - 1);
            if ("MULTI_CHOICE".equals(questionType)) {
                // 多选: "A,B,C,D,E,F" 或 "ABCD" → 只保留 ≤maxLetter 的字母
                String cleaned = studentAnswer.trim().toUpperCase();
                // 无分隔符的连续字母(如"ABC")拆分为单个字母
                if (cleaned.matches("^[A-Z]{2,}$")) {
                    cleaned = String.join(",", cleaned.split(""));
                }
                String[] parts = cleaned.split("[,，、\\s]+");
                StringBuilder sb = new StringBuilder();
                for (String p : parts) {
                    String letter = p.trim().toUpperCase();
                    if (letter.length() == 1 && letter.charAt(0) >= 'A' && letter.charAt(0) <= maxLetter) {
                        if (!sb.isEmpty()) sb.append(",");
                        sb.append(letter);
                    }
                }
                return sb.toString();
            } else {
                // 单选: 超范围的答案直接返回原值（在 answersMatch 中自然判错）
                // 不再修正为 maxLetter，防止误判正确
                return studentAnswer;
            }
        } catch (Exception e) {
            return studentAnswer;
        }
    }

    /**
     * 重新评分指定提交 — 用于修正因题目数据变更（如选项数修正）导致的误判。
     * 重新加载每题的最新数据，重新判分，更新 student_answers + task_submissions + wrong_questions。
     * 仅处理客观题（SINGLE_CHOICE/MULTI_CHOICE/TRUE_FALSE/FILL_IN），主观题保持原判。
     */
    @Transactional
    public Map<String, Object> regradeSubmission(Long submissionId) {
        TaskSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) throw new BusinessException(404, "提交记录不存在");
        if (!"SUBMITTED".equals(sub.getStatus()) && !"GRADED".equals(sub.getStatus()))
            throw new BusinessException(400, "仅已提交或已评分的记录可重新评分");

        // 加载该提交的所有答题记录
        List<StudentAnswer> existingAnswers = answerMapper.selectList(
            new LambdaQueryWrapper<StudentAnswer>()
                .eq(StudentAnswer::getSubmissionId, submissionId));
        if (existingAnswers.isEmpty()) return Map.of("message", "无答题记录", "changed", 0);

        // 批量加载题目数据
        Set<Long> qIds = existingAnswers.stream().map(StudentAnswer::getQuestionId).collect(Collectors.toSet());
        Map<Long, QuestionBank> qMap = questionBankMapper.selectBatchIds(qIds).stream()
            .collect(Collectors.toMap(QuestionBank::getId, q -> q, (a, b) -> a));

        // 加载任务题目（获取每题分值）
        List<TaskQuestion> tqList = taskQuestionMapper.selectList(
            new LambdaQueryWrapper<TaskQuestion>().eq(TaskQuestion::getTaskId, sub.getTaskId()));
        Map<Long, TaskQuestion> tqMap = tqList.stream()
            .collect(Collectors.toMap(TaskQuestion::getQuestionId, tq -> tq, (a, b) -> a));

        BigDecimal newTotal = BigDecimal.ZERO;
        int changed = 0;
        List<WrongQuestion> toAdd = new ArrayList<>();
        List<Long> toRemoveWrong = new ArrayList<>();

        for (StudentAnswer sa : existingAnswers) {
            QuestionBank q = qMap.get(sa.getQuestionId());
            if (q == null) continue;

            // 仅重评客观题
            if (!OBJECTIVE_TYPES.contains(q.getQuestionType())) {
                if (sa.getAutoScore() != null) newTotal = newTotal.add(sa.getAutoScore());
                continue;
            }

            String studentAns = sa.getStudentAnswer() != null ? sa.getStudentAnswer() : "";
            // 关键修复：题目选项已从>4缩减到≤4，学生答案中超出A-D的字母需剥离
            studentAns = normalizeAnswerToOptions(studentAns, q.getOptions(), q.getQuestionType());
            // 同样修正教师正确答案（存量数据可能仍有E/F等超出范围的答案字母）
            String correctAns = normalizeAnswerToOptions(q.getCorrectAnswer(), q.getOptions(), q.getQuestionType());
            TaskQuestion tq = tqMap.get(sa.getQuestionId());
            BigDecimal maxScore = tq != null && tq.getScore() != null ? tq.getScore() : BigDecimal.ONE;

            boolean nowCorrect = answersMatch(q.getQuestionType(), correctAns, studentAns);
            int oldIsCorrect = sa.getIsCorrect() != null ? sa.getIsCorrect() : 0;
            int newIsCorrect = nowCorrect ? 1 : 0;

            if (oldIsCorrect != newIsCorrect) {
                changed++;
                sa.setIsCorrect(newIsCorrect);
                sa.setAutoScore(nowCorrect ? maxScore : BigDecimal.ZERO);
                answerMapper.updateById(sa);

                if (nowCorrect) {
                    // 之前判错现在判对 → 从错题本移除
                    toRemoveWrong.add(sa.getQuestionId());
                } else {
                    // 之前判对现在判错 → 收录错题本
                    WrongQuestion wq = new WrongQuestion();
                    wq.setStudentId(sub.getStudentId());
                    wq.setQuestionId(q.getId());
                    wq.setWrongCount(1);
                    wq.setLastWrongTime(LocalDateTime.now());
                    wq.setIsMastered(0);
                    toAdd.add(wq);
                }
            }

            if (nowCorrect) newTotal = newTotal.add(maxScore);
        }

        // 错题本变更
        if (!toRemoveWrong.isEmpty()) {
            for (Long qid : toRemoveWrong) {
                wrongQuestionMapper.delete(new LambdaQueryWrapper<WrongQuestion>()
                    .eq(WrongQuestion::getStudentId, sub.getStudentId())
                    .eq(WrongQuestion::getQuestionId, qid));
            }
        }
        if (!toAdd.isEmpty()) batchUpsertWrong(toAdd);

        // 更新提交总分
        if (changed > 0) {
            Task task = taskMapper.selectById(sub.getTaskId());
            if (task.getTotalScore() != null && newTotal.compareTo(task.getTotalScore()) > 0) {
                newTotal = task.getTotalScore();
            }
            sub.setScore(newTotal);
            sub.setObjectiveScore(newTotal);
            // 如果已评分，保留教师评分状态但更新客观分
            if ("GRADED".equals(sub.getStatus())) {
                sub.setGradingMessage("客观题已重新评分(" + newTotal + "分)，" + changed + "题结果变更");
            }
            submissionMapper.updateById(sub);
        }

        log.info("重评分完成: submissionId={}, changed={}, newTotal={}", submissionId, changed, newTotal);
        return Map.of("submissionId", submissionId, "changed", changed, "newTotal", newTotal,
            "message", changed > 0 ? "已修正" + changed + "道题的评分，新总分" + newTotal : "无需修正，评分结果无变化");
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.school.teaching.mapper.TaskMapper taskMapper;

    /** 更新掌握度（加权移动平均，减少近因偏差） */
    private void updatePrecisionProgress(Long studentId, Long nodeId, String subject, boolean isCorrect) {
        if (studentId == null || nodeId == null) return;
        PrecisionProgress pp = progressMapper.selectOne(
            new LambdaQueryWrapper<PrecisionProgress>()
                .eq(PrecisionProgress::getStudentId, studentId)
                .eq(PrecisionProgress::getNodeId, nodeId));
        if (pp == null) {
            pp = new PrecisionProgress();
            pp.setStudentId(studentId);
            pp.setNodeId(nodeId);
            pp.setSubject(subject != null && subject.length() > 50 ? subject.substring(0, 50) : subject);
            pp.setStatus("learning");
            pp.setMasteryPercent(BigDecimal.ZERO);
            pp.setTotalAttempts(0);
            pp.setTotalCorrect(0);
        }
        int oldMastery = pp.getMasteryPercent() != null ? pp.getMasteryPercent().intValue() : 0;
        // 加权移动平均：减少单次答题对掌握度的影响，避免近因偏差
        // weight 随答题次数增加而增大：N=1→0.12, N=20→0.5
        int totalAttempts = pp.getTotalAttempts() != null ? pp.getTotalAttempts() : 0;
        double weight = Math.min(0.5, 0.1 + totalAttempts * 0.02);
        double currentScore = isCorrect ? 100.0 : 0.0;
        int newMastery = (int) Math.max(0, Math.min(100, Math.round(weight * currentScore + (1 - weight) * oldMastery)));
        pp.setMasteryPercent(BigDecimal.valueOf(newMastery));
        pp.setTotalAttempts((pp.getTotalAttempts() != null ? pp.getTotalAttempts() : 0) + 1);
        if (isCorrect) {
            pp.setTotalCorrect((pp.getTotalCorrect() != null ? pp.getTotalCorrect() : 0) + 1);
        }
        pp.setLastPracticeAt(LocalDateTime.now());
        if (newMastery >= 80) pp.setStatus("mastered");
        else if (newMastery >= 40) pp.setStatus("learning");
        else pp.setStatus("weak");
        if (pp.getId() == null) progressMapper.insert(pp);
        else progressMapper.updateById(pp);
    }

    /** 检查该生该任务是否已有任意轮次达标 */
    private boolean hasAnyAttemptPassed(Long taskId, Long studentId, Task task, BigDecimal total) {
        List<TaskSubmission> allSubs = submissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getTaskId, taskId)
                .eq(TaskSubmission::getStudentId, studentId)
                .in(TaskSubmission::getStatus, "SUBMITTED", "GRADED"));
        return allSubs.stream().anyMatch(s -> {
            if (s.getScore() == null || total.compareTo(BigDecimal.ZERO) <= 0) return false;
            // 与 onSubmit 保持一致：根据 passMode 选择判定分数
            BigDecimal jScore;
            if ("all".equals(task.getPassMode())) {
                jScore = s.getScore();
            } else {
                jScore = s.getObjectiveScore() != null ? s.getObjectiveScore() : s.getScore();
            }
            double rate = jScore != null ? jScore.doubleValue() / total.doubleValue() * 100 : 0;
            // 优先使用该提交创建时的 passRate 快照
            int rateAtTime = extractPassRateFromScoreJson(s.getScoreJson());
            if (rateAtTime <= 0) rateAtTime = task.getPassRate();
            return rate >= rateAtTime;
        });
    }

    /** 从 scoreJson 中提取 passRate 快照值 */
    private int extractPassRateFromScoreJson(String scoreJson) {
        if (scoreJson == null || scoreJson.isBlank()) return 0;
        try {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> meta = new ObjectMapper()
                .readValue(scoreJson, java.util.Map.class);
            Object val = meta.get("passRateAtCreation");
            if (val instanceof Number n) return n.intValue();
        } catch (Exception ignored) {
            log.warn("passRateAtCreation 解析失败，返回 0");
        }
        return 0;
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }
}

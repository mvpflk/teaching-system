package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.QuestionBank;
import com.school.teaching.entity.Task;
import com.school.teaching.entity.TaskQuestion;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.QuestionBankMapper;
import com.school.teaching.mapper.TaskMapper;
import com.school.teaching.mapper.TaskQuestionMapper;
import com.school.teaching.service.SubmitLockService;
import com.school.teaching.service.TaskQuestionService;
import com.school.teaching.utils.JsonUtils;
import com.school.teaching.utils.ScoreUtils;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskQuestionServiceImpl implements TaskQuestionService {

    private final TaskMapper taskMapper;
    private final TaskQuestionMapper taskQuestionMapper;
    private final QuestionBankMapper questionBankMapper;
    private final SubmitLockService submitLockService;
    private final SqlSessionFactory sqlSessionFactory;

    private static final Logger log = LoggerFactory.getLogger(TaskQuestionServiceImpl.class);

    @Override
    public void batchSaveQuestions(List<TaskQuestion> questions) {
        if (questions == null || questions.isEmpty()) return;
        try (SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            TaskQuestionMapper batchMapper = sqlSession.getMapper(TaskQuestionMapper.class);
            for (TaskQuestion tq : questions) {
                batchMapper.insert(tq);
            }
            sqlSession.commit();
        }
    }

    @Override
    public List<TaskQuestion> getQuestions(Long taskId) {
        return taskQuestionMapper.selectList(
            new LambdaQueryWrapper<TaskQuestion>().eq(TaskQuestion::getTaskId, taskId)
                .orderByAsc(TaskQuestion::getSortOrder));
    }

    @Override
    public List<Map<String, Object>> getQuestionsWithDetails(Long taskId) {
        List<TaskQuestion> tqList = getQuestions(taskId);
        if (tqList.isEmpty()) return java.util.List.of();

        List<Long> qbIds = tqList.stream().map(TaskQuestion::getQuestionId)
                .filter(id -> id != null && id > 0).distinct().toList();
        Map<Long, QuestionBank> qbMap = qbIds.isEmpty() ? java.util.Map.of()
                : questionBankMapper.selectBatchIds(qbIds).stream()
                    .collect(Collectors.toMap(QuestionBank::getId, q -> q));

        List<Map<String, Object>> result = new ArrayList<>();
        int skippedDeleted = 0;
        int skippedMissing = 0;
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        for (TaskQuestion tq : tqList) {
            QuestionBank qb = qbMap.get(tq.getQuestionId());
            if (qb != null && qb.getStatus() != null && qb.getStatus() == -1) {
                skippedDeleted++;
                continue;
            }
            if (qb == null) {
                skippedMissing++;
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", tq.getId());
            item.put("taskId", tq.getTaskId());
            item.put("questionId", tq.getQuestionId());
            item.put("questionType", tq.getQuestionType());
            item.put("sortOrder", tq.getSortOrder());
            item.put("score", tq.getScore());
            item.put("questionText", qb.getQuestionText());
            item.put("correctAnswer", qb.getCorrectAnswer());
            try {
                item.put("options", qb.getOptions() != null ? om.readValue(qb.getOptions(), java.util.List.class) : java.util.List.of());
            } catch (Exception e) { item.put("options", java.util.List.of()); }
            item.put("explanation", qb.getExplanation());
            item.put("difficultyLevel", qb.getDifficultyLevel());
            result.add(item);
        }
        if (skippedDeleted > 0) {
            log.warn("任务{}题目详情中跳过了{}道已删除题目", taskId, skippedDeleted);
        }
        if (skippedMissing > 0) {
            log.warn("任务{}题目详情中跳过了{}道孤儿引用(question_bank中不存在)", taskId, skippedMissing);
        }
        return result;
    }

    @Override
    @Transactional
    @CacheEvict(value = "task_list", allEntries = true)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addQuestions(Long taskId, List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) return;
        var editLock = submitLockService.tryLockTaskEdit(taskId);
        if (editLock == null) throw new BusinessException(409, "任务正在被其他操作编辑，请稍后重试");
        try {
            List<Long> ids = new ArrayList<>();
            for (Object v : (List) questionIds) ids.add(((Number) v).longValue());
            Task task = taskMapper.selectById(taskId);
            Map<String, Integer> presets = task != null ? task.getScorePresets() : null;
            TaskQuestion last = taskQuestionMapper.selectOne(
                new LambdaQueryWrapper<TaskQuestion>().eq(TaskQuestion::getTaskId, taskId)
                    .orderByDesc(TaskQuestion::getSortOrder).last("LIMIT 1"));
            int sort = (last != null && last.getSortOrder() != null) ? last.getSortOrder() + 1 : 0;
            Map<Long, QuestionBank> qMap = questionBankMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(QuestionBank::getId, q -> q));
            for (Long qid : ids) {
                QuestionBank q = qMap.get(qid);
                if (q == null) continue;
                TaskQuestion tq = new TaskQuestion();
                tq.setTaskId(taskId);
                tq.setQuestionId(qid);
                tq.setQuestionType(q.getQuestionType());
                tq.setSortOrder(sort++);
                tq.setScore(scoreFromPresets(q.getQuestionType(), presets));
                taskQuestionMapper.insert(tq);
            }
            recalcTaskTotalScore(taskId);
        } finally {
            editLock.close();
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "task_list", allEntries = true)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void removeQuestions(Long taskId, List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) return;
        var editLock = submitLockService.tryLockTaskEdit(taskId);
        if (editLock == null) throw new BusinessException(409, "任务正在被其他操作编辑，请稍后重试");
        try {
            List<Long> ids = new ArrayList<>();
            for (Object v : (List) questionIds) ids.add(((Number) v).longValue());
            taskQuestionMapper.delete(new LambdaQueryWrapper<TaskQuestion>()
                .eq(TaskQuestion::getTaskId, taskId)
                .in(TaskQuestion::getQuestionId, ids));
            recalcTaskTotalScore(taskId);
        } finally {
            editLock.close();
        }
    }

    @Override
    public void recalcTaskTotalScore(Long taskId) {
        List<TaskQuestion> allQ = taskQuestionMapper.selectList(
            new LambdaQueryWrapper<TaskQuestion>().eq(TaskQuestion::getTaskId, taskId));
        BigDecimal total = allQ.stream()
            .map(q -> q.getScore() != null ? q.getScore() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        Task task = new Task();
        task.setId(taskId);
        task.setTotalScore(total);
        taskMapper.updateById(task);
    }

    @Override
    public List<Map<String, Object>> getStudentQuestions(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        Map<String, Object> config = task != null ? parseConfig(task.getTaskConfig()) : Map.of();
        boolean shuffleQuestions = Boolean.TRUE.equals(config.get("shuffleQuestions"))
            || Boolean.TRUE.equals(config.get("shuffle_questions"));
        boolean randomizeOptions = Boolean.TRUE.equals(config.get("randomizeOptions"))
            || Boolean.TRUE.equals(config.get("randomize_options"))
            || Boolean.TRUE.equals(config.get("shuffleOptions"));

        List<TaskQuestion> tqList = getQuestions(taskId);
        if (tqList.isEmpty()) return List.of();
        Set<Long> qIds = tqList.stream().map(TaskQuestion::getQuestionId).collect(Collectors.toSet());
        Map<Long, QuestionBank> qMap = questionBankMapper.selectBatchIds(qIds).stream()
            .collect(Collectors.toMap(QuestionBank::getId, q -> q));
        List<Map<String, Object>> result = new ArrayList<>();
        for (TaskQuestion tq : tqList) {
            QuestionBank q = qMap.get(tq.getQuestionId());
            if (q == null) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", tq.getQuestionId());
            item.put("questionType", tq.getQuestionType());
            item.put("questionText", q.getQuestionText());
            // 仅选择题类需要归一化选项格式（MATCHING/DRAG_SORT 等保留原始对象格式）
            String rawOpts = q.getOptions();
            String qt = q.getQuestionType();
            boolean isChoiceType = "SINGLE_CHOICE".equals(qt) || "MULTI_CHOICE".equals(qt) || "TRUE_FALSE".equals(qt);
            String processedOpts = isChoiceType ? normalizeOptionsFormat(rawOpts) : rawOpts;
            if (randomizeOptions && !"TRUE_FALSE".equals(qt)) {
                String shuffled = randomizeOptionOrder(processedOpts);
                item.put("options", shuffled);
                item.put("optionMapping", buildOptionMapping(processedOpts, shuffled));
            } else {
                item.put("options", processedOpts);
            }
            item.put("score", tq.getScore());
            result.add(item);
        }
        if (shuffleQuestions) Collections.shuffle(result, new Random(System.nanoTime()));
        return result;
    }

    @Override
    public void fixQuestionStatus(List<Map<String, Object>> questions, String subject, Long schoolId) {
        for (Map<String, Object> q : questions) {
            Object qidObj = q.get("id");
            if (qidObj instanceof Number) {
                try {
                    QuestionBank qb = questionBankMapper.selectById(((Number) qidObj).longValue());
                    if (qb != null) {
                        qb.setStatus(1);
                        if (qb.getSubject() == null || qb.getSubject().isEmpty()) qb.setSubject(subject);
                        if (qb.getSchoolId() == null) qb.setSchoolId(schoolId);
                        questionBankMapper.updateById(qb);
                    }
                } catch (Exception e) {
                    log.warn("修复题目 {} 状态失败: {}", qidObj, e.getMessage());
                }
            }
        }
    }

    private int[] buildOptionMapping(String originalOptionsJson, String shuffledOptionsJson) {
        if (originalOptionsJson == null || shuffledOptionsJson == null) return new int[0];
        try {
            List<Object> orig = JsonUtils.MAPPER.readValue(originalOptionsJson,
                new com.fasterxml.jackson.core.type.TypeReference<List<Object>>() {});
            List<Object> shuffled = JsonUtils.MAPPER.readValue(shuffledOptionsJson,
                new com.fasterxml.jackson.core.type.TypeReference<List<Object>>() {});
            int[] mapping = new int[shuffled.size()];
            boolean[] used = new boolean[orig.size()];
            for (int i = 0; i < shuffled.size(); i++) {
                Object target = shuffled.get(i);
                for (int j = 0; j < orig.size(); j++) {
                    if (!used[j] && target.equals(orig.get(j))) {
                        mapping[i] = j;
                        used[j] = true;
                        break;
                    }
                }
            }
            return mapping;
        } catch (Exception e) { return new int[0]; }
    }

    /**
     * 归一化选项格式：将 [{"key":"A","text":"..."}, ...] 对象数组
     * 统一转为 ["A. 文本", "B. 文本", ...] 字符串数组 JSON。
     * 已是字符串数组格式的则原样返回。
     */
    static String normalizeOptionsFormat(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) return optionsJson;
        try {
            Object parsed = JsonUtils.MAPPER.readValue(optionsJson, Object.class);
            if (!(parsed instanceof List<?> list) || list.isEmpty()) return optionsJson;

            // 检查是否有对象格式的元素
            boolean hasObject = list.stream().anyMatch(e -> e instanceof Map);
            if (!hasObject) return optionsJson; // 已经是字符串数组，不用转换

            // 转换对象格式为 "A. 文本" 格式
            List<String> result = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                if (item instanceof Map<?, ?> m) {
                    String text = m.containsKey("text") ? String.valueOf(m.get("text"))
                            : m.containsKey("label") ? String.valueOf(m.get("label"))
                            : m.containsKey("value") ? String.valueOf(m.get("value"))
                            : String.valueOf(item);
                    String key = m.containsKey("key") ? String.valueOf(m.get("key"))
                            : String.valueOf((char) ('A' + i));
                    result.add(key + ". " + text);
                } else {
                    result.add(String.valueOf(item));
                }
            }
            return JsonUtils.toJson(result);
        } catch (Exception e) {
            log.warn("选项格式归一化失败: {}", e.getMessage());
            return optionsJson;
        }
    }

    private String randomizeOptionOrder(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) return optionsJson;
        try {
            if (optionsJson.trim().startsWith("[")) {
                List<Object> list = JsonUtils.MAPPER.readValue(optionsJson,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Object>>() {});
                Collections.shuffle(list);
                return JsonUtils.toJson(list);
            }
            Map<String, Object> opts = JsonUtils.parseMap(optionsJson);
            if (opts.isEmpty() || opts.containsKey("raw")) return optionsJson;
            List<String> keys = new ArrayList<>(opts.keySet());
            Collections.shuffle(keys);
            Map<String, Object> shuffled = new LinkedHashMap<>();
            for (String k : keys) shuffled.put(k, opts.get(k));
            return JsonUtils.toJson(shuffled);
        } catch (Exception e) { return optionsJson; }
    }

    private static BigDecimal scoreFromPresets(String questionType, Map<String, Integer> presets) {
        return ScoreUtils.scoreFromPresets(questionType, presets);
    }

    private Map<String, Object> parseConfig(String json) {
        if (json == null || json.isBlank()) return Map.of();
        return JsonUtils.parseMap(json);
    }
}
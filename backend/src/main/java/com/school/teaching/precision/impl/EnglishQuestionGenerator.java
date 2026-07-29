package com.school.teaching.precision.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.KnowledgeNode;
import com.school.teaching.entity.QuestionBank;
import com.school.teaching.mapper.KnowledgeNodeMapper;
import com.school.teaching.mapper.PrecisionEnglishReadingPassageMapper;
import com.school.teaching.mapper.QuestionBankMapper;
import com.school.teaching.service.impl.DeepSeekGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnglishQuestionGenerator {

    private final DeepSeekGateway deepSeekGateway;
    private final QuestionBankMapper questionMapper;
    private final KnowledgeNodeMapper nodeMapper;
    private final PrecisionEnglishReadingPassageMapper passageMapper;
    private static final ObjectMapper OM = new ObjectMapper();

    @Async("aiExecutor")
    public void generateGrammarQuestions(Long nodeId, int count) {
        try {
            KnowledgeNode node = nodeMapper.selectById(nodeId);
            if (node == null) return;
            String prompt = String.format("""
                请生成 %d 道关于"%s"的英语语法选择题（中职对口升学难度）。
                每道题包含：题干(questionText)、4个选项A/B/C/D(options数组)、正确答案(answer)、解析(explanation)。
                题干中的动词请使用以下基础词汇：go, have, study, play, finish, help, like, want, need, make。
                所有题目输出为 JSON 数组格式：[{"questionText":"...","options":["A. go","B. goes"...],"answer":"B","explanation":"..."}]

                【答案自审】生成前请逐题确认：正确答案语法正确、与题干时态/人称一致，如有不确定请在explanation末尾标注（待复核）。""",
                count, node.getName());
            String aiResponse = deepSeekGateway.generateContent(Map.of("prompt", prompt, "temperature", 0.5, "maxTokens", 2000));
            if (aiResponse == null || aiResponse.isBlank()) return;
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> questions = parseAiQuestions(aiResponse);
            int inserted = 0;
            for (Map<String, Object> q : questions) {
                // 字段兼容(questionText/stem/question) + null 安全，避免存字面量 "null"
                String stem = com.school.teaching.service.impl.DeepSeekResponseParser.stemOf(q);
                Object ansObj = com.school.teaching.service.impl.DeepSeekResponseParser.answerOf(q);
                String answer = ansObj != null ? String.valueOf(ansObj).trim() : "";
                // 有效性过滤：题干或答案为空则跳过，不落脏数据(草稿池)
                if (stem == null || stem.isBlank() || answer.isBlank()) {
                    log.warn("英语语法题无效(题干/答案为空)，跳过 nodeId={}", nodeId);
                    continue;
                }
                QuestionBank qb = new QuestionBank();
                qb.setSubject("英语[职高]"); qb.setQuestionType("SINGLE_CHOICE");
                qb.setQuestionText(stem);
                // 选项：null→空数组(不写字面量 "null")，剥离 A. 前缀，截断到 4
                java.util.List<String> opts = new java.util.ArrayList<>();
                if (q.get("options") instanceof java.util.List<?> rawOpts) {
                    for (Object o : rawOpts) opts.add(com.school.teaching.service.impl.DeepSeekResponseParser.stripOptionPrefix(String.valueOf(o)));
                    if (opts.size() > 4) { log.warn("英语语法题选项数>4({}), 截断为前4个", opts.size()); opts = opts.subList(0, 4); }
                }
                qb.setOptions(OM.writeValueAsString(opts));
                qb.setCorrectAnswer(answer);
                String expl = q.get("explanation") != null ? String.valueOf(q.get("explanation")) : "";
                if (expl.contains("待复核")) log.warn("AI英语题自审待复核: {}", stem);
                // 答案字母必须在选项范围内（4选项→仅限A-D）
                if (answer.length() == 1 && answer.charAt(0) > 'D' && answer.charAt(0) <= 'Z') {
                    log.warn("英语语法题答案超出范围: answer={}, 标记待审核", answer);
                    expl = (expl.isEmpty() ? "" : expl + " ") + "【答案可能异常，请教师审核】";
                }
                qb.setExplanation(expl);
                qb.setGrammarNodeId(nodeId);
                qb.setStatus(0); qb.setVersion(1); qb.setIsLatest(1);
                qb.setDifficultyLevel(1);
                questionMapper.insert(qb);
                inserted++;
            }
            log.info("AI 生成 {} 道语法题 nodeId={}", inserted, nodeId);
        } catch (Exception e) { log.error("AI 语法题生成失败 nodeId={}", nodeId, e); }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseAiQuestions(String response) {
        try {
            int start = response.indexOf('['), end = response.lastIndexOf(']');
            if (start >= 0 && end > start) response = response.substring(start, end + 1);
            return OM.readValue(response, List.class);
        } catch (Exception e) { log.warn("AI 问题解析失败"); return List.of(); }
    }
}

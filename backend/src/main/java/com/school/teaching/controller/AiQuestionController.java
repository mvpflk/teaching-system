package com.school.teaching.controller;

import com.school.teaching.common.R;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.AiQuestionGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI 出题 Controller — 衍生练习、针对性补强
 */
@Slf4j
@RestController
@RequestMapping("/ai/questions")
@RequiredArgsConstructor
public class AiQuestionController {

    private final AiQuestionGeneratorService aiQuestionGeneratorService;

    /** 针对薄弱知识点生成 AI 衍生练习 */
    @PostMapping("/remedial")
    public R<?> remedialQuestions(@RequestBody Map<String, Object> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        @SuppressWarnings("unchecked")
        List<Number> rawIds = (List<Number>) body.get("knowledgeNodeIds");
        String subject = (String) body.get("subject");
        if (rawIds == null || rawIds.isEmpty()) {
            return R.error(400, "请提供薄弱知识点");
        }
        List<Integer> knowledgeNodeIds = rawIds.stream().map(Number::intValue).toList();
        Map<String, Object> result = aiQuestionGeneratorService.generateRemedial(userId, knowledgeNodeIds, subject);
        return R.ok(result);
    }
}

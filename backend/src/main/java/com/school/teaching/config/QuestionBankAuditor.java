package com.school.teaching.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.QuestionBank;
import com.school.teaching.mapper.QuestionBankMapper;
import com.school.teaching.service.impl.QuestionQualityValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 题库质量启动审计 — 服务器启动时扫描全部已发布的选择题，
 * 运行 QualityValidator 规则引擎，发现选项格式/答案异常等潜在问题并 log.warn。
 *
 * <p>不影响启动流程，发现问题仅报警不阻塞。</p>
 *
 * @since 2026-07-26 (R122+hotfix)
 */
@Slf4j
@Component
public class QuestionBankAuditor implements CommandLineRunner {

    private final QuestionBankMapper questionBankMapper;

    public QuestionBankAuditor(QuestionBankMapper questionBankMapper) {
        this.questionBankMapper = questionBankMapper;
    }

    @Override
    public void run(String... args) {
        log.info("题库质量审计开始 — 扫描全部已发布的选择题...");

        // 查询所有已发布的选择题
        List<QuestionBank> questions = questionBankMapper.selectList(
                new LambdaQueryWrapper<QuestionBank>()
                        .eq(QuestionBank::getStatus, 1)
                        .in(QuestionBank::getQuestionType,
                                "SINGLE_CHOICE", "MULTI_CHOICE", "TRUE_FALSE")
                        .orderByAsc(QuestionBank::getId));

        int total = questions.size();
        int issueCount = 0;
        int formatIssueCount = 0;

        for (QuestionBank qb : questions) {
            List<QuestionQualityValidator.Issue> issues = QuestionQualityValidator.validate(qb);
            if (!issues.isEmpty()) {
                issueCount++;
                boolean hasFormat = issues.stream()
                        .anyMatch(i -> "options".equals(i.field())
                                && i.message().contains("对象数组"));
                if (hasFormat) formatIssueCount++;

                for (QuestionQualityValidator.Issue issue : issues) {
                    log.warn("题库审计: qid={} type={} field={} msg=\"{}\" 题干=\"{}\"",
                            qb.getId(), qb.getQuestionType(), issue.field(), issue.message(),
                            truncate(qb.getQuestionText(), 60));
                }
            }
        }

        if (issueCount > 0) {
            log.warn("题库审计完成: 共{}题, 发现{}题有潜在问题(其中{}题选项格式异常)",
                    total, issueCount, formatIssueCount);
        } else {
            log.info("题库审计完成: 共{}题, 全部通过规则检查 ✅", total);
        }
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}

package com.school.teaching.common.handler;

import com.school.teaching.common.QuestionTypeEnum;
import com.school.teaching.common.QuestionTypeHandler;
import com.school.teaching.entity.QuestionBank;
import com.school.teaching.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 主观题统一处理器 — 覆盖 SHORT_ANSWER/PROGRAMMING/FILE_UPLOAD/AUDIO_VIDEO/ESSAY。
 * scoreAnswer 统一返回 null（需教师手工评分）。
 */
@Component
public class SubjectiveHandler implements QuestionTypeHandler {

    private static final Logger log = LoggerFactory.getLogger(SubjectiveHandler.class);

    private static final Set<QuestionTypeEnum> TYPES = Set.of(
        QuestionTypeEnum.SHORT_ANSWER, QuestionTypeEnum.PROGRAMMING,
        QuestionTypeEnum.FILE_UPLOAD, QuestionTypeEnum.AUDIO_VIDEO, QuestionTypeEnum.ESSAY);

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        ".jpg",".jpeg",".png",".gif",".bmp",".webp",".pdf",
        ".doc",".docx",".xls",".xlsx",".ppt",".pptx",
        ".zip",".rar",".java",".py",".cpp",".c",".js",".ts",".html",".css",
        ".mp3",".mp4",".avi",".mov",".wav");

    @Override
    public QuestionTypeEnum getType() {
        return QuestionTypeEnum.SHORT_ANSWER; // Map注入用，实际通过 supportsTypes
    }

    /** 此 Handler 支持的所有题型 */
    public static Set<QuestionTypeEnum> supportedTypes() { return TYPES; }

    @Override
    public Map<String, Object> renderQuestion(QuestionBank q) {
        Map<String, Object> r = new LinkedHashMap<>();
        String type = q.getQuestionType();
        r.put("type", type != null ? type.toLowerCase() : "essay");
        r.put("text", q.getQuestionText());
        if (QuestionTypeEnum.PROGRAMMING.name().equals(type))
            r.put("codeLang", extractCodeLang(q));
        if (QuestionTypeEnum.FILE_UPLOAD.name().equals(type) || QuestionTypeEnum.AUDIO_VIDEO.name().equals(type))
            r.put("allowedExtensions", ALLOWED_EXTENSIONS);
        return r;
    }

    @Override
    public void validateAnswer(QuestionBank q, Object answer) {
        QuestionTypeEnum type = QuestionTypeEnum.fromString(q.getQuestionType());
        if (type == null) throw new BusinessException(400, "未知题型");

        if (answer == null || answer.toString().trim().isEmpty())
            throw new BusinessException(400, "请填写答案");

        if (type == QuestionTypeEnum.PROGRAMMING && answer.toString().length() > 50000)
            throw new BusinessException(400, "代码超出长度限制(50000字符)");

        if (type == QuestionTypeEnum.ESSAY && answer.toString().length() < 10)
            throw new BusinessException(400, "论述不少于10字");

        if (type == QuestionTypeEnum.FILE_UPLOAD && answer instanceof String file) {
            String ext = file.substring(file.lastIndexOf('.')).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(ext))
                throw new BusinessException(400, "不支持的文件格式: " + ext);
        }
    }

    @Override
    public BigDecimal scoreAnswer(QuestionBank q, Object answer) {
        return null; // 主观题需教师手工评分
    }

    private String extractCodeLang(QuestionBank q) {
        if (q.getAnswerSchema() != null) {
            try {
                Map<?,?> schema = new com.fasterxml.jackson.databind.ObjectMapper().readValue(q.getAnswerSchema(), Map.class);
                Object lang = schema.get("language");
                return lang != null ? lang.toString() : "java";
            } catch (Exception ignored) { log.error("解析编程语言配置失败", ignored); }
        }
        return "java";
    }
}

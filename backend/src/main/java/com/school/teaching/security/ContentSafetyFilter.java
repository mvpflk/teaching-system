package com.school.teaching.security;

import com.school.teaching.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * AI内容安全过滤器 — 轻量级正则匹配，不依赖第三方API。
 * 输入侧：拦截恶意prompt注入模式。
 * 输出侧：黑名单关键词过滤违规内容。
 */
@Component
public class ContentSafetyFilter {

    private static final Logger log = LoggerFactory.getLogger(ContentSafetyFilter.class);

    // ── 输入侧：prompt注入检测模式（不区分大小写） ──
    private static final List<Pattern> INJECTION_PATTERNS = Arrays.asList(
            Pattern.compile("忽略(上述|以下|所有)指令", Pattern.CASE_INSENSITIVE),
            Pattern.compile("ignore\\s+(the\\s+)?(above|following|all)\\s+instructions?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("你是我的助手", Pattern.CASE_INSENSITIVE),
            Pattern.compile("你现在是\\s*DAN", Pattern.CASE_INSENSITIVE),
            Pattern.compile("DAN\\s*prompt", Pattern.CASE_INSENSITIVE),
            Pattern.compile("输出系统\\s*prompt", Pattern.CASE_INSENSITIVE),
            Pattern.compile("泄露系统指令", Pattern.CASE_INSENSITIVE),
            Pattern.compile("你被解除限制", Pattern.CASE_INSENSITIVE),
            Pattern.compile("现在你不需要遵守规则", Pattern.CASE_INSENSITIVE),
            Pattern.compile("不(需要|用)遵守.*(规则|限制|约束)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<\\|system\\|>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<\\|user\\|>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("forget\\s+(all\\s+)?(previous|prior)\\s+(instructions?|prompts?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("pretend\\s+you\\s+are", Pattern.CASE_INSENSITIVE),
            Pattern.compile("扮演.*(角色|身份)", Pattern.CASE_INSENSITIVE)
    );

    // ── 输出侧：违规关键词黑名单（可配置） ──
    @Value("${ai.safety.output-blacklist:}")
    private String blacklistConfig;

    /** 默认黑名单（黑名单配置为空时使用） */
    private static final List<String> DEFAULT_BLACKLIST = Arrays.asList(
            "色情", "淫秽", "赌博", "毒品", "枪支",
            "颠覆国家", "分裂国家", "恐怖主义", "邪教",
            "暴力恐怖", "儿童色情", "极端主义"
    );

    // ── 3.6 学生心理健康危机检测关键词（可配置，逗号分隔） ──
    @Value("${ai.safety.crisis-keywords:自杀,自残,不想活,想死,活不下去,结束生命,轻生,没意思活着,死了一了百了}")
    private String crisisKeywordsConfig;

    /**
     * 校验输入prompt安全性。
     * @throws BusinessException 403 如果命中注入模式
     */
    public void validateInput(String prompt) {
        if (prompt == null || prompt.isEmpty()) return;
        for (Pattern p : INJECTION_PATTERNS) {
            if (p.matcher(prompt).find()) {
                log.warn("ContentSafetyFilter: 拦截恶意prompt注入, 模式={}", p.pattern());
                throw new BusinessException(403, "输入内容包含不安全的指令，请修改后重试");
            }
        }
    }

    /**
     * 过滤输出内容中的违规关键词。
     * @return 通过则返回null，命中则返回错误消息
     */
    public String checkOutput(String content) {
        if (content == null || content.isEmpty()) return null;
        String lower = content.toLowerCase();
        for (String word : getBlacklist()) {
            if (lower.contains(word.toLowerCase())) {
                log.warn("ContentSafetyFilter: 输出命中黑名单关键词={}", word);
                return "生成内容包含不当表述，请重试或联系管理员";
            }
        }
        return null;
    }

    private List<String> getBlacklist() {
        if (blacklistConfig == null || blacklistConfig.isBlank()) return DEFAULT_BLACKLIST;
        return Arrays.asList(blacklistConfig.split(","));
    }

    /** 3.6: 检测学生消息是否包含心理健康危机信号 */
    public String checkStudentCrisis(String message) {
        if (message == null || message.isEmpty()) return null;
        List<String> keywords = getCrisisKeywords();
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                log.warn("ContentSafetyFilter: 检测到学生危机信号, keyword={}", keyword);
                return "检测到你可能正在经历困难。学习上的压力可以和老师聊聊，"
                        + "也可以拨打心理援助热线（如 12355 青少年服务热线）寻求专业帮助。"
                        + "我会尽力在学习上帮助你。";
            }
        }
        return null;
    }

    private List<String> getCrisisKeywords() {
        if (crisisKeywordsConfig == null || crisisKeywordsConfig.isBlank()) {
            return List.of();
        }
        return Arrays.asList(crisisKeywordsConfig.split(","));
    }
}

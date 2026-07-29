package com.school.teaching.controller;

import com.school.teaching.annotation.AuditEventType;
import com.school.teaching.annotation.AuditLog;
import com.school.teaching.common.R;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.AiConfigHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 配置控制器（独立前端页面 views/settings/AiConfig.vue）。
 * 拆分自 SettingsController，路由前缀 /settings/ai-config 保持不变。
 */
@RestController
@RequestMapping("/settings/ai-config")
@io.swagger.v3.oas.annotations.tags.Tag(name = "AI配置", description = "AI Provider 配置与切换")
public class AiConfigController {

    @Autowired private AiConfigHolder aiConfig;

    private static final Map<String, String> PROVIDER_DEFAULTS = new HashMap<>();
    static {
        PROVIDER_DEFAULTS.put("deepseek", "https://api.deepseek.com/v1");
        PROVIDER_DEFAULTS.put("agnes", "https://apihub.agnes-ai.com/v1");
    }
    private static final Map<String, String> PROVIDER_MODELS = new HashMap<>();
    static {
        PROVIDER_MODELS.put("deepseek", "deepseek-v4-pro");
        PROVIDER_MODELS.put("agnes", "agnes-2.0-flash");
    }

    private String configPrefix(String provider) {
        if ("deepseek".equalsIgnoreCase(provider)) return "ai.deepseek";
        if ("agnes".equalsIgnoreCase(provider)) return "ai.agnes";
        if (provider != null && provider.startsWith("custom.")) return "ai." + provider;
        return "ai.deepseek";
    }

    @GetMapping("/providers")
    public R<List<Map<String, String>>> listAiProviders() {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        return R.ok(aiConfig.listProviders());
    }

    @GetMapping
    public R<Map<String, String>> getAiConfig(@RequestParam(defaultValue = "deepseek") String provider) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        String prefix = configPrefix(provider);
        Map<String, String> cfg = new HashMap<>();
        cfg.put("apiKey", aiConfig.getDecrypted(prefix + ".api-key"));
        cfg.put("model", aiConfig.get(prefix + ".model"));
        cfg.put("baseUrl", aiConfig.get(prefix + ".base-url"));
        cfg.put("timeout", String.valueOf(aiConfig.getTimeout(prefix + ".timeout-seconds")));
        cfg.put("provider", aiConfig.getProvider());
        return R.ok(cfg);
    }

    @PutMapping
    @AuditLog(eventType = AuditEventType.PARAM_UPDATE, description = "更新AI配置")
    public R<String> updateAiConfig(@RequestBody Map<String, String> body,
                                    @RequestParam(defaultValue = "deepseek") String provider) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        String prefix = configPrefix(provider);
        String defaultUrl = PROVIDER_DEFAULTS.getOrDefault(provider, "https://api.openai.com/v1");
        String baseUrl = body.getOrDefault("baseUrl", defaultUrl);
        validateAiUrl(baseUrl);  // 2.8: SSRF 防护
        String defaultModel = PROVIDER_MODELS.getOrDefault(provider, "gpt-3.5-turbo");
        Map<String, String> updates = new HashMap<>();
        updates.put(prefix + ".base-url", baseUrl);
        updates.put(prefix + ".model", body.getOrDefault("model", defaultModel));
        String timeout = body.get("timeout");
        if (timeout != null && !timeout.isBlank()) updates.put(prefix + ".timeout-seconds", timeout);
        aiConfig.update(updates);
        String apiKey = body.get("apiKey");
        if (apiKey != null && !apiKey.isBlank()) aiConfig.updateEncrypted(prefix + ".api-key", apiKey);
        return R.ok(provider + " AI配置已更新");
    }

    @PutMapping("/provider")
    @AuditLog(eventType = AuditEventType.PARAM_UPDATE, description = "切换AI Provider")
    public R<String> switchAiProvider(@RequestBody Map<String, String> body) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        String provider = body.get("provider");
        if (provider == null || provider.isBlank()) return R.error(400, "provider 不能为空");
        String prefix = configPrefix(provider);
        String apiKey = aiConfig.getDecrypted(prefix + ".api-key");
        String baseUrl = aiConfig.get(prefix + ".base-url");
        if ((apiKey == null || apiKey.isBlank()) && (baseUrl == null || baseUrl.isBlank())) {
            return R.error(400, "请先配置「" + provider + "」的 API Key 或 Base URL");
        }
        aiConfig.update(Map.of("ai.provider", provider));
        return R.ok("已切换为 " + provider);
    }

    @DeleteMapping("/custom/{name}")
    @AuditLog(eventType = AuditEventType.PARAM_UPDATE, description = "删除自定义AI Provider")
    public R<String> deleteCustomProvider(@PathVariable String name) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        String current = aiConfig.getProvider();
        if (("custom." + name).equals(current)) {
            return R.error(400, "不能删除当前正在使用的 Provider，请先切换到其他 Provider");
        }
        aiConfig.deleteCustomProvider(name);
        return R.ok(name + " 已删除");
    }

    /** 2.8: SSRF 防护 — 校验 AI API 端点 URL，禁止内网地址 + 强制 HTTPS */
    private void validateAiUrl(String url) {
        if (url == null || url.isBlank()) return;  // 允许空值，使用默认 URL
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(400, "API 端点 URL 格式无效: " + url);
        }
        String host = uri.getHost();
        if (host == null) throw new BusinessException(400, "API 端点 URL 缺少主机名");

        // 禁止内网地址
        if (host.equals("localhost") || host.equals("127.0.0.1") || host.startsWith("127.")
                || host.startsWith("10.") || host.startsWith("192.168.")
                || (host.startsWith("172.") && isPrivate172(host))
                || host.startsWith("0.") || host.startsWith("169.254.")
                || host.startsWith("fc") || host.startsWith("fd")  // IPv6 ULA
                || host.equals("[::1]") || host.equals("0.0.0.0")) {
            throw new BusinessException(400, "不允许使用内网地址作为 API 端点");
        }

        // 强制 HTTPS（生产安全要求）
        String scheme = uri.getScheme();
        if (scheme != null && !"https".equalsIgnoreCase(scheme)) {
            throw new BusinessException(400, "API 端点必须使用 HTTPS");
        }
    }

    /** 判断 172.x 是否在私有 B 类范围 (172.16.0.0 – 172.31.255.255) */
    private boolean isPrivate172(String host) {
        try {
            String[] parts = host.split("\\.");
            if (parts.length < 2) return false;
            int second = Integer.parseInt(parts[1]);
            return second >= 16 && second <= 31;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

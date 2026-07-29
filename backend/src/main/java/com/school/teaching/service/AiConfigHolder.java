package com.school.teaching.service;

import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.SystemSettingMapper;
import com.school.teaching.entity.SystemSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.school.teaching.util.EncryptionUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AiConfigHolder {

    private static final Logger log = LoggerFactory.getLogger(AiConfigHolder.class);

    private final ConcurrentHashMap<String, String> config = new ConcurrentHashMap<>();

    @Value("${jwt.secret}") private String jwtSecret;

    @Autowired(required = false)
    private SystemSettingMapper settingMapper;

    private SecretKeySpec aesKey;

    private EncryptionUtil encryptionUtil;

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final Map<String, String> DEFAULTS = new HashMap<>();
    static {
        DEFAULTS.put("ai.provider", "deepseek");
        DEFAULTS.put("ai.deepseek.api-key", "");
        DEFAULTS.put("ai.deepseek.base-url", "https://api.deepseek.com/v1");
        DEFAULTS.put("ai.deepseek.model", "deepseek-v4-pro");
        DEFAULTS.put("ai.deepseek.timeout-seconds", "180");
        DEFAULTS.put("ai.agnes.api-key", "");
        DEFAULTS.put("ai.agnes.base-url", "https://apihub.agnes-ai.com/v1");
        DEFAULTS.put("ai.agnes.model", "agnes-2.0-flash");
        DEFAULTS.put("ai.agnes.timeout-seconds", "120");
    }

    @PostConstruct
    public void init() {
        byte[] keyBytes = new byte[16];
        byte[] src = jwtSecret.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(src, 0, keyBytes, 0, Math.min(src.length, 16));
        aesKey = new SecretKeySpec(keyBytes, "AES");
        this.encryptionUtil = new EncryptionUtil(aesKey);
        DEFAULTS.forEach(config::put);
        if (settingMapper != null) {
            try {
                for (SystemSetting row : settingMapper.selectList(null)) {
                    String k = row.getSettingKey();
                    if (k != null && k.startsWith("ai.")) {
                        String v = row.getSettingValue();
                        if (v != null) config.put(k, v);
                    }
                }
            } catch (Exception ignored) {
                log.warn("首次加载 AI 配置失败，将使用默认值");
            }
        }
    }

    public String get(String key) {
        String v = config.getOrDefault(key, DEFAULTS.getOrDefault(key, ""));
        if (v == null || v.isEmpty()) {
            String envVal = System.getenv(envKeyOf(key));
            if (envVal != null && !envVal.isBlank()) return envVal;
        }
        return v;
    }

    /** API Key 类走解密 */
    public String getDecrypted(String key) {
        String v = get(key);
        if (v == null || v.isEmpty() || v.length() < 20) {
            String envVal = System.getenv(envKeyOf(key));
            if (envVal != null && !envVal.isBlank()) return envVal;
            return v;
        }
        try { return decrypt(v); } catch (Exception e) { return v; }
    }

    /** 配置键 → 环境变量名: 仅去掉开头的 "ai." 前缀, 再将 . 和 - 转 _ 并大写。
     *  例: ai.deepseek.api-key → DEEPSEEK_API_KEY, ai.openai.api-key → OPENAI_API_KEY */
    private static String envKeyOf(String key) {
        String k = key.startsWith("ai.") ? key.substring(3) : key;
        return k.replace(".", "_").replace("-", "_").toUpperCase();
    }

    public int getTimeout() {
        return getTimeout("ai.deepseek.timeout-seconds");
    }

    public int getTimeout(String key) {
        String v = get(key);
        try { return Integer.parseInt(v); } catch (Exception e) { return 60; }
    }

    public String getProvider() {
        return get("ai.provider");
    }

    public void update(Map<String, String> updates) {
        updates.forEach((k, v) -> { if (v != null) config.put(k, v); });
        if (settingMapper != null) {
            updates.forEach((k, v) -> {
                try {
                    SystemSetting row = new SystemSetting();
                    row.setSettingKey(k);
                    row.setSettingValue(v != null ? v : "");
                    SystemSetting existing = settingMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SystemSetting>()
                            .eq(SystemSetting::getSettingKey, k));
                    if (existing != null) { existing.setSettingValue(v); settingMapper.updateById(existing); }
                    else settingMapper.insert(row);
                } catch (Exception ignored) {
                    log.warn("AI 配置写入 DB 失败，仅更新内存缓存: key={}", k);
                }
            });
        }
    }

    /** 返回所有已配置的 provider 列表（内置 + 自定义） */
    public List<Map<String, String>> listProviders() {
        List<Map<String, String>> providers = new ArrayList<>();

        // 内置
        providers.add(Map.of("key", "deepseek", "label", "DeepSeek",
            "baseUrl", get("ai.deepseek.base-url"), "model", get("ai.deepseek.model")));
        providers.add(Map.of("key", "agnes", "label", "Agnes.ai",
            "baseUrl", get("ai.agnes.base-url"), "model", get("ai.agnes.model")));

        // 扫描自定义 provider
        String currentPrefix = null;
        for (String k : config.keySet()) {
            if (k.startsWith("ai.custom.") && k.endsWith(".api-key")) {
                String name = k.substring("ai.custom.".length(), k.length() - ".api-key".length());
                String label = name.substring(0, 1).toUpperCase() + name.substring(1);
                providers.add(Map.of(
                    "key", "custom." + name,
                    "label", label,
                    "baseUrl", get("ai.custom." + name + ".base-url"),
                    "model", get("ai.custom." + name + ".model")
                ));
            }
        }

        // 标记当前激活的
        String active = getProvider();
        for (int i = 0; i < providers.size(); i++) {
            Map<String, String> p = new LinkedHashMap<>(providers.get(i));
            p.put("active", String.valueOf(active.equals(p.get("key"))));
            providers.set(i, p);
        }

        return providers;
    }

    public void deleteCustomProvider(String name) {
        config.remove("ai.custom." + name + ".api-key");
        config.remove("ai.custom." + name + ".base-url");
        config.remove("ai.custom." + name + ".model");
        config.remove("ai.custom." + name + ".timeout-seconds");
    }

    /** 加密后存储 api-key（加密失败绝不存明文） */
    public void updateEncrypted(String key, String plainValue) {
        if (plainValue == null || plainValue.isEmpty()) { update(Map.of(key, "")); return; }
        try {
            update(Map.of(key, encrypt(plainValue)));
        } catch (Exception e) {
            throw new BusinessException(500, "AI配置加密失败，API Key 未保存：" + e.getMessage());
        }
    }

    private String encrypt(String plain) {
        return encryptionUtil.encrypt(plain);
    }

    private String decrypt(String b64) {
        return encryptionUtil.decrypt(b64);
    }
}

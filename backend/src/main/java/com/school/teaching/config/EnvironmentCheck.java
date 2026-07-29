package com.school.teaching.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentCheck implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(EnvironmentCheck.class);
    private static final String[] REQUIRED = {"DB_PASSWORD", "JWT_SECRET"};

    private static final String[] JWT_DEFAULTS = {
        "your-256-bit-secret-key-here-change-in-production",
        "your-secret-key-change-in-production",
        "changeme", "secret", "default"
    };

    private final Environment env;
    public EnvironmentCheck(Environment env) { this.env = env; }

    @Override
    public void run(String... args) {
        boolean isLocal = java.util.Arrays.asList(env.getActiveProfiles()).contains("local");
        if (isLocal) {
            log.info("Local profile — skipping env var check");
        } else {
            for (String key : REQUIRED) {
                String val = env.getProperty(key);
                if (val == null || val.isBlank()) {
                    throw new IllegalStateException(
                        "Missing required environment variable: " + key
                        + ". Set it in your deployment config or use 'local' profile for development.");
                }
            }
            String jwtSecret = env.getProperty("JWT_SECRET");
            for (String weak : JWT_DEFAULTS) {
                if (weak.equals(jwtSecret)) {
                    throw new IllegalStateException(
                        "JWT_SECRET is set to a known default value. Generate a strong key: openssl rand -base64 32");
                }
            }
            log.info("Environment check passed — all {} required vars present and valid", REQUIRED.length);
        }

        // API文档暴露警告
        boolean knife4jEnabled = "true".equals(env.getProperty("knife4j.enable"));
        boolean isProd = java.util.Arrays.asList(env.getActiveProfiles()).contains("prod");
        if (knife4jEnabled && !isLocal) {
            log.warn("⚠ Knife4j API docs are enabled in non-local profile — consider disabling in production (knife4j.enable=false)");
        }
    }
}

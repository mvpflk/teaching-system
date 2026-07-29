package com.school.teaching.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * 共享 Jackson ObjectMapper 实例 — 避免各处 new ObjectMapper()。
 */
public final class JsonUtils {

    public static final ObjectMapper MAPPER = new ObjectMapper();

    /** 将 JSON 字符串解析为 Map */
    public static Map<String, Object> parseMap(String json) {
        try {
            return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of("raw", json);
        }
    }

    /** 将对象序列化为 JSON 字符串 */
    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    /** 将 JSON 字符串解析为指定类型 */
    public static <T> T parse(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            return null;
        }
    }

    /** 将 JSON 字符串解析为指定泛型类型 */
    public static <T> T parse(String json, TypeReference<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            return null;
        }
    }

    private JsonUtils() {}
}

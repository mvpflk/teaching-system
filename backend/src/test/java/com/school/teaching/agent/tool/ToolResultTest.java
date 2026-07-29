package com.school.teaching.agent.tool;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ToolResultTest {

    @Test
    @DisplayName("ok: 创建成功结果")
    void okCreatesSuccessResult() {
        ToolResult result = ToolResult.ok("操作成功");
        assertTrue(result.isSuccess());
        assertEquals("操作成功", result.getData());
        assertNull(result.getError());
    }

    @Test
    @DisplayName("ok: 支持 Map 类型 data")
    void okWithMapData() {
        Map<String, Object> data = Map.of("taskId", 1L, "status", "DRAFT");
        ToolResult result = ToolResult.ok(data);
        assertTrue(result.isSuccess());
        assertEquals(data, result.getData());
    }

    @Test
    @DisplayName("ok: 支持 null data（不应 NPE）")
    void okWithNullData() {
        ToolResult result = ToolResult.ok(null);
        assertTrue(result.isSuccess());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("fail: 创建失败结果")
    void failCreatesErrorResult() {
        ToolResult result = ToolResult.fail("出错了");
        assertFalse(result.isSuccess());
        assertEquals("出错了", result.getError());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("fail: 支持空错误消息")
    void failWithEmptyError() {
        ToolResult result = ToolResult.fail("");
        assertFalse(result.isSuccess());
        assertEquals("", result.getError());
    }

    @Test
    @DisplayName("builder: 构建完整的 ToolResult")
    void builderFull() {
        ToolResult result = ToolResult.builder()
                .success(true)
                .data(42)
                .error(null)
                .build();
        assertTrue(result.isSuccess());
        assertEquals(42, result.getData());
        assertNull(result.getError());
    }
}
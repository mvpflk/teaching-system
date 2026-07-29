package com.school.teaching.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RTest {

    @Test
    void ok_无参返回200() {
        R<Object> r = R.ok();
        assertEquals(200, r.getCode());
        assertEquals("Success", r.getMessage());
        assertNull(r.getData());
        assertNotNull(r.getTimestamp());
    }

    @Test
    void ok_带数据返回200() {
        R<String> r = R.ok("hello");
        assertEquals(200, r.getCode());
        assertEquals("hello", r.getData());
    }

    @Test
    void ok_带数据和消息返回200() {
        R<Integer> r = R.ok(42, "custom msg");
        assertEquals(200, r.getCode());
        assertEquals(42, r.getData());
        assertEquals("custom msg", r.getMessage());
    }

    @Test
    void error_无参返回500() {
        R<Object> r = R.error();
        assertEquals(500, r.getCode());
        assertEquals("Fail", r.getMessage());
        assertNull(r.getData());
    }

    @Test
    void error_带消息返回500() {
        R<Object> r = R.error("something wrong");
        assertEquals(500, r.getCode());
        assertEquals("something wrong", r.getMessage());
    }

    @Test
    void error_带状态码返回指定码() {
        R<Object> r = R.error(502, "bad gateway");
        assertEquals(502, r.getCode());
        assertEquals("bad gateway", r.getMessage());
    }

    @Test
    void fail_返回400() {
        R<Object> r = R.fail("bad request");
        assertEquals(400, r.getCode());
        assertEquals("bad request", r.getMessage());
    }

    @Test
    void notFound_返回404() {
        R<Object> r = R.notFound("not found");
        assertEquals(404, r.getCode());
        assertEquals("not found", r.getMessage());
    }

    @Test
    void forbidden_返回403() {
        R<Object> r = R.forbidden("no permission");
        assertEquals(403, r.getCode());
    }

    @Test
    void unauthorized_返回401() {
        R<Object> r = R.unauthorized("login expired");
        assertEquals(401, r.getCode());
    }

    @Test
    void isSuccess_200为true() {
        assertTrue(R.ok().isSuccess());
    }

    @Test
    void isSuccess_500为false() {
        assertFalse(R.error().isSuccess());
    }

    @Test
    void isSuccess_400为false() {
        assertFalse(R.fail("bad").isSuccess());
    }

    @Test
    void timestamp_非空且合理() {
        long before = System.currentTimeMillis();
        R<Object> r = R.ok();
        long after = System.currentTimeMillis();
        assertTrue(r.getTimestamp() >= before && r.getTimestamp() <= after);
    }
}

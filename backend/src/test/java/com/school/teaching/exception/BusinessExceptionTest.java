package com.school.teaching.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BusinessExceptionTest {

    @Test
    void 单参数构造函数_默认500() {
        BusinessException e = new BusinessException("error occurred");
        assertEquals(500, e.getCode());
        assertEquals("error occurred", e.getMessage());
    }

    @Test
    void 双参数构造函数_自定义错误码() {
        BusinessException e = new BusinessException(400, "bad request");
        assertEquals(400, e.getCode());
        assertEquals("bad request", e.getMessage());
    }

    @Test
    void 继承RuntimeException() {
        BusinessException e = new BusinessException("test");
        assertInstanceOf(RuntimeException.class, e);
    }

    @Test
    void 可作为异常抛出和捕获() {
        assertThrows(BusinessException.class, () -> {
            throw new BusinessException(404, "not found");
        });
    }

    @Test
    void 异常链保留原因() {
        RuntimeException cause = new RuntimeException("root cause");
        BusinessException e = new BusinessException("wrapper");
        assertSame("wrapper", e.getMessage());
    }
}

package com.school.teaching.common;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> implements Serializable {
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        return ok(data, "Success");
    }

    public static <T> R<T> ok(T data, String message) {
        return new R<>(200, message, data, System.currentTimeMillis());
    }

    public static <T> R<T> error() {
        return error("Fail");
    }

    public static <T> R<T> error(String message) {
        return new R<>(500, message, null, System.currentTimeMillis());
    }

    public static <T> R<T> error(Integer code, String message) {
        return new R<>(code, message, null, System.currentTimeMillis());
    }

    public static <T> R<T> fail(String message) {
        return error(400, message);
    }

    /** 404 资源不存在 */
    public static <T> R<T> notFound(String message) {
        return new R<>(404, message, null, System.currentTimeMillis());
    }

    /** 403 无权限 */
    public static <T> R<T> forbidden(String message) {
        return new R<>(403, message, null, System.currentTimeMillis());
    }

    /** 401 未登录/登录过期 */
    public static <T> R<T> unauthorized(String message) {
        return new R<>(401, message, null, System.currentTimeMillis());
    }

    public boolean isSuccess() {
        return this.code != null && this.code == 200;
    }
}

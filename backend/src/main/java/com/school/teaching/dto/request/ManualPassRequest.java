package com.school.teaching.dto.request;

import lombok.Data;
import java.io.Serializable;

/**
 * 手动标记通过请求。
 */
@Data
public class ManualPassRequest implements Serializable {
    /** 通过原因（可选） */
    private String reason;
}

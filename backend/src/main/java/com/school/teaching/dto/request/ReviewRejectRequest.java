package com.school.teaching.dto.request;

import lombok.Data;
import java.io.Serializable;

/**
 * 审核退回请求。
 */
@Data
public class ReviewRejectRequest implements Serializable {
    /** 退回原因（可选） */
    private String reason;
}

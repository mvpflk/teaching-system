package com.school.teaching.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import java.io.Serializable;

/**
 * 达标配置请求（达标线 + 重测参数）。
 */
@Data
public class PassRateRequest implements Serializable {
    /** 达标得分率（0-100；0=关闭达标模式） */
    @Min(value = 0, message = "达标率不能小于0")
    @Max(value = 100, message = "达标率不能大于100")
    private Integer passRate;

    /** 最大次数（含首次，≥2 才启用重测） */
    @Min(value = 1, message = "最大次数不能小于1")
    @Max(value = 10, message = "最大次数不能大于10")
    private Integer maxAttempts;

    /** 重测截止时间（距首次提交的小时数） */
    @Min(value = 1, message = "重测截止时间不能小于1小时")
    @Max(value = 720, message = "重测截止时间不能大于720小时")
    private Integer retakeDeadlineHours;
}

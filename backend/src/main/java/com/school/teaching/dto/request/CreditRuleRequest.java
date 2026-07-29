package com.school.teaching.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;

/**
 * 积分规则请求。
 * 创建时：ruleCode、creditValue 必填。
 * 更新时：所有字段可选，仅更新非 null 字段（部分更新）。
 */
@Data
public class CreditRuleRequest implements Serializable {
    /** 规则编码（创建必填，更新可选） */
    @NotBlank(message = "规则编码不能为空")
    private String ruleCode;

    /** 积分值（创建必填，更新可选） */
    @NotNull(message = "积分值不能为空")
    private Integer creditValue;
}

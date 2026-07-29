package com.school.teaching.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;

@Data
public class CreditRedeemRequest implements Serializable {
    @NotNull(message = "商品ID不能为空")
    private Long itemId;
}

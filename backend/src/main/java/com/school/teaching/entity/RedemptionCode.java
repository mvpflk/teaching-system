package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("redemption_codes")
public class RedemptionCode implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;
    private Long itemId;
    private Long studentId;
    private LocalDateTime redeemTime;
    private LocalDateTime useTime;
    private String useStatus;
    private LocalDateTime expireTime;
    private Long orderId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

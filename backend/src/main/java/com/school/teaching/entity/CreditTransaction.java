package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("credit_transactions")
public class CreditTransaction implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;
    private Long ruleId;
    private String transactionType;
    private Integer creditAmount;
    private Integer balanceAfter;
    private String sourceType;
    private Long sourceId;
    private String description;
    private String bizKey;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

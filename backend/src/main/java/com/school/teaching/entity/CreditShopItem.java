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
@TableName("credit_shop_items")
public class CreditShopItem implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String itemCode;
    private String itemName;
    private String itemType;
    private Integer creditPrice;
    private Integer stockCount;
    private Integer soldCount;
    private String description;
    private String usageRules;
    private Integer validDays;
    private String imageUrl;
    private Integer status;
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

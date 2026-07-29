package com.school.teaching.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;

/**
 * 商城商品请求。
 * 创建时：itemName、creditPrice 必填。
 * 更新时：所有字段可选，仅更新非 null 字段（部分更新）。
 */
@Data
public class ShopItemRequest implements Serializable {
    /** 商品名称（创建必填，更新可选） */
    @NotBlank(message = "商品名称不能为空")
    private String itemName;

    /** 积分价格（创建必填，更新可选） */
    @NotNull(message = "积分价格不能为空")
    private Integer creditPrice;

    /** 商品描述（可选） */
    private String description;
}

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
@TableName("achievement_definitions")
public class AchievementDefinition implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;
    private String name;
    private String description;
    private String iconUrl;
    private String category;
    private Integer creditReward;
    private String unlockConditions;
    private Integer rarityLevel;
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

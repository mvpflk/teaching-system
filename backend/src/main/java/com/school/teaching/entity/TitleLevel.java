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
@TableName("title_levels")
public class TitleLevel implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer levelNumber;
    private String levelName;
    private Integer minCredits;
    private Integer maxCredits;
    private String badgeIcon;
    private String privileges;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("showcase_comments")
public class ShowcaseComment implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long workId;
    private Long userId;
    private String content;
    private String status;
    private Long reviewerId;
    private LocalDateTime reviewTime;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String userName;

    @TableField(exist = false)
    private String userAvatar;

    @TableField(exist = false)
    private String workTitle;
}

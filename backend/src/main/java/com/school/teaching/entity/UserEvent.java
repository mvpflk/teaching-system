package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("user_events")
public class UserEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String roleName;
    private String eventType;
    private String eventData;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

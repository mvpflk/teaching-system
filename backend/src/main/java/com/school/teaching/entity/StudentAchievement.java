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
@TableName("student_achievements")
public class StudentAchievement implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;
    private Long achievementId;
    private LocalDateTime unlockTime;
    private Integer notificationShown;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

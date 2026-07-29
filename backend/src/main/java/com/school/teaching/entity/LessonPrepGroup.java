package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("lesson_prep_group")
public class LessonPrepGroup implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long teachingGroupId;
    private Long gradeId;
    private Long subjectId;
    private String classType;
    private Long schoolId;
    private Long stageId;
    private String leaderIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

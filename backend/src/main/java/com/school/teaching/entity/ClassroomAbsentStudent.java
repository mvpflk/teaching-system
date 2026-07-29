package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("classroom_absent_students")
public class ClassroomAbsentStudent implements Serializable {
    private Long classId;
    private Long studentId;
    private Long sessionId;
    private LocalDateTime markedAt;
}

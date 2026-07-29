package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("student_class_history")
public class StudentClassHistory implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long classId;
    private Long stageId;
    private Long schoolId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String changeReason;
    private Long operatorId;
    private LocalDateTime createdAt;
}

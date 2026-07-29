package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("student_stage_change_log")
public class StudentStageChangeLog implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long fromStageId;
    private Long toStageId;
    private Long fromClassId;
    private Long toClassId;
    private LocalDate changeDate;
    private String reason;
    private Long approvedBy;
    private LocalDateTime createdAt;
}

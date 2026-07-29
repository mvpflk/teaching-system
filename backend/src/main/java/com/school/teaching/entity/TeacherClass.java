package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

@Data
@TableName("teacher_classes")
public class TeacherClass implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teacherId;
    private Long classId;
    private String subject;
}

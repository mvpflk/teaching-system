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
@TableName("backup_exam_questions")
public class ExamQuestion implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long examId;
    private String questionType;
    private String questionText;
    private String questionImageUrl;
    private String options;
    private String correctAnswer;
    private String explanation;
    private Integer score;
    private Integer difficultyLevel;
    private Integer sortOrder;
    private String attachmentUrl;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

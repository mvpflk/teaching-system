package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学生答题明细 — 兼用旧考试链路(exam_results)和新统一任务链路(task_submissions)。
 */
@Data
@TableName("student_answers")
public class StudentAnswer implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;

    // 旧链路：考试结果
    private Long resultId;
    // 新链路：任务提交
    private Long submissionId;
    private Long taskId;

    private Long questionId;
    private String studentAnswer;
    private Integer isCorrect;       // 0=错 1=对 2=主观待评分

    // 旧链路：得分
    private Integer score;
    // 新链路：分开的客观/主观分
    private BigDecimal autoScore;
    private BigDecimal teacherScore;

    private Integer addedToWrong;

    private Long schoolId;
    private Long stageId;

    private LocalDateTime answerTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("classroom_questions")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClassroomQuestion implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long teacherId;
    private String subject;
    private String chapter;
    private String tag;
    private String content;
    private String referenceAnswer;
    private Integer difficulty;
    private String source;
    private Long sourceQuestionId;
    private Integer usageCount;
    /** 同步到 question_bank 后的对应 ID，用于 wrong_questions FK 约束 */
    private Long syncedQuestionBankId;
    /** 题目类型：SHORT_ANSWER/TRUE_FALSE 等 */
    private String questionType;
    /** AI教学助手推送标记：0=非AI, 1=AI生成 */
    private Integer fromAi;
    /** AI题目意图（教师用，如"检查IP概念理解"） */
    private String intent;
    /** AI题目分类：RECALL/COMPREHEND/APPLY/EXTEND */
    private String aiCategory;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

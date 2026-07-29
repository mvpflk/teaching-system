package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("question_bank")
public class QuestionBank implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private String subject;
    private Long categoryId;
    private Long grammarNodeId;
    private String questionType;
    private String questionText;
    private String options;
    private String correctAnswer;
    private String explanation;
    private String attachmentUrl;
    private Integer difficultyLevel;
    private Long createdBy;
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String creatorName;
    private Long schoolId;
    private Long stageId;
    private Integer status;
    private String contentJson;
    private String answerSchema;
    private String knowledgePoints;
    private String intent;
    private String category;
    /** 考纲维度: THEORY(应知)/PRACTICE(应会) */
    private String knowledgeDim;
    /** 入库途径: MANUAL/AI/WORD_IMPORT/EXCEL_IMPORT/PAPER_IMPORT（历史数据为 NULL，前端显示「其他」） */
    private String source;
    /** 题目层级: BASIC/MEDIUM/ADVANCED */
    private String tier;
    private Integer version;
    private Integer isLatest;
    /** 是否经教师编辑（区别AI原文入库和教师修正入库） */
    private Integer editedByTeacher;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

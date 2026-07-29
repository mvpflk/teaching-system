package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("knowledge_articles")
public class KnowledgeArticle implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String contentMd;
    private String excerpt;
    private Long subjectId;
    private String chapter;
    private String task;
    private Long nodeId;
    private String memoryTips;
    private String examFocus;
    private Integer difficulty;
    private String tags;
    private String syllabusRefs;
    private String quiz;
    private Integer viewCount;
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private java.util.List<KnowledgeFlashcard> flashcards;
}

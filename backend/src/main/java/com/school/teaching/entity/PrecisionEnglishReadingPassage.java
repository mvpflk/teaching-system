package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName(value = "precision_english_reading_passages", autoResultMap = true)
public class PrecisionEnglishReadingPassage implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private Integer wordCount;
    private Integer difficultyLevel;
    private Integer minVocabSize;
    private String grammarTags;
    private String newWordList;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> questionIds;
    private String source;
    private LocalDateTime createdAt;
}

package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;

/**
 * 英语词汇种子表 — 考纲2000词词库
 * 表: precision_vocabulary_seeds
 */
@Data
@TableName("precision_vocabulary_seeds")
public class PrecisionVocabularySeed implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String word;          // 单词
    private String meaning;       // 中文释义
    private String phonetic;      // 音标
    private String example;       // 例句
    private Integer frequencyRank; // 考纲频率排名(越小越高频)
    private Integer level;        // 难度等级 1-5
}

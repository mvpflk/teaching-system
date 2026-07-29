package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("checkpoint_keyword_log")
public class CheckpointKeywordLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;
    private Long configId;
    private Integer keywordIndex;
    private Integer attemptNo;
    private String studentInput;
    private Integer isCorrect;
    private Integer followupCorrect;
    private LocalDateTime createdAt;
}

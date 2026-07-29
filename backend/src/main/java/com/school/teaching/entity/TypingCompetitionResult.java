package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("typing_competition_results")
public class TypingCompetitionResult implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long competitionId;
    private Long studentId;
    private Integer totalChars;
    private Integer correctChars;
    private Integer wrongChars;
    private Integer backspaceCount;
    private Integer durationSeconds;
    private BigDecimal speedWpm;
    private BigDecimal accuracy;
    private String errorDetails;
    private BigDecimal score;
    private String keystrokeData;
    private LocalDateTime finishedAt;
}

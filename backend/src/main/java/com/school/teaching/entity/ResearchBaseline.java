package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("research_baseline")
public class ResearchBaseline implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String studentName;
    private Long classId;
    private String className;
    private String researchGroup;    // EXPERIMENT/CONTROL
    private String subject;
    private Long nodeId;
    private String nodeName;
    private Integer nodeLevel;
    private BigDecimal masteryPercent;
    private Integer totalAttempts;
    private Integer totalCorrect;
    private String status;
    private LocalDateTime snapshotTime;
    private String snapshotLabel;    // PRETEST/MIDTEST/POSTTEST
    private Long schoolId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

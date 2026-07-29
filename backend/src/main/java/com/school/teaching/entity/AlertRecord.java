package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("alert_records")
public class AlertRecord implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ruleId;
    private Long studentId;
    private Long classId;
    private String matchedSubmissionIds;
    private String alertSummary;
    private Integer notifiedTeacher;
    private Integer notifiedParents;
    private String handledStatus;
    private Long handledBy;
    private LocalDateTime handledAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

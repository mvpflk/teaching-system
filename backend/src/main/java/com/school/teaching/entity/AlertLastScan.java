package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("alert_last_scan")
public class AlertLastScan implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String scanType;
    private Long lastSubmissionId;
    private LocalDateTime lastScanTime;
    private Integer scannedCount;
    private Integer alertCount;
    private String status;
    private String errorMsg;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

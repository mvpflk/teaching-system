package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("answer_sheet_ocr")
public class AnswerSheetOcr implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;
    private Long studentId;
    private String studentName;
    private Long classId;
    private String photoPath;
    private String ocrRawText;
    private String parsedAnswers;     // JSON
    private BigDecimal overallConfidence;
    private String autoGradeResult;   // JSON
    private String status;            // parsed/graded/manual_entry/failed/reviewed
    private Long reviewerId;
    private String reviewNote;
    private Long graderId;
    private Long schoolId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

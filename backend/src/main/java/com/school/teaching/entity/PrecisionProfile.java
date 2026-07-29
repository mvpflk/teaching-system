package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("precision_profile")
@Schema(description = "学生偏科提分画像")
public class PrecisionProfile implements Serializable {
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "学生ID")
    private Long studentId;

    @Schema(description = "学科名称(如: 数学[职高], 英语[职高])")
    private String subject;

    @Schema(description = "诊断分数")
    private Integer diagnoseScore;

    @Schema(description = "预估高考分")
    private Integer estimatedScore;

    @Schema(description = "上次诊断日期")
    private LocalDate lastDiagnoseAt;

    @Schema(description = "连续学习周数")
    private Integer streakWeeks;

    @Schema(description = "最近小测分数")
    private Integer lastOnlineTestScore;

    @Schema(description = "上次提交日期")
    private LocalDate lastSubmitDate;

    @Schema(description = "学习包题目ID列表(JSON)")
    private String lastPackQuestionIds;

    @Schema(description = "学习包周数")
    private Integer lastPackWeekNo;

    @Schema(description = "扩展数据(JSON)")
    private String extraData;

    @Version
    @Schema(description = "乐观锁版本号")
    private Integer version;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

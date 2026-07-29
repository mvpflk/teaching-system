package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("students")
@Schema(description = "学生实体")
public class Student implements Serializable {
    @TableId(type = IdType.AUTO)
    @Schema(description = "学生ID", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "关联用户ID")
    private Long userId;
    @Schema(description = "学号")
    private String studentNumber;
    @Schema(description = "性别: 0=女, 1=男")
    private Integer gender;
    @Schema(description = "出生日期")
    private LocalDate birthday;
    @Schema(description = "入学年份")
    private Integer enrollmentYear;
    @Schema(description = "班级ID")
    private Long classId;
    @Schema(description = "学校ID")
    private Long schoolId;
    @Schema(description = "当前学段ID")
    private Long currentStageId;
    @Schema(description = "原始学段ID")
    private Long originalStageId;
    @Schema(description = "总积分")
    private Integer totalCredits;
    @Schema(description = "称号等级")
    private Integer titleLevel;
    @Schema(description = "当前连续打卡天数")
    private Integer currentStreak;
    @Schema(description = "自定义称号")
    private String customTitle;
    @Schema(description = "自定义称号设置时间")
    private LocalDateTime customTitleSetAt;
    @Schema(description = "当前类型")
    private String currentType;
    @Schema(description = "入学类型")
    private String enrollmentType;
    @Schema(description = "状态: active/leave/withdraw/transfer/retain/graduated")
    private String status;

    @Schema(description = "偏科提分档案JSON")
    private String precisionProfile;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

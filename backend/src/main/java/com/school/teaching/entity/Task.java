package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 统一任务主表 — 概念统一考试/作业/练习/测验等所有教学任务。
 */
@Data
@TableName("tasks")
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "任务实体")
public class Task implements Serializable {
    private static final long serialVersionUID = -8146457136231197479L;
    @TableId(type = IdType.AUTO)
    @Schema(description = "任务ID", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "学校ID")
    private Long schoolId;
    @Schema(description = "学段ID")
    private Long stageId;

    @Schema(description = "任务标题", required = true, example = "第三章单元测试")
    private String title;
    @Schema(description = "任务描述")
    private String description;
    @Schema(description = "学科", example = "数学[职高]")
    private String subject;
    @Schema(description = "年级ID")
    private Long gradeId;
    @Schema(description = "创建教师ID")
    private Long teacherId;

    @Schema(description = "总分", example = "100")
    private BigDecimal totalScore;
    @Schema(description = "分值类型", example = "SCORE")
    private String scoreType;

    @Schema(description = "任务类型", example = "EXAM")
    private String taskType;
    @Schema(description = "目标类型", example = "CLASS")
    private String targetType;
    @Schema(description = "目标ID")
    private Long targetId;
    @Schema(description = "可见范围")
    private String viewScope;

    @Schema(description = "截止时间")
    private LocalDateTime deadline;
    @Schema(description = "任务状态", example = "PUBLISHED")
    private String status;
    @Schema(description = "审核状态")
    private String reviewStatus;

    @Schema(description = "任务配置JSON")
    private String taskConfig;

    @Schema(description = "是否通知家长")
    private Integer notifyParents;
    @Schema(description = "允许重交")
    private Integer allowResubmit;
    @Schema(description = "是否必修")
    private Integer isRequired;
    @Schema(description = "五育标签")
    private String wuyuTag;
    @Schema(description = "竞赛模式")
    private Integer isCompetitionMode;
    @Schema(description = "自动错题本")
    private Integer autoWrongbook;
    @Schema(description = "强制任务")
    private Integer isForced;
    @Schema(description = "匿名提交")
    private Integer isAnonymous;
    @Schema(description = "问卷结构JSON")
    private String surveySchema;
    @Schema(description = "学期ID")
    private Long termId;
    @Schema(description = "评分标准ID")
    private Long rubricId;
    @Schema(description = "允许自定义步骤")
    private Integer allowCustomSteps;
    @Schema(description = "参考图片JSON")
    private String referenceImages;
    @Schema(description = "定时发布时间")
    private LocalDateTime scheduledPublishAt;
    @Schema(description = "关联考纲ID")
    private Long syllabusId;
    @Schema(description = "源任务ID")
    private Long sourceTaskId;
    @Schema(description = "难度等级", example = "MEDIUM")
    private String difficultyLevel;
    @Schema(description = "达标得分率(%), 0=不启用, 50-100=启用")
    private Integer passRate;
    @Schema(description = "最大尝试次数(含首次), 1=不重测")
    private Integer maxAttempts;
    @Schema(description = "重测截止时间(小时)")
    private Integer retakeDeadlineHours;
    @Schema(description = "达标判定策略: objective=仅客观题, all=全判定", example = "objective")
    private String passMode;

    @TableField(exist = false)
    private List<Long> targetIds;
    @TableField(exist = false)
    private List<Long> groupIds;
    @TableField(exist = false)
    private List<Long> questionIds;
    @TableField(exist = false)
    private Boolean isOwner;
    @TableField(exist = false)
    private String className;
    @TableField(exist = false)
    private String grade;
    @TableField(exist = false)
    private String teacherName;
    @TableField(exist = false)
    private Map<String, Integer> scorePresets;
    @TableField(exist = false)
    private String submissionStatus;
    @TableField(exist = false)
    private BigDecimal score;
    @TableField(exist = false)
    private String scoreJson;
    @TableField(exist = false)
    private Long submissionId;
    @TableField(exist = false)
    private String gradingMessage;
    @TableField(exist = false)
    private Integer cheatTerminated;
    @TableField(exist = false)
    private String content;
    @TableField(exist = false)
    private String attachments;
    @TableField(exist = false)
    private Long pendingGradingCount;
    @TableField(exist = false)
    private Long submittedCount;
    @TableField(exist = false)
    private Long totalStudents;
    @TableField(exist = false)
    private Double avgScore;
    @TableField(exist = false)
    private String taskBehavior;
    @TableField(exist = false)
    private String reflection;
    /** 是否需要重测（由 getStudentTasksWithSubmission 填充） */
    @TableField(exist = false)
    private Boolean needsRetake;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

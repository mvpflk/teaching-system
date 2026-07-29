package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("classes")
@Schema(description = "班级实体")
public class Classes implements Serializable {
    @TableId(type = IdType.AUTO)
    @Schema(description = "班级ID", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "班级名称", required = true, example = "2024级计算机1班")
    private String className;
    @Schema(description = "班级代码")
    private String classCode;
    @Schema(description = "年级", example = "2024级")
    private String grade;
    @Schema(description = "专业")
    private String major;
    @Schema(description = "学年", example = "2024-2025")
    private String academicYear;
    @Schema(description = "学期", example = "第一学期")
    private String semester;
    @Schema(description = "班主任用户ID")
    private Long headTeacherId;
    @Schema(description = "班级类型")
    private String classType;
    @Schema(description = "学校ID")
    private Long schoolId;
    @Schema(description = "学段ID")
    private Long stageId;
    @Schema(description = "状态: 1=启用, 0=禁用")
    private Integer status;
    @Schema(description = "学生人数")
    private Integer studentCount;
    @Schema(description = "课题组别: EXPERIMENT=实验班, CONTROL=对照班")
    private String researchGroup;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

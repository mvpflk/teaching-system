package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("prompt_template")
@Schema(description = "AI 提示词模板")
public class PromptTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "TEMPLATE / FINAL")
    private String type;

    @Schema(description = "模板唯一标识")
    private String name;

    @Schema(description = "中文名称")
    private String label;

    @Schema(description = "关联学科，NULL=通用")
    private String subject;

    @Schema(description = "版本号")
    private Integer version;

    @Schema(description = "提示词正文")
    private String content;

    @Schema(description = "是否生效")
    private Boolean isActive;

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}

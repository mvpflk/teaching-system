package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ai_outputs")
public class AiOutput implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联知识点ID（FK → knowledge_nodes） */
    private Long nodeId;

    /** 教师ID */
    private Long teacherId;

    /** 产出类型 */
    private String outputType;

    /** 产出标题 */
    private String title;

    /** 学科 */
    private String subject;

    /** 产出内容（LONGTEXT Markdown） */
    private String content;

    /** 1=最新版本, 0=历史版本 */
    private Integer isLatest;

    /** 版本序号: 1=最新, 2=次新, ... (同node+type内递增) */
    private Integer versionSeq;

    /** 状态：0=草稿, 1=已发布, 2=已归档, 3=待审核(PENDING_REVIEW) */
    private Integer status;

    /** Token用量 */
    private Integer tokensUsed;

    /** 延迟毫秒数 */
    private Integer latencyMs;

    /** 教师评分 1-5 */
    private Integer rating;

    /** 教师文字反馈 */
    private String feedback;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

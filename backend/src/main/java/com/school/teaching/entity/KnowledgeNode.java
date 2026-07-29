package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_nodes")
public class KnowledgeNode implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父节点ID，null表示根节点 */
    private Long parentId;

    /** 所属学科ID */
    private Long subjectId;

    /** 层级：1=学科, 2=章节, 3=任务, 4=知识点 */
    private Integer level;

    /** 节点名称 */
    private String name;

    /** 节点内容（MEDIUMTEXT Markdown） */
    private String content;

    /** 排序序号 */
    private Integer sortOrder;

    /** 语法分类: tense/passive/clause/non_finite/lexical/sentence (仅语法节点) */
    private String grammarCategory;

    /** 解锁所需阶段 1-7 (仅语法节点，NULL=非语法节点) */
    private Integer unlockStage;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    /** 节点状态: ACTIVE(正常)/LEGACY(陈旧但仍考)/DEPRECATED(过时)/OBSOLETE(淘汰) */
    private String status;

    /** 相关度 1-10，默认5，≤3需在命题中淡化，≥8加星标 */
    private Integer relevanceLevel;

    /** 陈旧原因说明 */
    private String deprecationNote;

    /** 学习资源JSON: {videoUrl, exampleIds:[], practiceIds:[]} */
    private String learningResources;

    /** 最近一次AI生成时间 */
    private LocalDateTime resourceGeneratedAt;

    /** 审核状态: PENDING/APPROVED/REJECTED */
    private String resourceStatus;

    /** 教师拒绝原因（仅REJECTED时有值） */
    private String resourceRejectReason;

    /** 视频链接上次检查时间 */
    private LocalDateTime videoCheckedAt;

    /** 资源版本号，每次修改+1 */
    private Integer resourceVersion;

    /** 最后审核时间 */
    private LocalDateTime lastReviewedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** v167: 考纲权重 HIGH/MEDIUM/LOW，教师标记 */
    private String examWeight;

    /** v151: 自定义渲染类型: vocab_drill=背单词模块, NULL=标准Markdown文章 */
    private String renderType;
}

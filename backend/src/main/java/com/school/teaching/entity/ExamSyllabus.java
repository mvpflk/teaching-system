package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("exam_syllabus")
public class ExamSyllabus implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long subjectId;
    private String examType;
    private String knowledgeDim;
    /** 关联知识点节点ID(knowledge_nodes.id)，支持精确考纲→知识点映射，NULL=未关联 */
    private Long nodeId;
    private String title;
    private String content;
    /** 结构化考纲元数据 JSON<br>程序读取用(避免脆弱的Markdown正则解析)<br>示例: {"reciteList":"...","scoreDistribution":"...","examPointOrder":"...","compositionThemes":"..."} */
    private String syllabusMeta;
    private String version;
    private Integer status;
    private Long createdBy;
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

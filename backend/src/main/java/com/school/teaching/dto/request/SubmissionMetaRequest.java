package com.school.teaching.dto.request;

import lombok.Data;
import java.io.Serializable;

/**
 * 提交元数据更新请求。
 * 更新时：所有字段可选，仅更新非 null 字段（部分更新）。
 */
@Data
public class SubmissionMetaRequest implements Serializable {
    /** 评语（可选） */
    private String comment;
    /** 是否为优秀范例（可选） */
    private Integer isExemplar;
}

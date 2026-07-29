package com.school.teaching.dto.request;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 衍生训练生成请求。
 */
@Data
public class RemedialGenerateRequest implements Serializable {
    /** 选中的知识点ID列表（可选，为空则使用全部薄弱点） */
    private List<Integer> nodeIds;
}

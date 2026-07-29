package com.school.teaching.dto.request;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 批量重新评分请求。
 * 三者选一：submissionIds / questionId / taskId
 */
@Data
public class BatchRegradeRequest implements Serializable {
    /** 提交ID列表（可选） */
    private List<Number> submissionIds;

    /** 任务ID（可选，按任务批量重评） */
    private Number taskId;

    /** 题目ID（可选，按题目批量重评） */
    private Number questionId;
}

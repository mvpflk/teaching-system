package com.school.teaching.service;

import java.util.Map;

public interface ExamPaperService {
    /** 分页查我的试卷库 */
    Map<String, Object> pageByCreator(int page, int pageSize, String subject);

    /** 软删除 */
    void softDelete(Long id);

    /** 从试卷库快速创建任务 */
    Map<String, Object> createTaskFromPaper(Long paperId, Map<String, Object> body);

    /** 标记为标准化试卷（P0-1） */
    void markStandardized(Long paperId, String paperRole, Long parallelPaperId);

    /** 解除标准化标记（P0-1） */
    void unmarkStandardized(Long paperId);

    /** 锁定/解锁试卷（P0-1） */
    void lockPaper(Long paperId);
    void unlockPaper(Long paperId);
}

package com.school.teaching.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.entity.ExamSyllabus;
import org.springframework.web.multipart.MultipartFile;

public interface ExamSyllabusService {

    Page<ExamSyllabus> list(Long subjectId, String examType, int page, int size);

    ExamSyllabus getById(Long id);

    ExamSyllabus getBySubject(Long subjectId, String examType);

    ExamSyllabus create(ExamSyllabus syllabus);

    ExamSyllabus update(Long id, ExamSyllabus syllabus);

    void delete(Long id);

    void toggleStatus(Long id);

    /** 获取考纲关联的知识节点ID列表 */
    java.util.List<Long> getNodeIds(Long syllabusId);

    /** 保存考纲-知识节点关联关系 */
    void saveNodeRelations(Long syllabusId, java.util.List<Long> nodeIds);

    /** 根据知识节点ID查找关联的考纲列表 */
    java.util.List<ExamSyllabus> getSyllabiByNodeId(Long nodeId);

    /** 获取用于Prompt注入的考纲摘要（截取应知/应会/考试范围关键段落，≤3000字） */
    String getSyllabusPromptContext(Long subjectId);

    /** 精确模式：根据知识节点ID获取关联考纲摘要 */
    String getSyllabusPromptContextByNode(Long nodeId);

    /** 获取结构化考纲元数据 JSON（供程序稳健读取，非Markdown解析） */
    String getSyllabusMeta(Long subjectId);

    /** 考纲健康检查：校验所有已启用考纲的结构化数据完整性 */
    java.util.Map<String, Object> healthCheck();
}

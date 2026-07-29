package com.school.teaching.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.teaching.entity.QuestionBank;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface QuestionBankService {

    /** 根据主键 ID 获取题目 */
    QuestionBank getById(Long id);

    /** 获取题库列表（支持筛选） */
    List<QuestionBank> listQuestions(String subject, Long categoryId, String questionType,
                                     Integer difficultyLevel, String keyword);

    /** 获取题库列表（分页 + 筛选） */
    IPage<QuestionBank> pageQuestions(String subject, Long categoryId, String questionType,
                                      Integer difficultyLevel, String keyword,
                                      Integer status, String tier, String knowledgeDim,
                                      String source, String sort, Integer page, Integer pageSize);

    /** 获取学科列表（id + subjectName） */
    List<Map<String, Object>> getSubjects();

    /** 创建题目 */
    QuestionBank createQuestion(Long userId, QuestionBank question);

    /** 更新题目（含所有权校验） */
    QuestionBank updateQuestion(Long id, QuestionBank question, Long userId, boolean isAdmin);

    /** 删除题目（软删除） */
    void deleteQuestion(Long id, Long userId, boolean isAdmin);

    /** 批量清空题库（仅管理员） */
    int batchClearQuestions();

    /** 从Word导入题目 */
    Map<String, Object> importFromWord(MultipartFile file, Long categoryId, Long userId);

    /** 批量Word导入 */
    Map<String, Object> importFromWordBatch(List<MultipartFile> files, String mappings, Long userId);

    /** 从Excel导入题目 */
    Map<String, Object> importFromExcel(MultipartFile file, Long categoryId, Long userId);

    /** 从题库选题加入试卷 */
    int addToExam(Long examId, List<Long> questionIds, Long userId, boolean isAdmin);

    /** 一键组卷：从题库选题创建试卷 */
    Map<String, Object> composeExam(Map<String, Object> body, Long userId, String role);

    /** 递归获取综合题的所有子题（一次查询，内存组装，防 N+1） */
    List<Map<String, Object>> getCompositeChildren(Long parentQuestionId);

    /** 获取题型处理器 */
    com.school.teaching.common.QuestionTypeHandler getHandler(com.school.teaching.common.QuestionTypeEnum type);

    /** 自由组题：按知识点+题型数量从题库匹配题目 */
    List<Map<String, Object>> matchQuestions(List<Map<String, Object>> knowledgePoints, List<Long> excludeIds);

    /** AI审核题目：调用大模型评估题目质量，返回审核结果 */
    Map<String, Object> aiReview(List<Long> questionIds, boolean autoApprove, Long userId);

    /** 批量通过题目（status: 0→1） */
    Map<String, Object> batchApprove(List<Long> questionIds);

    /** 批量驳回题目（status: 0→2） */
    Map<String, Object> batchReject(List<Long> questionIds, Long userId);

    /** 按批次ID获取题目列表（用于AI生成后编辑） */
    List<QuestionBank> listByBatchId(String batchId, Long teacherId);

    /** 按ID列表获取题目（用于组卷导出等场景） */
    List<QuestionBank> listByIds(List<Long> ids);

    /** 批量统计题目被组卷引用次数（含已关闭任务的历史引用） */
    java.util.Map<Long, Long> usageStats(java.util.List<Long> questionIds);
}

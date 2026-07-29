package com.school.teaching.service;

import com.school.teaching.entity.KnowledgeNode;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface KnowledgeNodeService {

    /** 获取完整知识节点树（4级：学科→章节→任务→知识点） */
    List<Map<String, Object>> getTree();

    /** 获取知识节点树（可按学科过滤） */
    List<Map<String, Object>> getTree(Long subjectId);

    /** 知识树版本号（所有节点最大 updatedAt 时间戳） */
    long getTreeVersion();

    /** 按父节点或层级查询 */
    List<KnowledgeNode> list(Long parentId, Integer level);

    /** 获取指定学科的所有节点（按 sortOrder 排序） */
    List<KnowledgeNode> listBySubjectId(Long subjectId);

    /** 创建节点 */
    KnowledgeNode create(KnowledgeNode node);

    /** 更新节点（名称、内容、排序、父节点） */
    KnowledgeNode update(Long id, KnowledgeNode node);

    /** 删除节点（级联删除子节点+关联AI产出） */
    void delete(Long id);

    /** 获取节点知识库内容 */
    String getContent(Long nodeId);

    /** 获取节点内容，可选包含所有子节点聚合 */
    String getContent(Long nodeId, boolean includeChildren);

    /** 设置节点知识库内容 */
    void setContent(Long nodeId, String markdownContent);

    /** 获取指定学科下所有节点的内容映射 */
    Map<Long, String> getSubjectKnowledgeMap(Long subjectId);

    /** Excel批量导入（4列旧格式或5列新格式） */
    int importFromExcel(MultipartFile file) throws Exception;

    /** ZIP批量导入（2级旧路径或3级新路径） */
    int importFromZip(Long subjectId, MultipartFile zipFile) throws Exception;

    /** 向上追溯节点的学科根节点ID */
    Long findSubjectRoot(Long nodeId);

    /** 获取节点的完整路径字符串（学科 > 章节 > 任务 > 知识点） */
    String getNodeFullPath(Long nodeId);

    /** 清空指定学科下所有知识节点（含关联AI产出和题库题目） */
    void clearBySubject(Long subjectId);

    /** TXT 文件导入（#标记层级结构） */
    int importFromTxt(Long subjectId, MultipartFile file) throws Exception;

    /** Word(.docx) 文件导入（标题样式作层级结构） */
    int importFromDocx(Long subjectId, MultipartFile file) throws Exception;

    // ── AI 学习资源 ──

    /** AI 生成学习资源，返回 {nodeId, subject, learningResources, resourceStatus, resourceVersion} */
    Map<String, Object> generateLearningResources(Long nodeId);

    /** 教师审核学习资源: status=APPROVED/REJECTED, rejectReason可选 */
    void reviewResource(Long nodeId, String status, String rejectReason);

    /** 检查已审核资源中的视频链接有效性，返回失效列表 */
    List<Map<String, Object>> checkVideoLinks();

    /** 获取节点学习资源（学生端），含状态判断 */
    Map<String, Object> getNodeLearningResources(Long nodeId);
}

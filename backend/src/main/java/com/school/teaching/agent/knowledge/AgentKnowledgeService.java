package com.school.teaching.agent.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.ExamSyllabus;
import com.school.teaching.entity.KnowledgeNode;
import com.school.teaching.mapper.ExamSyllabusMapper;
import com.school.teaching.mapper.KnowledgeNodeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Agent 知识库查询服务 — 从 AgentController 提取，遵循 Controller→Service→Mapper 分层约定。
 * 提供学科知识树和考纲的预注入查询，Agent 启动时即可获得学科全景而无须运行时工具调用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentKnowledgeService {

    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final ExamSyllabusMapper examSyllabusMapper;

    /**
     * 从知识库预取学科上下文（L2 模块 + 考纲摘要），缓存复用。
     * 消除 Agent 对系统能力的一无所知，不再依赖运行时工具调用来发现学科结构。
     */
    public String buildSubjectContext(String subject) {
        return buildSubjectContextInternal(subject);
    }

    private String buildSubjectContextInternal(String subject) {
        try {
            // 1. 匹配学科名 → 获取 subject_id
            // 先精确匹配，再模糊匹配；优先职高（[职高]）版本
            Long subjectId = null;
            List<KnowledgeNode> roots = knowledgeNodeMapper.selectList(
                    new LambdaQueryWrapper<KnowledgeNode>()
                            .eq(KnowledgeNode::getName, subject + "[职高]")
                            .eq(KnowledgeNode::getLevel, 1).last("LIMIT 1"));
            if (roots.isEmpty()) {
                roots = knowledgeNodeMapper.selectList(
                        new LambdaQueryWrapper<KnowledgeNode>()
                                .like(KnowledgeNode::getName, subject)
                                .eq(KnowledgeNode::getLevel, 1)
                                .orderByDesc(KnowledgeNode::getSubjectId)
                                .last("LIMIT 1"));
            }
            if (roots.isEmpty()) return null;
            subjectId = roots.get(0).getSubjectId();

            // 2. 查该学科所有 L2 模块（兼容 status=NULL 的节点）
            LambdaQueryWrapper<KnowledgeNode> l2Q = new LambdaQueryWrapper<KnowledgeNode>()
                    .eq(KnowledgeNode::getSubjectId, subjectId)
                    .eq(KnowledgeNode::getLevel, 2)
                    .and(w -> w.eq(KnowledgeNode::getStatus, "ACTIVE")
                             .or().isNull(KnowledgeNode::getStatus))
                    .orderByAsc(KnowledgeNode::getSortOrder)
                    .last("LIMIT 20");
            List<KnowledgeNode> l2Nodes = knowledgeNodeMapper.selectList(l2Q);

            // 3. 查考纲摘要（取 content 首 300 字）
            String syllabusBrief = null;
            List<ExamSyllabus> syllabi = examSyllabusMapper.selectList(
                    new LambdaQueryWrapper<ExamSyllabus>()
                            .eq(ExamSyllabus::getSubjectId, subjectId)
                            .last("LIMIT 1"));
            if (!syllabi.isEmpty() && syllabi.get(0).getContent() != null) {
                String content = syllabi.get(0).getContent();
                syllabusBrief = content.length() > 300
                        ? content.substring(0, 300).replace('\n', ' ') + "…"
                        : content.replace('\n', ' ');
            }

            // 4. 格式化为紧凑 Markdown
            StringBuilder sb = new StringBuilder();
            sb.append("📋 系统知识库 — ").append(subject).append("\n");
            if (!l2Nodes.isEmpty()) {
                sb.append(l2Nodes.size()).append("个模块：");
                List<String> names = l2Nodes.stream()
                        .map(KnowledgeNode::getName).collect(Collectors.toList());
                sb.append(String.join(" | ", names)).append("\n");
            } else {
                sb.append("该学科暂无预置模块树。");
                sb.append("请使用 knowledge_search(keyword=\"具体知识点\") 搜索相关内容。\n");
            }
            if (syllabusBrief != null) {
                sb.append("考纲：").append(syllabusBrief).append("\n");
            } else {
                sb.append("考纲：暂无。可尝试 syllabus_lookup 查询。\n");
            }
            sb.append("（以上为系统知识库概况。如需详细内容请调 knowledge_search，切勿因模块数少而拒绝回答——可用自身知识补充）");
            log.info("buildSubjectContext: subject={}, subjectId={}, L2count={}",
                    subject, subjectId, l2Nodes.size());
            return sb.toString();
        } catch (Exception e) {
            log.warn("构建学科上下文失败: subject={}", subject, e);
            return null;
        }
    }
}

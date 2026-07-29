package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.KnowledgeNode;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.KnowledgeNodeMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KnowledgeNodeReadService {

    @Autowired private KnowledgeNodeMapper nodeMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int LV_SUBJECT = 1;
    private static final int LV_CHAPTER = 2;
    private static final int LV_TASK    = 3;
    private static final int LV_KP      = 4;

    public long getTreeVersion() {
        KnowledgeNode latest = nodeMapper.selectOne(
            new LambdaQueryWrapper<KnowledgeNode>()
                .orderByDesc(KnowledgeNode::getUpdatedAt)
                .last("LIMIT 1"));
        if (latest == null || latest.getUpdatedAt() == null) return 0L;
        return latest.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    @org.springframework.cache.annotation.Cacheable(value = "knowledge_tree", unless = "#result == null || #result.isEmpty()")
    public List<Map<String, Object>> getTree() {
        List<KnowledgeNode> all = nodeMapper.selectList(
                new LambdaQueryWrapper<KnowledgeNode>()
                        .orderByAsc(KnowledgeNode::getLevel, KnowledgeNode::getSortOrder));

        Map<Long, List<KnowledgeNode>> byParent = all.stream()
                .filter(n -> n.getParentId() != null)
                .collect(Collectors.groupingBy(KnowledgeNode::getParentId));

        return all.stream()
                .filter(n -> n.getParentId() == null)
                .sorted(Comparator.comparing(KnowledgeNode::getSortOrder))
                .map(n -> buildNode(n, byParent))
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getTree(Long subjectId) {
        if (subjectId == null) {
            return getTree();
        }
        List<KnowledgeNode> subjectNodes = nodeMapper.selectList(
                new LambdaQueryWrapper<KnowledgeNode>()
                        .eq(KnowledgeNode::getSubjectId, subjectId)
                        .orderByAsc(KnowledgeNode::getLevel, KnowledgeNode::getSortOrder));
        if (subjectNodes.isEmpty()) return List.of();

        KnowledgeNode root = subjectNodes.stream()
                .filter(n -> n.getParentId() == null)
                .findFirst().orElse(null);
        if (root == null) return List.of();

        Map<Long, List<KnowledgeNode>> byParent = subjectNodes.stream()
                .filter(n -> n.getParentId() != null)
                .collect(Collectors.groupingBy(KnowledgeNode::getParentId));

        return List.of(buildNode(root, byParent));
    }

    public List<KnowledgeNode> list(Long parentId, Integer level) {
        LambdaQueryWrapper<KnowledgeNode> w = new LambdaQueryWrapper<>();
        if (parentId != null) w.eq(KnowledgeNode::getParentId, parentId);
        if (level != null) w.eq(KnowledgeNode::getLevel, level);
        w.orderByAsc(KnowledgeNode::getSortOrder);
        return nodeMapper.selectList(w);
    }

    public List<KnowledgeNode> listBySubjectId(Long subjectId) {
        return nodeMapper.selectList(
            new LambdaQueryWrapper<KnowledgeNode>()
                .eq(KnowledgeNode::getSubjectId, subjectId)
                .orderByAsc(KnowledgeNode::getSortOrder));
    }

    public String getContent(Long nodeId) {
        return getContent(nodeId, false);
    }

    public String getContent(Long nodeId, boolean includeChildren) {
        KnowledgeNode node = nodeMapper.selectById(nodeId);
        if (node == null) return null;
        if (!includeChildren) {
            // 节点自身有内容则直接返回
            if (node.getContent() != null && !node.getContent().isEmpty()) {
                return node.getContent();
            }
            // 与 AiContentGeneratorServiceImpl.buildKnowledgePointContext 的 parent 回退一致：
            // 叶子节点无内容时向上查找父节点，确保 contentEmpty 检查与 AI 实际使用的 RAG 上下文一致
            if (node.getParentId() != null) {
                KnowledgeNode parent = nodeMapper.selectById(node.getParentId());
                if (parent != null && parent.getContent() != null && !parent.getContent().isEmpty()) {
                    return parent.getContent();
                }
            }
            return null;
        }

        Long subjectId = node.getSubjectId();
        if (subjectId == null) subjectId = findSubjectRoot(nodeId);

        List<KnowledgeNode> all = nodeMapper.selectList(
            new LambdaQueryWrapper<KnowledgeNode>()
                .eq(subjectId != null, KnowledgeNode::getSubjectId, subjectId)
        );
        Map<Long, List<KnowledgeNode>> grouped = all.stream()
            .filter(n -> n.getParentId() != null)
            .collect(Collectors.groupingBy(KnowledgeNode::getParentId));

        StringBuilder sb = new StringBuilder();
        if (node.getContent() != null && !node.getContent().isEmpty()) {
            sb.append("【").append(node.getName()).append("】\n").append(node.getContent()).append("\n\n");
        }
        collectContentRecursive(nodeId, grouped, sb);
        return sb.length() > 0 ? sb.toString() : null;
    }

    public Map<Long, String> getSubjectKnowledgeMap(Long subjectId) {
        List<KnowledgeNode> all = nodeMapper.selectList(
                new LambdaQueryWrapper<KnowledgeNode>()
                        .eq(KnowledgeNode::getSubjectId, subjectId));
        Map<Long, String> map = new LinkedHashMap<>();
        for (KnowledgeNode node : all) {
            if (node.getContent() != null && !node.getContent().isEmpty()) {
                map.put(node.getId(), node.getContent());
            }
        }
        return map;
    }

    public String getNodeFullPath(Long nodeId) {
        StringBuilder sb = new StringBuilder();
        Long cur = nodeId;
        int d = 0;
        while (cur != null && d < 10) {
            KnowledgeNode node = nodeMapper.selectById(cur);
            if (node == null) break;
            sb.insert(0, node.getName());
            if (node.getParentId() != null) sb.insert(0, " > ");
            cur = node.getParentId();
            d++;
        }
        return sb.toString();
    }

    public Long findSubjectRoot(Long nodeId) {
        KnowledgeNode node = nodeMapper.selectById(nodeId);
        if (node == null) return null;
        if (node.getSubjectId() != null) return node.getSubjectId();
        Long current = node.getParentId();
        int d = 0;
        while (current != null && d < 10) {
            KnowledgeNode parent = nodeMapper.selectById(current);
            if (parent == null) break;
            if (parent.getSubjectId() != null) return parent.getSubjectId();
            current = parent.getParentId();
            d++;
        }
        return null;
    }

    public Map<String, Object> getNodeLearningResources(Long nodeId) {
        KnowledgeNode node = nodeMapper.selectById(nodeId);
        if (node == null) throw new BusinessException(404, "知识节点不存在");

        String lr = node.getLearningResources();
        String status = node.getResourceStatus();
        String rejectReason = node.getResourceRejectReason();

        if (lr == null || lr.isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("nodeId", nodeId);
            empty.put("hasResources", false);
            if (status != null) empty.put("resourceStatus", status);
            empty.put("message", "该知识点暂无学习资源");
            return empty;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nodeId", nodeId);
        result.put("hasResources", true);
        result.put("resourceStatus", status);

        if ("REJECTED".equals(status)) {
            result.put("message", "该资源已被教师拒绝" +
                (rejectReason != null ? "，原因: " + rejectReason : ""));
            return result;
        }

        if ("PENDING".equals(status)) {
            result.put("message", "学习资源生成中，请稍候");
            return result;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> lrMap = objectMapper.readValue(lr, Map.class);
            result.put("learningResources", lrMap);
        } catch (Exception e) {
            result.put("error", "资源数据解析失败");
        }
        return result;
    }

    public List<Long> collectChildIds(Long parentId, Long subjectId) {
        List<KnowledgeNode> all = nodeMapper.selectList(
            new LambdaQueryWrapper<KnowledgeNode>()
                .eq(subjectId != null, KnowledgeNode::getSubjectId, subjectId)
        );
        Map<Long, List<KnowledgeNode>> grouped = all.stream()
            .filter(n -> n.getParentId() != null)
            .collect(Collectors.groupingBy(KnowledgeNode::getParentId));
        List<Long> result = new ArrayList<>();
        collectRecursive(parentId, grouped, result);
        return result;
    }

    private Map<String, Object> buildNode(KnowledgeNode node,
                                          Map<Long, List<KnowledgeNode>> children) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", node.getId());
        item.put("parentId", node.getParentId());
        item.put("subjectId", node.getSubjectId());
        item.put("name", node.getName());
        item.put("content", node.getContent());
        item.put("level", node.getLevel());
        item.put("sortOrder", node.getSortOrder());
        item.put("renderType", node.getRenderType());
        item.put("resourceStatus", node.getResourceStatus());
        item.put("resourceGeneratedAt", node.getResourceGeneratedAt());
        item.put("resourceVersion", node.getResourceVersion());
        if ("APPROVED".equals(node.getResourceStatus()) && node.getLearningResources() != null
                && node.getLearningResources().length() < 2048) {
            try {
                item.put("learningResources", objectMapper.readValue(node.getLearningResources(), Map.class));
            } catch (Exception ignored) { log.debug("学习资源JSON解析失败", ignored); }
        }
        List<Map<String, Object>> subs = new ArrayList<>();
        List<KnowledgeNode> kids = children.get(node.getId());
        if (kids != null) {
            kids.stream().sorted(Comparator.comparing(KnowledgeNode::getSortOrder))
                    .forEach(k -> subs.add(buildNode(k, children)));
        }
        item.put("children", subs);
        return item;
    }

    private void collectRecursive(Long parentId, Map<Long, List<KnowledgeNode>> grouped, List<Long> result) {
        List<KnowledgeNode> children = grouped.getOrDefault(parentId, Collections.emptyList());
        for (KnowledgeNode child : children) {
            result.add(child.getId());
            collectRecursive(child.getId(), grouped, result);
        }
    }

    private void collectContentRecursive(Long parentId, Map<Long, List<KnowledgeNode>> grouped, StringBuilder sb) {
        List<KnowledgeNode> children = grouped.getOrDefault(parentId, Collections.emptyList());
        for (KnowledgeNode child : children) {
            if (child.getContent() != null && !child.getContent().isEmpty()) {
                sb.append("【").append(child.getName()).append("】\n").append(child.getContent()).append("\n\n");
            }
            collectContentRecursive(child.getId(), grouped, sb);
        }
    }
}

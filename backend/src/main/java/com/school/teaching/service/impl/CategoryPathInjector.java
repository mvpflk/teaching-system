package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.AiCallLog;
import com.school.teaching.entity.KnowledgeNode;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.AiCallLogMapper;
import com.school.teaching.mapper.KnowledgeNodeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryPathInjector {

    private final KnowledgeNodeMapper nodeMapper;
    private final AiCallLogMapper aiCallLogMapper;

    public void injectCategoryPath(Map<String, Object> params) {
        Object cidObj = params.get("categoryId");
        if (cidObj == null) return;
        try {
            Long cid = cidObj instanceof Number n ? n.longValue() : Long.parseLong(cidObj.toString());
            // 全量加载知识节点构建路径映射（后续可改为按 parentId 链式查询或 Redis 缓存）
            List<KnowledgeNode> all = nodeMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeNode>()
                    .eq(KnowledgeNode::getStatus, 1));
            Map<Long, KnowledgeNode> nodeMap = new HashMap<>();
            for (KnowledgeNode c : all) nodeMap.put(c.getId(), c);
            List<String> path = new ArrayList<>();
            KnowledgeNode current = nodeMap.get(cid);
            while (current != null) {
                path.add(0, current.getName());
                current = current.getParentId() != null ? nodeMap.get(current.getParentId()) : null;
            }
            if (!path.isEmpty()) {
                params.put("categoryPath", String.join(" → ", path));
                params.put("subject", path.get(0));
                Object cidsObj = params.get("categoryIds");
                if (cidsObj instanceof List && !((List<?>) cidsObj).isEmpty()) {
                    List<Map<String, Object>> paths = new ArrayList<>();
                    for (Object o : (List<?>) cidsObj) {
                        Long nid = o instanceof Number ? ((Number) o).longValue() : Long.parseLong(o.toString());
                        List<String> p = new ArrayList<>();
                        Long cur = nid;
                        int depth = 0;
                        while (cur != null && depth < 6) {
                            KnowledgeNode kn = nodeMap.get(cur);
                            if (kn == null) break;
                            p.add(0, kn.getName());
                            cur = kn.getParentId();
                            depth++;
                        }
                        Map<String, Object> entry = new HashMap<>();
                        entry.put("nodeId", nid);
                        entry.put("path", String.join(" → ", p));
                        entry.put("name", p.isEmpty() ? "" : p.get(p.size() - 1));
                        paths.add(entry);
                    }
                    params.put("categoryPaths", paths);
                    params.put("nodeCount", paths.size());
                }
            }
            params.putIfAbsent("stageHint", "");
        } catch (Exception e) {
            log.warn("注入分类路径失败: {}", e.getMessage());
        }
    }

    public long checkQuota(Long teacherId, int dailyQuota) {
        long today = aiCallLogMapper.selectCount(
            new LambdaQueryWrapper<AiCallLog>()
                .eq(AiCallLog::getUserId, teacherId)
                .eq(AiCallLog::getCapability, "QUESTION_GEN")
                .ge(AiCallLog::getCreatedAt, LocalDate.now().atStartOfDay()));
        if (today > dailyQuota) {
            throw new BusinessException(429, "今日生成配额已用完（" + dailyQuota + "道/天）");
        }
        return today;
    }
}

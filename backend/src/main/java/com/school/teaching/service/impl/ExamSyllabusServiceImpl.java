package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.entity.ExamSyllabus;
import com.school.teaching.entity.ExamSyllabusNodeRelation;
import com.school.teaching.entity.KnowledgeNode;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.ExamSyllabusMapper;
import com.school.teaching.mapper.ExamSyllabusNodeRelationMapper;
import com.school.teaching.mapper.KnowledgeNodeMapper;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.ExamSyllabusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExamSyllabusServiceImpl implements ExamSyllabusService {

    @Autowired private ExamSyllabusMapper mapper;
    @Autowired private ExamSyllabusNodeRelationMapper relationMapper;
    @Autowired private KnowledgeNodeMapper nodeMapper;

    @Override
    public Page<ExamSyllabus> list(Long subjectId, String examType, int page, int size) {
        LambdaQueryWrapper<ExamSyllabus> w = new LambdaQueryWrapper<>();
        if (subjectId != null) w.eq(ExamSyllabus::getSubjectId, subjectId);
        if (examType != null && !examType.isEmpty()) w.eq(ExamSyllabus::getExamType, examType);
        w.orderByAsc(ExamSyllabus::getSubjectId);
        return mapper.selectPage(new Page<>(page, size), w);
    }

    @Override
    public ExamSyllabus getById(Long id) {
        ExamSyllabus s = mapper.selectById(id);
        if (s == null) throw new BusinessException(404, "考纲不存在");
        return s;
    }

    @Override
    public ExamSyllabus getBySubject(Long subjectId, String examType) {
        if (examType == null || examType.isEmpty()) examType = "GENERAL";
        return mapper.selectOne(new LambdaQueryWrapper<ExamSyllabus>()
                .eq(ExamSyllabus::getSubjectId, subjectId)
                .eq(ExamSyllabus::getExamType, examType)
                .eq(ExamSyllabus::getStatus, 1));
    }

    @Override
    @Transactional
    public ExamSyllabus create(ExamSyllabus syllabus) {
        Long userId = SecurityUtils.getCurrentUserId();
        syllabus.setCreatedBy(userId);
        syllabus.setUpdatedBy(userId);
        if (syllabus.getStatus() == null) syllabus.setStatus(1);
        if (syllabus.getExamType() == null) syllabus.setExamType("GENERAL");
        if (syllabus.getKnowledgeDim() == null) syllabus.setKnowledgeDim("BOTH");
        if (syllabus.getVersion() == null) syllabus.setVersion("1.0");
        mapper.insert(syllabus);
        return syllabus;
    }

    @Override
    @Transactional
    public ExamSyllabus update(Long id, ExamSyllabus syllabus) {
        ExamSyllabus existing = getById(id);
        if (syllabus.getTitle() != null) existing.setTitle(syllabus.getTitle());
        if (syllabus.getContent() != null) existing.setContent(syllabus.getContent());
        if (syllabus.getExamType() != null) existing.setExamType(syllabus.getExamType());
        if (syllabus.getKnowledgeDim() != null) existing.setKnowledgeDim(syllabus.getKnowledgeDim());
        if (syllabus.getVersion() != null) existing.setVersion(syllabus.getVersion());
        existing.setUpdatedBy(SecurityUtils.getCurrentUserId());
        mapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getById(id);
        mapper.deleteById(id);
    }

    @Override
    @Transactional
    public void toggleStatus(Long id) {
        ExamSyllabus s = getById(id);
        s.setStatus(s.getStatus() == 1 ? 0 : 1);
        mapper.updateById(s);
    }

    @Override
    public String getSyllabusPromptContext(Long subjectId) {
        ExamSyllabus syllabus = getBySubject(subjectId, "GENERAL");
        if (syllabus == null || syllabus.getContent() == null) return "";

        String content = syllabus.getContent();
        StringBuilder ctx = new StringBuilder();
        ctx.append("【升学考试考纲依据 — ").append(syllabus.getTitle()).append("】\n");

        String[] sections = java.util.regex.Pattern.compile("(?=^#{1,3}\\s)", java.util.regex.Pattern.MULTILINE).split(content);
        for (String section : sections) {
            String lower = section.toLowerCase();
            if (lower.contains("应知") || lower.contains("应会")
                    || lower.contains("考试范围") || lower.contains("考试内容")
                    || lower.contains("分值") || lower.contains("题型")
                    || lower.contains("考核") || lower.contains("要求")) {
                ctx.append(section.trim()).append("\n\n");
            }
        }
        String result = ctx.toString();
        if (result.length() > 3000) result = result.substring(0, 3000) + "\n...(考纲内容已截断)";
        return result;
    }

    // ── 知识节点关联 ──

    @Override
    public List<Long> getNodeIds(Long syllabusId) {
        return relationMapper.selectList(new LambdaQueryWrapper<ExamSyllabusNodeRelation>()
                .eq(ExamSyllabusNodeRelation::getSyllabusId, syllabusId))
                .stream().map(ExamSyllabusNodeRelation::getNodeId).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void saveNodeRelations(Long syllabusId, List<Long> nodeIds) {
        // 清空旧关联
        relationMapper.delete(new LambdaQueryWrapper<ExamSyllabusNodeRelation>()
                .eq(ExamSyllabusNodeRelation::getSyllabusId, syllabusId));
        // 写入新关联
        if (nodeIds != null) {
            for (Long nodeId : nodeIds.stream().distinct().toList()) {
                ExamSyllabusNodeRelation r = new ExamSyllabusNodeRelation();
                r.setSyllabusId(syllabusId);
                r.setNodeId(nodeId);
                relationMapper.insert(r);
            }
        }
    }

    @Override
    public List<ExamSyllabus> getSyllabiByNodeId(Long nodeId) {
        List<Long> nodeIds = getAncestorNodeIds(nodeId);
        if (nodeIds.isEmpty()) return Collections.emptyList();
        List<Long> syllabusIds = relationMapper.selectList(new LambdaQueryWrapper<ExamSyllabusNodeRelation>()
                .in(ExamSyllabusNodeRelation::getNodeId, nodeIds))
                .stream().map(ExamSyllabusNodeRelation::getSyllabusId).distinct().toList();
        if (syllabusIds.isEmpty()) return Collections.emptyList();
        return mapper.selectList(new LambdaQueryWrapper<ExamSyllabus>()
                .in(ExamSyllabus::getId, syllabusIds)
                .eq(ExamSyllabus::getStatus, 1));
    }

    /** 收集节点及其所有祖先节点ID */
    private List<Long> getAncestorNodeIds(Long nodeId) {
        if (nodeId == null) return Collections.emptyList();
        List<Long> nodeIds = new ArrayList<>();
        Long current = nodeId;
        while (current != null) {
            nodeIds.add(current);
            KnowledgeNode node = nodeMapper.selectById(current);
            current = node != null ? node.getParentId() : null;
        }
        return nodeIds;
    }

    /** 判断考纲section是否匹配当前节点的祖先链 */
    private boolean sectionMatchesNode(String section, List<Long> ancestorIds) {
        // 提取 <!-- node:ID --> 标记
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("<!--\\s*node:(\\d+)\\s*-->").matcher(section);
        if (!m.find()) return true; // 无标记 → 通用段落（考试范围/题型分值/考核要求），始终包含
        long markerId = Long.parseLong(m.group(1));
        return ancestorIds.contains(markerId);
    }

    @Override
    public String getSyllabusPromptContextByNode(Long nodeId) {
        List<Long> ancestorIds = getAncestorNodeIds(nodeId);
        List<ExamSyllabus> syllabi = getSyllabiByNodeId(nodeId);
        if (syllabi.isEmpty()) return "";

        StringBuilder ctx = new StringBuilder();
        ctx.append("【升学考试考纲依据(精准匹配)】\n");
        int totalLen = 0;
        int maxLen = 2000;
        for (ExamSyllabus syllabus : syllabi) {
            if (totalLen >= maxLen) break;
            String content = syllabus.getContent();
            if (content == null) continue;
            // 按 ### 级别拆分，每小节可独立匹配
            String[] sections = content.split("(?=^#{2,3}\\s)");
            boolean hasMatched = false;
            for (String section : sections) {
                if (totalLen >= maxLen) break;
                if (!sectionMatchesNode(section, ancestorIds)) continue;
                String lower = section.toLowerCase();
                if (lower.contains("应知") || lower.contains("应会")
                        || lower.contains("考试范围") || lower.contains("考核")
                        || lower.contains("分值") || lower.contains("要求")) {
                    if (!hasMatched) {
                        ctx.append("\n— ").append(syllabus.getTitle()).append(" —\n");
                        hasMatched = true;
                    }
                    ctx.append(section.trim()).append("\n\n");
                    totalLen += section.length();
                }
            }
        }
        if (totalLen == 0) return "";
        String result = ctx.toString();
        if (result.length() > maxLen) result = result.substring(0, maxLen) + "\n...(已截断)";
        return result;
    }

    @Override
    public String getSyllabusMeta(Long subjectId) {
        if (subjectId == null) return null;
        ExamSyllabus syl = mapper.selectOne(
            new LambdaQueryWrapper<ExamSyllabus>()
                .eq(ExamSyllabus::getSubjectId, subjectId)
                .eq(ExamSyllabus::getStatus, 1)
                .orderByDesc(ExamSyllabus::getVersion)
                .last("LIMIT 1"));
        return syl != null ? syl.getSyllabusMeta() : null;
    }

    @Override
    public java.util.Map<String, Object> healthCheck() {
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        java.util.List<java.util.Map<String, Object>> items = new java.util.ArrayList<>();
        java.util.List<ExamSyllabus> all = mapper.selectList(
            new LambdaQueryWrapper<ExamSyllabus>().eq(ExamSyllabus::getStatus, 1));
        int ok = 0, warn = 0;
        for (ExamSyllabus s : all) {
            java.util.Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("id", s.getId()); item.put("title", s.getTitle());
            boolean valid = s.getSyllabusMeta() != null;
            if (valid) {
                try {
                    com.fasterxml.jackson.databind.JsonNode meta = new com.fasterxml.jackson.databind.ObjectMapper().readTree(s.getSyllabusMeta());
                    // 只需 JSON 可解析且至少含 1 个 key 即视为健康
                    valid = meta.size() > 0;
                    if (!valid) item.put("reason", "syllabus_meta JSON is empty");
                } catch (Exception e) { valid = false; item.put("parseError", e.getMessage()); }
            }
            item.put("valid", valid);
            if (valid) ok++; else { warn++; item.put("level", valid ? null : "L3 fallback active — check syllabus_meta JSON"); }
            items.add(item);
        }
        result.put("total", all.size()); result.put("healthy", ok); result.put("degraded", warn); result.put("items", items);
        if (warn > 0) result.put("warning", warn + " syllabi using L3 hardcoded defaults instead of structured metadata");
        return result;
    }
}

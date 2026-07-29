package com.school.teaching.service.impl;

import com.school.teaching.entity.KnowledgeNode;
import com.school.teaching.service.KnowledgeNodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class KnowledgeNodeServiceImpl implements KnowledgeNodeService {

    @Autowired
    private KnowledgeNodeReadService readService;

    @Autowired
    private KnowledgeNodeManageService manageService;

    // === Read methods → ReadService ===

    @Override
    public long getTreeVersion() {
        return readService.getTreeVersion();
    }

    @Override
    public List<Map<String, Object>> getTree() {
        return readService.getTree();
    }

    @Override
    public List<Map<String, Object>> getTree(Long subjectId) {
        return readService.getTree(subjectId);
    }

    @Override
    public List<KnowledgeNode> list(Long parentId, Integer level) {
        return readService.list(parentId, level);
    }

    @Override
    public List<KnowledgeNode> listBySubjectId(Long subjectId) {
        return readService.listBySubjectId(subjectId);
    }

    @Override
    public String getContent(Long nodeId) {
        return readService.getContent(nodeId);
    }

    @Override
    public String getContent(Long nodeId, boolean includeChildren) {
        return readService.getContent(nodeId, includeChildren);
    }

    @Override
    public Map<Long, String> getSubjectKnowledgeMap(Long subjectId) {
        return readService.getSubjectKnowledgeMap(subjectId);
    }

    @Override
    public Long findSubjectRoot(Long nodeId) {
        return readService.findSubjectRoot(nodeId);
    }

    @Override
    public String getNodeFullPath(Long nodeId) {
        return readService.getNodeFullPath(nodeId);
    }

    @Override
    public Map<String, Object> getNodeLearningResources(Long nodeId) {
        return readService.getNodeLearningResources(nodeId);
    }

    // === Write methods → ManageService ===

    @Override
    public KnowledgeNode create(KnowledgeNode node) {
        return manageService.create(node);
    }

    @Override
    public KnowledgeNode update(Long id, KnowledgeNode node) {
        return manageService.update(id, node);
    }

    @Override
    public void delete(Long id) {
        manageService.delete(id);
    }

    @Override
    public void setContent(Long nodeId, String markdownContent) {
        manageService.setContent(nodeId, markdownContent);
    }

    @Override
    public int importFromExcel(MultipartFile file) throws Exception {
        return manageService.importFromExcel(file);
    }

    @Override
    public int importFromZip(Long subjectId, MultipartFile zipFile) throws Exception {
        return manageService.importFromZip(subjectId, zipFile);
    }

    @Override
    public int importFromTxt(Long subjectId, MultipartFile file) throws Exception {
        return manageService.importFromTxt(subjectId, file);
    }

    @Override
    public int importFromDocx(Long subjectId, MultipartFile file) throws Exception {
        return manageService.importFromDocx(subjectId, file);
    }

    @Override
    public void clearBySubject(Long subjectId) {
        manageService.clearBySubject(subjectId);
    }

    @Override
    public Map<String, Object> generateLearningResources(Long nodeId) {
        return manageService.generateLearningResources(nodeId);
    }

    @Override
    public void reviewResource(Long nodeId, String status, String rejectReason) {
        manageService.reviewResource(nodeId, status, rejectReason);
    }

    @Override
    public List<Map<String, Object>> checkVideoLinks() {
        return manageService.checkVideoLinks();
    }
}

package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.DictSubject;
import com.school.teaching.entity.KnowledgeNode;
import com.school.teaching.mapper.KnowledgeNodeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.school.teaching.service.impl.KnowledgeNodeHelper.isValidContent;

@Component
public class KnowledgeNodeImportHelper {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeNodeImportHelper.class);

    private final KnowledgeNodeMapper nodeMapper;

    public KnowledgeNodeImportHelper(KnowledgeNodeMapper nodeMapper) {
        this.nodeMapper = nodeMapper;
    }

    private static final int LV_SUBJECT = 1;
    private static final int LV_CHAPTER = 2;
    private static final int LV_TASK    = 3;
    private static final int LV_KP      = 4;

    @Transactional
    public int doImportFromLines(Long subjectId, DictSubject subject, String text) {
        KnowledgeNode subjectNode = ensureSubjectNode(subjectId, subject);

        List<KnowledgeNode> existing = nodeMapper.selectList(
                new LambdaQueryWrapper<KnowledgeNode>()
                        .eq(KnowledgeNode::getSubjectId, subjectId));
        Map<String, KnowledgeNode> chapterMap = new LinkedHashMap<>();
        Map<String, KnowledgeNode> taskMap = new LinkedHashMap<>();
        Map<String, KnowledgeNode> kpMap = new LinkedHashMap<>();
        for (KnowledgeNode n : existing) {
            if (n.getName() == null) continue;
            if (n.getLevel() != null && n.getLevel() == LV_CHAPTER) {
                chapterMap.put(n.getName().trim(), n);
            } else if (n.getLevel() != null && n.getLevel() == LV_TASK && n.getParentId() != null) {
                taskMap.put((n.getParentId() + ":" + n.getName()).trim(), n);
            } else if (n.getLevel() != null) {
                kpMap.put((n.getParentId() + ":" + n.getName()).trim(), n);
            }
        }

        int[] stats = {0, 0, 0};
        KnowledgeNode[] ctx = new KnowledgeNode[3];
        StringBuilder currentContent = new StringBuilder();

        String[] lines = text.replace("\r\n", "\n").replace("\r", "\n").split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            if (trimmed.startsWith("### ")) {
                flushCurrentKp(ctx[2], currentContent);
                String kpName = trimmed.substring(4).trim();
                if (!kpName.isEmpty() && ctx[1] != null) {
                    boolean existed = kpMap.containsKey(ctx[1].getId() + ":" + kpName);
                    ctx[2] = ensureKp(subjectId, ctx[1], kpName, LV_KP, kpMap);
                    if (!existed) stats[2]++;
                    currentContent.setLength(0);
                }
            } else if (trimmed.startsWith("## ")) {
                flushCurrentKp(ctx[2], currentContent);
                String taskName = trimmed.substring(3).trim();
                if (!taskName.isEmpty() && ctx[0] != null) {
                    boolean existed = taskMap.containsKey(ctx[0].getId() + ":" + taskName);
                    ctx[1] = ensureTask(subjectId, ctx[0], taskName, taskMap);
                    if (!existed) stats[1]++;
                    ctx[2] = null;
                }
            } else if (trimmed.startsWith("# ")) {
                flushCurrentKp(ctx[2], currentContent);
                String chapterName = trimmed.substring(2).trim();
                if (!chapterName.isEmpty()) {
                    boolean existed = chapterMap.containsKey(chapterName.trim());
                    ctx[0] = ensureChapter(subjectId, subjectNode, chapterName, chapterMap);
                    if (!existed) stats[0]++;
                    ctx[1] = null; ctx[2] = null;
                }
            } else {
                if (ctx[2] != null) {
                    if (currentContent.length() > 0) currentContent.append("\n");
                    currentContent.append(trimmed);
                } else if (ctx[0] != null && trimmed.length() >= 10) {
                    String kpName = trimmed.length() > 40 ? trimmed.substring(0, 40) : trimmed;
                    ctx[2] = ensureKp(subjectId, ctx[0], kpName, ctx[0].getLevel() + 1, kpMap);
                    currentContent.append(trimmed);
                    stats[2]++;
                    flushCurrentKp(ctx[2], currentContent);
                    ctx[2] = null;
                }
            }
        }
        flushCurrentKp(ctx[2], currentContent);

        return stats[0] + stats[1] + stats[2];
    }

    private KnowledgeNode ensureSubjectNode(Long subjectId, DictSubject subject) {
        KnowledgeNode sn = nodeMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeNode>()
                        .eq(KnowledgeNode::getLevel, LV_SUBJECT)
                        .eq(KnowledgeNode::getSubjectId, subjectId)
                        .last("LIMIT 1"));
        if (sn == null) {
            sn = new KnowledgeNode();
            sn.setSubjectId(subjectId); sn.setLevel(LV_SUBJECT);
            sn.setName(subject.getSubjectName()); sn.setSortOrder(0);
            nodeMapper.insert(sn);
        }
        return sn;
    }

    private KnowledgeNode ensureChapter(Long subjectId, KnowledgeNode subjectNode,
                                        String name, Map<String, KnowledgeNode> cache) {
        KnowledgeNode n = cache.get(name.trim());
        if (n == null) {
            n = new KnowledgeNode();
            n.setSubjectId(subjectId); n.setParentId(subjectNode.getId());
            n.setName(name.trim()); n.setLevel(LV_CHAPTER); n.setSortOrder(0);
            nodeMapper.insert(n);
            cache.put(name.trim(), n);
        }
        return n;
    }

    private KnowledgeNode ensureTask(Long subjectId, KnowledgeNode chapter,
                                     String name, Map<String, KnowledgeNode> cache) {
        String key = chapter.getId() + ":" + name.trim();
        KnowledgeNode n = cache.get(key);
        if (n == null) {
            n = new KnowledgeNode();
            n.setSubjectId(subjectId); n.setParentId(chapter.getId());
            n.setName(name.trim()); n.setLevel(LV_TASK); n.setSortOrder(0);
            nodeMapper.insert(n);
            cache.put(key, n);
        }
        return n;
    }

    private KnowledgeNode ensureKp(Long subjectId, KnowledgeNode parent,
                                   String name, int level, Map<String, KnowledgeNode> cache) {
        String key = parent.getId() + ":" + name.trim();
        KnowledgeNode n = cache.get(key);
        if (n == null) {
            n = new KnowledgeNode();
            n.setSubjectId(subjectId); n.setParentId(parent.getId());
            n.setName(name.trim()); n.setLevel(level); n.setSortOrder(0);
            nodeMapper.insert(n);
            cache.put(key, n);
        }
        return n;
    }

    private void flushCurrentKp(KnowledgeNode kp, StringBuilder buf) {
        if (kp == null || buf.length() == 0) return;
        String c = buf.toString().trim();
        if (isValidContent(c, kp.getName())) {
            kp.setContent(c);
            nodeMapper.updateById(kp);
        }
        buf.setLength(0);
    }
}

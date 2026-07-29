package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.dto.DailyCardDTO;
import com.school.teaching.dto.ExamPrepPackDTO;
import com.school.teaching.dto.FlashcardDTO;
import com.school.teaching.dto.QuickReviewDTO;
import com.school.teaching.dto.RelatedCardsDTO;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.service.CardRecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CardRecommendationServiceImpl implements CardRecommendationService {

    @Autowired private KnowledgeFlashcardMapper flashcardMapper;
    @Autowired private KnowledgeArticleMapper articleMapper;
    @Autowired private KnowledgeReviewScheduleMapper scheduleMapper;
    @Autowired private KnowledgeNodeMapper nodeMapper;
    @Autowired private PrecisionProgressMapper precisionProgressMapper;
    @Autowired private WrongQuestionMapper wrongQuestionMapper;
    @Autowired private QuestionBankMapper questionBankMapper;
    @Autowired private StudentAnswerMapper studentAnswerMapper;
    @Autowired private TaskSubmissionMapper taskSubmissionMapper;
    @Autowired private TeacherClassMapper teacherClassMapper;
    @Autowired private StudentMapper studentMapper;
    @Autowired private ClassesMapper classesMapper;              // v169: 学生专业隔离
    @Autowired private DictMajorMapper majorMapper;              // v169: 学生专业隔离
    @Autowired private DictMajorSubjectMapper majorSubjectMapper; // v169: 学生专业隔离
    @Autowired private CreditTransactionMapper creditTransactionMapper;
    @Autowired private TaskQuestionMapper taskQuestionMapper;
    @Autowired private TaskMapper taskMapper;

    @Override
    @Transactional(readOnly = true)
    public DailyCardDTO getDailyCard(Long studentId) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        Set<Long> reviewedIds = scheduleMapper.selectList(
            new LambdaQueryWrapper<KnowledgeReviewSchedule>()
                .eq(KnowledgeReviewSchedule::getStudentId, studentId)
                .ge(KnowledgeReviewSchedule::getLastReviewAt, todayStart)
        ).stream().map(KnowledgeReviewSchedule::getFlashcardId).collect(Collectors.toSet());

        int todayReviewed = reviewedIds.size();

        // v167: 加权推荐 — 从候选池按优先级加权随机选择
        DailyCardDTO dto = pickWeightedCard(studentId, reviewedIds);

        if (dto != null) fillStats(dto, studentId, todayReviewed);
        return dto;
    }

    /**
     * v167: 从候选池加权随机选卡。
     * 候选来源：错题关联 → 薄弱知识点 → 随机兜底
     * 权重 = qualityWeight × masteryGap × examWeight × reviewBonus
     */
    private DailyCardDTO pickWeightedCard(Long studentId, Set<Long> reviewedIds) {
        List<CardCandidate> candidates = new java.util.ArrayList<>();

        // v169: 解析学生可访问的学科（专业隔离，防跨专业推荐）
        Set<Long> accessibleSubjectIds = getAccessibleSubjectIds(studentId);

        collectWrongQuestionCards(studentId, reviewedIds, candidates, accessibleSubjectIds);
        collectWeakPointCards(studentId, reviewedIds, candidates, accessibleSubjectIds);
        collectRandomCards(reviewedIds, candidates, accessibleSubjectIds);

        if (candidates.isEmpty()) return null;

        // 批量预加载：收集所有候选卡片的 nodeId，一次性查 knowledge_nodes + precision_progress
        Set<Long> nodeIds = candidates.stream()
            .map(c -> c.nodeId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, KnowledgeNode> nodeMap = Collections.emptyMap();
        if (!nodeIds.isEmpty()) {
            nodeMap = nodeMapper.selectBatchIds(nodeIds).stream()
                .collect(Collectors.toMap(KnowledgeNode::getId, n -> n, (a, b) -> a));
        }
        Map<Long, PrecisionProgress> progressMap = Collections.emptyMap();
        if (!nodeIds.isEmpty()) {
            progressMap = precisionProgressMapper.selectList(
                new LambdaQueryWrapper<PrecisionProgress>()
                    .eq(PrecisionProgress::getStudentId, studentId)
                    .in(PrecisionProgress::getNodeId, nodeIds))
                .stream().collect(Collectors.toMap(PrecisionProgress::getNodeId, p -> p, (a, b) -> a));
        }

        // 计算权重
        for (CardCandidate c : candidates) {
            KnowledgeNode node = nodeMap.get(c.nodeId);
            PrecisionProgress pp = progressMap.get(c.nodeId);
            c.weight = getQualityWeight(c.card)
                     * getMasteryGap(pp)
                     * getExamWeightValue(node)
                     * getReviewStatusBonus(c.card);
        }

        // 按权重降序，Top20加权随机
        candidates.sort((a, b) -> Double.compare(b.weight, a.weight));
        int poolSize = Math.min(20, candidates.size());
        double totalWeight = 0;
        for (int i = 0; i < poolSize; i++) totalWeight += candidates.get(i).weight;

        if (totalWeight <= 0) {
            return candidates.get((int) (Math.random() * poolSize)).toDTO();
        }

        double random = Math.random() * totalWeight;
        double cumulative = 0;
        for (int i = 0; i < poolSize; i++) {
            cumulative += candidates.get(i).weight;
            if (random <= cumulative) return candidates.get(i).toDTO();
        }
        return candidates.get(0).toDTO();
    }

    private double getQualityWeight(KnowledgeFlashcard card) {
        if (card.getQualityScore() == null) return 0.6;
        double score = card.getQualityScore().doubleValue();
        if (score >= 80) return 1.0;
        if (score >= 60) return 0.8;
        if (score >= 40) return 0.5;
        return 0.2;
    }

    /** 掌握度缺口：precisionProgress 已批量预加载，无需再查 DB */
    private double getMasteryGap(PrecisionProgress pp) {
        if (pp == null || pp.getMasteryPercent() == null) return 0.7;
        return Math.max(0.1, (100.0 - pp.getMasteryPercent().doubleValue()) / 100.0);
    }

    /** 考纲权重系数：knowledgeNode 已批量预加载 */
    private double getExamWeightValue(KnowledgeNode node) {
        if (node == null || node.getExamWeight() == null) return 1.0;
        return switch (node.getExamWeight()) {
            case "HIGH" -> 1.5;
            case "LOW" -> 0.5;
            default -> 1.0;
        };
    }

    /** 审核状态加成：已审核的优先，待审核降权，已拒绝排除 */
    private double getReviewStatusBonus(KnowledgeFlashcard card) {
        String status = card.getReviewStatus();
        if (status == null) return 0.5;  // 旧卡无状态
        return switch (status) {
            case "APPROVED" -> 1.0;
            case "PENDING" -> 0.4;
            case "REJECTED" -> 0.0;  // 完全排除
            default -> 0.5;
        };
    }

    /** 候选卡片内部类 */
    private class CardCandidate {
        KnowledgeFlashcard card;
        KnowledgeArticle article;
        Long nodeId;  // 缓存 article.nodeId，避免 getMasteryGap/getExamWeightValue 重复查 DB
        String reason;
        double weight;

        DailyCardDTO toDTO() {
            return buildDTO(card, article, reason);
        }
    }

    private void collectWrongQuestionCards(Long studentId, Set<Long> reviewedIds,
                                            List<CardCandidate> candidates, Set<Long> accessibleSubjectIds) {
        List<WrongQuestion> wrongList = wrongQuestionMapper.selectList(
            new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getStudentId, studentId)
                .eq(WrongQuestion::getIsMastered, 0)
                .orderByDesc(WrongQuestion::getLastWrongTime)
                .last("LIMIT 10"));
        if (wrongList.isEmpty()) return;

        Set<Long> questionIds = wrongList.stream()
            .map(WrongQuestion::getQuestionId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (questionIds.isEmpty()) return;

        List<QuestionBank> questions = questionBankMapper.selectBatchIds(questionIds);
        Set<Long> nodeIds = questions.stream()
            .map(QuestionBank::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (nodeIds.isEmpty()) return;

        for (Long nodeId : nodeIds) {
            List<KnowledgeArticle> articles = articleMapper.selectList(
                new LambdaQueryWrapper<KnowledgeArticle>()
                    .eq(KnowledgeArticle::getNodeId, nodeId)
                    .eq(KnowledgeArticle::getStatus, "PUBLISHED"));
            for (KnowledgeArticle article : articles) {
                // v169: 专业隔离 — 仅推荐学生本专业 + 公共学科的文章
                if (accessibleSubjectIds != null && !accessibleSubjectIds.contains(article.getSubjectId())) continue;
                List<KnowledgeFlashcard> cards = flashcardMapper.selectList(
                    new LambdaQueryWrapper<KnowledgeFlashcard>()
                        .eq(KnowledgeFlashcard::getArticleId, article.getId()));
                for (KnowledgeFlashcard card : cards) {
                    if (!isQualityCard(card)) continue;
                    if (reviewedIds.contains(card.getId())) continue;
                    if ("REJECTED".equals(card.getReviewStatus())) continue;
                    CardCandidate c = new CardCandidate();
                    c.card = card; c.article = article; c.nodeId = article.getNodeId(); c.reason = "wrong_question";
                    candidates.add(c);
                }
            }
        }
    }

    private void collectWeakPointCards(Long studentId, Set<Long> reviewedIds,
                                        List<CardCandidate> candidates, Set<Long> accessibleSubjectIds) {
        List<PrecisionProgress> weakList = precisionProgressMapper.selectList(
            new LambdaQueryWrapper<PrecisionProgress>()
                .eq(PrecisionProgress::getStudentId, studentId)
                .lt(PrecisionProgress::getMasteryPercent, new java.math.BigDecimal("70"))
                .orderByAsc(PrecisionProgress::getMasteryPercent)
                .last("LIMIT 10"));

        for (PrecisionProgress pp : weakList) {
            if (pp.getNodeId() == null) continue;
            List<KnowledgeArticle> articles = articleMapper.selectList(
                new LambdaQueryWrapper<KnowledgeArticle>()
                    .eq(KnowledgeArticle::getNodeId, pp.getNodeId())
                    .eq(KnowledgeArticle::getStatus, "PUBLISHED"));
            for (KnowledgeArticle article : articles) {
                // v169: 专业隔离 — 仅推荐学生本专业 + 公共学科的文章
                if (accessibleSubjectIds != null && !accessibleSubjectIds.contains(article.getSubjectId())) continue;
                List<KnowledgeFlashcard> cards = flashcardMapper.selectList(
                    new LambdaQueryWrapper<KnowledgeFlashcard>()
                        .eq(KnowledgeFlashcard::getArticleId, article.getId()));
                for (KnowledgeFlashcard card : cards) {
                    if (reviewedIds.contains(card.getId())) continue;
                    if ("REJECTED".equals(card.getReviewStatus())) continue;
                    CardCandidate c = new CardCandidate();
                    c.card = card; c.article = article; c.nodeId = article.getNodeId(); c.reason = "weak_point";
                    candidates.add(c);
                }
            }
        }
    }

    private void collectRandomCards(Set<Long> reviewedIds, List<CardCandidate> candidates,
                                     Set<Long> accessibleSubjectIds) {
        // 只有前两个来源都没候选时才补充随机卡片
        if (!candidates.isEmpty()) return;
        List<KnowledgeFlashcard> allCards = flashcardMapper.selectList(null);
        if (allCards.isEmpty()) return;
        Collections.shuffle(allCards);
        int limit = Math.min(30, allCards.size());
        for (int i = 0; i < limit; i++) {
            KnowledgeFlashcard card = allCards.get(i);
            if (!isQualityCard(card)) continue;
            if (reviewedIds.contains(card.getId())) continue;
            if ("REJECTED".equals(card.getReviewStatus())) continue;
            KnowledgeArticle article = articleMapper.selectById(card.getArticleId());
            if (article != null && "PUBLISHED".equals(article.getStatus())) {
                // v169: 专业隔离 — 仅推荐学生本专业 + 公共学科的文章
                if (accessibleSubjectIds != null && !accessibleSubjectIds.contains(article.getSubjectId())) continue;
                CardCandidate c = new CardCandidate();
                c.card = card; c.article = article; c.nodeId = article.getNodeId(); c.reason = "random";
                candidates.add(c);
            }
        }
    }

    /** 卡片基础质量检查：前后文本均不为空且有意义 */
    private boolean isQualityCard(KnowledgeFlashcard card) {
        if (card == null) return false;
        String front = card.getFrontText();
        String back = card.getBackText();
        if (front == null || front.isBlank() || front.length() < 2) return false;
        if (back == null || back.isBlank() || back.length() < 2) return false;
        // 过滤纯占位符或无效内容
        String trimmed = front.trim();
        if (trimmed.equals("？") || trimmed.equals("?") || trimmed.equals("...")) return false;
        return true;
    }

    private DailyCardDTO buildDTO(KnowledgeFlashcard card, KnowledgeArticle article, String reason) {
        DailyCardDTO dto = new DailyCardDTO();
        dto.setCardId(card.getId());
        dto.setFrontText(card.getFrontText());
        dto.setBackText(card.getBackText());
        dto.setArticleId(article.getId());
        dto.setKnowledgeNodeId(article.getNodeId());
        dto.setReason(reason);
        dto.setCardType(card.getCardType() != null ? card.getCardType() : "DEFINITION");

        if (article.getNodeId() != null) {
            String[] names = resolveNodePath(article.getNodeId());
            dto.setKnowledgeNodeName(names[1]);
            dto.setSubjectName(names[0]);
        }
        return dto;
    }

    private String[] resolveNodePath(Long nodeId) {
        KnowledgeNode node = nodeMapper.selectById(nodeId);
        if (node == null) return new String[] { null, null };
        if (node.getLevel() != null && node.getLevel() == 1) {
            return new String[] { node.getName(), node.getName() };
        }
        KnowledgeNode level1 = findLevel1Ancestor(node);
        return new String[] {
            level1 != null ? level1.getName() : null,
            node.getName()
        };
    }

    private KnowledgeNode findLevel1Ancestor(KnowledgeNode node) {
        Long parentId = node.getParentId();
        int safety = 10;
        while (parentId != null && safety-- > 0) {
            KnowledgeNode parent = nodeMapper.selectById(parentId);
            if (parent == null) break;
            if (parent.getLevel() != null && parent.getLevel() == 1) return parent;
            parentId = parent.getParentId();
        }
        return null;
    }

    private void fillStats(DailyCardDTO dto, Long studentId, int todayReviewed) {
        dto.setTodayReviewed(todayReviewed);
        dto.setStreakDays(calcStreak(studentId));
    }

    private int calcStreak(Long studentId) {
        int streak = 0;
        LocalDate cursor = LocalDate.now();
        while (true) {
            LocalDateTime dayStart = cursor.atStartOfDay();
            LocalDateTime dayEnd = cursor.plusDays(1).atStartOfDay();
            Long count = scheduleMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeReviewSchedule>()
                    .eq(KnowledgeReviewSchedule::getStudentId, studentId)
                    .ge(KnowledgeReviewSchedule::getLastReviewAt, dayStart)
                    .lt(KnowledgeReviewSchedule::getLastReviewAt, dayEnd)
            );
            if (count > 0) {
                streak++;
                cursor = cursor.minusDays(1);
            } else {
                break;
            }
        }
        return streak;
    }

    // ═══════════════ v169: 专业隔离 ═══════════════

    /**
     * 解析学生可访问的学科 ID 集合（本专业学科 + 公共学科）。
     * 链: Student → Classes.major → DictMajor → DictMajorSubject → subjectIds
     * 返回 null 表示无法解析（兜底不过滤，即允许所有）。
     */
    private Set<Long> getAccessibleSubjectIds(Long studentId) {
        if (studentId == null) return null;
        try {
            Student student = studentMapper.selectById(studentId);
            if (student == null || student.getClassId() == null) return null;
            Classes cls = classesMapper.selectById(student.getClassId());
            if (cls == null || cls.getMajor() == null || cls.getMajor().isBlank()) return null;

            // 按专业名匹配 DictMajor
            DictMajor major = majorMapper.selectOne(
                new LambdaQueryWrapper<DictMajor>()
                    .eq(DictMajor::getMajorName, cls.getMajor())
                    .eq(DictMajor::getStatus, 1));
            if (major == null) {
                major = majorMapper.selectOne(
                    new LambdaQueryWrapper<DictMajor>()
                        .like(DictMajor::getMajorName, cls.getMajor())
                        .eq(DictMajor::getStatus, 1));
            }
            if (major == null) return null;

            // 获取该专业下所有学科
            List<DictMajorSubject> mappings = majorSubjectMapper.selectList(
                new LambdaQueryWrapper<DictMajorSubject>()
                    .eq(DictMajorSubject::getMajorId, major.getId()));
            if (mappings.isEmpty()) return null;

            return mappings.stream()
                .map(DictMajorSubject::getSubjectId)
                .collect(Collectors.toSet());
        } catch (Exception e) {
            return null; // 解析失败兜底不过滤
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RelatedCardsDTO getRelatedCards(Long submissionId, Long studentId, int limit) {
        TaskSubmission sub = taskSubmissionMapper.selectById(submissionId);
        if (sub == null || !Objects.equals(sub.getStudentId(), studentId)) return null;

        List<StudentAnswer> wrongAnswers = studentAnswerMapper.selectList(
            new LambdaQueryWrapper<StudentAnswer>()
                .eq(StudentAnswer::getSubmissionId, submissionId)
                .eq(StudentAnswer::getIsCorrect, 0)
        );
        if (wrongAnswers.isEmpty()) return null;

        Set<Long> questionIds = wrongAnswers.stream()
            .map(StudentAnswer::getQuestionId).filter(Objects::nonNull).collect(Collectors.toSet());

        List<QuestionBank> questions = questionBankMapper.selectBatchIds(questionIds);

        Map<Long, Integer> nodeWrongCount = new LinkedHashMap<>();
        Map<Long, String> nodeNameMap = new LinkedHashMap<>();
        for (QuestionBank q : questions) {
            Long nodeId = q.getCategoryId();
            if (nodeId == null || nodeId <= 0) continue;
            nodeWrongCount.merge(nodeId, 1, Integer::sum);
            if (!nodeNameMap.containsKey(nodeId)) {
                KnowledgeNode node = nodeMapper.selectById(nodeId);
                nodeNameMap.put(nodeId, node != null ? node.getName() : null);
            }
        }

        List<RelatedCardsDTO.WrongNode> wrongNodes = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : nodeWrongCount.entrySet()) {
            RelatedCardsDTO.WrongNode wn = new RelatedCardsDTO.WrongNode();
            wn.setNodeId(e.getKey());
            wn.setNodeName(nodeNameMap.get(e.getKey()));
            wn.setWrongCount(e.getValue());
            wrongNodes.add(wn);
        }

        List<RelatedCardsDTO.CardItem> cards = new ArrayList<>();
        for (RelatedCardsDTO.WrongNode wn : wrongNodes) {
            if (cards.size() >= limit) break;
            List<KnowledgeArticle> articles = articleMapper.selectList(
                new LambdaQueryWrapper<KnowledgeArticle>()
                    .eq(KnowledgeArticle::getNodeId, wn.getNodeId())
                    .eq(KnowledgeArticle::getStatus, "PUBLISHED")
            );
            for (KnowledgeArticle article : articles) {
                if (cards.size() >= limit) break;
                List<KnowledgeFlashcard> flashs = flashcardMapper.selectList(
                    new LambdaQueryWrapper<KnowledgeFlashcard>()
                        .eq(KnowledgeFlashcard::getArticleId, article.getId())
                );
                for (KnowledgeFlashcard f : flashs) {
                    if (cards.size() >= limit) break;
                    RelatedCardsDTO.CardItem ci = new RelatedCardsDTO.CardItem();
                    ci.setCardId(f.getId());
                    ci.setFrontText(f.getFrontText());
                    ci.setBackText(f.getBackText());
                    ci.setArticleId(article.getId());
                    ci.setKnowledgeNodeId(wn.getNodeId());
                    ci.setEstimatedMinutes(2);
                    cards.add(ci);
                }
            }
        }

        RelatedCardsDTO dto = new RelatedCardsDTO();
        dto.setWrongKnowledgeNodes(wrongNodes);
        dto.setCards(cards);
        dto.setTotalCards(cards.size());
        dto.setEstimatedMinutes(cards.size() * 2);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlashcardDTO> getCardsByNodeId(Long nodeId, int limit) {
        List<FlashcardDTO> result = new ArrayList<>();
        List<KnowledgeArticle> articles = articleMapper.selectList(
            new LambdaQueryWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getNodeId, nodeId)
                .eq(KnowledgeArticle::getStatus, "PUBLISHED")
        );
        for (KnowledgeArticle article : articles) {
            if (result.size() >= limit) break;
            List<KnowledgeFlashcard> flashs = flashcardMapper.selectList(
                new LambdaQueryWrapper<KnowledgeFlashcard>()
                    .eq(KnowledgeFlashcard::getArticleId, article.getId())
            );
            for (KnowledgeFlashcard f : flashs) {
                if (result.size() >= limit) break;
                FlashcardDTO dto = new FlashcardDTO();
                dto.setCardId(f.getId());
                dto.setFrontText(f.getFrontText());
                dto.setBackText(f.getBackText());
                dto.setArticleId(article.getId());
                dto.setKnowledgeNodeId(nodeId);
                dto.setEstimatedMinutes(2);
                result.add(dto);
            }
        }
        return result;
    }

    private static class QRSession {
        Long classId;
        List<QuickReviewDTO.CardItem> cards;
        LocalDateTime createdAt = LocalDateTime.now();
    }

    private final ConcurrentHashMap<String, QRSession> qrSessions = new ConcurrentHashMap<>();

    @Scheduled(fixedRate = 3600000)
    public void cleanupExpiredQRSessions() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        qrSessions.entrySet().removeIf(entry -> entry.getValue().createdAt.isBefore(threshold));
    }

    @Override
    @Transactional(readOnly = true)
    public QuickReviewDTO startQuickReview(Long classId, Long subjectId, Long nodeId, int limit) {
        if (subjectId == null) {
            Long userId = com.school.teaching.security.SecurityUtils.getCurrentUserId();
            if (userId != null) {
                TeacherClass tc = teacherClassMapper.selectOne(
                    new LambdaQueryWrapper<TeacherClass>()
                        .eq(TeacherClass::getClassId, classId)
                        .eq(TeacherClass::getTeacherId, userId)
                        .last("LIMIT 1")
                );
                if (tc != null && tc.getSubject() != null && !tc.getSubject().isEmpty()) {
                    List<KnowledgeNode> subjects = nodeMapper.selectList(
                        new LambdaQueryWrapper<KnowledgeNode>()
                            .eq(KnowledgeNode::getName, tc.getSubject())
                            .eq(KnowledgeNode::getLevel, 1)
                    );
                    if (!subjects.isEmpty()) {
                        subjectId = subjects.get(0).getId();
                    }
                }
            }
        }

        List<KnowledgeArticle> articles;
        if (nodeId != null) {
            articles = getArticlesBeforeNode(nodeId);
        } else if (subjectId != null) {
            articles = articleMapper.selectList(
                new LambdaQueryWrapper<KnowledgeArticle>()
                    .eq(KnowledgeArticle::getSubjectId, subjectId)
                    .eq(KnowledgeArticle::getStatus, "PUBLISHED")
            );
        } else {
            articles = articleMapper.selectList(
                new LambdaQueryWrapper<KnowledgeArticle>()
                    .eq(KnowledgeArticle::getStatus, "PUBLISHED")
            );
        }
        if (articles.isEmpty()) throw new BusinessException(400, "暂无知识卡片，请先生成");

        Collections.shuffle(articles);
        List<QuickReviewDTO.CardItem> cards = new ArrayList<>();
        int idx = 0;
        for (KnowledgeArticle article : articles) {
            if (cards.size() >= limit) break;
            List<KnowledgeFlashcard> flashs = flashcardMapper.selectList(
                new LambdaQueryWrapper<KnowledgeFlashcard>()
                    .eq(KnowledgeFlashcard::getArticleId, article.getId())
            );
            for (KnowledgeFlashcard f : flashs) {
                if (cards.size() >= limit) break;
                QuickReviewDTO.CardItem ci = new QuickReviewDTO.CardItem();
                ci.setIndex(idx++);
                ci.setCardId(f.getId());
                ci.setFrontText(f.getFrontText());
                ci.setBackText(f.getBackText());
                ci.setArticleId(article.getId());
                cards.add(ci);
            }
        }

        String sessionId = "qr_" + classId + "_" + java.time.LocalDate.now().toString().replace("-", "") + "_" + System.currentTimeMillis() % 10000;
        QRSession session = new QRSession();
        session.classId = classId;
        session.cards = cards;
        qrSessions.put(sessionId, session);

        QuickReviewDTO dto = new QuickReviewDTO();
        dto.setSessionId(sessionId);
        dto.setCards(cards);
        dto.setTotalCards(cards.size());
        return dto;
    }

    private List<KnowledgeArticle> getArticlesBeforeNode(Long nodeId) {
        KnowledgeNode currentNode = nodeMapper.selectById(nodeId);
        if (currentNode == null) {
            return List.of();
        }

        List<Long> ancestorIds = getAncestorIds(currentNode);
        Long parentId = currentNode.getParentId();
        Integer currentSortOrder = currentNode.getSortOrder();
        Integer level = currentNode.getLevel();

        // 同父下 sortOrder 更小的兄弟节点
        List<KnowledgeNode> priorNodes = nodeMapper.selectList(
            new LambdaQueryWrapper<KnowledgeNode>()
                .eq(KnowledgeNode::getParentId, parentId)
                .lt(KnowledgeNode::getSortOrder, currentSortOrder)
                .eq(KnowledgeNode::getLevel, level)
                .eq(KnowledgeNode::getStatus, "ACTIVE")
        );

        Set<Long> allNodeIds = new LinkedHashSet<>();
        // 1) 同父的优先兄弟节点
        priorNodes.stream().map(KnowledgeNode::getId).forEach(allNodeIds::add);
        // 2) 祖先节点本身
        allNodeIds.addAll(ancestorIds);
        // 3) 祖先节点的所有子孙节点（递归收集，覆盖"同祖先不同父分支遗漏"问题）
        for (Long ancestorId : ancestorIds) {
            collectDescendantIds(ancestorId, allNodeIds);
        }

        if (allNodeIds.isEmpty()) {
            return List.of();
        }

        List<KnowledgeArticle> articles = articleMapper.selectList(
            new LambdaQueryWrapper<KnowledgeArticle>()
                .in(KnowledgeArticle::getNodeId, new ArrayList<>(allNodeIds))
                .eq(KnowledgeArticle::getStatus, "PUBLISHED")
        );

        return articles;
    }

    /** 递归收集节点的所有子孙节点 ID（知识树深度≤4，递归开销可接受） */
    private void collectDescendantIds(Long nodeId, Set<Long> result) {
        List<KnowledgeNode> children = nodeMapper.selectList(
            new LambdaQueryWrapper<KnowledgeNode>()
                .eq(KnowledgeNode::getParentId, nodeId)
                .eq(KnowledgeNode::getStatus, "ACTIVE")
        );
        for (KnowledgeNode child : children) {
            result.add(child.getId());
            collectDescendantIds(child.getId(), result);
        }
    }

    private List<Long> getAncestorIds(KnowledgeNode node) {
        List<Long> ancestorIds = new java.util.ArrayList<>();
        Long parentId = node.getParentId();
        while (parentId != null) {
            KnowledgeNode parent = nodeMapper.selectById(parentId);
            if (parent == null) break;
            if (parent.getLevel() > 1) {
                ancestorIds.add(parentId);
            }
            parentId = parent.getParentId();
        }
        return ancestorIds;
    }

    @Override
    @Transactional
    public void recordQuickReview(String sessionId, Long studentId, int cardIndex, boolean correct) {
        QRSession session = qrSessions.get(sessionId);
        if (session == null) throw new BusinessException(404, "复习会话不存在或已过期");

        if (studentMapper.selectById(studentId) == null)
            throw new BusinessException(400, "学生不存在");

        int rating = correct ? 4 : 2;

        QuickReviewDTO.CardItem target = session.cards.stream().filter(c -> c.getIndex() == cardIndex).findFirst().orElse(null);
        if (target == null) return;

        KnowledgeReviewSchedule existing = scheduleMapper.selectOne(
            new LambdaQueryWrapper<KnowledgeReviewSchedule>()
                .eq(KnowledgeReviewSchedule::getStudentId, studentId)
                .eq(KnowledgeReviewSchedule::getFlashcardId, target.getCardId())
        );

        if (existing != null) {
            existing.setRepetitions((existing.getRepetitions() != null ? existing.getRepetitions() : 0) + 1);
            existing.setLastRating(rating);
            existing.setLastReviewAt(LocalDateTime.now());
            existing.setEaseFactor(rating >= 4 ? java.math.BigDecimal.valueOf(2.5) : java.math.BigDecimal.valueOf(1.3));
            existing.setIntervalDays(rating >= 4 ? 1 : 0);
            existing.setNextReviewAt(LocalDateTime.now().plusDays(rating >= 4 ? 1 : 0));
            existing.setUpdatedAt(LocalDateTime.now());
            scheduleMapper.updateById(existing);
        } else {
            KnowledgeReviewSchedule s = new KnowledgeReviewSchedule();
            s.setStudentId(studentId);
            s.setFlashcardId(target.getCardId());
            s.setArticleId(target.getArticleId());
            s.setRepetitions(1);
            s.setLastRating(rating);
            s.setLastReviewAt(LocalDateTime.now());
            s.setEaseFactor(java.math.BigDecimal.valueOf(2.5));
            s.setIntervalDays(1);
            s.setNextReviewAt(LocalDateTime.now().plusDays(1));
            s.setIsMastered(correct ? 0 : 0);
            s.setCreatedAt(LocalDateTime.now());
            s.setUpdatedAt(LocalDateTime.now());
            scheduleMapper.insert(s);
        }

        if (correct) {
            String bizKey = "quick_review_" + sessionId + "_" + studentId + "_" + cardIndex;
            Long count = creditTransactionMapper.selectCount(
                new LambdaQueryWrapper<CreditTransaction>()
                    .eq(CreditTransaction::getBizKey, bizKey)
            );
            if (count == 0) {
                CreditTransaction txn = new CreditTransaction();
                txn.setStudentId(studentId);
                txn.setTransactionType("earn");
                txn.setCreditAmount(1);
                txn.setSourceType("QUICK_REVIEW");
                txn.setDescription("课前三分钟复习");
                txn.setBizKey(bizKey);
                txn.setBalanceAfter(0);
                txn.setCreateTime(LocalDateTime.now());
                creditTransactionMapper.insert(txn);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ExamPrepPackDTO getExamPrepPack(Long studentId, Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) return null;
        if (task.getDeadline() == null || task.getDeadline().isAfter(LocalDateTime.now().plusDays(3))) {
            ExamPrepPackDTO no = new ExamPrepPackDTO();
            no.setTaskId(taskId);
            no.setTaskTitle(task.getTitle());
            no.setDaysUntilDeadline(task.getDeadline() != null
                ? java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), task.getDeadline().toLocalDate()) : 99);
            no.setReason("not_urgent");
            return no;
        }

        List<TaskQuestion> tqs = taskQuestionMapper.selectList(
            new LambdaQueryWrapper<TaskQuestion>().eq(TaskQuestion::getTaskId, taskId));
        if (tqs.isEmpty()) return null;

        Set<Long> qIds = tqs.stream().map(TaskQuestion::getQuestionId).filter(Objects::nonNull).collect(Collectors.toSet());
        List<QuestionBank> questions = questionBankMapper.selectBatchIds(qIds);
        Set<Long> nodeIds = questions.stream().map(QuestionBank::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (nodeIds.isEmpty()) return null;

        List<WrongQuestion> wrongList = wrongQuestionMapper.selectList(
            new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getStudentId, studentId)
                .in(WrongQuestion::getQuestionId, qIds)
                .eq(WrongQuestion::getIsMastered, 0)
        );
        Set<Long> wrongQuestionIds = wrongList.stream().map(WrongQuestion::getQuestionId).collect(Collectors.toSet());
        Set<Long> weakNodeIds = questions.stream()
            .filter(q -> wrongQuestionIds.contains(q.getId()) && q.getCategoryId() != null)
            .map(QuestionBank::getCategoryId).collect(Collectors.toSet());

        Map<Long, java.math.BigDecimal> masteryMap = new HashMap<>();
        if (!nodeIds.isEmpty()) {
            List<PrecisionProgress> pps = precisionProgressMapper.selectList(
                new LambdaQueryWrapper<PrecisionProgress>()
                    .eq(PrecisionProgress::getStudentId, studentId)
                    .in(PrecisionProgress::getNodeId, nodeIds)
            );
            for (PrecisionProgress pp : pps) {
                if (pp.getNodeId() != null) masteryMap.put(pp.getNodeId(), pp.getMasteryPercent());
            }
        }

        List<ExamPrepPackDTO.WeakPoint> weakPoints = new ArrayList<>();
        List<ExamPrepPackDTO.SkipPoint> skipPoints = new ArrayList<>();
        LocalDateTime today = LocalDate.now().atStartOfDay();
        Set<Long> todayReviewed = scheduleMapper.selectList(
            new LambdaQueryWrapper<KnowledgeReviewSchedule>()
                .eq(KnowledgeReviewSchedule::getStudentId, studentId)
                .ge(KnowledgeReviewSchedule::getLastReviewAt, today)
        ).stream().map(KnowledgeReviewSchedule::getFlashcardId).collect(Collectors.toSet());

        int totalCards = 0;
        for (Long nodeId : nodeIds) {
            java.math.BigDecimal m = masteryMap.getOrDefault(nodeId, java.math.BigDecimal.ZERO);
            KnowledgeNode node = nodeMapper.selectById(nodeId);
            String name = node != null ? node.getName() : null;

            if (weakNodeIds.contains(nodeId) || m.compareTo(java.math.BigDecimal.valueOf(70)) < 0) {
                ExamPrepPackDTO.WeakPoint wp = new ExamPrepPackDTO.WeakPoint();
                wp.setNodeId(nodeId);
                wp.setNodeName(name);
                wp.setMasteryPercent(m);

                List<KnowledgeArticle> articles = articleMapper.selectList(
                    new LambdaQueryWrapper<KnowledgeArticle>()
                        .eq(KnowledgeArticle::getNodeId, nodeId)
                        .eq(KnowledgeArticle::getStatus, "PUBLISHED")
                );
                int cnt = 0;
                for (KnowledgeArticle a : articles) {
                    if (cnt >= 3 || totalCards >= 10) break;
                    List<KnowledgeFlashcard> fs = flashcardMapper.selectList(
                        new LambdaQueryWrapper<KnowledgeFlashcard>()
                            .eq(KnowledgeFlashcard::getArticleId, a.getId())
                    );
                    for (KnowledgeFlashcard f : fs) {
                        if (cnt >= 3 || totalCards >= 10) break;
                        if (!todayReviewed.contains(f.getId())) {
                            cnt++;
                            totalCards++;
                        }
                    }
                }
                if (cnt > 0) {
                    wp.setCardCount(cnt);
                    wp.setEstimatedMinutes(cnt * 2);
                    weakPoints.add(wp);
                }
            } else {
                ExamPrepPackDTO.SkipPoint sp = new ExamPrepPackDTO.SkipPoint();
                sp.setNodeId(nodeId);
                sp.setNodeName(name);
                sp.setMasteryPercent(m);
                skipPoints.add(sp);
            }
        }

        ExamPrepPackDTO dto = new ExamPrepPackDTO();
        dto.setTaskId(taskId);
        dto.setTaskTitle(task.getTitle());
        dto.setDaysUntilDeadline(java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), task.getDeadline().toLocalDate()));
        dto.setWeakPoints(weakPoints);
        dto.setSkipPoints(skipPoints);
        dto.setTotalCards(totalCards);
        dto.setTotalEstimatedMinutes(totalCards * 2);
        return dto;
    }

    // ══════════════════════════════════════════
    //  v167: 教师审核 + 考纲权重
    // ══════════════════════════════════════════

    @Override
    public java.util.Map<String, Object> getReviewQueue(Long subjectId, String status, int page, int size) {
        // 如果指定了学科，先找到该学科下所有文章ID
        java.util.Set<Long> subjectArticleIds = null;
        if (subjectId != null) {
            // 找该学科 L1 根节点 → 收集所有子孙 nodeId → 找对应文章
            var rootNode = nodeMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.school.teaching.entity.KnowledgeNode>()
                    .eq(com.school.teaching.entity.KnowledgeNode::getSubjectId, subjectId)
                    .eq(com.school.teaching.entity.KnowledgeNode::getLevel, 1)
                    .last("LIMIT 1"));
            if (rootNode != null) {
                java.util.Set<Long> subNodeIds = collectDescendantNodeIds(rootNode.getId());
                subjectArticleIds = articleMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.school.teaching.entity.KnowledgeArticle>()
                        .in(com.school.teaching.entity.KnowledgeArticle::getNodeId, subNodeIds)
                        .select(com.school.teaching.entity.KnowledgeArticle::getId))
                    .stream().map(com.school.teaching.entity.KnowledgeArticle::getId)
                    .collect(java.util.stream.Collectors.toSet());
                if (subjectArticleIds.isEmpty()) {
                    return java.util.Map.of("items", java.util.List.of(), "total", 0, "page", page, "size", size);
                }
            }
        }

        var qw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.school.teaching.entity.KnowledgeFlashcard>();
        qw.eq(com.school.teaching.entity.KnowledgeFlashcard::getReviewStatus, status != null ? status : "PENDING");
        if (subjectArticleIds != null) {
            qw.in(com.school.teaching.entity.KnowledgeFlashcard::getArticleId, subjectArticleIds);
        }
        // PENDING: 未评估(NULL)排最前(急需评估), 已评估按质量分升序(最差排前)
        // APPROVED: 按质量分降序(最好排前)
        if ("APPROVED".equals(status)) {
            qw.isNotNull(com.school.teaching.entity.KnowledgeFlashcard::getQualityScore);
            qw.orderByDesc(com.school.teaching.entity.KnowledgeFlashcard::getQualityScore);
        } else {
            qw.orderByAsc(com.school.teaching.entity.KnowledgeFlashcard::getQualityScore);
        }
        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(1, size));
        qw.last("LIMIT " + ((safePage - 1) * safeSize) + ", " + safeSize);

        java.util.List<com.school.teaching.entity.KnowledgeFlashcard> cards = flashcardMapper.selectList(qw);

        var countQw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.school.teaching.entity.KnowledgeFlashcard>()
            .eq(com.school.teaching.entity.KnowledgeFlashcard::getReviewStatus, status != null ? status : "PENDING");
        if (subjectArticleIds != null) {
            countQw.in(com.school.teaching.entity.KnowledgeFlashcard::getArticleId, subjectArticleIds);
        }
        Long total = flashcardMapper.selectCount(countQw);

        java.util.List<java.util.Map<String, Object>> items = new java.util.ArrayList<>();
        for (var card : cards) {
            java.util.Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("id", card.getId());
            item.put("frontText", card.getFrontText());
            item.put("backText", card.getBackText());
            item.put("cardType", card.getCardType());
            item.put("qualityScore", card.getQualityScore());
            item.put("aiComment", card.getAiComment());
            item.put("contextPath", card.getContextPath());
            items.add(item);
        }

        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("items", items);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    /** 一次性加载学科所有子孙节点ID（避免递归 N+1） */
    private java.util.Set<Long> collectDescendantNodeIds(Long rootNodeId) {
        java.util.Set<Long> result = new java.util.LinkedHashSet<>();
        result.add(rootNodeId);
        // 一次性加载该学科所有节点，通过 parentId 构建父子关系
        java.util.List<com.school.teaching.entity.KnowledgeNode> allNodes = nodeMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.school.teaching.entity.KnowledgeNode>()
                .eq(com.school.teaching.entity.KnowledgeNode::getSubjectId,
                    nodeMapper.selectById(rootNodeId).getSubjectId()));
        java.util.Map<Long, java.util.List<com.school.teaching.entity.KnowledgeNode>> childrenMap = new java.util.LinkedHashMap<>();
        for (var node : allNodes) {
            if (node.getParentId() != null) {
                childrenMap.computeIfAbsent(node.getParentId(), k -> new java.util.ArrayList<>()).add(node);
            }
        }
        // BFS 展开所有子孙
        java.util.Queue<Long> queue = new java.util.LinkedList<>();
        queue.add(rootNodeId);
        while (!queue.isEmpty()) {
            Long pid = queue.poll();
            java.util.List<com.school.teaching.entity.KnowledgeNode> children = childrenMap.get(pid);
            if (children != null) {
                for (var child : children) {
                    result.add(child.getId());
                    queue.add(child.getId());
                }
            }
        }
        return result;
    }

    @Override
    @Transactional
    public int batchReviewCards(java.util.List<Long> cardIds, String action, boolean adoptAiVersion, Long reviewerId) {
        int count = 0;
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        for (Long cardId : cardIds) {
            var card = flashcardMapper.selectById(cardId);
            if (card == null) continue;
            card.setReviewStatus(action);
            card.setReviewedBy(reviewerId);
            card.setReviewedAt(now);
            // 采纳 AI 改写版本
            if (adoptAiVersion && "APPROVED".equals(action) && card.getAiComment() != null) {
                try {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> comment = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(card.getAiComment(), java.util.Map.class);
                    String improved = (String) comment.get("improvedVersion");
                    if (improved != null && !improved.isBlank()) {
                        card.setBackText(improved);
                    }
                } catch (Exception ignored) { log.warn("卡片质量评估失败: {}", ignored.getMessage()); }
            }
            flashcardMapper.updateById(card);
            count++;
        }
        return count;
    }

    @Autowired(required = false) private CardQualityEvaluator cardQualityEvaluator;

    @Override
    public void setExamWeight(Long nodeId, String weight) {
        var node = nodeMapper.selectById(nodeId);
        if (node == null) throw new com.school.teaching.exception.BusinessException(404, "知识点不存在");
        node.setExamWeight(weight);
        nodeMapper.updateById(node);
    }

    @Override
    public void triggerBatchEvaluate(List<Long> cardIds) {
        if (cardQualityEvaluator != null && !cardIds.isEmpty()) {
            cardQualityEvaluator.batchEvaluate(cardIds);
        }
    }

    @Override
    public java.util.Map<String, Object> getEvaluationProgress() {
        Long total = flashcardMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.school.teaching.entity.KnowledgeFlashcard>());
        Long evaluated = flashcardMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.school.teaching.entity.KnowledgeFlashcard>()
                .isNotNull(com.school.teaching.entity.KnowledgeFlashcard::getQualityScore));
        Long pending = flashcardMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.school.teaching.entity.KnowledgeFlashcard>()
                .eq(com.school.teaching.entity.KnowledgeFlashcard::getReviewStatus, "PENDING"));
        return java.util.Map.of("total", total, "evaluated", evaluated, "pending", pending);
    }

    @Override
    public int triggerBatchEvaluateAll() {
        if (cardQualityEvaluator == null) return 0;
        List<com.school.teaching.entity.KnowledgeFlashcard> unscored = flashcardMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.school.teaching.entity.KnowledgeFlashcard>()
                .isNull(com.school.teaching.entity.KnowledgeFlashcard::getQualityScore));
        if (unscored.isEmpty()) return 0;
        List<Long> ids = unscored.stream().map(com.school.teaching.entity.KnowledgeFlashcard::getId).collect(java.util.stream.Collectors.toList());
        // 分块提交，每块 50 张，块间由异步线程的 sleep 控制节奏
        int chunkSize = 50;
        for (int i = 0; i < ids.size(); i += chunkSize) {
            int end = Math.min(i + chunkSize, ids.size());
            cardQualityEvaluator.batchEvaluate(ids.subList(i, end));
        }
        return ids.size();
    }
}

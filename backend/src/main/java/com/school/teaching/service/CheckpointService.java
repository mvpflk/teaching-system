package com.school.teaching.service;

import com.school.teaching.entity.CheckpointConfig;

import java.util.List;
import java.util.Map;

public interface CheckpointService {

    /** 学生端：获取可用学科列表（专业隔离+已审核） */
    List<Map<String, Object>> listSubjects(Long studentId);

    /** 学生端：关卡总览（含掌握度面板） */
    Map<String, Object> getOverview(Long studentId, Long subjectId);

    /** 学生端：开始关卡 */
    Map<String, Object> startCheckpoint(Long studentId, Long configId);

    /** 学生端：提交关键词确认 */
    Map<String, Object> verifyKeywords(Long studentId, Long configId, List<Map<String, Object>> answers);

    /** 学生端：跳过单个关键词（消耗1积分） */
    Map<String, Object> skipKeyword(Long studentId, Long configId, int keywordIndex);

    /** 学生端：提交验证闯关题 */
    Map<String, Object> submitCheckpoint(Long studentId, Long configId, Map<String, Object> answer);

    /** 学生端：开始Boss战 */
    Map<String, Object> startBoss(Long studentId, Long configId);

    /** 学生端：提交Boss战 */
    Map<String, Object> submitBoss(Long studentId, Long configId, List<Map<String, Object>> answers);

    /** 学生端：重考Boss战 */
    Map<String, Object> retryBoss(Long studentId, Long configId);

    /** 学生端：开始混合战 */
    Map<String, Object> startMixed(Long studentId, Long configId);

    /** 学生端：提交混合战 */
    Map<String, Object> submitMixed(Long studentId, Long configId, List<Map<String, Object>> answers);

    /** 学生端：重考混合战 */
    Map<String, Object> retryMixed(Long studentId, Long configId);

    /** 学生端：记忆卡列表 */
    List<Map<String, Object>> listMemoryCards(Long studentId, Long subjectId);

    /** 学生端：记忆卡详情 */
    Map<String, Object> getMemoryCard(Long cardId);

    /** 学生端：复习记忆卡 */
    void reviewMemoryCard(Long cardId);

    /** 学生端：待复习卡片数 */
    int getUnreviewedCount(Long studentId);

    /** 白名单检查：学生是否在开放班级中 */
    boolean isStudentInWhitelist(Long userId);

    /** 教师端：关卡列表 */
    Map<String, Object> adminList(Long subjectId, String reviewStatus, int page, int size);

    /** 教师端：编辑关卡 */
    void adminUpdate(Long configId, CheckpointConfig config);

    /** 教师端：审核关卡 */
    void adminReview(Long configId, boolean approved, String comment);

    /** 教师端：一键通过某学科全部DRAFT */
    int adminBatchApprove(Long subjectId);

    /** 积分消耗（乐观锁扣减）：用于重考/跳过关键词等 */
    boolean consumeCredits(Long studentId, int amount, String reason);

    /** 发送卡关求助通知 */
    void sendSOS(Long studentId, Long configId);

    /** 记录追问结果 */
    void recordFollowup(Long studentId, Long configId, int keywordIndex, boolean correct);
}

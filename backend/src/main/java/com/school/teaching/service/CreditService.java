package com.school.teaching.service;

import com.school.teaching.entity.CreditRule;
import com.school.teaching.entity.CreditShopItem;
import com.school.teaching.entity.CreditTransaction;
import com.school.teaching.entity.TitleLevel;
import java.util.List;
import java.util.Map;

public interface CreditService {

    Map<String, Object> getCreditInfo(Long studentId);

    List<CreditTransaction> getTransactions(Long studentId);

    /** 获取排行榜（支持范围过滤） */
    List<Map<String, Object>> getRanking(String type, int limit, Long classId, String grade, String major);

    /** 德育行为积分 — 发放积分（sourceType=BEHAVIOR） */
    Map<String, Object> awardMoralCredit(Long studentId, int amount, String reason);

    /** 德育行为积分排行榜（支持年级/班级筛选） */
    List<Map<String, Object>> getMoralRanking(Long classId, String grade, int limit);

    Map<String, Object> signIn(Long studentId);

    List<CreditShopItem> getShopItems();

    Map<String, Object> redeemItem(Long studentId, Long itemId);

    List<TitleLevel> getTitleLevels();

    List<Map<String, Object>> getAchievements(Long studentId);

    List<com.school.teaching.entity.SignRecord> getSignRecordsByRange(Long studentId, String startDate, String endDate);

    // 管理员功能
    List<Map<String, Object>> listStudents(String keyword);
    Map<String, Object> adjustCredit(Long studentId, int amount, String reason);
    CreditShopItem createShopItem(CreditShopItem item);
    CreditShopItem updateShopItem(CreditShopItem item);
    void deleteShopItem(Long id);
    List<CreditRule> getRules();
    CreditRule createRule(CreditRule rule);
    CreditRule updateRule(CreditRule rule);
    void deleteRule(Long id);
    TitleLevel updateTitle(TitleLevel title);
    List<Map<String, Object>> getDeliveries(String status);
    void markDelivered(Long deliveryId);
    void setCustomTitle(Long studentId, String titleCode);
}

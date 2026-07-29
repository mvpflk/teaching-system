package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.service.CreditService;
import com.school.teaching.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CreditServiceImpl implements CreditService {

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private CreditRuleMapper creditRuleMapper;

    @Autowired
    private CreditTransactionMapper transactionMapper;

    @Autowired
    private CreditShopItemMapper shopItemMapper;

    @Autowired
    private SignRecordMapper signRecordMapper;

    @Autowired
    private TitleLevelMapper titleLevelMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ClassesMapper classesMapper;

    @Autowired
    private RedeemDeliveryMapper redeemDeliveryMapper;

    @Autowired
    private TaskSubmissionMapper taskSubmissionMapper;

    @Autowired
    private NotificationService notificationService;

    @Override
    public Map<String, Object> getCreditInfo(Long studentId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) return Collections.emptyMap();

        String titleName = "未定级";
        TitleLevel title = titleLevelMapper.selectOne(
            new LambdaQueryWrapper<TitleLevel>()
                .le(TitleLevel::getMinCredits, student.getTotalCredits())
                .and(w -> w.isNull(TitleLevel::getMaxCredits)
                    .or(t -> t.ge(TitleLevel::getMaxCredits, student.getTotalCredits())))
                .orderByDesc(TitleLevel::getLevelNumber)
                .last("LIMIT 1")
        );
        if (title != null) titleName = title.getLevelName();

        // 今日获得和消费
        Integer todayEarned = 0;
        Integer todaySpent = 0;
        LambdaQueryWrapper<CreditTransaction> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.eq(CreditTransaction::getStudentId, studentId)
            .apply("DATE(create_time) = CURDATE()");
        List<CreditTransaction> todayTxns = transactionMapper.selectList(todayWrapper);
        for (CreditTransaction t : todayTxns) {
            if ("earn".equals(t.getTransactionType())) {
                todayEarned += t.getCreditAmount();
            } else {
                todaySpent += t.getCreditAmount();
            }
        }

        // 今日是否已签到
        boolean signedToday = signRecordMapper.isSignedToday(studentId) > 0;

        Map<String, Object> info = new HashMap<>();
        info.put("studentId", student.getId());
        info.put("totalCredits", student.getTotalCredits());
        info.put("titleLevel", student.getTitleLevel());
        info.put("titleName", titleName);
        info.put("currentStreak", student.getCurrentStreak());
        info.put("todayEarned", todayEarned);
        info.put("todaySpent", todaySpent);
        info.put("signedToday", signedToday);
        return info;
    }

    @Override
    public List<CreditTransaction> getTransactions(Long studentId) {
        LambdaQueryWrapper<CreditTransaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CreditTransaction::getStudentId, studentId);
        wrapper.orderByDesc(CreditTransaction::getCreateTime);
        return transactionMapper.selectList(wrapper);
    }

    @Override
    public List<Map<String, Object>> getRanking(String type, int limit, Long classId, String grade, String major) {
        LambdaQueryWrapper<Student> wrapper = buildRankingScope(classId, grade, major);
        if (wrapper == null) return Collections.emptyList();
        wrapper.orderByDesc(Student::getTotalCredits);
        wrapper.last("LIMIT " + Math.min(Math.max(limit, 1), 200));
        List<Student> students = studentMapper.selectList(wrapper);
        return buildRankingItems(students);
    }

    private LambdaQueryWrapper<Student> buildRankingScope(Long classId, String grade, String major) {
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        if (classId != null) { wrapper.eq(Student::getClassId, classId); return wrapper; }
        if (grade != null && !grade.isEmpty()) {
            LambdaQueryWrapper<Classes> cw = new LambdaQueryWrapper<>();
            cw.eq(Classes::getGrade, grade);
            if (major != null && !major.isEmpty()) cw.eq(Classes::getMajor, major);
            List<Classes> classes = classesMapper.selectList(cw);
            if (classes.isEmpty()) return null;
            wrapper.in(Student::getClassId, classes.stream().map(Classes::getId).toList());
        }
        return wrapper;
    }

    private List<Map<String, Object>> buildRankingItems(List<Student> students) {
        Set<Long> rankUserIds = students.stream().map(Student::getUserId).collect(Collectors.toSet());
        Map<Long, User> rankUserMap = rankUserIds.isEmpty() ? Collections.emptyMap() :
            userMapper.selectBatchIds(rankUserIds).stream().collect(Collectors.toMap(User::getId, u -> u));
        List<Map<String, Object>> ranking = new ArrayList<>();
        for (int i = 0; i < students.size(); i++) {
            Student s = students.get(i);
            User u = rankUserMap.get(s.getUserId());
            Map<String, Object> item = new HashMap<>();
            item.put("rank", i + 1);
            item.put("studentId", s.getId());
            item.put("realName", u != null ? u.getRealName() : "学生" + s.getId());
            item.put("totalCredits", s.getTotalCredits());
            item.put("titleLevel", s.getTitleLevel());
            item.put("titleName", getTitleName(s.getTitleLevel()));
            item.put("currentStreak", s.getCurrentStreak());
            item.put("avatarUrl", u != null ? u.getAvatarUrl() : null);
            String ct = s.getCustomTitle() != null && s.getCustomTitleSetAt() != null &&
                java.time.LocalDateTime.now().isBefore(s.getCustomTitleSetAt().plusDays(7)) ? s.getCustomTitle() : null;
            item.put("customTitle", ct);
            ranking.add(item);
        }
        return ranking;
    }

    @Override
    @Transactional
    public Map<String, Object> signIn(Long studentId) {
        // 检查是否已签到
        if (signRecordMapper.isSignedToday(studentId) > 0) {
            throw new BusinessException(409, "今日已签到");
        }

        Student student = studentMapper.selectById(studentId);
        if (student == null) throw new BusinessException(404, "学生不存在");

        // 计算连续签到
        SignRecord lastSign = signRecordMapper.findLastSign(studentId);
        int streak = 1;
        if (lastSign != null) {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            if (lastSign.getSignDate().equals(yesterday)) {
                streak = (lastSign.getStreakDay() != null ? lastSign.getStreakDay() : 0) + 1;
            }
        }

        // 签到积分（级距平缓，避免积分通胀）
        int creditEarned = 2;
        if (streak >= 30) creditEarned = 5;
        else if (streak >= 7) creditEarned = 4;
        else if (streak >= 3) creditEarned = 3;

        // 保存签到记录
        SignRecord record = new SignRecord();
        record.setStudentId(studentId);
        record.setSignDate(LocalDate.now());
        record.setSignTime(LocalDateTime.now());
        record.setCreditEarned(creditEarned);
        record.setStreakDay(streak);
        try {
            signRecordMapper.insert(record);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new BusinessException(409, "今日已签到，请勿重复操作");
        }

        // 原子更新积分（避免并发丢失更新）
        studentMapper.update(null, new LambdaUpdateWrapper<Student>()
            .eq(Student::getId, studentId)
            .setSql("total_credits = COALESCE(total_credits, 0) + " + creditEarned)
            .set(Student::getCurrentStreak, streak));

        // 重新读取以获取准确余额
        Student refreshed = studentMapper.selectById(studentId);
        int newBalance = refreshed != null && refreshed.getTotalCredits() != null ? refreshed.getTotalCredits() : creditEarned;
        addTransaction(studentId, null, "earn", creditEarned, newBalance, "sign", null, "签到奖励");

        String oldTitle = student.getTitleLevel() != null ? student.getTitleLevel().toString() : null;
        updateTitleLevel(refreshed != null ? refreshed : student);
        if (refreshed != null) sendSignInNotification(refreshed, creditEarned, streak, oldTitle);

        Map<String, Object> result = new HashMap<>();
        result.put("creditEarned", creditEarned);
        result.put("currentStreak", streak);
        result.put("totalCredits", newBalance);
        result.put("isFirstStep", streak == 1);
        return result;
    }

    @Override
    @Cacheable("shopItems")
    public List<CreditShopItem> getShopItems() {
        LambdaQueryWrapper<CreditShopItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CreditShopItem::getStatus, 1);
        wrapper.orderByAsc(CreditShopItem::getSortOrder);
        return shopItemMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public Map<String, Object> redeemItem(Long studentId, Long itemId) {
        Student student = studentMapper.selectById(studentId);
        CreditShopItem item = shopItemMapper.selectById(itemId);

        if (student == null || item == null) {
            throw new BusinessException(404, "学生或商品不存在");
        }
        if (item.getStatus() != 1) {
            throw new BusinessException(400, "商品已下架");
        }
        if (student.getTotalCredits() < item.getCreditPrice()) {
            throw new BusinessException(400, "积分不足");
        }

        // 乐观锁扣减积分：仅当余额 ≥ 商品价格时才更新
        int price = item.getCreditPrice();
        if (price < 0) throw new BusinessException(400, "商品价格异常");
        int updated = studentMapper.update(null,
            new LambdaUpdateWrapper<Student>()
                .eq(Student::getId, studentId)
                .ge(Student::getTotalCredits, price)
                .setSql("total_credits = total_credits - " + price));
        if (updated == 0) throw new BusinessException(400, "积分不足或系统繁忙，请重试");
        // 重新读取最新余额
        student = studentMapper.selectById(studentId);

        // 记录交易
        addTransaction(studentId, null, "consume", item.getCreditPrice(), student.getTotalCredits(),
            "redeem", itemId, "兑换: " + item.getItemName());

        // 更新商品销量
        item.setSoldCount((item.getSoldCount() != null ? item.getSoldCount() : 0) + 1);
        shopItemMapper.updateById(item);

        // 创建发货记录
        RedeemDelivery delivery = new RedeemDelivery();
        delivery.setStudentId(studentId);
        delivery.setItemId(itemId);
        delivery.setItemName(item.getItemName());
        delivery.setStatus("pending");
        delivery.setCreatedAt(LocalDateTime.now());
        redeemDeliveryMapper.insert(delivery);

        // 更新称号
        updateTitleLevel(student);

        // 通知学生兑换成功
        notificationService.notify(student.getUserId(), "redeem_success",
            "兑换成功", "你已成功兑换「" + item.getItemName() + "」，消耗 " + item.getCreditPrice() + " 积分", itemId);

        Map<String, Object> result = new HashMap<>();
        result.put("itemName", item.getItemName());
        result.put("creditPrice", item.getCreditPrice());
        result.put("remainingCredits", student.getTotalCredits());
        result.put("message", "兑换成功！消耗 " + item.getCreditPrice() + " 积分");
        return result;
    }

    @Override
    public List<TitleLevel> getTitleLevels() {
        return titleLevelMapper.selectList(
            new LambdaQueryWrapper<TitleLevel>().orderByAsc(TitleLevel::getLevelNumber)
        );
    }

    @Override
    public List<Map<String, Object>> getAchievements(Long studentId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) return Collections.emptyList();

        int totalCredits = student.getTotalCredits() != null ? student.getTotalCredits() : 0;
        int streak = student.getCurrentStreak() != null ? student.getCurrentStreak() : 0;

        Long totalEarned = transactionMapper.selectCount(
            new LambdaQueryWrapper<CreditTransaction>()
                .eq(CreditTransaction::getStudentId, studentId)
                .eq(CreditTransaction::getTransactionType, "earn"));
        Long taskCompleted = transactionMapper.selectCount(
            new LambdaQueryWrapper<CreditTransaction>()
                .eq(CreditTransaction::getStudentId, studentId)
                .eq(CreditTransaction::getSourceType, "TASK"));

        List<Map<String, Object>> achievements = new ArrayList<>();

        // 成就1: 积分里程碑
        addAchievement(achievements, "积分新手", "累计获得积分达到10分", totalEarned >= 10, false);
        addAchievement(achievements, "积分达人", "累计获得积分达到50分", totalEarned >= 50, false);
        addAchievement(achievements, "积分大师", "累计获得积分达到100分", totalEarned >= 100, false);
        addAchievement(achievements, "积分传奇", "累计获得积分达到200分", totalEarned >= 200, false);

        // 成就2: 任务完成
        addAchievement(achievements, "初次任务", "完成第一次任务", taskCompleted >= 1, false);
        addAchievement(achievements, "勤奋好学", "完成5次任务", taskCompleted >= 5, false);
        addAchievement(achievements, "学霸", "完成10次任务", taskCompleted >= 10, false);

        // 成就3: 签到
        addAchievement(achievements, "初来乍到", "连续签到3天", streak >= 3, false);
        addAchievement(achievements, "坚持不懈", "连续签到7天", streak >= 7, false);
        addAchievement(achievements, "签到王者", "连续签到30天", streak >= 30, false);

        // 成就4: 称号
        addAchievement(achievements, "青铜勇士", "达到青铜称号", student.getTitleLevel() != null && student.getTitleLevel() >= 1, false);
        addAchievement(achievements, "黄金贵族", "达到黄金称号", student.getTitleLevel() != null && student.getTitleLevel() >= 3, false);
        addAchievement(achievements, "钻石精英", "达到钻石称号", student.getTitleLevel() != null && student.getTitleLevel() >= 5, false);

        // 成就5: 等级获得
        boolean hasGradeA = taskSubmissionMapper.selectCount(
            new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getStudentId, studentId)
                .eq(TaskSubmission::getGradeLevel, "A")) > 0;
        addAchievement(achievements, "A+达人", "获得过最高等级评价", hasGradeA, taskCompleted > 0);

        return achievements;
    }

    private void addAchievement(List<Map<String, Object>> list, String name, String desc, boolean achieved, boolean hidden) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("description", desc);
        item.put("achieved", achieved);
        item.put("hidden", hidden && !achieved);
        list.add(item);
    }

    private void sendSignInNotification(Student student, int creditEarned, int streak, String oldTitle) {
        User u = userMapper.selectById(student.getUserId());
        if (u == null) return;
        String streakMsg = streak > 1 ? "连续签到" + streak + "天！" : "";
        notificationService.notify(u.getId(), "credit_sign",
            "✅ 签到成功",
            "签到成功！获得" + creditEarned + "积分" + (streakMsg.isEmpty() ? "" : "，" + streakMsg),
            null);
        String newTitle = student.getTitleLevel() != null ? student.getTitleLevel().toString() : null;
        if (oldTitle != null && newTitle != null && !oldTitle.equals(newTitle)) {
            TitleLevel tl = titleLevelMapper.selectById(student.getTitleLevel());
            notificationService.notify(u.getId(), "credit_level_up",
                "🎉 称号升级",
                "恭喜！你的称号已升级为「" + (tl != null ? tl.getLevelName() : "新称号") + "」",
                null);
        }
    }

    // --- Helper methods ---

    private void addTransaction(Long studentId, Long ruleId, String type, int amount, Integer balance,
                                String sourceType, Long sourceId, String description) {
        addTransaction(studentId, ruleId, type, amount, balance, sourceType, sourceId, description, null);
    }

    private void addTransaction(Long studentId, Long ruleId, String type, int amount, Integer balance,
                                String sourceType, Long sourceId, String description, String bizKey) {
        CreditTransaction txn = new CreditTransaction();
        txn.setStudentId(studentId);
        txn.setRuleId(ruleId);
        txn.setTransactionType(type);
        txn.setCreditAmount(amount);
        txn.setBalanceAfter(balance);
        txn.setSourceType(sourceType);
        txn.setSourceId(sourceId);
        txn.setDescription(description);
        txn.setBizKey(bizKey);
        txn.setCreateTime(LocalDateTime.now());
        transactionMapper.insert(txn);
    }

    private void updateTitleLevel(Student student) {
        List<TitleLevel> titles = titleLevelMapper.selectList(
            new LambdaQueryWrapper<TitleLevel>().orderByDesc(TitleLevel::getLevelNumber)
        );
        for (TitleLevel title : titles) {
            int credits = student.getTotalCredits() != null ? student.getTotalCredits() : 0;
            if (credits >= title.getMinCredits()) {
                if (title.getMaxCredits() == null || credits <= title.getMaxCredits()) {
                    student.setTitleLevel(title.getLevelNumber());
                    studentMapper.updateById(student);
                    break;
                }
            }
        }
    }

    private String getTitleName(Integer level) {
        if (level == null) return "未定级";
        String[] names = {"", "青铜", "白银", "黄金", "钻石", "王者"};
        return level >= 0 && level < names.length ? names[level] : "未知";
    }

    // ========== 管理员功能 ==========

    @Override
    public List<Map<String, Object>> listStudents(String keyword) {
        LambdaQueryWrapper<Student> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            // 先查用户表
            LambdaQueryWrapper<User> uw = new LambdaQueryWrapper<>();
            uw.like(User::getRealName, keyword).or().like(User::getUsername, keyword);
            List<User> users = userMapper.selectList(uw);
            if (!users.isEmpty()) {
                w.in(Student::getUserId, users.stream().map(User::getId).toList());
            } else {
                return Collections.emptyList();
            }
        }
        List<Student> students = studentMapper.selectList(w);
        Set<Long> stuUserIds = students.stream().map(Student::getUserId).collect(Collectors.toSet());
        Map<Long, User> stuUserMap = stuUserIds.isEmpty() ? Collections.emptyMap() :
            userMapper.selectBatchIds(stuUserIds).stream().collect(Collectors.toMap(User::getId, u -> u));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Student s : students) {
            User u = stuUserMap.get(s.getUserId());
            Map<String, Object> item = new HashMap<>();
            item.put("studentId", s.getId());
            item.put("userId", s.getUserId());
            item.put("studentNumber", s.getStudentNumber());
            item.put("realName", u != null ? u.getRealName() : "?");
            item.put("totalCredits", s.getTotalCredits());
            item.put("titleLevel", s.getTitleLevel());
            item.put("titleName", getTitleName(s.getTitleLevel()));
            result.add(item);
        }
        return result;
    }

    @Override
    @Transactional
    @CacheEvict(value = "classHome", allEntries = true)
    public Map<String, Object> adjustCredit(Long studentId, int amount, String reason) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) throw new BusinessException(404, "学生不存在");

        String type = amount >= 0 ? "earn" : "consume";
        int absAmount = Math.abs(amount);

        // 原子更新（扣分时校验余额），amount 为 int 无注入风险
        if (absAmount > 999999) throw new BusinessException(400, "调整额度超出范围");
        LambdaUpdateWrapper<Student> updateW = new LambdaUpdateWrapper<Student>()
            .eq(Student::getId, studentId)
            .setSql("total_credits = COALESCE(total_credits, 0) + " + amount);
        if (amount < 0) updateW.ge(Student::getTotalCredits, absAmount);
        int updated = studentMapper.update(null, updateW);
        if (updated == 0) throw new BusinessException(400, amount < 0 ? "积分不足，无法扣除" : "学生不存在");

        // 重新读取以获取准确余额写交易记录
        Student refreshed = studentMapper.selectById(studentId);
        int newBalance = refreshed != null && refreshed.getTotalCredits() != null ? refreshed.getTotalCredits() : amount;
        addTransaction(studentId, null, type, absAmount, newBalance, "admin", null, reason);

        updateTitleLevel(student);

        // 通知学生积分调整
        String adjType = amount >= 0 ? "增加" : "扣除";
        notificationService.notify(student.getUserId(), "credit_adjust",
            "积分调整", "管理员" + adjType + "了 " + Math.abs(amount) + " 积分" +
                (reason != null && !reason.isEmpty() ? "，原因：" + reason : ""), null);

        Map<String, Object> result = new HashMap<>();
        result.put("oldBalance", newBalance - amount);
        result.put("newBalance", newBalance);
        result.put("amount", amount);
        return result;
    }

    @Override
    @CacheEvict(value = "shopItems", allEntries = true)
    public CreditShopItem createShopItem(CreditShopItem item) {
        shopItemMapper.insert(item);
        return item;
    }

    @Override
    @CacheEvict(value = "shopItems", allEntries = true)
    public CreditShopItem updateShopItem(CreditShopItem item) {
        shopItemMapper.updateById(item);
        return item;
    }

    @Override
    @CacheEvict(value = "shopItems", allEntries = true)
    public void deleteShopItem(Long id) {
        shopItemMapper.deleteById(id);
    }

    @Override
    @Cacheable("creditRules")
    public List<CreditRule> getRules() {
        return creditRuleMapper.selectList(null);
    }

    @Override
    @CacheEvict(value = "creditRules", allEntries = true)
    public CreditRule createRule(CreditRule rule) {
        creditRuleMapper.insert(rule);
        return rule;
    }

    @Override
    @CacheEvict(value = "creditRules", allEntries = true)
    public CreditRule updateRule(CreditRule rule) {
        creditRuleMapper.updateById(rule);
        return rule;
    }

    @Override
    @CacheEvict(value = "creditRules", allEntries = true)
    public void deleteRule(Long id) {
        creditRuleMapper.deleteById(id);
    }

    @Override
    public TitleLevel updateTitle(TitleLevel title) {
        titleLevelMapper.updateById(title);
        return title;
    }

    @Override
    public List<SignRecord> getSignRecordsByRange(Long studentId, String startDate, String endDate) {
        return signRecordMapper.getSignRecordsByRange(studentId, startDate, endDate);
    }

    @Override
    @Transactional
    public Map<String, Object> awardMoralCredit(Long studentId, int amount, String reason) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) throw new BusinessException(404, "学生不存在");

        // 原子更新，amount 为 int 无注入风险
        if (amount < 1 || amount > 999999) throw new BusinessException(400, "德育积分额度超出范围");
        studentMapper.update(null, new LambdaUpdateWrapper<Student>()
            .eq(Student::getId, studentId)
            .setSql("total_credits = COALESCE(total_credits, 0) + " + amount));

        Student refreshed = studentMapper.selectById(studentId);
        int newBalance = refreshed != null && refreshed.getTotalCredits() != null ? refreshed.getTotalCredits() : amount;
        addTransaction(studentId, null, "earn", amount, newBalance, "BEHAVIOR", null, reason);
        updateTitleLevel(student);

        Map<String, Object> result = new HashMap<>();
        result.put("oldBalance", newBalance - amount);
        result.put("newBalance", newBalance);
        result.put("amount", amount);
        return result;
    }

    @Override
    public List<Map<String, Object>> getMoralRanking(Long classId, String grade, int limit) {
        if (creditRuleMapper.selectCount(
            new LambdaQueryWrapper<CreditRule>()
                .eq(CreditRule::getRuleCode, "MORAL_BEHAVIOR")
                .eq(CreditRule::getStatus, 1)) == 0) return List.of();

        Set<Long> gradeClassIds = resolveGradeClassIds(grade);
        if (grade != null && !grade.isEmpty() && gradeClassIds.isEmpty()) return List.of();

        Map<Long, Long> scoreMap = aggregateMoralScores();
        if (scoreMap.isEmpty()) return List.of();

        Map<Long, Student> stuMap = filterStudentsByScope(scoreMap.keySet(), classId, gradeClassIds);
        Map<Long, User> userMap = loadUserMap(stuMap);
        Map<Long, Classes> classMap = loadClassMap(stuMap);

        return buildMoralRankingResult(scoreMap, stuMap, userMap, classMap, limit);
    }

    private Set<Long> resolveGradeClassIds(String grade) {
        if (grade == null || grade.isEmpty()) return null;
        return classesMapper.selectList(
            new LambdaQueryWrapper<Classes>().eq(Classes::getGrade, grade))
            .stream().map(Classes::getId).collect(Collectors.toSet());
    }

    private Map<Long, Long> aggregateMoralScores() {
        List<CreditTransaction> allTxs = transactionMapper.selectList(
            new LambdaQueryWrapper<CreditTransaction>()
                .eq(CreditTransaction::getTransactionType, "earn")
                .eq(CreditTransaction::getSourceType, "BEHAVIOR"));
        Map<Long, Long> scoreMap = new LinkedHashMap<>();
        for (CreditTransaction tx : allTxs) {
            scoreMap.merge(tx.getStudentId(), (long) tx.getCreditAmount(), Long::sum);
        }
        return scoreMap;
    }

    private Map<Long, Student> filterStudentsByScope(Set<Long> studentIds, Long classId, Set<Long> gradeClassIds) {
        Map<Long, Student> stuMap = new HashMap<>();
        for (Student s : studentMapper.selectBatchIds(new ArrayList<>(studentIds))) {
            if (classId != null && !classId.equals(s.getClassId())) continue;
            if (gradeClassIds != null && !gradeClassIds.contains(s.getClassId())) continue;
            stuMap.put(s.getId(), s);
        }
        return stuMap;
    }

    private Map<Long, User> loadUserMap(Map<Long, Student> stuMap) {
        Set<Long> userIds = stuMap.values().stream().map(Student::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        return userIds.isEmpty() ? Collections.emptyMap() :
            userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));
    }

    private Map<Long, Classes> loadClassMap(Map<Long, Student> stuMap) {
        Set<Long> classIds = stuMap.values().stream().map(Student::getClassId).filter(Objects::nonNull).collect(Collectors.toSet());
        return classIds.isEmpty() ? Collections.emptyMap() :
            classesMapper.selectBatchIds(classIds).stream().collect(Collectors.toMap(Classes::getId, c -> c));
    }

    private List<Map<String, Object>> buildMoralRankingResult(Map<Long, Long> scoreMap, Map<Long, Student> stuMap,
                                                               Map<Long, User> userMap, Map<Long, Classes> classMap, int limit) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : scoreMap.entrySet()) {
            Student s = stuMap.get(entry.getKey());
            if (s == null) continue;
            User u = userMap.get(s.getUserId());
            Classes c = classMap.get(s.getClassId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("studentId", s.getId());
            item.put("studentName", u != null ? u.getRealName() : "未知");
            item.put("className", c != null ? c.getClassName() : "");
            item.put("grade", c != null ? c.getGrade() : "");
            item.put("moralScore", entry.getValue());
            result.add(item);
        }
        result.sort((a, b) -> Long.compare((Long) b.get("moralScore"), (Long) a.get("moralScore")));
        return result.size() > limit ? result.subList(0, limit) : result;
    }

    @Override
    public List<Map<String, Object>> getDeliveries(String status) {
        LambdaQueryWrapper<RedeemDelivery> w = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty() && !"all".equals(status))
            w.eq(RedeemDelivery::getStatus, status);
        w.orderByDesc(RedeemDelivery::getCreatedAt);
        List<RedeemDelivery> list = redeemDeliveryMapper.selectList(w);
        Set<Long> sids = list.stream().map(RedeemDelivery::getStudentId).collect(Collectors.toSet());
        Map<Long, Student> sMap = sids.isEmpty() ? Collections.emptyMap() :
            studentMapper.selectBatchIds(sids).stream().collect(Collectors.toMap(Student::getId, s -> s));
        Set<Long> uids = sMap.values().stream().map(Student::getUserId).collect(Collectors.toSet());
        Map<Long, User> uMap = uids.isEmpty() ? Collections.emptyMap() :
            userMapper.selectBatchIds(uids).stream().collect(Collectors.toMap(User::getId, u -> u));
        List<Map<String, Object>> result = new ArrayList<>();
        for (RedeemDelivery d : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", d.getId()); m.put("studentId", d.getStudentId()); m.put("itemName", d.getItemName());
            m.put("status", d.getStatus()); m.put("createTime", d.getCreatedAt());
            Student s = sMap.get(d.getStudentId());
            m.put("studentName", s != null ? uMap.getOrDefault(s.getUserId(), new User()).getRealName() : "?");
            result.add(m);
        }
        return result;
    }

    @Override @Transactional
    public void markDelivered(Long deliveryId) {
        RedeemDelivery d = redeemDeliveryMapper.selectById(deliveryId);
        if (d == null) throw new BusinessException(404, "交付记录不存在");
        d.setStatus("delivered"); d.setDeliveredAt(LocalDateTime.now());
        redeemDeliveryMapper.updateById(d);
        Student s = studentMapper.selectById(d.getStudentId());
        if (s != null) {
            User u = userMapper.selectById(s.getUserId());
            if (u != null) notificationService.notify(u.getId(), "delivery_shipped",
                "物品已交付", "你兑换的「" + d.getItemName() + "」已由管理员交付，请查收！", d.getId());
        }
    }

    @Override @Transactional
    public void setCustomTitle(Long studentId, String titleCode) {
        Student s = studentMapper.selectById(studentId);
        if (s == null) throw new BusinessException(404, "学生不存在");
        s.setCustomTitle(titleCode); s.setCustomTitleSetAt(LocalDateTime.now());
        studentMapper.updateById(s);
    }
}
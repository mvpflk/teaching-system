package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.dto.request.ClassroomQuestionRequest;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import com.school.teaching.service.ClassroomService;
import com.school.teaching.service.CreditService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ClassroomServiceImpl implements ClassroomService {

    @Autowired private ClassroomSessionMapper sessionMapper;
    @Autowired private ClassroomQuestionMapper questionMapper;
    @Autowired private ClassroomParticipationMapper participationMapper;
    @Autowired private StudentMapper studentMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private CreditService creditService;
    @Autowired private WrongQuestionMapper wrongQuestionMapper;
    @Autowired private QuestionBankMapper questionBankMapper;
    @Lazy
    @Autowired private ClassroomServiceImpl self;
    @Autowired private ObjectMapper objectMapper;

    // SSE 订阅管理：classId → List<SseEmitter>
    private final Map<Long, List<SseEmitter>> classroomEmitters = new ConcurrentHashMap<>();
    // 单班级最大SSE连接数（防止微机室学生同时打开页面冲垮服务器）
    private static final int MAX_EMITTERS_PER_CLASS = 50;
    // R112v2: 追踪每个emitter对应的userId，用于抽问时仅从在线学生中选人
    private final Map<SseEmitter, Long> emitterUserMap = new ConcurrentHashMap<>();
    // R112v2: 班级在线学生ID集合（classId → userIds），用于抽问候选池过滤
    private final Map<Long, Set<Long>> connectedUserIds = new ConcurrentHashMap<>();

    // 活动状态缓存：classId → 当前活动信息（用于短轮询降级）
    private final Map<Long, Map<String, Object>> activeSessionCache = new ConcurrentHashMap<>();

    // SSE心跳保活：每30秒向所有emitter发送空事件，防止校园网防火墙静默断开长连接
    private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "sse-heartbeat");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean heartbeatStarted = new AtomicBoolean(false);

    // SSE广播线程池：异步并行发送，防止单个慢连接阻塞全班消息
    private final ExecutorService broadcastPool = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "sse-broadcast");
        t.setDaemon(true);
        return t;
    });

    // 投票自动关闭定时器：sessionId → 延迟任务句柄（可手动关闭时取消）
    private final Map<Long, ScheduledFuture<?>> pollTimers = new ConcurrentHashMap<>();
    // 投票自动关闭线程池（支持多个班级同时关闭投票）
    private final ScheduledExecutorService pollScheduler = Executors.newScheduledThreadPool(4, r -> {
        Thread t = new Thread(r, "poll-autoclose");
        t.setDaemon(true);
        return t;
    });

    @PreDestroy
    public void shutdownSchedulers() {
        heartbeatScheduler.shutdownNow();
        broadcastPool.shutdownNow();
        pollScheduler.shutdownNow();
    }

    // ======================== SSE 订阅 ========================

    @Override
    public SseEmitter subscribeClassroom(Long classId, Long userId, Long studentId) {
        // 连接数限制：单班级最多50个SSE连接
        List<SseEmitter> existing = classroomEmitters.get(classId);
        if (existing != null && existing.size() >= MAX_EMITTERS_PER_CLASS) {
            SseEmitter rejectEmitter = new SseEmitter(0L);
            try {
                rejectEmitter.send(SseEmitter.event().name("error").data("班级连接数已满，请稍后重试"));
            } catch (IOException e) { /* ignore */ }
            rejectEmitter.complete();
            return rejectEmitter;
        }

        SseEmitter emitter = new SseEmitter(120_000L);
        classroomEmitters.computeIfAbsent(classId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // R112v2: 追踪在线学生（用于抽问候选池过滤）
        if (userId != null) {
            emitterUserMap.put(emitter, userId);
            connectedUserIds.computeIfAbsent(classId, k -> ConcurrentHashMap.newKeySet()).add(userId);
        }

        emitter.onCompletion(() -> disconnectEmitter(classId, emitter));
        emitter.onTimeout(() -> disconnectEmitter(classId, emitter));
        emitter.onError(e -> disconnectEmitter(classId, emitter));
        try {
            // R112v3: connected事件携带studentId，前端直接取用无需额外API
            Map<String, Object> connData = new LinkedHashMap<>();
            connData.put("status", "ok");
            if (studentId != null) connData.put("studentId", studentId);
            emitter.send(SseEmitter.event().name("connected").data(connData));
        } catch (IOException e) {
            log.warn("SSE send connected event failed, classId={}, userId={}", classId, userId);
        }

        // 首次订阅时启动心跳（全局单次）
        ensureHeartbeat();

        return emitter;
    }

    /** 断开时清理emitter + userId追踪 */
    private void disconnectEmitter(Long classId, SseEmitter emitter) {
        removeEmitter(classId, emitter);
        Long userId = emitterUserMap.remove(emitter);
        if (userId != null && classId != null) {
            Set<Long> uids = connectedUserIds.get(classId);
            if (uids != null) uids.remove(userId);
        }
    }

    /** 启动SSE心跳定时器（全局仅一次），每30秒向所有连接发送空事件保活 */
    private void ensureHeartbeat() {
        if (!heartbeatStarted.compareAndSet(false, true)) return;
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            classroomEmitters.forEach((classId, emitters) -> {
                Set<Long> uids = connectedUserIds.get(classId);
                int onlineCount = uids != null ? uids.size() : 0;
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("onlineCount", onlineCount);
                for (SseEmitter e : emitters) {
                    try {
                        e.send(SseEmitter.event().name("heartbeat").data(data));
                    } catch (IOException ex) {
                        removeEmitter(classId, e);
                        Long uid = emitterUserMap.remove(e);
                        if (uid != null) {
                            if (uids != null) uids.remove(uid);
                        }
                    }
                }
            });
        }, 30, 30, TimeUnit.SECONDS);
    }

    // ======================== 短轮询降级缓存 ========================

    /** 缓存当前活动状态（用于学生端SSE失败时短轮询降级） */
    private void setActiveSessionCache(Long classId, String type, Map<String, Object> data) {
        Map<String, Object> cache = new LinkedHashMap<>();
        cache.put("hasActivity", true);
        cache.put("sessionType", type);
        cache.put("data", data);
        cache.put("cachedAt", System.currentTimeMillis());
        activeSessionCache.put(classId, cache);
    }

    /** 清除活动缓存（活动结束时调用） */
    private void clearActiveSessionCache(Long classId) {
        activeSessionCache.remove(classId);
    }

    /** 获取班级当前SSE在线人数 */
    @Override
    public int getClassroomOnlineCount(Long classId) {
        List<SseEmitter> emitters = classroomEmitters.get(classId);
        return emitters != null ? emitters.size() : 0;
    }

    /** 获取当前活动缓存（供Controller轮询端点使用） */
    @Override
    public Map<String, Object> getActiveSessionState(Long classId) {
        Map<String, Object> cached = activeSessionCache.get(classId);
        if (cached == null) {
            return Map.of("hasActivity", false);
        }
        // 超过5分钟自动过期
        long age = System.currentTimeMillis() - (long) cached.getOrDefault("cachedAt", 0L);
        if (age > 300_000) {
            activeSessionCache.remove(classId);
            return Map.of("hasActivity", false);
        }
        return cached;
    }

    private void removeEmitter(Long classId, SseEmitter emitter) {
        List<SseEmitter> list = classroomEmitters.get(classId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                classroomEmitters.remove(classId, list);
            }
        }
    }

    /** 异步并行广播：每个emitter独立线程发送，5s超时。超时仅warn不杀emitter（留给心跳清理） */
    private void broadcastClassroom(Long classId, String event, Object data) {
        List<SseEmitter> emitters = classroomEmitters.get(classId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("广播跳过：班级{}无在线订阅者 event={}", classId, event);
            return;
        }
        List<SseEmitter> snapshot = new ArrayList<>(emitters);
        log.info("SSE广播: 班级{} event={} 目标{}人", classId, event, snapshot.size());
        for (SseEmitter e : snapshot) {
            CompletableFuture.runAsync(() -> {
                try {
                    e.send(SseEmitter.event().name(event).data(data));
                } catch (IOException ex) {
                    // IOException → 连接已死，清理emitter + userId
                    log.warn("SSE发送失败: 班级{} event={} err={}", classId, event, ex.getMessage());
                    removeEmitter(classId, e);
                    Long uid = emitterUserMap.remove(e);
                    if (uid != null) {
                        Set<Long> uids = connectedUserIds.get(classId);
                        if (uids != null) uids.remove(uid);
                    }
                }
            }, broadcastPool).orTimeout(5, TimeUnit.SECONDS)
            .exceptionally(ex -> {
                // 超时 → 仅warn，不杀emitter（可能是TCP缓冲区满，留给心跳检测）
                log.warn("SSE发送超时(5s): 班级{} event={}", classId, event);
                return null;
            });
        }
    }

    // ======================== 抽问 ========================

    @Override
    @Transactional
    public Map<String, Object> startQuiz(Long classId, Long teacherId,
            Long questionId, String questionText, String sceneMode,
            String questionType, String options,
            List<Long> excludeStudentIds, Map<Long, Double> studentWeights) {
        // 1. 获取班级active学生
        List<Student> students = studentMapper.selectList(
                new LambdaQueryWrapper<Student>()
                        .eq(Student::getClassId, classId)
                        .eq(Student::getStatus, "active"));
        if (students.isEmpty()) {
            throw new BusinessException(404, "班级无活跃学生");
        }

        // 2. 查询本堂课（最近2小时）已抽过的学生 → 自动排重
        Set<Long> alreadyQuizzed = new HashSet<>();
        LocalDateTime since = LocalDateTime.now().minusHours(2);
        List<ClassroomSession> recentSessions = sessionMapper.selectList(
                new LambdaQueryWrapper<ClassroomSession>()
                        .eq(ClassroomSession::getClassId, classId)
                        .eq(ClassroomSession::getSessionType, "QUIZ")
                        .ge(ClassroomSession::getCreatedAt, since));
        if (!recentSessions.isEmpty()) {
            Set<Long> sessionIds = recentSessions.stream()
                    .map(ClassroomSession::getId).collect(Collectors.toSet());
            List<ClassroomParticipation> participations = participationMapper.selectList(
                    new LambdaQueryWrapper<ClassroomParticipation>()
                            .in(ClassroomParticipation::getSessionId, sessionIds)
                            .eq(ClassroomParticipation::getParticipationType, "QUIZZED"));
            alreadyQuizzed = participations.stream()
                    .map(ClassroomParticipation::getStudentId)
                    .collect(Collectors.toSet());
        }

        // 3. 合并排除列表：前端传入的 + 已抽过的
        Set<Long> excludeSet = new HashSet<>();
        if (excludeStudentIds != null) excludeSet.addAll(excludeStudentIds);
        excludeSet.addAll(alreadyQuizzed);

        List<Student> candidates = students.stream()
                .filter(s -> !excludeSet.contains(s.getId()))
                .collect(Collectors.toList());

        // 4. 如果全部抽过 → 返回提示，由教师决定是否重置（不再自动清空历史）
        if (candidates.isEmpty()) {
            throw new BusinessException(409, "ALL_PICKED:本轮所有学生已被抽过，请确认是否开始新一轮");
        }

        // 5. 仅从当前SSE在线学生中抽取（R112v2: 抽问公平性）
        Set<Long> onlineUserIds = connectedUserIds.getOrDefault(classId, Collections.emptySet());
        List<Student> onlinePool = candidates.stream()
                .filter(s -> onlineUserIds.contains(s.getUserId()))
                .collect(Collectors.toList());

        Student picked;
        boolean offlinePick = false;
        if (!onlinePool.isEmpty()) {
            picked = weightedRandomPick(onlinePool, studentWeights);
        } else {
            // 无在线学生 → 回退到全量候选池，并标记离线抽取
            log.warn("抽问班级{}无在线学生，回退全量候选池{}人", classId, candidates.size());
            picked = weightedRandomPick(candidates, studentWeights);
            offlinePick = true;
        }

        // 6. 创建会话
        ClassroomSession session = new ClassroomSession();
        session.setClassId(classId);
        session.setTeacherId(teacherId);
        session.setSessionType("QUIZ");
        session.setSceneMode(sceneMode != null ? sceneMode : "LAB");
        session.setQuestionText(questionText);
        session.setQuestionId(questionId);
        session.setStatus("ACTIVE");
        sessionMapper.insert(session);

        // 5. 更新题目使用次数
        if (questionId != null) {
            ClassroomQuestion q = questionMapper.selectById(questionId);
            if (q != null) {
                q.setUsageCount((q.getUsageCount() == null ? 0 : q.getUsageCount()) + 1);
                questionMapper.updateById(q);
            }
        }

        // 6. 获取学生姓名
        User u = userMapper.selectById(picked.getUserId());
        String studentName = u != null ? u.getRealName() : "未知";

        // 7. SSE通知（全班广播，学生端按studentId过滤）
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("sessionId", session.getId());
        eventData.put("studentId", picked.getId());       // 目标学生ID，前端必须校验
        eventData.put("studentName", studentName);
        eventData.put("questionText", questionText);
        if (questionType != null) eventData.put("questionType", questionType);
        if (options != null) eventData.put("options", options);
        if (offlinePick) eventData.put("offlinePick", true);
        broadcastClassroom(classId, "quiz:selected", eventData);

        // 缓存活动状态（供短轮询降级使用，含targetStudentId用于前端过滤）
        setActiveSessionCache(classId, "QUIZ", eventData);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", session.getId());
        result.put("studentId", picked.getId());
        result.put("studentName", studentName);
        result.put("offlinePick", offlinePick);
        result.put("onlineCount", onlineUserIds.size());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> submitQuizAnswer(Long sessionId, Long studentId, Long userId, String answerText) {
        ClassroomSession session = sessionMapper.selectById(sessionId);
        if (session == null || !"ACTIVE".equals(session.getStatus())) {
            throw new BusinessException(404, "会话不存在或已关闭");
        }

        // 查找已有参与记录
        ClassroomParticipation cp = participationMapper.selectOne(
            new LambdaQueryWrapper<ClassroomParticipation>()
                .eq(ClassroomParticipation::getSessionId, sessionId)
                .eq(ClassroomParticipation::getStudentId, studentId)
                .eq(ClassroomParticipation::getParticipationType, "QUIZZED"));

        if (cp != null) {
            cp.setResponse(answerText);
            participationMapper.updateById(cp);
        } else {
            cp = new ClassroomParticipation();
            cp.setSessionId(sessionId);
            cp.setStudentId(studentId);
            cp.setUserId(userId);
            cp.setParticipationType("QUIZZED");
            cp.setResponse(answerText);
            cp.setScoreEarned(0);
            participationMapper.insert(cp);
        }

        // SSE 通知教师：学生已提交答案
        broadcastClassroom(session.getClassId(), "answer:submitted",
            Map.of("sessionId", sessionId, "studentId", studentId, "answerText", answerText));

        return Map.of("submitted", true);
    }

    @Override
    @Transactional
    public Map<String, Object> gradeQuiz(Long sessionId, Long studentId,
            int result, String response, int score) {
        ClassroomSession session = sessionMapper.selectById(sessionId);
        if (session == null || !"ACTIVE".equals(session.getStatus())) {
            throw new BusinessException(404, "会话不存在或已关闭");
        }

        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException(404, "学生不存在");
        }
        Long userId = student.getUserId();

        int scoreEarned = 0;
        int isCorrect;

        if (result == 1) {
            // 正确
            scoreEarned = score;
            isCorrect = 1;
            creditService.adjustCredit(studentId, score, "课堂抽问正确 +" + score + "分");
        } else if (result == 2) {
            // 部分正确
            scoreEarned = score;
            isCorrect = 1;
            creditService.adjustCredit(studentId, score, "课堂抽问部分正确 +" + score + "分");
        } else {
            // 错误
            isCorrect = 0;
            // L4: 记录错题（防重：同一会话同一题目不重复插入）
            Long originalQId = resolveOriginalQuestionId(session.getQuestionId());
            if (originalQId != null) {
                Long existingCount = wrongQuestionMapper.selectCount(
                    new LambdaQueryWrapper<WrongQuestion>()
                        .eq(WrongQuestion::getStudentId, studentId)
                        .eq(WrongQuestion::getQuestionId, originalQId)
                        .eq(WrongQuestion::getSourceType, "QUIZ"));
                if (existingCount == 0) {
                    WrongQuestion wq = new WrongQuestion();
                    wq.setStudentId(studentId);
                    wq.setQuestionId(originalQId);
                    wq.setWrongCount(1);
                    wq.setLastWrongTime(LocalDateTime.now());
                    wq.setIsMastered(0);
                    wq.setCreateTime(LocalDateTime.now());
                    wq.setSourceType("QUIZ");
                    wq.setSourceSessionId(sessionId);
                    wrongQuestionMapper.insert(wq);
                }
            }
        }

        // 写入/更新参与记录（若学生先提交了答案，则更新而非重复插入）
        ClassroomParticipation cp = participationMapper.selectOne(
            new LambdaQueryWrapper<ClassroomParticipation>()
                .eq(ClassroomParticipation::getSessionId, sessionId)
                .eq(ClassroomParticipation::getStudentId, studentId)
                .eq(ClassroomParticipation::getParticipationType, "QUIZZED"));
        if (cp != null) {
            cp.setIsCorrect(isCorrect);
            cp.setScoreEarned(scoreEarned);
            if (response != null && !response.isEmpty()) cp.setResponse(response);
            participationMapper.updateById(cp);
        } else {
            cp = new ClassroomParticipation();
            cp.setSessionId(sessionId);
            cp.setStudentId(studentId);
            cp.setUserId(userId);
            cp.setParticipationType("QUIZZED");
            cp.setIsCorrect(isCorrect);
            cp.setScoreEarned(scoreEarned);
            cp.setResponse(response);
            participationMapper.insert(cp);
        }

        // 关闭会话
        session.setStatus("CLOSED");
        sessionMapper.updateById(session);

        // 清除活动缓存
        clearActiveSessionCache(session.getClassId());

        // SSE通知
        broadcastClassroom(session.getClassId(), "quiz:result",
                Map.of("sessionId", sessionId, "studentId", studentId,
                        "result", result, "scoreEarned", scoreEarned));
        broadcastClassroom(session.getClassId(), "score:update",
                Map.of("studentId", studentId, "scoreEarned", scoreEarned));

        return Map.of("scoreEarned", scoreEarned, "isCorrect", isCorrect);
    }

    // ======================== 抢答 ========================

    @Override
    @Transactional
    public Map<String, Object> startBuzz(Long classId, Long teacherId,
            String questionText, int scoreReward) {
        ClassroomSession session = new ClassroomSession();
        session.setClassId(classId);
        session.setTeacherId(teacherId);
        session.setSessionType("BUZZ");
        session.setQuestionText(questionText);
        session.setStatus("ACTIVE");
        session.setSceneMode("LAB");
        sessionMapper.insert(session);

        Map<String, Object> buzzData = Map.of("sessionId", session.getId(),
                "questionText", questionText, "scoreReward", scoreReward);
        broadcastClassroom(classId, "buzz:start", buzzData);

        // 缓存活动状态（供短轮询降级使用）
        setActiveSessionCache(classId, "BUZZ", buzzData);

        return Map.of("sessionId", session.getId());
    }

    @Override
    @Transactional
    public Map<String, Object> submitBuzz(Long sessionId, Long studentId, Long userId) {
        // 重新查询会话，确保拿到最新状态
        ClassroomSession session = sessionMapper.selectById(sessionId);
        if (session == null || !"ACTIVE".equals(session.getStatus())) {
            return Map.of("buzzClosed", true);
        }

        // 直接使用条件UPDATE保证原子性，避免TOCTOU竞态
        long buzzTime = System.currentTimeMillis();

        LambdaUpdateWrapper<ClassroomSession> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ClassroomSession::getId, sessionId)
               .isNull(ClassroomSession::getBuzzWinnerId)
               .set(ClassroomSession::getBuzzWinnerId, studentId)
               .set(ClassroomSession::getBuzzWinnerTime, buzzTime);

        int rows = sessionMapper.update(null, wrapper);
        if (rows == 0) {
            return Map.of("won", false, "message", "已被抢走");
        }

        User u = userMapper.selectById(userId);
        String studentName = u != null ? u.getRealName() : "未知";

        broadcastClassroom(session.getClassId(), "buzz:end",
                Map.of("sessionId", sessionId, "winnerStudentId", studentId,
                       "winnerName", studentName, "buzzTime", buzzTime));

        return Map.of("won", true, "buzzTime", buzzTime);
    }

    @Override
    @Transactional
    public Map<String, Object> gradeBuzz(Long sessionId, Long studentId,
            int result, String response) {
        ClassroomSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(404, "会话不存在");
        }

        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException(404, "学生不存在");
        }
        Long userId = student.getUserId();

        int scoreEarned = 0;
        if (result == 1) {
            scoreEarned = 3;
            creditService.adjustCredit(studentId, 3, "抢答成功 +3分");
        } else {
            // L4: 答错 → 错题本（防重）
            Long originalQId = session.getQuestionId() != null
                    ? resolveOriginalQuestionId(session.getQuestionId()) : null;
            if (originalQId != null) {
                Long existingCount = wrongQuestionMapper.selectCount(
                    new LambdaQueryWrapper<WrongQuestion>()
                        .eq(WrongQuestion::getStudentId, studentId)
                        .eq(WrongQuestion::getQuestionId, originalQId)
                        .eq(WrongQuestion::getSourceType, "BUZZ"));
                if (existingCount == 0) {
                    WrongQuestion wq = new WrongQuestion();
                    wq.setStudentId(studentId);
                    wq.setQuestionId(originalQId);
                    wq.setWrongCount(1);
                    wq.setLastWrongTime(LocalDateTime.now());
                    wq.setIsMastered(0);
                    wq.setCreateTime(LocalDateTime.now());
                    wq.setSourceType("BUZZ");
                    wq.setSourceSessionId(sessionId);
                    wrongQuestionMapper.insert(wq);
                }
            }
        }

        ClassroomParticipation cp = new ClassroomParticipation();
        cp.setSessionId(sessionId);
        cp.setStudentId(studentId);
        cp.setUserId(userId);
        cp.setParticipationType("BUZZED");
        cp.setIsCorrect(result == 1 ? 1 : 0);
        cp.setScoreEarned(scoreEarned);
        cp.setResponse(response);
        participationMapper.insert(cp);

        session.setStatus("CLOSED");
        sessionMapper.updateById(session);

        // 清除活动缓存
        clearActiveSessionCache(session.getClassId());

        broadcastClassroom(session.getClassId(), "score:update",
                Map.of("studentId", studentId, "scoreEarned", scoreEarned));

        return Map.of("scoreEarned", scoreEarned);
    }

    // ======================== 投票 ========================

    @Override
    @Transactional
    public Map<String, Object> startPoll(Long classId, Long teacherId,
            String questionText, List<String> options,
            int durationSeconds, boolean anonymous) {
        // 构建 pollData JSON
        List<Map<String, Object>> pollItems = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("option", options.get(i));
            item.put("count", 0);
            item.put("index", i);
            pollItems.add(item);
        }

        Map<String, Object> pollData = new LinkedHashMap<>();
        pollData.put("options", pollItems);
        pollData.put("anonymous", anonymous);
        pollData.put("durationSeconds", durationSeconds);
        pollData.put("totalVotes", 0);

        String pollDataJson;
        try {
            pollDataJson = objectMapper.writeValueAsString(pollData);
        } catch (IOException e) {
            throw new BusinessException(404, "投票数据序列化失败");
        }

        ClassroomSession session = new ClassroomSession();
        session.setClassId(classId);
        session.setTeacherId(teacherId);
        session.setSessionType("POLL");
        session.setQuestionText(questionText);
        session.setPollData(pollDataJson);
        session.setStatus("ACTIVE");
        session.setSceneMode("LAB");
        sessionMapper.insert(session);

        // 启动自动关闭定时器（教师随时可通过 endPoll 手动关闭）
        final Long pollId = session.getId();
        ScheduledFuture<?> future = pollScheduler.schedule(
                () -> autoClosePoll(pollId),
                durationSeconds, TimeUnit.SECONDS);
        pollTimers.put(pollId, future);

        Map<String, Object> pollStartData = Map.of("sessionId", pollId,
                "questionText", questionText, "pollData", pollData,
                "durationSeconds", durationSeconds);
        broadcastClassroom(classId, "poll:start", pollStartData);

        // 缓存活动状态（供短轮询降级使用）
        setActiveSessionCache(classId, "POLL", pollStartData);

        return Map.of("sessionId", pollId);
    }

    @Override
    @Transactional
    public Map<String, Object> submitVote(Long sessionId, Long studentId,
            Long userId, int optionIndex) {
        ClassroomSession session = sessionMapper.selectById(sessionId);
        if (session == null || !"ACTIVE".equals(session.getStatus())) {
            throw new BusinessException(404, "投票会话不存在或已结束");
        }

        // 防刷票：检查是否已投过票
        if (participationMapper.selectCount(
            new LambdaQueryWrapper<ClassroomParticipation>()
                .eq(ClassroomParticipation::getSessionId, sessionId)
                .eq(ClassroomParticipation::getStudentId, studentId)
                .eq(ClassroomParticipation::getParticipationType, "VOTED")) > 0) {
            return Map.of("success", false, "message", "已投过票");
        }

        // 原子投票：使用 MySQL JSON_SET 在数据库层完成计票 +1
        // 无需 synchronized / 乐观锁，数据库行锁保证 write 互斥
        int affected = sessionMapper.incrementPollVote(sessionId, optionIndex);
        if (affected == 0) {
            throw new BusinessException(404, "投票会话已结束");
        }

        // 提取当前 classId（用于后续广播）
        Long classId = session.getClassId();

        // 非匿名投票 → 记录谁投了什么
        ClassroomSession updated = sessionMapper.selectById(sessionId);
        Map<String, Object> pollData;
        try {
            pollData = objectMapper.readValue(updated.getPollData(),
                    new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            throw new BusinessException(404, "投票数据解析失败");
        }

        boolean anonymous = (boolean) pollData.getOrDefault("anonymous", false);
        if (!anonymous) {
            ClassroomParticipation cp = new ClassroomParticipation();
            cp.setSessionId(sessionId);
            cp.setStudentId(studentId);
            cp.setUserId(userId);
            cp.setParticipationType("VOTED");
            cp.setIsCorrect(null);
            cp.setScoreEarned(0);
            cp.setResponse("投票选项" + (optionIndex + 1));
            participationMapper.insert(cp);
        }

        // 投票 +1 积分
        creditService.adjustCredit(studentId, 1, "参与课堂投票 +1分");

        // 广播实时更新（读数据库最新值，不再依赖本地内存状态）
        broadcastClassroom(classId, "poll:update", pollData);

        return Map.of("success", true, "pollData", pollData);
    }

    @Override
    @Transactional
    public Map<String, Object> endPoll(Long sessionId, List<Integer> manualCounts) {
        // 取消自动关闭定时器（若存在）
        ScheduledFuture<?> timer = pollTimers.remove(sessionId);
        if (timer != null && !timer.isDone()) {
            timer.cancel(false);
        }
        return doEndPoll(sessionId, manualCounts);
    }

    /** 自动关闭投票（由调度器调用） */
    private void autoClosePoll(Long sessionId) {
        try {
            pollTimers.remove(sessionId);
            // 通过代理调用 endPoll，确保 @Transactional 生效
            self.endPoll(sessionId, null);
        } catch (Exception e) {
            log.error("autoClosePoll failed, sessionId={}", sessionId, e);
        }
    }

    /** 投票关闭的核心逻辑（手动/自动共用）。manualCounts非空时用于CLASSROOM模式手动计票 */
    Map<String, Object> doEndPoll(Long sessionId, List<Integer> manualCounts) {
        ClassroomSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(404, "投票会话不存在");
        }

        // 已关闭则不再操作
        if ("CLOSED".equals(session.getStatus())) {
            try {
                return session.getPollData() != null
                    ? objectMapper.readValue(session.getPollData(),
                        new TypeReference<Map<String, Object>>() {})
                    : new LinkedHashMap<>();
            } catch (IOException e) {
                return new LinkedHashMap<>();
            }
        }

        // 解析 pollData
        Map<String, Object> pollData;
        try {
            if (session.getPollData() != null) {
                pollData = objectMapper.readValue(session.getPollData(),
                        new TypeReference<Map<String, Object>>() {});
            } else {
                pollData = new LinkedHashMap<>();
            }
        } catch (IOException e) {
            pollData = new LinkedHashMap<>();
        }

        // CLASSROOM模式手动计票：将手动计数写入pollData并持久化
        if (manualCounts != null && !manualCounts.isEmpty()) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> options = (List<Map<String, Object>>) pollData.get("options");
            int total = 0;
            if (options != null) {
                for (int i = 0; i < options.size() && i < manualCounts.size(); i++) {
                    int cnt = manualCounts.get(i) != null ? manualCounts.get(i) : 0;
                    options.get(i).put("count", cnt);
                    total += cnt;
                }
                pollData.put("totalVotes", total);
                pollData.put("options", options);
            }
            try {
                session.setPollData(objectMapper.writeValueAsString(pollData));
            } catch (IOException e) { /* ignore */ }
        }

        // 关闭会话
        session.setStatus("CLOSED");
        sessionMapper.updateById(session);

        // 清除活动缓存
        clearActiveSessionCache(session.getClassId());

        // 广播投票结束
        broadcastClassroom(session.getClassId(), "poll:end",
                Map.of("sessionId", sessionId, "pollData", pollData));

        return pollData;
    }

    // ── 随堂速答 (LIVE_QUIZ) ──

    @Override
    @Transactional
    public Map<String, Object> startLiveQuiz(Long classId, Long teacherId,
                                             String questionText, String mode,
                                             List<String> options, String correctAnswer,
                                             int durationSeconds) {
        // 检测是否有进行中的 LIVE_QUIZ
        Long activeCount = sessionMapper.selectCount(
            new LambdaQueryWrapper<ClassroomSession>()
                .eq(ClassroomSession::getClassId, classId)
                .eq(ClassroomSession::getSessionType, "LIVE_QUIZ")
                .eq(ClassroomSession::getStatus, "ACTIVE"));
        if (activeCount > 0) {
            throw new BusinessException(400, "本班已有进行中的随堂速答");
        }

        // 构建 pollData JSON（options 可能为 null）
        Map<String, Object> pollData = new LinkedHashMap<>();
        pollData.put("options", options != null ? options : List.of());
        pollData.put("correctAnswer", correctAnswer);
        pollData.put("mode", mode != null ? mode : "NORMAL");
        pollData.put("durationSeconds", durationSeconds);

        String pollDataJson;
        try {
            pollDataJson = objectMapper.writeValueAsString(pollData);
        } catch (IOException e) {
            throw new BusinessException(500, "随堂速答数据序列化失败");
        }

        ClassroomSession session = new ClassroomSession();
        session.setClassId(classId);
        session.setTeacherId(teacherId);
        session.setSessionType("LIVE_QUIZ");
        session.setQuestionText(questionText);
        session.setPollData(pollDataJson);
        session.setStatus("ACTIVE");
        session.setSceneMode("LAB");
        sessionMapper.insert(session);

        // 启动自动关闭定时器
        final Long quizId = session.getId();
        ScheduledFuture<?> future = pollScheduler.schedule(
                () -> autoCloseLiveQuiz(quizId),
                durationSeconds, TimeUnit.SECONDS);
        pollTimers.put(quizId, future);

        // SSE广播（不包含正确答案）
        Map<String, Object> broadcastData = new LinkedHashMap<>();
        broadcastData.put("sessionId", quizId);
        broadcastData.put("questionText", questionText);
        broadcastData.put("options", options != null ? options : List.of());
        broadcastData.put("mode", mode);
        broadcastData.put("durationSeconds", durationSeconds);
        broadcastClassroom(classId, "live-quiz:start", broadcastData);

        // 缓存活动状态（供短轮询降级使用）
        setActiveSessionCache(classId, "LIVE_QUIZ", broadcastData);

        return Map.of("sessionId", quizId);
    }

    @Override
    @Transactional
    public Map<String, Object> submitLiveQuizAnswer(Long sessionId, Long studentId, Long userId, String answer) {
        ClassroomSession session = sessionMapper.selectById(sessionId);
        if (session == null || !"ACTIVE".equals(session.getStatus())) {
            throw new BusinessException(404, "随堂速答会话不存在或已结束");
        }

        // 防重复提交
        Long existing = participationMapper.selectCount(
            new LambdaQueryWrapper<ClassroomParticipation>()
                .eq(ClassroomParticipation::getSessionId, sessionId)
                .eq(ClassroomParticipation::getStudentId, studentId)
                .eq(ClassroomParticipation::getParticipationType, "LIVE_QUIZ"));
        if (existing > 0) {
            return Map.of("correct", false, "message", "已提交过答案");
        }

        // 解析 pollData 获取 correctAnswer 和 mode
        Map<String, Object> pollData;
        try {
            pollData = objectMapper.readValue(session.getPollData(),
                    new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            throw new BusinessException(500, "随堂速答数据解析失败");
        }

        String correctAnswer = (String) pollData.get("correctAnswer");
        String quizMode = (String) pollData.getOrDefault("mode", "NORMAL");

        // 判分：字符串忽略大小写对比
        boolean isCorrect = answer != null && correctAnswer != null
                && answer.trim().equalsIgnoreCase(correctAnswer.trim());
        int scoreEarned = isCorrect ? 3 : 0;

        // 记录 ClassroomParticipation
        ClassroomParticipation cp = new ClassroomParticipation();
        cp.setSessionId(sessionId);
        cp.setStudentId(studentId);
        cp.setUserId(userId);
        cp.setParticipationType("LIVE_QUIZ");
        cp.setIsCorrect(isCorrect ? 1 : 0);
        cp.setScoreEarned(scoreEarned);
        cp.setResponse(answer);
        participationMapper.insert(cp);

        // 正确则加积分
        if (isCorrect) {
            creditService.adjustCredit(studentId, 3, "随堂速答正确 +3分");
        }

        // 统计当前所有提交结果
        Long totalCount = participationMapper.selectCount(
            new LambdaQueryWrapper<ClassroomParticipation>()
                .eq(ClassroomParticipation::getSessionId, sessionId)
                .eq(ClassroomParticipation::getParticipationType, "LIVE_QUIZ"));

        List<ClassroomParticipation> allAnswers = participationMapper.selectList(
            new LambdaQueryWrapper<ClassroomParticipation>()
                .eq(ClassroomParticipation::getSessionId, sessionId)
                .eq(ClassroomParticipation::getParticipationType, "LIVE_QUIZ"));

        // 构建选项统计
        Map<String, Integer> optionCounts = new LinkedHashMap<>();
        for (ClassroomParticipation p : allAnswers) {
            String ans = p.getResponse();
            optionCounts.merge(ans, 1, Integer::sum);
        }

        // SSE广播进度（不含正确答案）
        Map<String, Object> progressData = new LinkedHashMap<>();
        progressData.put("sessionId", sessionId);
        progressData.put("totalAnswered", totalCount);
        progressData.put("optionCounts", optionCounts);
        broadcastClassroom(session.getClassId(), "live-quiz:progress", progressData);

        return Map.of("correct", isCorrect, "correctAnswer", correctAnswer, "scoreEarned", scoreEarned);
    }

    @Override
    @Transactional
    public Map<String, Object> endLiveQuiz(Long sessionId, boolean revealAnswer) {
        // 取消自动关闭定时器（若存在）
        ScheduledFuture<?> timer = pollTimers.remove(sessionId);
        if (timer != null && !timer.isDone()) {
            timer.cancel(false);
        }

        ClassroomSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(404, "随堂速答会话不存在");
        }

        // 已关闭则不再操作
        if ("ENDED".equals(session.getStatus())) {
            try {
                return objectMapper.readValue(session.getPollData(),
                        new TypeReference<Map<String, Object>>() {});
            } catch (IOException e) {
                return Map.of();
            }
        }

        // 更新状态
        session.setStatus("ENDED");
        sessionMapper.updateById(session);

        // 解析 pollData
        Map<String, Object> pollData;
        try {
            pollData = objectMapper.readValue(session.getPollData(),
                    new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            pollData = new LinkedHashMap<>();
        }

        // 统计所有参与记录
        List<ClassroomParticipation> allAnswers = participationMapper.selectList(
            new LambdaQueryWrapper<ClassroomParticipation>()
                .eq(ClassroomParticipation::getSessionId, sessionId)
                .eq(ClassroomParticipation::getParticipationType, "LIVE_QUIZ"));

        String correctAnswer = (String) pollData.get("correctAnswer");
        @SuppressWarnings("unchecked")
        List<String> options = (List<String>) pollData.getOrDefault("options", Collections.emptyList());

        // 构建选项计数（用 A/B/C/D 字母做 key，与学生提交的 answer 格式一致）
        Map<String, Integer> optionCountMap = new LinkedHashMap<>();
        optionCountMap.put("A", 0);
        optionCountMap.put("B", 0);
        optionCountMap.put("C", 0);
        optionCountMap.put("D", 0);
        int correctCount = 0;
        for (ClassroomParticipation p : allAnswers) {
            String ans = p.getResponse();
            if (ans != null) {
                optionCountMap.merge(ans, 1, Integer::sum);
                if (correctAnswer != null && ans.trim().equalsIgnoreCase(correctAnswer.trim())) {
                    correctCount++;
                }
            }
        }

        int totalCount = allAnswers.size();
        double accuracy = totalCount > 0 ? (double) correctCount / totalCount : 0.0;
        accuracy = Math.round(accuracy * 100.0) / 100.0;

        // 构建结果
        Map<String, Object> results = new LinkedHashMap<>();
        results.put("totalAnswered", totalCount);
        results.put("correctCount", correctCount);
        results.put("accuracy", accuracy);
        results.put("optionCounts", optionCountMap);
        if (revealAnswer) {
            results.put("correctAnswer", correctAnswer);
        }

        // 参与者列表
        List<Map<String, Object>> participants = new ArrayList<>();
        for (ClassroomParticipation p : allAnswers) {
            Map<String, Object> pData = new LinkedHashMap<>();
            pData.put("studentId", p.getStudentId());
            pData.put("answer", p.getResponse());
            pData.put("isCorrect", p.getIsCorrect() != null && p.getIsCorrect() == 1 ? 1 : 0);
            participants.add(pData);
        }
        results.put("participants", participants);

        // 清除活动缓存
        clearActiveSessionCache(session.getClassId());

        // SSE广播结果
        Map<String, Object> resultData = new LinkedHashMap<>();
        resultData.put("sessionId", sessionId);
        resultData.put("results", results);
        broadcastClassroom(session.getClassId(), "live-quiz:result", resultData);

        return Map.of("results", results);
    }

    @Override
    @Transactional
    public Map<String, Object> pickLiveQuizStudent(Long classId, Long teacherId, List<Long> excludeStudentIds) {
        // 获取班级活跃学生
        List<Student> students = studentMapper.selectList(
                new LambdaQueryWrapper<Student>()
                        .eq(Student::getClassId, classId)
                        .eq(Student::getStatus, "active"));
        if (students.isEmpty()) {
            throw new BusinessException(404, "班级无活跃学生");
        }

        // 过滤排除列表
        Set<Long> excludeSet = new HashSet<>();
        if (excludeStudentIds != null) {
            excludeSet.addAll(excludeStudentIds);
        }

        List<Student> candidates = students.stream()
                .filter(s -> !excludeSet.contains(s.getId()))
                .collect(Collectors.toList());
        if (candidates.isEmpty()) {
            throw new BusinessException(400, "所有学生已被排除，无可选学生");
        }

        // 随机抽取
        Student picked = candidates.get(new Random().nextInt(candidates.size()));

        // 获取学生姓名
        User u = userMapper.selectById(picked.getUserId());
        String studentName = u != null ? u.getRealName() : "未知";

        // 加积分
        creditService.adjustCredit(picked.getId(), 1, "课堂被点名 +1分");

        // SSE广播
        Map<String, Object> pickData = new LinkedHashMap<>();
        pickData.put("studentId", picked.getId());
        pickData.put("studentName", studentName);
        broadcastClassroom(classId, "live-quiz:pick", pickData);

        return Map.of("studentId", picked.getId(), "studentName", studentName);
    }

    /** 自动关闭随堂速答（由调度器调用） */
    private void autoCloseLiveQuiz(Long sessionId) {
        try {
            pollTimers.remove(sessionId);
            self.endLiveQuiz(sessionId, true);
        } catch (Exception e) {
            log.error("autoCloseLiveQuiz failed, sessionId={}", sessionId, e);
        }
    }

    // ======================== 会话查询 ========================

    @Override
    public List<Map<String, Object>> getSessions(Long classId, int limit) {
        List<ClassroomSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<ClassroomSession>()
                        .eq(ClassroomSession::getClassId, classId)
                        .orderByDesc(ClassroomSession::getCreatedAt)
                        .last("LIMIT " + Math.min(Math.max(limit, 1), 200)));

        if (sessions.isEmpty()) return Collections.emptyList();

        // L5: 批量加载教师姓名（避免N+1）
        Set<Long> teacherIds = sessions.stream()
                .map(ClassroomSession::getTeacherId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> teacherNameMap = teacherIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(teacherIds).stream()
                    .collect(Collectors.toMap(User::getId, User::getRealName, (a, b) -> a));

        // L5: 批量加载参与人数（避免N+1）
        Set<Long> sessionIds = sessions.stream().map(ClassroomSession::getId).collect(Collectors.toSet());
        List<ClassroomParticipation> allParts = participationMapper.selectList(
                new LambdaQueryWrapper<ClassroomParticipation>()
                        .in(ClassroomParticipation::getSessionId, sessionIds));
        Map<Long, Long> participantCountMap = allParts.stream()
                .collect(Collectors.groupingBy(ClassroomParticipation::getSessionId, Collectors.counting()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (ClassroomSession session : sessions) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", session.getId());
            map.put("sessionType", session.getSessionType());
            map.put("sceneMode", session.getSceneMode());
            map.put("status", session.getStatus());
            map.put("questionText", session.getQuestionText());
            map.put("createdAt", session.getCreatedAt());
            map.put("teacherName", session.getTeacherId() != null
                    ? teacherNameMap.getOrDefault(session.getTeacherId(), "未知") : "未知");
            map.put("participantCount", participantCountMap.getOrDefault(session.getId(), 0L));

            if ("POLL".equals(session.getSessionType()) && session.getPollData() != null) {
                try {
                    Map<String, Object> pd = objectMapper.readValue(session.getPollData(),
                            new TypeReference<Map<String, Object>>() {});
                    map.put("pollData", pd);
                } catch (IOException e) {
                    map.put("pollData", null);
                }
            }

            result.add(map);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getClassroomScores(Long classId) {
        // 获取班级全体活跃学生
        List<Student> classStudents = studentMapper.selectList(
                new LambdaQueryWrapper<Student>()
                        .eq(Student::getClassId, classId)
                        .eq(Student::getStatus, "active"));
        if (classStudents.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> studentIds = classStudents.stream().map(Student::getId).collect(Collectors.toSet());

        // 最近24小时参与积分（scoreEarned > 0）
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<ClassroomParticipation> participations = participationMapper.selectList(
                new LambdaQueryWrapper<ClassroomParticipation>()
                        .in(ClassroomParticipation::getStudentId, studentIds)
                        .ge(ClassroomParticipation::getCreatedAt, since)
                        .gt(ClassroomParticipation::getScoreEarned, 0));

        // 按 studentId 聚合积分
        Map<Long, Integer> scoreMap = new HashMap<>();
        for (ClassroomParticipation p : participations) {
            scoreMap.merge(p.getStudentId(), p.getScoreEarned(), Integer::sum);
        }

        // 批量加载用户姓名+头像
        Set<Long> uids = classStudents.stream().map(Student::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = uids.isEmpty() ? Collections.emptyMap() :
            userMapper.selectBatchIds(uids).stream().collect(Collectors.toMap(User::getId, u -> u));

        // 构造结果：全体学生，积分默认0
        List<Map<String, Object>> result = new ArrayList<>();
        for (Student s : classStudents) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("studentId", s.getId());
            User user = userMap.get(s.getUserId());
            item.put("name", user != null ? user.getRealName() : "未知");
            if (user != null && user.getAvatarUrl() != null && !user.getAvatarUrl().isBlank()) {
                item.put("avatarUrl", user.getAvatarUrl());
            }
            item.put("sessionScore", scoreMap.getOrDefault(s.getId(), 0));
            result.add(item);
        }

        // 按积分降序
        result.sort((a, b) -> Integer.compare(
                (Integer) b.get("sessionScore"), (Integer) a.get("sessionScore")));

        return result;
    }

    // ======================== 抽问题库 CRUD ========================

    @Override
    public Map<String, Object> getQuestions(Long teacherId, Long taskId,
            String keyword, Integer page, Integer pageSize) {
        LambdaQueryWrapper<ClassroomQuestion> wrapper = new LambdaQueryWrapper<ClassroomQuestion>()
                .eq(ClassroomQuestion::getTeacherId, teacherId);

        if (taskId != null) {
            wrapper.eq(ClassroomQuestion::getTaskId, taskId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(ClassroomQuestion::getContent, keyword);
        }
        wrapper.orderByDesc(ClassroomQuestion::getUpdatedAt);

        int pn = (page != null && page > 0) ? page : 1;
        int ps = (pageSize != null && pageSize > 0) ? pageSize : 20;
        Page<ClassroomQuestion> pageObj = questionMapper.selectPage(new Page<>(pn, ps), wrapper);

        List<Map<String, Object>> records = new ArrayList<>();
        for (ClassroomQuestion q : pageObj.getRecords()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", q.getId());
            map.put("taskId", q.getTaskId());
            map.put("content", q.getContent());
            map.put("createdAt", q.getCreatedAt());
            records.add(map);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", pageObj.getTotal());
        result.put("pageNum", pageObj.getCurrent());
        result.put("pageSize", pageObj.getSize());
        return result;
    }

    @Override
    public Map<String, Object> getQuestionFilters(Long teacherId) {
        LambdaQueryWrapper<ClassroomQuestion> wrapper = new LambdaQueryWrapper<ClassroomQuestion>()
                .eq(ClassroomQuestion::getTeacherId, teacherId);
        List<ClassroomQuestion> all = questionMapper.selectList(wrapper);

        Set<String> subjects = new LinkedHashSet<>();
        Set<String> chapters = new LinkedHashSet<>();
        Set<String> tags = new LinkedHashSet<>();
        for (ClassroomQuestion q : all) {
            if (q.getSubject() != null && !q.getSubject().isEmpty()) subjects.add(q.getSubject());
            if (q.getChapter() != null && !q.getChapter().isEmpty()) chapters.add(q.getChapter());
            if (q.getTag() != null && !q.getTag().isEmpty()) tags.add(q.getTag());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("subjects", new ArrayList<>(subjects));
        result.put("chapters", new ArrayList<>(chapters));
        result.put("tags", new ArrayList<>(tags));
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> addQuestion(Long teacherId, ClassroomQuestionRequest request) {
        ClassroomQuestion q = new ClassroomQuestion();
        q.setTeacherId(teacherId);
        q.setTaskId(request.getTaskId());
        q.setSubject(request.getSubject());
        q.setChapter(request.getChapter());
        q.setTag(request.getTag());
        q.setContent(request.getContent());
        q.setReferenceAnswer(request.getReferenceAnswer());
        q.setDifficulty(request.getDifficulty() != null ? request.getDifficulty() : 2);
        q.setSource(request.getSource() != null ? request.getSource() : "MANUAL");
        q.setQuestionType(request.getQuestionType());
        q.setIntent(request.getIntent());
        q.setAiCategory(request.getAiCategory());
        q.setUsageCount(0);
        questionMapper.insert(q);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", q.getId());
        result.put("content", q.getContent());
        result.put("subject", q.getSubject());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> updateQuestion(Long id, ClassroomQuestionRequest request) {
        ClassroomQuestion q = questionMapper.selectById(id);
        if (q == null) {
            throw new BusinessException(404, "题目不存在");
        }

        if (request.getTaskId() != null) {
            q.setTaskId(request.getTaskId());
        }
        if (request.getSubject() != null) {
            q.setSubject(request.getSubject());
        }
        if (request.getChapter() != null) {
            q.setChapter(request.getChapter());
        }
        if (request.getTag() != null) {
            q.setTag(request.getTag());
        }
        if (request.getContent() != null) {
            q.setContent(request.getContent());
        }
        if (request.getReferenceAnswer() != null) {
            q.setReferenceAnswer(request.getReferenceAnswer());
        }
        if (request.getDifficulty() != null) {
            q.setDifficulty(request.getDifficulty());
        }
        if (request.getSource() != null) {
            q.setSource(request.getSource());
        }
        if (request.getQuestionType() != null) {
            q.setQuestionType(request.getQuestionType());
        }
        if (request.getIntent() != null) {
            q.setIntent(request.getIntent());
        }
        if (request.getAiCategory() != null) {
            q.setAiCategory(request.getAiCategory());
        }
        questionMapper.updateById(q);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", q.getId());
        result.put("content", q.getContent());
        return result;
    }

    @Override
    @Transactional
    public void deleteQuestion(Long id) {
        questionMapper.deleteById(id);
    }

    @Override
    @Transactional
    public List<Map<String, Object>> batchImportQuestions(Long teacherId,
            List<Map<String, Object>> rows) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            ClassroomQuestion q = new ClassroomQuestion();
            q.setTeacherId(teacherId);
            q.setSubject((String) row.get("subject"));
            q.setChapter((String) row.get("chapter"));
            q.setTag((String) row.get("tag"));
            q.setContent((String) row.get("content"));
            q.setReferenceAnswer((String) row.get("referenceAnswer"));
            q.setDifficulty(row.get("difficulty") != null
                    ? ((Number) row.get("difficulty")).intValue() : 2);
            q.setSource("IMPORT");
            q.setUsageCount(0);
            questionMapper.insert(q);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", q.getId());
            item.put("content", q.getContent());
            result.add(item);
        }
        return result;
    }

    @Override
    @Transactional
    public List<Map<String, Object>> importFromQuestionBank(Long teacherId,
            List<Long> questionBankIds) {
        if (questionBankIds == null || questionBankIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 使用 selectList + in 代替 selectBatchIds（兼容性更好）
        List<QuestionBank> bankQuestions = questionBankMapper.selectList(
                new LambdaQueryWrapper<QuestionBank>()
                        .in(QuestionBank::getId, questionBankIds));

        List<Map<String, Object>> result = new ArrayList<>();
        for (QuestionBank bq : bankQuestions) {
            ClassroomQuestion q = new ClassroomQuestion();
            q.setTeacherId(teacherId);
            q.setSubject(bq.getSubject());
            q.setContent(bq.getQuestionText());
            q.setReferenceAnswer(bq.getCorrectAnswer());
            q.setDifficulty(bq.getDifficultyLevel() != null ? bq.getDifficultyLevel() : 2);
            q.setSource("QUESTION_BANK");
            q.setSourceQuestionId(bq.getId());
            q.setUsageCount(0);
            questionMapper.insert(q);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", q.getId());
            item.put("content", q.getContent());
            item.put("sourceQuestionId", bq.getId());
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> parseExcelRows(MultipartFile file) throws Exception {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            // 第一行为表头：学科|章节|标签|题目内容|参考答案|难度
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) throw new IllegalArgumentException("Excel第一行为表头，不能为空");
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String content = getCellString(row, 3);
                if (content == null || content.isBlank()) continue; // 跳过空行
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("subject", getCellString(row, 0));
                item.put("chapter", getCellString(row, 1));
                item.put("tag", getCellString(row, 2));
                item.put("content", content);
                item.put("referenceAnswer", getCellString(row, 4));
                String diffStr = getCellString(row, 5);
                int difficulty = 2;
                if (diffStr != null && !diffStr.isBlank()) {
                    try { difficulty = (int) Double.parseDouble(diffStr.trim()); } catch (NumberFormatException e) { /* use default */ }
                }
                item.put("difficulty", difficulty);
                item.put("source", "IMPORT");
                rows.add(item);
            }
        }
        return rows;
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> cell.toString().trim();
        };
    }

    // ======================== txt 批量导入 ========================

    @Override
    @Transactional
    public List<Map<String, Object>> batchImportTxt(Long teacherId, MultipartFile file, Long taskId) throws Exception {
        String content = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
        String[] lines = content.split("\\r?\\n");
        List<Map<String, Object>> result = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            ClassroomQuestion q = new ClassroomQuestion();
            q.setTeacherId(teacherId);
            q.setTaskId(taskId);
            q.setContent(trimmed);
            q.setSource("TXT_IMPORT");
            q.setUsageCount(0);
            questionMapper.insert(q);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", q.getId());
            item.put("content", q.getContent());
            result.add(item);
        }
        return result;
    }

    // ======================== 抽问池管理 ========================

    @Override
    @Transactional
    public void removeFromQuizPool(Long classId, Long studentId) {
        // 利用缺席表记录来"主动移除"：追加到缺席列表
        List<Long> absent = getAbsentStudents(classId);
        if (!absent.contains(studentId)) {
            absent.add(studentId);
            markAbsentStudents(classId, absent);
        }
    }

    @Override
    @Transactional
    public void resetQuizPool(Long classId) {
        LocalDateTime since = LocalDateTime.now().minusHours(2);
        List<ClassroomSession> recentSessions = sessionMapper.selectList(
                new LambdaQueryWrapper<ClassroomSession>()
                        .eq(ClassroomSession::getClassId, classId)
                        .eq(ClassroomSession::getSessionType, "QUIZ")
                        .ge(ClassroomSession::getCreatedAt, since));
        if (!recentSessions.isEmpty()) {
            Set<Long> sessionIds = recentSessions.stream()
                    .map(ClassroomSession::getId).collect(Collectors.toSet());
            participationMapper.delete(
                    new LambdaQueryWrapper<ClassroomParticipation>()
                            .in(ClassroomParticipation::getSessionId, sessionIds)
                            .eq(ClassroomParticipation::getParticipationType, "QUIZZED"));
        }
        log.info("抽问池已重置: 班级{} 清除{}条参与记录", classId,
                recentSessions.isEmpty() ? 0 : recentSessions.size());
    }

    // ======================== 缺席学生管理 ========================

    @Autowired private ClassroomAbsentStudentMapper absentStudentMapper;

    @Override
    @Transactional
    public void markAbsentStudents(Long classId, List<Long> studentIds) {
        if (classId == null) return;
        // 先清除该班级旧DB记录
        absentStudentMapper.delete(new LambdaQueryWrapper<ClassroomAbsentStudent>()
            .eq(ClassroomAbsentStudent::getClassId, classId));
        if (studentIds == null || studentIds.isEmpty()) return;
        // 写入DB
        LocalDateTime now = LocalDateTime.now();
        for (Long sid : studentIds) {
            ClassroomAbsentStudent rec = new ClassroomAbsentStudent();
            rec.setClassId(classId);
            rec.setStudentId(sid);
            rec.setMarkedAt(now);
            absentStudentMapper.insert(rec);
        }
    }

    @Override
    @Transactional
    public void unmarkAbsentStudent(Long classId, Long studentId) {
        if (classId == null || studentId == null) return;
        absentStudentMapper.delete(new LambdaQueryWrapper<ClassroomAbsentStudent>()
            .eq(ClassroomAbsentStudent::getClassId, classId)
            .eq(ClassroomAbsentStudent::getStudentId, studentId));
    }

    @Override
    public List<Long> getAbsentStudents(Long classId) {
        if (classId == null) return Collections.emptyList();
        // 直接从DB读取，确保多实例一致性
        return loadAbsentCache(classId);
    }

    @Override
    @Transactional
    public void clearAbsentStudents(Long classId) {
        if (classId == null) return;
        absentStudentMapper.delete(new LambdaQueryWrapper<ClassroomAbsentStudent>()
            .eq(ClassroomAbsentStudent::getClassId, classId));
    }

    @Override
    public void broadcastTaskStarted(Long classId, Long taskId, String taskTitle) {
        broadcastClassroom(classId, "task:started",
            Map.of("taskId", taskId, "title", taskTitle));
    }

    @Override
    public Map<String, Object> getClassroomAnalytics(Long classId, String dateRange) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 计算日期范围
        LocalDateTime since;
        switch (dateRange != null ? dateRange : "30d") {
            case "7d": since = LocalDateTime.now().minusDays(7); break;
            case "90d": since = LocalDateTime.now().minusDays(90); break;
            default: since = LocalDateTime.now().minusDays(30); break;
        }

        // 1. 覆盖率
        List<Student> classStudents = studentMapper.selectList(
            new LambdaQueryWrapper<Student>()
                .eq(Student::getClassId, classId)
                .eq(Student::getStatus, "active"));
        Set<Long> classStudentIds = classStudents.stream().map(Student::getId).collect(Collectors.toSet());
        int totalStudents = classStudentIds.size();

        List<ClassroomParticipation> participations = participationMapper.selectList(
            new LambdaQueryWrapper<ClassroomParticipation>()
                .ge(ClassroomParticipation::getCreatedAt, since));
        Set<Long> participatedStudents = participations.stream()
            .map(ClassroomParticipation::getStudentId)
            .filter(classStudentIds::contains)
            .collect(Collectors.toSet());
        double coverageRate = totalStudents > 0 ? (double) participatedStudents.size() / totalStudents : 0;
        result.put("coverageRate", Math.round(coverageRate * 100.0) / 100.0);
        result.put("totalStudents", totalStudents);
        result.put("participatedStudents", participatedStudents.size());

        // 2. 学生参与分布
        Map<Long, Integer> studentCounts = new LinkedHashMap<>();
        for (ClassroomParticipation p : participations) {
            if (!classStudentIds.contains(p.getStudentId())) continue;
            studentCounts.merge(p.getStudentId(), 1, Integer::sum);
        }

        Map<Long, String> studentNameMap = new HashMap<>();
        if (!studentCounts.isEmpty()) {
            Set<Long> uids = classStudents.stream()
                .filter(s -> studentCounts.containsKey(s.getId()))
                .map(Student::getUserId)
                .collect(Collectors.toSet());
            if (!uids.isEmpty()) {
                userMapper.selectBatchIds(uids).forEach(u -> {
                    Student s = classStudents.stream()
                        .filter(st -> st.getUserId().equals(u.getId()))
                        .findFirst().orElse(null);
                    if (s != null) studentNameMap.put(s.getId(), u.getRealName());
                });
            }
        }

        List<Map<String, Object>> studentDistribution = studentCounts.entrySet().stream()
            .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
            .limit(20)
            .map(e -> Map.<String, Object>of(
                "studentId", e.getKey(),
                "studentName", studentNameMap.getOrDefault(e.getKey(), "未知"),
                "count", e.getValue()))
            .collect(Collectors.toList());
        result.put("studentDistribution", studentDistribution);

        // 3. 知识点正确率（从classroom_sessions + participations聚合）
        List<ClassroomSession> sessions = sessionMapper.selectList(
            new LambdaQueryWrapper<ClassroomSession>()
                .eq(ClassroomSession::getClassId, classId)
                .ge(ClassroomSession::getCreatedAt, since));
        Map<String, int[]> knowledgeStats = new LinkedHashMap<>(); // [correctCount, totalCount]
        for (ClassroomSession session : sessions) {
            List<ClassroomParticipation> sessionParts = participations.stream()
                .filter(p -> p.getSessionId().equals(session.getId()))
                .collect(Collectors.toList());
            for (ClassroomParticipation p : sessionParts) {
                if (p.getIsCorrect() == null) continue;
                String tag = session.getQuestionText() != null && session.getQuestionText().length() > 20
                    ? session.getQuestionText().substring(0, 20) + "..."
                    : (session.getQuestionText() != null ? session.getQuestionText() : "未命名");
                int[] stats = knowledgeStats.computeIfAbsent(tag, k -> new int[2]);
                stats[1]++;
                if (p.getIsCorrect() == 1) stats[0]++;
            }
        }
        List<Map<String, Object>> knowledgeAccuracy = knowledgeStats.entrySet().stream()
            .map(e -> Map.<String, Object>of(
                "knowledge", e.getKey(),
                "accuracy", e.getValue()[1] > 0 ? Math.round((double) e.getValue()[0] / e.getValue()[1] * 100.0) / 100.0 : 0,
                "total", e.getValue()[1]))
            .sorted((a, b) -> Double.compare((Double) b.get("accuracy"), (Double) a.get("accuracy")))
            .limit(10)
            .collect(Collectors.toList());
        result.put("knowledgeAccuracy", knowledgeAccuracy);

        // 4. 互动趋势（按周聚合）
        Map<String, int[]> weekStats = new LinkedHashMap<>(); // [quiz, buzz, poll]
        for (ClassroomSession session : sessions) {
            String week = getWeekKey(session.getCreatedAt());
            int[] stats = weekStats.computeIfAbsent(week, k -> new int[3]);
            switch (session.getSessionType() != null ? session.getSessionType() : "") {
                case "QUIZ": stats[0]++; break;
                case "BUZZ": stats[1]++; break;
                case "POLL": stats[2]++; break;
            }
        }
        List<Map<String, Object>> interactionTrend = weekStats.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> Map.<String, Object>of(
                "week", e.getKey(),
                "quiz", e.getValue()[0],
                "buzz", e.getValue()[1],
                "poll", e.getValue()[2]))
            .collect(Collectors.toList());
        result.put("interactionTrend", interactionTrend);

        // 5. 热门题目TOP5
        List<ClassroomQuestion> questions = questionMapper.selectList(
            new LambdaQueryWrapper<ClassroomQuestion>()
                .orderByDesc(ClassroomQuestion::getUsageCount)
                .last("LIMIT 5"));
        List<Map<String, Object>> topQuestions = questions.stream()
            .map(q -> Map.<String, Object>of(
                "id", q.getId(),
                "content", q.getContent() != null && q.getContent().length() > 50
                    ? q.getContent().substring(0, 50) + "..." : q.getContent(),
                "usageCount", q.getUsageCount() != null ? q.getUsageCount() : 0))
            .collect(Collectors.toList());
        result.put("topQuestions", topQuestions);

        return result;
    }

    private String getWeekKey(LocalDateTime dt) {
        if (dt == null) return "unknown";
        int year = dt.getYear();
        int week = dt.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
        return String.format("%d-W%02d", year, week);
    }

    /** 从DB加载缺席学生列表 */
    private List<Long> loadAbsentCache(Long classId) {
        List<ClassroomAbsentStudent> records = absentStudentMapper.selectList(
            new LambdaQueryWrapper<ClassroomAbsentStudent>()
                .eq(ClassroomAbsentStudent::getClassId, classId));
        return records.stream()
            .map(ClassroomAbsentStudent::getStudentId)
            .collect(Collectors.toList());
    }

    // ======================== 私有方法 ========================

    private Student weightedRandomPick(List<Student> candidates, Map<Long, Double> weights) {
        Map<Long, Double> effective = new LinkedHashMap<>();
        double totalWeight = 0;
        for (Student s : candidates) {
            double w = weights != null ? weights.getOrDefault(s.getId(), 1.0) : 1.0;
            w = Math.max(w, 0.01); // 避免权重为0
            effective.put(s.getId(), w);
            totalWeight += w;
        }
        double r = Math.random() * totalWeight;
        double cumulative = 0;
        for (Map.Entry<Long, Double> e : effective.entrySet()) {
            cumulative += e.getValue();
            if (r <= cumulative) {
                Long id = e.getKey();
                return candidates.stream()
                        .filter(s -> s.getId().equals(id))
                        .findFirst()
                        .orElse(candidates.get(candidates.size() - 1));
            }
        }
        return candidates.get(candidates.size() - 1);
    }

    /**
     * 将 classroom_questions.id 解析为 question_bank.id。
     * - 已有 syncedQuestionBankId → 直接返回
     * - 来源为题库(sourceQuestionId) → 返回原题ID
     * - 手动题目 → 查重/插入 question_bank(type=CLASSROOM_MANUAL) → 回填 synced_question_bank_id
     */
    private Long resolveOriginalQuestionId(Long sessionQuestionId) {
        if (sessionQuestionId == null) {
            return null;
        }
        ClassroomQuestion cq = questionMapper.selectById(sessionQuestionId);
        if (cq == null) {
            return sessionQuestionId;
        }
        // 1. 已有同步记录 → 直接使用
        if (cq.getSyncedQuestionBankId() != null) {
            return cq.getSyncedQuestionBankId();
        }
        // 2. 来自题库 → 直接使用 sourceQuestionId
        if (cq.getSourceQuestionId() != null) {
            return cq.getSourceQuestionId();
        }
        // 3. 手动题目 → 查重或插入 question_bank，然后回填
        String stem = cq.getContent();
        if (stem == null || stem.isBlank()) {
            return sessionQuestionId;
        }
        QuestionBank existing = questionBankMapper.selectOne(
            new LambdaQueryWrapper<QuestionBank>()
                .eq(QuestionBank::getQuestionText, stem)
                .last("LIMIT 1"));
        Long qbId;
        if (existing != null) {
            qbId = existing.getId();
        } else {
            QuestionBank qb = new QuestionBank();
            qb.setQuestionType("CLASSROOM_MANUAL");
            qb.setQuestionText(stem);
            qb.setCorrectAnswer(cq.getReferenceAnswer());
            qb.setDifficultyLevel(cq.getDifficulty() != null ? cq.getDifficulty() : 2);
            qb.setStatus(1);
            questionBankMapper.insert(qb);
            qbId = qb.getId();
        }
        // 回填 synced_question_bank_id
        cq.setSyncedQuestionBankId(qbId);
        questionMapper.updateById(cq);
        return qbId;
    }

    @Override
    public List<ClassroomQuestion> getAiRecommended(Long teacherId, String subject, String tag) {
        LambdaQueryWrapper<ClassroomQuestion> qw = new LambdaQueryWrapper<>();
        qw.eq(ClassroomQuestion::getFromAi, 1)
           .eq(ClassroomQuestion::getTeacherId, teacherId);
        if (subject != null && !subject.isEmpty()) qw.eq(ClassroomQuestion::getSubject, subject);
        if (tag != null && !tag.isEmpty()) qw.eq(ClassroomQuestion::getAiCategory, tag);
        qw.orderByDesc(ClassroomQuestion::getCreatedAt);
        return questionMapper.selectList(qw);
    }

    @Override
    public void insertAiGeneratedQuestion(ClassroomQuestion question) {
        questionMapper.insert(question);
    }
}

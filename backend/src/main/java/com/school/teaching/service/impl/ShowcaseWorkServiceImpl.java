package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.school.teaching.entity.Classes;
import com.school.teaching.entity.ShowcaseWork;
import com.school.teaching.entity.Student;
import com.school.teaching.entity.TaskSubmission;
import com.school.teaching.entity.User;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.ClassesMapper;
import com.school.teaching.mapper.ShowcaseWorkMapper;
import com.school.teaching.mapper.StudentMapper;
import com.school.teaching.mapper.UserMapper;
import com.school.teaching.service.CreditService;
import com.school.teaching.service.NotificationService;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.ShowcaseWorkService;
import com.school.teaching.service.TeacherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ShowcaseWorkServiceImpl implements ShowcaseWorkService {

    private static final Logger log = LoggerFactory.getLogger(ShowcaseWorkServiceImpl.class);

    @Autowired private ShowcaseWorkMapper showcaseMapper;
    @Autowired private StudentMapper studentMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private ClassesMapper classesMapper;
    @Autowired private com.school.teaching.mapper.TaskSubmissionMapper submissionMapper;
    @Autowired private com.school.teaching.mapper.TaskMapper taskMapper;
    @Autowired private CreditService creditService;
    @Autowired private NotificationService notificationService;
    @Autowired private TeacherService teacherService;

    private static final Map<String, Integer> SCOPE_CREDITS = Map.of(
        "CLASS", 5, "MULTI_CLASS", 15, "SCHOOL", 30
    );

    @Override
    @Transactional
    public ShowcaseWork recommendWork(Map<String, Object> request) {
        Long teacherUserId = SecurityUtils.getCurrentUserId();
        if (teacherUserId == null) throw new BusinessException(401, "未登录");

        User currentUser = userMapper.selectById(teacherUserId);
        if (currentUser == null) throw new BusinessException(404, "用户不存在");

        Long classId = request.get("classId") != null ? Long.valueOf(request.get("classId").toString()) : null;
        String showScope = (String) request.getOrDefault("showScope", "CLASS");

        validateRecommendPermission(teacherUserId, showScope, classId);

        String title = (String) request.get("title");
        String sourceType = (String) request.get("sourceType");
        Long sourceId = request.get("sourceId") != null ? Long.valueOf(request.get("sourceId").toString()) : null;
        Long studentId = Long.valueOf(request.get("studentId").toString());
        String subject = (String) request.get("subject");
        String teacherComment = (String) request.get("teacherComment");
        String targetClassIds = (String) request.get("targetClassIds");

        Student student = studentMapper.selectById(studentId);
        if (student == null) throw new BusinessException(404, "学生不存在");

        int creditValue = SCOPE_CREDITS.getOrDefault(showScope, 5);

        ShowcaseWork work = createWorkRecord(title, sourceType, sourceId, studentId, classId,
            subject, teacherUserId, teacherComment, showScope, targetClassIds, creditValue);

        // 发放积分
        creditService.adjustCredit(studentId, creditValue, "优秀作品推荐(" + showScope + ")");

        // 发送通知
        sendRecommendNotifications(work, student, title, sourceType, teacherUserId, showScope,
            targetClassIds, classId, creditValue);

        return work;
    }

    private void validateRecommendPermission(Long teacherUserId, String showScope, Long classId) {
        boolean isAdminOrInspector = SecurityUtils.isAdmin() || SecurityUtils.isInspector();
        boolean isHeadTeacher = teacherService.isHeadTeacher(teacherUserId);
        List<Long> teachingClassIds = teacherService.getTeachingClassIds(teacherUserId);
        List<Long> accessibleClassIds = teacherService.getAccessibleClassIds(teacherUserId);

        if (isAdminOrInspector) return;
        if (isHeadTeacher) {
            if (!"CLASS".equals(showScope)) {
                throw new BusinessException(403, "班主任仅可推荐至本班，如需跨班或全校请联系管理员");
            }
            if (classId != null && !accessibleClassIds.contains(classId)) {
                throw new BusinessException(403, "仅可推荐自己班级的作品");
            }
        } else {
            if (!"CLASS".equals(showScope)) {
                throw new BusinessException(403, "教师仅可推荐至本班，如需跨班或全校请联系管理员");
            }
            if (classId != null && !teachingClassIds.contains(classId)) {
                throw new BusinessException(403, "仅可推荐自己任教班级的作品");
            }
        }
    }

    private ShowcaseWork createWorkRecord(String title, String sourceType, Long sourceId,
                                           Long studentId, Long classId, String subject,
                                           Long teacherUserId, String teacherComment,
                                           String showScope, String targetClassIds, int creditValue) {
        ShowcaseWork work = new ShowcaseWork();
        work.setTitle(title);
        work.setSourceType(sourceType);
        work.setSourceId(sourceId);
        work.setStudentId(studentId);
        work.setClassId(classId);
        work.setSubject(subject);
        work.setTeacherId(teacherUserId);
        work.setTeacherComment(teacherComment);
        work.setShowScope(showScope);
        work.setTargetClassIds(targetClassIds);
        work.setCreditAwarded(creditValue);
        work.setViewCount(0);
        work.setLikeCount(0);
        work.setStatus(1);
        showcaseMapper.insert(work);
        return work;
    }

    private void sendRecommendNotifications(ShowcaseWork work, Student student,
                                             String title, String sourceType,
                                             Long teacherUserId, String showScope,
                                             String targetClassIds, Long classId, int creditValue) {
        String scopeLabel = getScopeLabel(showScope);
        String studentNotifyContent = "恭喜！您的《" + title + "》被" + scopeLabel + "推荐为优秀作品，"
            + "这是对你努力学习的最好肯定！获得 +" + creditValue + " 积分奖励，继续加油！";
        notificationService.notify(student.getUserId(), "SHOWCASE_RECOMMEND",
            "作品上墙通知", studentNotifyContent, work.getId());

        String broadTitle = "新优秀作品上墙：《" + title + "》";
        User teacher = userMapper.selectById(teacherUserId);
        String teacherName = teacher != null ? teacher.getRealName() : "教师";
        String broadContent = teacherName + " 老师推荐了一篇优秀" + sourceTypeLabel(sourceType)
            + "作品《" + title + "》，获得 +" + creditValue + " 积分！"
            + "学好计算机并不难，只要动手练习，勤于思考、善于总结、乐于分享，你也能写出优秀的作品。快来展示墙学习交流吧！";
        notifyScope(showScope, targetClassIds, classId, broadTitle, broadContent, work.getId());
    }

    @Override
    @Transactional
    public ShowcaseWork updateWork(Long workId, Map<String, Object> request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) throw new BusinessException(401, "未登录");

        ShowcaseWork work = showcaseMapper.selectById(workId);
        if (work == null) throw new BusinessException(404, "作品不存在");

        // 仅推荐教师或管理员可以编辑
        User currentUser = userMapper.selectById(userId);
        boolean isAdmin = SecurityUtils.isAdmin();
        if (!isAdmin && !work.getTeacherId().equals(userId)) {
            throw new BusinessException(403, "无权编辑此作品");
        }

        String oldScope = work.getShowScope();
        String newScope = (String) request.getOrDefault("showScope", oldScope);

        // 更新字段
        if (request.containsKey("title")) work.setTitle((String) request.get("title"));
        if (request.containsKey("teacherComment")) work.setTeacherComment((String) request.get("teacherComment"));
        if (request.containsKey("targetClassIds")) work.setTargetClassIds((String) request.get("targetClassIds"));
        if (request.containsKey("subject")) work.setSubject((String) request.get("subject"));
        work.setShowScope(newScope);
        showcaseMapper.updateById(work);

        // 如果展示范围升级，补发积分差额
        if (!newScope.equals(oldScope)) {
            int oldCredits = SCOPE_CREDITS.getOrDefault(oldScope, 5);
            int newCredits = SCOPE_CREDITS.getOrDefault(newScope, 5);
            if (newCredits > oldCredits) {
                int diff = newCredits - oldCredits;
                creditService.adjustCredit(work.getStudentId(), diff,
                    "优秀作品展示范围升级(" + oldScope + "→" + newScope + ")补发");
                work.setCreditAwarded(newCredits);
                showcaseMapper.updateById(work);
            }
        }

        return work;
    }

    @Override
    @Transactional
    public void deleteWork(Long workId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) throw new BusinessException(401, "未登录");

        ShowcaseWork work = showcaseMapper.selectById(workId);
        if (work == null) throw new BusinessException(404, "作品不存在");

        User currentUser = userMapper.selectById(userId);
        boolean isAdmin = SecurityUtils.isAdmin();
        if (!isAdmin && !work.getTeacherId().equals(userId)) {
            throw new BusinessException(403, "无权删除此作品");
        }

        work.setStatus(0);
        showcaseMapper.updateById(work);
    }

    @Override
    public Map<String, Object> listWorks(Integer pageNum, Integer pageSize,
                                          String sourceType, String subject, Long classId, String grade) {
        Long studentClassId = getStudentClassId();
        List<Long> gradeClassIds = queryGradeClassIds(grade);
        if (grade != null && !grade.isEmpty() && gradeClassIds.isEmpty()) {
            return emptyPage(pageNum, pageSize);
        }

        LambdaQueryWrapper<ShowcaseWork> w = buildListQuery(sourceType, subject, gradeClassIds, studentClassId, classId);
        long total = showcaseMapper.selectCount(w);
        int offset = (pageNum - 1) * pageSize;
        int safePageSize = Math.min(Math.max(pageSize, 1), 1000);
        w.last("LIMIT " + offset + "," + safePageSize);
        List<ShowcaseWork> records = showcaseMapper.selectList(w);
        enrichShowcaseWorks(records);
        return buildPageResult(records, total, pageNum, pageSize);
    }

    private Long getStudentClassId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId != null && SecurityUtils.isStudent()) {
            LambdaQueryWrapper<Student> sw = new LambdaQueryWrapper<>();
            sw.eq(Student::getUserId, userId);
            Student s = studentMapper.selectOne(sw);
            if (s != null) return s.getClassId();
        }
        return null;
    }

    private List<Long> queryGradeClassIds(String grade) {
        if (grade == null || grade.isEmpty()) return null;
        LambdaQueryWrapper<Classes> cw = new LambdaQueryWrapper<>();
        cw.eq(Classes::getGrade, grade);
        return classesMapper.selectList(cw).stream().map(Classes::getId).toList();
    }

    private Map<String, Object> emptyPage(int pageNum, int pageSize) {
        Map<String, Object> empty = new HashMap<>();
        empty.put("records", Collections.emptyList());
        empty.put("total", 0L);
        empty.put("pageNum", pageNum);
        empty.put("pageSize", pageSize);
        return empty;
    }

    private LambdaQueryWrapper<ShowcaseWork> buildListQuery(String sourceType, String subject,
                                                             List<Long> gradeClassIds, Long studentClassId, Long classId) {
        LambdaQueryWrapper<ShowcaseWork> w = new LambdaQueryWrapper<>();
        w.eq(ShowcaseWork::getStatus, 1);
        if (sourceType != null && !sourceType.isEmpty()) w.eq(ShowcaseWork::getSourceType, sourceType);
        if (subject != null && !subject.isEmpty()) w.eq(ShowcaseWork::getSubject, subject);
        if (gradeClassIds != null) w.in(ShowcaseWork::getClassId, gradeClassIds);

        if (studentClassId != null) {
            Long finalStudentClassId = studentClassId;
            w.and(wr -> wr
                .eq(ShowcaseWork::getShowScope, "SCHOOL")
                .or().eq(ShowcaseWork::getClassId, finalStudentClassId)
                .or().like(ShowcaseWork::getTargetClassIds, String.valueOf(finalStudentClassId))
            );
        } else if (classId != null) {
            w.eq(ShowcaseWork::getClassId, classId);
        }
        w.orderByDesc(ShowcaseWork::getCreateTime);
        return w;
    }

    public void enrichShowcaseWorks(List<ShowcaseWork> records) {
        if (records.isEmpty()) return;
        Set<Long> studentIds = records.stream().map(ShowcaseWork::getStudentId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> teacherUserIds = records.stream().map(ShowcaseWork::getTeacherId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> classIds = records.stream().map(ShowcaseWork::getClassId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, Student> stuMap = new HashMap<>();
        Map<Long, User> userMap = new HashMap<>();
        Map<Long, Classes> classMap = new HashMap<>();

        if (!studentIds.isEmpty()) {
            List<Student> students = studentMapper.selectBatchIds(studentIds);
            for (Student s : students) stuMap.put(s.getId(), s);
            Set<Long> uids = students.stream().map(Student::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
            if (!uids.isEmpty()) {
                for (User u : userMapper.selectBatchIds(uids)) userMap.put(u.getId(), u);
            }
        }
        if (!teacherUserIds.isEmpty()) {
            for (User u : userMapper.selectBatchIds(teacherUserIds)) userMap.put(u.getId(), u);
        }
        if (!classIds.isEmpty()) {
            for (Classes c : classesMapper.selectBatchIds(classIds)) classMap.put(c.getId(), c);
        }

        for (ShowcaseWork sw : records) {
            Student stu = stuMap.get(sw.getStudentId());
            if (stu != null) {
                User stuUser = userMap.get(stu.getUserId());
                sw.setStudentName(stuUser != null ? stuUser.getRealName() : null);
            }
            User teacher = userMap.get(sw.getTeacherId());
            sw.setTeacherName(teacher != null ? teacher.getRealName() : null);
            Classes cls = classMap.get(sw.getClassId());
            if (cls != null) {
                sw.setClassName(cls.getClassName());
                sw.setGrade(cls.getGrade());
            }
        }

        // 批量填充提交内容快照（列表卡片用）
        Set<Long> sourceIds = new HashSet<>();
        Set<Long> stuIds = new HashSet<>();
        for (ShowcaseWork sw : records) {
            if (sw.getSourceId() != null) {
                sourceIds.add(sw.getSourceId());
                stuIds.add(sw.getStudentId());
            }
        }
        if (!sourceIds.isEmpty() && !stuIds.isEmpty()) {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            for (ShowcaseWork sw : records) {
                if (sw.getSourceId() == null) continue;
                TaskSubmission sub = submissionMapper.selectOne(new LambdaQueryWrapper<TaskSubmission>()
                    .eq(TaskSubmission::getTaskId, sw.getSourceId())
                    .eq(TaskSubmission::getStudentId, sw.getStudentId())
                    .orderByDesc(TaskSubmission::getCreatedAt)
                    .last("LIMIT 1"));
                if (sub != null) {
                    // 内容截断100字
                    if (sub.getContent() != null && !sub.getContent().isEmpty()) {
                        String truncated = sub.getContent().length() > 100
                            ? sub.getContent().substring(0, 100) + "..."
                            : sub.getContent();
                        sw.setSubmissionContent(truncated);
                    }
                    // 附件
                    if (sub.getAttachments() != null) {
                        sw.setSubmissionAttachments(sub.getAttachments());
                    }
                    // 提取首张图片URL
                    if (sub.getAttachments() != null) {
                        try {
                            String[] urls = mapper.readValue(sub.getAttachments(), String[].class);
                            for (String url : urls) {
                                if (url != null && url.matches(".*\\.(jpg|jpeg|png|gif|webp)(\\?.*)?$")) {
                                    sw.setFirstImageUrl(url);
                                    break;
                                }
                            }
                        } catch (Exception ignored) { log.debug("解析附件JSON失败: {}", ignored.getMessage()); }
                    }
                }
            }
        }
    }

    private Map<String, Object> buildPageResult(List<ShowcaseWork> records, long total,
                                                  int pageNum, int pageSize) {
        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", total);
        data.put("pageNum", pageNum);
        data.put("pageSize", pageSize);
        return data;
    }

    @Override
    public ShowcaseWork getWorkDetail(Long workId) {
        ShowcaseWork work = showcaseMapper.selectById(workId);
        if (work == null) return null;

        // 填充学生信息
        Student student = studentMapper.selectById(work.getStudentId());
        if (student != null) {
            User stuUser = userMapper.selectById(student.getUserId());
            work.setStudentName(stuUser != null ? stuUser.getRealName() : null);
        }

        // 填充教师信息
        User teacher = userMapper.selectById(work.getTeacherId());
        work.setTeacherName(teacher != null ? teacher.getRealName() : null);

        // 填充班级信息
        if (work.getClassId() != null) {
            Classes cls = classesMapper.selectById(work.getClassId());
            if (cls != null) {
                work.setClassName(cls.getClassName());
                work.setGrade(cls.getGrade());
            }
        }

        // 填充提交内容（TASK 类型）
        if ("TASK".equals(work.getSourceType()) && work.getSourceId() != null) {
            var sub = submissionMapper.selectOne(new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getTaskId, work.getSourceId())
                .eq(TaskSubmission::getStudentId, work.getStudentId())
                .orderByDesc(TaskSubmission::getCreatedAt)
                .last("LIMIT 1"));
            if (sub != null) {
                work.setSubmissionContent(sub.getContent());
                work.setSubmissionAttachments(sub.getAttachments());
                if (sub.getScore() != null) work.setSubmissionScore(sub.getScore().toString());
                else if (sub.getGradeLevel() != null) work.setSubmissionScore(sub.getGradeLevel());
            }
        }

        return work;
    }

    @Override
    public List<ShowcaseWork> getMyRecommended() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) throw new BusinessException(401, "未登录");
        LambdaQueryWrapper<ShowcaseWork> w = new LambdaQueryWrapper<>();
        w.eq(ShowcaseWork::getTeacherId, userId).eq(ShowcaseWork::getStatus, 1);
        w.orderByDesc(ShowcaseWork::getCreateTime);
        List<ShowcaseWork> records = showcaseMapper.selectList(w);
        enrichShowcaseWorks(records);
        return records;
    }

    @Override
    public void incrementLikeCount(Long id) {
        showcaseMapper.update(null,
            new LambdaUpdateWrapper<ShowcaseWork>()
                .eq(ShowcaseWork::getId, id)
                .setSql("like_count = COALESCE(like_count, 0) + 1"));
    }

    @Override
    public void updateLikeCount(Long id, Integer count) {
        ShowcaseWork work = new ShowcaseWork();
        work.setId(id);
        work.setLikeCount(count);
        showcaseMapper.updateById(work);
    }

    private void notifyScope(String showScope, String targetClassIds, Long sourceClassId,
                             String title, String content, Long relatedId) {
        switch (showScope) {
            case "SCHOOL":
                notificationService.notifyAllStudents("SHOWCASE_NEW", title, content);
                break;
            case "MULTI_CLASS":
                if (targetClassIds != null && !targetClassIds.isEmpty()) {
                    try {
                        String[] parts = targetClassIds.replace("[", "").replace("]", "").replace("\"", "").split(",");
                        for (String s : parts) {
                            Long cid = Long.valueOf(s.trim());
                            notificationService.notifyClassStudents(cid, "SHOWCASE_NEW", title, content, relatedId);
                        }
                    } catch (Exception ignored) { log.error("解析跨班通知targetClassIds失败", ignored); }
                }
                break;
            case "CLASS":
            default:
                if (sourceClassId != null) {
                    notificationService.notifyClassStudents(sourceClassId, "SHOWCASE_NEW", title, content, relatedId);
                }
                break;
        }
    }

    private String getScopeLabel(String scope) {
        return switch (scope) {
            case "SCHOOL" -> "全校";
            case "MULTI_CLASS" -> "跨班";
            default -> "班级";
        };
    }

    private String sourceTypeLabel(String type) {
        return switch (type != null ? type : "") {
            case "HOMEWORK" -> "作业";
            case "EXAM" -> "考试";
            case "PRACTICAL" -> "实训";
            default -> "作品";
        };
    }

    @Override
    public List<ShowcaseWork> getWeeklyStars() {
        return showcaseMapper.selectList(
            new LambdaQueryWrapper<ShowcaseWork>()
                .eq(ShowcaseWork::getStatus, 1)
                .ge(ShowcaseWork::getCreateTime, java.time.LocalDateTime.now().minusDays(7))
                .orderByDesc(ShowcaseWork::getLikeCount)
                .last("LIMIT 3"));
    }

    @Override
    public List<ShowcaseWork> getWorksByIds(java.util.Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return showcaseMapper.selectBatchIds(ids);
    }

}

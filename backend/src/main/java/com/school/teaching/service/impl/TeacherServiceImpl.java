package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.school.teaching.entity.Classes;
import com.school.teaching.entity.DictSubject;
import com.school.teaching.entity.GroupMember;
import com.school.teaching.entity.Teacher;
import com.school.teaching.entity.TeacherClass;
import com.school.teaching.entity.TeacherQuickComment;
import com.school.teaching.entity.User;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.ClassesMapper;
import com.school.teaching.mapper.DictSubjectMapper;
import com.school.teaching.mapper.GroupMemberMapper;
import com.school.teaching.mapper.TeacherClassMapper;
import com.school.teaching.mapper.TeacherMapper;
import com.school.teaching.mapper.TeacherQuickCommentMapper;
import com.school.teaching.mapper.UserMapper;
import com.school.teaching.service.TeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.school.teaching.security.SecurityUtils;
import jakarta.annotation.PostConstruct;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TeacherServiceImpl implements TeacherService {

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private TeacherClassMapper teacherClassMapper;

    @Autowired
    private DictSubjectMapper dictSubjectMapper;

    @Autowired
    private GroupMemberMapper groupMemberMapper;

    @PostConstruct
    void init() { SecurityUtils.setTeacherService(this); }

    @Autowired
    private ClassesMapper classesMapper;

    @Autowired
    private TeacherQuickCommentMapper teacherQuickCommentMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public Long getTeacherIdByUserId(Long userId) {
        LambdaQueryWrapper<Teacher> w = new LambdaQueryWrapper<>();
        w.eq(Teacher::getUserId, userId);
        Teacher t = teacherMapper.selectOne(w);
        return t != null ? t.getId() : null;
    }

    @Override
    public Teacher getTeacherEntityByUserId(Long userId) {
        return teacherMapper.selectOne(
            new LambdaQueryWrapper<Teacher>().eq(Teacher::getUserId, userId));
    }

    @Override
    public List<Long> getTeachingClassIds(Long userId) {
        Long teacherId = getTeacherIdByUserId(userId);
        if (teacherId == null) return Collections.emptyList();

        LambdaQueryWrapper<TeacherClass> w = new LambdaQueryWrapper<>();
        w.eq(TeacherClass::getTeacherId, teacherId);
        List<TeacherClass> list = teacherClassMapper.selectList(w);
        return list.stream().map(TeacherClass::getClassId).distinct().toList();
    }

    @Override
    public List<Long> getAccessibleClassIds(Long userId) {
        Set<Long> ids = new HashSet<>(getTeachingClassIds(userId));

        // 如果是班主任，加自己所管班级
        LambdaQueryWrapper<Classes> cw = new LambdaQueryWrapper<>();
        cw.eq(Classes::getHeadTeacherId, userId);
        List<Classes> headClasses = classesMapper.selectList(cw);
        ids.addAll(headClasses.stream().map(Classes::getId).toList());

        return new ArrayList<>(ids);
    }

    @Override
    public List<String> getTeachingSubjects(Long userId) {
        Long teacherId = getTeacherIdByUserId(userId);
        if (teacherId == null) return Collections.emptyList();

        LambdaQueryWrapper<TeacherClass> w = new LambdaQueryWrapper<>();
        w.eq(TeacherClass::getTeacherId, teacherId);
        List<TeacherClass> list = teacherClassMapper.selectList(w);
        return list.stream()
            .map(TeacherClass::getSubject)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    @Override
    public List<Map<String, Object>> getTeachingSubjectsWithIds(Long userId) {
        Long teacherId = getTeacherIdByUserId(userId);
        if (teacherId == null) return Collections.emptyList();

        // 收集需要查询任教学科的教师ID集合
        Set<Long> teacherIds = new LinkedHashSet<>();
        teacherIds.add(teacherId);

        // 教研组长：纳入组内所有成员
        if (isTeachingGroupLeader(teacherId)) {
            List<GroupMember> leaderRecords = groupMemberMapper.selectList(
                new LambdaQueryWrapper<GroupMember>()
                    .eq(GroupMember::getGroupType, "TEACHING")
                    .eq(GroupMember::getRole, "LEADER")
                    .eq(GroupMember::getTeacherId, teacherId));
            Set<Long> groupIds = leaderRecords.stream().map(GroupMember::getGroupId).collect(Collectors.toSet());
            if (!groupIds.isEmpty()) {
                List<GroupMember> allMembers = groupMemberMapper.selectList(
                    new LambdaQueryWrapper<GroupMember>()
                        .eq(GroupMember::getGroupType, "TEACHING")
                        .in(GroupMember::getGroupId, groupIds));
                allMembers.stream().map(GroupMember::getTeacherId).forEach(teacherIds::add);
            }
        }

        // 收集所有相关教师的任教学科原始名称（teacher_classes.subject 可能是逗号分隔的多学科）
        Set<String> rawSubjectNames = new LinkedHashSet<>();
        for (Long tid : teacherIds) {
            List<TeacherClass> tcs = teacherClassMapper.selectList(
                new LambdaQueryWrapper<TeacherClass>().eq(TeacherClass::getTeacherId, tid));
            for (TeacherClass tc : tcs) {
                if (tc.getSubject() == null || tc.getSubject().isBlank()) continue;
                // 兼容逗号/顿号/中文逗号分隔的多学科字段
                for (String part : tc.getSubject().split("[,，、]")) {
                    String s = part.trim();
                    if (!s.isEmpty()) rawSubjectNames.add(s);
                }
            }
        }
        if (rawSubjectNames.isEmpty()) return Collections.emptyList();

        // 加载所有启用的字典学科
        List<DictSubject> allDict = dictSubjectMapper.selectList(
            new LambdaQueryWrapper<DictSubject>().eq(DictSubject::getStatus, 1));

        // 将每个原始名称映射到字典学科ID（精确→模糊），结果按 dict_subject.id 去重
        Map<Long, String> idToDisplayName = new LinkedHashMap<>();
        for (String raw : rawSubjectNames) {
            DictSubject matched = null;
            for (DictSubject ds : allDict) {
                if (ds.getSubjectName() == null) continue;
                String dn = ds.getSubjectName().trim();
                if (raw.equals(dn)) { matched = ds; break; }
            }
            if (matched == null) {
                for (DictSubject ds : allDict) {
                    if (ds.getSubjectName() == null) continue;
                    String dn = ds.getSubjectName().trim();
                    if (raw.contains(dn) || dn.contains(raw)) { matched = ds; break; }
                }
            }
            if (matched != null && !idToDisplayName.containsKey(matched.getId())) {
                // 使用 dict_subject 的标准名称，保证同一学科统一显示
                idToDisplayName.put(matched.getId(), matched.getSubjectName().trim());
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, String> e : idToDisplayName.entrySet()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", e.getKey());
            m.put("subjectName", e.getValue());
            result.add(m);
        }
        return result;
    }

    private boolean isTeachingGroupLeader(Long teacherId) {
        if (teacherId == null) return false;
        return groupMemberMapper.selectCount(
            new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupType, "TEACHING")
                .eq(GroupMember::getTeacherId, teacherId)
                .eq(GroupMember::getRole, "LEADER")) > 0;
    }

    @Override
    public boolean isHeadTeacher(Long userId) {
        return classesMapper.selectCount(
            new LambdaQueryWrapper<Classes>().eq(Classes::getHeadTeacherId, userId)) > 0;
    }

    @Override
    public boolean isTeacherOrAdmin(Long roleId) {
        return roleId != null && ((roleId & 11) != 0); // ADMIN=1, TEACHER=2, SUPER_ADMIN=8
    }

    @Override
    public List<Map<String, Object>> getTeachingAssignments(Long userId) {
        Long teacherId = getTeacherIdByUserId(userId);
        if (teacherId == null) return Collections.emptyList();

        LambdaQueryWrapper<TeacherClass> w = new LambdaQueryWrapper<>();
        w.eq(TeacherClass::getTeacherId, teacherId);
        List<TeacherClass> list = teacherClassMapper.selectList(w);

        Set<Long> cids = list.stream().map(TeacherClass::getClassId).collect(Collectors.toSet());
        Map<Long, Classes> cmap = cids.isEmpty() ? Map.of() :
            classesMapper.selectBatchIds(cids).stream().collect(Collectors.toMap(Classes::getId, c -> c));

        // 构建学科名→学科ID映射（从 dict_subject 字典表）
        Map<String, Long> subjectMap = new HashMap<>();
        List<DictSubject> allSubjects = dictSubjectMapper.selectList(null);
        for (DictSubject ds : allSubjects) {
            if (ds.getSubjectName() != null) subjectMap.put(ds.getSubjectName(), ds.getId());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (TeacherClass tc : list) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", tc.getId());
            item.put("classId", tc.getClassId());
            item.put("subject", tc.getSubject());
            item.put("subjectId", subjectMap.getOrDefault(tc.getSubject(), 0L));
            Classes c = cmap.get(tc.getClassId());
            item.put("className", c != null ? c.getClassName() : "未知班级");
            item.put("grade", c != null ? c.getGrade() : "");
            item.put("major", c != null ? c.getMajor() : "");
            result.add(item);
        }
        return result;
    }

    @Override
    @Transactional
    public void setTeachingAssignments(Long userId, List<Map<String, Object>> assignments) {
        Long teacherId = getTeacherIdByUserId(userId);
        if (teacherId == null) throw new BusinessException(404, "教师记录不存在");

        // 删除旧的任课关系
        teacherClassMapper.delete(
            new LambdaQueryWrapper<TeacherClass>().eq(TeacherClass::getTeacherId, teacherId));

        // 插入新的
        List<TeacherClass> batch = new ArrayList<>();
        for (Map<String, Object> a : assignments) {
            TeacherClass tc = new TeacherClass();
            tc.setTeacherId(teacherId);
            tc.setClassId(Long.valueOf(a.get("classId").toString()));
            tc.setSubject((String) a.get("subject"));
            batch.add(tc);
        }
        for (TeacherClass tc : batch) teacherClassMapper.insert(tc);
    }

    @Override
    public Map<String, Object> getTeacherSummary(Long userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("teachingClasses", getTeachingAssignments(userId));
        result.put("isHeadTeacher", isHeadTeacher(userId));

        // 如果班主任，返回所管班级
        LambdaQueryWrapper<Classes> cw = new LambdaQueryWrapper<>();
        cw.eq(Classes::getHeadTeacherId, userId);
        List<Classes> headClasses = classesMapper.selectList(cw);
        if (!headClasses.isEmpty()) {
            result.put("headClass", headClasses.get(0).getClassName());
            result.put("headClassId", headClasses.get(0).getId());
        }
        return result;
    }

    // ===== Admin CRUD methods =====

    @Override
    public Map<String, Object> adminListTeachers(String keyword) {
        LambdaQueryWrapper<User> uw = new LambdaQueryWrapper<>();
        uw.apply("(role_id & 11) != 0");
        if (keyword != null && !keyword.trim().isEmpty())
            uw.and(w -> w.like(User::getRealName, keyword).or().like(User::getUsername, keyword));
        List<User> users = userMapper.selectList(uw);
        List<Long> userIds = users.stream().map(User::getId).toList();
        Map<Long, Teacher> tMap = new HashMap<>();
        if (!userIds.isEmpty())
            teacherMapper.selectList(new LambdaQueryWrapper<Teacher>().in(Teacher::getUserId, userIds))
                .forEach(t -> tMap.put(t.getUserId(), t));
        Set<Long> headUids = new HashSet<>();
        Map<Long, Long> headCids = new HashMap<>();
        if (!userIds.isEmpty())
            classesMapper.selectList(new LambdaQueryWrapper<Classes>().in(Classes::getHeadTeacherId, userIds))
                .forEach(c -> { headUids.add(c.getHeadTeacherId()); headCids.put(c.getHeadTeacherId(), c.getId()); });
        List<Map<String, Object>> records = new ArrayList<>();
        for (User u : users) {
            Map<String, Object> item = new HashMap<>();
            item.put("userId", u.getId()); item.put("username", u.getUsername());
            item.put("realName", u.getRealName()); item.put("roleId", u.getRoleId()); item.put("status", u.getStatus());
            Teacher t = tMap.get(u.getId());
            if (t != null) { item.put("id", t.getId()); item.put("teacherNumber", t.getTeacherNumber()); item.put("gender", t.getGender()); item.put("subject", t.getSubject()); }
            item.put("isHeadTeacher", headUids.contains(u.getId())); item.put("headClassId", headCids.get(u.getId()));
            records.add(item);
        }
        // 批量加载任教班级，彻底消除 N+1
        if (!tMap.isEmpty()) {
            Map<Long, List<Map<String, Object>>> assignMap = batchLoadAssignments(
                tMap.values().stream().map(Teacher::getId).toList());
            for (Map<String, Object> item : records) {
                Teacher t = tMap.get(item.get("userId"));
                if (t != null) {
                    List<Map<String, Object>> tcs = assignMap.getOrDefault(t.getId(), List.of());
                    item.put("teachingClasses", tcs);
                    // 提取任教年级
                    Set<String> grades = new LinkedHashSet<>();
                    for (Map<String, Object> tc : tcs) {
                        String g = (String) tc.get("grade");
                        if (g != null && !g.isEmpty()) grades.add(g);
                    }
                    item.put("teachingGrades", new ArrayList<>(grades));
                    // 提取所有任教科目
                    Set<String> subjects = new LinkedHashSet<>();
                    for (Map<String, Object> tc : tcs) {
                        String s = (String) tc.get("subject");
                        if (s != null && !s.isEmpty()) subjects.add(s);
                    }
                    if (t.getSubject() != null && !t.getSubject().isEmpty()) subjects.add(t.getSubject());
                    item.put("subjects", new ArrayList<>(subjects));
                }
            }
        }
        return Map.of("records", records, "total", (long) records.size());
    }

    private Map<Long, List<Map<String, Object>>> batchLoadAssignments(List<Long> teacherIds) {
        if (teacherIds.isEmpty()) return Map.of();
        List<TeacherClass> tcList = teacherClassMapper.selectList(
            new LambdaQueryWrapper<TeacherClass>().in(TeacherClass::getTeacherId, teacherIds));
        if (tcList.isEmpty()) return Map.of();
        Set<Long> cids = tcList.stream().map(TeacherClass::getClassId).collect(Collectors.toSet());
        Map<Long, Classes> cmap = classesMapper.selectBatchIds(cids).stream()
            .collect(Collectors.toMap(Classes::getId, c -> c));
        Map<Long, List<Map<String, Object>>> result = new HashMap<>();
        for (TeacherClass tc : tcList) {
            Classes c = cmap.get(tc.getClassId());
            result.computeIfAbsent(tc.getTeacherId(), k -> new ArrayList<>()).add(Map.of(
                "id", tc.getId(), "classId", tc.getClassId(), "subject", tc.getSubject() != null ? tc.getSubject() : "",
                "className", c != null ? c.getClassName() : "", "grade", c != null && c.getGrade() != null ? c.getGrade() : ""));
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> adminListSimpleTeachers() {
        LambdaQueryWrapper<User> uw = new LambdaQueryWrapper<>();
        uw.apply("(role_id & 11) != 0");
        List<User> users = userMapper.selectList(uw);
        return users.stream().map(u -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", u.getId());
            item.put("name", u.getRealName());
            item.put("username", u.getUsername());
            return item;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> adminGetTeacher(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return null;
        Teacher teacher = teacherMapper.selectOne(new LambdaQueryWrapper<Teacher>().eq(Teacher::getUserId, userId));
        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId()); result.put("username", user.getUsername());
        result.put("realName", user.getRealName()); result.put("roleId", user.getRoleId()); result.put("status", user.getStatus());
        if (teacher != null) { result.put("id", teacher.getId()); result.put("teacherNumber", teacher.getTeacherNumber()); result.put("gender", teacher.getGender()); result.put("subject", teacher.getSubject()); }
        return result;
    }

    @Override @Transactional
    public Map<String, Object> adminCreateTeacher(Map<String, Object> body) {
        String username = (String) body.get("username"); String realName = (String) body.get("realName");
        String password = (String) body.get("password"); String teacherNumber = (String) body.get("teacherNumber");
        Integer gender = (Integer) body.get("gender"); String subject = (String) body.get("subject");
        int roleId = body.get("roleId") != null ? Integer.parseInt(body.get("roleId").toString()) : 2;
        // 仅允许教师(2)角色位，禁止创建含SUPER_ADMIN(8)/INSPECTOR(16)/REGION_ADMIN(64)的组合账号
        if ((roleId & ~2) != 0) throw new BusinessException(400, "教师创建仅允许教师角色，不可包含管理员/巡视员/区域管理员权限");
        if (username == null || username.trim().isEmpty()) throw new BusinessException(400, "请输入用户名");
        boolean hasTeacherRole = (roleId & 2) != 0;
        if (hasTeacherRole && (teacherNumber == null || teacherNumber.trim().isEmpty())) throw new BusinessException(400, "请输入工号");
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username)) > 0)
            throw new BusinessException(400, "用户名已存在");
        if (hasTeacherRole && teacherNumber != null && !teacherNumber.trim().isEmpty()
            && teacherMapper.selectCount(new LambdaQueryWrapper<Teacher>().eq(Teacher::getTeacherNumber, teacherNumber)) > 0)
            throw new BusinessException(400, "工号已存在");
        User user = new User();
        String generatedPwd = com.school.teaching.utils.PasswordUtils.generateRandomPassword();
        user.setUsername(username); user.setPassword(com.school.teaching.utils.PasswordUtils.encode(password != null ? password : generatedPwd));
        user.setRealName(realName); user.setRoleId((long) roleId); user.setRoleName(roleIdToRoleName(roleId));
        user.setStatus(1);
        user.setMustChangePassword(1);
        userMapper.insert(user);
        Map<String, Object> result = new HashMap<>(); result.put("userId", user.getId());
        if (password == null || password.trim().isEmpty()) result.put("defaultPassword", generatedPwd);
        if (hasTeacherRole) {
            Teacher teacher = new Teacher();
            teacher.setUserId(user.getId()); teacher.setTeacherNumber(teacherNumber); teacher.setGender(gender); teacher.setSubject(subject);
            teacherMapper.insert(teacher); result.put("id", teacher.getId());
        }
        return result;
    }

    @Override @Transactional
    public void adminUpdateTeacher(Long userId, Map<String, Object> body) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");
        String realName = (String) body.get("realName"); String password = (String) body.get("password");
        Integer status = body.get("status") != null ? Integer.parseInt(body.get("status").toString()) : null;
        Integer roleId = body.get("roleId") != null ? Integer.parseInt(body.get("roleId").toString()) : null;
        if (roleId != null && (roleId & ~2) != 0) throw new BusinessException(400, "教师角色仅允许教师权限，不可包含管理员/巡视员/区域管理员权限");
        if (realName != null) user.setRealName(realName);
        if (status != null) user.setStatus(status);
        if (roleId != null) { long er = user.getRoleId() != null ? user.getRoleId() : 0; if ((er & 8) != 0) roleId |= 8; user.setRoleId((long) roleId); user.setRoleName(roleIdToRoleName(roleId)); }
        if (password != null && !password.trim().isEmpty()) user.setPassword(com.school.teaching.utils.PasswordUtils.encode(password));
        userMapper.updateById(user);
        Teacher teacher = teacherMapper.selectOne(new LambdaQueryWrapper<Teacher>().eq(Teacher::getUserId, userId));
        String newTn = (String) body.get("teacherNumber");
        // 工号重复检查（排除自身）
        if (newTn != null && !newTn.trim().isEmpty()) {
            LambdaQueryWrapper<Teacher> dupW = new LambdaQueryWrapper<Teacher>()
                .eq(Teacher::getTeacherNumber, newTn);
            if (teacher != null) dupW.ne(Teacher::getId, teacher.getId());
            if (teacherMapper.selectCount(dupW) > 0)
                throw new BusinessException(400, "工号已存在");
        }
        if (teacher == null) {
            teacher = new Teacher();
            teacher.setUserId(userId);
            teacher.setTeacherNumber((String) body.getOrDefault("teacherNumber", ""));
            teacher.setSubject((String) body.getOrDefault("subject", ""));
            teacher.setGender(body.get("gender") != null ? Integer.parseInt(body.get("gender").toString()) : 0);
            teacherMapper.insert(teacher);
        }
        String tn = (String) body.get("teacherNumber"); Integer g = (Integer) body.get("gender"); String s = (String) body.get("subject");
        if (tn != null) teacher.setTeacherNumber(tn); if (g != null) teacher.setGender(g); if (s != null) teacher.setSubject(s);
        teacherMapper.updateById(teacher);
    }

    @Override @Transactional
    public void adminDeleteTeacher(Long userId) {
        // 级联清理任教班级关联
        teacherClassMapper.delete(new LambdaQueryWrapper<TeacherClass>().eq(TeacherClass::getTeacherId, userId));
        // 清除班主任引用
        classesMapper.update(null, new LambdaUpdateWrapper<Classes>()
            .eq(Classes::getHeadTeacherId, userId).set(Classes::getHeadTeacherId, null));
        // 清除快捷评语
        teacherQuickCommentMapper.delete(new LambdaQueryWrapper<TeacherQuickComment>().eq(TeacherQuickComment::getTeacherId, userId));
        teacherMapper.delete(new LambdaQueryWrapper<Teacher>().eq(Teacher::getUserId, userId));
        userMapper.deleteById(userId);
    }

    @Override @Transactional
    public void adminSetHeadClass(Long userId, Long classId) {
        if (classId != null) {
            Classes target = classesMapper.selectById(classId);
            if (target != null && target.getHeadTeacherId() != null && !target.getHeadTeacherId().equals(userId)) {
                target.setHeadTeacherId(null); classesMapper.updateById(target);
            }
        }
        classesMapper.update(null, new LambdaUpdateWrapper<Classes>()
            .eq(Classes::getHeadTeacherId, userId)
            .set(Classes::getHeadTeacherId, null));
        if (classId != null) {
            Classes target = classesMapper.selectById(classId);
            if (target != null) { target.setHeadTeacherId(userId); classesMapper.updateById(target); }
        }
    }

    /**
     * 位掩码 role_id → 字符串 role_name 映射。
     * 与 AuthController.getRoleName 保持一致。
     */
    private static String roleIdToRoleName(int roleId) {
        if ((roleId & 8) != 0) return "SUPER_ADMIN";
        if ((roleId & 64) != 0) return "REGION_ADMIN";
        if ((roleId & 1) != 0) return "ADMIN";
        if ((roleId & 16) != 0) return "INSPECTOR";
        if ((roleId & 2) != 0) return "TEACHER";
        if ((roleId & 32) != 0) return "PARENT";
        return "STUDENT";
    }

    private static final String[] DEFAULT_COMMENTS = {
        "完成得很好，继续保持！", "格式需要规范，注意排版",
        "思路正确，但细节还需完善", "请认真审题，按要求完成",
        "作业已阅", "优秀，值得表扬！"
    };

    @Override
    public List<TeacherQuickComment> getQuickComments(Long teacherId) {
        List<TeacherQuickComment> list = teacherQuickCommentMapper.selectList(
            new LambdaQueryWrapper<TeacherQuickComment>()
                .eq(TeacherQuickComment::getTeacherId, teacherId)
                .orderByAsc(TeacherQuickComment::getSortOrder));
        // 首次使用：自动填充种子评语
        if (list.isEmpty()) {
            for (int i = 0; i < DEFAULT_COMMENTS.length; i++) {
                TeacherQuickComment c = new TeacherQuickComment();
                c.setTeacherId(teacherId);
                c.setCommentText(DEFAULT_COMMENTS[i]);
                c.setSortOrder(i);
                teacherQuickCommentMapper.insert(c);
                list.add(c);
            }
        }
        return list;
    }

    @Override
    public TeacherQuickComment addQuickComment(Long teacherId, String commentText) {
        TeacherQuickComment c = new TeacherQuickComment();
        c.setTeacherId(teacherId);
        c.setCommentText(commentText);
        c.setSortOrder(0);
        teacherQuickCommentMapper.insert(c);
        return c;
    }

    @Override
    public void deleteQuickComment(Long commentId, Long teacherId) {
        TeacherQuickComment c = teacherQuickCommentMapper.selectById(commentId);
        if (c == null || !c.getTeacherId().equals(teacherId)) {
            throw new BusinessException(403, "无权删除");
        }
        teacherQuickCommentMapper.deleteById(commentId);
    }

    @Override
    public boolean isTeacherOfClass(Long teacherId, Long classId) {
        return teacherClassMapper.selectCount(
            new LambdaQueryWrapper<TeacherClass>()
                .eq(TeacherClass::getTeacherId, teacherId)
                .eq(TeacherClass::getClassId, classId)) > 0;
    }

    @Override
    public boolean isUserTeacherOfClass(Long userId, Long classId) {
        Long teacherId = getTeacherIdByUserId(userId);
        return teacherId != null && isTeacherOfClass(teacherId, classId);
    }

    @Override
    public List<Teacher> getTeachersByIds(java.util.Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return teacherMapper.selectBatchIds(ids);
    }
}

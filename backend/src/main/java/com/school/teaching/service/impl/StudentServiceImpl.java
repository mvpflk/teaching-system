package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.NotificationService;
import com.school.teaching.service.StudentService;
import com.school.teaching.utils.PasswordUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StudentServiceImpl implements StudentService {

    @Autowired private StudentMapper studentMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private ClassesMapper classesMapper;
    @Autowired private SignRecordMapper signRecordMapper;
    @Autowired private CreditTransactionMapper creditTxnMapper;
    @Autowired private StudentClassHistoryMapper classHistoryMapper;
    @Autowired private StudentTimelineMapper timelineMapper;
    @Autowired private StudentGroupMemberMapper groupMemberMapper;
    @Autowired private ParentChildRelationMapper parentChildRelationMapper;
    @Autowired private NotificationService notificationService;

    @Override
    public Map<String, Object> adminListStudents(Long classId, String grade, String keyword, int page, int pageSize) {
        return adminListStudents(classId, grade, keyword, page, pageSize, null);
    }

    @Override
    public Map<String, Object> adminListStudents(Long classId, String grade, String keyword, int page, int pageSize, List<Long> accessibleClassIds) {
        LambdaQueryWrapper<Student> w = new LambdaQueryWrapper<>();
        if (classId != null) w.eq(Student::getClassId, classId);
        if (grade != null && !grade.isEmpty()) {
            List<Long> cids = classesMapper.selectList(new LambdaQueryWrapper<Classes>().eq(Classes::getGrade, grade))
                .stream().map(Classes::getId).toList();
            if (cids.isEmpty()) return Map.of("records", List.of(), "total", 0L);
            w.in(Student::getClassId, cids);
        }
        // 非管理员教师任教范围过滤
        if (accessibleClassIds != null && !accessibleClassIds.isEmpty()) {
            w.in(Student::getClassId, accessibleClassIds);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            List<Long> uids = userMapper.selectList(new LambdaQueryWrapper<User>()
                .like(User::getRealName, keyword).or().like(User::getUsername, keyword))
                .stream().map(User::getId).toList();
            if (uids.isEmpty()) return Map.of("records", List.of(), "total", 0L);
            w.in(Student::getUserId, uids);
        }
        long total = studentMapper.selectCount(w);
        int pg = page > 0 ? page : 1, ps = Math.min(Math.max(pageSize, 1), 1000);
        w.last("LIMIT " + ((pg - 1) * ps) + "," + ps);
        List<Student> students = studentMapper.selectList(w);

        Set<Long> uids = students.stream().map(Student::getUserId).collect(Collectors.toSet());
        Set<Long> cids = students.stream().map(Student::getClassId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> uMap = uids.isEmpty() ? Map.of() : userMapper.selectBatchIds(uids).stream().collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, Classes> cMap = cids.isEmpty() ? Map.of() : classesMapper.selectBatchIds(cids).stream().collect(Collectors.toMap(Classes::getId, c -> c));

        List<Map<String, Object>> records = new ArrayList<>();
        for (Student s : students) {
            User u = uMap.get(s.getUserId());
            Classes c = cMap.get(s.getClassId());
            Map<String, Object> item = new HashMap<>();
            item.put("id", s.getId()); item.put("userId", s.getUserId()); item.put("studentNumber", s.getStudentNumber());
            item.put("realName", u != null ? u.getRealName() : "?"); item.put("username", u != null ? u.getUsername() : "?");
            item.put("gender", s.getGender()); item.put("status", s.getStatus()); item.put("classId", s.getClassId());
            item.put("className", c != null ? c.getClassName() : null); item.put("grade", c != null ? c.getGrade() : null);
            item.put("major", c != null ? c.getMajor() : null);
            item.put("currentType", s.getCurrentType()); item.put("enrollmentType", s.getEnrollmentType());
            item.put("totalCredits", s.getTotalCredits());
            records.add(item);
        }
        return Map.of("records", records, "total", total, "page", pg, "pageSize", ps);
    }

    @Override
    public Student getStudentById(Long id) {
        return studentMapper.selectById(id);
    }

    @Override
    public Map<String, Object> adminGetStudent(Long studentId) {
        Student s = studentMapper.selectById(studentId);
        if (s == null) return null;
        User u = userMapper.selectById(s.getUserId());
        Classes c = s.getClassId() != null ? classesMapper.selectById(s.getClassId()) : null;
        Map<String, Object> item = new HashMap<>();
        item.put("id", s.getId()); item.put("userId", s.getUserId()); item.put("studentNumber", s.getStudentNumber());
        item.put("realName", u != null ? u.getRealName() : "?"); item.put("username", u != null ? u.getUsername() : "?");
        item.put("gender", s.getGender()); item.put("status", s.getStatus()); item.put("classId", s.getClassId());
        item.put("className", c != null ? c.getClassName() : null); item.put("grade", c != null ? c.getGrade() : null);
        item.put("major", c != null ? c.getMajor() : null);
        item.put("totalCredits", s.getTotalCredits());
        item.put("birthday", s.getBirthday()); item.put("enrollmentYear", s.getEnrollmentYear());
        item.put("currentType", s.getCurrentType()); item.put("enrollmentType", s.getEnrollmentType());
        item.put("phone", u != null ? u.getPhone() : null); item.put("email", u != null ? u.getEmail() : null);
        return item;
    }

    @Override @Transactional
    public Map<String, Object> adminCreateStudent(Map<String, Object> body) {
        String username = (String) body.getOrDefault("username", body.get("studentNumber"));
        String studentNumber = (String) body.get("studentNumber");
        String realName = (String) body.getOrDefault("realName", studentNumber);
        if (studentNumber == null || studentNumber.isEmpty()) throw new BusinessException(400, "学号必填");
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username)) > 0)
            throw new BusinessException(400, "用户名已存在");
        if (studentMapper.selectCount(new LambdaQueryWrapper<Student>().eq(Student::getStudentNumber, studentNumber)) > 0)
            throw new BusinessException(400, "学号已存在");

        String randomPwd = PasswordUtils.generateRandomPassword();
        // 如果表单传了密码则使用表单密码，否则使用随机密码
        String passwordInput = (String) body.get("password");
        String finalPwd = (passwordInput != null && !passwordInput.isBlank()) ? passwordInput : randomPwd;
        User user = new User();
        user.setUsername(username); user.setPassword(PasswordUtils.encode(finalPwd));
        user.setRealName(realName); user.setRoleId(4L); user.setRoleName("STUDENT"); user.setStatus(1);
        user.setMustChangePassword(1);
        user.setCreateTime(LocalDateTime.now());
        // 手机号、邮箱
        if (body.get("phone") != null) user.setPhone(body.get("phone").toString());
        if (body.get("email") != null) user.setEmail(body.get("email").toString());
        userMapper.insert(user);

        Student student = new Student();
        student.setUserId(user.getId()); student.setStudentNumber(studentNumber);
        student.setGender((Integer) body.get("gender"));
        student.setClassId(body.get("classId") != null ? Long.valueOf(body.get("classId").toString()) : null);
        // 出生日期
        if (body.get("birthday") != null && !body.get("birthday").toString().isBlank()) {
            try { student.setBirthday(java.time.LocalDate.parse(body.get("birthday").toString())); } catch (Exception ignored) { log.warn("生日解析失败: {}", body.get("birthday")); }
        }
        // 入学年份
        if (body.get("enrollmentYear") != null) {
            try { student.setEnrollmentYear(Integer.parseInt(body.get("enrollmentYear").toString())); } catch (Exception ignored) { log.warn("入学年解析失败: {}", body.get("enrollmentYear")); }
        }
        // 自动设置入学类型和当前类型（从班级读取）
        if (student.getClassId() != null) {
            Classes cls = classesMapper.selectById(student.getClassId());
            if (cls != null && cls.getClassType() != null) {
                student.setEnrollmentType(cls.getClassType());
                student.setCurrentType(cls.getClassType());
            }
        }
        student.setTotalCredits(0); student.setTitleLevel(1); student.setCurrentStreak(0);
        if (body.get("status") != null) student.setStatus(body.get("status").toString());
        else student.setStatus("active");
        studentMapper.insert(student);

        notificationService.notify(user.getId(), "welcome", "欢迎加入", "欢迎 " + realName + " 加入教学系统！", student.getId());
        if (student.getClassId() != null) updateClassStudentCount(student.getClassId());

        Map<String, Object> result = new HashMap<>();
        result.put("id", student.getId()); result.put("userId", user.getId());
        result.put("username", username); result.put("realName", realName); result.put("studentNumber", studentNumber);
        result.put("defaultPassword", passwordInput != null && !passwordInput.isBlank() ? finalPwd : randomPwd);
        return result;
    }

    @Override @Transactional
    public Map<String, Object> adminUpdateStudent(Long studentId, Map<String, Object> body) {
        Student s = studentMapper.selectById(studentId);
        if (s == null) throw new BusinessException(404, "学生不存在");
        Long oldClassId = s.getClassId();

        User u = userMapper.selectById(s.getUserId());
        if (u == null) throw new BusinessException(404, "用户不存在");

        if (body.get("realName") != null) u.setRealName((String) body.get("realName"));
        if (body.get("username") != null) u.setUsername((String) body.get("username"));
        if (body.get("password") != null && !body.get("password").toString().isBlank()) {
            u.setPassword(PasswordUtils.encode(body.get("password").toString()));
        }
        if (body.get("phone") != null) u.setPhone(body.get("phone").toString());
        if (body.get("email") != null) u.setEmail(body.get("email").toString());
        // User.status 仅接受 0/1 整数
        if (body.get("userStatus") != null) {
            int st = Integer.parseInt(body.get("userStatus").toString());
            u.setStatus(st == 1 ? 1 : 0);
        }
        userMapper.updateById(u);

        if (body.get("gender") != null) s.setGender((Integer) body.get("gender"));
        if (body.get("studentNumber") != null) s.setStudentNumber((String) body.get("studentNumber"));
        // 出生日期
        if (body.get("birthday") != null && !body.get("birthday").toString().isBlank()) {
            try { s.setBirthday(java.time.LocalDate.parse(body.get("birthday").toString())); } catch (Exception ignored) { log.warn("生日解析失败: {}", body.get("birthday")); }
        }
        // 入学年份
        if (body.get("enrollmentYear") != null) {
            try { s.setEnrollmentYear(Integer.parseInt(body.get("enrollmentYear").toString())); } catch (Exception ignored) { log.warn("入学年解析失败: {}", body.get("enrollmentYear")); }
        }
        // Student.status 接受 "active"/"leave"/"withdraw" 等字符串
        String studentStatus = (String) body.get("studentStatus");
        if (studentStatus != null && !studentStatus.isBlank()) {
            if (!java.util.Set.of("active", "leave", "withdraw", "graduated", "suspended").contains(studentStatus))
                return Map.of("error", "无效的学生状态: " + studentStatus);
            s.setStatus(studentStatus);
        }
        if (body.containsKey("classId")) {
            Long newClassId = body.get("classId") != null ? Long.valueOf(body.get("classId").toString()) : null;
            s.setClassId(newClassId);
            // 班级变更时自动更新 current_type
            if (newClassId != null) {
                Classes cls = classesMapper.selectById(newClassId);
                if (cls != null && cls.getClassType() != null) s.setCurrentType(cls.getClassType());
            }
        }
        studentMapper.updateById(s);

        if (oldClassId != null) updateClassStudentCount(oldClassId);
        if (s.getClassId() != null && !s.getClassId().equals(oldClassId)) {
            updateClassStudentCount(s.getClassId());
            notificationService.notify(u.getId(), "class_changed", "班级变更", "你已被重新分配班级", s.getClassId());
        }

        return adminGetStudent(studentId);
    }

    @Override @Transactional
    public void adminUpdateStudentStatus(Long studentId, String status, Long operatorUserId) {
        Student s = studentMapper.selectById(studentId);
        if (s == null) throw new BusinessException(404, "学生不存在");

        // 非管理员需校验是本班班主任
        if (!SecurityUtils.isAdmin()) {
            Classes cls = classesMapper.selectById(s.getClassId());
            if (cls == null || !operatorUserId.equals(cls.getHeadTeacherId())) {
                throw new BusinessException(403, "仅班主任可变更本班学生状态");
            }
        }

        List<String> validStatuses = List.of("active", "leave", "withdraw", "transfer", "retain");
        if (!validStatuses.contains(status)) throw new BusinessException(400, "无效的状态值");

        s.setStatus(status);
        studentMapper.updateById(s);
    }

    @Override @Transactional
    public void adminDeleteStudent(Long studentId) {
        Student s = studentMapper.selectById(studentId);
        if (s == null) throw new BusinessException(404, "学生不存在");
        Long classId = s.getClassId();
        Long userId = s.getUserId();
        // 级联清理关联数据
        signRecordMapper.delete(new LambdaQueryWrapper<SignRecord>().eq(SignRecord::getStudentId, studentId));
        creditTxnMapper.delete(new LambdaQueryWrapper<CreditTransaction>().eq(CreditTransaction::getStudentId, studentId));
        classHistoryMapper.delete(new LambdaQueryWrapper<StudentClassHistory>().eq(StudentClassHistory::getStudentId, studentId));
        timelineMapper.delete(new LambdaQueryWrapper<StudentTimeline>().eq(StudentTimeline::getStudentId, studentId));
        groupMemberMapper.delete(new LambdaQueryWrapper<StudentGroupMember>().eq(StudentGroupMember::getStudentId, studentId));
        if (studentId != null) parentChildRelationMapper.delete(new LambdaQueryWrapper<ParentChildRelation>().eq(ParentChildRelation::getStudentId, studentId));
        userMapper.deleteById(userId);
        studentMapper.deleteById(studentId);
        if (classId != null) updateClassStudentCount(classId);
    }

    @Override @Transactional
    public void adminBatchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        List<Student> students = studentMapper.selectBatchIds(ids);
        if (students.isEmpty()) return;
        Set<Long> userIds = students.stream().map(Student::getUserId).collect(Collectors.toSet());
        Set<Long> classIds = students.stream().map(Student::getClassId).filter(Objects::nonNull).collect(Collectors.toSet());

        signRecordMapper.delete(new LambdaQueryWrapper<SignRecord>().in(SignRecord::getStudentId, ids));
        creditTxnMapper.delete(new LambdaQueryWrapper<CreditTransaction>().in(CreditTransaction::getStudentId, ids));
        classHistoryMapper.delete(new LambdaQueryWrapper<StudentClassHistory>().in(StudentClassHistory::getStudentId, ids));
        timelineMapper.delete(new LambdaQueryWrapper<StudentTimeline>().in(StudentTimeline::getStudentId, ids));
        groupMemberMapper.delete(new LambdaQueryWrapper<StudentGroupMember>().in(StudentGroupMember::getStudentId, ids));
        parentChildRelationMapper.delete(new LambdaQueryWrapper<ParentChildRelation>().in(ParentChildRelation::getStudentId, ids));
        userMapper.deleteBatchIds(userIds);
        studentMapper.deleteBatchIds(ids);
        for (Long cid : classIds) updateClassStudentCount(cid);
    }

    @Override @Transactional
    public Map<String, Object> adminBatchGraduate(String scope, Long targetId) {
        LambdaQueryWrapper<Student> w = new LambdaQueryWrapper<>();
        switch (scope != null ? scope : "all") {
            case "class" -> { if (targetId != null) w.eq(Student::getClassId, targetId); }
            case "grade" -> {
                if (targetId != null) {
                    List<Long> cids = classesMapper.selectList(new LambdaQueryWrapper<Classes>().eq(Classes::getGrade, String.valueOf(targetId)))
                        .stream().map(Classes::getId).toList();
                    if (cids.isEmpty()) return Map.of("count", 0);
                    w.in(Student::getClassId, cids);
                }
            }
        }
        List<Student> students = studentMapper.selectList(w);
        if (!students.isEmpty()) {
            List<Long> studentIds = students.stream().map(Student::getId).toList();
            Set<Long> uids = students.stream().map(Student::getUserId).collect(Collectors.toSet());
            studentMapper.update(null, new LambdaUpdateWrapper<Student>()
                .in(Student::getId, studentIds)
                .set(Student::getStatus, "graduated")
                .set(Student::getClassId, null));
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                .in(User::getId, uids)
                .set(User::getStatus, 0));
        }
        return Map.of("count", students.size());
    }

    @Override
    public byte[] downloadTemplate() {
        try (java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream()) {
            org.apache.poi.ss.usermodel.Workbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
            // Sheet1: 导入模板
            org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet("学生导入模板");
            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            String[] cols = {"学号", "姓名", "用户名", "性别", "出生日期", "年级", "班级", "手机号", "邮箱"};
            for (int i = 0; i < cols.length; i++) header.createCell(i).setCellValue(cols[i]);
            org.apache.poi.ss.usermodel.Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("2024001");
            sample.createCell(1).setCellValue("张三");
            sample.createCell(2).setCellValue("zhangsan");
            sample.createCell(3).setCellValue("男");
            sample.createCell(4).setCellValue("2006-05-15");
            sample.createCell(5).setCellValue("2024级");
            sample.createCell(6).setCellValue("计算机1班");
            sample.createCell(7).setCellValue("13800138000");
            sample.createCell(8).setCellValue("zhangsan@school.edu");
            // Sheet2: 系统已有班级参考
            org.apache.poi.ss.usermodel.Sheet ref = wb.createSheet("系统班级对照表");
            org.apache.poi.ss.usermodel.Row refHdr = ref.createRow(0);
            refHdr.createCell(0).setCellValue("年级");
            refHdr.createCell(1).setCellValue("班级名称");
            refHdr.createCell(2).setCellValue("班级编号");
            refHdr.createCell(3).setCellValue("专业");
            refHdr.createCell(4).setCellValue("类型");
            List<Classes> allClasses = classesMapper.selectList(null);
            int r = 1;
            for (Classes c : allClasses) {
                org.apache.poi.ss.usermodel.Row refRow = ref.createRow(r++);
                refRow.createCell(0).setCellValue(c.getGrade() != null ? c.getGrade() : "");
                refRow.createCell(1).setCellValue(c.getClassName());
                refRow.createCell(2).setCellValue(c.getClassCode() != null ? c.getClassCode() : "");
                refRow.createCell(3).setCellValue(c.getMajor() != null ? c.getMajor() : "");
                refRow.createCell(4).setCellValue(c.getClassType() != null ? c.getClassType() : "");
            }
            // Sheet3: 填写说明
            org.apache.poi.ss.usermodel.Sheet guide = wb.createSheet("填写说明");
            org.apache.poi.ss.usermodel.Row g = guide.createRow(0);
            g.createCell(0).setCellValue("填写说明");
            String[] tips = {
                "1. 学号为必填项，不可与系统中已有学号重复",
                "2. 班级名称必须与「系统班级对照表」中的名称完全一致（不要简写）",
                "3. 年级填写格式如：2024级、2025级，需与系统一致",
                "4. 出生日期格式：YYYY-MM-DD（如 2006-05-15）",
                "5. 用户名留空则默认使用学号",
                "6. 姓名留空则默认使用学号",
                "7. 密码留空则自动生成随机8位密码，首次登录需修改",
                "8. 手机号、邮箱为选填"
            };
            for (int i = 0; i < tips.length; i++) guide.createRow(i + 1).createCell(0).setCellValue(tips[i]);
            wb.write(bos);
            wb.close();
            return bos.toByteArray();
        } catch (Exception e) { return new byte[0]; }
    }

    @Override @Transactional
    public Map<String, Object> batchImport(MultipartFile file) {
        return null; // Delegated to StudentImportService
    }

    @Override
    public Student getStudentByUserId(Long userId) {
        return studentMapper.selectOne(
            new LambdaQueryWrapper<Student>().eq(Student::getUserId, userId));
    }

    @Override
    public Student getById(Long id) {
        return studentMapper.selectById(id);
    }

    @Override
    public boolean isUserInClass(Long userId, Long classId) {
        return studentMapper.selectCount(
            new LambdaQueryWrapper<Student>()
                .eq(Student::getUserId, userId)
                .eq(Student::getClassId, classId)) > 0;
    }

    @Override
    public long countStudentsByClassId(Long classId) {
        return studentMapper.selectCount(
            new LambdaQueryWrapper<Student>().eq(Student::getClassId, classId));
    }

    private void updateClassStudentCount(Long classId) {
        long cnt = studentMapper.selectCount(new LambdaQueryWrapper<Student>().eq(Student::getClassId, classId));
        classesMapper.update(null, new LambdaUpdateWrapper<Classes>().eq(Classes::getId, classId).set(Classes::getStudentCount, (int) cnt));
    }
}

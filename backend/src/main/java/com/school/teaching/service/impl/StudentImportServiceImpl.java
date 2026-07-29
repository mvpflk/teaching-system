package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.service.NotificationService;
import com.school.teaching.service.StudentImportService;
import com.school.teaching.utils.PasswordUtils;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class StudentImportServiceImpl implements StudentImportService {

    @Autowired private ClassesMapper classesMapper;
    @Autowired private StudentMapper studentMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private NotificationService notificationService;

    @Override
    @Transactional
    public Map<String, Object> batchImport(MultipartFile file) {
        Map<String, Long> classMap = buildClassMap();

        List<String> errors = new ArrayList<>();
        List<String> debugLog = new ArrayList<>();
        int successCount = 0, skipCount = 0, classMatchedCount = 0;
        List<Map<String, String>> rows = new ArrayList<>();

        try (InputStream is = file.getInputStream(); Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() < 2) throw new BusinessException(400, "Excel 为空，至少需要标题行+1行数据");

            Map<String, Integer> colMap = parseHeader(sheet.getRow(0), debugLog);

            String numCol = colMap.containsKey("学号") ? "学号" : colMap.containsKey("学号*") ? "学号*" : colMap.containsKey("studentNumber") ? "studentNumber" : null;
            if (numCol == null) throw new BusinessException(400, "Excel 缺少「学号」列（必须）");

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                ImportResult ir = processRow(row, r, colMap, numCol, classMap, errors);
                if (ir == null) { skipCount++; continue; }
                if (ir.classMatched) classMatchedCount++;
                if (ir.skipped) { skipCount++; continue; }

                rows.add(ir.rowData);
                successCount++;
            }
        } catch (BusinessException e) { throw e;
        } catch (Exception e) {
            throw new BusinessException(400, "解析失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", successCount); result.put("skip", skipCount);
        result.put("classMatched", classMatchedCount); result.put("rows", rows);
        result.put("errors", errors); result.put("debug", debugLog);
        return result;
    }

    private Map<String, Long> buildClassMap() {
        Map<String, Long> classMap = new HashMap<>();
        for (Classes c : classesMapper.selectList(null)) {
            classMap.put(c.getClassName().trim(), c.getId());
            String fullKey = (c.getGrade() != null ? c.getGrade().trim() : "") + c.getClassName().trim();
            classMap.put(fullKey, c.getId());
            if (c.getClassCode() != null) classMap.put(c.getClassCode().trim(), c.getId());
        }
        return classMap;
    }

    private Map<String, Integer> parseHeader(Row headerRow, List<String> debugLog) {
        Map<String, Integer> colMap = new HashMap<>();
        StringBuilder headerDebug = new StringBuilder("检测到的列: ");
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            String h = cellValue(headerRow.getCell(i)).trim();
            if (!h.isEmpty()) {
                colMap.put(h, i);
                if (headerDebug.length() > 0) headerDebug.append(" | ");
                headerDebug.append("[").append(h).append("]");
            }
        }
        debugLog.add(headerDebug.toString());
        return colMap;
    }

    /** 处理单行，返回 null 表示空行跳过，skipped=true 表示已知原因跳过 */
    private ImportResult processRow(Row row, int rowNum, Map<String, Integer> colMap,
                                     String numCol, Map<String, Long> classMap, List<String> errors) {
        String studentNumber = cellValue(row.getCell(colMap.get(numCol))).trim();
        if (studentNumber.isEmpty()) return null;

        if (studentMapper.selectCount(new LambdaQueryWrapper<Student>().eq(Student::getStudentNumber, studentNumber)) > 0) {
            errors.add("第" + (rowNum + 1) + "行: 学号 " + studentNumber + " 已存在，跳过");
            return ImportResult.SKIPPED;
        }

        String username = studentNumber;
        if (colMap.containsKey("用户名")) {
            String u = cellValue(row.getCell(colMap.get("用户名"))).trim();
            if (!u.isEmpty()) username = u;
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username)) > 0) {
            errors.add("第" + (rowNum + 1) + "行: 用户名 " + username + " 已存在，跳过");
            return ImportResult.SKIPPED;
        }

        String nameCol = colMap.containsKey("姓名") ? "姓名" : colMap.containsKey("realName") ? "realName" : null;
        String realName = nameCol != null ? cellValue(row.getCell(colMap.get(nameCol))).trim() : studentNumber;
        if (realName.isEmpty()) realName = studentNumber;

        Integer gender = parseGender(colMap, row);
        ClassMatchResult cm = matchClass(colMap, row, classMap);
        if (cm.notFound) {
            errors.add("第" + (rowNum + 1) + "行: " + cm.debugInfo + " — 系统中未找到匹配的班级，该学生未分配班级（可稍后手动分配）");
        }

        User user = createUser(username, realName, colMap, row);
        Student student = createStudent(user.getId(), studentNumber, gender, cm.classId, colMap, row);
        notificationService.notify(user.getId(), "welcome",
            "欢迎加入", "欢迎 " + realName + " 加入教学系统！你的学号是 " + studentNumber, student.getId());
        if (cm.classId != null) updateClassStudentCount(cm.classId);

        Map<String, String> rowMap = new HashMap<>();
        rowMap.put("username", username); rowMap.put("realName", realName); rowMap.put("studentNumber", studentNumber);
        ImportResult ir = new ImportResult();
        ir.rowData = rowMap;
        ir.classMatched = cm.classId != null;
        return ir;
    }

    private Integer parseGender(Map<String, Integer> colMap, Row row) {
        if (!colMap.containsKey("性别")) return null;
        String gStr = cellValue(row.getCell(colMap.get("性别"))).trim();
        if ("男".equals(gStr) || "1".equals(gStr)) return 1;
        if ("女".equals(gStr) || "2".equals(gStr)) return 2;
        return null;
    }

    private ClassMatchResult matchClass(Map<String, Integer> colMap, Row row, Map<String, Long> classMap) {
        ClassMatchResult result = new ClassMatchResult();
        if (!colMap.containsKey("班级")) return result;

        String className = cellValue(row.getCell(colMap.get("班级"))).trim();
        if (className.isEmpty()) return result;

        String gradeFromExcel = null;
        if (colMap.containsKey("年级")) gradeFromExcel = cellValue(row.getCell(colMap.get("年级"))).trim();

        List<String> keysToTry = new ArrayList<>();
        if (gradeFromExcel != null && !gradeFromExcel.isEmpty()) {
            keysToTry.add(gradeFromExcel + className);
            String gradeNoSuffix = gradeFromExcel.replace("级", "").replace("年", "").trim();
            if (!gradeNoSuffix.equals(gradeFromExcel)) keysToTry.add(gradeNoSuffix + className);
            keysToTry.add(className);
        } else {
            keysToTry.add(className);
        }
        for (String key : keysToTry) {
            result.classId = classMap.get(key);
            if (result.classId != null) return result;
        }
        result.notFound = true;
        result.debugInfo = gradeFromExcel != null && !gradeFromExcel.isEmpty()
            ? "年级「" + gradeFromExcel + "」+ 班级「" + className + "」"
            : "班级「" + className + "」";
        return result;
    }

    private User createUser(String username, String realName,
                            Map<String, Integer> colMap, Row row) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(PasswordUtils.encode(PasswordUtils.generateRandomPassword()));
        user.setRealName(realName);
        user.setRoleId(4L);
        user.setRoleName("STUDENT");
        user.setStatus(1);
        user.setMustChangePassword(1);
        user.setCreateTime(LocalDateTime.now());
        // 手机号
        if (colMap.containsKey("手机号")) {
            String phone = cellValue(row.getCell(colMap.get("手机号"))).trim();
            if (!phone.isEmpty()) user.setPhone(phone);
        }
        // 邮箱
        if (colMap.containsKey("邮箱")) {
            String email = cellValue(row.getCell(colMap.get("邮箱"))).trim();
            if (!email.isEmpty()) user.setEmail(email);
        }
        userMapper.insert(user);
        return user;
    }

    private Student createStudent(Long userId, String studentNumber, Integer gender, Long classId,
                                   Map<String, Integer> colMap, Row row) {
        Student student = new Student();
        student.setUserId(userId);
        student.setStudentNumber(studentNumber);
        student.setGender(gender);
        student.setClassId(classId);
        student.setTotalCredits(0);
        student.setTitleLevel(1);
        student.setCurrentStreak(0);
        student.setStatus("active");
        // 出生日期
        if (colMap.containsKey("出生日期")) {
            String bd = cellValue(row.getCell(colMap.get("出生日期"))).trim();
            if (!bd.isEmpty()) {
                try { student.setBirthday(java.time.LocalDate.parse(bd)); }
                catch (Exception ignored) { /* 格式不对则跳过 */ }
            }
        }
        // 入学年份：从年级自动推导（如"2024级"→2024）
        if (colMap.containsKey("年级")) {
            String grade = cellValue(row.getCell(colMap.get("年级"))).trim();
            if (!grade.isEmpty()) {
                try { student.setEnrollmentYear(Integer.parseInt(grade.replaceAll("[^0-9]", ""))); }
                catch (Exception ignored) { /* */ }
            }
        }
        if (classId != null) {
            Classes cls = classesMapper.selectById(classId);
            if (cls != null && cls.getClassType() != null) {
                student.setEnrollmentType(cls.getClassType());
                student.setCurrentType(cls.getClassType());
            }
        }
        studentMapper.insert(student);
        return student;
    }

    private void updateClassStudentCount(Long classId) {
        long cnt = studentMapper.selectCount(new LambdaQueryWrapper<Student>().eq(Student::getClassId, classId));
        classesMapper.update(null, new LambdaUpdateWrapper<Classes>().eq(Classes::getId, classId).set(Classes::getStudentCount, (int) cnt));
    }

    // ── helper types ──

    private static class ImportResult {
        static final ImportResult SKIPPED = new ImportResult();
        boolean skipped, classMatched;
        Map<String, String> rowData;
        static { SKIPPED.skipped = true; }
    }

    private static class ClassMatchResult {
        Long classId;
        boolean notFound;
        String debugInfo;
    }

    private String cellValue(Cell cell) {
        if (cell == null) return "";
        CellType type = cell.getCellType();
        if (type == CellType.STRING) return cell.getStringCellValue();
        if (type == CellType.NUMERIC) {
            double val = cell.getNumericCellValue();
            if (val == Math.floor(val) && !Double.isInfinite(val)) return String.valueOf((long) val);
            return String.valueOf(val);
        }
        if (type == CellType.BOOLEAN) return String.valueOf(cell.getBooleanCellValue());
        return "";
    }
}

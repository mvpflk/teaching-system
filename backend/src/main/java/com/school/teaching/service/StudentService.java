package com.school.teaching.service;

import com.school.teaching.entity.Student;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

public interface StudentService {
    Map<String, Object> adminListStudents(Long classId, String grade, String keyword, int page, int pageSize);
    Map<String, Object> adminListStudents(Long classId, String grade, String keyword, int page, int pageSize, java.util.List<Long> accessibleClassIds);
    Map<String, Object> adminGetStudent(Long studentId);
    Map<String, Object> adminCreateStudent(Map<String, Object> body);
    Map<String, Object> adminUpdateStudent(Long studentId, Map<String, Object> body);
    /** 变更学生状态（班主任/管理员），非管理员需校验是本班学生 */
    void adminUpdateStudentStatus(Long studentId, String status, Long operatorUserId);
    void adminDeleteStudent(Long studentId);
    void adminBatchDelete(List<Long> ids);
    Map<String, Object> adminBatchGraduate(String scope, Long targetId);
    byte[] downloadTemplate();
    Map<String, Object> batchImport(MultipartFile file);

    /** 根据 userId 查询学生记录（唯一），无记录返回 null */
    Student getStudentByUserId(Long userId);

    /** 根据主键 ID 查询学生记录 */
    Student getStudentById(Long id);

    /** 根据主键 ID 查询学生记录 */
    Student getById(Long id);

    /** 判断用户是否属于指定班级 */
    boolean isUserInClass(Long userId, Long classId);

    /** 统计班级学生总数 */
    long countStudentsByClassId(Long classId);
}

package com.school.teaching.service;

import com.school.teaching.entity.Classes;
import com.school.teaching.entity.Student;
import com.school.teaching.entity.User;
import java.util.List;
import java.util.Map;

public interface ClassService {

    List<Classes> getClassList();

    Classes getClassById(Long id);

    Classes createClass(Classes classes);

    Classes updateClass(Classes classes);

    void deleteClass(Long id);

    /** 获取班级的学生列表（含用户名/学号） */
    List<Map<String, Object>> getStudents(Long classId);

    /** 添加学生到班级 */
    void addStudent(Long classId, Long studentId);

    /** 从班级移除学生 */
    void removeStudent(Long classId, Long studentId);

    /** 获取可添加的学生列表（尚未分配班级的学生） */
    List<Map<String, Object>> getAvailableStudents();

    /** Admin: 班级列表（含学生数+班主任名，批量加载） */
    Map<String, Object> adminListClasses();

    /** 教师：返回自己任教的班级列表 */
    Map<String, Object> adminListClassesByTeacher(Long userId);

    /** 学生调班/转学段 */
    Map<String, Object> changeStudentClass(Long studentId, Long newClassId, String reason, Long approvedBy);

    /** 批量更新班级类型（同步学生 current_type + 历史记录） */
    int batchUpdateClassType(List<Long> classIds, String classType);

    /** 获取班级的专业名称 */
    String getClassMajor(Long classId);

    /** 判断用户是否是该班级的任教教师 */
    boolean isTeacherOfClass(Long userId, Long classId);

    /** 判断用户是否是该班级的班主任 */
    boolean isHeadTeacherOfClass(Long userId, Long classId);
}

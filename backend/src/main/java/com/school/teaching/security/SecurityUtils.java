package com.school.teaching.security;

import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private static final Set<String> ADMIN_SET = Set.of("ADMIN", "SUPER_ADMIN");
    private static final Set<String> TEACHER_SET = Set.of("TEACHER", "HEAD_TEACHER", "ADMIN", "SUPER_ADMIN");
    private static final Set<String> INSPECTOR_SET = Set.of("INSPECTOR", "ADMIN", "SUPER_ADMIN");
    private static final Set<String> REGION_ADMIN_SET = Set.of("REGION_ADMIN", "ADMIN", "SUPER_ADMIN");

    public static CustomUserDetails getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        if (auth.getPrincipal() instanceof CustomUserDetails details) return details;
        return null;
    }

    public static Long getCurrentUserId() {
        CustomUserDetails u = getCurrentUser(); return u != null ? u.getUserId() : null;
    }
    public static String getCurrentUsername() {
        CustomUserDetails u = getCurrentUser(); return u != null ? u.getUsername() : null;
    }
    public static String getCurrentRole() {
        CustomUserDetails u = getCurrentUser(); return u != null ? u.getRole() : null;
    }
    public static boolean isAdmin()         { return ADMIN_SET.contains(getCurrentRole()); }
    public static boolean isSuperAdmin()    { return "SUPER_ADMIN".equals(getCurrentRole()); }
    public static boolean isTeacherOrAdmin(){ return TEACHER_SET.contains(getCurrentRole()); }
    public static boolean isInspector()     { return INSPECTOR_SET.contains(getCurrentRole()); }
    public static boolean isStudent()       { return "STUDENT".equals(getCurrentRole()); }
    public static boolean isParent()        { return "PARENT".equals(getCurrentRole()); }
    public static boolean isRegionAdmin()   { return "REGION_ADMIN".equals(getCurrentRole()); }
    public static boolean isRegionAdminOrAbove() { return REGION_ADMIN_SET.contains(getCurrentRole()); }
    public static boolean isParentOrRegionAdmin() { String r = getCurrentRole(); return "PARENT".equals(r) || "REGION_ADMIN".equals(r); }
    public static boolean isPureTeacher()   { return "TEACHER".equals(getCurrentRole()); }
    public static boolean isHeadTeacher()   { return "HEAD_TEACHER".equals(getCurrentRole()); }
    public static boolean isTeacherOrHeadTeacher() { String r = getCurrentRole(); return "HEAD_TEACHER".equals(r) || "TEACHER".equals(r); }

    private static com.school.teaching.service.TeacherService _teacherService;
    public static void setTeacherService(com.school.teaching.service.TeacherService ts) { _teacherService = ts; }
    public static com.school.teaching.service.TeacherService teacherService() { return _teacherService; }
    private static com.school.teaching.mapper.GroupMemberMapper _groupMemberMapper;
    public static void setGroupMemberMapper(com.school.teaching.mapper.GroupMemberMapper m) { _groupMemberMapper = m; }
    public static com.school.teaching.mapper.GroupMemberMapper groupMemberMapper() { return _groupMemberMapper; }

    public static boolean isAdminByRole(String role)  { return ADMIN_SET.contains(role); }
    public static boolean isTeacherByRole(String role) { return TEACHER_SET.contains(role); }

    public static Long getCurrentSchoolId() {
        Long ctx = com.school.teaching.common.SchoolContext.get();
        if (ctx != null) return ctx;
        CustomUserDetails u = getCurrentUser(); return u != null ? u.getSchoolId() : null;
    }
    public static Long getCurrentStageId()  { CustomUserDetails u = getCurrentUser(); return u != null ? u.getStageId() : null; }

    public static boolean isTeachingGroupLeader() {
        if (!isTeacherOrAdmin()) return false;
        Long userId = getCurrentUserId();
        if (userId == null) return false;
        Long teacherId = _teacherService != null ? _teacherService.getTeacherIdByUserId(userId) : null;
        if (teacherId == null) return false;
        if (_groupMemberMapper == null) return false;
        return _groupMemberMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.school.teaching.entity.GroupMember>()
                .eq(com.school.teaching.entity.GroupMember::getGroupType, "TEACHING")
                .eq(com.school.teaching.entity.GroupMember::getTeacherId, teacherId)
                .eq(com.school.teaching.entity.GroupMember::getRole, "LEADER")) > 0;
    }

    private SecurityUtils() {}
}

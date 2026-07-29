package com.school.teaching.common;

import com.school.teaching.entity.User;

/**
 * 统一权限评估器 — 基于位掩码角色体系
 *
 * 角色位定义：
 *   bit 0 (1)   ADMIN        管理员
 *   bit 1 (2)   TEACHER      教师
 *   bit 2 (4)   STUDENT      学生
 *   bit 3 (8)   SUPER_ADMIN  超级管理员
 *   bit 4 (16)  INSPECTOR    教务巡视
 *
 * 组合示例：roleId=9  → SUPER_ADMIN + ADMIN
 *           roleId=3  → ADMIN + TEACHER
 *           roleId=25 → SUPER_ADMIN + ADMIN + INSPECTOR
 *
 * 使用示例：
 *   if (!PermissionEvaluator.isAdmin(user)) throw new AccessDeniedException();
 *   if (!PermissionEvaluator.canManageExams(user)) return R.error(403, "无权限");
 */
public final class PermissionEvaluator {

    // ==================== 角色位常量 ====================
    public static final int ROLE_ADMIN       = 1;  // bit 0
    public static final int ROLE_TEACHER     = 2;  // bit 1
    public static final int ROLE_STUDENT     = 4;  // bit 2
    public static final int ROLE_SUPER_ADMIN = 8;  // bit 3
    public static final int ROLE_INSPECTOR   = 16; // bit 4
    public static final int ROLE_REGION_ADMIN = 64; // bit 6
    public static final int ROLE_PARENT      = 32; // bit 5

    // ==================== 组合位掩码 ====================
    /** 管理员（含超级管理员）：bit 0 + bit 3 = 9 */
    public static final int MASK_ADMIN       = ROLE_ADMIN | ROLE_SUPER_ADMIN;       // 9

    /** 教师（含管理员+超级管理员）：bit 1 + bit 0 + bit 3 = 11 */
    public static final int MASK_TEACHER     = ROLE_TEACHER | ROLE_ADMIN | ROLE_SUPER_ADMIN; // 11

    /** 教职工（可查看教学数据）：bit 0+1+3+4+6 = 91 */
    public static final int MASK_STAFF       = ROLE_ADMIN | ROLE_TEACHER | ROLE_SUPER_ADMIN | ROLE_INSPECTOR | ROLE_REGION_ADMIN; // 91

    /** 巡视员（含管理员）：bit 4 + bit 0 + bit 3 = 25 */
    public static final int MASK_INSPECTOR   = ROLE_INSPECTOR | ROLE_ADMIN | ROLE_SUPER_ADMIN; // 25

    /** 区域管理员：bit 6 */
    public static final int MASK_REGION_ADMIN = ROLE_REGION_ADMIN; // 64

    /** 家长：bit 5 */
    public static final int MASK_PARENT = ROLE_PARENT; // 32

    // ==================== 基础角色判断 ====================

    /** 是否超级管理员 */
    public static boolean isSuperAdmin(User user) {
        return hasRole(user, ROLE_SUPER_ADMIN);
    }

    /** 是否管理员（含超级管理员） */
    public static boolean isAdmin(User user) {
        return hasAny(user, MASK_ADMIN);
    }

    /** 是否教师（含班主任/管理员/超级管理员） */
    public static boolean isTeacher(User user) {
        return hasAny(user, MASK_TEACHER);
    }

    /** 是否纯教师（不含管理员位） */
    public static boolean isPureTeacher(User user) {
        return hasRole(user, ROLE_TEACHER) && !hasRole(user, ROLE_ADMIN) && !hasRole(user, ROLE_SUPER_ADMIN);
    }

    /** 是否学生 */
    public static boolean isStudent(User user) {
        return hasRole(user, ROLE_STUDENT);
    }

    /** 是否教务巡视（含管理员/超级管理员） */
    public static boolean isInspector(User user) {
        return hasAny(user, MASK_INSPECTOR);
    }

    /** 是否区域管理员 */
    public static boolean isRegionAdmin(User user) {
        return hasRole(user, ROLE_REGION_ADMIN);
    }

    /** 是否家长 */
    public static boolean isParent(User user) {
        return hasRole(user, ROLE_PARENT);
    }

    // ==================== 组合权限判断 ====================

    /** 是否教职工（可查看教学数据、班级、学生信息） */
    public static boolean isStaff(User user) {
        return hasAny(user, MASK_STAFF);
    }

    /** 是否有权管理试卷（创建/编辑/删除） */
    public static boolean canManageExams(User user) {
        return hasAny(user, MASK_TEACHER); // 教师+管理员+超级管理员
    }

    /** 是否有权管理题库（编辑分类/批量清空仅管理员） */
    public static boolean canManageQuestionBank(User user) {
        return hasAny(user, MASK_TEACHER);
    }

    /** 是否有权管理班级（增删改班级仅管理员） */
    public static boolean canManageClasses(User user) {
        return hasAny(user, MASK_ADMIN);
    }

    /** 是否有权管理教师/学生账号（仅管理员） */
    public static boolean canManageUsers(User user) {
        return hasAny(user, MASK_ADMIN);
    }

    /** 是否有权修改系统设置（仅超级管理员） */
    public static boolean canManageSettings(User user) {
        return hasRole(user, ROLE_SUPER_ADMIN);
    }

    /** 是否有权查看积分管理后台（仅管理员+巡视） */
    public static boolean canAccessCreditAdmin(User user) {
        return hasAny(user, MASK_ADMIN);
    }

    /** 是否有权管理BBS（置顶/加精/禁言） */
    public static boolean canModerateBbs(User user) {
        return hasAny(user, MASK_TEACHER);
    }

    // ==================== 底层位运算 ====================

    /**
     * 检查用户是否拥有指定角色位（精确匹配）
     * @param user 用户实体，可为 null
     * @param roleBit 角色位值（1/2/4/8/16）
     * @return 是否拥有该角色
     */
    public static boolean hasRole(User user, int roleBit) {
        if (user == null || user.getRoleId() == null) return false;
        return (user.getRoleId() & roleBit) != 0;
    }

    /**
     * 检查用户是否拥有掩码中的任意角色位
     * @param user 用户实体，可为 null
     * @param mask 组合位掩码
     * @return 是否拥有掩码中至少一个角色
     */
    public static boolean hasAny(User user, int mask) {
        if (user == null || user.getRoleId() == null) return false;
        return (user.getRoleId() & mask) != 0;
    }

    /**
     * 检查用户是否拥有掩码中的全部角色位
     * @param user 用户实体，可为 null
     * @param mask 组合位掩码
     * @return 是否拥有掩码中所有角色
     */
    public static boolean hasAll(User user, int mask) {
        if (user == null || user.getRoleId() == null) return false;
        return (user.getRoleId() & mask) == mask;
    }

    // ==================== 便捷方法（接收 Long roleId） ====================

    public static boolean isAdmin(Long roleId) {
        return roleId != null && (roleId & MASK_ADMIN) != 0;
    }

    public static boolean isTeacherOrAdmin(Long roleId) {
        return roleId != null && (roleId & MASK_TEACHER) != 0;
    }

    public static boolean isStaff(Long roleId) {
        return roleId != null && (roleId & MASK_STAFF) != 0;
    }

    public static boolean hasRole(Long roleId, int roleBit) {
        return roleId != null && (roleId & roleBit) != 0;
    }

    // ==================== JWT 角色字符串判断 ====================

    /** 从 JWT role 字符串判断是否管理员及以上 */
    public static boolean isAdminRole(String jwtRole) {
        return "SUPER_ADMIN".equals(jwtRole) || "ADMIN".equals(jwtRole);
    }

    /** 从 JWT role 字符串判断是否教师及以上 */
    public static boolean isTeacherRole(String jwtRole) {
        return "SUPER_ADMIN".equals(jwtRole) || "ADMIN".equals(jwtRole)
            || "TEACHER".equals(jwtRole) || "HEAD_TEACHER".equals(jwtRole);
    }

    /** 从 JWT role 字符串判断是否教职工（含巡视、区域管理员） */
    public static boolean isStaffRole(String jwtRole) {
        return isTeacherRole(jwtRole) || "INSPECTOR".equals(jwtRole) || "REGION_ADMIN".equals(jwtRole);
    }

    /** 从 JWT role 字符串判断是否巡视员及以上 */
    public static boolean isInspectorRole(String jwtRole) {
        return "SUPER_ADMIN".equals(jwtRole) || "ADMIN".equals(jwtRole) || "INSPECTOR".equals(jwtRole);
    }

    private PermissionEvaluator() {
        // 工具类，禁止实例化
    }
}

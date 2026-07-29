package com.school.teaching.common;

public class SchoolContext {
    private static final ThreadLocal<Long> CONTEXT = new ThreadLocal<>();

    public static void set(Long schoolId) { CONTEXT.set(schoolId); }
    public static Long get() { return CONTEXT.get(); }
    public static void clear() { CONTEXT.remove(); }
}

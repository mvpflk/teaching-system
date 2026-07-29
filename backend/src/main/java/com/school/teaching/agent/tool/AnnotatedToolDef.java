package com.school.teaching.agent.tool;

import com.school.teaching.agent.tool.annotation.ToolCategory;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 注解扫描到的工具定义，在 ToolRegistry 中转换为 RegisteredTool。
 */
public class AnnotatedToolDef {
    public String name;
    public String description;
    public ToolCategory category;
    public Set<String> allowedRoles;
    public Map<String, Object> parameters;
    public List<String> requiredParams;
    public Method method;
    public Object bean;
    public String beanName;
}

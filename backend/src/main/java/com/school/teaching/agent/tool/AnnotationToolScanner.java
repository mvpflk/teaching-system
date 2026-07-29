package com.school.teaching.agent.tool;

import com.school.teaching.agent.tool.annotation.AgentTool;
import com.school.teaching.agent.tool.annotation.ToolParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * 注解驱动工具扫描器 — 扫描所有 Spring Bean 中 @AgentTool 标注的方法，
 * 自动提取工具元数据（名称、描述、分类、角色、参数），供 ToolRegistry 注册。
 * 与手动 register() 共存：已有工具保持手动注册，新增工具可通过注解声明。
 */
@Slf4j
@Component
public class AnnotationToolScanner {

    private final ApplicationContext context;

    public AnnotationToolScanner(ApplicationContext context) {
        this.context = context;
    }

    /**
     * 扫描所有 Spring Bean，返回 @AgentTool 标注的工具定义列表。
     */
    public List<AnnotatedToolDef> scan() {
        List<AnnotatedToolDef> results = new ArrayList<>();
        String[] beanNames = context.getBeanDefinitionNames();

        for (String beanName : beanNames) {
            Object bean;
            try {
                bean = context.getBean(beanName);
            } catch (Exception e) {
                continue; // 跳过无法获取的 bean（如 scoped proxy）
            }

            Class<?> clazz = bean.getClass();
            // 处理 CGLIB 代理，获取原始类
            if (clazz.getName().contains("$$")) {
                clazz = clazz.getSuperclass();
            }

            for (Method method : clazz.getDeclaredMethods()) {
                AgentTool annotation = method.getAnnotation(AgentTool.class);
                if (annotation == null) continue;

                AnnotatedToolDef def = new AnnotatedToolDef();
                def.name = annotation.name();
                def.description = annotation.description();
                def.category = annotation.category();
                def.allowedRoles = Set.of(annotation.allowedRoles());
                def.method = method;
                def.bean = bean;
                def.beanName = beanName;

                // 提取 @ToolParam 参数元数据
                def.parameters = extractParams(method);
                def.requiredParams = extractRequiredParams(method);

                log.info("AnnotationToolScanner: 发现 @AgentTool name={}, bean={}.{}",
                        def.name, beanName, method.getName());
                results.add(def);
            }
        }

        log.info("AnnotationToolScanner: 扫描完成，共发现 {} 个 @AgentTool 工具", results.size());
        return results;
    }

    private Map<String, Object> extractParams(Method method) {
        Map<String, Object> params = new LinkedHashMap<>();
        Parameter[] javaParams = method.getParameters();
        for (Parameter p : javaParams) {
            ToolParam tp = p.getAnnotation(ToolParam.class);
            if (tp == null) continue;
            String type = inferJsonType(p.getType());
            boolean required = tp.required();
            if (required) {
                params.put(p.getName(), Map.of("type", type, "description", tp.description()));
            }
        }
        return params;
    }

    private List<String> extractRequiredParams(Method method) {
        List<String> required = new ArrayList<>();
        for (Parameter p : method.getParameters()) {
            ToolParam tp = p.getAnnotation(ToolParam.class);
            if (tp != null && tp.required()) {
                required.add(p.getName());
            }
        }
        return required;
    }

    private String inferJsonType(Class<?> javaType) {
        if (javaType == String.class) return "string";
        if (javaType == Long.class || javaType == long.class
                || javaType == Integer.class || javaType == int.class) return "number";
        if (javaType == Boolean.class || javaType == boolean.class) return "boolean";
        if (javaType == List.class || javaType.isArray()) return "array";
        if (javaType == Map.class) return "object";
        return "string";
    }
}

package com.school.teaching.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TaskTypeHandler 策略选择器 — 根据 TaskCategory 自动选择对应处理器。
 * 一个 Handler 可注册多个 Category，新增无需修改此处。
 */
@Component
public class TaskTypeHandlerSelector {

    private static final Logger log = LoggerFactory.getLogger(TaskTypeHandlerSelector.class);
    private final Map<TaskCategory, TaskTypeHandler> map;

    @Autowired
    public TaskTypeHandlerSelector(List<TaskTypeHandler> handlerList) {
        map = new HashMap<>();
        for (TaskTypeHandler h : handlerList) {
            for (TaskCategory cat : h.getCategories()) {
                TaskTypeHandler existing = map.put(cat, h);
                if (existing != null && existing.getClass() != h.getClass()) {
                    log.warn("TaskCategory {} 被 {} 和 {} 同时注册，后者覆盖前者",
                        cat, existing.getClass().getSimpleName(), h.getClass().getSimpleName());
                }
            }
        }
    }

    public TaskTypeHandler get(TaskCategory category) {
        return map.get(category); // null if not registered → caller handles gracefully
    }
}

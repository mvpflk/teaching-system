package com.school.teaching.event;

import org.springframework.context.ApplicationEvent;
import java.util.Map;

/**
 * 系统参数变更事件 — 发布后由其他组件监听处理（如刷新JWT配置）
 */
public class ParamChangedEvent extends ApplicationEvent {

    private final Map<String, String> changedParams;

    public ParamChangedEvent(Object source, Map<String, String> changedParams) {
        super(source);
        this.changedParams = changedParams;
    }

    /** 变更的参数 key → newValue */
    public Map<String, String> getChangedParams() {
        return changedParams;
    }
}

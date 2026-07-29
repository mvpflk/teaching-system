package com.school.teaching.agent.tool;

import com.school.teaching.agent.security.UserContext;

import java.util.Map;

@FunctionalInterface
public interface ToolHandler {

    ToolResult execute(Map<String, Object> args, UserContext caller);
}

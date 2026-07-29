package com.school.teaching.agent.tool;

import com.school.teaching.exception.BusinessException;

public class ToolAccessDeniedException extends BusinessException {

    public ToolAccessDeniedException(String message) {
        super(403, message);
    }
}

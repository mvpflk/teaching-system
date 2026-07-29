package com.school.teaching.service;

import java.util.Map;

public interface TaskCheatService {

    /** 记录切屏警告（考试防作弊），syncOnly=true 时仅查询当前状态不递增计数，返回 {cheatWarnings, maxCheatWarnings, terminated} */
    Map<String, Object> recordCheatWarning(Long taskId, Long studentId, String eventType, boolean syncOnly);
}

package com.school.teaching.service;

import java.util.Map;

public interface PaperImportService {

    /** 解析上传的试卷文件，返回识别的题目列表和统计（不入库） */
    Map<String, Object> parse(byte[] fileBytes, String fileName, String title, String subject);

    /** 接收解析结果+赋分+配置，批量创建题目、任务、试卷库 */
    Map<String, Object> create(Map<String, Object> request);
}

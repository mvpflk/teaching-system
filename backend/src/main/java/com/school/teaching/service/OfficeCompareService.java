package com.school.teaching.service;

import java.util.Map;

/**
 * Office 文档比对引擎。
 * 支持 Word (.docx) 文本相似度分析和 Excel (.xlsx) 数据+公式比对。
 */
public interface OfficeCompareService {
    /** 比对两个文档，返回差异报告 */
    Map<String, Object> compare(String templatePath, String studentFilePath, String fileType);
}

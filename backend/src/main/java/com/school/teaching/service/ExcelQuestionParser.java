package com.school.teaching.service;

import com.school.teaching.dto.ExcelQuestionImportRow;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ExcelQuestionParser {
    /** 解析 Excel 文件，返回题目行数据列表 */
    List<ExcelQuestionImportRow> parse(MultipartFile file);
}

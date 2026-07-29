package com.school.teaching.service.impl;

import com.alibaba.excel.EasyExcel;
import com.school.teaching.dto.ExcelQuestionImportRow;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.service.ExcelQuestionParser;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelQuestionParserImpl implements ExcelQuestionParser {

    @SuppressWarnings("unchecked")
    @Override
    public List<ExcelQuestionImportRow> parse(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.endsWith(".xlsx")) {
            throw new IllegalArgumentException("仅支持 .xlsx 格式的 Excel 文件");
        }
        try {
            List<Object> raw = EasyExcel.read(file.getInputStream())
                .head(ExcelQuestionImportRow.class)
                .sheet()
                .doReadSync();
            List<ExcelQuestionImportRow> result = new ArrayList<>();
            if (raw != null) {
                for (Object o : raw) {
                    if (o instanceof ExcelQuestionImportRow row && row.getQuestionText() != null
                        && !row.getQuestionText().trim().isEmpty()) {
                        result.add(row);
                    }
                }
            }
            return result;
        } catch (IOException e) {
            throw new BusinessException(500, "读取 Excel 文件失败: " + e.getMessage());
        }
    }
}

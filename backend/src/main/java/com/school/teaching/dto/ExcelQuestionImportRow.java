package com.school.teaching.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * Excel 导入题目的行数据模型。
 * 模板列顺序：题型, 题干, 选项A, 选项B, 选项C, 选项D, 选项E, 正确答案, 解析, 学科
 */
@Data
public class ExcelQuestionImportRow {

    @ExcelProperty("题型")
    private String questionType;

    @ExcelProperty("题干")
    private String questionText;

    @ExcelProperty("选项A")
    private String optionA;

    @ExcelProperty("选项B")
    private String optionB;

    @ExcelProperty("选项C")
    private String optionC;

    @ExcelProperty("选项D")
    private String optionD;

    @ExcelProperty("选项E")
    private String optionE;

    @ExcelProperty("正确答案")
    private String correctAnswer;

    @ExcelProperty("解析")
    private String explanation;

    @ExcelProperty("学科")
    private String subject;
}

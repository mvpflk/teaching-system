package com.school.teaching.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/** Excel 题库导入模板行数据 */
@Data
public class ExcelTemplateRow {
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

    public ExcelTemplateRow() {}

    public ExcelTemplateRow(String questionType, String questionText, String optionA, String optionB,
                            String optionC, String optionD, String optionE,
                            String correctAnswer, String explanation, String subject) {
        this.questionType = questionType;
        this.questionText = questionText;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.optionE = optionE;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
        this.subject = subject;
    }
}

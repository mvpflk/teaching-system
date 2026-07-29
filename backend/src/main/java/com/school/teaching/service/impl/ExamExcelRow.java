package com.school.teaching.service.impl;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamExcelRow {

    @ExcelProperty("试卷标题")
    private String examTitle;

    @ExcelProperty("科目")
    private String subject;

    @ExcelProperty("题目类型")
    private String questionType;

    @ExcelProperty("题目内容")
    private String questionText;

    @ExcelProperty("选项(用|分隔)")
    private String options;

    @ExcelProperty("正确答案")
    private String correctAnswer;

    @ExcelProperty("分值")
    private Integer score;

    @ExcelProperty("解析")
    private String explanation;
}

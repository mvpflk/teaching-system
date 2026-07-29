package com.school.teaching.vo;

import lombok.Data;

@Data
public class SystemParamVO {
    private Long id;
    private String settingKey;
    private String settingValue;
    private String defaultValue;
    private String valueType;
    private String category;
    private Integer isEditable;
    private String options;
    private String validationRule;
    private String description;
    private Integer orderNum;
}

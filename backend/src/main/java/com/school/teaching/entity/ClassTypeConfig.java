package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("class_type_config")
public class ClassTypeConfig implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long stageId;
    private String typeCode;
    private String typeName;
    private String defaultMajor;
    private String category;
    private Integer sortOrder;
    private Long schoolId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

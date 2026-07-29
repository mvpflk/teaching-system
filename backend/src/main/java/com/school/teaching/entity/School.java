package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("schools")
public class School implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String code;
    private Long regionId;
    private Integer status;
    private LocalDateTime createdAt;
}

package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;

@Data
@TableName("practice_step_files")
public class PracticeStepFile implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long stepId;
    private String fileUrl;
    private String originalName;
    private Long fileSize;
}

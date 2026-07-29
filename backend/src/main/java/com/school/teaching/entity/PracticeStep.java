package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("practice_steps")
public class PracticeStep implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long studentId;
    private Integer stepIndex;
    private String title;
    private String description;
    private String images;
    private String files;
    private Integer version;
    private String attachmentMode;
    private String referenceAttachments;
    private String templateFile;
    private String templateChecksum;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

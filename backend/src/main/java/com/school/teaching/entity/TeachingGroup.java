package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("teaching_group")
public class TeachingGroup implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String subjectIds;
    private Long schoolId;
    private String stageIds;
    private String leaderIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("teaching_research_activities")
public class TeachingResearchActivity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teachingGroupId;
    private String activityType;
    private String title;
    private LocalDate activityDate;
    private Integer participantCount;
    private String summary;
    private Long recordedBy;
    private Long schoolId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

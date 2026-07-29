package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;

@Data
@TableName("task_group_visibility")
public class TaskGroupVisibility implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long taskId;
    private Long groupId;
}

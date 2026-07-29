package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import java.io.Serializable;

@Data
@TableName("bbs_muted_users")
public class BbsMutedUser implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long mutedBy;
    private String reason;
    private LocalDateTime expireTime;
    private LocalDateTime createdAt;
}

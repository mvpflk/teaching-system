package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("class_album")
public class ClassAlbum implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long classId;
    private Long uploaderId;
    private String imageUrl;
    private String caption;
    private Integer likeCount;
    private String status;
    private Long reviewerId;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private Boolean likedByCurrentUser;
}

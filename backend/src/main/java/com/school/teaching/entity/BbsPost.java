package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import java.io.Serializable;

@Data
@TableName("bbs_posts")
public class BbsPost implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long categoryId;
    private String title;
    private String content;
    private String images;
    private Long authorId;
    private Integer isSticky;
    private Integer isHighlighted;
    private String status;
    private Integer viewCount;
    private Integer likeCount;
    private Integer replyCount;
    private Integer isMoralBehavior;
    private Long praisedStudentId;
    private LocalDateTime lastReplyTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import java.io.Serializable;

@Data
@TableName("bbs_replies")
public class BbsReply implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long postId;
    private Long parentId;
    private String content;
    private Long authorId;
    private Integer likeCount;
    private String status;
    private LocalDateTime createTime;
}

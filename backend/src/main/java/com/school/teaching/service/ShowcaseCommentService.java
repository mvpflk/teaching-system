package com.school.teaching.service;

import com.school.teaching.entity.ShowcaseComment;
import java.util.List;

public interface ShowcaseCommentService {

    /** 获取作品已审核评论 */
    List<ShowcaseComment> getApprovedComments(Long workId);

    /** 提交评论 */
    ShowcaseComment addComment(Long workId, Long userId, String content, boolean autoApprove);

    /** 审核通过评论 */
    void approveComment(Long commentId, Long reviewerId);

    /** 拒绝评论 */
    void rejectComment(Long commentId, Long reviewerId);

    /** 获取待审核评论列表 */
    List<ShowcaseComment> getPendingComments();

    /** 根据 ID 获取评论 */
    ShowcaseComment getById(Long commentId);
}

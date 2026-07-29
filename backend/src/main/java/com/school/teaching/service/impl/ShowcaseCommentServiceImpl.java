package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.ShowcaseComment;
import com.school.teaching.mapper.ShowcaseCommentMapper;
import com.school.teaching.service.ShowcaseCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShowcaseCommentServiceImpl implements ShowcaseCommentService {

    @Autowired
    private ShowcaseCommentMapper commentMapper;

    @Override
    public List<ShowcaseComment> getApprovedComments(Long workId) {
        return commentMapper.selectList(
            new LambdaQueryWrapper<ShowcaseComment>()
                .eq(ShowcaseComment::getWorkId, workId)
                .eq(ShowcaseComment::getStatus, "APPROVED")
                .orderByAsc(ShowcaseComment::getCreatedAt));
    }

    @Override
    public ShowcaseComment addComment(Long workId, Long userId, String content, boolean autoApprove) {
        ShowcaseComment comment = new ShowcaseComment();
        comment.setWorkId(workId);
        comment.setUserId(userId);
        comment.setContent(content.trim());
        comment.setStatus(autoApprove ? "APPROVED" : "PENDING");
        commentMapper.insert(comment);
        return comment;
    }

    @Override
    public void approveComment(Long commentId, Long reviewerId) {
        ShowcaseComment c = commentMapper.selectById(commentId);
        if (c == null) throw new com.school.teaching.exception.BusinessException(404, "评论不存在");
        c.setStatus("APPROVED");
        c.setReviewerId(reviewerId);
        c.setReviewTime(LocalDateTime.now());
        commentMapper.updateById(c);
    }

    @Override
    public void rejectComment(Long commentId, Long reviewerId) {
        ShowcaseComment c = commentMapper.selectById(commentId);
        if (c == null) throw new com.school.teaching.exception.BusinessException(404, "评论不存在");
        c.setStatus("REJECTED");
        c.setReviewerId(reviewerId);
        c.setReviewTime(LocalDateTime.now());
        commentMapper.updateById(c);
    }

    @Override
    public List<ShowcaseComment> getPendingComments() {
        return commentMapper.selectList(
            new LambdaQueryWrapper<ShowcaseComment>()
                .eq(ShowcaseComment::getStatus, "PENDING")
                .orderByAsc(ShowcaseComment::getCreatedAt));
    }

    @Override
    public ShowcaseComment getById(Long commentId) {
        return commentMapper.selectById(commentId);
    }
}

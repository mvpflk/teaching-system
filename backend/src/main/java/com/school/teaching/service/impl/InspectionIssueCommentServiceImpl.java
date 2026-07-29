package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.InspectionIssueComment;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.InspectionIssueCommentMapper;
import com.school.teaching.service.InspectionIssueCommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InspectionIssueCommentServiceImpl implements InspectionIssueCommentService {

    private final InspectionIssueCommentMapper mapper;

    @Override
    @Transactional
    public InspectionIssueComment addComment(Long issueId, Long userId, String content, Integer isSystem) {
        if (issueId == null) throw new BusinessException(400, "问题ID不能为空");
        if (userId == null) throw new BusinessException(400, "用户ID不能为空");
        if (content == null || content.isBlank()) throw new BusinessException(400, "评论内容不能为空");
        InspectionIssueComment comment = new InspectionIssueComment();
        comment.setIssueId(issueId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setIsSystem(isSystem != null ? isSystem : 0);
        mapper.insert(comment);
        return comment;
    }

    @Override
    public List<InspectionIssueComment> getCommentsByIssue(Long issueId) {
        return mapper.selectList(new LambdaQueryWrapper<InspectionIssueComment>()
            .eq(InspectionIssueComment::getIssueId, issueId)
            .orderByAsc(InspectionIssueComment::getCreatedAt));
    }
}

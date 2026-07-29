package com.school.teaching.dto;

import com.school.teaching.common.EncodingUtils;
import com.school.teaching.entity.QuestionBank;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Data
public class QuestionBankVO {
    private Long id;
    private String subject;
    private Long categoryId;
    private String questionType;
    private String questionText;
    private String options;
    private String explanation;
    private String attachmentUrl;
    private Integer difficultyLevel;
    private Long createdBy;
    private String creatorName;
    private Integer status;
    private String category;
    private String knowledgeDim;
    private String tier;
    private String source;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @Getter @Setter
    private String correctAnswer;

    public static QuestionBankVO fromEntity(QuestionBank q) {
        QuestionBankVO vo = new QuestionBankVO();
        vo.setId(q.getId());
        vo.setSubject(q.getSubject());
        vo.setCategoryId(q.getCategoryId());
        vo.setQuestionType(q.getQuestionType());
        vo.setQuestionText(EncodingUtils.fix(q.getQuestionText()));
        vo.setOptions(EncodingUtils.fix(q.getOptions()));
        vo.setExplanation(EncodingUtils.fix(q.getExplanation()));
        vo.setAttachmentUrl(q.getAttachmentUrl());
        vo.setDifficultyLevel(q.getDifficultyLevel());
        vo.setCreatedBy(q.getCreatedBy());
        vo.setCreatorName(q.getCreatorName());
        vo.setStatus(q.getStatus());
        vo.setCategory(q.getCategory());
        vo.setKnowledgeDim(q.getKnowledgeDim());
        vo.setTier(q.getTier());
        vo.setSource(q.getSource());
        vo.setVersion(q.getVersion());
        vo.setCreateTime(q.getCreateTime());
        vo.setUpdateTime(q.getUpdateTime());
        return vo;
    }

    public static QuestionBankVO fromEntityForTeacher(QuestionBank q) {
        QuestionBankVO vo = fromEntity(q);
        vo.setCorrectAnswer(q.getCorrectAnswer());
        return vo;
    }
}
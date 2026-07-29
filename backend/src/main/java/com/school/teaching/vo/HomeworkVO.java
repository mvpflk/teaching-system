package com.school.teaching.vo;

import com.school.teaching.entity.HomeworkAssignment;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class HomeworkVO {
    private Long id;
    private String title;
    private String content;
    private String contentType;
    private Long classId;
    private String className;
    private Long teacherId;
    private String teacherName;
    private String subject;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer allowLateSubmit;
    private Integer maxScore;
    private Integer submitCount;
    private Integer gradedCount;
    private LocalDateTime createTime;

    public static HomeworkVO from(HomeworkAssignment h) {
        HomeworkVO vo = new HomeworkVO();
        vo.setId(h.getId());
        vo.setTitle(h.getTitle());
        vo.setContent(h.getContent());
        vo.setContentType(h.getContentType());
        vo.setClassId(h.getClassId());
        vo.setClassName(h.getClassName());
        vo.setTeacherId(h.getTeacherId());
        vo.setSubject(h.getSubject());
        vo.setStartTime(h.getStartTime());
        vo.setEndTime(h.getEndTime());
        vo.setAllowLateSubmit(h.getAllowLateSubmit());
        vo.setMaxScore(h.getMaxScore());
        vo.setCreateTime(h.getCreateTime());
        return vo;
    }
}

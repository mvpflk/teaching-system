package com.school.teaching.vo;

import com.school.teaching.entity.Student;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class StudentVO {
    private Long id;
    private Long userId;
    private String studentNumber;
    private String realName;
    private String className;
    private Integer gender;
    private Integer totalCredits;
    private Integer titleLevel;
    private String titleName;
    private Integer currentStreak;
    private LocalDateTime createTime;

    public static StudentVO from(Student s) {
        StudentVO vo = new StudentVO();
        vo.setId(s.getId());
        vo.setUserId(s.getUserId());
        vo.setStudentNumber(s.getStudentNumber());
        vo.setGender(s.getGender());
        vo.setTotalCredits(s.getTotalCredits());
        vo.setTitleLevel(s.getTitleLevel());
        vo.setCurrentStreak(s.getCurrentStreak());
        vo.setCreateTime(s.getCreateTime());
        return vo;
    }
}

package com.school.teaching.service;

import com.school.teaching.entity.Exam;
import com.school.teaching.entity.HomeworkAssignment;
import com.school.teaching.entity.Task;

public interface ParentNotificationService {
    void notifyParentsForTask(Task task);
    void notifyParentsForExam(Exam exam);
    void notifyParentsForHomework(HomeworkAssignment hw);
}

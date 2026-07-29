package com.school.teaching.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.teaching.common.R;
import com.school.teaching.entity.LessonPrepGroup;
import com.school.teaching.entity.LessonPrepRecord;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.LessonPrepWorkbenchService;
import com.school.teaching.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/teacher/lesson-prep")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','HEAD_TEACHER','ADMIN','SUPER_ADMIN')")
public class LessonPrepWorkbenchController {

    private final LessonPrepWorkbenchService workbenchService;
    private final TeacherService teacherService;

    private Long resolveTeacherId() {
        Long userId = SecurityUtils.getCurrentUserId();
        return teacherService.getTeacherIdByUserId(userId);
    }

    @GetMapping("/my-group")
    public R<?> getMyGroup() {
        Long teacherId = resolveTeacherId();
        if (teacherId == null) return R.ok(null, "非教师用户");
        LessonPrepGroup group = workbenchService.getMyLessonPrepGroup(teacherId);
        if (group == null) return R.ok(null, "您不是任何备课组的组长");
        return R.ok(group);
    }

    @GetMapping("/records")
    public R<IPage<LessonPrepRecord>> getRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Long teacherId = resolveTeacherId();
        if (teacherId == null) return R.ok(null, "非教师用户");
        LessonPrepGroup group = workbenchService.getMyLessonPrepGroup(teacherId);
        if (group == null) return R.ok(null, "您不是任何备课组的组长");
        return R.ok(workbenchService.getRecords(group.getId(), startDate, endDate, page, size));
    }

    @PostMapping("/records")
    public R<?> createRecord(@RequestBody LessonPrepRecord record) {
        Long teacherId = resolveTeacherId();
        if (teacherId == null) return R.ok(null, "非教师用户");
        LessonPrepGroup group = workbenchService.getMyLessonPrepGroup(teacherId);
        if (group == null) return R.ok(null, "您不是任何备课组的组长");
        return R.ok(workbenchService.createRecord(record, teacherId, group.getId()));
    }

    @PutMapping("/records/{id}")
    public R<?> updateRecord(@PathVariable Long id, @RequestBody LessonPrepRecord data) {
        Long teacherId = resolveTeacherId();
        if (teacherId == null) return R.ok(null, "非教师用户");
        return R.ok(workbenchService.updateRecord(id, data, teacherId));
    }

    @DeleteMapping("/records/{id}")
    public R<String> deleteRecord(@PathVariable Long id) {
        Long teacherId = resolveTeacherId();
        if (teacherId == null) return R.ok(null, "非教师用户");
        workbenchService.deleteRecord(id, teacherId);
        return R.ok("已删除");
    }

    @GetMapping("/members")
    public R<?> getMembers() {
        Long teacherId = resolveTeacherId();
        if (teacherId == null) return R.ok(null, "非教师用户");
        LessonPrepGroup group = workbenchService.getMyLessonPrepGroup(teacherId);
        if (group == null) return R.ok(null, "您不是任何备课组的组长");
        return R.ok(workbenchService.getMembers(group.getId()));
    }

    @GetMapping("/pending-reviews")
    public R<?> getPendingReviews() {
        Long teacherId = resolveTeacherId();
        if (teacherId == null) return R.ok(null, "非教师用户");
        LessonPrepGroup group = workbenchService.getMyLessonPrepGroup(teacherId);
        if (group == null) return R.ok(null, "您不是任何备课组的组长");
        return R.ok(workbenchService.getPendingReviews(group.getId()));
    }
}

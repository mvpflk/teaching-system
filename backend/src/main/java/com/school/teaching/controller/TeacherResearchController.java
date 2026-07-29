package com.school.teaching.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.teaching.common.R;
import com.school.teaching.entity.TeachingGroup;
import com.school.teaching.entity.TeachingResearchActivity;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.TeachingGroupService;
import com.school.teaching.service.TeachingResearchActivityService;
import com.school.teaching.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/teacher/research")
@RequiredArgsConstructor
public class TeacherResearchController {

    private final TeachingGroupService teachingGroupService;
    private final TeachingResearchActivityService activityService;
    private final TeacherService teacherService;

    private Long resolveTeacherId() {
        Long userId = SecurityUtils.getCurrentUserId();
        return teacherService.getTeacherIdByUserId(userId);
    }

    /** 获取当前教师所属教研组（作为组长） */
    @GetMapping("/my-group")
    public R<TeachingGroup> getMyGroup() {
        Long teacherId = resolveTeacherId();
        if (teacherId == null) return R.ok(null, "非教师用户");
        List<Long> groupIds = teachingGroupService.getGroupIdsForTeacher(teacherId);
        if (groupIds == null || groupIds.isEmpty()) return R.ok(null, "您不是任何教研组的组长");
        // 返回第一个教研组的完整信息
        Long groupId = groupIds.get(0);
        TeachingGroup group = teachingGroupService.getById(groupId);
        return R.ok(group);
    }

    /** 获取教研活动列表 */
    @GetMapping("/activities")
    public R<IPage<TeachingResearchActivity>> getActivities(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String activityType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Long teacherId = resolveTeacherId();
        if (teacherId == null) return R.ok(null, "非教师用户");
        List<Long> groupIds = teachingGroupService.getGroupIdsForTeacher(teacherId);
        if (groupIds == null || groupIds.isEmpty()) return R.ok(null, "您不是任何教研组的组长");
        Long groupId = groupIds.get(0);
        return R.ok(activityService.getPage(groupId, activityType, startDate, endDate, page, size));
    }

    /** 创建教研活动 */
    @PostMapping("/activities")
    public R<TeachingResearchActivity> createActivity(@RequestBody TeachingResearchActivity data) {
        Long teacherId = resolveTeacherId();
        if (teacherId == null) return R.ok(null, "非教师用户");
        List<Long> groupIds = teachingGroupService.getGroupIdsForTeacher(teacherId);
        if (groupIds == null || groupIds.isEmpty()) return R.error(403, "您不是任何教研组的组长");
        data.setTeachingGroupId(groupIds.get(0));
        data.setRecordedBy(teacherId);
        return R.ok(activityService.create(data), "创建成功");
    }

    /** 更新教研活动 */
    @PutMapping("/activities/{id}")
    public R<TeachingResearchActivity> updateActivity(@PathVariable Long id, @RequestBody TeachingResearchActivity data) {
        Long teacherId = resolveTeacherId();
        if (teacherId == null) return R.ok(null, "非教师用户");
        return R.ok(activityService.update(id, data), "更新成功");
    }

    /** 删除教研活动 */
    @DeleteMapping("/activities/{id}")
    public R<String> deleteActivity(@PathVariable Long id) {
        Long teacherId = resolveTeacherId();
        if (teacherId == null) return R.ok(null, "非教师用户");
        activityService.delete(id);
        return R.ok("已删除");
    }

    /** 获取教研组成员 */
    @GetMapping("/members")
    public R<List<Map<String, Object>>> getMembers() {
        Long teacherId = resolveTeacherId();
        if (teacherId == null) return R.ok(null, "非教师用户");
        List<Long> groupIds = teachingGroupService.getGroupIdsForTeacher(teacherId);
        if (groupIds == null || groupIds.isEmpty()) return R.ok(null, "您不是任何教研组的组长");
        return R.ok(teachingGroupService.getMembers(groupIds.get(0)));
    }

    /** 获取待审阅列表 */
    @GetMapping("/pending-reviews")
    public R<?> getPendingReviews() {
        Long teacherId = resolveTeacherId();
        if (teacherId == null) return R.ok(null, "非教师用户");
        List<Long> groupIds = teachingGroupService.getGroupIdsForTeacher(teacherId);
        if (groupIds == null || groupIds.isEmpty()) return R.ok(null, "您不是任何教研组的组长");
        // 暂返回空列表，后续可扩展
        return R.ok(List.of());
    }
}

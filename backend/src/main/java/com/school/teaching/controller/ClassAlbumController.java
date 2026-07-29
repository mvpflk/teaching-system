package com.school.teaching.controller;

import com.school.teaching.annotation.AuditLog;
import com.school.teaching.annotation.AuditEventType;
import com.school.teaching.common.R;
import com.school.teaching.entity.ClassAlbum;
import com.school.teaching.entity.ClassAlbumComment;
import com.school.teaching.service.ClassAlbumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/class-album")
public class ClassAlbumController {

    @Autowired private ClassAlbumService albumService;

    @PostMapping("/upload")
    @AuditLog(eventType = AuditEventType.OTHER, description = "上传班级相册照片")
    public R<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                          @RequestParam Long classId,
                                          @RequestParam(required = false) String caption) {
        Map<String, Object> result = albumService.upload(file, classId, caption);
        String status = (String) result.get("status");
        return R.ok(result, "APPROVED".equals(status) ? "上传成功" : "上传成功，等待班主任审核");
    }

    @PostMapping("/photo/{id}/review")
    public R<?> review(@PathVariable Long id, @RequestBody Map<String, String> body) {
        albumService.review(id, body.get("action"));
        return R.ok("approve".equals(body.get("action")) ? "已通过" : "已拒绝");
    }

    @GetMapping("/pending")
    public R<List<ClassAlbum>> pending() {
        return R.ok(albumService.getPendingPhotos());
    }

    @GetMapping("/class/{classId}")
    public R<Map<String, Object>> list(@PathVariable Long classId,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "12") int pageSize) {
        return R.ok(albumService.listPhotos(classId, page, pageSize));
    }

    @PostMapping("/photo/{id}/like")
    public R<Map<String, Object>> like(@PathVariable Long id) {
        return R.ok(albumService.like(id), "点赞成功");
    }
    @PostMapping("/photo/like-status")
    public R<Map<Long, Boolean>> likeStatus(@RequestBody Map<String, List<Long>> body) {
        return R.ok(albumService.getUserLikeStatus(body.get("photoIds")));
    }

    @PostMapping("/photo/{id}/comment")
    public R<ClassAlbumComment> comment(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return R.ok(albumService.comment(id, body.get("content")), "评论成功");
    }

    @GetMapping("/photo/{id}/comments")
    public R<List<ClassAlbumComment>> comments(@PathVariable Long id) {
        return R.ok(albumService.getComments(id));
    }

    @DeleteMapping("/photo/{id}")
    public R<?> delete(@PathVariable Long id) {
        albumService.deletePhoto(id);
        return R.ok("已删除");
    }
}

package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.entity.ClassAlbum;
import com.school.teaching.entity.ClassAlbumComment;
import com.school.teaching.entity.ClassAlbumLike;
import com.school.teaching.entity.Student;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.ClassAlbumCommentMapper;
import com.school.teaching.mapper.ClassAlbumLikeMapper;
import com.school.teaching.mapper.ClassAlbumMapper;
import com.school.teaching.mapper.StudentMapper;
import com.school.teaching.mapper.UserMapper;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.ClassAlbumService;
import com.school.teaching.service.StudentTimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ClassAlbumServiceImpl implements ClassAlbumService {

    private final ClassAlbumMapper albumMapper;
    private final ClassAlbumCommentMapper commentMapper;
    private final ClassAlbumLikeMapper likeMapper;
    private final UserMapper userMapper;
    private final StudentMapper studentMapper;
    private final StudentTimelineService studentTimelineService;

    @Value("${teaching.upload-dir:/data/uploads}")
    private String baseUploadDir;

    private static final Set<String> IMAGE_EXTS = Set.of(".jpg", ".jpeg", ".png", ".gif");
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024;
    private static final Map<String, byte[]> MAGIC_BYTES = Map.of(
        ".jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
        ".jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
        ".png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47},
        ".gif", new byte[]{0x47, 0x49, 0x46, 0x38}
    );

    @Override
    public Map<String, Object> upload(MultipartFile file, Long classId, String caption) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) throw new BusinessException(401, "未登录");
        if (file.isEmpty()) throw new BusinessException(400, "请选择图片");
        if (file.getSize() > MAX_FILE_SIZE) throw new BusinessException(400, "文件不能超过20MB");

        String ext = file.getOriginalFilename();
        if (ext != null && ext.contains(".")) ext = ext.substring(ext.lastIndexOf('.')).toLowerCase();
        else ext = ".jpg";
        if (!IMAGE_EXTS.contains(ext)) throw new BusinessException(400, "不支持的文件类型");

        byte[] fileBytes;
        try { fileBytes = file.getBytes(); } catch (IOException e) { throw new BusinessException(400, "文件读取失败"); }

        byte[] expectedMagic = MAGIC_BYTES.get(ext);
        if (expectedMagic != null) {
            if (fileBytes.length < expectedMagic.length) throw new BusinessException(400, "文件损坏或格式不符");
            for (int i = 0; i < expectedMagic.length; i++)
                if (fileBytes[i] != expectedMagic[i]) throw new BusinessException(400, "文件内容与扩展名不匹配");
        }

        boolean isTeacherOrAdmin = SecurityUtils.isTeacherOrAdmin();
        if (studentMapper != null && !isTeacherOrAdmin) {
            Student s = studentMapper.selectOne(new LambdaQueryWrapper<Student>().eq(Student::getUserId, userId));
            if (s == null || !classId.equals(s.getClassId()))
                throw new BusinessException(403, "只能上传到自己班级的相册");
        }

        String status = isTeacherOrAdmin ? "APPROVED" : "PENDING";
        try {
            String filename = "album_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
            File dir = new File(baseUploadDir, "album");
            if (!dir.exists()) Files.createDirectories(dir.toPath());
            Files.write(new File(dir, filename).toPath(), fileBytes);
            String url = "/uploads/album/" + filename;

            ClassAlbum album = new ClassAlbum();
            album.setClassId(classId); album.setUploaderId(userId);
            album.setImageUrl(url); album.setCaption(caption);
            album.setLikeCount(0); album.setStatus(status);
            if (isTeacherOrAdmin) { album.setReviewerId(userId); album.setReviewedAt(LocalDateTime.now()); }
            albumMapper.insert(album);

            Map<String, Object> result = new HashMap<>();
            result.put("id", album.getId()); result.put("url", url); result.put("caption", caption); result.put("status", status);
            return result;
        } catch (IOException e) { throw new BusinessException(500, "保存失败"); }
    }

    @Override
    public ClassAlbum review(Long photoId, String action) {
        if (!SecurityUtils.isTeacherOrAdmin()) throw new BusinessException(403, "仅教师/管理员可审核");
        ClassAlbum album = albumMapper.selectById(photoId);
        if (album == null) throw new BusinessException(404, "照片不存在");
        if (!"approve".equals(action) && !"reject".equals(action))
            throw new BusinessException(400, "操作无效，仅支持 approve/reject");
        album.setStatus("approve".equals(action) ? "APPROVED" : "REJECTED");
        album.setReviewerId(SecurityUtils.getCurrentUserId());
        album.setReviewedAt(LocalDateTime.now());
        albumMapper.updateById(album);
        return album;
    }

    @Override
    public List<ClassAlbum> getPendingPhotos() {
        if (!SecurityUtils.isTeacherOrAdmin()) throw new BusinessException(403, "仅教师/管理员可查看");
        return albumMapper.selectList(new LambdaQueryWrapper<ClassAlbum>().eq(ClassAlbum::getStatus, "PENDING")
            .orderByDesc(ClassAlbum::getCreatedAt));
    }

    @Override
    public Map<String, Object> listPhotos(Long classId, int page, int pageSize) {
        LambdaQueryWrapper<ClassAlbum> qw = new LambdaQueryWrapper<ClassAlbum>().eq(ClassAlbum::getClassId, classId);
        if (!SecurityUtils.isTeacherOrAdmin()) qw.eq(ClassAlbum::getStatus, "APPROVED");
        qw.orderByDesc(ClassAlbum::getCreatedAt);
        Page<ClassAlbum> pg = albumMapper.selectPage(new Page<>(page, pageSize), qw);
        if (!pg.getRecords().isEmpty()) {
            // 附加上传者姓名
            if (userMapper != null) {
                Set<Long> uids = new HashSet<>();
                pg.getRecords().forEach(p -> uids.add(p.getUploaderId()));
                Map<Long, String> nameMap = new HashMap<>();
                userMapper.selectBatchIds(uids).forEach(u -> nameMap.put(u.getId(), u.getRealName()));
                pg.getRecords().forEach(p -> {
                    String cap = p.getCaption();
                    if (cap == null) cap = "";
                    p.setCaption(cap + "|uploader:" + nameMap.getOrDefault(p.getUploaderId(), ""));
                });
            }
            // 附加当前用户点赞状态
            Long currentUserId = SecurityUtils.getCurrentUserId();
            if (currentUserId != null && likeMapper != null) {
                List<Long> photoIds = pg.getRecords().stream().map(ClassAlbum::getId).toList();
                List<ClassAlbumLike> likes = likeMapper.selectList(new LambdaQueryWrapper<ClassAlbumLike>()
                    .eq(ClassAlbumLike::getUserId, currentUserId)
                    .in(ClassAlbumLike::getPhotoId, photoIds));
                Set<Long> likedIds = likes.stream().map(ClassAlbumLike::getPhotoId).collect(java.util.stream.Collectors.toSet());
                pg.getRecords().forEach(p -> p.setLikedByCurrentUser(likedIds.contains(p.getId())));
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("records", pg.getRecords()); data.put("total", pg.getTotal());
        data.put("page", page); data.put("pageSize", pageSize);
        return data;
    }

    @Override
    public Map<String, Object> like(Long photoId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) throw new BusinessException(401, "未登录");

        ClassAlbum album = albumMapper.selectById(photoId);
        if (album == null) throw new BusinessException(404, "照片不存在");
        if (!"APPROVED".equals(album.getStatus())) throw new BusinessException(400, "仅审核通过的照片可点赞");

        // 检查是否已点赞
        ClassAlbumLike existing = likeMapper.selectOne(new LambdaQueryWrapper<ClassAlbumLike>()
            .eq(ClassAlbumLike::getPhotoId, photoId)
            .eq(ClassAlbumLike::getUserId, userId));
        if (existing != null) throw new BusinessException(409, "已经点过赞了");

        // 写入点赞记录
        ClassAlbumLike like = new ClassAlbumLike();
        like.setPhotoId(photoId);
        like.setUserId(userId);
        likeMapper.insert(like);

        // 原子自增 like_count
        albumMapper.update(null,
            new LambdaUpdateWrapper<ClassAlbum>()
                .eq(ClassAlbum::getId, photoId)
                .setSql("like_count = COALESCE(like_count, 0) + 1"));

        int newCount = album.getLikeCount() != null ? album.getLikeCount() + 1 : 1;
        return Map.of("likeCount", newCount, "liked", true);
    }

    @Override
    public Map<Long, Boolean> getUserLikeStatus(List<Long> photoIds) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null || photoIds == null || photoIds.isEmpty()) return Map.of();
        List<ClassAlbumLike> likes = likeMapper.selectList(new LambdaQueryWrapper<ClassAlbumLike>()
            .eq(ClassAlbumLike::getUserId, userId)
            .in(ClassAlbumLike::getPhotoId, photoIds));
        Map<Long, Boolean> result = new HashMap<>();
        likes.forEach(l -> result.put(l.getPhotoId(), true));
        return result;
    }

    @Override
    public ClassAlbumComment comment(Long photoId, String content) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) throw new BusinessException(401, "未登录");
        if (content == null || content.isBlank()) throw new BusinessException(400, "评论不能为空");
        ClassAlbum album = albumMapper.selectById(photoId);
        if (album == null) throw new BusinessException(404, "照片不存在");
        ClassAlbumComment c = new ClassAlbumComment();
        c.setPhotoId(photoId); c.setUserId(userId); c.setContent(content);
        commentMapper.insert(c);
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("@(\\S+)").matcher(content);
            while (m.find()) {
                String name = m.group(1).replaceAll("[，。！？,.!?@]", "").trim();
                if (name.length() >= 2 && name.length() <= 10) {
                    Long sid = studentTimelineService.findStudentIdByName(name);
                    if (sid != null) studentTimelineService.recordEvent(sid, "album_mention",
                        "在班级相册中被提及", null, "/class-album/photo/" + photoId);
                }
            }
        } catch (Exception ignored) { log.warn("学生时间线记录失败: {}", ignored.getMessage()); }
        return c;
    }

    @Override
    public List<ClassAlbumComment> getComments(Long photoId) {
        return commentMapper.selectList(new LambdaQueryWrapper<ClassAlbumComment>()
            .eq(ClassAlbumComment::getPhotoId, photoId).orderByAsc(ClassAlbumComment::getCreatedAt));
    }

    @Override
    public void deletePhoto(Long photoId) {
        if (!SecurityUtils.isTeacherOrAdmin()) throw new BusinessException(403, "仅班主任/管理员可删除");
        ClassAlbum album = albumMapper.selectById(photoId);
        if (album == null) throw new BusinessException(404, "照片不存在");
        try {
            String url = album.getImageUrl();
            if (url != null && url.startsWith("/uploads/")) {
                File f = new File(baseUploadDir, url.substring("/uploads/".length()));
                if (f.exists()) f.delete();
            }
        } catch (Exception ignored) { log.warn("相册文件删除失败: {}", ignored.getMessage()); }
        commentMapper.delete(new LambdaQueryWrapper<ClassAlbumComment>().eq(ClassAlbumComment::getPhotoId, photoId));
        albumMapper.deleteById(photoId);
    }
}

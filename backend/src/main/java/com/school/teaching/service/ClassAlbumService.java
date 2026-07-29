package com.school.teaching.service;

import com.school.teaching.entity.ClassAlbum;
import com.school.teaching.entity.ClassAlbumComment;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface ClassAlbumService {
    Map<String, Object> upload(MultipartFile file, Long classId, String caption);
    ClassAlbum review(Long photoId, String action);
    List<ClassAlbum> getPendingPhotos();
    Map<String, Object> listPhotos(Long classId, int page, int pageSize);
    Map<String, Object> like(Long photoId);
    Map<Long, Boolean> getUserLikeStatus(List<Long> photoIds);
    ClassAlbumComment comment(Long photoId, String content);
    List<ClassAlbumComment> getComments(Long photoId);
    void deletePhoto(Long photoId);
}

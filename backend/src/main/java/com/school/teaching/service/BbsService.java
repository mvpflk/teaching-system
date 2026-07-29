package com.school.teaching.service;

import com.school.teaching.entity.*;
import java.util.List;
import java.util.Map;

public interface BbsService {
    // 版块
    List<BbsCategory> getCategories();

    // 帖子列表 (分页 + 版块筛选 + 排序 + 置顶优先)
    Map<String, Object> getPosts(Long categoryId, String keyword, int page, int pageSize, String sort, Boolean highlightOnly, Long currentUserId);

    // 帖子详情
    Map<String, Object> getPostDetail(Long postId, Long currentUserId);

    // 发帖
    BbsPost createPost(BbsPost post);

    // 编辑帖子
    BbsPost updatePost(BbsPost post);

    // 删帖
    void deletePost(Long postId, Long userId, String role);

    // 置顶/取消置顶 (教师)
    void toggleSticky(Long postId);

    // 使用置顶券置顶 (学生)
    void useStickyCoupon(Long postId, Long userId);

    // 加精/取消加精 (教师)
    void toggleHighlight(Long postId);

    // 增加浏览量
    void incrementViewCount(Long postId);

    // 获取帖子回复（分页）
    Map<String, Object> getReplies(Long postId, Long currentUserId, int page, int pageSize);

    // 回复
    Map<String, Object> createReply(Long postId, Long parentId, String content, Long authorId, String authorName);

    // 删除回复 (教师)
    void deleteReply(Long replyId, Long userId, String role);

    // 点赞切换
    Map<String, Object> toggleLike(Long userId, Long targetId, String targetType);

    // 收藏切换
    Map<String, Object> toggleBookmark(Long userId, Long postId);

    // 我的帖子
    List<Map<String, Object>> getMyPosts(Long userId);

    // 我的收藏
    List<Map<String, Object>> getMyBookmarks(Long userId);

    // 我的回复
    List<Map<String, Object>> getMyReplies(Long userId);

    // 禁言 (教师) — durationDays: null=永久, 1/3/7=期限
    void muteUser(Long userId, Long mutedBy, String reason, Integer durationDays);

    // 解禁 (教师)
    void unmuteUser(Long userId);

    // 检查是否被禁言
    boolean isMuted(Long userId);

    // 德育表扬
    Map<String, Object> createMoralPost(Map<String, Object> body, Long userId, List<Long> teachingClassIds);

    // 全站热帖（按回复数排序）
    List<Map<String, Object>> getHotPosts(int limit, Long currentUserId);

    // 全站活跃用户（近7天发帖+回复数Top）
    List<Map<String, Object>> getActiveUsers(int limit);
}

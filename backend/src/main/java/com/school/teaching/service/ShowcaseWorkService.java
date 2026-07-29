package com.school.teaching.service;

import com.school.teaching.entity.ShowcaseWork;
import java.util.List;
import java.util.Map;

public interface ShowcaseWorkService {

    /** 推荐作品到展示墙（含权限校验） */
    ShowcaseWork recommendWork(Map<String, Object> request);

    /** 更新展示作品（教师只能更新自己推荐的） */
    ShowcaseWork updateWork(Long workId, Map<String, Object> request);

    /** 下架/删除作品 */
    void deleteWork(Long workId);

    /** 分页查询展示作品（学生按班级权限过滤） */
    Map<String, Object> listWorks(Integer pageNum, Integer pageSize, String sourceType, String subject, Long classId, String grade);

    /** 教师查看自己推荐的作品 */
    List<ShowcaseWork> getMyRecommended();

    /** 获取单个作品详情（含关联信息） */
    ShowcaseWork getWorkDetail(Long workId);

    /** 批量填充作品关联信息（姓名、班级、提交内容快照、首图） */
    void enrichShowcaseWorks(List<ShowcaseWork> records);

    /** 原子自增点赞数（避免并发丢失） */
    void incrementLikeCount(Long id);

    /** 更新点赞数 */
    void updateLikeCount(Long id, Integer count);

    /** 获取本周之星（最近7天点赞数前3） */
    List<ShowcaseWork> getWeeklyStars();

    /** 根据 ID 列表批量获取作品 */
    List<ShowcaseWork> getWorksByIds(java.util.Collection<Long> ids);
}

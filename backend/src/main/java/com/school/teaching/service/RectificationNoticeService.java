package com.school.teaching.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.teaching.entity.RectificationNotice;

public interface RectificationNoticeService {
    RectificationNotice getById(Long id);
    RectificationNotice create(RectificationNotice notice);
    RectificationNotice acknowledge(Long id, Long teacherId);
    RectificationNotice comply(Long id, Long teacherId);
    IPage<RectificationNotice> getPage(Long userId, String role, int page, int size);
}

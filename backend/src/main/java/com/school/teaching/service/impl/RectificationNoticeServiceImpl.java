package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.entity.RectificationNotice;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.RectificationNoticeMapper;
import com.school.teaching.service.RectificationNoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RectificationNoticeServiceImpl implements RectificationNoticeService {

    private final RectificationNoticeMapper mapper;

    @Override
    public RectificationNotice getById(Long id) {
        RectificationNotice n = mapper.selectById(id);
        if (n == null) throw new BusinessException(404, "整改通知书不存在");
        return n;
    }

    @Override
    @Transactional
    public RectificationNotice create(RectificationNotice notice) {
        if (notice.getStatus() == null) notice.setStatus("SENT");
        if ("SENT".equals(notice.getStatus())) {
            notice.setSentAt(LocalDateTime.now());
        }
        mapper.insert(notice);
        return notice;
    }

    @Override
    @Transactional
    public RectificationNotice acknowledge(Long id, Long teacherId) {
        RectificationNotice n = getById(id);
        if (!"SENT".equals(n.getStatus())) {
            throw new BusinessException(409, "仅已发送状态的通知书可签收");
        }
        if (!teacherId.equals(n.getRecipientId())) {
            throw new BusinessException(403, "仅收件人可签收");
        }
        n.setStatus("ACKNOWLEDGED");
        n.setAcknowledgedAt(LocalDateTime.now());
        mapper.updateById(n);
        return n;
    }

    @Override
    @Transactional
    public RectificationNotice comply(Long id, Long teacherId) {
        RectificationNotice n = getById(id);
        if (!"ACKNOWLEDGED".equals(n.getStatus())) {
            throw new BusinessException(409, "仅已确认状态的通知书可完成");
        }
        if (!teacherId.equals(n.getRecipientId())) {
            throw new BusinessException(403, "仅收件人可提交完成");
        }
        n.setStatus("COMPLIED");
        n.setCompliedAt(LocalDateTime.now());
        mapper.updateById(n);
        return n;
    }

    @Override
    public IPage<RectificationNotice> getPage(Long userId, String role, int page, int size) {
        LambdaQueryWrapper<RectificationNotice> q = new LambdaQueryWrapper<>();
        if ("INSPECTOR".equals(role)) {
            q.eq(RectificationNotice::getSenderId, userId);
        } else {
            q.eq(RectificationNotice::getRecipientId, userId);
        }
        q.orderByDesc(RectificationNotice::getCreatedAt);
        return mapper.selectPage(new Page<>(page, size), q);
    }
}

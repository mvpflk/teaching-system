package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.entity.InspectionRecord;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.InspectionRecordMapper;
import com.school.teaching.service.InspectionRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class InspectionRecordServiceImpl implements InspectionRecordService {

    private final InspectionRecordMapper mapper;

    @Override
    public InspectionRecord getById(Long id) {
        InspectionRecord r = mapper.selectById(id);
        if (r == null) throw new BusinessException(404, "巡视记录不存在");
        return r;
    }

    @Override
    @Transactional
    public InspectionRecord create(InspectionRecord record) {
        if (record.getStatus() == null) record.setStatus("DRAFT");
        if (record.getSeverity() == null) record.setSeverity("INFO");
        if (record.getRecordType() == null) record.setRecordType("CASUAL");
        mapper.insert(record);
        return record;
    }

    @Override
    @Transactional
    public InspectionRecord update(Long id, InspectionRecord data) {
        InspectionRecord existing = getById(id);
        if (!"DRAFT".equals(existing.getStatus())) {
            throw new BusinessException(409, "仅草稿状态的巡视记录可编辑");
        }
        if (data.getInspectorId() != null) existing.setInspectorId(data.getInspectorId());
        if (data.getRecordType() != null) existing.setRecordType(data.getRecordType());
        if (data.getTitle() != null) existing.setTitle(data.getTitle());
        if (data.getDescription() != null) existing.setDescription(data.getDescription());
        if (data.getLocation() != null) existing.setLocation(data.getLocation());
        if (data.getTargetClassId() != null) existing.setTargetClassId(data.getTargetClassId());
        if (data.getTargetTeacherId() != null) existing.setTargetTeacherId(data.getTargetTeacherId());
        if (data.getSeverity() != null) existing.setSeverity(data.getSeverity());
        if (data.getStatus() != null) existing.setStatus(data.getStatus());
        if (data.getAttachmentUrls() != null) existing.setAttachmentUrls(data.getAttachmentUrls());
        if (data.getRecordDate() != null) existing.setRecordDate(data.getRecordDate());
        mapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        InspectionRecord r = getById(id);
        if (!"DRAFT".equals(r.getStatus())) {
            throw new BusinessException(409, "仅草稿状态的巡视记录可删除");
        }
        mapper.deleteById(id);
    }

    @Override
    public IPage<InspectionRecord> getPage(Long inspectorId, String recordType,
                                           String startDate, String endDate,
                                           int page, int size) {
        LambdaQueryWrapper<InspectionRecord> q = new LambdaQueryWrapper<>();
        if (inspectorId != null) q.eq(InspectionRecord::getInspectorId, inspectorId);
        if (recordType != null && !recordType.isEmpty()) q.eq(InspectionRecord::getRecordType, recordType);
        if (startDate != null && !startDate.isEmpty()) q.ge(InspectionRecord::getRecordDate, LocalDate.parse(startDate));
        if (endDate != null && !endDate.isEmpty()) q.le(InspectionRecord::getRecordDate, LocalDate.parse(endDate));
        q.orderByDesc(InspectionRecord::getRecordDate);
        return mapper.selectPage(new Page<>(page, size), q);
    }
}

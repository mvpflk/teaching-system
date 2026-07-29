package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.PrecisionProgress;
import com.school.teaching.mapper.PrecisionProgressMapper;
import com.school.teaching.service.PrecisionProgressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PrecisionProgressServiceImpl implements PrecisionProgressService {

    @Autowired
    private PrecisionProgressMapper precisionProgressMapper;

    @Override
    public void markWeakIfNeeded(Long studentId, Long nodeId, Long configId) {
        PrecisionProgress progress = precisionProgressMapper.selectOne(
            new LambdaQueryWrapper<PrecisionProgress>()
                .eq(PrecisionProgress::getStudentId, studentId)
                .eq(PrecisionProgress::getNodeId, nodeId)
                .last("LIMIT 1"));

        if (progress == null) {
            progress = new PrecisionProgress();
            progress.setStudentId(studentId);
            progress.setNodeId(nodeId);
            progress.setMasteryPercent(BigDecimal.valueOf(0.40));
            progress.setStatus("WEAK");
            progress.setTotalAttempts(0);
            progress.setTotalCorrect(0);
            progress.setConsecutiveCorrect(0);
            try {
                precisionProgressMapper.insert(progress);
                log.info("创建薄弱记录 studentId={} nodeId={}", studentId, nodeId);
            } catch (Exception e) {
                log.warn("创建薄弱记录失败 studentId={} nodeId={}", studentId, nodeId, e);
            }
            return;
        }

        BigDecimal threshold = BigDecimal.valueOf(0.60);
        if (progress.getMasteryPercent().compareTo(threshold) < 0) {
            progress.setStatus("WEAK");
            precisionProgressMapper.updateById(progress);
            log.info("标记弱项 studentId={} nodeId={} masteryPercent={}", studentId, nodeId, progress.getMasteryPercent());
        }
    }

    @Override
    public List<Long> findWeakNodeIds(Long studentId, BigDecimal threshold) {
        List<PrecisionProgress> list = precisionProgressMapper.selectList(
            new LambdaQueryWrapper<PrecisionProgress>()
                .eq(PrecisionProgress::getStudentId, studentId)
                .and(w -> w
                    .eq(PrecisionProgress::getStatus, "WEAK")
                    .or()
                    .lt(PrecisionProgress::getMasteryPercent, threshold)));
        return list.stream().map(PrecisionProgress::getNodeId).collect(Collectors.toList());
    }
}

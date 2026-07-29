package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.ClassTypeConfig;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.ClassTypeConfigMapper;
import com.school.teaching.service.ClassTypeConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassTypeConfigServiceImpl implements ClassTypeConfigService {

    private final ClassTypeConfigMapper mapper;

    @Override
    @Cacheable("classTypeConfig")
    public List<ClassTypeConfig> listByStageId(Long stageId) {
        return mapper.selectList(
            new LambdaQueryWrapper<ClassTypeConfig>()
                .eq(stageId != null, ClassTypeConfig::getStageId, stageId)
                .orderByAsc(ClassTypeConfig::getSortOrder));
    }

    @Override
    @Cacheable("classTypeConfig")
    public List<ClassTypeConfig> listAll() {
        return mapper.selectList(
            new LambdaQueryWrapper<ClassTypeConfig>()
                .orderByAsc(ClassTypeConfig::getStageId, ClassTypeConfig::getSortOrder));
    }

    @Override
    @Transactional
    @CacheEvict(value = "classTypeConfig", allEntries = true)
    public ClassTypeConfig update(Long id, ClassTypeConfig config) {
        ClassTypeConfig existing = mapper.selectById(id);
        if (existing == null) throw new BusinessException(404, "类型配置不存在");
        if (config.getTypeName() != null) existing.setTypeName(config.getTypeName());
        if (config.getCategory() != null) existing.setCategory(config.getCategory());
        if (config.getSortOrder() != null) existing.setSortOrder(config.getSortOrder());
        if (config.getStageId() != null) existing.setStageId(config.getStageId());
        if (config.getTypeCode() != null) existing.setTypeCode(config.getTypeCode());
        if (config.getDefaultMajor() != null) existing.setDefaultMajor(config.getDefaultMajor());
        mapper.updateById(existing);
        return existing;
    }

    // [已禁用] create / delete / getTypeStats — 类型固化为普高班+职高班，由 v31 SQL 种子数据维护
}

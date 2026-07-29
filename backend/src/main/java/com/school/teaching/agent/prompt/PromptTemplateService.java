package com.school.teaching.agent.prompt;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.PromptTemplate;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.PromptTemplateMapper;
import com.school.teaching.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PromptTemplateService {

    @Autowired
    private PromptTemplateMapper mapper;

    @Autowired
    private PromptTemplateCache cache;

    public List<PromptTemplate> listByType(String type) {
        return mapper.selectList(
                new LambdaQueryWrapper<PromptTemplate>()
                        .eq(PromptTemplate::getType, type)
                        .orderByAsc(PromptTemplate::getName, PromptTemplate::getVersion));
    }

    public List<PromptTemplate> listVersions(String name) {
        return mapper.selectList(
                new LambdaQueryWrapper<PromptTemplate>()
                        .eq(PromptTemplate::getName, name)
                        .orderByDesc(PromptTemplate::getVersion));
    }

    public PromptTemplate getById(Long id) {
        return mapper.selectById(id);
    }

    public PromptTemplate getActive(String name) {
        return mapper.selectOne(
                new LambdaQueryWrapper<PromptTemplate>()
                        .eq(PromptTemplate::getName, name)
                        .eq(PromptTemplate::getIsActive, true));
    }

    @Transactional
    public PromptTemplate createVersion(PromptTemplate template) {
        PromptTemplate maxVer = mapper.selectOne(
                new LambdaQueryWrapper<PromptTemplate>()
                        .eq(PromptTemplate::getName, template.getName())
                        .orderByDesc(PromptTemplate::getVersion)
                        .last("LIMIT 1"));
        int nextVersion = maxVer != null ? maxVer.getVersion() + 1 : 1;
        template.setVersion(nextVersion);
        template.setIsActive(false);
        template.setCreatedBy(SecurityUtils.getCurrentUsername());
        mapper.insert(template);
        return template;
    }

    @Transactional
    public void activateVersion(String name, Long id) {
        PromptTemplate old = new PromptTemplate();
        old.setIsActive(false);
        mapper.update(old,
                new LambdaQueryWrapper<PromptTemplate>()
                        .eq(PromptTemplate::getName, name)
                        .eq(PromptTemplate::getIsActive, true));
        PromptTemplate target = mapper.selectById(id);
        if (target == null) throw new BusinessException(400, "版本不存在");
        target.setIsActive(true);
        mapper.updateById(target);
        cache.refresh();
    }

    @Transactional
    public PromptTemplate upsertFinal(PromptTemplate template) {
        template.setType("FINAL");
        template.setVersion(1);
        PromptTemplate existing = mapper.selectOne(
                new LambdaQueryWrapper<PromptTemplate>()
                        .eq(PromptTemplate::getName, template.getName())
                        .eq(PromptTemplate::getSubject, template.getSubject() != null ? template.getSubject() : "")
                        .eq(PromptTemplate::getType, "FINAL"));
        if (existing != null) {
            existing.setContent(template.getContent());
            existing.setIsActive(true);
            existing.setCreatedBy(SecurityUtils.getCurrentUsername());
            mapper.updateById(existing);
            cache.refresh();
            return existing;
        }
        template.setIsActive(true);
        template.setCreatedBy(SecurityUtils.getCurrentUsername());
        mapper.insert(template);
        cache.refresh();
        return template;
    }

    @Transactional
    public void delete(Long id) {
        PromptTemplate pt = mapper.selectById(id);
        if (pt == null) throw new BusinessException(400, "模板不存在");
        mapper.deleteById(id);
        cache.refresh();
    }
}

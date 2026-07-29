package com.school.teaching.agent.prompt;

import com.school.teaching.entity.PromptTemplate;
import com.school.teaching.mapper.PromptTemplateMapper;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PromptTemplateCache {

    @Autowired
    private PromptTemplateMapper mapper;

    @Getter
    @AllArgsConstructor
    static class CacheEntry {
        private String content;
        private int version;
    }

    private volatile Map<String, CacheEntry> templateCache = Collections.emptyMap();
    private volatile Map<String, String> finalCache = Collections.emptyMap();

    @PostConstruct
    public void init() {
        refresh();
    }

    public void refresh() {
        List<PromptTemplate> all = mapper.selectList(null);
        templateCache = all.stream()
                .filter(p -> "TEMPLATE".equals(p.getType()) && Boolean.TRUE.equals(p.getIsActive()))
                .collect(Collectors.toConcurrentMap(
                        p -> p.getSubject() != null ? p.getName() + "_" + p.getSubject() : p.getName(),
                        p -> new CacheEntry(p.getContent(), p.getVersion()),
                        (a, b) -> b));
        finalCache = all.stream()
                .filter(p -> "FINAL".equals(p.getType()) && Boolean.TRUE.equals(p.getIsActive()))
                .collect(Collectors.toConcurrentMap(
                        p -> p.getName() + ":" + (p.getSubject() != null ? p.getSubject() : ""),
                        PromptTemplate::getContent,
                        (a, b) -> b));
        log.info("PromptTemplateCache refreshed: {} TEMPLATE, {} FINAL", templateCache.size(), finalCache.size());
    }

    public String getTemplate(String name, String subject) {
        CacheEntry entry = getTemplateEntry(name, subject);
        return entry != null ? entry.getContent() : null;
    }

    public int getTemplateVersion(String name, String subject) {
        CacheEntry entry = getTemplateEntry(name, subject);
        return entry != null ? entry.getVersion() : 0;
    }

    private CacheEntry getTemplateEntry(String name, String subject) {
        if (subject != null) {
            CacheEntry val = templateCache.get(name + "_" + subject);
            if (val != null) return val;
        }
        return templateCache.get(name);
    }

    public String getFinal(String name, String subject) {
        String key = name + ":" + (subject != null ? subject : "");
        String val = finalCache.get(key);
        if (val != null) return val;
        if (subject != null) return finalCache.get(name + ":");
        return null;
    }
}

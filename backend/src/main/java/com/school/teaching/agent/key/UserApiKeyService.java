package com.school.teaching.agent.key;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.UserApiKey;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.UserApiKeyMapper;
import com.school.teaching.util.EncryptionUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserApiKeyService {

    private final UserApiKeyMapper mapper;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private EncryptionUtil encryptionUtil;

    @PostConstruct
    public void init() {
        this.encryptionUtil = new EncryptionUtil(jwtSecret);
    }

    public UserApiKey addKey(Long userId, String label, String baseUrl, String apiKey, String model) {
        UserApiKey entity = new UserApiKey();
        entity.setUserId(userId);
        entity.setLabel(label);
        entity.setBaseUrl(baseUrl);
        entity.setEncryptedKey(encryptionUtil.encrypt(apiKey));
        entity.setModel(model);
        entity.setIsActive(1);
        mapper.insert(entity);
        log.info("UserApiKey: userId={} 添加Key label={}", userId, label);
        return entity;
    }

    public void updateKey(Long id, Long userId, String label, String baseUrl, String apiKey, String model) {
        UserApiKey existing = mapper.selectById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new BusinessException(404, "API Key 不存在");
        }
        existing.setLabel(label);
        existing.setBaseUrl(baseUrl);
        if (apiKey != null && !apiKey.isBlank()) {
            existing.setEncryptedKey(encryptionUtil.encrypt(apiKey));
        }
        existing.setModel(model);
        existing.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(existing);
    }

    public void deleteKey(Long id, Long userId) {
        UserApiKey existing = mapper.selectById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new BusinessException(404, "API Key 不存在");
        }
        mapper.deleteById(id);
    }

    public void setActive(Long id, Long userId, boolean active) {
        UserApiKey existing = mapper.selectById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new BusinessException(404, "API Key 不存在");
        }
        existing.setIsActive(active ? 1 : 0);
        mapper.updateById(existing);
    }

    public List<UserApiKey> listKeys(Long userId) {
        return mapper.selectList(new LambdaQueryWrapper<UserApiKey>()
                .eq(UserApiKey::getUserId, userId)
                .orderByDesc(UserApiKey::getCreatedAt));
    }

    public UserApiKey getActiveKey(Long userId) {
        return mapper.selectOne(new LambdaQueryWrapper<UserApiKey>()
                .eq(UserApiKey::getUserId, userId)
                .eq(UserApiKey::getIsActive, 1)
                .last("LIMIT 1"));
    }

    public String decryptKey(UserApiKey key) {
        return encryptionUtil.decrypt(key.getEncryptedKey());
    }
}
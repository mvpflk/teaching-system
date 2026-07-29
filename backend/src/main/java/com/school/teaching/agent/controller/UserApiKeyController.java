package com.school.teaching.agent.controller;

import com.school.teaching.agent.key.UserApiKeyService;
import com.school.teaching.agent.security.UserContext;
import com.school.teaching.agent.security.UserContextResolver;
import com.school.teaching.common.R;
import com.school.teaching.entity.UserApiKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/agent/api-keys")
@RequiredArgsConstructor
public class UserApiKeyController {

    private final UserApiKeyService userApiKeyService;
    private final UserContextResolver userContextResolver;

    @GetMapping
    public R<List<UserApiKey>> list() {
        UserContext caller = userContextResolver.resolve();
        return R.ok(userApiKeyService.listKeys(caller.getUserId()));
    }

    @PostMapping
    public R<UserApiKey> add(@RequestBody Map<String, String> body) {
        UserContext caller = userContextResolver.resolve();
        UserApiKey key = userApiKeyService.addKey(
                caller.getUserId(),
                body.get("label"),
                body.get("baseUrl"),
                body.get("apiKey"),
                body.get("model"));
        return R.ok(key, "添加成功");
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        UserContext caller = userContextResolver.resolve();
        userApiKeyService.updateKey(
                id, caller.getUserId(),
                body.get("label"),
                body.get("baseUrl"),
                body.get("apiKey"),
                body.get("model"));
        return R.ok(null, "更新成功");
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        UserContext caller = userContextResolver.resolve();
        userApiKeyService.deleteKey(id, caller.getUserId());
        return R.ok(null, "删除成功");
    }

    @PutMapping("/{id}/active")
    public R<Void> setActive(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        UserContext caller = userContextResolver.resolve();
        userApiKeyService.setActive(id, caller.getUserId(), body.getOrDefault("active", true));
        return R.ok(null, "已切换");
    }
}
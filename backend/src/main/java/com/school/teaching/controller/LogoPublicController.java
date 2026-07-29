package com.school.teaching.controller;

import com.school.teaching.common.R;
import com.school.teaching.service.SystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 公开接口 — 无需登录即可访问（供登录页获取 Logo 等）
 */
@RestController
@RequestMapping("/system")
public class LogoPublicController {

    @Autowired
    private SystemService systemService;

    /** 获取学校 Logo URL（公开，登录页需要） */
    @GetMapping("/logo")
    public R<Map<String, String>> getLogo() {
        String url = systemService.getLogoUrl();
        return R.ok(Map.of("url", url != null ? url : ""));
    }
}

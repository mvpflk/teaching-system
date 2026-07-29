package com.school.teaching.controller;

import com.school.teaching.common.R;
import com.school.teaching.entity.Message;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/messages")
@PreAuthorize("hasAnyRole('TEACHER','HEAD_TEACHER','PARENT','ADMIN','SUPER_ADMIN')")
public class MessageController {

    @Autowired private MessageService messageService;

    @GetMapping("/conversations")
    public R<List<Map<String, Object>>> getConversations() {
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(messageService.getConversations(userId));
    }

    @GetMapping("/conversations/{otherUserId}")
    public R<List<Map<String, Object>>> getMessages(@PathVariable Long otherUserId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(messageService.getMessages(userId, otherUserId));
    }

    @PostMapping("/send")
    public R<Map<String, Object>> send(@RequestBody Map<String, Object> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long receiverId = Long.valueOf(body.get("receiverId").toString());
        String content = (String) body.get("content");
        Message msg = messageService.send(userId, receiverId, content);
        return R.ok(Map.of("id", msg.getId(), "createdAt", msg.getCreatedAt()));
    }

    @GetMapping("/unread-count")
    public R<Map<String, Object>> getUnreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(Map.of("count", messageService.getUnreadCount(userId)));
    }
}

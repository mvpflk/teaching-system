package com.school.teaching.service;

import com.school.teaching.entity.Message;

import java.util.List;
import java.util.Map;

public interface MessageService {
    List<Map<String, Object>> getConversations(Long userId);
    List<Map<String, Object>> getMessages(Long userId, Long otherUserId);
    Message send(Long senderId, Long receiverId, String content);
    int getUnreadCount(Long userId);
}

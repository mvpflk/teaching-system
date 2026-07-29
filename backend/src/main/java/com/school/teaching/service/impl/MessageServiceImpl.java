package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.school.teaching.entity.Message;
import com.school.teaching.entity.User;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.MessageMapper;
import com.school.teaching.mapper.UserMapper;
import com.school.teaching.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired private MessageMapper messageMapper;
    @Autowired private UserMapper userMapper;

    @Override
    public List<Map<String, Object>> getConversations(Long userId) {
        List<Message> all = messageMapper.selectList(
            new LambdaQueryWrapper<Message>()
                .eq(Message::getSenderId, userId)
                .or()
                .eq(Message::getReceiverId, userId)
                .orderByDesc(Message::getCreatedAt));

        if (all.isEmpty()) return List.of();

        Map<Long, List<Message>> grouped = new LinkedHashMap<>();
        Set<Long> otherUserIds = new HashSet<>();
        for (Message m : all) {
            Long otherId = m.getSenderId().equals(userId) ? m.getReceiverId() : m.getSenderId();
            otherUserIds.add(otherId);
            grouped.computeIfAbsent(otherId, k -> new ArrayList<>()).add(m);
        }

        Map<Long, User> userMap = userMapper.selectBatchIds(otherUserIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<Message>> entry : grouped.entrySet()) {
            Long otherId = entry.getKey();
            List<Message> msgs = entry.getValue();
            Message last = msgs.get(0);
            User u = userMap.get(otherId);
            long unread = msgs.stream().filter(m -> m.getIsRead() == 0 && m.getReceiverId().equals(userId)).count();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("otherUserId", otherId);
            item.put("otherUserName", u != null ? u.getRealName() : "?");
            item.put("lastMessage", last.getContent() != null && last.getContent().length() > 50
                ? last.getContent().substring(0, 50) + "…" : last.getContent());
            item.put("lastTime", last.getCreatedAt());
            item.put("unreadCount", unread);
            result.add(item);
        }
        result.sort((a, b) -> ((Comparable) b.getOrDefault("lastTime", "")).compareTo(a.getOrDefault("lastTime", "")));
        return result;
    }

    @Override
    @Transactional
    public List<Map<String, Object>> getMessages(Long userId, Long otherUserId) {
        List<Message> msgs = messageMapper.selectList(
            new LambdaQueryWrapper<Message>()
                .and(w -> w.eq(Message::getSenderId, userId).eq(Message::getReceiverId, otherUserId))
                .or(w -> w.eq(Message::getSenderId, otherUserId).eq(Message::getReceiverId, userId))
                .orderByAsc(Message::getCreatedAt));

        messageMapper.update(null, new LambdaUpdateWrapper<Message>()
            .eq(Message::getSenderId, otherUserId)
            .eq(Message::getReceiverId, userId)
            .eq(Message::getIsRead, 0)
            .set(Message::getIsRead, 1));

        return msgs.stream().map(m -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", m.getId());
            item.put("senderId", m.getSenderId());
            item.put("receiverId", m.getReceiverId());
            item.put("content", m.getContent());
            item.put("isRead", m.getIsRead());
            item.put("createdAt", m.getCreatedAt());
            return item;
        }).collect(Collectors.toList());
    }

    @Override
    public Message send(Long senderId, Long receiverId, String content) {
        if (content == null || content.trim().isEmpty()) throw new BusinessException(400, "消息内容不能为空");
        if (content.length() > 5000) throw new BusinessException(400, "消息内容过长（最大5000字）");
        if (senderId.equals(receiverId)) throw new BusinessException(400, "不能给自己发消息");
        User receiver = userMapper.selectById(receiverId);
        if (receiver == null) throw new BusinessException(404, "接收者不存在");

        String safe = content.replaceAll("<[^>]*>", "").trim();
        if (safe.isEmpty()) throw new BusinessException(400, "消息内容不合法");

        Message msg = new Message();
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setContent(safe);
        msg.setIsRead(0);
        messageMapper.insert(msg);
        return msg;
    }

    @Override
    public int getUnreadCount(Long userId) {
        return messageMapper.selectCount(
            new LambdaQueryWrapper<Message>()
                .eq(Message::getReceiverId, userId)
                .eq(Message::getIsRead, 0)).intValue();
    }
}

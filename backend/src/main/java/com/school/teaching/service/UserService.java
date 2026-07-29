package com.school.teaching.service;

import com.school.teaching.entity.User;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface UserService {

    User login(String username, String password);

    User getUserById(Long id);

    User getUserByUsername(String username);
    Map<String, Object> getStudentClassInfo(Long userId);

    /** 批量获取用户 */
    List<User> getUsersByIds(Collection<Long> ids);
}

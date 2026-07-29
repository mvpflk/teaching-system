package com.school.teaching.vo;

import com.school.teaching.entity.User;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String role;
    private String roleName;
    private Integer status;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;

    public static UserVO from(User user, String role, String roleName) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setRole(role);
        vo.setRoleName(roleName);
        vo.setStatus(user.getStatus());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}

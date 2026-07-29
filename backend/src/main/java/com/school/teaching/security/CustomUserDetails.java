package com.school.teaching.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.Collections;

@Getter
public class CustomUserDetails implements UserDetails {
    private final Long userId;
    private final String username;
    private final String role;
    private final Long roleId;
    private final String tokenJti;
    private final Long schoolId;
    private final Long stageId;

    public CustomUserDetails(Long userId, String username, String role, Long roleId,
                             String tokenJti, Long schoolId, Long stageId) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.roleId = roleId;
        this.tokenJti = tokenJti;
        this.schoolId = schoolId;
        this.stageId = stageId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override public String getPassword() { return null; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}

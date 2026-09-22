package com.permission.common.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

@Data
@NoArgsConstructor
public class LoginUser implements UserDetails {

    private Long userId;
    private String username;
    private String password;
    private String nickname;
    private Long deptId;
    private String deptName;
    private Integer dataScope;
    private List<Long> deptIds;
    private Set<String> permissions;
    private Set<String> roles;

    @Builder
    public LoginUser(Long userId, String username, String password, String nickname,
                     Long deptId, String deptName, Integer dataScope, List<Long> deptIds,
                     Set<String> permissions, Set<String> roles) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.deptId = deptId;
        this.deptName = deptName;
        this.dataScope = dataScope;
        this.deptIds = deptIds;
        this.permissions = permissions != null ? permissions : new HashSet<>();
        this.roles = roles != null ? roles : new HashSet<>();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();
        if (permissions != null) {
            permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
        }
        if (roles != null) {
            roles.forEach(r -> authorities.add(new SimpleGrantedAuthority(r)));
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}


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

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.common.dto.LoginUser
// 【模块】permission-common（公共基础模块）
//
// 【使用的注解/技术】
//   - @Data — Lombok，生成 getter/setter
//   - @NoArgsConstructor — 提供无参构造（Spring Security 反序列化需要）
//   - @Builder — Lombok，为全字段构造器提供流式 builder 语法
//   - implements UserDetails — Spring Security 用户模型接口
//   - SimpleGrantedAuthority — 将权限字符串和角色统一封装为鉴权对象
//
// 【关联文件】
//   - 被 JwtAuthenticationFilter 放入 SecurityContextHolder → framework/security/JwtAuthenticationFilter.java
//   - 被 DetailServiceImpl.loadUserByUsername() 构造 → service/impl/DetailServiceImpl.java
//   - @PreAuthorize 基于 getAuthorities() 中的角色/权限字符串鉴权 → controller/*Controller.java
//   - 基础字段源自 SysUser → entity/SysUser.java
//
// 【核心作用】Spring Security 当前登录用户的上下文对象，持有用户身份、权限与角色。
//
// 【设计必要性】getAuthorities() 把角色编码（如 ROLE_ADMIN）与权限字符串都作为
//   GrantedAuthority 放入，@PreAuthorize("hasRole('ADMIN')") 与
//   @PreAuthorize("hasAuthority('system:user:list')") 才能同时匹配生效。
//   DataScope 数值由 DataDataScopeAspect 解析返回 SQL 过滤条件。
//
// 【注意事项】
//   - 四个布尔方法均返回 true，账号锁定/过期等业务状态由 UserStatus 枚举在服务层控制
//   - Builder 参数非法时 permissions/roles 在构造中被防护为至少空集合，避免空指针
// ============================================================

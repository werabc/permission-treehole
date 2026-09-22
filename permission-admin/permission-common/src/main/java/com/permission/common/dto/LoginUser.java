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
    /**
     * 登录期已解析好的"可见部门ID集合"（数据权限落地用）
     * 由 UserDetailsServiceImpl 依据角色 data_scope 预计算，拦截器直接使用，
     * 避免每条 SQL 都去递归组织树
     */
    private List<Long> deptIds;

    /** 当前用户所属部门层级：1-集团 2-公司 3-部门 4+-小组 */
    private Integer deptLevel;

    /**
     * sys_dept 表用的可见集合 = 可见部门 + 其全部祖先
     * 原因：部门以树形展示，若只放行叶子节点，父节点缺失会导致前端建树时把节点全部丢弃
     */
    private List<Long> deptTreeIds;

    /** 所属公司ID（ancestors 中层级=2 的节点；无公司层则回退为根节点） */
    private Long companyId;

    /** 所属集团ID（组织树根节点） */
    private Long groupId;

    private Set<String> permissions;
    private Set<String> roles;

    @Builder
    public LoginUser(Long userId, String username, String password, String nickname,
                     Long deptId, String deptName, Integer dataScope, List<Long> deptIds,
                     Integer deptLevel, List<Long> deptTreeIds, Long companyId, Long groupId,
                     Set<String> permissions, Set<String> roles) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.deptId = deptId;
        this.deptName = deptName;
        this.dataScope = dataScope;
        this.deptIds = deptIds != null ? deptIds : new ArrayList<>();
        this.deptLevel = deptLevel;
        this.deptTreeIds = deptTreeIds != null ? deptTreeIds : new ArrayList<>();
        this.companyId = companyId;
        this.groupId = groupId;
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


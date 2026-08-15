# permission-system 模块详解

> **模块名称**：Permission System  
> **Artifact ID**：`permission-system`  
> **包名根路径**：`com.permission.system`  
> **职责**：业务逻辑层 — Mapper 数据访问、Service 业务接口与实现、AOP 操作日志、安全用户加载、数据初始化  
> **依赖**：permission-common、permission-framework、mybatis-plus-spring-boot3-starter、mysql-connector-j、mapstruct、hutool-all、lombok

---

## 模块文件清单

### Mapper 数据访问层（8 个）
| 文件 | 说明 |
|------|------|
| `mapper/SysUserMapper.java` | 用户 Mapper（含角色编码、权限 SQL 查询） |
| `mapper/SysRoleMapper.java` | 角色 Mapper |
| `mapper/SysMenuMapper.java` | 菜单 Mapper（含角色菜单、用户菜单 SQL） |
| `mapper/SysDeptMapper.java` | 部门 Mapper |
| `mapper/SysUserRoleMapper.java` | 用户-角色关联 Mapper |
| `mapper/SysRoleMenuMapper.java` | 角色-菜单关联 Mapper |
| `mapper/SysOperationLogMapper.java` | 操作日志 Mapper |
| `mapper/SysLoginLogMapper.java` | 登录日志 Mapper |

### Service 业务接口层（6 个）
| 文件 | 说明 |
|------|------|
| `service/SysUserService.java` | 用户业务接口 |
| `service/SysRoleService.java` | 角色业务接口 |
| `service/SysMenuService.java` | 菜单业务接口 |
| `service/SysDeptService.java` | 部门业务接口 |
| `service/SysOperationLogService.java` | 操作日志业务接口 |
| `service/SysLoginLogService.java` | 登录日志业务接口 |

### Service 实现层（6 个）
| 文件 | 说明 |
|------|------|
| `service/impl/SysUserServiceImpl.java` | 用户业务实现（登录、CRUD、权限分配） |
| `service/impl/SysRoleServiceImpl.java` | 角色业务实现 |
| `service/impl/SysMenuServiceImpl.java` | 菜单业务实现（树形结构） |
| `service/impl/SysDeptServiceImpl.java` | 部门业务实现（树形 + 祖级列表） |
| `service/impl/SysOperationLogServiceImpl.java` | 操作日志业务实现 |
| `service/impl/SysLoginLogServiceImpl.java` | 登录日志业务实现 |

### AOP 与配置（2 个）
| 文件 | 说明 |
|------|------|
| `aspect/OperationLogAspect.java` | 操作日志 AOP 切面 |
| `config/DataInitializer.java` | 应用启动数据初始化 |

### 安全（1 个）
| 文件 | 说明 |
|------|------|
| `security/UserDetailsServiceImpl.java` | Spring Security 用户加载实现 |

---

## 一、Mapper 层详解

### 1.1 SysUserMapper.java

**路径**：`com.permission.system.mapper.SysUserMapper`

### 使用的注解/技术
| 注解 | 来源 | 作用 |
|------|------|------|
| `@Mapper` | MyBatis | 标识为 MyBatis 接口（与 `@MapperScan` 配合） |
| `extends BaseMapper<SysUser>` | MyBatis-Plus | 继承通用 CRUD 方法 |
| `@Select` | MyBatis | 内联 SQL 查询 |
| `@Param` | MyBatis | 参数绑定 |

### 方法定义
```java
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    // 查询用户的所有角色编码（需角色启用且未删除）
    @Select("SELECT DISTINCT r.role_code FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.status = 1 AND r.deleted = 0")
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    // 查询用户的所有权限标识（需菜单启用、未删除、权限标识非空）
    @Select("SELECT DISTINCT m.permission FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND m.status = 1 AND m.deleted = 0 " +
            "AND m.permission IS NOT NULL AND m.permission != ''")
    List<String> selectPermissionsByUserId(@Param("userId") Long userId);
}
```

### 设计意图
- 继承 `BaseMapper<SysUser>` 获得 `selectById`、`selectList`、`insert`、`updateById`、`deleteBatchIds` 等通用 CRUD 方法
- 两个自定义查询使用三表联查（user → role → menu），直接返回用户的角色编码和权限标识集合

### SQL 关系图
```
sys_user ──< sys_user_role >── sys_role ──< sys_role_menu >── sys_menu
```

---

### 1.2 SysMenuMapper.java

**路径**：`com.permission.system.mapper.SysMenuMapper`

```java
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    // 根据角色 ID 查询角色-菜单关联
    @Select("SELECT * FROM sys_role_menu WHERE role_id = #{roleId}")
    List<SysRoleMenu> selectRoleMenusByRoleId(@Param("roleId") Long roleId);

    // 根据用户 ID 查询用户拥有的所有菜单（去重 + 排序）
    @Select("SELECT m.* FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND m.status = 1 AND m.deleted = 0 " +
            "ORDER BY m.sort ASC")
    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId);
}
```

---

### 1.3 其余 Mapper（纯 BaseMapper 继承）

以下 Mapper 仅继承 `BaseMapper<T>`，无自定义方法，使用 MyBatis-Plus 提供的通用 CRUD：

| Mapper | 实体 | 说明 |
|--------|------|------|
| `SysDeptMapper` | `SysDept` | 部门基础 CRUD |
| `SysRoleMapper` | `SysRole` | 角色基础 CRUD |
| `SysRoleMenuMapper` | `SysRoleMenu` | 角色-菜单关联 CRUD |
| `SysUserRoleMapper` | `SysUserRole` | 用户-角色关联 CRUD |
| `SysOperationLogMapper` | `SysOperationLog` | 操作日志 CRUD |
| `SysLoginLogMapper` | `SysLoginLog` | 登录日志 CRUD |

---

## 二、Service 接口层详解

### 2.1 SysUserService.java — 用户业务接口

**路径**：`com.permission.system.service.SysUserService`

### 使用的注解/技术
| 注解 | 来源 | 作用 |
|------|------|------|
| `extends IService<SysUser>` | MyBatis-Plus | 继承通用 Service 接口（page、save、update、remove 等） |

### 方法定义
```java
public interface SysUserService extends IService<SysUser> {
    TokenVO login(LoginDTO loginDTO);                           // 登录
    TokenVO refreshToken(String refreshToken);                  // 刷新 Token
    void logout(String token);                                  // 退出登录
    IPage<SysUser> pageUsers(long pageNum, long pageSize,       // 分页查询
        String keyword, Long deptId, Integer status);
    SysUser getUserById(Long id);                               // 详情查询
    void createUser(SysUser user);                              // 新增
    void updateUser(SysUser user);                              // 修改
    void deleteUsers(List<Long> ids);                           // 批量删除
    void updateStatus(Long id, Integer status);                 // 修改状态
    void resetPassword(Long id, String newPassword);            // 重置密码
    void updatePassword(Long userId, String oldPassword,        // 修改个人密码
        String newPassword);
    void assignRoles(Long userId, Set<Long> roleIds);           // 分配角色
    Set<Long> getUserRoleIds(Long userId);                      // 获取用户角色 ID
}
```

---

### 2.2 SysRoleService.java — 角色业务接口

```java
public interface SysRoleService extends IService<SysRole> {
    IPage<SysRole> pageRoles(long pageNum, long pageSize, String keyword);
    SysRole getRoleById(Long id);
    void createRole(SysRole role);
    void updateRole(SysRole role);
    void deleteRoles(List<Long> ids);
    void updateStatus(Long id, Integer status);
    void assignMenus(Long roleId, Set<Long> menuIds);       // 分配菜单权限
    Set<Long> getRoleMenuIds(Long roleId);                  // 获取角色菜单 ID
    List<SysRole> getAllRoles();                            // 获取所有启用角色
}
```

---

### 2.3 SysMenuService.java — 菜单业务接口

```java
public interface SysMenuService extends IService<SysMenu> {
    List<SysMenu> getMenuTree(String keyword, Integer status);  // 菜单树
    SysMenu getMenuById(Long id);
    void createMenu(SysMenu menu);
    void updateMenu(SysMenu menu);
    void deleteMenu(Long id);
    List<SysMenu> getUserMenus(Long userId);                    // 用户菜单（动态路由）
    List<SysMenu> getMenuTreeSelect();                          // 菜单树下拉选择
}
```

---

### 2.4 SysDeptService.java — 部门业务接口

```java
public interface SysDeptService extends IService<SysDept> {
    List<SysDept> getDeptTree(String keyword, Integer status);  // 部门树
    SysDept getDeptById(Long id);
    void createDept(SysDept dept);
    void updateDept(SysDept dept);
    void deleteDept(Long id);
    List<SysDept> getDeptTreeSelect();                          // 部门树下拉选择
}
```

---

### 2.5 SysOperationLogService.java — 操作日志业务接口

```java
public interface SysOperationLogService extends IService<SysOperationLog> {
    IPage<SysOperationLog> pageLogs(long pageNum, long pageSize,
        String keyword, String module, String startDate, String endDate);
}
```

---

### 2.6 SysLoginLogService.java — 登录日志业务接口

```java
public interface SysLoginLogService extends IService<SysLoginLog> {
    IPage<SysLoginLog> pageLogs(long pageNum, long pageSize,
        String keyword, String startDate, String endDate);
    void recordLoginLog(String username, String ip, Integer status, String message);
}
```

---

## 三、Service 实现层详解

### 3.1 SysUserServiceImpl.java — 用户业务实现（核心）

**路径**：`com.permission.system.service.impl.SysUserServiceImpl`

### 使用的注解/技术
| 注解 | 来源 | 作用 |
|------|------|------|
| `@Slf4j` | Lombok | 日志 |
| `@Service` | Spring | 声明为 Service 组件 |
| `@RequiredArgsConstructor` | Lombok | 构造器注入 |
| `extends ServiceImpl<SysUserMapper, SysUser>` | MyBatis-Plus | 通用 Service 实现 |
| `implements SysUserService` | 项目 | 业务接口 |
| `@Transactional` | Spring | 事务管理 |
| `@Override` | Java | 方法重写 |

### 依赖注入字段
```java
private final AuthenticationManager authenticationManager;  // 认证管理器
private final JwtTokenProvider jwtTokenProvider;            // JWT 工具
private final PasswordEncoder passwordEncoder;              // 密码编码
private final RedisTemplate<String, Object> redisTemplate;  // Redis
private final SysUserRoleMapper userRoleMapper;             // 用户-角色 Mapper
private final SysRoleMapper roleMapper;                     // 角色 Mapper
private final SysMenuMapper menuMapper;                     // 菜单 Mapper
private final SysDeptMapper deptMapper;                     // 部门 Mapper
private final SysLoginLogService loginLogService;           // 登录日志 Service
```

### 密码强度正则
```java
private static final Pattern PASSWORD_PATTERN = Pattern.compile(
    "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[~!@#$%^&*()_+\\-=\\[\\]{}|;:',.<>?/]).{8,}$");
```
要求：≥8 位，包含小写字母、大写字母、数字、特殊字符。

---

#### login() 方法 — 登录（核心业务流程）

```java
@Override
public TokenVO login(LoginDTO loginDTO) {
    String username = loginDTO.getUsername();

    // ====== 第 1 步：IP 限流检查 ======
    String clientIp = getClientIp();
    String rateLimitKey = SecurityConstants.LOGIN_RATE_LIMIT_PREFIX + clientIp;
    Long rateCount = redisTemplate.opsForValue().increment(rateLimitKey);
    if (rateCount == 1) {
        redisTemplate.expire(rateLimitKey, SecurityConstants.LOGIN_RATE_LIMIT_WINDOW, TimeUnit.SECONDS);
    }
    if (rateCount != null && rateCount > SecurityConstants.LOGIN_RATE_LIMIT_MAX) {
        throw new BusinessException(ResultCode.RATE_LIMITED);  // 60s 内 > 5 次
    }

    // ====== 第 2 步：验证码检查 ======
    if (StrUtil.isNotBlank(loginDTO.getCaptchaKey()) || StrUtil.isNotBlank(loginDTO.getCaptchaCode())) {
        String captchaKey = loginDTO.getCaptchaKey();
        String correctCode = (String) redisTemplate.opsForValue().get(SecurityConstants.CAPTCHA_PREFIX + captchaKey);
        if (StrUtil.isBlank(correctCode)) {
            throw new BusinessException(ResultCode.CAPTCHA_ERROR);  // 验证码过期
        }
        if (!correctCode.equalsIgnoreCase(loginDTO.getCaptchaCode())) {
            throw new BusinessException(ResultCode.CAPTCHA_ERROR);  // 验证码错误
        }
        redisTemplate.delete(SecurityConstants.CAPTCHA_PREFIX + captchaKey);  // 验证成功删除
    }

    // ====== 第 3 步：账户锁定检查 ======
    String failKey = SecurityConstants.LOGIN_FAIL_PREFIX + username;
    Object failCountObj = redisTemplate.opsForValue().get(failKey);
    int failCount = failCountObj instanceof Integer ? (Integer) failCountObj : 0;
    if (failCount >= SecurityConstants.MAX_LOGIN_FAIL_COUNT) {
        throw new BusinessException(ResultCode.ACCOUNT_TEMP_LOCKED);  // 已锁定
    }

    try {
        // ====== 第 4 步：Spring Security 认证 ======
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, loginDTO.getPassword()));
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();

        // ====== 第 5 步：生成双 Token ======
        Map<String, Object> claims = new HashMap<>();
        claims.put("permissions", loginUser.getPermissions());
        claims.put("roles", loginUser.getRoles());
        String accessToken = jwtTokenProvider.createAccessToken(loginUser.getUserId(), loginUser.getUsername(), claims);
        String refreshToken = jwtTokenProvider.createRefreshToken(loginUser.getUserId());

        // ====== 第 6 步：缓存 Token 到 Redis ======
        redisTemplate.opsForValue().set(
                SecurityConstants.TOKEN_CACHE_PREFIX + accessToken,
                loginUser,
                SecurityConstants.TOKEN_EXPIRE,
                TimeUnit.SECONDS);

        // ====== 第 7 步：更新登录信息 ======
        SysUser user = new SysUser();
        user.setId(loginUser.getUserId());
        user.setLastLoginTime(java.time.LocalDateTime.now());
        baseMapper.updateById(user);

        // ====== 第 8 步：清理失败计数 + 记录日志 ======
        redisTemplate.delete(failKey);
        loginLogService.recordLoginLog(loginUser.getUsername(), clientIp, 1, "登录成功");

        return TokenVO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(SecurityConstants.TOKEN_EXPIRE)
                .build();

    } catch (BusinessException e) {
        throw e;
    } catch (org.springframework.security.core.AuthenticationException e) {
        // ====== 认证失败处理 ======
        redisTemplate.opsForValue().increment(failKey);              // 失败计数 +1
        redisTemplate.expire(failKey, SecurityConstants.ACCOUNT_LOCK_MINUTES, TimeUnit.MINUTES);
        loginLogService.recordLoginLog(username, clientIp, 0, e.getMessage());  // 记录失败日志
        throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
    }
}
```

**登录流程总结**：
```
IP 限流 → 验证码 → 账户锁定 → Spring Security 认证 → 生成 Token → Redis 缓存 → 更新登录信息 → 返回
```

---

#### refreshToken() 方法 — 刷新 Token

```java
@Override
public TokenVO refreshToken(String refreshToken) {
    try {
        Claims claims = jwtTokenProvider.parseToken(refreshToken);
        if (!"refresh".equals(claims.get("type"))) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }
        Long userId = jwtTokenProvider.getUserId(refreshToken);
        SysUser user = baseMapper.selectById(userId);
        if (user == null || user.getStatus() != UserStatus.ENABLED.getCode()) {
            throw new BusinessException(ResultCode.USER_ACCOUNT_DISABLED);
        }
        // 生成新的双 Token
        String newAccessToken = jwtTokenProvider.createAccessToken(userId, user.getUsername(), new HashMap<>());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);
        return TokenVO.builder()
                .accessToken(newAccessToken).refreshToken(newRefreshToken)
                .expiresIn(SecurityConstants.TOKEN_EXPIRE).build();
    } catch (ExpiredJwtException e) {
        throw new BusinessException(ResultCode.TOKEN_EXPIRED);
    } catch (BusinessException e) {
        throw e;
    } catch (Exception e) {
        throw new BusinessException(ResultCode.TOKEN_INVALID);
    }
}
```

---

#### logout() 方法 — 退出登录

```java
@Override
public void logout(String accessToken) {
    if (StrUtil.isNotBlank(accessToken)) {
        redisTemplate.delete(SecurityConstants.TOKEN_CACHE_PREFIX + accessToken);  // 删除缓存
        try {
            long remaining = jwtTokenProvider.getExpiration(accessToken) - System.currentTimeMillis();
            if (remaining > 0) {
                // 加入黑名单，过期时间为 Token 剩余有效期
                redisTemplate.opsForValue().set(
                        SecurityConstants.TOKEN_BLACKLIST_PREFIX + accessToken, "1",
                        remaining, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            log.warn("Failed to blacklist token: {}", e.getMessage());
        }
    }
}
```

---

#### 其他方法摘要

| 方法 | 说明 | 关键逻辑 |
|------|------|---------|
| `pageUsers()` | 分页查询 | 关键字模糊搜索（username/nickname/phone）+ 部门筛选 + 状态筛选 + 按部门排序 |
| `getUserById()` | 详情 | 查询后填充 `deptName` |
| `createUser()` | 新增 | 用户名唯一校验 → 密码强度校验 → BCrypt 加密 → 默认启用 |
| `updateUser()` | 修改 | 不可修改密码（`setPassword(null)`），可修改用户名但需唯一 |
| `deleteUsers()` | 批量删除 | 删除用户 + 删除关联的角色关系 |
| `updateStatus()` | 修改状态 | 部分更新 |
| `resetPassword()` | 重置密码 | 强度校验 + BCrypt 加密 |
| `updatePassword()` | 修改密码 | 原密码匹配 → 新密码强度 → 加密更新 |
| `assignRoles()` | 分配角色 | 先删除所有角色关联，再批量插入 |
| `getUserRoleIds()` | 获取角色 ID | 查询关联表 |

---

### 3.2 SysRoleServiceImpl.java — 角色业务实现

**路径**：`com.permission.system.service.impl.SysRoleServiceImpl`

### 核心方法

#### deleteRoles() — 删除角色（带安全检查）
```java
@Override
@Transactional
public void deleteRoles(List<Long> ids) {
    if (CollUtil.isNotEmpty(ids)) {
        for (Long id : ids) {
            long count = userRoleMapper.selectCount(
                    new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id));
            if (count > 0) {
                SysRole role = baseMapper.selectById(id);
                throw new BusinessException(ResultCode.ROLE_HAS_USERS,
                        "角色【" + (role != null ? role.getRoleName() : id) + "】已分配给用户，无法删除");
            }
        }
        baseMapper.deleteBatchIds(ids);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, ids));
    }
}
```
删除前检查是否有用户关联，有则拒绝删除并返回友好提示。

#### assignMenus() — 分配菜单权限
```java
@Override
@Transactional
public void assignMenus(Long roleId, Set<Long> menuIds) {
    // 先删除该角色所有菜单关联
    roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
    if (CollUtil.isNotEmpty(menuIds)) {
        // 批量插入新的关联
        for (Long menuId : menuIds) {
            SysRoleMenu roleMenu = new SysRoleMenu();
            roleMenu.setRoleId(roleId);
            roleMenu.setMenuId(menuId);
            roleMenuMapper.insert(roleMenu);
        }
    }
}
```

#### 唯一性校验
```java
private void validateRoleNameUnique(String roleName, Long excludeId) { ... }  // 角色名唯一
private void validateRoleCodeUnique(String roleCode, Long excludeId) { ... }  // 角色编码唯一
```
支持排除当前 ID（修改时自身不算重复）。

---

### 3.3 SysMenuServiceImpl.java — 菜单业务实现

**路径**：`com.permission.system.service.impl.SysMenuServiceImpl`

### 核心方法

#### getMenuTree() — 获取菜单树
```java
@Override
public List<SysMenu> getMenuTree(String keyword, Integer status) {
    LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
    if (StrUtil.isNotBlank(keyword)) {
        wrapper.like(SysMenu::getMenuName, keyword);
    }
    if (status != null) {
        wrapper.eq(SysMenu::getStatus, status);
    }
    wrapper.orderByAsc(SysMenu::getSort);
    List<SysMenu> allMenus = baseMapper.selectList(wrapper);
    return buildTree(allMenus, 0L);  // 递归构建树
}
```

#### getUserMenus() — 获取用户菜单（动态路由）
```java
@Override
public List<SysMenu> getUserMenus(Long userId) {
    List<SysMenu> menus = baseMapper.selectMenusByUserId(userId);  // 三表联查
    return buildTree(menus, 0L);
}
```

#### buildTree() — 递归构建树形结构
```java
private List<SysMenu> buildTree(List<SysMenu> menus, Long parentId) {
    List<SysMenu> tree = new ArrayList<>();
    for (SysMenu menu : menus) {
        if (parentId.equals(menu.getParentId())) {
            menu.setChildren(buildTree(menus, menu.getId()));  // 递归查找子菜单
            tree.add(menu);
        }
    }
    return tree;
}
```
从根节点（parentId=0）开始，递归将所有平铺数据转为嵌套树。

---

### 3.4 SysDeptServiceImpl.java — 部门业务实现

**路径**：`com.permission.system.service.impl.SysDeptServiceImpl`

### 核心方法

#### createDept() / updateDept() — 自动生成祖级列表
```java
@Override
@Transactional
public void createDept(SysDept dept) {
    validateDeptNameUnique(dept.getDeptName(), dept.getParentId(), null);
    if (dept.getParentId() == null) {
        dept.setParentId(0L);
    }
    dept.setAncestors(getAncestors(dept.getParentId()));  // 生成祖级列表
    baseMapper.insert(dept);
}
```

#### getAncestors() — 生成祖级路径
```java
private String getAncestors(Long parentId) {
    if (parentId == null || parentId == 0L) {
        return "0";
    }
    SysDept parent = baseMapper.selectById(parentId);
    if (parent != null) {
        String ancestors = parent.getAncestors();
        if (StrUtil.isNotBlank(ancestors)) {
            return ancestors + "," + parentId;  // 父级祖级 + 父级 ID
        }
        return "0," + parentId;
    }
    return "0";
}
```
例如：后端组（parentId=2 技术部）→ 技术部 ancestors="0,1" → 后端组 ancestors="0,1,2"

#### deleteDept() — 删除前检查
```java
@Override
@Transactional
public void deleteDept(Long id) {
    long childCount = baseMapper.selectCount(
            new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id));
    if (childCount > 0) {
        throw new BusinessException(ResultCode.DEPT_HAS_CHILDREN);
    }
    long userCount = userMapper.selectCount(
            new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeptId, id));
    if (userCount > 0) {
        throw new BusinessException(ResultCode.DEPT_HAS_USERS);
    }
    baseMapper.deleteById(id);
}
```

---

### 3.5 SysOperationLogServiceImpl.java — 操作日志业务实现

**路径**：`com.permission.system.service.impl.SysOperationLogServiceImpl`

```java
@Override
public IPage<SysOperationLog> pageLogs(long pageNum, long pageSize, String keyword,
                                        String module, String startDate, String endDate) {
    Page<SysOperationLog> page = new Page<>(pageNum, pageSize);
    LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();
    if (StrUtil.isNotBlank(keyword)) {
        wrapper.and(w -> w.like(SysOperationLog::getOperator, keyword)
                .or().like(SysOperationLog::getAction, keyword));
    }
    if (StrUtil.isNotBlank(module)) {
        wrapper.eq(SysOperationLog::getModule, module);
    }
    if (StrUtil.isNotBlank(startDate)) {
        wrapper.ge(SysOperationLog::getCreateTime, startDate);
    }
    if (StrUtil.isNotBlank(endDate)) {
        wrapper.le(SysOperationLog::getCreateTime, endDate + " 23:59:59");
    }
    wrapper.orderByDesc(SysOperationLog::getCreateTime);
    return baseMapper.selectPage(page, wrapper);
}
```

---

### 3.6 SysLoginLogServiceImpl.java — 登录日志业务实现

**路径**：`com.permission.system.service.impl.SysLoginLogServiceImpl`

```java
@Override
public IPage<SysLoginLog> pageLogs(long pageNum, long pageSize, String keyword,
                                    String startDate, String endDate) {
    Page<SysLoginLog> page = new Page<>(pageNum, pageSize);
    LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<>();
    if (StrUtil.isNotBlank(keyword)) {
        wrapper.and(w -> w.like(SysLoginLog::getUsername, keyword)
                .or().like(SysLoginLog::getIp, keyword));
    }
    if (StrUtil.isNotBlank(startDate)) {
        wrapper.ge(SysLoginLog::getLoginTime, startDate);
    }
    if (StrUtil.isNotBlank(endDate)) {
        wrapper.le(SysLoginLog::getLoginTime, endDate + " 23:59:59");
    }
    wrapper.orderByDesc(SysLoginLog::getLoginTime);
    return baseMapper.selectPage(page, wrapper);
}

@Override
public void recordLoginLog(String username, String ip, Integer status, String message) {
    SysLoginLog log = new SysLoginLog();
    log.setUsername(username);
    log.setIp(ip);
    log.setStatus(status);
    log.setMessage(message);
    log.setLoginTime(LocalDateTime.now());
    baseMapper.insert(log);
}
```

---

## 四、OperationLogAspect.java — 操作日志 AOP 切面

**路径**：`com.permission.system.aspect.OperationLogAspect`

### 使用的注解/技术
| 注解 | 来源 | 作用 |
|------|------|------|
| `@Slf4j` | Lombok | 日志 |
| `@Aspect` | Spring AOP | 声明为切面 |
| `@Component` | Spring | 声明为组件 |
| `@RequiredArgsConstructor` | Lombok | 构造器注入 |
| `@Around("@annotation(operationLog)")` | Spring AOP | 环绕通知，基于注解匹配 |

### 核心逻辑

```java
@Around("@annotation(operationLog)")
public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
    long startTime = System.currentTimeMillis();

    // 1. 初始化日志对象
    SysOperationLog logEntry = new SysOperationLog();
    logEntry.setModule(operationLog.module());                      // 模块名
    logEntry.setAction(operationLog.value());                       // 操作描述

    // 2. 获取方法签名
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    logEntry.setMethod(signature.getDeclaringTypeName() + "." + signature.getName());

    try {
        // 3. 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            logEntry.setRequestUrl(request.getRequestURI());
            logEntry.setRequestMethod(request.getMethod());
            logEntry.setOperatorIp(request.getRemoteAddr());
        }

        // 4. 记录请求参数
        logEntry.setRequestParams(JSONUtil.toJsonStr(joinPoint.getArgs()));

        // 5. 获取操作人
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            logEntry.setOperator(loginUser.getUsername());
        }

        // 6. 执行目标方法
        Object result = joinPoint.proceed();

        // 7. 记录成功结果
        logEntry.setExecuteTime(System.currentTimeMillis() - startTime);
        logEntry.setResponseResult(JSONUtil.toJsonStr(result));
        logEntry.setStatus(1);
        logEntry.setCreateTime(LocalDateTime.now());
        operationLogMapper.insert(logEntry);

        return result;
    } catch (Throwable e) {
        // 8. 记录失败结果
        logEntry.setExecuteTime(System.currentTimeMillis() - startTime);
        logEntry.setStatus(0);
        logEntry.setErrorMsg(e.getMessage());
        logEntry.setCreateTime(LocalDateTime.now());
        operationLogMapper.insert(logEntry);
        throw e;  // 重新抛出异常
    }
}
```

### 设计意图
- **非侵入式**：Controller 只需加 `@OperationLog(module = "xxx", value = "xxx")` 注解，无需手动写日志
- **环绕通知**：在方法执行前后分别记录开始时间和结束时间，计算执行耗时
- **异常处理**：无论方法成功或失败，都会记录日志，失败时记录错误信息

### 记录的日志字段
| 字段 | 来源 |
|------|------|
| module | 注解参数 |
| action | 注解参数 |
| method | 方法全路径 |
| requestUrl | HttpServletRequest |
| requestMethod | GET/POST/PUT/DELETE |
| requestParams | 方法参数 JSON |
| responseResult | 返回值 JSON（成功时） |
| executeTime | 耗时（ms） |
| operator | SecurityContext 中的用户名 |
| operatorIp | 请求远程地址 |
| status | 1-成功，0-失败 |
| errorMsg | 异常消息（失败时） |

---

## 五、DataInitializer.java — 数据初始化

**路径**：`com.permission.system.config.DataInitializer`

### 使用的注解/技术
| 注解 | 来源 | 作用 |
|------|------|------|
| `@Slf4j` | Lombok | 日志 |
| `@Component` | Spring | 声明为组件 |
| `@RequiredArgsConstructor` | Lombok | 构造器注入 |
| `implements CommandLineRunner` | Spring Boot | 应用启动后执行 |
| `@Transactional` | Spring | 事务管理 |

### 核心逻辑

```java
@Override
@Transactional
public void run(String... args) {
    // 检查是否已初始化
    if (userMapper.selectCount(new LambdaQueryWrapper<>()) > 0) {
        log.info("数据已初始化，跳过");
        return;
    }
    // 执行初始化...
}
```

### 初始化的数据

```
1. 部门（6 个）
   总公司(1)
   ├── 技术部(2)
   │   ├── 前端组(5)
   │   └── 后端组(6)
   ├── 产品部(3)
   └── 运营部(4)

2. 用户（3 个）
   admin / Admin@1234 → 超级管理员
   tech / Admin@1234 → 技术负责人
   backend / Admin@1234 → 后端开发

3. 角色（3 个）
   超级管理员 (data_scope=1, 全部数据)
   技术负责人 (data_scope=2, 本部门及子部门)
   普通用户 (data_scope=5, 仅本人)

4. 菜单（25 个）
   系统管理(目录)
   ├── 用户管理(菜单) → 用户查询/新增/编辑/删除/重置密码(按钮)
   ├── 角色管理(菜单) → 角色查询/新增/编辑/删除(按钮)
   ├── 菜单管理(菜单) → 菜单查询/新增/编辑/删除(按钮)
   ├── 部门管理(菜单) → 部门查询/新增/编辑/删除(按钮)
   └── 日志管理(目录) → 操作日志(菜单) + 登录日志(菜单)

5. 用户角色关联
   admin → 超级管理员
   tech → 技术负责人 + 普通用户
   backend → (无，仅 tech 有)

6. 角色菜单关联
   超级管理员 → 所有菜单
   技术负责人 → 系统管理 + 用户管理 + 查询按钮 + 角色管理 + 查询按钮 + 菜单管理 + 查询按钮 + 部门管理 + 查询按钮 + 日志管理
   普通用户 → 系统管理 + 用户管理 + 查询按钮 + 角色管理 + 查询按钮 + 菜单管理 + 查询按钮 + 部门管理 + 查询按钮
```

### 设计意图
- 应用启动时自动初始化基础数据，无需手动执行 SQL
- 幂等性检查：`selectCount > 0` 则跳过，避免重复插入
- 密码使用 `passwordEncoder.encode("Admin@1234")` 动态生成 BCrypt 哈希

---

## 六、UserDetailsServiceImpl.java — 用户加载实现

**路径**：`com.permission.system.security.UserDetailsServiceImpl`

### 使用的注解/技术
| 注解 | 来源 | 作用 |
|------|------|------|
| `@Slf4j` | Lombok | 日志 |
| `@Service` | Spring | 声明为 Service |
| `@RequiredArgsConstructor` | Lombok | 构造器注入 |
| `implements UserDetailsService, CustomUserDetailsService` | Spring Security / 项目 | 双接口实现 |

### 核心逻辑

#### loadUserByUsername() — Spring Security 登录时调用
```java
@Override
public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    SysUser user = userMapper.selectOne(
            new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
    if (user == null) {
        throw new UsernameNotFoundException("用户不存在: " + username);
    }
    if (user.getStatus() != null && user.getStatus() == UserStatus.DISABLED.getCode()) {
        throw new UsernameNotFoundException("账号已被禁用: " + username);
    }
    return buildLoginUser(user);
}
```

#### loadUserById() — JwtAuthenticationFilter 调用
```java
@Override
public LoginUser loadUserById(Long userId) {
    SysUser user = userMapper.selectById(userId);
    if (user == null) {
        return null;
    }
    return buildLoginUser(user);
}
```

#### buildLoginUser() — 构建用户详情
```java
private LoginUser buildLoginUser(SysUser user) {
    Set<String> permissions = new HashSet<>();
    Set<String> roles = new HashSet<>();

    // 1. 查询用户角色关联
    List<SysUserRole> userRoles = userRoleMapper.selectList(
            new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId()));
    List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).toList();

    if (CollUtil.isNotEmpty(roleIds)) {
        // 2. 查询角色信息（只取启用的）
        List<SysRole> roleList = roleMapper.selectBatchIds(roleIds);
        for (SysRole role : roleList) {
            if (role.getStatus() == 1) {
                roles.add(role.getRoleCode());  // 角色编码，如 "admin"
            }
        }

        // 3. 查询角色菜单关联 → 菜单信息 → 权限标识
        List<SysRoleMenu> roleMenus = new ArrayList<>();
        for (Long roleId : roleIds) {
            roleMenus.addAll(menuMapper.selectRoleMenusByRoleId(roleId));
        }
        List<Long> menuIds = roleMenus.stream().map(SysRoleMenu::getMenuId).distinct().toList();

        if (CollUtil.isNotEmpty(menuIds)) {
            List<SysMenu> menuList = menuMapper.selectBatchIds(menuIds);
            permissions.addAll(
                    menuList.stream()
                            .filter(m -> m.getPermission() != null && !m.getPermission().isEmpty())
                            .map(SysMenu::getPermission)
                            .collect(Collectors.toSet())
            );  // 权限标识，如 "system:user:list"
        }
    }

    // 4. 查询部门名称
    String deptName = "";
    if (user.getDeptId() != null) {
        SysDept dept = deptMapper.selectById(user.getDeptId());
        if (dept != null) {
            deptName = dept.getDeptName();
        }
    }

    // 5. 取最小 dataScope（权限最大）
    Integer dataScope = 5;
    if (CollUtil.isNotEmpty(roleIds)) {
        List<SysRole> roleList = roleMapper.selectBatchIds(roleIds);
        for (SysRole role : roleList) {
            if (role.getDataScope() != null && role.getDataScope() < dataScope) {
                dataScope = role.getDataScope();
            }
        }
    }

    // 6. 构建 LoginUser
    return LoginUser.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .password(user.getPassword())
            .nickname(user.getNickname() != null ? user.getNickname() : user.getUsername())
            .deptId(user.getDeptId())
            .deptName(deptName)
            .dataScope(dataScope)
            .deptIds(new ArrayList<>())
            .permissions(permissions)
            .roles(roles)
            .build();
}
```

### 数据加载流程
```
sys_user
    ↓ (userId)
sys_user_role → sys_role (status=1 的 role_code → roles)
    ↓ (roleIds)
sys_role_menu → sys_menu (permission → permissions)
    ↓ (deptId)
sys_dept (deptName)
    ↓
取最小 dataScope
    ↓
构建 LoginUser
```

---

## 七、模块总结

`permission-system` 是整个项目的**业务核心层**，提供了：

1. **数据访问**：8 个 Mapper 接口，其中 SysUserMapper 和 SysMenuMapper 包含自定义 SQL
2. **用户认证**：`UserDetailsServiceImpl` 加载用户权限信息
3. **登录流程**：`SysUserServiceImpl.login()` 实现 8 步安全登录（限流 → 验证码 → 锁定 → 认证 → Token → 缓存 → 更新 → 日志）
4. **CRUD 业务**：用户、角色、菜单、部门的完整 CRUD + 树形结构 + 唯一性校验
5. **操作日志**：`OperationLogAspect` 通过 AOP 自动记录所有标注 `@OperationLog` 的操作
6. **数据初始化**：`DataInitializer` 启动时自动插入基础数据（部门、用户、角色、菜单、关联）
package com.permission.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.permission.common.ResultCode;
import com.permission.common.constant.SecurityConstants;
import com.permission.common.dto.LoginDTO;
import com.permission.common.dto.LoginUser;
import com.permission.common.dto.ProfileDTO;
import com.permission.common.dto.TokenVO;
import com.permission.common.entity.*;
import com.permission.common.enums.UserStatus;
import com.permission.common.exception.BusinessException;
import com.permission.framework.security.JwtTokenProvider;
import com.permission.system.mapper.*;
import com.permission.system.service.OnlineUserService;
import com.permission.system.service.SysLoginLogService;
import com.permission.system.service.SysUserService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[~!@#$%^&*()_+\\-=\\[\\]{}|;:',.<>?/]).{8,}$");

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysDeptMapper deptMapper;
    private final SysLoginLogService loginLogService;
    private final OnlineUserService onlineUserService;

    @Override
    public TokenVO login(LoginDTO loginDTO) {
        String username = loginDTO.getUsername();

        // 1. Rate limit check (by IP)
        String clientIp = getClientIp();
        String rateLimitKey = SecurityConstants.LOGIN_RATE_LIMIT_PREFIX + clientIp;
        Long rateCount = redisTemplate.opsForValue().increment(rateLimitKey);
        if (rateCount == 1) {
            redisTemplate.expire(rateLimitKey, SecurityConstants.LOGIN_RATE_LIMIT_WINDOW, TimeUnit.SECONDS);
        }
        if (rateCount != null && rateCount > SecurityConstants.LOGIN_RATE_LIMIT_MAX) {
            throw new BusinessException(ResultCode.RATE_LIMITED);
        }

        // 2. Captcha check
        if (StrUtil.isNotBlank(loginDTO.getCaptchaKey()) || StrUtil.isNotBlank(loginDTO.getCaptchaCode())) {
            String captchaKey = loginDTO.getCaptchaKey();
            String correctCode = (String) redisTemplate.opsForValue().get(SecurityConstants.CAPTCHA_PREFIX + captchaKey);
            if (StrUtil.isBlank(correctCode)) {
                throw new BusinessException(ResultCode.CAPTCHA_ERROR);
            }
            if (!correctCode.equalsIgnoreCase(loginDTO.getCaptchaCode())) {
                throw new BusinessException(ResultCode.CAPTCHA_ERROR);
            }
            redisTemplate.delete(SecurityConstants.CAPTCHA_PREFIX + captchaKey);
        }

        // 3. Account lockout check
        String failKey = SecurityConstants.LOGIN_FAIL_PREFIX + username;
        Object failCountObj = redisTemplate.opsForValue().get(failKey);
        int failCount = failCountObj instanceof Integer ? (Integer) failCountObj : 0;
        if (failCount >= SecurityConstants.MAX_LOGIN_FAIL_COUNT) {
            throw new BusinessException(ResultCode.ACCOUNT_TEMP_LOCKED);
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, loginDTO.getPassword()));

            LoginUser loginUser = (LoginUser) authentication.getPrincipal();

            Map<String, Object> claims = new HashMap<>();
            claims.put("permissions", loginUser.getPermissions());
            claims.put("roles", loginUser.getRoles());

            String accessToken = jwtTokenProvider.createAccessToken(loginUser.getUserId(), loginUser.getUsername(), claims);
            String refreshToken = jwtTokenProvider.createRefreshToken(loginUser.getUserId());

            redisTemplate.opsForValue().set(
                    SecurityConstants.TOKEN_CACHE_PREFIX + accessToken,
                    loginUser,
                    SecurityConstants.TOKEN_EXPIRE,
                    TimeUnit.SECONDS);

            SysUser user = new SysUser();
            user.setId(loginUser.getUserId());
            user.setLastLoginTime(java.time.LocalDateTime.now());
            baseMapper.updateById(user);

            // Clear fail count on success
            redisTemplate.delete(failKey);

            loginLogService.recordLoginLog(loginUser.getUsername(), clientIp, 1, "登录成功");

            // 记录在线状态
            onlineUserService.userOnline(loginUser.getUserId(), loginUser.getUsername(),
                    loginUser.getNickname(), clientIp);

            return TokenVO.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(SecurityConstants.TOKEN_EXPIRE)
                    .build();

        } catch (BusinessException e) {
            throw e;
        } catch (org.springframework.security.core.AuthenticationException e) {
            // Increment fail count
            redisTemplate.opsForValue().increment(failKey);
            redisTemplate.expire(failKey, SecurityConstants.ACCOUNT_LOCK_MINUTES, TimeUnit.MINUTES);
            loginLogService.recordLoginLog(username, clientIp, 0, e.getMessage());
            throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }
    }

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

            Map<String, Object> tokenClaims = new HashMap<>();
            String newAccessToken = jwtTokenProvider.createAccessToken(userId, user.getUsername(), tokenClaims);
            String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);

            return TokenVO.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .expiresIn(SecurityConstants.TOKEN_EXPIRE)
                    .build();

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new BusinessException(ResultCode.TOKEN_EXPIRED);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }
    }

    @Override
    public void logout(String accessToken) {
        if (StrUtil.isNotBlank(accessToken)) {
            // Delete cached user info
            redisTemplate.delete(SecurityConstants.TOKEN_CACHE_PREFIX + accessToken);

            // Remove online status
            try {
                Long userId = jwtTokenProvider.getUserId(accessToken);
                if (userId != null) {
                    onlineUserService.forceLogout(userId);
                }
            } catch (Exception e) {
                log.warn("Failed to remove online status: {}", e.getMessage());
            }

            // Blacklist the access token
            try {
                long remaining = jwtTokenProvider.getExpiration(accessToken) - System.currentTimeMillis();
                if (remaining > 1000) {  // Only blacklist if more than 1 second remains
                    // Use seconds precision, not milliseconds
                    long remainingSeconds = Math.max(remaining / 1000, 1);
                    redisTemplate.opsForValue().set(
                            SecurityConstants.TOKEN_BLACKLIST_PREFIX + accessToken,
                            "1",
                            remainingSeconds,
                            TimeUnit.SECONDS);
                }
            } catch (Exception e) {
                log.warn("Failed to blacklist token: {}", e.getMessage());
            }
        }
    }

    @Override
    public IPage<SysUser> pageUsers(long pageNum, long pageSize, String keyword, Long deptId, Integer status) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();

        if (StrUtil.isNotBlank(keyword)) {
            // Escape LIKE special chars to prevent wildcard abuse
            String safeKeyword = keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            wrapper.and(w -> w.like(SysUser::getUsername, safeKeyword)
                    .or().like(SysUser::getNickname, safeKeyword)
                    .or().like(SysUser::getPhone, safeKeyword));
        }
        if (deptId != null) {
            wrapper.eq(SysUser::getDeptId, deptId);
        }
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }
        wrapper.orderByAsc(SysUser::getDeptId).orderByDesc(SysUser::getCreateTime);

        IPage<SysUser> result = baseMapper.selectPage(page, wrapper);
        List<SysUser> users = result.getRecords();
        if (CollUtil.isNotEmpty(users)) {
            Set<Long> deptIds = users.stream().map(SysUser::getDeptId).filter(Objects::nonNull).collect(Collectors.toSet());
            if (CollUtil.isNotEmpty(deptIds)) {
                List<SysDept> depts = deptMapper.selectBatchIds(deptIds);
                Map<Long, String> deptMap = depts.stream().collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName));
                users.forEach(u -> {
                    if (u.getDeptId() != null) {
                        u.setDeptName(deptMap.getOrDefault(u.getDeptId(), ""));
                    }
                    u.setPassword(null); // Never serialize password hash in response
                });
            }
        }
        return result;
    }

    @Override
    public SysUser getUserById(Long id) {
        SysUser user = baseMapper.selectById(id);
        if (user != null && user.getDeptId() != null) {
            SysDept dept = deptMapper.selectById(user.getDeptId());
            if (dept != null) {
                user.setDeptName(dept.getDeptName());
            }
        }
        if (user != null) {
            user.setPassword(null); // Never serialize password hash in response
        }
        return user;
    }

    @Override
    @Transactional
    public void createUser(SysUser user) {
        if (existsByUsername(user.getUsername())) {
            throw new BusinessException(1007, "用户名已存在");
        }
        validatePassword(user.getPassword());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(UserStatus.ENABLED.getCode());
        baseMapper.insert(user);
    }

    @Override
    @Transactional
    public void updateUser(SysUser user) {
        SysUser existing = baseMapper.selectById(user.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (!existing.getUsername().equals(user.getUsername()) && existsByUsername(user.getUsername())) {
            throw new BusinessException(1007, "用户名已存在");
        }
        user.setPassword(null);
        baseMapper.updateById(user);
    }

    @Override
    @Transactional
    public void deleteUsers(List<Long> ids) {
        if (CollUtil.isNotEmpty(ids)) {
            baseMapper.deleteBatchIds(ids);
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getUserId, ids));
        }
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        baseMapper.updateById(user);
    }

    @Override
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        validatePassword(newPassword);
        SysUser user = new SysUser();
        user.setId(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        baseMapper.updateById(user);
    }

    @Override
    @Transactional
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = baseMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);
        }
        validatePassword(newPassword);
        SysUser updateUser = new SysUser();
        updateUser.setId(userId);
        updateUser.setPassword(passwordEncoder.encode(newPassword));
        baseMapper.updateById(updateUser);
    }

    @Override
    @Transactional
    public void assignRoles(Long userId, Set<Long> roleIds) {
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (CollUtil.isNotEmpty(roleIds)) {
            for (Long roleId : roleIds) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                userRoleMapper.insert(userRole);
            }
        }
    }

    @Override
    public Set<Long> getUserRoleIds(Long userId) {
        List<SysUserRole> list = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        return list.stream().map(SysUserRole::getRoleId).collect(Collectors.toSet());
    }

    @Override
    public void updateProfile(Long userId, ProfileDTO profileDTO) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (profileDTO.getNickname() != null) {
            user.setNickname(profileDTO.getNickname());
        }
        if (profileDTO.getEmail() != null) {
            user.setEmail(profileDTO.getEmail());
        }
        if (profileDTO.getPhone() != null) {
            user.setPhone(profileDTO.getPhone());
        }
        if (profileDTO.getSex() != null) {
            user.setSex(profileDTO.getSex());
        }
        updateById(user);
    }

    @Override
    public void exportUsers(HttpServletResponse response) throws IOException {
        List<SysUser> users = list(new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeleted, 0).orderByDesc(SysUser::getId));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (SysUser u : users) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("用户ID", u.getId());
            row.put("用户名", u.getUsername());
            row.put("昵称", u.getNickname());
            row.put("部门", u.getDeptName() != null ? u.getDeptName() : "");
            row.put("邮箱", u.getEmail() != null ? u.getEmail() : "");
            row.put("手机号", u.getPhone() != null ? u.getPhone() : "");
            row.put("性别", u.getSex() != null ? (u.getSex() == 1 ? "男" : u.getSex() == 2 ? "女" : "保密") : "保密");
            row.put("状态", u.getStatus() != null && u.getStatus() == 1 ? "启用" : "禁用");
            row.put("最后登录", u.getLastLoginTime() != null ? u.getLastLoginTime() : "");
            row.put("创建时间", u.getCreateTime() != null ? u.getCreateTime().toString() : "");
            rows.add(row);
        }

        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        String fileName = java.net.URLEncoder.encode("用户列表_" + System.currentTimeMillis(), "UTF-8");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");

        cn.hutool.poi.excel.ExcelWriter writer = cn.hutool.poi.excel.ExcelUtil.getWriter(true);
        writer.write(rows, true);
        writer.flush(response.getOutputStream());
        writer.close();
    }

    @Override
    public void batchUpdateStatus(List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        SysUser user = new SysUser();
        user.setStatus(status);
        update(user, new LambdaQueryWrapper<SysUser>().in(SysUser::getId, ids));
    }

    private void validatePassword(String password) {
        if (StrUtil.isBlank(password) || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BusinessException(ResultCode.PASSWORD_WEAK);
        }
    }

    private String getClientIp() {
        // NOTE: This trusts X-Forwarded-For / X-Real-IP headers. In production, ensure a trusted
        // reverse proxy (e.g. Nginx) is configured to overwrite these headers, otherwise a client
        // can spoof arbitrary IPs. Consider using ForwardedHeaderFilter or a allowlist of proxy IPs.
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "unknown";
        }
        jakarta.servlet.http.HttpServletRequest request = attrs.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private boolean existsByUsername(String username) {
        return baseMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)) > 0;
    }
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.system.service.impl.SysUserServiceImpl
// 【模块】permission-system
//
// 【使用的注解/技术】
//   - @Slf4j — Lombok，注入日志对象
//   - @Service — Spring，声明业务层组件
//   - @RequiredArgsConstructor — Lombok，生成必需参数构造器（构造器注入）
//   - @Override — Java，标识重写接口/父类方法
//   - ServiceImpl<SysUserMapper, SysUser> — MyBatis-Plus，继承通用 Service 实现基类
//   - @Transactional — Spring，声明式事务，保证用户写操作与角色/关联清理的原子性
//   - LambdaQueryWrapper / Page / IPage — MyBatis-Plus，类型安全查询与分页
//   - Pattern — JDK 正则，密码强度校验
//   - AuthenticationManager — Spring Security，执行用户名/密码认证
//   - UsernamePasswordAuthenticationToken — Spring Security，认证 token
//   - RedisTemplate — Spring Data Redis，实现限流/验证码/Token 缓存/黑名单
//   - JwtTokenProvider（permission-framework 模块） → JWT 创建与解析
//   - TimeUnit — JDK，Redis 过期时间单位
//   - RequestContextHolder / ServletRequestAttributes — Spring，获取客户端 IP
//   - SecurityConstants — 安全常量（限流/验证码/Token/黑名单前缀与参数）
//   - ResultCode / UserStatus / BusinessException — 业务枚举与异常
//
// 【关键依赖】
//   - 依赖 AuthenticationManager → 执行 Spring Security 认证
//   - 依赖 JwtTokenProvider → 创建/解析 AccessToken 与 RefreshToken
//   - 依赖 PasswordEncoder（BCrypt） → 密码加密与匹配
//   - 依赖 RedisTemplate → 限流计数、验证码缓存、Token 缓存、账号锁定计数、Token 黑名单
//   - 依赖 SysUserRoleMapper / SysRoleMapper / SysMenuMapper / SysDeptMapper /
//     SysUserMapper → 用户 CRUD 与关联操作
//   - 依赖 SysLoginLogService → 写入登录日志
//   - 依赖 SecurityConstants → 各类安全常量（前缀、过期时间、限流参数）
//
// 【关联文件】
//   - 被 AuthController 调用（login / refresh / logout）
//   - 被 UserController 调用（用户 CRUD / 状态 / 密码 / 角色）
//   - 被 OperationLogAspect 切面拦截（带 @OperationLog 的用户操作记录日志）
//   - 依赖 permission-framework 模块的 JwtTokenProvider 与 CustomUserDetailsService
//
// 【核心作用】
//   用户业务服务实现，提供完整的认证生命周期：
//   1. 登录限流（Redis 累加，超阈值抛 RATE_LIMITED）
//   2. 验证码校验（Redis 存储，验证后删除）
//   3. 账号锁定检查（失败计数，超阈值抛 ACCOUNT_TEMP_LOCKED）
//   4. AuthenticationManager 用户名密码认证
//   5. 校验通过后生成 AccessToken + RefreshToken 并缓存 Token
//   6. 更新登录时间，清除失败计数，写入成功登录日志
//   7. 密码错误时累加失败计数（按 ACCOUNT_LOCK_MINUTES 窗口），写入失败日志
//   8. Token 刷新与登出（含 TTL 精确的 Redis 黑名单）
//   以及用户 CRUD、状态管理、密码策略校验、角色分配等业务。
//
// 【设计必要性】
//   登录流程涉及多步安全策略限流、验证码、账号锁定、Token 签发与黑名单，集中在一个
//   Service 实现中保证流程清晰、事务一致，便于维护与扩展（如加多因子认证）。
//
// 【注意事项/安全提示】
//   - 密码 field 返回前必须置 null：pageUsers 与 getUserById 中将 password 置 null，
//     防止密码 hash 序列化到前端
//   - blacklist TTL 修复：登出时黑名单 TTL 使用毫秒精度（剩余秒数），避免 Token 过期后空占 Redis
//   - LIKE 查询已转义通配符：pageUsers 中 keyword 的 /%/_ 被拼接为转义形式，防 LIKE 注入
//   - 密码强度：正则要求 8 位以上，包含大小写、数字、特殊字符
//   - 账号锁定通过 Redis 计数实现，锁定窗口到期自动清除
//   - getClientIp 取 X-Forwarded-For 等头，生产环境必须由可信反向代理覆盖这些头防客户端伪造
//   - 变更密码/重置密码/创建用户均先 validatePassword，保障全链路密码策略一致
// ============================================================

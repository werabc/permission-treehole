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
    private final com.permission.system.support.DataScopeGuard dataScopeGuard;

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

        // 2. Captcha check — 强制校验，不可绕过
        String captchaKey = loginDTO.getCaptchaKey();
        String captchaCode = loginDTO.getCaptchaCode();
        if (StrUtil.isBlank(captchaKey) || StrUtil.isBlank(captchaCode)) {
            throw new BusinessException(ResultCode.CAPTCHA_ERROR, "请完成验证码");
        }
        String correctCode = (String) redisTemplate.opsForValue().get(SecurityConstants.CAPTCHA_PREFIX + captchaKey);
        if (StrUtil.isBlank(correctCode)) {
            throw new BusinessException(ResultCode.CAPTCHA_ERROR, "验证码已过期，请刷新");
        }
        if (!correctCode.equalsIgnoreCase(captchaCode)) {
            throw new BusinessException(ResultCode.CAPTCHA_ERROR, "验证码错误");
        }
        redisTemplate.delete(SecurityConstants.CAPTCHA_PREFIX + captchaKey);

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

            // Alibaba-Java: 安全规约 — 缓存前清除密码hash，防止Redis泄露导致凭证暴露
            LoginUser cacheUser = LoginUser.builder()
                    .userId(loginUser.getUserId())
                    .username(loginUser.getUsername())
                    .nickname(loginUser.getNickname())
                    .deptId(loginUser.getDeptId())
                    .deptName(loginUser.getDeptName())
                    .dataScope(loginUser.getDataScope())
                    .deptIds(loginUser.getDeptIds())
                    .permissions(loginUser.getPermissions())
                    .roles(loginUser.getRoles())
                    .build();
            redisTemplate.opsForValue().set(
                    SecurityConstants.TOKEN_CACHE_PREFIX + accessToken,
                    cacheUser,
                    SecurityConstants.TOKEN_EXPIRE,
                    TimeUnit.SECONDS);

            SysUser user = new SysUser();
            user.setId(loginUser.getUserId());
            user.setLastLoginTime(java.time.LocalDateTime.now());
            baseMapper.updateById(user);

            // Clear fail count on success
            redisTemplate.delete(failKey);

            loginLogService.recordLoginLog(loginUser.getUsername(), clientIp, 1, "登录成功");

            // 注：管理员登录不记录在线状态，在线用户仅统计树洞端（ThUserServiceImpl 中记录）

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
        // 数据权限：拦截器会把数据范围拼进 SQL（返回 0 行），但不能区分"不存在"与"无权访问"。
        // 这里先做一次显式断言，把越权读变成明确的 403，而不是静默返回 null。
        dataScopeGuard.assertUserReadable(id);
        SysUser user = baseMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (user.getDeptId() != null) {
            SysDept dept = deptMapper.selectById(user.getDeptId());
            if (dept != null) {
                user.setDeptName(dept.getDeptName());
            }
        }
        user.setPassword(null); // Never serialize password hash in response
        return user;
    }

    @Override
    @Transactional
    public void createUser(SysUser user) {
        if (existsByUsername(user.getUsername())) {
            throw new BusinessException(ResultCode.DATA_EXISTS, "用户名已存在");
        }
        validatePassword(user.getPassword());
        // 数据权限：只能在有权管辖的部门下创建用户
        dataScopeGuard.assertUserDeptInScope(user.getDeptId());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(UserStatus.ENABLED.getCode());
        baseMapper.insert(user);
    }

    @Override
    @Transactional
    public void updateUser(SysUser user) {
        // 数据权限：先断言再读，否则被拦截器过滤后会误报"用户不存在"而掩盖越权
        dataScopeGuard.assertUserWritable(user.getId());
        SysUser existing = baseMapper.selectById(user.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        // 数据权限：不允许把用户迁移到看不见的部门
        dataScopeGuard.assertUserDeptInScope(user.getDeptId());
        if (!existing.getUsername().equals(user.getUsername()) && existsByUsername(user.getUsername())) {
            throw new BusinessException(ResultCode.DATA_EXISTS, "用户名已存在");
        }
        user.setPassword(null);
        baseMapper.updateById(user);
    }

    @Override
    @Transactional
    public void deleteUsers(List<Long> ids) {
        if (CollUtil.isNotEmpty(ids)) {
            // 数据权限：批量删除前逐个校验，任一越权即整体拒绝
            dataScopeGuard.assertUsersWritable(ids);
            baseMapper.deleteBatchIds(ids);
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getUserId, ids));
        }
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        // 数据权限：不能禁用/启用范围外用户
        dataScopeGuard.assertUserWritable(id);
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        baseMapper.updateById(user);
    }

    @Override
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        // 数据权限：重置密码是高危操作，必须校验归属（否则可越权重置超管密码）
        dataScopeGuard.assertUserWritable(id);
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
        // 数据权限：给他人分配角色是垂直提权的最短路径，必须校验归属
        dataScopeGuard.assertUserWritable(userId);
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
        // 数据权限：批量改状态前逐个校验，任一越权即整体拒绝
        dataScopeGuard.assertUsersWritable(ids);
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


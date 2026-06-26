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
import com.permission.common.dto.TokenVO;
import com.permission.common.entity.*;
import com.permission.common.enums.UserStatus;
import com.permission.common.exception.BusinessException;
import com.permission.framework.security.JwtTokenProvider;
import com.permission.system.mapper.*;
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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
            // Blacklist the access token
            try {
                long remaining = jwtTokenProvider.getExpiration(accessToken) - System.currentTimeMillis();
                if (remaining > 0) {
                    redisTemplate.opsForValue().set(
                            SecurityConstants.TOKEN_BLACKLIST_PREFIX + accessToken,
                            "1",
                            remaining,
                            TimeUnit.MILLISECONDS);
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
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getNickname, keyword)
                    .or().like(SysUser::getPhone, keyword));
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

    private void validatePassword(String password) {
        if (StrUtil.isBlank(password) || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BusinessException(ResultCode.PASSWORD_WEAK);
        }
    }

    private String getClientIp() {
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

package com.permission.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.permission.common.dto.LoginDTO;
import com.permission.common.dto.ProfileDTO;
import com.permission.common.dto.TokenVO;
import com.permission.common.entity.SysUser;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Set;

public interface SysUserService extends IService<SysUser> {

    TokenVO login(LoginDTO loginDTO);

    TokenVO refreshToken(String refreshToken);

    void logout(String token);

    IPage<SysUser> pageUsers(long pageNum, long pageSize, String keyword, Long deptId, Integer status);

    SysUser getUserById(Long id);

    void createUser(SysUser user);

    void updateUser(SysUser user);

    void deleteUsers(List<Long> ids);

    void updateStatus(Long id, Integer status);

    void resetPassword(Long id, String newPassword);

    void updatePassword(Long userId, String oldPassword, String newPassword);

    void assignRoles(Long userId, Set<Long> roleIds);

    Set<Long> getUserRoleIds(Long userId);

    void updateProfile(Long userId, ProfileDTO profileDTO);

    void exportUsers(HttpServletResponse response) throws java.io.IOException;

    void batchUpdateStatus(List<Long> ids, Integer status);
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.system.service.SysUserService
// 【模块】permission-system
//
// 【使用的注解/技术】
//   - IService<SysUser> — MyBatis-Plus，继承通用 Service 接口，提供用户基础 CRUD 能力
//   - IPage / Set / List — MyBatis-Plus / JDK，分页查询与集合返回类型
//
// 【关键依赖】
//   - 依赖 SysUser 实体 → 用户业务操作的数据载体
//   - 依赖 LoginDTO → 登录请求参数（用户名/密码/验证码等）
//   - 依赖 TokenVO → Token 响应载体（access/refresh token + 过期时间）
//
// 【关联文件】
//   - 被 SysUserServiceImpl 实现，封装用户业务逻辑（登录 8 步流程、密码策略、角色分配等）
//   - 被 UserController / AuthController 调用，提供用户管理与认证 API
//   - 被 OperationLogAspect 切面部分拦截（登录/关键用户操作记录日志）
//
// 【核心作用】
//   用户业务服务接口，涵盖完整的认证生命周期（登录/刷新 Token/登出）和用户管理 CRUD
//   （分页查询/详情/增删改/状态管理/密码重置/密码修改/角色分配）。
//
// 【设计必要性】
//   接口与实现分离，便于在实现层集中管理密码策略、登录限流、账号锁定、Token 黑名单等
//   安全细节，接口层仅定义契约。
//
// 【注意事项/安全提示】
//   - 密码重置（resetPassword）实现层校验密码强度
//   - 修改密码（updatePassword）实现层校验旧密码匹配
//   - 分页查询返回前实现层将所有用户密码字段置 null，防止密码 hash 泄露到前端
// ============================================================

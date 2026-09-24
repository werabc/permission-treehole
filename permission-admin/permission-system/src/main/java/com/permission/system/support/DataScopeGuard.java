package com.permission.system.support;

import com.permission.common.ResultCode;
import com.permission.common.dto.LoginUser;
import com.permission.common.entity.SysDept;
import com.permission.common.entity.SysUser;
import com.permission.common.exception.BusinessException;
import com.permission.system.mapper.SysDeptMapper;
import com.permission.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 写操作数据权限守门员
 *
 * 背景：MyBatis-Plus 的 DataPermissionInterceptor 只改写 SELECT，
 * 写操作（UPDATE/DELETE）此前没有任何数据范围校验，导致"看得见的管不了、
 * 看不见的反而改得了"——一个本部门管理员可以重置超管密码、给他人分配 admin 角色。
 *
 * 本类把读侧已有的可见范围判定（{@link DataScopeHelper}）补到写侧：
 * 所有写方法在进入业务逻辑前，先调用这里的 assert 方法做归属校验。
 *
 * 设计原则：
 *  1. **读写同源**：判定逻辑全部委托 DataScopeHelper，读不看得见 ⇒ 写也写不了
 *  2. **默认拒绝**：无登录态一律拒绝（写操作必须带登录用户）
 *  3. **超管豁免**：data_scope = 全部数据 直接放行（admin 持有 admin 权限码）
 *  4. **超管账号保护**：除 admin 本人外，任何人不得改动内置超管账号，防止系统被锁死
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataScopeGuard {

    /** 内置超管用户 ID：注册后由 DataInitializer 固定为 1 */
    public static final Long BUILTIN_ADMIN_ID = 1L;

    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;

    /**
     * 断言当前用户有权**读**目标用户详情
     *
     * 为什么读侧也需要显式断言：
     *   MyBatis-Plus 的 DataPermissionInterceptor 只对 selectList/selectPage 这类
     *   会生成 WHERE 的语句追加条件；**selectById（主键快路径）不会被拦截**，
     *   于是"列表里看不见、直接按 id 却读得到"——这是比写越权更隐蔽的读越权。
     *   凡是走 selectById 读单条实体的地方，都必须显式调用本方法。
     */
    public void assertUserReadable(Long targetUserId) {
        assertUserReadable(targetUserId, currentLoginUser());
    }

    public void assertUserReadable(Long targetUserId, LoginUser operator) {
        if (targetUserId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "目标用户ID不能为空");
        }
        if (operator == null) {
            throw new BusinessException(ResultCode.FORBIDDEN, "未登录，禁止访问");
        }
        SysUser target = userMapper.selectRawById(targetUserId);
        if (target == null || (target.getDeleted() != null && target.getDeleted() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        // 内置超管账号对他人不可见（避免被越权探测到完整信息）
        if (BUILTIN_ADMIN_ID.equals(targetUserId)
                && !BUILTIN_ADMIN_ID.equals(operator.getUserId())
                && com.permission.common.enums.DataScope.of(operator.getDataScope())
                   != com.permission.common.enums.DataScope.ALL) {
            throw new BusinessException(ResultCode.DATA_FORBIDDEN);
        }
        if (!DataScopeHelper.canWriteUser(operator, targetUserId, target.getDeptId())) {
            log.warn("[数据权限] 越权读用户被拒 operator={} scope={} target={}",
                    operator.getUserId(), operator.getDataScope(), targetUserId);
            throw new BusinessException(ResultCode.DATA_FORBIDDEN);
        }
    }

    /**
     * 断言当前用户有权**读**目标部门详情（同样绕不过 selectById 快路径）
     */
    public void assertDeptReadable(Long targetDeptId) {
        assertDeptReadable(targetDeptId, currentLoginUser());
    }

    public void assertDeptReadable(Long targetDeptId, LoginUser operator) {
        if (targetDeptId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "目标部门ID不能为空");
        }
        if (operator == null) {
            throw new BusinessException(ResultCode.FORBIDDEN, "未登录，禁止访问");
        }
        SysDept target = deptMapper.selectRawById(targetDeptId);
        if (target == null || (target.getDeleted() != null && target.getDeleted() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "部门不存在");
        }
        if (!DataScopeHelper.canWriteDept(operator, targetDeptId)) {
            log.warn("[数据权限] 越权读部门被拒 operator={} scope={} target={}",
                    operator.getUserId(), operator.getDataScope(), targetDeptId);
            throw new BusinessException(ResultCode.DATA_FORBIDDEN);
        }
    }

    // ==================== 用户写操作 ====================

    /**
     * 断言当前用户有权写目标用户（改/删/禁用/重置密码/分配角色）
     *
     * @throws BusinessException DATA_FORBIDDEN —— 目标不在可见范围内，或试图改动内置超管
     */
    public void assertUserWritable(Long targetUserId) {
        assertUserWritable(targetUserId, currentLoginUser());
    }

    /** 可注入登录用户的版本，便于单测与批量校验复用 */
    public void assertUserWritable(Long targetUserId, LoginUser operator) {
        if (targetUserId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "目标用户ID不能为空");
        }
        if (operator == null) {
            throw new BusinessException(ResultCode.FORBIDDEN, "未登录，禁止写操作");
        }

        SysUser target = userMapper.selectRawById(targetUserId);
        if (target == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (target.getDeleted() != null && target.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        // 内置超管账号保护：除本人外任何人不得改动，避免越权者禁用/删除超管导致系统锁死
        if (BUILTIN_ADMIN_ID.equals(targetUserId)
                && !BUILTIN_ADMIN_ID.equals(operator.getUserId())) {
            log.warn("[数据权限] 拒绝改动内置超管账号 operator={} target={}",
                    operator.getUserId(), targetUserId);
            throw new BusinessException(ResultCode.DATA_FORBIDDEN, "内置超级管理员账号不可由他人操作");
        }

        if (!DataScopeHelper.canWriteUser(operator, targetUserId, target.getDeptId())) {
            log.warn("[数据权限] 越权写用户被拒 operator={} scope={} target={} targetDept={}",
                    operator.getUserId(), operator.getDataScope(), targetUserId, target.getDeptId());
            throw new BusinessException(ResultCode.DATA_FORBIDDEN);
        }
    }

    /**
     * 批量写用户校验：任一目标越权即整体拒绝（fail-fast，避免部分成功造成数据不一致）
     */
    public void assertUsersWritable(Iterable<Long> targetUserIds) {
        LoginUser operator = currentLoginUser();
        if (targetUserIds == null) return;
        for (Long id : targetUserIds) {
            assertUserWritable(id, operator);
        }
    }

    /**
     * 断言新建/迁移用户时，目标部门是当前用户有权管辖的
     * （防止把用户创建到看不见的部门，或把人迁移出管辖范围）
     */
    public void assertUserDeptInScope(Long targetDeptId) {
        LoginUser operator = currentLoginUser();
        if (operator == null) {
            throw new BusinessException(ResultCode.FORBIDDEN, "未登录，禁止写操作");
        }
        if (targetDeptId == null) return;   // 允许不指定部门

        if (!DataScopeHelper.canWriteUser(operator, null, targetDeptId)) {
            log.warn("[数据权限] 越权把用户归入部门被拒 operator={} targetDept={}",
                    operator.getUserId(), targetDeptId);
            throw new BusinessException(ResultCode.DATA_FORBIDDEN, "无权在目标部门下操作用户");
        }
    }

    // ==================== 部门写操作 ====================

    /**
     * 断言当前用户有权写目标部门（改/删）
     */
    public void assertDeptWritable(Long targetDeptId) {
        assertDeptWritable(targetDeptId, currentLoginUser());
    }

    public void assertDeptWritable(Long targetDeptId, LoginUser operator) {
        if (targetDeptId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "目标部门ID不能为空");
        }
        if (operator == null) {
            throw new BusinessException(ResultCode.FORBIDDEN, "未登录，禁止写操作");
        }

        SysDept target = deptMapper.selectRawById(targetDeptId);
        if (target == null || (target.getDeleted() != null && target.getDeleted() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "部门不存在");
        }

        if (!DataScopeHelper.canWriteDept(operator, targetDeptId)) {
            log.warn("[数据权限] 越权写部门被拒 operator={} scope={} targetDept={}",
                    operator.getUserId(), operator.getDataScope(), targetDeptId);
            throw new BusinessException(ResultCode.DATA_FORBIDDEN);
        }
    }

    /**
     * 断言当前用户有权在指定父部门下新建部门
     * @param parentId 父部门ID，0/null 表示根节点
     */
    public void assertDeptParentInScope(Long parentId) {
        LoginUser operator = currentLoginUser();
        if (operator == null) {
            throw new BusinessException(ResultCode.FORBIDDEN, "未登录，禁止写操作");
        }
        if (parentId == null || parentId == 0L) {
            // 挂到根节点 = 创建顶级部门，只有全部数据范围才允许
            if (com.permission.common.enums.DataScope.of(operator.getDataScope())
                    != com.permission.common.enums.DataScope.ALL) {
                throw new BusinessException(ResultCode.DATA_FORBIDDEN, "无权创建顶级部门");
            }
            return;
        }
        assertDeptWritable(parentId);
    }

    // ==================== 工具 ====================

    /** 从 SecurityContext 取当前登录用户；未登录返回 null */
    public LoginUser currentLoginUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return null;
            Object principal = auth.getPrincipal();
            return principal instanceof LoginUser ? (LoginUser) principal : null;
        } catch (Exception e) {
            return null;
        }
    }
}

package com.permission.system.support;

import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.permission.common.dto.LoginUser;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 数据权限处理器 —— 挂在 MyBatis-Plus DataPermissionInterceptor 上，对白名单表自动追加数据范围条件
 *
 * 生效范围：sys_user（dept_id）、sys_dept（id），见 {@link DataScopeHelper#isScopedTable}
 *
 * 安全策略（宁可不拦截也不破坏 SQL）：
 *  - 无登录态（登录请求、定时任务、系统初始化）→ 不追加条件
 *  - data_scope = 全部数据 → 不追加条件
 *  - 解析异常 → 记错误日志并放行，避免因权限插件导致整个系统不可用
 *
 * 已知边界：MyBatis-Plus 的 DataPermissionInterceptor 只改写查询语句（SELECT），
 * 写操作（UPDATE/DELETE）的越权防护由 {@link DataScopeGuard} 在 Service 层显式校验承担。
 *
 * ⚠️ 豁免清单（{@link #BYPASS_METHODS}）：
 *   拦截器对**所有**命中白名单表的 SELECT 一视同仁，包括 Mapper 里自定义的原生 SQL。
 *   守门员 {@link DataScopeGuard} 需要"绕过数据范围"地读一次目标行，才能区分
 *   "数据不存在(404)" 与 "越权访问(403)"——若这次读取也被改写，两个场景都会变成
 *   0 行，守卫就无法给出正确语义。因此这里按 mappedStatementId 精确豁免。
 *   豁免仅限判定用的单行读，业务查询严禁走豁免路径。
 */
@Slf4j
@Component
public class DataPermissionHandler implements MultiDataPermissionHandler {

    /**
     * 不追加数据范围条件的语句（按 Mapper 方法名匹配，忽略全限定前缀）
     * 只允许放行"用于权限判定/系统内部"的单行读，禁止把业务查询加进来。
     */
    private static final Set<String> BYPASS_METHODS = Set.of(
            "selectRawById",              // 守门员判定目标用户/部门是否存在、拿 dept_id
            "selectRoleCodesByUserId",    // 登录时装载角色码（此时尚无完整登录态）
            "selectPermissionsByUserId"   // 登录时装载权限码
    );

    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
        try {
            if (table == null) return null;
            // 豁免：守门员判定用的原生单行读，不能被追加数据范围
            if (isBypassed(mappedStatementId)) {
                log.debug("[数据权限] 豁免语句 {} 不追加条件", mappedStatementId);
                return null;
            }
            String condition = DataScopeHelper.buildCondition(table.getName(), currentLoginUser());
            if (condition == null) return null;

            log.debug("[数据权限] 表 {} 追加条件: {}", table.getName(), condition);
            return CCJSqlParserUtil.parseCondExpression(condition);
        } catch (Exception e) {
            log.error("[数据权限] 条件解析失败，本次查询放行 table={} msId={}",
                    table == null ? null : table.getName(), mappedStatementId, e);
            return null;
        }
    }

    /** mappedStatementId 形如 com.permission.system.mapper.SysUserMapper.selectRawById，取末段匹配 */
    private boolean isBypassed(String mappedStatementId) {
        if (mappedStatementId == null) return false;
        int idx = mappedStatementId.lastIndexOf('.');
        String method = idx >= 0 ? mappedStatementId.substring(idx + 1) : mappedStatementId;
        return BYPASS_METHODS.contains(method);
    }

    /** 从 SecurityContext 取当前登录用户；未登录或非本系统主体时返回 null */
    private LoginUser currentLoginUser() {
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

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
 * 写操作（UPDATE/DELETE）的越权防护由 Service 层显式校验承担。
 */
@Slf4j
@Component
public class DataPermissionHandler implements MultiDataPermissionHandler {

    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
        try {
            if (table == null) return null;
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

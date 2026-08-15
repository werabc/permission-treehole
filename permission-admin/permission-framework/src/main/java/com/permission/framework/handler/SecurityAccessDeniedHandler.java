package com.permission.framework.handler;

import cn.hutool.json.JSONUtil;
import com.permission.common.R;
import com.permission.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SecurityAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.getWriter().write(JSONUtil.toJsonStr(R.fail(ResultCode.FORBIDDEN)));
    }
}
// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.framework.handler.SecurityAccessDeniedHandler
// 【模块】permission-framework（安全与基础设施模块）
//
// 【使用的注解/技术】
//   - @Component — Spring，声明 Bean 并被 SecurityConfig.exceptionHandling 自动装配
//   - implements AccessDeniedHandler — Spring Security，处理已通过认证但权限不足的场景
//
// 【关键依赖/注入】
//   - 依赖 R / ResultCode — 构造统一的 JSON 失败响应（force code）
//   - 依赖 JSONUtil — Hutool，将 R 对象序列化为 JSON 字符串
//   - 返回状态码：HttpServletResponse.SC_FORBIDDEN（403）
//
// 【关联文件】
//   - 被 SecurityConfig 通过 .exceptionHandling(ex -> ex.accessDeniedHandler(this)) 注入
//   - 与 SecurityAuthenticationEntryPoint 互为姊妹：EntryPoint 管"未登录401"，本 Handler 管"无权限403"
//
// 【核心作用】用户已通过身份（有合法 token），但访问资源权限不足时，返回统一的 403 JSON 响应。
//
// 【设计必要性】
//   - 如果不自定义，Spring Security 默认 /error 重定向（前后端分离场景前端拿不到明确权限错误信息）；
//   - 手写 JSON +R 包装保证与全局 R 响应结构一致，前端 axios 拦截器可统一处理。
//
// 【注意事项/安全提示】
//   - 403 提示信息当前使用 ResultCode.FORBIDDEN 默认文案，如需细化到"缺少 XX 角色"需在此扩展；
//   - 该 Handler 仅在认证成功但授权失败时触发；未认证走 AuthenticationEntryPoint，两者分工明确不可混淆；
//   - 返回全局状态码 403：网关/WAF 层面不要直接将 403 降级重定向，否则前端无法识别具体原因。
// ============================================================

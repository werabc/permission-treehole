package com.permission.framework.handler;

import cn.hutool.json.JSONUtil;
import com.permission.common.R;
import com.permission.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SecurityAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(JSONUtil.toJsonStr(R.fail(ResultCode.UNAUTHORIZED)));
    }
}
// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.framework.handler.SecurityAuthenticationEntryPoint
// 【模块】permission-framework（安全与基础设施模块）
//
// 【使用的注解/技术】
//   - @Component — Spring，声明 Bean 并被 SecurityConfig.exceptionHandling 自动装配
//   - implements AuthenticationEntryPoint — Spring Security，处理"未携带有效认证信息"访问受保护资源的场景
//
// 【关键依赖/注入】
//   - 依赖 R / ResultCode — 构造统一的 JSON 失败响应（UNAUTHORIZED）
//   - 依赖 JSONUtil — Hutool，将 R 对象序列化为 JSON 字符串
//   - 返回状态码：HttpServletResponse.SC_UNAUTHORIZED（401）
//
// 【关联文件】
//   - 被 SecurityConfig 通过 .exceptionHandling(ex -> ex.authenticationEntryPoint(this)) 注入
//   - 与 SecurityAccessDeniedHandler 互为姊妹：本 EntryPoint 管"未登录401"，Handler 管"无权限403"
//
// 【核心作用】请求没有携带有效认证信息或认证信息被拒绝时，返回统一的 401 JSON 响应。
//
// 【设计必要性】
//   - 默认 Spring Security EntryPoint 会重定向到登录页（前后端分离场景中前端期望 JSON，而非 302）；
//   - 统一 R.fail(ResultCode.UNAUTHORIZED) 结构后，前端根拦截器可据此自动跳转登录页/清除本地 Token。
//
// 【注意事项/安全提示】
//   - 该 EntryPoint 只处理"未认证"，不处理 token 格式错误/过期（由 JwtAuthenticationFilter 提前处理并直接写 401）；
//   - 401 提示信息保持通用化，勿在错误中泄露"用户不存在"与"密码错误"的区别，防止用户名枚举；
//   - 若引入 SSO/OAuth 重定向逻辑，需在此扩展，注意与纯 JSON 返回的路径分层。
// ============================================================

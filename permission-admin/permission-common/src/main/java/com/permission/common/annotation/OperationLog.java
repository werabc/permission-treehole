package com.permission.common.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    String value() default "";

    String module() default "";

    String action() default "";
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.common.annotation.OperationLog
// 【模块】permission-common（公共基础模块）
//
// 【使用的注解/技术】
//   - @Target(ElementType.METHOD) — 仅能标注在方法上
//   - @Retention(RetentionPolicy.RUNTIME) — 运行时可通过反射读取，AOP 切面依赖此策略
//   - @Documented — 纳入 Javadoc
//
// 【关联文件】
//   - 被 OperationLogAspect 切面拦截并记录日志 → framework/aspect/OperationLogAspect.java
//   - 被 Controller 上的公开方法引用 → controller/*Controller.java
//   - 日志入库对应实体 SysOperationLog → entity/SysOperationLog.java
//
// 【核心作用】声明式操作日志注解，AOP 在环绕通知中读取 module/action/value 并写入日志表。
//
// 【设计必要性】相比在方法内硬编码日志语句，注解+切面让业务代码无侵入，集中在
//   OperationLogAspect 处理异常信息、执行时长、操作者等通用字段。
//
// 【注意事项】
//   - 切面通过 ProceedingJoinPoint.proceed() 获取执行时长，方法抛异常会记录 errorMsg
//   - value 作为默认描述，优先显示 module + action
// ============================================================

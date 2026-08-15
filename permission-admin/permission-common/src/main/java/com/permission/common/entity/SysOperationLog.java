package com.permission.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_operation_log")
public class SysOperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String module;
    private String action;
    private String method;
    private String requestUrl;
    private String requestMethod;
    private String requestParams;
    private String responseResult;
    private Long executeTime;
    private String operator;
    private String operatorIp;
    private Integer status;
    private String errorMsg;
    private LocalDateTime createTime;
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.common.entity.SysOperationLog
// 【模块】permission-common（公共基础模块）
//
// 【使用的注解/技术】
//   - @Data — Lombok，生成访问器
//   - @TableName("sys_operation_log") — MyBatis-Plus 映射操作日志表
//   - @TableId(type = IdType.AUTO) — 自增主键
//   - implements Serializable — 日志可序列化
//
// 【关联文件】
//   - 被 OperationLogAspect 通过 @OperationLog 注解聚合写入 → framework/aspect/OperationLogAspect.java
//   - 对应注解定义 → annotation/OperationLog.java
//   - 由 OperationLogService/Impl 查询/清理 → service/*OperationLogService*.java
//
// 【核心作用】承载一次业务接口的操作日志，包含请求方法、参数、响应、执行时间与执行人。
//
// 【设计必要性】不继承 BaseEntity，因为只有 createTime（无 update/deleted），
//   字段独立满足 AOP 的高频写入性能需求。
//
// 【注意事项】
//   - executeTime 单位毫秒，由切面耗时统计
//   - status=1 成功/0 失败，失败时 errorMsg 被记录
//   - requestParams 与 responseResult 字段可能较大，必要时截断
// ============================================================

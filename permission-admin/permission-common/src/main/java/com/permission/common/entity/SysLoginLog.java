package com.permission.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_login_log")
public class SysLoginLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String ip;
    private String location;
    private String browser;
    private String os;
    private Integer status;
    private String message;
    private LocalDateTime loginTime;
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.common.entity.SysLoginLog
// 【模块】permission-common（公共基础模块）
//
// 【使用的注解/技术】
//   - @Data — Lombok，生成访问器
//   - @TableName("sys_login_log") — MyBatis-Plus 映射登录日志表
//   - @TableId(type = IdType.AUTO) — 自增主键
//   - implements Serializable — 日志对象可序列化
//
// 【关联文件】
//   - 被 LoginLogService/Impl 负责写入/查询 → service/*LoginLogService*.java
//   - 由 AuthServiceImpl 登录成功/失败时写入 → service/impl/AuthServiceImpl.java
//   - 关联 User-Agent 解析工具 → common/utils/AddressUtil.java
//
// 【核心作用】记录每次登录行为：账号、IP、网点、浏览器、OS、结果。
//
// 【设计必要性】不继承 BaseEntity 是因为日志表结构专用（含 ip/location/browser/os），
//   与通用业务实体字段不齐，故独立建 PO。
//
// 【注意事项】
//   - status: 1 成功 / 0 失败；message 失败时填原因
//   - loginTime 由服务层填充，不依赖自动填充
// ============================================================

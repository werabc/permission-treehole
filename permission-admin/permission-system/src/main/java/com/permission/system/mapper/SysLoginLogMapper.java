package com.permission.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.permission.common.entity.SysLoginLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysLoginLogMapper extends BaseMapper<SysLoginLog> {
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.system.mapper.SysLoginLogMapper
// 【模块】permission-system
//
// 【使用的注解/技术】
//   - @Mapper — MyBatis-Plus，标识映射接口，被 MapperScan 扫描注册为 Mapper Bean
//   - BaseMapper<SysLoginLog> — MyBatis-Plus 通用 Mapper，提供 sys_login_log 表的标准 CRUD
//
// 【关键依赖】
//   - 依赖 SysLoginLog 实体 → 登录日志表（sys_login_log）的 ORM 映射载体
//
// 【关联文件】
//   - 被 SysLoginLogService / SysLoginLogServiceImpl 调用，分页查询与写入登录记录
//   - 被 SysUserServiceImpl.login() 调用，登录成功/失败时写入日志
//   - 被 @MapperScan 扫描注册
//
// 【核心作用】
//   登录日志表（sys_login_log）的数据访问接口，记录用户登录行为（用户名、IP、状态、消息、
//   登录时间），是安全审计的重要依据。
//
// 【设计必要性】
//   独立 Mapper 便于日志表读写操作维护，与业务表解耦。
//
// 【注意事项/安全提示】
//   - 登录日志包含用户名与 IP 信息，查询接口需通过 @PreAuthorize 控制权限
//   - 日志数据可能持续增长，生产环境建议定期归档或分表
// ============================================================

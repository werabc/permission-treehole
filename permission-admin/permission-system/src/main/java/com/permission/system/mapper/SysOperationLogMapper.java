package com.permission.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.permission.common.entity.SysOperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysOperationLogMapper extends BaseMapper<SysOperationLog> {
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.system.mapper.SysOperationLogMapper
// 【模块】permission-system
//
// 【使用的注解/技术】
//   - @Mapper — MyBatis-Plus，标识映射接口，被 MapperScan 扫描注册
//   - BaseMapper<SysOperationLog> — MyBatis-Plus 通用 Mapper，提供 sys_operation_log 表标准 CRUD
//
// 【关键依赖】
//   - 依赖 SysOperationLog 实体 → 操作日志表（sys_operation_log）的 ORM 映射载体
//
// 【关联文件】
//   - 被 OperationLogAspect 调用，通过 insert 持久化操作日志
//   - 被 SysOperationLogService / SysOperationLogServiceImpl 调用，分页查询操作日志
//   - 被 @MapperScan 扫描注册
//
// 【核心作用】
//   操作日志表（sys_operation_log）的数据访问接口，记录标注了 @OperationLog 注解的接口
//   的操作行为（模块、动作、方法、请求参数、执行耗时、结果等）。
//
// 【设计必要性】
//   操作日志是系统审计的核心数据源，独立 Mapper 便于维护日志的写入与查询。
//
// 【注意事项/安全提示】
//   - 请求参数与响应结果的长度已经被 AOP 层截断（不超过 4000 字符），防止字段溢出
//   - 操作日志仅记录敏感操作（带 @OperationLog 注解的接口），非全量日志，性能开销可控
// ============================================================

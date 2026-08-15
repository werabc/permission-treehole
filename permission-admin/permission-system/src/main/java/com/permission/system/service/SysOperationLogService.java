package com.permission.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.permission.common.entity.SysOperationLog;

public interface SysOperationLogService extends IService<SysOperationLog> {

    IPage<SysOperationLog> pageLogs(long pageNum, long pageSize, String keyword, String module, String startDate, String endDate);
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.system.service.SysOperationLogService
// 【模块】permission-system
//
// 【使用的注解/技术】
//   - IService<SysOperationLog> — MyBatis-Plus，继承通用 Service 接口，提供操作日志基础 CRUD 能力
//   - IPage — MyBatis-Plus，分页接口，用于分页查询操作日志
//
// 【关键依赖】
//   - 依赖 SysOperationLog 实体 → 操作日志业务操作的数据载体
//
// 【关联文件】
//   - 被 SysOperationLogServiceImpl 实现，封装操作日志分页查询
//   - 被 OperationLogAspect 调用（间接通过 Mapper），持久化操作日志
//   - 被 LogController 调用，提供操作日志分页查询 API
//
// 【核心作用】
//   操作日志业务服务接口，定义按条件（关键词、模块、时间范围）分页查询操作日志的方法。
//
// 【设计必要性】
//   接口与实现分离，便于在实现层处理多条件组合查询与日期区间拼接。
//
// 【注意事项/安全提示】
//   - 实现层在查询时对 endDate 做了 endDate + " 23:59:59" 处理，确保包含当天末尾时间
//   - 操作日志查询接口需通过 @PreAuthorize 控制权限，防止无权限用户读取审计数据
// ============================================================

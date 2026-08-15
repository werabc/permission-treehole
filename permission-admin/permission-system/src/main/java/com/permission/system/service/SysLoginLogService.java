package com.permission.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.permission.common.entity.SysLoginLog;

public interface SysLoginLogService extends IService<SysLoginLog> {

    IPage<SysLoginLog> pageLogs(long pageNum, long pageSize, String keyword, String startDate, String endDate);

    void recordLoginLog(String username, String ip, Integer status, String message);
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.system.service.SysLoginLogService
// 【模块】permission-system
//
// 【使用的注解/技术】
//   - IService<SysLoginLog> — MyBatis-Plus，继承通用 Service 接口，提供登录日志基础 CRUD 能力
//
// 【关键依赖】
//   - 依赖 SysLoginLog 实体 → 登录日志业务操作的数据载体
//   - IPage — MyBatis-Plus，分页接口，用于分页查询登录日志
//
// 【关联文件】
//   - 被 SysLoginLogServiceImpl 实现，封装登录日志分页查询与记录写入
//   - 被 SysUserServiceImpl.login() 调用，写入登录日志
//   - 被 LogController 调用，提供登录日志分页查询 API
//
// 【核心作用】
//   登录日志业务服务接口，定义按条件（关键词、时间范围）分页查询登录日志，以及写入一次
//   登录记录的方法。
//
// 【设计必要性】
//   接口与实现分离，便于在实现层统一处理日期区间 23:59:59 拼接等细节。
//
// 【注意事项/安全提示】
//   - 实现层在查询时对 endDate 做了 endDate + " 23:59:59" 处理，确保包含当天末尾时间
//   - 写入登录日志常用 try-catch 保护（由调用方 SysUserServiceImpl 控制），避免日志写入失败影响登录主流程
// ============================================================

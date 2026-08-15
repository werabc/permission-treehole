package com.permission.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.permission.common.entity.SysLoginLog;
import com.permission.system.mapper.SysLoginLogMapper;
import com.permission.system.service.SysLoginLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SysLoginLogServiceImpl extends ServiceImpl<SysLoginLogMapper, SysLoginLog>
        implements SysLoginLogService {

    @Override
    public IPage<SysLoginLog> pageLogs(long pageNum, long pageSize, String keyword,
                                        String startDate, String endDate) {
        Page<SysLoginLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<>();

        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(SysLoginLog::getUsername, keyword)
                    .or().like(SysLoginLog::getIp, keyword));
        }
        if (StrUtil.isNotBlank(startDate)) {
            wrapper.ge(SysLoginLog::getLoginTime, startDate);
        }
        if (StrUtil.isNotBlank(endDate)) {
            wrapper.le(SysLoginLog::getLoginTime, endDate + " 23:59:59");
        }
        wrapper.orderByDesc(SysLoginLog::getLoginTime);
        return baseMapper.selectPage(page, wrapper);
    }

    @Override
    public void recordLoginLog(String username, String ip, Integer status, String message) {
        SysLoginLog log = new SysLoginLog();
        log.setUsername(username);
        log.setIp(ip);
        log.setStatus(status);
        log.setMessage(message);
        log.setLoginTime(LocalDateTime.now());
        baseMapper.insert(log);
    }
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.system.service.impl.SysLoginLogServiceImpl
// 【模块】permission-system
//
// 【使用的注解/技术】
//   - @Service — Spring，声明业务层组件
//   - @Override — Java，标识重写接口方法
//   - ServiceImpl<SysLoginLogMapper, SysLoginLog> — MyBatis-Plus，继承通用 Service 实现基类
//   - Page / IPage / LambdaQueryWrapper — MyBatis-Plus，分页查询与条件构造器
//   - StrUtil — Hutool，字符串非空判断
//   - LocalDateTime — JDK 8 时间 API，记录登录时刻
//
// 【关键依赖】
//   - 依赖 SysLoginLogMapper → 登录日志数据访问
//   - 依赖 SysLoginLogService 接口 → 实现该接口契约
//   - 依赖 SysLoginLog 实体 → 登录日志操作载体
//
// 【关联文件】
//   - 被 SysUserServiceImpl.login() 调用，写入登录成功/失败记录
//   - 被 LogController 调用，提供登录日志分页查询
//
// 【核心作用】
//   登录日志服务实现：按关键词（用户名/IP）和时间区间分页查询登录日志，以及写入一次
//   登录记录（含用户名、IP、状态、消息、登录时间）。
//
// 【设计必要性】
//   将登录日志封装在独立 Service 中，避免 SysUserServiceImpl 直接操作日志表，符合
//   单一职责原则；便于日志读写策略独立变化（如后续切到 NoSQL 或消息队列）。
//
// 【注意事项/安全提示】
//   - pageLogs 对 endDate 做 endDate + " 23:59:59" 处理，确保查询区间覆盖到当天末尾
//   - recordLoginLog 由 login 主流程调用，主流程用 try-catch 保护日志写入失败不影响登录
//   - 登录日志表数据可能快速增长，生产环境建议定期归档或分表
// ============================================================

package com.permission.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.permission.common.entity.SysOperationLog;
import com.permission.system.mapper.SysOperationLogMapper;
import com.permission.system.service.SysOperationLogService;
import org.springframework.stereotype.Service;

@Service
public class SysOperationLogServiceImpl extends ServiceImpl<SysOperationLogMapper, SysOperationLog>
        implements SysOperationLogService {

    @Override
    public IPage<SysOperationLog> pageLogs(long pageNum, long pageSize, String keyword,
                                            String module, String startDate, String endDate) {
        Page<SysOperationLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();

        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(SysOperationLog::getOperator, keyword)
                    .or().like(SysOperationLog::getAction, keyword));
        }
        if (StrUtil.isNotBlank(module)) {
            wrapper.eq(SysOperationLog::getModule, module);
        }
        if (StrUtil.isNotBlank(startDate)) {
            wrapper.ge(SysOperationLog::getCreateTime, startDate);
        }
        if (StrUtil.isNotBlank(endDate)) {
            wrapper.le(SysOperationLog::getCreateTime, endDate + " 23:59:59");
        }
        wrapper.orderByDesc(SysOperationLog::getCreateTime);
        return baseMapper.selectPage(page, wrapper);
    }
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.system.service.impl.SysOperationLogServiceImpl
// 【模块】permission-system
//
// 【使用的注解/技术】
//   - @Service — Spring，声明业务层组件
//   - @Override — Java，标识重写接口方法
//   - ServiceImpl<SysOperationLogMapper, SysOperationLog> — MyBatis-Plus，继承通用 Service 实现基类
//   - Page / IPage / LambdaQueryWrapper — MyBatis-Plus，分页查询与条件构造器
//   - StrUtil — Hutool，字符串非空判断
//
// 【关键依赖】
//   - 依赖 SysOperationLogMapper → 操作日志数据访问
//   - 依赖 SysOperationLogService 接口 → 实现该接口契约
//   - 依赖 SysOperationLog 实体 → 操作日志操作载体
//
// 【关联文件】
//   - 被 OperationLogAspect 调用（间接通过 Mapper），持久化操作日志
//   - 被 LogController 调用，提供操作日志分页查询
//
// 【核心作用】
//   操作日志服务实现：按多条件（关键词、模块、时间区间）分页查询操作日志，支持按操作者/动作
//   模糊搜索、按模块精确过滤、按创建时间区间筛选。
//
// 【设计必要性】
//   操作日志的查询条件组合较多（keyword、module、startDate、endDate），封装为独立 Service
//   方法便于统一排序规则与日期处理。
//
// 【注意事项/安全提示】
//   - pageLogs 对 endDate 做 endDate + " 23:59:59" 处理，确保查询区间覆盖到当天末尾
//   - 操作日志查询接口必须通过 @PreAuthorize 控制权限，防止无权限用户读取审计数据
// ============================================================

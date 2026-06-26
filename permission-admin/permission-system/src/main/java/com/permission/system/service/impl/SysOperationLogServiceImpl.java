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

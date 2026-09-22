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


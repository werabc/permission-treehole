package com.permission.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.permission.common.entity.SysLoginLog;

public interface SysLoginLogService extends IService<SysLoginLog> {

    IPage<SysLoginLog> pageLogs(long pageNum, long pageSize, String keyword, String startDate, String endDate);

    void recordLoginLog(String username, String ip, Integer status, String message);
}

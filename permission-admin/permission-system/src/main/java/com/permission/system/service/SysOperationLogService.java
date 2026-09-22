package com.permission.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.permission.common.entity.SysOperationLog;

public interface SysOperationLogService extends IService<SysOperationLog> {

    IPage<SysOperationLog> pageLogs(long pageNum, long pageSize, String keyword, String module, String startDate, String endDate);
}


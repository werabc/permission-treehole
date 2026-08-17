package com.permission.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.permission.common.entity.ThReport;

public interface ThReportService extends IService<ThReport> {

    /**
     * 分页查询举报列表
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param status   状态: 0-待处理 1-已处理-成立 2-已处理-不成立
     * @return 分页结果
     */
    IPage<ThReport> pageReports(long pageNum, long pageSize, Integer status);

    /**
     * 处理举报
     *
     * @param id         举报ID
     * @param status    处理结果: 1-成立 2-不成立
     * @param result    处理说明
     * @param handlerId 处理人ID
     */
    void handleReport(Long id, Integer status, String result, Long handlerId);

    /**
     * 创建举报
     *
     * @param report 举报信息
     */
    void createReport(ThReport report);
}

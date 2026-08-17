package com.permission.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.permission.common.entity.ThReport;
import com.permission.common.entity.ThUser;
import com.permission.common.exception.BusinessException;
import com.permission.common.ResultCode;
import com.permission.system.mapper.ThReportMapper;
import com.permission.system.mapper.ThUserMapper;
import com.permission.system.service.ThReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ThReportServiceImpl extends ServiceImpl<ThReportMapper, ThReport> implements ThReportService {

    private final ThReportMapper reportMapper;
    private final ThUserMapper userMapper;

    @Override
    public IPage<ThReport> pageReports(long pageNum, long pageSize, Integer status) {
        Page<ThReport> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ThReport> wrapper = new LambdaQueryWrapper<ThReport>()
                .eq(ThReport::getDeleted, 0)
                .eq(status != null, ThReport::getStatus, status)
                .orderByDesc(ThReport::getCreateTime);

        IPage<ThReport> result = reportMapper.selectPage(page, wrapper);

        // 填充举报人名称
        for (ThReport report : result.getRecords()) {
            if (report.getReporterId() != null) {
                ThUser user = userMapper.selectById(report.getReporterId());
                report.setReporterName(user != null ? user.getNickname() : "未知用户");
            }
        }

        return result;
    }

    @Override
    public void handleReport(Long id, Integer status, String result, Long handlerId) {
        ThReport report = reportMapper.selectById(id);
        if (report == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "举报记录不存在");
        }
        if (report.getStatus() != 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该举报已处理");
        }

        report.setStatus(status);
        report.setHandleResult(result);
        report.setHandlerId(handlerId);
        report.setHandleTime(LocalDateTime.now());
        reportMapper.updateById(report);
    }

    @Override
    public void createReport(ThReport report) {
        // 检查目标是否存在
        if (report.getTargetType() == null || report.getTargetId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "举报目标不能为空");
        }
        // 检查是否重复举报
        LambdaQueryWrapper<ThReport> wrapper = new LambdaQueryWrapper<ThReport>()
                .eq(ThReport::getReporterId, report.getReporterId())
                .eq(ThReport::getTargetType, report.getTargetType())
                .eq(ThReport::getTargetId, report.getTargetId())
                .eq(ThReport::getDeleted, 0);
        ThReport existing = reportMapper.selectOne(wrapper);
        if (existing != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "您已举报过该内容");
        }

        report.setStatus(0); // 待处理
        report.setCreateTime(LocalDateTime.now());
        report.setDeleted(0);
        reportMapper.insert(report);
    }
}

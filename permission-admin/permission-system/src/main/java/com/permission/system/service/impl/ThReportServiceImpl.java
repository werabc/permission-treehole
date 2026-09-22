package com.permission.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.permission.common.entity.ThComment;
import com.permission.common.entity.ThNotification;
import com.permission.common.entity.ThPost;
import com.permission.common.entity.ThReport;
import com.permission.common.entity.ThUser;
import com.permission.common.exception.BusinessException;
import com.permission.common.ResultCode;
import com.permission.system.mapper.ThCommentMapper;
import com.permission.system.mapper.ThNotificationMapper;
import com.permission.system.mapper.ThPostMapper;
import com.permission.system.mapper.ThReportMapper;
import com.permission.system.mapper.ThUserMapper;
import com.permission.system.service.ThReportService;
import com.permission.system.support.ThUserGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThReportServiceImpl extends ServiceImpl<ThReportMapper, ThReport> implements ThReportService {

    private final ThReportMapper reportMapper;
    private final ThUserMapper userMapper;
    private final ThPostMapper postMapper;
    private final ThCommentMapper commentMapper;
    private final ThNotificationMapper notificationMapper;
    private final ThUserGuard userGuard;

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
    @Transactional(rollbackFor = Exception.class)
    public void handleReport(Long id, Integer status, String result, Long handlerId) {
        ThReport report = reportMapper.selectById(id);
        if (report == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "举报记录不存在");
        }
        if (report.getStatus() != 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该举报已处理");
        }
        if (status == null || (status != 1 && status != 2)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "处理结果只能是 1-成立 或 2-不成立");
        }

        report.setStatus(status);
        report.setHandleResult(result);
        report.setHandlerId(handlerId);
        report.setHandleTime(LocalDateTime.now());
        reportMapper.updateById(report);

        // ========== 举报闭环：成立则下架内容 + 违规计分 + 自动处罚 + 告知举报人 ==========
        if (status == 1) {
            try {
                Long authorId = hideReportedContent(report);
                if (authorId != null) {
                    int score = userGuard.getReportScore();
                    userGuard.addViolation(authorId, score,
                            "被举报内容违规（举报ID " + report.getId() + "）");
                }
            } catch (Exception e) {
                // 内容下架/计分失败不应导致举报状态回滚不可用，但必须留痕
                log.error("举报成立后续处理失败 reportId={}", report.getId(), e);
            }
        }

        notifyReporter(report, status, result);
        log.info("举报处理完成 reportId={} status={} handler={}", report.getId(), status, handlerId);
    }

    /**
     * 隐藏被举报内容，返回内容作者ID（用于违规计分）
     * 采用"置为待审(status=0)"而非删除，保留可复核、可恢复的能力
     */
    private Long hideReportedContent(ThReport report) {
        String type = report.getTargetType();
        if ("POST".equalsIgnoreCase(type)) {
            ThPost post = postMapper.selectById(report.getTargetId());
            if (post == null || post.getDeleted() == 1) return null;
            post.setStatus(0);
            post.setAuditRemark("举报成立自动下架：" + report.getReason());
            postMapper.updateById(post);
            return post.getUserId();
        }
        if ("COMMENT".equalsIgnoreCase(type)) {
            ThComment comment = commentMapper.selectById(report.getTargetId());
            if (comment == null || comment.getDeleted() == 1) return null;
            comment.setStatus(0);
            commentMapper.updateById(comment);
            return comment.getUserId();
        }
        return null;
    }

    /** 通知举报人处理结果（失败不影响主流程） */
    private void notifyReporter(ThReport report, Integer status, String result) {
        if (report.getReporterId() == null) return;
        try {
            ThNotification notification = new ThNotification();
            notification.setUserId(report.getReporterId());
            notification.setSenderId(null);
            notification.setType("REPORT_RESULT");
            notification.setTargetType(report.getTargetType());
            notification.setTargetId(report.getTargetId());
            String verdict = status == 1 ? "举报成立，内容已下架" : "经核实未违规";
            String suffix = (result == null || result.isBlank()) ? "" : "（处理说明：" + result + "）";
            notification.setContent("你举报的内容已处理：" + verdict + suffix);
            notification.setIsRead(0);
            notificationMapper.insert(notification);
        } catch (Exception e) {
            log.error("通知举报人失败 reportId={} reporterId={}", report.getId(), report.getReporterId(), e);
        }
    }

    @Override
    public void createReport(ThReport report) {
        // 检查目标是否存在
        if (report.getTargetType() == null || report.getTargetId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "举报目标不能为空");
        }
        // 禁止举报自己
        if (report.getReporterId() != null && "POST".equalsIgnoreCase(report.getTargetType())) {
            ThPost post = postMapper.selectById(report.getTargetId());
            if (post != null && report.getReporterId().equals(post.getUserId())) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "不能举报自己发布的内容");
            }
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

        // 维护被举报帖子的举报数（原实现遗漏，导致管理端看不到热度）
        if ("POST".equalsIgnoreCase(report.getTargetType())) {
            postMapper.incrementReportCount(report.getTargetId());
        }
    }
}

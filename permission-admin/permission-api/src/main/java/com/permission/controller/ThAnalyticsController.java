package com.permission.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.permission.common.R;
import com.permission.common.entity.*;
import com.permission.system.mapper.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Tag(name = "树洞数据分析")
@RestController
@RequestMapping("/api/admin/th/analytics")
@RequiredArgsConstructor
public class ThAnalyticsController {

    private final ThUserMapper userMapper;
    private final ThPostMapper postMapper;
    private final ThCommentMapper commentMapper;
    private final ThCategoryMapper categoryMapper;
    private final ThReportMapper reportMapper;

    @Operation(summary = "概览数据")
    @GetMapping("/overview")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Map<String, Object>> overview() {
        Map<String, Object> result = new HashMap<>();

        // 基础统计
        long userCount = userMapper.selectCount(new LambdaQueryWrapper<ThUser>().eq(ThUser::getDeleted, 0));
        long postCount = postMapper.selectCount(new LambdaQueryWrapper<ThPost>().eq(ThPost::getDeleted, 0));
        long commentCount = commentMapper.selectCount(new LambdaQueryWrapper<ThComment>().eq(ThComment::getDeleted, 0));
        long categoryCount = categoryMapper.selectCount(new LambdaQueryWrapper<ThCategory>().eq(ThCategory::getDeleted, 0));

        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("userCount", userCount);
        stats.put("postCount", postCount);
        stats.put("commentCount", commentCount);
        stats.put("categoryCount", categoryCount);
        result.put("stats", stats);

        // 用户状态
        long activeUsers = userMapper.selectCount(new LambdaQueryWrapper<ThUser>().eq(ThUser::getDeleted, 0).eq(ThUser::getStatus, 1));
        long bannedUsers = userMapper.selectCount(new LambdaQueryWrapper<ThUser>().eq(ThUser::getDeleted, 0).eq(ThUser::getStatus, 0));
        Map<String, Long> userStatus = new LinkedHashMap<>();
        userStatus.put("active", activeUsers);
        userStatus.put("banned", bannedUsers);
        result.put("userStatus", userStatus);

        // 帖子状态
        long approvedPosts = postMapper.selectCount(new LambdaQueryWrapper<ThPost>().eq(ThPost::getDeleted, 0).eq(ThPost::getStatus, 1));
        long pendingPosts = postMapper.selectCount(new LambdaQueryWrapper<ThPost>().eq(ThPost::getDeleted, 0).eq(ThPost::getStatus, 0));
        long rejectedPosts = postMapper.selectCount(new LambdaQueryWrapper<ThPost>().eq(ThPost::getDeleted, 0).eq(ThPost::getStatus, 2));
        Map<String, Long> postStatus = new LinkedHashMap<>();
        postStatus.put("approved", approvedPosts);
        postStatus.put("pending", pendingPosts);
        postStatus.put("rejected", rejectedPosts);
        result.put("postStatus", postStatus);

        // 举报统计
        long pendingReports = reportMapper.selectCount(new LambdaQueryWrapper<ThReport>().eq(ThReport::getDeleted, 0).eq(ThReport::getStatus, 0));
        result.put("pendingReports", pendingReports);

        return R.ok(result);
    }

    @Operation(summary = "趋势数据")
    @GetMapping("/trends")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Map<String, Object>> trends(@RequestParam(defaultValue = "7") int days) {
        Map<String, Object> result = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<Long> userTrend = new ArrayList<>();
        List<Long> postTrend = new ArrayList<>();
        List<Long> commentTrend = new ArrayList<>();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();

            long users = userMapper.selectCount(new LambdaQueryWrapper<ThUser>()
                    .eq(ThUser::getDeleted, 0)
                    .ge(ThUser::getCreateTime, start).lt(ThUser::getCreateTime, end));
            long posts = postMapper.selectCount(new LambdaQueryWrapper<ThPost>()
                    .eq(ThPost::getDeleted, 0)
                    .ge(ThPost::getCreateTime, start).lt(ThPost::getCreateTime, end));
            long comments = commentMapper.selectCount(new LambdaQueryWrapper<ThComment>()
                    .eq(ThComment::getDeleted, 0)
                    .ge(ThComment::getCreateTime, start).lt(ThComment::getCreateTime, end));

            dates.add(date.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd")));
            userTrend.add(users);
            postTrend.add(posts);
            commentTrend.add(comments);
        }

        result.put("dates", dates);
        result.put("userTrend", userTrend);
        result.put("postTrend", postTrend);
        result.put("commentTrend", commentTrend);
        return R.ok(result);
    }

    @Operation(summary = "分类热度")
    @GetMapping("/categories")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<List<Map<String, Object>>> categories() {
        List<ThCategory> categories = categoryMapper.selectList(new LambdaQueryWrapper<ThCategory>()
                .eq(ThCategory::getDeleted, 0)
                .eq(ThCategory::getStatus, 1)
                .orderByAsc(ThCategory::getSort));

        List<Map<String, Object>> result = new ArrayList<>();
        for (ThCategory cat : categories) {
            long postCount = postMapper.selectCount(new LambdaQueryWrapper<ThPost>()
                    .eq(ThPost::getDeleted, 0)
                    .eq(ThPost::getCategoryId, cat.getId()));
            Map<String, Object> item = new HashMap<>();
            item.put("name", cat.getName());
            item.put("postCount", postCount);
            result.add(item);
        }
        return R.ok(result);
    }
}

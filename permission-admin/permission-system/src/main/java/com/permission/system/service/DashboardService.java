package com.permission.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.permission.common.entity.*;
import com.permission.system.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysDeptMapper deptMapper;
    private final SysLoginLogMapper loginLogMapper;
    private final ThUserMapper thUserMapper;
    private final ThPostMapper thPostMapper;
    private final ThCommentMapper thCommentMapper;
    private final ThReportMapper thReportMapper;

    /**
     * 综合仪表盘数据
     */
    public Map<String, Object> getDashboardOverview() {
        Map<String, Object> result = new HashMap<>();
        result.put("admin", getAdminStats());
        result.put("treehole", getTreeholeStats());
        result.put("pending", getPendingItems());
        result.put("trends", getTrends(7));
        return result;
    }

    public Map<String, Object> getAdminStats() {
        Map<String, Object> stats = new HashMap<>();
        long userCount = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeleted, 0));
        long roleCount = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>().eq(SysRole::getDeleted, 0));
        long menuCount = menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getDeleted, 0));
        long deptCount = deptMapper.selectCount(new LambdaQueryWrapper<SysDept>().eq(SysDept::getDeleted, 0));

        Map<String, Long> overview = new LinkedHashMap<>();
        overview.put("userCount", userCount);
        overview.put("roleCount", roleCount);
        overview.put("menuCount", menuCount);
        overview.put("deptCount", deptCount);
        stats.put("overview", overview);

        long activeUsers = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeleted, 0).eq(SysUser::getStatus, 1));
        long inactiveUsers = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeleted, 0).eq(SysUser::getStatus, 0));
        Map<String, Long> userStatus = new LinkedHashMap<>();
        userStatus.put("active", activeUsers);
        userStatus.put("inactive", inactiveUsers);
        stats.put("userStatus", userStatus);

        List<Map<String, Object>> deptUserCount = new ArrayList<>();
        List<SysDept> depts = deptMapper.selectList(new LambdaQueryWrapper<SysDept>().eq(SysDept::getDeleted, 0).orderByAsc(SysDept::getSort).last("LIMIT 10"));
        for (SysDept dept : depts) {
            long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeleted, 0).eq(SysUser::getDeptId, dept.getId()));
            Map<String, Object> item = new HashMap<>();
            item.put("name", dept.getDeptName());
            item.put("count", count);
            deptUserCount.add(item);
        }
        stats.put("deptUserCount", deptUserCount);

        // Last 7 days login trend
        List<String> dates = new ArrayList<>();
        List<Long> loginCounts = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            long count = loginLogMapper.selectCount(new LambdaQueryWrapper<SysLoginLog>().ge(SysLoginLog::getLoginTime, start).lt(SysLoginLog::getLoginTime, end));
            dates.add(date.format(formatter));
            loginCounts.add(count);
        }
        Map<String, Object> loginTrend = new HashMap<>();
        loginTrend.put("dates", dates);
        loginTrend.put("counts", loginCounts);
        stats.put("loginTrend", loginTrend);

        return stats;
    }

    public Map<String, Object> getTreeholeStats() {
        Map<String, Object> stats = new HashMap<>();

        long userCount = thUserMapper != null ? thUserMapper.selectCount(new LambdaQueryWrapper<ThUser>().eq(ThUser::getDeleted, 0)) : 0;
        long postCount = thPostMapper != null ? thPostMapper.selectCount(new LambdaQueryWrapper<ThPost>().eq(ThPost::getDeleted, 0)) : 0;
        long commentCount = thCommentMapper != null ? thCommentMapper.selectCount(new LambdaQueryWrapper<ThComment>().eq(ThComment::getDeleted, 0)) : 0;
        long reportCount = thReportMapper != null ? thReportMapper.selectCount(new LambdaQueryWrapper<ThReport>().eq(ThReport::getDeleted, 0)) : 0;

        Map<String, Long> overview = new LinkedHashMap<>();
        overview.put("userCount", userCount);
        overview.put("postCount", postCount);
        overview.put("commentCount", commentCount);
        overview.put("reportCount", reportCount);
        stats.put("overview", overview);

        long approvedPosts = thPostMapper != null ? thPostMapper.selectCount(new LambdaQueryWrapper<ThPost>().eq(ThPost::getDeleted, 0).eq(ThPost::getStatus, 1)) : 0;
        long pendingPosts = thPostMapper != null ? thPostMapper.selectCount(new LambdaQueryWrapper<ThPost>().eq(ThPost::getDeleted, 0).eq(ThPost::getStatus, 0)) : 0;
        long rejectedPosts = thPostMapper != null ? thPostMapper.selectCount(new LambdaQueryWrapper<ThPost>().eq(ThPost::getDeleted, 0).eq(ThPost::getStatus, 2)) : 0;
        Map<String, Long> postStatus = new LinkedHashMap<>();
        postStatus.put("approved", approvedPosts);
        postStatus.put("pending", pendingPosts);
        postStatus.put("rejected", rejectedPosts);
        stats.put("postStatus", postStatus);

        long activeUsers = thUserMapper != null ? thUserMapper.selectCount(new LambdaQueryWrapper<ThUser>().eq(ThUser::getDeleted, 0).eq(ThUser::getStatus, 1)) : 0;
        long bannedUsers = thUserMapper != null ? thUserMapper.selectCount(new LambdaQueryWrapper<ThUser>().eq(ThUser::getDeleted, 0).eq(ThUser::getStatus, 0)) : 0;
        Map<String, Long> userStatus = new LinkedHashMap<>();
        userStatus.put("active", activeUsers);
        userStatus.put("banned", bannedUsers);
        stats.put("userStatus", userStatus);

        long pendingReports = thReportMapper != null ? thReportMapper.selectCount(new LambdaQueryWrapper<ThReport>().eq(ThReport::getDeleted, 0).eq(ThReport::getStatus, 0)) : 0;
        stats.put("pendingReports", pendingReports);

        return stats;
    }

    public Map<String, Object> getPendingItems() {
        Map<String, Object> pending = new HashMap<>();
        long pendingPosts = thPostMapper != null ? thPostMapper.selectCount(new LambdaQueryWrapper<ThPost>().eq(ThPost::getDeleted, 0).eq(ThPost::getStatus, 0)) : 0;
        long pendingReports = thReportMapper != null ? thReportMapper.selectCount(new LambdaQueryWrapper<ThReport>().eq(ThReport::getDeleted, 0).eq(ThReport::getStatus, 0)) : 0;
        pending.put("pendingPosts", pendingPosts);
        pending.put("pendingReports", pendingReports);
        return pending;
    }

    public Map<String, Object> getTrends(int days) {
        Map<String, Object> trends = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<Long> userTrend = new ArrayList<>();
        List<Long> postTrend = new ArrayList<>();
        List<Long> commentTrend = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();

            long users = thUserMapper != null ? thUserMapper.selectCount(new LambdaQueryWrapper<ThUser>().eq(ThUser::getDeleted, 0).ge(ThUser::getCreateTime, start).lt(ThUser::getCreateTime, end)) : 0;
            long posts = thPostMapper != null ? thPostMapper.selectCount(new LambdaQueryWrapper<ThPost>().eq(ThPost::getDeleted, 0).ge(ThPost::getCreateTime, start).lt(ThPost::getCreateTime, end)) : 0;
            long comments = thCommentMapper != null ? thCommentMapper.selectCount(new LambdaQueryWrapper<ThComment>().eq(ThComment::getDeleted, 0).ge(ThComment::getCreateTime, start).lt(ThComment::getCreateTime, end)) : 0;

            dates.add(date.format(formatter));
            userTrend.add(users);
            postTrend.add(posts);
            commentTrend.add(comments);
        }

        trends.put("dates", dates);
        trends.put("userTrend", userTrend);
        trends.put("postTrend", postTrend);
        trends.put("commentTrend", commentTrend);
        return trends;
    }

    public Map<String, Object> getRealtimeStats() {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long newUsersToday = thUserMapper != null ? thUserMapper.selectCount(new LambdaQueryWrapper<ThUser>().eq(ThUser::getDeleted, 0).ge(ThUser::getCreateTime, todayStart)) : 0;
        long newPostsToday = thPostMapper != null ? thPostMapper.selectCount(new LambdaQueryWrapper<ThPost>().eq(ThPost::getDeleted, 0).ge(ThPost::getCreateTime, todayStart)) : 0;
        long newCommentsToday = thCommentMapper != null ? thCommentMapper.selectCount(new LambdaQueryWrapper<ThComment>().eq(ThComment::getDeleted, 0).ge(ThComment::getCreateTime, todayStart)) : 0;
        stats.put("newUsersToday", newUsersToday);
        stats.put("newPostsToday", newPostsToday);
        stats.put("newCommentsToday", newCommentsToday);
        return stats;
    }

    public Map<String, Object> getStatistics() {
        return getAdminStats();
    }
}

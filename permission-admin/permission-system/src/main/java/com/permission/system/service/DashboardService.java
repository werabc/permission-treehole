package com.permission.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.permission.common.entity.SysDept;
import com.permission.common.entity.SysLoginLog;
import com.permission.common.entity.SysMenu;
import com.permission.common.entity.SysRole;
import com.permission.common.entity.SysUser;
import com.permission.system.mapper.SysDeptMapper;
import com.permission.system.mapper.SysLoginLogMapper;
import com.permission.system.mapper.SysMenuMapper;
import com.permission.system.mapper.SysRoleMapper;
import com.permission.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysDeptMapper deptMapper;
    private final SysLoginLogMapper loginLogMapper;

    public Map<String, Object> getStatistics() {
        Map<String, Object> result = new HashMap<>();

        // 1. 基础统计
        long userCount = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeleted, 0));
        long roleCount = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>().eq(SysRole::getDeleted, 0));
        long menuCount = menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getDeleted, 0));
        long deptCount = deptMapper.selectCount(new LambdaQueryWrapper<SysDept>().eq(SysDept::getDeleted, 0));

        Map<String, Long> overview = new LinkedHashMap<>();
        overview.put("userCount", userCount);
        overview.put("roleCount", roleCount);
        overview.put("menuCount", menuCount);
        overview.put("deptCount", deptCount);
        result.put("overview", overview);

        // 2. 用户状态分布
        long activeUserCount = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeleted, 0).eq(SysUser::getStatus, 1));
        long inactiveUserCount = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeleted, 0).eq(SysUser::getStatus, 0));
        Map<String, Long> userStatus = new LinkedHashMap<>();
        userStatus.put("active", activeUserCount);
        userStatus.put("inactive", inactiveUserCount);
        result.put("userStatus", userStatus);

        // 3. 部门人数统计（Top 10）
        List<Map<String, Object>> deptUserCount = new ArrayList<>();
        List<SysDept> depts = deptMapper.selectList(
                new LambdaQueryWrapper<SysDept>().eq(SysDept::getDeleted, 0).orderByAsc(SysDept::getSort).last("LIMIT 10"));
        for (SysDept dept : depts) {
            long count = userMapper.selectCount(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeleted, 0).eq(SysUser::getDeptId, dept.getId()));
            Map<String, Object> item = new HashMap<>();
            item.put("name", dept.getDeptName());
            item.put("count", count);
            deptUserCount.add(item);
        }
        result.put("deptUserCount", deptUserCount);

        // 4. 最近 7 天登录趋势
        List<String> dates = new ArrayList<>();
        List<Long> loginCounts = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();

            long count = loginLogMapper.selectCount(
                    new LambdaQueryWrapper<SysLoginLog>()
                            .ge(SysLoginLog::getLoginTime, start)
                            .lt(SysLoginLog::getLoginTime, end));

            dates.add(date.format(formatter));
            loginCounts.add(count);
        }
        Map<String, Object> loginTrend = new HashMap<>();
        loginTrend.put("dates", dates);
        loginTrend.put("counts", loginCounts);
        result.put("loginTrend", loginTrend);

        return result;
    }
}

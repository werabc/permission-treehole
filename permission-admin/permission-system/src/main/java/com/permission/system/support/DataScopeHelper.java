package com.permission.system.support;

import com.permission.common.dto.LoginUser;
import com.permission.common.entity.SysDept;
import com.permission.common.enums.DataScope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 数据权限核心工具（纯函数，便于单测）
 *
 * 设计说明：
 *  1. 可见范围在**登录时**一次性解析成部门ID集合（LoginUser.deptIds），拦截器直接拼 SQL，
 *     不在每条 SQL 里递归组织树 —— 组织树规模小（通常 < 1000 节点），预计算更省也更可控
 *  2. 所有值都是服务端生成的 Long，直接内联无注入风险
 *  3. 组织树约定：sys_dept.ancestors 形如 "0" / "0,100" / "0,100,101"，
 *     层级 level = 分段数（"0"=1 集团，"0,100"=2 公司，"0,100,101"=3 部门，再往下=小组）
 *  4. sys_user 表始终额外放行"本人"（OR id = 当前用户ID），
 *     避免"自定义部门"把自己排除在外导致连自己的资料都读不到
 */
public final class DataScopeHelper {

    public static final String TABLE_USER = "sys_user";
    public static final String TABLE_DEPT = "sys_dept";

    /** 不支持的组织层级默认值 */
    private static final int DEFAULT_LEVEL = 1;

    private DataScopeHelper() {
    }

    // ==================== 表过滤范围 ====================

    /**
     * 该表是否需要施加数据权限
     * 说明：C 端业务表（th_*）为匿名用户数据，无部门属性，不参与管理端数据权限
     */
    public static boolean isScopedTable(String tableName) {
        if (tableName == null) return false;
        String t = tableName.toLowerCase();
        return TABLE_USER.equals(t) || TABLE_DEPT.equals(t);
    }

    // ==================== SQL 条件生成 ====================

    /**
     * 生成数据权限 SQL 片段
     *
     * @param tableName 主表物理表名
     * @param user      当前登录用户（可为 null：无登录态时不过滤，例如登录本身、定时任务）
     * @return 需要追加到 WHERE 的条件；返回 null 表示不限制
     */
    public static String buildCondition(String tableName, LoginUser user) {
        if (!isScopedTable(tableName) || user == null) return null;

        DataScope scope = DataScope.of(user.getDataScope());
        if (scope == DataScope.ALL) return null;

        String table = tableName.toLowerCase();
        List<Long> deptIds = user.getDeptIds() == null ? List.of() : user.getDeptIds();

        if (TABLE_USER.equals(table)) {
            return buildUserCondition(scope, deptIds, user);
        }
        return buildDeptCondition(scope, deptIds, user);
    }

    /** sys_user：按 dept_id 限定，且始终保证本人可见 */
    private static String buildUserCondition(DataScope scope, List<Long> deptIds, LoginUser user) {
        String deptPart = null;
        if (scope != DataScope.SELF) {
            deptPart = deptIds.isEmpty() ? "1=0" : "dept_id IN (" + join(deptIds) + ")";
        }
        String selfPart = user.getUserId() == null ? null : "id = " + user.getUserId();

        if (deptPart == null) {
            return selfPart == null ? "1=0" : selfPart;
        }
        if (selfPart == null) {
            return deptPart;
        }
        return "(" + deptPart + " OR " + selfPart + ")";
    }

    /** sys_dept：按 id 限定（用"可见部门 + 祖先"集合，保证树结构完整） */
    private static String buildDeptCondition(DataScope scope, List<Long> deptIds, LoginUser user) {
        if (scope == DataScope.SELF) {
            // 仅本人：只能看到自己所属部门这条记录
            return user.getDeptId() == null ? "1=0" : "id = " + user.getDeptId();
        }
        List<Long> treeIds = user.getDeptTreeIds() == null || user.getDeptTreeIds().isEmpty()
                ? deptIds : user.getDeptTreeIds();
        return treeIds.isEmpty() ? "1=0" : "id IN (" + join(treeIds) + ")";
    }

    private static String join(List<Long> ids) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(ids.get(i));
        }
        return sb.toString();
    }

    // ==================== 写侧可见性判定（与 buildCondition 同源） ====================

    /**
     * 当前用户是否有权"写"某个用户记录（改/删/禁用/重置密码/分配角色）
     *
     * 与 {@link #buildCondition} 的读侧规则保持**同一套语义**，避免读写漂移：
     *   - ALL           → 放行
     *   - 目标即本人    → 放行（读侧始终 OR id = 自己，写侧对齐）
     *   - SELF          → 仅本人
     *   - 其余          → 目标 dept_id 必须落在 deptIds 内
     *
     * @param user           当前登录用户
     * @param targetUserId   目标用户ID
     * @param targetDeptId   目标用户所属部门ID（可为 null）
     */
    public static boolean canWriteUser(LoginUser user, Long targetUserId, Long targetDeptId) {
        if (user == null) return false;                       // 无登录态：写操作一律不放行
        if (user.getUserId() != null && user.getUserId().equals(targetUserId)) return true;

        DataScope scope = DataScope.of(user.getDataScope());
        if (scope == DataScope.ALL) return true;
        if (scope == DataScope.SELF) return false;            // 仅本人，且目标不是本人

        return inVisibleDepts(user, targetDeptId);
    }

    /**
     * 当前用户是否有权"写"某个部门记录（改/删）
     *
     * 判定基准与读侧 sys_dept 一致：用"可见部门 + 祖先"集合，
     * 因为部门是树结构，能看见父节点才能对它做管理操作。
     *
     * @param user           当前登录用户
     * @param targetDeptId   目标部门ID
     */
    public static boolean canWriteDept(LoginUser user, Long targetDeptId) {
        if (user == null) return false;
        if (targetDeptId == null) return false;

        DataScope scope = DataScope.of(user.getDataScope());
        if (scope == DataScope.ALL) return true;

        if (scope == DataScope.SELF) {
            return user.getDeptId() != null && user.getDeptId().equals(targetDeptId);
        }
        List<Long> treeIds = user.getDeptTreeIds() == null || user.getDeptTreeIds().isEmpty()
                ? user.getDeptIds() : user.getDeptTreeIds();
        return treeIds != null && treeIds.contains(targetDeptId);
    }

    /**
     * 目标部门是否落在"可见部门集合"内（写侧 sys_user 判定用）
     * 注意：这里**不能**用 deptTreeIds（含祖先），否则会因祖先部门被放行而放大写权限。
     */
    private static boolean inVisibleDepts(LoginUser user, Long targetDeptId) {
        if (targetDeptId == null) return false;
        List<Long> deptIds = user.getDeptIds();
        return deptIds != null && deptIds.contains(targetDeptId);
    }

    // ==================== 组织树解析 ====================

    /** ancestors "0,100,101" → [0,100,101]；空或非法返回空列表 */
    public static List<Long> parseAncestorChain(String ancestors) {
        List<Long> chain = new ArrayList<>();
        if (ancestors == null || ancestors.isBlank()) return chain;
        for (String seg : ancestors.split(",")) {
            String s = seg.trim();
            if (s.isEmpty()) continue;
            try {
                chain.add(Long.parseLong(s));
            } catch (NumberFormatException ignored) {
                // 脏数据跳过，不影响整体解析
            }
        }
        return chain;
    }

    /** 由 ancestors 推导层级："0"=1，"0,100"=2，"0,100,101"=3 */
    public static int calcLevel(String ancestors) {
        List<Long> chain = parseAncestorChain(ancestors);
        return chain.isEmpty() ? DEFAULT_LEVEL : chain.size();
    }

    /** 组织树根节点ID（集团）：路径上的第一个节点（含自身） */
    public static Long resolveGroupId(SysDept dept) {
        if (dept == null) return null;
        List<Long> path = nonZeroPath(dept);
        return path.isEmpty() ? dept.getId() : path.get(0);
    }

    /**
     * 所属公司ID：路径上第 2 个节点（level=2）
     * 例：路径 [1(集团),100(公司),101(部门),103(小组)] → 100
     *     路径 [1,100]（自身即公司）→ 100
     *     路径 [1]（自身即集团）→ 1
     */
    public static Long resolveCompanyId(SysDept dept) {
        if (dept == null) return null;
        List<Long> path = nonZeroPath(dept);
        return path.size() >= 2 ? path.get(1) : (path.isEmpty() ? dept.getId() : path.get(0));
    }

    /** 由 ancestors（不含自身）拼出"从根到自身"的非 0 节点路径 */
    private static List<Long> nonZeroPath(SysDept dept) {
        List<Long> path = new ArrayList<>();
        for (Long id : parseAncestorChain(dept.getAncestors())) {
            if (id != null && id != 0L) path.add(id);
        }
        if (dept.getId() != null) path.add(dept.getId());
        return path;
    }

    // ==================== 可见范围解析 ====================

    /**
     * 解析当前用户在该数据范围下的"可见部门ID集合"
     *
     * @param dataScope      数据范围枚举编码
     * @param limitLevel     data_scope=5 时的层级深度 N
     * @param dept           用户所属部门（可为 null）
     * @param allDepts       全量部门（用于子树计算）
     * @param groupId        集团ID
     * @param companyId      公司ID
     * @param customDeptIds  自定义部门集合（data_scope=7）
     * @return 可见部门ID列表；ALL / SELF 返回空列表（由 buildCondition 单独处理）
     */
    public static List<Long> resolveVisibleDeptIds(int dataScope, Integer limitLevel,
                                                   SysDept dept, List<SysDept> allDepts,
                                                   Long groupId, Long companyId,
                                                   List<Long> customDeptIds) {
        DataScope scope = DataScope.of(dataScope);
        List<SysDept> depts = allDepts == null ? List.of() : allDepts;
        Long selfDeptId = dept == null ? null : dept.getId();

        switch (scope) {
            case ALL:
            case SELF:
                return new ArrayList<>();
            case GROUP_AND_SUB:
                return subtreeOf(groupId, depts);
            case COMPANY_AND_SUB:
                return subtreeOf(companyId, depts);
            case DEPT_AND_SUB:
                return subtreeOf(selfDeptId, depts);
            case DEPT_AND_SUB_LEVEL: {
                int n = (limitLevel == null || limitLevel < 1) ? 1 : limitLevel;
                int baseLevel = dept == null ? DEFAULT_LEVEL
                        : (dept.getDeptLevel() != null ? dept.getDeptLevel() : calcLevel(dept.getAncestors()));
                return subtreeOfWithinLevels(selfDeptId, baseLevel + n, depts);
            }
            case DEPT:
                return selfDeptId == null ? new ArrayList<>() : new ArrayList<>(List.of(selfDeptId));
            case CUSTOM:
                return customDeptIds == null ? new ArrayList<>() : new ArrayList<>(new LinkedHashSet<>(customDeptIds));
            default:
                return new ArrayList<>();
        }
    }

    /** 整棵子树（含自身） */
    public static List<Long> subtreeOf(Long rootId, List<SysDept> allDepts) {
        List<Long> result = new ArrayList<>();
        if (rootId == null) return result;
        result.add(rootId);
        for (SysDept d : allDepts) {
            if (d.getId() == null || d.getId().equals(rootId)) continue;
            if (isDescendantOf(d, rootId)) result.add(d.getId());
        }
        return result;
    }

    /** 子树但限制最大层级（含边界层级） */
    public static List<Long> subtreeOfWithinLevels(Long rootId, int maxLevel, List<SysDept> allDepts) {
        List<Long> result = new ArrayList<>();
        if (rootId == null) return result;
        for (SysDept d : allDepts) {
            if (d.getId() == null) continue;
            int level = d.getDeptLevel() != null ? d.getDeptLevel() : calcLevel(d.getAncestors());
            boolean inSubtree = d.getId().equals(rootId) || isDescendantOf(d, rootId);
            if (inSubtree && level <= maxLevel) result.add(d.getId());
        }
        return result;
    }

    /** 判断 dept 是否为 rootId 的后代：ancestors 链中包含 rootId */
    public static boolean isDescendantOf(SysDept dept, Long rootId) {
        if (dept == null || rootId == null) return false;
        return parseAncestorChain(dept.getAncestors()).contains(rootId);
    }

    /** 组装 id→部门 映射（便于按 id 取层级等属性） */
    public static Map<Long, SysDept> indexById(List<SysDept> depts) {
        Map<Long, SysDept> map = new HashMap<>();
        if (depts == null) return map;
        for (SysDept d : depts) {
            if (d.getId() != null) map.put(d.getId(), d);
        }
        return map;
    }

    /**
     * 可见部门 + 其全部祖先（用于 sys_dept 树查询）
     *
     * 为什么必须补祖先：部门以树形结构返回，若数据权限只放行叶子节点，
     * 父节点被过滤掉后前端建树会因"找不到父节点"而丢弃这些节点，表现为整个组织树空白。
     * 注意：该集合**仅用于 sys_dept 查询**，不可用于 sys_user，
     * 否则会因放行父部门而放大可见用户范围（越权）。
     */
    public static List<Long> withAncestors(List<Long> visibleDeptIds, List<SysDept> allDepts) {
        LinkedHashSet<Long> result = new LinkedHashSet<>();
        if (visibleDeptIds == null || visibleDeptIds.isEmpty()) return new ArrayList<>(result);

        Map<Long, SysDept> index = indexById(allDepts);
        result.addAll(visibleDeptIds);
        for (Long id : visibleDeptIds) {
            SysDept dept = index.get(id);
            if (dept == null) continue;
            for (Long ancestor : parseAncestorChain(dept.getAncestors())) {
                if (ancestor != null && ancestor != 0L) result.add(ancestor);
            }
        }
        return new ArrayList<>(result);
    }
}

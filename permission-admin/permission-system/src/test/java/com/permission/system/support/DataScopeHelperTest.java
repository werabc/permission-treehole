package com.permission.system.support;

import com.permission.common.dto.LoginUser;
import com.permission.common.entity.SysDept;
import com.permission.common.enums.DataScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据权限核心工具单元测试
 *
 * 组织结构（测试夹具）：
 *   1    集团               level 1   ancestors "0"
 *   ├─ 100 公司A           level 2   "0,1"
 *   │   ├─ 101 研发部      level 3   "0,1,100"
 *   │   │   └─ 103 前端组  level 4   "0,1,100,101"
 *   │   └─ 102 市场部      level 3   "0,1,100"
 *   └─ 200 公司B           level 2   "0,1"
 *       └─ 201 财务部      level 3   "0,1,200"
 */
class DataScopeHelperTest {

    private List<SysDept> depts;

    @BeforeEach
    void setUp() {
        depts = new ArrayList<>(Arrays.asList(
                dept(1L, "集团", 0L, "0"),
                dept(100L, "公司A", 1L, "0,1"),
                dept(101L, "研发部", 100L, "0,1,100"),
                dept(103L, "前端组", 101L, "0,1,100,101"),
                dept(102L, "市场部", 100L, "0,1,100"),
                dept(200L, "公司B", 1L, "0,1"),
                dept(201L, "财务部", 200L, "0,1,200")
        ));
    }

    private SysDept dept(Long id, String name, Long parentId, String ancestors) {
        SysDept d = new SysDept();
        d.setId(id);
        d.setDeptName(name);
        d.setParentId(parentId);
        d.setAncestors(ancestors);
        d.setDeptLevel(DataScopeHelper.calcLevel(ancestors));
        d.setDeleted(0);
        return d;
    }

    private SysDept find(Long id) {
        return depts.stream().filter(d -> d.getId().equals(id)).findFirst().orElseThrow();
    }

    private LoginUser user(Long userId, Long deptId, Integer dataScope, List<Long> deptIds) {
        return LoginUser.builder()
                .userId(userId).username("u" + userId)
                .deptId(deptId).dataScope(dataScope).deptIds(deptIds)
                .build();
    }

    // ==================== 组织树解析 ====================

    @Nested
    @DisplayName("组织树解析")
    class TreeResolve {

        @Test
        void calcLevel_ShouldDeriveLevelFromAncestors() {
            assertEquals(1, DataScopeHelper.calcLevel("0"));
            assertEquals(2, DataScopeHelper.calcLevel("0,1"));
            assertEquals(3, DataScopeHelper.calcLevel("0,1,100"));
            assertEquals(4, DataScopeHelper.calcLevel("0,1,100,101"));
        }

        @Test
        void calcLevel_NullOrBlank_ShouldDefaultToOne() {
            assertEquals(1, DataScopeHelper.calcLevel(null));
            assertEquals(1, DataScopeHelper.calcLevel(""));
        }

        @Test
        void calcLevel_DirtyData_ShouldNotThrow() {
            // 非法分段被忽略，仅统计合法层级：["0"] → 1
            assertEquals(1, DataScopeHelper.calcLevel("0,abc"));
            assertEquals(1, DataScopeHelper.calcLevel(" , "));
            assertEquals(2, DataScopeHelper.calcLevel("0,100,xyz"), "合法段仍应计数");
        }

        @Test
        void resolveGroupId_ShouldReturnRootOfPath() {
            assertEquals(1L, DataScopeHelper.resolveGroupId(find(103L)));
            assertEquals(1L, DataScopeHelper.resolveGroupId(find(201L)));
            assertEquals(1L, DataScopeHelper.resolveGroupId(find(1L)));
        }

        @Test
        void resolveCompanyId_ShouldReturnSecondNodeOnPath() {
            assertEquals(100L, DataScopeHelper.resolveCompanyId(find(103L)), "小组应归属公司A");
            assertEquals(100L, DataScopeHelper.resolveCompanyId(find(101L)));
            assertEquals(100L, DataScopeHelper.resolveCompanyId(find(100L)), "公司自身即公司节点");
            assertEquals(200L, DataScopeHelper.resolveCompanyId(find(201L)));
            assertEquals(1L, DataScopeHelper.resolveCompanyId(find(1L)), "集团无公司层时回退自身");
        }

        @Test
        void isDescendantOf_ShouldDetectSubtreeMembership() {
            assertTrue(DataScopeHelper.isDescendantOf(find(103L), 100L));
            assertTrue(DataScopeHelper.isDescendantOf(find(201L), 1L));
            assertFalse(DataScopeHelper.isDescendantOf(find(201L), 100L));
            assertFalse(DataScopeHelper.isDescendantOf(find(100L), 100L), "自身不算后代");
        }

        @Test
        void subtreeOf_ShouldIncludeSelfAndAllDescendants() {
            List<Long> ids = DataScopeHelper.subtreeOf(100L, depts);
            assertEquals(4, ids.size());
            assertTrue(ids.containsAll(Arrays.asList(100L, 101L, 102L, 103L)));
            assertFalse(ids.contains(200L), "不能跨公司");
        }

        @Test
        void subtreeOfWithinLevels_ShouldCutByLevel() {
            // 研发部(level 3) 限 1 级 → 最多到 level 4；前端组 level 4 命中
            List<Long> oneLevel = DataScopeHelper.subtreeOfWithinLevels(101L, 4, depts);
            assertTrue(oneLevel.containsAll(Arrays.asList(101L, 103L)));

            // 限到 level 3 → 只剩自己
            List<Long> none = DataScopeHelper.subtreeOfWithinLevels(101L, 3, depts);
            assertEquals(List.of(101L), none);
        }

        @Test
        void subtreeOf_NullRoot_ShouldReturnEmpty() {
            assertTrue(DataScopeHelper.subtreeOf(null, depts).isEmpty());
        }

        @Test
        @DisplayName("withAncestors：补全祖先，保证部门树不丢父节点")
        void withAncestors_ShouldAddParents() {
            // 可见 {103 前端组, 201 财务部} → 需补 101/100/1/200
            List<Long> ids = DataScopeHelper.withAncestors(Arrays.asList(103L, 201L), depts);
            assertTrue(ids.containsAll(Arrays.asList(103L, 101L, 100L, 1L, 201L, 200L)));
            assertEquals(6, ids.size());
        }

        @Test
        void withAncestors_EmptyInput_ShouldReturnEmpty() {
            assertTrue(DataScopeHelper.withAncestors(new ArrayList<>(), depts).isEmpty());
            assertTrue(DataScopeHelper.withAncestors(null, depts).isEmpty());
        }
    }

    // ==================== 可见部门集合解析（8 级） ====================

    @Nested
    @DisplayName("8 级数据范围解析")
    class ResolveVisible {

        private List<Long> resolve(int scope, Integer limitLevel, Long selfDeptId, List<Long> custom) {
            SysDept self = selfDeptId == null ? null : find(selfDeptId);
            Long groupId = DataScopeHelper.resolveGroupId(self);
            Long companyId = DataScopeHelper.resolveCompanyId(self);
            return DataScopeHelper.resolveVisibleDeptIds(scope, limitLevel, self, depts,
                    groupId, companyId, custom);
        }

        @Test
        @DisplayName("1 全部数据：无部门限制（交由 buildCondition 返回 null）")
        void all() {
            assertTrue(resolve(DataScope.ALL.getCode(), null, 101L, null).isEmpty());
        }

        @Test
        @DisplayName("2 本集团及以下：覆盖全部公司")
        void groupAndSub() {
            List<Long> ids = resolve(DataScope.GROUP_AND_SUB.getCode(), null, 101L, null);
            assertEquals(7, ids.size());
            assertTrue(ids.containsAll(Arrays.asList(1L, 100L, 200L, 201L)));
        }

        @Test
        @DisplayName("3 本公司及以下：只覆盖本公司")
        void companyAndSub() {
            List<Long> ids = resolve(DataScope.COMPANY_AND_SUB.getCode(), null, 103L, null);
            assertEquals(4, ids.size());
            assertTrue(ids.containsAll(Arrays.asList(100L, 101L, 102L, 103L)));
            assertFalse(ids.contains(200L));
        }

        @Test
        @DisplayName("4 本部门及以下：含全部子部门")
        void deptAndSub() {
            List<Long> ids = resolve(DataScope.DEPT_AND_SUB.getCode(), null, 100L, null);
            assertEquals(4, ids.size());
            assertTrue(ids.contains(103L));
        }

        @Test
        @DisplayName("5 本部门及以下(限N级)：深度可裁剪")
        void deptAndSubLevel() {
            List<Long> n1 = resolve(DataScope.DEPT_AND_SUB_LEVEL.getCode(), 1, 100L, null);
            assertTrue(n1.containsAll(Arrays.asList(100L, 101L, 102L)));
            assertFalse(n1.contains(103L), "限 1 级时小组(level 4)应被裁掉");

            List<Long> n2 = resolve(DataScope.DEPT_AND_SUB_LEVEL.getCode(), 2, 100L, null);
            assertTrue(n2.contains(103L));
        }

        @Test
        @DisplayName("5 本部门及以下(限N级)：N 非法时按 1 级兜底")
        void deptAndSubLevel_IllegalN() {
            List<Long> ids = resolve(DataScope.DEPT_AND_SUB_LEVEL.getCode(), 0, 100L, null);
            assertEquals(3, ids.size());
        }

        @Test
        @DisplayName("6 本部门：仅本部门不含子部门")
        void dept() {
            assertEquals(List.of(100L), resolve(DataScope.DEPT.getCode(), null, 100L, null));
        }

        @Test
        @DisplayName("7 自定义部门：按配置集合")
        void custom() {
            List<Long> ids = resolve(DataScope.CUSTOM.getCode(), null, 101L,
                    Arrays.asList(102L, 201L, 102L));
            assertEquals(2, ids.size(), "重复项应去重");
            assertTrue(ids.containsAll(Arrays.asList(102L, 201L)));
        }

        @Test
        @DisplayName("8 仅本人：返回空集合（由 id = userId 处理）")
        void self() {
            assertTrue(resolve(DataScope.SELF.getCode(), null, 101L, null).isEmpty());
        }

        @Test
        @DisplayName("无部门归属时不抛异常")
        void nullDept() {
            assertTrue(resolve(DataScope.DEPT_AND_SUB.getCode(), null, null, null).isEmpty());
        }
    }

    // ==================== SQL 条件矩阵 ====================

    @Nested
    @DisplayName("SQL 条件生成")
    class ConditionMatrix {

        @Test
        @DisplayName("非白名单表不施加数据权限")
        void notScopedTable() {
            assertNull(DataScopeHelper.buildCondition("th_post", user(9L, 101L, 1, null)));
            assertNull(DataScopeHelper.buildCondition("sys_menu", user(9L, 101L, 6, null)));
            assertFalse(DataScopeHelper.isScopedTable("th_comment"));
        }

        @Test
        @DisplayName("无登录态不施加数据权限（保护登录流程）")
        void noLoginUser() {
            assertNull(DataScopeHelper.buildCondition("sys_user", null));
            assertNull(DataScopeHelper.buildCondition("sys_dept", null));
        }

        @Test
        @DisplayName("1 全部数据：不追加条件")
        void allScope() {
            assertNull(DataScopeHelper.buildCondition("sys_user", user(9L, 101L, 1, null)));
            assertNull(DataScopeHelper.buildCondition("sys_dept", user(9L, 101L, 1, null)));
        }

        @Test
        @DisplayName("8 仅本人：按主键限定")
        void selfScope() {
            assertEquals("id = 9", DataScopeHelper.buildCondition("sys_user",
                    user(9L, 101L, 8, null)));
            assertEquals("id = 101", DataScopeHelper.buildCondition("sys_dept",
                    user(9L, 101L, 8, null)));
        }

        @Test
        @DisplayName("部门范围：user 表附带本人可见")
        void deptScope_userTableKeepsSelfVisible() {
            String cond = DataScopeHelper.buildCondition("sys_user",
                    user(9L, 101L, 6, List.of(101L)));
            assertEquals("(dept_id IN (101) OR id = 9)", cond);
        }

        @Test
        @DisplayName("部门范围：dept 表只按 id")
        void deptScope_deptTable() {
            assertEquals("id IN (101,103)", DataScopeHelper.buildCondition("sys_dept",
                    user(9L, 101L, 4, Arrays.asList(101L, 103L))));
        }

        @Test
        @DisplayName("dept 表优先使用 deptTreeIds（含祖先），user 表不受影响")
        void deptTable_UsesTreeIdsWithAncestors() {
            LoginUser u = LoginUser.builder()
                    .userId(9L).deptId(101L).dataScope(4)
                    .deptIds(List.of(101L))
                    .deptTreeIds(Arrays.asList(1L, 100L, 101L))
                    .build();
            assertEquals("id IN (1,100,101)", DataScopeHelper.buildCondition("sys_dept", u));
            // 关键：sys_user 仍按精确的可见部门集合过滤，不会因补祖先而放大可见用户范围
            assertEquals("(dept_id IN (101) OR id = 9)", DataScopeHelper.buildCondition("sys_user", u));
        }

        @Test
        @DisplayName("部门集合为空时收敛为 1=0（看不到数据）")
        void emptyDeptIds() {
            assertEquals("1=0", DataScopeHelper.buildCondition("sys_dept",
                    user(9L, 101L, 7, Collections.emptyList())));
            assertEquals("(1=0 OR id = 9)", DataScopeHelper.buildCondition("sys_user",
                    user(9L, 101L, 7, Collections.emptyList())));
        }

        @Test
        @DisplayName("用户无ID且部门为空：彻底收敛")
        void noUserNoDept() {
            assertEquals("1=0", DataScopeHelper.buildCondition("sys_user",
                    user(null, null, 4, Collections.emptyList())));
        }

        @Test
        @DisplayName("非法数据范围收敛为仅本人（最小权限）")
        void illegalScope() {
            assertEquals("id = 9", DataScopeHelper.buildCondition("sys_user",
                    user(9L, 101L, 999, null)));
            assertEquals("id = 9", DataScopeHelper.buildCondition("sys_user",
                    user(9L, 101L, null, null)));
        }

        @Test
        @DisplayName("表名大小写不敏感")
        void caseInsensitive() {
            assertEquals("id = 9", DataScopeHelper.buildCondition("SYS_USER",
                    user(9L, 101L, 8, null)));
        }
    }

    // ==================== 枚举契约 ====================

    @Test
    @DisplayName("枚举编码与旧的 5 级体系不冲突且覆盖 1-8")
    void enumContract() {
        assertEquals(8, DataScope.values().length);
        for (int i = 1; i <= 8; i++) {
            assertEquals(i, DataScope.of(i).getCode());
            assertNotNull(DataScope.of(i).getDesc());
        }
        assertEquals(DataScope.SELF, DataScope.of(null), "空值按最小权限处理");
        assertTrue(DataScope.DEPT_AND_SUB.needDeptResolve());
        assertTrue(DataScope.DEPT_AND_SUB_LEVEL.needDeptResolve());
        assertFalse(DataScope.SELF.needDeptResolve());
        assertFalse(DataScope.ALL.needDeptResolve());
    }
}

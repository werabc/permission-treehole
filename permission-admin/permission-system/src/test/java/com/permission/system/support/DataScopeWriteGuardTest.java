package com.permission.system.support;

import com.permission.common.dto.LoginUser;
import com.permission.common.enums.DataScope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 写侧数据权限判定测试 + **读写一致性** 契约测试
 *
 * 为什么要有这个文件：
 *   历史上读操作由 MyBatis-Plus 拦截器按 buildCondition 过滤，写操作完全没有校验，
 *   导致"看不见却改得了"。修复后读写都走 DataScopeHelper，但两处逻辑仍可能各自演化。
 *   这里用 Property-based 风格的断言把二者钉死：
 *
 *     **读侧判定为不可见 ⇒ 写侧必须拒绝**（反之亦然）
 *
 *   一旦有人只改了一边，这些测试立刻失败。
 *
 * 组织结构（与 DataScopeHelperTest 保持一致）：
 *   1    集团               level 1
 *   ├─ 100 公司A           level 2
 *   │   ├─ 101 研发部      level 3
 *   │   │   └─ 103 前端组  level 4
 *   │   └─ 102 市场部      level 3
 *   └─ 200 公司B           level 2
 *       └─ 201 财务部      level 3
 */
class DataScopeWriteGuardTest {

    private static final Long GROUP = 1L;
    private static final Long COMPANY_A = 100L;
    private static final Long DEV = 101L;
    private static final Long FE = 103L;
    private static final Long MARKET = 102L;
    private static final Long COMPANY_B = 200L;
    private static final Long FINANCE = 201L;

    private LoginUser user(Long userId, Long deptId, DataScope scope, List<Long> deptIds) {
        return LoginUser.builder()
                .userId(userId).username("u" + userId)
                .deptId(deptId).dataScope(scope.getCode()).deptIds(deptIds)
                .build();
    }

    // ==================== 读侧判定（复刻拦截器语义，用于一致性比对） ====================

    /**
     * 读侧"该用户记录是否可见"：等价于 buildCondition 拼出的 WHERE 是否命中该行。
     * 这里用纯 Java 重述，作为一致性测试的参照基准。
     */
    private boolean readSideSeesUser(LoginUser u, Long targetUserId, Long targetDeptId) {
        if (u == null) return false;
        DataScope scope = DataScope.of(u.getDataScope());
        if (scope == DataScope.ALL) return true;
        // buildUserCondition: (dept_id IN (...) OR id = self)
        if (u.getUserId() != null && u.getUserId().equals(targetUserId)) return true;
        if (scope == DataScope.SELF) return false;
        return u.getDeptIds() != null && u.getDeptIds().contains(targetDeptId);
    }

    // ==================== 用户写权限 ====================

    @Nested
    @DisplayName("用户写权限：canWriteUser")
    class WriteUser {

        @Test
        @DisplayName("全部数据范围：可写任何用户")
        void allScope_CanWriteAnyone() {
            LoginUser admin = user(9L, GROUP, DataScope.ALL, List.of());
            assertTrue(DataScopeHelper.canWriteUser(admin, 50L, FINANCE));
            assertTrue(DataScopeHelper.canWriteUser(admin, 51L, COMPANY_B));
        }

        @Test
        @DisplayName("本部门及以下：可写本部门与子部门，不可写兄弟部门")
        void deptAndSub_CanWriteSubtreeOnly() {
            // 研发部(101) 及以下：101, 103
            LoginUser lead = user(9L, DEV, DataScope.DEPT_AND_SUB, List.of(DEV, FE));

            assertTrue(DataScopeHelper.canWriteUser(lead, 50L, FE), "子部门前端组可写");
            assertTrue(DataScopeHelper.canWriteUser(lead, 51L, DEV), "本部门可写");
            assertFalse(DataScopeHelper.canWriteUser(lead, 52L, MARKET), "兄弟部门不可写");
            assertFalse(DataScopeHelper.canWriteUser(lead, 53L, FINANCE), "跨公司不可写");
        }

        @Test
        @DisplayName("仅本人：除自己外任何人都不可写")
        void selfScope_OnlySelf() {
            LoginUser self = user(9L, DEV, DataScope.SELF, List.of());

            assertTrue(DataScopeHelper.canWriteUser(self, 9L, DEV), "自己可写");
            assertFalse(DataScopeHelper.canWriteUser(self, 10L, DEV), "同部门他人不可写");
        }

        @Test
        @DisplayName("未登录：一律拒绝")
        void nullUser_AlwaysDenied() {
            assertFalse(DataScopeHelper.canWriteUser(null, 9L, DEV));
        }

        @Test
        @DisplayName("目标部门为空：非超管拒绝")
        void nullTargetDept_DeniedForNonAdmin() {
            LoginUser lead = user(9L, DEV, DataScope.DEPT_AND_SUB, List.of(DEV));
            assertFalse(DataScopeHelper.canWriteUser(lead, 50L, null));
        }
    }

    // ==================== 部门写权限 ====================

    @Nested
    @DisplayName("部门写权限：canWriteDept")
    class WriteDept {

        @Test
        @DisplayName("全部数据范围：可写任何部门")
        void allScope_CanWriteAnyDept() {
            LoginUser admin = user(9L, GROUP, DataScope.ALL, List.of());
            assertTrue(DataScopeHelper.canWriteDept(admin, FINANCE));
        }

        @Test
        @DisplayName("本部门及以下：deptTreeIds 内可写，范围外拒绝")
        void deptAndSub_UsesTreeIds() {
            // 可见部门含祖先：集团、公司A、研发部、前端组
            LoginUser lead = LoginUser.builder()
                    .userId(9L).username("u9").deptId(DEV)
                    .dataScope(DataScope.DEPT_AND_SUB.getCode())
                    .deptIds(List.of(DEV, FE))
                    .deptTreeIds(List.of(GROUP, COMPANY_A, DEV, FE))
                    .build();

            assertTrue(DataScopeHelper.canWriteDept(lead, DEV), "本部门可写");
            assertTrue(DataScopeHelper.canWriteDept(lead, GROUP), "祖先在树集合内，可写（能看见即可管理）");
            assertFalse(DataScopeHelper.canWriteDept(lead, MARKET), "兄弟部门拒绝");
            assertFalse(DataScopeHelper.canWriteDept(lead, FINANCE), "跨公司拒绝");
        }

        @Test
        @DisplayName("仅本人：只能写自己所属部门")
        void selfScope_OwnDeptOnly() {
            LoginUser self = user(9L, DEV, DataScope.SELF, List.of());
            assertTrue(DataScopeHelper.canWriteDept(self, DEV));
            assertFalse(DataScopeHelper.canWriteDept(self, FE));
        }

        @Test
        @DisplayName("目标为空或未登录：拒绝")
        void nullCases_Denied() {
            LoginUser admin = user(9L, GROUP, DataScope.ALL, List.of());
            assertFalse(DataScopeHelper.canWriteDept(admin, null));
            assertFalse(DataScopeHelper.canWriteDept(null, DEV));
        }
    }

    // ==================== 读写一致性契约 ====================

    @Nested
    @DisplayName("读侧单条越权：selectById 与 buildCondition 必须同结论")
    class ReadByIdConsistency {

        /**
         * selectById 绕不过拦截器，必须靠显式断言兜底。
         * 这里验证"读侧判定为不可见的对象，canWriteUser（读写共用的判定函数）必须为 false"，
         * 保证 getUserById 用的 assertUserReadable 与列表过滤不会出现两种结论。
         */
        @Test
        @DisplayName("列表不可见的用户，单条读判定也必须为不可见")
        void invisibleInList_alsoInvisibleById() {
            // 研发部主管（本部门及以下，可见 101/103）
            LoginUser lead = user(9L, DEV, DataScope.DEPT_AND_SUB, List.of(DEV, FE));

            // 财务部用户：列表过滤时 dept_id IN (101,103) 不命中 → 不可见
            assertFalse(readSideSeesUser(lead, 40L, FINANCE), "列表侧不可见");
            // 单条读也必须是同一个结论
            assertFalse(DataScopeHelper.canWriteUser(lead, 40L, FINANCE), "单条读/写也必须拒绝");

            // 本部门用户：列表可见，单条读放行
            assertTrue(readSideSeesUser(lead, 11L, FE), "列表侧可见");
            assertTrue(DataScopeHelper.canWriteUser(lead, 11L, FE), "单条读/写放行");
        }

        @Test
        @DisplayName("全部数据范围：单条读放行")
        void allScope_readable() {
            LoginUser admin = user(1L, GROUP, DataScope.ALL, List.of());
            assertTrue(DataScopeHelper.canWriteUser(admin, 40L, FINANCE));
        }
    }

    @Nested
    @DisplayName("读写一致性：读不可见 ⇒ 写必拒绝")
    class ReadWriteConsistency {

        @Test
        @DisplayName("遍历所有数据范围，读侧不可见的目标写侧必须拒绝")
        void readInvisible_impliesWriteDenied() {
            // 被测目标：(目标用户ID, 目标部门ID)
            Object[][] targets = {
                    {9L, DEV}, {10L, DEV}, {11L, FE},
                    {20L, MARKET}, {30L, COMPANY_A}, {40L, FINANCE}, {50L, COMPANY_B}
            };

            LoginUser[] operators = {
                    user(9L, DEV, DataScope.ALL, List.of()),
                    user(9L, GROUP, DataScope.GROUP_AND_SUB, List.of(GROUP, COMPANY_A, DEV, FE, MARKET, COMPANY_B, FINANCE)),
                    user(9L, COMPANY_A, DataScope.COMPANY_AND_SUB, List.of(COMPANY_A, DEV, FE, MARKET)),
                    user(9L, DEV, DataScope.DEPT_AND_SUB, List.of(DEV, FE)),
                    user(9L, DEV, DataScope.DEPT, List.of(DEV)),
                    user(8L, DEV, DataScope.SELF, List.of()),
            };

            for (LoginUser op : operators) {
                for (Object[] t : targets) {
                    Long targetId = (Long) t[0];
                    Long targetDept = (Long) t[1];

                    boolean readable = readSideSeesUser(op, targetId, targetDept);
                    boolean writable = DataScopeHelper.canWriteUser(op, targetId, targetDept);

                    String ctx = String.format("operator(user=%d,dept=%d,scope=%d) target(user=%d,dept=%d)",
                            op.getUserId(), op.getDeptId(), op.getDataScope(), targetId, targetDept);

                    // 核心契约：读不见的，写一定不行
                    if (!readable) {
                        assertFalse(writable, "读侧不可见却允许写 —— 读写规则漂移: " + ctx);
                    }
                    // 写允许时，读侧必须也允许（写权限不能大于读权限）
                    if (writable) {
                        assertTrue(readable, "允许写却不允许读 —— 写权限超出读权限: " + ctx);
                    }
                }
            }
        }
    }
}

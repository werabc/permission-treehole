package com.permission.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 数据权限范围（组织分级）
 *
 * 分级思路：按企业组织树的层级由宽到窄排列，code 越小范围越大。
 * 组织树约定（sys_dept.parent_id / ancestors 表达，dept_level 冗余存储便于查询）：
 *   L1 = 集团（根节点）
 *   L2 = 公司 / 事业部
 *   L3 = 部门
 *   L4+ = 小组 / 岗位组
 *
 * 兼容性说明：v1.1.0 之前仅有 5 级（1全部 2本部门及子部门 3本部门 4自定义 5仅本人），
 * v1.2.0 起扩展为 8 级，历史值由 migration_v1.2.0.sql 完成重映射：
 *   旧 5→新 8、旧 4→新 7、旧 3→新 6、旧 2→新 4（必须先降序迁移避免互相覆盖）
 */
@Getter
@AllArgsConstructor
public enum DataScope {

    /** 1 全部数据 —— 不受组织限制（如超级管理员、审计岗） */
    ALL(1, "全部数据"),

    /** 2 本集团及以下 —— 以组织树根节点为边界，覆盖全部下级公司/部门 */
    GROUP_AND_SUB(2, "本集团及以下"),

    /** 3 本公司及以下 —— 以所属公司(level=2)为边界 */
    COMPANY_AND_SUB(3, "本公司及以下"),

    /** 4 本部门及以下 —— 以所属部门为边界，含全部子部门 */
    DEPT_AND_SUB(4, "本部门及以下"),

    /** 5 本部门及以下（限 N 级）—— 解决"大部门下钻过深"的越权风险，N 由 sys_role.data_scope_level 配置 */
    DEPT_AND_SUB_LEVEL(5, "本部门及以下(限N级)"),

    /** 6 本部门 —— 仅本部门，不含子部门 */
    DEPT(6, "本部门"),

    /** 7 自定义部门 —— 由 sys_role_dept 指定，不含子部门（需含子部门请用 4/5） */
    CUSTOM(7, "自定义部门"),

    /** 8 仅本人 —— 只能看到自己创建/自己的数据 */
    SELF(8, "仅本人");

    private final int code;
    private final String desc;

    public static DataScope of(Integer code) {
        if (code == null) return SELF;
        return Arrays.stream(values())
                .filter(v -> v.code == code)
                .findFirst()
                .orElse(SELF);
    }

    /** 是否需要解析组织边界（用于登录期预计算可见部门集合） */
    public boolean needDeptResolve() {
        return this == GROUP_AND_SUB || this == COMPANY_AND_SUB
                || this == DEPT_AND_SUB || this == DEPT_AND_SUB_LEVEL
                || this == DEPT;
    }
}

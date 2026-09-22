-- ============================================================================
-- 权限管理系统 v1.2.0 —— 数据权限（组织分级）落地
-- ----------------------------------------------------------------------------
-- 背景：v1.1.0 之前 data_scope 字段只有"计算"没有"使用"，也没有 sys_role_dept 表，
--       数据权限完全是装饰；本脚本把整条链路补齐。
--
-- 变更内容：
--   1. sys_dept 增加 dept_level（组织层级：1集团 2公司 3部门 4+小组），并回填
--   2. sys_role 增加 data_scope_level（配合"本部门及以下(限N级)"使用）
--   3. 新增 sys_role_dept —— 自定义数据范围的部门关联表
--   4. 数据范围枚举由 5 级扩展为 8 级，历史数据重映射（必须降序执行）
--   5. 菜单：角色管理页的数据范围选项由前端枚举驱动，无需新增菜单
--
-- 兼容性：全部 IF NOT EXISTS / 幂等 UPDATE，可对已有库平滑升级
-- ============================================================================

USE permission_admin;

-- ----------------------------------------------------------------------------
-- 0. 幂等 DDL 辅助过程（MySQL 不支持 ADD COLUMN IF NOT EXISTS，用 information_schema 判断）
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_add_column_if_absent;
DELIMITER //
CREATE PROCEDURE sp_add_column_if_absent(
    IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_definition VARCHAR(500))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = p_column) THEN
        SET @ddl = CONCAT('ALTER TABLE ', p_table, ' ADD COLUMN ', p_column, ' ', p_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

-- ----------------------------------------------------------------------------
-- 1. sys_dept.dept_level：组织层级（冗余字段，避免每次递归计算）
--    回填规则：以 ancestors 中逗号数量推断层级（ancestors='0' → level 1）
-- ----------------------------------------------------------------------------
CALL sp_add_column_if_absent('sys_dept', 'dept_level',
    "TINYINT NOT NULL DEFAULT 1 COMMENT '组织层级: 1-集团 2-公司 3-部门 4+-小组'");

UPDATE sys_dept
SET dept_level = (LENGTH(ancestors) - LENGTH(REPLACE(ancestors, ',', '')) + 1)
WHERE deleted = 0;

-- ----------------------------------------------------------------------------
-- 2. sys_role.data_scope_level：层级深度 N（仅 data_scope=5 时生效）
-- ----------------------------------------------------------------------------
CALL sp_add_column_if_absent('sys_role', 'data_scope_level',
    "TINYINT NOT NULL DEFAULT 1 COMMENT '本部门及以下限N级(data_scope=5时生效)'");

-- ----------------------------------------------------------------------------
-- 2.1 Bug Fix：th_like 缺 update_time 列导致【点赞必定 500】（E2E 测试发现）
--     ThLike 实体继承 BaseEntity（含 update_time），但建表脚本漏了该列，
--     MyBatis-Plus 的 INSERT 会带上 update_time → Unknown column 'update_time'
-- ----------------------------------------------------------------------------
CALL sp_add_column_if_absent('th_like', 'update_time',
    "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");

-- ----------------------------------------------------------------------------
-- 3. sys_role_dept：自定义数据范围的部门集合
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_role_dept (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    role_id     BIGINT      NOT NULL COMMENT '角色ID',
    dept_id     BIGINT      NOT NULL COMMENT '部门ID',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_role_dept (role_id, dept_id),
    INDEX idx_rd_role (role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '角色-自定义数据范围部门关联表';

-- ----------------------------------------------------------------------------
-- 4. 数据范围枚举重映射（v1.1.0 的 5 级 → v1.2.0 的 8 级）
--    ⚠ 必须按【降序】执行：否则 2→4 之后，新的 4 会被下一句 4→7 再次迁移
-- ----------------------------------------------------------------------------
UPDATE sys_role SET data_scope = 8 WHERE data_scope = 5;  -- 旧"仅本人"
UPDATE sys_role SET data_scope = 7 WHERE data_scope = 4;  -- 旧"自定义"
UPDATE sys_role SET data_scope = 6 WHERE data_scope = 3;  -- 旧"本部门"
UPDATE sys_role SET data_scope = 4 WHERE data_scope = 2;  -- 旧"本部门及子部门"

-- 兜底：非法值统一收敛为"仅本人"，避免出现无匹配枚举
UPDATE sys_role SET data_scope = 8 WHERE data_scope NOT IN (1, 2, 3, 4, 5, 6, 7, 8);

-- ----------------------------------------------------------------------------
-- 5. 组织层级示例（按需调整；此处保证既有数据层级正确即可）
--    超级管理员角色保持"全部数据"
-- ----------------------------------------------------------------------------
UPDATE sys_role SET data_scope = 1 WHERE role_code = 'admin';

-- ----------------------------------------------------------------------------
-- 6. 校验输出（升级后人工确认一眼）
-- ----------------------------------------------------------------------------
SELECT r.id, r.role_name, r.role_code, r.data_scope, r.data_scope_level,
       CASE r.data_scope
           WHEN 1 THEN '全部数据' WHEN 2 THEN '本集团及以下' WHEN 3 THEN '本公司及以下'
           WHEN 4 THEN '本部门及以下' WHEN 5 THEN '本部门及以下(限N级)'
           WHEN 6 THEN '本部门' WHEN 7 THEN '自定义部门' WHEN 8 THEN '仅本人'
       END AS data_scope_desc
FROM sys_role r WHERE r.deleted = 0 ORDER BY r.id;

SELECT d.id, d.dept_name, d.parent_id, d.ancestors, d.dept_level
FROM sys_dept d WHERE d.deleted = 0 ORDER BY d.id;

-- ----------------------------------------------------------------------------
-- 7. 清理辅助过程
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_add_column_if_absent;

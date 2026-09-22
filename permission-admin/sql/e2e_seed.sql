-- ============================================================================
-- E2E 测试数据（数据权限 8 级矩阵）
-- 依赖：create_tables.sql + init.sql（提供菜单、基础部门 1-6、基础用户 1-3、角色 1-3）
--
-- 组织结构（在 init.sql 基础上补 7、8 两级）：
--   1 总公司            L1 集团    ancestors "0"
--   ├─ 2 技术部         L2 公司    "0,1"        ← 关键测试节点（company = 2）
--   │   ├─ 5 前端组     L3 部门    "0,1,2"
--   │   │   ├─ 7 前端一组 L4 小组  "0,1,2,5"
--   │   │   │   └─ 8 前端二组 L5   "0,1,2,5,7"
--   │   └─ 6 后端组     L3 部门    "0,1,2"
--   ├─ 3 产品部         L2 公司    "0,1"
--   └─ 4 运营部         L2 公司    "0,1"
--
-- 期望的"可见用户数"矩阵（共 11 个用户）：
--   admin(1 全部)            → 11
--   u_comp(3 本公司及以下,d2) → 10   （dept ∈ {2,5,6,7,8}）
--   u_dept(4 本部门及以下,d5) → 5    （dept ∈ {5,7,8}）
--   u_limit(5 限1级,d5)      → 4    （dept ∈ {5,7}，8 被层级裁剪）
--   u_depton(6 本部门,d5)    → 3    （dept = 5）
--   u_custom(7 自定义[3])    → 2    （dept = 3 + 本人）
--   u_self(8 仅本人)         → 1
-- ============================================================================

USE permission_admin_e2e;

-- 1. 补充两级组织（制造 L4/L5 深度，用于验证"限N级"裁剪）
INSERT IGNORE INTO sys_dept (id, dept_name, parent_id, ancestors, dept_level, sort, leader, status, deleted) VALUES
(7, '前端一组', 5, '0,1,2,5',   4, 1, 'G1 Leader', 1, 0),
(8, '前端二组', 7, '0,1,2,5,7', 5, 1, 'G2 Leader', 1, 0);

-- 2. 数据权限测试角色（复用新 8 级枚举编码）
INSERT IGNORE INTO sys_role (id, role_name, role_code, role_desc, data_scope, data_scope_level, status, deleted) VALUES
(101, '公司A管理员', 'comp_scope',  '本公司及以下',        3, 1, 1, 0),
(102, '部门管理员',  'dept_scope',  '本部门及以下',        4, 1, 1, 0),
(103, '限级管理员',  'limit_scope', '本部门及以下(限1级)',  5, 1, 1, 0),
(104, '部门专员',    'dept_only',   '本部门',             6, 1, 1, 0),
(105, '跨部门专员',  'custom_scope', '自定义部门(产品部)',  7, 1, 1, 0),
(106, '个人用户',    'self_scope',  '仅本人',             8, 1, 1, 0);

-- 3. 自定义数据范围：角色 105 指定部门 3（产品部）
INSERT IGNORE INTO sys_role_dept (role_id, dept_id) VALUES (105, 3);

-- 4. 测试用户（密码与 admin 相同：Admin@1234）
SET @pwd = (SELECT password FROM sys_user WHERE id = 1);

INSERT IGNORE INTO sys_user (id, username, password, nickname, email, dept_id, status, deleted) VALUES
(1000, 'u_comp',    @pwd, '公司级管理员', 'u_comp@e2e.com',    2, 1, 0),
(1001, 'u_dept',    @pwd, '部门级管理员', 'u_dept@e2e.com',    5, 1, 0),
(1002, 'u_limit',   @pwd, '限级管理员',   'u_limit@e2e.com',   5, 1, 0),
(1003, 'u_depton',  @pwd, '部门专员',     'u_depton@e2e.com',  5, 1, 0),
(1004, 'u_custom',  @pwd, '跨部门专员',   'u_custom@e2e.com',  2, 1, 0),
(1005, 'u_self',    @pwd, '个人用户',     'u_self@e2e.com',    2, 1, 0),
(1006, 'u_other',   @pwd, '产品部用户',   'u_other@e2e.com',   3, 1, 0),
(1007, 'u_g1',      @pwd, '前端一组用户', 'u_g1@e2e.com',      7, 1, 0),
(1008, 'u_g2',      @pwd, '前端二组用户', 'u_g2@e2e.com',      8, 1, 0);

-- 5. 角色分配（1006/1007/1008 用基础角色 3，仅影响其自身可见性，不影响被查询）
INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES
(1000, 101), (1001, 102), (1002, 103), (1003, 104),
(1004, 105), (1005, 106),
(1006, 3), (1007, 3), (1008, 3);

-- 5.1 给数据权限测试角色授予"用户管理-查询"菜单权限(菜单ID=2, system:user:list)
--     否则这些账号调用 /api/user/page 会先被 @PreAuthorize 拦掉，测不到数据权限本身
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(101, 2), (102, 2), (103, 2), (104, 2), (105, 2), (106, 2),
(101, 18), (102, 18), (103, 18), (104, 18), (105, 18), (106, 18);

-- 6. 校验组织层级回填是否正确
SELECT id, dept_name, parent_id, ancestors, dept_level FROM sys_dept WHERE deleted = 0 ORDER BY id;
SELECT id, username, dept_id FROM sys_user WHERE deleted = 0 ORDER BY id;
SELECT id, role_name, role_code, data_scope, data_scope_level FROM sys_role ORDER BY id;

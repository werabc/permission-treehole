-- ============================================================================
-- migration_v1.3.0.sql
--
-- 目标：把树洞管理端"只有 admin 能用"拆成细粒度权限，支持按角色授权。
--
-- 背景（两个叠加的根因，缺一不可）：
--   1) SecurityConfig 里 `.requestMatchers("/api/admin/**").hasAuthority("admin")`
--      是一道全局硬门，非 admin 角色在进入方法级鉴权之前就被 403 了；
--   2) 树洞管理 12 个菜单的 permission 全写死成 'admin'，即使放开硬门，
--      也没有任何细码可以授予 —— 授权体系无从下手。
--
-- 本脚本负责第 2 点（菜单/按钮权限数据 + 示例角色）；第 1 点已在代码中修改。
--
-- 幂等：全部使用 INSERT ... ON DUPLICATE KEY UPDATE / UPDATE，可重复执行。
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. 首页菜单补权限码（对应 DashboardController，原来是空的，非 admin 必 403）
-- ----------------------------------------------------------------------------
UPDATE sys_menu SET permission = 'system:dashboard:view' WHERE id = 37 AND (permission IS NULL OR permission = '');

-- ----------------------------------------------------------------------------
-- 2. 树洞管理 12 个菜单：'admin' → 细粒度列表码
-- ----------------------------------------------------------------------------
UPDATE sys_menu SET permission = 'th:user:list'         WHERE id = 28;
UPDATE sys_menu SET permission = 'th:moderation:list'   WHERE id = 29;
UPDATE sys_menu SET permission = 'th:post:list'         WHERE id = 30;
UPDATE sys_menu SET permission = 'th:comment:list'      WHERE id = 31;
UPDATE sys_menu SET permission = 'th:report:list'       WHERE id = 32;
UPDATE sys_menu SET permission = 'th:online:list'       WHERE id = 26;
UPDATE sys_menu SET permission = 'th:category:list'     WHERE id = 33;
UPDATE sys_menu SET permission = 'th:announcement:list' WHERE id = 34;
UPDATE sys_menu SET permission = 'th:analytics:view'    WHERE id = 35;
UPDATE sys_menu SET permission = 'th:settings:view'     WHERE id = 36;
UPDATE sys_menu SET permission = 'th:logs:view'         WHERE id = 39;
UPDATE sys_menu SET permission = 'th:sensitive:list'    WHERE id = 40;

-- ----------------------------------------------------------------------------
-- 3. 按钮级权限（挂在各菜单下，用于"能看页面但不能做危险操作"）
--    管理端页面用 v-permission="'xxx'" 控制按钮显隐
-- ----------------------------------------------------------------------------
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, icon, permission, sort, status, visible) VALUES
-- 用户管理
(50, 28, '用户详情',    'BUTTON', '', '', '', 'th:user:view',         1, 1, 1),
(51, 28, '禁言/解禁',   'BUTTON', '', '', '', 'th:user:mute',         2, 1, 1),
(52, 28, '封号/解封',   'BUTTON', '', '', '', 'th:user:ban',          3, 1, 1),
(53, 28, '解除处罚',    'BUTTON', '', '', '', 'th:user:release',      4, 1, 1),
(54, 28, '违规加减分',  'BUTTON', '', '', '', 'th:user:violation',    5, 1, 1),
-- 内容审核
(65, 29, '批量审核',    'BUTTON', '', '', '', 'th:moderation:audit',  1, 1, 1),
-- 帖子管理
(55, 30, '帖子详情',    'BUTTON', '', '', '', 'th:post:view',         1, 1, 1),
(56, 30, '帖子审核',    'BUTTON', '', '', '', 'th:post:audit',        2, 1, 1),
(57, 30, '帖子置顶',    'BUTTON', '', '', '', 'th:post:pin',          3, 1, 1),
(58, 30, '帖子隐藏',    'BUTTON', '', '', '', 'th:post:hide',         4, 1, 1),
(59, 30, '帖子删除',    'BUTTON', '', '', '', 'th:post:delete',       5, 1, 1),
-- 评论管理
(60, 31, '评论详情',    'BUTTON', '', '', '', 'th:comment:view',      1, 1, 1),
(61, 31, '评论隐藏',    'BUTTON', '', '', '', 'th:comment:hide',      2, 1, 1),
(62, 31, '评论删除',    'BUTTON', '', '', '', 'th:comment:delete',    3, 1, 1),
-- 举报管理
(63, 32, '举报详情',    'BUTTON', '', '', '', 'th:report:view',       1, 1, 1),
(64, 32, '举报处理',    'BUTTON', '', '', '', 'th:report:handle',     2, 1, 1),
-- 在线用户
(66, 26, '强制下线',    'BUTTON', '', '', '', 'th:online:kick',       1, 1, 1),
-- 分类管理
(67, 33, '分类新增',    'BUTTON', '', '', '', 'th:category:add',      1, 1, 1),
(68, 33, '分类编辑',    'BUTTON', '', '', '', 'th:category:edit',     2, 1, 1),
(69, 33, '分类删除',    'BUTTON', '', '', '', 'th:category:delete',   3, 1, 1),
-- 公告管理
(70, 34, '公告新增',    'BUTTON', '', '', '', 'th:announcement:add',  1, 1, 1),
(71, 34, '公告编辑',    'BUTTON', '', '', '', 'th:announcement:edit', 2, 1, 1),
(72, 34, '公告删除',    'BUTTON', '', '', '', 'th:announcement:delete',3, 1, 1),
-- 站点配置
(73, 36, '保存配置',    'BUTTON', '', '', '', 'th:settings:edit',     1, 1, 1),
-- 敏感词管理
(74, 40, '敏感词新增',  'BUTTON', '', '', '', 'th:sensitive:add',     1, 1, 1),
(75, 40, '敏感词编辑',  'BUTTON', '', '', '', 'th:sensitive:edit',    2, 1, 1),
(76, 40, '敏感词删除',  'BUTTON', '', '', '', 'th:sensitive:delete',  3, 1, 1),
(77, 40, '批量导入',    'BUTTON', '', '', '', 'th:sensitive:import',  4, 1, 1),
(78, 40, '刷新词库',    'BUTTON', '', '', '', 'th:sensitive:refresh', 5, 1, 1)
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id), menu_name = VALUES(menu_name), menu_type = VALUES(menu_type),
    permission = VALUES(permission), sort = VALUES(sort), status = VALUES(status);

-- ----------------------------------------------------------------------------
-- 4. 示例角色：内容审核员
--    只能看/审内容与举报，不能改站点配置、不能动词库、不能删帖
-- ----------------------------------------------------------------------------
INSERT INTO sys_role (id, role_name, role_code, role_desc, data_scope, data_scope_level, status) VALUES
(4, '内容审核员', 'th_auditor',  '树洞内容审核：审核待审内容、处理举报、必要时隐藏/删除', 1, 1, 1),
(5, '树洞运营',   'th_operator', '树洞运营：分类、公告、数据看板与在线用户维护',           1, 1, 1)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), role_desc = VALUES(role_desc), status = VALUES(status);

-- 审核员菜单：目录 + 首页 + 内容审核/帖子/评论/举报，以及这些页面里的安全操作按钮
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(4, 27), (4, 37),
(4, 29), (4, 30), (4, 31), (4, 32),
(4, 65),                                        -- 批量审核
(4, 55), (4, 56), (4, 57), (4, 58),             -- 帖子：查看/审核/置顶/隐藏（不含删除）
(4, 60), (4, 61),                               -- 评论：查看/隐藏（不含删除）
(4, 63), (4, 64)                                -- 举报：查看/处理
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- 运营菜单：目录 + 首页 + 分类/公告/数据看板/在线用户
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(5, 27), (5, 37),
(5, 33), (5, 34), (5, 35), (5, 26),
(5, 67), (5, 68), (5, 69),                      -- 分类：新增/编辑/删除
(5, 70), (5, 71), (5, 72),                      -- 公告：新增/编辑/删除
(5, 66)                                         -- 在线用户：强制下线
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- ----------------------------------------------------------------------------
-- 5. 演示账号，用于验证"非 admin 角色也能管树洞"
--    账号 th_auditor / th_operator
--    初始密码与 init.sql 里 admin 的种子密码相同（Admin@1234）
--    ★ 上线前必须改密，或直接删掉这两个演示账号
-- ----------------------------------------------------------------------------
INSERT IGNORE INTO sys_user (id, username, password, nickname, email, phone, status, dept_id) VALUES
(9101, 'th_auditor',  '$2a$10$YoZweBVj9TYL1Coop.9sTeQBvtEJMyGlYme2StFh2.mE60FkAMocW', '内容审核员', 'th_auditor@example.com',  '13800009101', 1, 4),
(9102, 'th_operator', '$2a$10$YoZweBVj9TYL1Coop.9sTeQBvtEJMyGlYme2StFh2.mE60FkAMocW', '树洞运营',   'th_operator@example.com', '13800009102', 1, 4);

INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES
(9101, 4),
(9102, 5);

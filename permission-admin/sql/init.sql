-- =============================================
-- 企业级权限管理系统 - 完整数据库初始化脚本
-- 包含：所有表结构 + 初始数据
-- 适用：首次部署（开发/测试/生产）
-- =============================================

CREATE DATABASE IF NOT EXISTS permission_admin
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE permission_admin;

-- =============================================
-- 1. 部门表
-- =============================================
DROP TABLE IF EXISTS sys_dept;
CREATE TABLE sys_dept (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '部门ID',
    dept_name   VARCHAR(100) NOT NULL COMMENT '部门名称',
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '父部门ID',
    ancestors   VARCHAR(500) NOT NULL DEFAULT '0' COMMENT '祖级列表',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    leader      VARCHAR(50)  DEFAULT NULL COMMENT '负责人',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
    email       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常 1-删除',
    INDEX idx_parent_id (parent_id),
    INDEX idx_ancestors (ancestors)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

-- =============================================
-- 2. 用户表
-- =============================================
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username        VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    password        VARCHAR(200) NOT NULL COMMENT '密码(BCrypt加密)',
    nickname        VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    email           VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    phone           VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    avatar          VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    sex             TINYINT      DEFAULT 0 COMMENT '性别: 0-未知 1-男 2-女',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    dept_id         BIGINT       DEFAULT NULL COMMENT '部门ID',
    last_login_time DATETIME     DEFAULT NULL COMMENT '最后登录时间',
    last_login_ip   VARCHAR(50)  DEFAULT NULL COMMENT '最后登录IP',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_dept_id (dept_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- =============================================
-- 3. 角色表
-- =============================================
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID',
    role_name   VARCHAR(50)  NOT NULL COMMENT '角色名称',
    role_code   VARCHAR(50)  NOT NULL UNIQUE COMMENT '角色编码',
    role_desc   VARCHAR(200) DEFAULT NULL COMMENT '角色描述',
    data_scope  TINYINT      DEFAULT 5 COMMENT '数据范围: 1-全部 2-本部门及子部门 3-本部门 4-自定义 5-仅本人',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- =============================================
-- 4. 菜单/权限表
-- =============================================
DROP TABLE IF EXISTS sys_menu;
CREATE TABLE sys_menu (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '菜单ID',
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '父菜单ID',
    menu_name   VARCHAR(50)  NOT NULL COMMENT '菜单名称',
    menu_type   VARCHAR(10)  NOT NULL DEFAULT 'MENU' COMMENT '菜单类型: CATALOG-目录 MENU-菜单 BUTTON-按钮',
    path        VARCHAR(200) DEFAULT NULL COMMENT '路由地址',
    component   VARCHAR(200) DEFAULT NULL COMMENT '组件路径',
    icon        VARCHAR(100) DEFAULT NULL COMMENT '图标',
    permission  VARCHAR(100) DEFAULT NULL COMMENT '权限标识(如 system:user:list)',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    visible     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否可见: 0-隐藏 1-可见',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_parent_id (parent_id),
    INDEX idx_permission (permission)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单权限表';

-- =============================================
-- 5. 用户-角色关联表
-- =============================================
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (user_id, role_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-角色关联表';

-- =============================================
-- 6. 角色-菜单关联表
-- =============================================
DROP TABLE IF EXISTS sys_role_menu;
CREATE TABLE sys_role_menu (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (role_id, menu_id),
    INDEX idx_role_id (role_id),
    INDEX idx_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色-菜单关联表';

-- =============================================
-- 7. 操作日志表
-- =============================================
DROP TABLE IF EXISTS sys_operation_log;
CREATE TABLE sys_operation_log (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    module         VARCHAR(50)  DEFAULT NULL COMMENT '操作模块',
    action         VARCHAR(100) DEFAULT NULL COMMENT '操作类型',
    method         VARCHAR(200) DEFAULT NULL COMMENT '执行方法',
    request_url    VARCHAR(200) DEFAULT NULL COMMENT '请求URL',
    request_method VARCHAR(10)  DEFAULT NULL COMMENT '请求方式',
    request_params TEXT         DEFAULT NULL COMMENT '请求参数',
    response_result LONGTEXT    DEFAULT NULL COMMENT '响应结果',
    execute_time   BIGINT       DEFAULT NULL COMMENT '执行时长(ms)',
    operator       VARCHAR(50)  DEFAULT NULL COMMENT '操作人',
    operator_ip    VARCHAR(50)  DEFAULT NULL COMMENT '操作IP',
    status         TINYINT      DEFAULT 1 COMMENT '状态: 0-失败 1-成功',
    error_msg      TEXT         DEFAULT NULL COMMENT '错误信息',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_operator (operator),
    INDEX idx_module (module),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- =============================================
-- 8. 登录日志表
-- =============================================
DROP TABLE IF EXISTS sys_login_log;
CREATE TABLE sys_login_log (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    username   VARCHAR(50)  DEFAULT NULL COMMENT '用户名',
    ip         VARCHAR(50)  DEFAULT NULL COMMENT '登录IP',
    location   VARCHAR(100) DEFAULT NULL COMMENT '登录地点',
    browser    VARCHAR(100) DEFAULT NULL COMMENT '浏览器',
    os         VARCHAR(50)  DEFAULT NULL COMMENT '操作系统',
    status     TINYINT      DEFAULT 1 COMMENT '状态: 0-失败 1-成功',
    message    TEXT DEFAULT NULL COMMENT '提示信息',
    login_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    INDEX idx_username (username),
    INDEX idx_login_time (login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';

-- =============================================
-- 9. 树洞用户表
-- =============================================
DROP TABLE IF EXISTS th_user;
CREATE TABLE th_user (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username        VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    password        VARCHAR(200) NOT NULL COMMENT '密码(BCrypt加密)',
    nickname        VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    avatar          VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    bio             VARCHAR(500) DEFAULT NULL COMMENT '个人简介',
    gender          TINYINT      DEFAULT 0 COMMENT '性别: 0-未知 1-男 2-女',
    email           VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0-封禁 1-正常',
    mute_until      DATETIME     DEFAULT NULL COMMENT '禁言截止时间',
    ban_until       DATETIME     DEFAULT NULL COMMENT '封号截止时间',
    post_count      INT          NOT NULL DEFAULT 0 COMMENT '发帖数',
    comment_count   INT          NOT NULL DEFAULT 0 COMMENT '评论数',
    violation_count INT          NOT NULL DEFAULT 0 COMMENT '违规次数',
    last_post_time  DATETIME     DEFAULT NULL COMMENT '最后发帖时间',
    last_login_ip   VARCHAR(50)  DEFAULT NULL COMMENT '最后登录IP',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_username (username),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='树洞用户表';

-- =============================================
-- 10. 树洞分类表
-- =============================================
DROP TABLE IF EXISTS th_category;
CREATE TABLE th_category (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID',
    name        VARCHAR(50)  NOT NULL COMMENT '分类名称',
    code        VARCHAR(50)  NOT NULL UNIQUE COMMENT '分类编码',
    icon        VARCHAR(100) DEFAULT NULL COMMENT '图标',
    description VARCHAR(200) DEFAULT NULL COMMENT '描述',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    post_count  INT          NOT NULL DEFAULT 0 COMMENT '帖子数',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_code (code),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='树洞分类表';

-- =============================================
-- 11. 树洞帖子表
-- =============================================
DROP TABLE IF EXISTS th_post;
CREATE TABLE th_post (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '帖子ID',
    user_id       BIGINT       NOT NULL COMMENT '用户ID',
    category_id   BIGINT       DEFAULT NULL COMMENT '分类ID',
    title         VARCHAR(200) DEFAULT NULL COMMENT '标题',
    content       TEXT         NOT NULL COMMENT '内容',
    images        JSON         DEFAULT NULL COMMENT '图片列表',
    is_anonymous  TINYINT      NOT NULL DEFAULT 0 COMMENT '是否匿名: 0-否 1-是',
    is_top        TINYINT      NOT NULL DEFAULT 0 COMMENT '是否置顶: 0-否 1-是',
    status        TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0-待审核 1-已通过 2-已拒绝',
    view_count    INT          NOT NULL DEFAULT 0 COMMENT '浏览数',
    like_count    INT          NOT NULL DEFAULT 0 COMMENT '点赞数',
    comment_count INT          NOT NULL DEFAULT 0 COMMENT '评论数',
    report_count  INT          NOT NULL DEFAULT 0 COMMENT '举报数',
    ip            VARCHAR(50)  DEFAULT NULL COMMENT '发布IP',
    audit_remark  VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
    auditor_id    BIGINT       DEFAULT NULL COMMENT '审核人ID',
    audit_time    DATETIME     DEFAULT NULL COMMENT '审核时间',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_user_id (user_id),
    INDEX idx_category_id (category_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time),
    INDEX idx_is_top (is_top)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='树洞帖子表';

-- =============================================
-- 12. 树洞评论表
-- =============================================
DROP TABLE IF EXISTS th_comment;
CREATE TABLE th_comment (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '评论ID',
    post_id      BIGINT       NOT NULL COMMENT '帖子ID',
    user_id      BIGINT       NOT NULL COMMENT '用户ID',
    parent_id    BIGINT       DEFAULT NULL COMMENT '父评论ID(回复)',
    reply_user_id BIGINT      DEFAULT NULL COMMENT '被回复人ID',
    content      TEXT         NOT NULL COMMENT '内容',
    is_anonymous TINYINT      NOT NULL DEFAULT 0 COMMENT '是否匿名',
    like_count   INT          NOT NULL DEFAULT 0 COMMENT '点赞数',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0-待审核 1-已通过 2-已拒绝',
    ip           VARCHAR(50)  DEFAULT NULL COMMENT '发布IP',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_post_id (post_id),
    INDEX idx_user_id (user_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='树洞评论表';

-- =============================================
-- 13. 树洞点赞表
-- =============================================
DROP TABLE IF EXISTS th_like;
CREATE TABLE th_like (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '点赞ID',
    user_id     BIGINT       NOT NULL COMMENT '用户ID',
    target_type VARCHAR(20)  NOT NULL COMMENT '目标类型: POST-帖子 COMMENT-评论',
    target_id   BIGINT       NOT NULL COMMENT '目标ID',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_user_target (user_id, target_type, target_id),
    INDEX idx_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='树洞点赞表';

-- =============================================
-- 14. 树洞举报表
-- =============================================
DROP TABLE IF EXISTS th_report;
CREATE TABLE th_report (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '举报ID',
    reporter_id     BIGINT       NOT NULL COMMENT '举报人ID',
    target_type     VARCHAR(20)  NOT NULL COMMENT '目标类型: POST-帖子 COMMENT-评论',
    target_id       BIGINT       NOT NULL COMMENT '目标ID',
    reason          VARCHAR(100) NOT NULL COMMENT '举报原因',
    description     TEXT         DEFAULT NULL COMMENT '举报描述',
    evidence_images JSON         DEFAULT NULL COMMENT '证据图片',
    status          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0-待处理 1-已处理-成立 2-已处理-不成立',
    handle_result   VARCHAR(500) DEFAULT NULL COMMENT '处理结果',
    handler_id      BIGINT       DEFAULT NULL COMMENT '处理人ID',
    handle_time     DATETIME     DEFAULT NULL COMMENT '处理时间',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_target (target_type, target_id),
    INDEX idx_status (status),
    INDEX idx_reporter (reporter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='树洞举报表';

-- =============================================
-- 15. 树洞通知表
-- =============================================
DROP TABLE IF EXISTS th_notification;
CREATE TABLE th_notification (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '通知ID',
    user_id     BIGINT NOT NULL COMMENT '接收用户ID',
    sender_id   BIGINT DEFAULT NULL COMMENT '发送者ID',
    type        VARCHAR(20) NOT NULL COMMENT '类型: LIKE-点赞 COMMENT-评论 REPORT-举报 SYSTEM-系统',
    target_type VARCHAR(20) DEFAULT NULL COMMENT '目标类型: POST-帖子 COMMENT-评论',
    target_id   BIGINT DEFAULT NULL COMMENT '目标ID',
    content     VARCHAR(500) NOT NULL COMMENT '通知内容',
    is_read     TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读: 0-未读 1-已读',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (is_read),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='树洞通知表';

-- =============================================
-- 16. 树洞公告表
-- =============================================
DROP TABLE IF EXISTS th_announcement;
CREATE TABLE th_announcement (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '公告ID',
    title        VARCHAR(200) NOT NULL COMMENT '标题',
    content      TEXT NOT NULL COMMENT '内容',
    type         VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '类型: NORMAL-普通 URGENT-紧急 MAINTENANCE-维护',
    status       TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-下架 1-上架',
    publish_time DATETIME DEFAULT NULL COMMENT '发布时间',
    expire_time  DATETIME DEFAULT NULL COMMENT '过期时间',
    creator_id   BIGINT NOT NULL COMMENT '创建者ID',
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted      TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_status (status),
    INDEX idx_type (type),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='树洞公告表';

-- =============================================
-- 17. 树洞用户行为日志表
-- =============================================
DROP TABLE IF EXISTS th_user_log;
CREATE TABLE th_user_log (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    user_id      BIGINT NOT NULL COMMENT '用户ID',
    action       VARCHAR(50) NOT NULL COMMENT '操作类型: LOGIN-登录 POST-发帖 COMMENT-评论 LIKE-点赞 REPORT-举报',
    target_type  VARCHAR(20) DEFAULT NULL COMMENT '目标类型: POST-帖子 COMMENT-评论',
    target_id    BIGINT DEFAULT NULL COMMENT '目标ID',
    detail       VARCHAR(500) DEFAULT NULL COMMENT '操作详情',
    ip           VARCHAR(50) DEFAULT NULL COMMENT '操作IP',
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_action (action),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='树洞用户行为日志表';

-- =============================================
-- 18. 树洞站点配置表
-- =============================================
DROP TABLE IF EXISTS th_setting;
CREATE TABLE th_setting (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '配置ID',
    config_key  VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    config_desc VARCHAR(200) DEFAULT NULL COMMENT '配置描述',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='树洞站点配置表';

-- =============================================
-- 初始数据
-- =============================================

-- 初始化部门
INSERT INTO sys_dept (id, dept_name, parent_id, ancestors, sort, leader, status) VALUES
(1, '总公司',    0, '0',   1, 'CEO',    1),
(2, '技术部',    1, '0,1', 1, 'CTO',    1),
(3, '产品部',    1, '0,1', 2, 'CPO',    1),
(4, '运营部',    1, '0,1', 3, 'COO',    1),
(5, '前端组',    2, '0,1,2', 1, '前端Leader', 1),
(6, '后端组',    2, '0,1,2', 2, '后端Leader', 1);

-- 初始化用户 (密码: Admin@1234, BCrypt加密)
INSERT INTO sys_user (id, username, password, nickname, email, phone, status, dept_id) VALUES
(1, 'admin',   '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '超级管理员', 'admin@example.com', '13800000001', 1, 1),
(2, 'tech',    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '技术负责人', 'tech@example.com',  '13800000002', 1, 2),
(3, 'backend', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '后端开发',   'backend@example.com','13800000003', 1, 6);

-- 初始化角色
INSERT INTO sys_role (id, role_name, role_code, role_desc, data_scope, status) VALUES
(1, '超级管理员', 'admin',    '拥有系统所有权限',      1, 1),
(2, '技术负责人', 'tech_lead','技术部门管理权限',      2, 1),
(3, '普通用户',   'user',     '基本查看权限',          5, 1);

-- 初始化菜单
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, icon, permission, sort, status, visible) VALUES
-- 系统管理(目录)
(1,  0,  '系统管理',  'CATALOG', '/system',      '',               'Setting',  '',                     1, 1, 1),
-- 用户管理
(2,  1,  '用户管理',  'MENU',    '/system/user', 'system/user/index', 'User',     'system:user:list',       1, 1, 1),
(3,  2,  '用户查询',  'BUTTON',  '',             '',                 '',         'system:user:query',       1, 1, 1),
(4,  2,  '用户新增',  'BUTTON',  '',             '',                 '',         'system:user:add',         2, 1, 1),
(5,  2,  '用户编辑',  'BUTTON',  '',             '',                 '',         'system:user:edit',        3, 1, 1),
(6,  2,  '用户删除',  'BUTTON',  '',             '',                 '',         'system:user:delete',      4, 1, 1),
(7,  2,  '重置密码',  'BUTTON',  '',             '',                 '',         'system:user:reset-pwd',   5, 1, 1),
-- 角色管理
(8,  1,  '角色管理',  'MENU',    '/system/role', 'system/role/index', 'UserFilled','system:role:list',      2, 1, 1),
(9,  8,  '角色查询',  'BUTTON',  '',             '',                 '',         'system:role:query',       1, 1, 1),
(10, 8,  '角色新增',  'BUTTON',  '',             '',                 '',         'system:role:add',         2, 1, 1),
(11, 8,  '角色编辑',  'BUTTON',  '',             '',                 '',         'system:role:edit',        3, 1, 1),
(12, 8,  '角色删除',  'BUTTON',  '',             '',                 '',         'system:role:delete',      4, 1, 1),
-- 菜单管理
(13, 1,  '菜单管理',  'MENU',    '/system/menu', 'system/menu/index', 'Menu',     'system:menu:list',       3, 1, 1),
(14, 13, '菜单查询',  'BUTTON',  '',             '',                 '',         'system:menu:query',       1, 1, 1),
(15, 13, '菜单新增',  'BUTTON',  '',             '',                 '',         'system:menu:add',         2, 1, 1),
(16, 13, '菜单编辑',  'BUTTON',  '',             '',                 '',         'system:menu:edit',        3, 1, 1),
(17, 13, '菜单删除',  'BUTTON',  '',             '',                 '',         'system:menu:delete',      4, 1, 1),
-- 部门管理
(18, 1,  '部门管理',  'MENU',    '/system/dept', 'system/dept/index', 'OfficeBuilding','system:dept:list',  4, 1, 1),
(19, 18, '部门查询',  'BUTTON',  '',             '',                 '',         'system:dept:query',       1, 1, 1),
(20, 18, '部门新增',  'BUTTON',  '',             '',                 '',         'system:dept:add',         2, 1, 1),
(21, 18, '部门编辑',  'BUTTON',  '',             '',                 '',         'system:dept:edit',        3, 1, 1),
(22, 18, '部门删除',  'BUTTON',  '',             '',                 '',         'system:dept:delete',      4, 1, 1),
-- 日志管理
(23, 1,  '日志管理',  'CATALOG', '/log',         '',                 'Document', '',                     5, 1, 1),
(24, 23, '操作日志',  'MENU',    '/log/operation','log/operation/index','Tickets','system:log:list',       1, 1, 1),
(25, 23, '登录日志',  'MENU',    '/log/login',   'log/login/index',  'Key',      'system:log:list',         2, 1, 1),
-- 在线用户
(26, 1,  '在线用户',  'MENU',    '/system/online', null,            'Monitor',  'admin',                 6, 1, 1),
-- 树洞管理
(27, 1,  '树洞管理',  'CATALOG', '/admin/treehole', null,           'ChatLineRound', '',                7, 1, 1),
(28, 27, '用户管理',  'MENU',    '/admin/th/user', null,            'User',     'admin',                 1, 1, 1),
(29, 27, '内容审核',  'MENU',    '/admin/th/moderation', null,       'View',     'admin',                 2, 1, 1),
(30, 27, '帖子管理',  'MENU',    '/admin/th/post', null,            'Document', 'admin',                 3, 1, 1),
(31, 27, '评论管理',  'MENU',    '/admin/th/comment', null,          'ChatDotRound', 'admin',             4, 1, 1),
(32, 27, '举报管理',  'MENU',    '/admin/th/report', null,           'Warning',  'admin',                 5, 1, 1),
(33, 27, '分类管理',  'MENU',    '/admin/th/category', null,         'Files',    'admin',                 6, 1, 1),
(34, 27, '公告管理',  'MENU',    '/admin/th/announcement', null,     'Bell',     'admin',                 7, 1, 1),
(35, 27, '数据分析',  'MENU',    '/admin/th/analytics', null,        'DataAnalysis', 'admin',             8, 1, 1),
(36, 27, '站点配置',  'MENU',    '/admin/th/settings', null,         'Setting',  'admin',                 9, 1, 1),
-- 仪表盘
(37, 0,  '首页',      'MENU',    '/dashboard',   null,               'HomeFilled','',                    0, 1, 1),
-- 个人信息
(38, 0,  '个人信息',  'MENU',    '/profile',     null,               'User',     '',                      1, 1, 1);

-- 分配用户角色
INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1), -- admin -> 超级管理员
(2, 2), -- tech -> 技术负责人
(3, 3); -- backend -> 普通用户

-- 分配角色菜单: 超级管理员拥有所有权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu;

-- 技术负责人权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 1), (2, 2), (2, 3), (2, 8), (2, 9), (2, 13), (2, 14),
(2, 18), (2, 19), (2, 23), (2, 24), (2, 25);

-- 普通用户权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(3, 1), (3, 2), (3, 3), (3, 8), (3, 9), (3, 13), (3, 14),
(3, 18), (3, 19), (3, 23), (3, 24), (3, 25);

-- 初始化树洞分类
INSERT INTO th_category (id, name, code, icon, description, sort, status, post_count) VALUES
(1, '情感树洞', 'emotion', '💕', '分享内心情感故事', 1, 1, 0),
(2, '生活随笔', 'life', '📝', '记录生活点滴', 2, 1, 0),
(3, '匿名吐槽', 'rant', '💨', '安全匿名吐槽空间', 3, 1, 0),
(4, '求助问答', 'help', '🙋', '提问与互助', 4, 1, 0),
(5, '趣味分享', 'fun', '🎉', '有趣的内容分享', 5, 1, 0);

-- 初始化树洞站点配置
INSERT INTO th_setting (config_key, config_value, config_desc) VALUES
('site_name', '树洞', '站点名称'),
('site_description', '匿名分享你的故事', '站点描述'),
('post_need_audit', '1', '发帖是否需要审核: 0-否 1-是'),
('comment_need_audit', '0', '评论是否需要审核: 0-否 1-是'),
('max_post_per_day', '10', '每日最大发帖数'),
('max_comment_per_day', '50', '每日最大评论数');

-- 初始化树洞公告
INSERT INTO th_announcement (id, title, content, type, status, publish_time, creator_id) VALUES
(1, '欢迎来到树洞！', '这是一个匿名分享的平台，请遵守社区规范，文明发言。', 'NORMAL', 1, NOW(), 1);

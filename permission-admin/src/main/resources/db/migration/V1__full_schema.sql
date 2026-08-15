-- ============================================
-- 树洞系统完整数据库 Schema
-- 字符集: utf8mb4
-- 排序规则: utf8mb4_unicode_ci
-- ============================================

CREATE DATABASE IF NOT EXISTS permission_admin
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE permission_admin;

-- ============================================
-- 系统管理表 (sys_ 前缀)
-- ============================================

CREATE TABLE IF NOT EXISTS sys_dept (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '部门ID',
    dept_name VARCHAR(100) NOT NULL COMMENT '部门名称',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父部门ID',
    ancestors VARCHAR(500) NOT NULL DEFAULT '0' COMMENT '祖级路径',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    leader VARCHAR(50) DEFAULT NULL COMMENT '负责人',
    phone VARCHAR(20) DEFAULT NULL,
    email VARCHAR(100) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(200) NOT NULL COMMENT 'BCrypt密码',
    nickname VARCHAR(50) DEFAULT NULL,
    email VARCHAR(100) DEFAULT NULL,
    phone VARCHAR(20) DEFAULT NULL,
    avatar VARCHAR(500) DEFAULT NULL,
    gender TINYINT DEFAULT 0,
    dept_id BIGINT DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0-禁用 1-启用',
    last_login_time DATETIME DEFAULT NULL,
    last_login_ip VARCHAR(50) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_dept_id (dept_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_code VARCHAR(50) NOT NULL COMMENT '角色编码',
    role_desc VARCHAR(200) DEFAULT NULL,
    data_scope TINYINT NOT NULL DEFAULT 5 COMMENT '数据范围',
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    parent_id BIGINT NOT NULL DEFAULT 0,
    menu_name VARCHAR(50) NOT NULL COMMENT '菜单名称',
    menu_type VARCHAR(10) NOT NULL COMMENT 'CATALOG/MENU/BUTTON',
    path VARCHAR(200) DEFAULT NULL,
    component VARCHAR(200) DEFAULT NULL,
    icon VARCHAR(100) DEFAULT NULL,
    permission VARCHAR(100) DEFAULT NULL,
    sort INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    visible TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单表';

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联';

CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id),
    KEY idx_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联';

CREATE TABLE IF NOT EXISTS sys_login_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50),
    ip VARCHAR(50),
    location VARCHAR(100),
    browser VARCHAR(100),
    os VARCHAR(50),
    status TINYINT DEFAULT 1,
    message VARCHAR(200),
    login_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_username (username),
    KEY idx_login_time (login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志';

CREATE TABLE IF NOT EXISTS sys_operation_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    module VARCHAR(50),
    action VARCHAR(100),
    method VARCHAR(200),
    request_url VARCHAR(200),
    request_method VARCHAR(10),
    request_params TEXT,
    response_result TEXT,
    execute_time BIGINT,
    operator VARCHAR(50),
    operator_ip VARCHAR(50),
    status TINYINT DEFAULT 1,
    error_msg TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_operator (operator),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志';

-- ============================================
-- 树洞系统表 (th_ 前缀)
-- ============================================

CREATE TABLE IF NOT EXISTS th_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(200) NOT NULL COMMENT 'BCrypt密码',
    nickname VARCHAR(50) NOT NULL COMMENT '昵称',
    avatar VARCHAR(500) DEFAULT 'default.png',
    bio VARCHAR(200) DEFAULT NULL,
    gender TINYINT DEFAULT 0 COMMENT '0-未知 1-男 2-女',
    email VARCHAR(100) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0-封禁 1-正常',
    mute_until DATETIME DEFAULT NULL COMMENT '禁言截止',
    ban_until DATETIME DEFAULT NULL COMMENT '封号截止',
    violation_count INT DEFAULT 0,
    post_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    last_login_time DATETIME DEFAULT NULL,
    last_login_ip VARCHAR(50) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='树洞用户表';

CREATE TABLE IF NOT EXISTS th_category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL,
    icon VARCHAR(100),
    description VARCHAR(200),
    sort INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    post_count INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分类表';

CREATE TABLE IF NOT EXISTS th_post (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    category_id BIGINT DEFAULT NULL,
    title VARCHAR(200) DEFAULT NULL,
    content TEXT NOT NULL,
    images JSON DEFAULT NULL,
    is_anonymous TINYINT DEFAULT 0,
    is_top TINYINT DEFAULT 0,
    status TINYINT DEFAULT 0 COMMENT '0-待审核 1-已通过 2-已拒绝 3-已删除',
    view_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    report_count INT DEFAULT 0,
    ip VARCHAR(50),
    audit_remark VARCHAR(500),
    auditor_id BIGINT DEFAULT NULL,
    audit_time DATETIME DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user (user_id),
    KEY idx_category (category_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time),
    KEY idx_top_time (is_top, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子表';

CREATE TABLE IF NOT EXISTS th_comment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id BIGINT DEFAULT NULL,
    reply_user_id BIGINT DEFAULT NULL,
    content TEXT NOT NULL,
    is_anonymous TINYINT DEFAULT 0,
    like_count INT DEFAULT 0,
    status TINYINT DEFAULT 1 COMMENT '0-隐藏 1-显示',
    ip VARCHAR(50),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_post (post_id),
    KEY idx_user (user_id),
    KEY idx_parent (parent_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

CREATE TABLE IF NOT EXISTS th_like (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL COMMENT 'POST/COMMENT',
    target_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_target (user_id, target_type, target_id),
    KEY idx_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点赞表';

CREATE TABLE IF NOT EXISTS th_report (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reporter_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL COMMENT 'POST/COMMENT/USER',
    target_id BIGINT NOT NULL,
    reason VARCHAR(50) NOT NULL COMMENT 'SPAM/ABUSE/PORN/VIOLENCE/OTHER',
    description VARCHAR(500),
    evidence_images JSON,
    status TINYINT DEFAULT 0 COMMENT '0-待处理 1-已处理 2-已驳回',
    handle_result VARCHAR(500),
    handler_id BIGINT DEFAULT NULL,
    handle_time DATETIME DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_target (target_type, target_id),
    KEY idx_status (status),
    KEY idx_reporter (reporter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='举报表';

CREATE TABLE IF NOT EXISTS th_attachment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    post_id BIGINT DEFAULT NULL,
    file_name VARCHAR(200) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_size BIGINT DEFAULT 0,
    sort INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_post (post_id),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='附件表';

-- ============================================
-- 初始化数据
-- ============================================

-- 默认部门
INSERT IGNORE INTO sys_dept (id, dept_name, parent_id, ancestors, sort, leader) VALUES
(1, '总公司', 0, '0', 1, 'CEO'),
(2, '技术部', 1, '0,1', 1, 'CTO'),
(3, '产品部', 1, '0,1', 2, 'CPO');

-- 默认管理员 (密码: Admin@1234 的 BCrypt 哈希)
INSERT IGNORE INTO sys_user (id, username, password, nickname, dept_id, status) VALUES
(1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '超级管理员', 1, 1);

-- 默认角色
INSERT IGNORE INTO sys_role (id, role_name, role_code, role_desc, data_scope) VALUES
(1, '超级管理员', 'admin', '拥有系统所有权限', 1),
(2, '普通用户', 'user', '基本查看权限', 5);

-- 默认菜单
INSERT IGNORE INTO sys_menu (id, parent_id, menu_name, menu_type, path, icon, permission, sort) VALUES
(1, 0, '系统管理', 'CATALOG', '/system', 'Setting', NULL, 1),
(2, 1, '用户管理', 'MENU', '/system/user', 'User', 'system:user:list', 1),
(3, 1, '角色管理', 'MENU', '/system/role', 'UserFilled', 'system:role:list', 2),
(4, 1, '菜单管理', 'MENU', '/system/menu', 'Menu', 'system:menu:list', 3),
(5, 1, '部门管理', 'MENU', '/system/dept', 'OfficeBuilding', 'system:dept:list', 4),
(6, 1, '日志管理', 'CATALOG', '/log', 'Document', NULL, 5),
(7, 6, '操作日志', 'MENU', '/log/operation', 'Tickets', 'system:log:list', 1),
(8, 6, '登录日志', 'MENU', '/log/login', 'Key', 'system:log:list', 2),
(9, 1, '在线用户', 'MENU', '/system/online', 'Monitor', 'admin', 6),
(10, 0, '树洞管理', 'CATALOG', '/admin/treehole', 'ChatLineRound', NULL, 2),
(11, 10, '帖子管理', 'MENU', '/admin/treehole/post', 'Document', 'admin', 1),
(12, 10, '评论管理', 'MENU', '/admin/treehole/comment', 'ChatDotRound', 'admin', 2),
(13, 10, '举报管理', 'MENU', '/admin/treehole/report', 'Warning', 'admin', 3),
(14, 10, '分类管理', 'MENU', '/admin/treehole/category', 'Files', 'admin', 4),
(15, 10, '用户管理', 'MENU', '/admin/treehole/user', 'User', 'admin', 5),
(16, 10, '数据统计', 'MENU', '/admin/treehole/statistics', 'DataAnalysis', 'admin', 6);

-- 授权管理员所有菜单
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) SELECT 1, id FROM sys_menu WHERE deleted = 0;
INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- 默认分类
INSERT IGNORE INTO th_category (id, name, code, icon, description, sort) VALUES
(1, '情感树洞', 'emotion', 'Heart', '分享你的情感故事', 1),
(2, '生活随笔', 'life', 'Coffee', '记录生活点滴', 2),
(3, '匿名吐槽', 'rant', 'ChatLineRound', '匿名吐槽不开心', 3),
(4, '求助问答', 'help', 'QuestionFilled', '有问题来这里', 4),
(5, '趣味分享', 'fun', 'Star', '分享有趣的事', 5);

USE permission_admin;

CREATE TABLE IF NOT EXISTS sys_dept (
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

CREATE TABLE IF NOT EXISTS sys_user (
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

CREATE TABLE IF NOT EXISTS sys_role (
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

CREATE TABLE IF NOT EXISTS sys_menu (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '菜单ID',
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '父菜单ID',
    menu_name   VARCHAR(50)  NOT NULL COMMENT '菜单名称',
    menu_type   VARCHAR(10)  NOT NULL DEFAULT 'MENU' COMMENT '菜单类型: CATALOG-目录 MENU-菜单 BUTTON-按钮',
    path        VARCHAR(200) DEFAULT NULL COMMENT '路由地址',
    component   VARCHAR(200) DEFAULT NULL COMMENT '组件路径',
    icon        VARCHAR(100) DEFAULT NULL COMMENT '图标',
    permission  VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    visible     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否可见: 0-隐藏 1-可见',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_parent_id (parent_id),
    INDEX idx_permission (permission)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单权限表';

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-角色关联表';

CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (role_id, menu_id),
    INDEX idx_role_id (role_id),
    INDEX idx_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色-菜单关联表';

CREATE TABLE IF NOT EXISTS sys_operation_log (
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

CREATE TABLE IF NOT EXISTS sys_login_log (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    username   VARCHAR(50)  DEFAULT NULL COMMENT '用户名',
    ip         VARCHAR(50)  DEFAULT NULL COMMENT '登录IP',
    location   VARCHAR(100) DEFAULT NULL COMMENT '登录地点',
    browser    VARCHAR(100) DEFAULT NULL COMMENT '浏览器',
    os         VARCHAR(50)  DEFAULT NULL COMMENT '操作系统',
    status     TINYINT      DEFAULT 1 COMMENT '状态: 0-失败 1-成功',
    message    VARCHAR(200) DEFAULT NULL COMMENT '提示信息',
    login_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    INDEX idx_username (username),
    INDEX idx_login_time (login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';

-- =============================================
-- 树洞模块
-- =============================================

CREATE TABLE IF NOT EXISTS th_user (
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

CREATE TABLE IF NOT EXISTS th_category (
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

CREATE TABLE IF NOT EXISTS th_post (
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

CREATE TABLE IF NOT EXISTS th_comment (
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

CREATE TABLE IF NOT EXISTS th_like (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '点赞ID',
    user_id     BIGINT       NOT NULL COMMENT '用户ID',
    target_type VARCHAR(20)  NOT NULL COMMENT '目标类型: POST-帖子 COMMENT-评论',
    target_id   BIGINT       NOT NULL COMMENT '目标ID',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_user_target (user_id, target_type, target_id, deleted),
    INDEX idx_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='树洞点赞表';

CREATE TABLE IF NOT EXISTS th_report (
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

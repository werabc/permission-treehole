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

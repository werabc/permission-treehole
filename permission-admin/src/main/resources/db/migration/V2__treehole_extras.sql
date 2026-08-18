-- ============================================
-- Flyway V2: 树洞系统补充表
-- 说明: V1 创建了核心表，V3 创建了 th_category/post/comment/report/like
--       本迁移补充 V1/V3 遗漏的 th_user/notification/announcement/user_log/setting 表
-- ============================================

-- 树洞用户表
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

-- 通知表
CREATE TABLE IF NOT EXISTS th_notification (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL COMMENT '接收用户ID',
    content     VARCHAR(500) NOT NULL COMMENT '通知内容',
    target_type VARCHAR(20) DEFAULT NULL COMMENT '目标类型',
    target_id   BIGINT DEFAULT NULL COMMENT '目标ID',
    is_read     TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 公告表
CREATE TABLE IF NOT EXISTS th_announcement (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(200) NOT NULL COMMENT '标题',
    content     TEXT NOT NULL COMMENT '内容',
    type        VARCHAR(20) NOT NULL DEFAULT 'NOTICE' COMMENT '类型: NOTICE/WARNING/ACTIVITY',
    status      TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    publish_time DATETIME DEFAULT NULL COMMENT '发布时间',
    expire_time DATETIME DEFAULT NULL COMMENT '过期时间',
    creator_id  BIGINT COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT NOT NULL DEFAULT 0,
    INDEX idx_status (status, expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 用户行为日志表
CREATE TABLE IF NOT EXISTS th_user_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    action      VARCHAR(50) NOT NULL COMMENT '操作',
    detail      VARCHAR(500) DEFAULT NULL,
    ip          VARCHAR(50) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 站点配置表
CREATE TABLE IF NOT EXISTS th_setting (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key  VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT,
    config_desc VARCHAR(200) DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

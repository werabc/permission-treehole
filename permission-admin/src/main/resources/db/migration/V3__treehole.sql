-- ============================================
-- Flyway V3: 树洞核心业务表
-- ============================================

-- 分类表
CREATE TABLE IF NOT EXISTS `th_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `code` VARCHAR(50) NOT NULL COMMENT '分类编码',
    `icon` VARCHAR(100) DEFAULT NULL COMMENT '图标',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '描述',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0-禁用 1-启用',
    `post_count` INT NOT NULL DEFAULT 0 COMMENT '帖子数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 帖子表
CREATE TABLE IF NOT EXISTS `th_post` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '作者ID',
    `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
    `title` VARCHAR(200) DEFAULT NULL COMMENT '标题',
    `content` TEXT NOT NULL COMMENT '内容',
    `images` JSON DEFAULT NULL COMMENT '图片URL数组',
    `is_anonymous` TINYINT NOT NULL DEFAULT 1 COMMENT '是否匿名',
    `is_top` TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-待审核 1-已通过 2-已拒绝',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览数',
    `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    `comment_count` INT NOT NULL DEFAULT 0 COMMENT '评论数',
    `report_count` INT NOT NULL DEFAULT 0 COMMENT '举报数',
    `ip` VARCHAR(50) DEFAULT NULL,
    `audit_remark` VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
    `auditor_id` BIGINT DEFAULT NULL COMMENT '审核人ID',
    `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_category` (`category_id`),
    KEY `idx_status` (`status`),
    KEY `idx_is_top` (`is_top`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 评论表
CREATE TABLE IF NOT EXISTS `th_comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `post_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL COMMENT '评论人ID',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父评论ID',
    `reply_user_id` BIGINT DEFAULT NULL COMMENT '被回复人ID',
    `content` TEXT NOT NULL,
    `is_anonymous` TINYINT NOT NULL DEFAULT 1,
    `like_count` INT NOT NULL DEFAULT 0,
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0-隐藏 1-显示',
    `ip` VARCHAR(50) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_post` (`post_id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 举报表
CREATE TABLE IF NOT EXISTS `th_report` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `target_type` VARCHAR(20) NOT NULL COMMENT 'POST/COMMENT',
    `target_id` BIGINT NOT NULL,
    `reason` VARCHAR(50) NOT NULL COMMENT '举报原因',
    `description` VARCHAR(500) DEFAULT NULL,
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-待处理 1-已成立 2-不成立',
    `result` VARCHAR(500) DEFAULT NULL COMMENT '处理结果',
    `reporter_id` BIGINT DEFAULT NULL COMMENT '举报人ID',
    `handler_id` BIGINT DEFAULT NULL COMMENT '处理人ID',
    `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_target` (`target_type`, `target_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 点赞表（使用IP防重复，支持匿名用户）
CREATE TABLE IF NOT EXISTS `th_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `target_type` VARCHAR(20) NOT NULL COMMENT 'POST/COMMENT',
    `target_id` BIGINT NOT NULL,
    `ip` VARCHAR(50) NOT NULL COMMENT '用户IP（防重复）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_target_ip` (`target_type`, `target_id`, `ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 附件表
CREATE TABLE IF NOT EXISTS `th_attachment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `file_name` VARCHAR(200) NOT NULL,
    `file_path` VARCHAR(500) NOT NULL,
    `file_size` INT NOT NULL DEFAULT 0,
    `mime_type` VARCHAR(100) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 初始化分类
INSERT IGNORE INTO `th_category` (`id`, `name`, `code`, `icon`, `description`, `sort`, `status`) VALUES
(1, '情感树洞', 'emotion', 'Heart', '分享你的情感故事', 1, 1),
(2, '生活随笔', 'life', 'Coffee', '记录生活点滴', 2, 1),
(3, '匿名吐槽', 'rant', 'ChatLineRound', '安全匿名吐槽空间', 3, 1),
(4, '求助问答', 'help', 'QuestionFilled', '提问与互助', 4, 1),
(5, '趣味分享', 'fun', 'Star', '有趣的内容分享', 5, 1);

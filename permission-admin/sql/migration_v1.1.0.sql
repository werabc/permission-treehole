-- ============================================================================
-- 树洞平台 升级 v1.1.0 —— 内容治理闭环 + C 端功能补全
-- ----------------------------------------------------------------------------
-- 变更内容：
--   1. 新增 th_sensitive_word（敏感词库，支持 L1 拦截 / L2 转审）
--   2. 新增 th_collect（用户收藏）
--   3. 补齐站点配置项：敏感词开关、违规处罚阈值、注册/匿名开关
--   4. 菜单：新增"敏感词管理"并授权给超级管理员
-- 兼容性：全部使用 IF NOT EXISTS / INSERT IGNORE，可对已有库平滑升级
-- ============================================================================

USE permission_admin;

-- ----------------------------------------------------------------------------
-- 1. 敏感词库
--    level: 1-直接拦截(不入库)  2-警告转人工审核
--    category: 便于后台按类别维护（广告/辱骂/色情/违法/其他）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS th_sensitive_word (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY   COMMENT '主键',
    word        VARCHAR(100) NOT NULL               COMMENT '敏感词',
    level       TINYINT      NOT NULL DEFAULT 1     COMMENT '等级：1-拦截 2-转审',
    category    VARCHAR(50)           DEFAULT '其他' COMMENT '分类：广告/辱骂/色情/违法/其他',
    status      TINYINT      NOT NULL DEFAULT 1     COMMENT '状态：1-启用 0-停用',
    remark      VARCHAR(200)          DEFAULT NULL  COMMENT '备注',
    deleted     TINYINT      NOT NULL DEFAULT 0     COMMENT '逻辑删除：0-正常 1-已删除',
    create_by   VARCHAR(50)           DEFAULT NULL  COMMENT '创建人',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_word (word),
    INDEX idx_sw_status (status),
    INDEX idx_sw_category (category)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '树洞敏感词库';

-- ----------------------------------------------------------------------------
-- 2. 用户收藏
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS th_collect (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY   COMMENT '主键',
    user_id     BIGINT       NOT NULL               COMMENT '用户ID',
    target_type VARCHAR(20)  NOT NULL DEFAULT 'POST' COMMENT '收藏对象类型：POST',
    target_id   BIGINT       NOT NULL               COMMENT '收藏对象ID',
    deleted     TINYINT      NOT NULL DEFAULT 0     COMMENT '逻辑删除：0-正常 1-已删除',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_target (user_id, target_type, target_id),
    INDEX idx_collect_user (user_id, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '树洞用户收藏表';

-- ----------------------------------------------------------------------------
-- 3. 站点配置补齐（治理策略全部可后台调参，避免硬编码）
-- ----------------------------------------------------------------------------
INSERT IGNORE INTO th_setting (config_key, config_value, config_desc) VALUES
('sensitive_filter_enabled', '1',  '是否开启敏感词过滤: 0-否 1-是'),
('violation_mute_threshold','5',  '违规分达到该值自动禁言'),
('violation_mute_days',     '3',  '自动禁言天数'),
('violation_ban_threshold', '10', '违规分达到该值自动封号'),
('violation_ban_days',      '7',  '自动封号天数'),
('report_violation_score',  '2',  '一次举报成立计多少违规分'),
('register_enabled',        '1',  '是否开放注册: 0-否 1-是'),
('anonymous_enabled',       '1',  '是否允许匿名发帖: 0-否 1-是');

-- ----------------------------------------------------------------------------
-- 4. 初始化敏感词（示例词库，后台可增删改）
-- ----------------------------------------------------------------------------
INSERT IGNORE INTO th_sensitive_word (word, level, category, remark) VALUES
('加微信',    1, '广告', '引流广告'),
('加我微信',  1, '广告', '引流广告'),
('代刷',      1, '广告', '违规代刷'),
('刷单',      1, '广告', '违规交易'),
('兼职日结',  2, '广告', '疑似招嫖/诈骗，转人工'),
('博彩',      1, '违法', '赌博类'),
('赌博',      1, '违法', '赌博类'),
('色情',      1, '色情', '色情类'),
('裸聊',      1, '色情', '色情类'),
('傻子',      2, '辱骂', '轻度辱骂，转人工'),
('去死',      1, '辱骂', '严重辱骂');

-- ----------------------------------------------------------------------------
-- 5. 菜单：敏感词管理（父菜单 27 = 树洞管理）
-- ----------------------------------------------------------------------------
INSERT IGNORE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, icon, permission, sort, status, visible) VALUES
(40, 27, '敏感词管理', 'MENU', '/admin/th/sensitive', null, 'Filter', 'admin', 12, 1, 1);

-- 超级管理员（role_id=1）授权
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (1, 40);

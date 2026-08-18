-- =============================================
-- 数据库迁移脚本 v1.0.1
-- 修复内容：
-- 1. 修复 th_like 表唯一索引（移除 deleted 字段）
-- 2. 为关联表添加 create_time 字段
-- =============================================

USE permission_admin;

-- =============================================
-- 1. 修复 th_like 表唯一索引
-- 原唯一索引 (user_id, target_type, target_id, deleted) 导致软删除后同一用户可重复点赞
-- 修复为 (user_id, target_type, target_id) 保证数据一致性
-- =============================================
ALTER TABLE th_like DROP INDEX uk_user_target;
ALTER TABLE th_like ADD UNIQUE KEY uk_user_target (user_id, target_type, target_id);

-- =============================================
-- 2. 为关联表添加 create_time 字段
-- =============================================
ALTER TABLE sys_user_role ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE sys_role_menu ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

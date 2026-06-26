-- Novel Website Expansion Tables
-- Execute against permission_admin database

-- 1. Novel Category (小说分类)
CREATE TABLE IF NOT EXISTS novel_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(50) NOT NULL COMMENT '分类名称',
    category_desc VARCHAR(200) COMMENT '分类描述',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态: 1=启用, 0=禁用',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除'
);

-- 2. Novel (小说)
CREATE TABLE IF NOT EXISTS novel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL COMMENT '小说标题',
    author_id BIGINT NOT NULL COMMENT '作者ID',
    author_name VARCHAR(50) COMMENT '作者笔名',
    category_id BIGINT COMMENT '分类ID',
    cover_url VARCHAR(500) COMMENT '封面图片URL',
    intro TEXT COMMENT '简介',
    status TINYINT DEFAULT 1 COMMENT '状态: 1=连载中, 2=已完结',
    word_count BIGINT DEFAULT 0 COMMENT '总字数',
    click_count BIGINT DEFAULT 0 COMMENT '点击量',
    like_count BIGINT DEFAULT 0 COMMENT '收藏数',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除'
);

-- 3. Novel Chapter (章节)
CREATE TABLE IF NOT EXISTS novel_chapter (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    novel_id BIGINT NOT NULL COMMENT '小说ID',
    chapter_title VARCHAR(200) NOT NULL COMMENT '章节标题',
    chapter_num INT NOT NULL COMMENT '章节序号',
    content MEDIUMTEXT COMMENT '章节内容',
    word_count INT DEFAULT 0 COMMENT '字数',
    is_free TINYINT DEFAULT 1 COMMENT '是否免费: 1=免费, 0=付费',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除'
);

-- 4. User Bookshelf (书架)
CREATE TABLE IF NOT EXISTS user_bookshelf (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    novel_id BIGINT NOT NULL COMMENT '小说ID',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除'
);

-- 5. Reading History (阅读历史)
CREATE TABLE IF NOT EXISTS reading_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    novel_id BIGINT NOT NULL COMMENT '小说ID',
    chapter_id BIGINT COMMENT '最后阅读章节ID',
    chapter_title VARCHAR(200) COMMENT '最后阅读章节标题',
    create_time DATETIME COMMENT '首次阅读时间',
    update_time DATETIME COMMENT '最后阅读时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除'
);

-- 6. Novel Comment (评论)
CREATE TABLE IF NOT EXISTS novel_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    novel_id BIGINT NOT NULL COMMENT '小说ID',
    chapter_id BIGINT COMMENT '章节ID(NULL=小说评论区)',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    user_name VARCHAR(50) COMMENT '用户名',
    content TEXT NOT NULL COMMENT '评论内容',
    parent_id BIGINT DEFAULT 0 COMMENT '父评论ID(0=顶级评论)',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除'
);

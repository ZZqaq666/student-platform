-- 书籍资源管理模块数据库脚本
-- 创建时间: 2026-03-27
-- 描述: 包含书籍表扩展、章节表、图片资源表的创建和初始化

-- 设置字符集
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 1. 扩展book表字段
-- ============================================
ALTER TABLE book
    ADD COLUMN IF NOT EXISTS price DECIMAL(10,2) DEFAULT NULL COMMENT '价格',
    ADD COLUMN IF NOT EXISTS publish_date DATE DEFAULT NULL COMMENT '出版日期',
    ADD COLUMN IF NOT EXISTS page_count INT DEFAULT NULL COMMENT '页数',
    ADD COLUMN IF NOT EXISTS language VARCHAR(20) DEFAULT '中文' COMMENT '语言',
    ADD COLUMN IF NOT EXISTS view_count INT DEFAULT 0 COMMENT '浏览次数';

-- 为book表添加管理相关索引
CREATE INDEX IF NOT EXISTS idx_book_status ON book(status);
CREATE INDEX IF NOT EXISTS idx_book_category ON book(category);
CREATE INDEX IF NOT EXISTS idx_book_created_at ON book(created_at);

-- ============================================
-- 2. 创建book_chapter章节表
-- ============================================
DROP TABLE IF EXISTS book_chapter;

CREATE TABLE book_chapter (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '章节ID',
    book_id BIGINT NOT NULL COMMENT '书籍ID',
    title VARCHAR(200) NOT NULL COMMENT '章节标题',
    content LONGTEXT COMMENT '章节内容（富文本HTML）',
    sort_order INT DEFAULT 0 COMMENT '排序号，越小越靠前',
    parent_id BIGINT DEFAULT NULL COMMENT '父章节ID，支持章节层级',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE/DELETED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_book_id (book_id),
    INDEX idx_sort_order (sort_order),
    INDEX idx_parent_id (parent_id),
    INDEX idx_status (status),
    CONSTRAINT fk_chapter_book FOREIGN KEY (book_id) REFERENCES book(id) ON DELETE CASCADE,
    CONSTRAINT fk_chapter_parent FOREIGN KEY (parent_id) REFERENCES book_chapter(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='书籍章节表';

-- ============================================
-- 3. 创建book_image图片资源表
-- ============================================
DROP TABLE IF EXISTS book_image;

CREATE TABLE book_image (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '图片ID',
    book_id BIGINT NOT NULL COMMENT '书籍ID',
    image_url VARCHAR(500) NOT NULL COMMENT '图片URL（OSS地址）',
    image_type VARCHAR(20) DEFAULT 'COVER' COMMENT '图片类型：COVER封面/CONTENT内容/GALLERY图集',
    file_size INT DEFAULT NULL COMMENT '文件大小（字节）',
    mime_type VARCHAR(50) DEFAULT NULL COMMENT 'MIME类型',
    is_primary TINYINT(1) DEFAULT 0 COMMENT '是否主图：0否/1是',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_book_id (book_id),
    INDEX idx_image_type (image_type),
    INDEX idx_is_primary (is_primary),
    INDEX idx_sort_order (sort_order),
    CONSTRAINT fk_image_book FOREIGN KEY (book_id) REFERENCES book(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='书籍图片资源表';

-- ============================================
-- 4. 创建操作日志表（可选，用于审计）
-- ============================================
DROP TABLE IF EXISTS book_operation_log;

CREATE TABLE book_operation_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    book_id BIGINT DEFAULT NULL COMMENT '书籍ID',
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型：CREATE/UPDATE/DELETE/IMPORT/EXPORT',
    operation_desc VARCHAR(500) DEFAULT NULL COMMENT '操作描述',
    operator_id BIGINT NOT NULL COMMENT '操作人ID',
    operator_name VARCHAR(100) DEFAULT NULL COMMENT '操作人姓名',
    request_data JSON DEFAULT NULL COMMENT '请求数据',
    response_data JSON DEFAULT NULL COMMENT '响应数据',
    ip_address VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_book_id (book_id),
    INDEX idx_operation_type (operation_type),
    INDEX idx_operator_id (operator_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='书籍操作日志表';

-- ============================================
-- 5. 插入示例章节数据（用于测试）
-- ============================================
-- 注意：需要先确保book表有数据，以下SQL假设book表已有id为1的书籍

-- 为高等数学（上册）添加章节示例
INSERT INTO book_chapter (book_id, title, content, sort_order, parent_id, status) VALUES
(1, '第一章 函数与极限', '<h2>1.1 映射与函数</h2><p>函数是数学中最基本的概念之一...</p><h2>1.2 数列的极限</h2><p>极限是微积分的基础概念...</p>', 1, NULL, 'ACTIVE'),
(1, '第二章 导数与微分', '<h2>2.1 导数的概念</h2><p>导数描述了函数在某一点的变化率...</p><h2>2.2 函数的求导法则</h2><p>掌握基本求导法则对于计算导数至关重要...</p>', 2, NULL, 'ACTIVE'),
(1, '第三章 微分中值定理与导数的应用', '<h2>3.1 微分中值定理</h2><p>微分中值定理是连接函数局部性质和整体性质的桥梁...</p>', 3, NULL, 'ACTIVE');

-- ============================================
-- 6. 验证表结构
-- ============================================
SELECT 'book表字段验证' as check_item, COUNT(*) as column_count 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'book';

SELECT 'book_chapter表已创建' as check_item, 1 as status;

SELECT 'book_image表已创建' as check_item, 1 as status;

-- ============================================
-- 7. 创建视图（可选，用于简化查询）
-- ============================================
-- 书籍完整信息视图（包含主图URL）
CREATE OR REPLACE VIEW v_book_info AS
SELECT 
    b.*,
    bi.image_url as primary_image_url
FROM book b
LEFT JOIN book_image bi ON b.id = bi.book_id AND bi.is_primary = 1 AND bi.image_type = 'COVER'
WHERE b.status != 'DELETED';

-- 书籍章节统计视图
CREATE OR REPLACE VIEW v_book_chapter_stats AS
SELECT 
    b.id as book_id,
    b.title as book_title,
    COUNT(bc.id) as chapter_count,
    MAX(bc.updated_at) as last_chapter_update
FROM book b
LEFT JOIN book_chapter bc ON b.id = bc.book_id AND bc.status = 'ACTIVE'
WHERE b.status != 'DELETED'
GROUP BY b.id, b.title;

SET FOREIGN_KEY_CHECKS = 1;

-- 脚本执行完成
SELECT '书籍资源管理模块数据库脚本执行完成' as message, NOW() as executed_at;

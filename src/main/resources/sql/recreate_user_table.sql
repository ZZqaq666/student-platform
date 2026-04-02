-- 删除user表并重新创建
-- 注意：此操作会删除所有用户数据，请确保已备份

-- 禁用外键检查
SET FOREIGN_KEY_CHECKS = 0;

-- 删除相关表的外键约束（如果存在）
ALTER TABLE user_book DROP FOREIGN KEY IF EXISTS fk_user_book_user;
ALTER TABLE wrong_book DROP FOREIGN KEY IF EXISTS fk_wrong_book_user;
ALTER TABLE qa_history DROP FOREIGN KEY IF EXISTS qa_history_ibfk_1;

-- 删除user表
DROP TABLE IF EXISTS user;

-- 重新创建user表
CREATE TABLE user (
  id bigint NOT NULL AUTO_INCREMENT,
  username varchar(50) NOT NULL,
  password varchar(255) NOT NULL,
  email varchar(100) DEFAULT NULL,
  phone varchar(20) DEFAULT NULL,
  nickname varchar(50) DEFAULT NULL,
  avatar varchar(255) DEFAULT NULL,
  role varchar(20) NOT NULL DEFAULT 'STUDENT',
  status varchar(20) NOT NULL DEFAULT 'ACTIVE',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username),
  UNIQUE KEY uk_email (email),
  UNIQUE KEY uk_phone (phone),
  KEY idx_role (role),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=4;

-- 重新添加外键约束（使用逻辑外键，不在数据库层面强制）
-- 注意：这里不添加物理外键，改用逻辑外键在应用层维护

-- 启用外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- 验证表结构
DESCRIBE user;
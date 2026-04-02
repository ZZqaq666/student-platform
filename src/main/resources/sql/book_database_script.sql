-- Book数据库操作脚本
-- 1. 安全删除所有数据记录（不删除表结构）
-- 2. 表结构创建语句
-- 3. 示例数据插入语句

-- 首先删除数据（按外键依赖顺序）
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM wrong_book;
DELETE FROM user_book;
DELETE FROM book;

SET FOREIGN_KEY_CHECKS = 1;

-- 表结构创建语句

-- book表结构
CREATE TABLE IF NOT EXISTS book (
  id bigint NOT NULL AUTO_INCREMENT,
  title varchar(200) NOT NULL,
  author varchar(100) DEFAULT NULL,
  publisher varchar(100) DEFAULT NULL,
  isbn varchar(20) DEFAULT NULL,
  cover_image varchar(255) DEFAULT NULL,
  description text DEFAULT NULL,
  subject varchar(50) DEFAULT NULL,
  grade varchar(20) DEFAULT NULL,
  version varchar(50) DEFAULT NULL,
  status varchar(20) NOT NULL DEFAULT 'ACTIVE',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  category varchar(50) DEFAULT NULL,
  course_type varchar(50) DEFAULT NULL,
  major varchar(100) DEFAULT NULL,
  semester varchar(20) DEFAULT NULL,
  university_level varchar(20) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_title (title),
  KEY idx_subject (subject),
  KEY idx_grade (grade)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- user_book表结构
CREATE TABLE IF NOT EXISTS user_book (
  id bigint NOT NULL AUTO_INCREMENT,
  user_id bigint NOT NULL,
  book_id bigint NOT NULL,
  progress decimal(5,2) NOT NULL DEFAULT '0.00',
  last_read_page int DEFAULT '0',
  last_read_at datetime DEFAULT NULL,
  status varchar(20) NOT NULL DEFAULT 'ACTIVE',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user_id (user_id),
  KEY idx_book_id (book_id),
  CONSTRAINT fk_user_book_book FOREIGN KEY (book_id) REFERENCES book (id) ON DELETE CASCADE,
  CONSTRAINT fk_user_book_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- wrong_book表结构
CREATE TABLE IF NOT EXISTS wrong_book (
  id bigint NOT NULL AUTO_INCREMENT,
  user_id bigint NOT NULL,
  exercise_id bigint NOT NULL,
  answer_record_id bigint DEFAULT NULL,
  wrong_count int NOT NULL DEFAULT '1',
  correct_count int NOT NULL DEFAULT '0',
  last_attempt_at datetime DEFAULT NULL,
  master_status varchar(20) NOT NULL DEFAULT 'NOT_MASTERED',
  notes text DEFAULT NULL,
  tags varchar(255) DEFAULT NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user_id (user_id),
  KEY idx_exercise_id (exercise_id),
  KEY idx_answer_record_id (answer_record_id),
  KEY idx_master_status (master_status),
  CONSTRAINT fk_wrong_book_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 示例数据插入

-- 禁用外键约束，确保数据插入成功
SET FOREIGN_KEY_CHECKS = 0;

-- 插入book表示例数据
INSERT INTO book (title, author, publisher, subject, category, course_type, major, semester, university_level, description, version) VALUES
('高等数学（上册）', '同济大学', '高等教育出版社', '数学', '主教材', '专业核心课', '数学', '大一', '本科', '高等数学上册，包含函数、极限、微积分等内容', '2024版'),
('高等数学（下册）', '同济大学', '高等教育出版社', '数学', '主教材', '专业核心课', '数学', '大一', '本科', '高等数学下册，包含多元微积分、级数等内容', '2024版'),
('线性代数', '同济大学', '高等教育出版社', '数学', '主教材', '专业基础课', '数学', '大二', '本科', '线性代数基础教程，包含矩阵、向量空间等内容', '2024版'),
('概率论与数理统计', '浙江大学', '高等教育出版社', '数学', '主教材', '专业基础课', '数学', '大二', '本科', '概率论与数理统计基础教程', '2024版'),
('大学物理（上册）', '清华大学出版社', '清华大学出版社', '物理', '主教材', '专业核心课', '物理', '大一', '本科', '大学物理上册，包含力学、热学等内容', '2024版'),
('大学物理（下册）', '清华大学出版社', '清华大学出版社', '物理', '主教材', '专业核心课', '物理', '大二', '本科', '大学物理下册，包含电磁学、光学等内容', '2024版'),
('数据结构与算法', '严蔚敏', '清华大学出版社', '计算机科学', '主教材', '专业核心课', '计算机科学与技术', '大二', '本科', '数据结构与算法基础教程', '2024版'),
('计算机网络', '谢希仁', '电子工业出版社', '计算机科学', '主教材', '专业核心课', '计算机科学与技术', '大三', '本科', '计算机网络基础教程', '2024版'),
('操作系统原理', '汤小丹', '西安电子科技大学出版社', '计算机科学', '主教材', '专业核心课', '计算机科学与技术', '大三', '本科', '操作系统原理基础教程', '2024版'),
('数据库系统概论', '王珊', '高等教育出版社', '计算机科学', '主教材', '专业核心课', '计算机科学与技术', '大三', '本科', '数据库系统基础教程', '2024版'),
('算法导论', 'Thomas H. Cormen', '机械工业出版社', '计算机科学', '参考书', '专业核心课', '计算机科学与技术', '大三', '本科', '算法导论经典教材', '2024版'),
('离散数学', '耿素云', '清华大学出版社', '计算机科学', '主教材', '专业基础课', '计算机科学与技术', '大二', '本科', '离散数学基础教程', '2024版'),
('经济学原理', '高鸿业', '中国人民大学出版社', '经济学', '主教材', '专业核心课', '经济学', '大二', '本科', '经济学原理基础教程', '2024版'),
('管理学原理', '周三多', '高等教育出版社', '管理学', '主教材', '专业核心课', '管理学', '大二', '本科', '管理学原理基础教程', '2024版'),
('市场营销学', '郭国庆', '高等教育出版社', '管理学', '主教材', '专业核心课', '市场营销', '大三', '本科', '市场营销学基础教程', '2024版'),
('财务会计', '陈国辉', '东北财经大学出版社', '管理学', '主教材', '专业核心课', '会计学', '大二', '本科', '财务会计基础教程', '2024版'),
('马克思主义基本原理', '本书编写组', '高等教育出版社', '通识教育', '主教材', '通识课', '通识教育', '大一', '本科', '马克思主义基本原理概论', '2024版'),
('大学英语', '李观仪', '上海外语教育出版社', '英语', '主教材', '通识课', '通识教育', '大一', '本科', '大学英语基础教程', '2024版'),
('大学语文', '王步高', '高等教育出版社', '语文', '主教材', '通识课', '通识教育', '大一', '本科', '大学语文基础教程', '2024版'),
('体育与健康', '体育教研组', '高等教育出版社', '体育', '主教材', '通识课', '通识教育', '大一', '本科', '体育与健康基础教程', '2024版');

-- 插入user_book表示例数据（假设用户ID为1）
INSERT INTO user_book (user_id, book_id, progress, last_read_page, last_read_at) VALUES
(1, 1, 85.50, 245, '2024-03-20 14:30:00'),
(1, 2, 60.25, 180, '2024-03-18 10:15:00'),
(1, 7, 90.00, 320, '2024-03-19 16:45:00'),
(1, 8, 45.75, 135, '2024-03-17 09:20:00'),
(1, 17, 100.00, 300, '2024-03-15 15:30:00');

-- 插入wrong_book表示例数据（假设用户ID为1，习题ID为1-5）
INSERT INTO wrong_book (user_id, exercise_id, wrong_count, correct_count, last_attempt_at, master_status, notes, tags) VALUES
(1, 1, 2, 1, '2024-03-20 14:30:00', 'IN_PROGRESS', '需要加强对极限概念的理解', '数学,极限,高等数学'),
(1, 2, 3, 0, '2024-03-19 10:15:00', 'NOT_MASTERED', '微积分应用题目需要多练习', '数学,微积分,高等数学'),
(1, 3, 1, 2, '2024-03-18 16:45:00', 'MASTERED', '矩阵运算已经掌握', '数学,线性代数,矩阵'),
(1, 4, 2, 1, '2024-03-17 09:20:00', 'IN_PROGRESS', '概率计算需要注意细节', '数学,概率论,统计'),
(1, 5, 1, 1, '2024-03-16 15:30:00', 'IN_PROGRESS', '网络协议理解还不够深入', '计算机,网络,协议');

-- 启用外键约束
SET FOREIGN_KEY_CHECKS = 1;

-- 验证数据插入结果
SELECT 'Book表数据量:' AS table_name, COUNT(*) AS count FROM book UNION ALL
SELECT 'User_book表数据量:', COUNT(*) FROM user_book UNION ALL
SELECT 'Wrong_book表数据量:', COUNT(*) FROM wrong_book;

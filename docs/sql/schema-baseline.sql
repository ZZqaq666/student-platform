-- ScholarPal 数据库结构基线（自动生成，请勿手工编辑）
--
-- 来源：本地开发库 student_platform（mysqldump --no-data）
-- 重新生成：
--   .tools/mysql-8.0.28-winx64/bin/mysqldump.exe -uroot -h127.0.0.1 \n--     --no-data --skip-triggers --skip-routines --skip-lock-tables --skip-comments \n--     student_platform | sed -E "s/DEFINER=`[^`]*`@`[^`]*`//g"
--
-- 说明：此前仓库里没有业务表 DDL（init.sql 实为 Nacos 架构 + 仅 note 表），
--   本文件是第一步——把结构纳入版本控制；接入 Flyway 时以它为 V1 基线。

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `answer_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `answer_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '答题记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `exercise_id` bigint NOT NULL COMMENT '练习题ID',
  `user_answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '用户答案',
  `is_correct` tinyint(1) NOT NULL COMMENT '是否正确',
  `score_obtained` int NOT NULL DEFAULT '0' COMMENT '得分',
  `time_spent` int DEFAULT NULL COMMENT '耗时（秒）',
  `attempt_count` int NOT NULL DEFAULT '1' COMMENT '尝试次数',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_exercise_id` (`exercise_id`),
  KEY `idx_is_correct` (`is_correct`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `answer_record_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `answer_record_ibfk_2` FOREIGN KEY (`exercise_id`) REFERENCES `exercise` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='答题记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `book`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '书籍ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '书名',
  `author` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '作者',
  `publisher` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '出版社',
  `isbn` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ISBN编号',
  `cover_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '封面图片URL',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '书籍描述',
  `subject` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '学科：MATH/PHYSICS/CHEMISTRY/BIOLOGY/CHINESE/ENGLISH/HISTORY/GEOGRAPHY/POLITICS',
  `grade` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '年级：GRADE_1/GRADE_2/.../GRADE_12',
  `version` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '版本',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/DISABLED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'TEXTBOOK, REFERENCE, SUPPLEMENTARY',
  `course_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'CORE, FOUNDATION, ELECTIVE, GENERAL',
  `major` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Major field: Computer Science, Economics, Management, etc.',
  `semester` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Semester: Year 1, Year 2, Year 3, Year 4',
  `university_level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'UNDERGRADUATE, POSTGRADUATE',
  `price` decimal(10,2) DEFAULT NULL COMMENT '价格',
  `publish_date` date DEFAULT NULL COMMENT '出版日期',
  `page_count` int DEFAULT NULL COMMENT '页数',
  `language` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '中文' COMMENT '语言',
  `view_count` int DEFAULT '0' COMMENT '浏览次数',
  PRIMARY KEY (`id`),
  KEY `idx_subject` (`subject`),
  KEY `idx_grade` (`grade`),
  KEY `idx_title` (`title`),
  KEY `idx_book_title` (`title`(100)),
  KEY `idx_book_category` (`category`),
  KEY `idx_book_status` (`status`),
  KEY `idx_book_subject` (`subject`),
  KEY `idx_book_author` (`author`(50)),
  KEY `idx_book_isbn` (`isbn`),
  KEY `idx_book_created_at` (`created_at`),
  KEY `idx_book_category_status` (`category`,`status`),
  KEY `idx_book_subject_category` (`subject`,`category`),
  KEY `idx_book_status_created` (`status`,`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=225 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='书籍表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `book_chapter`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book_chapter` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '章节ID',
  `book_id` bigint NOT NULL COMMENT '书籍ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '章节标题',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '章节内容（富文本HTML）',
  `sort_order` int DEFAULT '0' COMMENT '排序号，越小越靠前',
  `parent_id` bigint DEFAULT NULL COMMENT '父章节ID，支持章节层级',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE/DELETED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_book_id` (`book_id`),
  KEY `idx_sort_order` (`sort_order`),
  KEY `idx_status` (`status`),
  KEY `idx_chapter_book_sort` (`book_id`,`sort_order`),
  KEY `idx_chapter_book_status` (`book_id`,`status`),
  CONSTRAINT `fk_chapter_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_chapter_parent` FOREIGN KEY (`parent_id`) REFERENCES `book_chapter` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=10104 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='书籍章节表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `book_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book_image` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '图片ID',
  `book_id` bigint NOT NULL COMMENT '书籍ID',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '图片URL（OSS地址）',
  `image_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'COVER' COMMENT '图片类型：COVER封面/CONTENT内容/GALLERY图集',
  `file_size` int DEFAULT NULL COMMENT '文件大小（字节）',
  `mime_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'MIME类型',
  `is_primary` tinyint(1) DEFAULT '0' COMMENT '是否主图：0否/1是',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_book_id` (`book_id`),
  KEY `idx_is_primary` (`is_primary`),
  KEY `idx_sort_order` (`sort_order`),
  KEY `idx_image_book_id` (`book_id`),
  KEY `idx_image_type` (`image_type`),
  KEY `idx_image_is_primary` (`is_primary`),
  KEY `idx_image_sort_order` (`sort_order`),
  KEY `idx_image_book_primary` (`book_id`,`is_primary`),
  KEY `idx_image_book_type` (`book_id`,`image_type`),
  CONSTRAINT `fk_image_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=99 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='书籍图片资源表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `book_knowledge_point`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book_knowledge_point` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '知识点ID',
  `book_id` bigint NOT NULL COMMENT '书籍ID',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '知识点类型：RELATED/HIGH_FREQUENCY',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '知识点内容',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_book_id_type` (`book_id`,`type`),
  KEY `idx_book_id` (`book_id`)
) ENGINE=InnoDB AUTO_INCREMENT=121 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='书籍知识点表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `book_operation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book_operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `book_id` bigint DEFAULT NULL COMMENT '书籍ID',
  `operation_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作类型：CREATE/UPDATE/DELETE/IMPORT/EXPORT',
  `operation_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作描述',
  `operator_id` bigint NOT NULL COMMENT '操作人ID',
  `operator_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作人姓名',
  `request_data` json DEFAULT NULL COMMENT '请求数据',
  `response_data` json DEFAULT NULL COMMENT '响应数据',
  `ip_address` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'IP地址',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_book_id` (`book_id`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='书籍操作日志表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `conversation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `conversation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `session_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会话ID（外部使用的唯一标识）',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '会话标题',
  `last_message_preview` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最后一条消息预览',
  `message_count` int DEFAULT '0' COMMENT '消息数量',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-活跃，DELETED-已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_id` (`session_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `course`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('ACTIVE','DISABLED') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `video_link` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `exam_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `subject` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `score` int NOT NULL,
  `exam_date` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `duration` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `exam_history_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `exam_key_point`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_key_point` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `subject_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `order_num` int DEFAULT '0',
  `create_time` bigint DEFAULT NULL,
  `update_time` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `subject_id` (`subject_id`),
  CONSTRAINT `exam_key_point_ibfk_1` FOREIGN KEY (`subject_id`) REFERENCES `exam_subject` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `exam_qa_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_qa_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `subject` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `question_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `question` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `context` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `tokens_used` int DEFAULT NULL,
  `rating` int DEFAULT NULL,
  `feedback` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `session_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '会话ID',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `exam_subject`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_subject` (
  `id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` int DEFAULT '1',
  `create_time` bigint DEFAULT NULL,
  `update_time` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `exercise`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exercise` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '练习题ID',
  `book_id` bigint NOT NULL COMMENT '书籍ID',
  `knowledge_node_id` bigint DEFAULT NULL COMMENT '知识节点ID',
  `title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '题目标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '题目内容',
  `question_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '题型：SINGLE_CHOICE/MULTIPLE_CHOICE/FILL_BLANK/ESSAY',
  `options` json DEFAULT NULL COMMENT '选项（JSON格式）',
  `answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '正确答案',
  `analysis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '答案解析',
  `difficulty` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MEDIUM' COMMENT '难度：EASY/MEDIUM/HARD',
  `score` int NOT NULL DEFAULT '10' COMMENT '分值',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/DISABLED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_book_id` (`book_id`),
  KEY `idx_knowledge_node_id` (`knowledge_node_id`),
  KEY `idx_difficulty` (`difficulty`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='练习题表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `knowledge_node`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `knowledge_node` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '知识节点ID',
  `book_id` bigint NOT NULL COMMENT '书籍ID',
  `parent_id` bigint DEFAULT NULL COMMENT '父节点ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '知识点名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '知识点描述',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '知识点详细内容',
  `level` int NOT NULL DEFAULT '1' COMMENT '层级',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/DISABLED',
  `category` int NOT NULL DEFAULT '0' COMMENT '节点分类：0核心课程, 1章节模块, 2核心知识点, 3细分知识点',
  `value` int NOT NULL DEFAULT '50' COMMENT '节点值（用于知识密度）',
  `symbol_size` int NOT NULL DEFAULT '40' COMMENT '节点大小',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_book_id` (`book_id`),
  KEY `idx_parent_id` (`parent_id`),
  CONSTRAINT `knowledge_node_ibfk_1` FOREIGN KEY (`book_id`) REFERENCES `book` (`id`) ON DELETE CASCADE,
  CONSTRAINT `knowledge_node_ibfk_2` FOREIGN KEY (`parent_id`) REFERENCES `knowledge_node` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=388 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识节点表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `knowledge_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `knowledge_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关系ID',
  `source_node_id` bigint NOT NULL COMMENT '源节点ID',
  `target_node_id` bigint NOT NULL COMMENT '目标节点ID',
  `relation_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '关系类型：PREREQUISITE/RELEVANT/SUCCESSOR/PART_OF',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关系描述',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKpuhokpjpmrvdilxxstmmlx8wn` (`source_node_id`,`target_node_id`,`relation_type`),
  UNIQUE KEY `uk_source_target` (`source_node_id`,`target_node_id`,`relation_type`),
  KEY `idx_source_node_id` (`source_node_id`),
  KEY `idx_target_node_id` (`target_node_id`),
  CONSTRAINT `knowledge_relation_ibfk_1` FOREIGN KEY (`source_node_id`) REFERENCES `knowledge_node` (`id`) ON DELETE CASCADE,
  CONSTRAINT `knowledge_relation_ibfk_2` FOREIGN KEY (`target_node_id`) REFERENCES `knowledge_node` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=375 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识关系表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `learning_progress`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `learning_progress` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `date` date NOT NULL,
  `pages_read` int DEFAULT NULL,
  `progress` decimal(5,2) NOT NULL,
  `study_time_minutes` int DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `book_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKins606t72lg3v46np47e2jmu7` (`user_id`,`book_id`,`date`),
  KEY `FKqumi1ygqn0yom40nl2jh748lk` (`book_id`),
  CONSTRAINT `FKa2wwqgr3rwxftqrm30le2r7rr` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FKqumi1ygqn0yom40nl2jh748lk` FOREIGN KEY (`book_id`) REFERENCES `book` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会话ID',
  `message_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息ID（外部使用的唯一标识）',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色：user-用户，assistant-助手，system-系统',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息内容',
  `tokens_used` int DEFAULT '0' COMMENT '消耗的token数量',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'SUCCESS' COMMENT '状态：SENDING-发送中，SUCCESS-成功，FAILED-失败',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_message_id` (`message_id`),
  KEY `idx_role` (`role`)
) ENGINE=InnoDB AUTO_INCREMENT=137 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `note`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `note` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '所属用户',
  `book_id` bigint NOT NULL COMMENT '教材ID',
  `chapter_id` bigint DEFAULT NULL COMMENT '关联章节(可选)',
  `title` varchar(200) NOT NULL,
  `content` longtext,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_book` (`user_id`,`book_id`),
  KEY `idx_chapter` (`chapter_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学习笔记';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `qa_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qa_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '问答ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `book_id` bigint DEFAULT NULL COMMENT '书籍ID',
  `knowledge_node_id` bigint DEFAULT NULL COMMENT '知识节点ID',
  `question_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '问题类型：EXAM/HOMEWORK/PROFESSIONAL',
  `question` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户问题',
  `answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'AI回答',
  `context` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '上下文信息',
  `tokens_used` int DEFAULT '0' COMMENT '消耗的token数',
  `rating` int DEFAULT NULL COMMENT '评分(1-5)',
  `feedback` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '用户反馈',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `session_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '会话ID',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_book_id` (`book_id`),
  KEY `idx_knowledge_node_id` (`knowledge_node_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB AUTO_INCREMENT=237 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问答历史表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `senior`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `senior` (
  `id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `grade` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `major` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `subject` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tags` text COLLATE utf8mb4_unicode_ci,
  `answer_count` int DEFAULT '0',
  `rating` decimal(3,1) DEFAULT '0.0',
  `certified` tinyint(1) DEFAULT '0',
  `online` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `senior_answer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `senior_answer` (
  `id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `question_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `author_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `author_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `certified` tinyint(1) DEFAULT '0',
  `badge` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `time` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `likes` int DEFAULT '0',
  `liked_by_current_user` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `senior_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `source` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'seed',
  PRIMARY KEY (`id`),
  KEY `question_id` (`question_id`),
  KEY `idx_senior_answer_senior_id` (`senior_id`),
  CONSTRAINT `senior_answer_ibfk_1` FOREIGN KEY (`question_id`) REFERENCES `senior_question` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `senior_answer_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `senior_answer_like` (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `answer_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_answer_user` (`answer_id`,`user_id`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `fk_like_answer` FOREIGN KEY (`answer_id`) REFERENCES `senior_answer` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `senior_follow_up`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `senior_follow_up` (
  `id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `question_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `author_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `author` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `time` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `ai_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'none',
  PRIMARY KEY (`id`),
  KEY `question_id` (`question_id`),
  CONSTRAINT `senior_follow_up_ibfk_1` FOREIGN KEY (`question_id`) REFERENCES `senior_question` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `senior_question`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `senior_question` (
  `id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `category` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `author_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `author` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime NOT NULL,
  `views` int DEFAULT '0',
  `likes` int DEFAULT '0',
  `solved` tinyint(1) DEFAULT '0',
  `accepted_answer_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `images` text COLLATE utf8mb4_unicode_ci,
  `created_at_ms` bigint DEFAULT NULL,
  `answer_count` int DEFAULT '0',
  `hot_score` int DEFAULT '0',
  `created_at_timestamp` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `ai_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'none',
  `ai_senior_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `senior_question_mentor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `senior_question_mentor` (
  `id` int NOT NULL AUTO_INCREMENT,
  `question_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `senior_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `question_id` (`question_id`),
  KEY `senior_id` (`senior_id`),
  CONSTRAINT `senior_question_mentor_ibfk_1` FOREIGN KEY (`question_id`) REFERENCES `senior_question` (`id`) ON DELETE CASCADE,
  CONSTRAINT `senior_question_mentor_ibfk_2` FOREIGN KEY (`senior_id`) REFERENCES `senior` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `senior_question_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `senior_question_tag` (
  `id` int NOT NULL AUTO_INCREMENT,
  `question_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tag_id` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `question_id` (`question_id`),
  KEY `tag_id` (`tag_id`),
  CONSTRAINT `senior_question_tag_ibfk_1` FOREIGN KEY (`question_id`) REFERENCES `senior_question` (`id`) ON DELETE CASCADE,
  CONSTRAINT `senior_question_tag_ibfk_2` FOREIGN KEY (`tag_id`) REFERENCES `senior_tag` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `senior_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `senior_tag` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'STUDENT',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  UNIQUE KEY `uk_phone` (`phone`),
  KEY `idx_role` (`role`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `user_book`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_book` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `book_id` bigint NOT NULL COMMENT '书籍ID',
  `progress` decimal(5,2) NOT NULL DEFAULT '0.00' COMMENT '学习进度(0-100)',
  `last_read_page` int DEFAULT '0' COMMENT '最后阅读页码',
  `last_read_at` datetime DEFAULT NULL COMMENT '最后阅读时间',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/ARCHIVED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_book` (`user_id`,`book_id`),
  UNIQUE KEY `UKsqslawq5ttiq76c78ilcb461g` (`user_id`,`book_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_book_id` (`book_id`),
  CONSTRAINT `user_book_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `user_book_ibfk_2` FOREIGN KEY (`book_id`) REFERENCES `book` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户书籍表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `v_book_chapter_stats`;
/*!50001 DROP VIEW IF EXISTS `v_book_chapter_stats`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_book_chapter_stats` AS SELECT 
 1 AS `book_id`,
 1 AS `book_title`,
 1 AS `chapter_count`,
 1 AS `last_chapter_update`*/;
SET character_set_client = @saved_cs_client;
DROP TABLE IF EXISTS `v_book_info`;
/*!50001 DROP VIEW IF EXISTS `v_book_info`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_book_info` AS SELECT 
 1 AS `id`,
 1 AS `title`,
 1 AS `author`,
 1 AS `publisher`,
 1 AS `isbn`,
 1 AS `cover_image`,
 1 AS `description`,
 1 AS `subject`,
 1 AS `grade`,
 1 AS `version`,
 1 AS `status`,
 1 AS `created_at`,
 1 AS `updated_at`,
 1 AS `category`,
 1 AS `course_type`,
 1 AS `major`,
 1 AS `semester`,
 1 AS `university_level`,
 1 AS `price`,
 1 AS `publish_date`,
 1 AS `page_count`,
 1 AS `language`,
 1 AS `view_count`,
 1 AS `primary_image_url`*/;
SET character_set_client = @saved_cs_client;
DROP TABLE IF EXISTS `wrong_book`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wrong_book` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '错题ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `exercise_id` bigint NOT NULL COMMENT '练习题ID',
  `answer_record_id` bigint DEFAULT NULL COMMENT '答题记录ID',
  `wrong_count` int NOT NULL DEFAULT '1' COMMENT '错误次数',
  `correct_count` int NOT NULL DEFAULT '0' COMMENT '正确次数',
  `last_attempt_at` datetime DEFAULT NULL COMMENT '最后尝试时间',
  `master_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NOT_MASTERED' COMMENT '掌握状态：NOT_MASTERED/MASTERING/MASTERED',
  `notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '错题笔记',
  `tags` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标签（逗号分隔）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `error_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '错误类型：CONCEPT-概念错误, CALCULATION-计算错误, CARELESS-粗心大意, OTHER-其他',
  `subject` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '科目/分类',
  `difficulty` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '难度等级：EASY-简单, MEDIUM-中等, HARD-困难',
  `priority` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'MEDIUM' COMMENT '优先级：HIGH-高, MEDIUM-中, LOW-低',
  `next_review_at` datetime DEFAULT NULL COMMENT '下次复习时间',
  `review_count` int DEFAULT '0' COMMENT '复习次数',
  `user_answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '用户答案',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_exercise` (`user_id`,`exercise_id`),
  UNIQUE KEY `UK2idf0jsd8qfoidtjmkgqskfdl` (`user_id`,`exercise_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_exercise_id` (`exercise_id`),
  KEY `idx_master_status` (`master_status`),
  KEY `answer_record_id` (`answer_record_id`),
  KEY `idx_wrong_book_error_type` (`error_type`),
  KEY `idx_wrong_book_subject` (`subject`),
  KEY `idx_wrong_book_difficulty` (`difficulty`),
  KEY `idx_wrong_book_priority` (`priority`),
  KEY `idx_wrong_book_next_review_at` (`next_review_at`),
  CONSTRAINT `wrong_book_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `wrong_book_ibfk_2` FOREIGN KEY (`exercise_id`) REFERENCES `exercise` (`id`) ON DELETE CASCADE,
  CONSTRAINT `wrong_book_ibfk_3` FOREIGN KEY (`answer_record_id`) REFERENCES `answer_record` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='错题本表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50001 DROP VIEW IF EXISTS `v_book_chapter_stats`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_unicode_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013  SQL SECURITY DEFINER */
/*!50001 VIEW `v_book_chapter_stats` AS select `b`.`id` AS `book_id`,`b`.`title` AS `book_title`,count(`bc`.`id`) AS `chapter_count`,max(`bc`.`updated_at`) AS `last_chapter_update` from (`book` `b` left join `book_chapter` `bc` on(((`b`.`id` = `bc`.`book_id`) and (`bc`.`status` = 'ACTIVE')))) where (`b`.`status` <> 'DELETED') group by `b`.`id`,`b`.`title` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!50001 DROP VIEW IF EXISTS `v_book_info`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_unicode_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013  SQL SECURITY DEFINER */
/*!50001 VIEW `v_book_info` AS select `b`.`id` AS `id`,`b`.`title` AS `title`,`b`.`author` AS `author`,`b`.`publisher` AS `publisher`,`b`.`isbn` AS `isbn`,`b`.`cover_image` AS `cover_image`,`b`.`description` AS `description`,`b`.`subject` AS `subject`,`b`.`grade` AS `grade`,`b`.`version` AS `version`,`b`.`status` AS `status`,`b`.`created_at` AS `created_at`,`b`.`updated_at` AS `updated_at`,`b`.`category` AS `category`,`b`.`course_type` AS `course_type`,`b`.`major` AS `major`,`b`.`semester` AS `semester`,`b`.`university_level` AS `university_level`,`b`.`price` AS `price`,`b`.`publish_date` AS `publish_date`,`b`.`page_count` AS `page_count`,`b`.`language` AS `language`,`b`.`view_count` AS `view_count`,`bi`.`image_url` AS `primary_image_url` from (`book` `b` left join `book_image` `bi` on(((`b`.`id` = `bi`.`book_id`) and (`bi`.`is_primary` = 1) and (`bi`.`image_type` = 'COVER')))) where (`b`.`status` <> 'DELETED') */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;


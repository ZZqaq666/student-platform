-- 学长学姐答疑专区：从「假实现」升级为真实闭环
--
-- 变更内容：
--   1. senior_question 增加 ai_status / ai_senior_id（AI 回答生成状态与负责人设）
--   2. senior_answer   增加 senior_id / source（归属学长人设、回答来源 seed|ai）
--   3. senior_follow_up 增加 ai_status（追问同样异步生成学长回复）
--   4. 新增 senior_answer_like：点赞改为「按用户维度」，取代全局列 liked_by_current_user
--   5. 回填种子数据 senior_id；修复 s-3 人设姓名为空
--
-- 可重复执行：所有加列动作均通过存储过程校验 information_schema 后再执行。

USE student_platform;

DELIMITER $$

DROP PROCEDURE IF EXISTS senior_add_column_if_missing$$
CREATE PROCEDURE senior_add_column_if_missing(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_definition TEXT
)
BEGIN
    DECLARE v_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO v_exists
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table
      AND COLUMN_NAME = p_column;

    IF v_exists = 0 THEN
        SET @ddl = CONCAT('ALTER TABLE ', p_table, ' ADD COLUMN ', p_column, ' ', p_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

-- 问题：AI 回答生成状态 none|generating|done|failed，以及本次负责回答的学长人设
CALL senior_add_column_if_missing('senior_question', 'ai_status', "VARCHAR(20) NOT NULL DEFAULT 'none'");
CALL senior_add_column_if_missing('senior_question', 'ai_senior_id', "VARCHAR(50) DEFAULT NULL");

-- 回答：归属学长人设 + 来源（seed=种子数据，ai=AI 生成）
CALL senior_add_column_if_missing('senior_answer', 'senior_id', "VARCHAR(50) DEFAULT NULL");
CALL senior_add_column_if_missing('senior_answer', 'source', "VARCHAR(20) NOT NULL DEFAULT 'seed'");

-- 追问：同样支持异步生成学长回复
CALL senior_add_column_if_missing('senior_follow_up', 'ai_status', "VARCHAR(20) NOT NULL DEFAULT 'none'");

DROP PROCEDURE IF EXISTS senior_add_column_if_missing;

-- 点赞：按用户维度，取代 senior_answer.liked_by_current_user 全局标志
CREATE TABLE IF NOT EXISTS senior_answer_like (
    id          VARCHAR(50)  COLLATE utf8mb4_unicode_ci NOT NULL,
    answer_id   VARCHAR(50)  COLLATE utf8mb4_unicode_ci NOT NULL,
    user_id     BIGINT       NOT NULL,
    created_at  TIMESTAMP    NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_answer_user (answer_id, user_id),
    KEY idx_user_id (user_id),
    CONSTRAINT fk_like_answer FOREIGN KEY (answer_id) REFERENCES senior_answer (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_senior_answer_senior_id ON senior_answer (senior_id);

-- 修复人设数据：s-3（物理学）姓名为空
UPDATE senior SET name = '周学长' WHERE id = 's-3' AND (name IS NULL OR TRIM(name) = '');

-- 回填种子回答的归属学长（author_id 原即 senior.id）
UPDATE senior_answer a
    JOIN senior s ON a.author_id = s.id
SET a.senior_id = s.id
WHERE a.senior_id IS NULL;

-- 已有种子回答的问题视为已完成解答
UPDATE senior_question SET ai_status = 'done' WHERE ai_status = 'none';

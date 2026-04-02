-- 删除qa_history表的外键约束
-- 首先查看当前的外键约束
SHOW CREATE TABLE qa_history;

-- 删除外键约束
ALTER TABLE qa_history DROP FOREIGN KEY qa_history_ibfk_1;

-- 验证外键已删除
SHOW CREATE TABLE qa_history;
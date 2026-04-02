-- 创建书籍知识点表
CREATE TABLE IF NOT EXISTS book_knowledge_point (
    id BIGINT AUTO_INCREMENT COMMENT '知识点ID' PRIMARY KEY,
    book_id BIGINT NOT NULL COMMENT '书籍ID',
    type VARCHAR(20) NOT NULL COMMENT '知识点类型：RELATED/HIGH_FREQUENCY',
    content VARCHAR(255) NOT NULL COMMENT '知识点内容',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_book_id_type (book_id, type),
    INDEX idx_book_id (book_id)
) COMMENT '书籍知识点表';

-- 体育与健康
INSERT INTO book_knowledge_point (book_id, type, content) VALUES
(224, 'RELATED', '运动解剖学'),
(224, 'RELATED', '运动生理学'),
(224, 'RELATED', '营养学基础'),
(224, 'RELATED', '运动心理学'),
(224, 'HIGH_FREQUENCY', '体育锻炼对身体健康的影响'),
(224, 'HIGH_FREQUENCY', '常见运动损伤的预防与处理'),
(224, 'HIGH_FREQUENCY', '科学锻炼的原则与方法'),
(224, 'HIGH_FREQUENCY', '健康生活方式的养成');

-- 大学物理（上册）
INSERT INTO book_knowledge_point (book_id, type, content) VALUES
(209, 'RELATED', '高等数学基础'),
(209, 'RELATED', '线性代数'),
(209, 'RELATED', '微积分'),
(209, 'RELATED', '矢量分析'),
(209, 'HIGH_FREQUENCY', '牛顿运动定律'),
(209, 'HIGH_FREQUENCY', '动量守恒与能量守恒'),
(209, 'HIGH_FREQUENCY', '刚体转动'),
(209, 'HIGH_FREQUENCY', '振动与波');

-- 大学物理（下册）
INSERT INTO book_knowledge_point (book_id, type, content) VALUES
(210, 'RELATED', '高等数学基础'),
(210, 'RELATED', '电磁学基础'),
(210, 'RELATED', '波动光学'),
(210, 'RELATED', '量子力学基础'),
(210, 'HIGH_FREQUENCY', '静电场与电场强度'),
(210, 'HIGH_FREQUENCY', '磁场与电磁感应'),
(210, 'HIGH_FREQUENCY', '光的干涉与衍射'),
(210, 'HIGH_FREQUENCY', '狭义相对论基础');

-- 操作系统原理
INSERT INTO book_knowledge_point (book_id, type, content) VALUES
(213, 'RELATED', '计算机组成原理'),
(213, 'RELATED', '数据结构与算法'),
(213, 'RELATED', '计算机网络'),
(213, 'RELATED', '汇编语言'),
(213, 'HIGH_FREQUENCY', '进程管理与调度'),
(213, 'HIGH_FREQUENCY', '内存管理'),
(213, 'HIGH_FREQUENCY', '文件系统'),
(213, 'HIGH_FREQUENCY', '设备管理');

-- 数据库系统概论
INSERT INTO book_knowledge_point (book_id, type, content) VALUES
(214, 'RELATED', '数据结构与算法'),
(214, 'RELATED', '操作系统原理'),
(214, 'RELATED', '计算机网络'),
(214, 'RELATED', '离散数学'),
(214, 'HIGH_FREQUENCY', '关系数据库模型'),
(214, 'HIGH_FREQUENCY', 'SQL语言'),
(214, 'HIGH_FREQUENCY', '数据库设计'),
(214, 'HIGH_FREQUENCY', '事务与并发控制');

-- 数据结构与算法
INSERT INTO book_knowledge_point (book_id, type, content) VALUES
(211, 'RELATED', '高等数学基础'),
(211, 'RELATED', '离散数学'),
(211, 'RELATED', '计算机组成原理'),
(211, 'RELATED', '编程语言'),
(211, 'HIGH_FREQUENCY', '线性表'),
(211, 'HIGH_FREQUENCY', '树与二叉树'),
(211, 'HIGH_FREQUENCY', '图'),
(211, 'HIGH_FREQUENCY', '排序与查找算法');

-- 新视野大学英语1（第三版）
INSERT INTO book_knowledge_point (book_id, type, content) VALUES
(222, 'RELATED', '英语语法基础'),
(222, 'RELATED', '英语词汇'),
(222, 'RELATED', '英语听力'),
(222, 'RELATED', '英语写作'),
(222, 'HIGH_FREQUENCY', '日常英语会话'),
(222, 'HIGH_FREQUENCY', '英语阅读技巧'),
(222, 'HIGH_FREQUENCY', '英语语法要点'),
(222, 'HIGH_FREQUENCY', '英语词汇记忆方法');

-- 概率论与数理统计
INSERT INTO book_knowledge_point (book_id, type, content) VALUES
(208, 'RELATED', '高等数学基础'),
(208, 'RELATED', '线性代数'),
(208, 'RELATED', '微积分'),
(208, 'RELATED', '数学分析'),
(208, 'HIGH_FREQUENCY', '概率的基本概念'),
(208, 'HIGH_FREQUENCY', '随机变量及其分布'),
(208, 'HIGH_FREQUENCY', '数学期望与方差'),
(208, 'HIGH_FREQUENCY', '参数估计与假设检验');

-- 离散数学
INSERT INTO book_knowledge_point (book_id, type, content) VALUES
(216, 'RELATED', '高等数学基础'),
(216, 'RELATED', '线性代数'),
(216, 'RELATED', '集合论'),
(216, 'RELATED', '逻辑学基础'),
(216, 'HIGH_FREQUENCY', '命题逻辑与谓词逻辑'),
(216, 'HIGH_FREQUENCY', '集合与关系'),
(216, 'HIGH_FREQUENCY', '图论基础'),
(216, 'HIGH_FREQUENCY', '代数系统');

-- 线性代数
INSERT INTO book_knowledge_point (book_id, type, content) VALUES
(207, 'RELATED', '高等数学基础'),
(207, 'RELATED', '解析几何'),
(207, 'RELATED', '数学分析'),
(207, 'RELATED', '微积分'),
(207, 'HIGH_FREQUENCY', '矩阵及其运算'),
(207, 'HIGH_FREQUENCY', '行列式'),
(207, 'HIGH_FREQUENCY', '线性方程组'),
(207, 'HIGH_FREQUENCY', '特征值与特征向量');

-- 经济学原理
INSERT INTO book_knowledge_point (book_id, type, content) VALUES
(217, 'RELATED', '数学基础'),
(217, 'RELATED', '统计学'),
(217, 'RELATED', '社会学'),
(217, 'RELATED', '心理学'),
(217, 'HIGH_FREQUENCY', '供给与需求'),
(217, 'HIGH_FREQUENCY', '弹性理论'),
(217, 'HIGH_FREQUENCY', '消费者行为理论'),
(217, 'HIGH_FREQUENCY', '生产者行为理论');

-- 计算机网络
INSERT INTO book_knowledge_point (book_id, type, content) VALUES
(212, 'RELATED', '高等数学基础'),
(212, 'RELATED', '数学分析'),
(212, 'RELATED', '线性代数'),
(212, 'RELATED', '概率论与数理统计'),
(212, 'HIGH_FREQUENCY', '计算机网络是计算机科学与技术专业的核心课程'),
(212, 'HIGH_FREQUENCY', '知识密度100%，覆盖全部章节内容'),
(212, 'HIGH_FREQUENCY', 'TCP/IP协议栈'),
(212, 'HIGH_FREQUENCY', '网络安全基础');

-- 马克思主义基本原理
INSERT INTO book_knowledge_point (book_id, type, content) VALUES
(221, 'RELATED', '哲学基础'),
(221, 'RELATED', '政治经济学'),
(221, 'RELATED', '科学社会主义'),
(221, 'RELATED', '历史唯物主义'),
(221, 'HIGH_FREQUENCY', '辩证唯物主义'),
(221, 'HIGH_FREQUENCY', '剩余价值理论'),
(221, 'HIGH_FREQUENCY', '科学社会主义理论'),
(221, 'HIGH_FREQUENCY', '马克思主义的当代价值');

-- 高等数学（上册）
INSERT INTO book_knowledge_point (book_id, type, content) VALUES
(205, 'RELATED', '初等数学'),
(205, 'RELATED', '解析几何'),
(205, 'RELATED', '数学分析'),
(205, 'RELATED', '物理学基础'),
(205, 'HIGH_FREQUENCY', '函数与极限'),
(205, 'HIGH_FREQUENCY', '导数与微分'),
(205, 'HIGH_FREQUENCY', '中值定理与导数应用'),
(205, 'HIGH_FREQUENCY', '不定积分与定积分');

-- 高等数学（下册）
INSERT INTO book_knowledge_point (book_id, type, content) VALUES
(206, 'RELATED', '高等数学（上册）'),
(206, 'RELATED', '解析几何'),
(206, 'RELATED', '线性代数'),
(206, 'RELATED', '物理学基础'),
(206, 'HIGH_FREQUENCY', '多元函数微分法及其应用'),
(206, 'HIGH_FREQUENCY', '重积分'),
(206, 'HIGH_FREQUENCY', '曲线积分与曲面积分'),
(206, 'HIGH_FREQUENCY', '无穷级数');

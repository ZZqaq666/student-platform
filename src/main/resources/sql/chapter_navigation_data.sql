-- 章节导航数据初始化脚本
-- 创建时间: 2026-03-31
-- 描述: 为书籍添加章节导航数据

-- 设置字符集
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 1. 确保book_chapter表存在
-- ============================================

-- 检查book_chapter表是否存在
CREATE TABLE IF NOT EXISTS book_chapter (
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
    INDEX idx_chapter_book_sort (book_id, sort_order),
    INDEX idx_chapter_book_status (book_id, status),
    CONSTRAINT fk_chapter_book FOREIGN KEY (book_id) REFERENCES book(id) ON DELETE CASCADE,
    CONSTRAINT fk_chapter_parent FOREIGN KEY (parent_id) REFERENCES book_chapter(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='书籍章节表';

-- ============================================
-- 2. 插入示例章节数据
-- ============================================

-- 为高等数学（上册）添加章节
INSERT INTO book_chapter (book_id, title, content, sort_order, parent_id, status) VALUES
(1, '第一章 函数与极限', '<h2>1.1 映射与函数</h2><p>函数是数学中最基本的概念之一，它描述了两个集合之间的对应关系。</p><h2>1.2 数列的极限</h2><p>极限是微积分的基础概念，数列的极限描述了数列在无限趋近于某个值时的行为。</p><h2>1.3 函数的极限</h2><p>函数的极限是数列极限的推广，它描述了函数在自变量趋近于某个值时的行为。</p><h2>1.4 无穷小与无穷大</h2><p>无穷小和无穷大是微积分中的重要概念，它们在极限运算中有着广泛的应用。</p>', 1, NULL, 'ACTIVE'),
(1, '第二章 导数与微分', '<h2>2.1 导数的概念</h2><p>导数描述了函数在某一点的变化率，它是微积分的核心概念之一。</p><h2>2.2 函数的求导法则</h2><p>掌握基本求导法则对于计算导数至关重要，包括四则运算、复合函数求导等。</p><h2>2.3 高阶导数</h2><p>高阶导数是导数的导数，它在描述函数的变化率的变化率时有着重要应用。</p><h2>2.4 微分</h2><p>微分是导数的另一种表现形式，它在近似计算中有着广泛的应用。</p>', 2, NULL, 'ACTIVE'),
(1, '第三章 微分中值定理与导数的应用', '<h2>3.1 微分中值定理</h2><p>微分中值定理是连接函数局部性质和整体性质的桥梁，包括罗尔定理、拉格朗日中值定理和柯西中值定理。</p><h2>3.2 洛必达法则</h2><p>洛必达法则是计算不定式极限的有效方法，它基于导数的概念。</p><h2>3.3 函数的单调性与曲线的凹凸性</h2><p>利用导数可以判断函数的单调性和曲线的凹凸性，这对于函数的图像分析非常重要。</p><h2>3.4 函数的极值与最大值最小值</h2><p>函数的极值是函数在局部的最大值或最小值，而最大值最小值是函数在整个定义域上的最大或最小值。</p>', 3, NULL, 'ACTIVE'),
(1, '第四章 不定积分', '<h2>4.1 不定积分的概念与性质</h2><p>不定积分是导数的逆运算，它描述了所有具有相同导数的函数。</p><h2>4.2 换元积分法</h2><p>换元积分法是计算不定积分的重要方法，它通过变量替换将复杂的积分转化为简单的积分。</p><h2>4.3 分部积分法</h2><p>分部积分法是计算不定积分的另一种重要方法，它基于乘积的导数法则。</p><h2>4.4 有理函数的积分</h2><p>有理函数的积分可以通过部分分式分解转化为简单的积分。</p>', 4, NULL, 'ACTIVE'),
(1, '第五章 定积分', '<h2>5.1 定积分的概念与性质</h2><p>定积分是通过分割、近似、求和、取极限的方法定义的，它可以用来计算曲边梯形的面积等。</p><h2>5.2 微积分基本定理</h2><p>微积分基本定理建立了微分和积分之间的联系，它是微积分学的核心定理。</p><h2>5.3 定积分的换元法和分部积分法</h2><p>定积分的换元法和分部积分法是计算定积分的重要方法。</p><h2>5.4 反常积分</h2><p>反常积分是定积分的推广，它处理积分区间为无穷或被积函数在积分区间内有瑕点的情况。</p>', 5, NULL, 'ACTIVE');

-- 为大学物理（上册）添加章节
INSERT INTO book_chapter (book_id, title, content, sort_order, parent_id, status) VALUES
(2, '第一章 质点运动学', '<h2>1.1 参考系与坐标系</h2><p>参考系是描述物体运动的基准，坐标系是参考系的数学表示。</p><h2>1.2 质点的位移、速度和加速度</h2><p>位移是质点位置的变化，速度是位移的变化率，加速度是速度的变化率。</p><h2>1.3 直线运动</h2><p>直线运动是质点运动的最简单形式，包括匀速直线运动和匀变速直线运动。</p><h2>1.4 曲线运动</h2><p>曲线运动是质点在平面或空间中的运动，包括抛体运动和圆周运动。</p>', 1, NULL, 'ACTIVE'),
(2, '第二章 牛顿运动定律', '<h2>2.1 牛顿第一定律</h2><p>牛顿第一定律描述了物体在没有外力作用时的运动状态，即保持静止或匀速直线运动。</p><h2>2.2 牛顿第二定律</h2><p>牛顿第二定律描述了物体在外力作用下的加速度与力的关系，即F=ma。</p><h2>2.3 牛顿第三定律</h2><p>牛顿第三定律描述了作用力和反作用力的关系，即作用力和反作用力大小相等、方向相反、作用在不同物体上。</p><h2>2.4 牛顿定律的应用</h2><p>牛顿定律在解决力学问题中有着广泛的应用，包括质点的平衡和运动问题。</p>', 2, NULL, 'ACTIVE'),
(2, '第三章 动量守恒定律', '<h2>3.1 动量与冲量</h2><p>动量是物体质量与速度的乘积，冲量是力在时间上的累积。</p><h2>3.2 动量守恒定律</h2><p>动量守恒定律描述了系统在没有外力作用时总动量保持不变的规律。</p><h2>3.3 碰撞</h2><p>碰撞是物体之间的相互作用，包括弹性碰撞和非弹性碰撞。</p><h2>3.4 质心运动定理</h2><p>质心运动定理描述了系统质心的运动与系统所受合外力的关系。</p>', 3, NULL, 'ACTIVE');

-- 为大学英语（上册）添加章节
INSERT INTO book_chapter (book_id, title, content, sort_order, parent_id, status) VALUES
(3, 'Unit 1 College Life', '<h2>1.1 Listening & Speaking</h2><p>本单元听力和口语部分主要围绕大学生活的话题展开，包括校园生活、学习规划等。</p><h2>1.2 Reading Comprehension</h2><p>阅读理解部分包括两篇文章，分别讲述了大学生活的挑战和机遇。</p><h2>1.3 Vocabulary & Structure</h2><p>词汇和语法部分重点讲解与大学生活相关的词汇和常用语法结构。</p><h2>1.4 Writing</h2><p>写作部分要求学生写一篇关于大学生活规划的短文。</p>', 1, NULL, 'ACTIVE'),
(3, 'Unit 2 Learning Strategies', '<h2>2.1 Listening & Speaking</h2><p>本单元听力和口语部分主要讨论学习策略，包括时间管理、学习方法等。</p><h2>2.2 Reading Comprehension</h2><p>阅读理解部分包括两篇文章，分别讲述了有效的学习策略和学习方法。</p><h2>2.3 Vocabulary & Structure</h2><p>词汇和语法部分重点讲解与学习策略相关的词汇和常用语法结构。</p><h2>2.4 Writing</h2><p>写作部分要求学生写一篇关于自己学习策略的短文。</p>', 2, NULL, 'ACTIVE'),
(3, 'Unit 3 Career Planning', '<h2>3.1 Listening & Speaking</h2><p>本单元听力和口语部分主要围绕职业规划的话题展开，包括职业选择、职业准备等。</p><h2>3.2 Reading Comprehension</h2><p>阅读理解部分包括两篇文章，分别讲述了职业规划的重要性和方法。</p><h2>3.3 Vocabulary & Structure</h2><p>词汇和语法部分重点讲解与职业规划相关的词汇和常用语法结构。</p><h2>3.4 Writing</h2><p>写作部分要求学生写一篇关于自己职业规划的短文。</p>', 3, NULL, 'ACTIVE');

-- 为计算机基础添加章节
INSERT INTO book_chapter (book_id, title, content, sort_order, parent_id, status) VALUES
(4, '第一章 计算机基础知识', '<h2>1.1 计算机的发展与分类</h2><p>本章节介绍了计算机的发展历史、分类和应用领域。</p><h2>1.2 计算机系统组成</h2><p>计算机系统由硬件和软件两部分组成，本章节详细介绍了计算机的硬件组成和软件系统。</p><h2>1.3 计算机中信息的表示</h2><p>计算机中的信息以二进制形式表示，本章节介绍了二进制、十进制、十六进制等数制及其转换。</p><h2>1.4 计算机网络基础</h2><p>计算机网络是计算机技术的重要组成部分，本章节介绍了计算机网络的基本概念和分类。</p>', 1, NULL, 'ACTIVE'),
(4, '第二章 操作系统基础', '<h2>2.1 操作系统概述</h2><p>操作系统是计算机系统的核心软件，本章节介绍了操作系统的概念、功能和分类。</p><h2>2.2 Windows操作系统</h2><p>Windows是目前最流行的操作系统之一，本章节介绍了Windows操作系统的基本操作和使用技巧。</p><h2>2.3 Linux操作系统</h2><p>Linux是一种开源的操作系统，本章节介绍了Linux操作系统的基本概念和使用方法。</p><h2>2.4 操作系统安全</h2><p>操作系统安全是计算机安全的重要组成部分，本章节介绍了操作系统安全的基本概念和防护措施。</p>', 2, NULL, 'ACTIVE'),
(4, '第三章 办公软件应用', '<h2>3.1 Word文字处理</h2><p>Word是常用的文字处理软件，本章节介绍了Word的基本操作和高级功能。</p><h2>3.2 Excel电子表格</h2><p>Excel是常用的电子表格软件，本章节介绍了Excel的基本操作和数据处理功能。</p><h2>3.3 PowerPoint演示文稿</h2><p>PowerPoint是常用的演示文稿软件，本章节介绍了PowerPoint的基本操作和设计技巧。</p><h2>3.4 办公软件综合应用</h2><p>本章节介绍了办公软件的综合应用，包括文档协作、数据共享等。</p>', 3, NULL, 'ACTIVE');

-- ============================================
-- 3. 验证数据插入结果
-- ============================================

-- 验证章节数据
SELECT '章节数据验证' as check_item, COUNT(*) as chapter_count 
FROM book_chapter 
WHERE status = 'ACTIVE';

-- 按书籍统计章节数量
SELECT book_id, COUNT(*) as chapter_count 
FROM book_chapter 
WHERE status = 'ACTIVE' 
GROUP BY book_id;

SET FOREIGN_KEY_CHECKS = 1;

-- 脚本执行完成
SELECT '章节导航数据初始化脚本执行完成' as message, NOW() as executed_at;
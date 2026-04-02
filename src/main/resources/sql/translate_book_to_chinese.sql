-- 将book数据库中的英文内容翻译为中文

-- 1. 更新书名、作者、出版社为中文
UPDATE book SET title = '高等数学（上册）', author = '同济大学', publisher = '高等教育出版社' WHERE title = 'Advanced Mathematics (Volume 1)';
UPDATE book SET title = '高等数学（下册）', author = '同济大学', publisher = '高等教育出版社' WHERE title = 'Advanced Mathematics (Volume 2)';
UPDATE book SET title = '大学物理（上册）', author = '清华大学出版社', publisher = '清华大学出版社' WHERE title = 'University Physics (Volume 1)';
UPDATE book SET title = '数据结构与算法', author = '严蔚敏', publisher = '清华大学出版社' WHERE title = 'Data Structures and Algorithms';
UPDATE book SET title = '计算机网络', author = '谢希仁', publisher = '电子工业出版社' WHERE title = 'Computer Networks';
UPDATE book SET title = '数据库系统概论', author = '王珊', publisher = '高等教育出版社' WHERE title = 'Database System Concepts';
UPDATE book SET title = '操作系统原理', author = '汤小丹', publisher = '西安电子科技大学出版社' WHERE title = 'Operating System Principles';
UPDATE book SET title = '算法导论', author = 'Thomas H. Cormen', publisher = '机械工业出版社' WHERE title = 'Introduction to Algorithms';
UPDATE book SET title = '离散数学', author = '耿素云', publisher = '清华大学出版社' WHERE title = 'Discrete Mathematics';
UPDATE book SET title = '线性代数', author = '同济大学', publisher = '高等教育出版社' WHERE title = 'Linear Algebra';
UPDATE book SET title = '概率论与数理统计', author = '浙江大学', publisher = '高等教育出版社' WHERE title = 'Probability and Mathematical Statistics';
UPDATE book SET title = '经济学原理', author = '高鸿业', publisher = '中国人民大学出版社' WHERE title = 'Principles of Economics';
UPDATE book SET title = '管理学原理', author = '周三多', publisher = '高等教育出版社' WHERE title = 'Principles of Management';
UPDATE book SET title = '市场营销学', author = '郭国庆', publisher = '高等教育出版社' WHERE title = 'Marketing';
UPDATE book SET title = '财务会计', author = '陈国辉', publisher = '东北财经大学出版社' WHERE title = 'Financial Accounting';
UPDATE book SET title = '高等数学复习指南', author = '李永乐', publisher = '高等教育出版社' WHERE title = 'Advanced Mathematics Review Guide';
UPDATE book SET title = '大学物理习题集', author = '程守洙', publisher = '高等教育出版社' WHERE title = 'University Physics Exercise Set';
UPDATE book SET title = '计算机网络实验指导', author = '吴功宜', publisher = '清华大学出版社' WHERE title = 'Computer Network Experiment Guide';
UPDATE book SET title = '数据结构学习指导', author = '许卓群', publisher = '高等教育出版社' WHERE title = 'Data Structure Study Guide';
UPDATE book SET title = '马克思主义基本原理', author = '本书编写组', publisher = '高等教育出版社' WHERE title = 'Basic Principles of Marxism';
UPDATE book SET title = '大学英语', author = '李观仪', publisher = '上海外语教育出版社' WHERE title = 'College English';
UPDATE book SET title = '大学语文', author = '王步高', publisher = '高等教育出版社' WHERE title = 'College Chinese';
UPDATE book SET title = '体育与健康', author = '体育教研组', publisher = '高等教育出版社' WHERE title = 'Physical Education and Health';
UPDATE book SET title = '大学生职业规划', author = '张海翔', publisher = '高等教育出版社' WHERE title = 'College Student Career Planning';

-- 2. 更新专业领域为中文
UPDATE book SET major = '数学' WHERE major = 'Mathematics';
UPDATE book SET major = '物理' WHERE major = 'Physics';
UPDATE book SET major = '计算机科学与技术' WHERE major = 'Computer Science';
UPDATE book SET major = '经济学' WHERE major = 'Economics';
UPDATE book SET major = '管理学' WHERE major = 'Management';
UPDATE book SET major = '通识教育' WHERE major = 'General Education';
UPDATE book SET major = '英语' WHERE major = 'English';
UPDATE book SET major = '语文' WHERE major = 'Chinese';
UPDATE book SET major = '体育' WHERE major = 'PE';

-- 3. 更新学期为中文
UPDATE book SET semester = '大一' WHERE semester = 'Year 1';
UPDATE book SET semester = '大二' WHERE semester = 'Year 2';
UPDATE book SET semester = '大三' WHERE semester = 'Year 3';
UPDATE book SET semester = '大四' WHERE semester = 'Year 4';

-- 4. 更新大学层次为中文
UPDATE book SET university_level = '本科' WHERE university_level = 'UNDERGRADUATE';
UPDATE book SET university_level = '研究生' WHERE university_level = 'POSTGRADUATE';

-- 5. 更新教材分类为中文
UPDATE book SET category = '主教材' WHERE category = 'TEXTBOOK';
UPDATE book SET category = '参考书' WHERE category = 'REFERENCE';
UPDATE book SET category = '辅助资料' WHERE category = 'SUPPLEMENTARY';

-- 6. 更新课程类型为中文
UPDATE book SET course_type = '专业核心课' WHERE course_type = 'CORE';
UPDATE book SET course_type = '专业基础课' WHERE course_type = 'FOUNDATION';
UPDATE book SET course_type = '选修课' WHERE course_type = 'ELECTIVE';
UPDATE book SET course_type = '通识课' WHERE course_type = 'GENERAL';

-- 7. 更新学科分类为中文
UPDATE book SET subject = '数学' WHERE subject = 'MATH';
UPDATE book SET subject = '物理' WHERE subject = 'PHYSICS';
UPDATE book SET subject = '计算机科学' WHERE subject = 'COMPUTER_SCIENCE';
UPDATE book SET subject = '经济学' WHERE subject = 'ECONOMICS';
UPDATE book SET subject = '管理学' WHERE subject = 'MANAGEMENT';
UPDATE book SET subject = '通识教育' WHERE subject = 'GENERAL';
UPDATE book SET subject = '英语' WHERE subject = 'ENGLISH';
UPDATE book SET subject = '语文' WHERE subject = 'CHINESE';

-- 8. 更新版本为中文
UPDATE book SET version = '2024版' WHERE version = '2024';

-- 9. 验证翻译结果
SELECT id, title, author, publisher, subject, major, semester, course_type, category, university_level FROM book WHERE title LIKE '%高等数学%' OR title LIKE '%数据结构%' LIMIT 10;

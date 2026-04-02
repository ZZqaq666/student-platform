-- University Textbook Classification System Update Script

-- 1. Add university textbook related fields
ALTER TABLE book 
ADD COLUMN category VARCHAR(50) COMMENT 'TEXTBOOK, REFERENCE, SUPPLEMENTARY',
ADD COLUMN course_type VARCHAR(50) COMMENT 'CORE, FOUNDATION, ELECTIVE, GENERAL',
ADD COLUMN major VARCHAR(100) COMMENT 'Major field: Computer Science, Economics, Management, etc.',
ADD COLUMN semester VARCHAR(20) COMMENT 'Semester: Year 1, Year 2, Year 3, Year 4',
ADD COLUMN university_level VARCHAR(20) COMMENT 'UNDERGRADUATE, POSTGRADUATE';

-- 2. Update existing data to university textbooks
UPDATE book SET 
    category = 'TEXTBOOK',
    course_type = 'CORE',
    university_level = 'UNDERGRADUATE',
    major = 'Computer Science',
    semester = 'Year 1'
WHERE grade LIKE 'GRADE_%';

-- 3. Insert sample university textbook data
INSERT INTO book (title, author, publisher, isbn, cover_image, description, subject, grade, version, status, category, course_type, major, university_level, semester, created_at, updated_at) VALUES
('Advanced Mathematics (Volume 1)', 'Tongji University', 'Higher Education Press', '978-7-04-01234-5', NULL, 'Advanced Mathematics is an important foundation course for science and engineering majors, covering calculus, linear algebra, etc.', 'MATH', 'UNDERGRADUATE_1', '2024', 'ACTIVE', 'TEXTBOOK', 'CORE', 'Mathematics', 'UNDERGRADUATE', 'Year 1', NOW(), NOW()),
('Advanced Mathematics (Volume 2)', 'Tongji University', 'Higher Education Press', '978-7-04-01235-2', NULL, 'Advanced Mathematics is an important foundation course for science and engineering majors, covering multivariate calculus, infinite series, etc.', 'MATH', 'UNDERGRADUATE_2', '2024', 'ACTIVE', 'TEXTBOOK', 'CORE', 'Mathematics', 'UNDERGRADUATE', 'Year 2', NOW(), NOW()),
('University Physics (Volume 1)', 'Tsinghua University Press', 'Tsinghua University Press', '978-7-30-20567-8', NULL, 'University Physics covers mechanics, thermodynamics, electromagnetics, and other classic physics content', 'PHYSICS', 'UNDERGRADUATE_1', '2024', 'ACTIVE', 'TEXTBOOK', 'CORE', 'Physics', 'UNDERGRADUATE', 'Year 1', NOW(), NOW()),
('Data Structures and Algorithms', 'Yan Weimin', 'Tsinghua University Press', '978-7-11-04923-6', NULL, 'Computer science core course, covering data structures, algorithm design and analysis', 'COMPUTER_SCIENCE', 'UNDERGRADUATE_2', '2024', 'ACTIVE', 'TEXTBOOK', 'CORE', 'Computer Science', 'UNDERGRADUATE', 'Year 2', NOW(), NOW()),
('Computer Networks', 'Xie Xiren', 'Electronic Industry Press', '978-7-11-05123-4', NULL, 'Computer network principles, protocols and security', 'COMPUTER_SCIENCE', 'UNDERGRADUATE_3', '2024', 'ACTIVE', 'TEXTBOOK', 'CORE', 'Computer Science', 'UNDERGRADUATE', 'Year 3', NOW(), NOW()),
('Database System Concepts', 'Wang Shan', 'Higher Education Press', '978-7-30-25763-5', NULL, 'Database principles, design and implementation', 'COMPUTER_SCIENCE', 'UNDERGRADUATE_3', '2024', 'ACTIVE', 'TEXTBOOK', 'CORE', 'Computer Science', 'UNDERGRADUATE', 'Year 3', NOW(), NOW()),
('Operating System Principles', 'Tang Xiaodan', 'Xidian University Press', '978-7-11-04812-1', NULL, 'Operating system core concepts and implementation', 'COMPUTER_SCIENCE', 'UNDERGRADUATE_4', '2024', 'ACTIVE', 'TEXTBOOK', 'CORE', 'Computer Science', 'UNDERGRADUATE', 'Year 4', NOW(), NOW()),
('Introduction to Algorithms', 'Thomas H. Cormen', 'China Machine Press', '978-7-11-02733-4', NULL, 'Algorithm design and analysis foundation', 'COMPUTER_SCIENCE', 'UNDERGRADUATE_1', '2024', 'ACTIVE', 'REFERENCE', 'CORE', 'Computer Science', 'UNDERGRADUATE', 'Year 1', NOW(), NOW()),
('Discrete Mathematics', 'Geng Suyun', 'Tsinghua University Press', '978-7-11-02532-7', NULL, 'Discrete mathematics is an important mathematical foundation for computer science', 'COMPUTER_SCIENCE', 'UNDERGRADUATE_1', '2024', 'ACTIVE', 'REFERENCE', 'FOUNDATION', 'Computer Science', 'UNDERGRADUATE', 'Year 1', NOW(), NOW()),
('Linear Algebra', 'Tongji University', 'Higher Education Press', '978-7-04-01256-1', NULL, 'Linear algebra theory and applications', 'COMPUTER_SCIENCE', 'UNDERGRADUATE_1', '2024', 'ACTIVE', 'REFERENCE', 'FOUNDATION', 'Computer Science', 'UNDERGRADUATE', 'Year 1', NOW(), NOW()),
('Probability and Mathematical Statistics', 'Zhejiang University', 'Higher Education Press', '978-7-30-81234-5', NULL, 'Probability statistics foundation and applications', 'COMPUTER_SCIENCE', 'UNDERGRADUATE_2', '2024', 'ACTIVE', 'REFERENCE', 'FOUNDATION', 'Computer Science', 'UNDERGRADUATE', 'Year 2', NOW(), NOW()),
('Principles of Economics', 'Gao Hongye', 'Renmin University Press', '978-7-04-01234-1', NULL, 'Microeconomics and macroeconomics foundation', 'ECONOMICS', 'UNDERGRADUATE_1', '2024', 'ACTIVE', 'TEXTBOOK', 'CORE', 'Economics', 'UNDERGRADUATE', 'Year 1', NOW(), NOW()),
('Principles of Management', 'Zhou Sanduo', 'Higher Education Press', '978-7-04-01234-2', NULL, 'Management basic theories and methods', 'MANAGEMENT', 'UNDERGRADUATE_1', '2024', 'ACTIVE', 'TEXTBOOK', 'CORE', 'Management', 'UNDERGRADUATE', 'Year 1', NOW(), NOW()),
('Marketing', 'Guo Guoqing', 'Higher Education Press', '978-7-04-01234-3', NULL, 'Marketing theory and practice', 'MANAGEMENT', 'UNDERGRADUATE_2', '2024', 'ACTIVE', 'TEXTBOOK', 'CORE', 'Management', 'UNDERGRADUATE', 'Year 2', NOW(), NOW()),
('Financial Accounting', 'Chen Guohui', 'Dongbei University Press', '978-7-04-01234-4', NULL, 'Financial accounting foundation', 'MANAGEMENT', 'UNDERGRADUATE_2', '2024', 'ACTIVE', 'TEXTBOOK', 'CORE', 'Management', 'UNDERGRADUATE', 'Year 2', NOW(), NOW()),
('Advanced Mathematics Review Guide', 'Li Yongle', 'Higher Education Press', '978-7-04-01234-5', NULL, 'Advanced mathematics key points and difficulties analysis', 'MATH', 'UNDERGRADUATE_1', '2024', 'ACTIVE', 'SUPPLEMENTARY', 'CORE', 'Mathematics', 'UNDERGRADUATE', 'Year 1', NOW(), NOW()),
('University Physics Exercise Set', 'Cheng Shouzhu', 'Higher Education Press', '978-7-04-01234-6', NULL, 'University physics exercises and solutions', 'PHYSICS', 'UNDERGRADUATE_1', '2024', 'ACTIVE', 'SUPPLEMENTARY', 'CORE', 'Physics', 'UNDERGRADUATE', 'Year 1', NOW(), NOW()),
('Computer Network Experiment Guide', 'Wu Gongyi', 'Tsinghua University Press', '978-7-04-01234-7', NULL, 'Computer network experiment tutorial', 'COMPUTER_SCIENCE', 'UNDERGRADUATE_2', '2024', 'ACTIVE', 'SUPPLEMENTARY', 'CORE', 'Computer Science', 'UNDERGRADUATE', 'Year 2', NOW(), NOW()),
('Data Structure Study Guide', 'Xu Zhuoqun', 'Higher Education Press', '978-7-04-01234-8', NULL, 'Data structure learning points and exercises', 'COMPUTER_SCIENCE', 'UNDERGRADUATE_2', '2024', 'ACTIVE', 'SUPPLEMENTARY', 'CORE', 'Computer Science', 'UNDERGRADUATE', 'Year 2', NOW(), NOW()),
('Basic Principles of Marxism', 'Editorial Group', 'Higher Education Press', '978-7-04-01234-9', NULL, 'Introduction to basic principles of Marxism', 'GENERAL', 'UNDERGRADUATE_1', '2024', 'ACTIVE', 'TEXTBOOK', 'GENERAL', 'General Education', 'UNDERGRADUATE', 'Year 1', NOW(), NOW()),
('College English', 'Li Guanyi', 'Shanghai Foreign Language Press', '978-7-04-01234-10', NULL, 'College English reading, writing and translation', 'ENGLISH', 'UNDERGRADUATE_1', '2024', 'ACTIVE', 'TEXTBOOK', 'GENERAL', 'English', 'UNDERGRADUATE', 'Year 1', NOW(), NOW()),
('College Chinese', 'Wang Bugao', 'Higher Education Press', '978-7-04-01234-11', NULL, 'College Chinese and writing', 'CHINESE', 'UNDERGRADUATE_1', '2024', 'ACTIVE', 'TEXTBOOK', 'GENERAL', 'Chinese', 'UNDERGRADUATE', 'Year 1', NOW(), NOW()),
('Physical Education and Health', 'PE Teaching Group', 'Higher Education Press', '978-7-04-01234-12', NULL, 'College student physical education theory and practice', 'GENERAL', 'UNDERGRADUATE_1', '2024', 'ACTIVE', 'TEXTBOOK', 'GENERAL', 'PE', 'UNDERGRADUATE', 'Year 1', NOW(), NOW()),
('College Student Career Planning', 'Zhang Haixiang', 'Higher Education Press', '978-7-04-01234-13', NULL, 'College student career development and planning', 'GENERAL', 'UNDERGRADUATE_1', '2024', 'ACTIVE', 'TEXTBOOK', 'GENERAL', 'General Education', 'UNDERGRADUATE', 'Year 1', NOW(), NOW());

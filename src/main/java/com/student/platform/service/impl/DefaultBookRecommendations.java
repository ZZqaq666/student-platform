package com.student.platform.service.impl;

import com.student.platform.dto.BookRecommendationDTO;
import com.student.platform.dto.RecommendedBookDTO;
import com.student.platform.dto.UserReviewDTO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 默认书籍推荐数据
 * 根据书籍名称返回相应的推荐书籍和用户评价
 */
public class DefaultBookRecommendations {

    // 存储默认推荐数据的映射
    private static final Map<String, BookRecommendationDTO> DEFAULT_RECOMMENDATIONS = new HashMap<>();

    static {
        // 初始化默认推荐数据
        initializeDefaultRecommendations();
    }

    /**
     * 初始化默认推荐数据
     */
    private static void initializeDefaultRecommendations() {
        // 1. 体育与健康
        addSportsHealthRecommendations();

        // 2. 大学物理（上册）和（下册）
        addUniversityPhysicsRecommendations();

        // 3. 操作系统原理
        addOperatingSystemRecommendations();

        // 4. 数据库系统概论
        addDatabaseRecommendations();

        // 5. 数据结构与算法
        addDataStructureRecommendations();

        // 6. 新视野大学英语1（第三版）
        addEnglishRecommendations();

        // 7. 概率论与数理统计
        addProbabilityRecommendations();

        // 8. 离散数学
        addDiscreteMathRecommendations();

        // 9. 线性代数
        addLinearAlgebraRecommendations();

        // 10. 经济学原理
        addEconomicsRecommendations();

        // 11. 计算机网络
        addComputerNetworkRecommendations();

        // 12. 马克思主义基本原理
        addMarxismRecommendations();

        // 13. 高等数学（上册）和（下册）
        addAdvancedMathRecommendations();

        // 保留原始Java相关书籍推荐
        addJavaRecommendations();
    }

    // 1. 体育与健康推荐数据
    private static void addSportsHealthRecommendations() {
        BookRecommendationDTO sportsRecommendations = new BookRecommendationDTO();
        List<RecommendedBookDTO> sportsBooks = new ArrayList<>();
        List<UserReviewDTO> sportsReviews = new ArrayList<>();

        // 推荐书籍1
        RecommendedBookDTO book1 = new RecommendedBookDTO();
        book1.setId(301L);
        book1.setTitle("大学体育与健康教程");
        book1.setAuthor("高等教育出版社编写组");
        book1.setCoverImage("https://img1.doubanio.com/view/subject/l/public/s1081234.jpg");
        book1.setDescription("普通高等学校公共体育新形态教材，分为体育与健康知识和实用运动技能两篇共17章内容");
        book1.setMajor("全专业");
        book1.setSemester("大一");
        book1.setCourseType("公共必修课");
        book1.setSubject("体育");
        book1.setRecommendationReason("与体育与健康课程高度相关，内容全面，适合大学生体育学习");
        book1.setRelevanceScore(0.96);
        sportsBooks.add(book1);

        // 推荐书籍2
        RecommendedBookDTO book2 = new RecommendedBookDTO();
        book2.setId(302L);
        book2.setTitle("锻炼");
        book2.setAuthor("[美]丹尼尔·利伯曼");
        book2.setCoverImage("https://img9.doubanio.com/view/subject/l/public/s33776154.jpg");
        book2.setDescription("从进化角度解释人类为什么需要锻炼，科学解读运动与健康的关系");
        book2.setMajor("全专业");
        book2.setSemester("全学期");
        book2.setCourseType("公共选修课");
        book2.setSubject("体育健康");
        book2.setRecommendationReason("从科学角度解释运动的重要性，帮助理解体育与健康的深层联系");
        book2.setRelevanceScore(0.90);
        sportsBooks.add(book2);

        // 推荐书籍3
        RecommendedBookDTO book3 = new RecommendedBookDTO();
        book3.setId(303L);
        book3.setTitle("运动改造大脑");
        book3.setAuthor("[美]约翰·瑞迪");
        book3.setCoverImage("https://img1.doubanio.com/view/subject/l/public/s29771733.jpg");
        book3.setDescription("介绍运动如何对大脑产生积极影响，提升学习能力和记忆力");
        book3.setMajor("全专业");
        book3.setSemester("全学期");
        book3.setCourseType("公共选修课");
        book3.setSubject("体育心理学");
        book3.setRecommendationReason("结合神经科学，从全新角度理解体育锻炼的价值");
        book3.setRelevanceScore(0.87);
        sportsBooks.add(book3);

        // 用户评价1
        UserReviewDTO review1 = new UserReviewDTO();
        review1.setId(401L);
        review1.setBookId(224L);
        review1.setUserId(1002L);
        review1.setUsername("运动爱好者");
        review1.setRating(4);
        review1.setContent("教材内容全面，涵盖了各种运动项目和健康知识，适合作为体育课辅助教材");
        review1.setCreatedAt(LocalDateTime.now().minusDays(5));
        review1.setLikes(32);
        sportsReviews.add(review1);

        // 用户评价2
        UserReviewDTO review2 = new UserReviewDTO();
        review2.setId(402L);
        review2.setBookId(224L);
        review2.setUserId(1003L);
        review2.setUsername("健康生活家");
        review2.setRating(5);
        review2.setContent("《锻炼》这本书改变了我对运动的看法，科学的解释让我更有动力坚持锻炼");
        review2.setCreatedAt(LocalDateTime.now().minusDays(2));
        review2.setLikes(56);
        sportsReviews.add(review2);

        sportsRecommendations.setRecommendedBooks(sportsBooks);
        sportsRecommendations.setUserReviews(sportsReviews);
        DEFAULT_RECOMMENDATIONS.put("体育与健康", sportsRecommendations);
    }

    // 2. 大学物理推荐数据
    private static void addUniversityPhysicsRecommendations() {
        BookRecommendationDTO physicsRecommendations = new BookRecommendationDTO();
        List<RecommendedBookDTO> physicsBooks = new ArrayList<>();
        List<UserReviewDTO> physicsReviews = new ArrayList<>();

        // 推荐书籍1
        RecommendedBookDTO book1 = new RecommendedBookDTO();
        book1.setId(401L);
        book1.setTitle("物理学");
        book1.setAuthor("马文蔚");
        book1.setCoverImage("https://img3.doubanio.com/view/subject/l/public/s1081235.jpg");
        book1.setDescription("第七版，普通高等教育本科国家级规划教材，被众多高校选用为大学物理课程教材");
        book1.setMajor("理工科");
        book1.setSemester("大一");
        book1.setCourseType("公共必修课");
        book1.setSubject("物理学");
        book1.setRecommendationReason("与大学物理课程内容高度匹配，讲解清晰，例题丰富");
        book1.setRelevanceScore(0.95);
        physicsBooks.add(book1);

        // 推荐书籍2
        RecommendedBookDTO book2 = new RecommendedBookDTO();
        book2.setId(402L);
        book2.setTitle("大学物理学");
        book2.setAuthor("张三慧");
        book2.setCoverImage("https://img9.doubanio.com/view/subject/l/public/s1081236.jpg");
        book2.setDescription("清华大学出版社经典教材，涵盖力学、热学、电磁学、光学和量子物理五篇内容");
        book2.setMajor("理工科");
        book2.setSemester("大一");
        book2.setCourseType("公共必修课");
        book2.setSubject("物理学");
        book2.setRecommendationReason("内容全面，适合作为大学物理的辅助教材，帮助深入理解物理概念");
        book2.setRelevanceScore(0.92);
        physicsBooks.add(book2);

        // 推荐书籍3
        RecommendedBookDTO book3 = new RecommendedBookDTO();
        book3.setId(403L);
        book3.setTitle("University Physics with Modern Physics");
        book3.setAuthor("H. D. Young, R. A. Freedman");
        book3.setCoverImage("https://img1.doubanio.com/view/subject/l/public/s1081237.jpg");
        book3.setDescription("国际经典物理教材，被全球众多顶尖大学采用，内容前沿，例题丰富");
        book3.setMajor("理工科");
        book3.setSemester("大一");
        book3.setCourseType("公共必修课");
        book3.setSubject("物理学");
        book3.setRecommendationReason("原版教材，适合想提高物理英语水平和接触国际教学内容的学生");
        book3.setRelevanceScore(0.89);
        physicsBooks.add(book3);

        // 用户评价1
        UserReviewDTO review1 = new UserReviewDTO();
        review1.setId(501L);
        review1.setBookId(209L);
        review1.setUserId(1004L);
        review1.setUsername("物理爱好者");
        review1.setRating(5);
        review1.setContent("马文蔚的《物理学》讲解非常清晰，公式推导详细，例题难度适中，是学习大学物理的必备教材");
        review1.setCreatedAt(LocalDateTime.now().minusDays(7));
        review1.setLikes(89);
        physicsReviews.add(review1);

        // 用户评价2
        UserReviewDTO review2 = new UserReviewDTO();
        review2.setId(502L);
        review2.setBookId(210L);
        review2.setUserId(1005L);
        review2.setUsername("工科学生");
        review2.setRating(4);
        review2.setContent("张三慧的教材内容全面，下册的电磁学和光学部分讲解得很好，适合课后复习");
        review2.setCreatedAt(LocalDateTime.now().minusDays(3));
        review2.setLikes(45);
        physicsReviews.add(review2);

        physicsRecommendations.setRecommendedBooks(physicsBooks);
        physicsRecommendations.setUserReviews(physicsReviews);
        DEFAULT_RECOMMENDATIONS.put("大学物理（上册）", physicsRecommendations);
        DEFAULT_RECOMMENDATIONS.put("大学物理（下册）", physicsRecommendations);
    }

    // 3. 操作系统原理推荐数据
    private static void addOperatingSystemRecommendations() {
        BookRecommendationDTO osRecommendations = new BookRecommendationDTO();
        List<RecommendedBookDTO> osBooks = new ArrayList<>();
        List<UserReviewDTO> osReviews = new ArrayList<>();

        // 推荐书籍1
        RecommendedBookDTO book1 = new RecommendedBookDTO();
        book1.setId(501L);
        book1.setTitle("操作系统概念");
        book1.setAuthor("Abraham Silberschatz");
        book1.setCoverImage("https://img3.doubanio.com/view/subject/l/public/s1081238.jpg");
        book1.setDescription("原书第10版，操作系统领域的经典书籍，被国内外众多高校选作教材");
        book1.setMajor("计算机科学与技术");
        book1.setSemester("大三");
        book1.setCourseType("专业必修课");
        book1.setSubject("操作系统");
        book1.setRecommendationReason("与操作系统原理课程内容高度一致，全面讲解操作系统核心概念");
        book1.setRelevanceScore(0.96);
        osBooks.add(book1);

        // 推荐书籍2
        RecommendedBookDTO book2 = new RecommendedBookDTO();
        book2.setId(502L);
        book2.setTitle("现代操作系统");
        book2.setAuthor("Andrew S. Tanenbaum");
        book2.setCoverImage("https://img9.doubanio.com/view/subject/l/public/s1081239.jpg");
        book2.setDescription("minix之父Tanenbaum的经典著作，第五版更新了多核系统和移动计算内容");
        book2.setMajor("计算机科学与技术");
        book2.setSemester("大三");
        book2.setCourseType("专业必修课");
        book2.setSubject("操作系统");
        book2.setRecommendationReason("讲解深入浅出，结合大量实际案例，帮助理解操作系统底层原理");
        book2.setRelevanceScore(0.93);
        osBooks.add(book2);

        // 推荐书籍3
        RecommendedBookDTO book3 = new RecommendedBookDTO();
        book3.setId(503L);
        book3.setTitle("计算机操作系统");
        book3.setAuthor("汤小丹");
        book3.setCoverImage("https://img1.doubanio.com/view/subject/l/public/s1081240.jpg");
        book3.setDescription("国内经典操作系统教材，全面覆盖全国硕士研究生招生考试操作系统考试大纲");
        book3.setMajor("计算机科学与技术");
        book3.setSemester("大三");
        book3.setCourseType("专业必修课");
        book3.setSubject("操作系统");
        book3.setRecommendationReason("适合国内高校教学体系，内容贴合课程要求，考研必备");
        book3.setRelevanceScore(0.90);
        osBooks.add(book3);

        // 用户评价1
        UserReviewDTO review1 = new UserReviewDTO();
        review1.setId(601L);
        review1.setBookId(213L);
        review1.setUserId(1006L);
        review1.setUsername("编程达人");
        review1.setRating(5);
        review1.setContent("《操作系统概念》是学习操作系统的圣经，内容全面，讲解清晰，配合课程学习效果极佳");
        review1.setCreatedAt(LocalDateTime.now().minusDays(10));
        review1.setLikes(120);
        osReviews.add(review1);

        // 用户评价2
        UserReviewDTO review2 = new UserReviewDTO();
        review2.setId(602L);
        review2.setBookId(213L);
        review2.setUserId(1007L);
        review2.setUsername("考研学生");
        review2.setRating(4);
        review2.setContent("汤小丹的教材非常适合备考，内容紧扣考试大纲，例题和习题对巩固知识点很有帮助");
        review2.setCreatedAt(LocalDateTime.now().minusDays(4));
        review2.setLikes(78);
        osReviews.add(review2);

        osRecommendations.setRecommendedBooks(osBooks);
        osRecommendations.setUserReviews(osReviews);
        DEFAULT_RECOMMENDATIONS.put("操作系统原理", osRecommendations);
    }

    // 4. 数据库系统概论推荐数据
    private static void addDatabaseRecommendations() {
        BookRecommendationDTO dbRecommendations = new BookRecommendationDTO();
        List<RecommendedBookDTO> dbBooks = new ArrayList<>();
        List<UserReviewDTO> dbReviews = new ArrayList<>();

        // 推荐书籍1
        RecommendedBookDTO book1 = new RecommendedBookDTO();
        book1.setId(601L);
        book1.setTitle("数据库系统概论");
        book1.setAuthor("王珊, 萨师煊");
        book1.setCoverImage("https://img3.doubanio.com/view/subject/l/public/s1081241.jpg");
        book1.setDescription("第6版，国内数据库教学的权威教材，被众多高校选用为数据库课程教材");
        book1.setMajor("计算机科学与技术");
        book1.setSemester("大三");
        book1.setCourseType("专业必修课");
        book1.setSubject("数据库");
        book1.setRecommendationReason("与数据库系统概论课程内容完全匹配，系统讲解数据库核心理论");
        book1.setRelevanceScore(0.97);
        dbBooks.add(book1);

        // 推荐书籍2
        RecommendedBookDTO book2 = new RecommendedBookDTO();
        book2.setId(602L);
        book2.setTitle("Database System Concepts");
        book2.setAuthor("Hector Garcia-Molina");
        book2.setCoverImage("https://img9.doubanio.com/view/subject/l/public/s1081242.jpg");
        book2.setDescription("国际经典数据库教材，全面讲解数据库系统概念、设计和实现技术");
        book2.setMajor("计算机科学与技术");
        book2.setSemester("大三");
        book2.setCourseType("专业必修课");
        book2.setSubject("数据库");
        book2.setRecommendationReason("原版教材，内容前沿，适合想深入学习数据库理论的学生");
        book2.setRelevanceScore(0.92);
        dbBooks.add(book2);

        // 推荐书籍3
        RecommendedBookDTO book3 = new RecommendedBookDTO();
        book3.setId(603L);
        book3.setTitle("数据库管理系统原理与实现");
        book3.setAuthor("杜小勇, 陈红");
        book3.setCoverImage("https://img1.doubanio.com/view/subject/l/public/s1081243.jpg");
        book3.setDescription("清华大学出版社出版，深入讲解数据库管理系统的原理与实现细节");
        book3.setMajor("计算机科学与技术");
        book3.setSemester("大三");
        book3.setCourseType("专业选修课");
        book3.setSubject("数据库");
        book3.setRecommendationReason("适合想深入了解数据库底层实现的学生，与课程内容互补");
        book3.setRelevanceScore(0.88);
        dbBooks.add(book3);

        // 用户评价1
        UserReviewDTO review1 = new UserReviewDTO();
        review1.setId(701L);
        review1.setBookId(214L);
        review1.setUserId(1008L);
        review1.setUsername("数据库开发者");
        review1.setRating(5);
        review1.setContent("王珊和萨师煊的教材是数据库学习的经典，内容系统全面，是学习数据库的必备书籍");
        review1.setCreatedAt(LocalDateTime.now().minusDays(8));
        review1.setLikes(95);
        dbReviews.add(review1);

        // 用户评价2
        UserReviewDTO review2 = new UserReviewDTO();
        review2.setId(702L);
        review2.setBookId(214L);
        review2.setUserId(1009L);
        review2.setUsername("考研党");
        review2.setRating(4);
        review2.setContent("这本书对考研帮助很大，很多高校的数据库考研真题都能在书中找到对应的知识点");
        review2.setCreatedAt(LocalDateTime.now().minusDays(3));
        review2.setLikes(67);
        dbReviews.add(review2);

        dbRecommendations.setRecommendedBooks(dbBooks);
        dbRecommendations.setUserReviews(dbReviews);
        DEFAULT_RECOMMENDATIONS.put("数据库系统概论", dbRecommendations);
    }

    // 5. 数据结构与算法推荐数据
    private static void addDataStructureRecommendations() {
        BookRecommendationDTO dsRecommendations = new BookRecommendationDTO();
        List<RecommendedBookDTO> dsBooks = new ArrayList<>();
        List<UserReviewDTO> dsReviews = new ArrayList<>();

        // 推荐书籍1
        RecommendedBookDTO book1 = new RecommendedBookDTO();
        book1.setId(701L);
        book1.setTitle("算法导论");
        book1.setAuthor("Thomas H. Cormen");
        book1.setCoverImage("https://img3.doubanio.com/view/subject/l/public/s1081244.jpg");
        book1.setDescription("原书第3版，算法领域的经典著作，被全球众多高校选作教材，豆瓣评分8.4");
        book1.setMajor("计算机科学与技术");
        book1.setSemester("大二");
        book1.setCourseType("专业必修课");
        book1.setSubject("数据结构与算法");
        book1.setRecommendationReason("全面讲解各种数据结构和算法，与课程内容高度相关");
        book1.setRelevanceScore(0.95);
        dsBooks.add(book1);

        // 推荐书籍2
        RecommendedBookDTO book2 = new RecommendedBookDTO();
        book2.setId(702L);
        book2.setTitle("数据结构与算法分析");
        book2.setAuthor("Mark Allen Weiss");
        book2.setCoverImage("https://img9.doubanio.com/view/subject/l/public/s1081245.jpg");
        book2.setDescription("详细讲解数据结构的设计和分析方法，注重算法效率和实际应用");
        book2.setMajor("计算机科学与技术");
        book2.setSemester("大二");
        book2.setCourseType("专业必修课");
        book2.setSubject("数据结构与算法");
        book2.setRecommendationReason("内容深入浅出，适合作为数据结构课程的辅助教材");
        book2.setRelevanceScore(0.92);
        dsBooks.add(book2);

        // 推荐书籍3
        RecommendedBookDTO book3 = new RecommendedBookDTO();
        book3.setId(703L);
        book3.setTitle("数据结构(C语言版)");
        book3.setAuthor("严蔚敏");
        book3.setCoverImage("https://img1.doubanio.com/view/subject/l/public/s1081246.jpg");
        book3.setDescription("国内经典数据结构教材，被众多高校选用为数据结构课程的指定教材");
        book3.setMajor("计算机科学与技术");
        book3.setSemester("大二");
        book3.setCourseType("专业必修课");
        book3.setSubject("数据结构与算法");
        book3.setRecommendationReason("适合国内教学体系，内容贴合课程要求，例题丰富");
        book3.setRelevanceScore(0.90);
        dsBooks.add(book3);

        // 用户评价1
        UserReviewDTO review1 = new UserReviewDTO();
        review1.setId(801L);
        review1.setBookId(211L);
        review1.setUserId(1010L);
        review1.setUsername("算法爱好者");
        review1.setRating(5);
        review1.setContent("《算法导论》是学习算法的圣经，虽然有些难度，但内容全面，讲解深入，值得反复阅读");
        review1.setCreatedAt(LocalDateTime.now().minusDays(12));
        review1.setLikes(156);
        dsReviews.add(review1);

        // 用户评价2
        UserReviewDTO review2 = new UserReviewDTO();
        review2.setId(802L);
        review2.setBookId(211L);
        review2.setUserId(1011L);
        review2.setUsername("计算机专业学生");
        review2.setRating(4);
        review2.setContent("严蔚敏的教材非常适合初学者，代码示例清晰，配合课程学习能很好地掌握数据结构知识");
        review2.setCreatedAt(LocalDateTime.now().minusDays(5));
        review2.setLikes(89);
        dsReviews.add(review2);

        dsRecommendations.setRecommendedBooks(dsBooks);
        dsRecommendations.setUserReviews(dsReviews);
        DEFAULT_RECOMMENDATIONS.put("数据结构与算法", dsRecommendations);
    }

    // 6. 新视野大学英语1推荐数据
    private static void addEnglishRecommendations() {
        BookRecommendationDTO englishRecommendations = new BookRecommendationDTO();
        List<RecommendedBookDTO> englishBooks = new ArrayList<>();
        List<UserReviewDTO> englishReviews = new ArrayList<>();

        // 推荐书籍1
        RecommendedBookDTO book1 = new RecommendedBookDTO();
        book1.setId(801L);
        book1.setTitle("新视野大学英语1(读写教程)");
        book1.setAuthor("郑树棠");
        book1.setCoverImage("https://img3.doubanio.com/view/subject/l/public/s1081247.jpg");
        book1.setDescription("第三版，\"十二五\"普通高等教育本科国家级规划教材，外语教学与研究出版社出版");
        book1.setMajor("全专业");
        book1.setSemester("大一");
        book1.setCourseType("公共必修课");
        book1.setSubject("英语");
        book1.setRecommendationReason("与新视野大学英语1课程完全配套，内容一致");
        book1.setRelevanceScore(0.98);
        englishBooks.add(book1);

        // 推荐书籍2
        RecommendedBookDTO book2 = new RecommendedBookDTO();
        book2.setId(802L);
        book2.setTitle("新视野大学英语1(视听说教程)");
        book2.setAuthor("郑树棠");
        book2.setCoverImage("https://img9.doubanio.com/view/subject/l/public/s1081248.jpg");
        book2.setDescription("第三版，与读写教程配套，专注于英语听力和口语能力的培养");
        book2.setMajor("全专业");
        book2.setSemester("大一");
        book2.setCourseType("公共必修课");
        book2.setSubject("英语");
        book2.setRecommendationReason("提升英语听说能力，与课程内容互补");
        book2.setRelevanceScore(0.95);
        englishBooks.add(book2);

        // 推荐书籍3
        RecommendedBookDTO book3 = new RecommendedBookDTO();
        book3.setId(803L);
        book3.setTitle("新视野大学英语1(综合训练)");
        book3.setAuthor("郑树棠");
        book3.setCoverImage("https://img1.doubanio.com/view/subject/l/public/s1081249.jpg");
        book3.setDescription("第三版，针对高等学校英语各项技能训练而开发的配套练习教程");
        book3.setMajor("全专业");
        book3.setSemester("大一");
        book3.setCourseType("公共必修课");
        book3.setSubject("英语");
        book3.setRecommendationReason("提供丰富的练习，帮助巩固读写教程中所学的词汇和语法");
        book3.setRelevanceScore(0.93);
        englishBooks.add(book3);

        // 用户评价1
        UserReviewDTO review1 = new UserReviewDTO();
        review1.setId(901L);
        review1.setBookId(222L);
        review1.setUserId(1012L);
        review1.setRating(5);
        review1.setContent("教材内容丰富，课文有趣，配套的U校园平台提供了很多学习资源，对英语学习帮助很大");
        review1.setCreatedAt(LocalDateTime.now().minusDays(6));
        review1.setLikes(76);
        englishReviews.add(review1);

        // 用户评价2
        UserReviewDTO review2 = new UserReviewDTO();
        review2.setId(902L);
        review2.setBookId(222L);
        review2.setUserId(1013L);
        review2.setRating(4);
        review2.setContent("视听说教程的听力材料难度适中，配合综合训练，能有效提升英语综合能力");
        review2.setCreatedAt(LocalDateTime.now().minusDays(2));
        review2.setLikes(45);
        englishReviews.add(review2);

        englishRecommendations.setRecommendedBooks(englishBooks);
        englishRecommendations.setUserReviews(englishReviews);
        DEFAULT_RECOMMENDATIONS.put("新视野大学英语1（第三版）", englishRecommendations);
    }

    // 7. 概率论与数理统计推荐数据
    private static void addProbabilityRecommendations() {
        BookRecommendationDTO probabilityRecommendations = new BookRecommendationDTO();
        List<RecommendedBookDTO> probabilityBooks = new ArrayList<>();
        List<UserReviewDTO> probabilityReviews = new ArrayList<>();

        // 推荐书籍1
        RecommendedBookDTO book1 = new RecommendedBookDTO();
        book1.setId(901L);
        book1.setTitle("概率论与数理统计");
        book1.setAuthor("盛骤, 谢式千, 潘承毅");
        book1.setCoverImage("https://img3.doubanio.com/view/subject/l/public/s27683796.jpg");
        book1.setDescription("第四版，高等教育出版社出版，国内概率论与数理统计课程的经典教材");
        book1.setMajor("理工科");
        book1.setSemester("大二");
        book1.setCourseType("公共必修课");
        book1.setSubject("数学");
        book1.setRecommendationReason("与概率论与数理统计课程内容完全匹配");
        book1.setRelevanceScore(0.96);
        probabilityBooks.add(book1);

        // 推荐书籍2
        RecommendedBookDTO book2 = new RecommendedBookDTO();
        book2.setId(902L);
        book2.setTitle("概率论与数理统计教程");
        book2.setAuthor("茆诗松");
        book2.setCoverImage("https://img9.doubanio.com/view/subject/l/public/s29901444.jpg");
        book2.setDescription("高等教育出版社出版，适合数学类专业，特别是统计学、应用数学等专业的基础课教材");
        book2.setMajor("数学类");
        book2.setSemester("大二");
        book2.setCourseType("专业必修课");
        book2.setSubject("数学");
        book2.setRecommendationReason("内容深入，适合想深入学习概率论与数理统计的学生");
        book2.setRelevanceScore(0.92);
        probabilityBooks.add(book2);

        // 推荐书籍3
        RecommendedBookDTO book3 = new RecommendedBookDTO();
        book3.setId(903L);
        book3.setTitle("Introduction to Probability");
        book3.setAuthor("Joseph K. Blitzstein");
        book3.setCoverImage("https://img1.doubanio.com/view/subject/l/public/s34692440.jpg");
        book3.setDescription("国际经典概率论教材，注重实际应用和统计思维的培养");
        book3.setMajor("理工科");
        book3.setSemester("大二");
        book3.setCourseType("公共必修课");
        book3.setSubject("数学");
        book3.setRecommendationReason("原版教材，内容生动，适合想提高数学英语水平的学生");
        book3.setRelevanceScore(0.89);
        probabilityBooks.add(book3);

        // 用户评价1
        UserReviewDTO review1 = new UserReviewDTO();
        review1.setId(1001L);
        review1.setBookId(208L);
        review1.setUserId(1014L);
        review1.setUsername("数学爱好者");
        review1.setRating(5);
        review1.setContent("盛骤的教材是学习概率论与数理统计的经典，公式推导详细，例题丰富，适合作为教材");
        review1.setCreatedAt(LocalDateTime.now().minusDays(7));
        review1.setLikes(67);
        probabilityReviews.add(review1);

        // 用户评价2
        UserReviewDTO review2 = new UserReviewDTO();
        review2.setId(1002L);
        review2.setBookId(208L);
        review2.setUserId(1015L);
        review2.setUsername("统计专业学生");
        review2.setRating(5);
        review2.setContent("茆诗松的教程讲解更深入，适合想要夯实统计学基础的同学，课后习题质量很高");
        review2.setCreatedAt(LocalDateTime.now().minusDays(4));
        review2.setLikes(52);
        probabilityReviews.add(review2);

        probabilityRecommendations.setRecommendedBooks(probabilityBooks);
        probabilityRecommendations.setUserReviews(probabilityReviews);
        DEFAULT_RECOMMENDATIONS.put("概率论与数理统计", probabilityRecommendations);
    }

    // 8. 离散数学推荐数据
    private static void addDiscreteMathRecommendations() {
        BookRecommendationDTO discreteMathRecommendations = new BookRecommendationDTO();
        List<RecommendedBookDTO> discreteMathBooks = new ArrayList<>();
        List<UserReviewDTO> discreteMathReviews = new ArrayList<>();

        // 推荐书籍1
        RecommendedBookDTO book1 = new RecommendedBookDTO();
        book1.setId(1001L);
        book1.setTitle("离散数学");
        book1.setAuthor("屈婉玲");
        book1.setCoverImage("https://img3.doubanio.com/view/subject/l/public/s27683797.jpg");
        book1.setDescription("高等教育出版社，国内计算机专业离散数学核心教材，覆盖数理逻辑、集合论等核心内容");
        book1.setMajor("计算机科学与技术");
        book1.setSemester("大二");
        book1.setCourseType("专业必修课");
        book1.setSubject("数学");
        book1.setRecommendationReason("与离散数学课程完全匹配，高校指定核心教材");
        book1.setRelevanceScore(0.97);
        discreteMathBooks.add(book1);

        // 推荐书籍2
        RecommendedBookDTO book2 = new RecommendedBookDTO();
        book2.setId(1002L);
        book2.setTitle("离散数学及其应用");
        book2.setAuthor("Kenneth H. Rosen");
        book2.setCoverImage("https://img9.doubanio.com/view/subject/l/public/s33456789.jpg");
        book2.setDescription("国际经典离散数学教材，案例丰富，适合计算机专业入门与进阶");
        book2.setMajor("计算机科学与技术");
        book2.setSemester("大二");
        book2.setCourseType("专业必修课");
        book2.setSubject("数学");
        book2.setRecommendationReason("内容通俗易懂，适合辅助学习离散数学核心概念");
        book2.setRelevanceScore(0.93);
        discreteMathBooks.add(book2);

        // 推荐书籍3
        RecommendedBookDTO book3 = new RecommendedBookDTO();
        book3.setId(1003L);
        book3.setTitle("离散数学学习指导与习题解析");
        book3.setAuthor("屈婉玲");
        book3.setCoverImage("https://img1.doubanio.com/view/subject/l/public/s27683798.jpg");
        book3.setDescription("配套教材习题集，覆盖所有知识点，考研必备");
        book3.setMajor("计算机科学与技术");
        book3.setSemester("大二");
        book3.setCourseType("专业必修课");
        book3.setSubject("数学");
        book3.setRecommendationReason("配套习题解析，快速巩固离散数学知识点");
        book3.setRelevanceScore(0.90);
        discreteMathBooks.add(book3);

        // 用户评价
        UserReviewDTO review1 = new UserReviewDTO();
        review1.setId(1101L);
        review1.setBookId(216L);
        review1.setUserId(1016L);
        review1.setUsername("计算机学生");
        review1.setRating(5);
        review1.setContent("屈婉玲的离散数学是高校标配，讲解清晰，配合习题集学习效果翻倍");
        review1.setCreatedAt(LocalDateTime.now().minusDays(6));
        review1.setLikes(73);
        discreteMathReviews.add(review1);

        discreteMathRecommendations.setRecommendedBooks(discreteMathBooks);
        discreteMathRecommendations.setUserReviews(discreteMathReviews);
        DEFAULT_RECOMMENDATIONS.put("离散数学", discreteMathRecommendations);
    }

    // 9. 线性代数推荐数据
    private static void addLinearAlgebraRecommendations() {
        BookRecommendationDTO linearAlgebraRecommendations = new BookRecommendationDTO();
        List<RecommendedBookDTO> linearAlgebraBooks = new ArrayList<>();
        List<UserReviewDTO> linearAlgebraReviews = new ArrayList<>();

        // 推荐书籍1
        RecommendedBookDTO book1 = new RecommendedBookDTO();
        book1.setId(1101L);
        book1.setTitle("线性代数");
        book1.setAuthor("同济大学数学系");
        book1.setCoverImage("https://img3.doubanio.com/view/subject/l/public/s29869914.jpg");
        book1.setDescription("第七版，全国高校通用线性代数教材，公共必修课标配");
        book1.setMajor("全理工科");
        book1.setSemester("大一");
        book1.setCourseType("公共必修课");
        book1.setSubject("数学");
        book1.setRecommendationReason("与线性代数课程完全匹配，全国高校通用教材");
        book1.setRelevanceScore(0.98);
        linearAlgebraBooks.add(book1);

        // 推荐书籍2
        RecommendedBookDTO book2 = new RecommendedBookDTO();
        book2.setId(1102L);
        book2.setTitle("线性代数及其应用");
        book2.setAuthor("David C. Lay");
        book2.setCoverImage("https://img9.doubanio.com/view/subject/l/public/s33456790.jpg");
        book2.setDescription("国际经典线性代数教材，注重实际应用，通俗易懂");
        book2.setMajor("全理工科");
        book2.setSemester("大一");
        book2.setCourseType("公共必修课");
        book2.setSubject("数学");
        book2.setRecommendationReason("帮助理解线性代数的实际应用场景，突破抽象概念");
        book2.setRelevanceScore(0.94);
        linearAlgebraBooks.add(book2);

        // 推荐书籍3
        RecommendedBookDTO book3 = new RecommendedBookDTO();
        book3.setId(1103L);
        book3.setTitle("线性代数学习辅导与习题全解");
        book3.setAuthor("同济大学数学系");
        book3.setCoverImage("https://img1.doubanio.com/view/subject/l/public/s29869915.jpg");
        book3.setDescription("同济版线性代数配套习题解答，课后作业必备");
        book3.setMajor("全理工科");
        book3.setSemester("大一");
        book3.setCourseType("公共必修课");
        book3.setSubject("数学");
        book3.setRecommendationReason("配套教材使用，快速解决作业和考试难题");
        book3.setRelevanceScore(0.91);
        linearAlgebraBooks.add(book3);

        // 用户评价
        UserReviewDTO review1 = new UserReviewDTO();
        review1.setId(1201L);
        review1.setBookId(207L);
        review1.setUserId(1017L);
        review1.setUsername("工科学生");
        review1.setRating(5);
        review1.setContent("同济线性代数是大学必备，配合辅导书，轻松应对考试和考研");
        review1.setCreatedAt(LocalDateTime.now().minusDays(8));
        review1.setLikes(81);
        linearAlgebraReviews.add(review1);

        linearAlgebraRecommendations.setRecommendedBooks(linearAlgebraBooks);
        linearAlgebraRecommendations.setUserReviews(linearAlgebraReviews);
        DEFAULT_RECOMMENDATIONS.put("线性代数", linearAlgebraRecommendations);
    }

    // 10. 经济学原理推荐数据
    private static void addEconomicsRecommendations() {
        BookRecommendationDTO economicsRecommendations = new BookRecommendationDTO();
        List<RecommendedBookDTO> economicsBooks = new ArrayList<>();
        List<UserReviewDTO> economicsReviews = new ArrayList<>();

        // 推荐书籍1
        RecommendedBookDTO book1 = new RecommendedBookDTO();
        book1.setId(1201L);
        book1.setTitle("经济学原理（微观分册）");
        book1.setAuthor("[美]曼昆");
        book1.setCoverImage("https://img3.doubanio.com/view/subject/l/public/s27683799.jpg");
        book1.setDescription("全球最受欢迎的经济学入门教材，通俗易懂，案例贴近生活");
        book1.setMajor("经济学类");
        book1.setSemester("大二");
        book1.setCourseType("专业必修课");
        book1.setSubject("经济学");
        book1.setRecommendationReason("与经济学原理课程高度匹配，入门经济学必读经典");
        book1.setRelevanceScore(0.96);
        economicsBooks.add(book1);

        // 推荐书籍2
        RecommendedBookDTO book2 = new RecommendedBookDTO();
        book2.setId(1202L);
        book2.setTitle("经济学原理（宏观分册）");
        book2.setAuthor("[美]曼昆");
        book2.setCoverImage("https://img9.doubanio.com/view/subject/l/public/s27683800.jpg");
        book2.setDescription("曼昆经典教材，系统讲解宏观经济学核心理论");
        book2.setMajor("经济学类");
        book2.setSemester("大二");
        book2.setCourseType("专业必修课");
        book2.setSubject("经济学");
        book2.setRecommendationReason("完整学习经济学原理，宏观微观配套学习");
        book2.setRelevanceScore(0.93);
        economicsBooks.add(book2);

        // 推荐书籍3
        RecommendedBookDTO book3 = new RecommendedBookDTO();
        book3.setId(1203L);
        book3.setTitle("西方经济学（微观部分）");
        book3.setAuthor("高鸿业");
        book3.setCoverImage("https://img1.doubanio.com/view/subject/l/public/s27683801.jpg");
        book3.setDescription("国内高校经济学指定教材，考研必备");
        book3.setMajor("经济学类");
        book3.setSemester("大二");
        book3.setCourseType("专业必修课");
        book3.setSubject("经济学");
        book3.setRecommendationReason("贴合国内教学体系，经济学考研核心教材");
        book3.setRelevanceScore(0.90);
        economicsBooks.add(book3);

        // 用户评价
        UserReviewDTO review1 = new UserReviewDTO();
        review1.setId(1301L);
        review1.setBookId(217L);
        review1.setUserId(1018L);
        review1.setUsername("经济专业学生");
        review1.setContent("曼昆的经济学原理通俗易懂，零基础也能学会，非常适合入门");
        review1.setRating(5);
        review1.setCreatedAt(LocalDateTime.now().minusDays(5));
        review1.setLikes(69);
        economicsReviews.add(review1);

        economicsRecommendations.setRecommendedBooks(economicsBooks);
        economicsRecommendations.setUserReviews(economicsReviews);
        DEFAULT_RECOMMENDATIONS.put("经济学原理", economicsRecommendations);
    }

    // 11. 计算机网络推荐数据
    private static void addComputerNetworkRecommendations() {
        BookRecommendationDTO networkRecommendations = new BookRecommendationDTO();
        List<RecommendedBookDTO> networkBooks = new ArrayList<>();
        List<UserReviewDTO> networkReviews = new ArrayList<>();

        // 推荐书籍1
        RecommendedBookDTO book1 = new RecommendedBookDTO();
        book1.setId(1301L);
        book1.setTitle("计算机网络");
        book1.setAuthor("谢希仁");
        book1.setCoverImage("https://img3.doubanio.com/view/subject/l/public/s27683802.jpg");
        book1.setDescription("第八版，国内计算机网络权威教材，高校指定标配");
        book1.setMajor("计算机科学与技术");
        book1.setSemester("大三");
        book1.setCourseType("专业必修课");
        book1.setSubject("计算机网络");
        book1.setRecommendationReason("与计算机网络课程完全匹配，国内计算机专业标配教材");
        book1.setRelevanceScore(0.98);
        networkBooks.add(book1);

        // 推荐书籍2
        RecommendedBookDTO book2 = new RecommendedBookDTO();
        book2.setId(1302L);
        book2.setTitle("计算机网络：自顶向下方法");
        book2.setAuthor("James F. Kurose");
        book2.setCoverImage("https://img9.doubanio.com/view/subject/l/public/s33456791.jpg");
        book2.setDescription("国际经典计算机网络教材，自顶向下讲解，通俗易懂");
        book2.setMajor("计算机科学与技术");
        book2.setSemester("大三");
        book2.setCourseType("专业必修课");
        book2.setSubject("计算机网络");
        book2.setRecommendationReason("辅助学习计算机网络，理解网络原理更轻松");
        book2.setRelevanceScore(0.94);
        networkBooks.add(book2);

        // 推荐书籍3
        RecommendedBookDTO book3 = new RecommendedBookDTO();
        book3.setId(1303L);
        book3.setTitle("TCP/IP详解 卷1：协议");
        book3.setAuthor("[美]史蒂文斯");
        book3.setCoverImage("https://img1.doubanio.com/view/subject/l/public/s27683803.jpg");
        book3.setDescription("网络协议经典著作，深入理解TCP/IP协议栈");
        book3.setMajor("计算机科学与技术");
        book3.setSemester("大三");
        book3.setCourseType("专业选修课");
        book3.setSubject("计算机网络");
        book3.setRecommendationReason("深入学习网络协议，提升网络底层理解能力");
        book3.setRelevanceScore(0.89);
        networkBooks.add(book3);

        // 用户评价
        UserReviewDTO review1 = new UserReviewDTO();
        review1.setId(1401L);
        review1.setBookId(212L);
        review1.setUserId(1019L);
        review1.setUsername("网络工程师");
        review1.setRating(5);
        review1.setContent("谢希仁的计算机网络是考研和工作必备，讲解清晰，知识点全面");
        review1.setCreatedAt(LocalDateTime.now().minusDays(9));
        review1.setLikes(102);
        networkReviews.add(review1);

        networkRecommendations.setRecommendedBooks(networkBooks);
        networkRecommendations.setUserReviews(networkReviews);
        DEFAULT_RECOMMENDATIONS.put("计算机网络", networkRecommendations);
    }

    // 12. 马克思主义基本原理推荐数据
    private static void addMarxismRecommendations() {
        BookRecommendationDTO marxismRecommendations = new BookRecommendationDTO();
        List<RecommendedBookDTO> marxismBooks = new ArrayList<>();
        List<UserReviewDTO> marxismReviews = new ArrayList<>();

        // 推荐书籍1
        RecommendedBookDTO book1 = new RecommendedBookDTO();
        book1.setId(1401L);
        book1.setTitle("马克思主义基本原理概论");
        book1.setAuthor("本书编写组");
        book1.setCoverImage("https://img3.doubanio.com/view/subject/l/public/s27683804.jpg");
        book1.setDescription("2023年版，全国高校思想政治理论课统编教材");
        book1.setMajor("全专业");
        book1.setSemester("大二");
        book1.setCourseType("公共必修课");
        book1.setSubject("思想政治");
        book1.setRecommendationReason("与马原课程完全配套，官方指定统编教材");
        book1.setRelevanceScore(0.99);
        marxismBooks.add(book1);

        // 推荐书籍2
        RecommendedBookDTO book2 = new RecommendedBookDTO();
        book2.setId(1402L);
        book2.setTitle("马克思主义基本原理学习指南");
        book2.setAuthor("高等教育出版社");
        book2.setCoverImage("https://img9.doubanio.com/view/subject/l/public/s27683805.jpg");
        book2.setDescription("配套学习资料，知识点总结+习题解析，考试必备");
        book2.setMajor("全专业");
        book2.setSemester("大二");
        book2.setCourseType("公共必修课");
        book2.setSubject("思想政治");
        book2.setRecommendationReason("辅助学习马原，快速掌握考点，应对期末考试");
        book2.setRelevanceScore(0.95);
        marxismBooks.add(book2);

        // 推荐书籍3
        RecommendedBookDTO book3 = new RecommendedBookDTO();
        book3.setId(1403L);
        book3.setTitle("共产党宣言");
        book3.setAuthor("马克思, 恩格斯");
        book3.setCoverImage("https://img1.doubanio.com/view/subject/l/public/s27683806.jpg");
        book3.setDescription("马克思主义经典著作，理解马原核心思想必读");
        book3.setMajor("全专业");
        book3.setSemester("全学期");
        book3.setCourseType("公共选修课");
        book3.setSubject("思想政治");
        book3.setRecommendationReason("深入理解马克思主义核心理论，提升理论素养");
        book3.setRelevanceScore(0.90);
        marxismBooks.add(book3);

        // 用户评价
        UserReviewDTO review1 = new UserReviewDTO();
        review1.setId(1501L);
        review1.setBookId(221L);
        review1.setUserId(1020L);
        review1.setUsername("大学生");
        review1.setRating(4);
        review1.setContent("官方教材知识点全面，配合学习指南，轻松应对马原期末考试");
        review1.setCreatedAt(LocalDateTime.now().minusDays(3));
        review1.setLikes(47);
        marxismReviews.add(review1);

        marxismRecommendations.setRecommendedBooks(marxismBooks);
        marxismRecommendations.setUserReviews(marxismReviews);
        DEFAULT_RECOMMENDATIONS.put("马克思主义基本原理", marxismRecommendations);
    }

    // 13. 高等数学推荐数据
    private static void addAdvancedMathRecommendations() {
        BookRecommendationDTO advancedMathRecommendations = new BookRecommendationDTO();
        List<RecommendedBookDTO> advancedMathBooks = new ArrayList<>();
        List<UserReviewDTO> advancedMathReviews = new ArrayList<>();

        // 推荐书籍1
        RecommendedBookDTO book1 = new RecommendedBookDTO();
        book1.setId(1501L);
        book1.setTitle("高等数学");
        book1.setAuthor("同济大学数学系");
        book1.setCoverImage("https://img3.doubanio.com/view/subject/l/public/s27195142.jpg");
        book1.setDescription("第七版，全国高校通用高等数学教材，上下册通用");
        book1.setMajor("全理工科");
        book1.setSemester("大一");
        book1.setCourseType("公共必修课");
        book1.setSubject("数学");
        book1.setRecommendationReason("与高等数学上下册完全匹配，高校通用标配教材");
        book1.setRelevanceScore(0.98);
        advancedMathBooks.add(book1);

        // 推荐书籍2
        RecommendedBookDTO book2 = new RecommendedBookDTO();
        book2.setId(1502L);
        book2.setTitle("高等数学习题全解指导");
        book2.setAuthor("同济大学数学系");
        book2.setCoverImage("https://img9.doubanio.com/view/subject/l/public/s27195143.jpg");
        book2.setDescription("同济高数配套习题解答，课后作业、考研必备");
        book2.setMajor("全理工科");
        book2.setSemester("大一");
        book2.setCourseType("公共必修课");
        book2.setSubject("数学");
        book2.setRecommendationReason("配套教材使用，快速解决高数习题难题");
        book2.setRelevanceScore(0.94);
        advancedMathBooks.add(book2);

        // 推荐书籍3
        RecommendedBookDTO book3 = new RecommendedBookDTO();
        book3.setId(1503L);
        book3.setTitle("普林斯顿微积分读本");
        book3.setAuthor("[美]班尼特");
        book3.setCoverImage("https://img1.doubanio.com/view/subject/l/public/s33456792.jpg");
        book3.setDescription("通俗易懂的微积分入门书，攻克高数难点");
        book3.setMajor("全理工科");
        book3.setSemester("大一");
        book3.setCourseType("公共必修课");
        book3.setSubject("数学");
        book3.setRecommendationReason("辅助理解微积分核心概念，轻松搞定高数难点");
        book3.setRelevanceScore(0.91);
        advancedMathBooks.add(book3);

        // 用户评价
        UserReviewDTO review1 = new UserReviewDTO();
        review1.setId(1601L);
        review1.setBookId(205L);
        review1.setUserId(1021L);
        review1.setUsername("理工学生");
        review1.setRating(5);
        review1.setContent("同济高数是大学数学基础，配合习题全解，考研和期末考试都够用");
        review1.setCreatedAt(LocalDateTime.now().minusDays(10));
        review1.setLikes(98);
        advancedMathReviews.add(review1);

        advancedMathRecommendations.setRecommendedBooks(advancedMathBooks);
        advancedMathRecommendations.setUserReviews(advancedMathReviews);
        DEFAULT_RECOMMENDATIONS.put("高等数学（上册）", advancedMathRecommendations);
        DEFAULT_RECOMMENDATIONS.put("高等数学（下册）", advancedMathRecommendations);
    }

    // 14. Java推荐数据（保留原版）
    private static void addJavaRecommendations() {
        BookRecommendationDTO javaRecommendations = new BookRecommendationDTO();
        List<RecommendedBookDTO> javaBooks = new ArrayList<>();
        List<UserReviewDTO> javaReviews = new ArrayList<>();

        RecommendedBookDTO javaBook1 = new RecommendedBookDTO();
        javaBook1.setId(101L);
        javaBook1.setTitle("Java核心技术 卷1：基础知识");
        javaBook1.setAuthor("Cay S. Horstmann");
        javaBook1.setCoverImage("https://img3.doubanio.com/view/subject/l/public/s1074393.jpg");
        javaBook1.setDescription("Java领域经典之作，全面覆盖核心概念和技术");
        javaBook1.setMajor("计算机科学");
        javaBook1.setSemester("大二");
        javaBook1.setCourseType("专业必修课");
        javaBook1.setSubject("编程语言");
        javaBook1.setRecommendationReason("与Java相关书籍高度相关，内容深入浅出");
        javaBook1.setRelevanceScore(0.95);
        javaBooks.add(javaBook1);

        UserReviewDTO javaReview1 = new UserReviewDTO();
        javaReview1.setId(201L);
        javaReview1.setBookId(1L);
        javaReview1.setUserId(1001L);
        javaReview1.setUsername("编程爱好者");
        javaReview1.setRating(5);
        javaReview1.setContent("Java学习必备书籍，讲解清晰，例子丰富");
        javaReview1.setCreatedAt(LocalDateTime.now().minusDays(15));
        javaReview1.setLikes(45);
        javaReviews.add(javaReview1);

        javaRecommendations.setRecommendedBooks(javaBooks);
        javaRecommendations.setUserReviews(javaReviews);
        DEFAULT_RECOMMENDATIONS.put("java", javaRecommendations);
    }

    /**
     * 根据书籍名称获取默认推荐数据
     * @param bookName 书籍名称
     * @param bookId 书籍ID
     * @return 书籍推荐DTO
     */
    public static BookRecommendationDTO getDefaultRecommendations(String bookName, Long bookId) {
        String bookType = analyzeBookType(bookName);
        BookRecommendationDTO recommendations = DEFAULT_RECOMMENDATIONS.get(bookType);

        if (recommendations == null) {
            recommendations = DEFAULT_RECOMMENDATIONS.get("java");
        }

        BookRecommendationDTO clonedRecommendations = cloneRecommendationDTO(recommendations);

        if (clonedRecommendations.getUserReviews() != null) {
            for (UserReviewDTO review : clonedRecommendations.getUserReviews()) {
                review.setBookId(bookId);
            }
        }

        return clonedRecommendations;
    }

    /**
     * 优化书籍类型识别逻辑，精准匹配所有课程名称
     */
    private static String analyzeBookType(String bookName) {
        if (bookName == null || bookName.isEmpty()) {
            return "java";
        }

        String lowerName = bookName.toLowerCase();

        // 精准匹配所有课程
        if (lowerName.contains("体育与健康")) return "体育与健康";
        if (lowerName.contains("大学物理")) return "大学物理（上册）";
        if (lowerName.contains("操作系统")) return "操作系统原理";
        if (lowerName.contains("数据库系统")) return "数据库系统概论";
        if (lowerName.contains("数据结构")) return "数据结构与算法";
        if (lowerName.contains("新视野大学英语")) return "新视野大学英语1（第三版）";
        if (lowerName.contains("概率论")) return "概率论与数理统计";
        if (lowerName.contains("离散数学")) return "离散数学";
        if (lowerName.contains("线性代数")) return "线性代数";
        if (lowerName.contains("经济学原理")) return "经济学原理";
        if (lowerName.contains("计算机网络")) return "计算机网络";
        if (lowerName.contains("马克思主义")) return "马克思主义基本原理";
        if (lowerName.contains("高等数学")) return "高等数学（上册）";
        if (lowerName.contains("java") || lowerName.contains("编程")) return "java";

        return "java";
    }

    /**
     * 克隆对象，防止原始数据被修改
     */
    private static BookRecommendationDTO cloneRecommendationDTO(BookRecommendationDTO original) {
        if (original == null) {
            return new BookRecommendationDTO();
        }

        BookRecommendationDTO clone = new BookRecommendationDTO();

        if (original.getRecommendedBooks() != null) {
            List<RecommendedBookDTO> clonedBooks = new ArrayList<>();
            for (RecommendedBookDTO book : original.getRecommendedBooks()) {
                RecommendedBookDTO clonedBook = new RecommendedBookDTO();
                clonedBook.setId(book.getId());
                clonedBook.setTitle(book.getTitle());
                clonedBook.setAuthor(book.getAuthor());
                clonedBook.setCoverImage(book.getCoverImage());
                clonedBook.setDescription(book.getDescription());
                clonedBook.setMajor(book.getMajor());
                clonedBook.setSemester(book.getSemester());
                clonedBook.setCourseType(book.getCourseType());
                clonedBook.setSubject(book.getSubject());
                clonedBook.setRecommendationReason(book.getRecommendationReason());
                clonedBook.setRelevanceScore(book.getRelevanceScore());
                clonedBooks.add(clonedBook);
            }
            clone.setRecommendedBooks(clonedBooks);
        }

        if (original.getUserReviews() != null) {
            List<UserReviewDTO> clonedReviews = new ArrayList<>();
            for (UserReviewDTO review : original.getUserReviews()) {
                UserReviewDTO clonedReview = new UserReviewDTO();
                clonedReview.setId(review.getId());
                clonedReview.setBookId(review.getBookId());
                clonedReview.setUserId(review.getUserId());
                clonedReview.setUsername(review.getUsername());
                clonedReview.setRating(review.getRating());
                clonedReview.setContent(review.getContent());
                clonedReview.setCreatedAt(review.getCreatedAt());
                clonedReview.setLikes(review.getLikes());
                clonedReviews.add(clonedReview);
            }
            clone.setUserReviews(clonedReviews);
        }

        return clone;
    }
}
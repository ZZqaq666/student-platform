package com.student.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

/**
 * 学生学科智能问答平台 - 单体应用入口
 *
 * 合并原微服务版（gateway + user/qa/learning/exercise/knowledge 五个服务）为单一 Spring Boot 应用。
 * - 端口 8080，context-path 为 /api（与原网关对外路径保持一致，前端无需改动）
 * - Mapper 接口来自五个业务包，bean 名使用全限定类名生成器，避免跨包同名 Mapper 冲突
 */
@SpringBootApplication(nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
@MapperScan(
        basePackages = {
                "com.student.platform.user.mapper",
                "com.student.platform.qa.mapper",
                "com.student.platform.learning.mapper",
                "com.student.platform.exercise.mapper",
                "com.student.platform.knowledge.mapper"
        },
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class
)
public class StudentPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudentPlatformApplication.class, args);
    }
}

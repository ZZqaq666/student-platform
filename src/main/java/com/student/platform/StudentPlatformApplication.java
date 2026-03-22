package com.student.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.student.platform.mapper")
public class StudentPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudentPlatformApplication.class, args);
    }

}

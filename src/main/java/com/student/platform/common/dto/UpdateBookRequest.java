package com.student.platform.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 编辑共享目录的请求体。
 *
 * 刻意只列可改字段：不接收 status/isbn/coverImage 等，避免一次编辑顺带把书下架。
 */
@Data
public class UpdateBookRequest {

    @NotBlank(message = "书名不能为空")
    @Size(max = 200, message = "书名长度不能超过200")
    private String title;

    @Size(max = 100, message = "作者长度不能超过100")
    private String author;

    @Size(max = 2000, message = "简介长度不能超过2000")
    private String description;

    @Size(max = 100, message = "专业长度不能超过100")
    private String major;

    @Size(max = 50, message = "学期长度不能超过50")
    private String semester;

    @Size(max = 50, message = "课程类型长度不能超过50")
    private String courseType;
}

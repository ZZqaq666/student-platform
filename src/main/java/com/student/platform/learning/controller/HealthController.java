package com.student.platform.learning.controller;

import com.student.platform.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "健康检查", description = "系统健康检查接口")
@RestController
@RequestMapping("/health")
@Slf4j
public class HealthController {

    @Operation(
            summary = "健康检查",
            description = "检查系统是否正常运行，返回系统状态信息"
    )
    @GetMapping
    public Result<String> health() {
        log.debug("健康检查请求");
        return Result.success("系统运行正常");
    }

}

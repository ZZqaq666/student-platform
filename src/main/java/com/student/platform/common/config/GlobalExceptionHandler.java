package com.student.platform.common.config;

import com.student.platform.common.exception.BusinessException;
import com.student.platform.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import java.sql.SQLException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * 功能说明：
 * - 统一处理系统中各类异常，返回标准化的错误响应
 * - 使用@RestControllerAdvice注解，全局拦截所有Controller抛出的异常
 * - 根据异常类型返回不同的HTTP状态码和错误信息
 *
 * 异常处理分类：
 * - 业务异常(BusinessException)：返回自定义状态码和消息
 * - 参数校验异常：返回400状态码，包含具体校验失败信息
 * - 认证授权异常：返回401/403状态码
 * - 文件上传异常：返回413状态码
 * - 数据库异常：返回503状态码
 * - 未知异常：返回500状态码
 *
 * 错误响应格式：
 * {
 *   "code": 错误码,
 *   "message": "错误描述",
 *   "data": null
 * }
 *
 * @author student-platform
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     *
     * @param e       业务异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(value = BusinessException.class, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常 [{}] - 状态码: {}, 消息: {}",
                request.getRequestURI(), e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理方法参数校验异常(@Valid)
     *
     * @param e       参数校验异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(value = MethodArgumentNotValidException.class, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("参数校验异常 [{}] - {}", request.getRequestURI(), message);
        return Result.error(400, "参数校验失败: " + message);
    }

    /**
     * 处理约束违反异常(@Validated)
     *
     * @param e       约束违反异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(value = ConstraintViolationException.class, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.warn("约束违反异常 [{}] - {}", request.getRequestURI(), message);
        return Result.error(400, "参数校验失败: " + message);
    }

    /**
     * 处理参数绑定异常
     *
     * @param e       参数绑定异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(value = BindException.class, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("参数绑定异常 [{}] - {}", request.getRequestURI(), message);
        return Result.error(400, "参数绑定失败: " + message);
    }

    /**
     * 处理缺少请求参数异常
     *
     * @param e       缺少参数异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(value = MissingServletRequestParameterException.class, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e, HttpServletRequest request) {
        log.warn("缺少请求参数 [{}] - 参数名: {}", request.getRequestURI(), e.getParameterName());
        return Result.error(400, "缺少必要参数: " + e.getParameterName());
    }

    /**
     * 处理方法参数类型不匹配异常
     *
     * @param e       参数类型不匹配异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(value = MethodArgumentTypeMismatchException.class, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        log.warn("参数类型不匹配 [{}] - 参数名: {}, 期望类型: {}",
                request.getRequestURI(), e.getName(), e.getRequiredType());
        return Result.error(400, String.format("参数'%s'类型错误，期望类型: %s",
                e.getName(), e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知"));
    }

    /**
     * 处理请求体读取异常
     *
     * @param e       请求体读取异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(value = HttpMessageNotReadableException.class, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("请求体读取异常 [{}] - {}", request.getRequestURI(), e.getMessage());
        return Result.error(400, "请求体格式错误，请检查JSON格式");
    }

    /**
     * 处理访问拒绝异常
     *
     * @param e       访问拒绝异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(value = AccessDeniedException.class, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.warn("访问被拒绝 [{}] - {}", request.getRequestURI(), e.getMessage());
        return Result.error(403, "没有权限访问该资源");
    }

    /**
     * 处理认证异常
     *
     * @param e       认证异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(value = AuthenticationException.class, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthenticationException(AuthenticationException e, HttpServletRequest request) {
        log.warn("认证异常 [{}] - {}", request.getRequestURI(), e.getMessage());
        String message = "认证失败";
        if (e instanceof BadCredentialsException) {
            message = "用户名或密码错误";
        } else if (e instanceof DisabledException) {
            message = "账号已被禁用";
        }
        return Result.error(401, message);
    }

    /**
     * 处理文件上传大小超限异常
     *
     * @param e       文件大小超限异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(value = MaxUploadSizeExceededException.class, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public Result<Void> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException e, HttpServletRequest request) {
        log.warn("文件上传大小超限 [{}] - {}", request.getRequestURI(), e.getMessage());
        return Result.error(413, "上传文件大小超过限制");
    }

    /**
     * 处理404异常
     *
     * @param e       404异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(value = NoHandlerFoundException.class, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoHandlerFoundException(NoHandlerFoundException e, HttpServletRequest request) {
        log.warn("请求的资源不存在 [{}] - 方法: {}, URL: {}",
                request.getRequestURI(), e.getHttpMethod(), e.getRequestURL());
        return Result.error(404, "请求的资源不存在");
    }

    /**
     * 处理非法参数异常
     *
     * @param e       非法参数异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(value = IllegalArgumentException.class, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("非法参数 [{}] - {}", request.getRequestURI(), e.getMessage());
        return Result.error(400, e.getMessage());
    }

    /**
     * 处理非法状态异常
     *
     * @param e       非法状态异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(value = IllegalStateException.class, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalStateException(IllegalStateException e, HttpServletRequest request) {
        log.warn("非法状态 [{}] - {}", request.getRequestURI(), e.getMessage());
        return Result.error(400, e.getMessage());
    }

    /**
     * 处理空指针异常
     *
     * @param e       空指针异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(value = NullPointerException.class, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        log.error("空指针异常 [{}]", request.getRequestURI(), e);
        return Result.error(500, "系统内部错误");
    }

    /**
     * 处理数据库访问异常
     *
     * @param e       数据库访问异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(value = DataAccessException.class, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Result<Void> handleDataAccessException(DataAccessException e, HttpServletRequest request) {
        log.error("数据库访问异常 [{}] - {}", request.getRequestURI(), e.getMessage(), e);
        return Result.error(503, "数据库连接失败，请稍后重试");
    }

    /**
     * 处理SQL异常
     *
     * @param e       SQL异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(value = SQLException.class, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Result<Void> handleSQLException(SQLException e, HttpServletRequest request) {
        log.error("SQL异常 [{}] - 错误码: {}, 消息: {}", 
                request.getRequestURI(), e.getErrorCode(), e.getMessage(), e);
        return Result.error(503, "数据库操作失败，请稍后重试");
    }

    /**
     * 处理所有其他未捕获的异常
     *
     * @param e       异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(value = Exception.class, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 [{}] - 类型: {}, 消息: {}",
                request.getRequestURI(), e.getClass().getName(), e.getMessage(), e);
        return Result.error(500, "系统内部错误，请稍后重试");
    }

    /**
     * 未匹配到任何接口时应返回 404，而不是 500。
     *
     * Spring Boot 3.2+ 对未命中的路径抛 NoResourceFoundException；没接住就会落到
     * 上面的兜底分支——客户端拿到误导性的 500，日志则被扫描请求或拼错的 URL 以
     * ERROR 级别堆栈刷满。
     */
    @ExceptionHandler(value = NoResourceFoundException.class, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoResourceFoundException(NoResourceFoundException e, HttpServletRequest request) {
        log.warn("未匹配到接口 [{}] - 资源: {}", request.getRequestURI(), e.getResourcePath());
        return Result.error(404, "接口不存在");
    }
}

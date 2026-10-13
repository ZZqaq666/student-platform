package com.student.platform.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户实体类
 *
 * 功能说明：表示系统中的用户信息
 *
 * 对应数据库表：user
 *
 * 字段说明：
 * - 认证相关：username、password
 * - 联系信息：email、phone
 * - 显示信息：nickname、avatar
 * - 账户信息：role、status
 * - 时间戳：createdAt、updatedAt
 *
 * 使用场景：
 * - 用户登录注册
 * - 个人信息管理
 * - 权限控制
 *
 * @author student-platform
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user")
public class User {

    /**
     * 用户ID
     *
     * 主键，自增生成
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     *
     * 用于登录系统，唯一标识
     * 长度：4-20个字符
     * 格式：字母、数字、下划线
     */
    @TableField("username")
    private String username;

    /**
     * 密码
     *
     * BCrypt加密存储
     * 长度：6-20个字符
     * 要求：至少包含一个字母和一个数字
     */
    @TableField("password")
    private String password;

    /**
     * 邮箱
     *
     * 用于找回密码和接收通知
     * 格式：有效的邮箱地址
     */
    @TableField("email")
    private String email;

    /**
     * 手机号
     *
     * 可选字段
     * 格式：11位手机号码
     */
    @TableField("phone")
    private String phone;

    /**
     * 昵称
     *
     * 显示名称，可自定义
     */
    @TableField("nickname")
    private String nickname;

    /**
     * 头像URL
     *
     * 存储在阿里云OSS
     */
    @TableField("avatar")
    private String avatar;

    /**
     * 角色
     *
     * 取值：admin（管理员）、teacher（教师）、student（学生）
     */
    @TableField("role")
    private String role;

    /**
     * 账号状态
     *
     * 取值：active（正常）、disabled（禁用）
     */
    @TableField("status")
    private String status;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

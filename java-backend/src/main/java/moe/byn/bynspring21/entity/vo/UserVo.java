package moe.byn.bynspring21.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

public class UserVo {
    @Data
    @Builder
    @Schema(description = "用户注册请求参数")
    public static class RegisterUserVo {
        @Schema(description = "用户名", example = "john_doe", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String username;

        @Schema(description = "密码（至少8位）", example = "password123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String password;

        @Schema(description = "邮箱地址", example = "john@example.com")
        private String email;

        @NotBlank
        private String verifyCodeId;

        @NotBlank
        private String verifyCode;
    }

    @Data
    @Builder
    @Schema(description = "用户登录请求参数")
    public static class LoginUserVo {
        @Schema(description = "用户名或邮箱", example = "john_doe", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String username;

        @Schema(description = "密码", example = "password123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String password;

        @NotBlank
        private String verifyCodeId;

        @NotBlank
        private String verifyCode;
    }

    @Data
    @Builder
    @Schema(description = "邮箱验证码验证请求参数")
    public static class VerifyEmailCodeVo {
        @Schema(description = "用户ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        private Long userId;

        @Schema(description = "6位验证码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String code;
    }

    @Data
    @Builder
    @Schema(description = "修改密码请求参数")
    public static class ChangePasswordVo {
        @Schema(description = "新密码（至少8位）", example = "newPassword123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String newPassword;
    }

    @Data
    @Builder
    @Schema(description = "查看用户日志请求参数")
    public static class ViewUserLogVo {
        @Schema(description = "页码", example = "1", defaultValue = "1")
        private Integer pageNum = 1;

        @Schema(description = "每页大小", example = "10", defaultValue = "10")
        private Integer pageSize = 10;
    }

    @Data
    @Builder
    @Schema(description = "修改用户信息请求参数")
    public static class EditUserInfoVo {
        @Schema(description = "用户显示名称", example = "John Doe")
        private String displayName;
    }

    @Data
    @Builder
    @Schema(description = "刷新Token请求参数")
    public static class RefreshTokenVo {
        @Schema(description = "刷新令牌", example = "eyJhbGciOiJIUzM4NCJ9...", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "refreshToken 不能为空")
        private String refreshToken;
    }

    @Data
    @Builder
    @Schema(description = "更新头像响应")
    public static class UpdateAvatarResponse {
        @Schema(description = "头像对象路径", example = "avatars/123456/uuid.jpg")
        private String avatarUrl;
    }

    @Data
    @Builder
    @Schema(description = "修改登录邮箱请求参数")
    public static class ChangeLoginEmailVo {
        @Schema(description = "登录信息", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        private LoginUserVo loginInfo;

        @Schema(description = "新邮箱地址", example = "newemail@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String newEmail;
    }

    @Data
    @Builder
    @Schema(description = "忘记密码请求参数")
    public static class ForgetPasswordVo {
        @Schema(description = "用户名或邮箱", example = "john_doe", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String username;
    }

    @Data
    @Builder
    @Schema(description = "重置密码请求参数")
    public static class ResetPasswordVo {
        @Schema(description = "用户ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        private Long userId;

        @Schema(description = "6位验证码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String code;

        @Schema(description = "新密码（至少8位）", example = "newPassword123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String newPassword;
    }

    @Data
    @Builder
    @Schema(description = "绑定邮箱请求参数")
    public static class BindEmailVo {
        @Schema(description = "邮箱地址", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String email;
    }

    @Data
    @Builder
    @Schema(description = "验证绑定邮箱请求参数")
    public static class BindEmailVerifyVo {
        @Schema(description = "6位验证码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String emailCode;
    }

    @Data
    @Builder
    @Schema(description = "注销指定会话请求参数")
    public static class RevokeSessionVo {
        @Schema(description = "会话ID", example = "1234567890", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String sessionId;
    }

    @Data
    @Builder
    @Schema(description = "用户登出请求参数")
    public static class LogoutVo {
        @Schema(description = "刷新令牌", example = "eyJhbGciOiJIUzM4NCJ9...", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "refreshToken 不能为空")
        private String refreshToken;
    }
}

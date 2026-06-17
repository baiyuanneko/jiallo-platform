package moe.byn.bynspring21.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import moe.byn.bynspring21.entity.SsoClient;
import moe.byn.bynspring21.entity.User;

import java.util.Date;
import java.util.List;

public class AdminDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "管理员创建用户响应")
    public static class CreateUserResponse {
        @Schema(description = "用户ID")
        private String userId;

        @Schema(description = "用户名")
        private String username;

        @Schema(description = "初始密码（明文，请妥善保管）")
        private String initialPassword;

        @Schema(description = "是否已发送邮件")
        private Boolean emailSent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "管理员重置用户密码响应")
    public static class AdminResetPasswordResponse {
        @Schema(description = "新密码（明文）")
        private String newPassword;

        @Schema(description = "是否已发送邮件")
        private Boolean emailSent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "用户详细信息（管理员视图）")
    public static class UserDetailResponse {
        @Schema(description = "用户基本信息")
        private User user;

        @Schema(description = "所属用户组列表")
        private List<UserGroupSummary> groups;

        @Schema(description = "活跃会话数量")
        private Integer activeSessionCount;

        @Schema(description = "最近登录时间")
        private Date lastLoginTime;

        @Schema(description = "最近登录IP")
        private String lastLoginIp;

        @Schema(description = "总操作日志数量")
        private Long totalLogCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "用户组概要信息")
    public static class UserGroupSummary {
        @Schema(description = "用户组ID")
        private String groupId;

        @Schema(description = "用户组名称")
        private String groupName;

        @Schema(description = "用户组描述")
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "用户组成员信息")
    public static class GroupMemberResponse {
        @Schema(description = "用户ID")
        private String userId;

        @Schema(description = "用户名")
        private String username;

        @Schema(description = "显示名称")
        private String displayName;
    }

    // ==================== SSO Client 管理响应 DTO ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "管理员创建 SSO Client 响应")
    public static class CreateSsoClientResponse {
        @Schema(description = "客户端ID")
        private String clientId;

        @Schema(description = "客户端唯一名称")
        private String clientUniqueName;

        @Schema(description = "客户端密钥（请妥善保管，仅显示一次）")
        private String clientSecret;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "SSO Client 详情响应")
    public static class SsoClientDetailResponse {
        @Schema(description = "SSO Client 信息")
        private SsoClient ssoClient;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "管理员重置 SSO Client 密钥响应")
    public static class ResetSsoClientSecretResponse {
        @Schema(description = "新的客户端密钥（请妥善保管）")
        private String clientSecret;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "上传SSO客户端图标响应")
    public static class UploadSsoIconResponse {
        @Schema(description = "客户端ID")
        private String clientId;

        @Schema(description = "图标对象路径（MinIO object key）")
        private String iconUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "上传LLM供应商图标响应")
    public static class UploadProviderIconResponse {
        @Schema(description = "供应商ID")
        private String providerId;

        @Schema(description = "图标对象路径（MinIO object key）")
        private String providerIconUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "上传LLM模型图标响应")
    public static class UploadModelIconResponse {
        @Schema(description = "模型ID")
        private String modelId;

        @Schema(description = "图标对象路径（MinIO object key）")
        private String modelIconUrl;
    }
}

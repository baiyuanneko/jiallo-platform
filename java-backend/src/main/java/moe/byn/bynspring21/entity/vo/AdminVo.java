package moe.byn.bynspring21.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import moe.byn.bynspring21.entity.SsoClient;
import moe.byn.bynspring21.entity.User;
import moe.byn.bynspring21.entity.base.RoleType;
import moe.byn.bynspring21.entity.base.SsoClientPermissionType;

import java.util.Date;

public class AdminVo {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "管理员查询用户列表请求参数")
    public static class QueryUsersVo {
        @Schema(description = "页码", example = "1")
        @Builder.Default
        private Integer pageNum = 1;

        @Schema(description = "每页大小", example = "20")
        @Builder.Default
        private Integer pageSize = 20;

        @Schema(description = "搜索关键词（用户名、邮箱、显示名称）", example = "john")
        private String keyword;

        @Schema(description = "角色类型筛选", example = "USER")
        private RoleType roleType;

        @Schema(description = "封禁状态筛选（true=仅封禁，false=仅未封禁，null=全部）")
        private Boolean banned;

        @Schema(description = "邮箱验证状态筛选（true=已验证，false=未验证，null=全部）")
        private Boolean emailVerified;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "管理员创建用户请求参数")
    public static class CreateUserVo {
        @Schema(description = "用户名", example = "john_doe", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "用户名不能为空")
        private String username;

        @Schema(description = "显示名称", example = "John Doe")
        private String displayName;

        @Schema(description = "邮箱地址", example = "john@example.com")
        private String email;

        @Schema(description = "角色类型", example = "USER", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "角色类型不能为空")
        private RoleType roleType;

        @Schema(description = "是否发送邮件通知（包含随机密码）", example = "false")
        @Builder.Default
        private Boolean sendEmail = false;
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "管理员修改用户信息请求参数")
    public static class UpdateUserVo {
        @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "用户ID不能为空")
        private String userId;

        @Schema(description = "显示名称")
        private String displayName;

        @Schema(description = "邮箱地址")
        private String email;

        @Schema(description = "邮箱是否已验证")
        private Boolean emailVerified;

        @Schema(description = "角色类型")
        private RoleType roleType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "管理员重置用户密码请求参数")
    public static class AdminResetPasswordVo {
        @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "用户ID不能为空")
        private String userId;

        @Schema(description = "是否发送邮件通知", example = "false")
        @Builder.Default
        private Boolean sendEmail = false;
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "管理员封禁/解封用户请求参数")
    public static class BanUserVo {
        @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "用户ID不能为空")
        private String userId;

        @Schema(description = "是否封禁（true=封禁，false=解封）", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "封禁状态不能为空")
        private Boolean banned;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "管理员删除用户请求参数")
    public static class DeleteUserVo {
        @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "用户ID不能为空")
        private String userId;
    }


    // ==================== SSO Client 管理相关 VO ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "管理员查询 SSO Client 列表请求参数")
    public static class QuerySsoClientsVo {
        @Schema(description = "页码", example = "1")
        @Builder.Default
        private Integer pageNum = 1;

        @Schema(description = "每页大小", example = "20")
        @Builder.Default
        private Integer pageSize = 20;

        @Schema(description = "搜索关键词（客户端唯一名称、名称、描述）", example = "myapp")
        private String keyword;

        @Schema(description = "权限类型筛选")
        private SsoClientPermissionType permissionType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "管理员创建 SSO Client 请求参数")
    public static class CreateSsoClientVo {
        @Schema(description = "客户端唯一名称（英文，用于URL）", example = "myapp", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "客户端唯一名称不能为空")
        private String clientUniqueName;

        @Schema(description = "客户端显示名称", example = "我的应用", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "客户端名称不能为空")
        private String clientName;

        @Schema(description = "客户端描述", example = "这是我的测试应用")
        private String clientDesc;

        @Schema(description = "客户端官网", example = "https://example.com")
        private String clientWebsite;

        @Schema(description = "客户端作者名称", example = "张三")
        private String clientAuthorName;

        @Schema(description = "重定向URI", example = "https://example.com/callback", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "重定向URI不能为空")
        private String clientRedirectUri;

        @Schema(description = "权限类型", example = "BASIC_INFO", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "权限类型不能为空")
        private SsoClientPermissionType clientPermissionType;

        @Schema(description = "是否静默重定向（不显示授权页面）", example = "false")
        @Builder.Default
        private Boolean silentRedirect = false;
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "管理员修改 SSO Client 请求参数")
    public static class UpdateSsoClientVo {
        @Schema(description = "客户端ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "客户端ID不能为空")
        private String clientId;

        @Schema(description = "客户端显示名称")
        private String clientName;

        @Schema(description = "客户端描述")
        private String clientDesc;

        @Schema(description = "客户端官网")
        private String clientWebsite;

        @Schema(description = "客户端作者名称")
        private String clientAuthorName;

        @Schema(description = "重定向URI")
        private String clientRedirectUri;

        @Schema(description = "权限类型")
        private SsoClientPermissionType clientPermissionType;

        @Schema(description = "是否静默重定向")
        private Boolean silentRedirect;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "管理员删除 SSO Client 请求参数")
    public static class DeleteSsoClientVo {
        @Schema(description = "客户端ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "客户端ID不能为空")
        private String clientId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "管理员重置 SSO Client 密钥请求参数")
    public static class ResetSsoClientSecretVo {
        @Schema(description = "客户端ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "客户端ID不能为空")
        private String clientId;
    }

}

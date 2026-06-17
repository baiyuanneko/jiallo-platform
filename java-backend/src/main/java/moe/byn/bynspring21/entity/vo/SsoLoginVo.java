package moe.byn.bynspring21.entity.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

public class SsoLoginVo {
    @Data
    @Builder
    @Schema(description = "获取sso client信息请求参数")
    public static class GetSsoClientInfoVo {
        @Schema(description = "client的唯一名称", example = "NekoAgent")
        private String clientUniqueName;
    }

    @Data
    @Builder
    @Schema(description = "确认进行sso登录请求参数")
    public static class ConfirmLoginVo {
        @Schema(description = "client的唯一名称", example = "NekoAgent")
        private String clientUniqueName;
    }

    @Data
    @Builder
    @Schema(description = "sso client使用token换取用户信息请求参数")
    public static class ClientGetUserVo {
        @Schema(description = "client的唯一名称", example = "NekoAgent")
        private String clientUniqueName;

        @Schema(description = "token", example = "")
        private String token;

        @Schema(description = "client secret", example = "")
        private String clientSecret;
    }
}

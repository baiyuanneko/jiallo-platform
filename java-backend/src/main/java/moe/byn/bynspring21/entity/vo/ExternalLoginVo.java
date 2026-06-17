package moe.byn.bynspring21.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

public class ExternalLoginVo {
    @Data
    @Builder
    public static class LoginVo {
        @NotNull
        private String ssoToken;
    }
}

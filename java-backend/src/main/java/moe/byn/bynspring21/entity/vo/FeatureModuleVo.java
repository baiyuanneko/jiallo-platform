package moe.byn.bynspring21.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class FeatureModuleVo {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "检查功能模块授权参数")
    public static class CheckAuthVo {

        @Schema(description = "功能模块标识，如 claw_like", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "moduleCode不能为空")
        private String moduleCode;
    }
}

package moe.byn.bynspring21.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "忘记密码响应")
public class ForgetPasswordResult {

    @Schema(description = "用户ID", example = "1")
    private String userId;

    @Schema(description = "邮箱地址（部分隐藏）", example = "j***@example.com")
    private String email;
}

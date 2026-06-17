package moe.byn.bynspring21.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pyodide 代码执行结果提交")
public class PyodideToolResultVo {

    @Schema(description = "请求ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "requestId不能为空")
    private String requestId;

    @Schema(description = "标准输出")
    private String stdout;

    @Schema(description = "标准错误")
    private String stderr;

    @Schema(description = "错误信息")
    private String error;

    @Schema(description = "退出码")
    private Integer exitCode;
}

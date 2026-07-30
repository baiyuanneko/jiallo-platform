package moe.byn.bynspring21.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ClawFileVo {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Claw-like 工作区文件重命名参数")
    public static class RenameWorkspaceFileVo {

        @Schema(description = "会话ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "sessionId不能为空")
        private String sessionId;

        @Schema(description = "目标文件或目录的工作区相对路径", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "path不能为空")
        private String path;

        @Schema(description = "新名称（不允许包含 /、\\、..）", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "newName不能为空")
        private String newName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Claw-like 工作区文件删除参数")
    public static class DeleteWorkspaceFileVo {

        @Schema(description = "会话ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "sessionId不能为空")
        private String sessionId;

        @Schema(description = "目标文件或目录的工作区相对路径", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "path不能为空")
        private String path;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Claw-like 服务器文件重命名参数")
    public static class RenameServerFileVo {

        @Schema(description = "目标文件或目录的绝对路径", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "path不能为空")
        private String path;

        @Schema(description = "新名称（不允许包含 /、\\、..）", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "newName不能为空")
        private String newName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Claw-like 服务器文件删除参数")
    public static class DeleteServerFileVo {

        @Schema(description = "目标文件或目录的绝对路径", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "path不能为空")
        private String path;
    }
}

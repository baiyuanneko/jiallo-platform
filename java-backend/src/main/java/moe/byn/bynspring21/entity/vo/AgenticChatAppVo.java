package moe.byn.bynspring21.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import moe.byn.bynspring21.entity.base.AgenticToolEnum;

import java.util.List;

public class AgenticChatAppVo {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "聊天流式请求参数")
    public static class ChatStreamVo {

        @Schema(description = "会话ID，为空表示新建对话")
        private String sessionId;

        @Schema(description = "消息文本内容", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "消息内容不能为空")
        private String textContent;

        @Schema(description = "媒体内容列表，每项为Base64编码（支持 data URL 或纯 base64）")
        private List<String> mediaContentBase64List;

        @Schema(description = "Agent类型：0为chatClient、1为reactAgent、2为digitalByn", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "agentType不能为空")
        private Integer agentType;

        @Schema(description = "模型类型：0为sysModel、1为userModel", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "modelType不能为空")
        private Integer modelType;

        @Schema(description = "模型ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "模型ID不能为空")
        private String modelId;

        @Schema(description = "启用的工具列表")
        private List<AgenticToolEnum> enabledAgenticTools;

        @Schema(description = "传递给大模型的 reasoning_effort 参数")
        private String reasoningEffort;

        @Schema(description = "RAG 知识库ID列表")
        private List<String> ragLibraryIds;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "分页查询会话列表参数")
    public static class PageSessionsVo {

        @Schema(description = "页码，默认1")
        private Integer pageNum = 1;

        @Schema(description = "每页条数，默认20")
        private Integer pageSize = 20;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "编辑消息参数")
    public static class EditMessageVo {

        @Schema(description = "消息ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "消息ID不能为空")
        private String messageId;

        @Schema(description = "消息文本内容", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "消息内容不能为空")
        private String textContent;

        @Schema(description = "是否删除后续消息", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "deleteAfterMsgs不能为空")
        private Boolean deleteAfterMsgs;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "删除消息参数")
    public static class DeleteMessageVo {

        @Schema(description = "消息ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "消息ID不能为空")
        private String messageId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "编辑会话名称参数")
    public static class EditSessionNameVo {

        @Schema(description = "会话ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "会话ID不能为空")
        private String sessionId;

        @Schema(description = "会话名称", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "会话名称不能为空")
        private String sessionName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "删除会话参数")
    public static class DeleteSessionVo {

        @Schema(description = "会话ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "会话ID不能为空")
        private String sessionId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "分享/取消分享/重新分享会话参数")
    public static class ShareSessionVo {

        @Schema(description = "会话ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "会话ID不能为空")
        private String sessionId;

        @Schema(description = "是否仅分享文本内容（默认true：过滤推理过程和工具结果）")
        private Boolean shareTextContentOnly;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "取消聊天流参数")
    public static class StopChatStreamVo {

        @Schema(description = "流唯一标识，由 chatStream 接口通过 SSE streamUniqueKey 事件返回", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "streamUniqueKey不能为空")
        private String streamUniqueKey;
    }
}

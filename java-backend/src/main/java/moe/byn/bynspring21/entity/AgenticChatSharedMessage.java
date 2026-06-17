package moe.byn.bynspring21.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import moe.byn.bynspring21.entity.base.CommonEntity;

import java.io.Serial;
import java.io.Serializable;

@Data
@TableName("agentic_chat_shared_message")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgenticChatSharedMessage extends CommonEntity<AgenticChatSharedMessage> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String sessionId;
    private Integer messageIndex;
    /**
     * 0为system，1为user，2为assistant，3为toolResponse
     */
    private Integer messageType;
    private String textContent;
    /**
     * 媒体内容（JSON 数组）。格式：[{"mediaType":0,"mediaContentUrl":"minioKey"}, ...]
     */
    private String mediaContent;
    private Long promptTokenCount;
    private Long completionTokenCount;
    private Long cachedTokenCount;
    private Long reasoningTokenCount;
    /**
     * 工具内容（JSON）。messageType=2时存ToolCall列表，messageType=3时存ToolResponse列表
     */
    private String toolContent;
    /**
     * 推理/思考内容（reasoning tokens）。仅 OpenAI 模型会产生此内容。
     */
    private String reasoningContent;
    /**
     * 结构化工具内容（JSON）。messageType=3 时存储结构化的工具结果（如搜索卡片数据），
     * 用于前端渲染。toolContent 仍存储传给 AI 的纯文本版本。
     * JsonIgnore：不直接返回前端，由业务层决定返回哪个版本。
     */
    @JsonIgnore
    private String structuredToolContent;
}

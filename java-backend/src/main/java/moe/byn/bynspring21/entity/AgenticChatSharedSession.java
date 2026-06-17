package moe.byn.bynspring21.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import moe.byn.bynspring21.entity.base.CommonEntity;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@TableName("agentic_chat_shared_session")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgenticChatSharedSession extends CommonEntity<AgenticChatSharedSession> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String sessionName;
    private String modelId;
    /**
     * 0为sysModel
     */
    private Integer modelType;
    /**
     * 0为chatClient、1为reactAgent、2为digitalByn
     */
    private Integer agentType;
    /**
     * 原始会话ID
     */
    private String originalSessionId;
    /**
     * 是否仅分享文本内容。
     * null 或 true：过滤 reasoningContent / toolContent / structuredToolContent
     * false：分享完整内容
     */
    private Boolean shareTextContentOnly;

    @TableField(exist = false)
    private SysLlmModel model;

    @TableField(exist = false)
    private List<AgenticChatSharedMessage> messages;
}

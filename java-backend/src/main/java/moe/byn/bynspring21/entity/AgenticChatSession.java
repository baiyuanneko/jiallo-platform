package moe.byn.bynspring21.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import moe.byn.bynspring21.entity.base.AgenticToolEnum;
import moe.byn.bynspring21.entity.base.CommonEntity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@TableName("agentic_chat_session")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgenticChatSession extends CommonEntity<AgenticChatSession> implements Serializable {

    @Serial
    private static final long serialVersionUID = 2313183098518620206L;

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

    @TableField(exist = false)
    private List<AgenticChatMessage> messages;

    @TableField(exist = false)
    private List<AgenticToolEnum> enabledTools;

    @TableField(exist = false)
    private Boolean isShared;

    @TableField(exist = false)
    private String sharedSessionId;

    @TableField(exist = false)
    private Date sharedSessionCreateTime;

    @TableField(exist = false)
    private Boolean shareTextContentOnly;
}

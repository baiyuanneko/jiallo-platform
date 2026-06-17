package moe.byn.bynspring21.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import moe.byn.bynspring21.entity.base.CommonEntity;
import moe.byn.bynspring21.entity.base.AgenticToolEnum;

import java.io.Serial;
import java.io.Serializable;

@Data
@TableName("chatsess_enabled_tool")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatsessEnabledTool extends CommonEntity<ChatsessEnabledTool> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String sessionId;

    private AgenticToolEnum toolCode;
}

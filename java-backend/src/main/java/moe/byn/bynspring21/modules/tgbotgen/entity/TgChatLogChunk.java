package moe.byn.bynspring21.modules.tgbotgen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * Telegram 聊天记录全文检索分块
 */
@Data
@TableName("tg_chat_log_chunk")
public class TgChatLogChunk {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 对应 tg_chat_log.id */
    private String logId;

    private Integer chunkIndex;

    private String chunkContent;
}

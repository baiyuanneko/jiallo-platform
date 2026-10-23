package moe.byn.bynspring21.modules.tgbotgen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Telegram 聊天记录（只增不改，不继承 CommonEntity；create_time 由数据库默认值填充）
 */
@Data
@TableName("tg_chat_log")
public class TgChatLog {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** private/group/supergroup 等 */
    private String chatType;

    private Long chatId;

    /** 匿名/频道代发/机器人自身回复时为空 */
    private Long senderId;

    private String senderName;

    @TableField("sender_is_bot")
    private Boolean senderIsBot;

    /** 消息发送时间（Asia/Shanghai） */
    private LocalDateTime sendTime;

    /** 原始消息内容（文本或媒体占位符），特殊行为 [event]/[edited] 标记 */
    private String content;

    /** 1=服务事件或编辑消息 */
    @TableField("is_special_message")
    private Boolean specialMessage;

    /** 特殊消息描述：事件文案或编辑后的新内容 */
    private String eventContent;

    private Long messageId;
}

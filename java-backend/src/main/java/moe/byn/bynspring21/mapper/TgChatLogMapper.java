package moe.byn.bynspring21.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lombok.Data;
import moe.byn.bynspring21.modules.tgbotgen.entity.TgChatLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface TgChatLogMapper extends BaseMapper<TgChatLog> {

    /**
     * 群活跃成员排行项：昵称取该成员在本群最新一条消息的昵称
     */
    @Data
    class RankItem {
        private Long senderId;
        private String senderName;
        private Long messageCount;
    }

    /**
     * 统计指定聊天内发言条数排行：排除机器人、匿名/频道代发（sender_id 为空）及特殊消息（事件/编辑）
     *
     * @param since 统计起始时间，为空表示全部记录范围
     */
    @Select("""
            <script>
            SELECT t.sender_id AS senderId,
                   (SELECT l2.sender_name FROM tg_chat_log l2
                    WHERE l2.chat_id = #{chatId} AND l2.sender_id = t.sender_id
                    ORDER BY l2.send_time DESC LIMIT 1) AS senderName,
                   t.cnt AS messageCount
            FROM (
                SELECT sender_id, COUNT(*) AS cnt
                FROM tg_chat_log
                WHERE chat_id = #{chatId}
                  AND sender_id IS NOT NULL
                  AND sender_is_bot = 0
                  AND is_special_message = 0
                  <if test="since != null">
                    AND send_time &gt;= #{since}
                  </if>
                GROUP BY sender_id
                ORDER BY cnt DESC
                LIMIT #{limit}
            ) t
            ORDER BY t.cnt DESC
            </script>
            """)
    List<RankItem> rankSenders(@Param("chatId") Long chatId,
                               @Param("since") LocalDateTime since,
                               @Param("limit") int limit);
}

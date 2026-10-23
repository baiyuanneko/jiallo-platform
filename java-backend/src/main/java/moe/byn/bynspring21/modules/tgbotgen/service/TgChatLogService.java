package moe.byn.bynspring21.modules.tgbotgen.service;

import lombok.RequiredArgsConstructor;
import moe.byn.bynspring21.mapper.TgChatLogChunkMapper;
import moe.byn.bynspring21.mapper.TgChatLogMapper;
import moe.byn.bynspring21.modules.tgbotgen.entity.TgChatLog;
import moe.byn.bynspring21.modules.tgbotgen.entity.TgChatLogChunk;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Telegram 聊天记录：写入主表并按固定长度分块写入全文检索表；提供群活跃成员排行统计
 */
@Service
@RequiredArgsConstructor
public class TgChatLogService {

    private static final int CHUNK_SIZE = 1280;

    private final TgChatLogMapper chatLogMapper;

    private final TgChatLogChunkMapper chatLogChunkMapper;

    /**
     * 记录一条消息（主表 + 分块），事务保证两者同时成功或回滚。
     * 特殊消息（事件/编辑）的可检索文本为 eventContent
     */
    @Transactional(rollbackFor = Exception.class)
    public void record(TgChatLog log) {
        chatLogMapper.insert(log);
        String searchable = Boolean.TRUE.equals(log.getSpecialMessage()) ? log.getEventContent() : log.getContent();
        List<String> chunks = split(searchable);
        for (int i = 0; i < chunks.size(); i++) {
            TgChatLogChunk chunk = new TgChatLogChunk();
            chunk.setLogId(log.getId());
            chunk.setChunkIndex(i);
            chunk.setChunkContent(chunks.get(i));
            chatLogChunkMapper.insert(chunk);
        }
    }

    /**
     * 群活跃成员排行
     *
     * @param since 统计起始时间，为空表示全部记录范围
     */
    public List<TgChatLogMapper.RankItem> rank(Long chatId, LocalDateTime since, int limit) {
        return chatLogMapper.rankSenders(chatId, since, limit);
    }

    /**
     * 按固定长度硬切分；空内容兜底为单个 "[empty]" 块
     */
    private static List<String> split(String content) {
        if (content == null || content.isBlank()) {
            return List.of("[empty]");
        }
        List<String> chunks = new ArrayList<>();
        for (int start = 0; start < content.length(); start += CHUNK_SIZE) {
            chunks.add(content.substring(start, Math.min(start + CHUNK_SIZE, content.length())));
        }
        return chunks;
    }
}

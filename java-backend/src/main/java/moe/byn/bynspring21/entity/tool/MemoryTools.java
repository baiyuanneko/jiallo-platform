package moe.byn.bynspring21.entity.tool;

import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.entity.UserMemory;
import moe.byn.bynspring21.entity.base.MemoryStorageType;
import moe.byn.bynspring21.service.UserMemoryService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MemoryTools {

    private static final int MAX_CONTENT_LENGTH = 8192;

    private static final String OVERFLOW_ERROR =
            "当前记忆区总字符数超过8192个字符，请精简后通过overwriteMemory工具重新覆写精简后的完整记忆。" +
                    "并建议你在征求用户的同意，把当前的记忆概要展示给用户查看，让用户选择删减或精简哪些记忆。";

    private static final ThreadLocal<Integer> agentTypeHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> userIdHolder = new ThreadLocal<>();

    @Autowired
    private UserMemoryService userMemoryService;

    public static void setContext(String userId, Integer agentType) {
        userIdHolder.set(userId);
        agentTypeHolder.set(agentType);
    }

    public static void clearContext() {
        userIdHolder.remove();
        agentTypeHolder.remove();
    }

    private MemoryStorageType determineStorageType() {
        Integer agentType = agentTypeHolder.get();
        return (agentType != null && agentType == 2)
                ? MemoryStorageType.BYN_DIGITAL_CLONE
                : MemoryStorageType.DEFAULT;
    }

    @Tool(description = "读取你对当前用户的长期记忆。返回你对当前用户之前存储的长期记忆信息，如果没有长期记忆则返回提示。"
            + "在每次回答用户前**必须**调用本工具进行读取你对当前用户之前存储的长期记忆内容。若会话中已经调用过一次，可以不用调用。")
    public String readMemory() {
        try {
            String userId = userIdHolder.get();
            MemoryStorageType storageType = determineStorageType();
            UserMemory memory = userMemoryService.findOrCreate(userId, storageType);
            String content = memory.getContent();
            if (content == null || content.isBlank()) {
                return "当前记忆区暂无记忆内容";
            }
            return content;
        } catch (Exception e) {
            log.error("readMemory 执行失败", e);
            return "读取记忆失败，请稍后重试";
        }
    }

    @Tool(description = "完全覆写你对当前用户的长期记忆内容。将替换之前存储的所有长期记忆。content参数为要存储的新长期记忆内容，最多支持8192个字符。"
            + "必须在执行overwriteMemory前立刻执行一次readMemory以获取最新长期记忆。"
            + "请知悉本工具是用于覆盖（force overwrite）你对用户的全部长期记忆，旧的记忆将在此操作后被清除，请谨慎操作。")
    public String overwriteMemory(
            @ToolParam(description = "要存储的新长期记忆内容，最多8192个字符", required = true) String content) {
        try {
            if (content != null && content.length() > MAX_CONTENT_LENGTH) {
                return OVERFLOW_ERROR;
            }
            String userId = userIdHolder.get();
            MemoryStorageType storageType = determineStorageType();
            UserMemory memory = userMemoryService.findOrCreate(userId, storageType);
            memory.setContent(content);
            memory.setUpdateUser(userId);
            userMemoryService.updateById(memory);
            return "记忆已成功覆写";
        } catch (Exception e) {
            log.error("overwriteMemory 执行失败", e);
            return "覆写记忆失败，请稍后重试";
        }
    }

    @Tool(description = "在你对当前用户的长期记忆末尾追加新的内容。不会覆盖已有记忆。content参数为要追加的长期记忆内容，追加后的总长度不能超过8192个字符。"
            + "在每次回答用户前，**必须至少一次**调用此工具进行添加你对用户的新长期记忆。若尚未执行readMemory，必须读取一次再进行记忆。")
    public String appendMemory(
            @ToolParam(description = "要追加的记忆内容，追加后总长度不超过8192个字符", required = true) String content) {
        try {
            String userId = userIdHolder.get();
            MemoryStorageType storageType = determineStorageType();
            UserMemory memory = userMemoryService.findOrCreate(userId, storageType);
            String existing = memory.getContent();
            String newContent = (existing == null || existing.isBlank())
                    ? content
                    : existing + "\n" + content;
            if (newContent.length() > MAX_CONTENT_LENGTH) {
                return OVERFLOW_ERROR;
            }
            memory.setContent(newContent);
            memory.setUpdateUser(userId);
            userMemoryService.updateById(memory);
            return "记忆已成功追加";
        } catch (Exception e) {
            log.error("appendMemory 执行失败", e);
            return "追加记忆失败，请稍后重试";
        }
    }
}

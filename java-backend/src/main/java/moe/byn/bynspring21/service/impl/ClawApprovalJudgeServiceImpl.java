package moe.byn.bynspring21.service.impl;

import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.entity.base.ClawSecurityLevel;
import moe.byn.bynspring21.entity.tool.ClawToolContext;
import moe.byn.bynspring21.service.ClawApprovalJudgeService;
import moe.byn.bynspring21.service.SysConfigService;
import moe.byn.bynspring21.service.SysLlmModelService;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * Claw-like 工具调用危险性评判：使用系统配置的审批模型（clawLikeApprovalModelId）判断，
 * 未配置、调用异常或输出不可解析时一律按危险处理（降级人工确认）
 */
@Slf4j
@Service
public class ClawApprovalJudgeServiceImpl implements ClawApprovalJudgeService {

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private SysLlmModelService sysLlmModelService;

    @Override
    public boolean isDangerous(String toolName, String arguments) {
        String modelId = sysConfigService.getConfigValue("clawLikeApprovalModelId");
        if (!StringUtils.hasText(modelId)) {
            // 未配置审批模型：按危险处理，全部降级人工确认
            return true;
        }
        try {
            ChatModel judgeModel = sysLlmModelService.createChatModelByModelId(modelId);
            String levelDesc = "未知";
            Integer level = ClawToolContext.getLevel();
            if (level != null) {
                ClawSecurityLevel securityLevel = ClawSecurityLevel.fromCode(level);
                if (securityLevel != null) {
                    levelDesc = securityLevel.getDesc();
                }
            }
            String promptText = "你是 AI 工具调用的安全审批员。请判断下面这次工具调用是否具有危险性"
                    + "（例如删除或覆盖数据、执行有副作用的 shell 命令、修改系统配置、访问敏感文件等）。\n"
                    + "当前安全级别：" + levelDesc + "\n"
                    + "工具名称：" + toolName + "\n"
                    + "工具参数：" + arguments + "\n"
                    + "只输出一个词：安全输出 SAFE，危险输出 DANGEROUS，不要输出任何其他内容。";
            ChatResponse response = judgeModel.call(new Prompt(List.of(new UserMessage(promptText))));
            String output = response.getResult().getOutput().getText();
            if (output == null) {
                return true;
            }
            // 精确判断：剥离首尾标点/引号后仅 "SAFE" 视为安全，其余一切（含 UNSAFE、NOT SAFE）按危险处理
            String normalized = output.trim().toUpperCase(Locale.ROOT)
                    .replaceAll("^[\\p{Punct}“”‘’]+|[\\p{Punct}“”‘’]+$", "");
            if ("SAFE".equals(normalized)) {
                return false;
            }
            if (!"DANGEROUS".equals(normalized)) {
                log.warn("Claw 审批模型输出不可解析，按危险处理: toolName={}, output={}", toolName, output);
            }
            return true;
        } catch (Exception e) {
            log.warn("Claw 审批模型调用失败，按危险处理: toolName={}, error={}", toolName, e.getMessage());
            return true;
        }
    }
}

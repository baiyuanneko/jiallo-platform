package moe.byn.bynspring21.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.entity.AgenticChatMessage;
import moe.byn.bynspring21.entity.AgenticChatSharedMessage;
import moe.byn.bynspring21.entity.AgenticChatSharedSession;
import moe.byn.bynspring21.entity.SysLlmModel;
import moe.byn.bynspring21.entity.User;
import moe.byn.bynspring21.entity.AgenticChatSession;
import moe.byn.bynspring21.entity.base.AgenticToolEnum;
import moe.byn.bynspring21.entity.tool.BochaWebSearch;
import moe.byn.bynspring21.entity.tool.MemoryTools;
import moe.byn.bynspring21.entity.tool.RagKnowledgeBaseTool;
import moe.byn.bynspring21.entity.tool.ObservableToolCallingManager;
import moe.byn.bynspring21.entity.tool.PyodideCodeRunner;
import moe.byn.bynspring21.service.FeatureModuleAvailabilityService;
import moe.byn.bynspring21.entity.tool.PyodideResultPendingManager;
import moe.byn.bynspring21.entity.tool.ToolCallCollector;
import moe.byn.bynspring21.entity.vo.AgenticChatAppVo;
import moe.byn.bynspring21.entity.vo.PyodideToolResultVo;
import moe.byn.bynspring21.exception.BynBaseException;
import moe.byn.bynspring21.exception.ChatStreamCancelledException;
import moe.byn.bynspring21.service.AgenticChatAppService;
import moe.byn.bynspring21.service.AgenticChatMessageService;
import moe.byn.bynspring21.service.AgenticChatSharedMessageService;
import moe.byn.bynspring21.service.AgenticChatSharedSessionService;
import moe.byn.bynspring21.service.AgentTypeAvailabilityService;
import moe.byn.bynspring21.service.AgentTypeGroupAvailabilityService;
import moe.byn.bynspring21.service.AgenticChatSessionService;
import moe.byn.bynspring21.service.ChatsessEnabledToolService;
import moe.byn.bynspring21.security.SecurityUtil;
import moe.byn.bynspring21.service.MinioService;
import moe.byn.bynspring21.service.SysConfigService;
import moe.byn.bynspring21.service.SysLlmModelService;
import moe.byn.bynspring21.service.UserGroupMemberService;
import moe.byn.bynspring21.utils.AgenticToolResolver;
import moe.byn.bynspring21.utils.MimeUtils;
import moe.byn.bynspring21.utils.ReasoningContentHelper;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.resolution.StaticToolCallbackResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.UUID;

@Slf4j
@Service
public class AgenticChatAppServiceImpl implements AgenticChatAppService {

    private static final String[] DIGITAL_BYN_MODEL_PRIORITY = {
            "minimax/minimax-m2.7",
            "minimax-m2.7"
    };

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, AtomicBoolean>> cancellationFlags = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, CompletableFuture<Map<String, String>>> streamStopResults = new ConcurrentHashMap<>();

    private static final int SESSION_MESSAGE_LOCK_STRIPES = 256;

    private final Object[] sessionMessageLocks = new Object[SESSION_MESSAGE_LOCK_STRIPES];

    public AgenticChatAppServiceImpl() {
        for (int i = 0; i < sessionMessageLocks.length; i++) {
            sessionMessageLocks[i] = new Object();
        }
    }

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private AgenticChatSessionService agenticChatSessionService;

    @Autowired
    private AgenticChatMessageService agenticChatMessageService;

    @Autowired
    private SysLlmModelService sysLlmModelService;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private MinioService minioService;

    @Autowired
    private ChatsessEnabledToolService chatsessEnabledToolService;

    @Autowired
    private AgenticToolResolver agenticToolResolver;

    @Autowired
    private RagKnowledgeBaseTool ragKnowledgeBaseTool;

    @Autowired
    private PyodideResultPendingManager pyodideResultPendingManager;

    @Autowired
    private AgentTypeAvailabilityService agentTypeAvailabilityService;

    @Autowired
    private AgentTypeGroupAvailabilityService agentTypeGroupAvailabilityService;

    @Autowired
    private FeatureModuleAvailabilityService featureModuleAvailabilityService;

    @Autowired
    private UserGroupMemberService userGroupMemberService;

    @Autowired
    private AgenticChatSharedSessionService agenticChatSharedSessionService;

    @Autowired
    private AgenticChatSharedMessageService agenticChatSharedMessageService;

    @Autowired
    @Qualifier("sseStreamingExecutor")
    private ExecutorService sseStreamingExecutor;

    @Autowired
    @Qualifier("sseLightTaskExecutor")
    private ExecutorService sseLightTaskExecutor;

    @Override
    public SseEmitter chatStream(AgenticChatAppVo.ChatStreamVo vo) {
        SseEmitter emitter = new SseEmitter(900_000L);
        String userId = securityUtil.getUserId();

        // agentType 权限检查：在主线程中执行，确保有 SecurityContext
        User currentUser = securityUtil.getUser();
        List<String> userGroupIds = userGroupMemberService.getUserGroupIds(currentUser.getId());
        Set<Integer> availableAgentTypes = new HashSet<>(agentTypeAvailabilityService.getAvailableAgentTypes(currentUser.getRoleType()));
        if (!userGroupIds.isEmpty()) {
            availableAgentTypes.addAll(agentTypeGroupAvailabilityService.getAvailableAgentTypesForGroups(userGroupIds));
        }
        if (!availableAgentTypes.contains(vo.getAgentType())) {
            try {
                emitter.send(SseEmitter.event().name("error").data("当前角色无权使用 agentType=" + vo.getAgentType()));
                emitter.complete();
            } catch (IOException ignored) {}
            return emitter;
        }

        // 检查 RAG 知识库工具权限
        if (vo.getEnabledAgenticTools() != null
                && vo.getEnabledAgenticTools().contains(AgenticToolEnum.RAG_KNOWLEDGE_BASE)) {
            if (!featureModuleAvailabilityService.isModuleAvailable("rag_knowledge_base", currentUser.getRoleType(), userGroupIds)) {
                try {
                    emitter.send(SseEmitter.event().name("error").data("当前角色无权使用 RAG 知识库工具"));
                    emitter.complete();
                } catch (IOException ignored) {}
                return emitter;
            }
        }

        // agentType==2 时在主线程解析模型，避免异步线程拿不到 SecurityContext
        final String resolvedModelId;
        if (Integer.valueOf(2).equals(vo.getAgentType())) {
            List<SysLlmModel> availableModels = sysLlmModelService.listAvailableModelsByRole(currentUser.getRoleType(), userGroupIds);
            String matchedId = null;
            for (String candidate : DIGITAL_BYN_MODEL_PRIORITY) {
                matchedId = availableModels.stream()
                        .filter(m -> candidate.equals(m.getModelName()))
                        .map(SysLlmModel::getId)
                        .findFirst()
                        .orElse(null);
                if (matchedId != null) break;
            }
            if (matchedId == null) {
                try {
                    emitter.send(SseEmitter.event().name("error").data("未分配byn数字分身要求的模型"));
                    emitter.complete();
                } catch (IOException ignored) {}
                return emitter;
            }
            resolvedModelId = matchedId;
        } else {
            resolvedModelId = vo.getModelId();
        }

        assertModelAccessible(resolvedModelId, currentUser, userGroupIds);

        // 生成流唯一标识符，用于前端取消指定流
        String streamUniqueKey = UUID.randomUUID().toString();

        // 检查并发流数限制（每用户最多3个）
        ConcurrentHashMap<String, AtomicBoolean> userFlags;
        AtomicBoolean cancelFlag = new AtomicBoolean(false);
        synchronized (cancellationFlags) {
            userFlags = cancellationFlags.computeIfAbsent(userId, k -> new ConcurrentHashMap<>());
            if (userFlags.size() >= 3) {
                try {
                    emitter.send(SseEmitter.event().name("error").data("并发流数已达上限（最多3个），请先取消其他流"));
                    emitter.complete();
                } catch (IOException ignored) {}
                return emitter;
            }
            userFlags.put(streamUniqueKey, cancelFlag);
        }

        // 立即发送 streamUniqueKey，让前端能够在后续取消此流
        try {
            emitter.send(SseEmitter.event().name("streamUniqueKey").data(streamUniqueKey));
        } catch (IOException e) {
            removeUserStreamFlag(userId, userFlags, streamUniqueKey);
            return emitter;
        }

        try {
            CompletableFuture.runAsync(() -> {
            AgenticChatSession session = null;
            boolean isNewSession = false;
            CompletableFuture<String> titleFuture = null;
            List<AgenticToolEnum> enabledTools = Collections.emptyList();
            ToolCallCollector collector = new ToolCallCollector();
            StringBuilder fullResponse = new StringBuilder();
            StringBuilder fullReasoning = new StringBuilder();
            String finalAnswerText = null;
            long[] promptTokens = {0};
            long[] completionTokens = {0};
            long[] cachedTokens = {0};
            long[] reasoningTokens = {0};

            try {

                if (!Integer.valueOf(0).equals(vo.getModelType())) {
                    throw new BynBaseException("当前仅支持 modelType=0（sysModel）模式");
                }

                List<AgenticChatMessage> historyMessages = new ArrayList<>();

                if (vo.getSessionId() == null || vo.getSessionId().isBlank()) {
                    isNewSession = true;
                    session = AgenticChatSession.builder()
                            .sessionName(vo.getTextContent().length() > 50
                                    ? vo.getTextContent().substring(0, 50)
                                    : vo.getTextContent())
                            .modelId(vo.getModelId())
                            .modelType(vo.getModelType())
                            .agentType(vo.getAgentType())
                            .build();
                    session.setCreateUser(userId);
                    session.setUpdateUser(userId);
                } else {
                    isNewSession = false;
                    session = agenticChatSessionService.getOne(
                            new LambdaQueryWrapper<AgenticChatSession>()
                                    .eq(AgenticChatSession::getId, vo.getSessionId())
                                    .eq(AgenticChatSession::getCreateUser, userId)
                    );
                    if (session == null) {
                        throw new BynBaseException("会话不存在");
                    }

                    // 用户指定了新的 modelId/modelType，更新 session 并持久化
                    boolean sessionUpdated = false;
                    if (vo.getModelId() != null && !vo.getModelId().equals(session.getModelId())) {
                        assertModelAccessible(vo.getModelId(), currentUser, userGroupIds);
                        session.setModelId(vo.getModelId());
                        sessionUpdated = true;
                    }
                    if (vo.getModelType() != null && !vo.getModelType().equals(session.getModelType())) {
                        session.setModelType(vo.getModelType());
                        sessionUpdated = true;
                    }
                    if (sessionUpdated) {
                        session.setUpdateUser(userId);
                        agenticChatSessionService.updateById(session);
                    }

                    historyMessages = agenticChatMessageService.list(
                            new LambdaQueryWrapper<AgenticChatMessage>()
                                    .eq(AgenticChatMessage::getSessionId, vo.getSessionId())
                                    .orderByAsc(AgenticChatMessage::getMessageIndex)
                    );
                }

                List<Message> messages = new ArrayList<>();
                for (AgenticChatMessage historyMsg : historyMessages) {
                    switch (historyMsg.getMessageType()) {
                        case 0 -> messages.add(new SystemMessage(historyMsg.getTextContent()));
                        case 1 -> messages.add(buildUserMessageFromHistory(historyMsg.getTextContent(),
                                historyMsg.getMediaContent()));
                        case 2 -> {
                            Map<String, Object> props = new java.util.HashMap<>();
                            if (historyMsg.getReasoningContent() != null) {
                                props.put("reasoningContent", historyMsg.getReasoningContent());
                            }
                            if (historyMsg.getToolContent() != null) {
                                // 带工具调用的 assistant 消息
                                List<AssistantMessage.ToolCall> toolCalls =
                                        JSON.parseArray(historyMsg.getToolContent(), AssistantMessage.ToolCall.class);
                                messages.add(AssistantMessage.builder()
                                        .content(historyMsg.getTextContent())
                                        .properties(props)
                                        .toolCalls(toolCalls)
                                        .build());
                            } else {
                                // 纯文本 assistant 消息
                                messages.add(AssistantMessage.builder()
                                        .content(historyMsg.getTextContent())
                                        .properties(props)
                                        .build());
                            }
                        }
                        case 3 -> {
                            // 从 toolContent 还原 ToolResponseMessage
                            if (historyMsg.getToolContent() != null) {
                                List<ToolResponseMessage.ToolResponse> responses =
                                        JSON.parseArray(historyMsg.getToolContent(), ToolResponseMessage.ToolResponse.class);
                                messages.add(ToolResponseMessage.builder().responses(responses).build());
                            }
                        }
                        default -> log.warn("未知消息类型: {}, 跳过", historyMsg.getMessageType());
                    }
                }
                messages.add(buildUserMessageFromRequest(vo.getTextContent(),
                        vo.getMediaContentBase64List()));

                // agentType=2 时在最前面插入固定系统提示词
                if (Integer.valueOf(2).equals(vo.getAgentType())) {
                    String digitalBynPrompt = sysConfigService.getConfigValue("digitalBynSystemPrompt");
                    if (digitalBynPrompt != null && !digitalBynPrompt.isBlank()) {
                        messages.add(0, new SystemMessage(digitalBynPrompt));
                    }
                }

                String simpleTaskModelId = sysConfigService.getConfigValue("simpleTaskModelId");
                if (isNewSession && StringUtils.hasText(simpleTaskModelId)) {
                    final String userTextForTitle = vo.getTextContent();
                    titleFuture = CompletableFuture.supplyAsync(() -> generateSessionTitle(userTextForTitle), sseLightTaskExecutor);
                }

                ChatModel chatModel = sysLlmModelService.createChatModelByModelId(resolvedModelId);

                if (isNewSession) {
                    enabledTools = vo.getEnabledAgenticTools() != null ? vo.getEnabledAgenticTools()
                            : Collections.<AgenticToolEnum>emptyList();
                } else {
                    if (vo.getEnabledAgenticTools() != null) {
                        enabledTools = vo.getEnabledAgenticTools();
                        // 用户传入了工具列表，同步更新数据库
                        chatsessEnabledToolService.updateEnabledTools(
                                session.getId(), enabledTools, userId);
                    } else {
                        enabledTools = chatsessEnabledToolService.getEnabledTools(session.getId());
                    }
                }


                if (enabledTools.isEmpty()) {
                    OpenAiChatOptions.Builder streamOptionsBuilder = OpenAiChatOptions.builder()
                            .streamUsage(true);
                    if (StringUtils.hasText(vo.getReasoningEffort())) {
                        streamOptionsBuilder.reasoningEffort(vo.getReasoningEffort());
                    }
                    OpenAiChatOptions streamOptions = streamOptionsBuilder.build();
                    try {
                        ReasoningContentHelper.prepareMessages(messages);
                        Prompt prompt = new Prompt(messages, streamOptions);
                        streamChatAndSend(chatModel, prompt, emitter, fullResponse, fullReasoning,
                                promptTokens, completionTokens, cachedTokens, reasoningTokens, cancelFlag);
                        if (cancelFlag.get()) throw new ChatStreamCancelledException("用户取消");
                    } finally {
                        ReasoningContentHelper.clear();
                    }
                } else {
                    // 手动工具调用循环：ToolCallAdvisor.adviseStream() 在 1.1.2 未实现
                    var toolCallbacks = agenticToolResolver.resolveToolCallbacks(enabledTools);
                    ToolCallingManager observableManager = new ObservableToolCallingManager(
                            ToolCallingManager.builder()
                                    .toolCallbackResolver(new StaticToolCallbackResolver(Arrays.asList(toolCallbacks)))
                                    .build(),
                            emitter,
                            collector
                    );

                    OpenAiChatOptions.Builder toolOptionsBuilder = OpenAiChatOptions.builder()
                            .streamUsage(true);
                    if (StringUtils.hasText(vo.getReasoningEffort())) {
                        toolOptionsBuilder.reasoningEffort(vo.getReasoningEffort());
                    }
                    ToolCallingChatOptions toolOptions = toolOptionsBuilder.build();
                    toolOptions.setToolCallbacks(Arrays.asList(toolCallbacks));
                    toolOptions.setInternalToolExecutionEnabled(false);

                    List<Message> workingMessages = new ArrayList<>(messages);

                    // 设置 Pyodide 工具上下文（SSE emitter + userId）
                    PyodideCodeRunner.setContext(emitter, userId);
                    // 设置 Memory 工具上下文（userId + agentType）
                    MemoryTools.setContext(userId, vo.getAgentType());
                    // 设置 RAG 知识库工具上下文（scope + libraryIds）
                    ragKnowledgeBaseTool.setContext(vo.getRagLibraryIds(), userId);
                    try {
                        for (int iteration = 0; iteration < 50; iteration++) {
                            if (cancelFlag.get()) throw new ChatStreamCancelledException("用户取消");

                            Prompt iterationPrompt = new Prompt(new ArrayList<>(workingMessages), toolOptions);
                            StringBuilder[] outReasoning = {null};

                            // Prepare reasoning_content for DeepSeek thinking mode compatibility
                            ChatResponse aggregated;
                            try {
                                ReasoningContentHelper.prepareMessages(workingMessages);
                                aggregated = aggregateStream(chatModel, iterationPrompt, emitter, fullResponse, outReasoning,
                                        promptTokens, completionTokens, cachedTokens, reasoningTokens, cancelFlag);
                            } finally {
                                ReasoningContentHelper.clear();
                            }

                            if (cancelFlag.get()) throw new ChatStreamCancelledException("用户取消");

                            if (aggregated == null || !aggregated.hasToolCalls()) {
                                // 最终回复
                                if (outReasoning[0] != null) {
                                    fullReasoning.append(outReasoning[0]);
                                }
                                if (aggregated != null) {
                                    workingMessages.add(aggregated.getResult().getOutput());
                                    finalAnswerText = aggregated.getResult().getOutput().getText();
                                }
                                break;
                            }

                            // --- 工具调用迭代 ---
                            AssistantMessage assistantMsg = (AssistantMessage) aggregated.getResult().getOutput();
                            String roundText = assistantMsg.getText();
                            String roundReasoning = outReasoning[0] != null ? outReasoning[0].toString() : null;

                            ToolExecutionResult toolResult = observableManager.executeToolCalls(iterationPrompt, aggregated);
                            collector.setLastRoundTextAndReasoning(roundText, roundReasoning);
                            workingMessages = new ArrayList<>(toolResult.conversationHistory());
                        }
                    } finally {
                        PyodideCodeRunner.clearContext();
                        MemoryTools.clearContext();
                        RagKnowledgeBaseTool.clearContext();
                        BochaWebSearch.clearContext();
                    }
                }

                if (titleFuture != null) {
                    try {
                        String generatedTitle = titleFuture.get(10, TimeUnit.SECONDS);
                        if (generatedTitle != null) {
                            session.setSessionName(generatedTitle);
                        }
                    } catch (TimeoutException e) {
                        log.warn("标题生成超时，使用默认标题");
                        titleFuture.cancel(true);
                    } catch (Exception e) {
                        log.warn("标题生成失败，使用默认标题", e);
                    }
                }

                String responseText;
                if (enabledTools.isEmpty()) {
                    responseText = fullResponse.toString();  // 无工具路径：只有一次迭代，fullResponse 就是最终文本
                } else {
                    responseText = (finalAnswerText != null) ? finalAnswerText : "";  // 工具路径：只用最终答案
                }
                if (cancelFlag.get()) throw new ChatStreamCancelledException("用户取消");
                Map<String, String> savedIds = saveSessionAndMessages(session, vo, responseText,
                        fullReasoning.length() > 0 ? fullReasoning.toString() : null,
                        userId, isNewSession, collector,
                        promptTokens[0], completionTokens[0], cachedTokens[0], reasoningTokens[0]);

                if (isNewSession) {
                    emitter.send(SseEmitter.event().name("sessionTitle").data(
                            JSON.toJSONString(Map.of("sessionId", session.getId(),
                                    "sessionName", session.getSessionName()))));
                }

                emitter.send(SseEmitter.event().name("done").data(
                        JSON.toJSONString(Map.of("sessionId", session.getId(),
                                "userMessageId", savedIds.get("userMessageId"),
                                "messageId", savedIds.get("messageId"),
                                "promptTokenCount", promptTokens[0],
                                "completionTokenCount", completionTokens[0],
                                "cachedTokenCount", cachedTokens[0],
                                "reasoningTokenCount", reasoningTokens[0]))));

                CompletableFuture<Map<String, String>> pendingFuture = streamStopResults.remove(streamUniqueKey);
                if (pendingFuture != null) {
                    pendingFuture.complete(Map.of(
                            "sessionId", session.getId(),
                            "userMessageId", savedIds.get("userMessageId"),
                            "messageId", savedIds.get("messageId"),
                            "promptTokenCount", String.valueOf(promptTokens[0]),
                            "completionTokenCount", String.valueOf(completionTokens[0]),
                            "cachedTokenCount", String.valueOf(cachedTokens[0]),
                            "reasoningTokenCount", String.valueOf(reasoningTokens[0])));
                }

                emitter.complete();

            } catch (ChatStreamCancelledException e) {
                log.info("聊天流被用户取消，保存已有内容, userId={}", userId);
                try {
                    if (isNewSession && titleFuture != null && !titleFuture.isDone()) {
                        titleFuture.cancel(true);
                        session.setSessionName("无标题");
                    }

                    String cancelResponseText;
                    if (enabledTools.isEmpty()) {
                        cancelResponseText = fullResponse.toString();
                    } else {
                        cancelResponseText = (finalAnswerText != null) ? finalAnswerText : fullResponse.toString();
                    }

                    Map<String, String> savedIds = saveSessionAndMessages(session, vo, cancelResponseText,
                            fullReasoning.length() > 0 ? fullReasoning.toString() : null,
                            userId, isNewSession, collector,
                            promptTokens[0], completionTokens[0], cachedTokens[0], reasoningTokens[0]);

                    if (isNewSession) {
                        emitter.send(SseEmitter.event().name("sessionTitle").data(
                                JSON.toJSONString(Map.of("sessionId", session.getId(),
                                        "sessionName", session.getSessionName()))));
                    }

                    emitter.send(SseEmitter.event().name("done").data(
                            JSON.toJSONString(Map.of("sessionId", session.getId(),
                                    "userMessageId", savedIds.get("userMessageId"),
                                    "messageId", savedIds.get("messageId"),
                                    "promptTokenCount", promptTokens[0],
                                    "completionTokenCount", completionTokens[0],
                                    "cachedTokenCount", cachedTokens[0],
                                    "reasoningTokenCount", reasoningTokens[0]))));

                    CompletableFuture<Map<String, String>> pendingFuture = streamStopResults.remove(streamUniqueKey);
                    if (pendingFuture != null) {
                        pendingFuture.complete(Map.of(
                                "sessionId", session.getId(),
                                "userMessageId", savedIds.get("userMessageId"),
                                "messageId", savedIds.get("messageId"),
                                "promptTokenCount", String.valueOf(promptTokens[0]),
                                "completionTokenCount", String.valueOf(completionTokens[0]),
                                "cachedTokenCount", String.valueOf(cachedTokens[0]),
                                "reasoningTokenCount", String.valueOf(reasoningTokens[0])));
                    }
                } catch (Exception ex) {
                    log.error("保存已取消的流数据时出错", ex);
                }
                try { emitter.complete(); } catch (Exception ignored) {}
            } catch (Exception e) {
                log.error("chatStream 处理异常", e);
                try {
                    String errorMsg = e instanceof BynBaseException
                            ? e.getMessage()
                            : extractApiErrorDetail(e);
                    emitter.send(SseEmitter.event().name("error").data(errorMsg));
                } catch (IOException ex) {
                    // 连接已断开，无需处理
                }
                emitter.completeWithError(e);
            } finally {
                removeUserStreamFlag(userId, userFlags, streamUniqueKey);
            }
            }, sseStreamingExecutor);
        } catch (java.util.concurrent.RejectedExecutionException e) {
            removeUserStreamFlag(userId, userFlags, streamUniqueKey);
            try {
                emitter.send(SseEmitter.event().name("error").data("SSE 流式任务队列已满，请稍后重试"));
                emitter.complete();
            } catch (IOException ignored) {}
        }

        return emitter;
    }

    @Override
    public List<Integer> getAvailableAgentTypes() {
        User currentUser = securityUtil.getUser();
        List<String> groupIds = userGroupMemberService.getUserGroupIds(currentUser.getId());
        Set<Integer> availableAgentTypes = new HashSet<>(agentTypeAvailabilityService.getAvailableAgentTypes(currentUser.getRoleType()));
        if (!groupIds.isEmpty()) {
            availableAgentTypes.addAll(agentTypeGroupAvailabilityService.getAvailableAgentTypesForGroups(groupIds));
        }
        return new ArrayList<>(availableAgentTypes);
    }

    @Override
    public Page<AgenticChatSession> pageAgenticChatSessions(AgenticChatAppVo.PageSessionsVo vo) {
        String userId = securityUtil.getUserId();
        Page<AgenticChatSession> page = new Page<>(vo.getPageNum(), vo.getPageSize());
        return agenticChatSessionService.page(page,
                new LambdaQueryWrapper<AgenticChatSession>()
                        .eq(AgenticChatSession::getCreateUser, userId)
                        .orderByDesc(AgenticChatSession::getUpdateTime));
    }

    @Override
    public AgenticChatSession getAgenticChatSession(String sessionId) {
        String userId = securityUtil.getUserId();
        AgenticChatSession session = agenticChatSessionService.getOne(
                new LambdaQueryWrapper<AgenticChatSession>()
                        .eq(AgenticChatSession::getId, sessionId)
                        .eq(AgenticChatSession::getCreateUser, userId)
        );
        if (session == null) {
            throw new BynBaseException("会话不存在");
        }
        session.setMessages(agenticChatMessageService.list(
                new LambdaQueryWrapper<AgenticChatMessage>()
                        .eq(AgenticChatMessage::getSessionId, sessionId)
                        .orderByAsc(AgenticChatMessage::getMessageIndex)
        ).stream().map(msg -> {
            if (Integer.valueOf(3).equals(msg.getMessageType())
                    && msg.getStructuredToolContent() != null) {
                msg.setToolContent(msg.getStructuredToolContent());
            }
            if ((Integer.valueOf(2).equals(msg.getMessageType())
                    || Integer.valueOf(3).equals(msg.getMessageType()))
                    && msg.getToolContent() != null) {
                msg.setToolContent(convertToolNamesInJson(msg.getToolContent()));
            }
            return msg;
        }).toList());
        session.setEnabledTools(chatsessEnabledToolService.getEnabledTools(sessionId));

        AgenticChatSharedSession sharedSession = findSharedSessionByOriginalId(sessionId);
        if (sharedSession != null) {
            session.setIsShared(true);
            session.setSharedSessionId(sharedSession.getId());
            session.setSharedSessionCreateTime(sharedSession.getCreateTime());
            session.setShareTextContentOnly(sharedSession.getShareTextContentOnly());
        } else {
            session.setIsShared(false);
        }

        return session;
    }

    @Override
    public ResponseEntity<byte[]> getMessageMedia(String userId, String mediaFilename) {
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(mediaFilename)) {
            throw new BynBaseException("userId 和 mediaFilename 不能为空");
        }
        if (!userId.matches("\\d+")) {
            throw new BynBaseException("userId 格式不合法");
        }
        if (!mediaFilename.matches("[a-zA-Z0-9\\-.]+")) {
            throw new BynBaseException("mediaFilename 格式不合法");
        }

        String objectKey = "agenticChatApp/mediaContent/" + userId + "/" + mediaFilename;
        String contentType = minioService.getObjectContentType(objectKey);
        byte[] bytes = minioService.getMediaBytes(objectKey);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(bytes);
    }

    private String convertToolNamesInJson(String toolContent) {
        try {
            JSONArray array = JSON.parseArray(toolContent);
            for (int i = 0; i < array.size(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (obj.containsKey("name")) {
                    obj.put("name", AgenticToolEnum.toToolCode(obj.getString("name")));
                }
            }
            return array.toJSONString();
        } catch (Exception e) {
            log.warn("toolContent 工具名转换失败，返回原始内容", e);
            return toolContent;
        }
    }

    private Map<String, String> saveSessionAndMessages(AgenticChatSession session,
                                         AgenticChatAppVo.ChatStreamVo vo,
                                         String assistantResponse,
                                         String reasoningContent,
                                         String userId,
                                         boolean isNewSession,
                                         ToolCallCollector collector,
                                         long promptTokenCount,
                                         long completionTokenCount,
                                         long cachedTokenCount,
                                         long reasoningTokenCount) {
        if (isNewSession) {
            agenticChatSessionService.save(session);
            chatsessEnabledToolService.saveEnabledTools(
                    session.getId(), vo.getEnabledAgenticTools(), userId);
        }

        String sessionId = session.getId();

        synchronized (getSessionMessageLock(sessionId)) {
            return saveMessagesWithLock(sessionId, vo, assistantResponse, reasoningContent, userId, collector,
                    promptTokenCount, completionTokenCount, cachedTokenCount, reasoningTokenCount);
        }
    }

    private Map<String, String> saveMessagesWithLock(String sessionId,
                                         AgenticChatAppVo.ChatStreamVo vo,
                                         String assistantResponse,
                                         String reasoningContent,
                                         String userId,
                                         ToolCallCollector collector,
                                         long promptTokenCount,
                                         long completionTokenCount,
                                         long cachedTokenCount,
                                         long reasoningTokenCount) {
        long maxIndex = agenticChatMessageService.count(
                new LambdaQueryWrapper<AgenticChatMessage>()
                        .eq(AgenticChatMessage::getSessionId, sessionId)
        );

        AgenticChatMessage userMessage = AgenticChatMessage.builder()
                .sessionId(sessionId)
                .messageIndex((int) maxIndex)
                .messageType(1)
                .textContent(vo.getTextContent())
                .build();

        if (vo.getMediaContentBase64List() != null && !vo.getMediaContentBase64List().isEmpty()) {
            List<JSONObject> mediaJsonList = new ArrayList<>();
            for (int i = 0; i < vo.getMediaContentBase64List().size(); i++) {
                String base64Data = vo.getMediaContentBase64List().get(i);
                String objectKey = minioService.uploadChatMedia(base64Data, userId, 0);
                JSONObject mediaJson = new JSONObject();
                mediaJson.put("mediaType", 0);
                mediaJson.put("mediaContentUrl", objectKey);
                mediaJsonList.add(mediaJson);
            }
            userMessage.setMediaContent(JSON.toJSONString(mediaJsonList));
        }

        userMessage.setCreateUser(userId);
        userMessage.setUpdateUser(userId);
        agenticChatMessageService.save(userMessage);

        int nextIndex = (int) maxIndex + 1;

        for (ToolCallCollector.ToolCallRound round : collector.getRounds()) {
            AgenticChatMessage toolCallMsg = AgenticChatMessage.builder()
                    .sessionId(sessionId)
                    .messageIndex(nextIndex++)
                    .messageType(2)
                    .textContent(round.textContent())
                    .toolContent(JSON.toJSONString(round.toolCalls()))
                    .reasoningContent(round.reasoningContent())
                    .build();
            toolCallMsg.setCreateUser(userId);
            toolCallMsg.setUpdateUser(userId);
            agenticChatMessageService.save(toolCallMsg);

            AgenticChatMessage toolResponseMsg = AgenticChatMessage.builder()
                    .sessionId(sessionId)
                    .messageIndex(nextIndex++)
                    .messageType(3)
                    .toolContent(JSON.toJSONString(round.responses()))
                    .structuredToolContent(round.structuredToolContent())
                    .build();
            toolResponseMsg.setCreateUser(userId);
            toolResponseMsg.setUpdateUser(userId);
            agenticChatMessageService.save(toolResponseMsg);
        }

        AgenticChatMessage assistantMessage = AgenticChatMessage.builder()
                .sessionId(sessionId)
                .messageIndex(nextIndex)
                .messageType(2)
                .textContent(assistantResponse)
                .reasoningContent(reasoningContent)
                .promptTokenCount(promptTokenCount > 0 ? promptTokenCount : null)
                .completionTokenCount(completionTokenCount > 0 ? completionTokenCount : null)
                .cachedTokenCount(cachedTokenCount > 0 ? cachedTokenCount : null)
                .reasoningTokenCount(reasoningTokenCount > 0 ? reasoningTokenCount : null)
                .build();
        assistantMessage.setCreateUser(userId);
        assistantMessage.setUpdateUser(userId);
        agenticChatMessageService.save(assistantMessage);

        log.info("会话 {} 消息持久化完成，共 {} 条", sessionId, nextIndex + 1 - (int) maxIndex);
        return Map.of("userMessageId", userMessage.getId(), "messageId", assistantMessage.getId());
    }

    private Object getSessionMessageLock(String sessionId) {
        int index = Math.floorMod(sessionId.hashCode(), sessionMessageLocks.length);
        return sessionMessageLocks[index];
    }

    private void removeUserStreamFlag(String userId,
                                      ConcurrentHashMap<String, AtomicBoolean> userFlags,
                                      String streamUniqueKey) {
        synchronized (cancellationFlags) {
            userFlags.remove(streamUniqueKey);
            if (userFlags.isEmpty()) {
                cancellationFlags.remove(userId, userFlags);
            }
        }
    }

    private void assertModelAccessible(String modelId, User currentUser, List<String> userGroupIds) {
        if (!StringUtils.hasText(modelId)) {
            throw new BynBaseException("模型ID不能为空");
        }

        boolean available = sysLlmModelService.listAvailableModelsByRole(currentUser.getRoleType(), userGroupIds).stream()
                .anyMatch(model -> modelId.equals(model.getId()));
        if (!available) {
            throw new BynBaseException("当前角色/用户组无权使用该模型");
        }
    }

    /**
     * 从请求 VO 构建带多媒体的 UserMessage
     */
    private UserMessage buildUserMessageFromRequest(String textContent, List<String> base64List) {
        if (base64List == null || base64List.isEmpty()) {
            return new UserMessage(textContent);
        }

        List<Media> mediaList = new ArrayList<>();
        for (String base64Data : base64List) {
            byte[] bytes = minioService.getMediaBytes(base64Data);
            var mimeType = MimeUtils.resolveMimeType(base64Data, 0);
            mediaList.add(Media.builder().data(bytes).mimeType(mimeType).build());
        }
        return UserMessage.builder().text(textContent).media(mediaList).build();
    }

    /**
     * 从历史记录构建带多媒体的 UserMessage（mediaContent 为 JSON 数组）
     */
    private UserMessage buildUserMessageFromHistory(String textContent, String mediaContentJson) {
        if (!StringUtils.hasText(mediaContentJson)) {
            return new UserMessage(textContent);
        }

        JSONArray mediaArray = JSON.parseArray(mediaContentJson);
        List<Media> mediaList = new ArrayList<>();
        for (int i = 0; i < mediaArray.size(); i++) {
            JSONObject item = mediaArray.getJSONObject(i);
            String url = item.getString("mediaContentUrl");
            int mediaType = item.getIntValue("mediaType", 0);
            byte[] bytes = minioService.getMediaBytes(url);
            var mimeType = MimeUtils.resolveMimeType(url, mediaType);
            mediaList.add(Media.builder().data(bytes).mimeType(mimeType).build());
        }
        return UserMessage.builder().text(textContent).media(mediaList).build();
    }

    private void streamChatAndSend(ChatModel chatModel, Prompt prompt,
                                    SseEmitter emitter, StringBuilder fullResponse,
                                    StringBuilder reasoningContent,
                                    long[] promptTokens, long[] completionTokens,
                                    long[] cachedTokens, long[] reasoningTokens,
                                    AtomicBoolean cancelFlag) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Throwable[] streamError = {null};
        chatModel.stream(prompt)
                .doOnNext(chatResponse -> {
                    if (cancelFlag.get()) { latch.countDown(); return; }
                    try {
                        var generation = chatResponse.getResult();
                        if (generation == null) return;
                        var message = generation.getOutput();

                        Map<String, Object> metadata = message.getMetadata();
                        if (metadata != null && metadata.containsKey("reasoningContent")) {
                            String reasoning = (String) metadata.get("reasoningContent");
                            if (reasoning != null && !reasoning.isEmpty()) {
                                reasoningContent.append(reasoning);
                                emitter.send(SseEmitter.event().name("reasoning").data(reasoning));
                            }
                        }

                        String text = message.getText();
                        if (text != null && !text.isEmpty()) {
                            fullResponse.append(text);
                            emitter.send(SseEmitter.event().data(text));
                        }

                        Usage usage = chatResponse.getMetadata().getUsage();
                        if (usage != null) {
                            if (usage.getPromptTokens() != null && usage.getPromptTokens() > 0) {
                                promptTokens[0] = usage.getPromptTokens();
                            }
                            if (usage.getCompletionTokens() != null && usage.getCompletionTokens() > 0) {
                                completionTokens[0] = usage.getCompletionTokens();
                            }
                            if (usage.getNativeUsage() instanceof OpenAiApi.Usage openAiUsage) {
                                if (openAiUsage.promptTokensDetails() != null
                                        && openAiUsage.promptTokensDetails().cachedTokens() != null
                                        && openAiUsage.promptTokensDetails().cachedTokens() > 0) {
                                    cachedTokens[0] = openAiUsage.promptTokensDetails().cachedTokens();
                                }
                                if (openAiUsage.completionTokenDetails() != null
                                        && openAiUsage.completionTokenDetails().reasoningTokens() != null
                                        && openAiUsage.completionTokenDetails().reasoningTokens() > 0) {
                                    reasoningTokens[0] = openAiUsage.completionTokenDetails().reasoningTokens();
                                }
                            }
                        }
                    } catch (IOException e) {
                        log.error("SSE 发送失败，客户端可能已断开", e);
                    }
                })
                .doOnComplete(latch::countDown)
                .doOnError(e -> {
                    log.error("流式调用异常", e);
                    streamError[0] = e;
                    latch.countDown();
                })
                .subscribe();
        latch.await();
        if (streamError[0] != null) {
            throw new BynBaseException(extractApiErrorDetail(streamError[0]));
        }
    }

    private ChatResponse aggregateStream(ChatModel chatModel, Prompt prompt,
                                          SseEmitter emitter, StringBuilder fullResponse,
                                          StringBuilder[] outReasoning,
                                          long[] promptTokens, long[] completionTokens,
                                          long[] cachedTokens, long[] reasoningTokens,
                                          AtomicBoolean cancelFlag) throws InterruptedException {
        StringBuilder aggregatedText = new StringBuilder();
        List<AssistantMessage.ToolCall> toolCalls = new ArrayList<>();
        Map<String, Object> aggregatedMetadata = new java.util.HashMap<>();
        CountDownLatch latch = new CountDownLatch(1);
        Throwable[] streamError = {null};

        chatModel.stream(prompt)
                .doOnNext(chatResponse -> {
                    if (cancelFlag.get()) { latch.countDown(); return; }
                    try {
                        var generation = chatResponse.getResult();
                        if (generation == null) return;
                        var message = generation.getOutput();

                        Map<String, Object> metadata = message.getMetadata();
                        if (metadata != null && metadata.containsKey("reasoningContent")) {
                            String reasoning = (String) metadata.get("reasoningContent");
                            if (reasoning != null && !reasoning.isEmpty()) {
                                aggregatedMetadata.put("reasoningContent",
                                        aggregatedMetadata.getOrDefault("reasoningContent", "") + reasoning);
                                emitter.send(SseEmitter.event().name("reasoning").data(reasoning));
                            }
                        }

                        String text = message.getText();
                        if (text != null && !text.isEmpty()) {
                            aggregatedText.append(text);
                            fullResponse.append(text);
                            emitter.send(SseEmitter.event().data(text));
                        }

                        if (message.getToolCalls() != null) {
                            for (var tc : message.getToolCalls()) {
                                if (tc.name() != null && !toolCalls.stream().anyMatch(e -> e.id().equals(tc.id()))) {
                                    toolCalls.add(tc);
                                }
                            }
                        }

                        Usage usage = chatResponse.getMetadata().getUsage();
                        if (usage != null) {
                            if (usage.getPromptTokens() != null && usage.getPromptTokens() > 0) {
                                promptTokens[0] = usage.getPromptTokens();
                            }
                            if (usage.getCompletionTokens() != null && usage.getCompletionTokens() > 0) {
                                completionTokens[0] = usage.getCompletionTokens();
                            }
                            if (usage.getNativeUsage() instanceof OpenAiApi.Usage openAiUsage) {
                                if (openAiUsage.promptTokensDetails() != null
                                        && openAiUsage.promptTokensDetails().cachedTokens() != null
                                        && openAiUsage.promptTokensDetails().cachedTokens() > 0) {
                                    cachedTokens[0] = openAiUsage.promptTokensDetails().cachedTokens();
                                }
                                if (openAiUsage.completionTokenDetails() != null
                                        && openAiUsage.completionTokenDetails().reasoningTokens() != null
                                        && openAiUsage.completionTokenDetails().reasoningTokens() > 0) {
                                    reasoningTokens[0] = openAiUsage.completionTokenDetails().reasoningTokens();
                                }
                            }
                        }
                    } catch (IOException e) {
                        log.error("SSE 发送失败", e);
                    }
                })
                .doOnComplete(latch::countDown)
                .doOnError(e -> {
                    log.error("流式调用异常", e);
                    streamError[0] = e;
                    latch.countDown();
                })
                .subscribe();

        latch.await();
        if (streamError[0] != null) {
            throw new BynBaseException(extractApiErrorDetail(streamError[0]));
        }

        String aggregatedReasoning = (String) aggregatedMetadata.get("reasoningContent");
        if (aggregatedReasoning != null && !aggregatedReasoning.isEmpty()) {
            outReasoning[0] = new StringBuilder(aggregatedReasoning);
        }

        if (aggregatedText.isEmpty() && toolCalls.isEmpty()) {
            return null;
        }

        AssistantMessage assistantMessage = AssistantMessage.builder()
                .content(aggregatedText.toString())
                .properties(aggregatedMetadata)
                .toolCalls(toolCalls)
                .build();
        var generation = new org.springframework.ai.chat.model.Generation(assistantMessage);
        return new ChatResponse(List.of(generation));
    }

    /**
     * 从异常链中提取 AI API 返回的错误信息。
     * 覆盖三种场景：
     * 1. 流式调用报错 → WebClientResponseException（WebClient 抛出）
     * 2. 非流式调用报错 → NonTransientAiException，消息格式 "statusCode - responseBody"
     * 3. 其他 RestClient 报错 → HttpStatusCodeException
     * 优先从 response body 解析 {"error":{"message":"..."}}，
     * 解析失败则返回原始 body，兜底返回异常 message。
     */
    private String extractApiErrorDetail(Throwable e) {
        Throwable current = e;
        while (current != null) {
            // 场景1：流式调用 → WebClient 抛出 WebClientResponseException
            if (current instanceof WebClientResponseException webEx) {
                return parseApiErrorBody(webEx.getResponseBodyAsString());
            }
            // 场景2：非流式调用 → Spring AI 包成 NonTransientAiException，消息格式 "400 - {json}"
            if (current instanceof NonTransientAiException aiEx) {
                String msg = aiEx.getMessage();
                if (msg != null && msg.contains(" - ")) {
                    String body = msg.substring(msg.indexOf(" - ") + 3);
                    String parsed = parseApiErrorBody(body);
                    if (parsed != null) return parsed;
                }
            }
            // 场景3：RestClient 直接抛出 HttpStatusCodeException
            if (current instanceof HttpStatusCodeException httpEx) {
                return parseApiErrorBody(httpEx.getResponseBodyAsString());
            }
            current = current.getCause();
        }
        return e.getMessage();
    }

    /**
     * 解析 AI API 错误响应 body，尝试提取 error.message 字段。
     */
    private String parseApiErrorBody(String body) {
        if (body == null || body.isBlank()) {
            return body;
        }
        try {
            JSONObject json = JSON.parseObject(body);
            JSONObject error = json.getJSONObject("error");
            if (error != null && error.getString("message") != null) {
                return error.getString("message");
            }
        } catch (Exception ignored) {
        }
        return body;
    }

    private String generateSessionTitle(String userMessage) {
        try {
            String modelId = sysConfigService.getConfigValue("simpleTaskModelId");
            if (!StringUtils.hasText(modelId)) {
                return null;
            }
            ChatModel titleModel = sysLlmModelService.createChatModelByModelId(modelId);
            String promptText = "请根据以下用户消息，生成一个不超过20个字的简短标题，直接输出标题内容，不要加引号或其他格式：\n\n" + userMessage;
            Prompt prompt = new Prompt(List.of(new UserMessage(promptText)));
            ChatResponse response = titleModel.call(prompt);
            String title = response.getResult().getOutput().getText();
            if (title == null || title.isBlank()) {
                return null;
            }
            // 清理引号
            title = title.trim().replaceAll("^[\"'\u201c\u201d\u2018\u2019\u300a\u300b]|[\"'\u201c\u201d\u2018\u2019\u300a\u300b]$", "");
            // 截断50字符
            if (title.length() > 50) {
                title = title.substring(0, 50);
            }
            return title;
        } catch (Exception e) {
            log.warn("标题生成失败", e);
            return null;
        }
    }

    @Override
    public void editMessage(AgenticChatAppVo.EditMessageVo vo) {
        String userId = securityUtil.getUserId();
        AgenticChatMessage message = agenticChatMessageService.getById(vo.getMessageId());
        if (message == null) {
            throw new BynBaseException("消息不存在");
        }

        AgenticChatSession session = agenticChatSessionService.getById(message.getSessionId());
        if (session == null || !session.getCreateUser().equals(userId)) {
            throw new BynBaseException("无权操作此消息");
        }

        if (!vo.getDeleteAfterMsgs()) {
            message.setTextContent(vo.getTextContent());
            agenticChatMessageService.updateById(message);
            return;
        }

        // deleteAfterMsgs == true
        if (Integer.valueOf(1).equals(message.getMessageType())) {
            // messageType=1（user）：删除自身及后续所有消息，textContent 无效
            agenticChatMessageService.remove(
                    new LambdaQueryWrapper<AgenticChatMessage>()
                            .eq(AgenticChatMessage::getSessionId, message.getSessionId())
                            .ge(AgenticChatMessage::getMessageIndex, message.getMessageIndex())
            );
        } else if (Integer.valueOf(2).equals(message.getMessageType())) {
            // messageType=2（assistant）：updateById 修改 textContent，删除所有 messageIndex > 当前的消息
            message.setTextContent(vo.getTextContent());
            agenticChatMessageService.updateById(message);

            agenticChatMessageService.remove(
                    new LambdaQueryWrapper<AgenticChatMessage>()
                            .eq(AgenticChatMessage::getSessionId, message.getSessionId())
                            .gt(AgenticChatMessage::getMessageIndex, message.getMessageIndex())
            );
        } else {
            // 其他类型消息仅更新文本
            message.setTextContent(vo.getTextContent());
            agenticChatMessageService.updateById(message);
        }
    }

    @Override
    public void deleteMessage(AgenticChatAppVo.DeleteMessageVo vo) {
        String userId = securityUtil.getUserId();
        AgenticChatMessage message = agenticChatMessageService.getById(vo.getMessageId());
        if (message == null) {
            throw new BynBaseException("消息不存在");
        }

        AgenticChatSession session = agenticChatSessionService.getById(message.getSessionId());
        if (session == null || !session.getCreateUser().equals(userId)) {
            throw new BynBaseException("无权操作此消息");
        }

        agenticChatMessageService.remove(
                new LambdaQueryWrapper<AgenticChatMessage>()
                        .eq(AgenticChatMessage::getSessionId, message.getSessionId())
                        .ge(AgenticChatMessage::getMessageIndex, message.getMessageIndex())
        );
    }

    @Override
    public void editSessionName(AgenticChatAppVo.EditSessionNameVo vo) {
        String userId = securityUtil.getUserId();
        AgenticChatSession session = agenticChatSessionService.getOne(
                new LambdaQueryWrapper<AgenticChatSession>()
                        .eq(AgenticChatSession::getId, vo.getSessionId())
                        .eq(AgenticChatSession::getCreateUser, userId)
        );
        if (session == null) {
            throw new BynBaseException("会话不存在");
        }

        session.setSessionName(vo.getSessionName());
        agenticChatSessionService.updateById(session);
    }

    @Override
    public void deleteSession(AgenticChatAppVo.DeleteSessionVo vo) {
        String userId = securityUtil.getUserId();
        AgenticChatSession session = agenticChatSessionService.getOne(
                new LambdaQueryWrapper<AgenticChatSession>()
                        .eq(AgenticChatSession::getId, vo.getSessionId())
                        .eq(AgenticChatSession::getCreateUser, userId)
        );
        if (session == null) {
            throw new BynBaseException("会话不存在");
        }

        // 删除该 session 下所有消息（逻辑删除）
        agenticChatMessageService.remove(
                new LambdaQueryWrapper<AgenticChatMessage>()
                        .eq(AgenticChatMessage::getSessionId, vo.getSessionId())
        );

        // 删除 session 本身（逻辑删除）
        agenticChatSessionService.removeById(vo.getSessionId());
    }

    @Override
    public void submitPyodideToolResult(PyodideToolResultVo vo) {
        String userId = securityUtil.getUserId();
        PyodideResultPendingManager.PyodideResult result = new PyodideResultPendingManager.PyodideResult(
                vo.getStdout(),
                vo.getStderr(),
                vo.getError(),
                vo.getExitCode()
        );
        pyodideResultPendingManager.completeResult(vo.getRequestId(), userId, result);
    }

    @Override
    public Map<String, String> stopChatStream(String streamUniqueKey) {
        String userId = securityUtil.getUserId();
        ConcurrentHashMap<String, AtomicBoolean> userFlags = cancellationFlags.get(userId);
        if (userFlags != null) {
            AtomicBoolean flag = userFlags.get(streamUniqueKey);
            if (flag != null) {
                CompletableFuture<Map<String, String>> future = new CompletableFuture<>();
                streamStopResults.put(streamUniqueKey, future);
                flag.set(true);
                log.info("用户 {} 取消聊天流, streamUniqueKey={}", userId, streamUniqueKey);
                try {
                    return future.get(30, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    log.warn("停止流超时, streamUniqueKey={}", streamUniqueKey);
                    return Map.of("error", "timeout");
                } catch (Exception e) {
                    log.error("停止流异常, streamUniqueKey={}", streamUniqueKey, e);
                    return Map.of("error", "failed");
                } finally {
                    streamStopResults.remove(streamUniqueKey);
                }
            }
        }
        return Map.of();
    }

    // ==================== 会话分享功能 ====================

    private AgenticChatSession verifySessionOwnership(String sessionId) {
        String userId = securityUtil.getUserId();
        AgenticChatSession session = agenticChatSessionService.getOne(
                new LambdaQueryWrapper<AgenticChatSession>()
                        .eq(AgenticChatSession::getId, sessionId)
                        .eq(AgenticChatSession::getCreateUser, userId)
        );
        if (session == null) {
            throw new BynBaseException("会话不存在");
        }
        return session;
    }

    private AgenticChatSharedSession findSharedSessionByOriginalId(String originalSessionId) {
        return agenticChatSharedSessionService.getOne(
                new LambdaQueryWrapper<AgenticChatSharedSession>()
                        .eq(AgenticChatSharedSession::getOriginalSessionId, originalSessionId)
        );
    }

    private void copyMessagesToSharedSession(String originalSessionId, String sharedSessionId,
                                              String userId, boolean filterSensitive) {
        List<AgenticChatMessage> originalMessages = agenticChatMessageService.list(
                new LambdaQueryWrapper<AgenticChatMessage>()
                        .eq(AgenticChatMessage::getSessionId, originalSessionId)
                        .orderByAsc(AgenticChatMessage::getMessageIndex)
        );

        List<AgenticChatSharedMessage> sharedMessages = originalMessages.stream().map(msg -> {
            AgenticChatSharedMessage sharedMsg = AgenticChatSharedMessage.builder()
                    .sessionId(sharedSessionId)
                    .messageIndex(msg.getMessageIndex())
                    .messageType(msg.getMessageType())
                    .textContent(msg.getTextContent())
                    .mediaContent(msg.getMediaContent())
                    .promptTokenCount(msg.getPromptTokenCount())
                    .completionTokenCount(msg.getCompletionTokenCount())
                    .cachedTokenCount(msg.getCachedTokenCount())
                    .reasoningTokenCount(msg.getReasoningTokenCount())
                    .toolContent(filterSensitive ? null : msg.getToolContent())
                    .reasoningContent(filterSensitive ? null : msg.getReasoningContent())
                    .structuredToolContent(filterSensitive ? null : msg.getStructuredToolContent())
                    .build();
            sharedMsg.setCreateUser(userId);
            sharedMsg.setUpdateUser(userId);
            return sharedMsg;
        }).toList();

        agenticChatSharedMessageService.saveBatch(sharedMessages);
    }

    @Override
    public String shareSession(AgenticChatAppVo.ShareSessionVo vo) {
        AgenticChatSession session = verifySessionOwnership(vo.getSessionId());

        AgenticChatSharedSession existing = findSharedSessionByOriginalId(vo.getSessionId());
        if (existing != null) {
            throw new BynBaseException("该会话已分享，如需更新请使用重新分享");
        }

        boolean shareTextContentOnly = !Boolean.FALSE.equals(vo.getShareTextContentOnly());
        String userId = securityUtil.getUserId();
        AgenticChatSharedSession sharedSession = AgenticChatSharedSession.builder()
                .sessionName(session.getSessionName())
                .modelId(session.getModelId())
                .modelType(session.getModelType())
                .agentType(session.getAgentType())
                .originalSessionId(vo.getSessionId())
                .shareTextContentOnly(shareTextContentOnly)
                .build();
        sharedSession.setId(UUID.randomUUID().toString());
        sharedSession.setCreateUser(userId);
        sharedSession.setUpdateUser(userId);
        agenticChatSharedSessionService.save(sharedSession);

        copyMessagesToSharedSession(vo.getSessionId(), sharedSession.getId(), userId, shareTextContentOnly);

        return sharedSession.getId();
    }

    @Override
    public void unshareSession(String sessionId) {
        verifySessionOwnership(sessionId);

        AgenticChatSharedSession sharedSession = findSharedSessionByOriginalId(sessionId);
        if (sharedSession == null) {
            throw new BynBaseException("该会话未分享");
        }

        // 删除 shared messages（逻辑删除）
        agenticChatSharedMessageService.remove(
                new LambdaQueryWrapper<AgenticChatSharedMessage>()
                        .eq(AgenticChatSharedMessage::getSessionId, sharedSession.getId())
        );

        // 删除 shared session（逻辑删除）
        agenticChatSharedSessionService.removeById(sharedSession.getId());
    }

    @Override
    public String reshareSession(String sessionId) {
        AgenticChatSession session = verifySessionOwnership(sessionId);

        AgenticChatSharedSession sharedSession = findSharedSessionByOriginalId(sessionId);
        if (sharedSession == null) {
            throw new BynBaseException("该会话未分享，请先分享");
        }

        String userId = securityUtil.getUserId();

        sharedSession.setSessionName(session.getSessionName());
        sharedSession.setModelId(session.getModelId());
        sharedSession.setModelType(session.getModelType());
        sharedSession.setAgentType(session.getAgentType());
        sharedSession.setUpdateUser(userId);
        agenticChatSharedSessionService.updateById(sharedSession);

        agenticChatSharedMessageService.remove(
                new LambdaQueryWrapper<AgenticChatSharedMessage>()
                        .eq(AgenticChatSharedMessage::getSessionId, sharedSession.getId())
        );

        boolean filterSensitive = !Boolean.FALSE.equals(sharedSession.getShareTextContentOnly());
        copyMessagesToSharedSession(sessionId, sharedSession.getId(), userId, filterSensitive);

        return sharedSession.getId();
    }

    @Override
    public AgenticChatSharedSession getSharedSession(String sharedSessionId) {
        AgenticChatSharedSession sharedSession = agenticChatSharedSessionService.getById(sharedSessionId);
        if (sharedSession == null) {
            throw new BynBaseException("分享的会话不存在");
        }

        boolean filterSensitive = !Boolean.FALSE.equals(sharedSession.getShareTextContentOnly());

        List<AgenticChatSharedMessage> messages = agenticChatSharedMessageService.list(
                new LambdaQueryWrapper<AgenticChatSharedMessage>()
                        .eq(AgenticChatSharedMessage::getSessionId, sharedSessionId)
                        .orderByAsc(AgenticChatSharedMessage::getMessageIndex)
        ).stream().map(msg -> {
            if (filterSensitive) {
                msg.setReasoningContent(null);
                msg.setStructuredToolContent(null);
                if (msg.getToolContent() != null) {
                    msg.setToolContent(null);
                }
            }
            if (Integer.valueOf(3).equals(msg.getMessageType())
                    && msg.getStructuredToolContent() != null) {
                msg.setToolContent(msg.getStructuredToolContent());
            }
            if ((Integer.valueOf(2).equals(msg.getMessageType())
                    || Integer.valueOf(3).equals(msg.getMessageType()))
                    && msg.getToolContent() != null) {
                msg.setToolContent(convertToolNamesInJson(msg.getToolContent()));
            }
            return msg;
        }).toList();

        sharedSession.setModel(sysLlmModelService.getById(sharedSession.getModelId()));
        sharedSession.setMessages(messages);
        return sharedSession;
    }

    @Override
    public Page<AgenticChatSharedSession> pageSharedSessions(AgenticChatAppVo.PageSessionsVo vo) {
        String userId = securityUtil.getUserId();
        Page<AgenticChatSharedSession> page = new Page<>(vo.getPageNum(), vo.getPageSize());
        return agenticChatSharedSessionService.page(page,
                new LambdaQueryWrapper<AgenticChatSharedSession>()
                        .eq(AgenticChatSharedSession::getCreateUser, userId)
                        .orderByDesc(AgenticChatSharedSession::getUpdateTime));
    }
}

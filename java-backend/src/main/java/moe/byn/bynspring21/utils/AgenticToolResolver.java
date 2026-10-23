package moe.byn.bynspring21.utils;

import moe.byn.bynspring21.entity.base.AgenticToolEnum;
import moe.byn.bynspring21.entity.tool.BochaWebSearch;
import moe.byn.bynspring21.entity.tool.ClawApprovalPendingManager;
import moe.byn.bynspring21.entity.tool.ClawApprovalToolCallback;
import moe.byn.bynspring21.entity.tool.DeepseekWebFetch;
import moe.byn.bynspring21.entity.tool.DeepseekWebSearch;
import moe.byn.bynspring21.entity.tool.EditFile;
import moe.byn.bynspring21.entity.tool.ExecuteShellCommand;
import moe.byn.bynspring21.entity.tool.GetDatetime;
import moe.byn.bynspring21.entity.tool.ListFiles;
import moe.byn.bynspring21.entity.tool.MemoryTools;
import moe.byn.bynspring21.entity.tool.PyodideCodeRunner;
import moe.byn.bynspring21.entity.tool.RagKnowledgeBaseTool;
import moe.byn.bynspring21.entity.tool.ReadFile;
import moe.byn.bynspring21.entity.tool.SearchFiles;
import moe.byn.bynspring21.entity.tool.WebFetch;
import moe.byn.bynspring21.entity.tool.WebFetchProxyHidingToolCallback;
import moe.byn.bynspring21.entity.tool.WriteFile;
import moe.byn.bynspring21.service.ClawApprovalJudgeService;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class AgenticToolResolver {

    /**
     * Claw-like 专属工具集（6 个文件/Shell 工具），解析回调时包装审批装饰器
     */
    private static final List<AgenticToolEnum> CLAW_LIKE_TOOLS = List.of(
            AgenticToolEnum.EXECUTE_SHELL_COMMAND,
            AgenticToolEnum.READ_FILE,
            AgenticToolEnum.WRITE_FILE,
            AgenticToolEnum.EDIT_FILE,
            AgenticToolEnum.LIST_FILES,
            AgenticToolEnum.SEARCH_FILES);

    @Autowired
    private BochaWebSearch bochaWebSearch;

    @Autowired
    private PyodideCodeRunner pyodideCodeRunner;

    @Autowired
    private MemoryTools memoryTools;

    @Autowired
    private WebFetch webFetch;

    @Autowired
    private GetDatetime getDatetime;

    @Autowired
    private RagKnowledgeBaseTool ragKnowledgeBaseTool;

    @Autowired
    private ExecuteShellCommand executeShellCommand;

    @Autowired
    private ReadFile readFile;

    @Autowired
    private WriteFile writeFile;

    @Autowired
    private EditFile editFile;

    @Autowired
    private ListFiles listFiles;

    @Autowired
    private SearchFiles searchFiles;

    @Autowired
    private DeepseekWebSearch deepseekWebSearch;

    @Autowired
    private DeepseekWebFetch deepseekWebFetch;

    public List<Object> resolve(List<AgenticToolEnum> tools) {
        if (tools == null || tools.isEmpty()) {
            return Collections.emptyList();
        }
        return tools.stream()
                .<Object>map(tool -> switch (tool) {
                    case BOCHA_WEB_SEARCH -> bochaWebSearch;
                    case PYODIDE_CODE_RUNNER -> pyodideCodeRunner;
                    case MEMORY_TOOLS -> memoryTools;
                    case WEB_FETCH -> webFetch;
                    case GET_DATETIME -> getDatetime;
                    case RAG_KNOWLEDGE_BASE -> ragKnowledgeBaseTool;
                    case EXECUTE_SHELL_COMMAND -> executeShellCommand;
                    case READ_FILE -> readFile;
                    case WRITE_FILE -> writeFile;
                    case EDIT_FILE -> editFile;
                    case LIST_FILES -> listFiles;
                    case SEARCH_FILES -> searchFiles;
                    case DEEPSEEK_WEB_SEARCH -> deepseekWebSearch;
                    case DEEPSEEK_WEB_FETCH -> deepseekWebFetch;
                })
                .toList();
    }

    public ToolCallback[] resolveToolCallbacks(List<AgenticToolEnum> tools) {
        return resolveToolCallbacks(tools, null, null, false);
    }

    public ToolCallback[] resolveToolCallbacks(List<AgenticToolEnum> tools, boolean webFetchProxyAllowed) {
        return resolveToolCallbacks(tools, null, null, webFetchProxyAllowed);
    }

    public ToolCallback[] resolveToolCallbacks(List<AgenticToolEnum> tools,
                                               ClawApprovalJudgeService judgeService,
                                               ClawApprovalPendingManager pendingManager) {
        return resolveToolCallbacks(tools, judgeService, pendingManager, false);
    }

    /**
     * 解析工具回调，传入审批 bean 时对 Claw-like 专属工具（6 个文件/Shell 工具）包装审批装饰器，
     * 审批 bean 为空或普通聊天工具不受影响；
     * 未获网页抓取代理授权时对 web_fetch 包装 schema 隐藏装饰器，移除 withProxy 参数
     */
    public ToolCallback[] resolveToolCallbacks(List<AgenticToolEnum> tools,
                                               ClawApprovalJudgeService judgeService,
                                               ClawApprovalPendingManager pendingManager,
                                               boolean webFetchProxyAllowed) {
        if (tools == null || tools.isEmpty()) {
            return new ToolCallback[0];
        }
        boolean wrapClawTools = judgeService != null && pendingManager != null;
        // 逐工具对象解析，避免一个对象含多个 @Tool 方法时回调与枚举错位
        List<ToolCallback> result = new ArrayList<>();
        for (AgenticToolEnum tool : tools) {
            ToolCallback[] callbacks = ToolCallbacks.from(resolveToolObject(tool));
            if (wrapClawTools && CLAW_LIKE_TOOLS.contains(tool)) {
                for (ToolCallback callback : callbacks) {
                    result.add(new ClawApprovalToolCallback(callback, judgeService, pendingManager));
                }
            } else if (tool == AgenticToolEnum.WEB_FETCH && !webFetchProxyAllowed) {
                for (ToolCallback callback : callbacks) {
                    result.add(new WebFetchProxyHidingToolCallback(callback));
                }
            } else {
                result.addAll(Arrays.asList(callbacks));
            }
        }
        return result.toArray(new ToolCallback[0]);
    }

    private Object resolveToolObject(AgenticToolEnum tool) {
        return switch (tool) {
            case BOCHA_WEB_SEARCH -> bochaWebSearch;
            case PYODIDE_CODE_RUNNER -> pyodideCodeRunner;
            case MEMORY_TOOLS -> memoryTools;
            case WEB_FETCH -> webFetch;
            case GET_DATETIME -> getDatetime;
            case RAG_KNOWLEDGE_BASE -> ragKnowledgeBaseTool;
            case EXECUTE_SHELL_COMMAND -> executeShellCommand;
            case READ_FILE -> readFile;
            case WRITE_FILE -> writeFile;
            case EDIT_FILE -> editFile;
            case LIST_FILES -> listFiles;
            case SEARCH_FILES -> searchFiles;
            case DEEPSEEK_WEB_SEARCH -> deepseekWebSearch;
            case DEEPSEEK_WEB_FETCH -> deepseekWebFetch;
        };
    }
}

package moe.byn.bynspring21.utils;

import moe.byn.bynspring21.entity.base.AgenticToolEnum;
import moe.byn.bynspring21.entity.tool.BochaWebSearch;
import moe.byn.bynspring21.entity.tool.GetDatetime;
import moe.byn.bynspring21.entity.tool.MemoryTools;
import moe.byn.bynspring21.entity.tool.PyodideCodeRunner;
import moe.byn.bynspring21.entity.tool.RagKnowledgeBaseTool;
import moe.byn.bynspring21.entity.tool.WebFetch;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class AgenticToolResolver {

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
                })
                .toList();
    }

    public ToolCallback[] resolveToolCallbacks(List<AgenticToolEnum> tools) {
        if (tools == null || tools.isEmpty()) {
            return new ToolCallback[0];
        }
        Object[] toolObjects = tools.stream()
                .<Object>map(tool -> switch (tool) {
                    case BOCHA_WEB_SEARCH -> bochaWebSearch;
                    case PYODIDE_CODE_RUNNER -> pyodideCodeRunner;
                    case MEMORY_TOOLS -> memoryTools;
                    case WEB_FETCH -> webFetch;
                    case GET_DATETIME -> getDatetime;
                    case RAG_KNOWLEDGE_BASE -> ragKnowledgeBaseTool;
                })
                .toArray();
        return ToolCallbacks.from(toolObjects);
    }
}

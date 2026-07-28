package moe.byn.bynspring21.utils;

import moe.byn.bynspring21.entity.base.AgenticToolEnum;
import moe.byn.bynspring21.entity.tool.BochaWebSearch;
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
import moe.byn.bynspring21.entity.tool.WriteFile;
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
                    case EXECUTE_SHELL_COMMAND -> executeShellCommand;
                    case READ_FILE -> readFile;
                    case WRITE_FILE -> writeFile;
                    case EDIT_FILE -> editFile;
                    case LIST_FILES -> listFiles;
                    case SEARCH_FILES -> searchFiles;
                })
                .toArray();
        return ToolCallbacks.from(toolObjects);
    }
}

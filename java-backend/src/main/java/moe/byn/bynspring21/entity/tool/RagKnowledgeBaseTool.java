package moe.byn.bynspring21.entity.tool;

import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.entity.RagLibrary;
import moe.byn.bynspring21.mapper.RagDocChunkMapper;
import moe.byn.bynspring21.service.RagLibraryService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RagKnowledgeBaseTool {

    private static final ThreadLocal<List<String>> libraryIdsHolder = new ThreadLocal<>();

    @Autowired
    private RagLibraryService ragLibraryService;

    @Autowired
    private RagDocChunkMapper ragDocChunkMapper;

    public record RagSearchResultItem(
            String libraryName,
            String fileName,
            int chunkIndex,
            String chunkContent,
            double relevance
    ) {}

    private static final ThreadLocal<Deque<List<RagSearchResultItem>>> structuredResultsQueue =
            ThreadLocal.withInitial(ArrayDeque::new);

    public static List<RagSearchResultItem> consumeLastStructuredResults() {
        Deque<List<RagSearchResultItem>> queue = structuredResultsQueue.get();
        List<RagSearchResultItem> results = queue.pollFirst();
        return results != null ? results : Collections.emptyList();
    }

    public void setContext(List<String> libraryIds, String userId) {
        if (libraryIds == null || libraryIds.isEmpty()) {
            libraryIdsHolder.set(Collections.emptyList());
            return;
        }
        List<String> validIds = libraryIds.stream()
                .filter(id -> {
                    RagLibrary lib = ragLibraryService.getById(id);
                    return lib != null && userId.equals(lib.getCreateUser());
                })
                .toList();
        libraryIdsHolder.set(validIds);
    }

    public static void clearContext() {
        libraryIdsHolder.remove();
        structuredResultsQueue.remove();
    }

    @Tool(description = "在 RAG 知识库中搜索与用户问题相关的内容块。" +
            "当用户询问关于某个具体知识库内容时调用此工具。" +
            "返回最匹配的文本块及其来源文档名称和知识库名称。")
    public String searchRagKnowledgeBase(
            @ToolParam(description = "用户的搜索查询内容", required = true) String query) {
        try {
            List<String> libraryIds = libraryIdsHolder.get();

            // 未选择任何知识库
            if (libraryIds == null || libraryIds.isEmpty()) {
                return "未选择知识库";
            }

            // 直接调用 mapper 查询，不走 service（所有权校验已在 setContext 阶段完成）
            Map<String, String> libNameMap = ragLibraryService.listByIds(libraryIds).stream()
                    .collect(Collectors.toMap(RagLibrary::getId, RagLibrary::getName));

            List<Map<String, Object>> rawResults = ragDocChunkMapper.searchChunks(libraryIds, query, 5);

            if (rawResults.isEmpty()) {
                return "未找到相关结果";
            }

            StringBuilder sb = new StringBuilder("找到以下相关内容：\n\n");
            for (int i = 0; i < rawResults.size(); i++) {
                Map<String, Object> row = rawResults.get(i);
                String libId = (String) row.get("libraryId");
                sb.append("---\n");
                sb.append("结果 ").append(i + 1).append("\n");
                sb.append("知识库：").append(libNameMap.getOrDefault(libId, "")).append("\n");
                sb.append("文档：").append(row.get("fileName")).append("\n");
                sb.append("内容：").append(row.get("chunkContent")).append("\n");
            }
            List<RagSearchResultItem> structured = rawResults.stream()
                    .map(row -> new RagSearchResultItem(
                            libNameMap.getOrDefault((String) row.get("libraryId"), ""),
                            (String) row.get("fileName"),
                            row.get("chunkIndex") != null ? ((Number) row.get("chunkIndex")).intValue() : 0,
                            (String) row.get("chunkContent"),
                            row.get("relevance") != null ? ((Number) row.get("relevance")).doubleValue() : 0.0
                    ))
                    .toList();
            structuredResultsQueue.get().addLast(structured);

            return sb.toString();
        } catch (Exception e) {
            log.error("searchRagKnowledgeBase 执行失败, query={}", query, e);
            return "知识库搜索失败，请稍后重试";
        }
    }
}

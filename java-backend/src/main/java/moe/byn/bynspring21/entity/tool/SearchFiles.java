package moe.byn.bynspring21.entity.tool;

import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.utils.ClawWorkspaceUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Component
public class SearchFiles {

    private static final int MAX_RESULTS = 100;
    private static final long MAX_SEARCHABLE_FILE_SIZE = 1024 * 1024;
    private static final int MAX_LINE_LENGTH = 500;

    @Autowired
    private ClawWorkspaceUtil clawWorkspaceUtil;

    @Tool(name = "search_files", description = "在当前工作区内递归搜索包含指定关键词的文本行，返回 相对路径:行号:行内容 格式的匹配结果（最多 100 条）。")
    public String searchFiles(
            @ToolParam(description = "要搜索的关键词（纯文本，非正则）", required = true) String keyword,
            @ToolParam(description = "搜索范围（相对于当前工作区的目录），为空表示整个工作区", required = false) String path) {
        try {
            if (keyword == null || keyword.isEmpty()) {
                return "错误：keyword 不能为空";
            }
            Path workspace = clawWorkspaceUtil.resolveCurrentWorkspace();
            Path baseDir = clawWorkspaceUtil.resolveInWorkspace(workspace, path);
            if (!Files.exists(baseDir) || !Files.isDirectory(baseDir)) {
                return "错误：搜索目录不存在 - " + (path != null ? path : ".");
            }

            List<Path> files;
            try (Stream<Path> stream = Files.walk(baseDir)) {
                files = stream.filter(Files::isRegularFile)
                        .sorted(Comparator.comparing(Path::toString))
                        .toList();
            }

            StringBuilder sb = new StringBuilder();
            int matchCount = 0;
            for (Path file : files) {
                if (matchCount >= MAX_RESULTS) {
                    break;
                }
                if (Files.size(file) > MAX_SEARCHABLE_FILE_SIZE) {
                    continue;
                }
                matchCount = searchInFile(workspace, file, keyword, sb, matchCount);
            }

            if (matchCount == 0) {
                return "未找到包含关键词 \"" + keyword + "\" 的内容";
            }
            String header = "共找到 " + matchCount + " 处匹配：\n";
            String tail = matchCount >= MAX_RESULTS ? "...（匹配过多，仅显示前 " + MAX_RESULTS + " 条）\n" : "";
            return header + sb + tail;
        } catch (Exception e) {
            log.error("search_files 执行失败", e);
            return "错误：搜索文件失败 - " + e.getMessage();
        }
    }

    private int searchInFile(Path workspace, Path file, String keyword, StringBuilder sb, int matchCount) {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String relativePath = workspace.relativize(file).toString();
            String line;
            long lineNo = 0;
            while ((line = reader.readLine()) != null && matchCount < MAX_RESULTS) {
                lineNo++;
                if (line.contains(keyword)) {
                    String trimmed = line.strip();
                    if (trimmed.length() > MAX_LINE_LENGTH) {
                        trimmed = trimmed.substring(0, MAX_LINE_LENGTH) + "...";
                    }
                    sb.append(relativePath).append(":").append(lineNo).append(":").append(trimmed).append("\n");
                    matchCount++;
                }
            }
        } catch (Exception e) {
            // 非 UTF-8 文本（如二进制文件）或读取失败，直接跳过
            log.debug("search_files 跳过文件 {}: {}", file, e.getMessage());
        }
        return matchCount;
    }
}

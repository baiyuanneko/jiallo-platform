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

@Slf4j
@Component
public class ReadFile {

    private static final long DEFAULT_LINE_LIMIT = 200;
    private static final long MAX_LINE_LIMIT = 2000;
    private static final int MAX_OUTPUT_CHARS = 30 * 1024;

    @Autowired
    private ClawWorkspaceUtil clawWorkspaceUtil;

    @Tool(name = "read_file", description = "读取当前工作区内指定文件的文本内容，可通过行偏移和行数限制分段读取大文件。")
    public String readFile(
            @ToolParam(description = "文件路径（相对于当前工作区）", required = true) String path,
            @ToolParam(description = "起始行号（从 1 开始），默认 1", required = false) Long offset,
            @ToolParam(description = "最多读取的行数，默认 200，最大 2000", required = false) Long limit) {
        try {
            Path workspace = clawWorkspaceUtil.resolveCurrentWorkspace();
            Path file = clawWorkspaceUtil.resolveInWorkspace(workspace, path);
            if (!Files.exists(file)) {
                return "错误：文件不存在 - " + path;
            }
            if (!Files.isRegularFile(file)) {
                return "错误：目标不是普通文件 - " + path;
            }

            long startLine = (offset != null && offset > 0) ? offset : 1;
            long maxLines = (limit != null && limit > 0) ? Math.min(limit, MAX_LINE_LIMIT) : DEFAULT_LINE_LIMIT;

            StringBuilder sb = new StringBuilder();
            long totalLines = 0;
            long readLines = 0;
            boolean charTruncated = false;
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    totalLines++;
                    if (totalLines < startLine || readLines >= maxLines) {
                        continue;
                    }
                    if (sb.length() + line.length() + 1 > MAX_OUTPUT_CHARS) {
                        charTruncated = true;
                        continue;
                    }
                    sb.append(line).append("\n");
                    readLines++;
                }
            } catch (java.nio.charset.MalformedInputException e) {
                return "错误：文件不是有效的 UTF-8 文本 - " + path;
            }

            if (readLines == 0) {
                return totalLines == 0 ? "文件为空" : "错误：起始行号超出文件范围（共 " + totalLines + " 行）";
            }
            String header = "文件 " + path + " 第 " + startLine + "-" + (startLine + readLines - 1)
                    + " 行（共 " + totalLines + " 行）：\n";
            String tail = (readLines < totalLines - startLine + 1 || charTruncated)
                    ? "...（内容未完整显示，可通过 offset/limit 继续读取）\n"
                    : "";
            return header + sb + tail;
        } catch (Exception e) {
            log.error("read_file 执行失败", e);
            return "错误：读取文件失败 - " + e.getMessage();
        }
    }
}

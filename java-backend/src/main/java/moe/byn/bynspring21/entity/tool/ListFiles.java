package moe.byn.bynspring21.entity.tool;

import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.utils.ClawWorkspaceUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Component
public class ListFiles {

    private static final int MAX_ENTRIES = 200;

    @Autowired
    private ClawWorkspaceUtil clawWorkspaceUtil;

    @Tool(name = "list_files", description = "列出当前工作区内指定目录的一层内容（不递归），标注文件/目录类型和文件大小。不传 path 时列出工作区根目录。")
    public String listFiles(
            @ToolParam(description = "目录路径（相对于当前工作区），为空表示工作区根目录", required = false) String path) {
        try {
            Path workspace = clawWorkspaceUtil.resolveCurrentWorkspace();
            Path dir = clawWorkspaceUtil.resolveInWorkspace(workspace, path);
            if (!Files.exists(dir)) {
                return "错误：目录不存在 - " + (path != null ? path : ".");
            }
            if (!Files.isDirectory(dir)) {
                return "错误：目标不是目录 - " + path;
            }

            List<Path> entries;
            try (Stream<Path> stream = Files.list(dir)) {
                entries = stream.sorted(Comparator.comparing(p -> p.getFileName().toString())).toList();
            }
            if (entries.isEmpty()) {
                return "目录为空";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("目录 ").append(path != null && !path.isBlank() ? path : ".").append(" 共 ")
                    .append(entries.size()).append(" 项：\n");
            int shown = 0;
            for (Path entry : entries) {
                if (shown >= MAX_ENTRIES) {
                    sb.append("...（条目过多，仅显示前 ").append(MAX_ENTRIES).append(" 项）\n");
                    break;
                }
                if (Files.isDirectory(entry)) {
                    sb.append("[目录] ").append(entry.getFileName()).append("/\n");
                } else {
                    sb.append("[文件] ").append(entry.getFileName())
                            .append("（").append(Files.size(entry)).append(" 字节）\n");
                }
                shown++;
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("list_files 执行失败", e);
            return "错误：列出目录失败 - " + e.getMessage();
        }
    }
}

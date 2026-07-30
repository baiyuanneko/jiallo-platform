package moe.byn.bynspring21.entity.tool;

import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.utils.ClawWorkspaceUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
public class WriteFile {

    @Autowired
    private ClawWorkspaceUtil clawWorkspaceUtil;

    @Tool(name = "write_file", description = "将文本内容写入指定文件（覆盖写），父目录不存在时会自动创建。")
    public String writeFile(
            @ToolParam(description = "文件路径（相对于当前工作区；完全访问级别下也接受绝对路径）", required = true) String path,
            @ToolParam(description = "要写入的完整文本内容", required = true) String content) {
        try {
            Path workspace = clawWorkspaceUtil.resolveCurrentWorkspace();
            Path file = clawWorkspaceUtil.resolvePath(workspace, path);
            if (file.equals(workspace)) {
                return "错误：path 必须指向文件，不能是工作区根目录";
            }
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            byte[] bytes = (content != null ? content : "").getBytes(StandardCharsets.UTF_8);
            Files.write(file, bytes);
            return "文件写入成功：" + path + "（" + bytes.length + " 字节）";
        } catch (Exception e) {
            log.error("write_file 执行失败", e);
            return "错误：写入文件失败 - " + e.getMessage();
        }
    }
}

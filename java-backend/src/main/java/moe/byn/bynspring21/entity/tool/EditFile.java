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
public class EditFile {

    private static final long MAX_EDITABLE_FILE_SIZE = 1024 * 1024;

    @Autowired
    private ClawWorkspaceUtil clawWorkspaceUtil;

    @Tool(name = "edit_file", description = "对指定文件做精确字符串替换：将 oldString 替换为 newString。" +
            "oldString 必须在文件中唯一命中，否则替换失败，请提供足够的上下文保证唯一性。")
    public String editFile(
            @ToolParam(description = "文件路径（相对于当前工作区；完全访问级别下也接受绝对路径）", required = true) String path,
            @ToolParam(description = "要被替换的原始字符串（必须在文件中唯一出现）", required = true) String oldString,
            @ToolParam(description = "替换后的新字符串", required = true) String newString) {
        try {
            if (oldString == null || oldString.isEmpty()) {
                return "错误：oldString 不能为空";
            }
            Path workspace = clawWorkspaceUtil.resolveCurrentWorkspace();
            Path file = clawWorkspaceUtil.resolvePath(workspace, path);
            if (!Files.exists(file) || !Files.isRegularFile(file)) {
                return "错误：文件不存在 - " + path;
            }
            if (Files.size(file) > MAX_EDITABLE_FILE_SIZE) {
                return "错误：文件过大（超过 1MB），请改用 write_file 重写或 execute_shell_command 处理";
            }

            String text;
            try {
                text = Files.readString(file, StandardCharsets.UTF_8);
            } catch (java.nio.charset.MalformedInputException e) {
                return "错误：文件不是有效的 UTF-8 文本 - " + path;
            }

            int occurrences = countOccurrences(text, oldString);
            if (occurrences == 0) {
                return "错误：未在文件中找到要替换的 oldString 内容，请先用 read_file 确认文件实际内容（注意空格与换行需完全一致）";
            }
            if (occurrences > 1) {
                return "错误：oldString 在文件中命中 " + occurrences + " 处，无法确定唯一替换位置，请扩大 oldString 的上下文范围后重试";
            }

            String replaced = text.substring(0, text.indexOf(oldString))
                    + (newString != null ? newString : "")
                    + text.substring(text.indexOf(oldString) + oldString.length());
            Files.writeString(file, replaced, StandardCharsets.UTF_8);
            return "文件修改成功：" + path;
        } catch (Exception e) {
            log.error("edit_file 执行失败", e);
            return "错误：修改文件失败 - " + e.getMessage();
        }
    }

    private int countOccurrences(String text, String target) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(target, index)) != -1) {
            count++;
            index += target.length();
        }
        return count;
    }
}

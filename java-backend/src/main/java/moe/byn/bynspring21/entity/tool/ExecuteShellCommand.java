package moe.byn.bynspring21.entity.tool;

import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.utils.ClawWorkspaceUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ExecuteShellCommand {

    private static final int MAX_OUTPUT_CHARS = 30 * 1024;
    private static final long DEFAULT_TIMEOUT_SECONDS = 60;
    private static final long MAX_TIMEOUT_SECONDS = 600;

    @Autowired
    private ClawWorkspaceUtil clawWorkspaceUtil;

    @Tool(name = "execute_shell_command", description = "在当前工作区目录中以 bash 执行 shell 命令，返回标准输出、标准错误和退出码。" +
            "适用于编译构建、运行脚本、安装依赖、文件批处理等场景。命令的工作目录固定为当前工作区。")
    public String executeShellCommand(
            @ToolParam(description = "要执行的 bash 命令", required = true) String command,
            @ToolParam(description = "超时时间（秒），默认 60，最大 600", required = false) Long timeoutSeconds) {
        try {
            Path workspace = clawWorkspaceUtil.resolveCurrentWorkspace();
            long timeout = (timeoutSeconds != null && timeoutSeconds > 0)
                    ? Math.min(timeoutSeconds, MAX_TIMEOUT_SECONDS)
                    : DEFAULT_TIMEOUT_SECONDS;

            Process process = new ProcessBuilder("bash", "-c", command)
                    .directory(workspace.toFile())
                    .start();

            // stdout/stderr 分别异步读取，避免管道缓冲区写满导致子进程阻塞
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();
            Thread stdoutReader = new Thread(() -> drainStream(process.getInputStream(), stdout));
            Thread stderrReader = new Thread(() -> drainStream(process.getErrorStream(), stderr));
            stdoutReader.start();
            stderrReader.start();

            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                stdoutReader.join(1000);
                stderrReader.join(1000);
                return "错误：命令执行超时（" + timeout + " 秒），进程已被强制终止\n" + buildOutput(stdout, stderr, null);
            }

            stdoutReader.join();
            stderrReader.join();
            return buildOutput(stdout, stderr, process.exitValue());
        } catch (Exception e) {
            log.error("execute_shell_command 执行失败", e);
            return "错误：命令执行失败 - " + e.getMessage();
        }
    }

    private void drainStream(InputStream in, StringBuilder out) {
        try (InputStream stream = in) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                // 超过截断上限后继续丢弃读取，保证子进程不因管道写满而阻塞
                if (out.length() < MAX_OUTPUT_CHARS) {
                    out.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
                }
            }
        } catch (Exception ignored) {
        }
    }

    private String buildOutput(StringBuilder stdout, StringBuilder stderr, Integer exitCode) {
        StringBuilder sb = new StringBuilder();
        if (exitCode != null) {
            sb.append("exitCode: ").append(exitCode).append("\n");
        }
        appendSection(sb, "stdout", stdout);
        appendSection(sb, "stderr", stderr);
        return sb.toString();
    }

    private void appendSection(StringBuilder sb, String name, StringBuilder content) {
        if (content.isEmpty()) {
            return;
        }
        boolean truncated = content.length() > MAX_OUTPUT_CHARS;
        String text = truncated ? content.substring(0, MAX_OUTPUT_CHARS) : content.toString();
        sb.append(name).append(":\n").append(text);
        if (!text.endsWith("\n")) {
            sb.append("\n");
        }
        if (truncated) {
            sb.append("...（输出过长，已截断）\n");
        }
    }
}

package moe.byn.bynspring21.utils;

import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.entity.base.ClawSecurityLevel;
import moe.byn.bynspring21.entity.tool.ClawToolContext;
import moe.byn.bynspring21.exception.BynBaseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Claw-like 工作区工具类：工作区根路径为 {workspace-root}/{userId}/（用户级隔离，不按会话隔离），
 * 所有文件路径归一化到工作区内，防止 ../ 路径穿越
 */
@Slf4j
@Component
public class ClawWorkspaceUtil {

    @Value("${claw.like.workspace-root:./claw-workspace}")
    private String workspaceRoot;

    /**
     * 解析并创建当前上下文用户对应的工作区目录
     */
    public Path resolveCurrentWorkspace() {
        String userId = ClawToolContext.getUserId();
        if (userId == null) {
            throw new BynBaseException("Claw-like 工具上下文未初始化");
        }
        return resolveWorkspace(userId);
    }

    /**
     * 解析并创建指定用户对应的工作区目录（不依赖 ThreadLocal 上下文，供文件管理 API 使用）
     */
    public Path resolveWorkspace(String userId) {
        Path workspace = Paths.get(workspaceRoot).toAbsolutePath().normalize()
                .resolve(userId).normalize();
        try {
            Files.createDirectories(workspace);
        } catch (IOException e) {
            log.error("创建 Claw-like 工作区目录失败: {}", workspace, e);
            throw new BynBaseException("创建工作区目录失败");
        }
        return workspace;
    }

    /**
     * 将工具传入的相对路径归一化到工作区内，越界路径直接抛异常
     */
    public Path resolveInWorkspace(Path workspace, String relativePath) {
        if (relativePath == null || relativePath.isBlank() || ".".equals(relativePath)) {
            return workspace;
        }
        Path resolved = workspace.resolve(relativePath).normalize().toAbsolutePath();
        if (!resolved.startsWith(workspace)) {
            throw new BynBaseException("路径越界：只允许访问当前工作区目录内的文件");
        }
        return resolved;
    }

    /**
     * 按当前上下文安全级别解析路径：完全访问（FULL）级别下绝对路径与 .. 直接放行，
     * 其余级别限定在工作区内（同 resolveInWorkspace）
     */
    public Path resolvePath(Path workspace, String path) {
        Integer level = ClawToolContext.getLevel();
        if (level == null || level != ClawSecurityLevel.FULL.getCode()) {
            return resolveInWorkspace(workspace, path);
        }
        if (path == null || path.isBlank() || ".".equals(path)) {
            return workspace;
        }
        Path raw = Paths.get(path);
        if (raw.isAbsolute()) {
            return raw.normalize().toAbsolutePath();
        }
        return workspace.resolve(path).normalize().toAbsolutePath();
    }
}

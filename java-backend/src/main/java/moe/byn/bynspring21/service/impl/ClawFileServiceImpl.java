package moe.byn.bynspring21.service.impl;

import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.entity.AgenticChatSession;
import moe.byn.bynspring21.entity.User;
import moe.byn.bynspring21.entity.base.ClawSecurityLevel;
import moe.byn.bynspring21.entity.dto.ClawFileItemDto;
import moe.byn.bynspring21.exception.BynBaseException;
import moe.byn.bynspring21.security.SecurityUtil;
import moe.byn.bynspring21.service.AgenticChatSessionService;
import moe.byn.bynspring21.service.ClawFileService;
import moe.byn.bynspring21.service.ClawLikeSecurityLevelService;
import moe.byn.bynspring21.service.UserGroupMemberService;
import moe.byn.bynspring21.utils.ClawWorkspaceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Claw-like 文件管理服务实现
 */
@Slf4j
@Service
public class ClawFileServiceImpl implements ClawFileService {

    private static final Integer CLAW_LIKE_AGENT_TYPE = 3;

    /**
     * 上传文件大小上限（20MB）
     */
    private static final long MAX_UPLOAD_SIZE = 20L * 1024 * 1024;

    @Autowired
    private ClawWorkspaceUtil clawWorkspaceUtil;

    @Autowired
    private AgenticChatSessionService agenticChatSessionService;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private ClawLikeSecurityLevelService clawLikeSecurityLevelService;

    @Autowired
    private UserGroupMemberService userGroupMemberService;

    // ==================== 工作区 scope ====================

    @Override
    public List<ClawFileItemDto> listWorkspaceFiles(String sessionId, String path) {
        Path workspace = resolveOwnedWorkspace(sessionId);
        return listDir(clawWorkspaceUtil.resolveInWorkspace(workspace, path));
    }

    @Override
    public ResponseEntity<Resource> downloadWorkspaceFile(String sessionId, String path) {
        Path workspace = resolveOwnedWorkspace(sessionId);
        Path target = clawWorkspaceUtil.resolveInWorkspace(workspace, path);
        requireNotRoot(workspace, target);
        return downloadFile(target);
    }

    @Override
    public void uploadWorkspaceFile(String sessionId, String path, MultipartFile file) {
        Path workspace = resolveOwnedWorkspace(sessionId);
        uploadFile(clawWorkspaceUtil.resolveInWorkspace(workspace, path), file);
    }

    @Override
    public void renameWorkspaceFile(String sessionId, String path, String newName) {
        Path workspace = resolveOwnedWorkspace(sessionId);
        Path target = clawWorkspaceUtil.resolveInWorkspace(workspace, path);
        requireNotRoot(workspace, target);
        renameFile(target, newName);
    }

    @Override
    public void deleteWorkspaceFile(String sessionId, String path) {
        Path workspace = resolveOwnedWorkspace(sessionId);
        Path target = clawWorkspaceUtil.resolveInWorkspace(workspace, path);
        requireNotRoot(workspace, target);
        deletePath(target);
    }

    // ==================== 服务器 scope ====================

    @Override
    public List<ClawFileItemDto> listServerFiles(String path) {
        requireFullAccess();
        return listDir(resolveServerPath(path));
    }

    @Override
    public ResponseEntity<Resource> downloadServerFile(String path) {
        requireFullAccess();
        Path target = resolveServerPath(path);
        requireNotFsRoot(target);
        return downloadFile(target);
    }

    @Override
    public void uploadServerFile(String path, MultipartFile file) {
        requireFullAccess();
        uploadFile(resolveServerPath(path), file);
    }

    @Override
    public void renameServerFile(String path, String newName) {
        requireFullAccess();
        Path target = resolveServerPath(path);
        requireNotFsRoot(target);
        renameFile(target, newName);
    }

    @Override
    public void deleteServerFile(String path) {
        requireFullAccess();
        Path target = resolveServerPath(path);
        requireNotFsRoot(target);
        deletePath(target);
    }

    // ==================== 私有方法 ====================

    /**
     * 校验会话归属（createUser 为当前用户且 agentType=3），返回该会话的工作区目录
     */
    private Path resolveOwnedWorkspace(String sessionId) {
        String userId = securityUtil.getUserId();
        AgenticChatSession session = agenticChatSessionService.getById(sessionId);
        if (session == null) {
            throw new BynBaseException("会话不存在");
        }
        if (!userId.equals(session.getCreateUser())) {
            throw new BynBaseException("无权操作此会话");
        }
        if (!CLAW_LIKE_AGENT_TYPE.equals(session.getAgentType())) {
            throw new BynBaseException("该会话不是 Claw-like 会话");
        }
        return clawWorkspaceUtil.resolveWorkspace(userId, sessionId);
    }

    /**
     * 服务器 scope 每次请求重新校验完全访问（FULL）授权，非 FULL 抛 403
     */
    private void requireFullAccess() {
        User currentUser = securityUtil.getUser();
        List<String> userGroupIds = userGroupMemberService.getUserGroupIds(currentUser.getId());
        int level = clawLikeSecurityLevelService.resolveEffectiveLevel(currentUser.getRoleType(), userGroupIds);
        if (level != ClawSecurityLevel.FULL.getCode()) {
            throw new BynBaseException("当前安全级别不允许访问服务器文件，需要完全访问授权", 403);
        }
    }

    /**
     * 解析服务器路径（默认 /），必须为绝对路径，normalize 后不做工作区 confine
     */
    private Path resolveServerPath(String path) {
        if (!StringUtils.hasText(path)) {
            return Paths.get("/").normalize();
        }
        Path resolved = Paths.get(path).normalize();
        if (!resolved.isAbsolute()) {
            throw new BynBaseException("路径必须为绝对路径");
        }
        return resolved;
    }

    /**
     * 禁止对工作区根目录执行下载、重命名、删除
     */
    private void requireNotRoot(Path base, Path target) {
        if (target.equals(base) || target.getParent() == null) {
            throw new BynBaseException("不能对根目录执行该操作");
        }
    }

    /**
     * 禁止对文件系统根目录执行下载、重命名、删除
     */
    private void requireNotFsRoot(Path target) {
        if (target.getParent() == null) {
            throw new BynBaseException("不能对根目录执行该操作");
        }
    }

    /**
     * 列举目录单层内容，目录在前、按名称排序
     */
    private List<ClawFileItemDto> listDir(Path dir) {
        if (!Files.isDirectory(dir)) {
            throw new BynBaseException("目录不存在");
        }
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.map(p -> {
                        try {
                            boolean isDir = Files.isDirectory(p);
                            return ClawFileItemDto.builder()
                                    .name(p.getFileName().toString())
                                    .isDir(isDir)
                                    .size(isDir ? 0L : Files.size(p))
                                    .updateTime(Files.getLastModifiedTime(p).toMillis())
                                    .build();
                        } catch (IOException e) {
                            throw new BynBaseException("读取文件信息失败: " + e.getMessage());
                        }
                    })
                    .sorted(Comparator.comparing(ClawFileItemDto::getIsDir).reversed()
                            .thenComparing(ClawFileItemDto::getName))
                    .toList();
        } catch (IOException e) {
            log.error("读取目录失败: {}", dir, e);
            throw new BynBaseException("读取目录失败");
        }
    }

    /**
     * 下载文件（仅文件），流式返回带 Content-Disposition: attachment 的响应，避免大文件整读入内存
     */
    private ResponseEntity<Resource> downloadFile(Path file) {
        if (!Files.isRegularFile(file)) {
            throw new BynBaseException("文件不存在或不是普通文件");
        }
        try {
            InputStreamResource resource = new InputStreamResource(Files.newInputStream(file));
            String filename = URLEncoder.encode(file.getFileName().toString(), StandardCharsets.UTF_8)
                    .replace("+", "%20");
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(Files.size(file))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                    .body(resource);
        } catch (IOException e) {
            log.error("读取文件失败: {}", file, e);
            throw new BynBaseException("读取文件失败");
        }
    }

    /**
     * 上传文件到目标目录，限制 20MB，文件名净化后写入
     */
    private void uploadFile(Path dir, MultipartFile file) {
        if (file == null) {
            throw new BynBaseException("上传文件不能为空");
        }
        if (file.getSize() > MAX_UPLOAD_SIZE) {
            throw new BynBaseException("文件大小不能超过 20MB");
        }
        if (!Files.isDirectory(dir)) {
            throw new BynBaseException("目标目录不存在");
        }
        String filename = sanitizeFilename(file.getOriginalFilename());
        Path target = dir.resolve(filename).normalize();
        if (!target.startsWith(dir)) {
            throw new BynBaseException("文件名不合法");
        }
        try {
            file.transferTo(target);
        } catch (IOException e) {
            log.error("上传文件写入失败: {}", target, e);
            throw new BynBaseException("文件写入失败");
        }
    }

    /**
     * 重命名文件或目录，newName 不允许包含 /、\、..
     */
    private void renameFile(Path target, String newName) {
        if (!StringUtils.hasText(newName)
                || newName.contains("/") || newName.contains("\\") || newName.contains("..")) {
            throw new BynBaseException("newName 不合法：不允许包含 /、\\、..");
        }
        if (!Files.exists(target)) {
            throw new BynBaseException("文件或目录不存在");
        }
        Path newTarget = target.getParent().resolve(newName).normalize();
        if (Files.exists(newTarget)) {
            throw new BynBaseException("目标名称已存在");
        }
        try {
            Files.move(target, newTarget);
        } catch (IOException e) {
            log.error("重命名失败: {} -> {}", target, newTarget, e);
            throw new BynBaseException("重命名失败");
        }
    }

    /**
     * 删除文件或目录（目录递归删除）
     */
    private void deletePath(Path target) {
        if (!Files.exists(target)) {
            throw new BynBaseException("文件或目录不存在");
        }
        try {
            if (Files.isDirectory(target)) {
                try (Stream<Path> walk = Files.walk(target)) {
                    walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            throw new BynBaseException("删除失败: " + e.getMessage());
                        }
                    });
                }
            } else {
                Files.delete(target);
            }
        } catch (IOException e) {
            log.error("删除失败: {}", target, e);
            throw new BynBaseException("删除失败");
        }
    }

    /**
     * 净化上传文件名：去除路径部分，拒绝空名与 .、..
     */
    private String sanitizeFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            throw new BynBaseException("文件名不能为空");
        }
        String sanitized = filename.replace('\\', '/');
        int idx = sanitized.lastIndexOf('/');
        if (idx >= 0) {
            sanitized = sanitized.substring(idx + 1);
        }
        if (sanitized.isBlank() || ".".equals(sanitized) || "..".equals(sanitized)) {
            throw new BynBaseException("文件名不合法");
        }
        return sanitized;
    }
}

package moe.byn.bynspring21.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import moe.byn.bynspring21.entity.base.R;
import moe.byn.bynspring21.entity.dto.ClawFileItemDto;
import moe.byn.bynspring21.entity.vo.ClawFileVo;
import moe.byn.bynspring21.service.ClawFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("agenticChatApp")
@Tag(name = "ClawFile", description = "Claw-like 文件管理（工作区/服务器）")
@Validated
public class ClawFileController {

    @Autowired
    private ClawFileService clawFileService;

    // ==================== 工作区 scope ====================

    @Operation(
            summary = "列举 Claw-like 工作区目录",
            description = "单层列举指定会话工作区内的文件和目录，路径限定在工作区内",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @GetMapping("clawFiles/list")
    public R<List<ClawFileItemDto>> listWorkspaceFiles(
            @Parameter(description = "会话ID（可选；工作区按用户隔离，仅传入时用于校验会话归属）") @RequestParam(required = false) String sessionId,
            @Parameter(description = "工作区相对路径，为空表示根目录") @RequestParam(required = false) String path) {
        return R.ok(clawFileService.listWorkspaceFiles(sessionId, path));
    }

    @Operation(
            summary = "下载 Claw-like 工作区文件",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @GetMapping("clawFiles/download")
    public ResponseEntity<Resource> downloadWorkspaceFile(
            @Parameter(description = "会话ID（可选；工作区按用户隔离，仅传入时用于校验会话归属）") @RequestParam(required = false) String sessionId,
            @Parameter(description = "工作区相对路径", required = true) @RequestParam String path) {
        return clawFileService.downloadWorkspaceFile(sessionId, path);
    }

    @Operation(
            summary = "上传文件到 Claw-like 工作区",
            description = "上传文件到指定会话工作区的目标目录，文件大小不超过 20MB",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("clawFiles/upload")
    public R<Void> uploadWorkspaceFile(
            @Parameter(description = "会话ID（可选；工作区按用户隔离，仅传入时用于校验会话归属）") @RequestParam(required = false) String sessionId,
            @Parameter(description = "目标目录的工作区相对路径，为空表示根目录") @RequestParam(required = false) String path,
            @Parameter(description = "上传文件（最大 20MB）", required = true) @RequestParam("file") MultipartFile file) {
        clawFileService.uploadWorkspaceFile(sessionId, path, file);
        return R.ok();
    }

    @Operation(
            summary = "重命名 Claw-like 工作区文件或目录",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("clawFiles/rename")
    public R<Void> renameWorkspaceFile(@Validated @RequestBody ClawFileVo.RenameWorkspaceFileVo vo) {
        clawFileService.renameWorkspaceFile(vo.getSessionId(), vo.getPath(), vo.getNewName());
        return R.ok();
    }

    @Operation(
            summary = "删除 Claw-like 工作区文件或目录",
            description = "删除指定文件或目录，目录递归删除",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("clawFiles/delete")
    public R<Void> deleteWorkspaceFile(@Validated @RequestBody ClawFileVo.DeleteWorkspaceFileVo vo) {
        clawFileService.deleteWorkspaceFile(vo.getSessionId(), vo.getPath());
        return R.ok();
    }

    // ==================== 服务器 scope ====================

    @Operation(
            summary = "列举服务器目录",
            description = "单层列举服务器上的文件和目录，需要完全访问（FULL）授权",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @GetMapping("clawServerFiles/list")
    public R<List<ClawFileItemDto>> listServerFiles(
            @Parameter(description = "绝对路径，为空表示 /") @RequestParam(required = false) String path) {
        return R.ok(clawFileService.listServerFiles(path));
    }

    @Operation(
            summary = "下载服务器文件",
            description = "需要完全访问（FULL）授权",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @GetMapping("clawServerFiles/download")
    public ResponseEntity<Resource> downloadServerFile(
            @Parameter(description = "绝对路径", required = true) @RequestParam String path) {
        return clawFileService.downloadServerFile(path);
    }

    @Operation(
            summary = "上传文件到服务器目录",
            description = "上传文件到服务器上的目标目录，文件大小不超过 20MB，需要完全访问（FULL）授权",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("clawServerFiles/upload")
    public R<Void> uploadServerFile(
            @Parameter(description = "目标目录的绝对路径，为空表示 /") @RequestParam(required = false) String path,
            @Parameter(description = "上传文件（最大 20MB）", required = true) @RequestParam("file") MultipartFile file) {
        clawFileService.uploadServerFile(path, file);
        return R.ok();
    }

    @Operation(
            summary = "重命名服务器文件或目录",
            description = "需要完全访问（FULL）授权",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("clawServerFiles/rename")
    public R<Void> renameServerFile(@Validated @RequestBody ClawFileVo.RenameServerFileVo vo) {
        clawFileService.renameServerFile(vo.getPath(), vo.getNewName());
        return R.ok();
    }

    @Operation(
            summary = "删除服务器文件或目录",
            description = "删除指定文件或目录，目录递归删除，需要完全访问（FULL）授权",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("clawServerFiles/delete")
    public R<Void> deleteServerFile(@Validated @RequestBody ClawFileVo.DeleteServerFileVo vo) {
        clawFileService.deleteServerFile(vo.getPath());
        return R.ok();
    }
}

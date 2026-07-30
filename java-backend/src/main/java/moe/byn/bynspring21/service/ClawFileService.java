package moe.byn.bynspring21.service;

import moe.byn.bynspring21.entity.dto.ClawFileItemDto;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Claw-like 文件管理服务：工作区 scope 限定在会话工作区内（需会话归属校验），
 * 服务器 scope 可访问整个文件系统（每次请求重新校验完全访问授权）
 */
public interface ClawFileService {

    List<ClawFileItemDto> listWorkspaceFiles(String sessionId, String path);

    ResponseEntity<Resource> downloadWorkspaceFile(String sessionId, String path);

    void uploadWorkspaceFile(String sessionId, String path, MultipartFile file);

    void renameWorkspaceFile(String sessionId, String path, String newName);

    void deleteWorkspaceFile(String sessionId, String path);

    List<ClawFileItemDto> listServerFiles(String path);

    ResponseEntity<Resource> downloadServerFile(String path);

    void uploadServerFile(String path, MultipartFile file);

    void renameServerFile(String path, String newName);

    void deleteServerFile(String path);
}

package moe.byn.bynspring21.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import moe.byn.bynspring21.entity.AgenticChatSession;
import moe.byn.bynspring21.entity.AgenticChatSharedSession;
import moe.byn.bynspring21.entity.base.R;
import moe.byn.bynspring21.entity.vo.AgenticChatAppVo;
import moe.byn.bynspring21.entity.vo.PyodideToolResultVo;
import moe.byn.bynspring21.service.AgenticChatAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("agenticChatApp")
@Tag(name = "AgenticChatApp", description = "Agentic 聊天应用")
@Validated
public class AgenticChatAppController {

    @Autowired
    private AgenticChatAppService agenticChatAppService;

    @Operation(summary = "聊天流式对话")
    @PostMapping(value = "chatStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Validated @RequestBody AgenticChatAppVo.ChatStreamVo vo) {
        return agenticChatAppService.chatStream(vo);
    }

    @Operation(
            summary = "获取当前用户可用的 agentType 列表",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @GetMapping("availableAgentTypes")
    public R<java.util.List<Integer>> getAvailableAgentTypes() {
        return R.ok(agenticChatAppService.getAvailableAgentTypes());
    }

    @Operation(summary = "分页查询会话列表")
    @PostMapping("sessions/page")
    public R<Page<AgenticChatSession>> pageAgenticChatSessions(
            @Validated @RequestBody AgenticChatAppVo.PageSessionsVo vo) {
        return R.ok(agenticChatAppService.pageAgenticChatSessions(vo));
    }

    @Operation(summary = "获取会话详情（含消息列表）")
    @GetMapping("sessions/{sessionId}")
    public R<AgenticChatSession> getAgenticChatSession(
            @Parameter(description = "会话ID", required = true) @PathVariable String sessionId) {
        return R.ok(agenticChatAppService.getAgenticChatSession(sessionId));
    }

    @Operation(summary = "获取消息媒体文件")
    @GetMapping("getMessageMedia")
    public ResponseEntity<byte[]> getMessageMedia(
            @Parameter(description = "用户ID", required = true) @RequestParam String userId,
            @Parameter(description = "媒体文件名", required = true) @RequestParam String mediaFilename) {
        return agenticChatAppService.getMessageMedia(userId, mediaFilename);
    }

    @Operation(summary = "编辑消息")
    @PostMapping("messages/edit")
    public R<Void> editMessage(@Validated @RequestBody AgenticChatAppVo.EditMessageVo vo) {
        agenticChatAppService.editMessage(vo);
        return R.ok();
    }

    @Operation(summary = "删除消息")
    @PostMapping("messages/delete")
    public R<Void> deleteMessage(@Validated @RequestBody AgenticChatAppVo.DeleteMessageVo vo) {
        agenticChatAppService.deleteMessage(vo);
        return R.ok();
    }

    @Operation(summary = "编辑会话名称")
    @PostMapping("sessions/editName")
    public R<Void> editSessionName(@Validated @RequestBody AgenticChatAppVo.EditSessionNameVo vo) {
        agenticChatAppService.editSessionName(vo);
        return R.ok();
    }

    @Operation(summary = "删除会话")
    @PostMapping("sessions/delete")
    public R<Void> deleteSession(@Validated @RequestBody AgenticChatAppVo.DeleteSessionVo vo) {
        agenticChatAppService.deleteSession(vo);
        return R.ok();
    }

    @Operation(summary = "取消指定 streamUniqueKey 的聊天流式响应，已流式内容会存表并返回会话和消息ID")
    @PostMapping("stopChatStream")
    public R<Map<String, String>> stopChatStream(@Validated @RequestBody AgenticChatAppVo.StopChatStreamVo vo) {
        Map<String, String> result = agenticChatAppService.stopChatStream(vo.getStreamUniqueKey());
        return R.ok(result);
    }

    @Operation(summary = "提交 Pyodide 代码执行结果")
    @PostMapping("pyodideToolResult")
    public R<Void> submitPyodideToolResult(@Validated @RequestBody PyodideToolResultVo vo) {
        agenticChatAppService.submitPyodideToolResult(vo);
        return R.ok();
    }

    @Operation(summary = "分享会话")
    @PostMapping("sessions/share")
    public R<String> shareSession(@Validated @RequestBody AgenticChatAppVo.ShareSessionVo vo) {
        return R.ok(agenticChatAppService.shareSession(vo));
    }

    @Operation(summary = "取消分享会话")
    @PostMapping("sessions/unshare")
    public R<Void> unshareSession(@Validated @RequestBody AgenticChatAppVo.ShareSessionVo vo) {
        agenticChatAppService.unshareSession(vo.getSessionId());
        return R.ok();
    }

    @Operation(summary = "重新分享会话")
    @PostMapping("sessions/reshare")
    public R<String> reshareSession(@Validated @RequestBody AgenticChatAppVo.ShareSessionVo vo) {
        return R.ok(agenticChatAppService.reshareSession(vo.getSessionId()));
    }

    @Operation(summary = "获取分享的会话详情（公开接口）")
    @GetMapping("sessions/shared/{sharedSessionId}")
    public R<AgenticChatSharedSession> getSharedSession(
            @Parameter(description = "分享会话ID", required = true) @PathVariable String sharedSessionId) {
        return R.ok(agenticChatAppService.getSharedSession(sharedSessionId));
    }

    @Operation(summary = "分页查询当前用户已分享的会话列表")
    @PostMapping("sessions/shared/page")
    public R<Page<AgenticChatSharedSession>> pageSharedSessions(
            @Validated @RequestBody AgenticChatAppVo.PageSessionsVo vo) {
        return R.ok(agenticChatAppService.pageSharedSessions(vo));
    }
}

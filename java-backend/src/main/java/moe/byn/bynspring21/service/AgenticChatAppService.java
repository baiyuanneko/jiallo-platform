package moe.byn.bynspring21.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import moe.byn.bynspring21.entity.AgenticChatSession;
import moe.byn.bynspring21.entity.AgenticChatSharedSession;
import moe.byn.bynspring21.entity.vo.AgenticChatAppVo;
import moe.byn.bynspring21.entity.vo.PyodideToolResultVo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

public interface AgenticChatAppService {

    SseEmitter chatStream(AgenticChatAppVo.ChatStreamVo vo);

    List<Integer> getAvailableAgentTypes();

    Page<AgenticChatSession> pageAgenticChatSessions(AgenticChatAppVo.PageSessionsVo vo);

    AgenticChatSession getAgenticChatSession(String sessionId);

    ResponseEntity<byte[]> getMessageMedia(String userId, String mediaFilename);

    void editMessage(AgenticChatAppVo.EditMessageVo vo);

    void deleteMessage(AgenticChatAppVo.DeleteMessageVo vo);

    void editSessionName(AgenticChatAppVo.EditSessionNameVo vo);

    void deleteSession(AgenticChatAppVo.DeleteSessionVo vo);

    void submitPyodideToolResult(PyodideToolResultVo vo);

    Map<String, String> stopChatStream(String streamUniqueKey);

    String shareSession(AgenticChatAppVo.ShareSessionVo vo);

    void unshareSession(String sessionId);

    String reshareSession(String sessionId);

    AgenticChatSharedSession getSharedSession(String sharedSessionId);

    Page<AgenticChatSharedSession> pageSharedSessions(AgenticChatAppVo.PageSessionsVo vo);
}

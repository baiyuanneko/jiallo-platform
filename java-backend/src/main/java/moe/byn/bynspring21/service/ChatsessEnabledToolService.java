package moe.byn.bynspring21.service;

import com.baomidou.mybatisplus.extension.service.IService;
import moe.byn.bynspring21.entity.ChatsessEnabledTool;
import moe.byn.bynspring21.entity.base.AgenticToolEnum;

import java.util.List;

public interface ChatsessEnabledToolService extends IService<ChatsessEnabledTool> {

    void saveEnabledTools(String sessionId, List<AgenticToolEnum> tools, String userId);

    List<AgenticToolEnum> getEnabledTools(String sessionId);

    void updateEnabledTools(String sessionId, List<AgenticToolEnum> tools, String userId);
}

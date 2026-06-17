package moe.byn.bynspring21.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import moe.byn.bynspring21.entity.SysLlmModel;
import moe.byn.bynspring21.entity.base.RoleType;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;

public interface SysLlmModelService extends IService<SysLlmModel> {
    void removeModelWithAvailability(String modelId);

    ChatModel createChatModelByModelId(String modelId);

    Page<SysLlmModel> pageAvailableModelsByRole(RoleType roleType, Integer pageNum, Integer pageSize);

    List<SysLlmModel> listAvailableModelsByRole(RoleType roleType);

    Page<SysLlmModel> pageAvailableModelsByRole(RoleType roleType, List<String> groupIds, Integer pageNum, Integer pageSize);

    List<SysLlmModel> listAvailableModelsByRole(RoleType roleType, List<String> groupIds);
}

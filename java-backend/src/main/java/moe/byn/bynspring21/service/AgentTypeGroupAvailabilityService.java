package moe.byn.bynspring21.service;

import com.baomidou.mybatisplus.extension.service.IService;
import moe.byn.bynspring21.entity.AgentTypeGroupAvailability;

import java.util.List;

public interface AgentTypeGroupAvailabilityService extends IService<AgentTypeGroupAvailability> {

    List<String> getAgentTypeAvailableGroups(Integer agentType);

    void authorizeAgentTypeToGroups(Integer agentType, List<String> groupIds);

    void revokeAgentTypeOfGroups(Integer agentType, List<String> groupIds);

    List<Integer> getAvailableAgentTypesForGroups(List<String> groupIds);
}

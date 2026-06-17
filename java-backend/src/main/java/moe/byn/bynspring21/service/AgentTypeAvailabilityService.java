package moe.byn.bynspring21.service;

import com.baomidou.mybatisplus.extension.service.IService;
import moe.byn.bynspring21.entity.AgentTypeAvailability;
import moe.byn.bynspring21.entity.base.RoleType;

import java.util.List;

public interface AgentTypeAvailabilityService extends IService<AgentTypeAvailability> {

    List<Integer> getAvailableAgentTypes(RoleType roleType);

    List<RoleType> getAgentTypeAvailableRoles(Integer agentType);

    void authorizeAgentTypeToRoles(Integer agentType, List<RoleType> roleTypes);

    void revokeAgentTypeOfRoles(Integer agentType, List<RoleType> roleTypes);
}

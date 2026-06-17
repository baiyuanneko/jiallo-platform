package moe.byn.bynspring21.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import moe.byn.bynspring21.entity.AgentTypeAvailability;
import moe.byn.bynspring21.entity.base.RoleType;
import moe.byn.bynspring21.exception.BynBaseException;
import moe.byn.bynspring21.mapper.AgentTypeAvailabilityMapper;
import moe.byn.bynspring21.service.AgentTypeAvailabilityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class AgentTypeAvailabilityServiceImpl extends ServiceImpl<AgentTypeAvailabilityMapper, AgentTypeAvailability> implements AgentTypeAvailabilityService {

    @Override
    public List<Integer> getAvailableAgentTypes(RoleType roleType) {
        return this.list(new LambdaQueryWrapper<AgentTypeAvailability>()
                        .eq(AgentTypeAvailability::getRoleCode, roleType.getValue()))
                .stream()
                .map(AgentTypeAvailability::getAgentType)
                .toList();
    }

    @Override
    public List<RoleType> getAgentTypeAvailableRoles(Integer agentType) {
        return this.list(new LambdaQueryWrapper<AgentTypeAvailability>()
                        .eq(AgentTypeAvailability::getAgentType, agentType))
                .stream()
                .map(AgentTypeAvailability::getRoleCode)
                .map(RoleType::valueOf)
                .filter(roleType -> roleType != null)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void authorizeAgentTypeToRoles(Integer agentType, List<RoleType> roleTypes) {
        List<RoleType> distinctRoleTypes = roleTypes.stream()
                .filter(roleType -> roleType != null)
                .distinct()
                .toList();
        if (distinctRoleTypes.isEmpty()) {
            return;
        }

        List<Integer> roleCodes = distinctRoleTypes.stream()
                .map(RoleType::getValue)
                .toList();
        Set<Integer> existingRoleCodes = this.list(new LambdaQueryWrapper<AgentTypeAvailability>()
                        .eq(AgentTypeAvailability::getAgentType, agentType)
                        .in(AgentTypeAvailability::getRoleCode, roleCodes))
                .stream()
                .map(AgentTypeAvailability::getRoleCode)
                .collect(java.util.stream.Collectors.toSet());

        List<RoleType> duplicatedRoles = distinctRoleTypes.stream()
                .filter(roleType -> existingRoleCodes.contains(roleType.getValue()))
                .toList();
        if (!duplicatedRoles.isEmpty()) {
            throw new BynBaseException("以下角色已经授权该 agentType：" + duplicatedRoles.stream().map(RoleType::getName).toList());
        }

        this.saveBatch(distinctRoleTypes.stream()
                .map(roleType -> AgentTypeAvailability.builder()
                        .agentType(agentType)
                        .roleCode(roleType.getValue())
                        .build())
                .toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeAgentTypeOfRoles(Integer agentType, List<RoleType> roleTypes) {
        List<RoleType> distinctRoleTypes = roleTypes.stream()
                .filter(roleType -> roleType != null)
                .distinct()
                .toList();
        if (distinctRoleTypes.isEmpty()) {
            return;
        }

        List<Integer> roleCodes = distinctRoleTypes.stream()
                .map(RoleType::getValue)
                .toList();
        Set<Integer> existingRoleCodes = this.list(new LambdaQueryWrapper<AgentTypeAvailability>()
                        .eq(AgentTypeAvailability::getAgentType, agentType)
                        .in(AgentTypeAvailability::getRoleCode, roleCodes))
                .stream()
                .map(AgentTypeAvailability::getRoleCode)
                .collect(java.util.stream.Collectors.toSet());

        List<RoleType> missingRoles = distinctRoleTypes.stream()
                .filter(roleType -> !existingRoleCodes.contains(roleType.getValue()))
                .toList();
        if (!missingRoles.isEmpty()) {
            throw new BynBaseException("以下角色尚未授权该 agentType：" + missingRoles.stream().map(RoleType::getName).toList());
        }

        this.remove(new LambdaQueryWrapper<AgentTypeAvailability>()
                .eq(AgentTypeAvailability::getAgentType, agentType)
                .in(AgentTypeAvailability::getRoleCode, roleCodes));
    }
}
